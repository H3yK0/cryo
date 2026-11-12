// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureScreenRoboImage
import io.github.h3yk0.cryo.data.AppSettings
import io.github.h3yk0.cryo.data.ThemeMode
import io.github.h3yk0.cryo.data.db.TxType
import io.github.h3yk0.cryo.ui.LocalNav
import io.github.h3yk0.cryo.ui.Navigator
import io.github.h3yk0.cryo.ui.components.LocalContainer
import io.github.h3yk0.cryo.ui.components.LocalFixedToday
import io.github.h3yk0.cryo.ui.components.LocalSettings
import io.github.h3yk0.cryo.ui.screens.CardDetailScreen
import io.github.h3yk0.cryo.ui.theme.CryoTheme
import io.github.h3yk0.cryo.ui.tx.TxEditorScreen
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate

/** Imagens 92–94 (1.9): o cartão com "Estorno" e "Ajustar total", e o lançamento de um estorno com a escolha da fatura. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], application = TestApp::class, qualifiers = "+w393dp-h852dp-xxhdpi")
class Screens19Test {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var c: AppContainer
    private val day = LocalDate.now()
    private val settings = AppSettings(onboardingDone = true, defaultAccountId = 1, dynamicColor = false, userName = "Marina")

    @Before fun setUp() {
        io.github.h3yk0.cryo.domain.engine.EngineRunner.inline = true
        c = AppContainer(ApplicationProvider.getApplicationContext())
        runBlocking { seed(c, day) }
        rule.waitUntil(10_000) { (c.repo.snapshot.value?.txs?.size ?: 0) > 50 }
    }

    private fun show(content: @Composable () -> Unit) {
        rule.setContent {
            CryoTheme(mode = ThemeMode.LIGHT, dynamicColor = false) {
                val nav = rememberNavController()
                val navigator = remember { Navigator(nav) }
                CompositionLocalProvider(LocalContainer provides c, LocalSettings provides settings, LocalNav provides navigator, LocalFixedToday provides day) {
                    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) { content() }
                }
            }
        }
        rule.waitForIdle()
    }

    private fun waitText(t: String) = rule.waitUntil(10_000) { rule.onAllNodesWithText(t, substring = true).fetchSemanticsNodes().isNotEmpty() }
    private val cardId get() = c.repo.snapshot.value!!.cards.first().id

    /** RF-CAR-07 e 11: na tela do cartão, ao lado de "Nova compra", o estorno e o acerto do total com o do banco. */
    @Test fun cartaoComEstornoEAjustarTotal() {
        show { CardDetailScreen(cardId) }
        waitText("Total da fatura")
        rule.onNodeWithText("Estorno").assertHasClickAction()
        captureScreenRoboImage("screenshots/92_cartao_estorno_ajuste.png")
        rule.onNodeWithText("Ajustar total").performClick()
        waitText("Ajustar o total da fatura")
        waitText("Total no banco")
        captureScreenRoboImage("screenshots/93_ajustar_total_da_fatura.png")
    }

    /** RF-CAR-07 e 10: a receita lançada no cartão é um estorno, com a fatura sugerida e as vizinhas para escolher. */
    @Test fun lancarEstornoNoCartao() {
        show { TxEditorScreen(txId = null, initialType = TxType.INCOME, presetAccountId = null, presetCardId = cardId, onDone = {}) }
        waitText("Fatura em que entra")
        waitText("Estorno ou crédito no cartão")
        assertTrue(rule.onAllNodesWithText("Entra na fatura de", substring = true).fetchSemanticsNodes().isNotEmpty())
        captureScreenRoboImage("screenshots/94_estorno_no_cartao.png")
    }
}
