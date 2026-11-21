// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo

import io.github.h3yk0.cryo.data.db.Tx
import io.github.h3yk0.cryo.data.db.TxType
import io.github.h3yk0.cryo.domain.AccountStatement
import io.github.h3yk0.cryo.domain.TxSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** RF-EXT-09 (1.6, LA3): o Extrato filtrado por uma conta ou por um cartão, com o resumo do topo sendo o dela. */
class StatementTest {
    private val day = LocalDate.of(2026, 11, 10)
    private val txs = listOf(
        Tx(id = 1, type = TxType.EXPENSE, amount = 17_000, date = day, accountId = 1, description = "Assinatura RealGO"),
        Tx(id = 2, type = TxType.EXPENSE, amount = 5_499, date = day, accountId = 2, description = "Multa de trânsito"),
        Tx(id = 3, type = TxType.TRANSFER, amount = 10_000, date = day, accountId = 2, toAccountId = 1),
        Tx(id = 4, type = TxType.EXPENSE, amount = 8_000, date = day, cardId = 7, description = "Mercado"),
        Tx(id = 5, type = TxType.INCOME, amount = 1_000, date = day, cardId = 7, description = "Estorno"),
        Tx(id = 6, type = TxType.CARD_PAYMENT, amount = 20_000, date = day, accountId = 1, cardId = 7, invoiceYm = 202611),
        Tx(id = 7, type = TxType.INCOME, amount = 300_000, date = day, accountId = 1, description = "Salário"),
    )

    private fun of(s: TxSource) = txs.filter { AccountStatement.touches(it, s) }.map { it.id }

    @Test fun soOsLancamentosDaConta() {
        assertEquals(listOf(1L, 3L, 6L, 7L), of(TxSource.Account(1)))
        assertEquals(listOf(2L, 3L), of(TxSource.Account(2)))
        assertEquals(listOf(4L, 5L, 6L), of(TxSource.Card(7)))
    }

    @Test fun oResumoEhODaConta() {
        val a1 = AccountStatement.summary(txs.filter { AccountStatement.touches(it, TxSource.Account(1)) }, TxSource.Account(1))
        // Entrou: a transferência da conta 2 e o salário. Saiu: a assinatura e o pagamento da fatura.
        assertEquals(310_000L, a1.income)
        assertEquals(37_000L, a1.expense)
        assertEquals(273_000L, a1.result)
        val c = AccountStatement.summary(txs.filter { AccountStatement.touches(it, TxSource.Card(7)) }, TxSource.Card(7))
        // No cartão: a compra é saída; o estorno e o pagamento diminuem o que se deve.
        assertEquals(21_000L, c.income)
        assertEquals(8_000L, c.expense)
    }

    @Test fun transferenciaApareceNasDuasContas() {
        val t = txs.first { it.id == 3L }
        assertTrue(AccountStatement.touches(t, TxSource.Account(1)))
        assertTrue(AccountStatement.touches(t, TxSource.Account(2)))
        assertFalse(AccountStatement.touches(t, TxSource.Card(7)))
    }
}
