// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.h3yk0.cryo.R
import io.github.h3yk0.cryo.domain.Dates
import io.github.h3yk0.cryo.domain.FlowKind
import io.github.h3yk0.cryo.domain.FlowNode
import io.github.h3yk0.cryo.domain.Money
import io.github.h3yk0.cryo.domain.MoneyFlow
import io.github.h3yk0.cryo.domain.MonthBar
import io.github.h3yk0.cryo.domain.MonthForecast
import io.github.h3yk0.cryo.domain.Num
import io.github.h3yk0.cryo.domain.Res
import io.github.h3yk0.cryo.domain.engine.BandPoint
import io.github.h3yk0.cryo.domain.engine.Projection
import io.github.h3yk0.cryo.ui.theme.CryoTheme
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.max
import kotlin.math.min

/* ============================== Rosca ============================== */

@Composable
fun DonutChart(
    values: List<Long>,
    colors: List<Color>,
    modifier: Modifier = Modifier,
    thickness: Dp = 22.dp,
    description: String = "",
    center: @Composable BoxScope.() -> Unit = {},
) {
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    // DS-MOV-06: a rosca gira até as fatias ao aparecer.
    val appear by rememberAppear(durationMillis = 800)
    Box(modifier.aspectRatio(1f).semantics { contentDescription = description }, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxWidth().aspectRatio(1f)) {
            val stroke = thickness.toPx()
            val d = size.minDimension - stroke
            val tl = Offset((size.width - d) / 2, (size.height - d) / 2)
            val total = values.sum().toFloat()
            drawArc(track, 0f, 360f, false, tl, Size(d, d), style = Stroke(stroke))
            if (total <= 0f) return@Canvas
            val gap = if (values.count { it > 0 } > 1) 1.6f else 0f
            var start = -90f
            val end = -90f + 360f * appear
            values.forEachIndexed { i, v ->
                val sweep = v / total * 360f
                // Só a parte da fatia que a volta já alcançou.
                val visible = min(sweep, end - start)
                if (sweep > 0f && visible > 0f) {
                    drawArc(colors[i], start + gap / 2, max(0.4f, min(sweep - gap, visible)), false, tl, Size(d, d), style = Stroke(stroke))
                }
                start += sweep
            }
        }
        center()
    }
}

/* ===================== Previsão do fim do mês ===================== */

/**
 * O saldo das contas nos próximos [days] dias: a faixa (8 em cada 10 cenários) e a linha do mais provável. O dia mais
 * apertado ganha um ponto.
 */
