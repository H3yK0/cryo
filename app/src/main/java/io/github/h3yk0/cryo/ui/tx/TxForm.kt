// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.ui.tx

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
import io.github.h3yk0.cryo.data.db.Tx
import io.github.h3yk0.cryo.data.db.TxType
import io.github.h3yk0.cryo.domain.CardMath
import io.github.h3yk0.cryo.domain.Dates
import io.github.h3yk0.cryo.domain.Ledger
import io.github.h3yk0.cryo.domain.Money
import io.github.h3yk0.cryo.domain.ParsedEntry
import io.github.h3yk0.cryo.domain.key
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
) {
    var type by mutableStateOf(type)
    var amount by mutableLongStateOf(amount)
    var description by mutableStateOf(description)
    var categoryId by mutableStateOf(categoryId)
    var source by mutableStateOf(source)
    var toAccountId by mutableStateOf(toAccountId)
    var installments by mutableIntStateOf(installments)
    var date by mutableStateOf(date)
    var note by mutableStateOf(note)
    var investmentId by mutableStateOf(investmentId)
    var goalId by mutableStateOf(goalId)
    var debtId by mutableStateOf(debtId)

    val isEditing get() = original != null

    /** O que falta preencher (null = pode salvar). */
    fun problem(): String? = when {
        amount <= 0 -> "Informe o valor"
        type == TxType.EXPENSE && source == null -> "Escolha de onde saiu o dinheiro"
        type == TxType.INCOME && source !is Source.Acc -> "Escolha em qual conta entrou"
        type == TxType.TRANSFER && (source !is Source.Acc || toAccountId == null) -> "Escolha as duas contas"
        type == TxType.TRANSFER && (source as? Source.Acc)?.id == toAccountId -> "As contas precisam ser diferentes"
        (type == TxType.INVEST_IN || type == TxType.INVEST_OUT) && investmentId == null -> "Escolha o investimento"
        (type == TxType.GOAL_IN || type == TxType.GOAL_OUT) && goalId == null -> "Escolha a meta"
        type == TxType.DEBT_IN && debtId == null -> "Escolha a dívida"
        type == TxType.EXPENSE && debtId != null && source !is Source.Acc -> "Pagamentos de dívidas saem de uma conta"
        (type == TxType.INVEST_IN || type == TxType.INVEST_OUT || type == TxType.GOAL_IN || type == TxType.GOAL_OUT ||
            type == TxType.CARD_PAYMENT || type == TxType.DEBT_IN) && source !is Source.Acc -> "Escolha a conta"
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
            source = if (t.type == TxType.EXPENSE && t.cardId != null) Source.Card(t.cardId) else t.accountId?.let { Source.Acc(it) },
            toAccountId = t.toAccountId, installments = 1, date = t.date, note = t.note,
            investmentId = t.investmentId, goalId = t.goalId, debtId = t.debtId, original = t,
        )
    }
}

data class SaveResult(val alert: String?, val undo: suspend () -> Unit, val summary: String)

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
    if (d.type == TxType.EXPENSE && cardId != null) {
        val card = l.card[cardId]!!
        saved = if (o != null) {
            val keepParcel = o.installmentTotal > 1 && o.cardId == cardId
            val t = base.copy(
                cardId = cardId, accountId = null,
                invoiceYm = if (keepParcel) o.invoiceYm else CardMath.invoiceFor(card, d.date).key(),
                installmentGroup = if (keepParcel) o.installmentGroup else null,
                installmentNumber = if (keepParcel) o.installmentNumber else 0,
                installmentTotal = if (keepParcel) o.installmentTotal else 0,
            )
            repo.saveTx(t); t
        } else {
            repo.saveCardPurchase(base.copy(id = 0), card, d.installments)
        }
    } else {
        val t = base.copy(
            invoiceYm = if (d.type == TxType.CARD_PAYMENT) o?.invoiceYm else null,
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
        TxType.EXPENSE -> "Despesa"
        TxType.INCOME -> "Receita"
        else -> typeLabel(d.type)
    }
    val summary = when {
        o != null -> "Alterações salvas"
        saved.debtId != null && d.type == TxType.EXPENSE -> "Pagamento de ${Money.format(d.amount)} registrado na dívida"
        d.type == TxType.DEBT_IN -> "${Money.format(d.amount)} somados à dívida"
        else -> "$what de ${Money.format(d.amount)} registrada"
    }
    return SaveResult(alert, undo, summary)
}

/**
 * Formulário completo de lançamento, usado tanto no registro rápido (compacto) quanto na tela de edição.
 */
