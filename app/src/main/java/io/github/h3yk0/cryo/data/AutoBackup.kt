// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.data

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.provider.DocumentsContract.Document
import android.provider.OpenableColumns
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.github.h3yk0.cryo.domain.Snapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.FileNotFoundException
import java.io.IOException
import java.security.MessageDigest
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/*
 * Backup automático.
 *
 * O Cryo não tem permissão de internet. Ele grava o arquivo de backup por meio do seletor de arquivos do Android
 * (Storage Access Framework): numa pasta do celular ou num arquivo de um app de nuvem (Google Drive, OneDrive,
 * Nextcloud...). Quem envia o arquivo para a internet é o app da nuvem.
 *
 * Regras de segurança (para um backup nunca destruir outro):
 * - Só se substitui ou apaga o que este celular gravou. Um arquivo que já estava no lugar, ou que outro celular ou
 *   app alterou depois, só é substituído depois que a pessoa confere e decide (ao escolher o lugar).
 * - Sem nada cadastrado, nada é gravado. Se as movimentações caem para menos da metade de repente, o backup
 *   automático pausa; o "Fazer backup agora" confirma que foi de propósito.
 * - Antes de gravar, a impressão digital do último backup é apagada: se a gravação falhar no meio (ou o app for
 *   fechado), a próxima conferência grava de novo em vez de achar que "nada mudou".
 */

enum class BackupFrequency(val days: Long, val label: String) {
    DAILY(1, "Todo dia"),
    WEEKLY(7, "Toda semana"),
    MONTHLY(30, "Todo mês"),
}

/** Na pasta: sempre o mesmo arquivo, ou um arquivo novo a cada backup. */
enum class FolderMode { REPLACE, COPIES }

enum class BackupTarget { FOLDER, CLOUD }

/** Um destino do backup e como foi o último. */
data class BackupTargetState(
    val uri: String? = null,
    /** Nome para mostrar na tela (ex.: "Documents/Cryo · no celular"). */
    val name: String = "",
    /** Quando o último arquivo foi gravado (é também a data escrita dentro dele, em "exportedAt"). */
    val lastAt: Long? = null,
    /** Impressão digital dos dados do último arquivo gravado (para não gravar cópias iguais). */
    val lastHash: String? = null,
    /** Quantas movimentações o último arquivo gravado tinha (para notar se muita coisa sumiu de repente). */
    val lastTxCount: Int? = null,
    /** Arquivos da pasta que o backup automático gravou neste celular: só esses podem ser substituídos ou apagados. */
    val owned: Set<String> = emptySet(),
    /** Data escrita no último cryo-backup.json (ou arquivo da nuvem) que este celular gravou. */
    val singleAt: Long? = null,
    /** Tentativas de gravar desde o último sucesso (se uma falhou no meio, o arquivo pode estar com essa data dentro). */
    val attempts: Set<Long> = emptySet(),
    /**
     * A pessoa conferiu o lugar e decidiu usá-lo: impressão digital do começo do arquivo que ela viu (vazio se não
     * havia arquivo; [BackupPolicy.ANY] se não deu para ler). Só esse conteúdo pode ser substituído no primeiro backup.
     */
    val adoptHead: String? = null,
    /** Quando foi a última conferência que achou tudo igual (nada para gravar). */
    val checkedAt: Long? = null,
    /** Por que o último backup não foi salvo (falha, ou o Cryo segurou para proteger um arquivo). */
    val lastError: String? = null,
    val lastErrorAt: Long? = null,
) {
    val isSet: Boolean get() = uri != null
    val adopting: Boolean get() = adoptHead != null
}

data class AutoBackupSettings(
    val enabled: Boolean = false,
    val frequency: BackupFrequency = BackupFrequency.DAILY,
    val folderMode: FolderMode = FolderMode.COPIES,
    /** Quantas cópias guardar na pasta (0 = todas). */
    val keep: Int = 10,
    val folder: BackupTargetState = BackupTargetState(),
    val cloud: BackupTargetState = BackupTargetState(),
) {
    val hasTarget: Boolean get() = folder.isSet || cloud.isSet
    val hasProblem: Boolean get() = folder.lastError != null || cloud.lastError != null
    fun target(t: BackupTarget) = if (t == BackupTarget.FOLDER) folder else cloud

    /** Neste destino cada backup apaga o anterior? Na nuvem sempre; na pasta, se for "substituir sempre o mesmo". */
    fun overwrites(t: BackupTarget): Boolean = t == BackupTarget.CLOUD || folderMode == FolderMode.REPLACE
}

/* ============================== Ajustes guardados ============================== */

private val Context.autoBackupStore by preferencesDataStore(
    "auto_backup",
    // Arquivo de ajustes estragado: começa de novo em vez de travar o app.
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
)

class AutoBackupStore(private val context: Context) {
    private object K {
        val enabled = booleanPreferencesKey("enabled")
        val frequency = stringPreferencesKey("frequency")
        val mode = stringPreferencesKey("folder_mode")
        val keep = intPreferencesKey("keep")
    }

