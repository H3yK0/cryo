// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.PriorityHigh
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Timeline
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.h3yk0.cryo.R
import io.github.h3yk0.cryo.data.db.Bill
import io.github.h3yk0.cryo.data.db.CategoryKind
import io.github.h3yk0.cryo.data.db.TxType
import io.github.h3yk0.cryo.domain.Dates
import io.github.h3yk0.cryo.domain.Ledger
import io.github.h3yk0.cryo.domain.Money
import io.github.h3yk0.cryo.domain.Num
import io.github.h3yk0.cryo.domain.Res
import io.github.h3yk0.cryo.domain.engine.Anomaly
import io.github.h3yk0.cryo.domain.engine.AnomalyKind
import io.github.h3yk0.cryo.domain.engine.DebtPlanner
import io.github.h3yk0.cryo.domain.engine.DebtRate
import io.github.h3yk0.cryo.domain.engine.EngineReport
import io.github.h3yk0.cryo.domain.engine.Health
import io.github.h3yk0.cryo.domain.engine.HealthKind
import io.github.h3yk0.cryo.domain.engine.HealthLevel
import io.github.h3yk0.cryo.domain.engine.HealthPart
import io.github.h3yk0.cryo.domain.engine.PayoffStrategy
import io.github.h3yk0.cryo.domain.engine.Projection
import io.github.h3yk0.cryo.domain.engine.Recurrence
import io.github.h3yk0.cryo.domain.engine.RecurrencePeriod
import io.github.h3yk0.cryo.domain.engine.RecurrenceStatus
import io.github.h3yk0.cryo.domain.key
import io.github.h3yk0.cryo.domain.ym
import io.github.h3yk0.cryo.ui.LocalNav
import io.github.h3yk0.cryo.ui.Routes
import io.github.h3yk0.cryo.ui.components.AmountField
import io.github.h3yk0.cryo.ui.components.CryoTopBar
import io.github.h3yk0.cryo.ui.components.EmptyState
import io.github.h3yk0.cryo.ui.components.FlatItem
import io.github.h3yk0.cryo.ui.components.IconBadge
import io.github.h3yk0.cryo.ui.components.LocalContainer
import io.github.h3yk0.cryo.ui.components.LocalSettings
import io.github.h3yk0.cryo.ui.components.MoneyText
import io.github.h3yk0.cryo.ui.components.Pill
import io.github.h3yk0.cryo.ui.components.ProjectionChart
import io.github.h3yk0.cryo.ui.components.ScoreRing
import io.github.h3yk0.cryo.ui.components.Segmented
import io.github.h3yk0.cryo.ui.components.UsageBar
import io.github.h3yk0.cryo.ui.components.categoryIcon
import io.github.h3yk0.cryo.ui.components.depthCard
import io.github.h3yk0.cryo.ui.components.rememberEngine
import io.github.h3yk0.cryo.ui.components.rememberLedger
import io.github.h3yk0.cryo.ui.theme.CryoTheme
import kotlin.math.roundToInt

/*
 * Telas e cartões do motor financeiro (RF-MOT-14), no visual da 1.5 (DS-PROF): cartões com profundidade sobre o fundo
 * agrupado. Cada número tem a explicação de onde veio, em linguagem simples (PRI-05).
 */

private fun money(c: Long, hide: Boolean) = if (hide) Money.HIDDEN else Money.format(c)

@Composable
fun levelColor(level: HealthLevel): Color = when (level) {
    HealthLevel.GREAT, HealthLevel.GOOD -> CryoTheme.colors.income
    HealthLevel.BUILDING -> CryoTheme.colors.warning
    HealthLevel.ATTENTION -> CryoTheme.colors.expense
}

fun levelLabel(level: HealthLevel): String = Res.s(
    when (level) {
        HealthLevel.GREAT -> R.string.engine_level_great
        HealthLevel.GOOD -> R.string.engine_level_good
        HealthLevel.BUILDING -> R.string.engine_level_building
        HealthLevel.ATTENTION -> R.string.engine_level_attention
    },
)

private fun partName(k: HealthKind): String = Res.s(
    when (k) {
        HealthKind.SAVINGS -> R.string.engine_part_savings
        HealthKind.RESERVE -> R.string.engine_part_reserve
        HealthKind.COMMITMENTS -> R.string.engine_part_commitments
        HealthKind.CARD -> R.string.engine_part_card
        HealthKind.ON_TIME -> R.string.engine_part_on_time
        HealthKind.STABILITY -> R.string.engine_part_stability
    },
)

