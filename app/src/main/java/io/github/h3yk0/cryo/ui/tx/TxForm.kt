// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.ui.tx

import io.github.h3yk0.cryo.R
import io.github.h3yk0.cryo.domain.Res
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import io.github.h3yk0.cryo.data.DefaultData
import io.github.h3yk0.cryo.data.FinanceRepository
import io.github.h3yk0.cryo.data.db.CategoryKind
import io.github.h3yk0.cryo.data.db.CreditCard
import io.github.h3yk0.cryo.data.db.Tx
import io.github.h3yk0.cryo.data.db.TxType
import io.github.h3yk0.cryo.domain.CardMath
import io.github.h3yk0.cryo.domain.Dates
import io.github.h3yk0.cryo.domain.Ledger
import io.github.h3yk0.cryo.domain.Money
import io.github.h3yk0.cryo.domain.ParsedEntry
import io.github.h3yk0.cryo.domain.key
import io.github.h3yk0.cryo.domain.ym
import io.github.h3yk0.cryo.domain.ymOf
import io.github.h3yk0.cryo.domain.normalized
import io.github.h3yk0.cryo.ui.components.AccountChips
import io.github.h3yk0.cryo.ui.components.AmountField
import io.github.h3yk0.cryo.ui.components.CategoryGrid
import io.github.h3yk0.cryo.ui.components.DateChooser
import io.github.h3yk0.cryo.ui.components.FieldLabel
import io.github.h3yk0.cryo.ui.components.IconBadge
import io.github.h3yk0.cryo.ui.components.Segmented
import io.github.h3yk0.cryo.ui.components.Source
import io.github.h3yk0.cryo.ui.components.SourceChips
import io.github.h3yk0.cryo.ui.components.Stepper
import io.github.h3yk0.cryo.ui.components.categoryIcon
import io.github.h3yk0.cryo.ui.components.typeLabel
import io.github.h3yk0.cryo.ui.theme.CryoTheme
import java.time.LocalDate

