// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo

import io.github.h3yk0.cryo.data.db.Account
import io.github.h3yk0.cryo.data.db.Bill
import io.github.h3yk0.cryo.data.db.CategoryKind
import io.github.h3yk0.cryo.data.db.CreditCard
import io.github.h3yk0.cryo.data.db.Tx
import io.github.h3yk0.cryo.data.db.TxType
import io.github.h3yk0.cryo.domain.BillState
import io.github.h3yk0.cryo.domain.CardMath
import io.github.h3yk0.cryo.domain.InvoiceStatus
import io.github.h3yk0.cryo.domain.Ledger
import io.github.h3yk0.cryo.domain.Money
import io.github.h3yk0.cryo.domain.Snapshot
import io.github.h3yk0.cryo.domain.forecast
import io.github.h3yk0.cryo.domain.key
import io.github.h3yk0.cryo.domain.moneyFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class LedgerTest {
    private val today = LocalDate.of(2026, 9, 30)

    @Test fun formatacaoDeDinheiro() {
        assertEquals("R$ 1.234,56", Money.format(123456))
        assertEquals("R$ 0,05", Money.format(5))
        assertEquals("-R$ 10,00", Money.format(-1000))
        assertEquals(123450L, Money.parse("1.234,5"))
        assertEquals(1250L, Money.parse("12.50"))
        assertEquals(120000L, Money.parse("1200"))
    }

    @Test fun faturaPorDataDeCompra() {
        val c = CreditCard(id = 1, name = "C", limitAmount = 0, closingDay = 3, dueDay = 10, color = 0)
        assertEquals(YearMonth.of(2026, 9), CardMath.invoiceFor(c, LocalDate.of(2026, 9, 2)))
        assertEquals(YearMonth.of(2026, 10), CardMath.invoiceFor(c, LocalDate.of(2026, 9, 3)))
        assertEquals(YearMonth.of(2026, 10), CardMath.invoiceFor(c, LocalDate.of(2026, 9, 30)))
        val c2 = c.copy(closingDay = 28, dueDay = 5)
        assertEquals(YearMonth.of(2026, 10), CardMath.invoiceFor(c2, LocalDate.of(2026, 9, 27)))
        assertEquals(YearMonth.of(2026, 11), CardMath.invoiceFor(c2, LocalDate.of(2026, 9, 28)))
        assertEquals(LocalDate.of(2026, 9, 28), CardMath.closingDate(c2, YearMonth.of(2026, 10)))
        assertEquals(LocalDate.of(2026, 10, 5), CardMath.dueDate(c2, YearMonth.of(2026, 10)))
        // fechamento no dia 31 em fevereiro
        val c3 = c.copy(closingDay = 31, dueDay = 8)
        assertEquals(LocalDate.of(2027, 2, 28), CardMath.closingDate(c3, YearMonth.of(2027, 3)))
    }

    @Test fun parcelas() {
        assertEquals(listOf(33334L, 33333L, 33333L), CardMath.split(100000, 3))
        assertEquals(100000L, CardMath.split(100000, 7).sum())
    }

    @Test fun saldosEFaturas() {
        val acc = Account(id = 1, name = "Conta", initialBalance = 100000, color = 0)
        val acc2 = Account(id = 2, name = "Poupança", initialBalance = 0, color = 0)
        val card = CreditCard(id = 1, name = "Cartão", limitAmount = 200000, closingDay = 3, dueDay = 10, color = 0)
        val txs = listOf(
            Tx(type = TxType.EXPENSE, amount = 20000, date = today.minusDays(1), accountId = 1),
            Tx(type = TxType.TRANSFER, amount = 30000, date = today, accountId = 1, toAccountId = 2),
            Tx(type = TxType.EXPENSE, amount = 5000, date = today.plusDays(3), accountId = 1), // futuro: não conta
            Tx(type = TxType.EXPENSE, amount = 40000, date = LocalDate.of(2026, 9, 1), cardId = 1, invoiceYm = 202609),
            Tx(type = TxType.EXPENSE, amount = 10000, date = LocalDate.of(2026, 9, 20), cardId = 1, invoiceYm = 202610),
            Tx(type = TxType.CARD_PAYMENT, amount = 40000, date = LocalDate.of(2026, 9, 10), accountId = 1, cardId = 1, invoiceYm = 202609),
        )
        val l = Ledger(Snapshot(accounts = listOf(acc, acc2), cards = listOf(card), txs = txs), today)
        assertEquals(100000L - 20000 - 30000 - 40000, l.balance(1))
        assertEquals(30000L, l.balance(2))
        val sep = l.invoice(card, YearMonth.of(2026, 9))
        assertEquals(40000L, sep.total)
        assertEquals(0L, sep.remaining)
        assertEquals(InvoiceStatus.PAID, sep.status(today))
        val oct = l.invoice(card, YearMonth.of(2026, 10))
        assertEquals(InvoiceStatus.OPEN, oct.status(today))
        assertEquals(10000L, l.cardOutstanding(1))
        assertEquals(190000L, l.availableLimit(card))
        assertEquals(YearMonth.of(2026, 10), l.focusInvoice(card).ym)
        assertEquals(l.totalBalance - 10000, l.netWorth)
    }

    @Test fun contasFixasEPrevisao() {
        val acc = Account(id = 1, name = "Conta", initialBalance = 0, color = 0)
        val day10 = LocalDate.of(2026, 9, 10)
        val bills = listOf(
            Bill(id = 1, name = "Aluguel", amount = 100000, dueDay = 5, startYm = 202601),
            Bill(id = 2, name = "Internet", amount = 10000, dueDay = 20, startYm = 202601),
            Bill(id = 3, name = "Salário", amount = 500000, dueDay = 5, kind = CategoryKind.INCOME, startYm = 202601),
        )
        val txs = listOf(
            Tx(type = TxType.INCOME, amount = 500000, date = LocalDate.of(2026, 9, 5), accountId = 1, billId = 3, billYm = 202609),
            Tx(type = TxType.EXPENSE, amount = 100000, date = LocalDate.of(2026, 9, 5), accountId = 1, billId = 1, billYm = 202609),
            Tx(type = TxType.EXPENSE, amount = 20000, date = LocalDate.of(2026, 9, 6), accountId = 1),
        )
        val l = Ledger(Snapshot(accounts = listOf(acc), bills = bills, txs = txs), day10)
        val st = l.billStatuses(YearMonth.of(2026, 9)).associateBy { it.bill.id }
        assertEquals(BillState.PAID, st[1]?.state)
        assertEquals(BillState.UPCOMING, st[2]?.state)
        val f = l.forecast()
        assertEquals(500000L, f.projectedIncome)
        assertEquals(2000L, f.dailyRate) // 200 em 10 dias
        assertEquals(100000L + 20000 + 10000 + 2000L * 20, f.projectedExpense)
        assertEquals(f.projectedResult, f.series.last().projected)
        assertEquals(30, f.series.size)
        assertTrue(l.moneyFlow(YearMonth.of(2026, 9)).destinations.any { it.label == "Sobrou" })
        assertEquals(202609, YearMonth.of(2026, 9).key())
    }
}
