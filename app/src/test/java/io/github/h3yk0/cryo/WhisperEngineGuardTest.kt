// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo

import io.github.h3yk0.cryo.voice.WhisperBridge
import io.github.h3yk0.cryo.voice.WhisperEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread
import kotlin.random.Random

/**
 * ERR-065 (auditoria da 1.6): o ciclo de vida do Whisper com várias threads. Uma ponte falsa faz o papel do whisper.cpp e
 * marca toda chamada que chega **depois** de fechar: na ponte de verdade, cada uma delas mexeria num modelo já solto e o
 * app fecharia. Antes da correção, fechar no meio de uma conta soltava o modelo na hora (o primeiro teste falhava).
 */
class WhisperEngineGuardTest {
    /** Uma ponte que espera ser liberada (ou cancelada) e anota a ordem das chamadas. */
    private class FakeBridge(private val hold: Long = 0) : WhisperBridge {
        val events = ConcurrentLinkedQueue<String>()
        val afterClose = AtomicInteger()
        val closes = AtomicInteger()
        val aborted = AtomicBoolean(false)
        val started = CountDownLatch(1)
        @Volatile var release = CountDownLatch(0)
        @Volatile var onTranscribed: (() -> Unit)? = null
        @Volatile private var closed = false

        private fun mark(op: String) {
            if (closed) afterClose.incrementAndGet()
            events += op
        }

        override fun transcribe(
            handle: Long, pcm: FloatArray, samples: Int, language: String, prompt: String?,
            threads: Int, greedy: Boolean, audioCtx: Int, maxTokens: Int,
        ): ByteArray? {
            mark("transcribe")
            started.countDown()
            val limit = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
            while (!aborted.get() && release.count > 0 && System.nanoTime() < limit) Thread.sleep(1)
            if (hold > 0) Thread.sleep(hold)
            mark("transcribe-end")
            if (aborted.get()) return null
            onTranscribed?.invoke()
            return "almoço 32,50".toByteArray()
        }

        override fun noSpeech(handle: Long): Float { mark("noSpeech"); return 0f }
        override fun abort(handle: Long) { mark("abort"); aborted.set(true) }
        override fun close(handle: Long) { mark("close"); closed = true; closes.incrementAndGet() }
    }

    private val voice = FloatArray(16_000) { 0.1f }

    @Test fun fecharEsperaAContaEmAndamento() {
        // 1. A conta está rodando e alguém fecha: o fechar pede para parar, espera a conta terminar e só então solta.
        val bridge = FakeBridge().apply { release = CountDownLatch(1) }
        val engine = WhisperEngine(1, "pt", null, bridge)
        var result: String? = "?"
        val worker = thread { result = engine.transcribe(voice, voice.size, live = false) }
        assertTrue(bridge.started.await(5, TimeUnit.SECONDS))
        engine.close()
        worker.join(5_000)
        assertNull("a conta foi cancelada", result)
        assertEquals(listOf("transcribe", "abort", "transcribe-end", "close"), bridge.events.toList())
        assertEquals(0, bridge.afterClose.get())
        assertEquals(1, bridge.closes.get())

        // 2. Fechar chega logo depois do texto, antes do "não houve fala": o fechar espera esse segundo passo também.
        val b2 = FakeBridge()
        val e2 = WhisperEngine(2, "pt", null, b2)
        val closer = AtomicBoolean(false)
        var closing: Thread? = null
        b2.onTranscribed = { closing = thread { e2.close(); closer.set(true) }; Thread.sleep(50) }
        val text = e2.transcribe(voice, voice.size, live = false)
        closing!!.join(5_000)
        assertTrue(closer.get())
        assertEquals("almoço 32,50", text)
        assertEquals(0, b2.afterClose.get())
        assertEquals("close", b2.events.last())
        assertTrue(b2.events.indexOf("noSpeech") < b2.events.indexOf("close"))
    }

    @Test fun nadaChegaAoModeloDepoisDeFechar() {
        // Depois de fechar: transcrever devolve null sem chamar a ponte; cancelar e fechar de novo não chamam nada.
        val bridge = FakeBridge()
        val engine = WhisperEngine(1, "pt", null, bridge)
        engine.close()
        assertNull(engine.transcribe(voice, voice.size, live = false))
        engine.abort()
        engine.close()
        assertEquals(listOf("close"), bridge.events.toList())
        assertEquals(1, bridge.closes.get())

        // Várias threads transcrevendo e cancelando, e uma fechando num momento qualquer: nenhuma chamada depois de
        // fechar, o modelo solto uma vez só e ninguém fica preso (sem impasse).
        val rnd = Random(65)
        repeat(150) { round ->
            val b = FakeBridge(hold = rnd.nextLong(0, 2))
            val e = WhisperEngine(round.toLong(), "pt", null, b)
            val stop = AtomicBoolean(false)
            val workers = (0 until 6).map { k ->
                thread {
                    while (!stop.get()) {
                        if (k == 0) e.abort() else e.transcribe(voice, voice.size, live = k % 2 == 0)
                    }
                }
            }
            Thread.sleep(rnd.nextLong(0, 4))
            val closer = thread { e.close() }
            closer.join(5_000)
            stop.set(true)
            workers.forEach { it.join(5_000) }
            assertTrue("rodada $round: o fechar ficou preso", !closer.isAlive && workers.none { it.isAlive })
            assertEquals("rodada $round: chamada depois de fechar", 0, b.afterClose.get())
            assertEquals("rodada $round", 1, b.closes.get())
        }
    }

    @Test fun cancelarContinuaFuncionandoDuranteAConta() {
        val bridge = FakeBridge().apply { release = CountDownLatch(1) }
        val engine = WhisperEngine(1, "pt", null, bridge)
        var result: String? = "?"
        val worker = thread { result = engine.transcribe(voice, voice.size, live = true) }
        assertTrue(bridge.started.await(5, TimeUnit.SECONDS))
        engine.abort()
        worker.join(5_000)
        assertNull(result)
        engine.close()
        assertEquals(listOf("transcribe", "abort", "transcribe-end", "close"), bridge.events.toList())
    }
}
