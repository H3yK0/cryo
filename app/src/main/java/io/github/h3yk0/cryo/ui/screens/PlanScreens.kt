// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.ui.screens

import io.github.h3yk0.cryo.R
import io.github.h3yk0.cryo.domain.Res
import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EventRepeat
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.Unarchive
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.h3yk0.cryo.data.DefaultData
import io.github.h3yk0.cryo.data.Palette
import io.github.h3yk0.cryo.data.db.Bill
import io.github.h3yk0.cryo.data.db.CategoryKind
import io.github.h3yk0.cryo.data.db.Goal
import io.github.h3yk0.cryo.data.db.Tx
import io.github.h3yk0.cryo.data.db.TxType
import io.github.h3yk0.cryo.domain.BillState
import io.github.h3yk0.cryo.domain.BillStatus
import io.github.h3yk0.cryo.domain.BudgetUsage
import io.github.h3yk0.cryo.domain.Dates
import io.github.h3yk0.cryo.domain.DebtInfo
import io.github.h3yk0.cryo.domain.DebtMath
import io.github.h3yk0.cryo.domain.Ledger
import io.github.h3yk0.cryo.domain.Money
import io.github.h3yk0.cryo.domain.key
import io.github.h3yk0.cryo.domain.ym
import io.github.h3yk0.cryo.notify.Reminders
import io.github.h3yk0.cryo.ui.LocalNav
import io.github.h3yk0.cryo.ui.Routes
import io.github.h3yk0.cryo.ui.components.AmountField
import io.github.h3yk0.cryo.ui.components.CategoryGrid
import io.github.h3yk0.cryo.ui.components.ColorChooser
import io.github.h3yk0.cryo.ui.components.ConfirmDialog
import io.github.h3yk0.cryo.ui.components.CryoCard
import io.github.h3yk0.cryo.ui.components.FlatItem
import io.github.h3yk0.cryo.ui.components.CryoTopBar
import io.github.h3yk0.cryo.ui.components.DatePickerDialogField
import io.github.h3yk0.cryo.ui.components.EmojiChooser
import io.github.h3yk0.cryo.ui.components.EmptyState
import io.github.h3yk0.cryo.ui.components.FieldLabel
import io.github.h3yk0.cryo.ui.components.IconBadge
import io.github.h3yk0.cryo.ui.components.LocalContainer
import io.github.h3yk0.cryo.ui.components.LocalSettings
import io.github.h3yk0.cryo.ui.components.MoneyText
import io.github.h3yk0.cryo.ui.components.Pill
import io.github.h3yk0.cryo.ui.components.Segmented
import io.github.h3yk0.cryo.ui.components.Source
import io.github.h3yk0.cryo.ui.components.SourceChips
import io.github.h3yk0.cryo.ui.components.Stepper
import io.github.h3yk0.cryo.ui.components.SwitchRow
import io.github.h3yk0.cryo.ui.components.UsageBar
import io.github.h3yk0.cryo.ui.components.categoryIcon
import io.github.h3yk0.cryo.ui.components.rememberLedger
import io.github.h3yk0.cryo.ui.theme.CryoTheme
import io.github.h3yk0.cryo.ui.tx.LoadingBox
import java.time.temporal.ChronoUnit
import kotlin.math.max

@Composable
fun PlanScreen(initialTab: Int) {
    val l = rememberLedger() ?: return LoadingBox()
    var tab by rememberSaveable(initialTab) { mutableIntStateOf(initialTab.coerceIn(0, 2)) }
    Scaffold(topBar = { CryoTopBar(Res.s(R.string.tab_plan)) }, contentWindowInsets = WindowInsets(0, 0, 0, 0)) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            PrimaryTabRow(selectedTabIndex = tab) {
                listOf(Res.s(R.string.home_shortcut_budgets), Res.s(R.string.home_shortcut_bills), Res.s(R.string.home_shortcut_goals)).forEachIndexed { i, t ->
                    Tab(selected = tab == i, onClick = { tab = i }, text = { Text(t, maxLines = 1) })
                }
            }
            when (tab) {
                0 -> BudgetsTab(l)
                1 -> BillsTab(l)
                else -> GoalsTab(l)
            }
        }
    }
}

/* ============================== Orçamentos ============================== */

