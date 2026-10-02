// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.h3yk0.cryo.data.db.CategoryKind
import io.github.h3yk0.cryo.domain.Dates
import io.github.h3yk0.cryo.domain.FlowKind
import io.github.h3yk0.cryo.domain.FlowNode
import io.github.h3yk0.cryo.domain.Ledger
import io.github.h3yk0.cryo.domain.Money
import io.github.h3yk0.cryo.domain.MonthForecast
import io.github.h3yk0.cryo.domain.forecast
import io.github.h3yk0.cryo.domain.insights
import io.github.h3yk0.cryo.domain.key
import io.github.h3yk0.cryo.domain.lastMonths
import io.github.h3yk0.cryo.domain.moneyFlow
import io.github.h3yk0.cryo.domain.ym
import io.github.h3yk0.cryo.domain.ymOf
import io.github.h3yk0.cryo.ui.LocalNav
import io.github.h3yk0.cryo.ui.Routes
import io.github.h3yk0.cryo.ui.components.CryoCard
import io.github.h3yk0.cryo.ui.components.CryoTopBar
import io.github.h3yk0.cryo.ui.components.DonutChart
import io.github.h3yk0.cryo.ui.components.ForecastChart
import io.github.h3yk0.cryo.ui.components.HeatmapCalendar
import io.github.h3yk0.cryo.ui.components.IconBadge
import io.github.h3yk0.cryo.ui.components.LegendDot
import io.github.h3yk0.cryo.ui.components.LocalSettings
import io.github.h3yk0.cryo.ui.components.MoneyText
import io.github.h3yk0.cryo.ui.components.MonthBarsChart
import io.github.h3yk0.cryo.ui.components.MonthSelector
import io.github.h3yk0.cryo.ui.components.SankeyChart
import io.github.h3yk0.cryo.ui.components.SectionTitle
import io.github.h3yk0.cryo.ui.components.categoryIcon
import io.github.h3yk0.cryo.ui.components.rememberLedger
import io.github.h3yk0.cryo.ui.theme.CryoTheme
import io.github.h3yk0.cryo.ui.tx.LoadingBox