/** Rascunho de lançamento: tudo o que o formulário edita, antes de salvar. */
@Stable
class TxDraft(
    type: TxType = TxType.EXPENSE,
    amount: Long = 0,
    description: String = "",
    categoryId: Long? = null,
    source: Source? = null,
    toAccountId: Long? = null,
    installments: Int = 1,
    date: LocalDate = LocalDate.now(),
    note: String = "",
    investmentId: Long? = null,
    goalId: Long? = null,
    debtId: Long? = null,
    val original: Tx? = null,
    invoiceYm: Int? = null,
) {
    /**
     * RF-CAR-10: a fatura em que o lançamento do cartão entra (ou, no pagamento, a fatura que ele quita). null = o Cryo
     * decide pela data. Ao editar, começa na fatura em que o lançamento **já está** (ERR-044: mexer na categoria de uma
     * compra importada não pode mudá-la de fatura); só volta a seguir a data quando a pessoa muda a data, o cartão ou o tipo.
     */
    var invoiceYm by mutableStateOf(invoiceYm)

    private var typeState by mutableStateOf(type)
    var type: TxType
        get() = typeState
        set(v) { if (v != typeState) invoiceYm = null; typeState = v }
    var amount by mutableLongStateOf(amount)
    var description by mutableStateOf(description)
    var categoryId by mutableStateOf(categoryId)
    private var sourceState by mutableStateOf(source)
    var source: Source?
        get() = sourceState
        set(v) { if (v != sourceState && typeState != TxType.CARD_PAYMENT) invoiceYm = null; sourceState = v }
    var toAccountId by mutableStateOf(toAccountId)
    var installments by mutableIntStateOf(installments)
    private var dateState by mutableStateOf(date)
    var date: LocalDate
        get() = dateState
        set(v) { if (v != dateState && typeState != TxType.CARD_PAYMENT) invoiceYm = null; dateState = v }
    var note by mutableStateOf(note)
    var investmentId by mutableStateOf(investmentId)
    var goalId by mutableStateOf(goalId)
    var debtId by mutableStateOf(debtId)

    val isEditing get() = original != null

    /** A fatura deste lançamento do cartão: a escolhida; senão a da parcela (que não segue a data); senão a da data. */
    fun invoiceFor(card: CreditCard): java.time.YearMonth {
        invoiceYm?.let { return ymOf(it) }
        val o = original
        if (o != null && o.installmentTotal > 1 && o.cardId == card.id && o.invoiceYm != null) return ymOf(o.invoiceYm)
        return CardMath.invoiceFor(card, date)
    }

    /** O que falta preencher (null = pode salvar). */
    fun problem(): String? = when {
        amount <= 0 -> Res.s(R.string.tx_informe_valor)
        type == TxType.EXPENSE && source == null -> Res.s(R.string.tx_escolha_onde_saiu_dinheiro)
        // RF-CAR-07: a receita pode entrar numa conta ou num cartão (estorno, que diminui a fatura).
        type == TxType.INCOME && source == null -> Res.s(R.string.tx_escolha_qual_conta_entrou)
        type == TxType.TRANSFER && (source !is Source.Acc || toAccountId == null) -> Res.s(R.string.tx_escolha_duas_contas)
        type == TxType.TRANSFER && (source as? Source.Acc)?.id == toAccountId -> Res.s(R.string.tx_contas_precisam_ser_diferentes)
        (type == TxType.INVEST_IN || type == TxType.INVEST_OUT) && investmentId == null -> Res.s(R.string.tx_escolha_investimento)
        (type == TxType.GOAL_IN || type == TxType.GOAL_OUT) && goalId == null -> Res.s(R.string.tx_escolha_meta)
        type == TxType.DEBT_IN && debtId == null -> Res.s(R.string.tx_escolha_divida)
        type == TxType.EXPENSE && debtId != null && source !is Source.Acc -> Res.s(R.string.tx_pagamentos_dividas_saem_conta)
        (type == TxType.INVEST_IN || type == TxType.INVEST_OUT || type == TxType.GOAL_IN || type == TxType.GOAL_OUT ||
            type == TxType.CARD_PAYMENT || type == TxType.DEBT_IN) && source !is Source.Acc -> Res.s(R.string.tx_escolha_conta)
        else -> null
    }

    companion object {
        fun fromParsed(p: ParsedEntry) = TxDraft(
            type = p.type, amount = p.amount ?: 0, description = p.description, categoryId = p.categoryId,
            source = p.cardId?.let { Source.Card(it) } ?: p.accountId?.let { Source.Acc(it) },
            toAccountId = p.toAccountId, installments = p.installments, date = p.date,
            investmentId = p.investmentId, goalId = p.goalId, debtId = p.debtId,
        )

        fun fromTx(t: Tx) = TxDraft(
            type = t.type, amount = t.amount, description = t.description, categoryId = t.categoryId,
            // ERR-043: o crédito no cartão (estorno importado) continua no cartão ao editar; antes ele abria sem origem,
            // o formulário exigia uma conta e o estorno saía da fatura para virar receita da conta.
            source = if ((t.type == TxType.EXPENSE || t.type == TxType.INCOME) && t.cardId != null) Source.Card(t.cardId) else t.accountId?.let { Source.Acc(it) },
            toAccountId = t.toAccountId, installments = 1, date = t.date, note = t.note,
            investmentId = t.investmentId, goalId = t.goalId, debtId = t.debtId, original = t,
            invoiceYm = if (t.cardId != null) t.invoiceYm else null,
        )
    }
}

data class SaveResult(
    val alert: String?, val undo: suspend () -> Unit, val summary: String,
    /** RF-MOM: o momento que este lançamento provocou (meta completa, orçamento em 80%/estourado, sequência). */
    val moment: io.github.h3yk0.cryo.ui.components.Moment? = null,
)

/**
 * RF-MOM-01..03: o que este lançamento provocou. [l] é o livro-caixa de antes de salvar; [monthPart] é o que entra no mês
 * da categoria (a primeira parcela, numa compra parcelada no cartão).
 */
