// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollToKeyAction
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToKey
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureScreenRoboImage
import io.github.h3yk0.cryo.data.AppSettings
import io.github.h3yk0.cryo.data.ThemeMode
import io.github.h3yk0.cryo.domain.Dates
import io.github.h3yk0.cryo.domain.FlowKind
import io.github.h3yk0.cryo.domain.Ledger
import io.github.h3yk0.cryo.domain.Money
import io.github.h3yk0.cryo.domain.moneyFlow
import io.github.h3yk0.cryo.ui.CryoRoot
import io.github.h3yk0.cryo.ui.LocalNav
import io.github.h3yk0.cryo.ui.Navigator
import io.github.h3yk0.cryo.ui.components.FlowFunnel
import io.github.h3yk0.cryo.ui.components.HeatmapCalendar
import io.github.h3yk0.cryo.ui.components.LocalContainer
import io.github.h3yk0.cryo.ui.components.LocalFixedToday
import io.github.h3yk0.cryo.ui.components.LocalSettings
import io.github.h3yk0.cryo.ui.screens.InsightsScreen
import io.github.h3yk0.cryo.ui.theme.CryoTheme
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Imagens 126–132 e o comportamento das LAs da 1.7 em Análises: a saúde financeira sempre no topo (RF-ANA-08), o fluxo do
 * dinheiro numa caixa só com Barras e Funil e a escolha guardada (RF-ANA-09), o toque que abre o Extrato filtrado no mês
 * (RF-ANA-10, RF-EXT-12), o funil sem valores quando eles estão escondidos (ERR-068) e o calendário de calor (RF-ANA-02).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], application = TestApp::class, qualifiers = "+w393dp-h1800dp-xxhdpi")
class InsightsFlowTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var c: AppContainer
    private val day = LocalDate.now()
    private val settings = AppSettings(onboardingDone = true, dynamicColor = false, userName = "Marina")

    @Before fun setUp() {
        io.github.h3yk0.cryo.domain.engine.EngineRunner.inline = true
        c = AppContainer(ApplicationProvider.getApplicationContext())
        runBlocking { seed(c, day); c.settings.setFlowView(0) }
        rule.waitUntil(10_000) { (c.repo.snapshot.value?.txs?.size ?: 0) > 50 }
    }

    private fun show(dark: Boolean = false, s: AppSettings = settings, content: @Composable () -> Unit) {
        rule.setContent {
            CryoTheme(mode = if (dark) ThemeMode.DARK else ThemeMode.LIGHT, dynamicColor = false) {
                val nav = rememberNavController()
                val navigator = remember { Navigator(nav) }
                CompositionLocalProvider(LocalContainer provides c, LocalSettings provides s, LocalNav provides navigator, LocalFixedToday provides day) {
                    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) { content() }
                }
            }
        }
        rule.waitForIdle()
    }

    /** O app inteiro, com a navegação de verdade: abre no Início e vai para Análises pela barra de abas, como a pessoa. */
    private fun app(s: AppSettings = settings) {
        rule.setContent {
            CryoTheme(mode = ThemeMode.LIGHT, dynamicColor = false) {
                CompositionLocalProvider(LocalContainer provides c, LocalSettings provides s, LocalFixedToday provides day) {
                    CryoRoot(null)
                }
            }
        }
        rule.waitForIdle()
        waitText("Saldo em contas")
        rule.onNodeWithText("Análises").performClick()
        rule.waitForIdle()
    }

    private fun waitText(t: String) = rule.waitUntil(15_000) { rule.onAllNodesWithText(t, substring = true).fetchSemanticsNodes().isNotEmpty() }
    private fun top(t: String) = rule.onAllNodesWithText(t)[0].fetchSemanticsNode().boundsInRoot.top
    private fun ledger() = Ledger(c.repo.snapshot.value!!, day)

    /** Para as imagens: o título "Fluxo do dinheiro" no alto da tela, com a caixa inteira à vista. */
    private fun flowOnTop() {
        rule.waitUntil(15_000) { rule.onAllNodes(hasScrollToKeyAction()).fetchSemanticsNodes().isNotEmpty() }
        rule.onAllNodes(hasScrollToKeyAction()).onFirst().performScrollToKey("fluxo_titulo")
        rule.waitForIdle()
    }

    /** RF-ANA-08: a saúde é o primeiro bloco, acima do seletor do mês, e continua lá num mês passado. */
    @Test fun saudeSempreNoTopo() {
        show { InsightsScreen() }
        waitText("Saúde financeira")
        val month = Dates.monthYear(YearMonth.from(day), day)
        assertTrue("a saúde vem antes do mês", top("Saúde financeira") < top(month))
        rule.onNodeWithContentDescription("Mês anterior").performClick()
        rule.waitForIdle()
        waitText(Dates.monthYear(YearMonth.from(day).minusMonths(1), day))
        assertTrue(rule.onAllNodesWithText("Saúde financeira").fetchSemanticsNodes().isNotEmpty())
    }

    /** RF-ANA-09: tudo do fluxo numa caixa só, com Barras e Funil; a escolha fica guardada. */
    @Test fun barrasEFunilNaMesmaCaixa() {
        show { InsightsScreen() }
        flowOnTop()
        waitText("Toque numa parte")
        captureScreenRoboImage("screenshots/126_analises_fluxo_barras.png")
        rule.onNodeWithText("Funil").performClick()
        rule.waitForIdle()
        rule.waitUntil(5_000) { runBlocking { c.settings.current().flowView } == 1 }
        assertEquals(1, runBlocking { c.settings.current().flowView })
        // O funil está na tela: o desenho se descreve com as partes do fluxo.
        rule.waitUntil(5_000) { rule.onAllNodes(hasContentDescription("Salário", substring = true)).fetchSemanticsNodes().isNotEmpty() }
        captureScreenRoboImage("screenshots/127_analises_fluxo_funil.png")
    }

    @Test fun funilNoEscuro() {
        runBlocking { c.settings.setFlowView(1) }
        show(dark = true, s = settings.copy(flowView = 1)) { InsightsScreen() }
        flowOnTop()
        captureScreenRoboImage("screenshots/128_analises_funil_escuro.png")
    }

    @Config(qualifiers = "+ar-rSA-ldrtl-w393dp-h1800dp-xxhdpi")
    @Test fun funilEmArabe() {
        runBlocking { c.settings.setFlowView(1) }
        show(s = settings.copy(flowView = 1)) { InsightsScreen() }
        flowOnTop()
        captureScreenRoboImage("screenshots/129_analises_funil_arabe.png")
    }

    /** RF-ANA-10 e RF-EXT-12: tocar numa categoria de "Para onde foi" abre o Extrato filtrado por ela, no mês escolhido. */
    @Test fun tocarNaCategoriaAbreOExtratoDoMes() {
        val prev = YearMonth.from(day).minusMonths(1)
        val target = ledger().moneyFlow(prev).destinations.first { it.categoryId != null }
        app()
        waitText("Saúde financeira")
        rule.onNodeWithContentDescription("Mês anterior").performClick()
        rule.waitForIdle()
        rule.onAllNodes(hasScrollToNodeAction()).onFirst().performScrollToNode(hasText("Para onde foi"))
        rule.onAllNodes(hasScrollToNodeAction()).onFirst().performScrollToNode(hasText(target.label))
        rule.waitForIdle()
        rule.onAllNodesWithText(target.label)[0].performClick()
        rule.waitForIdle()
        // O Extrato abriu com a etiqueta da categoria (o "x" tira o filtro) e no mês de antes.
        rule.waitUntil(10_000) { rule.onAllNodes(hasContentDescription("Remover filtro de categoria")).fetchSemanticsNodes().isNotEmpty() }
        waitText(Dates.monthYear(prev, day))
        assertTrue(rule.onAllNodesWithText(target.label).fetchSemanticsNodes().isNotEmpty())
    }

    /** RF-ANA-10 e RF-EXT-12: "Outras categorias" abre o Extrato do mês só nas saídas. */
    @Test fun outrasCategoriasAbreAsSaidasDoMes() {
        val others = ledger().moneyFlow(YearMonth.from(day)).destinations.firstOrNull { it.kind == FlowKind.OTHERS }
        assertTrue("os dados de exemplo têm mais de 6 categorias de saída", others != null)
        app()
        waitText("Saúde financeira")
        rule.onAllNodes(hasScrollToNodeAction()).onFirst().performScrollToNode(hasText(others!!.label))
        rule.waitForIdle()
        rule.onAllNodesWithText(others.label)[0].performClick()
        rule.waitForIdle()
        rule.waitUntil(10_000) { rule.onAllNodes(hasText("Saídas") and isSelected()).fetchSemanticsNodes().isNotEmpty() }
        waitText(Dates.monthYear(YearMonth.from(day), day))
    }

    /** RF-ANA-10 no funil: tocar na faixa da primeira saída (a maior categoria) também abre o Extrato filtrado. */
    @Test fun tocarNoFunilAbreOExtrato() {
        runBlocking { c.settings.setFlowView(1) }
        app(settings.copy(flowView = 1))
        val first = ledger().moneyFlow(YearMonth.from(day)).destinations.first()
        assertTrue("a primeira saída é uma categoria", first.kind == FlowKind.CATEGORY && first.categoryId != null)
        val funnel = SemanticsMatcher("funil") { n ->
            n.config.contains(SemanticsProperties.ContentDescription) && n.config[SemanticsProperties.ContentDescription].any { it.contains(first.label) && it.length > 40 }
        }
        rule.onAllNodes(hasScrollToNodeAction()).onFirst().performScrollToNode(funnel)
        rule.waitForIdle()
        // A faixa 0 é o dinheiro do mês; a 1 é a maior saída. Toque no meio dela, na parte do funil (à esquerda).
        val y = with(rule.density) { (46.dp + 5.dp + 6.dp + 23.dp).toPx() }
        rule.onNode(funnel).performTouchInput { click(androidx.compose.ui.geometry.Offset(width * 0.27f, y)) }
        rule.waitForIdle()
        rule.waitUntil(10_000) { rule.onAllNodes(hasContentDescription("Remover filtro de categoria")).fetchSemanticsNodes().isNotEmpty() }
    }

    /** ERR-068: com "Esconder valores", o funil não mostra valor nenhum, nem para o leitor de tela. */
    @Test fun funilSemValoresQuandoEscondidos() {
        val flow = ledger().moneyFlow(YearMonth.from(day))
        show(s = settings.copy(hideValues = true)) { FlowFunnel(flow, hide = true, onNode = {}) }
        val desc = rule.onAllNodes(hasContentDescription(flow.destinations.first().label, substring = true))[0]
            .fetchSemanticsNode().config[SemanticsProperties.ContentDescription].joinToString()
        assertTrue(desc.contains(Money.HIDDEN))
        (flow.sources + flow.destinations).forEach { n -> assertFalse("${n.label} apareceu com valor", desc.contains(Money.format(n.value))) }
    }

    /** RF-ANA-02 e DS-COR-06: o calendário de calor nas cores da temperatura, no claro e no escuro. */
    @Config(qualifiers = "+w393dp-h430dp-xxhdpi")
    @Test fun calendarioDeTemperatura() {
        val l = ledger()
        val ym = YearMonth.from(day).minusMonths(1)
        show { Box(Modifier.padding(16.dp)) { HeatmapCalendar(ym, l.dailyExpenses(ym), day, null, onSelect = {}) } }
        captureScreenRoboImage("screenshots/130_calendario_temperatura.png")
    }

    @Config(qualifiers = "+w393dp-h430dp-xxhdpi")
    @Test fun calendarioDeTemperaturaNoEscuro() {
        val l = ledger()
        val ym = YearMonth.from(day).minusMonths(1)
        show(dark = true) { Box(Modifier.padding(16.dp)) { HeatmapCalendar(ym, l.dailyExpenses(ym), day, null, onSelect = {}) } }
        captureScreenRoboImage("screenshots/131_calendario_temperatura_escuro.png")
    }

    /** RF-ANA-02 em árabe: a semana da direita para a esquerda, com as iniciais e os números da língua do app. */
    @Config(qualifiers = "+ar-rSA-ldrtl-w393dp-h430dp-xxhdpi")
    @Test fun calendarioDeTemperaturaEmArabe() {
        val l = ledger()
        val ym = YearMonth.from(day).minusMonths(1)
        show { Box(Modifier.padding(16.dp)) { HeatmapCalendar(ym, l.dailyExpenses(ym), day, null, onSelect = {}) } }
        captureScreenRoboImage("screenshots/132_calendario_temperatura_arabe.png")
    }

    /** ERR-067: em inglês, as iniciais da semana são as do inglês (antes: "D S T Q Q S S" em todas as línguas). */
    @Config(qualifiers = "+en-rUS-w393dp-h1800dp-xxhdpi")
    @Test fun calendarioComAsIniciaisDaLingua() {
        val l = ledger()
        val ym = YearMonth.from(day)
        show { HeatmapCalendar(ym, l.dailyExpenses(ym), day, null, onSelect = {}) }
        rule.onNodeWithText("W").assertExists()
        assertTrue(rule.onAllNodesWithText("Q").fetchSemanticsNodes().isEmpty())
    }
}
