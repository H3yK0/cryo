// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo

import android.content.ClipDescription
import android.content.ClipboardManager
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.test.espresso.Espresso
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.h3yk0.cryo.data.Palette
import io.github.h3yk0.cryo.data.SettingsRepository
import io.github.h3yk0.cryo.data.db.Account
import io.github.h3yk0.cryo.data.db.AccountType
import io.github.h3yk0.cryo.data.db.Tx
import io.github.h3yk0.cryo.data.db.TxType
import io.github.h3yk0.cryo.importer.ImportReport
import io.github.h3yk0.cryo.importer.SampleFiles
import io.github.h3yk0.cryo.importer.StatementReader
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowLooper
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * O app de verdade (Application + Activity): 7 toques na versão ligam o modo desenvolvedor, o modo de teste mostra
 * dados de exemplo num banco separado e, ao sair, os dados de verdade estão exatamente como antes.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w393dp-h852dp-xxhdpi")
class DevModeFlowTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    private val app get() = rule.activity.application as CryoApp

    private fun waitFor(ms: Long = 10_000, condition: () -> Boolean) = rule.waitUntil(ms) {
        ShadowLooper.idleMainLooper()
        condition()
    }

    private fun exists(t: String) = rule.onAllNodesWithText(t, substring = true).fetchSemanticsNodes().isNotEmpty()
    private fun waitText(t: String) = waitFor { exists(t) }

    /**
     * Dados "de verdade": uma conta e um almoço. Os ajustes ficam num DataStore que o Robolectric não zera entre um
     * teste e outro, então cada teste começa limpando os dois (de verdade e de teste).
     */
    private fun realData() = runBlocking {
        val r = app.real
        r.settings.clear()
        SettingsRepository(app, sandbox = true).clear()
        val acc = r.repo.saveAccount(Account(name = "Minha conta", type = AccountType.CHECKING, initialBalance = 50000, color = Palette.pick(1)))
        r.repo.saveTx(Tx(type = TxType.EXPENSE, amount = 3250, date = LocalDate.now(), description = "Almoço de verdade", accountId = acc))
        r.settings.setDefaultAccount(acc)
        r.settings.setOnboardingDone(true)
    }

    @Test fun modoDeTesteNaoMexeNosDadosDeVerdade() {
        realData()
        waitText("Saldo em contas")
        rule.onNodeWithContentDescription("Ajustes").performClick()
        waitText("Aparência")
        val version = "Cryo ${BuildConfig.VERSION_NAME}"
        rule.onNode(hasScrollAction()).performScrollToNode(hasText(version))
        repeat(7) { rule.onNodeWithText(version).performClick() }
        waitText("Modo desenvolvedor")
        assertTrue(app.real.dev.enabled)
        rule.onNode(hasScrollAction()).performScrollToNode(hasText("Dados de teste, arquivos de exemplo", substring = true))
        rule.onNodeWithText("Dados de teste, arquivos de exemplo", substring = true).performClick()
        waitText("Usar dados de teste")
        rule.onRoot().captureRoboImage("screenshots/44_modo_desenvolvedor.png")

        rule.onNodeWithText("Usar dados de teste").performClick()
        waitText("Entrar no modo de teste")
        rule.onNodeWithText("Entrar no modo de teste").performClick()
        // A tela é recriada já com os dados de exemplo.
        waitFor(20_000) { app.container.sandbox && exists("Recriar os dados de exemplo") }
        assertTrue(runBlocking { app.container.repo.loadOnce().txs.size } > 50)

        Espresso.pressBack()
        Espresso.pressBack()
        waitText("Modo de teste: dados de exemplo")
        waitText("Saldo em contas")
        rule.onRoot().captureRoboImage("screenshots/46_modo_teste.png")

        // Os dados de verdade continuam exatamente iguais.
        val real = runBlocking { app.real.repo.loadOnce() }
        assertEquals(listOf("Almoço de verdade"), real.txs.map { it.description })
        assertEquals(listOf("Minha conta"), real.accounts.map { it.name })

        rule.onNodeWithText("Sair").performClick()
        waitFor(20_000) { !app.container.sandbox && exists("Saldo em contas") && !exists("Modo de teste: dados de exemplo") }
        assertEquals(listOf("Minha conta"), runBlocking { app.container.repo.loadOnce().accounts.map { it.name } })
        assertTrue(app.real.dev.enabled) // o modo desenvolvedor continua ligado; só os dados voltaram
        runBlocking { app.real.settings.clear(); SettingsRepository(app, sandbox = true).clear() }
    }

    @Test fun relatorioAnonimoDaUltimaLeitura() {
        realData()
        val sample = SampleFiles.csvAccount(LocalDate.of(2026, 10, 3))
        val read = StatementReader.read(sample.bytes, LocalDate.of(2026, 10, 3), allowJson = false) { _, _ -> emptyList() }
        val st = (read as StatementReader.Result.Ok).statement
        app.real.lastImport.value = ImportReport("extrato-joao-silva.csv", sample.bytes.size.toLong(), LocalDateTime.of(2026, 10, 3, 14, 5), st)
        app.real.dev.enabled = true
        waitText("Saldo em contas")
        rule.onNodeWithContentDescription("Ajustes").performClick()
        rule.onNode(hasScrollAction()).performScrollToNode(hasText("Dados de teste, arquivos de exemplo", substring = true))
        rule.onNodeWithText("Dados de teste, arquivos de exemplo", substring = true).performClick()
        waitText("Relatório da última leitura")
        rule.onNodeWithText("Relatório da última leitura").performScrollTo().performClick()
        waitText("Versão anônima")
        assertFalse(exists("joao"))
        assertFalse(exists("Maria Silva"))
        assertTrue(exists("arquivo.csv"))
        rule.onRoot().captureRoboImage("screenshots/45_relatorio_anonimo.png")

        // A versão completa tem os dados de verdade: copiar também pede confirmação, e a cópia vai marcada como sensível.
        val clipboard = app.getSystemService(ClipboardManager::class.java)
        rule.onNodeWithText("Completo").performClick()
        waitText("Esta versão tem as suas movimentações de verdade")
        rule.onNodeWithText("Copiar").performClick()
        waitText("Copiar o relatório completo?")
        assertNull(clipboard.primaryClip)
        rule.onNodeWithText("Copiar mesmo assim").performClick()
        waitFor { clipboard.primaryClip != null }
        val clip = clipboard.primaryClip!!
        assertTrue(clip.getItemAt(0).text.contains("Maria Silva", ignoreCase = true))
        assertTrue(clip.description.extras?.getBoolean(ClipDescription.EXTRA_IS_SENSITIVE) == true)
        app.real.dev.enabled = false
        runBlocking { app.real.settings.clear() }
    }

    @Test fun bloqueioVoltaQuandoOAndroidFechaOAppEmSegundoPlano() {
        realData()
        runBlocking { app.real.settings.setLock(true) }
        waitText("Saldo em contas")
        // O Android fechou o app em segundo plano e, depois, a tela volta "restaurada" (com o estado salvo).
        app.unlocked = false
        rule.activityRule.scenario.recreate()
        try {
            waitText("Cryo bloqueado")
            assertFalse(exists("Saldo em contas"))
        } finally {
            runBlocking { app.real.settings.clear() }
        }
    }

    @Test fun girarATelaNaoPedeDesbloqueioDeNovo() {
        realData()
        runBlocking { app.real.settings.setLock(true) }
        waitText("Saldo em contas")
        // Já desbloqueado neste processo (a pessoa estava usando o app).
        app.unlocked = true
        rule.activityRule.scenario.recreate()
        try {
            waitText("Saldo em contas")
            assertFalse(exists("Cryo bloqueado"))
        } finally {
            runBlocking { app.real.settings.clear() }
        }
    }
}
