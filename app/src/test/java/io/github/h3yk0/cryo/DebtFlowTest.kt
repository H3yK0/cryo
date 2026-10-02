// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.hasSetTextAction
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureScreenRoboImage
import io.github.h3yk0.cryo.data.AppSettings
import io.github.h3yk0.cryo.data.ThemeMode
import io.github.h3yk0.cryo.data.db.TxType
import io.github.h3yk0.cryo.domain.Ledger
import io.github.h3yk0.cryo.domain.Money
import io.github.h3yk0.cryo.ui.LocalNav
import io.github.h3yk0.cryo.ui.Navigator
import io.github.h3yk0.cryo.ui.components.LocalContainer
import io.github.h3yk0.cryo.ui.components.LocalFixedToday
import io.github.h3yk0.cryo.ui.components.LocalSettings
import io.github.h3yk0.cryo.ui.screens.DebtsScreen
import io.github.h3yk0.cryo.ui.screens.GoalEditorScreen
import io.github.h3yk0.cryo.ui.screens.GoalsArchiveScreen
import io.github.h3yk0.cryo.ui.screens.WalletArchiveScreen
import io.github.h3yk0.cryo.ui.theme.CryoTheme
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
import java.time.LocalDate

/** Usa as telas novas como uma pessoa usaria: pagar parcela, arquivar e restaurar. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], application = TestApp::class, qualifiers = "w393dp-h852dp-xxhdpi")
class DebtFlowTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var c: AppContainer
    private val day = LocalDate.of(2026, 10, 2)
    private val settings = AppSettings(onboardingDone = true, defaultAccountId = 1, dynamicColor = false)

    @Before fun setUp() {
        c = AppContainer(ApplicationProvider.getApplicationContext())
        runBlocking { seed(c, day) }
        rule.waitUntil(10_000) { (c.repo.snapshot.value?.debts?.size ?: 0) >= 4 && (c.repo.snapshot.value?.txs?.size ?: 0) > 50 }
    }

    private fun show(content: @Composable () -> Unit) {
        rule.setContent {
            CryoTheme(mode = ThemeMode.LIGHT, dynamicColor = false) {
                val nav = rememberNavController()
                val navigator = remember { Navigator(nav) }
                CompositionLocalProvider(
                    LocalContainer provides c, LocalSettings provides settings,
                    LocalNav provides navigator, LocalFixedToday provides day,
                ) {
                    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) { content() }
                }
            }
        }
        rule.waitForIdle()
    }

    private fun waitText(t: String) =
        rule.waitUntil(8_000) { rule.onAllNodesWithText(t, substring = true).fetchSemanticsNodes().isNotEmpty() }

    private fun ledger() = Ledger(runBlocking { c.repo.loadOnce() }, day)

    @Test fun pagarParcelaComJuros() {
        val loan = ledger().s.debts.first { it.name == "Empréstimo pessoal" }
        val before = ledger().debtInfo(loan)
        show { DebtsScreen() }
        // o empréstimo vence em 3 dias: o botão "Paguei" abre a folha de pagamento
        rule.onAllNodesWithText("Paguei").onFirst().performClick()
        waitText("Parcela ${before.installmentsPaid + 1} de ${before.installmentsTotal}")
        rule.onNode(hasSetTextAction() and hasContentDescription("Valor pago", substring = true)).performTextReplacement("19500")
        waitText("A diferença de ${Money.format(1_500)} foi:")
        captureScreenRoboImage("screenshots/27_pagar_parcela.png")
        rule.onNodeWithText("Registrar pagamento").performScrollTo().performClick()
        rule.waitUntil(8_000) { ledger().debtInfo(loan).installmentsPaid == before.installmentsPaid + 1 }

        val after = ledger()
        assertEquals(before.outstanding - 18_000, after.debtInfo(loan).outstanding)
        assertTrue(after.s.txs.any { it.type == TxType.EXPENSE && it.amount == 1_500L && it.description == "Juros/multa · Empréstimo pessoal" })
    }

    @Test fun arquivarERestaurarMeta() {
        val goal = ledger().activeGoals.first { it.name == "Viagem para a praia" }
        var archive by mutableStateOf(false)
        show { if (archive) GoalsArchiveScreen() else GoalEditorScreen(goal.id) }
        rule.onNodeWithText("Arquivar meta").performScrollTo().performClick()
        rule.waitUntil(8_000) { ledger().goal[goal.id]!!.archived }

        rule.runOnIdle { archive = true }
        waitText("Viagem para a praia")
        captureScreenRoboImage("screenshots/28_arquivo_de_metas.png")
        rule.onAllNodes(hasText("Restaurar") and hasClickAction()).onFirst().performClick()
        rule.waitUntil(8_000) { ledger().s.goals.count { it.archived } == 2 }
        // a meta restaurada era a primeira em ordem alfabética entre as arquivadas
        assertFalse(ledger().goal.values.first { it.name == "Notebook novo" }.archived)
    }

    @Test fun restaurarContaArquivada() {
        show { WalletArchiveScreen() }
        waitText("Conta antiga")
        rule.onAllNodes(hasText("Restaurar") and hasClickAction()).onFirst().performClick()
        rule.waitUntil(8_000) { ledger().s.accounts.none { it.archived } }
    }
}