    private class TargetKeys(prefix: String) {
        val uri = stringPreferencesKey("${prefix}_uri")
        val name = stringPreferencesKey("${prefix}_name")
        val lastAt = longPreferencesKey("${prefix}_last_at")
        val hash = stringPreferencesKey("${prefix}_hash")
        val txCount = intPreferencesKey("${prefix}_tx_count")
        val owned = stringSetPreferencesKey("${prefix}_owned")
        val singleAt = longPreferencesKey("${prefix}_single_at")
        val attempts = stringSetPreferencesKey("${prefix}_attempts")
        val adoptHead = stringPreferencesKey("${prefix}_adopt_head")
        val checkedAt = longPreferencesKey("${prefix}_checked_at")
        val error = stringPreferencesKey("${prefix}_error")
        val errorAt = longPreferencesKey("${prefix}_error_at")
        val history = listOf(lastAt, hash, txCount, owned, singleAt, attempts, adoptHead, checkedAt, error, errorAt)
    }

    private val folderKeys = TargetKeys("folder")
    private val cloudKeys = TargetKeys("cloud")
    private fun keys(t: BackupTarget) = if (t == BackupTarget.FOLDER) folderKeys else cloudKeys

    private fun Preferences.target(k: TargetKeys) = BackupTargetState(
        uri = this[k.uri], name = this[k.name] ?: "", lastAt = this[k.lastAt], lastHash = this[k.hash],
        lastTxCount = this[k.txCount], owned = this[k.owned] ?: emptySet(), singleAt = this[k.singleAt],
        attempts = this[k.attempts]?.mapNotNull { it.toLongOrNull() }?.toSet() ?: emptySet(), adoptHead = this[k.adoptHead],
        checkedAt = this[k.checkedAt], lastError = this[k.error], lastErrorAt = this[k.errorAt],
    )

    private fun Preferences.settings() = AutoBackupSettings(
        enabled = this[K.enabled] ?: false,
        frequency = this[K.frequency]?.let { runCatching { BackupFrequency.valueOf(it) }.getOrNull() } ?: BackupFrequency.DAILY,
        folderMode = this[K.mode]?.let { runCatching { FolderMode.valueOf(it) }.getOrNull() } ?: FolderMode.COPIES,
        keep = this[K.keep] ?: 10,
        folder = target(folderKeys),
        cloud = target(cloudKeys),
    )

    val flow: Flow<AutoBackupSettings> = context.autoBackupStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { it.settings() }

    suspend fun current(): AutoBackupSettings = flow.first()

    /** Como [current], mas devolve nulo se não der para ler (para nunca agir com base em ajustes vazios por engano). */
    suspend fun currentOrNull(): AutoBackupSettings? = runCatching { context.autoBackupStore.data.first().settings() }.getOrNull()

    suspend fun clear() = context.autoBackupStore.edit { it.clear() }
    suspend fun setEnabled(v: Boolean) = context.autoBackupStore.edit { it[K.enabled] = v }
    suspend fun setFrequency(f: BackupFrequency) = context.autoBackupStore.edit { it[K.frequency] = f.name }
    suspend fun setFolderMode(m: FolderMode) = context.autoBackupStore.edit { it[K.mode] = m.name }
    suspend fun setKeep(n: Int) = context.autoBackupStore.edit { it[K.keep] = n.coerceAtLeast(0) }

    /** Troca (ou tira, com uri nulo) o destino. O histórico do destino antigo é zerado. */
    suspend fun setTarget(t: BackupTarget, uri: String?, name: String) = context.autoBackupStore.edit { p ->
        val k = keys(t)
        k.history.forEach { p.remove(it) }
        if (uri == null) {
            p.remove(k.uri); p.remove(k.name)
        } else {
            p[k.uri] = uri; p[k.name] = name
        }
    }

    /** A pessoa conferiu o lugar e decidiu usá-lo; [head]: impressão digital do que ela viu (ver [BackupTargetState.adoptHead]). */
    suspend fun adopt(t: BackupTarget, head: String) = context.autoBackupStore.edit { it[keys(t).adoptHead] = head }

    /** Vai gravar: esquece a impressão digital, para uma gravação interrompida nunca passar por "nada mudou". */
    suspend fun markWriting(t: BackupTarget, at: Long) = context.autoBackupStore.edit { p ->
        val k = keys(t)
        p.remove(k.hash)
        p[k.attempts] = ((p[k.attempts] ?: emptySet()) + at.toString()).sortedBy { it.toLongOrNull() ?: 0 }.takeLast(50).toSet()
    }

    /** [single]: o arquivo gravado foi o cryo-backup.json da pasta ou o arquivo da nuvem. */
    suspend fun recordWritten(t: BackupTarget, at: Long, hash: String, txCount: Int, owned: Set<String>? = null, single: Boolean = false) =
        context.autoBackupStore.edit { p ->
            val k = keys(t)
            p[k.lastAt] = at; p[k.hash] = hash; p[k.txCount] = txCount
            if (owned != null) p[k.owned] = owned
            if (single) p[k.singleAt] = at
            listOf(k.error, k.errorAt, k.checkedAt, k.attempts, k.adoptHead).forEach { p.remove(it) }
        }

