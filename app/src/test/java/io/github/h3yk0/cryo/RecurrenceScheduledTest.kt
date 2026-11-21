// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo

import io.github.h3yk0.cryo.data.db.Account
import io.github.h3yk0.cryo.data.db.Tx
import io.github.h3yk0.cryo.data.db.TxType
import io.github.h3yk0.cryo.domain.Ledger
import io.github.h3yk0.cryo.domain.Snapshot
import io.github.h3yk0.cryo.domain.engine.Outlook
import io.github.h3yk0.cryo.domain.engine.Projections
import io.github.h3yk0.cryo.domain.engine.Recurrences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

/**
 * ERR-060 (auditoria da 1.6): um lançamento agendado com a mesma descrição de uma recorrência substitui só a ocorrência que
 * ele cobre. Antes, um salário agendado apagava a recorrência inteira da projeção: os outros 11 meses de salário sumiam.
 */
class RecurrenceScheduledTest {
    @Before fun strings() = JvmStrings.install()

    private val today = LocalDate.of(2026, 11, 21)
    private val account = Account(1, "Conta", initialBalance = 100000, color = 0)

    /** 12 salários de R$ 5.000 no dia 5 (o de novembro ainda não veio: a recorrência está atrasada). */
    private val salaries = (1..12).map { k -> Tx(k.toLong(), TxType.INCOME, 500000, today.minusMonths(k.toLong()).withDayOfMonth(5), "Salário", accountId = 1) }

    /** Sábados de feira (R$ 120) nas últimas 10 semanas. */
    private val market = (1..10).map { k -> Tx(100L + k, TxType.EXPENSE, 12000, LocalDate.of(2026, 11, 21).minusWeeks(k.toLong()), "Feira", accountId = 1) }

    private fun ledger(extra: List<Tx> = emptyList()) = Ledger(Snapshot(accounts = listOf(account), txs = salaries + market + extra), today)

    private fun salaryDates(l: Ledger, until: LocalDate) =
        Outlook.expectedItems(l, Recurrences.detect(l.s.txs, today), today, until).filter { it.recurrence.key == "salario" }.map { it.date }

    @Test fun umAgendadoTiraSoAOcorrenciaQueEleSubstitui() {
        val until = today.plusMonths(12)
        val plain = ledger()
        val withDec = ledger(listOf(Tx(500, TxType.INCOME, 500000, LocalDate.of(2026, 12, 5), "Salário", accountId = 1)))
        val before = salaryDates(plain, until)
        val after = salaryDates(withDec, until)
        // Sem agendado: o de novembro (atrasado, amanhã) e os 12 meses seguintes.
        assertEquals(13, before.size)
        // Com o de dezembro agendado: sai só o de dezembro.
        assertEquals(before - LocalDate.of(2026, 12, 5), after)

        // A projeção de 12 meses continua com os outros salários (antes caía de R$ 121 mil para R$ 66 mil).
        val rec = Recurrences.detect(withDec.s.txs, today)
        val p = Projections.project(withDec, rec, 7, months = 12, scenarios = 200)
        val p0 = Projections.project(plain, Recurrences.detect(plain.s.txs, today), 7, months = 12, scenarios = 200)
        assertTrue("fim de 12 meses: ${p.mid[p.horizonDays]} × ${p0.mid[p0.horizonDays]}", p.mid[p.horizonDays] >= p0.mid[p0.horizonDays] - 500000)

        // Semanal: o sábado agendado tira só aquele sábado.
        val sat = LocalDate.of(2026, 12, 12)
        val weekly = ledger(listOf(Tx(600, TxType.EXPENSE, 12000, sat, "Feira", accountId = 1)))
        fun marketDates(l: Ledger) = Outlook.expectedItems(l, Recurrences.detect(l.s.txs, today), today, until).filter { it.recurrence.key == "feira" }.map { it.date }
        val m0 = marketDates(plain)
        assertTrue(sat in m0)
        assertEquals(m0 - sat, marketDates(weekly))
    }

    @Test fun noMesOAgendadoContinuaNoLugarDaRecorrencia() {
        val monthEnd = today.withDayOfMonth(today.lengthOfMonth())
        // O salário de novembro, atrasado, aparece amanhã com metade da chance.
        assertEquals(listOf(today.plusDays(1)), salaryDates(ledger(), monthEnd))
        // Agendado para 28/11: ele toma o lugar do atrasado.
        assertEquals(emptyList<LocalDate>(), salaryDates(ledger(listOf(Tx(500, TxType.INCOME, 500000, LocalDate.of(2026, 11, 28), "Salário", accountId = 1))), monthEnd))
        // Agendado para 05/12 (até 7 dias depois do fim do mês, como antes): também.
        assertEquals(emptyList<LocalDate>(), salaryDates(ledger(listOf(Tx(500, TxType.INCOME, 500000, LocalDate.of(2026, 12, 5), "Salário", accountId = 1))), monthEnd))
        // Outra descrição não conta.
        assertEquals(listOf(today.plusDays(1)), salaryDates(ledger(listOf(Tx(500, TxType.INCOME, 500000, LocalDate.of(2026, 11, 28), "Venda do sofá", accountId = 1))), monthEnd))
    }
}
