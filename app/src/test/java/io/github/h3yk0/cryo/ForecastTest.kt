// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo

import io.github.h3yk0.cryo.domain.Ledger
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
}