    suspend fun recordUnchanged(t: BackupTarget, at: Long) = context.autoBackupStore.edit { p ->
        val k = keys(t)
        p[k.checkedAt] = at; p.remove(k.error); p.remove(k.errorAt)
    }

    /** O Cryo segurou o backup (para proteger um arquivo). */
    suspend fun recordHeld(t: BackupTarget, at: Long, message: String) = context.autoBackupStore.edit { p ->
        val k = keys(t)
        p[k.error] = message; p[k.errorAt] = at
    }

    /** Não deu para gravar: o próximo backup grava de novo, mesmo que os dados não mudem. */
    suspend fun recordFailure(t: BackupTarget, at: Long, message: String) = context.autoBackupStore.edit { p ->
        val k = keys(t)
        p[k.error] = message; p[k.errorAt] = at; p.remove(k.hash)
        // "Substituir o que estiver lá" sem ter conseguido ler vale só para a tentativa logo depois da decisão.
        if (p[k.adoptHead] == BackupPolicy.ANY) p.remove(k.adoptHead)
    }
}

/* ================================ Regras (puras) ================================ */

/** O que fazer na pasta neste backup. */
sealed interface FolderPlan {
    /** Já existe um cryo-backup.json que não foi gravado por este celular: não substituir sem a pessoa decidir. */
    data object HoldForeign : FolderPlan

    /** Gravar [name]; depois apagar [prune]. [owned]: os arquivos deste celular que ficam na pasta. */
    data class Write(val name: String, val prune: List<String>, val owned: Set<String>) : FolderPlan
}

object BackupPolicy {
    /** Nome do arquivo quando o backup substitui sempre o mesmo. */
    const val SINGLE_NAME = "cryo-backup.json"
    private const val HOUR = 3_600_000L
    private const val DAY = 24 * HOUR
    private val COPY = Regex("""cryo-backup-\d{4}-\d{2}-\d{2}-\d{6}\.json""")
    private val STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmmss")
    private val EXPORTED_AT = Regex(""""exportedAt"\s*:\s*(\d+)""")

    /** Nome de uma cópia: a data e a hora no nome fazem a ordem alfabética ser a ordem do tempo. */
    fun copyName(now: LocalDateTime): String = "cryo-backup-${now.format(STAMP)}.json"

    /** Nome no formato das cópias automáticas. */
    fun isCopy(name: String): Boolean = COPY.matches(name)

    /** Qualquer backup do Cryo: o automático, as cópias e os salvos à mão (ex.: "cryo-backup-2026-10-02.json"). */
    fun isCryoFile(name: String): Boolean = name.startsWith("cryo-backup") && name.endsWith(".json")

    /** Cópias a apagar para ficar só com as [keep] mais novas (0 = guardar todas). */
    fun toPrune(names: Collection<String>, keep: Int): List<String> =
        if (keep <= 0) emptyList() else names.filter(::isCopy).sortedDescending().drop(keep)

    /**
     * Decide o que gravar e o que apagar na pasta. Só entram na conta os arquivos que este celular gravou ([owned]):
     * backups de outro celular, salvos à mão ou de antes nunca são apagados, nem substituídos sem [adopt]
     * (a pessoa acabou de conferir o lugar e decidir).
     */
    fun planFolder(
        names: Collection<String>,
        mode: FolderMode,
        keep: Int,
        owned: Set<String>,
        now: LocalDateTime,
        adopt: Boolean,
    ): FolderPlan {
        val mine = names.filter { it in owned }.toSet()
        return when (mode) {
            FolderMode.REPLACE ->
                if (SINGLE_NAME in names && SINGLE_NAME !in owned && !adopt) FolderPlan.HoldForeign
                else FolderPlan.Write(SINGLE_NAME, emptyList(), mine + SINGLE_NAME)
            FolderMode.COPIES -> {
                val name = copyName(now)
                val all = mine + name
                val prune = toPrune(all, keep)
                FolderPlan.Write(name, prune, all - prune.toSet())
            }
        }
    }

    /** Marca de "substituir o que estiver lá" (quando não deu para ler o arquivo antes de a pessoa decidir). */
    const val ANY = "*"

    /** A data escrita no começo de um backup do Cryo. */
    fun exportedAt(head: String): Long? = EXPORTED_AT.find(head.take(400))?.groupValues?.get(1)?.toLongOrNull()

    /**
     * Pode substituir este arquivo? Sim se estiver vazio, se for o que a pessoa viu e decidiu substituir, ou se ainda for
     * o último que este celular gravou (ou tentou gravar). Se outro celular ou app gravou nele depois, a data é outra.
     */
    fun stillOurs(head: String, s: BackupTargetState): Boolean {
        if (head.isBlank()) return true
        if (s.adoptHead != null && (s.adoptHead == ANY || s.adoptHead == hash(head))) return true
        val at = exportedAt(head) ?: return false
        return at == s.singleAt || at in s.attempts
    }

