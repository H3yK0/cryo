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
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureScreenRoboImage
import io.github.h3yk0.cryo.data.AppSettings
import io.github.h3yk0.cryo.data.ThemeMode
import io.github.h3yk0.cryo.data.db.Budget
import io.github.h3yk0.cryo.domain.engine.EngineRunner
import io.github.h3yk0.cryo.ui.LocalNav
import io.github.h3yk0.cryo.ui.Navigator
import io.github.h3yk0.cryo.ui.components.LocalContainer
import io.github.h3yk0.cryo.ui.components.LocalFixedToday
import io.github.h3yk0.cryo.ui.components.LocalSettings
import io.github.h3yk0.cryo.ui.screens.InsightsScreen
import io.github.h3yk0.cryo.ui.screens.InvestmentDetailScreen
import io.github.h3yk0.cryo.ui.screens.PlanScreen
import io.github.h3yk0.cryo.ui.screens.WalletScreen
import io.github.h3yk0.cryo.ui.theme.CryoTheme
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

/**
 * Imagens 82–85 (1.8, RF-PRV-08): a previsão do mês conferida no passado e explicada, o fechamento de cada orçamento com a
 * chance de passar, e o investimento "se continuar como foi". Os dados são um mundo inventado de 26 meses do laboratório
 * ([ForecastWorld]), porque a conferência do passado só aparece com meses bastantes.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], application = TestApp::class, qualifiers = "+w393dp-h852dp-xxhdpi")
class Screens18Test {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var c: AppContainer
    private val day = LocalDate.now().let { if (it.dayOfMonth >= 27) it.withDayOfMonth(14) else it }
    private val settings = AppSettings(onboardingDone = true, defaultAccountId = 1, dynamicColor = false)
    private val colors = intArrayOf(0xFFE57373.toInt(), 0xFF4DB6AC.toInt(), 0xFF64B5F6.toInt(), 0xFFBA68C8.toInt(), 0xFFFFB74D.toInt(), 0xFF90A4AE.toInt())

    @Before fun setUp() {
        EngineRunner.inline = true
        c = AppContainer(ApplicationProvider.getApplicationContext())
        val world = ForecastWorld.generate(ForecastWorld.spec(ForecastWorld.Kind.MONTHLY_SHOP), day, 7)
        val snap = world.copy(
            categories = world.categories.mapIndexed { i, cat -> cat.copy(color = colors[i % colors.size]) },
            accounts = world.accounts.mapIndexed { i, a -> a.copy(color = colors[(i + 2) % colors.size]) },
            cards = world.cards.map { it.copy(color = colors[3]) },
            investments = world.investments.map { it.copy(color = colors[1]) },
            budgets = listOf(Budget(1, 60_000), Budget(2, 130_000), Budget(4, 40_000)),
        )
        runBlocking { c.repo.replaceAll(snap) }
        rule.waitUntil(10_000) { (c.repo.snapshot.value?.txs?.size ?: 0) > 500 }
    }

    private fun show(content: @Composable () -> Unit) {
        rule.setContent {
            CryoTheme(mode = ThemeMode.LIGHT, dynamicColor = false) {
                val nav = rememberNavController()
                val navigator = remember { Navigator(nav) }
                CompositionLocalProvider(
                    LocalContainer provides c, LocalSettings provides settings, LocalNav provides navigator, LocalFixedToday provides day,
                ) {
                    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) { content() }
                }
            }
        }
        rule.waitForIdle()
    }

    private fun waitText(t: String) =
        rule.waitUntil(10_000) { rule.onAllNodesWithText(t, substring = true).fetchSemanticsNodes().isNotEmpty() }

    @Config(qualifiers = "+w393dp-h1900dp-xxhdpi")
    @Test fun previsaoConferidaEExplicada() {
        show { InsightsScreen() }
        waitText("Conferida no seu passado")
        waitText("O que mais pesou na conta")
        captureScreenRoboImage("screenshots/82_previsao_conferida.png")
    }

    @Config(qualifiers = "+w393dp-h1200dp-xxhdpi")
    @Test fun orcamentosComOFechamentoDoMes() {
        show { PlanScreen(0) }
        waitText("Deve fechar o mês perto de")
        captureScreenRoboImage("screenshots/83_orcamentos_fechamento.png")
    }

    @Config(qualifiers = "+w393dp-h1700dp-xxhdpi")
    @Test fun investimentoSeContinuarComoFoi() {
        show { InvestmentDetailScreen(1) }
        waitText("Se continuar como foi")
        waitText("Em 5 anos")
        assertTrue(rule.onAllNodesWithText("não uma promessa", substring = true).fetchSemanticsNodes().isNotEmpty())
        captureScreenRoboImage("screenshots/84_investimento_projecao.png")
    }

    @Config(qualifiers = "+w393dp-h1400dp-xxhdpi")
    @Test fun carteiraComAProjecao() {
        show { WalletScreen() }
        waitText("sua carteira fica perto de")
        captureScreenRoboImage("screenshots/85_carteira_projecao.png")
    }
}
