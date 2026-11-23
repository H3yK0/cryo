// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.SemanticsNodeInteraction

import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import io.github.h3yk0.cryo.data.AppSettings
import io.github.h3yk0.cryo.data.ThemeMode
import io.github.h3yk0.cryo.ui.CryoRoot
import io.github.h3yk0.cryo.ui.components.LocalContainer
import io.github.h3yk0.cryo.ui.components.LocalFixedToday
import io.github.h3yk0.cryo.ui.components.LocalSettings
import io.github.h3yk0.cryo.ui.theme.CryoTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/** RF-GES-01..03 (1.7, LA7): arrastar para os lados troca de aba, passa pelas partes de Planejar e não briga com outros gestos. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = TestApp::class, qualifiers = "+w393dp-h852dp-xxhdpi")
class TabSwipeTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var c: AppContainer
    private val day = LocalDate.now()

    @Before fun setUp() {
        io.github.h3yk0.cryo.domain.engine.EngineRunner.inline = true
        c = AppContainer(ApplicationProvider.getApplicationContext())
        runBlocking { seed(c, day) }
        rule.waitUntil(10_000) { (c.repo.snapshot.value?.txs?.size ?: 0) > 50 }
        rule.setContent {
            CryoTheme(mode = ThemeMode.LIGHT, dynamicColor = false) {
                CompositionLocalProvider(
                    LocalContainer provides c,
                    LocalSettings provides AppSettings(onboardingDone = true, dynamicColor = false),
                    LocalFixedToday provides day,
                ) { CryoRoot(null) }
            }
        }
        // Na língua do teste (os testes em árabe também passam por aqui).
        val balance = c.app.getString(R.string.home_balance)
        rule.waitUntil(15_000) { rule.onAllNodesWithText(balance).fetchSemanticsNodes().isNotEmpty() }
    }

    /** A aba marcada na barra de baixo (o item selecionado com esse nome). */
    private fun tabSelected(label: String) =
        rule.onAllNodes(hasText(label) and isSelected()).fetchSemanticsNodes().isNotEmpty()

    private fun waitTab(label: String) = rule.waitUntil(10_000) { rule.waitForIdle(); tabSelected(label) }

    /** Arrasta pela tela, a [y] da altura, de [from] a [to] da largura. */
    private fun SemanticsNodeInteraction.drag(from: Float, to: Float, y: Float = 0.45f, ms: Long = 250) =
        performTouchInput { swipe(Offset(width * from, height * y), Offset(width * to, height * y), ms) }

    @Test fun arrastarParaOLadoTrocaDeAba() {
        assertTrue(tabSelected("Início"))
        // O dedo para a esquerda: a próxima aba.
        rule.onRoot().drag(0.8f, 0.2f)
        waitTab("Extrato")
        // Para a direita: volta.
        rule.onRoot().drag(0.2f, 0.8f)
        waitTab("Início")
        // Na primeira aba, para a direita não há aba antes: fica.
        rule.onRoot().drag(0.2f, 0.8f)
        rule.waitForIdle()
        assertTrue(tabSelected("Início"))
    }

    /** RF-GES-03: o gesto que começa na borda (onde fica o "voltar" do Android) não troca de aba. */
    @Test fun bordaFicaLivreParaOVoltarDoSistema() {
        val edge = with(rule.density) { 20.dp.toPx() }
        rule.onRoot().performTouchInput { swipe(Offset(width - edge, height * 0.45f), Offset(width * 0.2f, height * 0.45f), 250) }
        rule.waitForIdle()
        assertTrue(tabSelected("Início"))
        rule.onRoot().performTouchInput { swipe(Offset(edge, height * 0.45f), Offset(width * 0.8f, height * 0.45f), 250) }
        rule.waitForIdle()
        assertTrue(tabSelected("Início"))
    }

    /** RF-GES-03: rolar a tela para cima ou para baixo (mesmo meio torto) não troca de aba. */
    @Test fun rolarNaoTrocaDeAba() {
        rule.onRoot().performTouchInput { swipe(Offset(width * 0.5f, height * 0.7f), Offset(width * 0.42f, height * 0.25f), 300) }
        rule.waitForIdle()
        assertTrue(tabSelected("Início"))
    }

    /** RF-GES-02: em Planejar, o arrasto passa por Orçamentos › Contas fixas › Metas e só depois vai para Análises. */
    @Test fun planejarPassaPelasPartes() {
        rule.onNode(hasText("Planejar") and isSelectable()).performClick()
        waitTab("Planejar")
        fun part(label: String) = rule.waitUntil(10_000) {
            rule.waitForIdle()
            rule.onAllNodes(hasText(label) and isSelected()).fetchSemanticsNodes().any()
        }
        part("Orçamentos")
        rule.onRoot().drag(0.8f, 0.2f, y = 0.5f)
        part("Contas fixas")
        assertTrue(tabSelected("Planejar"))
        rule.onRoot().drag(0.8f, 0.2f, y = 0.5f)
        part("Metas")
        rule.onRoot().drag(0.8f, 0.2f, y = 0.5f)
        waitTab("Análises")
        // E de volta: Análises › Planejar (na parte em que estava, Metas) › Contas fixas.
        rule.onRoot().drag(0.2f, 0.8f, y = 0.5f)
        waitTab("Planejar")
        rule.onRoot().drag(0.2f, 0.8f, y = 0.5f)
        part("Contas fixas")
    }

    /** RF-GES-03: arrastar o gráfico do ano mexe no gráfico, não troca de aba. */
    @Config(qualifiers = "+w393dp-h1400dp-xxhdpi")
    @Test fun graficoDoAnoNaoTrocaDeAba() {
        rule.onNode(hasText("Análises") and isSelectable()).performClick()
        waitTab("Análises")
        rule.waitUntil(15_000) { rule.onAllNodes(hasContentDescription("Gráfico do saldo previsto", substring = true)).fetchSemanticsNodes().isNotEmpty() }
        rule.onNode(hasContentDescription("Gráfico do saldo previsto", substring = true)).drag(0.85f, 0.15f, y = 0.5f)
        rule.waitForIdle()
        assertTrue(tabSelected("Análises"))
    }

    /** RF-GES-01: em árabe (da direita para a esquerda), o dedo para a direita leva à próxima aba. */
    @Config(qualifiers = "+ar-rSA-ldrtl-w393dp-h852dp-xxhdpi")
    @Test fun emArabeOLadoEspelha() {
        val home = c.app.getString(R.string.tab_home)
        val txs = c.app.getString(R.string.tab_transactions)
        rule.waitUntil(10_000) { tabSelected(home) }
        rule.onRoot().drag(0.2f, 0.8f)
        waitTab(txs)
    }
}