@Composable
private fun BudgetsTab(l: Ledger) {
    val c = LocalContainer.current
    val hide = LocalSettings.current.hideValues
    val ym = l.today.ym()
    val usages = l.budgetUsages(ym)
    var editing by remember { mutableStateOf<Pair<Long?, Long>?>(null) } // categoria, valor sugerido
    val suggestions = remember(l) { budgetSuggestions(l) }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (usages.isNotEmpty()) {
            item {
                val limit = usages.sumOf { it.limit }
                val spent = usages.sumOf { it.spent }
                val daysLeft = ym.lengthOfMonth() - l.today.dayOfMonth + 1
                Column(Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 4.dp, bottom = 12.dp)) {
                    Text(Res.s(R.string.plan_total_planejado, Dates.monthInSentence(ym)), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSecondaryContainer)
                    Row(verticalAlignment = Alignment.Bottom) {
                        MoneyText(spent, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        Text(Res.s(R.string.amount_of_total, if (hide) Money.HIDDEN else Money.format(limit)), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                    Spacer(Modifier.height(8.dp))
                    UsageBar(spent.toFloat() / limit.coerceAtLeast(1))
                    Spacer(Modifier.height(6.dp))
                    Text(
                        if (limit - spent >= 0) Res.s(R.string.plan_restam_cerca_por_dia, if (hide) Money.HIDDEN else Money.format(limit - spent), if (hide) Money.HIDDEN else Money.format((limit - spent) / daysLeft))
                        else Res.s(R.string.plan_voce_passou_total_planejado, if (hide) Money.HIDDEN else Money.format(spent - limit)),
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }
        }
        if (usages.isEmpty()) {
            item {
                EmptyState(
                    Icons.Rounded.PieChart, Res.s(R.string.plan_defina_limites_por_categoria),
                    Res.s(R.string.plan_cryo_avisa_quando_voce),
                )
            }
        }
        items(usages, key = { it.category.id }) { u -> BudgetRow(u, hide) { editing = u.category.id to u.limit } }
        if (suggestions.isNotEmpty()) {
            item {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                        Icon(Icons.Rounded.AutoAwesome, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(6.dp))
                        Text(Res.s(R.string.plan_sugestoes_pela_sua_media), style = MaterialTheme.typography.labelLarge)
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        suggestions.forEach { (cat, v) ->
                            AssistChip(onClick = { editing = cat.id to v }, label = { Text("${cat.name} · ${Money.format(v)}") })
                        }
                    }
                }
            }
        }
        item {
            Button(onClick = { editing = null to 0L }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Icon(Icons.Rounded.Add, null); Spacer(Modifier.width(8.dp)); Text(Res.s(R.string.plan_novo_orcamento))
            }
        }
    }

    editing?.let { (catId, suggested) ->
        BudgetSheet(
            l, catId, if (catId != null) l.budgetOf(catId)?.limitAmount ?: suggested else suggested,
            onDismiss = { editing = null },
            onSave = { id, v -> editing = null; c.launch { c.repo.saveBudget(id, v); c.message(Res.s(R.string.plan_orcamento_salvo)) } },
            onDelete = { id -> editing = null; c.launch { c.repo.deleteBudget(id); c.message(Res.s(R.string.plan_orcamento_removido)) } },
        )
    }
}

/** Média dos últimos 3 meses, arredondada para cima (de R$ 10 em R$ 10), das maiores categorias sem orçamento. */
private fun budgetSuggestions(l: Ledger): List<Pair<io.github.h3yk0.cryo.data.db.Category, Long>> {
    val has = l.s.budgets.map { it.categoryId }.toSet()
    val months = (1..3).map { l.today.ym().minusMonths(it.toLong()) }
    return l.categories(CategoryKind.EXPENSE).filter { it.id !in has }.mapNotNull { cat ->
        val avg = months.sumOf { l.categorySpent(it, cat.id) } / 3
        if (avg < 5000) null else cat to ((avg + 999) / 1000) * 1000
    }.sortedByDescending { it.second }.take(3)
}

@Composable
private fun BudgetRow(u: BudgetUsage, hide: Boolean, onClick: () -> Unit) {
    FlatItem(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(categoryIcon(u.category.icon), Color(u.category.color), 36.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(u.category.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    Res.s(R.string.goal_progress, if (hide) Money.HIDDEN else Money.format(u.spent), if (hide) Money.HIDDEN else Money.format(u.limit)),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            val pct = (u.ratio * 100).toInt()
            Pill(
                "$pct%",
                when { u.ratio >= 1f -> CryoTheme.colors.expenseContainer; u.ratio >= 0.8f -> CryoTheme.colors.warningContainer; else -> MaterialTheme.colorScheme.surfaceContainerHighest },
                when { u.ratio >= 1f -> CryoTheme.colors.onExpenseContainer; u.ratio >= 0.8f -> CryoTheme.colors.onWarningContainer; else -> MaterialTheme.colorScheme.onSurface },
            )
        }
        Spacer(Modifier.height(10.dp))
        UsageBar(u.ratio)
        Text(
            if (u.remaining >= 0) Res.s(R.string.budget_left, if (hide) Money.HIDDEN else Money.format(u.remaining)) else Res.s(R.string.plan_passou_limite, if (hide) Money.HIDDEN else Money.format(-u.remaining)),
            style = MaterialTheme.typography.bodySmall,
            color = if (u.remaining >= 0) MaterialTheme.colorScheme.onSurfaceVariant else CryoTheme.colors.expense,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun BudgetSheet(
    l: Ledger,
    categoryId: Long?,
    initial: Long,
    onDismiss: () -> Unit,
    onSave: (Long, Long) -> Unit,
    onDelete: (Long) -> Unit,
) {
    var cat by remember { mutableStateOf(categoryId) }
    var amount by remember { mutableLongStateOf(initial) }
    val existing = categoryId?.let { l.budgetOf(it) }
    val options = l.categories(CategoryKind.EXPENSE).filter { it.id == categoryId || l.budgetOf(it.id) == null }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.verticalScroll(rememberScrollState()).imePadding().padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
            Text(if (existing != null) Res.s(R.string.plan_editar_orcamento) else Res.s(R.string.plan_novo_orcamento), style = MaterialTheme.typography.titleLarge)
            Text(Res.s(R.string.plan_quanto_voce_quer_gastar), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
            AmountField(amount, { amount = it }, label = Res.s(R.string.plan_limite_por_mes))
            if (existing == null) {
                FieldLabel(Res.s(R.string.plan_categoria))
                CategoryGrid(options, cat, { cat = it.id })
            } else {
                FieldLabel(Res.s(R.string.plan_categoria_2, l.category[categoryId]?.name))
            }
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = { cat?.let { onSave(it, amount) } }, enabled = cat != null && amount > 0,
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) { Text(Res.s(R.string.plan_salvar_orcamento)) }
            if (existing != null) {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { onDelete(existing.categoryId) }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                    Text(Res.s(R.string.plan_remover_orcamento), color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

/* ============================== Contas fixas ============================== */

@Composable
private fun BillsTab(l: Ledger) {
    val nav = LocalNav.current
    val c = LocalContainer.current
    val hide = LocalSettings.current.hideValues
    var ymKey by rememberSaveable { mutableIntStateOf(l.today.ym().key()) }
    val ym = java.time.YearMonth.of(ymKey / 100, ymKey % 100)
    val statuses = l.billStatuses(ym)
    val pay = statuses.filter { it.bill.kind == CategoryKind.EXPENSE }
    val receive = statuses.filter { it.bill.kind == CategoryKind.INCOME }
    val installments = l.activeDebts.mapNotNull { DebtMath.installmentIn(l.debtInfo(it), ym) }.sortedBy { it.due }
    var paying by remember { mutableStateOf<BillStatus?>(null) }
    var undoing by remember { mutableStateOf<BillStatus?>(null) }
    var payingDebt by remember { mutableStateOf<DebtInfo?>(null) }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            io.github.h3yk0.cryo.ui.components.MonthSelector(
                Dates.monthYear(ym, l.today), { ymKey = ym.minusMonths(1).key() }, { ymKey = ym.plusMonths(1).key() },
            )
        }
        if (statuses.isEmpty() && installments.isEmpty()) {
            item {
                EmptyState(
                    Icons.Rounded.EventRepeat, Res.s(R.string.plan_nenhuma_conta_fixa),
                    Res.s(R.string.plan_cadastre_aluguel_internet_assinaturas),
                )
            }
        }
        if (pay.isNotEmpty()) {
            item {
                val total = pay.sumOf { it.bill.amount }
                val paid = pay.filter { it.payment != null }
                SummaryLine(Res.s(R.string.plan_pagar), Res.s(R.string.plan_pagas, paid.size, pay.size), total, hide)
            }
            items(pay, key = { "p${it.bill.id}" }) { s -> BillRow(s, l, hide, onPay = { paying = s }, onUndo = { undoing = s }) }
        }
        if (installments.isNotEmpty()) {
            item {
                SummaryLine(
                    Res.s(R.string.plan_parcelas_dividas), Res.s(R.string.plan_pagas, installments.count { it.paid }, installments.size),
                    installments.sumOf { it.info.debt.installmentAmount }, hide,
                )
            }
            items(installments, key = { "d${it.info.debt.id}" }) { m -> DebtInstallmentRow(m, l, hide, onPay = { payingDebt = m.info }) }
        }
        if (receive.isNotEmpty()) {
            item {
                val total = receive.sumOf { it.bill.amount }
                val got = receive.count { it.payment != null }
                SummaryLine(Res.s(R.string.plan_receber), Res.s(R.string.plan_recebidos, got, receive.size), total, hide)
            }
            items(receive, key = { "r${it.bill.id}" }) { s -> BillRow(s, l, hide, onPay = { paying = s }, onUndo = { undoing = s }) }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Button(onClick = { nav.go(Routes.billEdit(kind = CategoryKind.EXPENSE)) }, modifier = Modifier.weight(1f).height(52.dp)) {
                    Icon(Icons.Rounded.Add, null); Spacer(Modifier.width(6.dp)); Text(Res.s(R.string.plan_conta_pagar))
                }
                OutlinedButton(onClick = { nav.go(Routes.billEdit(kind = CategoryKind.INCOME)) }, modifier = Modifier.weight(1f).height(52.dp)) {
                    Text(Res.s(R.string.plan_receber))
                }
            }
        }
    }

    paying?.let { s ->
        val b = s.bill
        val card = b.cardId?.let { l.card[it] }
        MoneyActionSheet(
            title = if (b.kind == CategoryKind.EXPENSE) Res.s(R.string.plan_marcar_como_paga, b.name) else Res.s(R.string.plan_marcar_como_recebido, b.name),
            subtitle = if (card != null) Res.s(R.string.plan_sera_lancada_cartao, card.name) else Res.s(R.string.plan_confira_valor_pode_mudar),
            initialAmount = b.amount, l = l,
            accountLabel = if (card != null) null else if (b.kind == CategoryKind.EXPENSE) Res.s(R.string.plan_pago_com) else Res.s(R.string.plan_recebido_2),
            initialAccount = b.accountId ?: LocalSettings.current.defaultAccountId,
            confirmLabel = Res.s(R.string.plan_confirmar),
            onDismiss = { paying = null },
            onConfirm = { amount, acc, date ->
                paying = null
                c.launch {
                    c.repo.markBillPaid(b, s.ym, amount, date, acc, card)
                    val alert = if (b.kind == CategoryKind.EXPENSE) c.repo.budgetAlert(b.categoryId, date, amount) else null
                    c.message(alert ?: if (b.kind == CategoryKind.EXPENSE) Res.s(R.string.plan_paga, b.name) else Res.s(R.string.plan_recebido_3, b.name))
                }
            },
        )
    }
    undoing?.let { s ->
        ConfirmDialog(
            Res.s(R.string.plan_desmarcar_pagamento), Res.s(R.string.plan_lancamento_neste_mes_sera, s.bill.name), Res.s(R.string.plan_desmarcar),
            onConfirm = { undoing = null; c.launch { c.repo.unmarkBillPaid(s.bill, s.ym) } },
            onDismiss = { undoing = null },
        )
    }
    payingDebt?.let { info -> DebtPaySheet(info, l, payoff = false, onDismiss = { payingDebt = null }) }
}

/** Parcela de uma dívida no mês escolhido (vem da área de Dívidas, não é uma conta fixa). */
@Composable
private fun DebtInstallmentRow(m: DebtMath.MonthInstallment, l: Ledger, hide: Boolean, onPay: () -> Unit) {
    val nav = LocalNav.current
    val info = m.info
    val d = info.debt
    val amount = if (m.payable) info.nextAmount else d.installmentAmount
    val (status, statusColor) = when {
        m.paid -> "paga" to CryoTheme.colors.income
        m.due.isBefore(l.today) -> Res.s(R.string.bill_was_due, Dates.relative(m.due, l.today)) to CryoTheme.colors.expense
        ChronoUnit.DAYS.between(l.today, m.due) <= 3 -> Res.s(R.string.bill_due, Dates.relative(m.due, l.today)) to CryoTheme.colors.warning
        else -> Res.s(R.string.plan_vence_dia, m.due.dayOfMonth) to MaterialTheme.colorScheme.onSurfaceVariant
    }
    FlatItem(onClick = { nav.go(Routes.debt(d.id)) }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(debtKindIcon(d.kind), Color(d.color))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(d.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    Res.s(R.string.plan_parcela, if (hide) Money.HIDDEN else Money.format(amount), m.number, info.installmentsTotal),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(status, style = MaterialTheme.typography.bodySmall, color = statusColor)
            }
            Spacer(Modifier.width(8.dp))
            when {
                m.paid -> Row(
                    Modifier.padding(horizontal = 12.dp).semantics { contentDescription = Res.s(R.string.plan_parcela_paga) },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.CheckCircle, null, Modifier.size(20.dp), tint = CryoTheme.colors.income)
                    Spacer(Modifier.width(6.dp))
                    Text(Res.s(R.string.wallet_paga), style = MaterialTheme.typography.labelLarge, color = CryoTheme.colors.income)
                }
                m.payable -> FilledTonalButton(onClick = onPay, contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
                    Text(Res.s(R.string.debt_paguei))
                }
            }
        }
        if (!m.paid && !m.payable) {
            Text(
                Res.s(R.string.plan_antes_desta_falta_pagar, info.installmentsPaid + 1),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

@Composable
private fun SummaryLine(title: String, sub: String, total: Long, hide: Boolean) {
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(if (hide) Money.HIDDEN else Money.format(total), style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun BillRow(s: BillStatus, l: Ledger, hide: Boolean, onPay: () -> Unit, onUndo: () -> Unit) {
    val nav = LocalNav.current
    val b = s.bill
    val cat = b.categoryId?.let { l.category[it] }
    val income = b.kind == CategoryKind.INCOME
    val value = if (hide) Money.HIDDEN else Money.format(s.payment?.amount ?: b.amount)
    val (status, statusColor) = when (s.state) {
        BillState.PAID -> Res.s(if (income) R.string.bill_received_on else R.string.bill_paid_on, Dates.short(s.payment!!.date)) to CryoTheme.colors.income
        BillState.OVERDUE -> Res.s(if (income) R.string.bill_late_since else R.string.bill_was_due, Dates.relative(s.due, l.today)) to CryoTheme.colors.expense
        BillState.DUE_SOON -> Res.s(if (income) R.string.bill_arrives else R.string.bill_due, Dates.relative(s.due, l.today)) to CryoTheme.colors.warning
        BillState.UPCOMING -> Res.s(if (income) R.string.bill_arrives_day else R.string.bill_due_day, s.due.dayOfMonth) to MaterialTheme.colorScheme.onSurfaceVariant
    }
    FlatItem(onClick = { nav.go(Routes.billEdit(b.id, b.kind)) }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (cat != null) IconBadge(categoryIcon(cat.icon), Color(cat.color)) else IconBadge(Icons.Rounded.EventRepeat, MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(b.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    value, style = MaterialTheme.typography.bodyMedium,
                    color = if (income) CryoTheme.colors.income else MaterialTheme.colorScheme.onSurface,
                )
                Text(status, style = MaterialTheme.typography.bodySmall, color = statusColor)
            }
            Spacer(Modifier.width(8.dp))
            if (s.payment == null) {
                FilledTonalButton(onClick = onPay, contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
                    Text(if (income) Res.s(R.string.plan_recebi) else Res.s(R.string.debt_paguei))
                }
            } else {
                TextButton(
                    onClick = onUndo,
                    modifier = Modifier.semantics { contentDescription = if (income) Res.s(R.string.plan_recebido_toque_para_desmarcar) else Res.s(R.string.plan_paga_toque_para_desmarcar) },
                ) {
                    Icon(Icons.Rounded.CheckCircle, null, Modifier.size(20.dp), tint = CryoTheme.colors.income)
                    Spacer(Modifier.width(6.dp))
                    Text(if (income) Res.s(R.string.plan_recebido) else Res.s(R.string.wallet_paga), color = CryoTheme.colors.income)
                }
            }
        }
    }
}

@Composable
fun BillEditorScreen(id: Long?, kindArg: CategoryKind) {
    val l = rememberLedger() ?: return LoadingBox()
    val c = LocalContainer.current
    val nav = LocalNav.current
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val existing = id?.let { l.bill[it] }
    var kind by rememberSaveable { mutableStateOf(existing?.kind ?: kindArg) }
    var name by rememberSaveable { mutableStateOf(existing?.name ?: "") }
    var amount by rememberSaveable { mutableLongStateOf(existing?.amount ?: 0L) }
    var day by rememberSaveable { mutableIntStateOf(existing?.dueDay ?: l.today.dayOfMonth.coerceAtMost(28)) }
    var cat by rememberSaveable { mutableStateOf(existing?.categoryId) }
    var source by remember {
        mutableStateOf<Source?>(existing?.cardId?.let { Source.Card(it) } ?: (existing?.accountId ?: l.activeAccounts.firstOrNull()?.id)?.let { Source.Acc(it) })
    }
    var remind by rememberSaveable { mutableStateOf(existing?.remind ?: (kindArg == CategoryKind.EXPENSE)) }
    var before by rememberSaveable { mutableIntStateOf(existing?.remindDaysBefore ?: 1) }
    var active by rememberSaveable { mutableStateOf(existing?.active ?: true) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    EditorScaffold(
        title = if (existing == null) (if (kind == CategoryKind.EXPENSE) Res.s(R.string.plan_nova_conta_fixa) else Res.s(R.string.plan_novo_recebimento_fixo)) else Res.s(R.string.plan_editar),
        canSave = name.isNotBlank() && amount > 0,
        onSave = {
            val b = (existing ?: Bill(name = name, amount = amount, dueDay = day, startYm = l.today.ym().key())).copy(
                name = name.trim(), amount = amount, dueDay = day, kind = kind, categoryId = cat,
                accountId = (source as? Source.Acc)?.id, cardId = if (kind == CategoryKind.EXPENSE) (source as? Source.Card)?.id else null,
                remind = remind, remindDaysBefore = before, active = active,
            )
            if (remind && Build.VERSION.SDK_INT >= 33 && !Reminders.canNotify(ctx)) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
            c.launch { c.repo.saveBill(b); c.message(Res.s(R.string.plan_salvo)) }
            nav.back()
        },
        onDelete = existing?.let { { confirmDelete = true } },
    ) {
        Segmented(
            listOf(CategoryKind.EXPENSE to Res.s(R.string.plan_pagar), CategoryKind.INCOME to Res.s(R.string.plan_receber)), kind,
            { kind = it; cat = null; if (it == CategoryKind.INCOME && source is Source.Card) source = l.activeAccounts.firstOrNull()?.let { a -> Source.Acc(a.id) } },
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            name, { name = it }, label = { Text(Res.s(R.string.wallet_nome)) }, singleLine = true,
            placeholder = { Text(if (kind == CategoryKind.EXPENSE) Res.s(R.string.plan_ex_aluguel_internet_netflix) else Res.s(R.string.plan_ex_salario)) },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences), modifier = Modifier.fillMaxWidth(),
        )
        FieldLabel(Res.s(R.string.plan_valor_todo_mes_aproximado))
        AmountField(amount, { amount = it }, color = if (kind == CategoryKind.INCOME) CryoTheme.colors.income else MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(8.dp))
        Stepper(day, { day = it }, 1..31, if (kind == CategoryKind.EXPENSE) Res.s(R.string.debt_vence_todo_dia) else Res.s(R.string.plan_cai_todo_dia))
        FieldLabel(Res.s(R.string.plan_categoria))
        CategoryGrid(l.categories(kind), cat, { cat = it.id })
        FieldLabel(if (kind == CategoryKind.EXPENSE) Res.s(R.string.debt_paga_com) else Res.s(R.string.wallet_entra_conta))
        SourceChips(l.activeAccounts, if (kind == CategoryKind.EXPENSE) l.activeCards else emptyList(), source, { source = it })
        if (kind == CategoryKind.EXPENSE) {
            Spacer(Modifier.height(8.dp))
            SwitchRow(Res.s(R.string.debt_lembrar_vencimento), remind, { remind = it }, Res.s(R.string.debt_notificacao_celular))
            if (remind) {
                Stepper(before, { before = it }, 0..7, Res.s(R.string.debt_avisar_antes), format = { if (it == 0) Res.s(R.string.debt_dia) else Res.p(R.plurals.count_days, it, it) })
            }
        }
        if (existing != null) SwitchRow(Res.s(R.string.plan_ativa), active, { active = it }, Res.s(R.string.plan_desligue_quando_conta_deixar))
    }
    if (confirmDelete && existing != null) {
        ConfirmDialog(
            Res.s(R.string.plan_apagar, existing.name), Res.s(R.string.plan_pagamentos_ja_registrados_continuam), Res.s(R.string.wallet_apagar),
            onConfirm = { confirmDelete = false; c.launch { c.repo.deleteBill(existing); c.message(Res.s(R.string.plan_conta_fixa_apagada)) }; nav.back() },
            onDismiss = { confirmDelete = false },
        )
    }
}

/* ================================= Metas ================================= */

@Composable
private fun GoalsTab(l: Ledger) {
    val nav = LocalNav.current
    val c = LocalContainer.current
    val hide = LocalSettings.current.hideValues
    val archived = l.s.goals.filter { it.archived }
    val archivedSaved = archived.sumOf { l.goalSaved(it.id) }
    var action by remember { mutableStateOf<Pair<Goal, Boolean>?>(null) } // meta, guardar?
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (l.activeGoals.isEmpty()) {
            item {
                if (archived.isEmpty()) {
                    EmptyState(
                        Icons.Rounded.Flag, Res.s(R.string.plan_crie_sua_primeira_meta),
                        Res.s(R.string.plan_viagem_reserva_emergencia_celular),
                    )
                } else {
                    EmptyState(
                        Icons.Rounded.Flag, Res.s(R.string.plan_nenhuma_meta_ativa),
                        Res.s(R.string.plan_suas_metas_arquivadas_continuam),
                    )
                }
            }
        } else {
            item {
                Column(Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 4.dp, bottom = 12.dp)) {
                    Text(Res.s(R.string.plan_guardado_metas), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSecondaryContainer)
                    MoneyText(
                        l.activeGoals.sumOf { l.goalSaved(it.id) }, style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                    if (archivedSaved > 0) {
                        Text(
                            Res.s(R.string.plan_mais_metas_arquivadas, if (hide) Money.HIDDEN else Money.format(archivedSaved)),
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                }
            }
        }
        items(l.activeGoals, key = { it.id }) { g ->
            GoalCard(
                g, l, hide,
                onClick = { nav.go(Routes.goalEdit(g.id)) },
                actions = { saved ->
                    FilledTonalButton(onClick = { action = g to true }) { Text(Res.s(R.string.plan_guardar)) }
                    OutlinedButton(onClick = { action = g to false }, enabled = saved > 0) { Text(Res.s(R.string.plan_retirar)) }
                    if (saved >= g.target) {
                        TextButton(onClick = { archiveGoal(c, g) }) {
                            Icon(Icons.Rounded.Archive, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text(Res.s(R.string.wallet_arquivar))
                        }
                    }
                },
            )
        }
        item {
            Button(onClick = { nav.go(Routes.goalEdit()) }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Icon(Icons.Rounded.Add, null); Spacer(Modifier.width(8.dp)); Text(Res.s(R.string.plan_nova_meta))
            }
        }
        if (archived.isNotEmpty()) {
            item {
                OutlinedButton(onClick = { nav.go(Routes.GOALS_ARCHIVE) }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                    Icon(Icons.Rounded.Inventory2, null); Spacer(Modifier.width(8.dp)); Text(Res.s(R.string.plan_metas_arquivadas_2, archived.size))
                }
            }
        }
    }
    action?.let { (g, deposit) -> GoalMoneySheet(g, deposit, l, onDismiss = { action = null }) }
}

private fun archiveGoal(c: io.github.h3yk0.cryo.AppContainer, g: Goal) {
    c.launch {
        c.repo.saveGoal(g.copy(archived = true))
        c.message(Res.s(R.string.plan_meta_arquivada_ela_fica), Res.s(R.string.wallet_desfazer)) { c.repo.saveGoal(g.copy(archived = false)) }
    }
}

/** Cartão de uma meta, com barra de progresso e quanto guardar por mês. */
@Composable
private fun GoalCard(
    g: Goal,
    l: Ledger,
    hide: Boolean,
    onClick: () -> Unit,
    faded: Boolean = false,
    actions: @Composable androidx.compose.foundation.layout.FlowRowScope.(saved: Long) -> Unit,
) {
    val saved = l.goalSaved(g.id)
    val ratio = saved.toFloat() / g.target.coerceAtLeast(1)
    FlatItem(onClick = onClick, faded = faded) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            io.github.h3yk0.cryo.ui.components.EmojiBadge(g.emoji, Color(g.color), 48.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(g.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    Res.s(R.string.goal_progress, if (hide) Money.HIDDEN else Money.format(saved), if (hide) Money.HIDDEN else Money.format(g.target)),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text("${(ratio * 100).toInt().coerceAtLeast(0)}%", style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(10.dp))
        UsageBar(ratio, baseColor = Color(g.color), height = 12.dp, warn = false)
        val need = l.goalMonthlyNeeded(g)
        val info = when {
            saved >= g.target -> Res.s(R.string.plan_meta_alcancada)
            g.archived && saved <= 0 -> Res.s(R.string.plan_arquivada_sem_dinheiro_guardado)
            g.archived -> Res.s(R.string.debt_arquivada_faltavam, if (hide) Money.HIDDEN else Money.format(max(0, g.target - saved)))
            need != null && g.deadline != null -> {
                val months = ChronoUnit.MONTHS.between(l.today.ym(), g.deadline.ym()) + 1
                Res.s(
                    R.string.goal_save_per_month, if (hide) Money.HIDDEN else Money.format(need),
                    Dates.monthYearInSentence(g.deadline.ym(), l.today), Res.p(R.plurals.count_months, months.toInt(), months.toInt()),
                )
            }
            else -> Res.s(R.string.plan_faltam, if (hide) Money.HIDDEN else Money.format(max(0, g.target - saved)))
        }
        Text(info, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp))
        FlowRow(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) { actions(saved) }
    }
}

/** Guardar dinheiro numa meta ou retirar dela. */
@Composable
private fun GoalMoneySheet(g: Goal, deposit: Boolean, l: Ledger, onDismiss: () -> Unit) {
    val c = LocalContainer.current
    MoneyActionSheet(
        title = if (deposit) Res.s(R.string.plan_guardar_2, g.name) else Res.s(R.string.plan_retirar_2, g.name),
        subtitle = if (deposit) Res.s(R.string.plan_dinheiro_sai_conta_escolhida) else Res.s(R.string.wallet_dinheiro_volta_para_conta),
        initialAmount = when {
            deposit -> l.goalMonthlyNeeded(g) ?: 0
            g.archived -> l.goalSaved(g.id).coerceAtLeast(0)
            else -> 0
        },
        l = l,
        accountLabel = if (deposit) Res.s(R.string.wallet_sai_conta) else Res.s(R.string.plan_volta_para_conta),
        initialAccount = LocalSettings.current.defaultAccountId, confirmLabel = if (deposit) Res.s(R.string.plan_guardar) else Res.s(R.string.plan_retirar),
        onDismiss = onDismiss,
        onConfirm = { amount, acc, date ->
            onDismiss()
            c.launch {
                c.repo.saveTx(
                    Tx(
                        type = if (deposit) TxType.GOAL_IN else TxType.GOAL_OUT, amount = amount, date = date,
                        description = if (deposit) Res.s(R.string.plan_guardado, g.name) else Res.s(R.string.plan_retirado, g.name), accountId = acc, goalId = g.id,
                    ),
                )
                c.message(if (deposit) Res.s(R.string.plan_guardados, Money.format(amount), g.name) else Res.s(R.string.plan_retirados, Money.format(amount), g.name))
            }
        },
    )
}

/* ============================ Metas arquivadas ============================ */

@Composable
fun GoalsArchiveScreen() {
    val l = rememberLedger() ?: return LoadingBox()
    val nav = LocalNav.current
    val c = LocalContainer.current
    val hide = LocalSettings.current.hideValues
    val archived = l.s.goals.filter { it.archived }.sortedBy { it.name.lowercase() }
    val stillSaved = archived.sumOf { l.goalSaved(it.id).coerceAtLeast(0) }
    var withdrawing by remember { mutableStateOf<Goal?>(null) }

    Scaffold(topBar = { CryoTopBar(Res.s(R.string.plan_metas_arquivadas), onBack = nav::back) }, contentWindowInsets = WindowInsets(0, 0, 0, 0)) { pad ->
        LazyColumn(
            Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (archived.isEmpty()) {
                item {
                    EmptyState(
                        Icons.Rounded.Inventory2, Res.s(R.string.plan_nenhuma_meta_arquivada),
                        Res.s(R.string.plan_quando_voce_arquivar_meta),
                    )
                }
            } else {
                item {
                    Text(
                        if (stillSaved > 0) {
                            Res.s(R.string.plan_ainda_ha_guardados_nestas, if (hide) Money.HIDDEN else Money.format(stillSaved)) +
                                Res.s(R.string.plan_esse_dinheiro_continua_seu)
                        } else {
                            Res.s(R.string.plan_metas_arquivadas_nao_aparecem)
                        },
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                    )
                }
            }
            items(archived, key = { it.id }) { g ->
                GoalCard(g, l, hide, faded = true, onClick = { nav.go(Routes.goalEdit(g.id)) }) { saved ->
                    FilledTonalButton(onClick = {
                        c.launch {
                            c.repo.saveGoal(g.copy(archived = false))
                            c.message(Res.s(R.string.plan_voltou_para_metas, g.name), Res.s(R.string.wallet_desfazer)) { c.repo.saveGoal(g.copy(archived = true)) }
                        }
                    }) { Icon(Icons.Rounded.Unarchive, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text(Res.s(R.string.wallet_restaurar)) }
                    if (saved > 0) OutlinedButton(onClick = { withdrawing = g }) { Text(Res.s(R.string.plan_retirar)) }
                }
            }
        }
    }
    withdrawing?.let { g -> GoalMoneySheet(g, deposit = false, l = l, onDismiss = { withdrawing = null }) }
}

@Composable
fun GoalEditorScreen(id: Long?) {
    val l = rememberLedger() ?: return LoadingBox()
    val c = LocalContainer.current
    val nav = LocalNav.current
    val existing = id?.let { l.goal[it] }
    var name by rememberSaveable { mutableStateOf(existing?.name ?: "") }
    var emoji by rememberSaveable { mutableStateOf(existing?.emoji ?: "🎯") }
    var target by rememberSaveable { mutableLongStateOf(existing?.target ?: 0L) }
    var deadline by rememberSaveable { mutableStateOf(existing?.deadline?.toEpochDay()) }
    var color by rememberSaveable { mutableIntStateOf(existing?.color ?: Palette.pick(l.s.goals.size + 8)) }
    var initial by rememberSaveable { mutableLongStateOf(0L) }
    var pickDate by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    EditorScaffold(
        title = when {
            existing == null -> Res.s(R.string.plan_nova_meta)
            existing.archived -> Res.s(R.string.plan_meta_arquivada)
            else -> Res.s(R.string.plan_editar_meta)
        },
        canSave = name.isNotBlank() && target > 0,
        onSave = {
            val g = (existing ?: Goal(name = name, target = target, color = color)).copy(
                name = name.trim(), emoji = emoji, target = target, color = color,
                deadline = deadline?.let { java.time.LocalDate.ofEpochDay(it) },
            )
            c.launch {
                val newId = c.repo.saveGoal(g)
                if (existing == null && initial > 0) {
                    c.repo.saveTx(Tx(type = TxType.GOAL_IN, amount = initial, date = l.today, description = Res.s(R.string.wallet_saldo_inicial), goalId = newId))
                }
            }
            nav.back()
        },
        onDelete = existing?.let { { confirmDelete = true } },
    ) {
        if (existing?.archived == true) {
            io.github.h3yk0.cryo.ui.components.Banner(
                Icons.Rounded.Inventory2, Res.s(R.string.plan_esta_meta_esta_arquivada),
                MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer,
                Modifier.padding(bottom = 12.dp),
            )
        }
        OutlinedTextField(
            name, { name = it }, label = { Text(Res.s(R.string.plan_nome_meta)) }, singleLine = true, placeholder = { Text(Res.s(R.string.plan_ex_viagem_reserva_emergencia)) },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences), modifier = Modifier.fillMaxWidth(),
        )
        FieldLabel(Res.s(R.string.plan_icone))
        EmojiChooser(DefaultData.goalEmojis, emoji) { emoji = it }
        FieldLabel(Res.s(R.string.plan_quanto_quer_juntar))
        AmountField(target, { target = it }, label = Res.s(R.string.plan_valor_meta))
        FieldLabel(Res.s(R.string.plan_prazo_opcional))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                deadline?.let { Dates.full(java.time.LocalDate.ofEpochDay(it)) } ?: Res.s(R.string.debt_sem_prazo),
                style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f).clickable { pickDate = true },
            )
            TextButton(onClick = { pickDate = true }) { Text(if (deadline == null) Res.s(R.string.debt_definir) else Res.s(R.string.debt_mudar)) }
            if (deadline != null) TextButton(onClick = { deadline = null }) { Text(Res.s(R.string.debt_tirar)) }
        }
        if (existing == null) {
            FieldLabel(Res.s(R.string.plan_ja_tem_algo_guardado))
            AmountField(initial, { initial = it }, label = Res.s(R.string.plan_ja_guardado))
        }
        FieldLabel(Res.s(R.string.wallet_cor))
        ColorChooser(color) { color = it }
        if (existing != null) {
            Spacer(Modifier.height(20.dp))
            if (existing.archived) {
                OutlinedButton(
                    onClick = {
                        c.launch { c.repo.saveGoal(existing.copy(archived = false)); c.message(Res.s(R.string.plan_voltou_para_metas, existing.name)) }
                        nav.back()
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) { Icon(Icons.Rounded.Unarchive, null); Spacer(Modifier.width(8.dp)); Text(Res.s(R.string.plan_restaurar_meta)) }
            } else {
                OutlinedButton(
                    onClick = { archiveGoal(c, existing); nav.back() },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) { Icon(Icons.Rounded.Archive, null); Spacer(Modifier.width(8.dp)); Text(Res.s(R.string.plan_arquivar_meta)) }
                Text(
                    Res.s(R.string.plan_para_metas_concluidas_ou),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
    if (pickDate) {
        DatePickerDialogField(
            deadline?.let { java.time.LocalDate.ofEpochDay(it) } ?: l.today.plusMonths(6),
            onDismiss = { pickDate = false }, onPick = { deadline = it.toEpochDay(); pickDate = false },
        )
    }
    if (confirmDelete && existing != null) {
        ConfirmDialog(
            Res.s(R.string.plan_apagar_meta), Res.s(R.string.plan_dinheiro_guardado_nela_volta), Res.s(R.string.wallet_apagar),
            onConfirm = { confirmDelete = false; c.launch { c.repo.deleteGoal(existing); c.message(Res.s(R.string.plan_meta_apagada)) }; nav.back() },
            onDismiss = { confirmDelete = false },
        )
    }
}