    /** Está na hora? Uma hora de folga para o horário não escorregar um pouco a cada dia. */
    fun isDue(lastAt: Long?, frequency: BackupFrequency, now: Long): Boolean =
        lastAt == null || now - lastAt >= frequency.days * DAY - HOUR

    fun hash(text: String): String =
        MessageDigest.getInstance("SHA-256").digest(text.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }

    /** Impressão digital só dos dados (sem a hora do backup). */
    fun dataHash(s: Snapshot): String = hash(Backup.export(s, exportedAt = 0))

    /** Sem nada cadastrado não há o que proteger, e um backup vazio poderia substituir um bom. */
    fun hasData(s: Snapshot): Boolean =
        s.txs.isNotEmpty() || s.accounts.isNotEmpty() || s.cards.isNotEmpty() || s.goals.isNotEmpty() ||
            s.bills.isNotEmpty() || s.debts.isNotEmpty() || s.investments.isNotEmpty()

    /**
     * Sumiu muita coisa desde o último backup (ex.: "Apagar todos os dados" sem querer)? Então o backup automático
     * pausa, para não trocar nem empurrar para fora os backups bons; só o "Fazer backup agora" grava.
     */
    fun shrankTooMuch(lastCount: Int?, nowCount: Int): Boolean =
        lastCount != null && lastCount >= 20 && nowCount * 2 < lastCount
}

/* ============================== Gravação dos arquivos ============================== */

/** Um arquivo dentro da pasta do backup. */
data class FolderFile(val name: String, val uri: Uri, val modified: Long)

interface BackupStorage {
    /** Os arquivos da pasta. */
    fun list(treeUri: Uri): List<FolderFile>
    /** Cria o arquivo [name] na pasta e grava. Se a gravação falhar, apaga o arquivo criado (nada de backup vazio). */
    fun create(treeUri: Uri, name: String, json: String): Uri
    /** Substitui o conteúdo de um arquivo que já existe (na pasta ou na nuvem). */
    fun overwrite(uri: Uri, json: String)
    fun delete(uri: Uri)
    fun read(uri: Uri): String?
    /** Só o começo do arquivo (para conferir a data dentro dele sem ler tudo). */
    fun readStart(uri: Uri): String?
}

/** Ler e gravar um arquivo escolhido pelo seletor de arquivos do Android. */
object SafFiles {
    /**
     * "wt" apaga o conteúdo anterior antes de gravar. Nem toda nuvem aceita esse modo (e cada uma recusa de um
     * jeito), então tenta "w" em seguida; se "w" também falhar, o erro dele é o que vale.
     */
    fun write(resolver: ContentResolver, uri: Uri, text: String) {
        val bytes = text.toByteArray(Charsets.UTF_8)
        val out = try {
            resolver.openOutputStream(uri, "wt")
        } catch (e: Exception) {
            null
        } ?: resolver.openOutputStream(uri, "w") ?: throw IOException("Não foi possível abrir o arquivo")
        out.use { it.write(bytes); it.flush() }
    }

    fun read(resolver: ContentResolver, uri: Uri): String? =
        resolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }

    fun readStart(resolver: ContentResolver, uri: Uri, max: Int = 512): String? =
        resolver.openInputStream(uri)?.use { input ->
            val buf = ByteArray(max)
            var n = 0
            while (n < max) {
                val r = input.read(buf, n, max - n)
                if (r < 0) break
                n += r
            }
            String(buf, 0, n, Charsets.UTF_8)
        }
}

/** Grava pelo seletor de arquivos do Android (pasta do celular ou app de nuvem). */
class SafBackupStorage(private val context: Context) : BackupStorage {
    private val resolver get() = context.contentResolver

