// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.domain.predict

import io.github.h3yk0.cryo.data.db.TxType
import io.github.h3yk0.cryo.domain.Ledger
import io.github.h3yk0.cryo.domain.engine.Rng
import io.github.h3yk0.cryo.domain.engine.Stats
import io.github.h3yk0.cryo.domain.ym
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToLong
import kotlin.math.sqrt

/**
 * Onde o investimento pode estar daqui a [years] anos, continuando os aportes de sempre. [putIn] = o valor de hoje mais os
 * aportes do caminho (sem rendimento nenhum), para comparar; [low], [mid] e [high] = 10%, 50% e 90% dos cenários; [grid] =
 * os percentis de 5 em 5 (5%...95%), de onde sai a chance de chegar a um valor.
 */
data class InvestHorizon(val years: Int, val putIn: Long, val low: Long, val mid: Long, val high: Long, val grid: List<Long>) {
    /** Em quantos cenários o valor chega a [target] (0 a 1; nunca "certeza": fica entre 2% e 98%). */
    fun chanceOf(target: Long): Double {
        if (grid.isEmpty()) return 0.0
        if (target <= grid.first()) return 0.98
        if (target > grid.last()) return 0.02
        for (i in 1 until grid.size) {
            if (target <= grid[i]) {
                val span = (grid[i] - grid[i - 1]).coerceAtLeast(1L)
                val below = (0.05 * i) + 0.05 * (target - grid[i - 1]).toDouble() / span
                return (1.0 - below).coerceIn(0.02, 0.98)
            }
        }
        return 0.02
    }
}

/**
 * RF-PRV-07: o retrato de um investimento (ou da carteira inteira, com [investmentId] nulo).
 * [monthly] e [annual] são a rentabilidade **do investimento** (o que ele rendeu por real aplicado, sem se deixar levar por
 * quando a pessoa aportou); [volatility] é o sobe e desce ao ano; [maxDrawdown] é a maior queda do topo até o fundo;
 * [negativeShare] é a parte dos períodos que fechou no negativo. Tudo é o passado: não é promessa (TXT do PRD).
 */
data class InvestOutlook(
    val investmentId: Long?,
    val value: Long,
    val periods: Int,
    val monthly: Double,
    val annual: Double?,
    val volatility: Double?,
    val maxDrawdown: Double,
    val negativeShare: Double,
    /** Aporte típico por mês (mediana dos últimos 12 meses; nunca negativo). */
    val contribution: Long,
    val horizons: List<InvestHorizon>,
)

object InvestForecasts {
    const val SCENARIOS = 1000
    val YEARS = listOf(1, 3, 5)

    /** Menos que isto de períodos com rendimento lançado: mostra o retrato, mas não projeta. */
    const val MIN_PERIODS = 6
    private const val MIN_PERIOD_DAYS = 20L
    private const val MONTH = 30.44

    private class Flow(val date: LocalDate, val amount: Double)

