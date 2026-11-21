// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.voice

import io.github.h3yk0.cryo.domain.AppLanguage
import java.io.File
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

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
 * As chamadas da ponte com o whisper.cpp que o [WhisperEngine] usa. Existe para os testes trocarem a ponte de verdade
 * por uma falsa e conferirem a ordem das chamadas entre várias threads (ERR-065, `WhisperEngineGuardTest`).
 */
internal interface WhisperBridge {
    fun transcribe(
        handle: Long, pcm: FloatArray, samples: Int, language: String, prompt: String?,
        threads: Int, greedy: Boolean, audioCtx: Int, maxTokens: Int,
    ): ByteArray?
    fun noSpeech(handle: Long): Float
    fun abort(handle: Long)
    fun close(handle: Long)
}

/** A ponte de verdade (`src/main/cpp/cryo_whisper.cpp`). */
internal object NativeWhisperBridge : WhisperBridge {
    override fun transcribe(
        handle: Long, pcm: FloatArray, samples: Int, language: String, prompt: String?,
        threads: Int, greedy: Boolean, audioCtx: Int, maxTokens: Int,
    ): ByteArray? = WhisperNative.nativeTranscribe(handle, pcm, samples, language, prompt, threads, greedy, audioCtx, maxTokens)
    override fun noSpeech(handle: Long): Float = WhisperNative.nativeNoSpeech(handle)
    override fun abort(handle: Long) = WhisperNative.nativeAbort(handle)
    override fun close(handle: Long) = WhisperNative.nativeClose(handle)
}

/**
 * RF-VOZ-01: o Whisper (modelo multilíngue que a pessoa instalou, RF-VOZ-15). O modelo é carregado ao abrir o microfone e
 * solto ao fechar (RF-VOZ-09): nada do Whisper fica na memória com o microfone fechado.
 *
 * ERR-065 (auditoria da 1.6): a ponte nativa não aguenta ser fechada enquanto alguém a usa (o modelo seria solto debaixo
 * da conta, ou do "não houve fala" logo depois dela, e o app fecharia). Por isso este motor conta as chamadas em
 * andamento: [close] pede para a conta parar, espera todas terminarem e só então solta o modelo, uma vez só; depois de
 * fechar, nenhuma chamada chega à ponte. [abort] ("Cancelar") continua parando a conta em andamento a qualquer momento.
 * Nunca chame [close] de dentro de uma transcrição (ela esperaria a si mesma).
 */
