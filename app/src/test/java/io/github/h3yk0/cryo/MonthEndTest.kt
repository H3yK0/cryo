// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo

import io.github.h3yk0.cryo.data.db.Account
import io.github.h3yk0.cryo.data.db.Bill
import io.github.h3yk0.cryo.data.db.BillSkip
import io.github.h3yk0.cryo.data.db.Budget
import io.github.h3yk0.cryo.data.db.Category
import io.github.h3yk0.cryo.data.db.CategoryKind
import io.github.h3yk0.cryo.data.db.CreditCard
import io.github.h3yk0.cryo.data.db.Debt
import io.github.h3yk0.cryo.data.db.Goal
import io.github.h3yk0.cryo.data.db.Investment
import io.github.h3yk0.cryo.data.db.Tx
import io.github.h3yk0.cryo.data.db.TxType
import io.github.h3yk0.cryo.domain.Ledger
import io.github.h3yk0.cryo.domain.Snapshot
import io.github.h3yk0.cryo.domain.predict.EndLine
import io.github.h3yk0.cryo.domain.predict.MonthEnd
import io.github.h3yk0.cryo.domain.predict.MonthEnds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

/**
 * RF-PRV-10 (reforma de 21/11/2026, LA11-1): o fim do mês real, renda × despesas, só com o que aconteceu e o que é fixo.
 * Guarda ERR-056 (o número é a soma das linhas; nada de entrada avulsa prevista) e ERR-064 (nunca orçamento planejado).
 */
class MonthEndTest {
    @Before fun strings() = JvmStrings.install()

    private val today = LocalDate.of(2026, 11, 12)
    private val acc = Account(1, "Conta", initialBalance = 200_000, color = 0)
    private val cats = listOf(
        Category(1, "Mercado", CategoryKind.EXPENSE, "cart", 0),
        Category(2, "Salário", CategoryKind.INCOME, "work", 0),
        Category(3, "Outros", CategoryKind.INCOME, "work", 0),
        Category(4, "Viagem", CategoryKind.EXPENSE, "flight", 0),
    )

    /** Quatro meses de mercado todo dia e salário de R$ 4.500 no dia 5; [extra] = entradas avulsas (data, valor). */
    private fun world(extra: List<Pair<LocalDate, Long>> = emptyList(), more: List<Tx> = emptyList(), until: LocalDate = today): Snapshot {
        val txs = ArrayList<Tx>()
        var id = 1L
        var day = today.minusMonths(4).withDayOfMonth(1)
        while (!day.isAfter(until)) {
            txs += Tx(id++, TxType.EXPENSE, 2_500L + (day.dayOfMonth % 5) * 700L, day, "Mercado", categoryId = 1, accountId = 1)
            if (day.dayOfMonth == 5) txs += Tx(id++, TxType.INCOME, 450_000, day, "Salário", categoryId = 2, accountId = 1)
            day = day.plusDays(1)
        }
        for ((d, v) in extra) txs += Tx(id++, TxType.INCOME, v, d, "Venda", categoryId = 3, accountId = 1)
        for (t in more) txs += t.copy(id = id++)
        return Snapshot(accounts = listOf(acc), categories = cats, txs = txs)
    }

    private fun end(s: Snapshot, day: LocalDate = today): MonthEnd = MonthEnds.of(Ledger(s, day))

    /** O mercado do mês até [day] (a única despesa variável do mundo). */
    private fun marketUntil(day: LocalDate): Long = (1..day.dayOfMonth).sumOf { 2_500L + (it % 5) * 700L }

    /** ERR-056: o número é sempre a soma das linhas, e a renda menos as despesas. */
    @Test fun oNumeroEhASomaDasLinhas() {
        val worlds = listOf(
            world(),
            world(extra = listOf(today.minusMonths(1).withDayOfMonth(20) to 80_000L, today.withDayOfMonth(9) to 50_000L)),
            Personas.generate(Personas.spec(Personas.Kind.STABLE), today, 7),
            Personas.generate(Personas.spec(Personas.Kind.IRREGULAR), today, 8),
            Personas.generate(Personas.spec(Personas.Kind.INDEBTED), today, 9),
            world().copy(budgets = listOf(Budget(4, 200_000), Budget(1, 240_000))),
        )
        for (s in worlds) {
            val e = end(s)
            assertEquals(e.items.sumOf { it.amount }, e.total)
            assertEquals(e.income - e.expenses, e.total)
            assertTrue("nenhuma linha zerada na tela", e.items.none { it.amount == 0L })
        }
    }

