// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo

import androidx.test.core.app.ApplicationProvider
import io.github.h3yk0.cryo.data.IncomePlanStore
import io.github.h3yk0.cryo.data.db.Account
import io.github.h3yk0.cryo.data.db.Bill
import io.github.h3yk0.cryo.data.db.Budget
import io.github.h3yk0.cryo.data.db.Category
import io.github.h3yk0.cryo.data.db.CategoryKind
import io.github.h3yk0.cryo.data.db.Investment
import io.github.h3yk0.cryo.data.db.InvestmentKind
import io.github.h3yk0.cryo.data.db.Tx
import io.github.h3yk0.cryo.data.db.TxType
import io.github.h3yk0.cryo.domain.Bucket
import io.github.h3yk0.cryo.domain.IncomeGuess
import io.github.h3yk0.cryo.domain.IncomePlanner
import io.github.h3yk0.cryo.domain.Ledger
import io.github.h3yk0.cryo.domain.PlanInputs
import io.github.h3yk0.cryo.domain.SavingKind
import io.github.h3yk0.cryo.domain.SavingSlice
import io.github.h3yk0.cryo.domain.Snapshot
import io.github.h3yk0.cryo.domain.key
import io.github.h3yk0.cryo.domain.ym
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/**
 * RF-ORC-07/08 (1.6): o orçamento pela renda. A renda que o Cryo acha, a divisão 50/30/20 que a pessoa ajusta, os limites
 * que cabem ou encolhem juntos, a reserva de 6 meses do essencial e os tipos de investimento (nunca produtos).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = TestApp::class)
class IncomePlanTest {
    private val today = LocalDate.of(2026, 11, 12)
    private val acc = Account(1, "Conta", initialBalance = 100_000, color = 0)
    private val market = Category(1, "Mercado", CategoryKind.EXPENSE, "cart", 0)
    private val home = Category(2, "Moradia", CategoryKind.EXPENSE, "home", 0)
    private val fun_ = Category(3, "Lazer", CategoryKind.EXPENSE, "movie", 0)
    private val salary = Category(4, "Salário", CategoryKind.INCOME, "work", 0)
    private val extra = Category(5, "Trabalhos", CategoryKind.INCOME, "laptop", 0)

    /** Três meses completos: mercado, aluguel e lazer todo mês, salário no dia 5 e freelas variados. */
    private fun world(
        marketPerMonth: List<Long> = listOf(80_000, 90_000, 100_000),
        funPerMonth: Long = 60_000,
        freelas: List<Long> = listOf(0, 150_000, 50_000),
        bills: List<Bill> = emptyList(),
        investments: List<Investment> = emptyList(),
        more: List<Tx> = emptyList(),
    ): Snapshot {
        val txs = ArrayList<Tx>()
        var id = 1L
        for (k in 3 downTo 1) {
            val ym = today.ym().minusMonths(k.toLong())
            txs += Tx(id++, TxType.INCOME, 500_000, ym.atDay(5), "Salário", categoryId = 4, accountId = 1)
            txs += Tx(id++, TxType.EXPENSE, marketPerMonth[3 - k], ym.atDay(8), "Supermercado", categoryId = 1, accountId = 1)
            txs += Tx(id++, TxType.EXPENSE, 150_000, ym.atDay(10), "Aluguel", categoryId = 2, accountId = 1)
            txs += Tx(id++, TxType.EXPENSE, funPerMonth, ym.atDay(20), "Cinema", categoryId = 3, accountId = 1)
            val f = freelas[3 - k]
            if (f > 0) txs += Tx(id++, TxType.INCOME, f, ym.atDay(22), "Freela", categoryId = 5, accountId = 1)
        }
        // Mais um salário antes (para o Cryo ter certeza de que ele se repete).
        txs += Tx(id++, TxType.INCOME, 500_000, today.ym().minusMonths(4).atDay(5), "Salário", categoryId = 4, accountId = 1)
        txs += Tx(id++, TxType.INCOME, 500_000, today.ym().atDay(5), "Salário", categoryId = 4, accountId = 1)
        for (t in more) txs += t.copy(id = id++)
        return Snapshot(
            accounts = listOf(acc), categories = listOf(market, home, fun_, salary, extra), txs = txs, bills = bills, investments = investments,
        )
    }

    private fun ledger(s: Snapshot = world()) = Ledger(s, today)

    @Test fun rendaFixaVemDoQueSeRepeteEAVariavelDoResto() {
        val g = IncomePlanner.guess(ledger())
        // O salário se repete: é a renda fixa (por mês).
        assertTrue("fixa ${g.fixed}", g.fixed in 490_000..510_000)
        // Os freelas dos 3 meses: 0, 1.500 e 500.
        assertEquals(0L, g.variableLow)
        assertEquals(150_000L, g.variableHigh)
        assertEquals(66_667L, g.variableAvg)
        assertEquals(2, g.variableMonths)
    }

    @Test fun contaFixaDeEntradaNaoMensalContaPelaParteDoMes() {
        // Um aluguel recebido a cada 3 meses (R$ 3.000): R$ 1.000 por mês na renda fixa.
        val rent = Bill(9, "Aluguel recebido", 300_000, dueDay = 15, kind = CategoryKind.INCOME, accountId = 1, startYm = today.ym().minusMonths(6).key(), every = 3)
        val without = IncomePlanner.guess(ledger()).fixed
        val with = IncomePlanner.guess(ledger(world(bills = listOf(rent)))).fixed
        assertEquals(100_000L, with - without)
    }

    @Test fun divisao503020EARendaVariavelSoQuandoLigada() {
        val l = ledger()
        val p = IncomePlanner.plan(l, PlanInputs(fixed = 500_000, variable = 100_000, useVariable = false))
        assertEquals(500_000L, p.base)
        assertEquals(250_000L, p.needs)
        assertEquals(150_000L, p.wants)
        assertEquals(100_000L, p.save)
        val v = IncomePlanner.plan(l, PlanInputs(fixed = 500_000, variable = 100_000, useVariable = true))
        assertEquals(600_000L, v.base)
        // A pessoa ajusta a divisão: o guardar é sempre o que sobra.
        val custom = IncomePlanner.plan(l, PlanInputs(500_000, 0, false, needsPct = 60, wantsPct = 30))
        assertEquals(300_000L, custom.needs)
        assertEquals(50_000L, custom.save)
    }

    @Test fun limitesSaoOTipicoQuandoCabem() {
        val p = IncomePlanner.plan(ledger(), PlanInputs(1_000_000, 0, false))
        val byId = p.categories.associateBy { it.categoryId }
        // Mediana do mercado (800, 900, 1.000) = 900; cabe nos 50% do essencial: o limite é o típico.
        assertEquals(90_000L, byId[1]!!.typical)
        assertEquals(90_000L, byId[1]!!.limit)
        assertEquals(Bucket.NEEDS, byId[1]!!.bucket)
        assertEquals(Bucket.NEEDS, byId[2]!!.bucket)
        assertEquals(Bucket.WANTS, byId[3]!!.bucket)
        assertEquals(240_000L, p.typicalNeeds)
        assertEquals(60_000L, p.typicalWants)
        // Primeiro o essencial, do maior gasto ao menor.
        assertEquals(listOf(2L, 1L, 3L), p.categories.map { it.categoryId })
    }

    @Test fun limitesEncolhemJuntosQuandoNaoCabem() {
        // Renda de R$ 4.000: essencial = R$ 2.000, mas a pessoa gasta R$ 2.400 (aluguel 1.500 + mercado 900).
        val p = IncomePlanner.plan(ledger(), PlanInputs(400_000, 0, false))
        val byId = p.categories.associateBy { it.categoryId }
        val rent = byId[2]!!.limit
        val mkt = byId[1]!!.limit
        // Mesma proporção para todos, arredondado para cima até a unidade da moeda.
        assertEquals(125_000L, rent)
        assertEquals(75_000L, mkt)
        assertTrue(rent + mkt <= p.needs + 200)
        assertEquals(0L, rent % 100)
        // O estilo de vida (R$ 600 de lazer) cabe nos 30% (R$ 1.200): fica como está.
        assertEquals(60_000L, byId[3]!!.limit)
    }

    @Test fun aPessoaTrocaACategoriaDeParte() {
        val p = IncomePlanner.plan(ledger(), PlanInputs(1_000_000, 0, false), buckets = mapOf(3L to Bucket.NEEDS, 1L to Bucket.WANTS))
        val byId = p.categories.associateBy { it.categoryId }
        assertEquals(Bucket.WANTS, byId[1]!!.bucket)
        assertEquals(Bucket.NEEDS, byId[3]!!.bucket)
        assertEquals(Bucket.NEEDS, IncomePlanner.bucketOf("cart", null))
        assertEquals(Bucket.WANTS, IncomePlanner.bucketOf("movie", null))
        assertEquals(Bucket.WANTS, IncomePlanner.bucketOf("cart", Bucket.WANTS))
    }

    @Test fun limiteDeHojeVemJunto() {
        val s = world().copy(budgets = listOf(Budget(1, 70_000)))
        val p = IncomePlanner.plan(ledger(s), PlanInputs(1_000_000, 0, false))
        assertEquals(70_000L, p.categories.first { it.categoryId == 1L }.current)
        assertNull(p.categories.first { it.categoryId == 2L }.current)
    }

    @Test fun contasFixasQuePassamDoEssencialAvisam() {
        val bills = listOf(
            Bill(1, "Aluguel", 150_000, dueDay = 10, categoryId = 2, accountId = 1, startYm = today.ym().minusMonths(6).key()),
            Bill(2, "IPVA", 120_000, dueDay = 10, categoryId = 2, accountId = 1, startYm = today.ym().minusMonths(6).key(), every = 12, installments = 3),
        )
        val l = ledger(world(bills = bills))
        // Aluguel inteiro + IPVA em 3x no ano (3/12 de R$ 1.200 = R$ 300 por mês).
        val p = IncomePlanner.plan(l, PlanInputs(300_000, 0, false))
        assertEquals(180_000L, p.fixedCosts)
        assertTrue(p.fixedCostsOver)
        assertFalse(IncomePlanner.plan(l, PlanInputs(1_000_000, 0, false)).fixedCostsOver)
    }

    @Test fun reservaPrimeiroDepoisObjetivosELongoPrazo() {
        // Reserva: 6 × o essencial de costume (R$ 2.400) = R$ 14.400. Guardando R$ 1.000 por mês, faltam 15 meses.
        val p = IncomePlanner.plan(ledger(), PlanInputs(500_000, 0, false))
        assertEquals(1_440_000L, p.reserveTarget)
        assertEquals(0L, p.reserveNow)
        assertEquals(listOf(SavingSlice(SavingKind.RESERVE, 100_000)), p.saving)
        assertEquals(15, p.reserveMonths)
        // Com a reserva completa (só aplicações de resgate rápido contam), o guardar vai metade para objetivos e metade para o longo prazo.
        val reserve = Investment(1, "Reserva", InvestmentKind.SAVINGS, color = 0)
        val stocks = Investment(2, "Ações", InvestmentKind.STOCKS, color = 0)
        val s = world(
            investments = listOf(reserve, stocks),
            more = listOf(
                Tx(0, TxType.INVEST_IN, 1_500_000, today.minusMonths(5), "Aporte", accountId = 1, investmentId = 1),
                Tx(0, TxType.INVEST_IN, 9_000_000, today.minusMonths(5), "Aporte", accountId = 1, investmentId = 2),
            ),
        )
        val done = IncomePlanner.plan(ledger(s), PlanInputs(500_000, 0, false))
        assertEquals("ações não contam como reserva", 1_500_000L, done.reserveNow)
        assertTrue(done.reserveDone)
        assertNull(done.reserveMonths)
        assertEquals(listOf(SavingSlice(SavingKind.GOALS, 50_000), SavingSlice(SavingKind.LONG_TERM, 50_000)), done.saving)
    }

    @Test fun semSobraNaoHaParaOndeGuardar() {
        val p = IncomePlanner.plan(ledger(), PlanInputs(500_000, 0, false, needsPct = 70, wantsPct = 30))
        assertEquals(0L, p.save)
        assertTrue(p.saving.isEmpty())
        assertNull(p.reserveMonths)
    }

    @Test fun pessoaNovaSemMesCompletoUsaOMesDeAgora() {
        // Só o mês de agora (até o dia 12): R$ 400 no mercado viram cerca de R$ 1.000 no mês de 30 dias.
        val s = Snapshot(
            accounts = listOf(acc), categories = listOf(market, salary),
            txs = listOf(Tx(1, TxType.EXPENSE, 40_000, today.withDayOfMonth(3), "Mercado", categoryId = 1, accountId = 1)),
        )
        val p = IncomePlanner.plan(Ledger(s, today), PlanInputs(300_000, 0, false))
        assertEquals(100_000L, p.categories.single().typical)
    }

    @Test fun escolhasFicamGuardadas() {
        val store = IncomePlanStore(ApplicationProvider.getApplicationContext(), sandbox = true)
        store.clear()
        val guess = IncomeGuess(fixed = 500_000, variableAvg = 60_000, variableLow = 20_000, variableHigh = 150_000, variableMonths = 3)
        // Sem escolha: o que o Cryo achou (a variável começa pelo mês mais fraco) e 50/30/20.
        assertEquals(PlanInputs(500_000, 20_000, false, 50, 30), store.inputs(guess))
        store.setFixed(450_000); store.setVariable(80_000); store.setUseVariable(true); store.setSplit(60, 50)
        // Essencial 60% e o estilo de vida no máximo o que sobra (40%).
        assertEquals(PlanInputs(450_000, 80_000, true, 60, 40), store.inputs(guess))
        store.resetIncome(); store.resetSplit()
        assertEquals(PlanInputs(500_000, 20_000, true, 50, 30), store.inputs(guess))
        store.setBucket(3, Bucket.NEEDS); store.setBucket(1, Bucket.WANTS); store.setBucket(3, Bucket.WANTS)
        assertEquals(mapOf(3L to Bucket.WANTS, 1L to Bucket.WANTS), store.buckets())
        store.clear()
        assertTrue(store.buckets().isEmpty())
    }
}
