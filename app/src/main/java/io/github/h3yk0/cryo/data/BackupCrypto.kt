// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.data

import android.content.Context
import io.github.h3yk0.cryo.R
import io.github.h3yk0.cryo.domain.Res
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.security.SecureRandom
import java.util.Arrays
import java.util.Base64
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * RF-SEG-03: backup protegido com senha. O arquivo continua sendo um JSON do Cryo, mas só o começo fica legível (app,
 * versão, data: o backup automático usa a data para saber se o arquivo é dele). O resto vai compactado e trancado com
 * **AES-256-GCM**; a chave sai da senha por **PBKDF2-HMAC-SHA256** com sal aleatório. O GCM também confere que nada foi
 * mudado: um arquivo adulterado (até no começo legível, que entra como "dado associado") é recusado.
 *
 * Só usa o que o próprio Android (e o Java) já traz: nenhuma biblioteca nova.
 */
object BackupCrypto {
    /** Voltas do PBKDF2 (recomendação da OWASP para PBKDF2-HMAC-SHA256 em 2021). */
    const val ITERATIONS = 310_000

    /** Versão do envelope: maior que a dos backups abertos, para a 1.4 dizer "backup de uma versão mais nova". */
    const val ENVELOPE_VERSION = 3
    private const val SALT_BYTES = 16
    private const val IV_BYTES = 12
    private const val TAG_BITS = 128

    /** A chave já derivada da senha (o que o backup automático guarda, trancado pelo cofre). */
    class Key(val bytes: ByteArray, val salt: ByteArray, val iterations: Int)

    class WrongPassword : IllegalArgumentException(Res.s(R.string.sec_wrong_password))
    class Damaged : IllegalArgumentException(Res.s(R.string.backup_invalid_file))

    private val random = SecureRandom()
    private fun b64(b: ByteArray) = Base64.getEncoder().encodeToString(b)
    private fun unb64(s: String) = Base64.getDecoder().decode(s)

    fun newSalt(): ByteArray = ByteArray(SALT_BYTES).also { random.nextBytes(it) }

    /** Deriva a chave de 256 bits da senha. Lento de propósito (contra quem tenta adivinhar senhas): chame fora da tela. */
    fun deriveKey(password: CharArray, salt: ByteArray, iterations: Int = ITERATIONS): Key {
        val spec = PBEKeySpec(password, salt, iterations, 256)
        try {
            val bytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
            return Key(bytes, salt, iterations)
        } finally {
            spec.clearPassword()
        }
    }

    /**
     * O arquivo é um backup protegido com senha? (Olha só o começo, sem abrir.) ERR-063 (auditoria da 1.6): o que conta é a
     * **chave** `"cipher":` do envelope. Dentro de um texto do JSON (o nome de uma conta, uma descrição) as aspas vêm
     * escapadas (`\"`), então nenhum dado da pessoa forma essa chave; antes bastava a palavra "cipher" entre aspas, e o
     * backup aberto de quem tinha uma conta chamada "cipher" pedia senha e não abria.
     */
    fun isEncrypted(text: String): Boolean {
        val head = text.take(600)
        return head.contains("\"Cryo\"") && CIPHER_KEY.containsMatchIn(head)
    }

    private val CIPHER_KEY = Regex("\"cipher\"\\s*:")

    private fun aad(app: String, version: Int, exportedAt: Long, testMode: Boolean, iterations: Int, salt: String) =
        "$app|$version|$exportedAt|$testMode|$iterations|$salt".toByteArray(Charsets.UTF_8)

