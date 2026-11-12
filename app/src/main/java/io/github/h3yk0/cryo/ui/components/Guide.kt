// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.ui.components

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EventRepeat
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.h3yk0.cryo.R
import io.github.h3yk0.cryo.data.db.TxType
import io.github.h3yk0.cryo.domain.GuideFacts
import io.github.h3yk0.cryo.domain.GuideLevel
import io.github.h3yk0.cryo.domain.GuidePlace
import io.github.h3yk0.cryo.domain.GuidePlanner
import io.github.h3yk0.cryo.domain.Lesson
import io.github.h3yk0.cryo.domain.Res
import io.github.h3yk0.cryo.AppContainer
import io.github.h3yk0.cryo.ui.LocalNav
import io.github.h3yk0.cryo.ui.Navigator
import io.github.h3yk0.cryo.ui.Routes
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import java.time.LocalDate

/*
 * RF-GUI-05: como o guia aparece. Um cartão que sobe acima da barra de abas (não é diálogo, não trava a tela) e, em
 * "Me mostra", a tela certa com o ponto contornado. Tudo com movimento finito (DS-MOV-10).
 */

/** Os pontos fixos da tela que o guia sabe contornar (o campo de frase, o botão de voz). */
class GuideSpots {
    val bounds = mutableStateMapOf<Lesson, Rect>()
    var active by mutableStateOf<Lesson?>(null)
}

val LocalGuideSpots = staticCompositionLocalOf<GuideSpots?> { null }

/** Marca esta peça como o ponto da lição [l]: o guia passa a saber onde ela está na tela. */
fun Modifier.guideTarget(l: Lesson): Modifier = composed {
    val spots = LocalGuideSpots.current ?: return@composed this
    DisposableEffect(l) { onDispose { spots.bounds.remove(l) } }
    onGloballyPositioned { val r = it.boundsInRoot(); if (spots.bounds[l] != r) spots.bounds[l] = r }
}

@StringRes
fun Lesson.titleRes(): Int = when (this) {
    Lesson.PHRASE -> R.string.lesson_phrase_t
    Lesson.VOICE -> R.string.lesson_voice_t
    Lesson.EDIT -> R.string.lesson_edit_t
    Lesson.CATEGORIES -> R.string.lesson_categories_t
    Lesson.CARD -> R.string.lesson_card_t
    Lesson.BILLS -> R.string.lesson_bills_t
    Lesson.BUDGETS -> R.string.lesson_budgets_t
    Lesson.MONTH -> R.string.lesson_month_t
    Lesson.GOALS -> R.string.lesson_goals_t
    Lesson.DEBTS -> R.string.lesson_debts_t
    Lesson.FORECAST -> R.string.lesson_forecast_t
    Lesson.HEALTH -> R.string.lesson_health_t
    Lesson.RECURRING -> R.string.lesson_recurring_t
    Lesson.BACKUP -> R.string.lesson_backup_t
    Lesson.LOCK -> R.string.lesson_lock_t
    Lesson.IMPORT -> R.string.lesson_import_t
    Lesson.SHOPPING -> R.string.lesson_shopping_t
    Lesson.INVEST -> R.string.lesson_invest_t
}

@StringRes
fun Lesson.textRes(): Int = when (this) {
    Lesson.PHRASE -> R.string.lesson_phrase_x
    Lesson.VOICE -> R.string.lesson_voice_x
    Lesson.EDIT -> R.string.lesson_edit_x
    Lesson.CATEGORIES -> R.string.lesson_categories_x
    Lesson.CARD -> R.string.lesson_card_x
    Lesson.BILLS -> R.string.lesson_bills_x
    Lesson.BUDGETS -> R.string.lesson_budgets_x
    Lesson.MONTH -> R.string.lesson_month_x
    Lesson.GOALS -> R.string.lesson_goals_x
    Lesson.DEBTS -> R.string.lesson_debts_x
    Lesson.FORECAST -> R.string.lesson_forecast_x
    Lesson.HEALTH -> R.string.lesson_health_x
    Lesson.RECURRING -> R.string.lesson_recurring_x
    Lesson.BACKUP -> R.string.lesson_backup_x
    Lesson.LOCK -> R.string.lesson_lock_x
    Lesson.IMPORT -> R.string.lesson_import_x
    Lesson.SHOPPING -> R.string.lesson_shopping_x
    Lesson.INVEST -> R.string.lesson_invest_x
}

@StringRes
fun GuideLevel.titleRes(): Int = when (this) {
    GuideLevel.START -> R.string.learn_level_start
    GuideLevel.ORGANIZE -> R.string.learn_level_organize
    GuideLevel.PLAN -> R.string.learn_level_plan
    GuideLevel.UNDERSTAND -> R.string.learn_level_understand
    GuideLevel.PROTECT -> R.string.learn_level_protect
    GuideLevel.BEYOND -> R.string.learn_level_beyond
}