class WhisperEngine internal constructor(
    private val handle: Long,
    private val language: String,
    private val prompt: String?,
    private val bridge: WhisperBridge = NativeWhisperBridge,
) : Recognizer {
    /** Guarda [closed] e [inFlight]; [idle] avisa quando a última chamada em andamento termina. */
    private val lock = ReentrantLock()
    private val idle = lock.newCondition()
    private var closed = false
    private var inFlight = 0

    override fun transcribe(pcm: FloatArray, samples: Int, live: Boolean): String? {
        if (!enter()) return null
        try {
            // Áudio sem voz nenhuma (abaixo de ~-50 dB): o Whisper inventaria uma frase ("O que é isso"). Nem chega a ele.
            if (rms(pcm, samples) < 0.003f) return ""
            // Ao vivo: janela do tamanho da fala (+ folga) e no máximo 48 pedaços de palavra: rápido, e se o Whisper entrar
            // em repetição a conta para logo. O final usa a janela inteira (30 s), que é o que mais acerta.
            // RF-VOZ-13: o final fica com a janela inteira. A janela curta seria umas 4 vezes mais rápida, mas no teste com o
            // modelo de verdade ela ouviu "32,40" onde a pessoa disse "32,50": valor errado não compensa a pressa.
            return if (live) run(pcm, samples, greedy = true, ctx = shortWindow(samples), maxTokens = 48)
            else run(pcm, samples, greedy = false, ctx = 0, maxTokens = 0)
        } finally {
            leave()
        }
    }

    /** As duas chamadas (o texto e o "não houve fala") ficam dentro da mesma vez: fechar espera as duas. */
    private fun run(pcm: FloatArray, samples: Int, greedy: Boolean, ctx: Int, maxTokens: Int): String? {
        val bytes = bridge.transcribe(handle, pcm, samples, language, prompt, THREADS, greedy, ctx, maxTokens) ?: return null
        val text = String(bytes, Charsets.UTF_8).trim()
        // O próprio Whisper diz quando acha que não houve fala (ruído): o texto é descartado.
        return if (bridge.noSpeech(handle) > 0.6f && text.length < 40) "" else text
    }

    /** Uma chamada começa a usar o modelo (false = já fechado: a chamada não acontece). */
    private fun enter(): Boolean = lock.withLock {
        if (closed) false else { inFlight++; true }
    }

    private fun leave() = lock.withLock {
        inFlight--
        if (inFlight == 0) idle.signalAll()
    }

    private fun rms(pcm: FloatArray, n: Int): Float {
        val m = minOf(n, pcm.size)
        if (m <= 0) return 0f
        var sum = 0.0
        for (i in 0 until m) sum += pcm[i] * pcm[i]
        return kotlin.math.sqrt(sum / m).toFloat()
    }

    // "Cancelar" pode chegar no mesmo instante em que a escuta solta o modelo: as duas coisas nunca rodam juntas (pedir
    // para parar um modelo já solto derrubaria o app). Pedir para parar é só uma marca: não espera a conta.
    override fun abort() {
        lock.withLock { if (!closed) bridge.abort(handle) }
    }

    override fun close() {
        lock.withLock {
            if (closed) return
            closed = true
            // A conta em andamento para no próximo passo (o Whisper confere a cada pedaço de áudio) e devolve a vez.
            if (inFlight > 0) bridge.abort(handle)
            // Sem ceder a interrupções: fechar no meio do caminho deixaria o modelo (dezenas de MB) preso na memória.
            while (inFlight > 0) idle.awaitUninterruptibly()
            bridge.close(handle)
        }
    }

    companion object {
        /** Os 4 núcleos rápidos dos celulares de hoje (o A55 tem 4 + 4): mais threads só disputariam com a tela. */
        val THREADS: Int = Runtime.getRuntime().availableProcessors().coerceIn(1, 4)

        /** A janela do tamanho da fala para as palavras ao vivo (cada unidade = 20 ms de áudio), com 2,5 s de folga. */
        internal fun shortWindow(samples: Int) = ((samples + 319) / 320 + 128).coerceIn(256, 1500)

        /** Este celular roda o Whisper? (ARM de 64 bits com fp16 e dotprod.) */
        val supported: Boolean get() = WhisperNative.supported

        /** A biblioteca carregou (então um modelo que não abre é culpa do arquivo, não do celular). */
        val loaded: Boolean get() = WhisperNative.loaded

        /**
         * Abre o modelo [model] (lento: ~0,5 s; chame fora da linha principal). Null se este celular não roda o Whisper ou
         * se o arquivo não abriu.
         */
        fun open(model: File, language: AppLanguage, prompt: String?, flash: Boolean = false): WhisperEngine? {
            if (!WhisperNative.loaded) return null
            return openLoaded(model, language, prompt, flash)
        }

        /** Com a biblioteca já carregada (o teste no computador carrega a dele). */
        internal fun openLoaded(model: File, language: AppLanguage, prompt: String?, flash: Boolean = false): WhisperEngine? {
            val h = runCatching { WhisperNative.nativeOpenFile(model.absolutePath, flash) }.getOrDefault(0L)
            // O Whisper conhece as línguas pelo código de duas letras ("pt", "zh", "he"), sem o país.
            return if (h == 0L) null else WhisperEngine(h, language.code, prompt)
        }
    }
}
