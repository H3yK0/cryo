// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.h3yk0.cryo.R
import io.github.h3yk0.cryo.domain.Res
import io.github.h3yk0.cryo.ui.theme.CryoTheme
import io.github.h3yk0.cryo.ui.theme.CryoTone

/*
 * DS-BLOCO, DS-EXP e DS-TOM (reforma visual da 1.6, LA10): as peças que deixam as telas leves. Um bloco responde uma pergunta
 * (um número e uma linha) e abre a tela completa; o detalhe começa fechado, numa linha com a setinha; a cor pastel só no que
 * pede prioridade (DS-PSI: carga cognitiva, revelação progressiva, isolamento).
 */
object Tiles {
    /** DS-COMP-27: cantos dos blocos, espaço entre eles e a altura mínima (quase quadrado, um pouco mais alto que largo). */
    val Shape = RoundedCornerShape(24.dp)
    val Gap = 12.dp
    val MinHeight = 148.dp
}

/**
 * DS-COMP-27: o bloco. [value] é o número principal (já formatado, ou [valueContent] para um `MoneyText`); [subtitle], uma
 * linha curta. Com [tone], o fundo fica na cor pastel da prioridade (DS-TOM); sem ele, o cartão neutro com profundidade.
 */
@Composable
fun CryoTile(
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    iconColor: Color? = null,
    value: String? = null,
    valueColor: Color? = null,
    subtitle: String? = null,
    tone: CryoTone? = null,
    progress: Float? = null,
    progressColor: Color? = null,
    onClick: (() -> Unit)? = null,
    /** No lugar do ícone (a ilustração da meta, por exemplo). */
    leading: (@Composable () -> Unit)? = null,
    /** No lugar da setinha (o anel de progresso da meta, por exemplo). */
    trailing: (@Composable () -> Unit)? = null,
    valueContent: (@Composable () -> Unit)? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val fg = tone?.onContainer ?: MaterialTheme.colorScheme.onSurface
    Column(
        modifier
            .heightIn(min = Tiles.MinHeight)
            .pressScale(interaction, 0.97f)
            .depthCard(shape = Tiles.Shape, color = tone?.container ?: CryoTheme.depth.card)
            .then(if (onClick != null) Modifier.clickable(interactionSource = interaction, indication = androidx.compose.material3.ripple(), role = Role.Button, onClick = onClick) else Modifier)
            .padding(16.dp),
    ) {
        CompositionLocalProvider(LocalContentColor provides fg) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                when {
                    leading != null -> leading()
                    icon != null -> IconBadge(icon, iconColor ?: tone?.accent ?: MaterialTheme.colorScheme.primary, 36.dp)
                }
                Spacer(Modifier.weight(1f))
                when {
                    trailing != null -> trailing()
                    onClick != null -> Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = fg.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
                }
            }
            Spacer(Modifier.weight(1f).heightIn(min = 12.dp))
            Text(
                title, style = MaterialTheme.typography.labelLarge, color = fg.copy(alpha = 0.78f),
                maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
            when {
                valueContent != null -> valueContent()
                value != null -> Text(
                    value, style = MaterialTheme.typography.titleLarge, color = valueColor ?: fg,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
            if (subtitle != null) {
                Text(
                    subtitle, style = MaterialTheme.typography.bodySmall, color = fg.copy(alpha = 0.72f),
                    maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp),
                )
            }
            if (progress != null) {
                Spacer(Modifier.height(8.dp))
                UsageBar(progress, height = 6.dp, baseColor = progressColor ?: tone?.accent ?: MaterialTheme.colorScheme.primary)
            }
        }
    }
}

/**
 * DS-BLOCO-01: dois blocos lado a lado, com a mesma altura (o mais alto manda). Com um só, ele fica na metade da esquerda,
 * para a grade não mudar de forma.
 */
@Composable
fun TileRow(modifier: Modifier = Modifier, first: @Composable (Modifier) -> Unit, second: (@Composable (Modifier) -> Unit)? = null) {
    Row(modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(Tiles.Gap)) {
        first(Modifier.weight(1f).fillMaxHeight())
        if (second != null) second(Modifier.weight(1f).fillMaxHeight()) else Spacer(Modifier.weight(1f))
    }
}

/**
 * DS-BLOCO-02: botão-bloco, para ações secundárias (planejar pela renda, painel do mês, o que se repete): ícone, título e uma
 * linha, com a setinha. Nunca um cartão de texto.
 */
