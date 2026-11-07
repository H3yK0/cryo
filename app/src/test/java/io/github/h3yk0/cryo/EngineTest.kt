// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo

import io.github.h3yk0.cryo.data.db.Account
import io.github.h3yk0.cryo.data.db.Bill
import io.github.h3yk0.cryo.data.db.Budget
import io.github.h3yk0.cryo.data.db.CreditCard
import io.github.h3yk0.cryo.data.db.Debt
import io.github.h3yk0.cryo.data.db.Goal
import io.github.h3yk0.cryo.data.db.Investment
import io.github.h3yk0.cryo.data.db.InvestmentYield
import io.github.h3yk0.cryo.data.db.Tx
import io.github.h3yk0.cryo.data.db.TxType
import io.github.h3yk0.cryo.domain.CardMath
import io.github.h3yk0.cryo.domain.Ledger
import io.github.h3yk0.cryo.domain.Snapshot
import io.github.h3yk0.cryo.domain.engine.Anomalies
import io.github.h3yk0.cryo.domain.engine.AnomalyKind
import io.github.h3yk0.cryo.domain.engine.CategoryModel
import io.github.h3yk0.cryo.domain.engine.DebtPlanner
import io.github.h3yk0.cryo.domain.engine.EngineOptions
import io.github.h3yk0.cryo.domain.engine.EngineRunner
import io.github.h3yk0.cryo.domain.engine.FinanceEngine
import io.github.h3yk0.cryo.domain.engine.GoalPlanner
import io.github.h3yk0.cryo.domain.engine.HealthKind
import io.github.h3yk0.cryo.domain.engine.HealthScore
import io.github.h3yk0.cryo.domain.engine.Outlook
import io.github.h3yk0.cryo.domain.engine.Pacing
import io.github.h3yk0.cryo.domain.engine.PayoffStrategy
import io.github.h3yk0.cryo.domain.engine.Projections
import io.github.h3yk0.cryo.domain.engine.RecurrencePeriod
import io.github.h3yk0.cryo.domain.engine.RecurrenceStatus
import io.github.h3yk0.cryo.domain.engine.Recurrences
import io.github.h3yk0.cryo.domain.engine.Returns
import io.github.h3yk0.cryo.domain.engine.Rng
import io.github.h3yk0.cryo.domain.engine.Stats
import io.github.h3yk0.cryo.domain.engine.TxKey
import io.github.h3yk0.cryo.domain.forecast
import io.github.h3yk0.cryo.domain.key
import io.github.h3yk0.cryo.domain.ym
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.abs

/** Testes do motor financeiro, um (ou mais) por requisito RF-MOT. */
class EngineTest {
    init { JvmStrings.install() }

    private val end = LocalDate.of(2026, 10, 31)

    /* ------------------------------- RF-MOT-01 ------------------------------- */

    @Test fun estatisticaRobusta() {
        val xs = doubleArrayOf(10.0, 12.0, 11.0, 13.0, 9.0)
        assertEquals(11.0, Stats.median(xs), 1e-9)
        // Um valor 100× maior quase não muda a mediana (a média dispara).
        val withOutlier = doubleArrayOf(10.0, 12.0, 11.0, 13.0, 9.0, 1100.0, 10.5)
        assertEquals(11.0, Stats.median(withOutlier), 1e-9)
        assertTrue(Stats.mean(withOutlier) > 150)
        assertEquals(2.5, Stats.quantile(doubleArrayOf(1.0, 2.0, 3.0, 4.0), 0.5), 1e-9)
        assertEquals(1.0, Stats.mad(doubleArrayOf(1.0, 2.0, 3.0, 4.0, 5.0)), 1e-9)
        assertEquals(0.0, Stats.median(DoubleArray(0)), 0.0)
        assertEquals(2.0, Stats.theilSen(doubleArrayOf(0.0, 1.0, 2.0, 3.0, 4.0), doubleArrayOf(1.0, 3.0, 5.0, 7.0, 100.0)), 1e-9)
        val w = Stats.winsorize(doubleArrayOf(1.0, 2.0, 3.0, 4.0, 1000.0), 0.0, 0.75)
        assertEquals(4.0, w.max(), 1e-9)
        assertEquals(0.5, Stats.decay(90, 90.0), 1e-9)
    }

