// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.annotation.VisibleForTesting
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * RF-SEG-01: o cofre do Cryo. As chaves ficam no cofre do Android (Android Keystore): o próprio Android guarda a chave e faz
 * as contas com ela, e ela nunca sai de lá (nem para o app). Duas chaves por conjunto de dados ([name]: os de verdade e os do
 * modo de teste):
 * - **HMAC-SHA256** ([hmac]): transforma um texto num código que não revela o texto e não dá para desfazer (RF-SEG-02).
 * - **AES-256-GCM** ([wrap]/[unwrap]): tranca a chave do backup com senha, para o backup automático (RF-SEG-03).
 *
 * Se o cofre do aparelho não funcionar (raro, aparelhos com defeito), o HMAC usa uma chave guardada só na área privada do
 * app ([hardware] = falso) e o [wrap] se recusa: nada que dependa do cofre é guardado sem ele.
 */
class Vault(private val ctx: Context, private val name: String) {
    private val keyStore: KeyStore? = runCatching { KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) } }.getOrNull()

    private val hmacKey: SecretKey by lazy { hardwareHmacKey() ?: softwareKey("hmac") }

    /** O cofre do aparelho está funcionando. */
    val hardware: Boolean get() = keyStore != null && hardwareHmacKey() != null

    /** Código HMAC-SHA256 (em hexadecimal) de [text]. O mesmo texto dá o mesmo código neste aparelho; em outro, outro. */
    fun hmac(text: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(hmacKey)
        return mac.doFinal(text.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
    }

    /** Tranca [secret] com a chave AES do cofre. Null se o cofre do aparelho não funciona (e nada é guardado). */
    fun wrap(secret: ByteArray): String? = runCatching {
        val key = aesKey() ?: return null
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        if (key.algorithm == "AES" && key is SecretKeySpec) {
            val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }
            c.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv))
        } else {
            c.init(Cipher.ENCRYPT_MODE, key) // o cofre escolhe o vetor inicial (sempre novo)
        }
        val out = c.iv + c.doFinal(secret)
        Base64.encodeToString(out, Base64.NO_WRAP)
    }.getOrNull()

    fun unwrap(blob: String): ByteArray? = runCatching {
        val key = aesKey() ?: return null
        val raw = Base64.decode(blob, Base64.NO_WRAP)
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, raw.copyOfRange(0, 12)))
        c.doFinal(raw, 12, raw.size - 12)
    }.getOrNull()

    private fun hardwareHmacKey(): SecretKey? = runCatching {
        val ks = keyStore ?: return null
        val alias = "$name.hmac"
        (ks.getKey(alias, null) as? SecretKey) ?: KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_HMAC_SHA256, ANDROID_KEYSTORE).run {
            init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_SIGN).build())
            generateKey()
        }
    }.getOrNull()

    private fun aesKey(): SecretKey? {
        val ks = keyStore
        if (ks != null) {
            val k = runCatching {
                val alias = "$name.wrap"
                (ks.getKey(alias, null) as? SecretKey) ?: KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE).run {
                    init(
                        KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                            .setKeySize(256)
                            .build(),
                    )
                    generateKey()
                }
            }.getOrNull()
            if (k != null) return k
        }
        return if (softwareWrapForTests) softwareKey("wrap") else null
    }

    /** Chave aleatória guardada na área privada do app (só quando o cofre do aparelho não funciona). */
    private fun softwareKey(kind: String): SecretKey {
        val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val k = "$name.$kind"
        val existing = prefs.getString(k, null)?.let { runCatching { Base64.decode(it, Base64.NO_WRAP) }.getOrNull() }
        val bytes = existing ?: ByteArray(32).also {
            SecureRandom().nextBytes(it)
            prefs.edit().putString(k, Base64.encodeToString(it, Base64.NO_WRAP)).apply()
        }
        return SecretKeySpec(bytes, if (kind == "hmac") "HmacSHA256" else "AES")
    }

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val PREFS = "cryo_vault"

        /** Nos testes (Robolectric não tem o cofre do Android), o [wrap] usa uma chave da área privada. */
        @VisibleForTesting @Volatile var softwareWrapForTests: Boolean = false
    }
}