    fun of(l: Ledger, investmentId: Long?, seed: Long, scenarios: Int = SCENARIOS): InvestOutlook? {
        val today = l.today
        val ids = if (investmentId == null) l.activeInvestments.mapTo(HashSet()) { it.id } else setOf(investmentId)
        val flows = ArrayList<Flow>()
        for (t in l.s.txs) {
            if (t.investmentId !in ids || t.date.isAfter(today)) continue
            when (t.type) {
                TxType.INVEST_IN -> flows += Flow(t.date, t.amount.toDouble())
                TxType.INVEST_OUT -> flows += Flow(t.date, -t.amount.toDouble())
                else -> {}
            }
        }
        if (flows.isEmpty()) return null
        flows.sortBy { it.date }
        val yields = l.s.yields.filter { it.investmentId in ids && !it.date.isAfter(today) }.sortedBy { it.date }
        val value = flows.sumOf { it.amount } + yields.sumOf { it.amount.toDouble() }

        // Períodos entre um rendimento lançado e o seguinte (Dietz modificado): o ganho dividido pelo dinheiro que estava
        // lá, contando cada aporte pelo tempo que ficou. Lançamentos muito próximos se juntam num período só.
        val rates = ArrayList<Double>()   // rendimento de cada período, já "por mês"
        var index = 1.0; var peak = 1.0; var drawdown = 0.0
        var start = flows.first().date
        var balance = 0.0                 // valor logo depois de `start`
        var fi = 0
        while (fi < flows.size && !flows[fi].date.isAfter(start)) balance += flows[fi++].amount
        var gain = 0.0
        var totalDays = 0L
        for ((k, y) in yields.withIndex()) {
            if (y.date.isBefore(start)) { balance += y.amount; continue }
            gain += y.amount
            val days = ChronoUnit.DAYS.between(start, y.date)
            val last = k == yields.lastIndex || yields[k + 1].date != y.date
            if (!last || days < MIN_PERIOD_DAYS) continue
            var weighted = 0.0; var moved = 0.0
            while (fi < flows.size && !flows[fi].date.isAfter(y.date)) {
                val f = flows[fi++]
                weighted += f.amount * ChronoUnit.DAYS.between(f.date, y.date).toDouble() / days
                moved += f.amount
            }
            val base = balance + weighted
            if (base > 1.0) {
                val r = (gain / base).coerceIn(-0.95, 5.0)
                rates += (1 + r).pow(MONTH / days) - 1
                index *= 1 + r
                if (index > peak) peak = index
                drawdown = maxOf(drawdown, 1 - index / peak)
                totalDays += days
            }
            balance += moved + gain
            gain = 0.0
            start = y.date
        }
        val n = rates.size
        val monthly = if (totalDays > 0) index.pow(MONTH / totalDays) - 1 else 0.0
        val annual = if (totalDays >= 30) index.pow(365.0 / totalDays) - 1 else null
        val logs = DoubleArray(n) { ln(1 + rates[it]) }
        val mean = if (n > 0) logs.average() else 0.0
        val sd = if (n >= 2) sqrt(logs.sumOf { (it - mean) * (it - mean) } / (n - 1)) else 0.0

        // Aporte típico: a mediana do que entrou menos o que saiu em cada um dos últimos 12 meses completos.
        val net = DoubleArray(12)
        val thisMonth = today.ym()
        for (f in flows) {
            val back = ChronoUnit.MONTHS.between(f.date.ym(), thisMonth).toInt()
            if (back in 1..12) net[back - 1] += f.amount
        }
        val firstMonth = flows.first().date.ym()
        val lived = ChronoUnit.MONTHS.between(firstMonth, thisMonth).toInt().coerceIn(0, 12)
        val contribution = if (lived >= 3) Stats.median(net.copyOf(lived)).coerceAtLeast(0.0) else 0.0

        val horizons = ArrayList<InvestHorizon>()
        if (n >= MIN_PERIODS && value > 0) {
            val rng = Rng(Rng.mix(seed, investmentId ?: -1L))
            val sims = scenarios.coerceAtLeast(20)
            val maxMonths = YEARS.max() * 12
            val finals = Array(YEARS.size) { DoubleArray(sims) }
            for (s in 0 until sims) {
                // Com poucos períodos, nem a média é certa: cada cenário tem a sua (o erro-padrão da média).
                val drift = sd / sqrt(n.toDouble()) * rng.nextGaussian()
                var v = value
                for (m in 1..maxMonths) {
                    val r = exp(logs[rng.nextInt(n)] + drift) - 1
                    v = (v * (1 + r)).coerceAtLeast(0.0) + contribution
                    val h = YEARS.indexOf(m / 12)
                    if (m % 12 == 0 && h >= 0) finals[h][s] = v
                }
            }
            for ((h, years) in YEARS.withIndex()) {
                val f = finals[h].also { it.sort() }
                horizons += InvestHorizon(
                    years = years,
                    putIn = (value + contribution * years * 12).roundToLong(),
                    low = Stats.quantileSorted(f, 0.1).roundToLong(),
                    mid = Stats.quantileSorted(f, 0.5).roundToLong(),
                    high = Stats.quantileSorted(f, 0.9).roundToLong(),
                    grid = (1..19).map { Stats.quantileSorted(f, it * 0.05).roundToLong() },
                )
            }
        }
        return InvestOutlook(
            investmentId = investmentId, value = value.roundToLong(), periods = n, monthly = monthly, annual = annual,
            volatility = if (n >= MIN_PERIODS) sd * sqrt(12.0) else null,
            maxDrawdown = drawdown,
            negativeShare = if (n > 0) rates.count { it < 0 }.toDouble() / n else 0.0,
            contribution = contribution.roundToLong(), horizons = horizons,
        )
    }
}
