// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.ui.components

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
            values.forEachIndexed { i, v ->
                val sweep = v / total * 360f
                if (sweep > 0f) {
                    drawArc(colors[i], start + gap / 2, max(0.4f, sweep - gap), false, tl, Size(d, d), style = Stroke(stroke))
                }
                start += sweep
            }
        }
        center()
    }
}

/* ===================== Previsão do fim do mês ===================== */

@Composable
fun ForecastChart(f: MonthForecast, modifier: Modifier = Modifier, height: Dp = 180.dp) {
    val primary = MaterialTheme.colorScheme.primary
    val grid = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val endColor = if (f.projectedResult >= 0) CryoTheme.colors.income else CryoTheme.colors.expense
    val measurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = labelColor)
    val endStyle = MaterialTheme.typography.labelLarge.copy(color = endColor, fontWeight = FontWeight.Bold)
    val desc = "Gráfico da previsão do mês. Hoje o resultado é ${Money.format(f.currentResult)}. " +
        "Previsão para o fim do mês: ${Money.format(f.projectedResult)}."
    Canvas(modifier.fillMaxWidth().height(height).semantics { contentDescription = desc }) {
        val pts = f.series
        if (pts.isEmpty()) return@Canvas
        val values = pts.flatMap { listOfNotNull(it.actual, it.projected) } + 0L
        var minV = values.min().toFloat()
        var maxV = values.max().toFloat()
        if (maxV - minV < 1f) { maxV += 100f; minV -= 100f }
        val padTop = 26.dp.toPx()
        val padBottom = 18.dp.toPx()
        val padRight = 8.dp.toPx()
        val w = size.width - padRight
        val h = size.height - padTop - padBottom
        fun x(day: Int) = (day - 1).toFloat() / max(1, f.daysInMonth - 1) * w
        fun y(v: Long) = padTop + (maxV - v) / (maxV - minV) * h

        // linha do zero
        val zy = y(0)
        drawLine(grid, Offset(0f, zy), Offset(w, zy), strokeWidth = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)))

        // área + linha do realizado
        val actual = pts.filter { it.actual != null }
        if (actual.isNotEmpty()) {
            val line = Path()
            val area = Path()
            actual.forEachIndexed { i, p ->
                val px = x(p.day); val py = y(p.actual!!)
                if (i == 0) { line.moveTo(px, py); area.moveTo(px, zy); area.lineTo(px, py) } else { line.lineTo(px, py); area.lineTo(px, py) }
            }
            area.lineTo(x(actual.last().day), zy); area.close()
            drawPath(area, primary.copy(alpha = 0.12f))
            drawPath(line, primary, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
        // projeção tracejada
        val proj = pts.filter { it.projected != null }
        if (proj.size > 1) {
            val p = Path()
            proj.forEachIndexed { i, pt -> if (i == 0) p.moveTo(x(pt.day), y(pt.projected!!)) else p.lineTo(x(pt.day), y(pt.projected!!)) }
            drawPath(
                p, endColor,
                style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f))),
            )
        }
        // ponto de hoje
        actual.lastOrNull()?.let { t ->
            drawCircle(primary, 5.dp.toPx(), Offset(x(t.day), y(t.actual!!)))
            drawCircle(Color.White, 2.dp.toPx(), Offset(x(t.day), y(t.actual)))
        }
        // valor previsto no fim: fixo no canto superior direito (área reservada), nunca encobre a linha
        val last = pts.last()
        val lastV = last.projected ?: last.actual ?: 0L
        val txt = measurer.measure("Fim do mês: ${Money.compact(lastV)}", endStyle)
        drawText(txt, topLeft = Offset((size.width - txt.size.width).coerceAtLeast(0f), 0f))
        if (proj.size > 1 || last.actual != null) drawCircle(endColor, 4.dp.toPx(), Offset(x(last.day), y(lastV)))

        // dias no eixo X
        listOf(1, 10, 20, f.daysInMonth).distinct().forEach { d ->
            val l = measurer.measure(d.toString(), labelStyle)
            drawText(l, topLeft = Offset((x(d) - l.size.width / 2).coerceIn(0f, size.width - l.size.width), size.height - l.size.height))
        }
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
    val desc = bars.joinToString(". ") {
        "${Dates.monthName(it.ym)}: entrou ${Money.format(it.income)}, saiu ${Money.format(it.expense)}"
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
                val ih = b.income / maxV * (h - 8.dp.toPx())
                val eh = b.expense / maxV * (h - 8.dp.toPx())
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
                            val label = if (daily[day] > 0) "Dia $day: gastou ${Money.format(daily[day])}" else "Dia $day: sem gastos"
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
            Text("Menos", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(6.dp))
            alphas.forEachIndexed { i, a ->
                Box(
                    Modifier.padding(horizontal = 2.dp).size(14.dp).clip(RoundedCornerShape(4.dp))
                        .background(if (i == 0) emptyBg else primary.copy(alpha = a).compositeOver(emptyBg)),
                )
            }
            Spacer(Modifier.width(6.dp))
            Text("Mais", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    val desc = "Fluxo do dinheiro. Entradas: " + flow.sources.joinToString { "${it.label} ${Money.format(it.value)}" } +
        ". Saídas: " + flow.destinations.joinToString { "${it.label} ${Money.format(it.value)}" }
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