private fun partValue(p: HealthPart): String = when (p.kind) {
    HealthKind.SAVINGS -> Res.s(R.string.engine_value_savings, Num.percent((p.value * 100).roundToInt().coerceAtLeast(-999)))
    HealthKind.RESERVE -> Res.s(R.string.engine_value_reserve, Num.decimal(p.value.coerceAtMost(99.0), 1))
    HealthKind.COMMITMENTS -> Res.s(R.string.engine_value_commitments, Num.percent((p.value * 100).roundToInt()))
    HealthKind.CARD -> Res.s(R.string.engine_value_card, Num.percent((p.value * 100).roundToInt()))
    HealthKind.ON_TIME -> if (p.value <= 0.0) Res.s(R.string.engine_value_on_time_none) else Res.p(R.plurals.engine_value_on_time, p.value.toInt(), p.value.toInt())
    HealthKind.STABILITY -> Res.s(R.string.engine_value_stability, Num.percent((p.value * 100).roundToInt()))
}

private fun partAdvice(k: HealthKind): String = Res.s(
    when (k) {
        HealthKind.SAVINGS -> R.string.engine_advice_savings
        HealthKind.RESERVE -> R.string.engine_advice_reserve
        HealthKind.COMMITMENTS -> R.string.engine_advice_commitments
        HealthKind.CARD -> R.string.engine_advice_card
        HealthKind.ON_TIME -> R.string.engine_advice_on_time
        HealthKind.STABILITY -> R.string.engine_advice_stability
    },
)

fun periodLabel(p: RecurrencePeriod): String = Res.s(
    when (p) {
        RecurrencePeriod.WEEKLY -> R.string.engine_period_weekly
        RecurrencePeriod.BIWEEKLY -> R.string.engine_period_biweekly
        RecurrencePeriod.MONTHLY -> R.string.engine_period_monthly
        RecurrencePeriod.YEARLY -> R.string.engine_period_yearly
    },
)

/** "2,1% ao mês (28,3% ao ano)", "Sem juros embutidos" ou "Juros desconhecidos". */
fun rateText(r: DebtRate?): String = when {
    r == null || !r.known -> Res.s(R.string.engine_rate_unknown)
    r.monthly <= 0.0 -> Res.s(R.string.engine_rate_none)
    else -> Res.s(R.string.engine_rate, Num.percent1(r.monthly), Num.percent1(r.yearly))
}

/* ============================== Cartões de Análises ============================== */