    @Test fun sorteioComSementeERepetivel() {
        val a = Rng(42); val b = Rng(42); val c = Rng(43)
        val sa = List(100) { a.nextLong() }
        assertEquals(sa, List(100) { b.nextLong() })
        assertFalse(sa == List(100) { c.nextLong() })
        val r = Rng(7)
        repeat(10_000) { val d = r.nextDouble(); assertTrue(d >= 0.0 && d < 1.0) }
        val mean = List(20_000) { r.nextGaussian() }.average()
        assertTrue("média da normal = $mean", abs(mean) < 0.05)
    }

    @Test fun chaveDaDescricao() {
        assertEquals("netflix", TxKey.of("NETFLIX.COM 12/03"))
        assertEquals("netflix", TxKey.of("Netflix.com"))
        assertEquals("padaria pao quente", TxKey.of("COMPRA DEBITO Padaria Pão Quente 05/11"))
        assertEquals("", TxKey.of("PIX 123456"))
        assertEquals("maria silva", TxKey.of("PIX ENVIADO MARIA SILVA"))
        // Outras escritas continuam inteiras.
        assertEquals("スターバックス", TxKey.of("スターバックス 12/03"))
        assertEquals(TxKey.of("x".repeat(10_000)).length <= TxKey.MAX_INPUT, true)
    }

    /* ------------------------------- RF-MOT-02 ------------------------------- */

    @Test fun recorrenciasSemFalsoPositivo() {
        val s = Personas.generate(Personas.spec(Personas.Kind.STABLE), end, 11)
        val rec = Recurrences.detect(s.txs, end)
        val keys = rec.map { it.key }.toSet()
        assertTrue(keys.toString(), "salario acme" in keys)
        assertTrue(keys.toString(), "netflix" in keys)
        assertTrue(keys.toString(), "spotify premium" in keys)
        assertTrue(keys.toString(), "academia corpo forte" in keys)
        assertTrue(keys.toString(), "conta luz energia" in keys)
        assertTrue(keys.toString(), "aluguel" in keys)
        // Nenhuma das compras soltas do dia a dia.
        val shops = listOf("padaria", "restaurante", "lanchonete", "supermercado", "mercadinho", "hortifruti", "uber", "posto", "cinema", "bar", "livraria", "farmacia", "loja")
        val wrong = keys.filter { k -> shops.any { k.startsWith(it) } }
        assertEquals("compras soltas tomadas como recorrentes", emptyList<String>(), wrong)
        val salary = rec.first { it.key == "salario acme" }
        assertEquals(RecurrencePeriod.MONTHLY, salary.period)
        assertEquals(TxType.INCOME, salary.type)
        assertEquals(520000L, salary.amount)
        assertEquals(5, salary.anchorDay)
        assertEquals(LocalDate.of(2026, 11, 5), salary.next)
        val light = rec.first { it.key == "conta luz energia" }
        assertTrue(light.variable)
        val rent = rec.first { it.key == "aluguel" }
        assertEquals(1L, rent.billId)
        // A Netflix subiu no mês 20 de 24: o aumento já foi há 4 meses, então não é mais "novo".
        assertEquals(4490L, rec.first { it.key == "netflix" }.amount)
    }

    @Test fun precoNovoEAtraso() {
        val txs = (0 until 8).map { i ->
            val d = YearMonth.of(2026, 3).plusMonths(i.toLong()).atDay(15)
            Tx(id = i + 1L, type = TxType.EXPENSE, amount = if (i == 7) 4490 else 3990, date = d, description = "NETFLIX.COM", accountId = 1)
        } + (0 until 6).map { i ->
            val d = YearMonth.of(2026, 4).plusMonths(i.toLong()).atDay(5)
            Tx(id = 100 + i.toLong(), type = TxType.INCOME, amount = 300000, date = d, description = "Salário", accountId = 1)
        }
        val today = LocalDate.of(2026, 10, 16)
        val rec = Recurrences.detect(txs, today)
        val nf = rec.first { it.key == "netflix" }
        assertEquals(500L, nf.priceChange)
        assertEquals(4490L, nf.amount)
        // Salário veio de abril a setembro, dia 5; em 16/10 está atrasado.
        val sal = rec.first { it.key == "salario" }
        assertEquals(RecurrenceStatus.LATE, sal.status)
        assertEquals(LocalDate.of(2026, 10, 5), sal.next)
    }