    override fun list(treeUri: Uri): List<FolderFile> {
        val parentId = DocumentsContract.getTreeDocumentId(treeUri)
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentId)
        val cols = arrayOf(Document.COLUMN_DOCUMENT_ID, Document.COLUMN_DISPLAY_NAME, Document.COLUMN_LAST_MODIFIED)
        // Forma de consulta com Bundle: é a que os provedores de documentos (pasta, nuvem) entendem.
        val cursor = resolver.query(childrenUri, cols, null as Bundle?, null) ?: throw FileNotFoundException("Pasta indisponível")
        return cursor.use { c ->
            // Busca as colunas pelo nome: alguns provedores devolvem mais colunas, ou em outra ordem.
            val idCol = c.getColumnIndex(Document.COLUMN_DOCUMENT_ID)
            val nameCol = c.getColumnIndex(Document.COLUMN_DISPLAY_NAME)
            val modCol = c.getColumnIndex(Document.COLUMN_LAST_MODIFIED)
            if (idCol < 0 || nameCol < 0) throw IOException("A pasta não informou os arquivos")
            buildList {
                while (c.moveToNext()) {
                    val id = c.getString(idCol) ?: continue
                    val name = c.getString(nameCol) ?: continue
                    val modified = if (modCol >= 0 && !c.isNull(modCol)) c.getLong(modCol) else 0L
                    add(FolderFile(name, DocumentsContract.buildDocumentUriUsingTree(treeUri, id), modified))
                }
            }
        }
    }

    override fun create(treeUri: Uri, name: String, json: String): Uri {
        val parent = DocumentsContract.buildDocumentUriUsingTree(treeUri, DocumentsContract.getTreeDocumentId(treeUri))
        val doc = DocumentsContract.createDocument(resolver, parent, "application/json", name)
            ?: throw IOException("Não foi possível criar o arquivo")
        try {
            SafFiles.write(resolver, doc, json)
        } catch (e: Exception) {
            runCatching { DocumentsContract.deleteDocument(resolver, doc) }
            throw e
        }
        return doc
    }

    override fun overwrite(uri: Uri, json: String) = SafFiles.write(resolver, uri, json)

    override fun delete(uri: Uri) {
        if (!DocumentsContract.deleteDocument(resolver, uri)) throw IOException("Não foi possível apagar")
    }

    override fun read(uri: Uri): String? = SafFiles.read(resolver, uri)

    override fun readStart(uri: Uri): String? = SafFiles.readStart(resolver, uri)
}

/* ============================== Quem faz o backup ============================== */

enum class BackupStatus {
    WRITTEN, UNCHANGED, NOT_DUE, NOTHING_TO_SAVE,
    /** O Cryo não gravou para proteger um backup (sumiu muita coisa, ou o arquivo não é deste celular). */
    HELD,
    FAILED,
}

data class BackupOutcome(val target: BackupTarget, val status: BackupStatus, val message: String? = null, val fileName: String? = null)

/** Um backup do Cryo que já estava no lugar escolhido, com dados diferentes dos deste celular. */
data class FoundBackup(val restored: Backup.Restored, val currentTxs: Int, val currentAccounts: Int) {
    val txs: Int get() = restored.snapshot.txs.size
    val accounts: Int get() = restored.snapshot.accounts.size
    val exportedAt: Long? get() = restored.exportedAt

    /** O backup tem mais movimentações que o celular: o mais provável é querer restaurar. */
    val richer: Boolean get() = txs > currentTxs
}

/** O que já existe no lugar que a pessoa acabou de escolher. */
sealed interface PlaceCheck {
    /** Nada a proteger: pode usar direto. */
    data object Clear : PlaceCheck

    /** Um backup do Cryo diferente dos dados deste celular. [atRisk]: usar o lugar substituiria esse arquivo. */
    data class HasBackup(val found: FoundBackup, val atRisk: Boolean) : PlaceCheck

    /** O arquivo que seria substituído não pôde ser lido ou não é um backup do Cryo: a pessoa confirma. */
    data class CantRead(val reason: String) : PlaceCheck

    /** Não deu nem para ver o que tem no lugar. */
    data class Unreachable(val reason: String) : PlaceCheck
}

