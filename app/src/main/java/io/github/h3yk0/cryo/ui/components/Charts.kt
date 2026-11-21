// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.ui.components

import androidx.compose.foundation.gestures.detectTransformGestures
import io.github.h3yk0.cryo.R
import io.github.h3yk0.cryo.domain.Res
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.drawscope.clipRect
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.h3yk0.cryo.domain.Dates
import io.github.h3yk0.cryo.domain.FlowKind
import io.github.h3yk0.cryo.domain.Money
import io.github.h3yk0.cryo.domain.MonthBar
import io.github.h3yk0.cryo.domain.MonthForecast
import io.github.h3yk0.cryo.domain.MoneyFlow
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

/** Mapeia o gasto do dia para um nível de 0 a 4, comparando com a mediana dos dias com gasto. */
fun heatLevels(daily: LongArray): IntArray {
    val nonZero = daily.drop(1).filter { it > 0 }.sorted()
    val median = if (nonZero.isEmpty()) 0L else nonZero[nonZero.size / 2]
    return IntArray(daily.size) { i ->
        val v = daily[i]
        when {
            i == 0 || v <= 0 -> 0
            v <= median / 2 -> 1
            v <= median -> 2
            v <= median * 2 -> 3
            else -> 4
        }
    }
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
    val primary = MaterialTheme.colorScheme.primary
    val alphas = listOf(0f, 0.22f, 0.42f, 0.7f, 1f)
    val emptyBg = MaterialTheme.colorScheme.surfaceContainerHigh
    val firstDow = ym.atDay(1).dayOfWeek.value % 7 // domingo = 0
    val days = ym.lengthOfMonth()
    val cells = firstDow + days
    val rows = (cells + 6) / 7
    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth()) {
            listOf("D", "S", "T", "Q", "Q", "S", "S").forEach {
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
                            val bg = if (lvl == 0) emptyBg else primary.copy(alpha = alphas[lvl]).compositeOver(emptyBg)
                            val fg = when {
                                future -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                lvl >= 3 -> MaterialTheme.colorScheme.onPrimary
                                else -> MaterialTheme.colorScheme.onSurface
                            }
                            val sel = selectedDay == day
                            val label = if (daily[day] > 0) Res.s(R.string.chart_dia_gastou, day, Money.format(daily[day])) else Res.s(R.string.chart_dia_sem_gastos, day)
                            Box(
                                Modifier
                                    .matchParentSizeCompat()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (future) Color.Transparent else bg)
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
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Text(Res.s(R.string.chart_menos), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(6.dp))
            alphas.forEachIndexed { i, a ->
                Box(
                    Modifier.padding(horizontal = 2.dp).size(14.dp).clip(RoundedCornerShape(4.dp))
                        .background(if (i == 0) emptyBg else primary.copy(alpha = a).compositeOver(emptyBg)),
                )
            }
            Spacer(Modifier.width(6.dp))
            Text(Res.s(R.string.chart_mais), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun Modifier.matchParentSizeCompat() = this.then(Modifier.fillMaxWidth().aspectRatio(1f))

fun Color.compositeOver(bg: Color): Color {
    val a = alpha
    return Color(
        red = red * a + bg.red * (1 - a),
        green = green * a + bg.green * (1 - a),
        blue = blue * a + bg.blue * (1 - a),
        alpha = 1f,
    )
}

/* ========================= Fluxo do dinheiro ========================= */

/**
 * Diagrama de fluxo (Sankey) na vertical: em cima, de onde o dinheiro veio; no meio, o total do mês;
 * embaixo, para onde ele foi. A largura de cada faixa é proporcional ao valor.
 */
@Composable
fun SankeyChart(flow: MoneyFlow, modifier: Modifier = Modifier, height: Dp = 260.dp) {
    val surplus = CryoTheme.colors.income
    val neutral = MaterialTheme.colorScheme.outline
    val mid = MaterialTheme.colorScheme.onSurface
    val onMid = MaterialTheme.colorScheme.surface
    val measurer = rememberTextMeasurer()
    val midStyle = TextStyle(color = onMid, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    fun colorOf(kind: FlowKind, raw: Int): Color = when (kind) {
        FlowKind.SURPLUS -> surplus
        FlowKind.FROM_BALANCE -> neutral
        else -> Color(raw)
    }
    val desc = Res.s(
        R.string.chart_flow_a11y,
        flow.sources.joinToString { "${it.label} ${Money.format(it.value)}" },
        flow.destinations.joinToString { "${it.label} ${Money.format(it.value)}" },
    )
    Canvas(modifier.fillMaxWidth().height(height).semantics { contentDescription = desc }) {
        val total = flow.total.toFloat()
        if (total <= 0f) return@Canvas
        val barH = 14.dp.toPx()
        val gap = 3.dp.toPx()
        val midW = size.width * 0.42f
        val midX = (size.width - midW) / 2
        val midY = size.height / 2 - barH / 2
        val bottomY = size.height - barH

        fun layout(values: List<Long>, width: Float, x0: Float, gapPx: Float): List<Pair<Float, Float>> {
            val usable = width - gapPx * max(0, values.size - 1)
            var x = x0
            return values.map { v ->
                val w = v / total * usable
                val r = x to x + w
                x += w + gapPx
                r
            }
        }

        val top = layout(flow.sources.map { it.value }, size.width, 0f, gap)
        val bottom = layout(flow.destinations.map { it.value }, size.width, 0f, gap)
        val midIn = layout(flow.sources.map { it.value }, midW, midX, 0f)
        val midOut = layout(flow.destinations.map { it.value }, midW, midX, 0f)

        fun ribbon(a: Pair<Float, Float>, ay: Float, b: Pair<Float, Float>, by: Float, c: Color) {
            val cy1 = ay + (by - ay) * 0.5f
            val p = Path().apply {
                moveTo(a.first, ay)
                cubicTo(a.first, cy1, b.first, cy1, b.first, by)
                lineTo(b.second, by)
                cubicTo(b.second, cy1, a.second, cy1, a.second, ay)
                close()
            }
            drawPath(p, c.copy(alpha = 0.38f))
        }

        flow.sources.forEachIndexed { i, n ->
            val c = colorOf(n.kind, n.color)
            ribbon(top[i], barH, midIn[i], midY, c)
            drawRoundRect(c, Offset(top[i].first, 0f), Size(max(1f, top[i].second - top[i].first), barH), CornerRadius(barH / 3))
        }
        flow.destinations.forEachIndexed { i, n ->
            val c = colorOf(n.kind, n.color)
            ribbon(midOut[i], midY + barH, bottom[i], bottomY, c)
            drawRoundRect(c, Offset(bottom[i].first, bottomY), Size(max(1f, bottom[i].second - bottom[i].first), barH), CornerRadius(barH / 3))
        }
        // nó central com o total
        val nodeH = 26.dp.toPx()
        drawRoundRect(mid, Offset(midX, midY + barH / 2 - nodeH / 2), Size(midW, nodeH), CornerRadius(nodeH / 2))
        val t = measurer.measure(Money.compact(flow.total), midStyle)
        drawText(t, topLeft = Offset(size.width / 2 - t.size.width / 2, midY + barH / 2 - t.size.height / 2))
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