    /* ------------------------------- RF-MOT-03 ------------------------------- */

    /**
     * Backtest: a previsão de cada mês, nos dias 5, 10, 15, 20 e 25, comparada com o que aconteceu de verdade, em 4 perfis.
     * Precisa errar menos que a previsão da 1.4 e a faixa de 80% precisa conter o real em 70% a 90% das vezes.
     */
    @Test fun backtestDaPrevisao() {
        var oldErr = 0.0; var newErr = 0.0; var inside = 0; var total = 0
        for (kind in listOf(Personas.Kind.STABLE, Personas.Kind.IRREGULAR, Personas.Kind.INDEBTED)) {
            for (seed in 1L..3L) {
                val full = Personas.generate(Personas.spec(kind), end, seed * 31 + kind.ordinal)
                val actual = Ledger(full, end)
                for (back in 1..10) {
                    val ym = end.ym().minusMonths(back.toLong())
                    val real = actual.monthSummary(ym).result
                    for (day in listOf(5, 10, 15, 20, 25)) {
                        val today = ym.atDay(day)
                        val snap = Personas.asOf(full, today)
                        val l = Ledger(snap, today)
                        val rec = Recurrences.detect(snap.txs, today)
                        val o = Outlook.month(l, rec, FinanceEngine.seedOf(snap, today), scenarios = 600)
                        oldErr += abs(l.forecast().projectedResult - real)
                        newErr += abs(o.mid - real)
                        if (real in o.low..o.high) inside++
                        total++
                    }
                }
            }
        }
        val coverage = inside.toDouble() / total
        println("backtest: erro médio 1.4 = ${oldErr / total / 100}, motor = ${newErr / total / 100}, cobertura = $coverage ($total casos)")
        assertTrue("o motor precisa errar menos (1.4: ${oldErr / total}, motor: ${newErr / total})", newErr < oldErr)
        assertTrue("cobertura da faixa de 80% = $coverage", coverage in 0.70..0.90)
    }

    @Test fun previsaoComFaixaEmOrdem() {
        val s = Personas.generate(Personas.spec(Personas.Kind.STABLE), end, 5)
        val today = LocalDate.of(2026, 10, 12)
        val snap = Personas.asOf(s, today)
        val l = Ledger(snap, today)
        val o = Outlook.month(l, Recurrences.detect(snap.txs, today), 99)
        assertTrue(o.low <= o.mid && o.mid <= o.high)
        assertEquals(o.base.daysInMonth - today.dayOfMonth + 1, o.band.size)
        o.band.forEach { assertTrue(it.low <= it.mid && it.mid <= it.high) }
        assertTrue(o.chanceNegative in 0.0..1.0)
        // Mesmos dados, mesma semente: mesmo resultado.
        assertEquals(o, Outlook.month(l, Recurrences.detect(snap.txs, today), 99))
    }

    @Test fun salarioQueAindaVaiVirEntraNaPrevisao() {
        val s = Personas.generate(Personas.spec(Personas.Kind.STABLE), end, 8)
        val today = LocalDate.of(2026, 10, 2) // o salário cai no dia 5
        val snap = Personas.asOf(s, today)
        val l = Ledger(snap, today)
        val o = Outlook.month(l, Recurrences.detect(snap.txs, today), 1)
        assertTrue(o.expected.any { it.recurrence.key == "salario acme" })
        assertTrue("a 1.4 não via o salário: ${l.forecast().projectedResult}; o motor: ${o.mid}", o.mid > l.forecast().projectedResult + 300000)
    }

    /* ------------------------------- RF-MOT-04 ------------------------------- */

