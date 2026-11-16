// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.ui.screens

import io.github.h3yk0.cryo.R
import io.github.h3yk0.cryo.domain.BillMatcher
import io.github.h3yk0.cryo.domain.Res
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.h3yk0.cryo.AppContainer
import io.github.h3yk0.cryo.data.SafFiles
import io.github.h3yk0.cryo.data.db.CategoryKind
import io.github.h3yk0.cryo.data.db.CreditCard
import io.github.h3yk0.cryo.data.db.TxType
import io.github.h3yk0.cryo.domain.Ledger
import io.github.h3yk0.cryo.domain.Snapshot
import io.github.h3yk0.cryo.importer.BalanceCheck
import io.github.h3yk0.cryo.importer.ColumnMap
import io.github.h3yk0.cryo.importer.ImportPlanner
import io.github.h3yk0.cryo.importer.ImportReport
import io.github.h3yk0.cryo.importer.ImportRow
import io.github.h3yk0.cryo.importer.ImportTarget
import io.github.h3yk0.cryo.importer.PdfText
import io.github.h3yk0.cryo.importer.RowFlag
import io.github.h3yk0.cryo.importer.SampleFiles
import io.github.h3yk0.cryo.importer.Statement
import io.github.h3yk0.cryo.importer.StatementFormat
import io.github.h3yk0.cryo.importer.StatementReader
import io.github.h3yk0.cryo.importer.Table
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * O passo a passo da importação: escolher o arquivo, ler, (às vezes) escolher colunas ou digitar a senha do PDF,
 * conferir e importar. Fica num ViewModel para nada se perder ao girar a tela.
 */
