// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo

import android.content.Intent
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import io.github.h3yk0.cryo.data.Palette
import io.github.h3yk0.cryo.data.SettingsRepository
import io.github.h3yk0.cryo.data.db.Account
import io.github.h3yk0.cryo.data.db.AccountType
import io.github.h3yk0.cryo.notify.Reminders
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

/**
 * RF-WID-05 (1.7, LA9) no app de verdade: o "Lançar" do widget abre o registro por frase; com o bloqueio ligado, ele passa
 * pelo desbloqueio antes (o widget não abre nada por fora do bloqueio).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "+w393dp-h852dp-xxhdpi")
class WidgetFlowTest {
    @get:Rule val rule = createEmptyComposeRule()

    private val app get() = ApplicationProvider.getApplicationContext<CryoApp>()

    @Before fun setUp() {
        io.github.h3yk0.cryo.data.GuideStore(app).enabled = false
        runBlocking {
            val r = app.real
            r.settings.clear()
            SettingsRepository(app, sandbox = true).clear()
            io.github.h3yk0.cryo.notify.AlertState(app).reminderAsked = true
            val acc = r.repo.saveAccount(Account(name = "Minha conta", type = AccountType.CHECKING, initialBalance = 50000, color = Palette.pick(1)))
            r.settings.setDefaultAccount(acc)
            r.settings.setOnboardingDone(true)
        }
        app.unlocked = false
    }

    @After fun tearDown() {
        runBlocking { app.real.settings.clear() }
    }

    private fun waitFor(ms: Long = 10_000, condition: () -> Boolean) = rule.waitUntil(ms) {
        ShadowLooper.idleMainLooper()
        condition()
    }

    private fun exists(t: String) = rule.onAllNodesWithText(t, substring = true).fetchSemanticsNodes().isNotEmpty()

    private fun fromWidget() = Intent(app, MainActivity::class.java).putExtra(Reminders.EXTRA_OPEN, "entry")

    /** RF-WID-05: "Lançar" abre o campo de escrever o lançamento, por cima do Início. */
    @Test fun lancarAbreORegistroPorFrase() {
        ActivityScenario.launch<MainActivity>(fromWidget()).use {
            waitFor { exists("Escrever o lançamento") }
        }
    }

    /** RF-WID-05 e RF-BLQ: com o bloqueio ligado, primeiro o desbloqueio; só depois o campo de escrever. */
    @Test fun comOBloqueioPassaPeloDesbloqueioAntes() {
        runBlocking { app.real.settings.setLock(true) }
        ActivityScenario.launch<MainActivity>(fromWidget()).use { scenario ->
            waitFor { exists("Cryo bloqueado") }
            assertFalse(exists("Escrever o lançamento"))
            scenario.onActivity { it.unlock() }
            waitFor { exists("Escrever o lançamento") }
            assertFalse(exists("Cryo bloqueado"))
        }
    }

    /** RF-WID-03: o widget é dos dados de verdade; tocar nele com o modo de teste ligado sai do modo de teste. */
    @Test fun comOModoDeTesteAbreOsDadosDeVerdade() {
        app.real.dev.enabled = true
        runBlocking { app.prepareTestData() }
        app.switchData(true)
        try {
            ActivityScenario.launch<MainActivity>(fromWidget()).use {
                assertFalse(app.container.sandbox)
                waitFor { exists("Escrever o lançamento") }
                assertTrue(app.real.dev.enabled) // só saiu do modo de teste
            }
        } finally {
            app.switchData(false)
            app.real.dev.enabled = false
        }
    }
}
