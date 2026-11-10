// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.domain.predict

import io.github.h3yk0.cryo.data.db.TxType
import io.github.h3yk0.cryo.domain.Ledger
import io.github.h3yk0.cryo.domain.engine.BandPoint
import io.github.h3yk0.cryo.domain.engine.Certainty
import io.github.h3yk0.cryo.domain.engine.MonthOutlook
import io.github.h3yk0.cryo.domain.engine.Outlook
import io.github.h3yk0.cryo.domain.engine.Recurrence
import io.github.h3yk0.cryo.domain.engine.Recurrences
import io.github.h3yk0.cryo.domain.engine.Rng
import io.github.h3yk0.cryo.domain.engine.SpendingModel
import io.github.h3yk0.cryo.domain.engine.Stats
import io.github.h3yk0.cryo.domain.forecast
import io.github.h3yk0.cryo.domain.ym
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.roundToLong
import kotlin.math.sign
import kotlin.math.sqrt

/**
 * RF-PRV-05: o quanto a previsão foi conferida no passado da própria pessoa. Em cada um dos [months] meses anteriores, no
 * mesmo dia do mês, o Cryo refez a previsão só com o que se sabia então e comparou com o que aconteceu. [hits] = em quantos
 * a faixa de hoje teria contido o resultado; [typicalError] = o erro do meio (metade dos meses errou menos que isso).
 */
data class Calibration(val months: Int, val hits: Int, val typicalError: Long, val bias: Long)

/** Quanto um modelo pesou na previsão do gasto do dia a dia (RF-PRV-08: a previsão se explica). */
data class ModelShare(val kind: ModelKind, val weight: Double)

/**
 * RF-PRV-04: a previsão do mês pelo conjunto. O que já está combinado (o que já aconteceu, agendados, contas fixas,
 * parcelas, o que se repete) entra como na 1.5; o que é incerto (o gasto do dia a dia e as entradas avulsas até o fim do
 * mês) vem do [Ensemble], e a faixa vem dos erros que ele cometeu no passado desta pessoa.
 */
object Forecaster {
    const val SCENARIOS = 2000
    private const val PAST_SCENARIOS = 300
    private const val PAST_MONTHS = 12
    private const val MIN_PAST = 4

