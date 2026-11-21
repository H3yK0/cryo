// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo

import io.github.h3yk0.cryo.data.db.Account
import io.github.h3yk0.cryo.data.db.Debt
import io.github.h3yk0.cryo.data.db.Tx
import io.github.h3yk0.cryo.data.db.TxType
import io.github.h3yk0.cryo.domain.Ledger
import io.github.h3yk0.cryo.domain.Snapshot
import io.github.h3yk0.cryo.domain.engine.Projection
import io.github.h3yk0.cryo.domain.engine.Projections
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.random.Random

/**
 * ERR-061 (auditoria da 1.6): parcelas de dívida na projeção (RF-MOT-04, RF-MOT-17). Sem histórico de gastos a projeção não
 * sorteia nada, então cada número abaixo é conta de cabeça: R$ 1.000 na conta e 5 parcelas de R$ 500 a partir de 05/12.
 */
class DebtProjectionTest {
    @Before fun strings() = JvmStrings.install()

    private val today = LocalDate.of(2026, 11, 21)
    private val account = Account(1, "Conta", initialBalance = 100000, color = 0)
    private val debt = Debt(1, "Empréstimo", color = 0, initialBalance = 250000, installmentAmount = 50000, installmentCount = 5, dueDay = 5, startYm = 202612, accountId = 1)
    private fun d(day: Int, month: Int, year: Int = if (month >= 11) 2026 else 2027) = LocalDate.of(year, month, day)

    private fun pay(id: Long, date: LocalDate, amount: Long = 50000) = Tx(id, TxType.EXPENSE, amount, date, "Parcela", accountId = 1, debtId = 1)

    private fun project(txs: List<Tx>, months: Int = 12, until: LocalDate? = null, today: LocalDate = this.today, debt: Debt = this.debt): Projection =
        Projections.project(Ledger(Snapshot(accounts = listOf(account), debts = listOf(debt), txs = txs), today), emptyList(), 1, months = months, scenarios = 10, until = until)

    private fun Projection.at(date: LocalDate) = mid[ChronoUnit.DAYS.between(start, date).toInt().coerceAtMost(horizonDays)]

    /** O caminho de quem paga cada parcela no vencimento. */
    private fun assertOnDueDates(p: Projection) {
        assertEquals(50000L, p.at(d(5, 12)))
        assertEquals(0L, p.at(d(5, 1)))
        assertEquals(-50000L, p.at(d(5, 2)))
        assertEquals(-100000L, p.at(d(5, 3)))
        assertEquals(-150000L, p.at(d(5, 4)))
        assertEquals(-150000L, p.mid[p.horizonDays])
        assertEquals(d(5, 2), p.firstNegativeWithin(p.horizonDays))
    }

    @Test fun semAgendadoCadaParcelaNoVencimento() = assertOnDueDates(project(emptyList()))

    @Test fun pagamentoNoDiaAntesDepoisParcialEAdiantado() {
        assertOnDueDates(project(listOf(pay(10, d(5, 12)))))
        // Antes do vencimento: sai no dia do pagamento e a parcela de dezembro já está paga.
        val early = project(listOf(pay(10, d(30, 11))))
        assertEquals(50000L, early.at(d(30, 11)))
        assertOnDueDates(early)
        // Cinco dias atrasado: a parcela de dezembro sai no dia 10 (é a parcela mais perto do pagamento).
        val late = project(listOf(pay(10, d(10, 12))))
        assertEquals(100000L, late.at(d(5, 12)))
        assertEquals(50000L, late.at(d(10, 12)))
        assertEquals(0L, late.at(d(5, 1)))
        // Parcial: o resto da parcela continua no vencimento.
        assertOnDueDates(project(listOf(pay(10, d(5, 12), 30000))))
        // Duas parcelas adiantadas de uma vez.
        val two = project(listOf(pay(10, d(30, 11), 100000)))
        assertEquals(0L, two.at(d(5, 12)))
        assertEquals(0L, two.at(d(5, 1)))
        assertEquals(-50000L, two.at(d(5, 2)))
    }

    @Test fun pagamentoDeMarcoNaoEscondeAParcelaDeDezembro() {
        // Antes: o pagamento de março "pagava" a parcela de dezembro, e o primeiro dia negativo passava de 05/02 para 05/03.
        val p = project(listOf(pay(10, d(5, 3))))
        assertOnDueDates(p)
        // Apagar o agendado volta ao caminho normal (a projeção é sempre refeita dos dados).
        assertOnDueDates(project(emptyList()))
    }

    @Test fun pagamentoForaDoPeriodoNaoCobreParcelaDeDentro() {
        // Período de 3 meses (até 21/02) e o pagamento em 05/03: as parcelas de dezembro, janeiro e fevereiro saem.
        val p = project(listOf(pay(10, d(5, 3))), months = 3)
        assertEquals(-50000L, p.mid[p.horizonDays])
        assertEquals(d(5, 2), p.firstNegativeWithin(p.horizonDays))
        // "Como vou fechar o ano?" com a parcela de janeiro agendada: dezembro continua lá.
        val year = project(listOf(pay(10, d(5, 1))), until = d(31, 12))
        assertEquals(50000L, year.mid[year.horizonDays])
    }

    @Test fun quitadaDeUmaVez() {
        val p = project(listOf(pay(10, d(1, 12), 250000)))
        assertEquals(-150000L, p.at(d(1, 12)))
        assertEquals(-150000L, p.mid[p.horizonDays])
    }

    @Test fun parcelaAtrasadaNaoSomeComOPagamentoDoMesSeguinte() {
        // Hoje é 06/12 e a parcela de 05/12 não foi paga; janeiro está agendado. A atrasada sai amanhã, janeiro no dia 5.
        val p = project(listOf(pay(10, d(5, 1))), today = d(6, 12))
        assertEquals(50000L, p.at(d(7, 12)))
        assertEquals(0L, p.at(d(5, 1)))
    }

    @Test fun ultimaParcelaPagaAtrasadaNaoContaDuasVezes() {
        val one = debt.copy(initialBalance = 50000, installmentCount = 1)
        val p = project(listOf(pay(10, d(10, 1))), debt = one)
        assertEquals(100000L, p.at(d(5, 12)))
        assertEquals(50000L, p.at(d(10, 1)))
        assertEquals(50000L, p.mid[p.horizonDays])
    }

    @Test fun nadaContaDuasVezesNemSome() {
        // Com o período cobrindo todas as parcelas, o que sai por causa da dívida é sempre o maior entre o que falta e o que
        // está agendado: nenhuma parcela conta duas vezes e nenhuma some, qualquer que seja a data dos pagamentos.
        val rnd = Random(61)
        repeat(200) { case ->
            val pays = (0 until rnd.nextInt(5)).map { k ->
                pay(100L + k, today.plusDays(1L + rnd.nextInt(190)), 10000L * (1 + rnd.nextInt(12)))
            }
            val p = project(pays)
            val expected = 100000L - maxOf(250000L, pays.sumOf { it.amount })
            assertEquals("caso $case: $pays", expected, p.mid[p.horizonDays])
        }
    }
}
