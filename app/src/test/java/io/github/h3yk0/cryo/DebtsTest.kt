// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo

import io.github.h3yk0.cryo.data.db.Account
import io.github.h3yk0.cryo.data.db.Debt
import io.github.h3yk0.cryo.data.db.DebtAdjustment
import io.github.h3yk0.cryo.data.db.DebtKind
import io.github.h3yk0.cryo.data.db.Tx
import io.github.h3yk0.cryo.data.db.TxType
import io.github.h3yk0.cryo.domain.DebtMath
import io.github.h3yk0.cryo.domain.DebtState
import io.github.h3yk0.cryo.domain.Ledger
import io.github.h3yk0.cryo.domain.Snapshot
import io.github.h3yk0.cryo.domain.forecast
import io.github.h3yk0.cryo.domain.insights
import io.github.h3yk0.cryo.domain.moneyFlow
import io.github.h3yk0.cryo.notify.debtReminderTitle
import io.github.h3yk0.cryo.notify.debtReminders
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

/** Regras das dívidas: quanto falta, parcelas, atrasos, previsão e patrimônio. */
class DebtsTest {
    /** 12 parcelas de R$ 500, 2 já pagas antes do Cryo, vencimento dia 10, próxima em setembro/2026. */
    private val moto = Debt(
        id = 1, name = "Moto", kind = DebtKind.FINANCING, color = 0, initialBalance = 500_000,
        installmentAmount = 50_000, installmentCount = 12, paidBefore = 2, dueDay = 10, startYm = 202609, accountId = 1,
    )

    private fun pay(amount: Long, date: LocalDate, debtId: Long = 1) =
        Tx(type = TxType.EXPENSE, amount = amount, date = date, accountId = 1, debtId = debtId)

    private fun info(today: LocalDate, txs: List<Tx> = emptyList(), adj: List<DebtAdjustment> = emptyList(), d: Debt = moto) =
        DebtMath.info(d, txs, adj, today)

    @Test fun parcelasEVencimentos() {
        val i = info(LocalDate.of(2026, 9, 5))
        assertEquals(500_000L, i.outstanding)
        assertEquals(2, i.installmentsPaid)
        assertEquals(12, i.installmentsTotal)
        assertEquals(10, i.installmentsLeft)
        assertEquals(LocalDate.of(2026, 9, 10), i.nextDue)
        assertEquals(50_000L, i.nextAmount)
        assertEquals(DebtState.UPCOMING, i.state)
        assertEquals(50_000L, i.pendingThisMonth)
        assertEquals(YearMonth.of(2027, 6), i.payoffYm)
        assertEquals(100_000L, i.paidAll) // 2 parcelas pagas antes do Cryo

        assertEquals(DebtState.DUE_SOON, info(LocalDate.of(2026, 9, 8)).state)
        val late = info(LocalDate.of(2026, 10, 11))
        assertEquals(DebtState.OVERDUE, late.state)
        assertEquals(2, late.overdueCount) // setembro e outubro
        assertEquals(100_000L, late.pendingThisMonth)
    }

    @Test fun pagamentosQuitamAParcelaMaisAntiga() {
        val day = LocalDate.of(2026, 9, 11)
        val paid = info(day, listOf(pay(50_000, LocalDate.of(2026, 9, 10))))
        assertEquals(450_000L, paid.outstanding)
        assertEquals(3, paid.installmentsPaid)
        assertEquals(DebtState.PAID_THIS_MONTH, paid.state)
        assertEquals(LocalDate.of(2026, 10, 10), paid.nextDue)
        assertEquals(0L, paid.pendingThisMonth)

        // pagou só uma parte: a parcela continua em aberto, faltando a diferença
        val partial = info(day, listOf(pay(30_000, LocalDate.of(2026, 9, 10))))
        assertEquals(2, partial.installmentsPaid)
        assertEquals(20_000L, partial.nextAmount)
        assertEquals(DebtState.OVERDUE, partial.state)
        assertEquals(20_000L, partial.pendingThisMonth)

        // centavos de diferença contam como parcela paga
        assertEquals(3, info(day, listOf(pay(49_990, LocalDate.of(2026, 9, 10)))).installmentsPaid)

        // pagamento agendado para depois de hoje ainda não conta
        assertEquals(500_000L, info(day, listOf(pay(50_000, LocalDate.of(2026, 9, 20)))).outstanding)
    }

