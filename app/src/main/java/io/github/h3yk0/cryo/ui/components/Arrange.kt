// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DashboardCustomize
import androidx.compose.material.icons.rounded.DragIndicator
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import io.github.h3yk0.cryo.R
import io.github.h3yk0.cryo.domain.Res
import io.github.h3yk0.cryo.domain.ScreenLayout
import kotlin.math.roundToInt

/** RF-ORG-01 (1.7, LA8): um bloco arrumável de uma tela: a chave (guardada), o nome na folha e o que ele põe na lista. */
class Block(val key: String, val label: String, val content: LazyListScope.() -> Unit)

/** RF-ORG-02: os blocos da tela na ordem da pessoa, sem os escondidos. */
fun LazyListScope.arranged(layout: ScreenLayout, blocks: List<Block>) {
    val byKey = blocks.associateBy { it.key }
    layout.visible(blocks.map { it.key }).forEach { key -> byKey[key]?.content?.invoke(this) }
}

/** Um grupo da folha de organizar: uma tela (ou a grade de blocos do Início), com o nome de cada bloco. */
class ArrangeGroup(val screen: String, val title: String?, val items: List<Pair<String, String>>)

/** RF-ORG-01: o botão do fim de cada tela de blocos. */
@Composable
fun OrganizeButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
    TileButton(Res.s(R.string.org_button), Icons.Rounded.DashboardCustomize, modifier, subtitle = Res.s(R.string.org_button_sub), onClick = onClick)
}

/**
 * RF-ORG-02..05: a folha de organizar uma tela. Cada bloco numa linha: segurar e arrastar (ou arrastar pela alça) muda o
 * lugar; o olho esconde ou mostra; "Voltar ao padrão" desfaz tudo. A tela muda na hora, por trás da folha. Para o leitor de
 * tela, cada linha tem as ações "Subir" e "Descer".
 */
@Composable
fun ArrangeSheet(groups: List<ArrangeGroup>, onDismiss: () -> Unit) {
    val c = LocalContainer.current
    CryoSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(start = 20.dp, end = 20.dp, bottom = 24.dp).verticalScroll(rememberScrollState())) {
            Text(Res.s(R.string.org_title), style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(4.dp))
            Text(Res.s(R.string.org_hint), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            groups.forEach { g ->
                if (g.title != null && groups.size > 1) GroupTitle(g.title)
                ArrangeList(g)
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { groups.forEach { g -> c.launch { c.settings.setLayout(g.screen, ScreenLayout()) } } }) {
                    Text(Res.s(R.string.org_reset))
                }
                Spacer(Modifier.weight(1f))
                CryoButton(onClick = onDismiss) { Text(Res.s(R.string.org_done)) }
            }
        }
    }
}

private val ROW = 56.dp

@Composable
private fun ArrangeList(group: ArrangeGroup) {
    val c = LocalContainer.current
    val layout = LocalSettings.current.layout(group.screen)
    val defaults = group.items.map { it.first }
    val labels = group.items.toMap()
    val keys = layout.arrange(defaults)
    val haptics = rememberHaptics()
    val rowPx = with(LocalDensity.current) { ROW.toPx() }
    // Os gestos leem a arrumação de agora (eles não reiniciam a cada mudança).
    val layoutNow by rememberUpdatedState(layout)
    val keysNow by rememberUpdatedState(keys)
    var dragKey by remember { mutableStateOf<String?>(null) }
    var dragY by remember { mutableFloatStateOf(0f) }
    val lastMsg = Res.s(R.string.org_last)
    fun save(next: ScreenLayout?) {
        if (next == null) c.message(lastMsg) else c.launch { c.settings.setLayout(group.screen, next) }
    }
    fun start(key: String) { haptics.tap(); dragKey = key; dragY = 0f }
    fun end() {
        val key = dragKey ?: return
        val from = keysNow.indexOf(key)
        val to = (from + (dragY / rowPx).roundToInt()).coerceIn(0, keysNow.lastIndex)
        if (from >= 0 && to != from) save(layoutNow.moved(defaults, from, to))
        dragKey = null
        dragY = 0f
    }
    fun cancel() { dragKey = null; dragY = 0f }
    val from = dragKey?.let { keys.indexOf(it) } ?: -1
    val to = if (from < 0) -1 else (from + (dragY / rowPx).roundToInt()).coerceIn(0, keys.lastIndex)
    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        keys.forEachIndexed { i, key ->
            val hidden = key in layout.hidden
            val dragging = key == dragKey
            val target = when {
                from < 0 || dragging -> 0f
                from < to && i in (from + 1)..to -> -rowPx
                from > to && i in to until from -> rowPx
                else -> 0f
            }
            val shift by animateFloatAsState(target, label = "orgShift")
            val label = labels[key] ?: key
            val up = Res.s(R.string.org_up)
            val down = Res.s(R.string.org_down)
            Row(
                Modifier.fillMaxWidth().height(ROW)
                    .zIndex(if (dragging) 1f else 0f)
                    .graphicsLayer {
                        translationY = if (dragging) dragY else shift
                        shadowElevation = if (dragging) 8.dp.toPx() else 0f
                    }
                    .clip(MaterialTheme.shapes.medium)
                    .background(if (dragging) MaterialTheme.colorScheme.surfaceContainerHighest else Color.Transparent)
                    .pointerInput(key) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { start(key) },
                            onDrag = { ch, d -> ch.consume(); dragY += d.y },
                            onDragEnd = { end() },
                            onDragCancel = { cancel() },
                        )
                    }
                    .semantics(mergeDescendants = true) {
                        customActions = buildList {
                            if (i > 0) add(CustomAccessibilityAction(up) { save(layoutNow.moved(defaults, i, i - 1)); true })
                            if (i < keys.lastIndex) add(CustomAccessibilityAction(down) { save(layoutNow.moved(defaults, i, i + 1)); true })
                        }
                    }
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // A alça: arrastar por ela não precisa segurar antes.
                Icon(
                    Icons.Rounded.DragIndicator, null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(40.dp).padding(8.dp).pointerInput(key) {
                        detectDragGestures(
                            onDragStart = { start(key) },
                            onDrag = { ch, d -> ch.consume(); dragY += d.y },
                            onDragEnd = { end() },
                            onDragCancel = { cancel() },
                        )
                    },
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    color = if (hidden) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface,
                )
                IconButton(onClick = { save(layoutNow.toggled(defaults, key)) }) {
                    Icon(
                        if (hidden) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                        Res.s(if (hidden) R.string.org_show else R.string.org_hide, label),
                        tint = if (hidden) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}
