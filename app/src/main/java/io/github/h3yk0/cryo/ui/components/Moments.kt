// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.h3yk0.cryo.R
import io.github.h3yk0.cryo.domain.GoalArt
import io.github.h3yk0.cryo.domain.Money
import io.github.h3yk0.cryo.domain.Res
import io.github.h3yk0.cryo.notify.Sound
import io.github.h3yk0.cryo.notify.Sounds
import io.github.h3yk0.cryo.ui.LocalNav
import io.github.h3yk0.cryo.ui.Routes
import io.github.h3yk0.cryo.ui.theme.ArtColors
import io.github.h3yk0.cryo.ui.theme.CryoTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.YearMonth
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/*
 * RF-MOM: os momentos animados do app (os "popups" do pedido do Hanry). Só aparecem como resposta a algo que a pessoa fez ou
 * na abertura do app (fechamento do mês), um por vez; tudo finito (DS-MOV-10) e direto no quadro final com "Remover
 * animações" (ACS-08); fecham com um toque ou com o Voltar; o leitor de tela lê o texto.
 */

sealed interface Moment {
    /** RF-MOM-02: a categoria chegou a 80% ou passou do limite ao salvar. [move] = de onde dá para cobrir. */
    data class Budget(
        val categoryId: Long, val name: String, val icon: String, val spent: Long, val limit: Long, val over: Boolean,
        val daysLeft: Int, val ym: Int, val move: BudgetMove?,
    ) : Moment

    data class BudgetMove(val fromId: Long, val fromName: String, val free: Long, val amount: Long)

    /** RF-MOM-01: a meta foi completada. */
    data class GoalDone(val name: String, val art: GoalArt, val color: Int, val saved: Long, val months: Int) : Moment

    /** RF-MOM-03: marcos da sequência de dias anotando. */
    data class Streak(val days: Int) : Moment

    /** RF-MOM-04: como o mês passado fechou (na primeira abertura do dia 1 ao 3). */
    data class MonthClosed(val ym: YearMonth, val result: Long) : Moment

    companion object {
        val STREAK_MARKS = setOf(3, 7, 14, 30, 60, 100, 200, 365)
    }
}

/** A fila dos momentos: um por vez (RF-MOM-05). */
class MomentBus {
    private val state = MutableStateFlow<Moment?>(null)
    val current: StateFlow<Moment?> = state

    fun show(m: Moment) { if (state.value == null) state.value = m }
    fun dismiss() { state.value = null }
}

/** Mostra o momento da vez por cima do app. */
@Composable
fun MomentHost(bus: MomentBus) {
    val m by bus.current.collectAsState()
    val ctx = LocalContext.current
    val moment = m ?: return
    LaunchedEffect(moment) {
        Sounds.play(
            ctx,
            when (moment) {
                is Moment.GoalDone -> Sound.ACHIEVEMENT
                is Moment.Budget -> if (moment.over) Sound.LIMIT else Sound.ATTENTION
                is Moment.Streak -> Sound.STREAK
                is Moment.MonthClosed -> if (moment.result >= 0) Sound.ACHIEVEMENT else Sound.ATTENTION
            },
        )
    }
    when (moment) {
        is Moment.GoalDone -> GoalMoment(moment, bus::dismiss)
        is Moment.Budget -> BudgetMoment(moment, bus::dismiss)
        is Moment.Streak -> CardMoment(Art.STREAK, Res.p(R.plurals.moment_streak_title, moment.days, moment.days), Res.s(R.string.moment_streak_text), bus::dismiss)
        is Moment.MonthClosed -> {
            val name = io.github.h3yk0.cryo.domain.Dates.monthName(moment.ym)
            val plus = moment.result >= 0
            CardMoment(
                if (plus) Art.MONTH else Art.NEGATIVE,
                Res.s(if (plus) R.string.alert_month_title_plus else R.string.alert_month_title_minus, name),
                Res.s(if (plus) R.string.alert_month_a_plus else R.string.alert_month_a_minus, Money.format(kotlin.math.abs(moment.result))),
                bus::dismiss,
            )
        }
    }
}

/** Um valor de 0 a 1 que anda uma vez (ou já começa em 1 com "Remover animações"). */
@Composable
private fun progress(durationMs: Int, delayMs: Int = 0): Float {
    val on = animationsOn()
    val a = remember { Animatable(if (on) 0f else 1f) }
    LaunchedEffect(Unit) { if (a.value < 1f) a.animateTo(1f, tween(durationMs, delayMs)) }
    return a.value
}

private fun easeOut(x: Float) = 1 - (1 - x) * (1 - x) * (1 - x)
private fun springy(x: Float): Float = if (x <= 0f) 0f else if (x >= 1f) 1f else (1 - exp(-6.0 * x) * cos(9.0 * x)).toFloat()

