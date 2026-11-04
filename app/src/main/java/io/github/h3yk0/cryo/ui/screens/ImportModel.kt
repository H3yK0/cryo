// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.ui.screens

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
        data class Done(val count: Int, val ids: List<Long>, val target: ImportTarget, val undone: Boolean = false) : Step
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
                fail("Não consegui abrir esse arquivo. Tente baixar de novo ou escolher outro.", size = 0)
                return@launch
            }
            if (data == null) {
                fileName = name
                fail("O arquivo é grande demais (mais de 20 MB). Peça ao banco um período menor.", size = StatementReader.MAX_BYTES.toLong() + 1)
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
                    fail("Este é um extrato de exemplo do Cryo (inventado). Ele só pode ser importado no modo de teste.", st)
                    return
                }
                if (st.lines.isEmpty()) {
                    report(st, "Nenhuma movimentação encontrada")
                    if (chosen != null) {
                        // Colunas escolhidas pela pessoa que não renderam nada: volta para ela escolher de novo.
                        error = "Com essas colunas, nenhuma linha virou movimentação. Confira qual coluna tem a data e qual tem o valor."
                        step = lastColumns ?: Step.Pick
                    } else {
                        fail("Não encontrei movimentações nesse arquivo.", st)
                    }
                    return
                }
                report(st, null)
                startReview(st)
            }
            is StatementReader.Result.NeedsColumns -> {
                report(null, "O Cryo pediu para escolher as colunas")
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
            fail("Crie uma conta ou um cartão na Carteira antes de importar.", st)
            return
        }
        edits.clear()
        planJob?.cancel()
        val (nt, inv) = withDefaults(t, st, snap.cards)
        target = nt
        invert = inv
        autoInverted = inv
        val result = withContext(Dispatchers.Default) { ImportPlanner.plan(st, nt, snap, Ledger(snap, today()).learnedCategories, inv) }
        planned = result
        rows = result.map(::applyEdit)
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
            val result = withContext(Dispatchers.Default) { ImportPlanner.plan(st, t, snap, Ledger(snap, today()).learnedCategories, inv) }
            planned = result
            rows = result.map(::applyEdit)
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
    fun saveRow(index: Int, description: String, categoryId: Long?, categoryKind: CategoryKind, paired: Long?, selected: Boolean) {
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
                c.lastImport.value = c.lastImport.value?.copy(imported = ids.size)
                step = Step.Done(ids.size, ids, t)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                c.message("Não consegui importar. Nada foi gravado.")
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
                c.lastImport.value = c.lastImport.value?.copy(imported = 0)
                step = d.copy(undone = true)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                c.message("Não consegui desfazer. Tente de novo.")
            } finally {
                busy = false
            }
        }
    }

    companion object {
        fun kindOf(type: TxType): CategoryKind = if (type == TxType.INCOME) CategoryKind.INCOME else CategoryKind.EXPENSE
    }
}
