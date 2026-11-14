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
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.printToString
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureScreenRoboImage
import io.github.h3yk0.cryo.data.AppSettings
import io.github.h3yk0.cryo.data.ThemeMode
import io.github.h3yk0.cryo.data.db.TxType
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import io.github.h3yk0.cryo.ui.CryoRoot
import io.github.h3yk0.cryo.ui.screens.InsightsScreen
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

    /** O app inteiro, para o que depende da barra de mensagem e das abas. */
    private fun showApp() {
        rule.setContent {
            CryoTheme(mode = ThemeMode.LIGHT, dynamicColor = false) {
                CompositionLocalProvider(LocalContainer provides c, LocalSettings provides settings, LocalFixedToday provides day) {
                    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) { CryoRoot(null) }
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

    /**
     * RF-CAR-10: "Outra…" abre **todas** as faturas do cartão, inclusive as antigas, para quem está pondo o histórico
     * no app (pedido de quem usa, issue #3: a compra de setembro caía em dezembro e não dava para mudar).
     */
    @Test fun escolherUmaFaturaAntigaPelaLista() {
        show { TxEditorScreen(txId = null, initialType = TxType.EXPENSE, presetAccountId = null, presetCardId = cardId, onDone = {}) }
        waitText("Fatura em que entra")
        rule.onNodeWithText("Outra…").performScrollTo().performClick()
        waitText("Em qual fatura entra")
        captureScreenRoboImage("screenshots/95_escolher_a_fatura.png")
        // Uma fatura de três meses atrás: fora dos chips vizinhos, mas na lista.
        val old = io.github.h3yk0.cryo.domain.CardMath.invoiceFor(c.repo.snapshot.value!!.cards.first(), day).minusMonths(3)
        val label = io.github.h3yk0.cryo.domain.Dates.monthYearInSentence(old, day).replaceFirstChar { it.uppercase() }
        rule.onAllNodes(hasScrollToNodeAction()).onLast().performScrollToNode(hasText(label, substring = true))
        rule.onNodeWithText(label, substring = true).performClick()
        rule.waitUntil(10_000) { rule.onAllNodesWithText("Em qual fatura entra").fetchSemanticsNodes().isEmpty() }
        // O formulário passa a dizer que a compra entra nessa fatura.
        waitText(io.github.h3yk0.cryo.domain.Dates.monthInSentence(old))
    }

    /**
     * ERR-046: a barra de desfazer cobria o botão "Lançar" do Extrato, e a pessoa tinha de esperar a barra sumir para
     * lançar de novo. Agora o botão sobe a altura da barra enquanto ela está à vista.
     */
    @Test fun aBarraDeDesfazerNaoCobreOBotaoDeLancar() {
        showApp()
        waitText("Saldo em contas")
        rule.onNodeWithText("Extrato").performClick()
        // ERR-047: o botão estendido só tem nome para o leitor de tela porque nós demos um.
        rule.waitUntil(10_000) { rule.onAllNodesWithContentDescription("Lançar").fetchSemanticsNodes().isNotEmpty() }
        fun fabBounds() = rule.onNodeWithContentDescription("Lançar").fetchSemanticsNode().boundsInRoot
        val before = fabBounds().bottom
        c.message("Lançamento salvo", "Desfazer") { }
        waitText("Desfazer")
        rule.waitUntil(10_000) { fabBounds().bottom < before - 10f }
        val fab = fabBounds()
        val bar = rule.onNodeWithText("Desfazer").fetchSemanticsNode().boundsInRoot
        assertTrue("o botão continua por baixo da barra", fab.bottom <= bar.top + 1f)
        captureScreenRoboImage("screenshots/96_desfazer_sem_cobrir_o_botao.png")
    }

    /** RF-MOT-04: em Análises, numa frase: a conta fica negativa? quando? qual o menor saldo? */
    @Test fun previsaoDizSeAContaFicaNegativa() {
        show { InsightsScreen() }
        rule.onNode(hasScrollToNodeAction()).performScrollToNode(hasText("Próximos meses", substring = true))
        rule.onNode(hasScrollToNodeAction()).performScrollToNode(hasText("Menor saldo previsto", substring = true))
        waitText("Menor saldo previsto")
        assertTrue(
            "tem de dizer se fica negativa ou não",
            rule.onAllNodesWithText("A conta não fica negativa", substring = true).fetchSemanticsNodes().isNotEmpty() ||
                rule.onAllNodesWithText("A conta fica negativa a partir", substring = true).fetchSemanticsNodes().isNotEmpty(),
        )
        captureScreenRoboImage("screenshots/97_previsao_saldo_minimo.png")
    }
}