    fun encrypt(json: String, key: Key, exportedAt: Long, testMode: Boolean = false): String {
        val iv = ByteArray(IV_BYTES).also { random.nextBytes(it) }
        val salt = b64(key.salt)
        val zipped = ByteArrayOutputStream().also { out -> GZIPOutputStream(out).use { it.write(json.toByteArray(Charsets.UTF_8)) } }.toByteArray()
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key.bytes, "AES"), GCMParameterSpec(TAG_BITS, iv))
        c.updateAAD(aad("Cryo", ENVELOPE_VERSION, exportedAt, testMode, key.iterations, salt))
        val sealed = c.doFinal(zipped)
        // A ordem importa: "exportedAt" fica nos primeiros caracteres, como nos backups abertos.
        return JSONObject().apply {
            put("app", "Cryo")
            put("version", ENVELOPE_VERSION)
            put("exportedAt", exportedAt)
            if (testMode) put("testMode", true)
            put("cipher", "AES-256-GCM")
            put("kdf", "PBKDF2-HMAC-SHA256")
            put("iterations", key.iterations)
            put("salt", salt)
            put("iv", b64(iv))
            put("data", b64(sealed))
        }.toString()
    }

    /** Abre com a senha. [WrongPassword] se a senha não serve; [Damaged] se o arquivo foi mexido ou não é um envelope. */
    fun decrypt(text: String, password: CharArray): String {
        val env = envelope(text)
        val key = deriveKey(password, unb64(env.getString("salt")), env.getInt("iterations"))
        return try { decrypt(env, key) } finally { Arrays.fill(key.bytes, 0) }
    }

    /** Abre com uma chave já derivada (a do backup automático deste aparelho). Null se a chave não é a deste arquivo. */
    fun decryptWithKey(text: String, key: Key): String? = runCatching {
        val env = envelope(text)
        if (env.getString("salt") != b64(key.salt) || env.getInt("iterations") != key.iterations) return null
        decrypt(env, key)
    }.getOrNull()

    private fun envelope(text: String): JSONObject {
        val env = try { JSONObject(text) } catch (e: Exception) { throw Damaged() }
        if (env.optString("app") != "Cryo" || !env.has("data") || !env.has("salt") || !env.has("iv")) throw Damaged()
        val it = env.optInt("iterations", 0)
        // Um número de voltas absurdo travaria o celular (arquivo malicioso): recusa.
        if (it < 1000 || it > 5_000_000) throw Damaged()
        return env
    }

    private fun decrypt(env: JSONObject, key: Key): String {
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        val iv = try { unb64(env.getString("iv")) } catch (e: Exception) { throw Damaged() }
        if (iv.size != IV_BYTES) throw Damaged()
        c.init(Cipher.DECRYPT_MODE, SecretKeySpec(key.bytes, "AES"), GCMParameterSpec(TAG_BITS, iv))
        c.updateAAD(aad(env.getString("app"), env.getInt("version"), env.optLong("exportedAt"), env.optBoolean("testMode"), env.getInt("iterations"), env.getString("salt")))
        val plain = try {
            c.doFinal(unb64(env.getString("data")))
        } catch (e: AEADBadTagException) {
            // Com GCM não dá para separar "senha errada" de "arquivo mexido": a senha errada é o caso comum.
            throw WrongPassword()
        } catch (e: IllegalArgumentException) {
            throw Damaged()
        }
        return GZIPInputStream(ByteArrayInputStream(plain)).use { input ->
            // Limite contra "bomba de compactação": um backup de verdade fica muito abaixo disso (50 mil movimentações ≈ 20 MB).
            val out = ByteArrayOutputStream()
            val buf = ByteArray(64 * 1024)
            var total = 0L
            while (true) {
                val n = input.read(buf)
                if (n < 0) break
                total += n
                if (total > Backup.MAX_BYTES) throw Damaged()
                out.write(buf, 0, n)
            }
            out.toString(Charsets.UTF_8.name())
        }
    }
}

/**
 * A proteção com senha dos backups deste conjunto de dados (RF-SEG-03). Guarda só a chave derivada, **trancada pelo cofre
 * do Android** (RF-SEG-01), e o sal; nunca a senha. Sem cofre que funcione, não liga.
 */
class BackupProtection(ctx: Context, sandbox: Boolean, private val vault: Vault) {
    private val prefs = ctx.getSharedPreferences(if (sandbox) "cryo_backup_lock_teste" else "cryo_backup_lock", Context.MODE_PRIVATE)
    private val _enabled = MutableStateFlow(prefs.contains(KEY))
    val enabled: StateFlow<Boolean> = _enabled

    /** Liga a proteção. Lento (deriva a chave): chame fora da linha principal. False se o cofre do aparelho não funciona. */
    fun enable(password: CharArray, iterations: Int = BackupCrypto.ITERATIONS): Boolean {
        val key = BackupCrypto.deriveKey(password, BackupCrypto.newSalt(), iterations)
        try {
            val wrapped = vault.wrap(key.bytes) ?: return false
            prefs.edit()
                .putString(KEY, wrapped)
                .putString(SALT, Base64.getEncoder().encodeToString(key.salt))
                .putInt(ITER, key.iterations)
                .apply()
            _enabled.value = true
            return true
        } finally {
            Arrays.fill(key.bytes, 0)
        }
    }

    fun disable() {
        prefs.edit().clear().apply()
        _enabled.value = false
    }

    /** A chave para trancar ou abrir backups, ou null se a proteção está desligada (ou o cofre não abre mais). */
    fun key(): BackupCrypto.Key? {
        val wrapped = prefs.getString(KEY, null) ?: return null
        val bytes = vault.unwrap(wrapped) ?: return null
        val salt = prefs.getString(SALT, null)?.let { runCatching { Base64.getDecoder().decode(it) }.getOrNull() } ?: return null
        return BackupCrypto.Key(bytes, salt, prefs.getInt(ITER, BackupCrypto.ITERATIONS))
    }

    /** A proteção está ligada, mas o cofre do aparelho não entregou a chave: nada é gravado sem senha. */
    class Unavailable : IllegalStateException(Res.s(R.string.sec_protection_unavailable))

    /**
     * O texto a gravar: trancado se a proteção está ligada. **Falha fechado**: com a proteção ligada e o cofre sem abrir a
     * chave (cofre do Android reiniciado, aparelho com defeito), lança [Unavailable] em vez de devolver o backup aberto.
     * (Até a 1.7 devolvia o texto sem senha: quem confiava na proteção podia mandar uma cópia aberta para a nuvem.)
     */
    fun seal(json: String, exportedAt: Long, testMode: Boolean = false): String {
        if (!prefs.contains(KEY)) return json
        val k = key() ?: throw Unavailable()
        try {
            return BackupCrypto.encrypt(json, k, exportedAt, testMode)
        } finally {
            Arrays.fill(k.bytes, 0) // a chave não fica na memória além da conta
        }
    }

    private companion object {
        const val KEY = "key"
        const val SALT = "salt"
        const val ITER = "iterations"
    }
}
