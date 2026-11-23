// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.ui.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.sign

/**
 * RF-GES-01..03 (1.7, LA7): arrastar o dedo para o lado passa para a aba vizinha. O anfitrião guarda as áreas que já se
 * arrastam para o lado (onde o gesto não vale), o passo "de dentro" de uma aba (as partes de Planejar) e o lado do último
 * arrasto (para a tela nova entrar pelo lado certo).
 */
class TabSwipeHost {
    internal val zones = mutableStateMapOf<Any, Rect>()

    /** RF-GES-02: a aba que tem partes (Planejar) responde primeiro; devolve true se ela mesma mudou de parte. */
    var inner: ((Int) -> Boolean)? = null

    /** O lado para onde o dedo foi no último arrasto que trocou de aba (-1 = para a esquerda, 1 = para a direita, 0 = nenhum). */
    var fingerSign by mutableIntStateOf(0)

    /** Quanto o dedo andou agora (para a tela acompanhar um pouco), em pixels. */
    var drag by mutableFloatStateOf(0f)

    internal var coords: LayoutCoordinates? = null
}

val LocalTabSwipe = staticCompositionLocalOf<TabSwipeHost?> { null }

/** RF-GES-03: a borda livre de cada lado, para o "voltar" do Android (que usa uns 24 dp) nunca brigar com o arrasto. */
val TAB_SWIPE_EDGE = 48.dp

/**
 * RF-GES-03: marca uma área que já se arrasta para o lado (o gráfico do ano, listas que rolam para o lado): o arrasto entre
 * abas não começa nela.
 */
fun Modifier.noTabSwipe(): Modifier = composed {
    val host = LocalTabSwipe.current ?: return@composed this
    val key = remember { Any() }
    DisposableEffect(host, key) { onDispose { host.zones.remove(key) } }
    onGloballyPositioned { host.zones[key] = it.boundsInWindow() }
}

/** RF-GES-02: a aba com partes (Planejar) diz como andar entre elas. [step] devolve true se mudou de parte. */
@Composable
fun TabSwipeInner(step: (Int) -> Boolean) {
    val host = LocalTabSwipe.current ?: return
    DisposableEffect(host, step) {
        host.inner = step
        onDispose { if (host.inner === step) host.inner = null }
    }
}

/**
 * O detector do arrasto entre abas. Ele olha o toque **antes** das telas (passo Initial) e só toma o gesto quando o dedo anda
 * claramente para o lado (mais que a folga do toque e mais para o lado que para cima ou para baixo); a partir daí, as telas
 * não veem mais o movimento (nada rola nem é tocado por engano). Um movimento para cima ou para baixo é da lista, e o
 * detector desiste. Ao soltar, passa de aba se o dedo andou o bastante ou foi rápido. [onSwipe] recebe +1 (a próxima aba)
 * ou -1 (a anterior), já espelhado em árabe e hebraico ([rtl]).
 */
fun Modifier.tabSwipe(host: TabSwipeHost, enabled: Boolean, rtl: Boolean, onSwipe: (Int) -> Unit): Modifier =
    this.onGloballyPositioned { host.coords = it }.pointerInput(enabled, rtl) {
        if (!enabled) return@pointerInput
        val edge = TAB_SWIPE_EDGE.toPx()
        val slop = viewConfiguration.touchSlop
        val distance = 80.dp.toPx()
        val fling = 900.dp.toPx()
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            if (down.position.x < edge || down.position.x > size.width - edge) return@awaitEachGesture
            val window = host.coords?.takeIf { it.isAttached }?.localToWindow(down.position) ?: down.position
            if (host.zones.values.any { it.contains(window) }) return@awaitEachGesture
            val velocity = VelocityTracker()
            velocity.addPosition(down.uptimeMillis, down.position)
            var dx = 0f
            var dy = 0f
            var claimed = false
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                // Dois dedos é pinça ou outra coisa: desiste.
                if (event.changes.count { it.pressed } > 1) {
                    host.drag = 0f
                    return@awaitEachGesture
                }
                val change = event.changes.firstOrNull { it.id == down.id } ?: run { host.drag = 0f; return@awaitEachGesture }
                val d = change.positionChange()
                dx += d.x
                dy += d.y
                velocity.addPosition(change.uptimeMillis, change.position)
                if (!claimed) {
                    if (abs(dy) > slop && abs(dy) >= abs(dx)) return@awaitEachGesture
                    if (abs(dx) > slop && abs(dx) > abs(dy) * 1.5f) claimed = true
                }
                if (claimed) {
                    change.consume()
                    host.drag = dx
                }
                if (!change.pressed) {
                    if (claimed) {
                        val v = velocity.calculateVelocity().x
                        val go = abs(dx) > distance || (abs(v) > fling && sign(v) == sign(dx))
                        if (go) {
                            host.fingerSign = if (dx < 0) -1 else 1
                            // O dedo para a esquerda leva à próxima aba (como virar a página); em árabe e hebraico, ao contrário.
                            val next = if (dx < 0) 1 else -1
                            onSwipe(if (rtl) -next else next)
                        }
                    }
                    host.drag = 0f
                    return@awaitEachGesture
                }
            }
        }
    }