private class Crystal(val angle: Float, val speed: Float, val spin: Float, val size: Float, val color: Color, val diamond: Boolean)

/** RF-MOM-01: meta concluída, em tela cheia, com cristais de gelo e dourados. */
@Composable
private fun GoalMoment(m: Moment.GoalDone, onClose: () -> Unit) {
    val nav = LocalNav.current
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        val t = progress(2600)
        val ms = (t * 2600).toInt()
        val crystals = remember {
            Random(3).let { r ->
                val colors = listOf(Color.White, ArtColors.ice, ArtColors.iceMid, ArtColors.goldLight, ArtColors.gold)
                List(54) { Crystal((r.nextFloat() * 2 * PI).toFloat(), 0.45f + r.nextFloat() * 0.75f, (r.nextFloat() - 0.5f) * 720f, 6f + r.nextFloat() * 10f, colors[r.nextInt(colors.size)], r.nextFloat() < 0.65f) }
            }
        }
        Box(
            Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(ArtColors.deep, ArtColors.tileTop.copy(alpha = 0.85f), ArtColors.deep)))
                .clickable(onClick = onClose),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2, size.height * 0.40f)
                val tt = (ms - 600) / 1000f
                if (tt > 0f && t < 1f) for (c in crystals) {
                    val dist = c.speed * size.width * 0.75f * easeOut((tt * 1.6f).coerceIn(0f, 1f))
                    val pos = center + Offset(cos(c.angle) * dist, sin(c.angle) * dist + 900f * tt * tt)
                    val a = 1f - ((tt - 0.9f) / 0.6f).coerceIn(0f, 1f)
                    rotate(c.spin * tt, pos) {
                        if (c.diamond) {
                            val s = c.size * 1.4f
                            drawPath(Path().apply { moveTo(pos.x, pos.y - s); lineTo(pos.x + s * 0.55f, pos.y); lineTo(pos.x, pos.y + s); lineTo(pos.x - s * 0.55f, pos.y); close() }, c.color.copy(alpha = a))
                        } else drawCircle(c.color.copy(alpha = a), c.size * 0.5f, pos)
                    }
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 28.dp)) {
                val pop = 1f + 0.18f * (springy(((ms - 600) / 500f).coerceIn(0f, 1f)) - ((ms - 600) / 500f).coerceIn(0f, 1f))
                Box(Modifier.size(170.dp), contentAlignment = Alignment.Center) {
                    Canvas(Modifier.fillMaxSize()) {
                        val sw = 12.dp.toPx()
                        val ring = 0.92f + 0.08f * easeOut((ms / 600f).coerceIn(0f, 1f))
                        drawArc(Color.White.copy(alpha = 0.18f), 0f, 360f, false, Offset(sw / 2, sw / 2), Size(size.width - sw, size.height - sw), style = androidx.compose.ui.graphics.drawscope.Stroke(sw))
                        drawArc(lerp(ArtColors.iceMid, ArtColors.gold, ((ms - 500) / 300f).coerceIn(0f, 1f)), -90f, 360f * ring, false, Offset(sw / 2, sw / 2), Size(size.width - sw, size.height - sw), style = androidx.compose.ui.graphics.drawscope.Stroke(sw, cap = androidx.compose.ui.graphics.StrokeCap.Round))
                    }
                    GoalImage(m.art, Color(m.color), 112.dp, Modifier.scale(pop))
                }
                Spacer(Modifier.height(28.dp))
                val k1 = easeOut(((ms - 700) / 450f).coerceIn(0f, 1f))
                Text(
                    Res.s(R.string.moment_goal_title), color = Color.White, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center, modifier = Modifier.graphicsLayer { translationY = (1 - k1) * 40f; alpha = k1 },
                )
                val k2 = easeOut(((ms - 850) / 450f).coerceIn(0f, 1f))
                Text(
                    Res.s(R.string.moment_goal_sub, m.name, Money.format(m.saved)), color = Color.White.copy(alpha = 0.92f),
                    style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp).graphicsLayer { translationY = (1 - k2) * 30f; alpha = k2 },
                )
                if (m.months > 0) Text(
                    Res.p(R.plurals.moment_goal_months, m.months, m.months), color = Color.White.copy(alpha = 0.78f),
                    style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp).alpha(k2),
                )
                Spacer(Modifier.height(40.dp))
                val k3 = springy(((ms - 1150) / 500f).coerceIn(0f, 1f))
                CryoButton(
                    onClick = { onClose(); nav.open(Routes.plan(2)) },
                    modifier = Modifier.fillMaxWidth().height(56.dp).scale(0.85f + 0.15f * k3).alpha(((ms - 1150) / 200f).coerceIn(0f, 1f)),
                ) { Text(Res.s(R.string.moment_goal_button)) }
            }
        }
    }
}