    fun month(l: Ledger, recurrences: List<Recurrence>, seed: Long, scenarios: Int = SCENARIOS, calibrate: Boolean = true): MonthOutlook {
        val base = l.forecast()
        val today = l.today
        val ym = today.ym()
        val d = today.dayOfMonth
        val days = ym.lengthOfMonth()
        val remaining = days - d

        val exclude = HashSet<Long>()
        val excludeIncome = HashSet<Long>()
        for (r in recurrences) if (r.type == TxType.EXPENSE) exclude += r.txIds else excludeIncome += r.txIds
        val spending = DailySeries.of(l.s.txs, today, SpendingModel.spending(exclude))
        val income = DailySeries.of(l.s.txs, today, SpendingModel.extraIncome(excludeIncome))
        val fcVar = Ensemble.forecast(spending, spending.n - 1, remaining)
        val fcInc = Ensemble.forecast(income, income.n - 1, remaining)
        val expected = Outlook.expectedItems(l, recurrences, today, ym.atEndOfMonth())

        // Como o gasto previsto se espalha pelos dias que faltam: pelo jeito de cada dia da semana.
        val share = DoubleArray(remaining)
        if (remaining > 0) {
            val factor = if (spending.n > 0) Models.weekProfile(spending.view(spending.n - 1)).second else DoubleArray(7) { 1.0 }
            var total = 0.0
            for (k in 0 until remaining) { share[k] = factor[SpendingModel.weekday(ym.atDay(d + 1 + k))]; total += share[k] }
            for (k in 0 until remaining) share[k] = if (total > 0) share[k] / total else 1.0 / remaining
        }

        val baseResult = base.currentResult
        val planned = LongArray(days + 1).also { arr -> base.plannedNet.forEachIndexed { i, v -> if (i <= days) arr[i] = v } }
        val rng = Rng(seed)
        val n = scenarios.coerceAtLeast(1)
        val results = DoubleArray(n)
        val variables = DoubleArray(n)
        val paths = Array(remaining) { DoubleArray(n) }
        val recNet = DoubleArray(days + 2)
        for (sc in 0 until n) {
            java.util.Arrays.fill(recNet, 0.0)
            for (e in expected) {
                if (rng.nextDouble() >= e.probability) continue
                val r = e.recurrence
                val amt = if (r.variable) r.amount * (1.0 + 0.08 * rng.nextGaussian()).coerceIn(0.5, 1.5) else r.amount.toDouble()
                recNet[e.date.dayOfMonth] += if (r.type == TxType.INCOME) amt else -amt
            }
            val v = if (remaining > 0) (fcVar.point * (1.0 + fcVar.drawError(rng))).coerceAtLeast(0.0) else 0.0
            val inc = if (remaining > 0) (fcInc.point * (1.0 + fcInc.drawError(rng))).coerceAtLeast(0.0) else 0.0
            var cum = baseResult.toDouble()
            for (k in 0 until remaining) {
                val day = d + 1 + k
                cum += planned[day] + recNet[day] - v * share[k] + inc / remaining
                paths[k][sc] = cum
            }
            variables[sc] = v
            results[sc] = if (remaining > 0) cum else baseResult.toDouble() + planned.sum()
        }
        val sorted = results.copyOf().also { it.sort() }
        variables.sort()
        var mid = Stats.quantileSorted(sorted, 0.5)
        var low = Stats.quantileSorted(sorted, 0.1)
        var high = Stats.quantileSorted(sorted, 0.9)

        // RF-PRV-05: a faixa conferida no passado (e o acerto de um erro que se repete sempre para o mesmo lado).
        var calibration: Calibration? = null
        var shift = 0.0
        var stretch = 1.0
        if (calibrate && remaining > 0) {
            val errors = pastErrors(l, seed)
            if (errors.size >= MIN_PAST) {
                val m = errors.size
                val median = Stats.median(errors)
                // Só corrige o centro quando o erro para um lado é maior que o acaso explicaria (o erro-padrão da mediana).
                val noise = 1.2533 * 1.4826 * Stats.mad(errors, median) / sqrt(m.toDouble())
                shift = sign(median) * (abs(median) - noise).coerceAtLeast(0.0)
                val centered = DoubleArray(m) { abs(errors[it] - median) }.also { it.sort() }
                // Conformal: o erro que cobre 80% dos meses passados, com a folga de quem só viu m meses.
                val rank = ceil((m + 1) * 0.8).toInt().coerceIn(1, m)
                val q = centered[rank - 1]
                val trust = m / (m + 4.0)
                val halfModel = (high - low) / 2
                val half = trust * q + (1 - trust) * halfModel
                if (halfModel > 0) stretch = (half / halfModel).coerceIn(0.5, 4.0)
                val newMid = mid + shift
                low = newMid - (mid - low) * stretch
                high = newMid + (high - mid) * stretch
                mid = newMid
                val hits = errors.count { abs(it - median) <= (high - low) / 2 }
                calibration = Calibration(m, hits, Stats.median(DoubleArray(m) { abs(errors[it]) }).roundToLong(), shift.roundToLong())
            }
        }
        fun adjust(x: Double, center: Double) = center + shift + (x - center) * stretch
        val midModel = Stats.quantileSorted(sorted, 0.5)
        val negatives = results.count { adjust(it, midModel) < 0.0 }

        val band = ArrayList<BandPoint>(remaining + 1)
        val actualToday = base.series.firstOrNull { it.day == d }?.actual ?: baseResult
        band += BandPoint(d, actualToday, actualToday, actualToday)
        for (k in 0 until remaining) {
            val col = paths[k].also { it.sort() }
            val c = Stats.quantileSorted(col, 0.5)
            // O ajuste do passado entra aos poucos, até valer inteiro no último dia.
            val part = (k + 1.0) / remaining
            val cMid = c + shift * part
            val s = 1 + (stretch - 1) * part
            band += BandPoint(
                d + 1 + k, (cMid + (Stats.quantileSorted(col, 0.1) - c) * s).roundToLong(), cMid.roundToLong(),
                (cMid + (Stats.quantileSorted(col, 0.9) - c) * s).roundToLong(),
            )
        }
        val historyDays = (spending.n - d).coerceAtLeast(0)
        return MonthOutlook(
            base = base,
            low = low.roundToLong(), mid = mid.roundToLong(), high = high.roundToLong(),
            chanceNegative = negatives.toDouble() / n,
            variableMid = Stats.quantileSorted(variables, 0.5).roundToLong(),
            dailyRate = if (remaining > 0) (fcVar.point / remaining).roundToLong() else if (spending.n > 0) spending.view(spending.n - 1).pace(28).roundToLong() else 0L,
            expected = expected,
            band = band,
            certainty = when {
                historyDays >= 90 && (calibration?.months ?: 0) >= 6 -> Certainty.HIGH
                historyDays >= 28 -> Certainty.MEDIUM
                else -> Certainty.LOW
            },
            historyDays = historyDays,
            scenarios = n,
            calibration = calibration,
            models = fcVar.weights.entries.sortedByDescending { it.value }.map { ModelShare(it.key, it.value) },
        )
    }

    /**
     * O erro da previsão (real − previsto) no mesmo dia do mês de cada mês passado, refazendo a conta só com o que se sabia
     * naquele dia. É o Cryo conferindo a si mesmo com os dados da própria pessoa.
     */
    private fun pastErrors(l: Ledger, seed: Long): DoubleArray {
        val today = l.today
        val first = l.s.txs.minOfOrNull { it.date } ?: return DoubleArray(0)
        val out = ArrayList<Double>(PAST_MONTHS)
        for (k in 1..PAST_MONTHS) {
            val ym = today.ym().minusMonths(k.toLong())
            val day = ym.atDay(minOf(today.dayOfMonth, ym.lengthOfMonth()))
            if (ChronoUnit.DAYS.between(first, day) < 45) break
            val snap = l.s.copy(txs = l.s.txs.filter { !it.date.isAfter(day) })
            val past = Ledger(snap, day)
            val o = month(past, Recurrences.detect(snap.txs, day), Rng.mix(seed, k.toLong()), PAST_SCENARIOS, calibrate = false)
            out += (l.monthSummary(ym).result - o.mid).toDouble()
        }
        return out.toDoubleArray()
    }

    /** Para os testes: o dia do passado visto de novo. */
    internal fun asOf(l: Ledger, day: LocalDate): Ledger = Ledger(l.s.copy(txs = l.s.txs.filter { !it.date.isAfter(day) }), day)
}