    @Test fun quitacaoComDescontoEJuros() {
        val day = LocalDate.of(2026, 9, 11)
        val discount = DebtAdjustment(debtId = 1, date = day, amount = -60_000)
        val off = info(day, listOf(pay(440_000, day)), listOf(discount))
        assertTrue(off.isPaidOff)
        assertEquals(DebtState.PAID_OFF, off.state)
        assertEquals(0L, off.remaining)
        assertEquals(1f, off.progress)
        assertNull(off.nextDue)

        val interest = DebtAdjustment(debtId = 1, date = day, amount = 12_000)
        assertEquals(512_000L, info(day, adj = listOf(interest)).outstanding)
    }

    @Test fun quitadaComDescontoContaTodasAsParcelas() {
        // 6 parcelas de R$ 150, 4 pagas antes; pagou R$ 150 e depois R$ 130 com R$ 20 de desconto
        val carne = moto.copy(initialBalance = 30_000, installmentAmount = 15_000, installmentCount = 6, paidBefore = 4, dueDay = 20, startYm = 202608)
        val day = LocalDate.of(2026, 10, 2)
        val i = info(
            day, listOf(pay(15_000, LocalDate.of(2026, 8, 20)), pay(13_000, LocalDate.of(2026, 9, 20))),
            listOf(DebtAdjustment(debtId = 1, date = LocalDate.of(2026, 9, 20), amount = -2_000)), carne,
        )
        assertTrue(i.isPaidOff)
        assertEquals(6, i.installmentsPaid)
        assertEquals(6, i.installmentsTotal)
        assertTrue(DebtMath.installmentIn(i, YearMonth.of(2026, 8))!!.paid)
        assertTrue(DebtMath.installmentIn(i, YearMonth.of(2026, 9))!!.paid)
        assertNull(DebtMath.installmentIn(i, YearMonth.of(2026, 10)))

        // quitou tudo de uma vez no primeiro mês: os meses seguintes não mostram parcelas
        val early = info(day, listOf(pay(500_000, LocalDate.of(2026, 9, 10))))
        assertEquals(12, early.installmentsPaid)
        assertTrue(DebtMath.installmentIn(early, YearMonth.of(2026, 9))!!.paid)
        assertNull(DebtMath.installmentIn(early, YearMonth.of(2026, 10)))
    }

    @Test fun parcelaDoMes() {
        val sep = YearMonth.of(2026, 9)
        val open = info(LocalDate.of(2026, 9, 5))
        assertNull(DebtMath.installmentIn(open, YearMonth.of(2026, 8)))
        DebtMath.installmentIn(open, sep)!!.let {
            assertEquals(3, it.number)
            assertTrue(it.payable)
            assertFalse(it.paid)
        }
        DebtMath.installmentIn(open, sep.plusMonths(1))!!.let { assertFalse(it.payable); assertFalse(it.paid) }
        assertNotNull(DebtMath.installmentIn(open, YearMonth.of(2027, 6)))
        assertNull(DebtMath.installmentIn(open, YearMonth.of(2027, 7)))

        val paid = info(LocalDate.of(2026, 9, 11), listOf(pay(50_000, LocalDate.of(2026, 9, 10))))
        assertTrue(DebtMath.installmentIn(paid, sep)!!.paid)
        assertTrue(DebtMath.installmentIn(paid, sep.plusMonths(1))!!.payable)
    }

