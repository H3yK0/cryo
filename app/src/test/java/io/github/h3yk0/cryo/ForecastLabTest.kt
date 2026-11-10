// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo

import io.github.h3yk0.cryo.domain.Ledger
import io.github.h3yk0.cryo.domain.engine.FinanceEngine
import io.github.h3yk0.cryo.domain.engine.MonthOutlook
import io.github.h3yk0.cryo.domain.engine.Outlook
import io.github.h3yk0.cryo.domain.engine.Recurrences
import io.github.h3yk0.cryo.data.db.TxType
import io.github.h3yk0.cryo.domain.engine.SpendingModel
import io.github.h3yk0.cryo.domain.engine.Stats
import io.github.h3yk0.cryo.domain.predict.DailySeries
import io.github.h3yk0.cryo.domain.predict.Ensemble
import io.github.h3yk0.cryo.domain.predict.Forecaster
import io.github.h3yk0.cryo.domain.predict.ModelKind
import io.github.h3yk0.cryo.domain.predict.Models
import io.github.h3yk0.cryo.domain.ym
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.time.LocalDate
import kotlin.math.abs

/**
 * Laboratório da previsão (RF-PRV-09): a tabela completa do motor da 1.5 contra o previsor novo, mundo por mundo. Demora
 * alguns minutos, por isso só roda com `-PforecastLab=true`; o resultado fica em `app/build/forecast-lab.txt`.
 */
class ForecastLabTest {
    private val end = LocalDate.of(2026, 10, 31)

    @Before fun strings() = JvmStrings.install()

    /** `-PforecastLab=true` roda tudo; `-PforecastLab=SEASONAL,PAYDAY:6` roda só esses mundos, com 6 sementes. */
    private val arg = System.getProperty("cryo.forecastLab") ?: "false"
    private val worlds = arg.substringBefore(':').let { a -> if (a == "true" || a == "false") ForecastWorld.Kind.entries else a.split(',').map { ForecastWorld.Kind.valueOf(it) } }
    private val seeds = arg.substringAfter(':', "").toLongOrNull()

    private class Acc {
        var n = 0; var err = 0.0; var inside = 0; var width = 0.0
        fun add(o: MonthOutlook, real: Long) { n++; err += abs(o.mid - real); if (real in o.low..o.high) inside++; width += (o.high - o.low) }
        fun mae() = if (n == 0) 0.0 else err / n / 100
        fun cover() = if (n == 0) 0.0 else inside * 100.0 / n
        fun wid() = if (n == 0) 0.0 else width / n / 100
    }

    @Test fun tabela() {
        assumeTrue(arg != "false")
        val out = StringBuilder()
        val totals = Array(3) { Acc() }
        out.append(String.format("%-11s %5s | %9s %5s %8s | %9s %5s %8s | %9s %5s %8s\n", "mundo", "casos", "1.5 erro", "cob%", "largura", "novo s/c", "cob%", "largura", "novo", "cob%", "largura"))
        for (kind in worlds) {
            val acc = Array(3) { Acc() }
            val byMonth = java.util.TreeMap<String, Array<Acc>>()
            for (seed in 1L..(seeds ?: 2L)) {
                val full = ForecastWorld.generate(ForecastWorld.spec(kind), end, seed * 97 + kind.ordinal)
                val actual = Ledger(full, end)
                for (back in 1..10) {
                    val ym = end.ym().minusMonths(back.toLong())
                    val real = actual.monthSummary(ym).result
                    for (day in listOf(5, 10, 15, 20, 25)) {
                        val today = ym.atDay(day)
                        val snap = Personas.asOf(full, today)
                        val l = Ledger(snap, today)
                        val rec = Recurrences.detect(snap.txs, today)
                        val s = FinanceEngine.seedOf(snap, today)
                        val o = listOf(
                            Outlook.month(l, rec, s, scenarios = 600),
                            Forecaster.month(Ledger(snap, today), rec, s, scenarios = 600, calibrate = false),
                            Forecaster.month(Ledger(snap, today), rec, s, scenarios = 600),
                        )
                        val m = byMonth.getOrPut("$ym d" + day.toString().padStart(2, '0')) { Array(3) { Acc() } }
                        val mm = byMonth.getOrPut("$ym") { Array(3) { Acc() } }
                        for (i in 0..2) { acc[i].add(o[i], real); m[i].add(o[i], real); mm[i].add(o[i], real) }
                    }
                }
            }
            out.append(String.format("%-11s %5d | %9.2f %5.1f %8.2f | %9.2f %5.1f %8.2f | %9.2f %5.1f %8.2f\n", kind.name, acc[0].n,
                acc[0].mae(), acc[0].cover(), acc[0].wid(), acc[1].mae(), acc[1].cover(), acc[1].wid(), acc[2].mae(), acc[2].cover(), acc[2].wid()))
            for (i in 0..2) { totals[i].n += acc[i].n; totals[i].err += acc[i].err; totals[i].inside += acc[i].inside; totals[i].width += acc[i].width }
            if (seeds != null) for ((k, a) in byMonth) if (!k.contains(" d")) out.append(String.format("  %-9s %5d | %9.2f %5.1f %8.2f | %9.2f %5.1f %8.2f | %9.2f %5.1f %8.2f\n", k, a[0].n,
                a[0].mae(), a[0].cover(), a[0].wid(), a[1].mae(), a[1].cover(), a[1].wid(), a[2].mae(), a[2].cover(), a[2].wid()))
            if (seeds != null) for (d in listOf(5, 10, 15, 20, 25)) {
                val a = Array(3) { Acc() }
                for ((k, v) in byMonth) if (k.endsWith(" d" + d.toString().padStart(2, '0'))) for (i in 0..2) { a[i].n += v[i].n; a[i].err += v[i].err; a[i].inside += v[i].inside; a[i].width += v[i].width }
                out.append(String.format("  dia %-5d %5d | %9.2f %5.1f %8.2f | %9.2f %5.1f %8.2f | %9.2f %5.1f %8.2f\n", d, a[0].n,
                    a[0].mae(), a[0].cover(), a[0].wid(), a[1].mae(), a[1].cover(), a[1].wid(), a[2].mae(), a[2].cover(), a[2].wid()))
            }
        }
        out.append(String.format("%-11s %5d | %9.2f %5.1f %8.2f | %9.2f %5.1f %8.2f | %9.2f %5.1f %8.2f\n", "TODOS", totals[0].n,
            totals[0].mae(), totals[0].cover(), totals[0].wid(), totals[1].mae(), totals[1].cover(), totals[1].wid(), totals[2].mae(), totals[2].cover(), totals[2].wid()))
        File("build/forecast-lab.txt").writeText(out.toString())
    }