fun Lesson.icon(): ImageVector = when (this) {
    Lesson.PHRASE -> Icons.Rounded.AutoAwesome
    Lesson.VOICE -> Icons.Rounded.Mic
    Lesson.EDIT -> Icons.Rounded.Edit
    Lesson.CATEGORIES -> Icons.Rounded.Category
    Lesson.CARD -> Icons.Rounded.CreditCard
    Lesson.BILLS -> Icons.Rounded.EventRepeat
    Lesson.BUDGETS -> Icons.Rounded.PieChart
    Lesson.MONTH -> Icons.Rounded.Dashboard
    Lesson.GOALS -> Icons.Rounded.Flag
    Lesson.DEBTS -> Icons.Rounded.AccountBalance
    Lesson.FORECAST -> Icons.Rounded.Insights
    Lesson.HEALTH -> Icons.Rounded.Favorite
    Lesson.RECURRING -> Icons.Rounded.Autorenew
    Lesson.BACKUP -> Icons.Rounded.Backup
    Lesson.LOCK -> Icons.Rounded.Lock
    Lesson.IMPORT -> Icons.Rounded.UploadFile
    Lesson.SHOPPING -> Icons.Rounded.ShoppingCart
    Lesson.INVEST -> Icons.AutoMirrored.Rounded.TrendingUp
}

/** O azulejo com o ícone do assunto da lição. */
@Composable
fun LessonIcon(l: Lesson, size: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier.size(size).background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(size * 0.3f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(l.icon(), null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(size * 0.55f))
    }
}

/** RF-GUI-01: o que o guia sabe agora, tirado dos dados e dos ajustes deste container. */
suspend fun guideFacts(c: AppContainer, today: LocalDate): GuideFacts {
    val s = c.repo.snapshot.value ?: return GuideFacts()
    return GuideFacts.of(
        s, today,
        backupOn = c.autoBackup.flow.first().enabled,
        lockOn = c.lockSettings.flow.first().lockEnabled,
        recurringOpen = c.engine.latest?.recurrences?.count { it.type == TxType.EXPENSE && it.billId == null } ?: 0,
        minutesSinceFirstOpen = c.guide.minutesSinceFirstOpen(),
    )
}

/** "Me mostra": abre a tela da lição e, quando ela tem um ponto fixo, pede o destaque. */
fun showLesson(l: Lesson, nav: Navigator, spots: GuideSpots?) {
    // As lições que moram numa aba fecham primeiro o que está por cima (o perfil, a lista), para a aba Início não
    // reabrir a lista quando a pessoa voltar a ela; as outras empilham a tela, e Voltar retorna para onde se estava.
    when (l) {
        Lesson.PHRASE, Lesson.VOICE, Lesson.SHOPPING -> nav.home()
        Lesson.EDIT -> { nav.home(); nav.tab(Routes.txs()) }
        Lesson.CATEGORIES -> nav.go(Routes.CATEGORIES)
        Lesson.CARD, Lesson.INVEST -> nav.go(Routes.WALLET)
        Lesson.BILLS -> { nav.home(); nav.open(Routes.plan(1)) }
        Lesson.BUDGETS -> { nav.home(); nav.open(Routes.plan(0)) }
        Lesson.MONTH -> nav.go(Routes.BUDGET_MONTH)
        Lesson.GOALS -> { nav.home(); nav.open(Routes.plan(2)) }
        Lesson.DEBTS -> nav.go(Routes.DEBTS)
        Lesson.FORECAST -> { nav.home(); nav.tab(Routes.INSIGHTS) }
        Lesson.HEALTH -> nav.go(Routes.HEALTH)
        Lesson.RECURRING -> nav.go(Routes.RECURRING)
        Lesson.BACKUP -> nav.go(Routes.AUTO_BACKUP)
        Lesson.LOCK -> nav.go(Routes.SETTINGS)
        Lesson.IMPORT -> nav.go(Routes.import())
    }
    if (l.spot) spots?.active = l
}

