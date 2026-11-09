// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import io.github.h3yk0.cryo.domain.GoalArt
import io.github.h3yk0.cryo.ui.theme.ArtColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/*
 * RF-ILU: as ilustrações do Cryo, desenhadas do zero em código (nada de emoji de teclado, nada de arquivo de imagem). Todas
 * seguem o ícone do app (DS-ICO): um azulejo com o degradê azul, fio de luz em cima e sombra suave; o desenho em branco e
 * azul-gelo, com o dourado da marca como destaque. Cada uma é feita num quadro de 100 × 100 e escala para qualquer tamanho:
 * a mesma ilustração serve na tela, na notificação e na figurinha animada ([t] = momento da animação, de 0 a 1, em laço).
 */

/** As situações (avisos e momentos). */
enum class Art {
    BUDGET, LIMIT, INVOICE, BILL, SALARY, NO_SPEND, STREAK, WEEK, MONTH, PRICE_UP, DUPLICATE, OUTLIER, NEGATIVE,
    LAST_INSTALLMENT, HEALTH, GOAL_NEAR, DEBT_FREE, CELEBRATE, SHOPPING,
}

/** Uma ilustração de situação, no azulejo. [description] vai para o leitor de tela (ACS-01). */
@Composable
fun ArtImage(art: Art, size: Dp, modifier: Modifier = Modifier, t: Float = 0f, description: String? = null) {
    Canvas(modifier.size(size).let { m -> if (description != null) m.semantics { contentDescription = description } else m }) {
        drawArtTile(art, t)
    }
}

/** A ilustração de uma meta, no azulejo da cor da meta. */
@Composable
fun GoalImage(art: GoalArt, color: Color, size: Dp, modifier: Modifier = Modifier, description: String? = null) {
    Canvas(modifier.size(size).let { m -> if (description != null) m.semantics { contentDescription = description } else m }) {
        drawGoalTile(art, color)
    }
}

/** A ilustração como imagem (para a notificação e para as figurinhas animadas). */
fun renderArt(px: Int, draw: DrawScope.() -> Unit): Bitmap {
    val img = ImageBitmap(px, px)
    val canvas = androidx.compose.ui.graphics.Canvas(img)
    CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, canvas, Size(px.toFloat(), px.toFloat()), draw)
    return img.asAndroidBitmap()
}

/* ============================================ azulejo ============================================ */

/** Desenha no quadro de 100 × 100, centrado e escalado para o tamanho do Canvas. */
private inline fun DrawScope.unit(block: DrawScope.() -> Unit) {
    val k = size.minDimension / 100f
    withTransform({
        translate((size.width - 100f * k) / 2f, (size.height - 100f * k) / 2f)
        scale(k, k, Offset.Zero)
    }, block)
}

private fun tilePath(): Path = Path().apply { addRoundRect(RoundRect(Rect(4f, 4f, 96f, 96f), CornerRadius(24f))) }

fun DrawScope.drawArtTile(art: Art, t: Float = 0f) = unit {
    tile(ArtColors.tileTop, ArtColors.tileBottom)
    clipPath(tilePath()) { drawArt(art, t) }
}

fun DrawScope.drawGoalTile(art: GoalArt, color: Color) = unit {
    // O azulejo pega a cor da meta: mais clara em cima, mais funda embaixo.
    tile(lerpColor(color, Color.White, 0.18f), lerpColor(color, Color.Black, 0.38f))
    clipPath(tilePath()) { drawGoal(art) }
}

private fun lerpColor(a: Color, b: Color, f: Float) = Color(
    a.red + (b.red - a.red) * f, a.green + (b.green - a.green) * f, a.blue + (b.blue - a.blue) * f, 1f,
)

private fun DrawScope.tile(top: Color, bottom: Color) {
    // Sombra suave embaixo (DS-PROF-01: larga e clara, nunca borda escura).
    drawRoundRect(Color.Black.copy(alpha = 0.10f), Offset(6f, 9f), Size(88f, 89f), CornerRadius(26f))
    drawRoundRect(Brush.verticalGradient(listOf(top, bottom), 4f, 96f), Offset(4f, 4f), Size(92f, 92f), CornerRadius(24f))
    // Brilho de cima para baixo e o fio de luz.
    drawRoundRect(
        Brush.radialGradient(listOf(Color.White.copy(alpha = 0.22f), Color.Transparent), Offset(30f, 14f), 70f),
        Offset(4f, 4f), Size(92f, 92f), CornerRadius(24f),
    )
    drawRoundRect(Color.White.copy(alpha = 0.35f), Offset(5f, 5f), Size(90f, 90f), CornerRadius(23f), style = Stroke(1.2f))
}

/* =========================================== peças =========================================== */

private val gold get() = Brush.verticalGradient(listOf(ArtColors.goldLight, ArtColors.gold, ArtColors.goldDeep))
private fun goldIn(top: Float, bottom: Float) = Brush.verticalGradient(listOf(ArtColors.goldLight, ArtColors.gold, ArtColors.goldDeep), top, bottom)
private fun iceIn(top: Float, bottom: Float) = Brush.verticalGradient(listOf(Color.White, ArtColors.ice), top, bottom)

/** Sombra do desenho sobre o azulejo: a mesma forma, escura e um pouco abaixo. */
private fun DrawScope.shadow(path: Path, dy: Float = 2.6f, alpha: Float = 0.22f) =
    translate(0f, dy) { drawPath(path, ArtColors.ink.copy(alpha = alpha)) }

private fun DrawScope.solid(path: Path, brush: Brush, dy: Float = 2.6f) {
    shadow(path, dy)
    drawPath(path, brush)
}

private fun rrect(x: Float, y: Float, w: Float, h: Float, r: Float) = Path().apply { addRoundRect(RoundRect(Rect(x, y, x + w, y + h), CornerRadius(r))) }
private fun circle(cx: Float, cy: Float, r: Float) = Path().apply { addOval(Rect(cx - r, cy - r, cx + r, cy + r)) }

private fun DrawScope.line(a: Offset, b: Offset, color: Color, w: Float) = drawLine(color, a, b, w, cap = StrokeCap.Round)