internal fun momentAfterSave(d: TxDraft, l: Ledger, monthPart: Long, isNew: Boolean): io.github.h3yk0.cryo.ui.components.Moment? {
    // Meta completa.
    if (isNew && d.type == TxType.GOAL_IN) {
        val g = d.goalId?.let { l.goal[it] }
        if (g != null && g.target > 0) {
            val before = l.goalSaved(g.id)
            if (before < g.target && before + d.amount >= g.target) {
                val since = java.time.Instant.ofEpochMilli(g.createdAt).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                val months = java.time.temporal.ChronoUnit.MONTHS.between(since.withDayOfMonth(1), l.today.withDayOfMonth(1)).toInt().coerceAtLeast(0)
                return io.github.h3yk0.cryo.ui.components.Moment.GoalDone(g.name, io.github.h3yk0.cryo.domain.GoalArt.forGoal(g), g.color, before + d.amount, months)
            }
        }
    }
    // Orçamento em 80% ou estourado (com o limite deste mês, já remanejado).
    val cat = d.categoryId
    if (d.type == TxType.EXPENSE && cat != null && monthPart > 0 && l.budgetOf(cat) != null) {
        val ym = d.date.ym()
        val limit = l.monthLimit(cat, ym)
        if (limit > 0) {
            val before = l.categorySpent(ym, cat)
            val after = before + monthPart
            val over = before < limit && after >= limit
            val eighty = !over && before * 10 < limit * 8 && after * 10 >= limit * 8
            if (over || eighty) {
                val c = l.category[cat]
                val move = if (over) {
                    // De onde cobrir: a categoria com mais folga neste mês (arredondado para cima, de 10 em 10).
                    val need = (((after - limit) + 999) / 1000) * 1000
                    l.budgetUsages(ym).filter { it.category.id != cat }.map { it to (it.limit - it.spent) }
                        .filter { (_, free) -> free >= need }.maxByOrNull { it.second }
                        ?.let { (u, free) -> io.github.h3yk0.cryo.ui.components.Moment.BudgetMove(u.category.id, u.category.name, free, need) }
                } else null
                return io.github.h3yk0.cryo.ui.components.Moment.Budget(
                    cat, c?.name.orEmpty(), c?.icon.orEmpty(), after, limit, over,
                    (ym.lengthOfMonth() - d.date.dayOfMonth).coerceAtLeast(0), ym.key(), move,
                )
            }
        }
    }
    // Sequência: o primeiro lançamento do dia que fecha um marco (3, 7, 14, 30... dias seguidos).
    if (isNew && d.date == l.today && l.s.txs.none { it.date == l.today } && l.s.noSpendDays.none { it.date == l.today }) {
        val logged = l.s.txs.mapTo(HashSet()) { it.date } + l.s.noSpendDays.map { it.date }
        var n = 1
        var day = l.today.minusDays(1)
        while (day in logged) { n++; day = day.minusDays(1) }
        if (n in io.github.h3yk0.cryo.ui.components.Moment.STREAK_MARKS) return io.github.h3yk0.cryo.ui.components.Moment.Streak(n)
    }
    return null
}