/** RF-MOM-02: o orçamento ao salvar. A barra passa da marca dos 100%, o cartão treme, e o Cryo sugere de onde cobrir. */
@Composable
private fun BudgetMoment(m: Moment.Budget, onClose: () -> Unit) {
    val c = LocalContainer.current
    val nav = LocalNav.current
    val red = CryoTheme.colors.expense
    val amber = CryoTheme.colors.warning
    CryoSheet(onDismissRequest = onClose, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        val t = progress(1800, 200)
        val ms = (t * 1800).toInt()
        val target = m.spent.toFloat() / m.limit.coerceAtLeast(1)
        val start = (target - 0.16f).coerceAtLeast(0f)
        val fill = start + (target - start) * easeOut((ms / 650f).coerceIn(0f, 1f))
        val shake = if (m.over && ms in 650..1150) (sin((ms - 650) / 500f * 6 * PI) * 14 * (1 - (ms - 650) / 500f)).toFloat() else 0f
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 16.dp).navigationBarsPadding().graphicsLayer { translationX = shake.dp.toPx() }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ArtImage(if (m.over) Art.LIMIT else Art.BUDGET, 52.dp)
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        if (m.over) Res.s(R.string.alert_over_title, m.name) else Res.s(R.string.alert_80_title, m.name, io.github.h3yk0.cryo.domain.Num.percent((target * 100).toInt())),
                        style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold,
                    )
                    Text(Res.p(R.plurals.moment_days_left, m.daysLeft, m.daysLeft), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(20.dp))
            val track = MaterialTheme.colorScheme.surfaceContainerHighest
            val mark = MaterialTheme.colorScheme.onSurface
            Canvas(Modifier.fillMaxWidth().height(14.dp)) {
                val r = CornerRadius(size.height / 2)
                val scale = maxOf(1.15f, fill + 0.05f)
                val w = size.width / scale
                drawRoundRect(track, cornerRadius = r)
                drawRoundRect(if (fill >= 1f) lerp(amber, red, ((fill - 1f) / 0.03f).coerceIn(0f, 1f)) else amber, size = Size(w * fill, size.height), cornerRadius = r)
                drawLine(mark, Offset(w, -6f), Offset(w, size.height + 6f), strokeWidth = 2.dp.toPx())
            }
            Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
                Text(Money.format(m.spent), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = if (m.over) red else MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.weight(1f))
                Text(Res.s(R.string.moment_limit, Money.format(m.limit)), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            val tip = easeOut(((ms - 1150) / 450f).coerceIn(0f, 1f))
            m.move?.let { mv ->
                Row(
                    Modifier.padding(top = 18.dp).fillMaxWidth().alpha(tip).graphicsLayer { translationY = (1 - tip) * 24f }
                        .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(16.dp)).padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ArtImage(Art.BUDGET, 32.dp)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        Res.s(R.string.moment_tip, mv.fromName, Money.format(mv.free), Money.format(mv.amount)),
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
                Spacer(Modifier.height(16.dp))
                CryoButton(
                    onClick = {
                        onClose()
                        c.launch {
                            val group = c.repo.moveBudget(mv.fromId, m.categoryId, mv.amount, m.ym)
                            c.message(Res.s(R.string.moment_moved, Money.format(mv.amount), mv.fromName, m.name), Res.s(R.string.wallet_desfazer)) { c.repo.undoBudgetMove(group) }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                ) { Text(Res.s(R.string.moment_move, Money.format(mv.amount), mv.fromName)) }
            }
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = { onClose(); nav.go(Routes.BUDGET_MONTH) }) { Text(Res.s(R.string.moment_panel)) }
                TextButton(onClick = onClose) { Text(Res.s(R.string.moment_ok)) }
            }
        }
    }
}

/** RF-MOM-03/04: sequência e fechamento do mês, num cartão no meio da tela com a ilustração saltando. */
@Composable
private fun CardMoment(art: Art, title: String, text: String, onClose: () -> Unit) {
    Dialog(onDismissRequest = onClose) {
        val on = animationsOn()
        val k = remember { Animatable(if (on) 0f else 1f) }
        LaunchedEffect(Unit) { if (k.value < 1f) k.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 300f)) }
        Column(
            Modifier.fillMaxWidth().depthCard(RoundedCornerShape(28.dp)).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ArtImage(art, 112.dp, Modifier.scale(0.6f + 0.4f * k.value))
            Spacer(Modifier.height(16.dp))
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text(text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
            Spacer(Modifier.height(20.dp))
            CryoButton(onClick = onClose, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text(Res.s(R.string.moment_continue)) }
        }
    }
}