    /** ERR-056: uma venda que aconteceu uma vez, no mês passado, não volta como previsão; a deste mês conta uma vez. */
    @Test fun entradaQueAconteceuUmaVezNaoContaDeNovo() {
        val base = end(world())
        val pastSale = end(world(extra = listOf(today.minusMonths(1).withDayOfMonth(18) to 120_000L)))
        assertEquals(base.total, pastSale.total)
        assertNull(pastSale.item(EndLine.IN_OTHER))
        // Vendas em dois dos três meses passados também não viram previsão (nada é estimado).
        val twoMonths = end(world(extra = listOf(today.minusMonths(1).withDayOfMonth(18) to 60_000L, today.minusMonths(2).withDayOfMonth(22) to 70_000L)))
        assertEquals(base.total, twoMonths.total)
        // A venda que já entrou neste mês conta, uma vez, como outra entrada.
        val now = end(world(extra = listOf(today.withDayOfMonth(9) to 50_000L)))
        assertEquals(50_000L, now.amount(EndLine.IN_OTHER))
        assertEquals(base.total + 50_000L, now.total)
    }

    /** ERR-056: o que foi aplicado e guardado no mês entra (menos o resgatado), numa linha só. */
    @Test fun investimentosECaixinhasDoMesEntram() {
        val base = end(world())
        val s = world(more = listOf(
            Tx(0, TxType.INVEST_IN, 100_000, today.withDayOfMonth(6), "Aporte", accountId = 1, investmentId = 1),
            Tx(0, TxType.INVEST_OUT, 30_000, today.withDayOfMonth(8), "Resgate", accountId = 1, investmentId = 1),
            Tx(0, TxType.GOAL_IN, 20_000, today.withDayOfMonth(9), "Caixinha", accountId = 1, goalId = 1),
        )).copy(investments = listOf(Investment(1, "Reserva", color = 0)), goals = listOf(Goal(1, "Viagem", target = 500_000, color = 0)))
        val e = end(s)
        assertEquals(-90_000L, e.amount(EndLine.OUT_GOALS))
        assertEquals(base.total - 90_000L, e.total)
    }

    /**
     * ERR-064: o fim do mês nunca conta orçamento planejado. A conta fixa que está num orçamento entra uma vez só, e ter ou
     * não orçamento não muda o número (não há motor nesta conta: é a mesma antes e depois dele responder).
     */
    @Test fun semOMotorNaoContaOOrcamentoEmDobro() {
        val bill = Bill(1, "Passagem", 100_000, dueDay = 25, categoryId = 4, accountId = 1, startYm = 202611)
        val withBill = world().copy(bills = listOf(bill))
        val e = end(withBill.copy(budgets = listOf(Budget(4, 150_000))))
        assertEquals(-100_000L, e.amount(EndLine.OUT_BILLS))
        assertEquals(-100_000L, e.item(EndLine.OUT_BILLS)!!.pending)
        assertEquals(end(withBill).total, e.total)
        assertEquals(end(world()).total - 100_000L, e.total)
    }

    /** Renda fixa que ainda vem: o salário de todo mês, antes do dia dele; depois de chegar, conta uma vez só. */
    @Test fun rendaFixaQueAindaVem() {
        val early = LocalDate.of(2026, 11, 3)
        val before = end(world(until = early), early)
        assertEquals(450_000L, before.amount(EndLine.IN_FIXED))
        assertEquals(450_000L, before.item(EndLine.IN_FIXED)!!.pending)
        val after = end(world())
        assertEquals(450_000L, after.amount(EndLine.IN_FIXED))
        assertEquals(0L, after.item(EndLine.IN_FIXED)!!.pending)
        // Agendado com a mesma descrição, ele toma o lugar do salário esperado (não conta duas vezes).
        val scheduled = end(world(until = early, more = listOf(Tx(0, TxType.INCOME, 470_000, LocalDate.of(2026, 11, 6), "Salário", accountId = 1))), early)
        assertEquals(470_000L, scheduled.amount(EndLine.IN_FIXED))
        // Conta fixa de entrada em aberto também é renda fixa que ainda vem.
        val rent = Bill(2, "Aluguel recebido", 120_000, dueDay = 28, kind = CategoryKind.INCOME, accountId = 1, startYm = 202611)
        val withRent = end(world().copy(bills = listOf(rent)))
        assertEquals(570_000L, withRent.amount(EndLine.IN_FIXED))
        assertEquals(120_000L, withRent.item(EndLine.IN_FIXED)!!.pending)
    }