    /**
     * Cada modelo sozinho e o conjunto com vários ajustes, só na pergunta "quanto vai somar o gasto do dia a dia até o fim
     * do mês?" (sem recorrências nem cenários): é rápido, e mostra quem acerta o quê. Resultado em `build/forecast-models.txt`.
     */
    @Test fun modelos() {
        assumeTrue(arg != "false")
        val kinds = ModelKind.entries
        val sets = listOf("conjunto", "sem época")
        val cols = ArrayList<String>()
        cols += "1.5"
        kinds.forEach { cols += it.name.take(9) }
        sets.forEach { cols += it }
        val out = StringBuilder()
        out.append(String.format("%-11s %5s", "mundo", "casos")); cols.forEach { out.append(String.format(" %9s", it)) }; out.append("\n")
        val total = DoubleArray(cols.size); var totalN = 0
        fun row(name: String, n: Int, err: DoubleArray) {
            out.append(String.format("%-11s %5d", name, n)); err.forEach { out.append(String.format(" %9.2f", it / n / 100)) }; out.append("\n")
        }
        for (flow in listOf("gasto", "entrada")) {
            java.util.Arrays.fill(total, 0.0); totalN = 0
            out.append("== $flow ==\n")
            for (kind in worlds) {
                if (flow == "entrada" && kind != ForecastWorld.Kind.FREELANCER) continue
                val err = DoubleArray(cols.size); var n = 0
                for (seed in 1L..(seeds ?: 4L)) {
                    val full = ForecastWorld.generate(ForecastWorld.spec(kind), end, seed * 97 + kind.ordinal)
                    val include = if (flow == "gasto") SpendingModel.spending(emptySet()) else SpendingModel.extraIncome(emptySet())
                    for (back in 1..10) {
                        val ym = end.ym().minusMonths(back.toLong())
                        for (day in listOf(5, 10, 15, 20, 25)) {
                            val today = ym.atDay(day)
                            val snap = Personas.asOf(full, today)
                            val h = ym.lengthOfMonth() - day
                            val real = full.txs.filter { it.date.isAfter(today) && !it.date.isAfter(ym.atEndOfMonth()) && include(it) }.sumOf { it.amount }.toDouble()
                            val series = DailySeries.of(snap.txs, today, include)
                            val v = series.view(series.n - 1)
                            var c = 0
                            err[c++] += if (flow == "gasto") abs(Outlook.month(Ledger(snap, today), emptyList(), seed, 300).variableMid - real) else 0.0
                            val fallback = Models.forecast(ModelKind.PROFILE, v, h)
                            for (k in kinds) { val f = Models.forecast(k, v, h); err[c++] += abs((if (f.isFinite()) f else fallback) - real) }
                            for (name in sets) err[c++] += abs(Ensemble.forecast(series, series.n - 1, h, useSeason = name == "conjunto").point - real)
                            n++
                        }
                    }
                }
                row(kind.name, n, err)
                for (i in err.indices) total[i] += err[i]
                totalN += n
            }
            row("TODOS", totalN, total)
        }
        File("build/forecast-models.txt").writeText(out.toString())
    }