/** O cartão da dica: o assunto, uma ou duas frases e os dois botões. O leitor de tela lê sem roubar o foco. */
@Composable
fun GuideTipCard(l: Lesson, onShow: () -> Unit, onLater: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().depthCard(RoundedCornerShape(24.dp)).semantics { liveRegion = LiveRegionMode.Polite }
            .padding(start = 16.dp, end = 12.dp, top = 16.dp, bottom = 10.dp),
    ) {
        Row {
            LessonIcon(l, 44.dp)
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(Res.s(R.string.guide_tip), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Text(Res.s(l.titleRes()), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(Res.s(l.textRes()), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onLater) { Text(Res.s(R.string.guide_later)) }
            Spacer(Modifier.width(8.dp))
            CryoButton(onClick = onShow) { Text(Res.s(R.string.guide_show)) }
        }
    }
}

/**
 * RF-GUI-03/04: decide se sai uma dica agora e mostra o cartão. [place] é a aba aberta (null fora das abas: lá o guia não
 * fala); [blocked] = há outra coisa pedindo a atenção (a pergunta do lembrete). No modo de teste o guia fica quieto.
 */
@Composable
fun GuideHost(place: GuidePlace?, ready: Boolean, blocked: Boolean, modifier: Modifier = Modifier) {
    val c = LocalContainer.current
    val nav = LocalNav.current
    val spots = LocalGuideSpots.current
    val today = rememberToday()
    val moment by c.moments.current.collectAsState()
    var tip by rememberSaveable(today) { mutableStateOf<Lesson?>(null) }

    // RF-GUI-03: no dia de um momento (meta completa, orçamento estourado...) o guia não fala.
    LaunchedEffect(moment != null) {
        if (moment != null && !c.sandbox) { c.guide.markMoment(today); tip = null }
    }
    LaunchedEffect(place, ready, blocked, moment != null) {
        if (c.sandbox || !ready || blocked || place == null || tip != null || moment != null || !c.guide.enabled) return@LaunchedEffect
        val next = GuidePlanner.next(guideFacts(c, today), c.guide.log(), today, place) ?: return@LaunchedEffect
        // Conta na hora em que aparece: fechar o app sem tocar em nada não faz a dica voltar no mesmo dia.
        c.guide.markShown(next, today)
        tip = next
    }

    val visible = tip != null && place != null && !blocked && moment == null
    // O cartão continua desenhado enquanto desce (a dica já virou null).
    var last by remember { mutableStateOf<Lesson?>(null) }
    if (tip != null) last = tip
    BackHandler(enabled = visible) { c.guide.markLater(today); tip = null }
    AnimatedVisibility(
        visible, modifier,
        enter = slideInVertically(tween(Motion.MEDIUM, easing = Motion.EmphasizedDecelerate)) { it / 2 } + fadeIn(tween(Motion.MEDIUM)),
        exit = slideOutVertically(tween(Motion.SHORT, easing = Motion.EmphasizedAccelerate)) { it / 2 } + fadeOut(tween(Motion.SHORT)),
    ) {
        last?.let { l ->
            GuideTipCard(
                l,
                onShow = { c.guide.markSeen(l); tip = null; showLesson(l, nav, spots) },
                onLater = { c.guide.markLater(today); tip = null },
                Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
    }
}

/** RF-GUI-05: o destaque. O resto da tela escurece e um anel contorna o ponto; um toque (ou Voltar) fecha. */
@Composable
fun GuideSpotlight(spots: GuideSpots) {
    val l = spots.active ?: return
    // O ponto pode não estar à vista (a tela rolou): espera um instante e desiste, sem deixar o destaque para depois.
    LaunchedEffect(l) {
        val found = withTimeoutOrNull(1_500) { snapshotFlow { spots.bounds[l] }.filterNotNull().first() }
        if (found == null) spots.active = null
    }
    val target = spots.bounds[l] ?: return
    val on = animationsOn()
    val k = remember(l) { Animatable(if (on) 0f else 1f) }
    LaunchedEffect(l) { if (k.value < 1f) k.animateTo(1f, tween(Motion.MEDIUM)) }
    var origin by remember { mutableStateOf(Offset.Zero) }
    val ring = MaterialTheme.colorScheme.primary
    val label = Res.s(l.titleRes())
    BackHandler { spots.active = null }
    Canvas(
        Modifier.fillMaxSize()
            .onGloballyPositioned { origin = it.positionInRoot() }
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen; alpha = k.value }
            .pointerInput(Unit) { detectTapGestures { spots.active = null } }
            .semantics { contentDescription = label; onClick { spots.active = null; true } },
    ) {
        drawRect(Color.Black.copy(alpha = 0.6f))
        val r = target.translate(-origin.x, -origin.y).inflate(8.dp.toPx())
        val corner = CornerRadius(minOf(r.height / 2, 32.dp.toPx()))
        drawRoundRect(Color.Transparent, r.topLeft, r.size, corner, blendMode = BlendMode.Clear)
        drawRoundRect(ring, r.topLeft, r.size, corner, style = Stroke(3.dp.toPx()))
    }
}
