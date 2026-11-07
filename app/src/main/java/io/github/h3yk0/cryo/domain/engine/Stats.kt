// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.domain.engine

import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.pow

/*
 * RF-MOT-01: o núcleo de estatística do motor. Tudo aqui é "robusto": um gasto 100 vezes maior que os outros não muda a
 * mediana, e os cortes nos extremos (winsorização) impedem que uma TV comprada à vista distorça o ritmo do mês.
 *
 * As funções aceitam listas vazias e devolvem 0 (nunca NaN nem infinito): o motor não pode quebrar por falta de dados.
 */
object Stats {
    /** Constante que faz o desvio robusto (MAD) valer o mesmo que o desvio padrão quando os dados são "normais". */
    const val MAD_TO_SD = 1.4826

    fun mean(xs: DoubleArray): Double = if (xs.isEmpty()) 0.0 else xs.sum() / xs.size

    fun median(xs: DoubleArray): Double = quantile(xs, 0.5)

    /** Quantil com interpolação linear (o "tipo 7", o mesmo das planilhas). [q] entre 0 e 1. */
    fun quantile(xs: DoubleArray, q: Double): Double {
        if (xs.isEmpty()) return 0.0
        val sorted = xs.copyOf().also { it.sort() }
        return quantileSorted(sorted, q)
    }

    fun quantileSorted(sorted: DoubleArray, q: Double): Double {
        if (sorted.isEmpty()) return 0.0
        if (sorted.size == 1) return sorted[0]
        val pos = q.coerceIn(0.0, 1.0) * (sorted.size - 1)
        val lo = floor(pos).toInt()
        val hi = minOf(lo + 1, sorted.size - 1)
        val frac = pos - lo
        return sorted[lo] + (sorted[hi] - sorted[lo]) * frac
    }

    /** Desvio absoluto mediano: a "distância típica" até a mediana, sem se deixar levar pelos extremos. */
    fun mad(xs: DoubleArray, center: Double = median(xs)): Double {
        if (xs.isEmpty()) return 0.0
        return median(DoubleArray(xs.size) { abs(xs[it] - center) })
    }

    /**
     * Quantos "desvios típicos" [x] está longe da mediana. [floor] evita a divisão por zero quando todos os valores são iguais
     * (ex.: a mesma assinatura todo mês).
     */
    fun robustZ(x: Double, median: Double, mad: Double, floor: Double): Double {
        val scale = maxOf(mad * MAD_TO_SD, floor, 1e-9)
        return (x - median) / scale
    }

    /** Corta os extremos: tudo abaixo do quantil [lo] vira o quantil [lo]; tudo acima de [hi] vira [hi]. */
    fun winsorize(xs: DoubleArray, lo: Double, hi: Double): DoubleArray {
        if (xs.isEmpty()) return xs
        val sorted = xs.copyOf().also { it.sort() }
        val a = quantileSorted(sorted, lo)
        val b = quantileSorted(sorted, hi)
        return DoubleArray(xs.size) { xs[it].coerceIn(a, b) }
    }

    fun weightedMean(xs: DoubleArray, ws: DoubleArray): Double {
        var sw = 0.0
        var s = 0.0
        for (i in xs.indices) {
            val w = ws[i]
            if (w <= 0.0 || !w.isFinite() || !xs[i].isFinite()) continue
            sw += w; s += w * xs[i]
        }
        return if (sw <= 0.0) 0.0 else s / sw
    }

    /** Peso pelo tempo: o que aconteceu há [halfLife] dias vale a metade do que aconteceu hoje. */
    fun decay(ageDays: Long, halfLife: Double): Double = if (ageDays <= 0) 1.0 else 0.5.pow(ageDays / halfLife)

    /**
     * Tendência resistente a extremos (Theil–Sen): a mediana das inclinações entre todos os pares de pontos. Com poucos
     * pontos (os meses de um ano), é barata e não se deixa levar por um mês fora da curva.
     */
    fun theilSen(xs: DoubleArray, ys: DoubleArray): Double {
        val n = minOf(xs.size, ys.size, 60)
        if (n < 2) return 0.0
        val slopes = ArrayList<Double>(n * (n - 1) / 2)
        for (i in 0 until n) for (j in i + 1 until n) {
            val dx = xs[j] - xs[i]
            if (dx != 0.0) slopes += (ys[j] - ys[i]) / dx
        }
        if (slopes.isEmpty()) return 0.0
        return median(slopes.toDoubleArray())
    }

