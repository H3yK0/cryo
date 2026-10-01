// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo

import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Abre o app de verdade (Application + Activity), passa pelas boas-vindas e registra um gasto falando/digitando. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w393dp-h852dp-xxhdpi")
class AppFlowTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    private fun waitText(t: String, ms: Long = 8000) =
        rule.waitUntil(ms) { rule.onAllNodesWithText(t, substring = true).fetchSemanticsNodes().isNotEmpty() }

    @Test fun primeiroUsoERegistroRapido() {
        waitText("Bem-vindo ao Cryo")
        rule.onNodeWithText("Começar").performClick()
        waitText("Sua conta principal")
        rule.onNodeWithText("Continuar").performClick()
        waitText("Cartão de crédito")
        rule.onNodeWithText("Pular").performClick()
        waitText("Tudo pronto!")
        rule.onNodeWithText("Ir para o Cryo").performClick()
        waitText("Saldo em contas")

        rule.onNode(hasSetTextAction() and hasText("Ex.: almoço 25 no débito")).performTextInput("almoço 32,50 no débito")
        rule.onNodeWithContentDescription("Registrar").performClick()
        waitText("Confira o lançamento")
        rule.onRoot().captureRoboImage("screenshots/20_fluxo_inicio.png")
        rule.onNodeWithText("Salvar").performClick()

        val app = rule.activity.application as CryoApp
        rule.waitUntil(8000) { !app.container.repo.snapshot.value?.txs.isNullOrEmpty() }
        val tx = app.container.repo.snapshot.value!!.txs.single()
        assertEquals(3250L, tx.amount)
        assertEquals("Almoço", tx.description)
        assertEquals(java.time.LocalDate.now(), tx.date)

        rule.onNodeWithText("Extrato").performClick()
        waitText("Extrato")
        waitText("Almoço")
        rule.onRoot().captureRoboImage("screenshots/21_fluxo_movimentos.png")
    }
}
