// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.ui.components

import android.app.Activity
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import io.github.h3yk0.cryo.R
import io.github.h3yk0.cryo.domain.Res
import io.github.h3yk0.cryo.ui.theme.CryoTheme

/*
 * Design system 2.0 (PRD, seção 9.1): o app organizado como um app de banco, com as cores do Cryo.
 * Seções planas separadas por linhas finas no lugar de cartões empilhados, atalhos redondos e um cabeçalho colorido no Início.
 */

/** Margem lateral das seções (DS-ESP-01). */
val SectionPadding = 20.dp

/**
 * DS-COMP-14: seção plana. Título com "›" quando leva a outra tela, o conteúdo e uma linha fina embaixo.
 * [onClick]: a seção inteira é tocável (o leitor de tela lê tudo junto). Quando o conteúdo tem os próprios toques (uma lista
 * de movimentações), use [headerOnly]: aí só a linha do título leva à outra tela.
 */
@Composable
fun CryoSection(
    title: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    headerOnly: Boolean = false,
    divider: Boolean = true,
    /** Ação no título, no lugar do "›" (ex.: "+ Nova"). */
    action: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier.fillMaxWidth()) {
        val whole = onClick != null && !headerOnly
        Column(Modifier.fillMaxWidth().then(if (whole) Modifier.clickable(onClick = onClick!!) else Modifier).padding(vertical = 8.dp)) {
            Row(
                Modifier.fillMaxWidth()
                    .then(if (onClick != null && headerOnly) Modifier.clickable(onClick = onClick) else Modifier)
                    .padding(horizontal = SectionPadding, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f).semantics { heading() },
                )
                when {
                    action != null -> action()
                    onClick != null -> Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Column(Modifier.fillMaxWidth().padding(start = SectionPadding, end = SectionPadding, bottom = 12.dp), content = content)
        }
        if (divider) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
    }
}

/**
 * DS-COMP-18: linha de lista dentro de uma seção (conta, cartão, investimento...): ícone, título, subtítulo e o valor à direita.
 * A linha inteira é tocável quando leva a outra tela.
 */