@Composable
fun TxForm(d: TxDraft, l: Ledger, compact: Boolean, modifier: Modifier = Modifier, autoFocusAmount: Boolean = false) {
    val simpleTypes = listOf(TxType.EXPENSE to "Despesa", TxType.INCOME to "Receita", TxType.TRANSFER to "Transferir")
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
                    }) { Text("Trocar para despesa") }
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
            label = { Text("Descrição") },
            placeholder = { Text(if (d.type == TxType.INCOME) "Ex.: Salário" else "Ex.: Almoço") },
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
                FieldLabel(if (d.type == TxType.EXPENSE) "Pago com" else "Recebido em")
                SourceChips(
                    accounts = l.activeAccounts,
                    cards = if (d.type == TxType.EXPENSE && d.debtId == null) l.activeCards else emptyList(),
                    selected = d.source,
                    onSelect = { d.source = it },
                )
                val card = (d.source as? Source.Card)?.let { l.card[it.id] }
                if (card != null && d.type == TxType.EXPENSE) {
                    if (d.original == null) {
                        Spacer(Modifier.height(8.dp))
                        Stepper(
                            value = d.installments, onChange = { d.installments = it }, range = 1..24, label = "Parcelas",
                            format = { if (it == 1) "À vista" else "${it}x" },
                        )
                    }
                    val inv = CardMath.invoiceFor(card, d.date)
                    val per = if (d.installments > 1) " · ${d.installments}x de ${Money.format(CardMath.split(d.amount, d.installments).last())}" else ""
                    Text(
                        "Entra na fatura de ${Dates.monthName(inv).lowercase()} (vence ${Dates.short(CardMath.dueDate(card, inv))})$per",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    if (d.original != null && d.original.installmentTotal > 1) {
                        Text(
                            "Parcela ${d.original.installmentNumber} de ${d.original.installmentTotal}. As alterações valem só para esta parcela.",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            TxType.TRANSFER -> {
                FieldLabel("De")
                AccountChips(l.activeAccounts, (d.source as? Source.Acc)?.id, { d.source = Source.Acc(it) })
                FieldLabel("Para")
                AccountChips(l.activeAccounts, d.toAccountId, { d.toAccountId = it }, exclude = (d.source as? Source.Acc)?.id)
            }
            TxType.INVEST_IN, TxType.INVEST_OUT -> {
                FieldLabel("Investimento")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    l.activeInvestments.forEach { i ->
                        FilterChip(selected = d.investmentId == i.id, onClick = { d.investmentId = i.id }, label = { Text(i.name) })
                    }
                }
                if (l.activeInvestments.isEmpty()) NoItemsText("Cadastre um investimento na aba Carteira.")
                FieldLabel(if (d.type == TxType.INVEST_IN) "Sai da conta" else "Entra na conta")
                AccountChips(l.activeAccounts, (d.source as? Source.Acc)?.id, { d.source = Source.Acc(it) })
            }
            TxType.GOAL_IN, TxType.GOAL_OUT -> {
                FieldLabel("Meta")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    l.activeGoals.forEach { g ->
                        FilterChip(selected = d.goalId == g.id, onClick = { d.goalId = g.id }, label = { Text("${g.emoji} ${g.name}") })
                    }
                }
                if (l.activeGoals.isEmpty()) NoItemsText("Crie uma meta na aba Planejar.")
                FieldLabel(if (d.type == TxType.GOAL_IN) "Sai da conta" else "Volta para a conta")
                AccountChips(l.activeAccounts, (d.source as? Source.Acc)?.id, { d.source = Source.Acc(it) })
            }
            TxType.DEBT_IN -> {
                FieldLabel("Dívida")
                if (l.activeDebts.isEmpty()) {
                    NoItemsText("Cadastre a dívida primeiro em Carteira › Dívidas. No cadastro dá para registrar o dinheiro que entrou.")
                } else {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        l.activeDebts.forEach { debt ->
                            FilterChip(selected = d.debtId == debt.id, onClick = { d.debtId = debt.id }, label = { Text(debt.name) })
                        }
                    }
                }
                FieldLabel("Entrou na conta")
                AccountChips(l.activeAccounts, (d.source as? Source.Acc)?.id, { d.source = Source.Acc(it) })
                Text(
                    "Dinheiro emprestado não conta como receita: ele entra na conta e aumenta a dívida.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
            TxType.CARD_PAYMENT -> {
                FieldLabel("Pago com a conta")
                AccountChips(l.activeAccounts, (d.source as? Source.Acc)?.id, { d.source = Source.Acc(it) })
                d.original?.cardId?.let { cid ->
                    Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.CreditCard, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Cartão ${l.cardName(cid)}", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        FieldLabel("Data")
        DateChooser(d.date, l.today, onChange = { d.date = it })

        if (!compact) {
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = d.note, onValueChange = { d.note = it }, label = { Text("Observação (opcional)") },
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
        FieldLabel("Categoria")
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(categoryIcon(sel.icon), Color(sel.color), 40.dp)
            Spacer(Modifier.width(12.dp))
            Text(sel.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            TextButton(onClick = { expanded = true }) { Text("Trocar") }
        }
    } else {
        FieldLabel("Categoria")
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
    FieldLabel("Pagamento de qual dívida?")
    if (debts.isEmpty()) {
        NoItemsText("Nenhuma dívida em aberto. Cadastre em Carteira › Dívidas para acompanhar quanto falta pagar.")
        return
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(selected = d.debtId == null, onClick = { d.debtId = null }, label = { Text("Nenhuma") })
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
