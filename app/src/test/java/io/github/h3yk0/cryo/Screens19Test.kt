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
import io.github.h3yk0.cryo.data.db.CategoryKind
import io.github.h3yk0.cryo.ui.screens.BillEditorScreen
import io.github.h3yk0.cryo.ui.screens.PlanScreen
import io.github.h3yk0.cryo.ui.screens.AlertSettingsScreen
import io.github.h3yk0.cryo.ui.screens.CardDetailScreen
import io.github.h3yk0.cryo.ui.screens.ImportsScreen
import io.github.h3yk0.cryo.ui.theme.CryoTheme
import io.github.h3yk0.cryo.ui.tx.TxEditorScreen
import kotlinx.coroutines.runBlocking
import io.github.h3yk0.cryo.ui.screens.MoneyActionSheet
import io.github.h3yk0.cryo.ui.components.rememberLedger
import io.github.h3yk0.cryo.domain.Res
import org.junit.Assert.assertEquals
import io.github.h3yk0.cryo.ui.screens.TransactionsScreen
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

    /**
     * RF-CFX-09..12: a conta fixa que não é mensal. O modelo "IPVA" já vem anual e em janeiro; a pessoa escolhe em
     * quantas parcelas paga e informa o preço à vista para ver os juros.
     */
    @Test fun contaFixaAnualAPartirDeUmModelo() {
        show { BillEditorScreen(null, CategoryKind.EXPENSE) }
        waitText("Começar por um modelo")
        rule.onNodeWithText("IPVA").performScrollTo().performClick()
        waitText("Uma vez por ano")
        waitText("Primeiro vencimento")
        waitText("Pago em parcelas")
        rule.onNodeWithText("3x").performScrollTo().performClick()
        waitText("Preço à vista")
        captureScreenRoboImage("screenshots/98_conta_fixa_anual.png")
    }

    /** RF-CFX-09/10: na lista, a conta que não é mensal diz o ritmo e a parcela do ciclo. */
    @Test fun aListaMostraORitmoEAParcela() {
        val ym = day.year * 100 + day.monthValue
        runBlocking {
            c.repo.saveBill(
                io.github.h3yk0.cryo.data.db.Bill(
                    name = "IPVA", amount = 42_000, dueDay = 10, startYm = ym, every = 12, installments = 3,
                    cashAmount = 120_000, accountId = c.repo.snapshot.value!!.accounts.first().id,
                ),
            )
        }
        rule.waitUntil(10_000) { c.repo.snapshot.value?.bills?.any { it.every == 12 } == true }
        show { PlanScreen(1) }
        waitText("IPVA")
        waitText("Uma vez por ano")
        waitText("Parcela 1 de 3")
        captureScreenRoboImage("screenshots/99_contas_fixas_anual.png")
    }

    /**
     * ERR-048: trocar o tom (ou qualquer chave) em Ajustes › Avisos aparecia só depois de sair e voltar da tela.
     * A tela lia o contador de mudanças mas nunca o usava, então o Compose não registrava leitura nenhuma.
     */
    @Test fun trocarOTomDoAvisoApareceNaHora() {
        io.github.h3yk0.cryo.notify.AlertState(ApplicationProvider.getApplicationContext<android.app.Application>()).tone =
            io.github.h3yk0.cryo.notify.AlertTone.FRIEND
        show { AlertSettingsScreen() }
        waitText("Frases curtas e animadas")
        rule.onNodeWithText("Discreto").performClick()
        waitText("Só o essencial")
        assertTrue(rule.onAllNodesWithText("Frases curtas e animadas", substring = true).fetchSemanticsNodes().isEmpty())
    }

    /** RF-IMP-23: a lista das importações feitas, com o botão de apagar a importação inteira (imagem 100). */
    /** RF-EXT-07: a busca do Extrato abre com o período (tudo, mês, ano) e as setas andam no ano escolhido. */
    @Test fun buscaDoExtratoComPeriodo() {
        show { TransactionsScreen(null) }
        rule.onNodeWithContentDescription("Buscar").performClick()
        waitText("Buscar por nome, categoria ou valor")
        waitText("Mês")
        rule.onNodeWithText("Ano").performClick()
        waitText(day.year.toString())
        rule.onNodeWithContentDescription("Ano anterior").assertExists()
        captureScreenRoboImage("screenshots/105_busca_com_periodo.png")
    }

    /** RF-FIX-11: a chave "não mexer no saldo" tira a escolha da conta e confirma por fora, sem conta nenhuma. */
    @Test fun pagarContaFixaPorFora() {
        var normal = 0
        var fora = 0L
        show {
            val l = rememberLedger()
            if (l != null) {
                MoneyActionSheet(
                    title = "Dízimo", subtitle = null, initialAmount = 20_000, l = l, accountLabel = "Pago com", initialAccount = null,
                    confirmLabel = "Confirmar", onDismiss = {}, onConfirm = { _, _, _ -> normal++ },
                    offLabel = Res.s(R.string.plan_fora_do_saldo), offHint = Res.s(R.string.plan_fora_do_saldo_hint),
                    onConfirmOff = { amount, _ -> fora = amount },
                )
            }
        }
        waitText("Não mexer no saldo de nenhuma conta")
        waitText("Pago com")
        rule.onNodeWithText("Não mexer no saldo de nenhuma conta").performClick()
        rule.waitUntil(5_000) { rule.onAllNodesWithText("Pago com").fetchSemanticsNodes().isEmpty() }
        captureScreenRoboImage("screenshots/104_conta_fixa_paga_por_fora.png")
        rule.onNodeWithText("Confirmar").performClick()
        rule.waitUntil(5_000) { fora == 20_000L }
        assertEquals(0, normal)
    }

    @Test fun listaDeImportacoesFeitas() {
        val acc = c.repo.snapshot.value!!.accounts.first().id
        runBlocking {
            val now = System.currentTimeMillis()
            c.repo.importTxs(
                listOf(
                    io.github.h3yk0.cryo.data.db.Tx(type = TxType.EXPENSE, amount = 2345, date = day.minusDays(3), description = "Padaria", accountId = acc, importKey = "a$acc|ofx.id:1", createdAt = now),
                    io.github.h3yk0.cryo.data.db.Tx(type = TxType.INCOME, amount = 120000, date = day.minusDays(2), description = "Transferência recebida", accountId = acc, importKey = "a$acc|ofx.id:2", createdAt = now),
                ),
            )
        }
        rule.waitUntil(10_000) { c.repo.snapshot.value?.txs?.any { it.importKey != null } == true }
        show { ImportsScreen() }
        waitText("Importações feitas")
        waitText("2 lançamentos")
        captureScreenRoboImage("screenshots/100_importacoes_feitas.png")
    }

    /**
     * RF-MOT-04: em Análises, numa frase: a conta fica negativa? quando? qual o menor saldo? Desde a 1.6 a frase fica no
     * cartão "Como vou fechar o ano?" (RF-MOT-17), que ocupou o lugar de "Próximos meses".
     */
    @Test fun previsaoDizSeAContaFicaNegativa() {
        show { InsightsScreen() }
        rule.onNode(hasScrollToNodeAction()).performScrollToNode(hasText("Como vou fechar o ano?", substring = true))
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