/** Grava o rascunho no banco. Devolve um aviso de orçamento (se houver) e como desfazer. */
suspend fun saveDraft(d: TxDraft, repo: FinanceRepository, l: Ledger): SaveResult {
    val o = d.original
    val accId = (d.source as? Source.Acc)?.id
    val cardId = (d.source as? Source.Card)?.id
    val base = (o ?: Tx(type = d.type, amount = d.amount, date = d.date)).copy(
        type = d.type, amount = d.amount, date = d.date, description = d.description.trim(), note = d.note.trim(),
        categoryId = if (d.type == TxType.EXPENSE || d.type == TxType.INCOME) d.categoryId else null,
        accountId = accId,
        toAccountId = if (d.type == TxType.TRANSFER) d.toAccountId else null,
        cardId = if (d.type == TxType.CARD_PAYMENT) o?.cardId else null,
        investmentId = if (d.type == TxType.INVEST_IN || d.type == TxType.INVEST_OUT) d.investmentId else null,
        goalId = if (d.type == TxType.GOAL_IN || d.type == TxType.GOAL_OUT) d.goalId else null,
        debtId = when {
            d.type == TxType.DEBT_IN -> d.debtId
            d.type == TxType.EXPENSE && d.source is Source.Acc -> d.debtId
            else -> null
        },
    )
    val saved: Tx
    if ((d.type == TxType.EXPENSE || d.type == TxType.INCOME) && cardId != null) {
        // Compra ou estorno no cartão: fica no cartão, na fatura do rascunho (a escolhida, a que já tinha ou a da data).
        val card = l.card[cardId]!!
        val invoice = d.invoiceFor(card)
        saved = if (o == null && d.type == TxType.EXPENSE) {
            repo.saveCardPurchase(base.copy(id = 0), card, d.installments, invoice)
        } else {
            val keepParcel = o != null && d.type == TxType.EXPENSE && o.installmentTotal > 1 && o.cardId == cardId
            val t = base.copy(
                cardId = cardId, accountId = null, invoiceYm = invoice.key(),
                installmentGroup = if (keepParcel) o?.installmentGroup else null,
                installmentNumber = if (keepParcel) o?.installmentNumber ?: 0 else 0,
                installmentTotal = if (keepParcel) o?.installmentTotal ?: 0 else 0,
            )
            t.copy(id = repo.saveTx(t))
        }
    } else {
        val t = base.copy(
            invoiceYm = if (d.type == TxType.CARD_PAYMENT) d.invoiceYm ?: o?.invoiceYm else null,
            installmentGroup = null, installmentNumber = 0, installmentTotal = 0,
        )
        saved = t.copy(id = repo.saveTx(t))
    }

    val alert = if (d.type == TxType.EXPENSE) {
        val added = d.amount - if (o != null && o.categoryId == d.categoryId && o.type == TxType.EXPENSE) o.amount else 0
        val monthPart = if (cardId != null && o == null && d.installments > 1) CardMath.split(d.amount, d.installments).first() else added
        repo.budgetAlert(d.categoryId, d.date, monthPart)
    } else null

    val undo: suspend () -> Unit = if (o != null) {
        { repo.saveTx(o) }
    } else if (saved.installmentGroup != null) {
        { repo.deleteTx(saved, wholeGroup = true) }
    } else {
        { repo.deleteTx(saved) }
    }
    val what = when (d.type) {
        TxType.EXPENSE -> Res.s(R.string.wallet_despesa)
        TxType.INCOME -> Res.s(R.string.wallet_receita)
        else -> typeLabel(d.type)
    }
    val summary = when {
        o != null -> Res.s(R.string.tx_alteracoes_salvas)
        saved.debtId != null && d.type == TxType.EXPENSE -> Res.s(R.string.tx_pagamento_divida_registrado, Money.format(d.amount))
        d.type == TxType.DEBT_IN -> Res.s(R.string.debt_somados_divida, Money.format(d.amount))
        else -> Res.s(R.string.tx_registrada, what, Money.format(d.amount))
    }
    val moment = runCatching {
        val added = if (d.type == TxType.EXPENSE) (if (cardId != null && o == null && d.installments > 1) CardMath.split(d.amount, d.installments).first() else d.amount - if (o != null && o.categoryId == d.categoryId && o.type == TxType.EXPENSE) o.amount else 0) else 0L
        momentAfterSave(d, l, added, isNew = o == null)
    }.getOrNull()
    return SaveResult(alert, undo, summary, moment)
}

