// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.core.app.ActivityOptionsCompat
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureScreenRoboImage
import io.github.h3yk0.cryo.data.AppSettings
import io.github.h3yk0.cryo.data.Backup
import io.github.h3yk0.cryo.data.BackupPolicy
import io.github.h3yk0.cryo.data.ThemeMode
import io.github.h3yk0.cryo.data.db.Account
import io.github.h3yk0.cryo.ui.LocalNav
import io.github.h3yk0.cryo.ui.Navigator
import io.github.h3yk0.cryo.ui.components.LocalContainer
import io.github.h3yk0.cryo.ui.components.LocalSettings
import io.github.h3yk0.cryo.ui.screens.AutoBackupScreen
import io.github.h3yk0.cryo.ui.theme.CryoTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.LocalDate

/** O backup automático usado como uma pessoa usaria: escolher a pasta, escolher a nuvem, restaurar ou desistir. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], application = TestApp::class, qualifiers = "w393dp-h852dp-xxhdpi")
class BackupFlowTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var c: AppContainer
    private lateinit var dir: File
    private val settings = AppSettings(onboardingDone = true, defaultAccountId = 1, dynamicColor = false)
    private val tree get() = DocumentsContract.buildTreeDocumentUri(TestDocsProvider.AUTHORITY, TestDocsProvider.ROOT)
    private fun doc(name: String) = DocumentsContract.buildDocumentUri(TestDocsProvider.AUTHORITY, "${TestDocsProvider.ROOT}/$name")

    /** Faz o papel do seletor de arquivos do Android: devolve o lugar que a pessoa "escolheu". */
    private var answerFolder: Uri? = null
    private var answerFile: Uri? = null
    private val registry = object : ActivityResultRegistry() {
        override fun <I, O> onLaunch(requestCode: Int, contract: ActivityResultContract<I, O>, input: I, options: ActivityOptionsCompat?) {
            val uri = when (contract) {
                is ActivityResultContracts.OpenDocumentTree -> answerFolder
                is ActivityResultContracts.CreateDocument -> answerFile
                else -> null
            }
            dispatchResult(requestCode, if (uri != null) Activity.RESULT_OK else Activity.RESULT_CANCELED, Intent().setData(uri))
        }
    }

    @Before fun setUp() {
        c = AppContainer(ApplicationProvider.getApplicationContext())
        runBlocking {
            seed(c, LocalDate.now())
            c.autoBackup.clear()
        }
        dir = File(ApplicationProvider.getApplicationContext<android.content.Context>().cacheDir, "flow-${System.nanoTime()}")
        TestDocsProvider.install(dir)
        rule.waitUntil(10_000) { (c.repo.snapshot.value?.txs?.size ?: 0) > 50 }
    }

    private fun show() {
        rule.setContent {
            CryoTheme(mode = ThemeMode.LIGHT, dynamicColor = false) {
                val nav = rememberNavController()
                val navigator = remember { Navigator(nav) }
                val owner = remember { object : ActivityResultRegistryOwner { override val activityResultRegistry = registry } }
                CompositionLocalProvider(
                    LocalContainer provides c, LocalSettings provides settings, LocalNav provides navigator,
                    LocalActivityResultRegistryOwner provides owner,
                ) {
                    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) { AutoBackupScreen() }
                }
            }
        }
        rule.waitForIdle()
    }

    private fun current() = runBlocking { c.autoBackup.current() }
    private fun txCount() = runBlocking { c.repo.loadOnce().txs.size }

    @Test fun escolherPastaVaziaJaFazOPrimeiroBackup() {
        show()
        answerFolder = tree
        rule.onNodeWithText("Escolher pasta").performScrollTo().performClick()
        rule.waitUntil(10_000) { current().folder.lastAt != null }

        val st = current()
        assertTrue(st.enabled)
        assertEquals(1, dir.list()!!.count(BackupPolicy::isCopy))
        val copy = dir.listFiles()!!.first { BackupPolicy.isCopy(it.name) }
        assertEquals(txCount(), Backup.import(copy.readText()).snapshot.txs.size)
        rule.waitUntil(5_000) { rule.onAllNodesWithTextExists("Último backup: hoje") }
        captureScreenRoboImage("screenshots/34_fluxo_pasta_escolhida.png")
    }

    @Test fun celularNovoRestauraDaNuvem() {
        // o arquivo na nuvem tem tudo do celular antigo
        val full = txCount()
        File(dir, "cryo-backup.json").writeText(runBlocking { Backup.export(c.repo.loadOnce()) })
        // celular novo: só a conta criada no início
        runBlocking {
            c.repo.eraseAll()
            c.repo.saveAccount(Account(name = "Conta principal", initialBalance = 0, color = 0))
        }
        show()
        answerFile = doc("cryo-backup.json")
        rule.onNodeWithText("Escolher na nuvem").performScrollTo().performClick()
        rule.waitUntil(10_000) { rule.onAllNodesWithTextExists("Já existe um backup aqui") }
        captureScreenRoboImage("screenshots/35_fluxo_backup_encontrado.png")
        assertFalse(current().cloud.isSet) // nada muda até a pessoa decidir

        rule.onNodeWithText("Restaurar este backup").performClick()
        rule.waitUntil(15_000) { current().cloud.lastAt != null }
        assertEquals(full, txCount())
        assertEquals(full, Backup.import(File(dir, "cryo-backup.json").readText()).snapshot.txs.size)
        assertTrue(current().enabled)
    }

    @Test fun desistirNaoMudaNada() {
        File(dir, "cryo-backup.json").writeText(runBlocking { Backup.export(c.repo.loadOnce()) })
        val before = File(dir, "cryo-backup.json").readText()
        runBlocking {
            c.repo.eraseAll()
            c.repo.saveAccount(Account(name = "Conta principal", initialBalance = 0, color = 0))
        }
        show()
        answerFile = doc("cryo-backup.json")
        rule.onNodeWithText("Escolher na nuvem").performScrollTo().performClick()
        rule.waitUntil(10_000) { rule.onAllNodesWithTextExists("Já existe um backup aqui") }
        rule.onNodeWithText("Cancelar").performClick()
        rule.waitForIdle()

        assertFalse(current().cloud.isSet)
        assertEquals(0, txCount())
        assertEquals(before, File(dir, "cryo-backup.json").readText()) // o backup bom continua lá
    }

    @Test fun substituirPeloCelularQuandoAPessoaQuer() {
        File(dir, "cryo-backup.json").writeText(runBlocking { Backup.export(c.repo.loadOnce()) })
        runBlocking {
            c.repo.eraseAll()
            c.repo.saveAccount(Account(name = "Conta principal", initialBalance = 0, color = 0))
        }
        show()
        answerFile = doc("cryo-backup.json")
        rule.onNodeWithText("Escolher na nuvem").performScrollTo().performClick()
        rule.waitUntil(10_000) { rule.onAllNodesWithTextExists("Já existe um backup aqui") }
        rule.onNodeWithText("Substituir pelos dados deste celular").performClick()
        rule.waitUntil(10_000) { current().cloud.lastAt != null }
        assertEquals(0, Backup.import(File(dir, "cryo-backup.json").readText()).snapshot.txs.size)
    }

    @Test fun backupQueNaoDaParaLerPedeConfirmacao() {
        val newer = runBlocking { Backup.export(c.repo.loadOnce()) }.replace("\"version\": 2", "\"version\": 99")
        File(dir, "cryo-backup.json").writeText(newer)
        show()
        answerFile = doc("cryo-backup.json")
        rule.onNodeWithText("Escolher na nuvem").performScrollTo().performClick()
        rule.waitUntil(10_000) { rule.onAllNodesWithTextExists("versão mais nova do Cryo") }
        captureScreenRoboImage("screenshots/36_fluxo_backup_ilegivel.png")
        rule.onNodeWithText("Cancelar").performClick()
        rule.waitForIdle()
        assertFalse(current().cloud.isSet)
        assertEquals(newer, File(dir, "cryo-backup.json").readText())
    }

    @Test fun passarASubstituirConfereOArquivoQueJaEstaNaPasta() {
        val all = txCount()
        val older = runBlocking { c.repo.loadOnce().let { it.copy(txs = it.txs.take(10)) } }
        File(dir, BackupPolicy.SINGLE_NAME).writeText(Backup.export(older)) // de outro celular, com menos dados
        runBlocking {
            c.autoBackup.setTarget(io.github.h3yk0.cryo.data.BackupTarget.FOLDER, tree.toString(), "Backups")
            c.autoBackup.setEnabled(true)
        }
        show()
        rule.onNodeWithText("Substituir sempre o mesmo arquivo").performScrollTo().performClick()
        rule.waitUntil(10_000) { rule.onAllNodesWithTextExists("Já existe um backup aqui") }
        rule.onNodeWithText("Cancelar").performClick()
        rule.waitForIdle()
        assertEquals(io.github.h3yk0.cryo.data.FolderMode.COPIES, current().folderMode) // nada mudou
        assertEquals(10, Backup.import(File(dir, BackupPolicy.SINGLE_NAME).readText()).snapshot.txs.size)

        rule.onNodeWithText("Substituir sempre o mesmo arquivo").performScrollTo().performClick()
        rule.waitUntil(10_000) { rule.onAllNodesWithTextExists("Já existe um backup aqui") }
        rule.onNodeWithText("Substituir pelos dados deste celular").performClick()
        rule.waitUntil(10_000) { current().folder.lastAt != null }
        assertEquals(io.github.h3yk0.cryo.data.FolderMode.REPLACE, current().folderMode)
        assertEquals(all, Backup.import(File(dir, BackupPolicy.SINGLE_NAME).readText()).snapshot.txs.size)
        assertTrue(BackupPolicy.SINGLE_NAME in current().folder.owned)
    }

    private fun androidx.compose.ui.test.junit4.ComposeTestRule.onAllNodesWithTextExists(text: String): Boolean =
        onAllNodes(androidx.compose.ui.test.hasText(text, substring = true)).fetchSemanticsNodes().isNotEmpty()
}