class AutoBackupRunner(
    private val repo: FinanceRepository,
    private val store: AutoBackupStore,
    private val storage: BackupStorage,
    private val clock: () -> Long = System::currentTimeMillis,
    private val zone: () -> ZoneId = ZoneId::systemDefault,
) {
    private val mutex = Mutex()

    /**
     * [force]: grava agora, mesmo fora do horário, sem mudanças ou depois de sumir muita coisa ("Fazer backup agora").
     * Nunca substitui um arquivo que não é deste celular: isso só depois que a pessoa confere o lugar ([adopt]).
     * [only]: só um destino.
     */
    suspend fun run(force: Boolean, only: BackupTarget? = null): List<BackupOutcome> = mutex.withLock {
        withContext(Dispatchers.IO) { runLocked(force, only) }
    }

    /**
     * A pessoa conferiu o lugar e decidiu usá-lo: guarda a impressão digital do arquivo que ela viu, que o primeiro
     * backup pode substituir (e só ele: se outro conteúdo aparecer lá depois, o Cryo segura de novo).
     */
    suspend fun adopt(t: BackupTarget, uri: Uri, mode: FolderMode) = withContext(Dispatchers.IO) {
        val head = try {
            val file = when {
                t == BackupTarget.CLOUD -> uri
                mode == FolderMode.REPLACE -> storage.list(uri).firstOrNull { it.name == BackupPolicy.SINGLE_NAME }?.uri
                else -> null
            }
            val text = file?.let { storage.readStart(it) }.orEmpty()
            if (text.isBlank()) "" else BackupPolicy.hash(text)
        } catch (e: Exception) {
            BackupPolicy.ANY
        }
        store.adopt(t, head)
    }

    private suspend fun runLocked(force: Boolean, only: BackupTarget?): List<BackupOutcome> {
        val st = store.current()
        if (!force && !st.enabled) return emptyList()
        val targets = BackupTarget.entries.filter { (only == null || it == only) && st.target(it).isSet }
        if (targets.isEmpty()) return emptyList()
        val now = clock()
        // Depois de um problema, tenta de novo na próxima conferência, sem esperar o dia/semana/mês.
        val due = targets.filter {
            val s = st.target(it)
            force || s.lastError != null || BackupPolicy.isDue(s.lastAt, st.frequency, now)
        }
        if (due.isEmpty()) return targets.map { BackupOutcome(it, BackupStatus.NOT_DUE) }

        val snap = try {
            repo.loadOnce()
        } catch (e: Exception) {
            return due.map { BackupOutcome(it, BackupStatus.FAILED, BackupErrors.READ_DATA) }
        }
        if (!BackupPolicy.hasData(snap)) return due.map { BackupOutcome(it, BackupStatus.NOTHING_TO_SAVE) }
        val hash = BackupPolicy.dataHash(snap)
        val json = Backup.export(snap, exportedAt = now)
        val localNow = LocalDateTime.ofInstant(Instant.ofEpochMilli(now), zone())
        return due.map { t -> backupOne(t, st, force, snap, hash, json, now, localNow) }
    }

    private suspend fun backupOne(
        t: BackupTarget,
        st: AutoBackupSettings,
        force: Boolean,
        snap: Snapshot,
        hash: String,
        json: String,
        now: Long,
        localNow: LocalDateTime,
    ): BackupOutcome {
        val state = st.target(t)
        return try {
            if (!force && state.lastHash == hash) {
                store.recordUnchanged(t, now)
                return BackupOutcome(t, BackupStatus.UNCHANGED)
            }
            if (!force && BackupPolicy.shrankTooMuch(state.lastTxCount, snap.txs.size)) {
                return held(t, now, BackupErrors.shrank(state.lastTxCount ?: 0, snap.txs.size))
            }
            val uri = Uri.parse(state.uri)
            when (t) {
                BackupTarget.FOLDER -> {
                    val files = storage.list(uri)
                    when (val plan = BackupPolicy.planFolder(files.map { it.name }, st.folderMode, st.keep, state.owned, localNow, state.adopting)) {
                        FolderPlan.HoldForeign -> held(t, now, BackupErrors.FOREIGN)
                        is FolderPlan.Write -> {
                            val existing = files.firstOrNull { it.name == plan.name }
                            if (existing != null && plan.name == BackupPolicy.SINGLE_NAME && !stillOurs(existing.uri, state)) {
                                return held(t, now, BackupErrors.CHANGED_FOLDER)
                            }
                            store.markWriting(t, now)
                            if (existing != null) storage.overwrite(existing.uri, json) else storage.create(uri, plan.name, json)
                            // Só apaga as antigas depois que a nova foi gravada; se não conseguir apagar, tenta da próxima vez.
                            val kept = plan.prune.filterNot { n ->
                                val f = files.firstOrNull { it.name == n }
                                f == null || runCatching { storage.delete(f.uri) }.isSuccess
                            }
                            store.recordWritten(t, now, hash, snap.txs.size, plan.owned + kept, single = plan.name == BackupPolicy.SINGLE_NAME)
                            BackupOutcome(t, BackupStatus.WRITTEN, fileName = plan.name)
                        }
                    }
                }
                BackupTarget.CLOUD -> {
                    if (!stillOurs(uri, state)) return held(t, now, BackupErrors.CHANGED_CLOUD)
                    store.markWriting(t, now)
                    storage.overwrite(uri, json)
                    store.recordWritten(t, now, hash, snap.txs.size, single = true)
                    BackupOutcome(t, BackupStatus.WRITTEN)
                }
            }
        } catch (e: Exception) {
            val msg = BackupErrors.message(e, t)
            runCatching { store.recordFailure(t, now, msg) }
            BackupOutcome(t, BackupStatus.FAILED, msg)
        }
    }

    /** Não deu para conferir o arquivo antes de substituir (ex.: nuvem sem conexão). */
    class CheckFailed(cause: Throwable) : IOException(cause)

    private fun stillOurs(uri: Uri, s: BackupTargetState): Boolean {
        val head = try {
            storage.readStart(uri)
        } catch (e: Exception) {
            throw CheckFailed(e)
        }
        return BackupPolicy.stillOurs(head.orEmpty(), s)
    }

    private suspend fun held(t: BackupTarget, now: Long, msg: String): BackupOutcome {
        store.recordHeld(t, now, msg)
        return BackupOutcome(t, BackupStatus.HELD, msg)
    }

    /**
     * Antes de passar a usar um lugar: o que já existe nele? Assim nenhum backup é substituído sem a pessoa decidir,
     * e dá para restaurar dali mesmo (ex.: celular novo apontando para a nuvem do celular antigo).
     * [mode]: como a pasta vai ser usada (no "substituir sempre o mesmo", o cryo-backup.json que já estiver lá corre risco).
     */
    suspend fun inspect(t: BackupTarget, uri: Uri, mode: FolderMode): PlaceCheck = withContext(Dispatchers.IO) {
        val atRisk: Uri?
        val others: List<Uri>
        if (t == BackupTarget.CLOUD) {
            atRisk = uri
            others = emptyList()
        } else {
            val files = try {
                storage.list(uri)
            } catch (e: Exception) {
                return@withContext PlaceCheck.Unreachable(BackupErrors.CANT_OPEN_FOLDER)
            }
            atRisk = if (mode == FolderMode.REPLACE) files.firstOrNull { it.name == BackupPolicy.SINGLE_NAME }?.uri else null
            others = files.filter { BackupPolicy.isCryoFile(it.name) && it.uri != atRisk }
                .sortedWith(compareByDescending<FolderFile> { it.modified }.thenByDescending { it.name })
                .take(3).map { it.uri }
        }
        val current = repo.loadOnce()
        val currentHash = BackupPolicy.dataHash(current)
        fun differs(r: Backup.Restored) = BackupPolicy.hasData(r.snapshot) && BackupPolicy.dataHash(r.snapshot) != currentHash
        fun found(r: Backup.Restored, risk: Boolean) = PlaceCheck.HasBackup(FoundBackup(r, current.txs.size, current.accounts.size), risk)

        if (atRisk != null) {
            val text = try {
                storage.read(atRisk)
            } catch (e: Exception) {
                return@withContext PlaceCheck.CantRead(BackupErrors.CANT_READ)
            }
            if (!text.isNullOrBlank()) {
                when (val p = parse(text)) {
                    is Parsed.Ok -> if (differs(p.restored)) return@withContext found(p.restored, true)
                    is Parsed.Broken -> return@withContext PlaceCheck.CantRead(p.reason)
                    // Na nuvem, o seletor do Android já perguntou se podia substituir esse arquivo; na pasta, não.
                    Parsed.NotCryo -> if (t == BackupTarget.FOLDER) return@withContext PlaceCheck.CantRead(BackupErrors.NOT_CRYO)
                }
            }
        }
        for (u in others) {
            val text = runCatching { storage.read(u) }.getOrNull() ?: continue
            val restored = (parse(text) as? Parsed.Ok)?.restored ?: continue
            if (differs(restored)) return@withContext found(restored, false)
            break // o backup mais novo é igual a este celular: nada a oferecer
        }
        PlaceCheck.Clear
    }

    private sealed interface Parsed {
        data class Ok(val restored: Backup.Restored) : Parsed
        data class Broken(val reason: String) : Parsed
        data object NotCryo : Parsed
    }

    private fun parse(text: String): Parsed = try {
        Parsed.Ok(Backup.import(text))
    } catch (e: Exception) {
        when {
            // O começo do arquivo diz que é do Cryo: é um backup, só que não deu para ler.
            !text.take(300).contains("\"Cryo\"") -> Parsed.NotCryo
            e.message?.contains("versão mais nova") == true -> Parsed.Broken(BackupErrors.NEWER)
            else -> Parsed.Broken(BackupErrors.DAMAGED)
        }
    }
}