    /** Depuração da época do ano: quantas vezes entrou, com que tamanho, e o erro com e sem nesses casos. */
    @Test fun epoca() {
        assumeTrue(arg != "false")
        val out = StringBuilder()
        out.append(String.format("%-11s %5s %6s %7s %9s %9s %9s %9s\n", "mundo", "casos", "usou%", "|f-1|", "sem", "com", "sem(us.)", "com(us.)"))
        for (kind in worlds) {
            val r = DoubleArray(8)
            for (seed in 1L..(seeds ?: 4L)) {
                val full = ForecastWorld.generate(ForecastWorld.spec(kind), end, seed * 97 + kind.ordinal)
                val include = SpendingModel.spending(emptySet())
                for (back in 1..10) {
                    val ym = end.ym().minusMonths(back.toLong())
                    for (day in listOf(5, 10, 15, 20, 25)) {
                        val today = ym.atDay(day)
                        val snap = Personas.asOf(full, today)
                        val h = ym.lengthOfMonth() - day
                        val real = full.txs.filter { it.date.isAfter(today) && !it.date.isAfter(ym.atEndOfMonth()) && include(it) }.sumOf { it.amount }.toDouble()
                        val series = DailySeries.of(snap.txs, today, include)
                        val a = Ensemble.forecast(series, series.n - 1, h, useSeason = false)
                        val b = Ensemble.forecast(series, series.n - 1, h, useSeason = true)
                        r[0] += 1.0; r[1] += abs(a.point - real); r[2] += abs(b.point - real)
                        if (b.season != 1.0) { r[3] += 1.0; r[4] += abs(b.season - 1); r[5] += abs(a.point - real); r[6] += abs(b.point - real) }
                    }
                }
            }
            val u = maxOf(r[3], 1.0)
            out.append(String.format("%-11s %5.0f %6.1f %7.3f %9.2f %9.2f %9.2f %9.2f\n", kind.name, r[0], 100 * r[3] / r[0], r[4] / u, r[1] / r[0] / 100, r[2] / r[0] / 100, r[5] / u / 100, r[6] / u / 100))
        }
        File("build/forecast-season.txt").writeText(out.toString())
    }

    /** RF-PRV-06: o fechamento do mês por categoria, o ritmo da 1.5 ([Pacing.projected]) contra o conjunto. */
    @Test fun categorias() {
        assumeTrue(arg != "false")
        val out = StringBuilder()
        out.append(String.format("%-11s %5s %9s %9s %6s\n", "mundo", "casos", "1.5", "novo", "cob%"))
        var to = 0.0; var tn = 0.0; var tc = 0; var ti = 0
        for (kind in worlds) {
            var old = 0.0; var new = 0.0; var n = 0; var inside = 0
            for (seed in 1L..(seeds ?: 2L)) {
                val full = ForecastWorld.generate(ForecastWorld.spec(kind), end, seed * 97 + kind.ordinal)
                val actual = Ledger(full, end)
                for (back in 1..10) {
                    val ym = end.ym().minusMonths(back.toLong())
                    for (day in listOf(5, 10, 15, 20, 25)) {
                        val today = ym.atDay(day)
                        val l = Ledger(Personas.asOf(full, today), today)
                        for (o in io.github.h3yk0.cryo.domain.predict.CategoryForecasts.of(l, Recurrences.detect(l.s.txs, today), seed)) {
                            val real = actual.categorySpent(ym, o.categoryId)
                            old += abs(io.github.h3yk0.cryo.domain.engine.Pacing.projected(l, o.categoryId, o.spent) - real)
                            new += abs(o.mid - real)
                            if (real in o.low..o.high) inside++
                            n++
                        }
                    }
                }
            }
            out.append(String.format("%-11s %5d %9.2f %9.2f %6.1f\n", kind.name, n, old / n / 100, new / n / 100, inside * 100.0 / n))
            to += old; tn += new; tc += n; ti += inside
        }
        out.append(String.format("%-11s %5d %9.2f %9.2f %6.1f\n", "TODOS", tc, to / tc / 100, tn / tc / 100, ti * 100.0 / tc))
        File("build/forecast-categories.txt").writeText(out.toString())
    }
}