@Composable
fun TileButton(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    tone: CryoTone? = null,
    iconColor: Color? = null,
    /** Em pé (ícone em cima, texto embaixo): para dois botões-bloco lado a lado, sem cortar a frase. */
    stacked: Boolean = false,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val fg = tone?.onContainer ?: MaterialTheme.colorScheme.onSurface
    if (stacked) {
        Column(
            modifier.fillMaxWidth()
                .pressScale(interaction, 0.98f)
                .depthCard(shape = Tiles.Shape, color = tone?.container ?: CryoTheme.depth.card)
                .clickable(interactionSource = interaction, indication = androidx.compose.material3.ripple(), role = Role.Button, onClick = onClick)
                .padding(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(icon, iconColor ?: tone?.accent ?: MaterialTheme.colorScheme.primary, 36.dp)
                Spacer(Modifier.weight(1f))
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = fg.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(10.dp))
            Text(title, style = MaterialTheme.typography.titleSmall, color = fg, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = fg.copy(alpha = 0.72f), maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        return
    }
    Row(
        modifier.fillMaxWidth()
            .heightIn(min = 64.dp)
            .pressScale(interaction, 0.98f)
            .depthCard(shape = Tiles.Shape, color = tone?.container ?: CryoTheme.depth.card)
            .clickable(interactionSource = interaction, indication = androidx.compose.material3.ripple(), role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBadge(icon, iconColor ?: tone?.accent ?: MaterialTheme.colorScheme.primary, 36.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = fg, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = fg.copy(alpha = 0.72f), maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = fg.copy(alpha = 0.6f))
    }
}

/**
 * DS-COMP-28, DS-EXP-01: o detalhe fechado numa linha com a setinha. Tocar abre ali mesmo (a setinha gira); com "Remover
 * animações", abre sem movimento. A escolha fica guardada enquanto a tela existe.
 */
@Composable
fun Expandable(
    title: String,
    modifier: Modifier = Modifier,
    summary: String? = null,
    initiallyExpanded: Boolean = false,
    titleStyle: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.titleSmall,
    content: @Composable ColumnScope.() -> Unit,
) {
    var open by rememberSaveable { mutableStateOf(initiallyExpanded) }
    val moving = animationsOn()
    val turn by animateFloatAsState(if (open) 180f else 0f, if (moving) tween(Motion.SHORT) else tween(0), label = "seta")
    val opened = Res.s(R.string.common_expanded)
    val closed = Res.s(R.string.common_collapsed)
    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                .clickable(role = Role.Button) { open = !open }
                .semantics { stateDescription = if (open) opened else closed }
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, style = titleStyle)
                if (summary != null) {
                    Text(summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
            Icon(Icons.Rounded.KeyboardArrowDown, null, modifier = Modifier.rotate(turn), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (moving) {
            AnimatedVisibility(open, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                Column(Modifier.fillMaxWidth(), content = content)
            }
        } else if (open) {
            Column(Modifier.fillMaxWidth(), content = content)
        }
    }
}

/**
 * DS-TOM, RF-INI-19/20: um aviso na cor da prioridade (a central e o destaque do Início): o ícone no destaque do tom, o texto
 * e a setinha; [onDismiss] mostra o "×" (o destaque do Início some e o aviso fica como visto).
 */
@Composable
fun ToneNotice(
    icon: ImageVector,
    text: String,
    tone: CryoTone,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
) {
    Row(
        modifier.fillMaxWidth()
            .depthCard(shape = MaterialTheme.shapes.large, color = tone.container)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(start = 14.dp, end = if (onDismiss != null) 4.dp else 14.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(36.dp).clip(CircleShape).background(tone.accent.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = tone.accent, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = tone.onContainer, modifier = Modifier.weight(1f))
        if (onDismiss != null) {
            val desc = Res.s(R.string.inbox_dismiss)
            IconButton(onClick = onDismiss, modifier = Modifier.semantics { contentDescription = desc }) {
                Icon(Icons.Rounded.Close, null, tint = tone.onContainer.copy(alpha = 0.7f))
            }
        } else if (onClick != null) {
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = tone.onContainer.copy(alpha = 0.6f))
        }
    }
}

/** O título de um grupo dentro de uma tela em blocos (DS-PSI-04: tipos diferentes separados por espaço e título). */
@Composable
fun GroupTitle(text: String, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.onSurface) {
    Text(
        text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = color,
        modifier = modifier.padding(top = 8.dp, bottom = 2.dp).semantics { heading() },
    )
}

/**
 * RF-MET-07: o anel de progresso (a meta), com a porcentagem no meio. Cresce ao aparecer (DS-MOV-06) e para no lugar com
 * "Remover animações".
 */
@Composable
fun ProgressRing(ratio: Float, color: Color, modifier: Modifier = Modifier, size: Dp = 44.dp) {
    val appear by rememberAppear()
    val value by animateFloatAsState(ratio.coerceIn(0f, 1f), tween(Motion.LONG, easing = Motion.Emphasized), label = "anel")
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
            val stroke = this.size.minDimension * 0.12f
            val inset = stroke / 2
            val arc = androidx.compose.ui.geometry.Size(this.size.width - stroke, this.size.height - stroke)
            val topLeft = androidx.compose.ui.geometry.Offset(inset, inset)
            drawArc(track, 0f, 360f, false, topLeft, arc, style = androidx.compose.ui.graphics.drawscope.Stroke(stroke))
            drawArc(
                color, -90f, 360f * value * appear, false, topLeft, arc,
                style = androidx.compose.ui.graphics.drawscope.Stroke(stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round),
            )
        }
        Text(
            io.github.h3yk0.cryo.domain.Num.percent((ratio * 100).toInt().coerceAtLeast(0)),
            style = MaterialTheme.typography.labelSmall, maxLines = 1,
        )
    }
}

/** Espaço padrão entre blocos de uma grade. */
val TileGap: Dp get() = Tiles.Gap
