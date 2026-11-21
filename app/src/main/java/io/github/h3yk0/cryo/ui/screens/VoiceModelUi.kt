// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.ui.screens

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.h3yk0.cryo.R
import io.github.h3yk0.cryo.domain.AppLocale
import io.github.h3yk0.cryo.domain.Res
import io.github.h3yk0.cryo.ui.components.CryoButton
import io.github.h3yk0.cryo.ui.components.FieldLabel
import io.github.h3yk0.cryo.ui.components.LocalContainer
import io.github.h3yk0.cryo.voice.ModelInstall
import io.github.h3yk0.cryo.voice.WhisperModel
import io.github.h3yk0.cryo.voice.WhisperModelStore
import io.github.h3yk0.cryo.voice.WhisperModels
import io.github.h3yk0.cryo.ui.components.ChoiceRow
import io.github.h3yk0.cryo.voice.WhisperEngine
import io.github.h3yk0.cryo.voice.VoiceEngineKind
import io.github.h3yk0.cryo.voice.VoiceSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import io.github.h3yk0.cryo.ui.components.CryoSheet

/*
 * RF-VOZ-15 (1.6): o modelo de voz que a pessoa baixa uma vez. O Cryo continua sem internet: os botões "Baixar" abrem o
 * navegador e o "Já baixei" abre o seletor de arquivos do Android. O arquivo só entra depois de conferido
 * ([WhisperModelStore.install]).
 */

/** Tamanho em MB para mostrar (arredondado), com os algarismos da língua do app (ERR-058). */
internal fun megabytes(bytes: Long): String =
    Res.s(R.string.voice_mb, NumberFormat.getIntegerInstance(AppLocale.locale).format((bytes + 512 * 1024) / (1024 * 1024)))

/** O texto de cada resultado que não deu certo (null quando instalou). */
internal fun installProblem(r: ModelInstall): String? = when (r) {
    is ModelInstall.Installed -> null
    is ModelInstall.NoSpace -> Res.s(R.string.voice_model_no_space, megabytes(r.needed))
    ModelInstall.Incomplete -> Res.s(R.string.voice_model_incomplete)
    ModelInstall.NotAModel -> Res.s(R.string.voice_model_not_model)
    ModelInstall.Unreadable -> Res.s(R.string.voice_model_unreadable)
}

/** A pessoa atualizou o app (não é a primeira instalação): quem vinha da 1.5 tinha a voz pronta e precisa do aviso. */
private fun updatedApp(ctx: Context): Boolean = runCatching {
    val info = ctx.packageManager.getPackageInfo(ctx.packageName, 0)
    info.lastUpdateTime > info.firstInstallTime
}.getOrDefault(false)

/**
 * Escolher o arquivo e conferir: devolve o que dar no [onPick] (abrir o seletor), e enquanto confere [checking] fica
 * ligado. [onDone] recebe o resultado já na linha principal.
 */
@Composable
private fun rememberModelPicker(store: WhisperModelStore, onStart: () -> Unit, onDone: (ModelInstall) -> Unit): () -> Unit {
    val scope = rememberCoroutineScope()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            onStart()
            scope.launch {
                val r = withContext(Dispatchers.IO) { runCatching { store.install(uri) }.getOrDefault(ModelInstall.Unreadable) }
                onDone(r)
            }
        }
    }
    // Qualquer tipo: o seletor não conhece o ".bin" do whisper.cpp (vem como "application/octet-stream" ou nada).
    return { runCatching { launcher.launch(arrayOf("*/*")) } }
}

/**
 * RF-VOZ-15: a folha que aparece ao tocar no microfone sem o modelo instalado. [onInstalled] = deu certo (a escuta começa);
 * [onUsePhone] = "Agora não": desta vez usa o reconhecedor do celular.
 */
