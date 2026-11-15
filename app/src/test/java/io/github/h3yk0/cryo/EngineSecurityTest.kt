// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.github.h3yk0.cryo.data.Backup
import io.github.h3yk0.cryo.data.BackupCrypto
import io.github.h3yk0.cryo.data.BackupProtection
import io.github.h3yk0.cryo.data.EngineStore
import io.github.h3yk0.cryo.data.Vault
import io.github.h3yk0.cryo.data.db.Tx
import io.github.h3yk0.cryo.data.db.TxType
import io.github.h3yk0.cryo.domain.engine.CategoryModel
import io.github.h3yk0.cryo.domain.engine.FinanceEngine
import io.github.h3yk0.cryo.domain.engine.Rng
import io.github.h3yk0.cryo.domain.engine.TxKey
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.time.LocalDate
import java.util.Base64

/*
 * RF-MOT-15, bloco 401–500: segurança. Textos maliciosos não travam nem derrubam o motor; o que o motor guarda é hash
 * (HMAC-SHA256) sem nada legível; backup com senha não deixa nada legível, recusa senha errada e arquivo adulterado;
 * nada do motor e da criptografia fala com rede ou escreve em logs. Roda com o Android simulado (Robolectric).
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(sdk = [35], application = TestApp::class)
class EngineSecurityTest(private val case: Int) {

    companion object {
        @JvmStatic @ParameterizedRobolectricTestRunner.Parameters(name = "segurança {0}")
        fun cases() = (401..500).map { arrayOf<Any>(it) }

        private val today = LocalDate.of(2026, 11, 7)

        /** Arquivos que guardam ou tratam dados sensíveis (conferidos um a um, sem rede e sem logs). */
        private val sensitiveFiles = listOf(
            "domain/engine/Stats.kt", "domain/engine/TxKey.kt", "domain/engine/Recurrences.kt", "domain/engine/Outlook.kt",
            "domain/engine/Projection.kt", "domain/engine/Signals.kt", "domain/engine/Health.kt", "domain/engine/Wealth.kt",
            "domain/engine/CategoryModel.kt", "domain/engine/Engine.kt", "data/Vault.kt", "data/BackupCrypto.kt", "data/EngineStore.kt",
        )
    }

    private val ctx: Context get() = ApplicationProvider.getApplicationContext()
    private val rng = Rng(case * 104_729L)
    private fun word(len: Int) = String(CharArray(len) { 'a' + rng.nextInt(26) })

    @Before fun setUp() {
        JvmStrings.install()
        Vault.softwareWrapForTests = true
    }

    @After fun tearDown() { Vault.softwareWrapForTests = false }

    @Test fun seguranca() {
        when (case % 10) {
            0 -> textosEnormes()
            1 -> textosDeControleEFormatacao()
            2 -> padroesQueTravariamExpressoes()
            3 -> oQueOMotorGuardaEHash()
            4 -> backupComSenhaNaoDeixaNadaLegivel()
            5 -> senhaErradaEArquivoAdulterado()
            6 -> protecaoGuardaSoAChaveTrancada()
            7 -> arquivosQuebradosNuncaDerrubamOApp()
            8 -> semRedeESemLogs()
            else -> hmacNaoRevelaOTexto()
        }
    }

    /** Descrições de até 1 milhão de caracteres: a chave corta no limite e o motor termina rápido. */
    private fun textosEnormes() {
        val size = 50_000 + rng.nextInt(950_000)
        val huge = word(size)
        val t0 = System.nanoTime()
        val key = TxKey.of(huge)
        assertTrue(key.length <= TxKey.MAX_INPUT)
        val s = Personas.generate(Personas.spec(Personas.Kind.STABLE).copy(months = 6), today, case.toLong())
        val evil = s.copy(txs = s.txs.mapIndexed { i, t -> if (i % 20 == 0) t.copy(description = huge) else t })
        val r = FinanceEngine.run(evil, today)
        val ms = (System.nanoTime() - t0) / 1_000_000
        assertTrue("caso $case: ${ms} ms", ms < 5_000)
        assertEquals(emptyList<String>(), Synthetic.invariants(r))
    }

    /** Caracteres de controle, inversão de escrita e "%s" em descrições e categorias: nada quebra, nada vira formatação. */
    private fun textosDeControleEFormatacao() {
        val evil = listOf("%s%s%s%n", "%1\$s %2\$d %99\$s", "‮alert‬", "\u0000\u001b[31m", "💣".repeat(100), "<script>x</script>", "'; DROP TABLE transactions;--")
        val label = evil[rng.nextInt(evil.size)] + word(4)
        val s = Personas.generate(Personas.spec(Personas.Kind.STABLE).copy(months = 8), today, case.toLong())
        val txs = s.txs + (0 until 6).map { Tx(8_000_000L + it, TxType.EXPENSE, 4_990, today.minusMonths(it.toLong()).withDayOfMonth(9), label, categoryId = 5, accountId = 1) }
        val cats = s.categories.map { if (it.id == 5L) it.copy(name = label) else it }
        val r = FinanceEngine.run(s.copy(txs = txs, categories = cats), today)
        assertEquals(emptyList<String>(), Synthetic.invariants(r))
        // A descrição aparece como texto, do jeito que foi escrita (sem ser interpretada como formato).
        r.recurrences.filter { rec -> rec.txIds.any { it >= 8_000_000L } }.forEach { assertEquals(label.trim().take(60), it.label) }
    }

    /** Textos que fariam uma expressão regular ingênua demorar muito: a chave e a categoria aprendida respondem na hora. */
    private fun padroesQueTravariamExpressoes() {
        val unit = listOf("a", "ab", "a ", "1/", "aa!", "ã", "á")[rng.nextInt(7)]
        val evil = unit.repeat(20_000 + rng.nextInt(30_000)) + "!"
        val model = CategoryModel.train(Personas.generate(Personas.spec(Personas.Kind.STABLE).copy(months = 6), today, case.toLong()).txs, today)
        val t0 = System.nanoTime()
        repeat(50) {
            TxKey.of(evil)
            model.guess(TxType.EXPENSE, evil, 1000, 1, null)
        }
        val ms = (System.nanoTime() - t0) / 1_000_000
        assertTrue("caso $case: $ms ms", ms < 2_000)
    }

    /** O motor só guarda códigos HMAC-SHA256: nenhuma descrição, valor ou nome no que fica gravado. */
    private fun oQueOMotorGuardaEHash() {
        val name = "cryo-seg-$case"
        val store = EngineStore(ctx, sandbox = case % 2 == 0, vault = Vault(ctx, name))
        val keys = (1..5).map { "padaria ${word(6)} ${rng.nextInt(1000)}" }
        keys.forEach { store.dismiss(it) }
        val prefs = ctx.getSharedPreferences(if (case % 2 == 0) "cryo_engine_teste" else "cryo_engine", Context.MODE_PRIVATE)
        val saved = prefs.all.values.flatMap { (it as? Set<*>)?.map { v -> v.toString() } ?: listOf(it.toString()) }
        assertTrue(saved.isNotEmpty())
        for (v in saved) {
            assertTrue("caso $case: '$v' não é um HMAC", v.matches(Regex("[0-9a-f]{64}")) || v.toDoubleOrNull() != null)
            keys.forEach { k -> assertFalse(v.contains(k.substringAfter(' ').take(6))) }
        }
        // Mesmo texto, mesmo código (o filtro funciona); outro cofre, outro código.
        assertEquals(store.hash(keys[0]), store.hash(keys[0]))
        assertNotEquals(store.hash(keys[0]), EngineStore(ctx, sandbox = true, vault = Vault(ctx, "$name-outro")).hash(keys[0]))
        assertTrue(store.options.value.dismissed.containsAll(keys.map { store.hash(it) }))
        store.clear()
    }

    /** Ida e volta com senha: o arquivo trancado não tem nomes, descrições nem os nomes dos campos. */
    private fun backupComSenhaNaoDeixaNadaLegivel() {
        val s = Personas.generate(Personas.spec(Personas.Kind.entries[case % 4]).copy(months = 3 + case % 12), today, case.toLong())
        val json = Backup.export(s, exportedAt = 1_700_000_000_000L + case)
        val password = "senha-${word(10)}".toCharArray()
        val key = BackupCrypto.deriveKey(password.copyOf(), BackupCrypto.newSalt(), 1_000 + case)
        val sealed = BackupCrypto.encrypt(json, key, exportedAt = 1_700_000_000_000L + case)
        assertTrue(BackupCrypto.isEncrypted(sealed))
        for (secret in listOf("transactions", "accounts", "Padaria", "Supermercado", "Conta principal", "Salário", s.txs.first().description)) {
            assertFalse("caso $case: '$secret' legível no backup trancado", sealed.contains(secret))
        }
        assertEquals(json, BackupCrypto.decrypt(sealed, password.copyOf()))
        assertEquals(json, BackupCrypto.decryptWithKey(sealed, key))
        // O começo continua legível para o backup automático reconhecer o arquivo e a data.
        assertTrue(sealed.take(200).contains("\"exportedAt\":${1_700_000_000_000L + case}"))
    }

    /** Senha errada é recusada; qualquer byte mudado (nos dados ou no começo legível) também. */
    private fun senhaErradaEArquivoAdulterado() {
        val json = """{"app":"Cryo","version":2,"exportedAt":1,"accounts":[],"note":"${word(200)}"}"""
        val key = BackupCrypto.deriveKey("certa-$case".toCharArray(), BackupCrypto.newSalt(), 1_000)
        val sealed = BackupCrypto.encrypt(json, key, exportedAt = 42)
        expect<BackupCrypto.WrongPassword> { BackupCrypto.decrypt(sealed, "errada-$case".toCharArray()) }
        val env = JSONObject(sealed)
        val data = Base64.getDecoder().decode(env.getString("data"))
        // O byte mexido é sempre diferente do original: xor de 1 a 255 no MESMO byte. Antes o teste lia um byte e
        // gravava em outro; 1 vez em 256 o valor "novo" era igual ao que já estava lá, o arquivo não mudava e o teste
        // falhava sem defeito nenhum no app (o texto cifrado muda a cada rodada; visto em 15/11/2026).
        val at = rng.nextInt(data.size)
        data[at] = (data[at].toInt() xor (1 + rng.nextInt(255))).toByte()
        val tampered = JSONObject(sealed).put("data", Base64.getEncoder().encodeToString(data)).toString()
        expect<IllegalArgumentException> { BackupCrypto.decrypt(tampered, "certa-$case".toCharArray()) }
        val headerTampered = JSONObject(sealed).put("exportedAt", 43).toString()
        expect<BackupCrypto.WrongPassword> { BackupCrypto.decrypt(headerTampered, "certa-$case".toCharArray()) }
        // Um número de voltas absurdo (para travar o celular) é recusado na hora.
        val t0 = System.nanoTime()
        expect<BackupCrypto.Damaged> { BackupCrypto.decrypt(JSONObject(sealed).put("iterations", 2_000_000_000).toString(), "x".toCharArray()) }
        assertTrue((System.nanoTime() - t0) / 1_000_000 < 1_000)
        assertNull(BackupCrypto.decryptWithKey(tampered, key))
    }

    /** A proteção guarda só a chave trancada pelo cofre e o sal: nunca a senha. Desligada, o backup sai aberto. */
    private fun protecaoGuardaSoAChaveTrancada() {
        val sandbox = case % 2 == 0
        val vault = Vault(ctx, "cryo-prot-$case")
        val protection = BackupProtection(ctx, sandbox, vault)
        val password = "minha-senha-${word(8)}"
        assertTrue(protection.enable(password.toCharArray(), iterations = 1_000))
        val prefs = ctx.getSharedPreferences(if (sandbox) "cryo_backup_lock_teste" else "cryo_backup_lock", Context.MODE_PRIVATE)
        prefs.all.values.forEach { assertFalse("caso $case: a senha ficou gravada", it.toString().contains(password)) }
        val json = """{"app":"Cryo","version":2,"exportedAt":7,"accounts":[{"name":"Conta secreta $case"}]}"""
        val sealed = protection.seal(json, 7)
        assertTrue(BackupCrypto.isEncrypted(sealed))
        assertFalse(sealed.contains("Conta secreta"))
        assertEquals(json, BackupCrypto.decrypt(sealed, password.toCharArray()))
        expect<Backup.PasswordNeeded> { Backup.import(sealed) }
        protection.disable()
        assertNull(protection.key())
        assertEquals(json, protection.seal(json, 7))
    }

    /** Arquivos cortados, base64 inválido, vetor curto, lixo: sempre uma mensagem amigável, nunca outro tipo de erro. */
    private fun arquivosQuebradosNuncaDerrubamOApp() {
        val key = BackupCrypto.deriveKey("s".toCharArray(), BackupCrypto.newSalt(), 1_000)
        val sealed = BackupCrypto.encrypt("""{"app":"Cryo","version":2,"exportedAt":1,"accounts":[]}""", key, exportedAt = 1)
        val variants = listOf(
            sealed.take(10 + rng.nextInt(sealed.length - 10)),
            JSONObject(sealed).put("data", "@@@não-é-base64@@@").toString(),
            JSONObject(sealed).put("iv", Base64.getEncoder().encodeToString(ByteArray(3))).toString(),
            JSONObject(sealed).put("salt", "").toString(),
            String(CharArray(1 + rng.nextInt(5000)) { (rng.nextInt(0x3000) + 1).toChar() }),
            """{"app":"Cryo","cipher":"AES-256-GCM"}""",
        )
        for (v in variants) {
            try { Backup.import(v); fail("caso $case: aceitou um arquivo quebrado") } catch (e: IllegalArgumentException) { assertTrue(e.message!!.isNotBlank()) }
            try { BackupCrypto.decrypt(v, "s".toCharArray()); fail("caso $case: abriu um arquivo quebrado") } catch (e: IllegalArgumentException) { assertTrue(e.message!!.isNotBlank()) }
        }
    }

    /** Cada arquivo do motor e da criptografia: sem rede, sem logs e (no motor) sem Android. */
    private fun semRedeESemLogs() {
        val rel = sensitiveFiles[(case / 10) % sensitiveFiles.size]
        val f = File("src/main/java/io/github/h3yk0/cryo/$rel")
        assertTrue(f.exists())
        val code = f.readLines().filter { !it.trim().startsWith("//") && !it.trim().startsWith("*") }
        val forbidden = listOf("java.net", "javax.net", "Socket(", "URL(", "HttpURLConnection", "Log.d(", "Log.i(", "Log.e(", "Log.w(", "println(", "printStackTrace(")
        forbidden.forEach { w -> assertTrue("caso $case: $rel usa $w", code.none { it.contains(w) }) }
        if (rel.startsWith("domain/engine")) assertTrue("caso $case: $rel usa Android", code.none { it.startsWith("import android.") })
    }

    /** HMAC: 64 dígitos hexadecimais, sempre o mesmo para o mesmo texto, muda inteiro com uma letra trocada. */
    private fun hmacNaoRevelaOTexto() {
        val vault = Vault(ctx, "cryo-hmac-$case")
        val text = "Netflix ${word(5 + rng.nextInt(30))}"
        val h1 = vault.hmac(text)
        assertTrue(h1.matches(Regex("[0-9a-f]{64}")))
        assertEquals(h1, vault.hmac(text))
        assertFalse(h1.contains(text.lowercase().take(4)))
        val other = text.dropLast(1) + (if (text.last() == 'z') 'y' else text.last() + 1)
        val h2 = vault.hmac(other)
        val bits = h1.chunked(2).zip(h2.chunked(2)).sumOf { (a, b) -> Integer.bitCount(a.toInt(16) xor b.toInt(16)) }
        assertTrue("caso $case: só $bits bits mudaram", bits in 64..192)
        assertNotEquals(h1, Vault(ctx, "cryo-hmac-$case-b").hmac(text))
    }

    private inline fun <reified T : Throwable> expect(block: () -> Unit) {
        try {
            block()
            fail("caso $case: esperava ${T::class.simpleName}")
        } catch (e: Throwable) {
            if (e is AssertionError) throw e
            assertTrue("caso $case: veio ${e::class.simpleName}: ${e.message}", e is T)
        }
    }

}
