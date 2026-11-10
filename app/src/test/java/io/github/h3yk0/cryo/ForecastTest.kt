// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo

import io.github.h3yk0.cryo.data.db.Account
import io.github.h3yk0.cryo.data.db.AccountType
import io.github.h3yk0.cryo.data.db.Budget
import io.github.h3yk0.cryo.data.db.Investment
import io.github.h3yk0.cryo.data.db.InvestmentYield
import io.github.h3yk0.cryo.data.db.Tx
import io.github.h3yk0.cryo.data.db.TxType
import io.github.h3yk0.cryo.domain.Ledger
import io.github.h3yk0.cryo.domain.Snapshot
import io.github.h3yk0.cryo.domain.engine.Pacing
import io.github.h3yk0.cryo.domain.engine.Rng
import io.github.h3yk0.cryo.domain.predict.CategoryForecasts
import io.github.h3yk0.cryo.domain.predict.InvestForecasts
import io.github.h3yk0.cryo.domain.engine.FinanceEngine
import io.github.h3yk0.cryo.domain.engine.Outlook
import io.github.h3yk0.cryo.domain.engine.Recurrences
import io.github.h3yk0.cryo.domain.engine.SpendingModel
import io.github.h3yk0.cryo.domain.predict.DailySeries
import io.github.h3yk0.cryo.domain.predict.Ensemble
import io.github.h3yk0.cryo.domain.predict.Forecaster
import io.github.h3yk0.cryo.domain.ym
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import kotlin.math.abs

/**
 * RF-PRV-09: o previsor novo conferido contra o passado, sempre que os testes rodam. A régua é o motor da 1.5
 * ([Outlook.month]): o novo só fica no app enquanto errar menos e a faixa dele cumprir o que promete. A tabela completa
 * (9 mundos, mais sementes) é a do [ForecastLabTest].
 */
class ForecastTest {
    private val end = LocalDate.of(2026, 10, 31)

    @Before fun strings() = JvmStrings.install()

    @Test fun erraMenosQueOMotorDa15EAFaixaCumpreOQuePromete() {
        var old = 0.0; var new = 0.0; var inside = 0; var n = 0
        for (kind in listOf(ForecastWorld.Kind.STEADY, ForecastWorld.Kind.PAYDAY, ForecastWorld.Kind.SHIFT, ForecastWorld.Kind.LUMPY, ForecastWorld.Kind.SAVER)) {
            val full = ForecastWorld.generate(ForecastWorld.spec(kind), end, 97L + kind.ordinal)
            val actual = Ledger(full, end)
            for (back in 1..6) {
                val ym = end.ym().minusMonths(back.toLong())
                val real = actual.monthSummary(ym).result
                for (day in listOf(8, 16, 24)) {
                    val today = ym.atDay(day)
                    val snap = Personas.asOf(full, today)
                    val rec = Recurrences.detect(snap.txs, today)
                    val seed = FinanceEngine.seedOf(snap, today)
                    old += abs(Outlook.month(Ledger(snap, today), rec, seed, scenarios = 400).mid - real)
                    val o = Forecaster.month(Ledger(snap, today), rec, seed, scenarios = 400)
                    new += abs(o.mid - real)
                    if (real in o.low..o.high) inside++
                    n++
                }
            }
        }
        assertTrue("erro novo ${new / n} x 1.5 ${old / n}", new < old)
        val cover = inside * 100.0 / n
        assertTrue("a faixa de 80% conteve $cover%", cover in 68.0..93.0)
    }

    @Test fun mesmosDadosMesmaPrevisaoEComAConferenciaDoPassado() {
        val full = ForecastWorld.generate(ForecastWorld.spec(ForecastWorld.Kind.STEADY), end, 5)
        val today = LocalDate.of(2026, 10, 12)
        val snap = Personas.asOf(full, today)
        val rec = Recurrences.detect(snap.txs, today)
        val a = Forecaster.month(Ledger(snap, today), rec, 7)
        val b = Forecaster.month(Ledger(snap, today), rec, 7)
        assertEquals(a, b)
        assertTrue(a.low <= a.mid && a.mid <= a.high)
        val c = a.calibration!!
        assertTrue(c.months >= 6 && c.hits in 0..c.months && c.typicalError > 0)
        assertEquals(1.0, a.models.sumOf { it.weight }, 0.01)
    }

