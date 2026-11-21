// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.voice

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.security.MessageDigest

/** RF-VOZ-15: o que aconteceu ao escolher um arquivo de modelo. */
sealed interface ModelInstall {
    /** Conferido (tamanho e SHA-256 de um dos modelos oficiais) e guardado. */
    data class Installed(val model: WhisperModel) : ModelInstall

    /** Falta espaço: [needed] bytes a liberar (com a folga que o Android precisa para continuar funcionando). */
    data class NoSpace(val needed: Long) : ModelInstall

    /** Tem o começo de um modelo do whisper.cpp, mas não bate com nenhum oficial: download cortado ou estragado. */
    data object Incomplete : ModelInstall

    /** Não é um modelo de voz (uma foto, um PDF, outro arquivo qualquer). */
    data object NotAModel : ModelInstall

    /** Não deu para ler o arquivo (permissão, nuvem fora do ar, arquivo que sumiu). */
    data object Unreadable : ModelInstall
}

/**
 * RF-VOZ-15 (1.6): o modelo de voz guardado no celular. Fica em `noBackupFilesDir/whisper/` (é do aparelho, não da
 * pessoa: não vai em backup nenhum nem passa para outro celular) e só entra depois de conferido inteiro, byte a byte,
 * contra o SHA-256 do arquivo oficial. Um modelo por vez: o de antes só sai quando o novo já está conferido.
 *
 * ERR-057: o modelo guardado é conferido de novo antes de cada uso (tamanho e o carimbo de quando foi guardado). Se
 * sumiu ou mudou, ele é tirado e a pessoa vê o aviso: o Whisper nunca abre um arquivo que não é o que foi conferido.
 * ERR-066 (auditoria da 1.6): no primeiro uso de cada abertura do app, o arquivo é conferido também pelo **SHA-256
 * inteiro** (um arquivo mexido que manteve o tamanho e a data passava); depois, enquanto o tamanho e a data forem os
 * mesmos, não é lido de novo (o modelo recomendado leva uns décimos de segundo, uma vez, enquanto o microfone liga). O
 * temporário de uma cópia interrompida (o Android fechou o app no meio) é apagado, e um modelo que bate com o SHA-256
 * nunca é apagado por não abrir ([openWith]).
 *
 * [freeSpace] = o espaço livre de uma pasta (troca nos testes).
 */