    @Test fun proximosMesesComFaturasParcelasEContas() {
        val today = LocalDate.of(2026, 11, 1)
        val card = CreditCard(1, "C", limitAmount = 500000, closingDay = 3, dueDay = 10, color = 0, payAccountId = 1)
        val s = Snapshot(
            accounts = listOf(Account(1, "Conta", initialBalance = 100000, color = 0)),
            cards = listOf(card),
            bills = listOf(Bill(1, "Aluguel", 50000, dueDay = 20, accountId = 1, startYm = 202611)),
            txs = listOf(
                // compra parcelada: 3 parcelas de 10.000 nas faturas de nov, dez e jan
                Tx(1, TxType.EXPENSE, 10000, LocalDate.of(2026, 10, 20), "TV 1/3", cardId = 1, invoiceYm = 202611, installmentGroup = "g", installmentNumber = 1, installmentTotal = 3),
                Tx(2, TxType.EXPENSE, 10000, LocalDate.of(2026, 11, 20), "TV 2/3", cardId = 1, invoiceYm = 202612, installmentGroup = "g", installmentNumber = 2, installmentTotal = 3),
                Tx(3, TxType.EXPENSE, 10000, LocalDate.of(2026, 12, 20), "TV 3/3", cardId = 1, invoiceYm = 202701, installmentGroup = "g", installmentNumber = 3, installmentTotal = 3),
                Tx(4, TxType.INCOME, 30000, LocalDate.of(2026, 11, 25), "Freela agendado", accountId = 1),
            ),
        )
        val p = Projections.project(Ledger(s, today), emptyList(), 1, months = 3, scenarios = 50)
        fun at(d: LocalDate) = p.mid[java.time.temporal.ChronoUnit.DAYS.between(today, d).toInt()]
        assertEquals(100000L, at(today))
        assertEquals(90000L, at(LocalDate.of(2026, 11, 10))) // fatura de novembro
        assertEquals(40000L, at(LocalDate.of(2026, 11, 20))) // aluguel
        assertEquals(70000L, at(LocalDate.of(2026, 11, 25))) // freela
        assertEquals(60000L, at(LocalDate.of(2026, 12, 10))) // fatura de dezembro
        assertEquals(10000L, at(LocalDate.of(2026, 12, 20))) // aluguel
        assertEquals(0L, at(LocalDate.of(2027, 1, 10))) // última parcela
        assertEquals(-50000L, at(LocalDate.of(2027, 1, 20))) // aluguel de janeiro: fica negativo
        assertEquals(LocalDate.of(2027, 1, 20), p.tightestDate)
        assertEquals(YearMonth.of(2027, 1), p.firstNegativeMonth)
        assertEquals(1.0, p.chanceNegative, 0.0)
    }

    @Test fun projecaoComHistoricoTemFaixaQueAbre() {
        val s = Personas.generate(Personas.spec(Personas.Kind.STABLE), end, 3)
        val p = Projections.project(Ledger(s, end), Recurrences.detect(s.txs, end), 4, months = 12)
        val spread30 = p.high[30] - p.low[30]
        val spread300 = p.high[300] - p.low[300]
        assertTrue("a faixa abre com o tempo ($spread30 → $spread300)", spread300 > spread30)
        for (i in p.mid.indices) assertTrue(p.low[i] <= p.mid[i] && p.mid[i] <= p.high[i])
        assertEquals(13, p.monthEnds.size)
    }

    /* ------------------------------- RF-MOT-05 ------------------------------- */

    @Test fun foraDoPadraoSemAlarmeFalso() {
        val s = Personas.generate(Personas.spec(Personas.Kind.STABLE).copy(bigEvery = 0), end, 21)
        val l = Ledger(s, end)
        val normal = Anomalies.find(l, Recurrences.detect(s.txs, end)).filter { it.kind != AnomalyKind.PRICE_UP }
        assertEquals("alarmes num histórico normal: $normal", 0, normal.size)
        // Uma compra fora da curva e uma cobrança em dobro.
        val extra = listOf(
            Tx(900001, TxType.EXPENSE, 95000, end.minusDays(3), "Restaurante Sabor Caseiro", categoryId = 1, accountId = 1),
            Tx(900002, TxType.EXPENSE, 23450, end.minusDays(2), "Loja Moda Única", categoryId = 9, accountId = 1),
            Tx(900003, TxType.EXPENSE, 23450, end.minusDays(1), "Loja Moda Única", categoryId = 9, accountId = 1),
        )
        val s2 = s.copy(txs = s.txs + extra)
        val found = Anomalies.find(Ledger(s2, end), Recurrences.detect(s2.txs, end))
        assertTrue(found.toString(), found.any { it.kind == AnomalyKind.OUTLIER && it.txId == 900001L })
        assertTrue(found.toString(), found.any { it.kind == AnomalyKind.DUPLICATE && it.txId == 900003L && it.otherTxId == 900002L })
    }