@Composable
fun InsightsScreen() {
    val l = rememberLedger() ?: return LoadingBox()
    var ymKey by rememberSaveable { mutableIntStateOf(l.today.ym().key()) }
    val ym = ymOf(ymKey)
    val isCurrent = ym == l.today.ym()
    val forecast = remember(l) { l.forecast() }
    val flow = remember(l, ymKey) { l.moneyFlow(ym) }
    val cats = remember(l, ymKey) { l.byCategory(ym, CategoryKind.EXPENSE) }
    val bars = remember(l, ymKey) { l.lastMonths(6, if (ym.isAfter(l.today.ym())) l.today.ym() else ym) }
    val insights = remember(l) { l.insights() }
    val summary = remember(l, ymKey) { l.monthSummary(ym) }
    val nav = LocalNav.current

    Scaffold(topBar = { CryoTopBar("Análises") }, contentWindowInsets = WindowInsets(0, 0, 0, 0)) { pad ->
        LazyColumn(
            Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                MonthSelector(
                    Dates.monthYear(ym, l.today), { ymKey = ym.minusMonths(1).key() }, { ymKey = ym.plusMonths(1).key() },
                    nextEnabled = ym.isBefore(l.today.ym().plusMonths(1)),
                )
            }

            if (isCurrent) {
                item { ForecastCard(forecast) }
                if (insights.isNotEmpty()) {
                    item {
                        CryoCard(color = MaterialTheme.colorScheme.tertiaryContainer) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Lightbulb, null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
                                Spacer(Modifier.width(8.dp))
                                Text("O Cryo percebeu", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onTertiaryContainer)
                            }
                            insights.forEach {
                                Text("• ${it.text}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onTertiaryContainer, modifier = Modifier.padding(top = 6.dp))
                            }
                        }
                    }
                }
            } else {
                item {
                    CryoCard {
                        Text("Resultado do mês", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        Line("Entrou", summary.income, CryoTheme.colors.income)
                        Line("Saiu", -summary.expense, CryoTheme.colors.expense)
                        if (summary.invested != 0L) Line("Investido", -summary.invested, MaterialTheme.colorScheme.onSurface)
                        if (summary.saved != 0L) Line("Guardado em metas", -summary.saved, MaterialTheme.colorScheme.onSurface)
                        HorizontalDivider(Modifier.padding(vertical = 6.dp))
                        Line("Sobrou", summary.result, if (summary.result >= 0) CryoTheme.colors.income else CryoTheme.colors.expense, bold = true)
                    }
                }
            }

            item { SectionTitle("Fluxo do dinheiro") }
            item {
                CryoCard {
                    if (flow.isEmpty) {
                        Text("Registre entradas e saídas para ver o caminho do seu dinheiro.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        Text(
                            "De onde o dinheiro veio (em cima) e para onde foi (embaixo).",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(12.dp))
                        SankeyChart(flow)
                        Spacer(Modifier.height(12.dp))
                        Row(Modifier.fillMaxWidth()) {
                            FlowLegend("De onde veio", flow.sources, flow.total, Modifier.weight(1f))
                            Spacer(Modifier.width(12.dp))
                            FlowLegend("Para onde foi", flow.destinations, flow.total, Modifier.weight(1f))
                        }
                    }
                }
            }

            item { SectionTitle("Gastos por categoria") }
            item {
                CryoCard {
                    if (cats.isEmpty()) {
                        Text("Sem gastos neste mês.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        val total = cats.sumOf { it.total }
                        DonutChart(
                            values = cats.map { it.total },
                            colors = cats.map { Color(it.category?.color ?: 0xFF9E9E9E.toInt()) },
                            modifier = Modifier.fillMaxWidth(0.62f).align(Alignment.CenterHorizontally),
                            description = "Gastos por categoria: " + cats.joinToString { "${it.category?.name}: ${Money.format(it.total)}" },
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Total", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                MoneyText(total, style = MaterialTheme.typography.titleMedium)
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        cats.forEach { ct ->
                            val cat = ct.category
                            Row(
                                Modifier.fillMaxWidth().clickable(enabled = cat != null) { cat?.let { nav.open(Routes.txs(it.id)) } }.padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                if (cat != null) IconBadge(categoryIcon(cat.icon), Color(cat.color), 32.dp)
                                Spacer(Modifier.width(10.dp))
                                Text(cat?.name ?: "Sem categoria", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${ct.total * 100 / total.coerceAtLeast(1)}%  ", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                MoneyText(ct.total, style = MaterialTheme.typography.titleSmall)
                            }
                        }
                    }
                }
            }

            item { SectionTitle("Calendário de calor") }
            item {
                var day by remember(ymKey) { mutableStateOf<Int?>(null) }
                val daily = remember(l, ymKey) { l.dailyExpenses(ym) }
                CryoCard {
                    HeatmapCalendar(ym, daily, l.today, day, onSelect = { day = it })
                    day?.let {
                        Text(
                            "Dia $it: ${if (daily[it] > 0) "gastou ${Money.format(daily[it])}" else "sem gastos"}",
                            style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                    val days = if (isCurrent) l.today.dayOfMonth else ym.lengthOfMonth()
                    val active = (1..days).count { daily[it] > 0 }
                    Text(
                        "Você gastou em $active de $days dias" + if (days - active > 0) " — ${days - active} dias sem gastar nada." else ".",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }

            item { SectionTitle("Últimos 6 meses") }
            item {
                CryoCard {
                    MonthBarsChart(bars)
                    Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        LegendDot(CryoTheme.colors.income); Text(" Entradas   ", style = MaterialTheme.typography.labelMedium)
                        LegendDot(CryoTheme.colors.expense); Text(" Saídas", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun ForecastCard(f: MonthForecast) {
    val hide = LocalSettings.current.hideValues
    CryoCard {
        Text("Previsão do fim do mês", style = MaterialTheme.typography.titleMedium)
        Row(verticalAlignment = Alignment.Bottom) {
            MoneyText(
                f.projectedResult, signed = true, style = MaterialTheme.typography.headlineMedium,
                color = if (f.projectedResult >= 0) CryoTheme.colors.income else CryoTheme.colors.expense,
            )
            Text(
                if (f.projectedResult >= 0) "  devem sobrar" else "  devem faltar",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }
        Spacer(Modifier.height(8.dp))
        ForecastChart(f)
        Text(
            "Linha cheia: o que já aconteceu. Tracejada: a previsão até o dia ${f.daysInMonth}.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        Text("Como cheguei nesse número", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(4.dp))
        Line("Já entrou", f.incomeSoFar, CryoTheme.colors.income)
        if (f.pendingBillsIncome + f.scheduledIncome > 0) Line("Ainda vai entrar (fixos e agendados)", f.pendingBillsIncome + f.scheduledIncome, CryoTheme.colors.income)
        Line("Já saiu", -f.expenseSoFar, CryoTheme.colors.expense)
        if (f.pendingBillsExpense > 0) Line("Contas fixas a pagar", -f.pendingBillsExpense, CryoTheme.colors.expense)
        if (f.scheduledExpense > 0) Line("Parcelas e agendados", -f.scheduledExpense, CryoTheme.colors.expense)
        if (f.pendingDebts > 0) Line("Parcelas de dívidas a pagar", -f.pendingDebts, CryoTheme.colors.expense)
        if (f.remainingDays > 0) {
            Line("Dia a dia (${if (hide) Money.HIDDEN else Money.format(f.dailyRate)}/dia × ${f.remainingDays} dias)", -f.projectedVariable, CryoTheme.colors.expense)
        }
        HorizontalDivider(Modifier.padding(vertical = 6.dp))
        Line("Resultado previsto", f.projectedResult, if (f.projectedResult >= 0) CryoTheme.colors.income else CryoTheme.colors.expense, bold = true)
        if (f.usedHistory) {
            Text(
                "Como o mês está no começo, também usei a sua média dos últimos meses para estimar o dia a dia.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

@Composable
private fun Line(label: String, value: Long, color: Color, bold: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            label, style = if (bold) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        MoneyText(value, signed = true, color = color, style = MaterialTheme.typography.bodyMedium, fontWeight = if (bold) FontWeight.Bold else null)
    }
}

@Composable
private fun FlowLegend(title: String, nodes: List<FlowNode>, total: Long, modifier: Modifier = Modifier) {
    val hide = LocalSettings.current.hideValues
    val surplus = CryoTheme.colors.income
    val neutral = MaterialTheme.colorScheme.outline
    Column(modifier) {
        Text(title, style = MaterialTheme.typography.labelLarge)
        nodes.forEach { n ->
            Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.Top) {
                LegendDot(
                    when (n.kind) {
                        FlowKind.SURPLUS -> surplus
                        FlowKind.FROM_BALANCE -> neutral
                        else -> Color(n.color)
                    },
                )
                Spacer(Modifier.width(6.dp))
                Column {
                    Text(n.label, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        (if (hide) "" else "${Money.compact(n.value)} · ") + "${n.value * 100 / total.coerceAtLeast(1)}%",
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Suppress("unused")
private fun Ledger.unused() = Unit
