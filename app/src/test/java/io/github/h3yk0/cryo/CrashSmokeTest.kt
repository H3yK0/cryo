// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import io.github.h3yk0.cryo.data.AppSettings
import io.github.h3yk0.cryo.data.ThemeMode
import io.github.h3yk0.cryo.data.db.CategoryKind
import io.github.h3yk0.cryo.data.db.TxType
import io.github.h3yk0.cryo.ui.LocalNav
import io.github.h3yk0.cryo.ui.Navigator
import io.github.h3yk0.cryo.ui.components.LocalContainer
import io.github.h3yk0.cryo.ui.components.LocalFixedToday
import io.github.h3yk0.cryo.ui.components.LocalSettings
import io.github.h3yk0.cryo.ui.screens.AccountDetailScreen
import io.github.h3yk0.cryo.ui.screens.AccountEditorScreen
import io.github.h3yk0.cryo.ui.screens.AlertSettingsScreen
import io.github.h3yk0.cryo.ui.screens.AutoBackupScreen
import io.github.h3yk0.cryo.ui.screens.BillEditorScreen
import io.github.h3yk0.cryo.ui.screens.BudgetIncomeScreen
import io.github.h3yk0.cryo.ui.screens.BudgetMonthScreen
import io.github.h3yk0.cryo.ui.screens.CardDetailScreen
import io.github.h3yk0.cryo.ui.screens.CardEditorScreen
import io.github.h3yk0.cryo.ui.screens.CategoriesScreen
import io.github.h3yk0.cryo.ui.screens.CategoryEditorScreen
import io.github.h3yk0.cryo.ui.screens.DebtDetailScreen
import io.github.h3yk0.cryo.ui.screens.DebtEditorScreen
import io.github.h3yk0.cryo.ui.screens.DebtPlanScreen
import io.github.h3yk0.cryo.ui.screens.DebtsScreen
import io.github.h3yk0.cryo.ui.screens.DevScreen
import io.github.h3yk0.cryo.ui.screens.GoalEditorScreen
import io.github.h3yk0.cryo.ui.screens.GoalsArchiveScreen
import io.github.h3yk0.cryo.ui.screens.HealthScreen
import io.github.h3yk0.cryo.ui.screens.HomeScreen
import io.github.h3yk0.cryo.ui.screens.ImportReportScreen
import io.github.h3yk0.cryo.ui.screens.ImportScreen
import io.github.h3yk0.cryo.ui.screens.ImportsScreen
import io.github.h3yk0.cryo.ui.screens.InboxScreen
import io.github.h3yk0.cryo.ui.screens.InsightsScreen
import io.github.h3yk0.cryo.ui.screens.InvestmentDetailScreen
import io.github.h3yk0.cryo.ui.screens.InvestmentEditorScreen
import io.github.h3yk0.cryo.ui.screens.LearnScreen
import io.github.h3yk0.cryo.ui.screens.PlanScreen
import io.github.h3yk0.cryo.ui.screens.ProfileScreen
import io.github.h3yk0.cryo.ui.screens.RecurringScreen
import io.github.h3yk0.cryo.ui.screens.SettingsScreen
import io.github.h3yk0.cryo.ui.screens.TourScreen
import io.github.h3yk0.cryo.ui.screens.TransactionsScreen
import io.github.h3yk0.cryo.ui.screens.VoiceSettingsScreen
import io.github.h3yk0.cryo.ui.screens.WalletArchiveScreen
import io.github.h3yk0.cryo.ui.screens.WalletScreen
import io.github.h3yk0.cryo.ui.theme.CryoTheme
import io.github.h3yk0.cryo.ui.tx.TxEditorScreen
import io.github.h3yk0.cryo.domain.Layouts
import io.github.h3yk0.cryo.domain.WidgetConfig
import io.github.h3yk0.cryo.ui.components.ArrangeGroup
import io.github.h3yk0.cryo.ui.components.ArrangeSheet
import io.github.h3yk0.cryo.ui.widget.WidgetConfigScreen
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * RNF-EST-01 (1.7, LA1 da 1.7): nenhuma tela fecha nem trava. Cada teste abre **todas** as telas do app, uma de cada vez,
 * numa situação difícil (sem dado nenhum, com os dados de exemplo, com os valores escondidos, no escuro, em árabe, com a
 * fonte em 200%), rola até o fim (o que está fora da tela só é desenhado ao rolar) e falha dizendo qual tela quebrou.
 * As telas de detalhe também abrem com um código que não existe (o aviso de uma conta já apagada, por exemplo).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], application = TestApp::class, qualifiers = "+w393dp-h852dp-xxhdpi")
class CrashSmokeTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var c: AppContainer
    private val day = LocalDate.now()
    private val base = AppSettings(onboardingDone = true, dynamicColor = false, userName = "Marina")

    @Before fun setUp() {
        io.github.h3yk0.cryo.domain.engine.EngineRunner.inline = true
        c = AppContainer(ApplicationProvider.getApplicationContext())
    }

    private fun seed() {
        runBlocking { seed(c, day) }
        rule.waitUntil(10_000) { (c.repo.snapshot.value?.txs?.size ?: 0) > 50 }
    }

    /** As telas do catálogo (TELA-*), com os códigos que existem nos dados (e um que não existe). */
    private fun screens(): List<Pair<String, @Composable () -> Unit>> {
        rule.waitUntil(10_000) { c.repo.snapshot.value != null }
        val s = c.repo.snapshot.value!!
        val missing = 987_654L
        val account = s.accounts.firstOrNull()?.id ?: missing
        val card = s.cards.firstOrNull()?.id ?: missing
        val inv = s.investments.firstOrNull()?.id ?: missing
        val debt = s.debts.firstOrNull()?.id ?: missing
        val goal = s.goals.firstOrNull()?.id ?: missing
        val bill = s.bills.firstOrNull()?.id ?: missing
        val tx = s.txs.firstOrNull()?.id ?: missing
        val cat = s.categories.firstOrNull()?.id ?: missing
        return listOf(
            "inicio" to { HomeScreen() },
            "central de avisos" to { InboxScreen() },
            "extrato" to { TransactionsScreen(null) },
            "extrato por categoria" to { TransactionsScreen(cat) },
            "orçamentos" to { PlanScreen(0) },
            "contas fixas" to { PlanScreen(1) },
            "metas" to { PlanScreen(2) },
            "análises" to { InsightsScreen() },
            "carteira" to { WalletScreen() },
            "arquivados da carteira" to { WalletArchiveScreen() },
            "dívidas" to { DebtsScreen() },
            "plano para quitar" to { DebtPlanScreen() },
            "saúde financeira" to { HealthScreen() },
            "o que se repete" to { RecurringScreen() },
            "painel do mês" to { BudgetMonthScreen() },
            "orçamento pela renda" to { BudgetIncomeScreen() },
            "metas arquivadas" to { GoalsArchiveScreen() },
            "ajustes" to { SettingsScreen() },
            "ajustes da voz" to { VoiceSettingsScreen() },
            "avisos e sons" to { AlertSettingsScreen() },
            "backup automático" to { AutoBackupScreen() },
            "categorias" to { CategoriesScreen() },
            "perfil" to { ProfileScreen() },
            "aprender o cryo" to { LearnScreen() },
            "importações feitas" to { ImportsScreen() },
            "importar extrato" to { ImportScreen() },
            "relatório da importação" to { ImportReportScreen() },
            "modo desenvolvedor" to { DevScreen() },
            "passeio" to { TourScreen() },
            "novo lançamento" to { TxEditorScreen(txId = null, initialType = TxType.EXPENSE, presetAccountId = null, presetCardId = null, onDone = {}) },
            "nova receita" to { TxEditorScreen(txId = null, initialType = TxType.INCOME, presetAccountId = null, presetCardId = null, onDone = {}) },
            "nova transferência" to { TxEditorScreen(txId = null, initialType = TxType.TRANSFER, presetAccountId = null, presetCardId = null, onDone = {}) },
            "editar lançamento" to { TxEditorScreen(txId = tx, initialType = TxType.EXPENSE, presetAccountId = null, presetCardId = null, onDone = {}) },
            "conta" to { AccountDetailScreen(account) },
            "editar conta" to { AccountEditorScreen(account) },
            "nova conta" to { AccountEditorScreen(null) },
            "cartão" to { CardDetailScreen(card) },
            "editar cartão" to { CardEditorScreen(card) },
            "investimento" to { InvestmentDetailScreen(inv) },
            "editar investimento" to { InvestmentEditorScreen(inv) },
            "dívida" to { DebtDetailScreen(debt) },
            "editar dívida" to { DebtEditorScreen(debt) },
            "nova dívida" to { DebtEditorScreen(null) },
            "editar meta" to { GoalEditorScreen(goal) },
            "nova meta" to { GoalEditorScreen(null) },
            "editar conta fixa" to { BillEditorScreen(bill, CategoryKind.EXPENSE) },
            "nova conta a receber" to { BillEditorScreen(null, CategoryKind.INCOME) },
            "editar categoria" to { CategoryEditorScreen(cat, CategoryKind.EXPENSE) },
            "nova categoria de entrada" to { CategoryEditorScreen(null, CategoryKind.INCOME) },
            "conta que não existe" to { AccountDetailScreen(missing) },
            "cartão que não existe" to { CardDetailScreen(missing) },
            "dívida que não existe" to { DebtDetailScreen(missing) },
            "investimento que não existe" to { InvestmentDetailScreen(missing) },
            "lançamento que não existe" to { TxEditorScreen(txId = missing, initialType = TxType.EXPENSE, presetAccountId = null, presetCardId = null, onDone = {}) },
            // 1.7 (DLA de 23/11/2026): o bloqueio por tempo e o modo privacidade só aparecem com o bloqueio ligado.
            "ajustes com o bloqueio" to {
                CompositionLocalProvider(LocalSettings provides LocalSettings.current.copy(lockEnabled = true, lockAfter = 0, privacyMode = true)) { SettingsScreen() }
            },
            // RF-ORG: as telas com blocos escondidos e fora de ordem, e a folha de organizar.
            "telas com blocos escondidos" to {
                val hidden = mapOf(
                    Layouts.HOME to "-mes,recentes,atalhos,frase,blocos", Layouts.HOME_TILES to "-contas,metas",
                    Layouts.INSIGHTS to "calendario,-saude,fim", Layouts.WALLET to "-patrimonio,cartoes",
                    Layouts.BUDGETS to "-geral,categorias", Layouts.BILLS to "-pagar,repete", Layouts.GOALS to "-total,metas",
                )
                CompositionLocalProvider(LocalSettings provides LocalSettings.current.copy(layouts = hidden)) {
                    androidx.compose.foundation.layout.Column {
                        androidx.compose.foundation.layout.Box(Modifier.weight(1f)) { HomeScreen() }
                        androidx.compose.foundation.layout.Box(Modifier.weight(1f)) { InsightsScreen() }
                    }
                }
            },
            "organizar esta tela" to {
                ArrangeSheet(
                    listOf(
                        ArrangeGroup(Layouts.HOME, "Partes", listOf("atalhos" to "Atalhos", "frase" to "Frase", "mes" to "O mês", "blocos" to "Blocos", "recentes" to "Recentes")),
                        ArrangeGroup(Layouts.HOME_TILES, "Blocos", listOf("contas" to "Contas", "orcamentos" to "Orçamentos", "metas" to "Metas")),
                    ),
                    onDismiss = {},
                )
            },
            // RF-WID-02: a tela de editar o widget (uma Activity própria, sem dados).
            "editar o widget" to { WidgetConfigScreen(WidgetConfig()) {} },
        )
    }

    private fun openAll(settings: AppSettings, dark: Boolean = false) {
        val list = screens()
        val index = mutableIntStateOf(0)
        rule.setContent {
            CryoTheme(mode = if (dark) ThemeMode.DARK else ThemeMode.LIGHT, dynamicColor = false) {
                val nav = rememberNavController()
                val navigator = remember { Navigator(nav) }
                CompositionLocalProvider(LocalContainer provides c, LocalSettings provides settings, LocalNav provides navigator, LocalFixedToday provides day) {
                    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) { list[index.intValue].second() }
                }
            }
        }
        for (i in list.indices) {
            val name = list[i].first
            try {
                rule.runOnUiThread { index.intValue = i }
                rule.waitForIdle()
                // O que fica fora da tela só é desenhado ao rolar: rola cada lista até o fim. Rolar a primeira pode tirar a
                // segunda da tela (a fileira de atalhos do Início): por isso a lista é buscada de novo a cada vez.
                for (k in 0 until 3) {
                    if (k >= rule.onAllNodes(hasScrollAction()).fetchSemanticsNodes().size) break
                    try {
                        rule.onAllNodes(hasScrollAction())[k].performTouchInput { repeat(6) { swipeUp() } }
                    } catch (e: AssertionError) {
                        // Só a lista que saiu da tela no meio do caminho; qualquer erro do app passa adiante.
                        if (e.message?.startsWith("Failed to inject touch input") != true) throw e
                    }
                    rule.waitForIdle()
                }
            } catch (e: Throwable) {
                throw AssertionError("A tela \"$name\" quebrou: ${e::class.simpleName}: ${e.message}", e)
            }
        }
    }

    @Test fun semDadoNenhum() = openAll(base)

    @Test fun comOsDadosDeExemplo() { seed(); openAll(base.copy(defaultAccountId = 1)) }

    @Test fun comOsValoresEscondidos() { seed(); openAll(base.copy(hideValues = true)) }

    @Test fun noEscuro() { seed(); openAll(base, dark = true) }

    @Config(qualifiers = "+ar-rSA-ldrtl")
    @Test fun emArabe() { seed(); openAll(base) }

    @Config(fontScale = 2.0f)
    @Test fun comAFonteGrande() { seed(); openAll(base) }
}
