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
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performScrollTo
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureScreenRoboImage
import io.github.h3yk0.cryo.data.AppSettings
import io.github.h3yk0.cryo.data.ThemeMode
import io.github.h3yk0.cryo.data.db.Bill
import io.github.h3yk0.cryo.data.db.TxType
import io.github.h3yk0.cryo.ui.LocalNav
import io.github.h3yk0.cryo.ui.Navigator
import io.github.h3yk0.cryo.ui.components.LocalContainer
import io.github.h3yk0.cryo.ui.components.LocalFixedToday
import io.github.h3yk0.cryo.ui.components.LocalSettings
import io.github.h3yk0.cryo.ui.screens.AccountDetailScreen
import io.github.h3yk0.cryo.ui.screens.HomeScreen
import io.github.h3yk0.cryo.ui.screens.InsightsScreen
import io.github.h3yk0.cryo.ui.screens.PlanScreen
import io.github.h3yk0.cryo.ui.screens.TransactionsScreen
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

/**
 * Imagens 106–115 (1.6, lotes LA1–LA8): a saúde no Início, o Extrato com a conta de cada lançamento, o filtro por conta, as
 * contas previstas, o extrato da conta, o orçamento com as contas fixas, "Como vou fechar o ano?", a previsão de metas e
 * o último dia no lançamento.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], application = TestApp::class, qualifiers = "+w393dp-h852dp-xxhdpi")
class Screens16Test {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var c: AppContainer
    private val day = LocalDate.now()
    private var settings = AppSettings(onboardingDone = true, defaultAccountId = 1, dynamicColor = false, userName = "Marina")

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

    private fun waitText(t: String) = rule.waitUntil(15_000) { rule.onAllNodesWithText(t, substring = true).fetchSemanticsNodes().isNotEmpty() }
    private val accountId get() = c.repo.snapshot.value!!.accounts.first().id

    /** RF-INI-18: o anel da saúde financeira ao lado do saldo. */
    @Test fun inicioComASaude() {
        show { HomeScreen() }
        waitText("Saldo em contas")
        rule.waitUntil(15_000) { rule.onAllNodes(androidx.compose.ui.test.hasContentDescription("Saúde financeira", substring = true)).fetchSemanticsNodes().isNotEmpty() }
        captureScreenRoboImage("screenshots/106_inicio_saude.png")
    }

    /** RF-EXT-08, RF-EXT-10 e RF-EXT-09: a conta de cada lançamento, o agendado e o filtro "Todas as contas". */
    @Test fun extratoComAsContasEOFiltro() {
        show { TransactionsScreen(null) }
        waitText("Todas as contas")
        captureScreenRoboImage("screenshots/107_extrato_contas.png")
        rule.onNodeWithText("Todas as contas").performClick()
        waitText("Ver o Extrato de")
        val name = c.repo.snapshot.value!!.accounts.first().name
        rule.onAllNodesWithText(name).onFirst().performClick()
        rule.waitUntil(10_000) { rule.onAllNodesWithText("Ver o Extrato de").fetchSemanticsNodes().isEmpty() }
        captureScreenRoboImage("screenshots/108_extrato_filtro_conta.png")
    }

    /** RF-EXT-11: as contas fixas do mês ainda em aberto, com Já paguei, Ajustar e Pular este mês. */
    @Test fun previstasNoExtrato() {
        // Uma conta fixa que vence no fim do mês, para sempre haver uma em aberto.
        runBlocking {
            c.repo.saveBill(Bill(name = "Academia", amount = 12_000, dueDay = 28, startYm = day.year * 100 + day.monthValue, accountId = accountId))
        }
        rule.waitUntil(10_000) { c.repo.snapshot.value?.bills?.any { it.name == "Academia" } == true }
        show { TransactionsScreen(null) }
        waitText("Previstas")
        rule.onNodeWithText("Previstas").performScrollTo().performClick()
        waitText("Contas fixas ainda em aberto")
        waitText("Pular este mês")
        captureScreenRoboImage("screenshots/109_previstas.png")
    }

    /** RF-CON-08: o extrato da conta, por dia, com o saldo depois de cada lançamento. */
    @Test fun extratoDaConta() {
        show { AccountDetailScreen(accountId) }
        waitText("Extrato da conta")
        waitText("Fim do dia")
        captureScreenRoboImage("screenshots/110_extrato_da_conta.png")
    }

    /** RF-ORC-09: as contas fixas que não cabem no orçamento, com a sugestão de ajustar. */
    @Test fun orcamentoComAsContasFixas() {
        val s = c.repo.snapshot.value!!
        // O Geral (código 0, RF-ORC-10) não é categoria: a conta fixa vai para o orçamento de uma categoria.
        val first = s.budgets.first { it.categoryId != io.github.h3yk0.cryo.domain.GeneralBudget.ID }
        val budgeted = first.categoryId
        val limit = first.limitAmount
        runBlocking {
            c.repo.saveBill(Bill(name = "Mensalidade", amount = limit + 20_000, dueDay = 12, startYm = day.year * 100 + day.monthValue, categoryId = budgeted, accountId = accountId))
        }
        rule.waitUntil(10_000) { c.repo.snapshot.value?.bills?.any { it.name == "Mensalidade" } == true }
        show { PlanScreen(0) }
        // RF-ORC-11: as sugestões das contas fixas ficam juntas num cartão, fechadas com setinha.
        waitText("Sugestões das contas fixas")
        rule.onNodeWithText("Sugestões das contas fixas", substring = true).performClick()
        waitText("Agora não")
        captureScreenRoboImage("screenshots/111_orcamento_contas_fixas.png")
    }

    /**
     * RF-MOT-17 e RF-MOT-16: "Como vou fechar o ano?" em Análises; a previsão de metas, desde 21/11/2026, em Planejar › Metas
     * (RF-ANA-07, RF-MET-07), no topo e em cada bloco.
     */
    @Test fun anoEMetasEmAnalises() {
        show { InsightsScreen() }
        waitText("Previsão do fim do mês")
        rule.onAllNodes(hasScrollToNodeAction()).onFirst().performScrollToNode(hasText("Como vou fechar o ano?"))
        rule.waitForIdle()
        captureScreenRoboImage("screenshots/112_como_vou_fechar_o_ano.png")
        assertTrue(rule.onAllNodesWithText("Próximos meses").fetchSemanticsNodes().isEmpty())
        assertTrue(rule.onAllNodesWithText("Previsão de metas").fetchSemanticsNodes().isEmpty())
    }

    @Test fun previsaoDeMetasEmPlanejar() {
        show { PlanScreen(2) }
        waitText("As metas pedem")
        captureScreenRoboImage("screenshots/113_previsao_de_metas.png")
    }

    /** RF-LAN-10: pela tela da conta, o lançamento começa no último dia usado, com o botão dele em "Data". */
    @Test fun ultimoDiaNoLancamento() {
        settings = settings.copy(lastTxDate = day.minusDays(9))
        show { TxEditorScreen(txId = null, initialType = TxType.EXPENSE, presetAccountId = accountId, presetCardId = null, onDone = {}) }
        waitText("Último:")
        captureScreenRoboImage("screenshots/114_ultimo_dia.png")
    }
}
