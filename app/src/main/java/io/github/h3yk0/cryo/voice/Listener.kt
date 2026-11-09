// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.voice

import io.github.h3yk0.cryo.domain.Spoken
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.log10
import kotlin.math.sqrt

/** Em que pé está a escuta (a tela de eco desenha a partir disso). */
enum class VoicePhase { STARTING, LISTENING, THINKING, DONE, FAILED }

/** Por que não deu: microfone, reconhecedor, ou não houve fala. */
enum class VoiceProblem { MIC, ENGINE, NOTHING }

/** Qual ouvido está trabalhando: o Whisper do Cryo ou o reconhecedor do próprio celular (só no aparelho). */
enum class VoiceEngineKind { WHISPER, PHONE }

/**
 * O estado da escuta: o volume agora (o microfone "respira"), quantas sílabas já soaram (cada uma solta um anel de eco),
 * a força da última e o texto ouvido até aqui.
 */
data class VoiceUi(
    val phase: VoicePhase = VoicePhase.STARTING,
    val level: Float = 0f,
    val syllables: Int = 0,
    val strength: Float = 0f,
    val text: String = "",
    val problem: VoiceProblem? = null,
    /** RF-VOZ-13: do fim da fala até o resultado, em ms (só aparece no modo desenvolvedor). */
    val finalMs: Long? = null,
)

/** Uma escuta: começa, termina quando a pessoa para de falar (ou toca em "Pronto") e entrega o texto. */
interface Listener {
    val kind: VoiceEngineKind
    val ui: StateFlow<VoiceUi>
    fun start()

    /** "Pronto": para de ouvir e entrega o que entendeu. */
    fun finish()

    /** "Cancelar" ou a tela fechou: para tudo e solta o microfone e o modelo. */
    fun cancel()
}

/**
 * Volume e sílabas a partir da energia do som (em dB), com o ruído de fundo medido sozinho. Serve aos dois ouvidos
 * (o do celular entrega o volume pronto).
 */
internal class VoiceMeter {
    private var floor = -60.0
    var level = 0f
        private set
    var syllables = 0
        private set
    var strength = 0f
        private set
    private var armed = true
    private var valley = 0f
    private var top = 0f

    /** Uma fatia de 20 ms. True se é voz (bem acima do ruído de fundo). */
    fun feed(db: Double): Boolean {
        // O ruído de fundo desce rápido e sobe devagar: acompanha o ambiente sem tomar a voz por ruído.
        floor += if (db < floor) (db - floor) * 0.3 else (db - floor) * 0.004
        val target = ((db - floor - 6) / 34).coerceIn(0.0, 1.0).toFloat()
        level += (target - level) * if (target > level) 0.6f else 0.3f
        peaks(level)
        return db > floor + 12 && db > -55
    }

    /** O ouvido do celular já dá o volume (0..1). */
    fun feedLevel(l: Float) {
        level += (l - level) * if (l > level) 0.6f else 0.2f
        peaks(level)
    }

    /**
     * Cada subida do volume depois de uma baixa = uma sílaba. Na fala contínua o som não chega a zerar entre as sílabas:
     * conta a subida em relação ao vale mais recente, não a um limite fixo.
     */
    private fun peaks(l: Float) {
        if (armed) {
            valley = minOf(valley, l)
            if (l > 0.35f && l - valley > 0.18f) {
                syllables++
                strength = l
                armed = false
                top = l
            }
        } else {
            top = maxOf(top, l)
            if (top - l > 0.18f || l < 0.2f) {
                armed = true
                valley = l
            }
        }
    }

    companion object {
        fun db(frame: ShortArray, n: Int): Double {
            if (n <= 0) return -90.0
            var sum = 0.0
            for (i in 0 until n) { val v = frame[i] / 32768.0; sum += v * v }
            val rms = sqrt(sum / n)
            return if (rms < 1e-5) -90.0 else 20 * log10(rms)
        }
    }
}

/**
 * RF-VOZ-03: a escuta com o Whisper. O microfone liga na hora (o modelo carrega ao mesmo tempo); as palavras aparecem
 * enquanto a pessoa fala (o áudio é relido a cada ~0,7 s); termina sozinha ~1,2 s depois que a fala para. Todo o tempo
 * é contado em áudio (amostras), não no relógio: o mesmo áudio dá sempre o mesmo resultado (e os testes são exatos).
 */