object BackupErrors {
    const val FOREIGN = "Esta pasta já tem um arquivo cryo-backup.json que não foi feito por este celular. Para não apagar " +
        "esse arquivo, o Cryo não o substituiu. Toque em Trocar pasta e escolha esta mesma pasta para decidir o que fazer, " +
        "ou escolha Guardar várias cópias."
    const val CHANGED_FOLDER = "O arquivo cryo-backup.json da pasta foi alterado por outro celular ou app depois do último " +
        "backup. Para não apagar essa versão, o Cryo não o substituiu. Toque em Trocar pasta e escolha esta mesma pasta " +
        "para decidir o que fazer."
    const val CHANGED_CLOUD = "O arquivo na nuvem foi alterado por outro celular ou app depois do último backup. Para não " +
        "apagar essa versão, o Cryo não o substituiu. Toque em Trocar e escolha o mesmo arquivo para decidir o que fazer."
    const val CANT_READ = "Não consegui ler o backup que já está nesse lugar (a nuvem pode estar sem conexão)."
    const val NEWER = "O backup que já está nesse lugar é de uma versão mais nova do Cryo."
    const val DAMAGED = "O backup que já está nesse lugar parece estar danificado."
    const val NOT_CRYO = "Esta pasta já tem um arquivo cryo-backup.json que não é um backup do Cryo."
    const val CANT_OPEN_FOLDER = "Não consegui abrir essa pasta. Tente de novo ou escolha outra."
    const val READ_DATA = "Não consegui ler os dados do Cryo agora. Vou tentar de novo mais tarde."
    const val CANT_CHECK = "Não consegui abrir o arquivo para conferir antes de gravar (a nuvem pode estar sem conexão, " +
        "ou ele foi apagado). Vou tentar de novo mais tarde; se continuar assim, escolha o lugar de novo."