    @Test fun dividaSemParcelas() {
        val joao = Debt(
            id = 2, name = "João", kind = DebtKind.PERSON, color = 0, initialBalance = 120_000,
            startYm = 202609, deadline = LocalDate.of(2026, 12, 15),
        )
        val i = info(LocalDate.of(2026, 9, 30), d = joao)
        assertEquals(DebtState.FREE, i.state)
        assertEquals(30_000L, i.monthlyNeeded) // 4 meses até dezembro
        assertEquals(YearMonth.of(2026, 12), i.payoffYm)
        assertEquals(DebtState.OVERDUE, info(LocalDate.of(2026, 12, 16), d = joao).state)
        // pegou mais emprestado: a dívida aumenta
        val more = Tx(type = TxType.DEBT_IN, amount = 20_000, date = LocalDate.of(2026, 9, 20), accountId = 1, debtId = 2)
        assertEquals(140_000L, info(LocalDate.of(2026, 9, 30), listOf(more), d = joao).outstanding)
        assertEquals(null, info(LocalDate.of(2026, 9, 30), d = joao.copy(deadline = null)).monthlyNeeded)
    }

    @Test fun primeiraParcelaSugerida() {
        assertEquals(YearMonth.of(2026, 10), DebtMath.suggestedStart(LocalDate.of(2026, 9, 30), 10))
        assertEquals(YearMonth.of(2026, 9), DebtMath.suggestedStart(LocalDate.of(2026, 9, 30), 30))
    }

    @Test fun livroCaixaComDividas() {
        val today = LocalDate.of(2026, 9, 5)
        val acc = Account(id = 1, name = "Conta", initialBalance = 100_000, color = 0)
        val joao = Debt(id = 2, name = "João", kind = DebtKind.PERSON, color = 0, initialBalance = 0, startYm = 202609)
        val old = moto.copy(id = 3, name = "Antiga", archived = true)
        val txs = listOf(
            Tx(type = TxType.DEBT_IN, amount = 30_000, date = LocalDate.of(2026, 9, 2), accountId = 1, debtId = 2),
            pay(10_000, LocalDate.of(2026, 9, 3), debtId = 2),
            Tx(type = TxType.INCOME, amount = 200_000, date = LocalDate.of(2026, 9, 1), accountId = 1),
        )
        val l = Ledger(Snapshot(accounts = listOf(acc), debts = listOf(moto, joao, old), txs = txs), today)
        assertEquals(100_000L + 30_000 - 10_000 + 200_000, l.balance(1)) // o empréstimo entrou na conta
        val m = l.monthSummary(YearMonth.of(2026, 9))
        assertEquals(200_000L, m.income) // empréstimo não é receita
        assertEquals(10_000L, m.expense) // o pagamento é um gasto
        assertEquals(30_000L, m.borrowed)
        assertEquals(500_000L + 20_000, l.totalDebts) // a arquivada não entra
        assertEquals(l.totalBalance - 520_000, l.netWorth)
        assertEquals(2, l.debtOverview.open.size)
        assertEquals(50_000L, l.debtOverview.monthlyInstallments)
        assertTrue(l.moneyFlow(YearMonth.of(2026, 9)).sources.any { it.label == "Dinheiro emprestado" })

        val f = l.forecast()
        assertEquals(50_000L, f.pendingDebts) // parcela da moto do dia 10
        assertTrue(f.projectedExpense >= 60_000L)

        // pagamento já agendado para o dia 10 não é contado duas vezes
        val sched = Ledger(l.s.copy(txs = txs + pay(50_000, LocalDate.of(2026, 9, 10))), today).forecast()
        assertEquals(0L, sched.pendingDebts)
        assertEquals(50_000L, sched.scheduledExpense)
    }

    @Test fun avisosEDicas() {
        val l = Ledger(Snapshot(debts = listOf(moto)), LocalDate.of(2026, 9, 9))
        val r = debtReminders(l)
        assertEquals(1, r.size)
        assertEquals("Parcela de Moto vence amanhã", debtReminderTitle(r.first(), l.today))
        assertTrue(debtReminders(Ledger(Snapshot(debts = listOf(moto.copy(remind = false))), l.today)).isEmpty())
        assertTrue(debtReminders(Ledger(Snapshot(debts = listOf(moto)), LocalDate.of(2026, 9, 5))).isEmpty())

        val late = Ledger(Snapshot(debts = listOf(moto)), LocalDate.of(2026, 9, 20))
        assertTrue(late.insights().any { it.text.contains("atrasada") })
    }
}
