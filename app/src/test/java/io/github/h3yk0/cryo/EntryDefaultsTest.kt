// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo

import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import io.github.h3yk0.cryo.data.SettingsRepository
import io.github.h3yk0.cryo.data.db.TxType
import io.github.h3yk0.cryo.ui.components.DateChooser
import io.github.h3yk0.cryo.ui.theme.CryoTheme
import io.github.h3yk0.cryo.ui.tx.EntryDefaults
import io.github.h3yk0.cryo.ui.components.Source
import io.github.h3yk0.cryo.ui.tx.TxDraft
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/**
 * RF-LAN-10 e RF-CAR-12 (1.6, LA4): o último dia usado fica guardado e é o começo do lançamento novo pela tela da conta;
 * a compra nova pela tela do cartão entra na fatura que estava na tela.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = TestApp::class)
class EntryDefaultsTest {
    @get:Rule val rule = createComposeRule()
    private val today = LocalDate.of(2026, 11, 20)
    private val nov5 = LocalDate.of(2026, 11, 5)

    @Test fun salvarGuardaODia() = runBlocking {
        val settings = SettingsRepository(ApplicationProvider.getApplicationContext())
        settings.setLastTxDate(nov5)
        assertEquals(nov5, settings.flow.first().lastTxDate)
    }

    @Test fun pelaContaComecaNoUltimoDia() {
        assertEquals(nov5, EntryDefaults.startDate(fromAccountScreen = true, lastTxDate = nov5, today = today))
        // Pelo resto do app, hoje; sem dia guardado, hoje.
        assertEquals(today, EntryDefaults.startDate(fromAccountScreen = false, lastTxDate = nov5, today = today))
        assertEquals(today, EntryDefaults.startDate(fromAccountScreen = true, lastTxDate = null, today = today))
    }

    @Test fun compraNovaEntraNaFaturaDaTela() {
        assertEquals(202612, EntryDefaults.invoice(fromCardScreen = true, viewedInvoice = 202612))
        assertNull(EntryDefaults.invoice(fromCardScreen = false, viewedInvoice = 202612))
        val d = TxDraft(type = TxType.EXPENSE, date = today, source = Source.Card(1), invoiceYm = EntryDefaults.invoice(true, 202612))
        assertEquals(202612, d.invoiceYm)
        // Mudar a data volta a seguir a data (RF-CAR-10).
        d.date = today.minusDays(3)
        assertNull(d.invoiceYm)
    }

    @Test fun botaoDoUltimoDia() {
        var picked: LocalDate? = null
        rule.setContent { CryoTheme { DateChooser(today, today, onChange = { picked = it }, last = nov5) } }
        rule.onNodeWithText("Último: 05/11").performClick()
        assertEquals(nov5, picked)
    }

    @Test fun ontemNaoViraBotaoRepetido() {
        rule.setContent { CryoTheme { DateChooser(today, today, onChange = {}, last = today.minusDays(1)) } }
        rule.onNodeWithText("Hoje").assertIsSelected()
        assertEquals(0, rule.onAllNodesWithTextCount("Último:"))
    }

    private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.onAllNodesWithTextCount(prefix: String): Int =
        onAllNodes(androidx.compose.ui.test.hasText(prefix, substring = true)).fetchSemanticsNodes().size
}