    fun message(e: Throwable, t: BackupTarget): String {
        val where = if (t == BackupTarget.FOLDER) "a pasta" else "o arquivo"
        return when {
            e is SecurityException || e.cause is SecurityException -> "O Cryo perdeu a permissão para usar $where. Escolha de novo."
            e is AutoBackupRunner.CheckFailed -> CANT_CHECK
            e is FileNotFoundException || e is IllegalArgumentException ->
                if (t == BackupTarget.FOLDER) "A pasta não existe mais. Escolha outra." else "O arquivo não existe mais. Escolha de novo."
            else -> "Não consegui gravar (sem espaço ou a nuvem não respondeu). Vou tentar de novo mais tarde."
        }
    }

    fun shrank(before: Int, now: Int): String =
        "As movimentações caíram de $before para $now desde o último backup. Para proteger os backups que você já tem, " +
            "o backup automático ficou pausado. Se foi de propósito, toque em Fazer backup agora."
}

/* ============================== Nomes e permissões ============================== */

object BackupPlaces {
    private val known = mapOf(
        "com.android.externalstorage.documents" to "no celular",
        "com.android.providers.downloads.documents" to "Downloads",
        "com.google.android.apps.docs.storage" to "Google Drive",
        "com.microsoft.skydrive.content.StorageAccessProvider" to "OneDrive",
        "org.nextcloud.documents" to "Nextcloud",
        "com.owncloud.android.providers" to "ownCloud",
        "com.dropbox.product.android.dbapp.document_provider.documents" to "Dropbox",
    )

    /** Ex.: "Documents/Cryo · no celular" ou "Backups · Nextcloud". */
    fun describeFolder(context: Context, treeUri: Uri): String {
        val docId = runCatching { DocumentsContract.getTreeDocumentId(treeUri) }.getOrNull()
        if (treeUri.authority == "com.android.externalstorage.documents" && docId != null) {
            val volume = docId.substringBefore(':')
            val path = docId.substringAfter(':', "").ifBlank { "pasta principal" }
            return if (volume == "primary") "$path · no celular" else "$path · cartão de memória"
        }
        val name = docId?.let { queryName(context, DocumentsContract.buildDocumentUriUsingTree(treeUri, it)) } ?: "Pasta"
        return "$name · ${providerName(context, treeUri)}"
    }

    /** Ex.: "cryo-backup.json · Google Drive". */
    fun describeFile(context: Context, uri: Uri): String {
        val name = queryName(context, uri) ?: BackupPolicy.SINGLE_NAME
        return "$name · ${providerName(context, uri)}"
    }

    private fun providerName(context: Context, uri: Uri): String {
        val authority = uri.authority ?: return "nuvem"
        known[authority]?.let { return it }
        return runCatching {
            val pm = context.packageManager
            pm.resolveContentProvider(authority, 0)?.loadLabel(pm)?.toString()
        }.getOrNull() ?: "nuvem"
    }

    private fun queryName(context: Context, uri: Uri): String? = runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null as Bundle?, null)?.use { c ->
            val col = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (col >= 0 && c.moveToFirst()) c.getString(col)?.takeIf { it.isNotBlank() } else null
        }
    }.getOrNull()

    const val RW = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION

    /** Guarda a permissão para usar o lugar depois, mesmo com o app fechado ou após reiniciar o celular. */
    fun keepAccess(context: Context, uri: Uri) = context.contentResolver.takePersistableUriPermission(uri, RW)

    fun releaseAccess(context: Context, uri: String?) {
        if (uri == null) return
        runCatching { context.contentResolver.releasePersistableUriPermission(Uri.parse(uri), RW) }
    }

    /**
     * Ao abrir o app: devolve as permissões de lugares que não são mais usados (ex.: escolha interrompida) e marca os
     * destinos que perderam a permissão (ex.: dados trazidos de outro celular), para a pessoa escolher de novo.
     * Devolve os destinos que acabaram de perder a permissão (para avisar).
     */
    suspend fun audit(context: Context, store: AutoBackupStore, now: Long = System.currentTimeMillis()): List<BackupOutcome> {
        // Sem conseguir ler os ajustes, não mexe em nada (senão devolveria permissões que ainda estão em uso).
        val st = store.currentOrNull() ?: return emptyList()
        val resolver = context.contentResolver
        val grants = resolver.persistedUriPermissions
        val used = setOfNotNull(st.folder.uri, st.cloud.uri)
        grants.filter { it.uri.toString() !in used }.forEach { g ->
            val flags = (if (g.isReadPermission) Intent.FLAG_GRANT_READ_URI_PERMISSION else 0) or
                (if (g.isWritePermission) Intent.FLAG_GRANT_WRITE_URI_PERMISSION else 0)
            runCatching { resolver.releasePersistableUriPermission(g.uri, flags) }
        }
        val writable = grants.filter { it.isWritePermission }.map { it.uri.toString() }.toSet()
        return BackupTarget.entries.mapNotNull { t ->
            val s = st.target(t)
            if (s.uri == null || s.uri in writable || s.lastError != null) return@mapNotNull null
            val msg = BackupErrors.message(SecurityException(), t)
            store.recordFailure(t, now, msg)
            BackupOutcome(t, BackupStatus.FAILED, msg)
        }
    }
}
