// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.voice

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import io.github.h3yk0.cryo.domain.AppLocale
import io.github.h3yk0.cryo.domain.Spoken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/**
 * Plano B (RF-VOZ-01): o reconhecedor do próprio celular, **só no aparelho** (Android 12+ com o pacote de voz offline).
 * Usado quando o celular não roda o Whisper ou quando a pessoa escolhe em Ajustes. Entrega o mesmo estado que o Whisper,
 * então a tela de eco é a mesma. Tem que ser criado e usado na linha principal (regra do Android).
 */
class SystemListener(private val context: Context) : Listener {
    override val kind = VoiceEngineKind.PHONE
    private val state = MutableStateFlow(VoiceUi())
    override val ui: StateFlow<VoiceUi> = state
    private var recognizer: SpeechRecognizer? = null
    private val meter = VoiceMeter()

    override fun start() {
        val r = create(context)
        if (r == null) {
            state.update { it.copy(phase = VoicePhase.FAILED, problem = VoiceProblem.ENGINE) }
            return
        }
        recognizer = r
        r.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) = state.update { it.copy(phase = VoicePhase.LISTENING) }
            override fun onBeginningOfSpeech() = Unit
            override fun onRmsChanged(rmsdB: Float) {
                // O volume do reconhecedor vai de ~-2 a ~10.
                meter.feedLevel(((rmsdB + 2f) / 12f).coerceIn(0f, 1f))
                state.update { it.copy(level = meter.level, syllables = meter.syllables, strength = meter.strength) }
            }
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() = state.update { it.copy(phase = VoicePhase.THINKING, level = 0f) }
            override fun onError(error: Int) {
                val nothing = error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT
                val mic = error == SpeechRecognizer.ERROR_AUDIO || error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS
                state.update {
                    when {
                        nothing -> it.copy(phase = VoicePhase.DONE, problem = VoiceProblem.NOTHING, text = "", level = 0f)
                        mic -> it.copy(phase = VoicePhase.FAILED, problem = VoiceProblem.MIC, level = 0f)
                        else -> it.copy(phase = VoicePhase.FAILED, problem = VoiceProblem.ENGINE, level = 0f)
                    }
                }
                release()
            }
            override fun onResults(results: Bundle?) {
                val t = Spoken.clean(results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty())
                state.update { it.copy(phase = VoicePhase.DONE, text = t, problem = if (t.isEmpty()) VoiceProblem.NOTHING else null, level = 0f) }
                release()
            }
            override fun onPartialResults(partialResults: Bundle?) {
                val t = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let(Spoken::clean).orEmpty()
                if (t.isNotEmpty()) state.update { if (it.phase == VoicePhase.LISTENING) it.copy(text = t) else it }
            }
            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        })
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, AppLocale.locale.toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
        }
        runCatching { r.startListening(intent) }.onFailure {
            state.update { it.copy(phase = VoicePhase.FAILED, problem = VoiceProblem.ENGINE) }
            release()
        }
    }

    override fun finish() { runCatching { recognizer?.stopListening() } }

    override fun cancel() {
        runCatching { recognizer?.cancel() }
        release()
    }

    private fun release() {
        recognizer?.let { runCatching { it.destroy() } }
        recognizer = null
    }

    companion object {
        /** O celular tem reconhecimento de voz que funciona sem internet? (Android 12+.) */
        fun available(context: Context): Boolean =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && runCatching { SpeechRecognizer.isOnDeviceRecognitionAvailable(context) }.getOrDefault(false)

        private fun create(context: Context): SpeechRecognizer? =
            if (available(context)) runCatching { SpeechRecognizer.createOnDeviceSpeechRecognizer(context) }.getOrNull() else null
    }
}
