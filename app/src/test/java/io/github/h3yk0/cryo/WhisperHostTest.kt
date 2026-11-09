// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo

import androidx.test.core.app.ApplicationProvider
import android.content.Context
import io.github.h3yk0.cryo.domain.AppLanguage
import io.github.h3yk0.cryo.domain.Spoken
import io.github.h3yk0.cryo.voice.WhisperEngine
import io.github.h3yk0.cryo.voice.WhisperNative
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * RF-VOZ-01: a ponte de verdade (src/main/cpp/cryo_whisper.cpp) com o modelo de verdade, compilada para o computador.
 * Não roda por padrão (precisa compilar o Whisper antes):
 *   app/src/test/cpp/build-host.sh
 *   ./gradlew testDebugUnitTest --tests '*WhisperHostTest' -PwhisperHost=true
 * O áudio do teste é uma voz sintética (espeak-ng) dizendo "Almoço 32,50 no débito": nada de voz de pessoa no repositório.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = TestApp::class)
class WhisperHostTest {
    companion object {
        @JvmStatic @BeforeClass fun load() {
            val lib = System.getProperty("cryo.whisperHostLib").orEmpty()
            assumeTrue("só com -PwhisperHost=true", lib.isNotEmpty() && File(lib).exists())
            System.load(lib)
        }
    }

    private val ctx: Context get() = ApplicationProvider.getApplicationContext()

    private fun wav(name: String): FloatArray {
        val bytes = javaClass.classLoader!!.getResourceAsStream(name)!!.readBytes()
        val data = ByteBuffer.wrap(bytes, 44, bytes.size - 44).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
        return FloatArray(data.remaining()) { data.get(it) / 32768f }
    }

    private val prompt = "Almoço 32,50 no débito. Uber 18,90 no cartão. Mercado 120 reais. Cartão Roxo, Conta principal."

    @Test fun transcreveFraseFalada() {
        assertTrue(WhisperNative.nativeAvailable())
        val engine = WhisperEngine.openLoaded(ctx.assets, AppLanguage.PT_BR, prompt)!!
        engine.use { e ->
            val pcm = wav("voice/almoco-sintetico.wav")
            val text = e.transcribe(pcm, pcm.size, live = false)!!
            val entry = Spoken.entries(text, io.github.h3yk0.cryo.domain.ParseContext(
                java.time.LocalDate.of(2026, 11, 9), emptyList(), emptyList(), emptyList(), language = AppLanguage.PT_BR,
            )).single()
            assertEquals("texto: $text", 3250L, entry.amount)
            // Ao vivo (janela curta) também sai algo com o valor.
            val live = e.transcribe(pcm, pcm.size, live = true)!!
            assertTrue("ao vivo: $live", live.contains("32"))
        }
    }

    @Test fun silencioNaoInventaTexto() {
        WhisperEngine.openLoaded(ctx.assets, AppLanguage.PT_BR, prompt)!!.use { e ->
            val silence = FloatArray(16_000 * 2)
            assertEquals("", Spoken.clean(e.transcribe(silence, silence.size, live = false).orEmpty()))
        }
    }

    @Test fun cancelarParaEFecharEhSeguro() {
        val e = WhisperEngine.openLoaded(ctx.assets, AppLanguage.PT_BR, prompt)!!
        e.abort()
        val pcm = wav("voice/almoco-sintetico.wav")
        assertNull(e.transcribe(pcm, pcm.size, live = false))
        e.close()
        e.close()   // fechar duas vezes não derruba
        assertNull(e.transcribe(pcm, pcm.size, live = false))
    }

    @Test fun modeloQueNaoExisteNaoAbre() {
        assertEquals(0L, WhisperNative.nativeOpen(ctx.assets, "whisper/nao-existe.bin"))
    }
}