    /** Cada despesa na sua linha: conta fixa, dívida, parcela do cartão, variável e agendada; transferência e fatura fora. */
    @Test fun cadaDespesaNaSuaLinha() {
        val card = CreditCard(1, "Roxo", limitAmount = 500_000, closingDay = 3, dueDay = 10, color = 0, payAccountId = 1)
        val bill = Bill(1, "Internet", 10_000, dueDay = 10, accountId = 1, startYm = 202611)
        val debt = Debt(1, "Empréstimo", color = 0, initialBalance = 300_000, installmentAmount = 50_000, installmentCount = 6, dueDay = 20, startYm = 202611, accountId = 1)
        val s = world(more = listOf(
            Tx(0, TxType.EXPENSE, 10_000, today.withDayOfMonth(10), "Internet", accountId = 1, billId = 1, billYm = 202611),
            Tx(0, TxType.EXPENSE, 30_000, today.withDayOfMonth(2), "TV 1/3", cardId = 1, invoiceYm = 202612, installmentGroup = "g", installmentNumber = 1, installmentTotal = 3),
            Tx(0, TxType.EXPENSE, 7_000, today.withDayOfMonth(11), "Cinema", cardId = 1, invoiceYm = 202612),
            Tx(0, TxType.EXPENSE, 25_000, today.withDayOfMonth(28), "Presente", accountId = 1),
            Tx(0, TxType.TRANSFER, 50_000, today.withDayOfMonth(7), "Poupança", accountId = 1, toAccountId = 1),
            Tx(0, TxType.CARD_PAYMENT, 40_000, today.withDayOfMonth(10), "Fatura", accountId = 1, cardId = 1, invoiceYm = 202611),
        )).copy(cards = listOf(card), bills = listOf(bill), debts = listOf(debt))
        val e = end(s)
        assertEquals(-10_000L, e.amount(EndLine.OUT_BILLS))
        assertEquals(-(30_000L + 50_000L), e.amount(EndLine.OUT_DEBTS))
        assertEquals(-50_000L, e.item(EndLine.OUT_DEBTS)!!.pending)
        assertEquals(-(marketUntil(today) + 7_000L), e.amount(EndLine.OUT_VARIABLE))
        assertEquals(-25_000L, e.amount(EndLine.OUT_SCHEDULED))
        assertEquals(450_000L - 10_000L - 80_000L - marketUntil(today) - 7_000L - 25_000L, e.total)
    }

    /** Nada estimado: a despesa variável é exatamente o que já saiu, e o número só cai quando uma despesa entra. */
    @Test fun nadaEstimado() {
        val e = end(world())
        assertEquals(-marketUntil(today), e.amount(EndLine.OUT_VARIABLE))
        assertEquals(450_000L - marketUntil(today), e.total)
        val next = end(world(more = listOf(Tx(0, TxType.EXPENSE, 12_345, today, "Farmácia", accountId = 1))))
        assertEquals(e.total - 12_345L, next.total)
    }

    /** Metas com prazo: o que falta guardar neste mês para chegar lá no prazo entra como despesa (o compromisso). */
    @Test fun metasComPrazoEntram() {
        // R$ 9.000 até julho de 2027: 9 meses, R$ 1.000 por mês; R$ 300 já guardados neste mês, faltam R$ 700.
        val goal = Goal(1, "Viagem", target = 900_000, deadline = LocalDate.of(2027, 7, 15), color = 0)
        val s = world(more = listOf(Tx(0, TxType.GOAL_IN, 30_000, today.withDayOfMonth(4), "Viagem", accountId = 1, goalId = 1))).copy(goals = listOf(goal))
        val e = end(s)
        assertEquals(-100_000L, e.amount(EndLine.OUT_GOALS))
        assertEquals(-70_000L, e.item(EndLine.OUT_GOALS)!!.pending)
        // Sem prazo, só o que já foi guardado.
        val free = end(s.copy(goals = listOf(goal.copy(deadline = null))))
        assertEquals(-30_000L, free.amount(EndLine.OUT_GOALS))
    }

    /** O que dá por dia sai da sobra (contando hoje); quando não sobra, zero. */
    @Test fun porDiaSaiDaSobra() {
        val e = end(world())
        assertEquals(18, e.remainingDays)
        assertEquals(e.total / 19, e.perDay)
        val red = end(world(more = listOf(Tx(0, TxType.EXPENSE, 900_000, today, "Conserto", accountId = 1))))
        assertTrue(red.total < 0)
        assertEquals(0L, red.perDay)
    }

    /** RF-FIX-12: a conta fixa pulada neste mês não entra no fim do mês. */
    @Test fun contaFixaPuladaNaoConta() {
        val bill = Bill(1, "Academia", 9_000, dueDay = 28, accountId = 1, startYm = 202611)
        val s = world().copy(bills = listOf(bill))
        assertEquals(-9_000L, end(s).amount(EndLine.OUT_BILLS))
        assertNull(end(s.copy(billSkips = listOf(BillSkip(1, 202611)))).item(EndLine.OUT_BILLS))
    }
}
