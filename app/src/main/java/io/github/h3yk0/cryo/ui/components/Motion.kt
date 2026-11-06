// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.ui.components

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import android.provider.Settings
import kotlin.math.roundToLong

/*
 * DS-MOV (PRD, seção 9.8): o movimento do Cryo num lugar só. O movimento mostra de onde a tela veio, para onde foi e o que
 * mudou; nunca enfeita nem faz a pessoa esperar.
 *
 * Regras que valem para tudo aqui (DS-MOV-10):
 * - Só as APIs de animação do Compose. Elas seguem a escala de animação do Android: com "Remover animações" ligado, tudo
 *   termina na hora e aparece no estado final (ACS-08). Um delay() não seguiria a escala, por isso nunca é usado para animar.
 * - Nenhuma animação infinita: além de gastar bateria, impediria os testes de saber quando a tela ficou pronta.
 */
object Motion {
    /** DS-MOV-01: toque, algo pequeno que aparece ou some. */
    const val SHORT = 150

    /** DS-MOV-01: trocar de tela, abrir uma parte. */
    const val MEDIUM = 300

    /** DS-MOV-01: números e gráficos. */
    const val LONG = 600

    /** Curvas "emphasized" do Material 3. */
    val Emphasized = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
    val EmphasizedAccelerate = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

    /** Quanto a tela desliza no eixo X (DS-MOV-02): um décimo da largura, só para mostrar a direção. */
    private const val SHIFT = 10

    /* ---------- DS-MOV-02: abrir e voltar telas (eixo X do Material) ---------- */

    /** A tela nova entra pela direita. */
    fun forwardEnter(): EnterTransition =
        slideInHorizontally(tween(MEDIUM, easing = EmphasizedDecelerate)) { it / SHIFT } +
            fadeIn(tween(MEDIUM - 90, delayMillis = 90, easing = EmphasizedDecelerate))

    /** A tela de baixo sai um pouco para a esquerda. */
    fun forwardExit(): ExitTransition =
        slideOutHorizontally(tween(MEDIUM, easing = EmphasizedAccelerate)) { -it / SHIFT } +
            fadeOut(tween(90, easing = EmphasizedAccelerate))

    /** Ao voltar, a tela de baixo volta da esquerda. */
    fun backEnter(): EnterTransition =
        slideInHorizontally(tween(MEDIUM, easing = EmphasizedDecelerate)) { -it / SHIFT } +
            fadeIn(tween(MEDIUM - 90, delayMillis = 90, easing = EmphasizedDecelerate))

    /** Ao voltar, a tela de cima sai pela direita. */
    fun backExit(): ExitTransition =
        slideOutHorizontally(tween(MEDIUM, easing = EmphasizedAccelerate)) { it / SHIFT } +
            fadeOut(tween(90, easing = EmphasizedAccelerate))

    /* ---------- DS-MOV-03: trocar de aba ("fade through") ---------- */

    fun tabEnter(): EnterTransition =
        fadeIn(tween(210, delayMillis = 90, easing = EmphasizedDecelerate)) +
            scaleIn(tween(210, delayMillis = 90, easing = EmphasizedDecelerate), initialScale = 0.94f)

    fun tabExit(): ExitTransition = fadeOut(tween(90, easing = EmphasizedAccelerate))

    /**
     * DS-MOV-09: passos (apresentação, passeio): para a frente quando o número do passo sobe, para trás quando desce.
     * Para usar no `transitionSpec` de um AnimatedContent com o número do passo.
     */
    fun AnimatedContentTransitionScope<Int>.steps(): ContentTransform =
        if (targetState >= initialState) forwardEnter() togetherWith forwardExit() else backEnter() togetherWith backExit()
}

/**
 * Liga ou desliga as animações de "aparecer" (DS-MOV-06). Fica ligado no app; as imagens de tela feitas fora de uma
 * Activity (que não andam os quadros da animação) desligam para mostrar o estado final.
 */
val LocalAppearAnimations = staticCompositionLocalOf { true }

/**
 * ACS-08: as animações estão ligadas? Desligadas quando a pessoa ligou "Remover animações" no Android (escala de animação 0)
 * ou quando a tela pede ([LocalAppearAnimations]). O Compose já termina na hora as animações com escala 0; olhar a escala
 * aqui também faz as peças que "aparecem" (e o passeio) começarem direto no quadro final, sem nem um quadro do começo.
 */
@Composable
fun animationsOn(): Boolean {
    if (!LocalAppearAnimations.current) return false
    val ctx = LocalContext.current
    return remember(ctx) {
        runCatching { Settings.Global.getFloat(ctx.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) }.getOrDefault(1f) > 0f
    }
}

/**
 * DS-MOV-06: vai de 0 a 1 uma vez, quando a peça aparece (barras crescendo, gráfico se desenhando). Use o valor como fração
 * do desenho final.
 */
@Composable
fun rememberAppear(durationMillis: Int = Motion.LONG, delayMillis: Int = 0): State<Float> {
    val enabled = animationsOn()
    val a = remember { Animatable(if (enabled) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (a.value < 1f) a.animateTo(1f, tween(durationMillis, delayMillis, Motion.Emphasized))
    }
    return a.asState()
}

/**
 * DS-MOV-05: um valor em centavos que conta até o novo valor quando muda. Na primeira vez aparece pronto. Se mudar no meio da
 * contagem, continua de onde está. O valor no fim é sempre o exato (a conta é feita em Double só durante o caminho).
 */
@Composable
fun animateMoney(target: Long, enabled: Boolean = true): Long {
    val on = enabled && animationsOn()
    var from by remember { mutableLongStateOf(target) }
    var to by remember { mutableLongStateOf(target) }
    val progress = remember { Animatable(1f) }
    LaunchedEffect(target, on) {
        if (target == to) return@LaunchedEffect
        if (!on) {
            from = target; to = target
            progress.snapTo(1f)
            return@LaunchedEffect
        }
        from = between(from, to, progress.value)
        to = target
        progress.snapTo(0f)
        progress.animateTo(1f, tween(Motion.LONG, easing = Motion.Emphasized))
    }
    // Enquanto a contagem não começou (o primeiro quadro depois da mudança), continua mostrando o valor de antes.
    return between(from, to, progress.value)
}

private fun between(from: Long, to: Long, t: Float): Long =
    if (t >= 1f) to else from + ((to - from).toDouble() * t).roundToLong()

/** DS-MOV-08: a peça encolhe de leve enquanto está pressionada e volta ao soltar. */
@Composable
fun Modifier.pressScale(interaction: InteractionSource, pressed: Float = 0.92f): Modifier {
    val isPressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) pressed else 1f, tween(Motion.SHORT, easing = Motion.Emphasized), label = "press")
    return this.graphicsLayer { scaleX = scale; scaleY = scale }
}
