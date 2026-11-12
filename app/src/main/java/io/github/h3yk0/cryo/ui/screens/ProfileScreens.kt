// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.github.h3yk0.cryo.R
import io.github.h3yk0.cryo.data.AppSettings
import io.github.h3yk0.cryo.data.Palette
import io.github.h3yk0.cryo.data.ThemeMode
import io.github.h3yk0.cryo.domain.GoalArt
import io.github.h3yk0.cryo.domain.GuidePlanner
import io.github.h3yk0.cryo.domain.Res
import io.github.h3yk0.cryo.ui.LocalNav
import io.github.h3yk0.cryo.ui.Routes
import io.github.h3yk0.cryo.ui.components.AvatarKind
import io.github.h3yk0.cryo.ui.components.Avatars
import io.github.h3yk0.cryo.ui.components.CryoButton
import io.github.h3yk0.cryo.ui.components.CryoTopBar
import io.github.h3yk0.cryo.ui.components.GoalImage
import io.github.h3yk0.cryo.ui.components.LocalContainer
import io.github.h3yk0.cryo.ui.components.LocalSettings
import io.github.h3yk0.cryo.ui.components.ProfileAvatar
import io.github.h3yk0.cryo.ui.components.SwitchRow
import io.github.h3yk0.cryo.ui.components.depthCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * RF-PRF-03 (TELA-PERFIL): o painel que a bolinha do Início abre. No alto, a pessoa; depois os atalhos do dia a dia e, em
 * cada linha, a situação de cada coisa em poucas palavras. A tela de Ajustes de sempre continua inteira em "Todos os ajustes".
 */
@Composable
fun ProfileScreen() {
    val c = LocalContainer.current
    val nav = LocalNav.current
    val s = LocalSettings.current
    val lock by c.lockSettings.flow.collectAsState(initial = null)
    val autoBackup by c.autoBackup.flow.collectAsState(initial = null)
    var editing by rememberSaveable { mutableStateOf(false) }
    val guide = rememberGuide()
    Scaffold(topBar = { CryoTopBar(Res.s(R.string.prf_title), onBack = nav::back) }, contentWindowInsets = WindowInsets(0, 0, 0, 0)) { pad ->
        LazyColumn(
            Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Column(
                    Modifier.fillMaxWidth().depthCard().clickable(role = Role.Button) { editing = true }.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    ProfileAvatar(s, 88.dp)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        if (s.userName.isNotBlank()) Res.s(R.string.home_hello, s.userName.trim()) else Res.s(R.string.home_hello_anonymous),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    TextButton(onClick = { editing = true }) { Text(Res.s(R.string.prf_edit)) }
                }
            }
            item {
                SettingsGroup {
                    SwitchRow(Res.s(R.string.hide_values), s.hideValues, { v -> c.launch { c.settings.setHideValues(v) } }, modifier = Modifier.padding(horizontal = 10.dp))
                    Row(Modifier.padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(Res.s(R.string.settings_tema), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        listOf(ThemeMode.SYSTEM to R.string.settings_automatico, ThemeMode.LIGHT to R.string.settings_claro, ThemeMode.DARK to R.string.settings_escuro).forEach { (mode, label) ->
                            FilterChip(selected = s.themeMode == mode, onClick = { c.launch { c.settings.setTheme(mode) } }, label = { Text(Res.s(label)) })
                        }
                    }
                }
            }
            // Os Ajustes de sempre ficam logo aqui, à vista sem rolar: antes eles estavam a um toque do Início.
            item { SettingsGroup { SettingLink(Icons.Rounded.Settings, Res.s(R.string.prf_all_settings), Res.s(R.string.prf_all_settings_hint)) { nav.go(Routes.SETTINGS) } } }
            item {
                SettingsGroup {
                    SettingLink(
                        Icons.Rounded.Backup, Res.s(R.string.settings_backup_automatico),
                        autoBackup?.let { autoBackupSummary(it) } ?: Res.s(R.string.alerts_off), warning = autoBackup?.hasProblem == true,
                    ) { nav.go(Routes.AUTO_BACKUP) }
                    SettingLink(
                        Icons.Rounded.Lock, Res.s(R.string.prf_lock),
                        Res.s(if (lock?.lockEnabled ?: s.lockEnabled) R.string.prf_on else R.string.alerts_off),
                    ) { nav.go(Routes.SETTINGS) }
                    SettingLink(Icons.Rounded.Notifications, Res.s(R.string.alerts_settings_title), Res.s(R.string.prf_alerts_hint)) { nav.go(Routes.ALERT_SETTINGS) }
                    SettingLink(Icons.Rounded.UploadFile, Res.s(R.string.import_importar_extrato), Res.s(R.string.settings_ofx_csv_planilha_ou)) { nav.go(Routes.import()) }
                }
            }
            item {
                SettingsGroup {
                    // RF-GUI-06: "Aprender o Cryo", com quanto a pessoa já conhece.
                    SettingLink(Icons.Rounded.AutoStories, Res.s(R.string.learn_title), Res.s(R.string.learn_count, GuidePlanner.progress(guide.first, guide.second), GuidePlanner.total)) { nav.go(Routes.LEARN) }
                    SettingLink(Icons.Rounded.School, Res.s(R.string.tour_how_to_use), Res.s(R.string.prf_tour_hint)) { nav.go(Routes.TOUR) }
                }
            }
        }
    }
    if (editing) ProfileEditSheet(s) { editing = false }
}