private fun DrawScope.check(cx: Float, cy: Float, s: Float, color: Color, w: Float) {
    val p = Path().apply { moveTo(cx - s, cy); lineTo(cx - s * 0.3f, cy + s * 0.7f); lineTo(cx + s, cy - s * 0.75f) }
    drawPath(p, color, style = Stroke(w, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

/** Selo redondo no canto (verde de ok, vermelho de atenção, dourado de destaque). */
private fun DrawScope.badge(cx: Float, cy: Float, r: Float, fill: Brush, content: DrawScope.() -> Unit) {
    drawCircle(ArtColors.ink.copy(alpha = 0.25f), r, Offset(cx, cy + 2f))
    drawCircle(fill, r, Offset(cx, cy))
    drawCircle(Color.White.copy(alpha = 0.9f), r, Offset(cx, cy), style = Stroke(2.2f))
    content()
}

private val greenBrush get() = Brush.verticalGradient(listOf(ArtColors.green, ArtColors.greenDeep))
private val redBrush get() = Brush.verticalGradient(listOf(ArtColors.red, ArtColors.redDeep))

private fun DrawScope.sparkle(cx: Float, cy: Float, r: Float, color: Color) {
    val p = Path().apply {
        moveTo(cx, cy - r); quadraticTo(cx, cy, cx + r, cy); quadraticTo(cx, cy, cx, cy + r); quadraticTo(cx, cy, cx - r, cy); quadraticTo(cx, cy, cx, cy - r); close()
    }
    drawPath(p, color)
}

private fun wave(t: Float, phase: Float = 0f) = sin((t + phase) * 2 * PI).toFloat()

/* ========================================= situações ========================================= */

fun DrawScope.drawArt(art: Art, t: Float) {
    when (art) {
        Art.BUDGET -> budget(t)
        Art.LIMIT -> limit(t)
        Art.INVOICE -> invoice(t)
        Art.BILL -> bill(t)
        Art.SALARY -> salary(t)
        Art.NO_SPEND -> noSpend(t)
        Art.STREAK -> streak(t)
        Art.WEEK -> week(t)
        Art.MONTH -> month(t)
        Art.PRICE_UP -> priceUp(t)
        Art.DUPLICATE -> duplicate(t)
        Art.OUTLIER -> outlier(t)
        Art.NEGATIVE -> negative(t)
        Art.LAST_INSTALLMENT -> lastInstallment(t)
        Art.HEALTH -> health(t)
        Art.GOAL_NEAR -> goalNear(t)
        Art.DEBT_FREE -> debtFree(t)
        Art.CELEBRATE -> celebrate(t)
        Art.SHOPPING -> shopping(t)
    }
}

/** Medidor do orçamento: o arco vai do verde ao vermelho, o ponteiro dourado balança perto dos 80%. */
private fun DrawScope.budget(t: Float) {
    val c = Offset(50f, 60f)
    val r = 30f
    val box = Rect(c.x - r, c.y - r, c.x + r, c.y + r)
    translate(0f, 2.6f) { drawArc(ArtColors.ink.copy(alpha = 0.22f), 180f, 180f, false, box.topLeft, box.size, style = Stroke(11f, cap = StrokeCap.Round)) }
    drawArc(Color.White.copy(alpha = 0.95f), 180f, 180f, false, box.topLeft, box.size, style = Stroke(11f, cap = StrokeCap.Round))
    drawArc(ArtColors.green, 180f, 95f, false, box.topLeft, box.size, style = Stroke(6f, cap = StrokeCap.Round))
    drawArc(ArtColors.amber, 280f, 42f, false, box.topLeft, box.size, style = Stroke(6f, cap = StrokeCap.Round))
    drawArc(ArtColors.red, 326f, 31f, false, box.topLeft, box.size, style = Stroke(6f, cap = StrokeCap.Round))
    // Ponteiro (balança de leve).
    val ang = (180f + 144f + 6f * wave(t)) * (PI / 180f).toFloat()
    val tip = Offset(c.x + cos(ang) * (r - 3f), c.y + sin(ang) * (r - 3f))
    line(c + Offset(0f, 2.4f), tip + Offset(0f, 2.4f), ArtColors.ink.copy(alpha = 0.25f), 5f)
    line(c, tip, ArtColors.gold, 5f)
    drawCircle(goldIn(c.y - 7f, c.y + 7f), 7f, c)
    drawCircle(Color.White, 2.6f, c)
}

/** Limite estourado: um cubo de gelo trincado e o selo vermelho de atenção. */
private fun DrawScope.limit(t: Float) {
    val cube = rrect(22f, 30f, 46f, 46f, 9f)
    solid(cube, iceIn(30f, 76f))
    drawPath(rrect(27f, 34f, 18f, 7f, 3.5f), Color.White)
    val crack = Path().apply { moveTo(46f, 30f); lineTo(41f, 45f); lineTo(51f, 52f); lineTo(43f, 64f); lineTo(48f, 76f) }
    drawPath(crack, ArtColors.deep, style = Stroke(2.6f, join = StrokeJoin.Round, cap = StrokeCap.Round))
    line(Offset(51f, 52f), Offset(60f, 56f), ArtColors.deep, 2f)
    // Lascas que tremem.
    val j = 1.2f * wave(t)
    drawPath(Path().apply { moveTo(64f + j, 26f); lineTo(70f + j, 22f); lineTo(69f + j, 30f); close() }, ArtColors.ice)
    badge(70f, 68f, 13f, redBrush) {
        line(Offset(70f, 61.5f), Offset(70f, 69.5f), Color.White, 3.6f)
        drawCircle(Color.White, 2.1f, Offset(70f, 75f))
    }
}

/** Fatura: o cartão inclinado com o chip dourado e um calendário na frente. */
private fun DrawScope.invoice(t: Float) {
    rotate(-12f, Offset(42f, 46f)) {
        val card = rrect(16f, 30f, 52f, 34f, 6f)
        solid(card, iceIn(30f, 64f))
        drawRect(ArtColors.deep.copy(alpha = 0.85f), Offset(16f, 37f), Size(52f, 6f))
        drawPath(rrect(22f, 48f, 11f, 8f, 2f), goldIn(48f, 56f))
        line(Offset(38f, 54f), Offset(58f, 54f), ArtColors.iceMid, 2.4f)
    }
    val cal = rrect(50f, 46f, 32f, 32f, 6f)
    solid(cal, iceIn(46f, 78f))
    drawPath(rrect(50f, 46f, 32f, 10f, 6f), redBrush)
    drawRect(ArtColors.redDeep, Offset(50f, 52f), Size(32f, 4f))
    line(Offset(58f, 42f), Offset(58f, 49f), ArtColors.ink, 3f)
    line(Offset(74f, 42f), Offset(74f, 49f), ArtColors.ink, 3f)
    val pulse = 1f + 0.06f * wave(t)
    drawCircle(goldIn(60f, 74f), 6.5f * pulse, Offset(66f, 67f))
}

/** Conta: o recibo com o sininho dourado. */
private fun DrawScope.bill(t: Float) {
    val r = Path().apply {
        moveTo(26f, 22f); lineTo(64f, 22f); lineTo(64f, 76f)
        var x = 64f
        var up = true
        while (x > 26f) { x -= 4.75f; lineTo(x.coerceAtLeast(26f), if (up) 72f else 76f); up = !up }
        close()
    }
    solid(r, iceIn(22f, 76f))
    listOf(32f, 40f, 48f).forEachIndexed { i, y -> line(Offset(32f, y), Offset(if (i == 2) 46f else 58f, y), ArtColors.iceMid, 3f) }
    line(Offset(32f, 60f), Offset(58f, 60f), ArtColors.deep, 3.4f)
    // Sino que balança.
    rotate(10f * wave(t), Offset(68f, 48f)) {
        val bell = Path().apply {
            moveTo(56f, 70f); quadraticTo(58f, 66f, 58f, 58f); cubicTo(58f, 50f, 63f, 47f, 68f, 47f)
            cubicTo(73f, 47f, 78f, 50f, 78f, 58f); quadraticTo(78f, 66f, 80f, 70f); close()
        }
        solid(bell, goldIn(47f, 70f))
        drawCircle(ArtColors.goldDeep, 3.4f, Offset(68f, 73f))
        drawCircle(ArtColors.goldLight, 2.2f, Offset(68f, 45f))
    }
}

/** Salário: moedas douradas caindo no bolso da carteira. */
private fun DrawScope.salary(t: Float) {
    val wallet = rrect(20f, 46f, 60f, 34f, 8f)
    solid(wallet, iceIn(46f, 80f))
    drawPath(rrect(56f, 56f, 24f, 14f, 5f), Brush.verticalGradient(listOf(ArtColors.ice, ArtColors.iceMid)))
    drawCircle(ArtColors.gold, 3f, Offset(64f, 63f))
    line(Offset(26f, 52f), Offset(50f, 52f), ArtColors.iceMid, 2.4f)
    // Cada moeda cai e some ao entrar na carteira; a figurinha repete sem pulo (aparece de novo devagar).
    fun coin(cx: Float, cy: Float, r: Float, a: Float) {
        drawCircle(ArtColors.ink.copy(alpha = 0.22f * a), r, Offset(cx, cy + 2f))
        drawCircle(goldIn(cy - r, cy + r), r, Offset(cx, cy), alpha = a)
        drawCircle(ArtColors.goldLight.copy(alpha = a), r * 0.62f, Offset(cx, cy), style = Stroke(1.8f))
    }
    fun alphaOf(f: Float) = (minOf(f / 0.15f, (1f - f) / 0.25f)).coerceIn(0f, 1f)
    val f1 = t % 1f
    val f2 = (t + 0.5f) % 1f
    coin(36f, 26f + 18f * f1, 9f, if (t == 0f) 1f else alphaOf(f1))
    coin(54f, 20f + 22f * f2, 7.5f, alphaOf(f2))
    sparkle(70f, 26f, 5f + 1.5f * wave(t), Color.White)
}

/** Dia sem gastos: a carteira fechada, presa pela alça dourada, e o selo verde. */
private fun DrawScope.noSpend(t: Float) {
    val body = rrect(16f, 32f, 60f, 44f, 10f)
    solid(body, iceIn(32f, 76f))
    // A aba de cima, um tom mais fundo, e a alça dourada que fecha a carteira.
    drawPath(rrect(16f, 32f, 60f, 20f, 10f), Brush.verticalGradient(listOf(ArtColors.ice, ArtColors.iceMid), 32f, 52f))
    line(Offset(22f, 52f), Offset(70f, 52f), ArtColors.iceMid, 1.6f)
    val strap = rrect(56f, 40f, 26f, 16f, 8f)
    solid(strap, goldIn(40f, 56f), 1.8f)
    drawCircle(ArtColors.goldDeep, 3.4f, Offset(73f, 48f))
    drawCircle(ArtColors.goldLight, 1.4f, Offset(72f, 47f))
    line(Offset(24f, 64f), Offset(44f, 64f), ArtColors.iceMid, 3f)
    val s = 1f + 0.05f * wave(t)
    badge(70f, 72f, 12f * s, greenBrush) { check(70f, 72f, 5.6f, Color.White, 3.2f) }
}

/** Sequência: a chama de cristal (dourada por fora, gelo no meio) que tremula. */
private fun DrawScope.streak(t: Float) {
    val sx = 1f + 0.035f * wave(t)
    val sy = 1f + 0.05f * wave(t, 0.25f)
    withTransform({ scale(sx, sy, Offset(50f, 80f)) }) {
        val outer = Path().apply {
            moveTo(50f, 16f); cubicTo(58f, 30f, 74f, 38f, 72f, 58f); cubicTo(70f, 74f, 60f, 82f, 50f, 82f)
            cubicTo(38f, 82f, 28f, 74f, 28f, 60f); cubicTo(28f, 50f, 34f, 44f, 38f, 38f); cubicTo(40f, 46f, 44f, 48f, 46f, 48f)
            cubicTo(44f, 36f, 46f, 26f, 50f, 16f); close()
        }
        solid(outer, goldIn(16f, 82f))
        val inner = Path().apply {
            moveTo(50f, 44f); cubicTo(55f, 52f, 62f, 58f, 61f, 67f); cubicTo(60f, 75f, 55f, 78f, 50f, 78f)
            cubicTo(44f, 78f, 39f, 74f, 39f, 67f); cubicTo(39f, 58f, 46f, 54f, 50f, 44f); close()
        }
        drawPath(inner, Brush.verticalGradient(listOf(Color.White, ArtColors.ice, ArtColors.iceMid), 44f, 78f))
    }
    sparkle(73f, 28f, 4.5f + 1.5f * wave(t, 0.5f), Color.White)
}

/** Resumo da semana: barras de gelo que crescem, a mais alta com a ponta dourada. */
private fun DrawScope.week(t: Float) {
    val base = 78f
    val hs = listOf(22f, 34f, 18f, 44f, 30f)
    hs.forEachIndexed { i, h ->
        val k = 1f + 0.08f * wave(t, i * 0.15f)
        val hh = h * k
        val x = 20f + i * 12.5f
        val bar = rrect(x, base - hh, 9f, hh, 4.5f)
        solid(bar, if (i == 3) goldIn(base - hh, base) else iceIn(base - hh, base))
    }
    line(Offset(16f, 80f), Offset(84f, 80f), Color.White.copy(alpha = 0.8f), 2.4f)
    drawLine(Color.White.copy(alpha = 0.7f), Offset(16f, 50f), Offset(84f, 50f), 1.6f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 3.5f)))
}

/** Fechamento do mês: o calendário com a estrela dourada. */
private fun DrawScope.month(t: Float) {
    val cal = rrect(20f, 26f, 60f, 54f, 9f)
    solid(cal, iceIn(26f, 80f))
    drawPath(rrect(20f, 26f, 60f, 16f, 9f), Brush.verticalGradient(listOf(ArtColors.tileTop, ArtColors.deep), 26f, 42f))
    drawRect(ArtColors.deep, Offset(20f, 36f), Size(60f, 6f))
    line(Offset(34f, 20f), Offset(34f, 31f), ArtColors.ink, 3.4f)
    line(Offset(66f, 20f), Offset(66f, 31f), ArtColors.ink, 3.4f)
    for (r in 0..1) for (col in 0..4) if (!(r == 1 && col >= 2)) drawCircle(ArtColors.iceMid, 2.2f, Offset(29f + col * 10.5f, 50f + r * 9f))
    val s = 1f + 0.08f * wave(t)
    star(62f, 66f, 11f * s, goldIn(55f, 77f))
}

private fun DrawScope.star(cx: Float, cy: Float, r: Float, brush: Brush) {
    val p = Path()
    for (i in 0 until 10) {
        val a = (-90 + i * 36) * PI / 180
        val rr = if (i % 2 == 0) r else r * 0.46f
        val x = cx + (cos(a) * rr).toFloat()
        val y = cy + (sin(a) * rr).toFloat()
        if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
    }
    p.close()
    shadow(p, 2f)
    drawPath(p, brush)
    drawPath(p, ArtColors.goldDeep, style = Stroke(1.2f, join = StrokeJoin.Round))
}

/** Assinatura mais cara: a etiqueta de preço e a seta dourada para cima. */
private fun DrawScope.priceUp(t: Float) {
    val tag = Path().apply { moveTo(20f, 46f); lineTo(38f, 28f); lineTo(64f, 28f); lineTo(64f, 64f); lineTo(38f, 64f); close() }
    rotate(-18f, Offset(44f, 46f)) {
        solid(tag, iceIn(28f, 64f))
        drawCircle(ArtColors.deep, 4f, Offset(33f, 46f))
        line(Offset(44f, 40f), Offset(58f, 40f), ArtColors.iceMid, 3f)
        line(Offset(44f, 50f), Offset(54f, 50f), ArtColors.iceMid, 3f)
    }
    val dy = -2.5f * wave(t)
    val arrow = Path().apply { moveTo(70f, 34f + dy); lineTo(84f, 52f + dy); lineTo(76f, 52f + dy); lineTo(76f, 74f + dy); lineTo(64f, 74f + dy); lineTo(64f, 52f + dy); lineTo(56f, 52f + dy); close() }
    solid(arrow, goldIn(34f, 74f))
}

/** Cobrança repetida: dois recibos iguais, um sobre o outro, e o selo âmbar. */
private fun DrawScope.duplicate(t: Float) {
    fun receipt(x: Float, y: Float) {
        val p = rrect(x, y, 34f, 44f, 5f)
        solid(p, iceIn(y, y + 44f))
        line(Offset(x + 6f, y + 10f), Offset(x + 28f, y + 10f), ArtColors.iceMid, 3f)
        line(Offset(x + 6f, y + 18f), Offset(x + 22f, y + 18f), ArtColors.iceMid, 3f)
        line(Offset(x + 6f, y + 32f), Offset(x + 28f, y + 32f), ArtColors.deep, 3.2f)
    }
    val d = 2f * wave(t)
    receipt(20f, 22f)
    receipt(36f + d, 34f)
    badge(72f, 72f, 12f, Brush.verticalGradient(listOf(ArtColors.amber, ArtColors.goldDeep))) {
        line(Offset(66f, 69f), Offset(78f, 69f), Color.White, 3f)
        line(Offset(66f, 75f), Offset(78f, 75f), Color.White, 3f)
    }
}

/** Fora do padrão: a linha com um pico e a lupa dourada em cima dele. */
private fun DrawScope.outlier(t: Float) {
    val p = Path().apply { moveTo(16f, 66f); lineTo(30f, 60f); lineTo(40f, 64f); lineTo(48f, 30f); lineTo(56f, 62f); lineTo(70f, 56f); lineTo(84f, 60f) }
    translate(0f, 2.4f) { drawPath(p, ArtColors.ink.copy(alpha = 0.22f), style = Stroke(5f, cap = StrokeCap.Round, join = StrokeJoin.Round)) }
    drawPath(p, Color.White, style = Stroke(5f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    drawCircle(ArtColors.red, 4.5f, Offset(48f, 30f))
    val o = Offset(56f + 2f * wave(t), 42f)
    line(o + Offset(10f, 10f), o + Offset(22f, 22f), ArtColors.goldDeep, 7f)
    drawCircle(ArtColors.ink.copy(alpha = 0.22f), 15f, o + Offset(0f, 2f))
    drawCircle(Color.White.copy(alpha = 0.28f), 13f, o)
    drawCircle(goldIn(o.y - 15f, o.y + 15f), 14f, o, style = Stroke(5f))
    drawArc(Color.White.copy(alpha = 0.8f), 200f, 60f, false, o - Offset(9f, 9f), Size(18f, 18f), style = Stroke(2.2f, cap = StrokeCap.Round))
}

/** Saldo negativo: a linha que desce e passa do zero para a área vermelha. */
private fun DrawScope.negative(t: Float) {
    drawRect(Brush.verticalGradient(listOf(ArtColors.red.copy(alpha = 0.55f), ArtColors.redDeep.copy(alpha = 0.85f)), 58f, 96f), Offset(4f, 58f), Size(92f, 40f))
    drawLine(Color.White.copy(alpha = 0.9f), Offset(12f, 58f), Offset(88f, 58f), 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 4f)))
    val p = Path().apply { moveTo(16f, 30f); lineTo(32f, 36f); lineTo(46f, 34f); lineTo(60f, 50f); lineTo(74f, 70f) }
    translate(0f, 2.4f) { drawPath(p, ArtColors.ink.copy(alpha = 0.22f), style = Stroke(5f, cap = StrokeCap.Round, join = StrokeJoin.Round)) }
    drawPath(p, Color.White, style = Stroke(5f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    val a = Path().apply { moveTo(80f, 80f); lineTo(66f, 77f); lineTo(78f, 66f); close() }
    drawPath(a, Color.White)
    drawCircle(Color.White, 5.5f + 1f * wave(t), Offset(60f, 50f))
    drawCircle(ArtColors.redDeep, 3f, Offset(60f, 50f))
}

/** Última parcela: os blocos das parcelas, o último dourado e marcado. */
private fun DrawScope.lastInstallment(t: Float) {
    for (i in 0..2) solid(rrect(14f + i * 17f, 42f, 13f, 20f, 4f), iceIn(42f, 62f))
    val s = 1f + 0.06f * wave(t)
    withTransform({ scale(s, s, Offset(72f, 52f)) }) {
        solid(rrect(63f, 40f, 19f, 24f, 5f), goldIn(40f, 64f))
        check(72.5f, 52f, 5.5f, Color.White, 3.2f)
    }
    line(Offset(16f, 72f), Offset(80f, 72f), Color.White.copy(alpha = 0.6f), 2.2f)
}

/** Saúde financeira: o anel que enche e a seta para cima. */
private fun DrawScope.health(t: Float) {
    val c = Offset(50f, 52f)
    translate(0f, 2.4f) { drawCircle(ArtColors.ink.copy(alpha = 0.2f), 28f, c, style = Stroke(10f)) }
    drawCircle(Color.White.copy(alpha = 0.35f), 28f, c, style = Stroke(10f))
    val sweep = 250f + 20f * wave(t)
    drawArc(goldIn(24f, 80f), -90f, sweep, false, c - Offset(28f, 28f), Size(56f, 56f), style = Stroke(10f, cap = StrokeCap.Round))
    val up = Path().apply { moveTo(50f, 36f); lineTo(61f, 50f); lineTo(54.5f, 50f); lineTo(54.5f, 66f); lineTo(45.5f, 66f); lineTo(45.5f, 50f); lineTo(39f, 50f); close() }
    solid(up, iceIn(36f, 66f), 2f)
}

/** Meta quase lá: o alvo com a flecha dourada quase no centro. */
private fun DrawScope.goalNear(t: Float) {
    val c = Offset(46f, 56f)
    drawCircle(ArtColors.ink.copy(alpha = 0.22f), 28f, c + Offset(0f, 2.6f))
    drawCircle(Color.White, 28f, c)
    drawCircle(ArtColors.iceMid, 20f, c)
    drawCircle(Color.White, 12f, c)
    drawCircle(Brush.verticalGradient(listOf(ArtColors.red, ArtColors.redDeep)), 5.5f, c)
    val k = 3f * wave(t)
    val tip = Offset(52f + k * 0.4f, 50f + k * 0.4f)
    line(tip + Offset(2f, 2f), Offset(82f, 22f) + Offset(2f, 2f), ArtColors.ink.copy(alpha = 0.25f), 4f)
    line(tip, Offset(82f, 22f), ArtColors.goldDeep, 4f)
    val fl = Path().apply { moveTo(82f, 22f); lineTo(90f, 16f); lineTo(88f, 26f); close(); moveTo(82f, 22f); lineTo(76f, 14f); lineTo(86f, 14f); close() }
    drawPath(fl, goldIn(12f, 28f))
}

/** Dívida quitada: o cadeado dourado aberto e brilhos. */
private fun DrawScope.debtFree(t: Float) {
    val lift = 2f * wave(t)
    val shackle = Path().apply { moveTo(38f, 46f); lineTo(38f, 32f - lift); cubicTo(38f, 18f - lift, 62f, 18f - lift, 62f, 30f - lift) }
    translate(0f, 2.4f) { drawPath(shackle, ArtColors.ink.copy(alpha = 0.22f), style = Stroke(7f, cap = StrokeCap.Round)) }
    drawPath(shackle, Brush.verticalGradient(listOf(Color.White, ArtColors.iceMid)), style = Stroke(7f, cap = StrokeCap.Round))
    val body = rrect(26f, 44f, 48f, 36f, 9f)
    solid(body, goldIn(44f, 80f))
    drawCircle(ArtColors.goldDeep, 5f, Offset(50f, 59f))
    drawRoundRect(ArtColors.goldDeep, Offset(48f, 60f), Size(4f, 10f), CornerRadius(2f))
    sparkle(76f, 34f, 6f + 1.5f * wave(t), Color.White)
    sparkle(22f, 32f, 4f + 1f * wave(t, 0.5f), ArtColors.goldLight)
}

/** Comemoração: estouro de cristais de gelo e dourados em volta de uma estrela. */
private fun DrawScope.celebrate(t: Float) {
    val c = Offset(50f, 52f)
    for (i in 0 until 10) {
        val a = (i * 36 + 18 * t) * PI / 180
        val d = 26f + 6f * wave(t, i * 0.1f)
        val p = c + Offset((cos(a) * d).toFloat(), (sin(a) * d).toFloat())
        val col = if (i % 3 == 0) ArtColors.goldLight else if (i % 3 == 1) Color.White else ArtColors.ice
        rotate((i * 36 + 90).toFloat(), p) {
            drawPath(Path().apply { moveTo(p.x, p.y - 5f); lineTo(p.x + 2.6f, p.y); lineTo(p.x, p.y + 5f); lineTo(p.x - 2.6f, p.y); close() }, col)
        }
    }
    star(50f, 52f, 18f * (1f + 0.07f * wave(t)), goldIn(34f, 70f))
}

/** Modo compras: o carrinho de gelo com as compras douradas. */
private fun DrawScope.shopping(t: Float) {
    val bounce = 1.5f * wave(t)
    val items = listOf(Triple(34f, 38f + bounce, 9f), Triple(48f, 34f - bounce, 11f), Triple(62f, 40f + bounce, 8f))
    items.forEach { (x, y, r) -> drawCircle(ArtColors.ink.copy(alpha = 0.2f), r, Offset(x, y + 2f)); drawCircle(goldIn(y - r, y + r), r, Offset(x, y)) }
    val basket = Path().apply { moveTo(20f, 42f); lineTo(76f, 42f); lineTo(70f, 66f); lineTo(28f, 66f); close() }
    solid(basket, iceIn(42f, 66f))
    for (i in 1..3) line(Offset(28f + i * 11f, 46f), Offset(30f + i * 10f, 62f), ArtColors.iceMid, 2.4f)
    line(Offset(12f, 32f), Offset(20f, 32f), Color.White, 4f)
    line(Offset(20f, 32f), Offset(26f, 66f), Color.White, 4f)
    drawCircle(Color.White, 5f, Offset(34f, 76f))
    drawCircle(Color.White, 5f, Offset(64f, 76f))
    drawCircle(ArtColors.deep, 2f, Offset(34f, 76f))
    drawCircle(ArtColors.deep, 2f, Offset(64f, 76f))
}

/* ============================================ metas ============================================ */

fun DrawScope.drawGoal(art: GoalArt) {
    when (art) {
        GoalArt.TARGET -> goalNear(0f)
        GoalArt.HOUSE -> house()
        GoalArt.PLANE -> plane()
        GoalArt.CAR -> car()
        GoalArt.GRADUATION -> graduation()
        GoalArt.RING -> ring()
        GoalArt.SHIELD -> shield()
        GoalArt.PHONE -> phone()
        GoalArt.LAPTOP -> laptop()
        GoalArt.GAME -> game()
        GoalArt.BEACH -> beach()
        GoalArt.BABY -> baby()
        GoalArt.PET -> pet()
        GoalArt.GUITAR -> guitar()
        GoalArt.MOTO -> moto()
        GoalArt.SAVINGS -> piggy()
        GoalArt.GIFT -> gift()
        GoalArt.CHURCH -> church()
        GoalArt.GYM -> gym()
        GoalArt.BOOKS -> books()
    }
}

private fun DrawScope.house() {
    val roof = Path().apply { moveTo(50f, 18f); lineTo(84f, 46f); lineTo(16f, 46f); close() }
    val body = rrect(24f, 42f, 52f, 38f, 4f)
    solid(body, iceIn(42f, 80f))
    solid(roof, goldIn(18f, 46f))
    drawPath(rrect(44f, 56f, 13f, 24f, 3f), Brush.verticalGradient(listOf(ArtColors.tileTop, ArtColors.deep)))
    drawCircle(ArtColors.goldLight, 1.6f, Offset(54f, 69f))
    drawPath(rrect(30f, 52f, 9f, 9f, 2f), ArtColors.iceMid)
    drawPath(rrect(62f, 52f, 9f, 9f, 2f), ArtColors.iceMid)
}

private fun DrawScope.plane() {
    rotate(-30f, Offset(50f, 50f)) {
        val wings = Path().apply { moveTo(46f, 44f); lineTo(20f, 58f); lineTo(20f, 64f); lineTo(46f, 56f); lineTo(54f, 56f); lineTo(80f, 64f); lineTo(80f, 58f); lineTo(54f, 44f); close() }
        solid(wings, iceIn(44f, 64f))
        val body = rrect(44f, 16f, 12f, 68f, 6f)
        solid(body, Brush.verticalGradient(listOf(Color.White, ArtColors.ice)))
        val tail = Path().apply { moveTo(46f, 76f); lineTo(36f, 84f); lineTo(36f, 87f); lineTo(50f, 82f); lineTo(64f, 87f); lineTo(64f, 84f); lineTo(54f, 76f); close() }
        drawPath(tail, goldIn(76f, 87f))
        drawPath(rrect(46.5f, 22f, 7f, 8f, 3f), ArtColors.deep)
    }
}

private fun DrawScope.car() {
    val top = Path().apply { moveTo(32f, 50f); lineTo(38f, 34f); lineTo(64f, 34f); lineTo(72f, 50f); close() }
    solid(top, iceIn(34f, 50f))
    drawPath(Path().apply { moveTo(37f, 49f); lineTo(41f, 38f); lineTo(51f, 38f); lineTo(51f, 49f); close() }, ArtColors.deep)
    drawPath(Path().apply { moveTo(54f, 49f); lineTo(54f, 38f); lineTo(62f, 38f); lineTo(67f, 49f); close() }, ArtColors.deep)
    val body = rrect(16f, 48f, 68f, 20f, 8f)
    solid(body, goldIn(48f, 68f))
    drawPath(rrect(18f, 54f, 8f, 5f, 2f), Color.White)
    drawPath(rrect(74f, 54f, 8f, 5f, 2f), ArtColors.red)
    listOf(32f, 68f).forEach { x ->
        drawCircle(ArtColors.ink, 8.5f, Offset(x, 69f))
        drawCircle(ArtColors.ice, 3.5f, Offset(x, 69f))
    }
}

private fun DrawScope.graduation() {
    val cap = Path().apply { moveTo(50f, 26f); lineTo(86f, 42f); lineTo(50f, 58f); lineTo(14f, 42f); close() }
    val base = Path().apply { moveTo(30f, 50f); lineTo(30f, 66f); cubicTo(40f, 74f, 60f, 74f, 70f, 66f); lineTo(70f, 50f); lineTo(50f, 60f); close() }
    solid(base, Brush.verticalGradient(listOf(ArtColors.ice, ArtColors.iceMid), 50f, 74f))
    solid(cap, iceIn(26f, 58f))
    line(Offset(50f, 42f), Offset(76f, 50f), ArtColors.gold, 2.6f)
    line(Offset(76f, 50f), Offset(76f, 66f), ArtColors.gold, 2.6f)
    drawCircle(goldIn(64f, 74f), 4.5f, Offset(76f, 69f))
    drawCircle(ArtColors.goldDeep, 2.6f, Offset(50f, 42f))
}

private fun DrawScope.ring() {
    translate(0f, 2.6f) { drawCircle(ArtColors.ink.copy(alpha = 0.22f), 20f, Offset(50f, 60f), style = Stroke(8f)) }
    drawCircle(goldIn(40f, 80f), 20f, Offset(50f, 60f), style = Stroke(8f))
    drawArc(ArtColors.goldLight, 200f, 70f, false, Offset(32f, 42f), Size(36f, 36f), style = Stroke(2.4f, cap = StrokeCap.Round))
    val gem = Path().apply { moveTo(40f, 32f); lineTo(46f, 24f); lineTo(54f, 24f); lineTo(60f, 32f); lineTo(50f, 44f); close() }
    solid(gem, Brush.verticalGradient(listOf(Color.White, ArtColors.iceMid), 24f, 44f))
    line(Offset(40f, 32f), Offset(60f, 32f), ArtColors.tileTop, 1.6f)
    sparkle(68f, 24f, 5f, Color.White)
}

private fun DrawScope.shield() {
    val p = Path().apply { moveTo(50f, 16f); lineTo(78f, 26f); cubicTo(78f, 54f, 68f, 72f, 50f, 84f); cubicTo(32f, 72f, 22f, 54f, 22f, 26f); close() }
    solid(p, iceIn(16f, 84f))
    val inner = Path().apply { moveTo(50f, 24f); lineTo(70f, 31f); cubicTo(70f, 53f, 62f, 66f, 50f, 75f); close() }
    drawPath(inner, ArtColors.iceMid.copy(alpha = 0.7f))
    drawCircle(goldIn(38f, 62f), 11f, Offset(50f, 50f))
    check(50f, 50f, 5f, Color.White, 3f)
}

private fun DrawScope.phone() {
    val p = rrect(32f, 16f, 36f, 68f, 8f)
    solid(p, Brush.verticalGradient(listOf(ArtColors.ink, ArtColors.deep), 16f, 84f))
    drawPath(rrect(35f, 22f, 30f, 54f, 4f), Brush.verticalGradient(listOf(ArtColors.tileTop, ArtColors.iceMid), 22f, 76f))
    drawPath(rrect(45f, 19f, 10f, 2.4f, 1.2f), Color.White.copy(alpha = 0.6f))
    drawCircle(goldIn(42f, 58f), 7f, Offset(50f, 48f))
    drawCircle(Color.White, 2.4f, Offset(50f, 48f))
}

private fun DrawScope.laptop() {
    val screen = rrect(24f, 26f, 52f, 36f, 5f)
    solid(screen, Brush.verticalGradient(listOf(ArtColors.ink, ArtColors.deep), 26f, 62f))
    drawPath(rrect(28f, 30f, 44f, 28f, 3f), Brush.verticalGradient(listOf(ArtColors.tileTop, ArtColors.iceMid), 30f, 58f))
    val base = Path().apply { moveTo(16f, 66f); lineTo(84f, 66f); lineTo(80f, 74f); lineTo(20f, 74f); close() }
    solid(base, iceIn(66f, 74f))
    drawPath(rrect(44f, 66f, 12f, 3f, 1.5f), ArtColors.iceMid)
    drawPath(Path().apply { moveTo(42f, 38f); lineTo(42f, 52f); lineTo(54f, 45f); close() }, ArtColors.goldLight)
}

private fun DrawScope.game() {
    val p = Path().apply {
        moveTo(26f, 40f); cubicTo(30f, 34f, 70f, 34f, 74f, 40f); cubicTo(82f, 52f, 86f, 70f, 78f, 72f)
        cubicTo(72f, 74f, 66f, 62f, 60f, 60f); lineTo(40f, 60f); cubicTo(34f, 62f, 28f, 74f, 22f, 72f); cubicTo(14f, 70f, 18f, 52f, 26f, 40f); close()
    }
    solid(p, iceIn(34f, 74f))
    drawRoundRect(ArtColors.deep, Offset(30f, 46f), Size(12f, 4f), CornerRadius(2f))
    drawRoundRect(ArtColors.deep, Offset(34f, 42f), Size(4f, 12f), CornerRadius(2f))
    drawCircle(goldIn(42f, 50f), 3.6f, Offset(64f, 45f))
    drawCircle(ArtColors.red, 3.6f, Offset(70f, 51f))
}

private fun DrawScope.beach() {
    drawRect(Brush.verticalGradient(listOf(ArtColors.goldLight, ArtColors.gold), 68f, 96f), Offset(4f, 68f), Size(92f, 30f))
    drawPath(Path().apply { moveTo(4f, 66f); quadraticTo(30f, 60f, 50f, 66f); quadraticTo(72f, 72f, 96f, 64f); lineTo(96f, 72f); lineTo(4f, 72f); close() }, Color.White.copy(alpha = 0.8f))
    line(Offset(48f, 30f), Offset(58f, 72f), Color.White, 3.6f)
    val um = Path().apply { moveTo(20f, 40f); quadraticTo(40f, 10f, 74f, 22f); quadraticTo(56f, 26f, 20f, 40f); close() }
    solid(um, Brush.horizontalGradient(listOf(ArtColors.red, ArtColors.goldLight, ArtColors.red), 20f, 74f))
    drawCircle(goldIn(10f, 30f), 9f, Offset(78f, 20f))
}

private fun DrawScope.baby() {
    // Mamadeira: o bico de gelo, a tampa dourada e o corpo com as marquinhas de medida.
    val nipple = Path().apply { moveTo(44f, 30f); cubicTo(44f, 20f, 46f, 14f, 50f, 14f); cubicTo(54f, 14f, 56f, 20f, 56f, 30f); close() }
    solid(nipple, Brush.verticalGradient(listOf(ArtColors.goldLight, ArtColors.gold), 14f, 30f), 2f)
    solid(rrect(36f, 28f, 28f, 10f, 4f), goldIn(28f, 38f))
    val body = rrect(34f, 36f, 32f, 48f, 10f)
    solid(body, iceIn(36f, 84f))
    drawPath(rrect(38f, 58f, 24f, 22f, 7f), Brush.verticalGradient(listOf(Color.White, ArtColors.ice), 58f, 80f))
    listOf(46f, 54f, 62f, 70f).forEachIndexed { i, y -> line(Offset(38f, y), Offset(if (i % 2 == 0) 46f else 43f, y), ArtColors.iceMid, 2.2f) }
    drawPath(rrect(56f, 42f, 5f, 34f, 2.5f), Color.White.copy(alpha = 0.75f))
}

private fun DrawScope.pet() {
    val pad = Path().apply { moveTo(50f, 46f); cubicTo(66f, 46f, 72f, 66f, 66f, 72f); cubicTo(60f, 78f, 54f, 72f, 50f, 72f); cubicTo(46f, 72f, 40f, 78f, 34f, 72f); cubicTo(28f, 66f, 34f, 46f, 50f, 46f); close() }
    solid(pad, goldIn(46f, 76f))
    listOf(Offset(28f, 40f) to 7f, Offset(40f, 28f) to 7.5f, Offset(60f, 28f) to 7.5f, Offset(72f, 40f) to 7f).forEach { (o, r) ->
        drawCircle(ArtColors.ink.copy(alpha = 0.22f), r, o + Offset(0f, 2.4f))
        drawCircle(iceIn(o.y - r, o.y + r), r, o)
    }
}

private fun DrawScope.guitar() {
    rotate(35f, Offset(50f, 50f)) {
        drawRoundRect(ArtColors.ink, Offset(47f, 12f), Size(6f, 40f), CornerRadius(2f))
        drawRoundRect(goldIn(8f, 16f), Offset(44f, 8f), Size(12f, 10f), CornerRadius(3f))
        val body = Path().apply {
            moveTo(50f, 46f); cubicTo(62f, 46f, 64f, 56f, 60f, 60f); cubicTo(68f, 62f, 70f, 74f, 64f, 82f)
            cubicTo(58f, 90f, 42f, 90f, 36f, 82f); cubicTo(30f, 74f, 32f, 62f, 40f, 60f); cubicTo(36f, 56f, 38f, 46f, 50f, 46f); close()
        }
        solid(body, goldIn(46f, 90f))
        drawCircle(ArtColors.ink, 6f, Offset(50f, 66f))
        drawRoundRect(ArtColors.ink, Offset(43f, 76f), Size(14f, 3f), CornerRadius(1.5f))
        line(Offset(50f, 14f), Offset(50f, 77f), Color.White.copy(alpha = 0.8f), 1.2f)
    }
}

private fun DrawScope.moto() {
    listOf(26f, 74f).forEach { x ->
        drawCircle(ArtColors.ink.copy(alpha = 0.25f), 13f, Offset(x, 67f))
        drawCircle(ArtColors.ink, 12f, Offset(x, 65f))
        drawCircle(ArtColors.ice, 4.5f, Offset(x, 65f))
    }
    // Garfo da frente, guidão e farol.
    line(Offset(74f, 65f), Offset(66f, 40f), Color.White, 4f)
    line(Offset(62f, 36f), Offset(72f, 34f), ArtColors.ink, 4f)
    drawCircle(goldIn(40f, 50f), 4.5f, Offset(72f, 44f))
    // Corpo: tanque, banco e rabeta numa peça só, dourada.
    val body = Path().apply {
        moveTo(22f, 52f); cubicTo(24f, 44f, 32f, 42f, 40f, 44f); lineTo(46f, 40f); cubicTo(52f, 36f, 64f, 38f, 66f, 46f)
        lineTo(62f, 56f); lineTo(36f, 58f); cubicTo(30f, 58f, 24f, 58f, 22f, 52f); close()
    }
    solid(body, goldIn(36f, 58f))
    drawPath(rrect(24f, 42f, 18f, 5f, 2.5f), ArtColors.ink)
    drawPath(Path().apply { moveTo(40f, 58f); lineTo(58f, 58f); lineTo(54f, 66f); lineTo(44f, 66f); close() }, Brush.verticalGradient(listOf(ArtColors.iceMid, ArtColors.deep), 58f, 66f))
}

private fun DrawScope.piggy() {
    val body = Path().apply { addOval(Rect(18f, 34f, 78f, 76f)) }
    solid(body, goldIn(34f, 76f))
    drawPath(Path().apply { moveTo(30f, 38f); lineTo(34f, 26f); lineTo(42f, 36f); close() }, ArtColors.goldDeep)
    drawPath(rrect(72f, 48f, 12f, 14f, 6f), ArtColors.gold)
    drawCircle(ArtColors.goldDeep, 1.6f, Offset(78f, 53f))
    drawCircle(ArtColors.goldDeep, 1.6f, Offset(78f, 58f))
    drawCircle(ArtColors.ink, 2.6f, Offset(62f, 48f))
    drawRoundRect(ArtColors.goldDeep, Offset(40f, 36f), Size(16f, 3.4f), CornerRadius(1.7f))
    listOf(30f, 62f).forEach { x -> drawRoundRect(ArtColors.goldDeep, Offset(x, 70f), Size(8f, 10f), CornerRadius(3f)) }
    // A moeda entrando pelo buraco das costas.
    drawCircle(ArtColors.ink.copy(alpha = 0.22f), 8f, Offset(48f, 24f))
    drawCircle(goldIn(14f, 30f), 8f, Offset(48f, 22f))
    drawCircle(ArtColors.goldLight, 4.6f, Offset(48f, 22f), style = Stroke(1.6f))
}

private fun DrawScope.gift() {
    val box = rrect(22f, 46f, 56f, 34f, 5f)
    solid(box, iceIn(46f, 80f))
    val lid = rrect(18f, 36f, 64f, 14f, 5f)
    solid(lid, Brush.verticalGradient(listOf(Color.White, ArtColors.ice), 36f, 50f))
    drawRect(goldIn(36f, 80f), Offset(45f, 36f), Size(10f, 44f))
    val bow = Path().apply {
        moveTo(50f, 36f); cubicTo(40f, 20f, 26f, 28f, 36f, 36f); close()
        moveTo(50f, 36f); cubicTo(60f, 20f, 74f, 28f, 64f, 36f); close()
    }
    drawPath(bow, goldIn(22f, 36f))
}

private fun DrawScope.church() {
    val tower = Path().apply { moveTo(50f, 16f); lineTo(62f, 34f); lineTo(62f, 80f); lineTo(38f, 80f); lineTo(38f, 34f); close() }
    val hall = rrect(20f, 50f, 60f, 30f, 3f)
    solid(hall, iceIn(50f, 80f))
    solid(tower, iceIn(16f, 80f))
    drawRect(goldIn(6f, 18f), Offset(48.5f, 4f), Size(3f, 14f))
    drawRect(goldIn(6f, 18f), Offset(44f, 8f), Size(12f, 3f))
    drawPath(Path().apply { moveTo(45f, 80f); lineTo(45f, 64f); cubicTo(45f, 57f, 55f, 57f, 55f, 64f); lineTo(55f, 80f); close() }, Brush.verticalGradient(listOf(ArtColors.tileTop, ArtColors.deep)))
    drawCircle(goldIn(38f, 48f), 5f, Offset(50f, 43f))
}

private fun DrawScope.gym() {
    line(Offset(24f, 50f), Offset(76f, 50f), Color.White, 6f)
    listOf(18f, 70f).forEach { x ->
        solid(rrect(x, 30f, 12f, 40f, 4f), goldIn(30f, 70f))
        solid(rrect(x + (if (x < 50f) 10f else -8f), 36f, 10f, 28f, 3.5f), iceIn(36f, 64f))
    }
}

private fun DrawScope.books() {
    solid(rrect(20f, 64f, 60f, 14f, 3f), goldIn(64f, 78f))
    solid(rrect(24f, 50f, 54f, 14f, 3f), iceIn(50f, 64f))
    solid(rrect(18f, 36f, 58f, 14f, 3f), Brush.verticalGradient(listOf(ArtColors.tileTop, ArtColors.deep), 36f, 50f))
    line(Offset(28f, 71f), Offset(72f, 71f), ArtColors.goldLight, 2f)
    line(Offset(30f, 57f), Offset(70f, 57f), ArtColors.iceMid, 2f)
    line(Offset(26f, 43f), Offset(68f, 43f), ArtColors.ice.copy(alpha = 0.6f), 2f)
    val mark = Path().apply { moveTo(62f, 36f); lineTo(62f, 28f); lineTo(68f, 28f); lineTo(68f, 36f); lineTo(65f, 33f); close() }
    drawPath(mark, ArtColors.red)
}

@Suppress("unused") private val keepFill = Fill