/** RF-CAR-10: as faturas vizinhas da sugerida ([center]), para escolher em qual o lançamento entra. */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun InvoiceChips(label: String, center: java.time.YearMonth, selected: java.time.YearMonth, today: LocalDate, onSelect: (Int) -> Unit) {
    val months = (listOf(center.minusMonths(1), center, center.plusMonths(1)) + selected).distinct().sorted()
    FieldLabel(label)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        months.forEach { ym ->
            FilterChip(
                selected = ym == selected, onClick = { onSelect(ym.key()) },
                label = { Text(if (ym.year == today.year) Dates.monthName(ym) else Dates.monthShortYear(ym)) },
            )
        }
    }
}

/**
 * Formulário completo de lançamento, usado tanto no registro rápido (compacto) quanto na tela de edição.
 */
@Composable
fun TxForm(d: TxDraft, l: Ledger, compact: Boolean, modifier: Modifier = Modifier, autoFocusAmount: Boolean = false) {
    val simpleTypes = listOf(TxType.EXPENSE to Res.s(R.string.wallet_despesa), TxType.INCOME to Res.s(R.string.wallet_receita), TxType.TRANSFER to Res.s(R.string.home_shortcut_transfer))
    Column(modifier) {
        if (d.type in simpleTypes.map { it.first } && (d.original == null || d.original.billId == null)) {
            Segmented(simpleTypes, d.type, onSelect = { t ->
                if (t != d.type) {
                    d.type = t
                    d.categoryId = null
                    if (t != TxType.EXPENSE && d.source is Source.Card) d.source = null
                    if (d.source == null) l.activeAccounts.firstOrNull()?.let { d.source = Source.Acc(it.id) }
                }
            })
            Spacer(Modifier.height(12.dp))
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
                Text(typeLabel(d.type), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                if (d.original == null) {
                    TextButton(onClick = {
                        d.type = TxType.EXPENSE
                        if (d.source !is Source.Acc) l.activeAccounts.firstOrNull()?.let { d.source = Source.Acc(it.id) }
                    }) { Text(Res.s(R.string.tx_trocar_para_despesa)) }
                }
            }
        }

        val amountColor = when (d.type) {
            TxType.EXPENSE -> CryoTheme.colors.expense
            TxType.INCOME -> CryoTheme.colors.income
            else -> MaterialTheme.colorScheme.onSurface
        }
        AmountField(d.amount, { d.amount = it }, color = amountColor, autoFocus = autoFocusAmount)

        // Descrição + sugestões do histórico
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = d.description,
            onValueChange = { d.description = it },
            label = { Text(Res.s(R.string.tx_descricao)) },
            placeholder = { Text(if (d.type == TxType.INCOME) Res.s(R.string.plan_ex_salario) else Res.s(R.string.tx_ex_almoco)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth(),
        )
        if (d.type == TxType.EXPENSE || d.type == TxType.INCOME) {
            val q = d.description.trim().normalized()
            val suggestions = remember(q, d.type, l) {
                if (q.isEmpty()) emptyList() else l.recentDescriptions(d.type, 60)
                    .filter { it.description.normalized().startsWith(q) && it.description.normalized() != q }.take(4)
            }
            if (suggestions.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                    suggestions.forEach { s ->
                        AssistChip(
                            onClick = {
                                d.description = s.description
                                if (s.categoryId != null) d.categoryId = s.categoryId
                            },
                            label = { Text(s.description) },
                            leadingIcon = { Icon(Icons.Rounded.History, null, Modifier.size(18.dp)) },
                        )
                    }
                }
            }
        }

        when (d.type) {
            TxType.EXPENSE, TxType.INCOME -> {
                CategorySection(d, l, compact)
                val isDebtCategory = d.categoryId?.let { l.category[it]?.icon } == DefaultData.DEBT_ICON
                if (d.type == TxType.EXPENSE && (d.debtId != null || isDebtCategory)) DebtPicker(d, l)
                FieldLabel(if (d.type == TxType.EXPENSE) Res.s(R.string.plan_pago_com) else Res.s(R.string.plan_recebido_2))
                SourceChips(
                    accounts = l.activeAccounts,
                    // RF-CAR-07: o estorno entra no cartão (diminui a fatura). Pagamento de dívida só sai de conta.
                    cards = if (d.debtId == null) l.activeCards else emptyList(),
                    selected = d.source,
                    onSelect = { d.source = it },
                )
                val card = (d.source as? Source.Card)?.let { l.card[it.id] }
                if (card != null) {
                    if (d.type == TxType.EXPENSE && d.original == null) {
                        Spacer(Modifier.height(8.dp))
                        Stepper(
                            value = d.installments, onChange = { d.installments = it }, range = 1..24, label = Res.s(R.string.tx_parcelas),
                            format = { if (it == 1) Res.s(R.string.tx_vista) else "${it}x" },
                        )
                    }
                    // RF-CAR-10: o Cryo sugere a fatura pela data; a pessoa pode mudar (o banco fecha em dias que variam).
                    val inv = d.invoiceFor(card)
                    InvoiceChips(Res.s(R.string.tx_invoice_label), CardMath.invoiceFor(card, d.date), inv, l.today) { d.invoiceYm = it }
                    val per = if (d.type == TxType.EXPENSE && d.installments > 1) Res.s(R.string.tx_1x, d.installments, Money.format(CardMath.split(d.amount, d.installments).last())) else ""
                    Text(
                        Res.s(R.string.tx_entra_fatura_vence, Dates.monthInSentence(inv), Dates.short(CardMath.dueDate(card, inv)), per),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    if (d.type == TxType.INCOME) {
                        Text(Res.s(R.string.card_refund_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (d.type == TxType.EXPENSE && d.original != null && d.original.installmentTotal > 1) {
                        Text(
                            Res.s(R.string.tx_parcela_alteracoes_valem_so, d.original.installmentNumber, d.original.installmentTotal),
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            TxType.TRANSFER -> {
                FieldLabel(Res.s(R.string.tx_texto))
                AccountChips(l.activeAccounts, (d.source as? Source.Acc)?.id, { d.source = Source.Acc(it) })
                FieldLabel(Res.s(R.string.tx_para))
                AccountChips(l.activeAccounts, d.toAccountId, { d.toAccountId = it }, exclude = (d.source as? Source.Acc)?.id)
            }
            TxType.INVEST_IN, TxType.INVEST_OUT -> {
                FieldLabel(Res.s(R.string.tx_investimento))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    l.activeInvestments.forEach { i ->
                        FilterChip(selected = d.investmentId == i.id, onClick = { d.investmentId = i.id }, label = { Text(i.name) })
                    }
                }
                if (l.activeInvestments.isEmpty()) NoItemsText(Res.s(R.string.tx_cadastre_investimento_aba_carteira))
                FieldLabel(if (d.type == TxType.INVEST_IN) Res.s(R.string.wallet_sai_conta) else Res.s(R.string.wallet_entra_conta))
                AccountChips(l.activeAccounts, (d.source as? Source.Acc)?.id, { d.source = Source.Acc(it) })
            }
            TxType.GOAL_IN, TxType.GOAL_OUT -> {
                FieldLabel(Res.s(R.string.tx_meta))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    l.activeGoals.forEach { g ->
                        FilterChip(selected = d.goalId == g.id, onClick = { d.goalId = g.id }, label = { Text(g.name) }, leadingIcon = { io.github.h3yk0.cryo.ui.components.GoalBadge(g, 22.dp) })
                    }
                }
                if (l.activeGoals.isEmpty()) NoItemsText(Res.s(R.string.tx_crie_meta_aba_planejar))
                FieldLabel(if (d.type == TxType.GOAL_IN) Res.s(R.string.wallet_sai_conta) else Res.s(R.string.plan_volta_para_conta))
                AccountChips(l.activeAccounts, (d.source as? Source.Acc)?.id, { d.source = Source.Acc(it) })
            }
            TxType.DEBT_IN -> {
                FieldLabel(Res.s(R.string.tx_divida))
                if (l.activeDebts.isEmpty()) {
                    NoItemsText(Res.s(R.string.tx_cadastre_divida_primeiro_carteira))
                } else {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        l.activeDebts.forEach { debt ->
                            FilterChip(selected = d.debtId == debt.id, onClick = { d.debtId = debt.id }, label = { Text(debt.name) })
                        }
                    }
                }
                FieldLabel(Res.s(R.string.debt_entrou_conta))
                AccountChips(l.activeAccounts, (d.source as? Source.Acc)?.id, { d.source = Source.Acc(it) })
                Text(
                    Res.s(R.string.tx_dinheiro_emprestado_nao_conta),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
            TxType.CARD_PAYMENT -> {
                FieldLabel(Res.s(R.string.debt_pago_com_conta))
                AccountChips(l.activeAccounts, (d.source as? Source.Acc)?.id, { d.source = Source.Acc(it) })
                d.original?.cardId?.let { cid ->
                    Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.CreditCard, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(Res.s(R.string.home_card_title, l.cardName(cid)), style = MaterialTheme.typography.bodyMedium)
                    }
                    // RF-CAR-10: qual fatura este pagamento quita (o Cryo sugere; dá para mudar quando o banco diz outra).
                    (d.invoiceYm ?: d.original.invoiceYm)?.let { paid ->
                        InvoiceChips(Res.s(R.string.tx_invoice_paid_label), ymOf(d.original.invoiceYm ?: paid), ymOf(paid), l.today) { d.invoiceYm = it }
                    }
                }
            }
        }

        FieldLabel(Res.s(R.string.wallet_data))
        DateChooser(d.date, l.today, onChange = { d.date = it })

        if (!compact) {
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = d.note, onValueChange = { d.note = it }, label = { Text(Res.s(R.string.debt_observacao_opcional)) },
                modifier = Modifier.fillMaxWidth(), minLines = 2,
            )
        }
    }
}

@Composable
private fun NoItemsText(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun CategorySection(d: TxDraft, l: Ledger, compact: Boolean) {
    val kind = if (d.type == TxType.INCOME) CategoryKind.INCOME else CategoryKind.EXPENSE
    val cats = l.categories(kind)
    var expanded by rememberSaveable(compact) { mutableStateOf(!compact || d.categoryId == null) }
    val sel = d.categoryId?.let { l.category[it] }
    if (!expanded && sel != null) {
        FieldLabel(Res.s(R.string.plan_categoria))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(categoryIcon(sel.icon), Color(sel.color), 40.dp)
            Spacer(Modifier.width(12.dp))
            Text(sel.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            TextButton(onClick = { expanded = true }) { Text(Res.s(R.string.onboarding_trocar)) }
        }
    } else {
        FieldLabel(Res.s(R.string.plan_categoria))
        CategoryGrid(cats, d.categoryId, onSelect = {
            d.categoryId = it.id
            if (it.icon != DefaultData.DEBT_ICON) d.debtId = null
            if (compact) expanded = false
        })
    }
}

/** Liga o gasto a uma dívida: ele passa a diminuir quanto falta pagar. */
@Composable
private fun DebtPicker(d: TxDraft, l: Ledger) {
    val debts = l.activeDebts.filter { !l.debtInfo(it).isPaidOff || it.id == d.debtId }
    FieldLabel(Res.s(R.string.tx_pagamento_qual_divida))
    if (debts.isEmpty()) {
        NoItemsText(Res.s(R.string.tx_nenhuma_divida_aberto_cadastre))
        return
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(selected = d.debtId == null, onClick = { d.debtId = null }, label = { Text(Res.s(R.string.tx_nenhuma)) })
        debts.forEach { debt ->
            FilterChip(
                selected = d.debtId == debt.id,
                onClick = {
                    d.debtId = debt.id
                    if (d.source !is Source.Acc) {
                        (debt.accountId?.takeIf { id -> l.activeAccounts.any { it.id == id } } ?: l.activeAccounts.firstOrNull()?.id)
                            ?.let { d.source = Source.Acc(it) }
                    }
                },
                label = { Text(debt.name) },
            )
        }
    }
}
