// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.domain.predict

import io.github.h3yk0.cryo.domain.engine.Stats
import kotlin.math.abs
import kotlin.math.pow

/*
 * RF-PRV-02: os modelos. Cada um responde à mesma pergunta ("quanto vai somar nos próximos h dias?") olhando o passado de
 * um jeito diferente. Nenhum é "o certo": cada vida financeira tem o seu, e é o teste contra o passado da própria pessoa
 * (RF-PRV-03) que decide quanto cada um vale. São métodos clássicos de previsão, escritos aqui do zero, sem biblioteca.
 *
 * Um modelo devolve NaN quando não tem o que dizer (pouco histórico, pergunta que não é a dele): ele fica de fora da vez.
 */
enum class ModelKind {
    /** O ritmo recente com o jeito de cada dia da semana (mais peso no que é novo). */
    PROFILE,
    /** A média dos últimos 3 meses: mais calma. */
    PACE_91,
    /** O mesmo trecho do mês nos meses passados (quem gasta mais depois do pagamento). */
    SAME_PHASE,
    /** O mesmo trecho dos meses passados, ajustado para o nível de agora (3 meses). */
    PHASE_LEVEL,
    /** Nível e tendência suavizados semana a semana (Holt amortecido). */
    HOLT,
    /** Método Theta: nível suavizado com metade da tendência. */
    THETA,
    /** A média dos últimos 6 meses: para o que acontece poucas vezes (entradas avulsas, compras raras). */
    PACE_182,
}

object Models {
    private const val HALF_LIFE = 90.0

    /** Soma prevista dos [h] dias depois do último dia de [v]. */
    fun forecast(kind: ModelKind, v: View, h: Int): Double {
        if (h <= 0) return 0.0
        if (v.size < MIN_DAYS) return Double.NaN
        val f = when (kind) {
            ModelKind.PROFILE -> profile(v, h)
            ModelKind.PACE_91 -> if (v.size >= 45) v.pace(91) * h else Double.NaN
            ModelKind.SAME_PHASE -> samePhase(v, h, adjust = false)
            ModelKind.PHASE_LEVEL -> samePhase(v, h, adjust = true)
            ModelKind.HOLT -> holt(v, h)
            ModelKind.THETA -> theta(v, h)
            ModelKind.PACE_182 -> if (v.size >= 120) v.pace(182) * h else Double.NaN
        }
        return if (f.isFinite()) f.coerceAtLeast(0.0) else Double.NaN
    }

    const val MIN_DAYS = 7

    /* ------------------------------- perfil da semana ------------------------------- */

    /** Nível (média com mais peso no recente) e o fator de cada dia da semana (1,2 = 20% acima da média). */
    fun weekProfile(v: View, end: Int = v.t): Pair<Double, DoubleArray> {
        val t = end.coerceAtMost(v.t)
        var sw = 0.0; var s = 0.0
        val wSum = DoubleArray(7); val wVal = DoubleArray(7); val count = IntArray(7)
        for (i in 0..t) {
            val w = 0.5.pow((t - i) / HALF_LIFE)
            sw += w; s += w * v.x[i]
            val k = v.s.dow[i]
            wSum[k] += w; wVal[k] += w * v.x[i]; count[k]++
        }
        val level = if (sw > 0) s / sw else 0.0
        val factor = DoubleArray(7) { 1.0 }
        if (level > 0) {
            for (k in 0 until 7) {
                val ratio = if (wSum[k] > 0) (wVal[k] / wSum[k]) / level else 1.0
                // Poucas semanas: o fator fica perto de 1 (quem ainda não tem certeza não exagera).
                factor[k] = (count[k] * ratio + 8.0) / (count[k] + 8.0)
            }
            val avg = factor.average()
            if (avg > 0) for (k in 0 until 7) factor[k] /= avg
        }
        return level to factor
    }

    private fun profile(v: View, h: Int): Double {
        val (level, factor) = weekProfile(v)
        var days = 0.0
        for (j in 1..h) days += factor[v.dowAhead(j)]
        return level * days
    }

    /* ------------------------------ o mesmo trecho do mês ------------------------------ */

