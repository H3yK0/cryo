// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.domain.predict

import io.github.h3yk0.cryo.domain.engine.Rng
import io.github.h3yk0.cryo.domain.engine.Stats
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Uma previsão do conjunto: o número e tudo o que o sustenta. [residuals] são os erros que o conjunto teria cometido no
 * passado da própria pessoa ((real − previsto) ÷ previsto), cada um medido **sem** usar aquele pedaço do passado para
 * escolher os pesos (deixa-um-de-fora): é deles que sai a faixa.
 */
class EnsembleForecast(
    /** Soma prevista para o período (o "normal": sem os extremos). */
    val point: Double,
    /** Quanto cada modelo pesou (soma 1). */
    val weights: Map<ModelKind, Double>,
    val residuals: DoubleArray,
    /** Quantos testes contra o passado sustentam os pesos. */
    val origins: Int,
    /** Variação relativa esperada quando ainda não há passado para medir. */
    val fallbackSpread: Double,
    /** A época do ano aplicada ao número (1 = época comum; 1,4 = estes dias costumam ser 40% mais caros). */
    val season: Double = 1.0,
) {
    /** Um erro relativo plausível: sorteado dos erros de verdade quando há bastantes, senão da variação esperada. */
    fun drawError(rng: Rng): Double {
        val n = residuals.size
        val useReal = n >= 4 && rng.nextDouble() < n / (n + 5.0)
        val e = if (useReal) residuals[rng.nextInt(n)] + 0.04 * rng.nextGaussian() else fallbackSpread * rng.nextGaussian()
        return e.coerceAtLeast(-1.0)
    }
}

/**
 * RF-PRV-03: o conjunto. Para cada previsão, o Cryo volta ao passado da pessoa e pergunta a cada modelo o que ele teria
 * previsto naqueles dias (o mesmo dia do mês nos meses anteriores, e algumas semanas atrás), sabendo só o que se sabia
 * então. Quem errou menos pesa mais. Com pouco passado, os pesos ficam perto de iguais: ninguém ganha confiança com dois
 * acertos. É a ideia que vence as competições de previsão com séries curtas: combinar métodos simples pelo desempenho.
 */
object Ensemble {
    /** O quanto o melhor modelo pesa mais que os outros (4 = erro 2x maior pesa 16x menos). */
    private const val POWER = 4.0
    private const val PRIOR_ORIGINS = 4.0
    private const val MIN_HISTORY = 28
    private const val MIN_SCORED = 3

    private class Origin(val i: Int, val h: Int, val weight: Double)

    fun forecast(
        s: DailySeries, t: Int, h: Int, kinds: List<ModelKind> = ModelKind.entries, power: Double = POWER, prior: Double = PRIOR_ORIGINS,
        useSeason: Boolean = true,
    ): EnsembleForecast {
        val now = s.view(t)
        if (h <= 0 || now.size == 0) return EnsembleForecast(0.0, emptyMap(), DoubleArray(0), 0, 0.3)
        // A época do ano (RF-PRV-03) multiplica o número de hoje; os pesos dos modelos são medidos sem ela (aplicá-la
        // também nos dias do passado bagunçava a escolha dos modelos e piorou o resultado no laboratório).
        val season = if (useSeason) Season.factor(now, h) else 1.0
        val target = DoubleArray(kinds.size) { Models.forecast(kinds[it], now, h) * season }
        val origins = origins(s, t, h)
        val m = kinds.size
        val f = Array(m) { DoubleArray(origins.size) }
        val views = origins.map { s.view(it.i) }
        for (k in 0 until m) for (o in origins.indices) f[k][o] = Models.forecast(kinds[k], views[o], origins[o].h)
        val actual = DoubleArray(origins.size) { now.sum(origins[it].i + 1, origins[it].i + origins[it].h) }
        val actualRaw = DoubleArray(origins.size) { o -> var a = 0.0; for (i in origins[o].i + 1..origins[o].i + origins[o].h) a += s.raw[i]; a }

        val weights = weights(f, actual, origins, target, skip = -1, power, prior)
        var point = 0.0
        for (k in 0 until m) if (weights[k] > 0) point += weights[k] * target[k]

        // O que o conjunto teria dito em cada dia do passado, com os pesos refeitos sem aquele dia ("de fora").
        val past = DoubleArray(origins.size) { Double.NaN }
        if (origins.size >= MIN_SCORED + 1) {
            for (o in origins.indices) {
                val col = DoubleArray(m) { f[it][o] }
                val w = weights(f, actual, origins, col, skip = o, power, prior)
                var fo = 0.0; var sw = 0.0
                for (k in 0 until m) if (w[k] > 0) { fo += w[k] * col[k]; sw += w[k] }
                if (sw > 0) past[o] = fo
            }
        }

        val scale = if (origins.isEmpty()) 0.0 else 0.3 * actual.average()
        val res = ArrayList<Double>(origins.size)
        for (o in origins.indices) if (past[o].isFinite()) res += (actualRaw[o] - past[o]) / maxOf(past[o], scale, 1.0)
        // O centro da previsão é o número do conjunto: dos erros do passado fica só o espalhamento (somar a mediana deles
        // ao centro piorou a previsão em todos os mundos do laboratório: era ruído, não viés).
        val residuals = res.toDoubleArray()
        if (residuals.size >= 4) { val med = Stats.median(residuals); for (i in residuals.indices) residuals[i] -= med }
        val out = LinkedHashMap<ModelKind, Double>()
        for (k in 0 until m) if (weights[k] > 0.0005) out[kinds[k]] = weights[k]
        return EnsembleForecast(point, out, residuals, origins.size, fallbackSpread(now, h), season)
    }