class WhisperModelStore(
    ctx: Context,
    val catalog: List<WhisperModel> = WhisperModels.ALL,
    private val freeSpace: (File) -> Long = { it.usableSpace },
) {
    private val app = ctx.applicationContext
    private val prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** A pasta do modelo (fora do backup do Android). */
    val dir: File = File(app.noBackupFilesDir, "whisper")

    /** O modelo instalado, se o arquivo continua exatamente como foi guardado (tamanho e data); senão, null. */
    fun installed(): WhisperModel? {
        dropStaleTemp()
        val id = prefs.getString(KEY_MODEL, null) ?: return null
        val model = catalog.firstOrNull { it.id == id } ?: return null
        val f = File(dir, model.file)
        return if (f.isFile && f.length() == model.size && f.lastModified() == prefs.getLong(KEY_STAMP, -1L)) model else null
    }

    /**
     * O arquivo para o Whisper abrir, ou null se não há modelo (lento na primeira vez de cada abertura do app: confere o
     * SHA-256 inteiro; chame fora da linha principal). ERR-057: havia um modelo guardado e ele não bate mais (apagado por
     * um "limpar" do sistema, trocado, cortado): sai da lista e fica o aviso para a pessoa. ERR-066: o mesmo vale para um
     * arquivo mexido que manteve o tamanho e a data. Um erro de leitura (não um arquivo diferente) só deixa de usar desta vez.
     */
    fun usableFile(): File? {
        val model = installed()
        if (model == null) {
            if (prefs.contains(KEY_MODEL)) markDamaged()
            return null
        }
        val f = File(dir, model.file)
        val stamp = stampOf(f)
        if (verified != stamp) {
            val sha = try { sha256(f) } catch (_: IOException) { return null } catch (_: SecurityException) { return null }
            if (sha != model.sha256) {
                markDamaged()
                return null
            }
            verified = stamp
        }
        return f
    }

    /**
     * Abre o modelo conferido com [open] (o Whisper). ERR-066: quando o arquivo bate com o SHA-256 e mesmo assim não abre
     * (falta de memória, por exemplo), o modelo **fica**: desta vez a voz usa o reconhecedor do celular, sem apagar nada
     * (antes ele era apagado como "estragado" e a pessoa precisava baixar 57 MB de novo à toa).
     */
    fun <T : Any> openWith(open: (File) -> T?): T? = usableFile()?.let(open)

    /** Apaga o temporário largado por uma cópia que não terminou (o Android fechou o app no meio), se não há cópia agora. */
    private fun dropStaleTemp() {
        if (installing.get() == 0) File(dir, TMP).takeIf { it.exists() }?.delete()
    }

    private fun stampOf(f: File) = "${f.absolutePath}|${f.length()}|${f.lastModified()}"

    private fun sha256(f: File): String {
        hashes.incrementAndGet()
        val digest = MessageDigest.getInstance("SHA-256")
        f.inputStream().use { input ->
            val buf = ByteArray(64 * 1024)
            while (true) {
                val n = input.read(buf)
                if (n < 0) break
                digest.update(buf, 0, n)
            }
        }
        return hex(digest.digest())
    }

    /** O modelo guardado não serve mais (não bateu, ou o Whisper não conseguiu abrir): sai, e a pessoa é avisada. */
    fun markDamaged() {
        remove()
        prefs.edit().putBoolean(KEY_DAMAGED, true).apply()
    }

    /** Houve um modelo que deixou de bater com o original (o aviso some quando outro é instalado ou removido). */
    val damaged: Boolean get() = prefs.getBoolean(KEY_DAMAGED, false)

    /** "Novidade da 1.6" já mostrada (aparece uma vez para quem atualizou e usava a voz). */
    var noticeSeen: Boolean
        get() = prefs.getBoolean(KEY_NOTICE, false)
        set(v) { prefs.edit().putBoolean(KEY_NOTICE, v).apply() }

    /** Tira o modelo (libera o espaço). */
    fun remove() {
        verified = null
        dir.listFiles()?.forEach { if (installing.get() == 0 || it.name != TMP) it.delete() }
        prefs.edit().remove(KEY_MODEL).remove(KEY_STAMP).putBoolean(KEY_DAMAGED, false).apply()
    }

    /** O arquivo escolhido no seletor do Android (lento: lê e confere tudo; chame fora da linha principal). */
    fun install(uri: Uri): ModelInstall {
        val resolver = app.contentResolver
        val size = runCatching {
            resolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { c ->
                if (c.moveToFirst() && !c.isNull(0)) c.getLong(0) else null
            }
        }.getOrNull()
        return install(size) { resolver.openInputStream(uri) }
    }

    /**
     * A conferência em si: [declaredSize] é o tamanho que o seletor informou (null se não sabe) e [open] abre o arquivo.
     * O arquivo é copiado para um temporário calculando o SHA-256 no caminho (nada de ler duas vezes 60 MB); só depois de
     * bater com um dos oficiais ele vira o modelo. Qualquer coisa no meio do caminho apaga o temporário.
     */
    internal fun install(declaredSize: Long?, open: () -> InputStream?): ModelInstall {
        // Enquanto a cópia roda, o temporário dela não é "largado" (ERR-066): ninguém apaga ele no meio do caminho.
        installing.incrementAndGet()
        try {
            return copyAndCheck(declaredSize, open)
        } finally {
            installing.decrementAndGet()
        }
    }

    private fun copyAndCheck(declaredSize: Long?, open: () -> InputStream?): ModelInstall {
        val maxSize = catalog.maxOf { it.size }
        // Maior que o maior modelo aceito: não é um deles (e não vale nem copiar).
        if (declaredSize != null && declaredSize > maxSize) return ModelInstall.NotAModel
        dir.mkdirs()
        // O modelo de antes continua no lugar até o novo ser conferido: o espaço precisa caber o novo inteiro, mais a
        // folga para o Android (sem ela o celular inteiro ficaria sem espaço no meio da cópia).
        val needed = (declaredSize ?: maxSize) + MARGIN
        val free = freeSpace(dir)
        if (free < needed) return ModelInstall.NoSpace(needed - free)

        val tmp = File(dir, TMP)
        val digest = MessageDigest.getInstance("SHA-256")
        val head = ByteArray(MAGIC.size)
        var total = 0L
        try {
            val input = open() ?: return ModelInstall.Unreadable
            input.use { src ->
                FileOutputStream(tmp).use { out ->
                    val buf = ByteArray(64 * 1024)
                    while (true) {
                        val n = src.read(buf)
                        if (n < 0) break
                        if (total < head.size) {
                            for (i in 0 until minOf(n, head.size - total.toInt())) head[total.toInt() + i] = buf[i]
                        }
                        total += n
                        // Passou do maior modelo aceito: para de copiar na hora.
                        if (total > maxSize) {
                            tmp.delete()
                            return ModelInstall.NotAModel
                        }
                        digest.update(buf, 0, n)
                        out.write(buf, 0, n)
                    }
                }
            }
        } catch (_: SecurityException) {
            tmp.delete()
            return ModelInstall.Unreadable
        } catch (_: IOException) {
            tmp.delete()
            // O jeito mais comum de a cópia falhar no meio é o espaço acabar (outro app gravou ao mesmo tempo).
            val now = freeSpace(dir)
            return if (now < needed) ModelInstall.NoSpace(needed - now) else ModelInstall.Unreadable
        }

        val hex = hex(digest.digest())
        val model = catalog.firstOrNull { it.sha256 == hex && it.size == total }
        if (model == null) {
            tmp.delete()
            // Começa como um modelo do whisper.cpp ("ggml"), mas não bate: veio cortado ou estragado.
            return if (head.contentEquals(MAGIC)) ModelInstall.Incomplete else ModelInstall.NotAModel
        }

        // Um modelo por vez: os outros saem agora, depois da conferência.
        val target = File(dir, model.file)
        dir.listFiles()?.forEach { if (it != tmp && it.name != model.file) it.delete() }
        if (target.exists()) target.delete()
        if (!tmp.renameTo(target)) {
            tmp.delete()
            return ModelInstall.Unreadable
        }
        prefs.edit()
            .putString(KEY_MODEL, model.id)
            .putLong(KEY_STAMP, target.lastModified())
            .putBoolean(KEY_DAMAGED, false)
            .apply()
        // O SHA-256 acabou de ser conferido na cópia: o primeiro uso não precisa ler o arquivo de novo.
        verified = stampOf(target)
        return ModelInstall.Installed(model)
    }

    companion object {
        /**
         * ERR-066: o arquivo (caminho, tamanho e data) que já passou pelo SHA-256 nesta abertura do app. Fica só na memória:
         * a cada abertura, o primeiro uso confere de novo.
         */
        @Volatile private var verified: String? = null

        /** Cópias de modelo em andamento (o temporário delas não é apagado como "largado"). */
        private val installing = java.util.concurrent.atomic.AtomicInteger(0)

        /** Quantas vezes um arquivo inteiro passou pelo SHA-256 depois de instalado (os testes conferem que é uma vez só). */
        internal val hashes = java.util.concurrent.atomic.AtomicInteger(0)

        /** Nos testes: como se o app tivesse acabado de abrir (nada conferido ainda nesta abertura). */
        internal fun newProcessForTests() { verified = null }

        /** As mesmas preferências da escolha do ouvido ([VoiceSettings]): do aparelho, fora do backup. */
        internal const val PREFS = "cryo_voice"
        internal const val KEY_MODEL = "model"
        internal const val KEY_STAMP = "model_stamp"
        internal const val KEY_DAMAGED = "model_damaged"
        internal const val KEY_NOTICE = "model_notice_16"
        internal const val TMP = "import.tmp"

        /** Folga de 50 MB além do modelo: o Android fica lento e falha com o armazenamento cheio. */
        const val MARGIN = 50L * 1024 * 1024

        /** Os 4 primeiros bytes de todo modelo do whisper.cpp ("ggml" ao contrário, como o número é gravado). */
        internal val MAGIC = byteArrayOf(0x6c, 0x6d, 0x67, 0x67)

        /** Hexadecimal minúsculo, sem depender da língua do celular (ERR-058: algarismos de outra escrita). */
        internal fun hex(bytes: ByteArray): String {
            val digits = "0123456789abcdef"
            val sb = StringBuilder(bytes.size * 2)
            for (b in bytes) sb.append(digits[(b.toInt() shr 4) and 0xf]).append(digits[b.toInt() and 0xf])
            return sb.toString()
        }
    }
}
