// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo

import android.content.Context
import android.provider.DocumentsContract
import androidx.test.core.app.ApplicationProvider
import io.github.h3yk0.cryo.data.AutoBackupRunner
import io.github.h3yk0.cryo.data.Backup
import io.github.h3yk0.cryo.data.BackupErrors
import io.github.h3yk0.cryo.data.BackupFrequency
import io.github.h3yk0.cryo.data.BackupPlaces
import io.github.h3yk0.cryo.data.BackupPolicy
import io.github.h3yk0.cryo.data.BackupStatus
import io.github.h3yk0.cryo.data.BackupTarget
import io.github.h3yk0.cryo.data.FolderMode
import io.github.h3yk0.cryo.data.FolderPlan
import io.github.h3yk0.cryo.data.PlaceCheck
import io.github.h3yk0.cryo.data.SafBackupStorage
import io.github.h3yk0.cryo.data.db.Account
import io.github.h3yk0.cryo.data.db.Tx
import io.github.h3yk0.cryo.data.db.TxType
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
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
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/** Backup automático de ponta a ponta: grava de verdade através de um provedor de documentos (como a pasta do celular ou uma nuvem). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = TestApp::class)
class AutoBackupTest {
    private lateinit var ctx: Context
    private lateinit var c: AppContainer
    private lateinit var dir: File
    private lateinit var runner: AutoBackupRunner
    private val zone = ZoneId.of("America/Sao_Paulo")
    private var now = LocalDateTime.of(2026, 10, 2, 22, 0).atZone(zone).toInstant().toEpochMilli()
    private val hour = 3_600_000L
    private val tree get() = DocumentsContract.buildTreeDocumentUri(TestDocsProvider.AUTHORITY, TestDocsProvider.ROOT)

    @Before fun setUp() = runBlocking {
        ctx = ApplicationProvider.getApplicationContext()
        c = AppContainer(ApplicationProvider.getApplicationContext())
        c.repo.ensureSeeded()
        dir = File(ctx.cacheDir, "docs-${System.nanoTime()}")
        TestDocsProvider.install(dir)
        c.autoBackup.clear()
        runner = AutoBackupRunner(c.repo, c.autoBackup, SafBackupStorage(ctx), clock = { now }, zone = { zone })
    }

    private suspend fun addExpense(cents: Long, day: Int = 1): Long {
        val acc = c.repo.loadOnce().accounts.firstOrNull()?.id ?: c.repo.saveAccount(Account(name = "Conta", initialBalance = 100_000, color = 0))
        return c.repo.saveTx(Tx(type = TxType.EXPENSE, amount = cents, date = LocalDate.of(2026, 10, day), accountId = acc, description = "Gasto $cents"))
    }

    private fun names() = dir.list()!!.sorted()
    private fun copies() = names().filter(BackupPolicy::isCopy)

    @Test fun regras() {
        val day = 24 * hour
        assertTrue(BackupPolicy.isDue(null, BackupFrequency.DAILY, now))
        assertFalse(BackupPolicy.isDue(now - 22 * hour, BackupFrequency.DAILY, now))
        assertTrue(BackupPolicy.isDue(now - 23 * hour, BackupFrequency.DAILY, now)) // uma hora de folga
        assertFalse(BackupPolicy.isDue(now - 6 * day, BackupFrequency.WEEKLY, now))
        assertTrue(BackupPolicy.isDue(now - 7 * day, BackupFrequency.WEEKLY, now))
        assertFalse(BackupPolicy.isDue(now - 20 * day, BackupFrequency.MONTHLY, now))
        assertTrue(BackupPolicy.isDue(now - 30 * day, BackupFrequency.MONTHLY, now))

        val name = BackupPolicy.copyName(LocalDateTime.of(2026, 10, 2, 3, 4, 5))
        assertEquals("cryo-backup-2026-10-02-030405.json", name)
        assertTrue(BackupPolicy.isCopy(name))
        assertFalse(BackupPolicy.isCopy("cryo-backup-2026-10-02.json")) // backup manual: nunca é apagado
        assertFalse(BackupPolicy.isCopy("cryo-backup.json"))
        val names = listOf(
            "cryo-backup-2026-09-30-030000.json", "notas.txt", "cryo-backup-2026-10-02-030000.json",
            "cryo-backup-2026-10-01-030000.json", "cryo-backup-2026-09-01.json",
        )
        assertEquals(listOf("cryo-backup-2026-09-30-030000.json"), BackupPolicy.toPrune(names, 2))
        assertTrue(BackupPolicy.toPrune(names, 0).isEmpty())
    }

    @Test fun pastaComVariasCopias() = runBlocking {
        addExpense(2_500)
        c.autoBackup.setTarget(BackupTarget.FOLDER, tree.toString(), "Backups")
        c.autoBackup.setEnabled(true)
        c.autoBackup.setFolderMode(FolderMode.COPIES)
        c.autoBackup.setKeep(2)
        File(dir, "notas.txt").writeText("não mexer")
        File(dir, "cryo-backup-2026-09-01.json").writeText("{}") // um backup manual antigo

        assertEquals(BackupStatus.WRITTEN, runner.run(force = false).single().status)
        assertEquals(1, copies().size)
        assertEquals(BackupStatus.NOT_DUE, runner.run(force = false).single().status) // ainda não deu um dia

        now += 24 * hour
        assertEquals(BackupStatus.UNCHANGED, runner.run(force = false).single().status) // nada mudou: não grava igual
        assertEquals(1, copies().size)
        assertNotNull(c.autoBackup.current().folder.checkedAt)

        addExpense(1_000)
        assertEquals(BackupStatus.WRITTEN, runner.run(force = false).single().status)
        now += 24 * hour
        addExpense(700)
        assertEquals(BackupStatus.WRITTEN, runner.run(force = false).single().status)

        // guarda só as 2 mais novas; os outros arquivos da pasta continuam lá
        assertEquals(2, copies().size)
        assertTrue("notas.txt" in names() && "cryo-backup-2026-09-01.json" in names())
        val newest = File(dir, copies().last()).readText()
        assertEquals(3, Backup.import(newest).snapshot.txs.size)
        assertNull(c.autoBackup.current().folder.lastError)
        assertEquals(now, c.autoBackup.current().folder.lastAt)
    }

    @Test fun pastaSubstituindoOMesmoArquivo() = runBlocking {
        val ids = listOf(addExpense(1_000), addExpense(2_000), addExpense(3_000))
        c.autoBackup.setTarget(BackupTarget.FOLDER, tree.toString(), "Backups")
        c.autoBackup.setEnabled(true)
        c.autoBackup.setFolderMode(FolderMode.REPLACE)
        assertEquals(BackupStatus.WRITTEN, runner.run(force = false).single().status)
        val first = File(dir, BackupPolicy.SINGLE_NAME).length()

        // menos dados: o arquivo novo é menor e não pode sobrar nada do anterior no fim
        ids.drop(1).forEach { id -> c.repo.loadOnce().txs.first { it.id == id }.let { c.repo.deleteTx(it) } }
        now += 25 * hour
        assertEquals(BackupStatus.WRITTEN, runner.run(force = false).single().status)
        assertEquals(listOf(BackupPolicy.SINGLE_NAME), names())
        val text = File(dir, BackupPolicy.SINGLE_NAME).readText()
        assertTrue(text.length < first)
        assertTrue(text.trimEnd().endsWith("}"))
        assertEquals(1, JSONObject(text).getJSONArray("transactions").length())
    }

    @Test fun nuvemArquivoUnico() = runBlocking {
        addExpense(4_200)
        File(dir, "cryo-backup.json").createNewFile() // o arquivo criado pelo seletor ("Salvar" na nuvem)
        val doc = DocumentsContract.buildDocumentUri(TestDocsProvider.AUTHORITY, "${TestDocsProvider.ROOT}/cryo-backup.json")
        assertEquals("cryo-backup.json · nuvem", BackupPlaces.describeFile(ctx, doc))
        c.autoBackup.setTarget(BackupTarget.CLOUD, doc.toString(), BackupPlaces.describeFile(ctx, doc))
        c.autoBackup.setEnabled(true)
        c.autoBackup.setFrequency(BackupFrequency.WEEKLY)

        val r = runner.run(force = false).single()
        assertEquals(BackupTarget.CLOUD, r.target)
        assertEquals(BackupStatus.WRITTEN, r.status)
        assertEquals(1, Backup.import(File(dir, "cryo-backup.json").readText()).snapshot.txs.size)

        now += 2 * 24 * hour
        addExpense(100)
        assertEquals(BackupStatus.NOT_DUE, runner.run(force = false).single().status) // semanal
        assertEquals(BackupStatus.WRITTEN, runner.run(force = true).single().status) // "Fazer backup agora"
        assertEquals(2, Backup.import(File(dir, "cryo-backup.json").readText()).snapshot.txs.size)
    }

    @Test fun pastaENuvemJuntas() = runBlocking {
        addExpense(900)
        File(dir, "nuvem.json").createNewFile()
        c.autoBackup.setTarget(BackupTarget.FOLDER, tree.toString(), "Backups")
        c.autoBackup.setTarget(BackupTarget.CLOUD, DocumentsContract.buildDocumentUri(TestDocsProvider.AUTHORITY, "root/nuvem.json").toString(), "nuvem.json")
        c.autoBackup.setEnabled(true)
        val r = runner.run(force = false)
        assertEquals(listOf(BackupStatus.WRITTEN, BackupStatus.WRITTEN), r.map { it.status })
        assertEquals(1, copies().size)
        assertTrue(File(dir, "nuvem.json").length() > 0)
    }

    @Test fun semDadosNaoSubstituiBackupBom() = runBlocking {
        File(dir, BackupPolicy.SINGLE_NAME).writeText("""{"app":"Cryo","version":2}""")
        c.autoBackup.setTarget(BackupTarget.FOLDER, tree.toString(), "Backups")
        c.autoBackup.setEnabled(true)
        c.autoBackup.setFolderMode(FolderMode.REPLACE)
        assertEquals(BackupStatus.NOTHING_TO_SAVE, runner.run(force = true).single().status)
        assertEquals("""{"app":"Cryo","version":2}""", File(dir, BackupPolicy.SINGLE_NAME).readText())
    }

    @Test fun desligadoSoFazQuandoPedir() = runBlocking {
        addExpense(500)
        c.autoBackup.setTarget(BackupTarget.FOLDER, tree.toString(), "Backups")
        assertTrue(runner.run(force = false).isEmpty())
        assertEquals(BackupStatus.WRITTEN, runner.run(force = true).single().status)
    }

    @Test fun falhaEDepoisVoltaAFuncionar() = runBlocking {
        addExpense(500)
        c.autoBackup.setTarget(BackupTarget.FOLDER, tree.toString(), "Backups")
        c.autoBackup.setEnabled(true)
        dir.deleteRecursively() // a pasta foi apagada
        val r = runner.run(force = false).single()
        assertEquals(BackupStatus.FAILED, r.status)
        assertEquals("A pasta não existe mais. Escolha outra.", r.message)
        assertEquals(r.message, c.autoBackup.current().folder.lastError)

        dir.mkdirs()
        now += hour
        assertEquals(BackupStatus.WRITTEN, runner.run(force = false).single().status)
        assertNull(c.autoBackup.current().folder.lastError)
        assertEquals(1, copies().size)
    }

    @Test fun seguraQuandoSomeMuitaCoisa() = runBlocking {
        repeat(25) { addExpense(100L + it) }
        c.autoBackup.setTarget(BackupTarget.FOLDER, tree.toString(), "Backups")
        c.autoBackup.setEnabled(true)
        c.autoBackup.setFolderMode(FolderMode.REPLACE)
        assertEquals(BackupStatus.WRITTEN, runner.run(force = false).single().status)
        assertEquals(25, c.autoBackup.current().folder.lastTxCount)

        // "Apagar todos os dados" sem querer: sem nada cadastrado, nada é gravado
        c.repo.eraseAll()
        now += 25 * hour
        assertEquals(BackupStatus.NOTHING_TO_SAVE, runner.run(force = false).single().status)

        // refaz o início (conta nova) e lança uma coisa: o backup bom não é substituído sozinho
        addExpense(999)
        val held = runner.run(force = false).single()
        assertEquals(BackupStatus.HELD, held.status)
        assertTrue(held.message!!.contains("de 25 para 1"))
        assertEquals(held.message, c.autoBackup.current().folder.lastError)
        assertEquals(25, Backup.import(File(dir, BackupPolicy.SINGLE_NAME).readText()).snapshot.txs.size)
        assertTrue(BackupPolicy.shrankTooMuch(25, 12))
        assertFalse(BackupPolicy.shrankTooMuch(25, 13))
        assertFalse(BackupPolicy.shrankTooMuch(19, 0)) // pouca coisa: não precisa segurar

        // guardando várias cópias também pausa: senão, dia após dia, as cópias boas sairiam da pasta
        c.autoBackup.setFolderMode(FolderMode.COPIES)
        now += 25 * hour
        assertEquals(BackupStatus.HELD, runner.run(force = false).single().status)
        assertTrue(copies().isEmpty())

        // de volta ao arquivo único: só o "Fazer backup agora" substitui, e aí o aviso some
        c.autoBackup.setFolderMode(FolderMode.REPLACE)
        assertEquals(BackupStatus.WRITTEN, runner.run(force = true).single().status)
        assertEquals(1, Backup.import(File(dir, BackupPolicy.SINGLE_NAME).readText()).snapshot.txs.size)
        assertNull(c.autoBackup.current().folder.lastError)
    }

    @Test fun regrasDaPasta() {
        val t = LocalDateTime.of(2026, 10, 2, 3, 0)
        val mine = setOf("cryo-backup-2026-09-30-030000.json", "cryo-backup-2026-10-01-030000.json")
        val folder = mine + setOf("cryo-backup-2026-01-01-030000.json", "cryo-backup.json", "notas.txt") // os outros são de outro celular
        // várias cópias: só as deste celular entram na conta para apagar
        val copies = BackupPolicy.planFolder(folder, FolderMode.COPIES, 2, mine, t, adopt = false) as FolderPlan.Write
        assertEquals("cryo-backup-2026-10-02-030000.json", copies.name)
        assertEquals(listOf("cryo-backup-2026-09-30-030000.json"), copies.prune)
        assertEquals(setOf("cryo-backup-2026-10-01-030000.json", "cryo-backup-2026-10-02-030000.json"), copies.owned)
        // substituir: um cryo-backup.json que não é deste celular só é trocado com "Fazer backup agora"
        assertEquals(FolderPlan.HoldForeign, BackupPolicy.planFolder(folder, FolderMode.REPLACE, 2, mine, t, adopt = false))
        assertTrue(BackupPolicy.planFolder(folder, FolderMode.REPLACE, 2, mine, t, adopt = true) is FolderPlan.Write)
        assertTrue(BackupPolicy.planFolder(folder, FolderMode.REPLACE, 2, mine + "cryo-backup.json", t, adopt = false) is FolderPlan.Write)
        assertTrue(BackupPolicy.planFolder(setOf("notas.txt"), FolderMode.REPLACE, 2, emptySet(), t, adopt = false) is FolderPlan.Write)
    }

    @Test fun copiasDeOutroCelularNuncaSaoApagadas() = runBlocking {
        val foreign = listOf("cryo-backup-2026-01-01-030000.json", "cryo-backup-2026-01-02-030000.json", "cryo-backup-2026-01-03-030000.json")
        foreign.forEach { File(dir, it).writeText("antigo") }
        c.autoBackup.setTarget(BackupTarget.FOLDER, tree.toString(), "Backups")
        c.autoBackup.setEnabled(true)
        c.autoBackup.setKeep(2)
        repeat(4) {
            addExpense(100L + it)
            assertEquals(BackupStatus.WRITTEN, runner.run(force = false).single().status)
            now += 24 * hour
        }
        val left = copies()
        assertTrue(left.containsAll(foreign))
        assertEquals(2, (left - foreign.toSet()).size) // as deste celular: só as 2 mais novas
        assertEquals((left - foreign.toSet()).toSet(), c.autoBackup.current().folder.owned)
    }

    @Test fun substituirNaoApagaArquivoDeOutroCelular() = runBlocking {
        File(dir, BackupPolicy.SINGLE_NAME).writeText("de outro celular")
        addExpense(700)
        c.autoBackup.setTarget(BackupTarget.FOLDER, tree.toString(), "Backups")
        c.autoBackup.setEnabled(true)
        c.autoBackup.setFolderMode(FolderMode.REPLACE)
        val r = runner.run(force = false).single()
        assertEquals(BackupStatus.HELD, r.status)
        assertEquals(BackupErrors.FOREIGN, r.message)
        assertEquals("de outro celular", File(dir, BackupPolicy.SINGLE_NAME).readText())

        // nem o "Fazer backup agora" apaga: a pessoa precisa conferir o lugar (escolher a pasta de novo) e decidir
        assertEquals(BackupStatus.HELD, runner.run(force = true).single().status)
        assertEquals("de outro celular", File(dir, BackupPolicy.SINGLE_NAME).readText())
        runner.adopt(BackupTarget.FOLDER, tree, FolderMode.REPLACE) // conferiu e decidiu substituir
        File(dir, "intruso.json").writeText("x") // (outros arquivos não importam)
        assertEquals(BackupStatus.WRITTEN, runner.run(force = true).single().status)
        now += 25 * hour
        addExpense(50)
        assertEquals(BackupStatus.WRITTEN, runner.run(force = false).single().status) // agora o arquivo é deste celular
        assertEquals(2, Backup.import(File(dir, BackupPolicy.SINGLE_NAME).readText()).snapshot.txs.size)
    }

    @Test fun falhaAoGravarNaoDeixaArquivoVazioERegravaDepois() = runBlocking {
        addExpense(300)
        c.autoBackup.setTarget(BackupTarget.FOLDER, tree.toString(), "Backups")
        c.autoBackup.setEnabled(true)
        assertEquals(BackupStatus.WRITTEN, runner.run(force = false).single().status)
        val good = c.autoBackup.current().folder.lastHash

        // disco cheio: a cópia nova não fica pela metade na pasta
        TestDocsProvider.failWrites = true
        addExpense(400)
        now += 25 * hour
        assertEquals(BackupStatus.FAILED, runner.run(force = false).single().status)
        assertEquals(1, copies().size)
        assertNull(c.autoBackup.current().folder.lastHash)

        // na nuvem: a gravação falhou depois de abrir o arquivo; a próxima conferência grava de novo
        File(dir, "nuvem.json").writeText("")
        c.autoBackup.setTarget(BackupTarget.CLOUD, DocumentsContract.buildDocumentUri(TestDocsProvider.AUTHORITY, "root/nuvem.json").toString(), "nuvem.json")
        assertEquals(BackupStatus.FAILED, runner.run(force = true, only = BackupTarget.CLOUD).single().status)
        TestDocsProvider.failWrites = false
        now += hour // ainda não daria um dia, mas depois de uma falha tenta de novo
        val again = runner.run(force = false)
        assertEquals(listOf(BackupStatus.WRITTEN, BackupStatus.WRITTEN), again.map { it.status })
        assertEquals(2, Backup.import(File(dir, "nuvem.json").readText()).snapshot.txs.size)
        assertNotNull(good)
    }

    @Test fun arquivoAlteradoPorOutroCelularNaoESubstituido() = runBlocking {
        addExpense(100)
        File(dir, "nuvem.json").writeText("")
        val doc = DocumentsContract.buildDocumentUri(TestDocsProvider.AUTHORITY, "root/nuvem.json")
        c.autoBackup.setTarget(BackupTarget.CLOUD, doc.toString(), "nuvem.json")
        c.autoBackup.setEnabled(true)
        assertEquals(BackupStatus.WRITTEN, runner.run(force = false).single().status) // arquivo vazio: nada a proteger

        // outro celular (ou app) grava no mesmo arquivo
        val other = Backup.export(c.repo.loadOnce(), exportedAt = now + 5 * hour)
        File(dir, "nuvem.json").writeText(other)
        addExpense(50)
        now += 25 * hour
        val r = runner.run(force = false).single()
        assertEquals(BackupStatus.HELD, r.status)
        assertEquals(BackupErrors.CHANGED_CLOUD, r.message)
        assertEquals(BackupStatus.HELD, runner.run(force = true).single().status) // nem o botão apaga a versão do outro
        assertEquals(other, File(dir, "nuvem.json").readText())

        // a pessoa escolheu o arquivo de novo e decidiu: grava, e o arquivo volta a ser deste celular
        runner.adopt(BackupTarget.CLOUD, doc, FolderMode.COPIES)
        assertEquals(BackupStatus.WRITTEN, runner.run(force = false).single().status)
        addExpense(10)
        now += 25 * hour
        assertEquals(BackupStatus.WRITTEN, runner.run(force = false).single().status)

        // uma gravação deste celular que parou no meio deixa a data da tentativa no arquivo: continua sendo dele
        val attempt = now + hour
        c.autoBackup.markWriting(BackupTarget.CLOUD, attempt)
        File(dir, "nuvem.json").writeText("{\n \"app\": \"Cryo\",\n \"version\": 2,\n \"exportedAt\": $attempt,\n \"accou")
        c.autoBackup.recordFailure(BackupTarget.CLOUD, attempt, "falhou")
        now += 2 * hour
        assertEquals(BackupStatus.WRITTEN, runner.run(force = false).single().status)
        assertEquals(3, Backup.import(File(dir, "nuvem.json").readText()).snapshot.txs.size)
    }

    @Test fun decisaoValeSoParaOArquivoQueAPessoaViu() = runBlocking {
        addExpense(100)
        File(dir, "nuvem.json").writeText(Backup.export(c.repo.loadOnce(), exportedAt = 1L)) // o que a pessoa viu
        val doc = DocumentsContract.buildDocumentUri(TestDocsProvider.AUTHORITY, "root/nuvem.json")
        c.autoBackup.setTarget(BackupTarget.CLOUD, doc.toString(), "nuvem.json")
        c.autoBackup.setEnabled(true)
        runner.adopt(BackupTarget.CLOUD, doc, FolderMode.COPIES)

        // a primeira gravação falha; enquanto isso, outro celular grava outra coisa no arquivo
        TestDocsProvider.failWrites = true
        assertEquals(BackupStatus.FAILED, runner.run(force = true).single().status)
        TestDocsProvider.failWrites = false
        val other = Backup.export(c.repo.loadOnce(), exportedAt = 2L)
        File(dir, "nuvem.json").writeText(other)
        assertEquals(BackupStatus.HELD, runner.run(force = true).single().status)
        assertEquals(other, File(dir, "nuvem.json").readText())

        // sem a mudança do outro celular, a decisão continua valendo depois da falha
        File(dir, "nuvem.json").writeText(Backup.export(c.repo.loadOnce(), exportedAt = 1L))
        assertEquals(BackupStatus.WRITTEN, runner.run(force = false).single().status)
    }

    @Test fun voltarASubstituirDepoisDeGuardarCopias() = runBlocking {
        addExpense(100)
        c.autoBackup.setTarget(BackupTarget.FOLDER, tree.toString(), "Backups")
        c.autoBackup.setEnabled(true)
        c.autoBackup.setFolderMode(FolderMode.REPLACE)
        assertEquals(BackupStatus.WRITTEN, runner.run(force = false).single().status)
        c.autoBackup.setFolderMode(FolderMode.COPIES)
        addExpense(200)
        now += 25 * hour
        assertEquals(BackupStatus.WRITTEN, runner.run(force = false).single().status)
        c.autoBackup.setFolderMode(FolderMode.REPLACE) // o cryo-backup.json continua sendo deste celular
        addExpense(300)
        now += 25 * hour
        assertEquals(BackupStatus.WRITTEN, runner.run(force = false).single().status)
        assertEquals(3, Backup.import(File(dir, BackupPolicy.SINGLE_NAME).readText()).snapshot.txs.size)
    }

    @Test fun pastaSubstituindoAlteradaPorOutroCelular() = runBlocking {
        addExpense(100)
        c.autoBackup.setTarget(BackupTarget.FOLDER, tree.toString(), "Backups")
        c.autoBackup.setEnabled(true)
        c.autoBackup.setFolderMode(FolderMode.REPLACE)
        assertEquals(BackupStatus.WRITTEN, runner.run(force = false).single().status)
        assertTrue(BackupPolicy.SINGLE_NAME in c.autoBackup.current().folder.owned)
        File(dir, BackupPolicy.SINGLE_NAME).writeText(Backup.export(c.repo.loadOnce(), exportedAt = 1L)) // sincronizado de outro celular
        addExpense(5)
        now += 25 * hour
        val r = runner.run(force = false).single()
        assertEquals(BackupStatus.HELD, r.status)
        assertEquals(BackupErrors.CHANGED_FOLDER, r.message)
    }

    @Test fun celularNovoEncontraOBackupNaNuvem() = runBlocking {
        repeat(3) { addExpense(1_000L * (it + 1)) }
        val oldPhone = Backup.export(c.repo.loadOnce(), exportedAt = now - 2 * hour)
        File(dir, "cryo-backup.json").writeText(oldPhone)
        val doc = DocumentsContract.buildDocumentUri(TestDocsProvider.AUTHORITY, "${TestDocsProvider.ROOT}/cryo-backup.json")

        // celular novo: só a conta criada no início
        c.repo.eraseAll()
        c.repo.saveAccount(Account(name = "Conta principal", initialBalance = 0, color = 0))
        val check = runner.inspect(BackupTarget.CLOUD, doc, FolderMode.COPIES) as PlaceCheck.HasBackup
        assertTrue(check.atRisk)
        val found = check.found
        assertEquals(3, found.txs)
        assertEquals(0, found.currentTxs)
        assertEquals(1, found.currentAccounts)
        assertTrue(found.richer)
        assertEquals(now - 2 * hour, found.exportedAt)

        // depois de restaurar, os dados são iguais: não pergunta de novo
        c.repo.replaceAll(found.restored.snapshot)
        assertEquals(PlaceCheck.Clear, runner.inspect(BackupTarget.CLOUD, doc, FolderMode.COPIES))

        // arquivo vazio (recém-criado pelo seletor) ou que não é do Cryo: nada a perguntar
        fun cloud(name: String, text: String): PlaceCheck {
            File(dir, name).writeText(text)
            return runBlocking { runner.inspect(BackupTarget.CLOUD, DocumentsContract.buildDocumentUri(TestDocsProvider.AUTHORITY, "root/$name"), FolderMode.COPIES) }
        }
        assertEquals(PlaceCheck.Clear, cloud("novo.json", ""))
        assertEquals(PlaceCheck.Clear, cloud("outro.json", """{"nome":"planilha"}"""))
        // backup do Cryo que não dá para ler: não substitui sem a pessoa confirmar
        assertEquals(PlaceCheck.CantRead(BackupErrors.NEWER), cloud("novo-cryo.json", oldPhone.replace("\"version\": 2", "\"version\": 99")))
        assertEquals(PlaceCheck.CantRead(BackupErrors.DAMAGED), cloud("estragado.json", oldPhone.take(120)))
    }

    @Test fun pastaEncontraOBackupMaisNovo() = runBlocking {
        addExpense(100)
        val one = Backup.export(c.repo.loadOnce())
        addExpense(200)
        val two = Backup.export(c.repo.loadOnce())
        fun put(name: String, text: String, ageHours: Int) =
            File(dir, name).apply { writeText(text); setLastModified(now - ageHours * hour) }
        put("cryo-backup-2026-09-01.json", one, 48)
        put("cryo-backup-2026-10-01-030000.json", two, 20)
        put("cryo-backup (1).json", "{ quebrado", 1) // o mais novo está estragado: usa o seguinte
        put("notas.json", one, 0) // não é backup do Cryo pelo nome

        c.repo.eraseAll()
        c.repo.saveAccount(Account(name = "Conta principal", initialBalance = 0, color = 0))
        val copies = runner.inspect(BackupTarget.FOLDER, tree, FolderMode.COPIES) as PlaceCheck.HasBackup
        assertEquals(2, copies.found.txs)
        assertFalse(copies.atRisk) // guardando várias cópias, nada do que já está lá é substituído

        // substituindo sempre o mesmo arquivo: o cryo-backup.json que já está lá é o que corre risco
        put(BackupPolicy.SINGLE_NAME, one, 100)
        val replace = runner.inspect(BackupTarget.FOLDER, tree, FolderMode.REPLACE) as PlaceCheck.HasBackup
        assertTrue(replace.atRisk)
        assertEquals(1, replace.found.txs)

        // um cryo-backup.json que não é do Cryo: na pasta, ninguém perguntou antes, então pergunta agora
        put(BackupPolicy.SINGLE_NAME, "minhas anotações", 1)
        assertEquals(PlaceCheck.CantRead(BackupErrors.NOT_CRYO), runner.inspect(BackupTarget.FOLDER, tree, FolderMode.REPLACE))

        // pasta sem backups do Cryo
        listOf("cryo-backup-2026-09-01.json", "cryo-backup-2026-10-01-030000.json", "cryo-backup (1).json", BackupPolicy.SINGLE_NAME)
            .forEach { File(dir, it).delete() }
        assertEquals(PlaceCheck.Clear, runner.inspect(BackupTarget.FOLDER, tree, FolderMode.REPLACE))

        // pasta que sumiu: nem dá para ver o que tem lá
        dir.deleteRecursively()
        assertEquals(PlaceCheck.Unreachable(BackupErrors.CANT_OPEN_FOLDER), runner.inspect(BackupTarget.FOLDER, tree, FolderMode.COPIES))
    }

    @Test fun trocarDestinoZeraOHistorico() = runBlocking {
        addExpense(500)
        c.autoBackup.setTarget(BackupTarget.FOLDER, tree.toString(), "Backups")
        c.autoBackup.setEnabled(true)
        runner.run(force = false)
        assertNotNull(c.autoBackup.current().folder.lastAt)
        c.autoBackup.setTarget(BackupTarget.FOLDER, tree.toString(), "Outra")
        val st = c.autoBackup.current().folder
        assertNull(st.lastAt)
        assertNull(st.lastHash)
        assertEquals("Outra", st.name)
        c.autoBackup.setTarget(BackupTarget.FOLDER, null, "")
        assertFalse(c.autoBackup.current().hasTarget)
    }
}