@Composable
fun VoiceModelNeededSheet(onDismiss: () -> Unit, onInstalled: () -> Unit, onUsePhone: () -> Unit) {
    val ctx = LocalContext.current
    val c = LocalContainer.current
    val store = remember { WhisperModelStore(ctx) }
    val model = remember { WhisperModels.recommended }
    // O aviso da 1.6 aparece uma vez para quem atualizou (para quem instalou agora a voz sempre foi assim).
    val updated = remember { updatedApp(ctx) && !store.noticeSeen }
    val damaged = remember { store.damaged }
    var checking by remember { mutableStateOf(false) }
    var problem by remember { mutableStateOf<String?>(null) }
    val pick = rememberModelPicker(store, onStart = { checking = true; problem = null }) { r ->
        checking = false
        if (r is ModelInstall.Installed) {
            store.noticeSeen = true
            c.message(Res.s(R.string.voice_model_ok))
            onInstalled()
        } else problem = installProblem(r)
    }
    val dismiss = { store.noticeSeen = true; onDismiss() }
    CryoSheet(onDismissRequest = dismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
            Icon(Icons.Rounded.GraphicEq, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
            Spacer(Modifier.height(12.dp))
            Text(Res.s(R.string.voice_model_needed_title), style = MaterialTheme.typography.titleLarge)
            if (damaged) {
                Spacer(Modifier.height(8.dp))
                Text(Res.s(R.string.voice_model_damaged), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
            }
            if (updated) {
                Spacer(Modifier.height(8.dp))
                Text(Res.s(R.string.voice_model_new_16), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                Res.s(R.string.voice_model_needed_text, megabytes(model.size)),
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(20.dp))
            CryoButton(onClick = { openLink(ctx, model.cryoUrl ?: model.officialUrl) }, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                Text(Res.s(R.string.voice_model_download, megabytes(model.size)))
            }
            TextButton(onClick = { openLink(ctx, model.officialUrl) }, modifier = Modifier.fillMaxWidth()) {
                Text(Res.s(R.string.voice_model_download_official))
            }
            Spacer(Modifier.height(4.dp))
            OutlinedButton(onClick = pick, enabled = !checking, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Text(Res.s(if (checking) R.string.voice_model_checking else R.string.voice_model_pick))
            }
            problem?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(4.dp))
            TextButton(onClick = { store.noticeSeen = true; onUsePhone() }, modifier = Modifier.fillMaxWidth()) {
                Text(Res.s(R.string.voice_model_use_phone))
            }
        }
    }
}

/**
 * TELA-VOZ (RF-VOZ-16, reforma visual da 1.6, LA13-2): Ajustes › Voz. A escolha do ouvido (RF-VOZ-01: o Whisper dentro do
 * Cryo ou o reconhecedor do celular) e, com o Whisper, o modelo de voz (RF-VOZ-15). Em Ajustes, a voz é uma linha só.
 */
@Composable
fun VoiceSettingsScreen() {
    val ctx = LocalContext.current
    val nav = io.github.h3yk0.cryo.ui.LocalNav.current
    val voice = remember { VoiceSettings(ctx) }
    var engine by remember { mutableStateOf(voice.engine) }
    androidx.compose.material3.Scaffold(
        topBar = { io.github.h3yk0.cryo.ui.components.CryoTopBar(Res.s(R.string.settings_voice_title), onBack = nav::back) },
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
    ) { pad ->
        Column(
            Modifier.padding(pad).verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
        ) {
            if (!WhisperEngine.supported) {
                SettingsGroup {
                    Text(
                        Res.s(R.string.settings_voice_unsupported), style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    )
                }
                return@Column
            }
            SettingsGroup {
                listOf(
                    Triple(VoiceEngineKind.WHISPER, R.string.settings_voice_whisper, R.string.settings_voice_whisper_desc),
                    Triple(VoiceEngineKind.PHONE, R.string.settings_voice_phone, R.string.settings_voice_phone_desc),
                ).forEach { (kind, title, desc) ->
                    ChoiceRow(Res.s(title), Res.s(desc), selected = engine == kind) {
                        voice.engine = kind
                        engine = kind
                    }
                }
            }
            // RF-VOZ-15: o modelo só importa para o Whisper.
            if (engine == VoiceEngineKind.WHISPER) {
                Spacer(Modifier.height(12.dp))
                SettingsGroup { VoiceModelSection() }
            }
        }
    }
}

/** RF-VOZ-15: Ajustes › Voz › Modelo de voz (o instalado, escolher outro, remover e a lista aceita). */
@Composable
fun VoiceModelSection() {
    val ctx = LocalContext.current
    val c = LocalContainer.current
    val store = remember { WhisperModelStore(ctx) }
    var installed by remember { mutableStateOf(store.installed()) }
    var checking by remember { mutableStateOf(false) }
    var problem by remember { mutableStateOf<String?>(null) }
    val pick = rememberModelPicker(store, onStart = { checking = true; problem = null }) { r ->
        checking = false
        if (r is ModelInstall.Installed) {
            installed = r.model
            store.noticeSeen = true
            c.message(Res.s(R.string.voice_model_ok))
        } else problem = installProblem(r)
    }
    FieldLabel(Res.s(R.string.settings_voice_model))
    Column(Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
        val m = installed
        Text(
            if (m != null) Res.s(R.string.voice_model_installed, Res.s(m.nameRes), megabytes(m.size)) else Res.s(R.string.voice_model_none),
            style = MaterialTheme.typography.bodyMedium,
        )
        if (m == null && store.damaged) {
            Spacer(Modifier.height(4.dp))
            Text(Res.s(R.string.voice_model_damaged), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = pick, enabled = !checking, modifier = Modifier.fillMaxWidth()) {
            Text(Res.s(if (checking) R.string.voice_model_checking else R.string.voice_model_pick))
        }
        problem?.let {
            Spacer(Modifier.height(4.dp))
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
        if (m != null) {
            TextButton(onClick = {
                store.remove()
                installed = null
                c.message(Res.s(R.string.voice_model_removed))
            }) { Text(Res.s(R.string.voice_model_remove, megabytes(m.size))) }
        }
        Spacer(Modifier.height(12.dp))
        Text(Res.s(R.string.voice_model_choose_title), style = MaterialTheme.typography.titleSmall)
        WhisperModels.ALL.forEach { ModelRow(it, ctx) }
        Spacer(Modifier.height(8.dp))
        Text(Res.s(R.string.voice_model_license), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Um modelo da lista: nome, tamanho, para que serve e de onde baixar (o recomendado também do GitHub do Cryo). */
@Composable
private fun ModelRow(model: WhisperModel, ctx: Context) {
    Column(Modifier.fillMaxWidth().padding(top = 10.dp)) {
        val title = buildString {
            append(Res.s(model.nameRes)).append(" · ").append(megabytes(model.size))
            if (model.recommended) append(" · ").append(Res.s(R.string.voice_model_recommended))
        }
        Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = if (model.recommended) FontWeight.SemiBold else null)
        Text(Res.s(model.descRes), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row {
            model.cryoUrl?.let { url -> TextButton(onClick = { openLink(ctx, url) }) { Text(Res.s(R.string.voice_model_from_cryo)) } }
            TextButton(onClick = { openLink(ctx, model.officialUrl) }) { Text(Res.s(R.string.voice_model_from_hf)) }
        }
    }
}
