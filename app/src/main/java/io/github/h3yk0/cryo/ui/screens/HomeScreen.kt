// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.EventRepeat
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import io.github.h3yk0.cryo.domain.BillState
import io.github.h3yk0.cryo.domain.Dates
import io.github.h3yk0.cryo.domain.InvoiceStatus
import io.github.h3yk0.cryo.domain.Ledger
import io.github.h3yk0.cryo.domain.Money
import io.github.h3yk0.cryo.domain.forecast
import io.github.h3yk0.cryo.domain.insights
import io.github.h3yk0.cryo.ui.LocalNav
import io.github.h3yk0.cryo.ui.Routes
import io.github.h3yk0.cryo.ui.components.Banner
import io.github.h3yk0.cryo.ui.components.CryoCard
import io.github.h3yk0.cryo.ui.components.ForecastChart
import io.github.h3yk0.cryo.ui.components.IconBadge
import io.github.h3yk0.cryo.ui.components.LocalContainer
import io.github.h3yk0.cryo.ui.components.LocalSettings
import io.github.h3yk0.cryo.ui.components.MoneyText
import io.github.h3yk0.cryo.ui.components.SectionTitle
import io.github.h3yk0.cryo.ui.components.TxRow
import io.github.h3yk0.cryo.ui.components.UsageBar
import io.github.h3yk0.cryo.ui.components.categoryIcon
import io.github.h3yk0.cryo.ui.components.rememberLedger
import io.github.h3yk0.cryo.ui.theme.CryoTheme
import io.github.h3yk0.cryo.ui.tx.LoadingBox
import io.github.h3yk0.cryo.ui.tx.QuickEntryBar
import io.github.h3yk0.cryo.ui.tx.QuickEntryHint