    /** Pouco ou nenhum histórico: responde sem quebrar, com a faixa larga e sem conferência do passado. */
    @Test fun semHistoricoNaoQuebra() {
        val full = ForecastWorld.generate(ForecastWorld.spec(ForecastWorld.Kind.STEADY).copy(months = 1), end, 3)
        for (day in listOf(1, 2, 10, 31)) {
            val today = end.withDayOfMonth(day)
            val snap = Personas.asOf(full, today)
            val o = Forecaster.month(Ledger(snap, today), Recurrences.detect(snap.txs, today), 1)
            assertTrue(o.low <= o.mid && o.mid <= o.high)
            assertEquals(null, o.calibration)
        }
        val empty = full.copy(txs = emptyList(), yields = emptyList())
        val o = Forecaster.month(Ledger(empty, end), emptyList(), 1)
        assertTrue(o.low <= o.high)
    }

    /** A época do ano só vale quando se repetiu em dois anos: dezembro caro duas vezes melhora a previsão do terceiro. */
    @Test fun epocaQueSeRepetiuEmDoisAnosEntraNaPrevisao() {
        val end = LocalDate.of(2026, 12, 31)
        var with = 0.0; var without = 0.0; var used = 0
        for (seed in 1L..3L) {
            val full = ForecastWorld.generate(ForecastWorld.spec(ForecastWorld.Kind.SEASONAL).copy(months = 40), end, seed)
            val include = SpendingModel.spending(emptySet())
            for (day in listOf(5, 12, 19)) {
                val today = LocalDate.of(2026, 12, day)
                val series = DailySeries.of(full.txs, today, include)
                val real = full.txs.filter { it.date.isAfter(today) && include(it) }.sumOf { it.amount }.toDouble()
                val a = Ensemble.forecast(series, series.n - 1, 31 - day)
                val b = Ensemble.forecast(series, series.n - 1, 31 - day, useSeason = false)
                with += abs(a.point - real); without += abs(b.point - real)
                if (a.season > 1.0) used++
            }
        }
        assertTrue("usou em $used de 9", used >= 5)
        assertTrue("com época $with x sem $without", with < without)
        // Num mundo sem épocas, o fator fica calado.
        val steady = ForecastWorld.generate(ForecastWorld.spec(ForecastWorld.Kind.STEADY).copy(months = 40), end, 9)
        var noise = 0
        for (m in 1..12) for (day in listOf(5, 15, 25)) {
            val today = LocalDate.of(2026, m, day)
            val series = DailySeries.of(steady.txs, today, SpendingModel.spending(emptySet()))
            if (Ensemble.forecast(series, series.n - 1, today.lengthOfMonth() - day).season != 1.0) noise++
        }
        assertTrue("alarmes falsos: $noise de 36", noise <= 2)
    }

    /** RF-PRV-06: o fechamento do mês por categoria erra menos que o ritmo da 1.5 ([Pacing.projected]). */
    @Test fun categoriasFechamOMesComMenosErroQueORitmoDa15() {
        var old = 0.0; var new = 0.0; var n = 0
        for (kind in listOf(ForecastWorld.Kind.STEADY, ForecastWorld.Kind.TREND, ForecastWorld.Kind.FREELANCER)) {
            val full = ForecastWorld.generate(ForecastWorld.spec(kind), end, 97L + kind.ordinal)
            val actual = Ledger(full, end)
            for (back in 1..5) {
                val ym = end.ym().minusMonths(back.toLong())
                for (day in listOf(8, 20)) {
                    val today = ym.atDay(day)
                    val l = Ledger(Personas.asOf(full, today), today)
                    val list = CategoryForecasts.of(l, Recurrences.detect(l.s.txs, today), 1)
                    assertEquals(list, CategoryForecasts.of(Ledger(l.s, today), Recurrences.detect(l.s.txs, today), 1))
                    for (o in list) {
                        val real = actual.categorySpent(ym, o.categoryId)
                        old += abs(Pacing.projected(l, o.categoryId, o.spent) - real)
                        new += abs(o.mid - real)
                        assertTrue(o.spent <= o.low && o.low <= o.mid && o.mid <= o.high)
                        n++
                    }
                }
            }
        }
        assertTrue("categorias: novo ${new / n} x 1.5 ${old / n}", new < old)
    }