/** RF-PRF-04: o nome e a bolinha (letra, desenho do Cryo, emoji do teclado ou foto), com a cor. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ProfileEditSheet(s: AppSettings, onDismiss: () -> Unit) {
    val c = LocalContainer.current
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf(s.userName) }
    var avatar by remember { mutableStateOf(s.avatar) }
    var color by remember { mutableIntStateOf(s.avatarColor) }
    val start = remember { Avatars.parse(s.avatar) }
    var emoji by remember { mutableStateOf((start as? AvatarKind.Emoji)?.text.orEmpty()) }
    var tab by remember { mutableIntStateOf(when (start) { is AvatarKind.Drawing -> 1; is AvatarKind.Emoji -> 2; is AvatarKind.Photo -> 3; else -> 0 }) }
    // RF-PRF-01: o seletor de fotos do Android entrega só a foto escolhida, sem o app pedir permissão nenhuma.
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            val ok = withContext(Dispatchers.IO) { Avatars.savePhoto(ctx, uri, c.sandbox) }
            if (ok) avatar = "photo:" + System.currentTimeMillis() else c.message(Res.s(R.string.prf_photo_failed))
        }
    }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.verticalScroll(rememberScrollState()).imePadding().padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProfileAvatar(s.copy(userName = name, avatar = avatar, avatarColor = color), 64.dp)
                Spacer(Modifier.size(16.dp))
                Text(Res.s(R.string.prf_edit), style = MaterialTheme.typography.titleLarge)
            }
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(name, { name = it.take(40) }, label = { Text(Res.s(R.string.prf_name)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(16.dp))
            Text(Res.s(R.string.prf_avatar), style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(R.string.prf_kind_letter, R.string.prf_kind_art, R.string.prf_kind_emoji, R.string.prf_kind_photo).forEachIndexed { i, label ->
                    FilterChip(selected = tab == i, onClick = { tab = i; if (i == 0) avatar = "" }, label = { Text(Res.s(label)) })
                }
            }
            when (tab) {
                1 -> FlowRow(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    GoalArt.entries.forEach { art ->
                        val chosen = avatar == "art:" + art.key
                        Box(
                            Modifier.size(52.dp).clip(CircleShape)
                                .border(if (chosen) 3.dp else 0.dp, if (chosen) MaterialTheme.colorScheme.primary else Color.Transparent, CircleShape)
                                .clickable(role = Role.RadioButton) { avatar = "art:" + art.key },
                            contentAlignment = Alignment.Center,
                        ) { GoalImage(art, if (color != 0) Color(color) else MaterialTheme.colorScheme.primary, 46.dp, Modifier.clip(CircleShape)) }
                    }
                }
                2 -> OutlinedTextField(
                    emoji, { emoji = Avatars.firstGrapheme(it); avatar = if (emoji.isEmpty()) "" else "emoji:$emoji" },
                    label = { Text(Res.s(R.string.prf_emoji_label)) }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                3 -> Column(Modifier.padding(top = 8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) { Text(Res.s(R.string.prf_photo_pick)) }
                        if (avatar.startsWith("photo:")) TextButton(onClick = { avatar = "" }) { Text(Res.s(R.string.prf_photo_remove)) }
                    }
                    Text(Res.s(R.string.prf_photo_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(Res.s(R.string.prf_color), style = MaterialTheme.typography.labelLarge)
            FlowRow(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                (listOf(0) + listOf(0, 2, 4, 5, 7, 8, 9, 13, 14, 15, 18).map { Palette.colors[it] }).forEach { value ->
                    val chosen = color == value
                    Box(
                        Modifier.size(36.dp).clip(CircleShape)
                            .background(if (value == 0) MaterialTheme.colorScheme.primaryContainer else Color(value))
                            .border(if (chosen) 3.dp else 0.dp, if (chosen) MaterialTheme.colorScheme.onSurface else Color.Transparent, CircleShape)
                            .clickable(role = Role.RadioButton) { color = value },
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
            CryoButton(
                onClick = {
                    val keep = avatar
                    c.launch {
                        c.settings.setName(name)
                        c.settings.setAvatar(keep, color)
                        // Sem foto escolhida, o arquivo antigo não fica guardado à toa.
                        if (!keep.startsWith("photo:")) withContext(Dispatchers.IO) { Avatars.photoFile(ctx, c.sandbox).delete() }
                    }
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) { Text(Res.s(R.string.import_pronto)) }
        }
    }
}
