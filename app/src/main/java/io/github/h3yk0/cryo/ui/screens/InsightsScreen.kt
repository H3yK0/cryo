// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.h3yk0.cryo.R
import io.github.h3yk0.cryo.data.db.CategoryKind
import io.github.h3yk0.cryo.domain.Dates
import io.github.h3yk0.cryo.domain.FlowKind
import io.github.h3yk0.cryo.domain.FlowNode
import io.github.h3yk0.cryo.domain.Money
import io.github.h3yk0.cryo.domain.MoneyFlow
import io.github.h3yk0.cryo.domain.Num
import io.github.h3yk0.cryo.domain.Res
import io.github.h3yk0.cryo.domain.key
import io.github.h3yk0.cryo.domain.lastMonths
import io.github.h3yk0.cryo.domain.moneyFlow
import io.github.h3yk0.cryo.domain.predict.EndItem
import io.github.h3yk0.cryo.domain.predict.EndLine
import io.github.h3yk0.cryo.domain.predict.ModelKind
import io.github.h3yk0.cryo.domain.predict.MonthEnd
import io.github.h3yk0.cryo.domain.predict.MonthEnds
import io.github.h3yk0.cryo.domain.ym
import io.github.h3yk0.cryo.domain.ymOf
import io.github.h3yk0.cryo.ui.LocalNav
import io.github.h3yk0.cryo.ui.Routes
import io.github.h3yk0.cryo.ui.components.CryoTopBar
import io.github.h3yk0.cryo.ui.components.DonutChart
import io.github.h3yk0.cryo.ui.components.Expandable
import io.github.h3yk0.cryo.ui.components.FlatItem
import io.github.h3yk0.cryo.ui.components.HeatmapCalendar
import io.github.h3yk0.cryo.ui.components.IconBadge
import io.github.h3yk0.cryo.ui.components.LegendDot
import io.github.h3yk0.cryo.ui.components.LocalContainer
import io.github.h3yk0.cryo.ui.components.LocalSettings
import io.github.h3yk0.cryo.ui.components.MoneyText
import io.github.h3yk0.cryo.ui.components.MonthBarsChart
import io.github.h3yk0.cryo.ui.components.MonthSelector
import io.github.h3yk0.cryo.ui.components.SectionTitle
import io.github.h3yk0.cryo.ui.components.Segmented
import io.github.h3yk0.cryo.ui.components.UsageBar
import io.github.h3yk0.cryo.ui.components.categoryIcon
import io.github.h3yk0.cryo.ui.components.depthCard
import io.github.h3yk0.cryo.ui.components.rememberEngine
import io.github.h3yk0.cryo.ui.components.rememberLedger
import io.github.h3yk0.cryo.ui.theme.CryoTheme
import io.github.h3yk0.cryo.ui.tx.LoadingBox
import io.github.h3yk0.cryo.domain.Layouts
import io.github.h3yk0.cryo.ui.components.ArrangeGroup
import io.github.h3yk0.cryo.ui.components.ArrangeSheet
import io.github.h3yk0.cryo.ui.components.Block
import io.github.h3yk0.cryo.ui.components.OrganizeButton

/**
 * TELA-ANALISES (RF-ANA-07, reforma visual da 1.6, LA11): o mês (seletor), o fim do mês real (RF-PRV-10), os últimos 6
 * meses, como vou fechar o ano, a saúde financeira, o fluxo do dinheiro em blocos, as categorias e o calendário de calor.
 * As percepções e o que saiu do padrão moram na central de avisos (RF-INI-19); a previsão de metas, em Planejar › Metas.
 */
