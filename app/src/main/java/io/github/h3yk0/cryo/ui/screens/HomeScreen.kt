// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.EventRepeat
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.RequestQuote
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.h3yk0.cryo.data.db.CategoryKind
import io.github.h3yk0.cryo.data.db.TxType
import io.github.h3yk0.cryo.domain.BillState
import io.github.h3yk0.cryo.domain.Dates
import io.github.h3yk0.cryo.domain.DebtState
import io.github.h3yk0.cryo.domain.InvoiceStatus
import io.github.h3yk0.cryo.domain.Ledger
import io.github.h3yk0.cryo.domain.Money
import io.github.h3yk0.cryo.domain.forecast
import io.github.h3yk0.cryo.domain.insights
import io.github.h3yk0.cryo.ui.LocalNav
import io.github.h3yk0.cryo.ui.Routes
import io.github.h3yk0.cryo.ui.components.CryoSection
import io.github.h3yk0.cryo.ui.components.HomeHeader
import io.github.h3yk0.cryo.ui.components.IconBadge
import io.github.h3yk0.cryo.ui.components.InfoCard
import io.github.h3yk0.cryo.ui.components.LocalContainer
import io.github.h3yk0.cryo.ui.components.LocalSettings
import io.github.h3yk0.cryo.ui.components.MoneyText
import io.github.h3yk0.cryo.ui.components.SectionPadding
import io.github.h3yk0.cryo.ui.components.ShortcutItem
import io.github.h3yk0.cryo.ui.components.Shortcuts
import io.github.h3yk0.cryo.ui.components.TxRow
import io.github.h3yk0.cryo.ui.components.UsageBar
import io.github.h3yk0.cryo.ui.components.accountIcon
import io.github.h3yk0.cryo.ui.components.categoryIcon
import io.github.h3yk0.cryo.ui.components.rememberLedger
import io.github.h3yk0.cryo.ui.theme.CryoTheme
import io.github.h3yk0.cryo.ui.tx.LoadingBox
import io.github.h3yk0.cryo.ui.tx.QuickEntryBar
import io.github.h3yk0.cryo.ui.tx.QuickEntryHint

/**
 * TELA-INICIO (PRD 6.1). Direção visual 2.0: cabeçalho na cor do Cryo com o saldo (RF-INI-11), atalhos (RF-INI-12),
 * registro rápido, avisos discretos (RF-INI-15) e seções planas que respondem uma pergunta cada (RF-INI-13).
 */