    /**
     * ERR-037: o mercado que se gasta todo no começo do mês não pode virar alarme no dia 6 (o conjunto dava 61% de chance de
     * passar, porque uma fatia fixa de peso ia para os modelos que erravam feio). E o lazer gasto rápido demais avisa.
     */
    @Test fun mercadoDoComecoDoMesNaoDaAlarmeFalso() {
        val today = LocalDate.of(2026, 10, 6)
        val txs = ArrayList<Tx>()
        var id = 1L
        for (m in 0 until 6) {
            val ym = java.time.YearMonth.of(2026, 4).plusMonths(m.toLong())
            for (d in 1..4) txs += Tx(id++, TxType.EXPENSE, 20000, ym.atDay(d), "Supermercado", categoryId = 2, accountId = 1)
            for (d in listOf(8, 16, 24)) txs += Tx(id++, TxType.EXPENSE, 5000, ym.atDay(d), "Lazer", categoryId = 4, accountId = 1)
        }
        for (d in 1..4) txs += Tx(id++, TxType.EXPENSE, 19000, LocalDate.of(2026, 10, d), "Supermercado", categoryId = 2, accountId = 1)
        txs += Tx(id++, TxType.EXPENSE, 9000, LocalDate.of(2026, 10, 3), "Lazer", categoryId = 4, accountId = 1)
        txs += Tx(id++, TxType.EXPENSE, 9000, LocalDate.of(2026, 10, 5), "Lazer", categoryId = 4, accountId = 1)
        val s = Snapshot(categories = Personas.categories, txs = txs, budgets = listOf(Budget(2, 82000), Budget(4, 20000)))
        val l = Ledger(s, today)
        val list = CategoryForecasts.of(l, Recurrences.detect(s.txs, today), 1)
        val market = list.first { it.categoryId == 2L }
        assertTrue("mercado: $market", market.mid <= 82000 && market.chanceOver < 0.1)
        val alerts = Pacing.alerts(l, list.associateBy { it.categoryId })
        assertTrue("sem alarme no mercado: $alerts", alerts.none { it.categoryId == 2L })
        assertTrue(alerts.first { it.categoryId == 4L }.crossDay in 7..20)
    }

    /** RF-PRV-06: com limite, sai a chance de passar; o que já passou é certeza, o que está longe é quase zero. */
    @Test fun chanceDePassarDoLimite() {
        val full = ForecastWorld.generate(ForecastWorld.spec(ForecastWorld.Kind.STEADY), end, 11)
        val today = LocalDate.of(2026, 10, 15)
        val base = Personas.asOf(full, today)
        fun chance(limit: Long): Double {
            val snap = base.copy(budgets = listOf(Budget(categoryId = 2, limitAmount = limit)))
            val l = Ledger(snap, today)
            return CategoryForecasts.of(l, Recurrences.detect(snap.txs, today), 1).first { it.categoryId == 2L }.chanceOver
        }
        assertEquals(1.0, chance(100), 0.0)
        assertEquals(0.0, chance(100_000_000), 0.0)
        val mid = CategoryForecasts.of(Ledger(base, today), Recurrences.detect(base.txs, today), 1).first { it.categoryId == 2L }.mid
        assertTrue(chance(mid) in 0.2..0.8)
    }

    private fun investment(months: Int, rate: (Int) -> Double): Snapshot {
        val start = LocalDate.of(2024, 1, 1)
        val txs = ArrayList<Tx>(); val yields = ArrayList<InvestmentYield>()
        var value = 0.0
        for (i in 0 until months) {
            val d = start.plusMonths(i.toLong())
            if (i > 0) { val y = (value * rate(i)).toLong(); yields += InvestmentYield(i.toLong(), 1, d, y); value += y }
            txs += Tx(id = i + 1L, type = TxType.INVEST_IN, amount = 50_000, date = d, description = "Aporte", accountId = 1, investmentId = 1)
            value += 50_000
        }
        return Snapshot(
            accounts = listOf(Account(1, "Conta", AccountType.CHECKING, initialBalance = 10_000_000, color = 0, sortOrder = 1)),
            txs = txs, investments = listOf(Investment(1, "Tesouro", color = 0)), yields = yields,
        )
    }

