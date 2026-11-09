// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.voice

import android.content.res.AssetManager
import io.github.h3yk0.cryo.domain.AppLanguage

/** O ouvido: transforma áudio em texto. O Whisper no app; nos testes, um falso. */
interface Recognizer : AutoCloseable {
    /**
     * [live] = rápido, para as palavras aparecerem enquanto a pessoa fala; senão, o resultado final (mais preciso).
     * Null se foi cancelado ou deu erro.
     */
    fun transcribe(pcm: FloatArray, samples: Int, live: Boolean): String?

    /** Pede para a conta em andamento parar (de qualquer thread). */
    fun abort()
}

/**
 * RF-VOZ-01: o Whisper (modelo "base" multilíngue, dentro do APK). O modelo é carregado ao abrir o microfone e solto ao
 * fechar (RF-VOZ-09): nada do Whisper fica na memória com o microfone fechado.
 */
class WhisperEngine private constructor(
    private val handle: Long,
    private val language: String,
    private val prompt: String?,
) : Recognizer {
    @Volatile private var closed = false

    override fun transcribe(pcm: FloatArray, samples: Int, live: Boolean): String? {
        if (closed) return null
        // Áudio sem voz nenhuma (abaixo de ~-50 dB): o Whisper inventaria uma frase ("O que é isso"). Nem chega a ele.
        if (rms(pcm, samples) < 0.003f) return ""
        // Ao vivo: janela do tamanho da fala (+ folga) e no máximo 48 pedaços de palavra: rápido, e se o Whisper entrar
        // em repetição a conta para logo. O final usa a janela inteira (30 s), que é o que mais acerta.
        val ctx = if (live) ((samples + 319) / 320 + 128).coerceIn(256, 1500) else 0
        val bytes = WhisperNative.nativeTranscribe(handle, pcm, samples, language, prompt, THREADS, live, ctx, if (live) 48 else 0) ?: return null
        val text = String(bytes, Charsets.UTF_8).trim()
        // O próprio Whisper diz quando acha que não houve fala (ruído): o texto é descartado.
        return if (WhisperNative.nativeNoSpeech(handle) > 0.6f && text.length < 40) "" else text
    }

    private fun rms(pcm: FloatArray, n: Int): Float {
        val m = minOf(n, pcm.size)
        if (m <= 0) return 0f
        var sum = 0.0
        for (i in 0 until m) sum += pcm[i] * pcm[i]
        return kotlin.math.sqrt(sum / m).toFloat()
    }

    // "Cancelar" pode chegar no mesmo instante em que a escuta solta o modelo: as duas coisas nunca rodam juntas (pedir
    // para parar um modelo já solto derrubaria o app).
    @Synchronized
    override fun abort() {
        if (!closed) WhisperNative.nativeAbort(handle)
    }

    @Synchronized
    override fun close() {
        if (closed) return
        closed = true
        WhisperNative.nativeClose(handle)
    }

    companion object {
        const val MODEL = "whisper/ggml-base-q5_1.bin"

        /** Os 4 núcleos rápidos dos celulares de hoje (o A55 tem 4 + 4): mais threads só disputariam com a tela. */
        val THREADS: Int = Runtime.getRuntime().availableProcessors().coerceIn(1, 4)

        /** Este celular roda o Whisper? (ARM de 64 bits com fp16 e dotprod.) */
        val supported: Boolean get() = WhisperNative.supported

        /** Abre o modelo (lento: ~0,5 s; chame fora da linha principal). Null se este celular não roda o Whisper. */
        fun open(assets: AssetManager, language: AppLanguage, prompt: String?): WhisperEngine? {
            if (!WhisperNative.loaded) return null
            return openLoaded(assets, language, prompt)
        }

        /** Com a biblioteca já carregada (o teste no computador carrega a dele). */
        internal fun openLoaded(assets: AssetManager, language: AppLanguage, prompt: String?): WhisperEngine? {
            val h = runCatching { WhisperNative.nativeOpen(assets, MODEL) }.getOrDefault(0L)
            return if (h == 0L) null else WhisperEngine(h, language.code, prompt)
        }
    }
}
