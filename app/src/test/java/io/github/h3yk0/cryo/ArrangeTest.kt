// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureScreenRoboImage
import io.github.h3yk0.cryo.data.AppSettings
import io.github.h3yk0.cryo.data.Backup
import io.github.h3yk0.cryo.data.ThemeMode
import io.github.h3yk0.cryo.domain.Layouts
import io.github.h3yk0.cryo.domain.ScreenLayout
import io.github.h3yk0.cryo.ui.LocalNav
import io.github.h3yk0.cryo.ui.Navigator
import io.github.h3yk0.cryo.ui.components.LocalContainer
import io.github.h3yk0.cryo.ui.components.LocalFixedToday
import io.github.h3yk0.cryo.ui.components.LocalSettings
import io.github.h3yk0.cryo.ui.screens.HomeScreen
import io.github.h3yk0.cryo.ui.screens.InsightsScreen
import io.github.h3yk0.cryo.ui.theme.CryoTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate

/** RF-ORG-01..05 (1.7, LA8): organizar os blocos das telas. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], application = TestApp::class, qualifiers = "+w393dp-h2200dp-xxhdpi")
class ArrangeTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var c: AppContainer
    private val day = LocalDate.now()

    @Before fun setUp() {
        io.github.h3yk0.cryo.domain.engine.EngineRunner.inline = true
        c = AppContainer(ApplicationProvider.getApplicationContext())
        runBlocking {
            c.settings.clear()
            c.settings.setOnboardingDone(true)
            seed(c, day)
        }
        rule.waitUntil(10_000) { (c.repo.snapshot.value?.txs?.size ?: 0) > 50 }
    }

    /** A tela com os ajustes de verdade (a folha de organizar muda a tela na hora). */
    private fun show(content: @Composable () -> Unit) {
        rule.setContent {
            CryoTheme(mode = ThemeMode.LIGHT, dynamicColor = false) {
                val nav = rememberNavController()
                val navigator = remember { Navigator(nav) }
                // Como no app: a tela nasce com os ajustes já lidos (a arrumação vale desde a primeira composição).
                val first = remember { runBlocking { c.settings.current() } }
                val s by c.settings.flow.collectAsState(initial = first)
                CompositionLocalProvider(
                    LocalContainer provides c, LocalSettings provides s.copy(dynamicColor = false), LocalNav provides navigator, LocalFixedToday provides day,
                ) {
                    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) { content() }
                }
            }
        }
        rule.waitForIdle()
    }

    /** Espera rodando a fila da linha principal (é por ela que a gravação dos ajustes chega à tela). */
    private fun waitFor(ms: Long = 10_000, condition: () -> Boolean) = rule.waitUntil(ms) {
        org.robolectric.shadows.ShadowLooper.idleMainLooper()
        condition()
    }

    private fun waitText(t: String) = waitFor(15_000) { rule.onAllNodesWithText(t, substring = true).fetchSemanticsNodes().isNotEmpty() }
    private fun gone(t: String) = rule.onAllNodesWithText(t).fetchSemanticsNodes().isEmpty()
    private fun top(t: String) = rule.onAllNodesWithText(t)[0].fetchSemanticsNode().boundsInRoot.top
    private fun layout(screen: String) = runBlocking { c.settings.current().layout(screen) }

    /* ------------------------------ a regra ------------------------------ */

    @Test fun ordemDaPessoaComBlocosNovosNoLugarPadrao() {
        val defaults = listOf("a", "b", "c", "novo", "d")
        // A pessoa pôs "d" primeiro e "c" antes de "a"; "novo" nunca foi visto: entra logo depois de "c" (o de antes no padrão).
        val l = ScreenLayout(order = listOf("d", "c", "a", "b", "sumiu"))
        assertEquals(listOf("d", "c", "novo", "a", "b"), l.arrange(defaults))
        // Um bloco que não existe mais ("sumiu") não atrapalha; um bloco novo sem anterior vai para o começo.
        assertEquals(listOf("x", "d", "c", "a", "b"), l.arrange(listOf("x", "a", "b", "c", "d")))
        // Sem arrumação, a ordem padrão.
        assertEquals(defaults, ScreenLayout().arrange(defaults))
    }

    @Test fun moverEsconderENuncaSumirTudo() {
        val defaults = listOf("a", "b", "c")
        val moved = ScreenLayout().moved(defaults, 0, 2)
        assertEquals(listOf("b", "c", "a"), moved.arrange(defaults))
        val hidden = moved.toggled(defaults, "b")!!
        assertEquals(listOf("c", "a"), hidden.visible(defaults))
        assertEquals(listOf("b", "c", "a"), hidden.toggled(defaults, "b")!!.visible(defaults))
        // RF-ORG-05: esconder o último à vista não vale.
        val onlyA = ScreenLayout(hidden = setOf("b", "c"))
        assertNull(onlyA.toggled(defaults, "a"))
        // E, se mesmo assim estiver tudo escondido (um backup mexido), aparece o primeiro.
        assertEquals(listOf("a"), ScreenLayout(hidden = setOf("a", "b", "c")).visible(defaults))
    }

    @Test fun textoGuardadoIdaEVolta() {
        val l = ScreenLayout(order = listOf("saude", "fluxo", "fim"), hidden = setOf("fluxo", "ano"))
        val back = ScreenLayout.decode(l.encode())
        assertEquals(l.order, back.order.filter { it in l.order })
        assertEquals(l.hidden, back.hidden)
        // Lixo não vira arrumação: chaves estranhas saem, texto enorme é ignorado.
        assertEquals(listOf("ok"), ScreenLayout.decode("ok,<script>,Assim Não,-").order)
        assertTrue(ScreenLayout.decode("a,".repeat(2_000)).isDefault)
        assertEquals(mapOf("home" to "a"), Layouts.clean(mapOf("home" to "a", "tela_que_nao_existe" to "b")))
    }

    /* ------------------------------ as telas ------------------------------ */

    /** RF-ORG-02: a ordem guardada vale na tela; o escondido some; o seletor do mês fica antes do primeiro bloco do mês. */
    @Test fun analisesNaOrdemDaPessoa() {
        runBlocking { c.settings.setLayout(Layouts.INSIGHTS, ScreenLayout(order = listOf("fluxo", "saude", "fim"), hidden = setOf("calendario"))) }
        show { InsightsScreen() }
        waitText("Fluxo do dinheiro")
        waitText("Saúde financeira")
        assertTrue("o fluxo veio para o alto", top("Fluxo do dinheiro") < top("Saúde financeira"))
        assertTrue(top("Saúde financeira") < top("Previsão do fim do mês"))
        rule.onAllNodes(hasScrollToNodeAction()).onFirst().performScrollToNode(hasText("Organizar esta tela"))
        assertTrue("o calendário escondido não aparece", gone("Calendário de calor"))
    }

    /** RF-ORG-02 e RF-ORG-04: a folha esconde, mostra e volta ao padrão, e a escolha fica guardada. */
    @Test fun folhaEscondeMoveEVoltaAoPadrao() {
        show { InsightsScreen() }
        rule.onAllNodes(hasScrollToNodeAction()).onFirst().performScrollToNode(hasText("Organizar esta tela"))
        rule.onNodeWithText("Organizar esta tela").performClick()
        waitText("Voltar ao padrão")
        captureScreenRoboImage("screenshots/134_organizar_analises.png")
        // O olho do calendário: esconde.
        rule.onNode(hasContentDescription("Esconder Calendário de calor")).performClick()
        waitFor(5_000) { "calendario" in layout(Layouts.INSIGHTS).hidden }
        // O leitor de tela: "Descer" na saúde financeira (a primeira) a leva para o segundo lugar.
        val row = rule.onAllNodes(hasText("Saúde financeira") and androidx.compose.ui.test.SemanticsMatcher.keyIsDefined(SemanticsActions.CustomActions))[0]
        val actions = row.fetchSemanticsNode().config[SemanticsActions.CustomActions]
        actions.first { it.label == "Descer" }.action()
        waitFor(5_000) { layout(Layouts.INSIGHTS).arrange(listOf("saude", "fim", "seis", "ano", "fluxo", "categorias", "calendario"))[1] == "saude" }
        // Voltar ao padrão desfaz tudo.
        rule.onNodeWithText("Voltar ao padrão").performClick()
        waitFor(5_000) { layout(Layouts.INSIGHTS).isDefault }
    }

    /** RF-ORG-03: os blocos da grade do Início também mudam de lugar e somem um a um. */
    @Test fun blocosDoInicio() {
        runBlocking {
            c.settings.setLayout(Layouts.HOME_TILES, ScreenLayout(order = listOf("metas", "contas"), hidden = setOf("investimentos")))
            // E as partes da tela: as últimas movimentações para o alto.
            // (o app sempre guarda a ordem inteira: ver ScreenLayout.moved)
            c.settings.setLayout(Layouts.HOME, ScreenLayout(order = listOf("recentes", "atalhos", "frase", "mes", "blocos")))
        }
        show { HomeScreen() }
        waitText("Saldo em contas")
        waitText("Metas")
        assertTrue("as metas vêm antes das contas", top("Metas") < top("Contas"))
        assertTrue(gone("Investimentos"))
        assertTrue("as últimas movimentações subiram", top("Últimas movimentações") < top("Metas"))
    }

    /** RF-ORG-04: a arrumação vai no backup e volta com ele; um backup antigo (sem ela) não muda nada. */
    @Test fun arrumacaoVaiNoBackup() {
        val snap = c.repo.snapshot.value!!
        val layouts = mapOf(Layouts.INSIGHTS to "fluxo,-calendario", Layouts.HOME_TILES to "metas")
        val json = Backup.export(snap, exportedAt = 1L, layouts = layouts)
        val back = Backup.import(json)
        assertEquals(layouts, back.layouts)
        assertEquals(snap.txs.size, back.snapshot.txs.size)
        assertTrue(Backup.import(Backup.export(snap, exportedAt = 1L)).layouts.isEmpty())
        // Restaurar aplica a arrumação.
        runBlocking { c.settings.setLayouts(back.layouts) }
        assertEquals(listOf("fluxo"), layout(Layouts.INSIGHTS).order.take(1))
        assertTrue("calendario" in layout(Layouts.INSIGHTS).hidden)
        assertFalse(layout(Layouts.WALLET).hidden.isNotEmpty())
    }
}