@Composable
fun ProjectionChart(p: Projection, days: Int, modifier: Modifier = Modifier, height: Dp = 170.dp) {
    val primary = MaterialTheme.colorScheme.primary
    val grid = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val negative = CryoTheme.colors.expense
    val measurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = labelColor)
    val n = days.coerceIn(1, p.horizonDays)
    val (tightDate, tightValue) = p.tightestWithin(n)
    val desc = Res.s(R.string.chart_projection_desc, Money.format(p.mid[n]), Dates.dayMonth(tightDate), Money.format(tightValue))
    val appear by rememberAppear(durationMillis = 900)
    Canvas(modifier.fillMaxWidth().height(height).semantics { contentDescription = desc }) {
        var minV = Float.MAX_VALUE; var maxV = -Float.MAX_VALUE
        for (i in 0..n) { minV = min(minV, p.low[i].toFloat()); maxV = max(maxV, p.high[i].toFloat()) }
        // O zero só entra na escala quando o saldo chega perto dele (senão a linha fica achatada no alto).
        if (minV < 0.25f * maxV) { minV = min(minV, 0f); maxV = max(maxV, 0f) }
        val padV = (maxV - minV) * 0.08f
        minV -= padV; maxV += padV
        if (maxV - minV < 1f) { maxV += 100f; minV -= 100f }
        val padTop = 8.dp.toPx(); val padBottom = 18.dp.toPx()
        val w = size.width
        val h = size.height - padTop - padBottom
        fun x(i: Int) = i.toFloat() / n * w
        fun y(v: Long) = padTop + (maxV - v) / (maxV - minV) * h
        if (minV <= 0f && maxV >= 0f) {
            val zy = y(0)
            drawLine(grid, Offset(0f, zy), Offset(w, zy), strokeWidth = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)))
        }
        // Um ponto a cada poucos dias basta (até 12 meses).
        val step = max(1, n / 120)
        val idx = (0..n step step).toMutableList().also { if (it.last() != n) it += n }
        clipRect(right = size.width * appear) {
            val area = Path()
            idx.forEachIndexed { k, i -> if (k == 0) area.moveTo(x(i), y(p.high[i])) else area.lineTo(x(i), y(p.high[i])) }
            idx.asReversed().forEach { i -> area.lineTo(x(i), y(p.low[i])) }
            area.close()
            drawPath(area, primary.copy(alpha = 0.14f))
            val line = Path()
            idx.forEachIndexed { k, i -> if (k == 0) line.moveTo(x(i), y(p.mid[i])) else line.lineTo(x(i), y(p.mid[i])) }
            drawPath(line, primary, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
        if (appear >= 1f) {
            val ti = java.time.temporal.ChronoUnit.DAYS.between(p.start, tightDate).toInt()
            val c = if (tightValue < 0) negative else primary
            drawCircle(c, 5.dp.toPx(), Offset(x(ti), y(tightValue)))
            drawCircle(Color.White, 2.dp.toPx(), Offset(x(ti), y(tightValue)))
        }
        // Meses no eixo X (o primeiro dia de cada mês).
        var m = YearMonth.from(p.start).plusMonths(1)
        val endDate = p.start.plusDays(n.toLong())
        val every = if (n > 200) 3 else if (n > 100) 2 else 1
        var k = 0
        while (!m.atDay(1).isAfter(endDate)) {
            if (k++ % every == 0) {
                val i = java.time.temporal.ChronoUnit.DAYS.between(p.start, m.atDay(1)).toInt()
                val l = measurer.measure(Dates.monthShort(m), labelStyle)
                drawText(l, topLeft = Offset((x(i) - l.size.width / 2).coerceIn(0f, size.width - l.size.width), size.height - l.size.height))
            }
            m = m.plusMonths(1)
        }
    }
}

/* ===================== Anel da saúde financeira (RF-MOT-06) ===================== */

@Composable
fun ScoreRing(score: Int, color: Color, modifier: Modifier = Modifier, size: Dp = 88.dp, stroke: Dp = 10.dp) {
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    val appear by rememberAppear(durationMillis = 900)
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val sw = stroke.toPx()
            val inset = sw / 2
            val arcSize = Size(this.size.width - sw, this.size.height - sw)
            drawArc(track, -90f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(sw, cap = StrokeCap.Round))
            drawArc(color, -90f, 360f * score / 100f * appear, false, Offset(inset, inset), arcSize, style = Stroke(sw, cap = StrokeCap.Round))
        }
        Text(score.toString(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
    }
}

/* ========================= Barras por mês ========================= */