/** RF-MOT-06: a nota no topo de Análises. */
@Composable
fun HealthCard(h: Health, onClick: () -> Unit) {
    FlatItem(onClick = onClick) {
        if (!h.ready) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(Icons.Rounded.FavoriteBorder, MaterialTheme.colorScheme.primary, 48.dp)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(Res.s(R.string.engine_health_title), style = MaterialTheme.typography.titleMedium)
                    Text(Res.s(R.string.engine_health_learning), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            return@FlatItem
        }
        val color = levelColor(h.level)
        Row(verticalAlignment = Alignment.CenterVertically) {
            ScoreRing(h.score, color)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(Res.s(R.string.engine_health_title), style = MaterialTheme.typography.titleMedium)
                Text(levelLabel(h.level), style = MaterialTheme.typography.titleSmall, color = color)
                h.weakest?.let {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        Res.s(R.string.engine_health_weakest, partName(it.kind)), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** RF-MOT-04: o saldo nos próximos 3, 6 ou 12 meses. */
@Composable
fun ProjectionCard(p: Projection) {
    val hide = LocalSettings.current.hideValues
    var months by rememberSaveable { mutableIntStateOf(3) }
    val days = p.monthsAhead(months)
    val endDate = p.dateAt(days)
    val (tightDate, tightValue) = p.tightestWithin(days)
    FlatItem {
        Text(Res.s(R.string.engine_projection_title), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Segmented(
            listOf(3 to Res.s(R.string.engine_months_3), 6 to Res.s(R.string.engine_months_6), 12 to Res.s(R.string.engine_months_12)),
            months, { months = it },
        )
        Spacer(Modifier.height(12.dp))
        Text(Res.s(R.string.engine_projection_balance_on, Dates.dayMonthYear(endDate)), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        MoneyText(p.mid[days], style = MaterialTheme.typography.headlineSmall, color = if (p.mid[days] < 0) CryoTheme.colors.expense else MaterialTheme.colorScheme.onSurface)
        Text(
            Res.s(R.string.engine_between, money(p.low[days], hide), money(p.high[days], hide)),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        ProjectionChart(p, days)
        Spacer(Modifier.height(8.dp))
        // RF-MOT-04: a pergunta que importa, logo abaixo do gráfico e em uma frase: a conta fica negativa? quando?
        // Quanto é o menor saldo? (Pedido de quem usa, issue #3: "com isso podemos nos programar melhor".)
        val firstNeg = p.firstNegativeWithin(days)
        val tint = if (firstNeg != null) CryoTheme.colors.expense else CryoTheme.colors.income
        Surface(color = tint.copy(alpha = 0.10f), shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (firstNeg != null) Icons.Rounded.PriorityHigh else Icons.Rounded.CheckCircle, null, Modifier.size(20.dp), tint = tint)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (firstNeg != null) Res.s(R.string.proj_negative_from, Dates.dayMonth(firstNeg)) else Res.s(R.string.proj_safe),
                        style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold,
                    )
                }
                Text(
                    Res.s(R.string.proj_min_balance, money(tightValue, hide), Dates.dayMonth(tightDate)),
                    style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp),
                )
                if (firstNeg != null) {
                    Text(
                        Res.s(R.string.proj_min_hint),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
        if (p.chanceNegative >= 0.01) {
            StatLine(Icons.Rounded.PriorityHigh, Res.s(R.string.engine_chance_negative, Num.percent((p.chanceNegative * 100).roundToInt())), CryoTheme.colors.warning)
        }
        Spacer(Modifier.height(6.dp))
        Text(Res.s(R.string.engine_projection_how), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun StatLine(icon: ImageVector, text: String, tint: Color?) {
    Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, Modifier.size(18.dp), tint = tint ?: MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

/** RF-MOT-02: o que se repete (as maiores), com o total por mês. */
@Composable
fun RecurringCard(r: EngineReport, l: Ledger, onAll: () -> Unit) {
    val hide = LocalSettings.current.hideValues
    val list = r.recurrences.filter { it.status != RecurrenceStatus.ENDED }
    if (list.isEmpty()) return
    FlatItem(onClick = onAll) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(Res.s(R.string.engine_recurring_title), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Text(Res.s(R.string.engine_see_all), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
        Text(
            Res.s(R.string.engine_recurring_month, money(r.monthlyRecurringExpense, hide)),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(6.dp))
        list.take(4).forEach { RecurrenceRow(it, l, hide) }
    }
}

@Composable
private fun RecurrenceRow(rec: Recurrence, l: Ledger, hide: Boolean, onClick: (() -> Unit)? = null) {
    val cat = rec.categoryId?.let { l.category[it] }
    Row(
        Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBadge(
            if (cat != null) categoryIcon(cat.icon) else Icons.Rounded.Autorenew,
            if (cat != null) Color(cat.color) else MaterialTheme.colorScheme.primary, 36.dp,
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(rec.label, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                if (rec.status == RecurrenceStatus.ENDED) periodLabel(rec.period)
                else Res.s(R.string.engine_recurring_next, periodLabel(rec.period), Dates.dayMonth(rec.next)),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
            RecurrencePills(rec)
        }
        Spacer(Modifier.width(8.dp))
        MoneyText(
            if (rec.type == TxType.INCOME) rec.amount else -rec.amount, signed = true, style = MaterialTheme.typography.titleSmall,
            color = if (rec.type == TxType.INCOME) CryoTheme.colors.income else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun RecurrencePills(rec: Recurrence) {
    val pills = buildList {
        if (rec.status == RecurrenceStatus.LATE) add(Triple(Res.s(R.string.engine_pill_late), CryoTheme.colors.warningContainer, CryoTheme.colors.onWarningContainer))
        if (rec.status == RecurrenceStatus.ENDED) add(Triple(Res.s(R.string.engine_pill_ended), MaterialTheme.colorScheme.surfaceContainerHighest, MaterialTheme.colorScheme.onSurfaceVariant))
        if (rec.priceChange > 0) add(Triple(Res.s(R.string.engine_pill_price_up), CryoTheme.colors.expenseContainer, CryoTheme.colors.onExpenseContainer))
        if (rec.variable) add(Triple(Res.s(R.string.engine_pill_variable), MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer))
        if (rec.billId != null) add(Triple(Res.s(R.string.engine_pill_bill), MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer))
    }
    if (pills.isEmpty()) return
    FlowRow(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        pills.forEach { (t, bg, fg) -> Pill(t, bg, fg) }
    }
}

/** RF-MOT-05: o que está fora do padrão. Tocar abre a movimentação. */
@Composable
fun AnomaliesCard(list: List<Anomaly>, onOpen: (Long) -> Unit) {
    if (list.isEmpty()) return
    val hide = LocalSettings.current.hideValues
    FlatItem {
        Text(Res.s(R.string.engine_anomalies_title), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        list.take(5).forEach { a ->
            val (icon, text) = when (a.kind) {
                AnomalyKind.OUTLIER -> Icons.AutoMirrored.Rounded.TrendingUp to Res.s(R.string.engine_anomaly_outlier, money(a.usual, hide))
                AnomalyKind.DUPLICATE -> Icons.Rounded.ContentCopy to Res.s(R.string.engine_anomaly_duplicate)
                AnomalyKind.PRICE_UP -> Icons.Rounded.Autorenew to Res.s(R.string.engine_anomaly_price_up, money(a.usual, hide))
                AnomalyKind.CATEGORY_HIGH -> Icons.Rounded.Insights to Res.s(R.string.engine_anomaly_category, money(a.usual, hide))
            }
            Row(
                Modifier.fillMaxWidth().then(if (a.txId != null) Modifier.clickable { onOpen(a.txId) } else Modifier).padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconBadge(icon, CryoTheme.colors.warning, 36.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(a.label, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.width(8.dp))
                MoneyText(a.amount, style = MaterialTheme.typography.titleSmall)
            }
        }
    }
}

/* ================================ Saúde financeira ================================ */

@Composable
fun HealthScreen() {
    val nav = LocalNav.current
    val r = rememberEngine()
    Scaffold(topBar = { CryoTopBar(Res.s(R.string.engine_health_title), onBack = nav::back) }, contentWindowInsets = WindowInsets(0, 0, 0, 0)) { pad ->
        val h = r?.health
        LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp)) {
            if (h == null) return@LazyColumn
            if (!h.ready) {
                item { EmptyState(Icons.Rounded.FavoriteBorder, Res.s(R.string.engine_health_learning_title), Res.s(R.string.engine_health_learning)) }
                return@LazyColumn
            }
            item {
                Column(
                    Modifier.fillMaxWidth().padding(vertical = 8.dp).depthCard().padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    ScoreRing(h.score, levelColor(h.level), size = 132.dp, stroke = 14.dp)
                    Spacer(Modifier.height(10.dp))
                    Text(levelLabel(h.level), style = MaterialTheme.typography.titleLarge, color = levelColor(h.level))
                    Spacer(Modifier.height(6.dp))
                    Text(Res.s(R.string.engine_health_not_advice), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            items(h.parts, key = { it.kind.name }) { p ->
                FlatItem {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(partName(p.kind), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                        Text(
                            Res.s(R.string.engine_points, Num.decimal(p.points, 0), p.kind.max),
                            style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    UsageBar(p.ratio.toFloat(), warn = false, baseColor = if (p.ratio >= 0.6) CryoTheme.colors.income else if (p.ratio >= 0.3) CryoTheme.colors.warning else CryoTheme.colors.expense)
                    Spacer(Modifier.height(6.dp))
                    Text(partValue(p), style = MaterialTheme.typography.bodyMedium)
                    if (p.ratio < 0.999) {
                        Text(partAdvice(p.kind), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
            item {
                Text(
                    Res.s(R.string.engine_health_how, h.months), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp, start = 4.dp, end = 4.dp),
                )
            }
        }
    }
}

/* ================================ Recorrências ================================ */

@Composable
fun RecurringScreen() {
    val nav = LocalNav.current
    val c = LocalContainer.current
    val l = rememberLedger()
    val r = rememberEngine()
    val hide = LocalSettings.current.hideValues
    var open by remember { mutableStateOf<Recurrence?>(null) }
    Scaffold(topBar = { CryoTopBar(Res.s(R.string.engine_recurring_title), onBack = nav::back) }, contentWindowInsets = WindowInsets(0, 0, 0, 0)) { pad ->
        if (l == null || r == null) return@Scaffold
        val incomes = r.recurrences.filter { it.type == TxType.INCOME }
        val expenses = r.recurrences.filter { it.type == TxType.EXPENSE }
        LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp)) {
            if (r.recurrences.isEmpty()) {
                item { EmptyState(Icons.Rounded.Autorenew, Res.s(R.string.engine_recurring_empty_title), Res.s(R.string.engine_recurring_empty)) }
                return@LazyColumn
            }
            item {
                FlatItem {
                    Text(Res.s(R.string.engine_recurring_month, money(r.monthlyRecurringExpense, hide)), style = MaterialTheme.typography.titleSmall)
                    val inc = incomes.filter { it.status != RecurrenceStatus.ENDED }.sumOf { it.monthlyAmount }
                    if (inc > 0) Text(Res.s(R.string.engine_recurring_income_month, money(inc, hide)), style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(6.dp))
                    Text(Res.s(R.string.engine_recurring_how), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            listOf(Res.s(R.string.insights_entradas) to incomes, Res.s(R.string.insights_saidas) to expenses).forEach { (title, list) ->
                if (list.isNotEmpty()) {
                    item { Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 4.dp, top = 12.dp, bottom = 4.dp)) }
                    item {
                        Column(Modifier.fillMaxWidth().padding(vertical = 4.dp).depthCard().padding(horizontal = 16.dp, vertical = 4.dp)) {
                            list.forEachIndexed { i, rec ->
                                if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                RecurrenceRow(rec, l, hide) { open = rec }
                            }
                        }
                    }
                }
            }
        }
    }
    open?.let { rec -> RecurrenceSheet(rec, l, onDismiss = { open = null }) { open = null } }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun RecurrenceSheet(rec: Recurrence, l: Ledger?, onDismiss: () -> Unit, onDone: () -> Unit) {
    val c = LocalContainer.current
    val hide = LocalSettings.current.hideValues
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp).navigationBarsPadding()) {
            Text(rec.label, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(8.dp))
            Text(Res.s(R.string.engine_recurring_detail, periodLabel(rec.period), rec.count, Dates.monthYearInSentence(rec.first.ym(), rec.last)), style = MaterialTheme.typography.bodyMedium)
            Text(Res.s(R.string.engine_recurring_usual, money(rec.amount, hide)), style = MaterialTheme.typography.bodyMedium)
            if (rec.status != RecurrenceStatus.ENDED) Text(Res.s(R.string.engine_recurring_next_date, Dates.dayMonthYear(rec.next)), style = MaterialTheme.typography.bodyMedium)
            if (rec.priceChange > 0) {
                Text(
                    Res.s(R.string.engine_ins_price_up, rec.label, money(rec.lastAmount - rec.priceChange, hide), money(rec.lastAmount, hide)),
                    style = MaterialTheme.typography.bodyMedium, color = CryoTheme.colors.expense,
                )
            }
            Spacer(Modifier.height(16.dp))
            // Virar conta fixa: só as mensais que ainda não são (com lembrete e tudo).
            if (rec.billId == null && rec.period == RecurrencePeriod.MONTHLY && rec.status != RecurrenceStatus.ENDED && l != null) {
                io.github.h3yk0.cryo.ui.components.CryoButton(
                    onClick = {
                        onDone()
                        c.launch {
                            val bill = Bill(
                                name = rec.label, amount = rec.amount, dueDay = rec.anchorDay.coerceIn(1, 28).let { if (rec.anchorDay > 28) rec.anchorDay.coerceAtMost(31) else it },
                                kind = if (rec.type == TxType.INCOME) CategoryKind.INCOME else CategoryKind.EXPENSE,
                                categoryId = rec.categoryId, accountId = rec.accountId, cardId = rec.cardId, startYm = rec.next.ym().key(),
                            )
                            val id = c.repo.saveBill(bill)
                            c.message(Res.s(R.string.engine_bill_created), Res.s(R.string.wallet_desfazer)) { c.repo.deleteBill(bill.copy(id = id)) }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) { Text(Res.s(R.string.engine_make_bill)) }
                Spacer(Modifier.height(8.dp))
            }
            OutlinedButton(
                onClick = {
                    onDone()
                    c.engineStore.dismiss(rec.key)
                    c.message(Res.s(R.string.engine_dismissed), Res.s(R.string.wallet_desfazer)) { c.engineStore.undismiss(rec.key) }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) { Text(Res.s(R.string.engine_not_recurring)) }
        }
    }
}

/* ================================ Plano para quitar ================================ */

@Composable
fun DebtPlanScreen() {
    val nav = LocalNav.current
    val l = rememberLedger()
    val r = rememberEngine()
    val hide = LocalSettings.current.hideValues
    var extra by rememberSaveable { mutableLongStateOf(0L) }
    var strategy by rememberSaveable { mutableStateOf(PayoffStrategy.AVALANCHE) }
    Scaffold(topBar = { CryoTopBar(Res.s(R.string.engine_plan_title), onBack = nav::back) }, contentWindowInsets = WindowInsets(0, 0, 0, 0)) { pad ->
        if (l == null) return@Scaffold
        val plan = remember(l, extra, strategy) { DebtPlanner.plan(l, extra, strategy) }
        val rates = remember(r, l) { (r?.debtRates ?: DebtPlanner.rates(l)).associateBy { it.debtId } }
        LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp)) {
            if (plan == null) {
                item { EmptyState(Icons.Rounded.Savings, Res.s(R.string.engine_plan_empty_title), Res.s(R.string.engine_plan_empty)) }
                return@LazyColumn
            }
            item {
                Text(Res.s(R.string.engine_plan_intro), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(4.dp))
                AmountField(extra, { extra = it }, label = Res.s(R.string.engine_plan_extra))
                Spacer(Modifier.height(12.dp))
                Segmented(
                    listOf(PayoffStrategy.AVALANCHE to Res.s(R.string.engine_plan_avalanche), PayoffStrategy.SNOWBALL to Res.s(R.string.engine_plan_snowball)),
                    strategy, { strategy = it },
                )
                Text(
                    Res.s(if (strategy == PayoffStrategy.AVALANCHE) R.string.engine_plan_avalanche_why else R.string.engine_plan_snowball_why),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(4.dp),
                )
            }
            item {
                PlanResult {
                    val months = plan.months
                    if (months == null) {
                        Text(Res.s(R.string.engine_plan_never), style = MaterialTheme.typography.titleMedium, color = CryoTheme.colors.expense)
                    } else {
                        Text(Res.s(R.string.engine_plan_free_in), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(Dates.monthYearInSentence(plan.freeYm!!, l.today).replaceFirstChar { it.titlecase() }, style = MaterialTheme.typography.headlineSmall)
                        plan.monthsSaved?.takeIf { it > 0 }?.let {
                            Text(Res.p(R.plurals.engine_plan_months_saved, it, it), style = MaterialTheme.typography.bodyMedium, color = CryoTheme.colors.income)
                        }
                        Spacer(Modifier.height(6.dp))
                        if (plan.interest > 0) Text(Res.s(R.string.engine_plan_interest, money(plan.interest, hide)), style = MaterialTheme.typography.bodyMedium)
                        if (plan.savedInterest > 0) {
                            Text(Res.s(R.string.engine_plan_saved, money(plan.savedInterest, hide)), style = MaterialTheme.typography.bodyMedium, color = CryoTheme.colors.income)
                        }
                    }
                }
            }
            item { Text(Res.s(R.string.engine_plan_order), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 4.dp, top = 12.dp, bottom = 4.dp)) }
            items(plan.order) { id ->
                val d = l.debt[id] ?: return@items
                val info = l.debtInfo(d)
                FlatItem(onClick = { nav.go(Routes.debt(id)) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(d.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(rateText(rates[id]), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        MoneyText(info.remaining, style = MaterialTheme.typography.titleSmall)
                    }
                }
            }
            item {
                Text(Res.s(R.string.engine_plan_how), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp, start = 4.dp, end = 4.dp))
            }
        }
    }
}

@Composable
private fun PlanResult(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp).depthCard().padding(18.dp), content = content)
}

/** Ícone para o atalho do plano (na tela de dívidas). */
val PlanIcon: ImageVector get() = Icons.Rounded.Timeline