    /* ------------------------------- RF-MOT-06 ------------------------------- */

    @Test fun saudeFinanceiraParteAParte() {
        val s = Personas.generate(Personas.spec(Personas.Kind.STABLE), end, 2)
        val h = HealthScore.of(Ledger(s, end))
        assertTrue(h.ready)
        assertEquals(6, h.parts.size)
        assertEquals(h.parts.sumOf { it.points }.let { Math.round(it).toInt() }, h.score)
        h.parts.forEach { assertTrue(it.points >= 0 && it.points <= it.kind.max) }
        // Sem cartão: nota cheia no cartão. Sem atrasos: nota cheia em "em dia".
        assertEquals(10.0, h.parts.first { it.kind == HealthKind.ON_TIME }.points, 1e-9)
        // Pouco tempo de dados: ainda conhecendo.
        val newbie = Personas.generate(Personas.spec(Personas.Kind.NEWCOMER), end, 2)
        assertFalse(HealthScore.of(Ledger(Personas.asOf(newbie, end), end)).ready)
    }

    @Test fun saudeComContaAtrasadaPerdePontos() {
        val s = Personas.generate(Personas.spec(Personas.Kind.STABLE), end, 2)
        val late = s.copy(bills = s.bills + Bill(9, "Internet", 9900, dueDay = 3, accountId = 1, startYm = 202609))
        val h = HealthScore.of(Ledger(late, end))
        // Internet sem pagamento em setembro, outubro (e novembro ainda não venceu): 2 atrasos = −8 pontos.
        assertEquals(2.0, h.parts.first { it.kind == HealthKind.ON_TIME }.points, 1e-9)
    }

    /* ------------------------------- RF-MOT-07 ------------------------------- */

    @Test fun ritmoDaCategoriaSemAlarmeNoComeco() {
        val today = LocalDate.of(2026, 10, 6)
        val txs = ArrayList<Tx>()
        var id = 1L
        // Mercado: tudo no começo do mês (dias 1 a 4), ~R$ 800.
        for (m in 0 until 6) {
            val ym = YearMonth.of(2026, 4).plusMonths(m.toLong())
            for (d in 1..4) txs += Tx(id++, TxType.EXPENSE, 20000, ym.atDay(d), "Supermercado", categoryId = 2, accountId = 1)
            for (d in listOf(8, 16, 24)) txs += Tx(id++, TxType.EXPENSE, 5000, ym.atDay(d), "Lazer", categoryId = 4, accountId = 1)
        }
        for (d in 1..4) txs += Tx(id++, TxType.EXPENSE, 19000, LocalDate.of(2026, 10, d), "Supermercado", categoryId = 2, accountId = 1)
        // Lazer: gasto rápido demais neste mês.
        txs += Tx(id++, TxType.EXPENSE, 9000, LocalDate.of(2026, 10, 3), "Lazer", categoryId = 4, accountId = 1)
        txs += Tx(id++, TxType.EXPENSE, 9000, LocalDate.of(2026, 10, 5), "Lazer", categoryId = 4, accountId = 1)
        val s = Snapshot(categories = Personas.categories, txs = txs, budgets = listOf(Budget(2, 82000), Budget(4, 20000)))
        val alerts = Pacing.alerts(Ledger(s, today))
        assertTrue("mercado gasta tudo no começo: sem alarme. $alerts", alerts.none { it.categoryId == 2L })
        val lazer = alerts.first { it.categoryId == 4L }
        assertTrue(lazer.crossDay in 7..20)
    }

    @Test fun limiteSugeridoArredondado() {
        assertEquals(29000L, Pacing.niceCeil(28743.0))
        assertEquals(600L, Pacing.niceCeil(520.0))
        assertEquals(1300000L, Pacing.niceCeil(1234500.0))
        val s = Personas.generate(Personas.spec(Personas.Kind.STABLE), end, 2)
        val sug = Pacing.suggestions(Ledger(s, end))
        assertTrue(sug.isNotEmpty())
        sug.forEach { assertTrue(it.amount >= it.usual) }
    }

    /* ------------------------------- RF-MOT-08 ------------------------------- */

