// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.res.painterResource
import io.github.h3yk0.cryo.R
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import io.github.h3yk0.cryo.ui.theme.CryoTheme

/*
 * DS-PROF (PRD, seção 9.9): a profundidade do Cryo, com a mesma luz do ícone (vinda de cima). O que importa parece um objeto
 * que se pode tocar: cartões claros sobre um fundo agrupado, sombras largas e suaves, um fio de luz no topo e vibração ao
 * toque. Discreto e consistente: nada de brilho exagerado, reflexo ou textura.
 */
object Depth {
    /** DS-PROF-02: cantos e margem dos cartões. */
    val CardShape = RoundedCornerShape(24.dp)
    val Margin = 16.dp
}

/**
 * DS-PROF-01/02: um cartão com profundidade. Sombra larga e clara (o "ar" embaixo), uma sombra de contato bem curta e, no
 * escuro, um fio de luz no topo que separa o cartão do fundo.
 */
@Composable
fun Modifier.depthCard(shape: Shape = Depth.CardShape, color: Color = CryoTheme.depth.card, lifted: Boolean = true): Modifier {
    val d = CryoTheme.depth
    return this
        .then(
            if (lifted) {
                Modifier
                    .dropShadow(shape, Shadow(radius = 18.dp, color = d.shadow, offset = DpOffset(0.dp, 6.dp)))
                    .dropShadow(shape, Shadow(radius = 2.dp, color = d.shadowContact, offset = DpOffset(0.dp, 1.dp)))
            } else Modifier,
        )
        .clip(shape)
        .background(color)
        .then(
            if (d.highlight.alpha > 0f) Modifier.border(1.dp, Brush.verticalGradient(listOf(d.highlight, Color.Transparent), endY = 60f), shape)
            else Modifier,
        )
}

/**
 * DS-PROF-02: uma linha de lista agrupada, como as listas do iPhone: as linhas de um grupo (as movimentações de um dia, as
 * compras de uma fatura) formam um cartão só, com cantos só na primeira e na última.
 */
@Composable
fun Modifier.groupedRow(index: Int, count: Int): Modifier {
    val r = 20.dp
    val first = index == 0
    val last = index == count - 1
    val shape = RoundedCornerShape(
        topStart = if (first) r else 0.dp, topEnd = if (first) r else 0.dp,
        bottomStart = if (last) r else 0.dp, bottomEnd = if (last) r else 0.dp,
    )
    return this.padding(horizontal = Depth.Margin).clip(shape).background(CryoTheme.depth.card)
        .padding(top = if (first) 6.dp else 0.dp, bottom = if (last) 6.dp else 0.dp)
}

/**
 * DS-PROF-04/05: volume de uma peça tocável (círculo dos atalhos, botão principal): degradê com luz em cima, fio de luz e
 * sombra na cor da peça.
 */
@Composable
fun Modifier.raised(shape: Shape, base: Color, shadowColor: Color = CryoTheme.depth.shadow, glow: Dp = 10.dp): Modifier {
    val top = lerp(base, Color.White, if (CryoTheme.colors.isDark) 0.10f else 0.16f)
    val bottom = lerp(base, Color.Black, if (CryoTheme.colors.isDark) 0.12f else 0.06f)
    return this
        .dropShadow(shape, Shadow(radius = glow, color = shadowColor, offset = DpOffset(0.dp, glow * 0.4f)))
        .clip(shape)
        .background(Brush.verticalGradient(listOf(top, bottom)))
        .border(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = if (CryoTheme.colors.isDark) 0.14f else 0.55f), Color.Transparent), endY = 40f), shape)
}


/**
 * DS-PROF-07: vibrações com nome, como no iPhone. Usa o `HapticFeedback` do sistema, que segue o ajuste "vibração ao toque"
 * do celular e não precisa de permissão.
 */
class Haptics(private val h: HapticFeedback) {
    /** Salvar, importar, concluir. */
    fun confirm() = h.performHapticFeedback(HapticFeedbackType.Confirm)

    /** Algo deu errado ou foi recusado. */
    fun reject() = h.performHapticFeedback(HapticFeedbackType.Reject)

    /** Interruptor ligado ou desligado. */
    fun toggle(on: Boolean) = h.performHapticFeedback(if (on) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff)

    /** Trocar de aba, de segmento ou de página. */
    fun tick() = h.performHapticFeedback(HapticFeedbackType.SegmentTick)

    /** Toque leve num atalho. */
    fun tap() = h.performHapticFeedback(HapticFeedbackType.ContextClick)
}

@Composable
fun rememberHaptics(): Haptics {
    val h = LocalHapticFeedback.current
    return remember(h) { Haptics(h) }
}

/**
 * DS-COMP-24, DS-PROF-05: o botão principal com volume. Mesmos parâmetros do `Button`: degradê da cor principal, fio de luz,
 * sombra colorida, encolhe ao toque (mola) e vibra ao confirmar. Desligado, fica plano como os outros.
 */
@Composable
fun CryoButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    content: @Composable RowScope.() -> Unit,
) {
    val haptics = rememberHaptics()
    val interaction = remember { MutableInteractionSource() }
    val primary = MaterialTheme.colorScheme.primary
    val shape = CircleShape
    Button(
        onClick = { haptics.confirm(); onClick() },
        modifier = modifier
            .heightIn(min = 48.dp)
            .pressScale(interaction, 0.97f)
            .then(if (enabled) Modifier.raised(shape, primary, shadowColor = primary.copy(alpha = 0.38f), glow = 14.dp) else Modifier),
        enabled = enabled,
        shape = shape,
        colors = if (enabled) ButtonDefaults.buttonColors(containerColor = Color.Transparent) else ButtonDefaults.buttonColors(),
        contentPadding = contentPadding,
        interactionSource = interaction,
        content = content,
    )
}

/**
 * DS-ICO-03: o ícone do Cryo (G4) como selo, com a sombra do próprio ícone, para onde a marca aparece dentro do app
 * (apresentação, bloqueio, Sobre). Decorativo para o leitor de tela: o texto ao lado diz o nome.
 */
@Composable
fun CryoBadge(modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier) {
        val shape = RoundedCornerShape(maxWidth * 0.22f)
        Image(
            painterResource(R.drawable.ic_cryo_badge), contentDescription = null,
            modifier = Modifier.fillMaxSize()
                .dropShadow(shape, Shadow(radius = maxWidth * 0.14f, color = CryoTheme.depth.headerShadow, offset = DpOffset(0.dp, maxWidth * 0.06f))),
        )
    }
}

/**
 * DS-PAD-02, DS-PROF-06: a faixa de baixo com a ação principal das telas internas, na cor dos cartões e com sombra para
 * cima (a mesma da barra de abas): o botão fica "apoiado" sobre a tela.
 */
@Composable
fun BottomBarSurface(content: @Composable () -> Unit) {
    val d = CryoTheme.depth
    androidx.compose.material3.Surface(
        color = d.card,
        modifier = Modifier.dropShadow(androidx.compose.ui.graphics.RectangleShape, Shadow(radius = 16.dp, color = d.shadow, offset = DpOffset(0.dp, (-2).dp))),
        content = content,
    )
}
