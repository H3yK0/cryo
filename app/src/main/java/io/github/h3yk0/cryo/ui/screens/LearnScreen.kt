// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.h3yk0.cryo.R
import io.github.h3yk0.cryo.domain.GuideFacts
import io.github.h3yk0.cryo.domain.GuideLevel
import io.github.h3yk0.cryo.domain.GuideLog
import io.github.h3yk0.cryo.domain.GuidePlanner
import io.github.h3yk0.cryo.domain.Lesson
import io.github.h3yk0.cryo.domain.Res
import io.github.h3yk0.cryo.ui.LocalNav
import io.github.h3yk0.cryo.ui.components.CryoTopBar
import io.github.h3yk0.cryo.ui.components.LessonIcon
import io.github.h3yk0.cryo.ui.components.LocalContainer
import io.github.h3yk0.cryo.ui.components.LocalGuideSpots
import io.github.h3yk0.cryo.ui.components.SwitchRow
import io.github.h3yk0.cryo.ui.components.depthCard
import io.github.h3yk0.cryo.ui.components.guideFacts
import io.github.h3yk0.cryo.ui.components.rememberToday
import io.github.h3yk0.cryo.ui.components.showLesson
import io.github.h3yk0.cryo.ui.components.textRes
import io.github.h3yk0.cryo.ui.components.titleRes

/** O que a tela "Aprender o Cryo" e a linha do perfil precisam: os fatos de agora e o caderninho do guia. */
@Composable
internal fun rememberGuide(): Pair<GuideFacts, GuideLog> {
    val c = LocalContainer.current
    val today = rememberToday()
    val snap by c.repo.snapshot.collectAsStateWithLifecycle()
    val version by c.guide.version.collectAsState()
    val lock by c.lockSettings.flow.collectAsState(initial = null)
    val backup by c.autoBackup.flow.collectAsState(initial = null)
    var facts by remember { mutableStateOf(GuideFacts()) }
    LaunchedEffect(snap, today, lock, backup) { facts = guideFacts(c, today) }
    val log = remember(version) { c.guide.log() }
    return facts to log
}

/**
 * RF-GUI-06 (TELA-APRENDER): tudo o que o Cryo faz, em seis degraus, com o que a pessoa já usa marcado. Tocar numa lição
 * mostra onde ela fica, mesmo que já esteja feita. Aqui também mora a chave "Dicas do Cryo".
 */
@Composable
fun LearnScreen() {
    val c = LocalContainer.current
    val nav = LocalNav.current
    val spots = LocalGuideSpots.current
    val (facts, log) = rememberGuide()
    val known = GuidePlanner.progress(facts, log)
    val total = GuidePlanner.total
    Scaffold(topBar = { CryoTopBar(Res.s(R.string.learn_title), onBack = nav::back) }, contentWindowInsets = WindowInsets(0, 0, 0, 0)) { pad ->
        LazyColumn(
            Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "top") {
                Row(Modifier.fillMaxWidth().depthCard().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.clearAndSetSemantics { }) {
                        // Anel parado (progresso definido): não é animação sem fim (DS-MOV-10).
                        CircularProgressIndicator(
                            progress = { known.toFloat() / total },
                            modifier = Modifier.size(72.dp), strokeWidth = 7.dp,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        )
                        Text(known.toString(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    }
                    Column(Modifier.padding(start = 16.dp)) {
                        Text(Res.s(R.string.learn_progress, known, total), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(Res.s(R.string.learn_intro), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
            item(key = "tips") {
                SettingsGroup {
                    SwitchRow(Res.s(R.string.learn_tips), log.enabled, { c.guide.enabled = it }, subtitle = Res.s(R.string.learn_tips_hint), modifier = Modifier.padding(horizontal = 10.dp))
                }
            }
            GuideLevel.entries.forEach { level ->
                item(key = level.name) {
                    Column {
                        Text(
                            Res.s(level.titleRes()), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 12.dp, top = 4.dp, bottom = 6.dp),
                        )
                        SettingsGroup {
                            Lesson.entries.filter { it.level == level }.forEach { l ->
                                LessonRow(l, done = GuidePlanner.known(l, facts, log)) {
                                    c.guide.markSeen(l)
                                    showLesson(l, nav, spots)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LessonRow(l: Lesson, done: Boolean, onClick: () -> Unit) {
    val state = if (done) Res.s(R.string.learn_done) else ""
    Row(
        Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).clickable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) { if (done) stateDescription = state }
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LessonIcon(l, 40.dp)
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(Res.s(l.titleRes()), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(Res.s(l.textRes()), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (done) Icon(Icons.Rounded.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
    }
}
