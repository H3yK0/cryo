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
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.longClick
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureScreenRoboImage
import io.github.h3yk0.cryo.data.AppSettings
import io.github.h3yk0.cryo.data.ThemeMode
import io.github.h3yk0.cryo.domain.GoalArt
import io.github.h3yk0.cryo.domain.key
import io.github.h3yk0.cryo.domain.ym
import io.github.h3yk0.cryo.notify.AlertState
import io.github.h3yk0.cryo.notify.Shopping
import io.github.h3yk0.cryo.ui.CryoRoot
import io.github.h3yk0.cryo.ui.LocalNav
import io.github.h3yk0.cryo.ui.Navigator
import io.github.h3yk0.cryo.ui.components.LocalContainer
import io.github.h3yk0.cryo.ui.components.LocalFixedToday
import io.github.h3yk0.cryo.ui.components.LocalSettings
import io.github.h3yk0.cryo.ui.components.Moment
import io.github.h3yk0.cryo.ui.components.MomentBus
import io.github.h3yk0.cryo.ui.components.MomentHost
import io.github.h3yk0.cryo.ui.screens.AlertSettingsScreen
import io.github.h3yk0.cryo.ui.screens.BudgetMonthScreen
import io.github.h3yk0.cryo.ui.screens.ReminderTimeSheet
import io.github.h3yk0.cryo.ui.screens.ShoppingStartSheet
import io.github.h3yk0.cryo.ui.theme.CryoTheme
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate
import java.time.YearMonth

