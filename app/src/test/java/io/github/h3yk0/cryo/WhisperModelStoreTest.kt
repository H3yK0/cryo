// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.github.h3yk0.cryo.voice.ModelInstall
import io.github.h3yk0.cryo.voice.WhisperModel
import io.github.h3yk0.cryo.voice.WhisperModelStore
import io.github.h3yk0.cryo.voice.WhisperModels
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.security.MessageDigest
import java.util.Locale

/**
 * RF-VOZ-15 e ERR-057 (1.6): o modelo de voz que a pessoa baixa e escolhe. Os testes usam um catálogo pequeno, com
 * "modelos" de poucos KB que começam como um modelo do whisper.cpp: a regra é a mesma dos arquivos de 30 a 260 MB.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = TestApp::class)
class WhisperModelStoreTest {
    private val ctx: Context get() = ApplicationProvider.getApplicationContext()

    private fun model(id: String, bytes: ByteArray, recommended: Boolean = false) = WhisperModel(
        id, "ggml-$id.bin", bytes.size.toLong(), sha(bytes), R.string.voice_model_base, R.string.voice_model_base_desc, recommended,
    )

    private fun sha(b: ByteArray) = MessageDigest.getInstance("SHA-256").digest(b).joinToString("") { String.format(Locale.ROOT, "%02x", it) }

    /** Um "modelo" de [size] bytes: o número mágico do whisper.cpp e depois bytes que dependem de [seed]. */
    private fun fakeModel(size: Int, seed: Int): ByteArray = ByteArray(size) { i -> if (i < 4) WhisperModelStoreTest.MAGIC[i] else ((i * 31 + seed) % 251).toByte() }

    private val small = fakeModel(4_000, 1)
    private val big = fakeModel(9_000, 2)
    private val catalog by lazy { listOf(model("small", small), model("big", big, recommended = true)) }
    private var free = Long.MAX_VALUE
    private val store by lazy { WhisperModelStore(ctx, catalog) { free } }

    @Before fun clean() {
        WhisperModelStore(ctx).remove()
        ctx.getSharedPreferences("cryo_voice", Context.MODE_PRIVATE).edit().clear().commit()
        WhisperModelStore.newProcessForTests()
    }
    @After fun cleanAfter() { clean() }

    private fun install(bytes: ByteArray, declared: Long? = bytes.size.toLong()) = store.install(declared) { ByteArrayInputStream(bytes) }

    @Test fun catalogoTemOsSeisModelosOficiais() {
        val all = WhisperModels.ALL
        assertEquals(listOf("tiny-q5_1", "tiny-q8_0", "base-q5_1", "base-q8_0", "small-q5_1", "small-q8_0"), all.map { it.id })
        // O recomendado é o mesmo "base" de 5 bits que ia dentro do APK até a 1.5 (o SHA-256 do arquivo de antes).
        val rec = WhisperModels.recommended
        assertEquals("base-q5_1", rec.id)
        assertEquals(59_707_625L, rec.size)
        assertEquals("422f1ae452ade6f30a004d7e5c6a43195e4433bc370bf23fac9cc591f01a8898", rec.sha256)
        assertEquals(1, all.count { it.recommended })
        for (m in all) {
            assertTrue(m.sha256, m.sha256.matches(Regex("[0-9a-f]{64}")))
            assertEquals("https://huggingface.co/ggerganov/whisper.cpp/resolve/main/${m.file}", m.officialUrl)
            assertTrue(m.file.startsWith("ggml-") && m.file.endsWith(".bin"))
        }
        // Só o recomendado tem a cópia na Release "voice-model-1" do GitHub do Cryo.
        assertEquals("https://github.com/H3yK0/cryo/releases/download/voice-model-1/ggml-base-q5_1.bin", rec.cryoUrl)
        assertTrue(all.filter { !it.recommended }.all { it.cryoUrl == null })
        assertEquals(all.size, all.map { it.sha256 }.toSet().size)
    }

    @Test fun instalaOArquivoCertoForaDoBackup() {
        val r = install(small)
        assertEquals(ModelInstall.Installed(catalog[0]), r)
        assertEquals(catalog[0], store.installed())
        val f = store.usableFile()!!
        // Fora do backup do Android (noBackupFilesDir), com o nome oficial.
        assertEquals(File(ctx.noBackupFilesDir, "whisper/ggml-small.bin").canonicalPath, f.canonicalPath)
        assertTrue(f.readBytes().contentEquals(small))
        // Nada de temporário sobrando.
        assertEquals(listOf("ggml-small.bin"), store.dir.list()!!.toList())
    }

    @Test fun mesmoTamanhoComOutroConteudoEhIncompleto() {
        // Começa como um modelo do whisper.cpp, mas o SHA-256 não bate (download estragado): recusado e nada fica.
        val wrong = small.copyOf().also { it[2000] = (it[2000] + 1).toByte() }
        assertEquals(ModelInstall.Incomplete, install(wrong))
        assertNull(store.installed())
        assertTrue(store.dir.list().isNullOrEmpty())
    }

    @Test fun downloadCortadoNoMeioEhIncompleto() {
        assertEquals(ModelInstall.Incomplete, install(big.copyOf(5_000), declared = null))
        assertNull(store.installed())
    }

    @Test fun outroArquivoQualquerNaoEhModelo() {
        val photo = ByteArray(3_000) { (it % 7).toByte() }.also { it[0] = 0xFF.toByte(); it[1] = 0xD8.toByte() }
        assertEquals(ModelInstall.NotAModel, install(photo))
        assertTrue(store.dir.list().isNullOrEmpty())
    }

    @Test fun maiorQueOMaiorModeloNemComecaACopiar() {
        var opened = false
        val r = store.install(50_000L) { opened = true; ByteArrayInputStream(ByteArray(10)) }
        assertEquals(ModelInstall.NotAModel, r)
        assertFalse("nem abriu o arquivo", opened)
        // Sem tamanho informado: para de copiar quando passa do maior aceito.
        assertEquals(ModelInstall.NotAModel, install(fakeModel(20_000, 3), declared = null))
        assertTrue(store.dir.list().isNullOrEmpty())
    }

    @Test fun faltaDeEspacoAvisaQuantoLiberar() {
        free = 1_000
        val r = install(small)
        // O modelo inteiro mais a folga de 50 MB, menos o que já está livre.
        assertEquals(ModelInstall.NoSpace(small.size + WhisperModelStore.MARGIN - 1_000), r)
        assertNull(store.installed())
        assertTrue(store.dir.list().isNullOrEmpty())
    }

    @Test fun oModeloDeAntesNaoContaComoEspacoLivre() {
        // O de antes só sai depois que o novo está conferido: o novo precisa caber inteiro no espaço livre de agora.
        assertTrue(install(small) is ModelInstall.Installed)
        free = big.size + WhisperModelStore.MARGIN - 1
        assertEquals(ModelInstall.NoSpace(1), install(big))
        assertEquals("o de antes continua", catalog[0], store.installed())
    }

    @Test fun naoDeuParaLer() {
        assertEquals(ModelInstall.Unreadable, store.install(10L) { null })
        assertEquals(ModelInstall.Unreadable, store.install(10L) { throw SecurityException("sem permissão") })
        // Erro no meio da cópia: o temporário some.
        val broken = object : InputStream() {
            var n = 0
            override fun read(): Int = if (n++ < 100) 7 else throw IOException("nuvem caiu")
            override fun read(b: ByteArray, off: Int, len: Int): Int { if (n > 0) throw IOException("nuvem caiu"); n++; b.fill(7, off, off + 100); return 100 }
        }
        assertEquals(ModelInstall.Unreadable, store.install(null) { broken })
        assertTrue(store.dir.list().isNullOrEmpty())
    }

    @Test fun umModeloPorVezEOAntigoSoSaiDepoisDeConferido() {
        assertTrue(install(small) is ModelInstall.Installed)
        // Um arquivo errado não tira o modelo que já funciona.
        assertEquals(ModelInstall.Incomplete, install(big.copyOf(8_000), declared = null))
        assertEquals(catalog[0], store.installed())
        // O certo troca: o de antes sai.
        assertTrue(install(big) is ModelInstall.Installed)
        assertEquals(catalog[1], store.installed())
        assertEquals(listOf("ggml-big.bin"), store.dir.list()!!.toList())
    }

    /** ERR-057: o arquivo guardado mudou depois de instalado (outro app, limpeza, cartão de memória): sai e a pessoa vê o aviso. */
    @Test fun modeloQueMudouDepoisSaiComAviso() {
        assertTrue(install(small) is ModelInstall.Installed)
        val f = store.usableFile()!!
        // Mesmo tamanho, outro conteúdo, gravado depois (o carimbo muda).
        f.writeBytes(small.reversedArray())
        f.setLastModified(f.lastModified() + 5_000)
        assertNull(store.installed())
        assertFalse(store.damaged)
        assertNull("o Whisper nunca abre um arquivo que não é o conferido", store.usableFile())
        assertTrue(store.damaged)
        assertFalse(f.exists())
        // Instalar de novo tira o aviso.
        assertTrue(install(small) is ModelInstall.Installed)
        assertFalse(store.damaged)
    }

    @Test fun modeloQueSumiuTambemAvisa() {
        assertTrue(install(small) is ModelInstall.Installed)
        store.usableFile()!!.delete()
        assertNull(store.usableFile())
        assertTrue(store.damaged)
        // Sem modelo nenhum antes, não há o que avisar.
        store.remove()
        assertNull(store.usableFile())
        assertFalse(store.damaged)
    }

    @Test fun removerLiberaTudo() {
        assertTrue(install(small) is ModelInstall.Installed)
        store.remove()
        assertNull(store.installed())
        assertTrue(store.dir.list().isNullOrEmpty())
    }

    @Test fun avisoDaVersaoNovaApareceUmaVez() {
        assertFalse(store.noticeSeen)
        store.noticeSeen = true
        assertTrue(WhisperModelStore(ctx).noticeSeen)
    }

    /** ERR-058: o SHA-256 em hexadecimal não pode sair com algarismos de outra escrita (árabe, por exemplo). */
    @Test fun conferenciaNaoDependeDaLinguaDoCelular() {
        val before = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("ar-EG-u-nu-arab"))
            assertTrue(install(small) is ModelInstall.Installed)
            assertNotNull(store.installed())
        } finally {
            Locale.setDefault(before)
        }
    }

    /* ------------------------------ auditoria da 1.6 (ERR-066) ------------------------------ */

    /**
     * ERR-066: um arquivo mexido que manteve o tamanho e a data passava pela conferência de cada uso. Agora, no primeiro uso
     * de cada abertura do app, o SHA-256 inteiro é conferido de novo: ele sai e a pessoa vê o aviso.
     */
    @Test fun modeloMexidoComMesmoTamanhoEDataSaiComAviso() {
        assertTrue(install(small) is ModelInstall.Installed)
        val f = File(store.dir, catalog[0].file)
        val stamp = f.lastModified()
        java.io.RandomAccessFile(f, "rw").use { it.seek(2000); it.write(small[2000].toInt() xor 0x55) }
        f.setLastModified(stamp)
        assertEquals("o tamanho e a data não mudaram", catalog[0], store.installed())
        WhisperModelStore.newProcessForTests()
        assertNull("o Whisper nunca abre um arquivo que não é o conferido", store.usableFile())
        assertTrue(store.damaged)
        assertFalse(f.exists())
    }

    /** ERR-066: no primeiro uso de cada abertura o arquivo é lido uma vez só; instalar já conta como conferido. */
    @Test fun conferidoUmaVezPorAbertura() {
        assertTrue(install(small) is ModelInstall.Installed)
        val before = WhisperModelStore.hashes.get()
        assertNotNull(store.usableFile())
        assertEquals("logo depois de instalar não precisa ler de novo", before, WhisperModelStore.hashes.get())
        WhisperModelStore.newProcessForTests()
        assertNotNull(store.usableFile())
        assertNotNull(store.usableFile())
        assertNotNull(WhisperModelStore(ctx, catalog) { free }.usableFile())
        assertEquals("uma leitura por abertura do app", before + 1, WhisperModelStore.hashes.get())
    }

    /**
     * ERR-066: uma cópia interrompida (o Android fechou o app no meio de 60 MB) deixava o temporário para sempre. Ele é
     * apagado na próxima vez que o Cryo olha o modelo, mas nunca no meio de uma cópia que está rodando.
     */
    @Test fun copiaInterrompidaNaoDeixaArquivo() {
        store.dir.mkdirs()
        val leftover = File(store.dir, "import.tmp").apply { writeBytes(ByteArray(3_000)) }
        assertNull(store.installed())
        assertFalse("o temporário largado foi apagado", leftover.exists())
        // Durante uma cópia de verdade, olhar o modelo não apaga o temporário dela.
        var seenDuringCopy = false
        val slow = object : InputStream() {
            private val data = ByteArrayInputStream(small)
            override fun read(): Int = data.read()
            override fun read(b: ByteArray, off: Int, len: Int): Int {
                val n = data.read(b, off, minOf(len, 1_000))
                if (n > 0 && !seenDuringCopy && File(store.dir, "import.tmp").exists()) {
                    store.installed(); store.usableFile()
                    seenDuringCopy = File(store.dir, "import.tmp").exists()
                }
                return n
            }
        }
        assertEquals(ModelInstall.Installed(catalog[0]), store.install(small.size.toLong()) { slow })
        assertTrue("o temporário da cópia em andamento continuou", seenDuringCopy)
        assertEquals(listOf("ggml-small.bin"), store.dir.list()!!.toList())
    }

    /** ERR-066: um modelo que bate com o SHA-256 e não abre (falta de memória, por exemplo) não é apagado. */
    @Test fun modeloBomQueNaoAbreNaoEhApagado() {
        assertTrue(install(small) is ModelInstall.Installed)
        WhisperModelStore.newProcessForTests()
        assertNull(store.openWith<Any> { null })
        assertEquals(catalog[0], store.installed())
        assertFalse(store.damaged)
        assertEquals("abriu", store.openWith { "abriu" })
    }

    companion object {
        val MAGIC = byteArrayOf(0x6c, 0x6d, 0x67, 0x67)
    }
}