@Composable
fun InsightsScreen() {
    val l = rememberLedger() ?: return LoadingBox()
    var ymKey by rememberSaveable { mutableIntStateOf(l.today.ym().key()) }
    val ym = ymOf(ymKey)
    val isCurrent = ym == l.today.ym()
    val flow = remember(l, ymKey) { l.moneyFlow(ym) }
    val cats = remember(l, ymKey) { l.byCategory(ym, CategoryKind.EXPENSE) }
    val bars = remember(l, ymKey) { l.lastMonths(6, if (ym.isAfter(l.today.ym())) l.today.ym() else ym) }
    val summary = remember(l, ymKey) { l.monthSummary(ym) }
    // RF-PRV-10: o mesmo número do Início (renda × despesas, só o real).
    val end = remember(l) { MonthEnds.of(l) }
    val nav = LocalNav.current
    // RF-MOT: o motor calcula fora da linha principal; enquanto não chega, a tela mostra o que já tinha.
    val engine = rememberEngine()
    var organizing by rememberSaveable { mutableStateOf(false) }
    val layout = LocalSettings.current.layout(Layouts.INSIGHTS)

    Scaffold(topBar = { CryoTopBar(Res.s(R.string.tab_insights)) }, contentWindowInsets = WindowInsets(0, 0, 0, 0)) { pad ->
        LazyColumn(
            Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // RF-ORG-01..05 (1.7, LA8): cada parte é um bloco que a pessoa põe na ordem dela e esconde. A ordem padrão é a de
            // sempre: a saúde financeira primeiro (RF-ANA-08: a nota é de agora, não do mês escolhido). O seletor do mês não
            // é bloco: fica logo antes do primeiro bloco que depende do mês.
            val blocks = listOfNotNull(
                Block("saude", Res.s(R.string.engine_health_title)) {
                    item(key = "saude") {
                        val e = engine
                        // Enquanto o motor calcula, o lugar fica reservado, para a tela não pular.
                        if (e != null) HealthCard(e.health) { nav.go(Routes.HEALTH) } else HealthPlaceholder()
                    }
                },
                Block("fim", Res.s(R.string.insights_previsao_fim_mes)) {
                    item(key = "fim") { if (isCurrent) MonthEndCard(end) else MonthResultCard(summary) }
                },
                // DS-PSI-02: a comparação rápida logo abaixo do mês.
                Block("seis", Res.s(R.string.insights_ultimos_6_meses)) {
                    item(key = "seis_titulo") { SectionTitle(Res.s(R.string.insights_ultimos_6_meses)) }
                    item(key = "seis") {
                        FlatItem {
                            MonthBarsChart(bars)
                            Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                LegendDot(CryoTheme.colors.income); Text(Res.s(R.string.insights_entradas), style = MaterialTheme.typography.labelMedium)
                                LegendDot(CryoTheme.colors.expense); Text(Res.s(R.string.insights_saidas), style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                },
                // RF-MOT-17: o ano (só no mês de agora, quando o motor já respondeu).
                engine?.year?.takeIf { isCurrent }?.let { y -> Block("ano", Res.s(R.string.yr_title)) { item(key = "ano") { YearOutlookCard(l, y) } } },
                Block("fluxo", Res.s(R.string.insights_fluxo_dinheiro)) {
                    item(key = "fluxo_titulo") { SectionTitle(Res.s(R.string.insights_fluxo_dinheiro)) }
                    item(key = "fluxo") {
                        // RF-ANA-09 (1.7): a vista guardada (Barras ou Funil); a local responde na hora, a guardada vale da próxima vez.
                        val c = LocalContainer.current
                        val saved = LocalSettings.current.flowView
                        var view by rememberSaveable(saved) { mutableIntStateOf(saved) }
                        FlowBox(flow, view, onView = { v -> view = v; c.launch { c.settings.setFlowView(v) } }) { node -> openFlowNode(nav, node, ym) }
                    }
                },
                Block("categorias", Res.s(R.string.insights_gastos_por_categoria_2)) {
                    item(key = "categorias_titulo") { SectionTitle(Res.s(R.string.insights_gastos_por_categoria_2)) }
                    item(key = "categorias") { CategoriesBlock(cats) { id -> nav.open(Routes.txs(id, ym.key())) } }
                },
                Block("calendario", Res.s(R.string.insights_calendario_calor)) {
                    item(key = "calendario_titulo") { SectionTitle(Res.s(R.string.insights_calendario_calor)) }
                    item(key = "calendario") { HeatBlock(l, ym, ymKey, isCurrent) }
                },
            )
            var selectorDone = false
            val byKey = blocks.associateBy { it.key }
            layout.visible(blocks.map { it.key }).forEach { key ->
                if (!selectorDone && key != "saude") {
                    selectorDone = true
                    item(key = "mes") {
                        MonthSelector(
                            Dates.monthYear(ym, l.today), { ymKey = ym.minusMonths(1).key() }, { ymKey = ym.plusMonths(1).key() },
                            nextEnabled = ym.isBefore(l.today.ym().plusMonths(1)),
                        )
                    }
                }
                byKey[key]?.content?.invoke(this)
            }
            item(key = "organizar") { OrganizeButton(Modifier.padding(top = 8.dp)) { organizing = true } }
        }
    }
    if (organizing) ArrangeSheet(listOf(ArrangeGroup(Layouts.INSIGHTS, null, INSIGHTS_BLOCKS.map { (k, r) -> k to Res.s(r) })), onDismiss = { organizing = false })
}

/** RF-ORG: os blocos de Análises na ordem padrão (todos, mesmo os que só aparecem em alguns meses), com o nome de cada um. */
private val INSIGHTS_BLOCKS = listOf(
    "saude" to R.string.engine_health_title,
    "fim" to R.string.insights_previsao_fim_mes,
    "seis" to R.string.insights_ultimos_6_meses,
    "ano" to R.string.yr_title,
    "fluxo" to R.string.insights_fluxo_dinheiro,
    "categorias" to R.string.insights_gastos_por_categoria_2,
    "calendario" to R.string.insights_calendario_calor,
)

/** RF-ANA-02: o calendário de calor do mês, o dia tocado e quantos dias tiveram gasto. */
@Composable
private fun HeatBlock(l: io.github.h3yk0.cryo.domain.Ledger, ym: java.time.YearMonth, ymKey: Int, isCurrent: Boolean) {
                var day by remember(ymKey) { mutableStateOf<Int?>(null) }
                val daily = remember(l, ymKey) { l.dailyExpenses(ym) }
                FlatItem {
                    HeatmapCalendar(ym, daily, l.today, day, onSelect = { day = it })
                    day?.let {
                        Text(
                            Res.s(R.string.insights_dia, it, if (daily[it] > 0) Res.s(R.string.insights_gastou, Money.format(daily[it])) else Res.s(R.string.insights_sem_gastos)),
                            style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                    val days = if (isCurrent) l.today.dayOfMonth else ym.lengthOfMonth()
                    val active = (1..days).count { daily[it] > 0 }
                    Text(
                        if (days - active > 0) Res.s(R.string.insights_spent_days_with_free, active, days, Res.p(R.plurals.insights_days_without_spending, days - active, days - active))
                        else Res.s(R.string.insights_spent_days, active, days),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
}

/**
 * RF-PRV-10 e RF-ANA-07: o fim do mês real, na cor do resultado (DS-TOM: menta quando sobra, rosa quando falta). O número,
 * a frase de quanto dá por dia, Renda × Despesas em duas barras e "Como cheguei nesse número" fechado com setinha (DS-EXP).
 * ERR-056: o número de cima é sempre a soma das linhas de baixo (e o mesmo do Início).
 */
@Composable
private fun MonthEndCard(end: MonthEnd) {
    val hide = LocalSettings.current.hideValues
    val tone = if (end.total >= 0) CryoTheme.tones.good else CryoTheme.tones.alert
    Column(
        Modifier.fillMaxWidth().padding(vertical = 5.dp)
            .depthCard(shape = MaterialTheme.shapes.large, color = tone.container)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        CompositionLocalProvider(LocalContentColor provides tone.onContainer) {
            Text(Res.s(R.string.insights_previsao_fim_mes), style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
            Row(verticalAlignment = Alignment.Bottom) {
                MoneyText(end.total, signed = true, style = MaterialTheme.typography.headlineMedium, color = tone.accent)
                Text(
                    if (end.total >= 0) Res.s(R.string.insights_devem_sobrar) else Res.s(R.string.insights_devem_faltar),
                    style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 4.dp),
                )
            }
            Text(
                when {
                    end.total > 0 -> Res.s(R.string.home_month_left, money(end.total, hide), money(end.perDay, hide))
                    end.total < 0 -> Res.s(R.string.home_month_short, money(-end.total, hide))
                    else -> Res.s(R.string.home_month_even)
                },
                style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 2.dp),
            )
            Spacer(Modifier.height(14.dp))
            val top = maxOf(end.income, end.expenses).coerceAtLeast(1)
            EndBar(Res.s(R.string.insights_end_income), end.income, top, CryoTheme.colors.income)
            Spacer(Modifier.height(10.dp))
            EndBar(Res.s(R.string.insights_end_expenses), end.expenses, top, CryoTheme.colors.expense)
            Spacer(Modifier.height(6.dp))
            Expandable(Res.s(R.string.insights_como_cheguei_nesse_numero)) {
                Text(
                    Res.s(R.string.insights_end_note), style = MaterialTheme.typography.bodySmall,
                    color = LocalContentColor.current.copy(alpha = 0.8f), modifier = Modifier.padding(bottom = 6.dp),
                )
                for (item in end.items) {
                    Line(
                        endLineLabel(item.line), item.amount,
                        if (item.amount >= 0) CryoTheme.colors.income else CryoTheme.colors.expense,
                        detail = pendingText(item, hide),
                    )
                }
                HorizontalDivider(Modifier.padding(vertical = 6.dp), color = LocalContentColor.current.copy(alpha = 0.2f))
                Line(Res.s(R.string.month_end), end.total, tone.accent, bold = true)
            }
        }
    }
}

/** RF-ANA-07: uma barra de Renda ou de Despesas, do tamanho da maior das duas. */
@Composable
private fun EndBar(label: String, value: Long, top: Long, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
        MoneyText(value, style = MaterialTheme.typography.titleSmall)
    }
    Spacer(Modifier.height(4.dp))
    UsageBar(value.toFloat() / top, height = 12.dp, baseColor = color, warn = false)
}

/** Mês que não é o atual: o resultado de verdade (entrou, saiu, investido, guardado e o que sobrou). */
@Composable
private fun MonthResultCard(summary: io.github.h3yk0.cryo.domain.MonthSummary) {
    FlatItem {
        Text(Res.s(R.string.insights_resultado_mes), style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
        Spacer(Modifier.height(8.dp))
        Line(Res.s(R.string.month_in), summary.income, CryoTheme.colors.income)
        Line(Res.s(R.string.month_out), -summary.expense, CryoTheme.colors.expense)
        if (summary.invested != 0L) Line(Res.s(R.string.insights_investido), -summary.invested, MaterialTheme.colorScheme.onSurface)
        if (summary.saved != 0L) Line(Res.s(R.string.plan_guardado_metas), -summary.saved, MaterialTheme.colorScheme.onSurface)
        HorizontalDivider(Modifier.padding(vertical = 6.dp))
        Line(Res.s(R.string.insights_sobrou), summary.result, if (summary.result >= 0) CryoTheme.colors.income else CryoTheme.colors.expense, bold = true)
    }
}

/** RF-PRV-10: o nome de cada linha de "Como cheguei nesse número". */
private fun endLineLabel(line: EndLine): String = Res.s(
    when (line) {
        EndLine.IN_FIXED -> R.string.end_in_fixed
        EndLine.IN_OTHER -> R.string.end_in_other
        EndLine.OUT_BILLS -> R.string.end_out_bills
        EndLine.OUT_GOALS -> R.string.end_out_goals
        EndLine.OUT_DEBTS -> R.string.end_out_debts
        EndLine.OUT_VARIABLE -> R.string.end_out_variable
        EndLine.OUT_SCHEDULED -> R.string.end_out_scheduled
    },
)

/** O que ainda vem (ou falta pagar, ou guardar) dentro de uma linha; null quando já aconteceu tudo. */
private fun pendingText(item: EndItem, hide: Boolean): String? {
    if (item.pending == 0L || item.line == EndLine.OUT_SCHEDULED) return null
    val v = money(kotlin.math.abs(item.pending), hide)
    return Res.s(
        when {
            item.pending > 0 -> R.string.end_pending_in
            item.line == EndLine.OUT_GOALS -> R.string.end_pending_save
            else -> R.string.end_pending_out
        },
        v,
    )
}

private fun money(v: Long, hide: Boolean) = if (hide) Money.HIDDEN else Money.format(v)

/** RF-ANA-08: o lugar da saúde financeira enquanto o motor faz a primeira conta (a tela não pula quando ela chega). */
@Composable
private fun HealthPlaceholder() {
    FlatItem {
        Text(Res.s(R.string.engine_health_title), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(56.dp))
    }
}

/**
 * RF-ANA-10 (1.7): para onde cada parte do fluxo leva. Uma categoria abre o Extrato do mês filtrado por ela (RF-EXT-12);
 * investimentos, metas e o dinheiro emprestado abrem as telas deles; "Outras categorias" abre o Extrato do mês só nas saídas.
 * "Sobrou" e "Do saldo que você já tinha" não são movimentações: não levam a lugar nenhum.
 */
private fun flowTarget(node: FlowNode): Boolean = node.categoryId != null || node.kind in setOf(FlowKind.INVEST, FlowKind.GOALS, FlowKind.BORROWED, FlowKind.OTHERS)

private fun openFlowNode(nav: io.github.h3yk0.cryo.ui.Navigator, node: FlowNode, ym: java.time.YearMonth) {
    when {
        node.categoryId != null -> nav.open(Routes.txs(node.categoryId, ym.key()))
        node.kind == FlowKind.INVEST -> nav.go(Routes.WALLET)
        node.kind == FlowKind.GOALS -> nav.open(Routes.plan(2))
        node.kind == FlowKind.BORROWED -> nav.go(Routes.DEBTS)
        node.kind == FlowKind.OTHERS -> nav.open(Routes.txs(0, ym.key(), out = true))
    }
}

/**
 * RF-ANA-09 e RF-ANA-03 (1.7, LA3 e LA5 da 1.7): o fluxo do dinheiro numa caixa só (região comum, DS-PSI-04): o seletor
 * Barras | Funil, os três números em caixinhas de dentro e o gráfico escolhido. Tocar numa parte chama [onNode] (RF-ANA-10).
 */
@Composable
private fun FlowBox(flow: MoneyFlow, view: Int, onView: (Int) -> Unit, onNode: (FlowNode) -> Unit) {
    val hide = LocalSettings.current.hideValues
    FlatItem {
        if (flow.isEmpty) {
            Text(Res.s(R.string.insights_registre_entradas_saidas_para), color = MaterialTheme.colorScheme.onSurfaceVariant)
            return@FlatItem
        }
        Segmented(listOf(0 to Res.s(R.string.insights_flow_view_bars), 1 to Res.s(R.string.insights_flow_view_funnel)), view, onView)
        Spacer(Modifier.height(12.dp))
        val totalIn = flow.sources.filter { it.kind != FlowKind.FROM_BALANCE }.sumOf { it.value }
        val totalOut = flow.destinations.filter { it.kind != FlowKind.SURPLUS }.sumOf { it.value }
        val left = totalIn - totalOut
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FlowStat(Res.s(R.string.month_in), totalIn, CryoTheme.colors.income, Modifier.weight(1f))
            FlowStat(Res.s(R.string.month_out), totalOut, CryoTheme.colors.expense, Modifier.weight(1f))
            FlowStat(Res.s(R.string.insights_sobrou), left, if (left >= 0) CryoTheme.colors.income else CryoTheme.colors.expense, Modifier.weight(1f), signed = true)
        }
        Spacer(Modifier.height(10.dp))
        Text(Res.s(R.string.insights_flow_tap_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(6.dp))
        if (view == 1) {
            io.github.h3yk0.cryo.ui.components.FlowFunnel(flow, hide, onNode = { if (flowTarget(it)) onNode(it) }, modifier = Modifier.padding(top = 4.dp), canOpen = ::flowTarget)
        } else {
            FlowBars(Res.s(R.string.insights_onde_veio), flow.sources, totalIn.coerceAtLeast(1), onNode)
            Spacer(Modifier.height(8.dp))
            FlowBars(Res.s(R.string.insights_para_onde_foi), flow.destinations, maxOf(totalIn, totalOut).coerceAtLeast(1), onNode)
        }
    }
}

/** Um dos três números do fluxo (Entrou, Saiu, Sobrou), numa caixinha de dentro, no tom do fundo (sem sombra dentro de sombra). */
@Composable
private fun FlowStat(label: String, value: Long, color: Color, modifier: Modifier, signed: Boolean = false) {
    Column(
        modifier.clip(MaterialTheme.shapes.medium).background(CryoTheme.depth.grouped).padding(horizontal = 10.dp, vertical = 10.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(4.dp))
        MoneyText(value, style = MaterialTheme.typography.titleSmall, color = color, signed = signed)
    }
}

/** "De onde veio" ou "Para onde foi": cada parte numa linha com o valor, quanto é do total e a barra; as que levam a algum lugar têm a setinha. */
@Composable
private fun FlowBars(title: String, nodes: List<FlowNode>, total: Long, onNode: (FlowNode) -> Unit) {
    val hide = LocalSettings.current.hideValues
    val surplus = CryoTheme.colors.income
    val neutral = MaterialTheme.colorScheme.outline
    Text(title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 4.dp).semantics { heading() })
    nodes.forEach { n ->
        val color = when (n.kind) {
            FlowKind.SURPLUS -> surplus
            FlowKind.FROM_BALANCE -> neutral
            else -> Color(n.color)
        }
        val target = flowTarget(n)
        Column(
            Modifier.fillMaxWidth().clip(MaterialTheme.shapes.small)
                .then(if (target) Modifier.clickable(role = androidx.compose.ui.semantics.Role.Button) { onNode(n) } else Modifier)
                .padding(vertical = 8.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(n.label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    (if (hide) "" else Money.compact(n.value) + " · ") + Num.percent((n.value * 100 / total).toInt()),
                    style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (target) {
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                } else {
                    Spacer(Modifier.width(18.dp))
                }
            }
            Spacer(Modifier.height(4.dp))
            UsageBar(n.value.toFloat() / total, height = 8.dp, baseColor = color, warn = false)
        }
    }
}

/** RF-ANA-07: o gráfico das categorias, as 4 maiores e o resto fechado com setinha (DS-EXP). */
@Composable
private fun CategoriesBlock(cats: List<io.github.h3yk0.cryo.domain.CategoryTotal>, onOpen: (Long) -> Unit) {
    FlatItem {
        if (cats.isEmpty()) {
            Text(Res.s(R.string.insights_sem_gastos_neste_mes), color = MaterialTheme.colorScheme.onSurfaceVariant)
            return@FlatItem
        }
        val total = cats.sumOf { it.total }
        DonutChart(
            values = cats.map { it.total },
            colors = cats.map { Color(it.category?.color ?: 0xFF9E9E9E.toInt()) },
            modifier = Modifier.fillMaxWidth(0.62f).align(Alignment.CenterHorizontally),
            description = Res.s(R.string.insights_gastos_por_categoria) + cats.joinToString { "${it.category?.name}: ${Money.format(it.total)}" },
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(Res.s(R.string.insights_total), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                MoneyText(total, style = MaterialTheme.typography.titleMedium)
            }
        }
        Spacer(Modifier.height(12.dp))
        cats.take(TOP_CATEGORIES).forEach { CategoryLine(it, total, onOpen) }
        if (cats.size > TOP_CATEGORIES) {
            Expandable(Res.s(R.string.insights_cats_all), Modifier.padding(top = 4.dp)) {
                cats.drop(TOP_CATEGORIES).forEach { CategoryLine(it, total, onOpen) }
            }
        }
    }
}

private const val TOP_CATEGORIES = 4

@Composable
private fun CategoryLine(ct: io.github.h3yk0.cryo.domain.CategoryTotal, total: Long, onOpen: (Long) -> Unit) {
    val cat = ct.category
    Row(
        Modifier.fillMaxWidth().clickable(enabled = cat != null) { cat?.let { onOpen(it.id) } }.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (cat != null) IconBadge(categoryIcon(cat.icon), Color(cat.color), 32.dp)
        Spacer(Modifier.width(10.dp))
        Text(cat?.name ?: Res.s(R.string.insights_sem_categoria), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(Num.percent((ct.total * 100 / total.coerceAtLeast(1)).toInt()) + "  ", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        MoneyText(ct.total, style = MaterialTheme.typography.titleSmall)
    }
}

/** O nome simples de cada modelo do previsor (RF-PRV-02), para a tela explicar a conta. */
internal fun modelName(k: ModelKind): Int = when (k) {
    ModelKind.PROFILE -> R.string.prv_model_profile
    ModelKind.PACE_91 -> R.string.prv_model_pace91
    ModelKind.PACE_182 -> R.string.prv_model_pace182
    ModelKind.SAME_PHASE -> R.string.prv_model_same_phase
    ModelKind.PHASE_LEVEL -> R.string.prv_model_phase_level
    ModelKind.HOLT -> R.string.prv_model_holt
    ModelKind.THETA -> R.string.prv_model_theta
}

@Composable
private fun Line(label: String, value: Long, color: Color, bold: Boolean = false, detail: String? = null) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, style = if (bold) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium)
            detail?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = LocalContentColor.current.copy(alpha = 0.75f))
            }
        }
        MoneyText(value, signed = true, color = color, style = MaterialTheme.typography.bodyMedium, fontWeight = if (bold) FontWeight.Bold else null)
    }
}
