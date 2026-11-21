// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.EventRepeat
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.RequestQuote
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import io.github.h3yk0.cryo.R
import io.github.h3yk0.cryo.domain.Inbox
import io.github.h3yk0.cryo.domain.InboxItem
import io.github.h3yk0.cryo.domain.InboxKind
import io.github.h3yk0.cryo.domain.InboxLevel
import io.github.h3yk0.cryo.domain.InboxTarget
import io.github.h3yk0.cryo.domain.Res
import io.github.h3yk0.cryo.domain.predict.MonthEnds
import io.github.h3yk0.cryo.ui.LocalNav
import io.github.h3yk0.cryo.ui.Navigator
import io.github.h3yk0.cryo.ui.Routes
import io.github.h3yk0.cryo.ui.components.CryoTopBar
import io.github.h3yk0.cryo.ui.components.EmptyState
import io.github.h3yk0.cryo.ui.components.GroupTitle
import io.github.h3yk0.cryo.ui.components.LocalContainer
import io.github.h3yk0.cryo.ui.components.LocalSettings
import io.github.h3yk0.cryo.ui.components.ToneNotice
import io.github.h3yk0.cryo.ui.components.rememberEngine
import io.github.h3yk0.cryo.ui.components.rememberLedger
import io.github.h3yk0.cryo.ui.theme.CryoTheme
import io.github.h3yk0.cryo.ui.theme.CryoTone
import io.github.h3yk0.cryo.ui.tx.LoadingBox

/**
 * TELA-CENTRAL (RF-INI-19, reforma visual da 1.6, LA9): os avisos em três grupos, na cor da prioridade (DS-TOM): o que
 * precisa de atenção, os próximos dias e o que o Cryo percebeu. Cada aviso leva à tela certa. Abrir a central marca o que
 * está nela como visto (só o HMAC da chave fica guardado, RF-SEG-02).
 */
@Composable
fun InboxScreen() {
    val l = rememberLedger() ?: return LoadingBox()
    val c = LocalContainer.current
    val nav = LocalNav.current
    val hide = LocalSettings.current.hideValues
    val engine = rememberEngine()
    val end = remember(l) { MonthEnds.of(l) }
    val items = remember(l, engine, hide, end) { Inbox.items(l, engine, end, hide) }
    LaunchedEffect(items.map { it.key }) { c.inbox.markSeen(items.map { it.key }, l.today.toEpochDay()) }

    androidx.compose.material3.Scaffold(
        topBar = { CryoTopBar(Res.s(R.string.inbox_title), onBack = nav::back) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { pad ->
        if (items.isEmpty()) {
            EmptyState(
                Icons.Rounded.TaskAlt, Res.s(R.string.inbox_empty_title), Res.s(R.string.inbox_empty_text),
                Modifier.padding(pad).padding(24.dp),
            )
            return@Scaffold
        }
        val tones = CryoTheme.tones
        LazyColumn(
            Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            for ((level, title) in listOf(
                InboxLevel.ALERT to R.string.inbox_group_attention,
                InboxLevel.ATTENTION to R.string.inbox_group_soon,
                InboxLevel.INFO to R.string.inbox_group_insights,
            )) {
                val group = Inbox.group(items, level)
                if (group.isEmpty()) continue
                item(key = "g$level") { GroupTitle(Res.s(title)) }
                items(group, key = { it.key }) { item ->
                    ToneNotice(
                        inboxIcon(item.kind), item.text, toneOf(item, tones),
                        onClick = item.target?.let { t -> { openTarget(nav, t) } },
                    )
                }
            }
        }
    }
}

/** DS-TOM: a cor de cada aviso (a percepção boa fica verde-menta; as outras, azul-gelo). */
internal fun toneOf(item: InboxItem, tones: io.github.h3yk0.cryo.ui.theme.CryoTones): CryoTone = when (item.level) {
    InboxLevel.ALERT -> tones.alert
    InboxLevel.ATTENTION -> tones.attention
    InboxLevel.INFO -> if (item.positive == true) tones.good else tones.info
}

internal fun inboxIcon(kind: InboxKind): ImageVector = when (kind) {
    InboxKind.BILL -> Icons.Rounded.EventRepeat
    InboxKind.DEBT -> Icons.Rounded.RequestQuote
    InboxKind.INVOICE -> Icons.Rounded.CreditCard
    InboxKind.BUDGET -> Icons.Rounded.PieChart
    InboxKind.MONTH -> Icons.AutoMirrored.Rounded.TrendingDown
    InboxKind.INSIGHT -> Icons.Rounded.Lightbulb
    InboxKind.ANOMALY -> Icons.AutoMirrored.Rounded.TrendingUp
    InboxKind.RECURRING -> Icons.Rounded.Autorenew
}

/** Abre a tela de cada aviso (as abas abrem como aba; o resto empilha). */
internal fun openTarget(nav: Navigator, t: InboxTarget) = when (t) {
    InboxTarget.Bills -> nav.open(Routes.plan(1))
    InboxTarget.Budgets -> nav.open(Routes.plan(0))
    InboxTarget.Insights -> nav.open(Routes.INSIGHTS)
    InboxTarget.Recurring -> nav.go(Routes.RECURRING)
    InboxTarget.Goals -> nav.open(Routes.plan(2))
    is InboxTarget.Category -> nav.open(Routes.txs(t.id))
    is InboxTarget.Debt -> nav.go(Routes.debt(t.id))
    is InboxTarget.Card -> nav.go(Routes.card(t.id))
    is InboxTarget.Tx -> nav.go(Routes.txEdit(t.id))
}