    @Test fun jurosEmbutidosTabelaPrice() {
        // R$ 10.000 em 12 parcelas a 2% ao mês: parcela de R$ 945,60.
        val i = DebtPlanner.impliedMonthlyRate(1_000_000.0, 94_560.0, 12)
        assertEquals(0.02, i, 0.0001)
        assertEquals(0.0, DebtPlanner.impliedMonthlyRate(1_200_000.0, 100_000.0, 12), 0.0)
        // 8% ao mês, 48 parcelas.
        val pv = DebtPlanner.presentValue(50_000.0, 48, 0.08)
        assertEquals(0.08, DebtPlanner.impliedMonthlyRate(pv, 50_000.0, 48), 1e-6)
    }

    @Test fun planoParaQuitarAvalancheEBolaDeNeve() {
        val today = LocalDate.of(2026, 11, 7)
        val debts = listOf(
            // cara: 1.200 hoje, 12 parcelas de 130 (≈ 1,3% a.m.)
            Debt(1, "Cartão parcelado", color = 0, initialBalance = 120000, installmentAmount = 13000, installmentCount = 12, startYm = 202612, dueDay = 10),
            // muito cara: 3.000 hoje, 24 parcelas de 220 (≈ 4,7% a.m.)
            Debt(2, "Empréstimo", color = 0, initialBalance = 300000, installmentAmount = 22000, installmentCount = 24, startYm = 202612, dueDay = 10),
        )
        val l = Ledger(Snapshot(debts = debts), today)
        val rates = DebtPlanner.rates(l).associateBy { it.debtId }
        assertTrue(rates.getValue(2).monthly > rates.getValue(1).monthly)
        val av = DebtPlanner.plan(l, extra = 20000, PayoffStrategy.AVALANCHE)!!
        val sb = DebtPlanner.plan(l, extra = 20000, PayoffStrategy.SNOWBALL)!!
        assertEquals(listOf(2L, 1L), av.order)
        assertEquals(listOf(1L, 2L), sb.order)
        assertNotNull(av.months)
        assertEquals(24, av.baselineMonths)
        assertTrue("avalanche paga menos juros", av.interest <= sb.interest)
        assertTrue(av.savedInterest > 0)
        assertTrue(av.months!! < 24)
        val ant = DebtPlanner.anticipate(l, 2, 3)!!
        assertEquals(66000L, ant.nominal)
        assertTrue(ant.saving > 0)
    }

    /* ------------------------------- RF-MOT-09 ------------------------------- */

    @Test fun rendimentoRealAoAno() {
        val start = LocalDate.of(2025, 11, 7)
        val today = LocalDate.of(2026, 11, 7)
        val s = Snapshot(
            investments = listOf(Investment(1, "CDB", color = 0)),
            txs = listOf(Tx(1, TxType.INVEST_IN, 100000, start, investmentId = 1, accountId = 1)),
            yields = listOf(InvestmentYield(1, 1, today, 10000)),
        )
        val r = Returns.of(Ledger(s, today), 1)!!
        assertEquals(0.10, r.annual!!, 1e-6)
        assertEquals(0.10, r.total, 1e-9)
        assertEquals(0.0476, r.real(0.05)!!, 1e-3)
        // Aportes mensais: a TIR com datas bate com a conta feita à mão (12 aportes de 1.000 a 1% a.m.).
        val monthly = (0 until 12).map { Tx(10L + it, TxType.INVEST_IN, 100000, start.plusMonths(it.toLong()), investmentId = 2) }
        var fv = 0.0
        for (k in 0 until 12) fv = (fv + 100000) * 1.01
        val s2 = Snapshot(investments = listOf(Investment(2, "Fundo", color = 0)), txs = monthly,
            yields = listOf(InvestmentYield(2, 2, today, Math.round(fv) - 1_200_000)))
        val r2 = Returns.of(Ledger(s2, today), 2)!!
        assertEquals(Math.pow(1.01, 12.0) - 1, r2.annual!!, 0.002)
        // Menos de 30 dias: sem taxa ao ano.
        assertNull(Returns.of(Ledger(s, start.plusDays(10)), 1)!!.annual)
    }

    /* ------------------------------- RF-MOT-10 ------------------------------- */