    /** RF-PRV-07: rendimento certinho de 1% ao mês = rentabilidade de 1%, sem sobe e desce, sem queda, faixa estreita. */
    @Test fun investimentoDeRendaFixa() {
        val today = LocalDate.of(2026, 1, 15)
        val o = InvestForecasts.of(Ledger(investment(25) { 0.01 }, today), 1, 7)!!
        assertEquals(0.01, o.monthly, 0.0006)
        assertEquals(0.127, o.annual!!, 0.01)
        assertTrue(o.volatility!! < 0.005)
        assertEquals(0.0, o.maxDrawdown, 1e-9)
        assertEquals(50_000L, o.contribution)
        assertEquals(listOf(1, 3, 5), o.horizons.map { it.years })
        val h = o.horizons.last()
        // Valor futuro de verdade: o de hoje a 1% ao mês por 60 meses, mais 60 aportes.
        val fv = o.value * Math.pow(1.01, 60.0) + 50_000 * (Math.pow(1.01, 60.0) - 1) / 0.01
        assertEquals(fv, h.mid.toDouble(), fv * 0.03)
        assertTrue(h.putIn < h.low && h.low <= h.mid && h.mid <= h.high && h.high < h.mid * 1.1)
        assertEquals(o, InvestForecasts.of(Ledger(investment(25) { 0.01 }, today), 1, 7))
    }

    /** RF-PRV-07: com sobe e desce, aparecem a volatilidade, a maior queda e uma faixa que se abre com o tempo. */
    @Test fun investimentoComSobeEDesce() {
        val rng = Rng(42)
        val rates = DoubleArray(40) { 0.008 + 0.03 * rng.nextGaussian() }
        val today = LocalDate.of(2026, 12, 15)
        val o = InvestForecasts.of(Ledger(investment(36) { rates[it] }, today), 1, 7)!!
        assertTrue("ao ano ${o.volatility}", o.volatility!! in 0.06..0.16)
        assertTrue(o.maxDrawdown > 0.01 && o.negativeShare in 0.1..0.7)
        val (one, _, five) = o.horizons
        assertTrue((five.high - five.low).toDouble() / five.mid > (one.high - one.low).toDouble() / one.mid)
        assertEquals(0.5, five.chanceOf(five.mid), 0.06)
        assertEquals(0.02, five.chanceOf(five.high * 10), 0.0)
        assertEquals(0.98, five.chanceOf(1), 0.0)
        assertTrue(five.chanceOf(five.low) > five.chanceOf(five.high))
        // A carteira inteira (id nulo) com um investimento só dá o mesmo retrato e a mesma projeção (a tela mostra as duas).
        val all = InvestForecasts.of(Ledger(investment(36) { rates[it] }, today), null, 7)!!
        assertEquals(o.monthly, all.monthly, 1e-12)
        assertEquals(o.horizons, all.horizons)
    }

    /** RF-PRV-07: a rentabilidade é a do investimento, não a de quando a pessoa aportou (um aporte grande não muda a taxa). */
    @Test fun aporteGrandeNaoMudaARentabilidadeEPoucoHistoricoNaoProjeta() {
        val today = LocalDate.of(2026, 1, 15)
        val base = investment(25) { if (it % 2 == 0) 0.03 else -0.01 }
        val a = InvestForecasts.of(Ledger(base, today), 1, 7)!!
        // Um aporte 40 vezes maior no meio do caminho, com os rendimentos proporcionais ao saldo (a mesma taxa).
        val start = LocalDate.of(2024, 1, 1)
        val txs = ArrayList<Tx>(); val yields = ArrayList<InvestmentYield>(); var value = 0.0
        for (i in 0 until 25) {
            val d = start.plusMonths(i.toLong())
            if (i > 0) { val y = (value * (if (i % 2 == 0) 0.03 else -0.01)).toLong(); yields += InvestmentYield(i.toLong(), 1, d, y); value += y }
            val amount = if (i == 12) 2_000_000L else 50_000L
            txs += Tx(id = i + 1L, type = TxType.INVEST_IN, amount = amount, date = d, description = "Aporte", accountId = 1, investmentId = 1)
            value += amount
        }
        val b = InvestForecasts.of(Ledger(base.copy(txs = txs, yields = yields), today), 1, 7)!!
        assertEquals(a.monthly, b.monthly, 0.0003)
        // Poucos rendimentos lançados: mostra o retrato, mas não projeta nem fala de sobe e desce.
        val few = InvestForecasts.of(Ledger(investment(4) { 0.01 }, LocalDate.of(2024, 4, 15)), 1, 7)!!
        assertTrue(few.horizons.isEmpty() && few.volatility == null && few.periods == 3)
        assertEquals(null, InvestForecasts.of(Ledger(base.copy(txs = emptyList(), yields = emptyList()), today), 1, 7))
    }
}