class WhisperListener(
    private val scope: CoroutineScope,
    private val input: AudioInput,
    private val openRecognizer: () -> Recognizer?,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) : Listener {
    override val kind = VoiceEngineKind.WHISPER
    private val state = MutableStateFlow(VoiceUi())
    override val ui: StateFlow<VoiceUi> = state

    @Volatile private var finishRequested = false
    @Volatile private var cancelled = false
    @Volatile private var recognizer: Recognizer? = null

    override fun start() {
        scope.launch(io) {
            // Nada aqui pode derrubar o app: um erro vira "não deu" e a tela passa a vez ao reconhecedor do celular.
            runCatching { session() }.onFailure {
                runCatching { input.stop() }
                recognizer?.let { r -> runCatching { r.close() } }
                state.update { it.copy(phase = VoicePhase.FAILED, problem = VoiceProblem.ENGINE, level = 0f) }
            }
        }
    }

    override fun finish() { finishRequested = true }

    override fun cancel() {
        cancelled = true
        recognizer?.abort()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun session() = coroutineScope {
        val loading = async(io) { runCatching(openRecognizer).getOrNull().also { recognizer = it } }
        if (!input.start()) {
            loading.await()?.close()
            state.update { it.copy(phase = VoicePhase.FAILED, problem = VoiceProblem.MIC) }
            return@coroutineScope
        }
        state.update { it.copy(phase = VoicePhase.LISTENING) }
        val audio = FloatArray(MAX_SAMPLES)
        var n = 0
        val frame = ShortArray(FRAME)
        val meter = VoiceMeter()
        var speechFrames = 0
        var started = false
        var silence = 0
        var heard = 0
        var lastLive = 0
        var live: Deferred<Unit>? = null
        try {
            while (!finishRequested && !cancelled) {
                val r = input.read(frame)
                if (r <= 0) break
                heard += r
                // O som de "pode falar" (RF-SOM, 0,18 s) sai pelo alto-falante e entra no microfone: os primeiros 0,3 s não
                // vão para o Whisper (ele ouviria "bipe + fala" e erraria a primeira palavra) e não contam como começo da fala.
                val earcon = heard <= EARCON
                if (!earcon) for (k in 0 until r) if (n < audio.size) audio[n++] = frame[k] / 32768f
                val speech = meter.feed(VoiceMeter.db(frame, r)) && !earcon
                state.update { it.copy(level = meter.level, syllables = meter.syllables, strength = meter.strength) }
                if (speech) { speechFrames++; silence = 0 } else if (started) silence += r
                if (!started) {
                    if (speechFrames >= START_FRAMES) started = true
                    // Antes da fala só fica o último meio segundo (o começo da primeira palavra): menos áudio para o Whisper.
                    else if (n > PREROLL * 2) { System.arraycopy(audio, n - PREROLL, audio, 0, PREROLL); n = PREROLL }
                }
                if (started && silence >= END_SILENCE) break
                if (!started && heard >= NO_SPEECH) break
                if (n >= audio.size) break
                // Ao vivo: relê o áudio acumulado, uma releitura por vez (se o celular for lento, simplesmente relê menos).
                val rec = if (loading.isCompleted) loading.getCompleted() else null
                if (started && rec != null && live?.isCompleted != false && n - lastLive >= LIVE_STEP) {
                    lastLive = n
                    val copy = audio.copyOf(n)
                    live = async(io) {
                        val t = runCatching { rec.transcribe(copy, copy.size, live = true) }.getOrNull()
                        copy.fill(0f)
                        if (t != null && !cancelled) {
                            val clean = Spoken.clean(t)
                            state.update { if (it.phase == VoicePhase.LISTENING && clean.isNotEmpty()) it.copy(text = clean) else it }
                        }
                    }
                }
            }
        } finally {
            input.stop()
        }
        val endedAt = System.nanoTime()
        val rec = loading.await()
        if (cancelled) {
            live?.await()
            audio.fill(0f)
            rec?.close()
            return@coroutineScope
        }
        state.update { it.copy(phase = VoicePhase.THINKING, level = 0f) }
        if (!started || rec == null) {
            live?.await()
            audio.fill(0f)
            rec?.close()
            state.update { it.copy(phase = if (rec == null) VoicePhase.FAILED else VoicePhase.DONE, problem = if (rec == null) VoiceProblem.ENGINE else VoiceProblem.NOTHING, text = "") }
            return@coroutineScope
        }
        live?.await()
        val text = rec.transcribe(audio, n, live = false)
        audio.fill(0f)   // RF-VOZ-02: o áudio não fica guardado
        rec.close()
        recognizer = null
        if (cancelled) return@coroutineScope
        val clean = Spoken.clean(text.orEmpty())
        state.update {
            if (clean.isEmpty()) it.copy(phase = VoicePhase.DONE, problem = VoiceProblem.NOTHING, text = "")
            else it.copy(phase = VoicePhase.DONE, text = clean, problem = null, finalMs = (System.nanoTime() - endedAt) / 1_000_000)
        }
    }

    companion object {
        const val RATE = 16_000
        const val FRAME = 320                      // 20 ms
        const val START_FRAMES = 8                 // 160 ms de voz = começou a falar
        const val END_SILENCE = RATE * 12 / 10     // 1,2 s de silêncio depois da fala = terminou (o valor aprovado na 1.6; 1,0 s arriscava cortar pausas no meio da frase)
        const val NO_SPEECH = RATE * 8             // 8 s sem falar = "não ouvi nada"
        const val LIVE_STEP = RATE * 7 / 10        // relê a cada 0,7 s de áudio novo
        const val PREROLL = RATE / 2
        const val EARCON = RATE * 3 / 10
        const val MAX_SAMPLES = RATE * 28          // a janela do Whisper é de 30 s
    }
}