    /** Variação robusta (desvio robusto ÷ mediana): 0,1 = os valores costumam ficar 10% em volta do normal. */
    fun robustCv(xs: DoubleArray): Double {
        val m = median(xs)
        if (m <= 0.0) return 0.0
        return mad(xs, m) * MAD_TO_SD / m
    }

    /** Interpolação linear entre [a] (em [x0]) e [b] (em [x1]), presa às pontas. */
    fun ramp(x: Double, x0: Double, x1: Double, a: Double, b: Double): Double {
        if (x0 == x1) return if (x <= x0) a else b
        val t = ((x - x0) / (x1 - x0)).coerceIn(0.0, 1.0)
        return a + (b - a) * t
    }
}

/**
 * RF-MOT-01: sorteio com semente (SplitMix64). Mesma semente, mesma sequência: o motor dá sempre o mesmo resultado para os
 * mesmos dados, o que deixa a tela estável e os testes repetíveis.
 */
class Rng(seed: Long) {
    private var state = seed

    fun nextLong(): Long {
        state += GOLDEN
        var z = state
        z = (z xor (z ushr 30)) * MIX1
        z = (z xor (z ushr 27)) * MIX2
        return z xor (z ushr 31)
    }

    /** Número entre 0 (incluído) e 1 (excluído). */
    fun nextDouble(): Double = (nextLong() ushr 11) * (1.0 / (1L shl 53))

    /** Inteiro entre 0 (incluído) e [bound] (excluído). */
    fun nextInt(bound: Int): Int = if (bound <= 1) 0 else ((nextLong() ushr 1) % bound).toInt()

    /** Normal padrão (Box–Muller), para pequenas variações em torno de um valor. */
    fun nextGaussian(): Double {
        val u1 = nextDouble().coerceAtLeast(1e-12)
        val u2 = nextDouble()
        return kotlin.math.sqrt(-2.0 * kotlin.math.ln(u1)) * kotlin.math.cos(2.0 * Math.PI * u2)
    }

    companion object {
        private const val GOLDEN = -0x61C8864680B583EBL // 0x9E3779B97F4A7C15
        private const val MIX1 = -0x40A7B892E31B1A47L // 0xBF58476D1CE4E5B9
        private const val MIX2 = -0x6B2FB644ECCEEE15L // 0x94D049BB133111EB

        /** Mistura um valor numa semente (para tirar a semente dos próprios dados). */
        fun mix(seed: Long, v: Long): Long {
            var z = seed xor (v + GOLDEN + (seed shl 6) + (seed ushr 2))
            z = (z xor (z ushr 30)) * MIX1
            z = (z xor (z ushr 27)) * MIX2
            return z xor (z ushr 31)
        }
    }
}

/**
 * Sorteio ponderado entre itens fixos (ex.: os dias do histórico, com mais peso nos recentes). Monta a soma acumulada uma
 * vez; cada sorteio é uma busca binária.
 */
class WeightedPicker(weights: DoubleArray) {
    private val cumulative = DoubleArray(weights.size)
    val total: Double
    val size: Int get() = cumulative.size

    init {
        var acc = 0.0
        for (i in weights.indices) {
            val w = weights[i]
            acc += if (w > 0.0 && w.isFinite()) w else 0.0
            cumulative[i] = acc
        }
        total = acc
    }

    /** Índice sorteado, ou -1 se não houver nenhum item com peso. */
    fun pick(rng: Rng): Int {
        if (total <= 0.0) return -1
        val r = rng.nextDouble() * total
        var lo = 0
        var hi = cumulative.size - 1
        while (lo < hi) {
            val mid = (lo + hi) ushr 1
            if (cumulative[mid] > r) hi = mid else lo = mid + 1
        }
        return lo
    }
}
