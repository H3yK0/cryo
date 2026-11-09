// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.voice

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder

/** De onde vem o áudio: o microfone no app; nos testes, um áudio pronto. 16 kHz, mono. */
interface AudioInput {
    /** Liga. False se o microfone não abriu (sem permissão ou em uso por outro app). */
    fun start(): Boolean

    /** Lê até [buf].size amostras (espera o microfone). 0 ou menos = acabou. */
    fun read(buf: ShortArray): Int

    fun stop()
}

/**
 * RF-VOZ-02: o microfone. O áudio só passa pela memória (nunca vira arquivo) e é apagado quando a fala termina.
 * Fonte "reconhecimento de voz": o Android entrega o som com o mínimo de filtros, que é o que o Whisper prefere.
 */
class MicInput : AudioInput {
    private var rec: AudioRecord? = null

    @SuppressLint("MissingPermission") // quem chama confere a permissão antes (VoiceSheet)
    override fun start(): Boolean = runCatching {
        val min = AudioRecord.getMinBufferSize(RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        if (min <= 0) return false
        val r = AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, maxOf(min, RATE))
        if (r.state != AudioRecord.STATE_INITIALIZED) { r.release(); return false }
        r.startRecording()
        if (r.recordingState != AudioRecord.RECORDSTATE_RECORDING) { r.release(); return false }
        rec = r
        true
    }.getOrDefault(false)

    override fun read(buf: ShortArray): Int = rec?.read(buf, 0, buf.size) ?: -1

    override fun stop() {
        rec?.let { runCatching { it.stop() }; it.release() }
        rec = null
    }

    companion object { const val RATE = 16_000 }
}