/**
 * Imagens 71–79 (1.7): o painel do mês (RF-ORC-06), Ajustes › Avisos e sons (RF-AVI-08), a pergunta do lembrete do dia
 * (RF-AVI-11), os momentos (RF-MOM) e o modo compras (RF-AVI-10), além do toque e do segurar no botão de voz (RF-ALC-01).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], application = TestApp::class, qualifiers = "+w393dp-h852dp-xxhdpi")
class Screens17Test {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var c: AppContainer
    private val day = LocalDate.now()
    private val settings = AppSettings(onboardingDone = true, defaultAccountId = 1, dynamicColor = false)

    @Before fun setUp() {
        io.github.h3yk0.cryo.domain.engine.EngineRunner.inline = true
        c = AppContainer(ApplicationProvider.getApplicationContext())
        runBlocking { seed(c, day) }
        rule.waitUntil(10_000) { (c.repo.snapshot.value?.txs?.size ?: 0) > 50 }
    }

    @After fun tearDown() = Shopping.stop(ApplicationProvider.getApplicationContext())

    private fun show(dark: Boolean = false, content: @Composable () -> Unit) {
        rule.setContent {
            CryoTheme(mode = if (dark) ThemeMode.DARK else ThemeMode.LIGHT, dynamicColor = false) {
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
        rule.waitUntil(8_000) { rule.onAllNodesWithText(t, substring = true).fetchSemanticsNodes().isNotEmpty() }

    @Config(qualifiers = "+w393dp-h1600dp-xxhdpi")
    @Test fun painelDoMes() {
        // O Geral (código 0, RF-ORC-10) não é categoria: o remanejamento é entre categorias.
        val budgets = runBlocking { c.repo.loadOnce().budgets }.filter { it.categoryId != io.github.h3yk0.cryo.domain.GeneralBudget.ID }
        runBlocking { c.repo.moveBudget(budgets[1].categoryId, budgets[0].categoryId, 5_000, day.ym().key()) }
        rule.waitUntil(5_000) { c.repo.snapshot.value?.budgetAdjustments?.isNotEmpty() == true }
        show { BudgetMonthScreen() }
        waitText("Gastos de")
        captureScreenRoboImage("screenshots/71_painel_do_mes.png")
    }

    @Config(qualifiers = "+w393dp-h1700dp-xxhdpi")
    @Test fun avisosESons() {
        show { AlertSettingsScreen() }
        waitText("Ver exemplo")
        captureScreenRoboImage("screenshots/72_avisos_e_sons.png")
    }

    @Test fun perguntaDoLembrete() {
        show { ReminderTimeSheet(AlertState.DEFAULT_REMINDER, onDismiss = {}, onChoose = {}) }
        waitText("Quando eu te lembro")
        captureScreenRoboImage("screenshots/73_lembrete_do_dia.png")
    }

    @Test fun momentoOrcamento() {
        val budgets = runBlocking { c.repo.loadOnce().budgets }.filter { it.categoryId != io.github.h3yk0.cryo.domain.GeneralBudget.ID }
        val cat = c.repo.snapshot.value!!.categories.first { it.id == budgets[0].categoryId }
        val other = c.repo.snapshot.value!!.categories.first { it.id == budgets[1].categoryId }
        show {
            val bus = remember {
                MomentBus().apply {
                    show(Moment.Budget(cat.id, cat.name, cat.icon, 61_500, 60_000, over = true, daysLeft = 12, ym = day.ym().key(), move = Moment.BudgetMove(other.id, other.name, 18_000, 2_000)))
                }
            }
            MomentHost(bus)
        }
        waitText("passou do limite")
        captureScreenRoboImage("screenshots/74_momento_orcamento.png")
    }

    @Test fun momentoMeta() {
        show {
            val bus = remember { MomentBus().apply { show(Moment.GoalDone("Viagem", GoalArt.PLANE, 0xFF2AB2D3.toInt(), 500_000, 8)) } }
            MomentHost(bus)
        }
        waitText("Meta completa")
        captureScreenRoboImage("screenshots/75_momento_meta.png")
    }

    @Test fun momentoSequenciaEMes() {
        show {
            val bus = remember { MomentBus().apply { show(Moment.Streak(7)) } }
            MomentHost(bus)
        }
        waitText("7 dias seguidos")
        captureScreenRoboImage("screenshots/76_momento_sequencia.png")
    }

    @Test fun momentoMesFechou() {
        show(dark = true) {
            val bus = remember { MomentBus().apply { show(Moment.MonthClosed(YearMonth.from(day).minusMonths(1), 82_000)) } }
            MomentHost(bus)
        }
        waitText("Continuar")
        captureScreenRoboImage("screenshots/77_momento_mes.png")
    }

    /** RF-MOM-05: o momento fecha com o Voltar, e o leitor de tela lê o texto. */
    @Test fun momentoFechaComVoltar() {
        val bus = MomentBus().apply { show(Moment.Streak(3)) }
        show { MomentHost(bus) }
        waitText("3 dias seguidos")
        androidx.test.espresso.Espresso.pressBack()
        rule.waitUntil(5_000) { bus.current.value == null }
    }

    /** RF-ALC-04: com a fonte do sistema em 200%, nada importante some. */
    @Config(qualifiers = "+w393dp-h852dp-xxhdpi", fontScale = 2.0f)
    @Test fun fonteGrande() {
        show { CryoRoot(null) }
        waitText("Saldo em contas")
        captureScreenRoboImage("screenshots/80_inicio_fonte_200.png")
        rule.onNodeWithContentDescription("Registrar falando").assertExists()
    }

    @Config(qualifiers = "+w393dp-h1400dp-xxhdpi", fontScale = 2.0f)
    @Test fun fonteGrandePainel() {
        show { BudgetMonthScreen() }
        waitText("Gastos de")
        captureScreenRoboImage("screenshots/81_painel_fonte_200.png")
    }

    @Test fun modoCompras() {
        show { ShoppingStartSheet(onDismiss = {}) }
        waitText("Vou às compras")
        captureScreenRoboImage("screenshots/78_compras_comecar.png")
    }

    @Test fun inicioComComprasEBotaoDeVoz() {
        Shopping.start(ApplicationProvider.getApplicationContext(), 30_000, c.repo.snapshot.value)
        show { CryoRoot(null) }
        waitText("Encerrar")
        captureScreenRoboImage("screenshots/79_inicio_compras_e_voz.png")
        // Segurar o botão de voz abre o campo de escrever (tocar abre a voz; aqui, sem microfone, ficaria no pedido).
        rule.onNodeWithContentDescription("Registrar falando").assertExists()
        rule.onNodeWithContentDescription("Registrar falando").performTouchInput { longClick() }
        waitText("Escrever o lançamento")
        assertTrue(rule.onAllNodesWithText("Escrever o lançamento").fetchSemanticsNodes().isNotEmpty())
    }
}