@Composable
fun HomeScreen() {
    val l = rememberLedger() ?: return LoadingBox()
    val nav = LocalNav.current
    val c = LocalContainer.current
    val settings = LocalSettings.current
    val forecast = remember(l) { l.forecast() }
    val month = remember(l) { l.monthSummary(l.today.ym()) }
    val insights = remember(l) { l.insights() }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { nav.go(Routes.txNew()) },
                icon = { Icon(Icons.Rounded.Add, null) },
                text = { Text("Lançar") },
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { pad ->
        LazyColumn(
            Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(Modifier.fillMaxWidth().statusBarsPadding().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (settings.userName.isNotBlank()) "Olá, ${settings.userName}" else "Olá!",
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.semantics { heading() },
                        )
                        Text(
                            Dates.longDate(l.today),
                            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = { c.launch { c.settings.setHideValues(!settings.hideValues) } }) {
                        Icon(
                            if (settings.hideValues) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                            if (settings.hideValues) "Mostrar valores" else "Esconder valores",
                        )
                    }
                    IconButton(onClick = { nav.go(Routes.SETTINGS) }) { Icon(Icons.Rounded.Settings, "Ajustes") }
                }
            }

            item { BalanceHero(l) }

            item {
                Column {
                    QuickEntryBar(l)
                    QuickEntryHint(Modifier.padding(start = 16.dp, end = 16.dp, top = 6.dp))
                }
            }

            item { MonthCard(l, month.income, month.expense, forecast.projectedResult, forecast.dailyAllowance, forecast.remainingDays) }

            val alerts = buildAlerts(l)
            if (alerts.isNotEmpty()) {
                items(alerts) { a ->
                    val (container, content) = when (a.level) {
                        2 -> CryoTheme.colors.expenseContainer to CryoTheme.colors.onExpenseContainer
                        1 -> CryoTheme.colors.warningContainer to CryoTheme.colors.onWarningContainer
                        else -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
                    }
                    Banner(a.icon, a.text, container, content, onClick = { nav.smart(a.route) })
                }
            }

            item {
                CryoCard(onClick = { nav.tab(Routes.INSIGHTS) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Previsão do mês", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        Text("Ver análises", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(Modifier.height(8.dp))
                    ForecastChart(forecast, height = 140.dp)
                }
            }

            if (insights.isNotEmpty()) {
                item {
                    CryoCard(color = MaterialTheme.colorScheme.tertiaryContainer) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Lightbulb, null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
                            Spacer(Modifier.width(8.dp))
                            Text("O Cryo percebeu", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onTertiaryContainer)
                        }
                        insights.take(2).forEach {
                            Text(
                                "• ${it.text}", style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer, modifier = Modifier.padding(top = 6.dp),
                            )
                        }
                    }
                }
            }

            if (l.activeCards.isNotEmpty()) {
                item { SectionTitle("Cartões") { TextButton(onClick = { nav.tab(Routes.WALLET) }) { Text("Ver todos") } } }
                items(l.activeCards, key = { "card${it.id}" }) { card ->
                    val inv = l.focusInvoice(card)
                    val st = inv.status(l.today)
                    CryoCard(onClick = { nav.go(Routes.card(card.id)) }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconBadge(Icons.Rounded.CreditCard, Color(card.color))
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(card.name, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    invoiceStatusText(st, inv.due, l),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (st == InvoiceStatus.OVERDUE) CryoTheme.colors.expense else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            MoneyText(if (st == InvoiceStatus.OPEN) inv.total else inv.remaining, style = MaterialTheme.typography.titleMedium)
                        }
                        Spacer(Modifier.height(10.dp))
                        val used = (card.limitAmount - l.availableLimit(card)).toFloat() / card.limitAmount.coerceAtLeast(1)
                        UsageBar(used, height = 6.dp, baseColor = Color(card.color))
                        Text(
                            "Limite disponível: ${if (settings.hideValues) Money.HIDDEN else Money.format(l.availableLimit(card))}",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }

            val budgets = l.budgetUsages(l.today.ym())
            if (budgets.isNotEmpty()) {
                item { SectionTitle("Orçamentos do mês") { TextButton(onClick = { nav.open(Routes.plan(0)) }) { Text("Ver todos") } } }
                item {
                    CryoCard {
                        budgets.take(3).forEachIndexed { i, b ->
                            if (i > 0) Spacer(Modifier.height(14.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconBadge(categoryIcon(b.category.icon), Color(b.category.color), 32.dp)
                                Spacer(Modifier.width(10.dp))
                                Text(b.category.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${(b.ratio * 100).toInt()}%", style = MaterialTheme.typography.labelLarge)
                            }
                            Spacer(Modifier.height(6.dp))
                            UsageBar(b.ratio)
                            Text(
                                if (b.remaining >= 0) "Restam ${fmt(b.remaining, settings.hideValues)}" else "Passou ${fmt(-b.remaining, settings.hideValues)}",
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 2.dp),
                            )
                        }
                    }
                }
            }

            if (l.activeGoals.isNotEmpty()) {
                item { SectionTitle("Metas e caixinhas") { TextButton(onClick = { nav.open(Routes.plan(2)) }) { Text("Ver todas") } } }
                item {
                    CryoCard {
                        l.activeGoals.take(3).forEachIndexed { i, g ->
                            if (i > 0) Spacer(Modifier.height(14.dp))
                            val saved = l.goalSaved(g.id)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(g.emoji, style = MaterialTheme.typography.titleLarge)
                                Spacer(Modifier.width(10.dp))
                                Text(g.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${(saved * 100 / g.target.coerceAtLeast(1)).coerceAtLeast(0)}%", style = MaterialTheme.typography.labelLarge)
                            }
                            Spacer(Modifier.height(6.dp))
                            UsageBar(saved.toFloat() / g.target.coerceAtLeast(1), baseColor = Color(g.color), warn = false)
                        }
                    }
                }
            }

            item { SectionTitle("Últimas movimentações") { TextButton(onClick = { nav.tab(Routes.txs()) }) { Text("Ver todas") } } }
            val recent = l.s.txs.filter { !it.date.isAfter(l.today) }.take(6)
            if (recent.isEmpty()) {
                item {
                    CryoCard(color = MaterialTheme.colorScheme.primaryContainer) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.AutoAwesome, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            Spacer(Modifier.width(10.dp))
                            Text("Comece pelo registro rápido", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        Text(
                            "Toque no microfone e fale, por exemplo, “almoço 25 no débito”. O Cryo entende o valor, a categoria e a conta. Você só confere e salva.",
                            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                }
            } else {
                item {
                    CryoCard(Modifier, color = MaterialTheme.colorScheme.surfaceContainerLow) {
                        recent.forEachIndexed { i, t ->
                            if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            TxRow(t, l, onClick = { nav.go(Routes.txEdit(t.id)) }, modifier = Modifier.padding(horizontal = 0.dp))
                        }
                    }
                }
            }
        }
    }
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

@Composable
private fun BalanceHero(l: Ledger) {
    val nav = LocalNav.current
    CryoCard(onClick = { nav.tab(Routes.WALLET) }, color = MaterialTheme.colorScheme.primaryContainer) {
        Text("Saldo em contas", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
        MoneyText(
            l.totalBalance, style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Patrimônio ", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
            MoneyText(l.netWorth, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
        Text(
            "Contas + investimentos + caixinhas − cartões",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
        )
    }
}

@Composable
private fun MonthCard(l: Ledger, income: Long, expense: Long, projected: Long, allowance: Long, remainingDays: Int) {
    val nav = LocalNav.current
    CryoCard(onClick = { nav.tab(Routes.INSIGHTS) }) {
        Text(Dates.monthName(java.time.YearMonth.from(l.today)), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth()) {
            Stat("Entrou", income, CryoTheme.colors.income, Icons.AutoMirrored.Rounded.TrendingUp, Modifier.weight(1f))
            Stat("Saiu", expense, CryoTheme.colors.expense, Icons.AutoMirrored.Rounded.TrendingDown, Modifier.weight(1f))
            Stat(
                "Fim do mês", projected,
                if (projected >= 0) CryoTheme.colors.income else CryoTheme.colors.expense,
                Icons.Rounded.AutoAwesome, Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(10.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        Spacer(Modifier.height(10.dp))
        val hide = LocalSettings.current.hideValues
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

private fun buildAlerts(l: Ledger): List<AlertItem> {
    val out = ArrayList<AlertItem>()
    val ym = java.time.YearMonth.from(l.today)
    l.billStatuses(ym).forEach { s ->
        if (s.bill.kind != CategoryKind.EXPENSE) return@forEach
        when (s.state) {
            BillState.OVERDUE -> out += AlertItem("${s.bill.name} venceu ${Dates.relative(s.due, l.today)} · ${Money.format(s.bill.amount)}", 2, Icons.Rounded.ErrorOutline, Routes.plan(1))
            BillState.DUE_SOON -> out += AlertItem("${s.bill.name} vence ${Dates.relative(s.due, l.today)} · ${Money.format(s.bill.amount)}", 1, Icons.Rounded.EventRepeat, Routes.plan(1))
            else -> {}
        }
    }
    l.activeCards.forEach { card ->
        val inv = l.focusInvoice(card)
        when (inv.status(l.today)) {
            InvoiceStatus.OVERDUE -> out += AlertItem("Fatura do ${card.name} vencida: ${Money.format(inv.remaining)}", 2, Icons.Rounded.CreditCard, Routes.card(card.id))
            InvoiceStatus.CLOSED -> if (inv.due.toEpochDay() - l.today.toEpochDay() <= 5) {
                out += AlertItem("Fatura do ${card.name} vence ${Dates.relative(inv.due, l.today)}: ${Money.format(inv.remaining)}", 1, Icons.Rounded.CreditCard, Routes.card(card.id))
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