    @Test fun metaQuandoChegaLa() {
        val today = LocalDate.of(2026, 11, 7)
        val created = LocalDate.of(2026, 5, 1).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        val g = Goal(1, "Viagem", target = 600000, deadline = LocalDate.of(2027, 12, 31), color = 0, createdAt = created)
        val txs = (0 until 7).map { Tx(it + 1L, TxType.GOAL_IN, 50000, LocalDate.of(2026, 5, 6).plusMonths(it.toLong()), goalId = 1, accountId = 1) }
        val o = GoalPlanner.of(Ledger(Snapshot(goals = listOf(g), txs = txs), today), g, 1)
        assertEquals(350000L, o.saved)
        assertEquals(50000L, o.pace)
        assertEquals(YearMonth.of(2027, 4), o.etaYm)
        assertEquals(1.0, o.chanceByDeadline!!, 0.0)
        assertEquals((250000L + 13) / 14, o.neededMonthly)
        // Sem depósitos: explica (sem data), em vez de prever.
        val empty = GoalPlanner.of(Ledger(Snapshot(goals = listOf(g)), today), g, 1)
        assertNull(empty.etaYm)
        assertEquals(0.0, empty.chanceByDeadline!!, 0.0)
    }

    /* ------------------------------- RF-MOT-11 ------------------------------- */

    @Test fun percepcoesOrdenadasESemRepetir() {
        val s = Personas.generate(Personas.spec(Personas.Kind.INDEBTED), end, 4)
        val r = FinanceEngine.run(s, LocalDate.of(2026, 10, 18))
        assertTrue(r.insights.size in 1..5)
        assertEquals(r.insights.sortedByDescending { it.score }, r.insights)
        assertEquals(r.insights.size, r.insights.map { it.topic }.toSet().size)
        assertEquals(r.insights.size, r.insights.map { it.text }.toSet().size)
    }

    /* ------------------------------- RF-MOT-12 ------------------------------- */

    @Test fun categoriaAprendidaAcerta85PorCento() {
        val s = Personas.generate(Personas.spec(Personas.Kind.STABLE), end, 77)
        val labeled = s.txs.filter { (it.type == TxType.EXPENSE || it.type == TxType.INCOME) && it.categoryId != null && it.debtId == null }
        val test = labeled.filterIndexed { i, _ -> i % 5 == 0 }
        val train = labeled.filterIndexed { i, _ -> i % 5 != 0 }
        val model = CategoryModel.train(train, end)
        var right = 0; var answered = 0
        for (t in test) {
            val g = model.guess(t.type, t.description, t.amount, t.accountId, t.cardId) ?: continue
            answered++
            if (g.categoryId == t.categoryId) right++
        }
        assertTrue("respondeu $answered de ${test.size}", answered >= test.size * 0.9)
        assertTrue("acertou $right de $answered", right >= answered * 0.85)
        // Palavra nunca vista: não chuta.
        assertNull(model.guess(TxType.EXPENSE, "Joalheria Brilho Eterno", 50000, 1, null))
    }

    /* ------------------------------- RF-MOT-13 ------------------------------- */

    @Test fun cacheEDesempenho() = runBlocking {
        val s = Personas.generate(Personas.spec(Personas.Kind.STABLE).copy(months = 60, perDay = 4.5), end, 9)
        assertTrue("dados de teste: ${s.txs.size}", s.txs.size >= 9000)
        val runner = EngineRunner()
        FinanceEngine.run(s, end) // aquece a JVM
        val t0 = System.nanoTime()
        val a = runner.run(s, end, EngineOptions())
        val ms = (System.nanoTime() - t0) / 1_000_000
        println("motor com ${s.txs.size} movimentações: $ms ms")
        assertTrue("motor levou $ms ms", ms < 1500)
        val b = runner.run(s, end, EngineOptions())
        assertTrue(a === b)
        assertEquals(1, runner.computations)
        runner.run(s.copy(), end, EngineOptions())
        assertEquals(2, runner.computations)
    }

    @Test fun motorDeterministico() {
        val s = Personas.generate(Personas.spec(Personas.Kind.IRREGULAR), end, 12)
        assertEquals(FinanceEngine.run(s, end), FinanceEngine.run(s, end))
    }

    @Test fun motorComDadosVaziosNaoQuebra() {
        val r = FinanceEngine.run(Snapshot(), end)
        assertFalse(r.health.ready)
        assertTrue(r.recurrences.isEmpty())
        assertEquals(0L, r.outlook.mid)
        assertEquals(0L, r.projection.mid.last())
    }
}