@Composable
fun CryoListRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    below: (@Composable ColumnScope.() -> Unit)? = null,
) {
    Column(
        modifier.fillMaxWidth().clip(MaterialTheme.shapes.small)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (leading != null) {
                leading()
                Spacer(Modifier.width(12.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (subtitle != null) {
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
            if (trailing != null) {
                Spacer(Modifier.width(8.dp))
                trailing()
            }
        }
        below?.invoke(this)
    }
}

/**
 * DS-COMP-19: destaque de uma tela interna (saldo da conta, total da fatura, valor do investimento): o número grande, sem
 * cartão em volta, e uma linha fina embaixo (PRI-04, DS-DIR-01).
 */
@Composable
fun HeroBlock(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth().padding(start = SectionPadding, end = SectionPadding, top = 4.dp, bottom = 16.dp), content = content)
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
}

/**
 * DS-COMP-20: item de lista sem cartão, com uma linha fina embaixo, para itens de várias linhas (orçamento, conta fixa, meta,
 * dívida). Substitui os cartões empilhados da 1.3 (DS-DIR-01).
 */
@Composable
fun FlatItem(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, faded: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth()) {
        Column(
            Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium)
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .then(if (faded) Modifier.background(MaterialTheme.colorScheme.surfaceContainerLowest) else Modifier)
                .padding(horizontal = 4.dp, vertical = 14.dp),
            content = content,
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    }
}

/** Um atalho do Início. */
data class ShortcutItem(val label: String, val icon: ImageVector, val onClick: () -> Unit)

/** DS-COMP-15: fila de atalhos redondos, com rolagem para o lado. */
@Composable
fun Shortcuts(items: List<ShortcutItem>, modifier: Modifier = Modifier) {
    LazyRow(
        modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = SectionPadding - 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items(items, key = { it.label }) { Shortcut(it) }
    }
}

/** Um atalho: círculo de 64 dp com o ícone na cor do Cryo e o nome embaixo. O toque inteiro (círculo e nome) é um botão. */
@Composable
fun Shortcut(item: ShortcutItem) {
    // DS-MOV-08: o círculo encolhe de leve enquanto o dedo está nele.
    val interaction = remember { MutableInteractionSource() }
    Column(
        Modifier.width(80.dp).clip(MaterialTheme.shapes.medium)
            .clickable(interactionSource = interaction, indication = androidx.compose.material3.ripple(), onClick = item.onClick)
            .semantics { role = Role.Button }
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.pressScale(interaction).size(64.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            Icon(item.icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(26.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(
            item.label, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center,
            maxLines = 2, overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * DS-COMP-16: cabeçalho colorido do Início (DS-COR-04). No claro, a cor principal do Cryo; no escuro, a versão escura dela,
 * para não ofuscar. Segue o Material You quando as cores do papel de parede estão ligadas.
 * Enquanto ele está na tela, os ícones da barra de status ficam claros (sobre a cor) e voltam ao normal depois.
 */
@Composable
fun HomeHeader(
    name: String,
    subtitle: String,
    hideValues: Boolean,
    onToggleHide: () -> Unit,
    onSettings: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val dark = CryoTheme.colors.isDark
    val container = if (dark) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.primary
    val onContainer = if (dark) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onPrimary
    val view = LocalView.current
    DisposableEffect(dark) {
        val window = (view.context as? Activity)?.window
        val bars = window?.let { WindowCompat.getInsetsController(it, view) }
        bars?.isAppearanceLightStatusBars = false
        onDispose { bars?.isAppearanceLightStatusBars = !dark }
    }
    Surface(color = container, contentColor = onContainer, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.statusBarsPadding().padding(start = SectionPadding, end = 8.dp, top = 8.dp, bottom = 24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(40.dp).clip(CircleShape).background(onContainer.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center,
                ) {
                    // A inicial do nome, como nos apps de banco; sem nome, um ícone de pessoa.
                    val initial = name.trim().firstOrNull()?.uppercaseChar()
                    if (initial != null) Text(initial.toString(), style = MaterialTheme.typography.titleMedium)
                    else Icon(Icons.Rounded.Person, null, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        if (name.isNotBlank()) stringResource(R.string.home_hello, name.trim()) else stringResource(R.string.home_hello_anonymous),
                        style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.semantics { heading() },
                    )
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = onContainer.copy(alpha = 0.8f))
                }
                IconButton(onClick = onToggleHide) {
                    Icon(
                        if (hideValues) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                        stringResource(if (hideValues) R.string.show_values else R.string.hide_values),
                    )
                }
                IconButton(onClick = onSettings) { Icon(Icons.Rounded.Settings, stringResource(R.string.settings)) }
            }
            Spacer(Modifier.height(20.dp))
            Column(Modifier.padding(end = 12.dp), content = content)
        }
    }
}

/**
 * DS-COMP-17: aviso discreto. Fundo neutro e o ícone na cor do tipo (vermelho atrasado, âmbar vencendo, cor do Cryo para
 * dicas), em vez de uma faixa toda colorida: o Início fica calmo e o que é urgente continua se destacando.
 */
@Composable
fun InfoCard(icon: ImageVector, text: String, tint: Color, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow, shape = MaterialTheme.shapes.medium,
        modifier = modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(36.dp).clip(CircleShape).background(tint.copy(alpha = if (CryoTheme.colors.isDark) 0.28f else 0.14f)),
                contentAlignment = Alignment.Center,
            ) { Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp)) }
            Spacer(Modifier.width(12.dp))
            Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            if (onClick != null) {
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/**
 * DS-COMP-21: pontos de página (passeio, RF-TOUR-05). O da página atual vira uma pílula na cor do Cryo; a troca é animada
 * (DS-MOV-01). O leitor de tela lê "Página 2 de 5".
 */
@Composable
fun PageDots(current: Int, count: Int, modifier: Modifier = Modifier) {
    val desc = Res.s(R.string.tour_page, current + 1, count)
    Row(
        modifier.semantics { contentDescription = desc },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(count) { i ->
            val on = i == current
            val w by animateDpAsState(if (on) 24.dp else 8.dp, tween(Motion.MEDIUM, easing = Motion.Emphasized), label = "dot")
            val color by animateColorAsState(
                if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, tween(Motion.MEDIUM), label = "dotColor",
            )
            Box(Modifier.size(width = w, height = 8.dp).clip(CircleShape).background(color))
        }
    }
}