class ImportModel(
    private val c: AppContainer,
    private val today: () -> LocalDate = LocalDate::now,
    /** Texto de cada página de um PDF (nos testes, um texto pronto). */
    private val pdfPages: (ByteArray, String?) -> List<String> = { bytes, password -> PdfText.extract(c.app, bytes, password) },
) : ViewModel() {

    sealed interface Step {
        data object Pick : Step
        data object Reading : Step
        data class Password(val retry: Boolean) : Step
        data class Columns(val table: Table, val format: StatementFormat, val guess: ColumnMap?) : Step
        data object Review : Step
        data class Done(
            val count: Int, val ids: List<Long>, val target: ImportTarget, val undone: Boolean = false,
            /** RF-IMP-24: quanto o saldo inicial da conta foi acertado por causa desta importação (0 = nada). */
            val shift: Long = 0,
        ) : Step
    }

    /**
     * O que a pessoa mudou numa linha da conferência. A marcação vale só enquanto a linha continua com o mesmo aviso
     * ([selectedFlag]): se trocar o destino e ela passar a "parecer repetida", volta a ficar desmarcada.
     */
    data class RowEdit(
        val selected: Boolean? = null,
        val selectedFlag: RowFlag? = null,
        val description: String? = null,
        val categoryId: Long? = null,
        val categoryKind: CategoryKind? = null,
        /** Pagamento de fatura: o outro lado escolhido (ou nenhum). */
        val paired: Long? = null,
        val pairedSet: Boolean = false,
        /** RF-CFX-02: a pessoa disse se a linha é (ou não é) o pagamento da conta fixa sugerida. */
        val payBill: Boolean? = null,
    )

    var step: Step by mutableStateOf(Step.Pick)
        private set
    var error: String? by mutableStateOf(null)
        private set
    var fileName: String by mutableStateOf("")
        private set
    var statement: Statement? by mutableStateOf(null)
        private set
    var target: ImportTarget? by mutableStateOf(null)
        private set
    var invert: Boolean by mutableStateOf(false)
        private set
    /** O Cryo virou os sinais sozinho (fatura com compras positivas). */
    var autoInverted: Boolean by mutableStateOf(false)
        private set
    var rows: List<ImportRow> by mutableStateOf(emptyList())
        private set
    var busy: Boolean by mutableStateOf(false)
        private set

    private var bytes: ByteArray? = null
    private var password: String? = null
    private var columns: ColumnMap? = null
    private var planned: List<ImportRow> = emptyList()
    private val edits = HashMap<Int, RowEdit>()
    private var preset: ImportTarget? = null

    /** Já tem algo em andamento (leitura ou conferência) que se perderia ao voltar. */
    val inProgress: Boolean get() = step is Step.Review || step is Step.Columns || step is Step.Password

    fun preset(accountId: Long?, cardId: Long?) {
        preset = when {
            cardId != null -> ImportTarget(cardId = cardId)
            accountId != null -> ImportTarget(accountId = accountId)
            else -> null
        }
    }

    /* ------------------------------------- leitura ------------------------------------- */

    fun open(uri: Uri) {
        reviewShift = 0
        error = null
        fileName = ""
        step = Step.Reading
        viewModelScope.launch {
            val read = withContext(Dispatchers.IO) {
                runCatching {
                    val name = SafFiles.displayName(c.app.contentResolver, uri) ?: uri.lastPathSegment?.substringAfterLast('/') ?: "arquivo"
                    name to SafFiles.readBytes(c.app.contentResolver, uri, StatementReader.MAX_BYTES)
                }
            }
            val (name, data) = read.getOrElse {
                fail(Res.s(R.string.import_nao_consegui_abrir_esse), size = 0)
                return@launch
            }
            if (data == null) {
                fileName = name
                fail(Res.s(R.string.import_arquivo_grande_demais_mais), size = StatementReader.MAX_BYTES.toLong() + 1)
                return@launch
            }
            openBytes(name, data)
        }
    }

    /** Lê um arquivo que já está na memória (também usado pelos testes e pelos arquivos de exemplo). */
    fun openBytes(name: String, data: ByteArray) {
        fileName = name
        bytes = data
        password = null
        columns = null
        edits.clear()
        viewModelScope.launch { parse() }
    }

    private suspend fun parse() {
        val data = bytes ?: return
        error = null
        step = Step.Reading
        val chosen = columns
        val result = withContext(Dispatchers.Default) {
            // O JSON de teste só entra no modo de teste: nunca nos dados de verdade.
            StatementReader.read(data, today(), allowJson = c.sandbox, password = password, columns = chosen, pdfPages = pdfPages)
        }
        when (result) {
            is StatementReader.Result.Ok -> {
                val st = result.statement
                if (!c.sandbox && SampleFiles.isSample(st)) {
                    fail(Res.s(R.string.import_este_extrato_exemplo_cryo), st)
                    return
                }
                if (st.lines.isEmpty()) {
                    report(st, Res.s(R.string.import_nenhuma_movimentacao_encontrada))
                    if (chosen != null) {
                        // Colunas escolhidas pela pessoa que não renderam nada: volta para ela escolher de novo.
                        error = Res.s(R.string.import_com_essas_colunas_nenhuma)
                        step = lastColumns ?: Step.Pick
                    } else {
                        fail(Res.s(R.string.import_nao_encontrei_movimentacoes_nesse), st)
                    }
                    return
                }
                report(st, null)
                startReview(st)
            }
            is StatementReader.Result.NeedsColumns -> {
                report(null, Res.s(R.string.import_cryo_pediu_para_escolher))
                Step.Columns(result.table, result.format, result.guess).also { lastColumns = it; step = it }
            }
            StatementReader.Result.NeedsPassword -> {
                step = Step.Password(retry = password != null)
            }
            is StatementReader.Result.Failed -> fail(result.message)
        }
    }

    private var lastColumns: Step.Columns? = null

    fun submitPassword(p: String) {
        password = p
        viewModelScope.launch { parse() }
    }

    fun useColumns(map: ColumnMap) {
        columns = map
        viewModelScope.launch { parse() }
    }

    /** Volta para o começo (outro arquivo). */
    fun reset() {
        planJob?.cancel()
        planning = false
        step = Step.Pick
        error = null
        bytes = null
        password = null
        columns = null
        statement = null
        rows = emptyList()
        planned = emptyList()
        edits.clear()
        lastColumns = null
    }

    private fun fail(message: String, st: Statement? = null, size: Long? = null) {
        report(st, message, size)
        bytes = null
        step = Step.Pick
        error = message
    }

    /** Guarda como o arquivo foi lido, para o relatório do modo desenvolvedor (só com ele ligado). */
    private fun report(st: Statement?, message: String?, size: Long? = null) {
        if (!c.dev.enabled) {
            c.lastImport.value = null
            return
        }
        c.lastImport.value = ImportReport.of(
            fileName = fileName, size = size ?: bytes?.size?.toLong() ?: 0, at = LocalDateTime.now(),
            statement = st, message = message,
        )
    }

    /* ----------------------------------- conferência ----------------------------------- */

    private suspend fun snapshot(): Snapshot = c.repo.snapshot.value ?: c.repo.loadOnce()

    /** Recalculando a conferência (outra conta, outra fatura, sinais virados): o botão de importar espera. */
    var planning: Boolean by mutableStateOf(false)
        private set

    /** RF-IMP-24: o acerto de saldo feito ao reabrir um extrato que já está inteiro no Cryo (0 = nada). */
    var reviewShift: Long by mutableStateOf(0L)
        private set
    private var planJob: Job? = null

    private suspend fun startReview(st: Statement) {
        statement = st
        val snap = snapshot()
        val accounts = snap.accounts.filter { !it.archived }
        val cards = snap.cards.filter { !it.archived }
        val defaultAccount = c.settings.current().defaultAccountId?.takeIf { id -> accounts.any { it.id == id } }
        val p = preset?.takeIf { t -> t.accountId?.let { id -> accounts.any { it.id == id } } ?: cards.any { it.id == t.cardId } }
        val t = when {
            p != null -> p
            st.isCard && cards.isNotEmpty() -> ImportTarget(cardId = cards.first().id)
            defaultAccount != null -> ImportTarget(accountId = defaultAccount)
            accounts.isNotEmpty() -> ImportTarget(accountId = accounts.first().id)
            cards.isNotEmpty() -> ImportTarget(cardId = cards.first().id)
            else -> null
        }
        if (t == null) {
            fail(Res.s(R.string.import_crie_conta_ou_cartao), st)
            return
        }
        edits.clear()
        planJob?.cancel()
        val (nt, inv) = withDefaults(t, st, snap.cards)
        target = nt
        invert = inv
        autoInverted = inv
        val result = withContext(Dispatchers.Default) { Ledger(snap, today()).let { lg -> ImportPlanner.plan(st, nt, snap, lg.learnedCategories, inv, lg.categoryModel, alias = { d -> c.engineStore.billAlias(d) }) } }
        planned = result
        rows = result.map(::applyEdit)
        syncBalance(st, nt, snap)
        planning = false
        step = Step.Review
    }

    /** No cartão: a fatura sugerida e se os sinais precisam virar (faturas que listam as compras como positivas). */
    private fun withDefaults(t: ImportTarget, st: Statement, cards: List<CreditCard>): Pair<ImportTarget, Boolean> {
        val card = t.cardId?.let { id -> cards.firstOrNull { it.id == id } }
        val nt = if (card != null && t.invoiceYm == null) t.copy(invoiceYm = ImportPlanner.defaultInvoice(st, card)) else t
        return nt to ImportPlanner.suggestInvert(st, nt)
    }

    /**
     * Pede uma conferência nova. O que a pessoa escolheu aparece na hora (destino, fatura, sinais), e as linhas são
     * recalculadas em segundo plano; toques seguidos partem sempre da última escolha, nunca de uma conta pela metade.
     */
    private fun request(t: ImportTarget, inv: Boolean, auto: Boolean) {
        val st = statement ?: return
        target = t
        invert = inv
        autoInverted = auto
        planJob?.cancel()
        planning = true
        planJob = viewModelScope.launch {
            val snap = snapshot()
            val result = withContext(Dispatchers.Default) { Ledger(snap, today()).let { lg -> ImportPlanner.plan(st, t, snap, lg.learnedCategories, inv, lg.categoryModel, alias = { d -> c.engineStore.billAlias(d) }) } }
            planned = result
            rows = result.map(::applyEdit)
            syncBalance(st, t, snap)
            planning = false
        }
    }

    /** Conta ou cartão de destino. */
    fun chooseTarget(t: ImportTarget) {
        val st = statement ?: return
        val cur = target
        if (cur != null && cur.accountId == t.accountId && cur.cardId == t.cardId) return
        // O "outro lado" de um pagamento de fatura muda de sentido entre conta e cartão: escolhe de novo.
        if (cur != null && cur.isCard != t.isCard) edits.replaceAll { _, e -> e.copy(paired = null, pairedSet = false) }
        val (nt, inv) = withDefaults(t, st, c.repo.snapshot.value?.cards.orEmpty())
        request(nt, inv, auto = inv)
    }

    /** Fatura escolhida (ano*100 + mês), ou null = cada compra na fatura da sua data. */
    fun setInvoice(ym: Int?) {
        val t = target ?: return
        if (!t.isCard || t.invoiceYm == ym) return
        request(t.copy(invoiceYm = ym), invert, autoInverted)
    }

    fun toggleInvert() {
        val t = target ?: return
        request(t, !invert, auto = false)
    }

    private fun applyEdit(row: ImportRow): ImportRow {
        val e = edits[row.index] ?: return row
        var r = row
        e.description?.let { r = r.copy(line = r.line.copy(description = it)) }
        if (e.categoryId != null && e.categoryKind == kindOf(r.type)) r = r.copy(categoryId = e.categoryId)
        if (e.pairedSet && r.canBeInvoicePayment) r = r.copy(pairedId = e.paired)
        if (e.selected != null && e.selectedFlag == r.flag) r = r.copy(selected = e.selected)
        if (e.payBill != null && r.bill != null) r = r.copy(payBill = e.payBill)
        return r
    }

    private fun edit(index: Int, change: (RowEdit) -> RowEdit) {
        edits[index] = change(edits[index] ?: RowEdit())
        rows = planned.map(::applyEdit)
    }

    fun toggle(index: Int) {
        if (planning) return
        val row = rows.firstOrNull { it.index == index } ?: return
        edit(index) { it.copy(selected = !row.selected, selectedFlag = row.flag) }
    }

    fun selectAll(selected: Boolean) {
        if (planning) return
        planned.forEach { r -> edits[r.index] = (edits[r.index] ?: RowEdit()).copy(selected = selected, selectedFlag = r.flag) }
        rows = planned.map(::applyEdit)
    }

    /**
     * Salva o que a pessoa mudou numa linha (descrição, categoria, pagamento de fatura e se entra).
     * [categoryKind]: de que tipo era a lista de categorias na tela (se a linha virou de tipo nesse meio-tempo,
     * a categoria escolhida não vale).
     */
    fun saveRow(index: Int, description: String, categoryId: Long?, categoryKind: CategoryKind, paired: Long?, selected: Boolean, payBill: Boolean? = null) {
        val row = rows.firstOrNull { it.index == index } ?: return
        edit(index) {
            it.copy(
                selected = selected,
                selectedFlag = row.flag,
                description = description.trim().ifBlank { row.line.description },
                categoryId = categoryId ?: it.categoryId,
                categoryKind = if (categoryId != null) categoryKind else it.categoryKind,
                paired = paired,
                pairedSet = row.canBeInvoicePayment,
                payBill = payBill ?: it.payBill,
            )
        }
    }

    /** Marcadas que parecem já estar no Cryo (vão ficar duplicadas se entrarem). */
    val selectedDuplicates: Int get() = rows.count { it.selected && (it.flag == RowFlag.IMPORTED_BEFORE || it.flag == RowFlag.LOOKS_DUPLICATE) }

    /**
     * Marcadas que parecem pagamento de fatura, mas sem o outro lado escolhido (ex.: depois de "Marcar todas"): entram
     * como gasto comum na conta (ou crédito no cartão, sem sair de conta nenhuma), e a fatura pode contar duas vezes.
     */
    val selectedUnpairedPayments: Int get() = rows.count { it.selected && it.canBeInvoicePayment && it.pairedId == null }

    val selectedCount: Int get() = rows.count { it.selected }

    /** RF-CFX-02: linhas marcadas que são (ou parecem ser) pagamento de conta fixa. */
    val billRows: Int get() = rows.count { it.selected && it.bill != null }

    /* ------------------------------------- importar ------------------------------------ */

    fun import() {
        val t = target ?: return
        val chosen = rows.filter { it.selected }
        if (chosen.isEmpty() || busy || planning) return
        busy = true
        viewModelScope.launch {
            try {
                val snap = c.repo.loadOnce()
                val cards = snap.cards.associateBy { it.id }
                val card = t.cardId?.let { cards[it] }
                val now = System.currentTimeMillis()
                val txs = chosen.map { ImportPlanner.toTx(it, t, card, now, cards) }
                val ids = c.repo.importTxs(txs)
                // RF-CFX-04: o nome que o banco usa passa a valer para a conta fixa que a pessoa confirmou.
                for (r in chosen) {
                    val b = r.bill?.bill ?: continue
                    if (r.payBill && !BillMatcher.sameName(b.name, r.line.description)) c.engineStore.learnBill(r.line.description, b.id)
                }
                c.lastImport.value = c.lastImport.value?.copy(imported = ids.size)
                // RF-IMP-24: o extrato diz quanto havia na conta antes dele? O Cryo passa a partir do mesmo ponto,
                // sozinho. A tela final mostra o que foi feito e deixa desfazer (PRI-09).
                val shift = statement?.let { st -> t.accountId?.let { BalanceCheck.autoShift(st, snap, it) } } ?: 0L
                if (shift != 0L) c.repo.shiftBalance(t.accountId!!, shift)
                step = Step.Done(ids.size, ids, t, shift = shift)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                c.message(Res.s(R.string.import_nao_consegui_importar_nada))
            } finally {
                busy = false
            }
        }
    }

    /**
     * RF-IMP-24: o extrato aberto já está **inteiro** no Cryo, importado deste mesmo arquivo para esta mesma conta
     * (toda linha "já importada")? Então não há nada a importar, mas o saldo de partida que ele diz continua valendo:
     * o Cryo acerta sozinho. É o caso de quem importou numa versão que ainda não lia o saldo.
     */
    private suspend fun syncBalance(st: Statement, t: ImportTarget, snap: Snapshot) {
        val account = t.accountId ?: return
        if (reviewShift != 0L || rows.isEmpty() || rows.any { it.flag != RowFlag.IMPORTED_BEFORE }) return
        val shift = BalanceCheck.autoShift(st, snap, account) ?: return
        if (shift != 0L && c.repo.shiftBalance(account, shift) != null) reviewShift = shift
    }

    /** RF-IMP-24: o botão "Acertar o saldo" — soma [delta] ao saldo inicial da conta para ficar igual ao banco. */
    fun fixBalance(delta: Long) = shiftBalance(delta)

    /** RF-IMP-24: desfaz o acerto de saldo (o automático e o do botão), na tela final ou na conferência. */
    fun undoShift() = shiftBalance(-((step as? Step.Done)?.shift ?: reviewShift))

    private fun shiftBalance(delta: Long) {
        val d = step as? Step.Done
        val account = (d?.target ?: target)?.accountId ?: return
        if (delta == 0L || d?.undone == true || busy) return
        busy = true
        viewModelScope.launch {
            try {
                if (c.repo.shiftBalance(account, delta) != null) {
                    if (d != null) step = d.copy(shift = d.shift + delta) else reviewShift += delta
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Nada mudou: a tela continua mostrando a diferença e o botão.
            } finally {
                busy = false
            }
        }
    }

    fun undo() {
        val d = step as? Step.Done ?: return
        if (d.undone || busy) return
        busy = true
        viewModelScope.launch {
            try {
                c.repo.undoImport(d.ids)
                // O acerto de saldo veio com esta importação: sai junto com ela.
                if (d.shift != 0L) d.target.accountId?.let { c.repo.shiftBalance(it, -d.shift) }
                c.lastImport.value = c.lastImport.value?.copy(imported = 0)
                step = d.copy(undone = true, shift = 0)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                c.message(Res.s(R.string.import_nao_consegui_desfazer_tente))
            } finally {
                busy = false
            }
        }
    }

    companion object {
        fun kindOf(type: TxType): CategoryKind = if (type == TxType.INCOME) CategoryKind.INCOME else CategoryKind.EXPENSE
    }
}