@Composable
fun HomeScreen() {
    val l = rememberLedger() ?: return LoadingBox()
    val nav = LocalNav.current
    val c = LocalContainer.current
    val settings = LocalSettings.current
    val hide = settings.hideValues
    val forecast = remember(l) { l.forecast() }
    val month = remember(l) { l.monthSummary(l.today.ym()) }
    val insights = remember(l) { l.insights() }

    // RF-INI-16: sem botão flutuante; "Lançar" é o primeiro atalho.
    Scaffold(contentWindowInsets = WindowInsets(0, 0, 0, 0)) { pad ->
        LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(bottom = 24.dp)) {
            item {
                HomeHeader(
                    name = settings.userName, subtitle = Dates.longDate(l.today), hideValues = hide,
                    onToggleHide = { c.launch { c.settings.setHideValues(!hide) } },
                    onSettings = { nav.go(Routes.SETTINGS) },
                ) { BalanceBlock(l, onClick = { nav.tab(Routes.WALLET) }) }
            }

            item { Shortcuts(homeShortcuts(l, nav), Modifier.padding(top = 16.dp)) }

            item {
                Column(Modifier.padding(start = SectionPadding, end = SectionPadding, top = 12.dp)) {
                    QuickEntryBar(l)
                    QuickEntryHint(Modifier.padding(start = 16.dp, end = 16.dp, top = 6.dp))
                }
            }

            val alerts = buildAlerts(l, hide)
            if (alerts.isNotEmpty()) {
                item { Spacer(Modifier.height(8.dp)) }
                items(alerts) { a ->
                    InfoCard(
                        a.icon, a.text,
                        tint = when (a.level) {
                            2 -> CryoTheme.colors.expense
                            1 -> CryoTheme.colors.warning
                            else -> MaterialTheme.colorScheme.primary
                        },
                        modifier = Modifier.padding(horizontal = SectionPadding, vertical = 4.dp),
                        onClick = { nav.smart(a.route) },
                    )
                }
            }

            item {
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
            }

            item {
                CryoSection(Dates.monthName(l.today.ym()), onClick = { nav.tab(Routes.INSIGHTS) }) {
                    MonthContent(month.income, month.expense, forecast.projectedResult, forecast.dailyAllowance, forecast.remainingDays, hide)
                }
            }

            items(l.activeCards, key = { "card${it.id}" }) { card ->
                val inv = l.focusInvoice(card)
                val st = inv.status(l.today)
                CryoSection("Cartão ${card.name}", onClick = { nav.go(Routes.card(card.id)) }) {
                    Text(
                        invoiceStatusText(st, inv.due, l), style = MaterialTheme.typography.bodyMedium,
                        color = if (st == InvoiceStatus.OVERDUE) CryoTheme.colors.expense else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    MoneyText(if (st == InvoiceStatus.OPEN) inv.total else inv.remaining, style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(10.dp))
                    val used = (card.limitAmount - l.availableLimit(card)).toFloat() / card.limitAmount.coerceAtLeast(1)
                    UsageBar(used, height = 6.dp, baseColor = Color(card.color))
                    Text(
                        "Limite disponível: ${fmt(l.availableLimit(card), hide)}",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }

            val accounts = l.activeAccounts
            if (accounts.isNotEmpty()) {
                item {
                    CryoSection("Contas", onClick = { nav.tab(Routes.WALLET) }) {
                        accounts.take(MAX_ROWS).forEachIndexed { i, a ->
                            if (i > 0) Spacer(Modifier.height(12.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconBadge(accountIcon(a.type), Color(a.color), 36.dp)
                                Spacer(Modifier.width(12.dp))
                                Text(a.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                MoneyText(l.balance(a.id), style = MaterialTheme.typography.titleSmall)
                            }
                        }
                        if (accounts.size > MAX_ROWS) MoreLine(accounts.size - MAX_ROWS, "conta", "contas")
                    }
                }
            }

            if (l.debtOverview.open.isNotEmpty()) {
                item { CryoSection("Dívidas", onClick = { nav.go(Routes.DEBTS) }) { DebtSummaryContent(l, hide) } }
            }

            val budgets = l.budgetUsages(l.today.ym())
            if (budgets.isNotEmpty()) {
                item {
                    CryoSection("Orçamentos do mês", onClick = { nav.open(Routes.plan(0)) }) {
                        budgets.take(MAX_ROWS).forEachIndexed { i, b ->
                            if (i > 0) Spacer(Modifier.height(14.dp))
                            ProgressLine(
                                lead = { IconBadge(categoryIcon(b.category.icon), Color(b.category.color), 32.dp) },
                                title = b.category.name, ratio = b.ratio,
                                detail = if (b.remaining >= 0) "Restam ${fmt(b.remaining, hide)}" else "Passou ${fmt(-b.remaining, hide)}",
                            )
                        }
                    }
                }
            }

            if (l.activeGoals.isNotEmpty()) {
                item {
                    CryoSection("Metas e caixinhas", onClick = { nav.open(Routes.plan(2)) }) {
                        l.activeGoals.take(MAX_ROWS).forEachIndexed { i, g ->
                            if (i > 0) Spacer(Modifier.height(14.dp))
                            val saved = l.goalSaved(g.id)
                            ProgressLine(
                                lead = { Text(g.emoji, style = MaterialTheme.typography.titleLarge) },
                                title = g.name, ratio = saved.toFloat() / g.target.coerceAtLeast(1),
                                detail = "${fmt(saved, hide)} de ${fmt(g.target, hide)}", color = Color(g.color), warn = false,
                            )
                        }
                    }
                }
            }

            val recent = l.s.txs.filter { !it.date.isAfter(l.today) }.take(6)
            item {
                CryoSection("Últimas movimentações", onClick = { nav.tab(Routes.txs()) }, headerOnly = true, divider = insights.isNotEmpty()) {
                    if (recent.isEmpty()) {
                        InfoCard(
                            Icons.Rounded.AutoAwesome,
                            "Comece pelo registro rápido: toque no microfone e fale, por exemplo, “almoço 25 no débito”. " +
                                "O Cryo entende o valor, a categoria e a conta. Você só confere e salva.",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    } else {
                        recent.forEachIndexed { i, t ->
                            if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            TxRow(t, l, onClick = { nav.go(Routes.txEdit(t.id)) }, modifier = Modifier.padding(horizontal = 0.dp))
                        }
                    }
                }
            }

            if (insights.isNotEmpty()) {
                item {
                    Column(Modifier.padding(start = SectionPadding, end = SectionPadding, top = 16.dp)) {
                        Text(
                            "O Cryo percebeu", style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 8.dp).semantics { heading() },
                        )
                        insights.take(2).forEach {
                            InfoCard(Icons.Rounded.Lightbulb, it.text, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(vertical = 4.dp))
                        }
                    }
                }
            }
        }
    }
}

/** Quantas linhas cada seção do Início mostra (o resto fica na tela da seção). */
private const val MAX_ROWS = 3

/** RF-INI-12: as ações mais usadas, na ordem do dia a dia. "Fatura" só aparece com cartão. */
private fun homeShortcuts(l: Ledger, nav: io.github.h3yk0.cryo.ui.Navigator): List<ShortcutItem> = listOfNotNull(
    ShortcutItem("Lançar", Icons.Rounded.Add) { nav.go(Routes.txNew()) },
    ShortcutItem("Importar", Icons.Rounded.UploadFile) { nav.go(Routes.import()) },
    ShortcutItem("Transferir", Icons.Rounded.SwapHoriz) { nav.go(Routes.txNew(TxType.TRANSFER)) },
    l.activeCards.takeIf { it.isNotEmpty() }?.let { cards ->
        ShortcutItem("Fatura", Icons.Rounded.CreditCard) { if (cards.size == 1) nav.go(Routes.card(cards.single().id)) else nav.tab(Routes.WALLET) }
    },
    ShortcutItem("Contas fixas", Icons.Rounded.EventRepeat) { nav.open(Routes.plan(1)) },
    ShortcutItem("Metas", Icons.Rounded.Savings) { nav.open(Routes.plan(2)) },
    ShortcutItem("Orçamentos", Icons.Rounded.PieChart) { nav.open(Routes.plan(0)) },
    ShortcutItem("Dívidas", Icons.Rounded.RequestQuote) { nav.go(Routes.DEBTS) },
)

@Composable
private fun MoreLine(n: Int, one: String, many: String) {
    Text(
        if (n == 1) "+ 1 $one" else "+ $n $many", style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 12.dp),
    )
}

@Composable
private fun ProgressLine(
    lead: @Composable () -> Unit,
    title: String,
    ratio: Float,
    detail: String,
    color: Color = MaterialTheme.colorScheme.primary,
    warn: Boolean = true,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        lead()
        Spacer(Modifier.width(10.dp))
        Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text("${(ratio * 100).toInt().coerceAtLeast(0)}%", style = MaterialTheme.typography.labelLarge)
    }
    Spacer(Modifier.height(6.dp))
    UsageBar(ratio, baseColor = color, warn = warn)
    Text(
        detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 2.dp),
    )
}

private fun fmt(v: Long, hide: Boolean) = if (hide) Money.HIDDEN else Money.format(v)

private fun java.time.LocalDate.ym() = java.time.YearMonth.from(this)

fun invoiceStatusText(st: InvoiceStatus, due: java.time.LocalDate, l: Ledger): String = when (st) {
    InvoiceStatus.OPEN -> "Fatura aberta · vence ${Dates.short(due)}"
    InvoiceStatus.CLOSED -> "Fatura fechada · vence ${Dates.relative(due, l.today)}"
    InvoiceStatus.PAID -> "Fatura paga"
    InvoiceStatus.OVERDUE -> "Fatura vencida em ${Dates.short(due)}"
    InvoiceStatus.EMPTY -> "Sem gastos nesta fatura"
}

/** RF-INI-02/03: o saldo em contas, o maior número da tela, e o patrimônio logo abaixo (toque leva à Carteira). */
@Composable
private fun BalanceBlock(l: Ledger, onClick: () -> Unit) {
    // Sem recorte de canto: ele cortava a primeira letra de "Saldo" e "Patrimônio", que ficam rente à margem.
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Text("Saldo em contas", style = MaterialTheme.typography.labelLarge, color = LocalContentColor.current.copy(alpha = 0.85f))
        MoneyText(l.totalBalance, style = MaterialTheme.typography.displaySmall, color = LocalContentColor.current)
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Patrimônio ", style = MaterialTheme.typography.bodyMedium, color = LocalContentColor.current.copy(alpha = 0.85f))
            MoneyText(l.netWorth, style = MaterialTheme.typography.titleSmall, color = LocalContentColor.current)
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, Modifier.size(18.dp))
        }
    }
}

