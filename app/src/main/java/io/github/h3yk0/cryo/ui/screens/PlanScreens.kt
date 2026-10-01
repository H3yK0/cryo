// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.ui.screens

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
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EventRepeat
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.PieChart
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
    Scaffold(topBar = { CryoTopBar("Planejar") }, contentWindowInsets = WindowInsets(0, 0, 0, 0)) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            PrimaryTabRow(selectedTabIndex = tab) {
                listOf("Orçamentos", "Contas fixas", "Metas").forEachIndexed { i, t ->
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
                CryoCard(color = MaterialTheme.colorScheme.secondaryContainer) {
                    Text("Total planejado em ${Dates.monthName(ym).lowercase()}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSecondaryContainer)
                    Row(verticalAlignment = Alignment.Bottom) {
                        MoneyText(spent, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        Text(" de ${if (hide) Money.HIDDEN else Money.format(limit)}", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                    Spacer(Modifier.height(8.dp))
                    UsageBar(spent.toFloat() / limit.coerceAtLeast(1))
                    Spacer(Modifier.height(6.dp))
                    Text(
                        if (limit - spent >= 0) "Restam ${if (hide) Money.HIDDEN else Money.format(limit - spent)} · cerca de ${if (hide) Money.HIDDEN else Money.format((limit - spent) / daysLeft)} por dia"
                        else "Você passou ${if (hide) Money.HIDDEN else Money.format(spent - limit)} do total planejado",
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }
        }
        if (usages.isEmpty()) {
            item {
                EmptyState(
                    Icons.Rounded.PieChart, "Defina limites por categoria",
                    "O Cryo avisa quando você chegar a 80% e quando estourar o limite.",
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
                        Text("Sugestões pela sua média de gastos", style = MaterialTheme.typography.labelLarge)
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
                Icon(Icons.Rounded.Add, null); Spacer(Modifier.width(8.dp)); Text("Novo orçamento")
            }
        }
    }

    editing?.let { (catId, suggested) ->
        BudgetSheet(
            l, catId, if (catId != null) l.budgetOf(catId)?.limitAmount ?: suggested else suggested,
            onDismiss = { editing = null },
            onSave = { id, v -> editing = null; c.launch { c.repo.saveBudget(id, v); c.message("Orçamento salvo") } },
            onDelete = { id -> editing = null; c.launch { c.repo.deleteBudget(id); c.message("Orçamento removido") } },
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
    CryoCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(categoryIcon(u.category.icon), Color(u.category.color), 36.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(u.category.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "${if (hide) Money.HIDDEN else Money.format(u.spent)} de ${if (hide) Money.HIDDEN else Money.format(u.limit)}",
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
            if (u.remaining >= 0) "Restam ${if (hide) Money.HIDDEN else Money.format(u.remaining)}" else "Passou ${if (hide) Money.HIDDEN else Money.format(-u.remaining)} do limite",
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
            Text(if (existing != null) "Editar orçamento" else "Novo orçamento", style = MaterialTheme.typography.titleLarge)
            Text("Quanto você quer gastar no máximo por mês nesta categoria?", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
            AmountField(amount, { amount = it }, label = "Limite por mês")
            if (existing == null) {
                FieldLabel("Categoria")
                CategoryGrid(options, cat, { cat = it.id })
            } else {
                FieldLabel("Categoria: ${l.category[categoryId]?.name}")
            }
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = { cat?.let { onSave(it, amount) } }, enabled = cat != null && amount > 0,
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) { Text("Salvar orçamento") }
            if (existing != null) {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { onDelete(existing.categoryId) }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                    Text("Remover orçamento", color = MaterialTheme.colorScheme.error)
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
    var paying by remember { mutableStateOf<BillStatus?>(null) }
    var undoing by remember { mutableStateOf<BillStatus?>(null) }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            io.github.h3yk0.cryo.ui.components.MonthSelector(
                Dates.monthYear(ym, l.today), { ymKey = ym.minusMonths(1).key() }, { ymKey = ym.plusMonths(1).key() },
            )
        }
        if (statuses.isEmpty()) {
            item {
                EmptyState(
                    Icons.Rounded.EventRepeat, "Nenhuma conta fixa",
                    "Cadastre aluguel, internet, assinaturas e também o salário. O Cryo lembra do vencimento e usa isso na previsão do mês.",
                )
            }
        }
        if (pay.isNotEmpty()) {
            item {
                val total = pay.sumOf { it.bill.amount }
                val paid = pay.filter { it.payment != null }
                SummaryLine("A pagar", "${paid.size} de ${pay.size} pagas", total, hide)
            }
            items(pay, key = { "p${it.bill.id}" }) { s -> BillRow(s, l, hide, onPay = { paying = s }, onUndo = { undoing = s }) }
        }
        if (receive.isNotEmpty()) {
            item {
                val total = receive.sumOf { it.bill.amount }
                val got = receive.count { it.payment != null }
                SummaryLine("A receber", "$got de ${receive.size} recebidos", total, hide)
            }
            items(receive, key = { "r${it.bill.id}" }) { s -> BillRow(s, l, hide, onPay = { paying = s }, onUndo = { undoing = s }) }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Button(onClick = { nav.go(Routes.billEdit(kind = CategoryKind.EXPENSE)) }, modifier = Modifier.weight(1f).height(52.dp)) {
                    Icon(Icons.Rounded.Add, null); Spacer(Modifier.width(6.dp)); Text("Conta a pagar")
                }
                OutlinedButton(onClick = { nav.go(Routes.billEdit(kind = CategoryKind.INCOME)) }, modifier = Modifier.weight(1f).height(52.dp)) {
                    Text("A receber")
                }
            }
        }
    }

    paying?.let { s ->
        val b = s.bill
        val card = b.cardId?.let { l.card[it] }
        MoneyActionSheet(
            title = if (b.kind == CategoryKind.EXPENSE) "Marcar ${b.name} como paga" else "Marcar ${b.name} como recebido",
            subtitle = if (card != null) "Será lançada no cartão ${card.name}" else "Confira o valor (pode mudar de um mês para outro)",
            initialAmount = b.amount, l = l,
            accountLabel = if (card != null) null else if (b.kind == CategoryKind.EXPENSE) "Pago com" else "Recebido em",
            initialAccount = b.accountId ?: LocalSettings.current.defaultAccountId,
            confirmLabel = "Confirmar",
            onDismiss = { paying = null },
            onConfirm = { amount, acc, date ->
                paying = null
                c.launch {
                    c.repo.markBillPaid(b, s.ym, amount, date, acc, card)
                    val alert = if (b.kind == CategoryKind.EXPENSE) c.repo.budgetAlert(b.categoryId, date, amount) else null
                    c.message(alert ?: if (b.kind == CategoryKind.EXPENSE) "${b.name} paga" else "${b.name} recebido")
                }
            },
        )
    }
    undoing?.let { s ->
        ConfirmDialog(
            "Desmarcar pagamento?", "O lançamento de ${s.bill.name} neste mês será apagado.", "Desmarcar",
            onConfirm = { undoing = null; c.launch { c.repo.unmarkBillPaid(s.bill, s.ym) } },
            onDismiss = { undoing = null },
        )
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
        BillState.PAID -> (if (income) "recebido em " else "paga em ") + Dates.short(s.payment!!.date) to CryoTheme.colors.income
        BillState.OVERDUE -> (if (income) "atrasado desde " else "venceu ") + Dates.relative(s.due, l.today) to CryoTheme.colors.expense
        BillState.DUE_SOON -> (if (income) "cai " else "vence ") + Dates.relative(s.due, l.today) to CryoTheme.colors.warning
        BillState.UPCOMING -> (if (income) "cai dia " else "vence dia ") + s.due.dayOfMonth to MaterialTheme.colorScheme.onSurfaceVariant
    }
    CryoCard(onClick = { nav.go(Routes.billEdit(b.id, b.kind)) }) {
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
                    Text(if (income) "Recebi" else "Paguei")
                }
            } else {
                TextButton(
                    onClick = onUndo,
                    modifier = Modifier.semantics { contentDescription = if (income) "Recebido. Toque para desmarcar" else "Paga. Toque para desmarcar" },
                ) {
                    Icon(Icons.Rounded.CheckCircle, null, Modifier.size(20.dp), tint = CryoTheme.colors.income)
                    Spacer(Modifier.width(6.dp))
                    Text(if (income) "Recebido" else "Paga", color = CryoTheme.colors.income)
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
        title = if (existing == null) (if (kind == CategoryKind.EXPENSE) "Nova conta fixa" else "Novo recebimento fixo") else "Editar",
        canSave = name.isNotBlank() && amount > 0,
        onSave = {
            val b = (existing ?: Bill(name = name, amount = amount, dueDay = day, startYm = l.today.ym().key())).copy(
                name = name.trim(), amount = amount, dueDay = day, kind = kind, categoryId = cat,
                accountId = (source as? Source.Acc)?.id, cardId = if (kind == CategoryKind.EXPENSE) (source as? Source.Card)?.id else null,
                remind = remind, remindDaysBefore = before, active = active,
            )
            if (remind && Build.VERSION.SDK_INT >= 33 && !Reminders.canNotify(ctx)) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
            c.launch { c.repo.saveBill(b); c.message("Salvo") }
            nav.back()
        },
        onDelete = existing?.let { { confirmDelete = true } },
    ) {
        Segmented(
            listOf(CategoryKind.EXPENSE to "A pagar", CategoryKind.INCOME to "A receber"), kind,
            { kind = it; cat = null; if (it == CategoryKind.INCOME && source is Source.Card) source = l.activeAccounts.firstOrNull()?.let { a -> Source.Acc(a.id) } },
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            name, { name = it }, label = { Text("Nome") }, singleLine = true,
            placeholder = { Text(if (kind == CategoryKind.EXPENSE) "Ex.: Aluguel, Internet, Netflix" else "Ex.: Salário") },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences), modifier = Modifier.fillMaxWidth(),
        )
        FieldLabel("Valor de todo mês (aproximado)")
        AmountField(amount, { amount = it }, color = if (kind == CategoryKind.INCOME) CryoTheme.colors.income else MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(8.dp))
        Stepper(day, { day = it }, 1..31, if (kind == CategoryKind.EXPENSE) "Vence todo dia" else "Cai todo dia")
        FieldLabel("Categoria")
        CategoryGrid(l.categories(kind), cat, { cat = it.id })
        FieldLabel(if (kind == CategoryKind.EXPENSE) "Paga com" else "Entra na conta")
        SourceChips(l.activeAccounts, if (kind == CategoryKind.EXPENSE) l.activeCards else emptyList(), source, { source = it })
        if (kind == CategoryKind.EXPENSE) {
            Spacer(Modifier.height(8.dp))
            SwitchRow("Lembrar do vencimento", remind, { remind = it }, "Notificação no celular")
            if (remind) {
                Stepper(before, { before = it }, 0..7, "Avisar antes", format = { if (it == 0) "No dia" else "$it dia${if (it > 1) "s" else ""}" })
            }
        }
        if (existing != null) SwitchRow("Ativa", active, { active = it }, "Desligue quando a conta deixar de existir")
    }
    if (confirmDelete && existing != null) {
        ConfirmDialog(
            "Apagar ${existing.name}?", "Os pagamentos já registrados continuam no histórico.", "Apagar",
            onConfirm = { confirmDelete = false; c.launch { c.repo.deleteBill(existing); c.message("Conta fixa apagada") }; nav.back() },
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
    var action by remember { mutableStateOf<Pair<Goal, Boolean>?>(null) } // meta, guardar?
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (l.activeGoals.isEmpty()) {
            item {
                EmptyState(
                    Icons.Rounded.Flag, "Crie sua primeira meta",
                    "Viagem, reserva de emergência, celular novo... Defina o valor e o prazo, e o Cryo mostra quanto guardar por mês.",
                )
            }
        } else {
            item {
                CryoCard(color = MaterialTheme.colorScheme.secondaryContainer) {
                    Text("Guardado em metas", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSecondaryContainer)
                    MoneyText(l.totalGoals, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }
        }
        items(l.activeGoals, key = { it.id }) { g ->
            val saved = l.goalSaved(g.id)
            val ratio = saved.toFloat() / g.target.coerceAtLeast(1)
            CryoCard(onClick = { nav.go(Routes.goalEdit(g.id)) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    io.github.h3yk0.cryo.ui.components.EmojiBadge(g.emoji, Color(g.color), 48.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(g.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            "${if (hide) Money.HIDDEN else Money.format(saved)} de ${if (hide) Money.HIDDEN else Money.format(g.target)}",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text("${(ratio * 100).toInt().coerceAtLeast(0)}%", style = MaterialTheme.typography.titleMedium)
                }
                Spacer(Modifier.height(10.dp))
                UsageBar(ratio, baseColor = Color(g.color), height = 12.dp, warn = false)
                val need = l.goalMonthlyNeeded(g)
                val info = when {
                    saved >= g.target -> "Meta alcançada! 🎉"
                    need != null && g.deadline != null -> {
                        val months = ChronoUnit.MONTHS.between(l.today.ym(), g.deadline.ym()) + 1
                        "Guarde ${if (hide) Money.HIDDEN else Money.format(need)} por mês para chegar lá em ${Dates.monthYear(g.deadline.ym(), l.today).lowercase()} ($months ${if (months == 1L) "mês" else "meses"})"
                    }
                    else -> "Faltam ${if (hide) Money.HIDDEN else Money.format(max(0, g.target - saved))}"
                }
                Text(info, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp))
                Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(onClick = { action = g to true }) { Text("Guardar") }
                    OutlinedButton(onClick = { action = g to false }, enabled = saved > 0) { Text("Retirar") }
                }
            }
        }
        item {
            Button(onClick = { nav.go(Routes.goalEdit()) }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Icon(Icons.Rounded.Add, null); Spacer(Modifier.width(8.dp)); Text("Nova meta")
            }
        }
    }
    action?.let { (g, deposit) ->
        val need = l.goalMonthlyNeeded(g)
        MoneyActionSheet(
            title = if (deposit) "Guardar em ${g.name}" else "Retirar de ${g.name}",
            subtitle = if (deposit) "O dinheiro sai da conta escolhida e fica separado na meta" else "O dinheiro volta para a conta escolhida",
            initialAmount = if (deposit) need ?: 0 else 0, l = l,
            accountLabel = if (deposit) "Sai da conta" else "Volta para a conta",
            initialAccount = LocalSettings.current.defaultAccountId, confirmLabel = if (deposit) "Guardar" else "Retirar",
            onDismiss = { action = null },
            onConfirm = { amount, acc, date ->
                action = null
                c.launch {
                    c.repo.saveTx(
                        Tx(
                            type = if (deposit) TxType.GOAL_IN else TxType.GOAL_OUT, amount = amount, date = date,
                            description = if (deposit) "Guardado: ${g.name}" else "Retirado: ${g.name}", accountId = acc, goalId = g.id,
                        ),
                    )
                    c.message(if (deposit) "${Money.format(amount)} guardados em ${g.name}" else "${Money.format(amount)} retirados de ${g.name}")
                }
            },
        )
    }
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
        title = if (existing == null) "Nova meta" else "Editar meta",
        canSave = name.isNotBlank() && target > 0,
        onSave = {
            val g = (existing ?: Goal(name = name, target = target, color = color)).copy(
                name = name.trim(), emoji = emoji, target = target, color = color,
                deadline = deadline?.let { java.time.LocalDate.ofEpochDay(it) },
            )
            c.launch {
                val newId = c.repo.saveGoal(g)
                if (existing == null && initial > 0) {
                    c.repo.saveTx(Tx(type = TxType.GOAL_IN, amount = initial, date = l.today, description = "Saldo inicial", goalId = newId))
                }
            }
            nav.back()
        },
        onDelete = existing?.let { { confirmDelete = true } },
    ) {
        OutlinedTextField(
            name, { name = it }, label = { Text("Nome da meta") }, singleLine = true, placeholder = { Text("Ex.: Viagem, Reserva de emergência") },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences), modifier = Modifier.fillMaxWidth(),
        )
        FieldLabel("Ícone")
        EmojiChooser(DefaultData.goalEmojis, emoji) { emoji = it }
        FieldLabel("Quanto quer juntar")
        AmountField(target, { target = it }, label = "Valor da meta")
        FieldLabel("Prazo (opcional)")
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                deadline?.let { Dates.full(java.time.LocalDate.ofEpochDay(it)) } ?: "Sem prazo",
                style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f).clickable { pickDate = true },
            )
            TextButton(onClick = { pickDate = true }) { Text(if (deadline == null) "Definir" else "Mudar") }
            if (deadline != null) TextButton(onClick = { deadline = null }) { Text("Tirar") }
        }
        if (existing == null) {
            FieldLabel("Já tem algo guardado? (opcional)")
            AmountField(initial, { initial = it }, label = "Já guardado")
        }
        FieldLabel("Cor")
        ColorChooser(color) { color = it }
        if (existing != null) {
            Spacer(Modifier.height(8.dp))
            SwitchRow("Arquivar meta", existing.archived, { v -> c.launch { c.repo.saveGoal(existing.copy(archived = v)) } }, "Para metas concluídas")
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
            "Apagar meta?", "O dinheiro guardado nela volta para as contas de onde saiu.", "Apagar",
            onConfirm = { confirmDelete = false; c.launch { c.repo.deleteGoal(existing); c.message("Meta apagada") }; nav.back() },
            onDismiss = { confirmDelete = false },
        )
    }
}
