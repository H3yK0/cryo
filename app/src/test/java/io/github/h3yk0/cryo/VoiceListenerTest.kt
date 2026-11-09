// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo

import io.github.h3yk0.cryo.voice.AudioInput
import io.github.h3yk0.cryo.voice.Recognizer
import io.github.h3yk0.cryo.voice.VoicePhase
import io.github.h3yk0.cryo.voice.VoiceProblem
import io.github.h3yk0.cryo.voice.VoiceUi
import io.github.h3yk0.cryo.voice.WhisperListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.PI
import kotlin.math.sin

/**
 * RF-VOZ-02/03/09: a escuta com um microfone falso (um áudio pronto) e um Whisper falso. Confere quando a fala começa e
 * termina, as palavras ao vivo, o resultado final, o "não ouvi nada", o cancelamento e que o modelo e o microfone são
 * sempre soltos. Como o tempo é contado em áudio, cada caso dá sempre o mesmo resultado.
 */
class VoiceListenerTest {
    private val rate = 16_000

    /** Silêncio com um chiado baixo, "sílabas" (rajadas de som) e silêncio de novo. */
    private fun speech(leadSec: Double, syllables: Int, tailSec: Double): ShortArray {
        val out = ArrayList<Short>()
        val rnd = java.util.Random(7)
        fun noise(sec: Double) = repeat((sec * rate).toInt()) { out += (rnd.nextGaussian() * 30).toInt().toShort() }
        noise(leadSec)
        repeat(syllables) {
            val len = (0.16 * rate).toInt()
            for (i in 0 until len) {
                val env = sin(PI * i / len)
                out += (env * 9000 * sin(2 * PI * 180 * i / rate) + rnd.nextGaussian() * 30).toInt().toShort()
            }
            noise(0.07)
        }
        noise(tailSec)
        return out.toShortArray()
    }

    /** [paced] = lê no ritmo de um microfone (mais rápido, mas sem despejar tudo de uma vez). */
    private class FakeMic(private val audio: ShortArray, private val ok: Boolean = true, private val paced: Boolean = false) : AudioInput {
        var pos = 0
        var started = false
        var stopped = false
        override fun start() = ok.also { started = it }
        override fun read(buf: ShortArray): Int {
            if (pos >= audio.size) return 0
            if (paced) Thread.sleep(1)
            val n = minOf(buf.size, audio.size - pos)
            System.arraycopy(audio, pos, buf, 0, n)
            pos += n
            return n
        }
        override fun stop() { stopped = true }
    }

    private class FakeWhisper(private val live: String, private val final: String) : Recognizer {
        val liveCalls = AtomicInteger()
        val finalCalls = AtomicInteger()
        var lastSamples = 0
        @Volatile var closed = false
        @Volatile var aborted = false
        override fun transcribe(pcm: FloatArray, samples: Int, live: Boolean): String? {
            if (aborted) return null
            if (live) { liveCalls.incrementAndGet(); return this.live }
            finalCalls.incrementAndGet()
            lastSamples = samples
            return final
        }
        override fun abort() { aborted = true }
        override fun close() { closed = true }
    }

    private fun run(mic: FakeMic, rec: FakeWhisper?, act: (WhisperListener) -> Unit = {}): VoiceUi = runBlocking {
        val l = WhisperListener(CoroutineScope(SupervisorJob() + Dispatchers.IO), mic, { rec })
        l.start()
        act(l)
        withTimeout(10_000) { l.ui.first { it.phase == VoicePhase.DONE || it.phase == VoicePhase.FAILED } }
    }

    @Test fun ouveEntendeETerminaSozinho() {
        val audio = speech(0.6, 14, 3.0)
        val mic = FakeMic(audio)
        val rec = FakeWhisper(live = "Uber 18", final = " Uber, 18 e 90.")
        val ui = run(mic, rec)
        assertEquals(VoicePhase.DONE, ui.phase)
        assertEquals("Uber, 18 e 90", ui.text)
        assertTrue("sílabas: ${ui.syllables}", ui.syllables in 8..20)
        assertEquals(1, rec.finalCalls.get())
        assertTrue("releituras ao vivo: ${rec.liveCalls.get()}", rec.liveCalls.get() >= 1)
        // Terminou ~1,2 s depois da fala, sem ler o áudio todo; e o Whisper recebeu só a fala (+ meio segundo antes).
        assertTrue(mic.pos < audio.size)
        assertTrue("amostras: ${rec.lastSamples}", rec.lastSamples < (0.5 + 14 * 0.23 + 1.4) * rate)
        assertTrue(mic.stopped)
        assertTrue(rec.closed)
    }

    @Test fun silencioViraNaoOuviNada() {
        val mic = FakeMic(speech(9.0, 0, 0.0))
        val rec = FakeWhisper("", "Legendas pela comunidade Amara.org")
        val ui = run(mic, rec)
        assertEquals(VoicePhase.DONE, ui.phase)
        assertEquals(VoiceProblem.NOTHING, ui.problem)
        assertEquals(0, rec.finalCalls.get())
        assertTrue(rec.closed)
    }

    @Test fun alucinacaoDoWhisperViraNaoOuviNada() {
        val ui = run(FakeMic(speech(0.5, 6, 2.0)), FakeWhisper("", "Obrigado por assistir."))
        assertEquals(VoiceProblem.NOTHING, ui.problem)
        assertEquals("", ui.text)
    }

    @Test fun microfoneOcupado() {
        val rec = FakeWhisper("", "")
        val ui = run(FakeMic(ShortArray(0), ok = false), rec)
        assertEquals(VoicePhase.FAILED, ui.phase)
        assertEquals(VoiceProblem.MIC, ui.problem)
        assertTrue(rec.closed)
    }

    @Test fun semWhisperAvisaParaUsarOutroOuvido() {
        val ui = run(FakeMic(speech(0.5, 6, 2.0)), null)
        assertEquals(VoicePhase.FAILED, ui.phase)
        assertEquals(VoiceProblem.ENGINE, ui.problem)
    }

    @Test fun prontoTerminaNaHora() {
        val audio = speech(0.5, 60, 2.0)
        val mic = FakeMic(audio, paced = true)
        val rec = FakeWhisper("", "mercado 120")
        val ui = run(mic, rec) { l -> while (mic.pos < rate * 2) Thread.sleep(1); l.finish() }
        assertEquals("mercado 120", ui.text)
        assertTrue(mic.pos < audio.size)
    }

    @Test fun cancelarSoltaTudo() = runBlocking {
        val mic = FakeMic(speech(0.5, 200, 2.0), paced = true)
        val rec = FakeWhisper("", "nunca")
        val l = WhisperListener(CoroutineScope(SupervisorJob() + Dispatchers.IO), mic, { rec })
        l.start()
        while (mic.pos < rate) Thread.sleep(1)
        l.cancel()
        withTimeout(5_000) { while (!rec.closed || !mic.stopped) Thread.sleep(2) }
        assertFalse(l.ui.value.phase == VoicePhase.DONE)
        assertEquals(0, rec.finalCalls.get())
    }
}
