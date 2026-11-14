// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo

import io.github.h3yk0.cryo.data.BillTemplates
import io.github.h3yk0.cryo.data.db.Account
import io.github.h3yk0.cryo.data.db.Bill
import io.github.h3yk0.cryo.data.db.Tx
import io.github.h3yk0.cryo.data.db.TxType
import io.github.h3yk0.cryo.domain.BillCycle
import io.github.h3yk0.cryo.domain.BillState
import io.github.h3yk0.cryo.domain.Ledger
import io.github.h3yk0.cryo.domain.Snapshot
import io.github.h3yk0.cryo.domain.key
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

/**
 * RF-CFX-09..12: contas fixas que não são mensais (anuais, semestrais), pagas em parcelas, com juros e com prazo.
 * Regras puras, sem Android.
 */
class BillCycleTest {
    companion object {
        @JvmStatic @BeforeClass fun strings() = JvmStrings.install()
    }

    private val jan = YearMonth.of(2026, 1)
    private fun ym(y: Int, m: Int) = YearMonth.of(y, m)

    /** IPVA: uma vez por ano, em janeiro, em 3 parcelas. Vence em jan, fev e mar; nos outros meses, nada. */
    private val ipva = Bill(
        id = 1, name = "IPVA", amount = 42_000, dueDay = 10, startYm = jan.key(),
        every = 12, installments = 3, cashAmount = 120_000, accountId = 1,
    )

    @Test fun aContaMensalContinuaIgual() {
        val b = Bill(id = 1, name = "Internet", amount = 9_990, dueDay = 15, startYm = 202601, accountId = 1)
        for (m in 1..12) assertEquals("mês $m", 1, BillCycle.partOf(b, ym(2026, m)))
        assertEquals(1, BillCycle.parts(b))
        assertEquals(9_990L, BillCycle.cycleTotal(b))
        assertEquals(0L, BillCycle.interest(b))
        // Antes de começar, não existe.
        assertNull(BillCycle.partOf(b, ym(2025, 12)))
    }

    @Test fun aContaAnualParceladaVenceSoNosMesesDoCiclo() {
        assertEquals(1, BillCycle.partOf(ipva, ym(2026, 1)))
        assertEquals(2, BillCycle.partOf(ipva, ym(2026, 2)))
        assertEquals(3, BillCycle.partOf(ipva, ym(2026, 3)))
        for (m in 4..12) assertNull("mês $m", BillCycle.partOf(ipva, ym(2026, m)))
        // No ano seguinte, tudo de novo.
        assertEquals(1, BillCycle.partOf(ipva, ym(2027, 1)))
        assertEquals(3, BillCycle.partOf(ipva, ym(2027, 3)))
        assertEquals(3, BillCycle.parts(ipva))
        assertEquals(126_000L, BillCycle.cycleTotal(ipva))
        // Juros: 3 × 420 = 1.260 contra 1.200 à vista.
        assertEquals(6_000L, BillCycle.interest(ipva))
        assertEquals(0L, BillCycle.interest(ipva.copy(cashAmount = 0)))
        assertEquals(0L, BillCycle.interest(ipva.copy(cashAmount = 130_000)))
    }

    @Test fun semestralBimestralEPrazo() {
        val seguro = Bill(id = 2, name = "Seguro", amount = 60_000, dueDay = 10, startYm = 202603, every = 6, accountId = 1)
        assertEquals(1, BillCycle.partOf(seguro, ym(2026, 3)))
        assertNull(BillCycle.partOf(seguro, ym(2026, 8)))
        assertEquals(1, BillCycle.partOf(seguro, ym(2026, 9)))
        val condo = Bill(id = 3, name = "Taxa", amount = 10_000, dueDay = 10, startYm = 202601, every = 2, accountId = 1)
        assertEquals(listOf(1, 3, 5, 7, 9, 11), (1..12).filter { BillCycle.partOf(condo, ym(2026, it)) != null })
        // Prazo: o curso que acaba em junho não aparece em julho.
        val curso = Bill(id = 4, name = "Curso", amount = 30_000, dueDay = 10, startYm = 202601, endYm = 202606, accountId = 1)
        assertNotNull(BillCycle.partOf(curso, ym(2026, 6)))
        assertNull(BillCycle.partOf(curso, ym(2026, 7)))
    }

    /** O "parcelas" nunca passa do tamanho do ciclo: uma conta mensal não pode ser paga em 3 vezes mensais. */
    @Test fun asParcelasNuncaPassamDoCiclo() {
        val b = Bill(id = 5, name = "X", amount = 1_000, dueDay = 1, startYm = 202601, every = 1, installments = 3)
        assertEquals(1, BillCycle.parts(b))
        for (m in 1..12) assertEquals(1, BillCycle.partOf(b, ym(2026, m)))
    }

    /** O Ledger só mostra (e só cobra na previsão) a conta nos meses em que ela vence, com a parcela certa. */
    @Test fun oLedgerSegueOCiclo() {
        val s = Snapshot(accounts = listOf(Account(1, "Conta", color = 0)), bills = listOf(ipva))
        val emFevereiro = Ledger(s, LocalDate.of(2026, 2, 5)).billStatuses(ym(2026, 2))
        assertEquals(1, emFevereiro.size)
        assertEquals(2, emFevereiro.single().part)
        assertEquals(3, emFevereiro.single().parts)
        assertTrue(Ledger(s, LocalDate.of(2026, 5, 5)).billStatuses(ym(2026, 5)).isEmpty())
        // Pagou a primeira parcela: só ela fica paga.
        val pago = s.copy(txs = listOf(Tx(1, TxType.EXPENSE, 42_000, LocalDate.of(2026, 1, 10), "IPVA", accountId = 1, billId = 1, billYm = 202601)))
        assertEquals(BillState.PAID, Ledger(pago, LocalDate.of(2026, 2, 5)).billStatuses(ym(2026, 1)).single().state)
        assertEquals(BillState.OVERDUE, Ledger(pago, LocalDate.of(2026, 2, 20)).billStatuses(ym(2026, 2)).single().state)
    }

    /** RF-CFX-12: os modelos existem em todas as línguas e trazem um ritmo que faz sentido. */
    @Test fun osModelosTemNomeERitmo() {
        assertTrue(BillTemplates.all.size >= 8)
        for (t in BillTemplates.all) {
            assertTrue("modelo sem nome", BillTemplates.label(t).isNotBlank())
            assertTrue("ritmo estranho em ${BillTemplates.label(t)}", t.every in listOf(1, 2, 3, 6, 12))
            assertTrue(t.dueDay in 1..28)
        }
        // A anual de janeiro cai no próximo janeiro, nunca num mês que já passou.
        val tax = BillTemplates.all.first { it.month == 1 }
        assertEquals(YearMonth.of(2027, 1), BillTemplates.firstDue(tax, LocalDate.of(2026, 11, 13)))
        assertEquals(YearMonth.of(2026, 1), BillTemplates.firstDue(tax, LocalDate.of(2026, 1, 5)))
        // A mensal começa no mês em que se está.
        val rent = BillTemplates.all.first { it.every == 1 }
        assertEquals(YearMonth.of(2026, 11), BillTemplates.firstDue(rent, LocalDate.of(2026, 11, 13)))
    }
}