@Composable
fun MonthBarsChart(bars: List<MonthBar>, modifier: Modifier = Modifier, height: Dp = 180.dp) {
    val inc = CryoTheme.colors.income
    val exp = CryoTheme.colors.expense
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val selColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val measurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = labelColor)
    var selected by remember(bars) { mutableIntStateOf(bars.lastIndex) }
    // DS-MOV-06: as barras sobem ao aparecer.
    val appear by rememberAppear()
    val desc = bars.joinToString(". ") {
        Res.s(R.string.chart_entrou_saiu, Dates.monthName(it.ym), Money.format(it.income), Money.format(it.expense))
    }
    Column(modifier) {
        bars.getOrNull(selected)?.let { b ->
            Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(Dates.monthName(b.ym), style = MaterialTheme.typography.labelLarge)
                Row {
                    LegendDot(inc); Text(" ${Money.compact(b.income)}   ", style = MaterialTheme.typography.labelLarge)
                    LegendDot(exp); Text(" ${Money.compact(b.expense)}", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(height)
                .semantics { contentDescription = desc }
                .pointerInput(bars) {
                    detectTapGestures { o ->
                        val gw = size.width / bars.size.toFloat()
                        selected = (o.x / gw).toInt().coerceIn(0, bars.lastIndex)
                    }
                },
        ) {
            if (bars.isEmpty()) return@Canvas
            val maxV = max(1L, bars.maxOf { max(it.income, it.expense) }).toFloat()
            val labelH = 18.dp.toPx()
            val h = size.height - labelH
            val gw = size.width / bars.size
            val bw = min(18.dp.toPx(), gw * 0.28f)
            bars.forEachIndexed { i, b ->
                val cx = gw * i + gw / 2
                if (i == selected) drawRoundRect(selColor, Offset(gw * i + 2, 0f), Size(gw - 4, h), CornerRadius(12f, 12f))
                val ih = b.income / maxV * (h - 8.dp.toPx()) * appear
                val eh = b.expense / maxV * (h - 8.dp.toPx()) * appear
                drawRoundRect(inc, Offset(cx - bw - 2, h - ih), Size(bw, ih), CornerRadius(bw / 2, bw / 2))
                drawRoundRect(exp, Offset(cx + 2, h - eh), Size(bw, eh), CornerRadius(bw / 2, bw / 2))
                val l = measurer.measure(Dates.monthShort(b.ym), labelStyle)
                drawText(l, topLeft = Offset(cx - l.size.width / 2, size.height - l.size.height))
            }
        }
    }
}

@Composable
fun LegendDot(color: Color, size: Dp = 10.dp) {
    Box(Modifier.padding(top = 4.dp).size(size).clip(CircleShape).background(color))
}

/* ======================= Calendário de calor ======================= */

/**
 * RF-ANA-02 (1.7): o nível de temperatura de cada dia, de 0 a 5, comparando com a mediana dos dias com gasto do próprio mês
 * (o que é muito para uma pessoa não é para outra): 0 = sem gasto; 1 = até a metade da mediana; 2 = até a mediana; 3 = até
 * 1,5×; 4 = até 2,5×; 5 = acima. As contas são em inteiros (centavos), sem arredondar a mediana.
 */
fun heatLevels(daily: LongArray): IntArray {
    val nonZero = daily.drop(1).filter { it > 0 }.sorted()
    val median = if (nonZero.isEmpty()) 0L else nonZero[nonZero.size / 2]
    return IntArray(daily.size) { i ->
        val v = daily[i]
        when {
            i == 0 || v <= 0 -> 0
            v * 2 <= median -> 1
            v <= median -> 2
            v * 2 <= median * 3 -> 3
            v * 2 <= median * 5 -> 4
            else -> 5
        }
    }
}

/** ERR-067: as iniciais dos dias da semana, de domingo a sábado, na língua do app (eram fixas em português). */
fun weekInitials(locale: java.util.Locale): List<String> = (0L..6L).map {
    java.time.DayOfWeek.SUNDAY.plus(it).getDisplayName(java.time.format.TextStyle.NARROW, locale)
}

@Composable
fun HeatmapCalendar(
    ym: YearMonth,
    daily: LongArray,
    today: LocalDate,
    selectedDay: Int?,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val levels = remember(daily) { heatLevels(daily) }
    val heat = CryoTheme.heat
    val primary = MaterialTheme.colorScheme.primary
    val emptyBg = MaterialTheme.colorScheme.surfaceContainerHigh
    fun bgOf(lvl: Int) = if (lvl == 0) emptyBg else heat.levels[lvl - 1]
    val initials = remember(io.github.h3yk0.cryo.domain.AppLocale.locale) { weekInitials(io.github.h3yk0.cryo.domain.AppLocale.locale) }
    val firstDow = ym.atDay(1).dayOfWeek.value % 7 // domingo = 0
    val days = ym.lengthOfMonth()
    val cells = firstDow + days
    val rows = (cells + 6) / 7
    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth()) {
            initials.forEach {
                Text(
                    it, Modifier.weight(1f), textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        for (r in 0 until rows) {
            Row(Modifier.fillMaxWidth()) {
                for (c in 0 until 7) {
                    val idx = r * 7 + c
                    val day = idx - firstDow + 1
                    Box(Modifier.weight(1f).aspectRatio(1f).padding(3.dp)) {
                        if (day in 1..days) {
                            val date = ym.atDay(day)
                            val future = date.isAfter(today)
                            val lvl = levels[day]
                            val fg = when {
                                future -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                lvl == 0 -> MaterialTheme.colorScheme.onSurface
                                else -> heat.on[lvl - 1]
                            }
                            val sel = selectedDay == day
                            val label = if (daily[day] > 0) Res.s(R.string.chart_dia_gastou, day, Money.format(daily[day])) else Res.s(R.string.chart_dia_sem_gastos, day)
                            Box(
                                Modifier
                                    .matchParentSizeCompat()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (future) Color.Transparent else bgOf(lvl))
                                    .then(
                                        if (sel) Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, RoundedCornerShape(10.dp))
                                        else if (date == today) Modifier.border(1.5.dp, primary, RoundedCornerShape(10.dp))
                                        else Modifier,
                                    )
                                    .selectable(selected = sel, enabled = !future, role = Role.Button) { onSelect(day) }
                                    .semantics { contentDescription = label },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    day.toString(), color = fg,
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = if (date == today) FontWeight.Bold else FontWeight.Medium,
                                    ),
                                )
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        // A legenda: as seis cores, do frio ao quente.
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Text(Res.s(R.string.chart_menos), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(6.dp))
            for (lvl in 0..5) {
                Box(Modifier.padding(horizontal = 2.dp).size(14.dp).clip(RoundedCornerShape(4.dp)).background(bgOf(lvl)))
            }
            Spacer(Modifier.width(6.dp))
            Text(Res.s(R.string.chart_mais), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun Modifier.matchParentSizeCompat() = this.then(Modifier.fillMaxWidth().aspectRatio(1f))

/* ========================= Fluxo do dinheiro ========================= */

/** Um polígono com os cantos arredondados (cada canto vira uma curva de raio até [r], sem passar da metade do lado). */
private fun roundedPolygon(points: List<Offset>, r: Float): Path {
    val path = Path()
    val n = points.size
    for (i in 0 until n) {
        val p = points[i]
        val prev = points[(i - 1 + n) % n]
        val next = points[(i + 1) % n]
        val d1 = prev - p
        val d2 = next - p
        val l1 = d1.getDistance().coerceAtLeast(0.001f)
        val l2 = d2.getDistance().coerceAtLeast(0.001f)
        val rr = min(r, min(l1, l2) / 2)
        val a = p + d1 * (rr / l1)
        val b = p + d2 * (rr / l2)
        if (i == 0) path.moveTo(a.x, a.y) else path.lineTo(a.x, a.y)
        path.quadraticTo(p.x, p.y, b.x, b.y)
    }
    path.close()
    return path
}

/** Uma faixa do funil: o que ela mostra e as larguras (frações da área do funil) em cima e embaixo. */
private class FunnelBand(
    val node: FlowNode?,
    val name: String,
    val value: Long,
    val color: Color,
    val topFrac: Float,
    val bottomFrac: Float,
)

/**
 * As faixas do funil, de cima para baixo (RF-ANA-09, revisto na DLA de 23/11/2026): a primeira é todo o dinheiro do mês
 * (o que entrou e, se faltou, o que saiu do saldo de antes); cada saída, da maior para a menor, estreita o funil pelo
 * tanto que levou; no fundo, o que sobrou. A boca nunca fecha num ponto ([MIN_MOUTH] da largura), para caber o toque.
 */
private fun funnelBands(flow: MoneyFlow, topName: String, topColor: Color, surplusColor: Color, neutral: Color): List<FunnelBand> {
    val total = flow.total.toFloat().coerceAtLeast(1f)
    fun frac(remaining: Long) = MIN_MOUTH + (1f - MIN_MOUTH) * (remaining.coerceAtLeast(0) / total)
    val bands = mutableListOf(FunnelBand(null, topName, flow.total, topColor, 1f, 1f))
    var remaining = flow.total
    flow.destinations.filter { it.kind != FlowKind.SURPLUS }.forEach { d ->
        val top = frac(remaining)
        remaining -= d.value
        val color = if (d.kind == FlowKind.FROM_BALANCE) neutral else Color(d.color)
        bands += FunnelBand(d, d.label, d.value, color, top, frac(remaining))
    }
    flow.destinations.firstOrNull { it.kind == FlowKind.SURPLUS }?.let { sp ->
        val w = frac(sp.value)
        bands += FunnelBand(sp, sp.label, sp.value, surplusColor, w, w)
    }
    return bands
}

private const val MIN_MOUTH = 0.24f
private val FUNNEL_ROW = 46.dp
private val FUNNEL_SIDE = 5.dp
private val FUNNEL_GAP = 6.dp

/**
 * RF-ANA-09 (1.7, LA4: "mais com cara de funil mesmo, só que saltado e clicável"): o funil do fluxo do dinheiro. Em cima, as
 * entradas como etiquetas que levam às movimentações delas; embaixo, o funil: o dinheiro do mês entra pela boca larga e
 * escorre pelas saídas, cada faixa mais estreita que a anterior pelo tanto que aquela saída levou, até o que sobrou. As
 * faixas têm volume (degradê, a beirada de baixo mais escura e sombra suave, DS-PROF) e afundam um pouco ao toque; o nome, o
 * valor e a porcentagem ficam ao lado. Em árabe e hebraico, espelhado. Com [hide], nenhum valor aparece, nem para o leitor
 * de tela (ERR-068). Tocar numa faixa (ou no nome dela) que [canOpen] aceita chama [onNode] (RF-ANA-10).
 */
@Composable
fun FlowFunnel(
    flow: MoneyFlow,
    hide: Boolean,
    onNode: (FlowNode) -> Unit,
    modifier: Modifier = Modifier,
    canOpen: (FlowNode) -> Boolean = { true },
) {
    if (flow.isEmpty) return
    val rtl = androidx.compose.ui.platform.LocalLayoutDirection.current == androidx.compose.ui.unit.LayoutDirection.Rtl
    val measurer = rememberTextMeasurer()
    val haptics = rememberHaptics()
    val dark = CryoTheme.colors.isDark
    val income = CryoTheme.colors.income
    val neutral = MaterialTheme.colorScheme.outline
    val shadow = CryoTheme.depth.shadow
    val nameStyle = MaterialTheme.typography.labelLarge.copy(color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
    val valueStyle = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
    val topName = Res.s(R.string.insights_funnel_top)
    // No escuro, o verde das letras é claro demais para encher uma faixa: o funil usa um verde do meio, como as outras cores.
    val bandIncome = if (dark) androidx.compose.ui.graphics.lerp(income, Color.Black, 0.42f) else income
    val bands = remember(flow, topName, bandIncome, neutral) { funnelBands(flow, topName, bandIncome, bandIncome, neutral) }
    val total = flow.total.coerceAtLeast(1)
    val appear by rememberAppear(durationMillis = Motion.LONG * 2)
    var pressed by remember { mutableIntStateOf(-1) }
    val press by androidx.compose.animation.core.animateFloatAsState(if (pressed >= 0) 1f else 0f, label = "funnelPress")
    var lastPressed by remember { mutableIntStateOf(-1) }
    if (pressed >= 0) lastPressed = pressed
    fun valueOf(v: Long) = (if (hide) Money.HIDDEN else Money.compact(v)) + " · " + Num.percent((v * 100 / total).toInt())
    // O texto mais largo ao lado das faixas (nome ou valor): o funil fica com o resto da largura, entre 42% e 56%.
    val longest = remember(bands, hide, nameStyle, valueStyle) {
        bands.maxOf { b ->
            max(measurer.measure(b.name, nameStyle, maxLines = 1).size.width, measurer.measure(valueOf(b.value), valueStyle, maxLines = 1).size.width)
        }.toFloat()
    }
    // ACS-04 e ERR-068: com os valores escondidos, o leitor de tela também não lê valor nenhum.
    val desc = Res.s(
        R.string.chart_flow_a11y,
        flow.sources.joinToString { if (hide) "${it.label} ${Money.HIDDEN}" else "${it.label} ${Money.format(it.value)}" },
        flow.destinations.joinToString { if (hide) "${it.label} ${Money.HIDDEN}" else "${it.label} ${Money.format(it.value)}" },
    )
    Column(modifier.fillMaxWidth()) {
        // A boca do funil: de onde o dinheiro veio, cada entrada uma etiqueta (as que são movimentações levam a elas).
        androidx.compose.foundation.layout.FlowRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            flow.sources.forEach { n ->
                val open = canOpen(n)
                val color = if (n.kind == FlowKind.FROM_BALANCE) neutral else Color(n.color)
                Row(
                    Modifier.clip(RoundedCornerShape(50)).background(CryoTheme.depth.grouped)
                        .then(if (open) Modifier.clickable(role = Role.Button) { haptics.tap(); onNode(n) } else Modifier)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(10.dp).clip(CircleShape).background(color))
                    Spacer(Modifier.width(6.dp))
                    Text(n.label, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.width(6.dp))
                    Text(valueOf(n.value), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        val rowStep = FUNNEL_ROW + FUNNEL_SIDE + FUNNEL_GAP
        val height = rowStep * bands.size
        Canvas(
            Modifier.fillMaxWidth().height(height)
                .pointerInput(bands, rtl) {
                    val step = rowStep.toPx()
                    fun bandAt(y: Float): Int {
                        val i = (y / step).toInt()
                        val inside = y - i * step <= (FUNNEL_ROW + FUNNEL_SIDE).toPx()
                        return if (i in bands.indices && inside && bands[i].node?.let(canOpen) == true) i else -1
                    }
                    detectTapGestures(
                        onPress = { p ->
                            val i = bandAt(p.y)
                            if (i >= 0) {
                                pressed = i
                                tryAwaitRelease()
                                pressed = -1
                            }
                        },
                        onTap = { p -> bandAt(p.y).takeIf { it >= 0 }?.let { i -> haptics.tap(); bands[i].node?.let(onNode) } },
                    )
                }
                .semantics { contentDescription = desc },
        ) {
            val w = size.width
            val labelGap = 14.dp.toPx()
            val funnelW = (w - labelGap - longest).coerceIn(w * 0.42f, w * 0.56f)
            val labelW = (w - funnelW - labelGap).toInt().coerceAtLeast(1)
            val row = FUNNEL_ROW.toPx()
            val side = FUNNEL_SIDE.toPx()
            val step = rowStep.toPx()
            val radius = 10.dp.toPx()
            fun mx(x: Float) = if (rtl) w - x else x
            bands.forEachIndexed { i, b ->
                // As faixas aparecem uma a uma, de cima para baixo (o dinheiro escorrendo).
                val t = (appear * (bands.size + 1) - i).coerceIn(0f, 1f)
                if (t <= 0f) return@forEachIndexed
                val sink = if (i == lastPressed) press * (side - 1.dp.toPx()) else 0f
                val top = i * step + sink
                val cx = funnelW / 2
                val tw = funnelW * b.topFrac / 2
                val bw = funnelW * b.bottomFrac / 2
                // O trapézio da faixa (cantos arredondados), deslocado [dy] para baixo: a beirada e a sombra são cópias dele.
                fun body(dy: Float) = roundedPolygon(
                    listOf(
                        Offset(mx(cx - tw), top + dy), Offset(mx(cx + tw), top + dy),
                        Offset(mx(cx + bw), top + dy + row), Offset(mx(cx - bw), top + dy + row),
                    ),
                    radius,
                )
                val a = t
                // Sombra suave embaixo (some quando a faixa afunda).
                drawPath(body(side + 3.dp.toPx() - sink * 0.6f), shadow.copy(alpha = shadow.alpha * a))
                // A beirada de baixo, mais escura: dá a espessura ("saltado").
                drawPath(body(side - sink), androidx.compose.ui.graphics.lerp(b.color, Color.Black, if (dark) 0.45f else 0.30f).copy(alpha = a))
                // O corpo, com degradê de cima (mais claro) para baixo.
                drawPath(
                    body(0f),
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        listOf(
                            androidx.compose.ui.graphics.lerp(b.color, Color.White, if (dark) 0.10f else 0.22f).copy(alpha = a),
                            b.color.copy(alpha = a),
                        ),
                        startY = top, endY = top + row,
                    ),
                )
                // O fio de luz em cima.
                drawLine(
                    Color.White.copy(alpha = (if (dark) 0.18f else 0.45f) * a),
                    Offset(mx(cx - tw + radius), top + 1.dp.toPx()), Offset(mx(cx + tw - radius), top + 1.dp.toPx()),
                    strokeWidth = 1.5.dp.toPx(), cap = StrokeCap.Round,
                )
                // O nome, o valor e a porcentagem ao lado, alinhados ao meio da faixa.
                val name = measurer.measure(b.name, nameStyle, overflow = TextOverflow.Ellipsis, maxLines = 1, constraints = Constraints(maxWidth = labelW))
                val value = measurer.measure(valueOf(b.value), valueStyle, overflow = TextOverflow.Ellipsis, maxLines = 1, constraints = Constraints(maxWidth = labelW))
                val blockH = name.size.height + value.size.height
                val y = top + (row + side) / 2 - blockH / 2
                val lx = funnelW + labelGap
                fun x(width: Int) = if (rtl) w - lx - width else lx
                drawText(name, topLeft = Offset(x(name.size.width), y), alpha = a)
                drawText(value, topLeft = Offset(x(value.size.width), y + name.size.height), alpha = a)
            }
        }
    }
}

/**
 * RF-MOT-17 (1.6, LA8-3): o saldo previsto até o fim do ano, numa janela que a pessoa arrasta e aproxima com os dedos.
 * [window] = os dias mostrados (a tela controla: o zoom da primeira vista e o "Ver o período todo"); [onWindow] = o que os
 * dedos pedem. Os sinais vermelhos pulsam três vezes ([pulse] de 0 a 1 em cada pulso; sem animação, ficam parados).
 */
@Composable
fun YearChart(
    p: Projection, days: Int,
    signals: List<io.github.h3yk0.cryo.domain.engine.YearSignal>,
    window: io.github.h3yk0.cryo.domain.engine.ChartWindow,
    onWindow: (io.github.h3yk0.cryo.domain.engine.ChartWindow) -> Unit,
    pulse: Float,
    description: String,
    modifier: Modifier = Modifier, height: Dp = 200.dp,
) {
    val primary = MaterialTheme.colorScheme.primary
    val grid = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val negative = CryoTheme.colors.expense
    val measurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = labelColor)
    val n = days.coerceIn(1, p.horizonDays)
    val current = androidx.compose.runtime.rememberUpdatedState(window)
    val widthPx = androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(1f) }
    Canvas(
        modifier.fillMaxWidth().height(height)
            .noTabSwipe()
            .semantics { contentDescription = description }
            .pointerInput(n) {
                detectTransformGestures { centroid, pan, zoom, _ ->
                    val w = current.value
                    val perDay = widthPx.floatValue / w.span.coerceAtLeast(1f)
                    val focus = w.from + centroid.x / perDay
                    // Arrastar para a esquerda leva para o futuro (como numa linha do tempo).
                    var next = io.github.h3yk0.cryo.domain.engine.YearOutlooks.pan(w, -pan.x / perDay, n)
                    if (zoom != 1f) next = io.github.h3yk0.cryo.domain.engine.YearOutlooks.zoom(next, zoom, focus, n)
                    onWindow(next)
                }
            },
    ) {
        widthPx.floatValue = size.width
        val from = window.from.coerceIn(0f, n.toFloat())
        val to = window.to.coerceIn(from + 1f, n.toFloat())
        val i0 = kotlin.math.floor(from).toInt().coerceIn(0, n)
        val i1 = kotlin.math.ceil(to).toInt().coerceIn(i0, n)
        var minV = Float.MAX_VALUE; var maxV = -Float.MAX_VALUE
        for (i in i0..i1) { minV = min(minV, p.low[i].toFloat()); maxV = max(maxV, p.high[i].toFloat()) }
        if (minV < 0.25f * maxV) { minV = min(minV, 0f); maxV = max(maxV, 0f) }
        val padV = (maxV - minV) * 0.08f
        minV -= padV; maxV += padV
        if (maxV - minV < 1f) { maxV += 100f; minV -= 100f }
        val padTop = 10.dp.toPx(); val padBottom = 18.dp.toPx()
        val w = size.width
        val h = size.height - padTop - padBottom
        fun x(i: Float) = (i - from) / (to - from) * w
        fun y(v: Long) = padTop + (maxV - v) / (maxV - minV) * h
        if (minV <= 0f && maxV >= 0f) {
            val zy = y(0)
            drawLine(grid, Offset(0f, zy), Offset(w, zy), strokeWidth = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)))
        }
        val step = max(1, (i1 - i0) / 160)
        val idx = (i0..i1 step step).toMutableList().also { if (it.last() != i1) it += i1 }
        clipRect {
            val area = Path()
            idx.forEachIndexed { k, i -> if (k == 0) area.moveTo(x(i.toFloat()), y(p.high[i])) else area.lineTo(x(i.toFloat()), y(p.high[i])) }
            idx.asReversed().forEach { i -> area.lineTo(x(i.toFloat()), y(p.low[i])) }
            area.close()
            drawPath(area, primary.copy(alpha = 0.14f))
            val line = Path()
            idx.forEachIndexed { k, i -> if (k == 0) line.moveTo(x(i.toFloat()), y(p.mid[i])) else line.lineTo(x(i.toFloat()), y(p.mid[i])) }
            drawPath(line, primary, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            // Os sinais: um ponto vermelho no saldo daquele dia, com o anel que pulsa.
            for (s in signals) {
                val i = io.github.h3yk0.cryo.domain.engine.YearOutlooks.index(p, s.date)
                if (i < i0 || i > i1) continue
                val c = Offset(x(i.toFloat()), y(p.mid[i]))
                if (pulse > 0f && pulse < 1f) drawCircle(negative.copy(alpha = 0.35f * (1f - pulse)), (6 + 14 * pulse).dp.toPx(), c)
                drawCircle(negative, 6.dp.toPx(), c)
                drawCircle(Color.White, 2.5.dp.toPx(), c)
            }
        }
        // Meses no eixo X, conforme o zoom (todo mês, ou um a cada 2 ou 3).
        val span = to - from
        val every = if (span > 300) 3 else if (span > 150) 2 else 1
        var m = YearMonth.from(p.start).plusMonths(1)
        val endDate = p.start.plusDays(n.toLong())
        var k = 0
        while (!m.atDay(1).isAfter(endDate)) {
            val i = java.time.temporal.ChronoUnit.DAYS.between(p.start, m.atDay(1)).toFloat()
            if (k++ % every == 0 && i >= from && i <= to) {
                val text = if (m.monthValue == 1) "${Dates.monthShort(m)} ${m.year}" else Dates.monthShort(m)
                val lay = measurer.measure(text, labelStyle)
                drawText(lay, topLeft = Offset((x(i) - lay.size.width / 2).coerceIn(0f, size.width - lay.size.width), size.height - lay.size.height))
            }
            m = m.plusMonths(1)
        }
    }
}
