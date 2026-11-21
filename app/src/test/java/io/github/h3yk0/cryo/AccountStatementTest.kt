// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo

import io.github.h3yk0.cryo.domain.Snapshot
import io.github.h3yk0.cryo.data.db.Account
import io.github.h3yk0.cryo.data.db.CreditCard
import io.github.h3yk0.cryo.data.db.Tx
import io.github.h3yk0.cryo.data.db.TxType
import io.github.h3yk0.cryo.domain.AccountStatement
import io.github.h3yk0.cryo.domain.Ledger
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

/**
 * RF-CON-08 (1.6, LA2): o extrato da conta, como o do banco. O saldo depois de cada lançamento segue a mesma conta do
 * saldo da conta (`Ledger.balances`): depois do último lançamento de hoje, é o saldo atual.
 */
class AccountStatementTest {
    init { JvmStrings.install() }

    private val today = LocalDate.of(2026, 11, 20)
    private val nubank = Account(id = 1, name = "Nubank", initialBalance = 100_000, color = 0)
    private val inter = Account(id = 2, name = "Inter", initialBalance = 50_000, color = 0)
    private val card = CreditCard(id = 1, name = "Roxinho", limitAmount = 500_000, closingDay = 3, dueDay = 10, color = 0)
    private fun d(day: Int, month: Int = 11) = LocalDate.of(2026, month, day)

    private val txs = listOf(
        Tx(id = 1, type = TxType.INCOME, amount = 400_000, date = d(28, 10), accountId = 1, description = "Salário"),
        Tx(id = 2, type = TxType.EXPENSE, amount = 3_250, date = d(3), accountId = 1, description = "Almoço"),
        Tx(id = 3, type = TxType.TRANSFER, amount = 20_000, date = d(3), accountId = 1, toAccountId = 2),
        Tx(id = 4, type = TxType.CARD_PAYMENT, amount = 80_000, date = d(10), accountId = 1, cardId = 1, invoiceYm = 202611),
        Tx(id = 5, type = TxType.EXPENSE, amount = 9_000, date = d(12), cardId = 1, description = "Mercado no cartão"),
        Tx(id = 6, type = TxType.INVEST_IN, amount = 30_000, date = d(15), accountId = 1, investmentId = 1),
        Tx(id = 7, type = TxType.TRANSFER, amount = 5_000, date = d(18), accountId = 2, toAccountId = 1),
        Tx(id = 8, type = TxType.EXPENSE, amount = 1_500, date = d(20), accountId = 1, description = "Café"),
        Tx(id = 9, type = TxType.EXPENSE, amount = 12_000, date = d(25), accountId = 1, description = "Luz (agendada)"),
    )
    private val l = Ledger(Snapshot(accounts = listOf(nubank, inter), cards = listOf(card), txs = txs), today)

    @Test fun depoisDoUltimoLancamentoDeHojeEhOSaldoDaConta() {
        val days = AccountStatement.month(l, 1, YearMonth.of(2026, 11))
        val todayDay = days.first { it.date == today }
        assertEquals(l.balance(1), todayDay.endBalance)
        assertEquals(l.balance(1), todayDay.lines.first().balanceAfter)
        assertEquals(l.balance(2), AccountStatement.month(l, 2, YearMonth.of(2026, 11)).first { it.date <= today }.endBalance)
    }

    @Test fun cadaLancamentoComOSaldoDepoisDele() {
        val days = AccountStatement.month(l, 1, YearMonth.of(2026, 11))
        // O mês começa com o saldo inicial + o salário de outubro.
        val before = AccountStatement.balanceBefore(l, 1, d(1))
        assertEquals(500_000L, before)
        val lines = days.reversed().flatMap { it.lines.reversed() }
        assertEquals(listOf(2L, 3L, 4L, 6L, 7L, 8L, 9L), lines.map { it.tx.id })
        // A compra no cartão não mexe na conta; o pagamento da fatura sim.
        assertEquals(listOf(496_750L, 476_750L, 396_750L, 366_750L, 371_750L, 370_250L, 358_250L), lines.map { it.balanceAfter })
        // No mesmo dia, o lançado primeiro vem primeiro na conta (e por último na tela).
        val day3 = days.first { it.date == d(3) }
        assertEquals(listOf(3L, 2L), day3.lines.map { it.tx.id })
        assertEquals(476_750L, day3.endBalance)
    }

    @Test fun transferenciaContaDosDoisLados() {
        val t = txs.first { it.id == 3L }
        assertEquals(-20_000L, AccountStatement.effect(t, 1))
        assertEquals(20_000L, AccountStatement.effect(t, 2))
        assertEquals(0L, AccountStatement.effect(txs.first { it.id == 5L }, 1))
    }

    @Test fun agendadoMostraOSaldoPrevisto() {
        val days = AccountStatement.month(l, 1, YearMonth.of(2026, 11))
        val future = days.first { it.date == d(25) }
        assertEquals(l.balance(1) - 12_000, future.endBalance)
    }

    @Test fun mesSemLancamentoNaoTemDias() {
        assertEquals(emptyList<AccountStatement.Day>(), AccountStatement.month(l, 2, YearMonth.of(2026, 9)))
    }
}
