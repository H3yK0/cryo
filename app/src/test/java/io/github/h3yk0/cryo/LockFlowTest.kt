// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo

import android.content.Intent
import android.net.Uri
import android.view.WindowManager
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.github.takahirom.roborazzi.captureRoboImage
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ApplicationProvider
import io.github.h3yk0.cryo.data.Palette
import io.github.h3yk0.cryo.data.SettingsRepository
import io.github.h3yk0.cryo.data.db.Account
import io.github.h3yk0.cryo.data.db.AccountType
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper
import org.robolectric.shadows.ShadowSystemClock
import java.time.Duration

/**
 * RF-BLQ-02, RF-BLQ-07 e RF-BLQ-08 (1.7, LA2 e LA6, issues #6 e #7, ERR-070) no app de verdade: o bloqueio volta no tempo
 * que a pessoa escolheu, girar a tela e voltar do que o próprio Cryo abriu não pedem, e o modo privacidade protege a janela.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "+w393dp-h852dp-xxhdpi")
class LockFlowTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    private val app get() = rule.activity.application as CryoApp

    @Before fun setUp() {
        io.github.h3yk0.cryo.data.GuideStore(ApplicationProvider.getApplicationContext<android.app.Application>()).enabled = false
        runBlocking {
            val r = app.real
            r.settings.clear()
            SettingsRepository(app, sandbox = true).clear()
            io.github.h3yk0.cryo.notify.AlertState(app).reminderAsked = true
            val acc = r.repo.saveAccount(Account(name = "Minha conta", type = AccountType.CHECKING, initialBalance = 50000, color = Palette.pick(1)))
            r.settings.setDefaultAccount(acc)
            r.settings.setOnboardingDone(true)
        }
        waitText("Saldo em contas")
    }

    @After fun tearDown() {
        runBlocking { app.real.settings.clear() }
    }

    private fun waitFor(ms: Long = 10_000, condition: () -> Boolean) = rule.waitUntil(ms) {
        ShadowLooper.idleMainLooper()
        condition()
    }

    private fun exists(t: String) = rule.onAllNodesWithText(t, substring = true).fetchSemanticsNodes().isNotEmpty()
    private fun waitText(t: String) = waitFor { exists(t) }

    /** Liga o bloqueio com o tempo escolhido e deixa a pessoa já dentro (desbloqueada). */
    private fun lockWith(seconds: Int, privacy: Boolean = false) {
        runBlocking {
            app.real.settings.setLock(true)
            app.real.settings.setLockAfter(seconds)
            app.real.settings.setPrivacyMode(privacy)
        }
        app.unlocked = true
        // Espera a tela ler os ajustes novos (a leitura é por fluxo, fora da linha principal).
        waitFor { rule.activity.lockAfterNow == seconds }
        rule.waitForIdle()
    }

    /** Sai do app (segundo plano), espera [away] e volta. */
    private fun leaveAndReturn(away: Duration) {
        rule.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        ShadowSystemClock.advanceBy(away)
        rule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        rule.waitForIdle()
    }

    /** ERR-070: com "Na hora", toda volta pede o desbloqueio, até a de meio segundo (apagar e acender a tela). */
    @Test fun naHoraPedeDeNovoACadaVolta() {
        lockWith(0)
        leaveAndReturn(Duration.ofMillis(500))
        waitText("Cryo bloqueado")
        rule.runOnUiThread { rule.activity.unlock() }
        waitText("Saldo em contas")
        // E de novo: não foi "uma vez só" (a issue #6).
        leaveAndReturn(Duration.ofSeconds(2))
        waitText("Cryo bloqueado")
    }

    /** RF-BLQ-02: com 1 minuto, 30 s fora não pede; mais de 1 minuto pede. */
    @Test fun umMinutoEsperaOTempo() {
        lockWith(60)
        leaveAndReturn(Duration.ofSeconds(30))
        assertFalse(exists("Cryo bloqueado"))
        leaveAndReturn(Duration.ofSeconds(61))
        waitText("Cryo bloqueado")
    }

    /** Girar a tela (e as recriações do próprio app) não é sair do app, nem com "Na hora". */
    @Test fun girarATelaComNaHoraNaoPede() {
        lockWith(0)
        rule.activityRule.scenario.recreate()
        rule.waitForIdle()
        waitText("Saldo em contas")
        assertFalse(exists("Cryo bloqueado"))
    }

    /** RF-BLQ-08: voltar do que o próprio Cryo abriu (aqui, o navegador) não bloqueia; uma saída comum depois, sim. */
    @Test fun voltarDoQueOCryoAbriuNaoBloqueia() {
        lockWith(0)
        rule.runOnUiThread { rule.activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://example.org"))) }
        leaveAndReturn(Duration.ofSeconds(40))
        assertFalse(exists("Cryo bloqueado"))
        leaveAndReturn(Duration.ofSeconds(1))
        waitText("Cryo bloqueado")
    }

    /** RF-BLQ-07 (ERR-070, issue #7): o modo privacidade protege a janela; sem ele, ou sem o bloqueio, não (Android 13+). */
    @Test fun modoPrivacidadeProtegeAJanela() {
        fun secure() = rule.activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE != 0
        lockWith(60, privacy = true)
        waitFor { secure() }
        runBlocking { app.real.settings.setPrivacyMode(false) }
        waitFor { !secure() }
        runBlocking { app.real.settings.setPrivacyMode(true) }
        waitFor { secure() }
        // Desligar o bloqueio desliga a proteção.
        runBlocking { app.real.settings.setLock(false) }
        waitFor { !secure() }
        assertFalse(secure())
    }

    /** Imagem 133: Ajustes › Privacidade e segurança com o bloqueio ligado ("Bloquear de novo" e o modo privacidade). */
    @org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
    @Test fun ajustesDoBloqueio() {
        lockWith(0, privacy = true)
        rule.onNode(androidx.compose.ui.test.hasContentDescription("Perfil e ajustes")).performClick()
        waitText("Todos os ajustes")
        rule.onNode(androidx.compose.ui.test.hasText("Todos os ajustes")).performClick()
        waitText("Privacidade e segurança")
        rule.onAllNodes(androidx.compose.ui.test.hasScrollToNodeAction())[0]
            .performScrollToNode(androidx.compose.ui.test.hasText("Modo privacidade"))
        rule.waitForIdle()
        assertTrue(exists("Bloquear de novo"))
        assertTrue(exists("Na hora"))
        rule.onRoot().captureRoboImage("screenshots/133_ajustes_bloqueio.png")
        // A folha das escolhas.
        rule.onNode(androidx.compose.ui.test.hasText("Bloquear de novo")).performClick()
        waitText("Depois de 15 minutos")
        rule.onAllNodes(androidx.compose.ui.test.hasText("Depois de 5 minutos"))[0].performClick()
        waitFor { runBlocking { app.real.settings.current().lockAfter } == 300 }
    }

    /** RF-BLQ-05: no Android 12 ou mais antigo, com o bloqueio ligado, a janela fica sempre protegida (a miniatura em Recentes). */
    @Config(sdk = [32])
    @Test fun androidAntigoComBloqueioSempreProtege() {
        fun secure() = rule.activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE != 0
        lockWith(60, privacy = false)
        waitFor { secure() }
        assertTrue(secure())
    }
}