/** RF-INI-05: entrou, saiu, fim do mês previsto e a frase com quanto dá para gastar por dia. */
@Composable
private fun MonthContent(income: Long, expense: Long, projected: Long, allowance: Long, remainingDays: Int, hide: Boolean) {
    Column {
        Row(Modifier.fillMaxWidth()) {
            Stat("Entrou", income, CryoTheme.colors.income, Icons.AutoMirrored.Rounded.TrendingUp, Modifier.weight(1f))
            Stat("Saiu", expense, CryoTheme.colors.expense, Icons.AutoMirrored.Rounded.TrendingDown, Modifier.weight(1f))
            Stat(
                "Fim do mês", projected,
                if (projected >= 0) CryoTheme.colors.income else CryoTheme.colors.expense,
                Icons.Rounded.AutoAwesome, Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            when {
                allowance <= 0 ->
                    "Atenção: só as contas já previstas passam do que vai entrar. Veja onde cortar em Análises."
                projected < 0 ->
                    "No ritmo atual o mês fecha no vermelho. Para fechar no azul, gaste no máximo " +
                        "${fmt(allowance, hide)} por dia" + if (remainingDays > 0) " até o fim do mês." else " hoje."
                else ->
                    "No ritmo atual vai sobrar dinheiro. Você pode gastar até ${fmt(allowance, hide)} por dia" +
                        if (remainingDays > 0) " e ainda fechar no azul." else " hoje e ainda fechar no azul."
            },
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun Stat(label: String, value: Long, color: Color, icon: ImageVector, modifier: Modifier = Modifier) {
    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
        MoneyText(value, style = MaterialTheme.typography.titleSmall, color = color)
    }
}

private data class AlertItem(val text: String, val level: Int, val icon: ImageVector, val route: String)

private fun buildAlerts(l: Ledger, hide: Boolean): List<AlertItem> {
    val out = ArrayList<AlertItem>()
    val ym = java.time.YearMonth.from(l.today)
    l.billStatuses(ym).forEach { s ->
        if (s.bill.kind != CategoryKind.EXPENSE) return@forEach
        when (s.state) {
            BillState.OVERDUE -> out += AlertItem("${s.bill.name} venceu ${Dates.relative(s.due, l.today)} · ${fmt(s.bill.amount, hide)}", 2, Icons.Rounded.ErrorOutline, Routes.plan(1))
            BillState.DUE_SOON -> out += AlertItem("${s.bill.name} vence ${Dates.relative(s.due, l.today)} · ${fmt(s.bill.amount, hide)}", 1, Icons.Rounded.EventRepeat, Routes.plan(1))
            else -> {}
        }
    }
    l.debtOverview.open.forEach { i ->
        val due = i.nextDue ?: return@forEach
        when (i.state) {
            DebtState.OVERDUE -> out += AlertItem(
                if (i.hasInstallments) {
                    "Parcela de ${i.debt.name} atrasada desde ${Dates.short(due)} · ${fmt(i.nextAmount, hide)}"
                } else {
                    "O prazo de ${i.debt.name} venceu em ${Dates.short(due)}"
                },
                2, Icons.Rounded.RequestQuote, Routes.debt(i.debt.id),
            )
            DebtState.DUE_SOON -> out += AlertItem(
                "Parcela de ${i.debt.name} vence ${Dates.relative(due, l.today)} · ${fmt(i.nextAmount, hide)}",
                1, Icons.Rounded.RequestQuote, Routes.debt(i.debt.id),
            )
            else -> {}
        }
    }
    l.activeCards.forEach { card ->
        val inv = l.focusInvoice(card)
        when (inv.status(l.today)) {
            InvoiceStatus.OVERDUE -> out += AlertItem("Fatura do ${card.name} vencida: ${fmt(inv.remaining, hide)}", 2, Icons.Rounded.CreditCard, Routes.card(card.id))
            InvoiceStatus.CLOSED -> if (inv.due.toEpochDay() - l.today.toEpochDay() <= 5) {
                out += AlertItem("Fatura do ${card.name} vence ${Dates.relative(inv.due, l.today)}: ${fmt(inv.remaining, hide)}", 1, Icons.Rounded.CreditCard, Routes.card(card.id))
            }
            else -> {}
        }
    }
    l.budgetUsages(ym).filter { it.ratio >= 0.8f }.take(2).forEach { b ->
        out += AlertItem(
            if (b.ratio >= 1f) "Orçamento de ${b.category.name} estourado" else "Orçamento de ${b.category.name} em ${(b.ratio * 100).toInt()}%",
            if (b.ratio >= 1f) 2 else 1, Icons.Rounded.Warning, Routes.plan(0),
        )
    }
    return out
}