    /** Os dias do passado em que a mesma pergunta já tem resposta. */
    private fun origins(s: DailySeries, t: Int, h: Int): List<Origin> {
        val out = ArrayList<Origin>(20)
        val seen = HashSet<Int>()
        val toMonthEnd = s.dom[t] + h == s.monthLen[t]
        val today = s.date(t)
        // O mesmo dia do mês, nos últimos 12 meses (o mais parecido com a pergunta de hoje).
        for (k in 1..12) {
            val ym = java.time.YearMonth.from(today).minusMonths(k.toLong())
            val d = ym.atDay(minOf(today.dayOfMonth, ym.lengthOfMonth()))
            val i = s.indexOf(d)
            if (i < MIN_HISTORY) break
            val hk = if (toMonthEnd) ym.lengthOfMonth() - d.dayOfMonth else h
            if (hk <= 0 || i + hk > t) continue
            if (seen.add(i)) out += Origin(i, hk, 0.9.pow(k - 1))
        }
        // E algumas semanas atrás, com o mesmo tamanho de período (mais amostras, com menos peso).
        for (j in 0..7) {
            val i = t - h - 7 * j
            if (i < MIN_HISTORY) break
            if (seen.add(i)) out += Origin(i, h, 0.5 * 0.9.pow(j))
        }
        return out
    }

    /**
     * Pesos dos modelos para as previsões em [target], pelo erro médio de cada um nos dias do passado ([skip] fica de fora).
     * Modelo sem previsão para o alvo pesa zero; sem passado suficiente, todos os que responderam pesam igual.
     */
    private fun weights(
        f: Array<DoubleArray>, actual: DoubleArray, origins: List<Origin>, target: DoubleArray, skip: Int, power: Double, prior: Double,
    ): DoubleArray {
        val m = f.size
        val score = DoubleArray(m) { Double.NaN }
        val count = IntArray(m)
        for (k in 0 until m) {
            if (!target[k].isFinite()) continue
            var sw = 0.0; var se = 0.0; var c = 0
            for (o in origins.indices) {
                if (o == skip || !f[k][o].isFinite()) continue
                sw += origins[o].weight; se += origins[o].weight * abs(f[k][o] - actual[o]); c++
            }
            if (c >= MIN_SCORED && sw > 0) { score[k] = se / sw; count[k] = c }
        }
        val w = DoubleArray(m)
        val scored = (0 until m).filter { score[it].isFinite() }
        if (scored.isEmpty()) {
            val answering = (0 until m).filter { target[it].isFinite() }
            for (k in answering) w[k] = 1.0 / answering.size
            return w
        }
        // Pouco passado = pouca certeza de quem é o melhor: o erro de cada modelo encolhe para o erro médio de todos, tanto
        // mais quanto menos dias do passado o mediram. (Encolher os pesos para "todos iguais", como antes, dava uma fatia fixa
        // até a quem errava feio: no mercado que se gasta todo no começo do mês, era um alarme falso.)
        val pooled = scored.sumOf { score[it] } / scored.size
        for (k in scored) score[k] = (count[k] * score[k] + prior * pooled) / (count[k] + prior)
        val best = scored.minOf { score[it] }.coerceAtLeast(1e-9)
        var sum = 0.0
        for (k in scored) { w[k] = (best / maxOf(score[k], 1e-9)).pow(power); sum += w[k] }
        for (k in scored) w[k] /= sum
        return w
    }

    /** Sem passado para medir o erro: a variação de um dia para o outro, diluída no período, mais a incerteza do nível. */
    private fun fallbackSpread(v: View, h: Int): Double {
        val mean = v.x.average()
        if (v.size < 2 || mean <= 0) return 0.5
        var ss = 0.0
        for (x in v.x) ss += (x - mean) * (x - mean)
        val cv = sqrt(ss / (v.size - 1)) / mean
        val level = if (v.size >= 90) 0.15 else if (v.size >= 28) 0.25 else 0.4
        return sqrt(cv * cv / h + level * level).coerceIn(0.1, 1.5)
    }

    /** Erro relativo típico (mediana dos erros absolutos) do conjunto no passado, ou null sem passado. */
    fun typicalError(fc: EnsembleForecast): Double? =
        if (fc.residuals.size < 4) null else Stats.median(DoubleArray(fc.residuals.size) { abs(fc.residuals[it]) })
}
