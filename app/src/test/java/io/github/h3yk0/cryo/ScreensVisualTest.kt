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
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureScreenRoboImage
import io.github.h3yk0.cryo.data.AppSettings
import io.github.h3yk0.cryo.data.ThemeMode
import io.github.h3yk0.cryo.data.db.Bill
import io.github.h3yk0.cryo.domain.GeneralBudget
import io.github.h3yk0.cryo.domain.Inbox
import io.github.h3yk0.cryo.domain.Ledger
import io.github.h3yk0.cryo.domain.predict.MonthEnds
import io.github.h3yk0.cryo.ui.LocalNav
import io.github.h3yk0.cryo.ui.Navigator
import io.github.h3yk0.cryo.ui.components.LocalContainer
import io.github.h3yk0.cryo.ui.components.LocalFixedToday
import io.github.h3yk0.cryo.ui.components.LocalSettings
import io.github.h3yk0.cryo.ui.screens.HomeScreen
import io.github.h3yk0.cryo.ui.screens.InboxScreen
import io.github.h3yk0.cryo.ui.screens.InsightsScreen
import io.github.h3yk0.cryo.ui.screens.PlanScreen
import io.github.h3yk0.cryo.ui.screens.SettingsScreen
import io.github.h3yk0.cryo.ui.screens.VoiceSettingsScreen
import io.github.h3yk0.cryo.ui.theme.CryoTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
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
 * Imagens 115–124 (reforma visual da 1.6, lotes LA9 a LA13): o Início com o sino, o destaque e os blocos (RF-INI-19/20/21),
 * a central de avisos, Análises resumida (RF-ANA-07), o Planejar em blocos com o Geral (RF-ORC-10/11, RF-FIX-13, RF-MET-07),
 * os Ajustes por tipo (RF-AJU-13) e Ajustes › Voz (RF-VOZ-16).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], application = TestApp::class, qualifiers = "+w393dp-h852dp-xxhdpi")
class ScreensVisualTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var c: AppContainer
    private val day = LocalDate.now()
    private val settings = AppSettings(onboardingDone = true, defaultAccountId = 1, dynamicColor = false, userName = "Marina")

    @Before fun setUp() {
        io.github.h3yk0.cryo.domain.engine.EngineRunner.inline = true
        c = AppContainer(ApplicationProvider.getApplicationContext())
        c.inbox.clear()
        runBlocking { seed(c, day) }
        rule.waitUntil(10_000) { (c.repo.snapshot.value?.txs?.size ?: 0) > 50 }
    }

    private fun show(dark: Boolean = false, content: @Composable () -> Unit) {
        rule.setContent {
            CryoTheme(mode = if (dark) ThemeMode.DARK else ThemeMode.LIGHT, dynamicColor = false) {
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
    private fun gone(t: String) = rule.onAllNodesWithText(t, substring = true).fetchSemanticsNodes().isEmpty()

    /** Uma conta fixa vencida (alerta) para o sino e o destaque terem o que mostrar. */
    private fun overdueBill(): String {
        val name = "Condomínio"
        val due = day.minusDays(2).takeIf { it.monthValue == day.monthValue } ?: day
        runBlocking {
            c.repo.saveBill(Bill(name = name, amount = 45_000, dueDay = due.dayOfMonth, startYm = day.year * 100 + day.monthValue, accountId = c.repo.snapshot.value!!.accounts.first().id))
        }
        rule.waitUntil(10_000) { c.repo.snapshot.value?.bills?.any { it.name == name } == true }
        return name
    }

    private fun ledger() = Ledger(c.repo.snapshot.value!!, day)

    /** RF-INI-19/20/21: o sino com o número, um destaque só e os blocos em duas colunas. */
    @Config(qualifiers = "+w393dp-h1700dp-xxhdpi")
    @Test fun inicioEmBlocos() {
        val bill = overdueBill()
        show { HomeScreen() }
        waitText("Saldo em contas")
        rule.waitUntil(10_000) { rule.onAllNodes(hasContentDescription("Abrir os avisos", substring = true)).fetchSemanticsNodes().isNotEmpty() }
        // A lista de avisos e "O Cryo percebeu" saíram do meio do Início (DS-PSI-06).
        assertTrue(gone("O Cryo percebeu"))
        waitText(bill)
        waitText("Orçamentos")
        captureScreenRoboImage("screenshots/115_inicio_blocos.png")
        // "×" dá o destaque por visto: ele some, nenhum outro entra no lugar nesta visita e o sino guarda o resto.
        rule.onNode(hasContentDescription("Dispensar")).performClick()
        rule.waitForIdle()
        assertTrue(gone("venceu"))
        assertTrue(rule.onAllNodes(hasContentDescription("Dispensar")).fetchSemanticsNodes().isEmpty())
        val l = ledger()
        val items = Inbox.items(l, null, MonthEnds.of(l), hide = false)
        assertTrue(c.inbox.seen()(items.first { it.text.contains(bill) }.key))
    }

    @Test fun inicioNoEscuro() {
        overdueBill()
        show(dark = true) { HomeScreen() }
        waitText("Saldo em contas")
        captureScreenRoboImage("screenshots/116_inicio_escuro.png")
    }

    /** TELA-CENTRAL: os grupos na cor da prioridade; abrir dá tudo por visto (o sino zera). */
    @Config(qualifiers = "+w393dp-h1300dp-xxhdpi")
    @Test fun centralDeAvisos() {
        val bill = overdueBill()
        show { InboxScreen() }
        waitText("Precisa de atenção")
        waitText(bill)
        captureScreenRoboImage("screenshots/117_central_de_avisos.png")
        val l = ledger()
        val items = Inbox.items(l, null, MonthEnds.of(l), hide = false)
        rule.waitUntil(5_000) { Inbox.unseenImportant(items, c.inbox.seen()) == 0 }
        assertEquals(0, Inbox.unseenImportant(items, c.inbox.seen()))
    }

    /** RF-ANA-07: o fim do mês real, os 6 meses logo abaixo, o ano, a saúde, o fluxo em blocos, as categorias e o calendário. */
    @Config(qualifiers = "+w393dp-h3400dp-xxhdpi")
    @Test fun analisesResumida() {
        show { InsightsScreen() }
        waitText("Previsão do fim do mês")
        waitText("Últimos 6 meses")
        waitText("De onde veio")
        assertTrue(gone("Previsão de metas"))
        assertTrue(gone("Com o dia a dia"))
        captureScreenRoboImage("screenshots/118_analises.png")
    }

    /** RF-ORC-10/11: o Geral no topo, os botões-bloco e as categorias em blocos; a folha do Geral com a sugestão. */
    @Config(qualifiers = "+w393dp-h1700dp-xxhdpi")
    @Test fun orcamentosComOGeral() {
        show { PlanScreen(0) }
        waitText("Geral")
        waitText("Dividido entre as categorias")
        waitText("Por categoria")
        captureScreenRoboImage("screenshots/119_orcamentos_geral.png")
        rule.onNodeWithText("Geral").performClick()
        waitText("Orçamento Geral")
        captureScreenRoboImage("screenshots/120_orcamento_geral_folha.png")
    }

    /** Sem o Geral, o convite com a sugestão pela renda fixa (RF-ORC-10, PRI-09). */
    @Test fun semOGeralOConvite() {
        runBlocking { c.repo.deleteBudget(GeneralBudget.ID) }
        rule.waitUntil(10_000) { c.repo.snapshot.value?.budgets?.none { it.categoryId == GeneralBudget.ID } == true }
        show { PlanScreen(0) }
        waitText("Definir o Geral")
    }

    /** RF-FIX-13: os três blocos, as que faltam primeiro e as pagas fechadas com setinha; "O que se repete" no fim. */
    @Config(qualifiers = "+w393dp-h1700dp-xxhdpi")
    @Test fun contasFixasLimpas() {
        show { PlanScreen(1) }
        waitText("Total do mês")
        waitText("O que se repete")
        captureScreenRoboImage("screenshots/121_contas_fixas.png")
    }

    /** RF-MET-07: o total guardado e cada meta num bloco; tocar abre a folha com as quatro ações. */
    @Config(qualifiers = "+w393dp-h1300dp-xxhdpi")
    @Test fun metasEmBlocos() {
        show { PlanScreen(2) }
        waitText("Guardado em metas")
        captureScreenRoboImage("screenshots/122_metas.png")
        val goal = c.repo.snapshot.value!!.goals.first { !it.archived }
        rule.onAllNodesWithText(goal.name)[0].performClick()
        waitText("Retirar")
        waitText("Arquivar")
        captureScreenRoboImage("screenshots/123_meta_folha.png")
    }

    /** RF-AJU-13 e RF-VOZ-16: Ajustes por tipo, a voz numa linha só e a tela dela. */
    @Config(qualifiers = "+w393dp-h2600dp-xxhdpi")
    @Test fun ajustesPorTipo() {
        show { SettingsScreen() }
        waitText("Lançamentos")
        waitText("Reconhecimento de voz")
        assertTrue(gone("Whisper, dentro do Cryo"))
        captureScreenRoboImage("screenshots/124_ajustes.png")
    }

    @Test fun ajustesDaVoz() {
        show { VoiceSettingsScreen() }
        waitText("Reconhecimento de voz")
        captureScreenRoboImage("screenshots/125_ajustes_voz.png")
    }
}