    private const val LEVEL_DAYS = 91

    private fun samePhase(v: View, h: Int, adjust: Boolean): Double {
        val t = v.t
        val today = v.s.date(t)
        val now = v.pace(LEVEL_DAYS)
        val sums = ArrayList<Double>(6)
        for (k in 1..6) {
            val ym = java.time.YearMonth.from(today).minusMonths(k.toLong())
            val d = ym.atDay(minOf(today.dayOfMonth, ym.lengthOfMonth()))
            val i = v.s.indexOf(d)
            if (i < 13 || i + h > t) continue
            val sum = v.sum(i + 1, i + h)
            if (!adjust) sums += sum else {
                val then = v.pace(LEVEL_DAYS, i)
                if (then > 0 && now > 0) sums += sum * (now / then).coerceIn(0.4, 2.5)
            }
        }
        return if (sums.size < 2) Double.NaN else Stats.median(sums.toDoubleArray())
    }

    /* ----------------------------------- semanas ----------------------------------- */

    /** Somas das semanas que terminam no último dia conhecido, da mais antiga para a mais nova (no máximo um ano). */
    private fun weeks(v: View): DoubleArray {
        val w = minOf(52, v.size / 7)
        return DoubleArray(w) { k ->
            val end = v.t - 7 * (w - 1 - k)
            v.sum(end - 6, end)
        }
    }

    /** Soma de uma previsão semana a semana sobre [h] dias (a última semana entra pela fração). */
    private inline fun overWeeks(h: Int, week: (Int) -> Double): Double {
        var total = 0.0
        var left = h
        var j = 1
        while (left > 0) {
            val share = minOf(left, 7) / 7.0
            total += week(j).coerceAtLeast(0.0) * share
            left -= 7; j++
        }
        return total
    }

    private val ALPHAS = doubleArrayOf(0.1, 0.2, 0.35, 0.5, 0.7)
    private val BETAS = doubleArrayOf(0.0, 0.05, 0.15)
    private const val PHI = 0.9

    /**
     * Holt amortecido: acompanha o nível e a tendência das semanas, e a tendência vai perdendo força para a frente (ninguém
     * sobe ou desce para sempre). Os dois ajustes (o quanto ouve a última semana) são os que melhor teriam previsto as
     * semanas passadas.
     */
    private fun holt(v: View, h: Int): Double {
        val y = weeks(v)
        if (y.size < 8) return Double.NaN
        var best = Double.MAX_VALUE; var bestLevel = 0.0; var bestTrend = 0.0
        val warm = minOf(4, y.size)
        val init = y.copyOf(warm).average()
        for (a in ALPHAS) for (b in BETAS) {
            var level = init; var trend = 0.0; var err = 0.0
            for (i in y.indices) {
                val f = level + PHI * trend
                if (i >= 3) err += abs(y[i] - f)
                val nl = a * y[i] + (1 - a) * f
                trend = b * (nl - level) + (1 - b) * PHI * trend
                level = nl
            }
            if (err < best) { best = err; bestLevel = level; bestTrend = trend }
        }
        return overWeeks(h) { j ->
            var damp = 0.0; var p = PHI
            repeat(j) { damp += p; p *= PHI }
            bestLevel + damp * bestTrend
        }
    }

    /** Theta: o nível suavizado mais metade da tendência (resistente a semanas fora da curva). */
    private fun theta(v: View, h: Int): Double {
        val y = weeks(v)
        if (y.size < 8) return Double.NaN
        var best = Double.MAX_VALUE; var bestLevel = y[0]; var bestAlpha = 0.2
        for (a in ALPHAS) {
            var level = y[0]; var err = 0.0
            for (i in 1 until y.size) {
                if (i >= 3) err += abs(y[i] - level)
                level = a * y[i] + (1 - a) * level
            }
            if (err < best) { best = err; bestLevel = level; bestAlpha = a }
        }
        val slope = Stats.theilSen(DoubleArray(y.size) { it.toDouble() }, y)
        return overWeeks(h) { j -> bestLevel + 0.5 * slope * (1.0 / bestAlpha + j - 1) }
    }
}
