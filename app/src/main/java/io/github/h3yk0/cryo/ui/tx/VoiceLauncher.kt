// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.ui.tx

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import io.github.h3yk0.cryo.R
import io.github.h3yk0.cryo.domain.AppLocale
import io.github.h3yk0.cryo.domain.Ledger
import io.github.h3yk0.cryo.domain.PhraseParser
import io.github.h3yk0.cryo.domain.Res
import io.github.h3yk0.cryo.ui.components.CryoButton
import io.github.h3yk0.cryo.ui.components.LocalContainer
import io.github.h3yk0.cryo.ui.components.LocalSettings
import io.github.h3yk0.cryo.voice.SystemListener
import io.github.h3yk0.cryo.voice.VoiceEngineKind
import io.github.h3yk0.cryo.voice.VoiceSettings
import io.github.h3yk0.cryo.voice.WhisperEngine
import io.github.h3yk0.cryo.voice.WhisperModelStore
import io.github.h3yk0.cryo.ui.screens.VoiceModelNeededSheet
import io.github.h3yk0.cryo.ui.components.CryoSheet

/** O que o botão de voz sabe fazer: ouvir (toque) e abrir o campo de digitar (toque longo). */
class VoiceLauncher(val listen: () -> Unit, val type: () -> Unit)

/**
 * RF-ALC-01/02 e RF-VOZ: tudo o que a voz precisa (o pedido do microfone com explicação, a tela de eco, o plano B da tela do
 * sistema e a conferência do que foi entendido), num lugar só. Quem chama (o botão do meio da barra, o aviso "Registrar
 * agora") só decide quando ouvir.
 */
@Composable
fun rememberVoiceLauncher(l: Ledger?): VoiceLauncher {
    val c = LocalContainer.current
    val settings = LocalSettings.current
    val context = LocalContext.current
    var draft by remember { mutableStateOf<TxDraft?>(null) }
    var typing by remember { mutableStateOf(false) }
    val voiceSettings = remember { VoiceSettings(context) }
    var listening by remember { mutableStateOf<VoiceEngineKind?>(null) }
    var askMic by remember { mutableStateOf(false) }
    // RF-VOZ-15 (1.6): o Whisper sem o modelo instalado pede o arquivo antes de ouvir.
    var needModel by remember { mutableStateOf(false) }
    // Com qual ouvido começar quando o microfone for liberado (o Whisper recém-instalado ou o "Agora não" do celular).
    var pendingKind by remember { mutableStateOf<VoiceEngineKind?>(null) }

    fun submit(t: String) {
        if (t.isBlank() || l == null) return
        draft = TxDraft.fromParsed(PhraseParser(l.parseContext(settings)).parse(t))
    }

    val legacy = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        if (res.resultCode == Activity.RESULT_OK) {
            res.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.let { submit(it) }
        }
    }

    /** A tela de voz do sistema (como na 1.5): para celulares sem o Whisper e sem reconhecimento offline. */
    fun legacyListen() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, AppLocale.locale.toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PROMPT, Res.s(R.string.quick_fale_por_exemplo))
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
        }
        try {
            legacy.launch(intent)
        } catch (_: ActivityNotFoundException) {
            c.message(Res.s(R.string.quick_este_aparelho_nao_tem))
        }
    }

    val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        askMic = false
        if (granted) listening = pendingKind ?: voiceSettings.engine
        else { c.message(Res.s(R.string.voice_mic_denied)); legacyListen() }
        pendingKind = null
    }

    /** Começa a ouvir com [kind], pedindo o microfone antes se ainda não pode. */
    fun start(kind: VoiceEngineKind) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) listening = kind
        else { listening = null; askMic = true; pendingKind = kind }
    }

    fun listen() {
        if (l == null) return
        c.guideCount(io.github.h3yk0.cryo.domain.GuideCount.VOICE)
        val engine = voiceSettings.engine
        val inApp = (engine == VoiceEngineKind.WHISPER && WhisperEngine.supported) || SystemListener.available(context)
        if (!inApp) return legacyListen()
        val kind = if (engine == VoiceEngineKind.PHONE && !SystemListener.available(context)) VoiceEngineKind.WHISPER else engine
        // RF-VOZ-15: o Whisper precisa do modelo que a pessoa baixa uma vez (desde a 1.6 ele não vem no app).
        if (kind == VoiceEngineKind.WHISPER && WhisperModelStore(context).installed() == null) { needModel = true; return }
        start(kind)
    }

    if (l != null) {
        draft?.let { d -> QuickConfirmSheet(d, l, onDismiss = { draft = null }, onSaved = { draft = null }) }
        if (typing) TypeSheet(onDismiss = { typing = false }, onSend = { typing = false; c.guideCount(io.github.h3yk0.cryo.domain.GuideCount.PHRASE); submit(it) })
        if (askMic) MicPermissionSheet(onDismiss = { askMic = false; pendingKind = null }, onAllow = { micPermission.launch(Manifest.permission.RECORD_AUDIO) })
        if (needModel) VoiceModelNeededSheet(
            onDismiss = { needModel = false },
            onInstalled = { needModel = false; start(VoiceEngineKind.WHISPER) },
            // "Agora não": desta vez o reconhecedor do celular (só no aparelho), ou a tela de voz do sistema.
            onUsePhone = { needModel = false; if (SystemListener.available(context)) start(VoiceEngineKind.PHONE) else legacyListen() },
        )
        listening?.let { kind -> VoiceSheet(l, kind, onDismiss = { listening = null }, onLegacy = { listening = null; legacyListen() }) }
    }
    // Um novo a cada composição: o "ouvir" sempre enxerga o Ledger de agora.
    return VoiceLauncher(listen = { listen() }, type = { typing = true })
}

/** RF-ALC-02: o toque longo no botão de voz abre o campo de digitar a frase, já com o teclado. */
@Composable
private fun TypeSheet(onDismiss: () -> Unit, onSend: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    CryoSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.imePadding().padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
            Text(Res.s(R.string.bar_type_title), style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = text, onValueChange = { text = it },
                placeholder = { Text(Res.s(R.string.quick_ex_almoco_25_debito)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { if (text.isNotBlank()) onSend(text) }),
                modifier = Modifier.fillMaxWidth().focusRequester(focus),
            )
            Spacer(Modifier.height(12.dp))
            CryoButton(onClick = { onSend(text) }, enabled = text.isNotBlank(), modifier = Modifier.fillMaxWidth().height(54.dp)) {
                Text(Res.s(R.string.debt_registrar))
            }
        }
    }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
}
