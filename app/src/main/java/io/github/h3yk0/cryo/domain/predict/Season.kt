// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.domain.predict

import io.github.h3yk0.cryo.domain.engine.Stats
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * RF-PRV-03: a época do ano. "Nestes mesmos dias do ano passado, o gasto ficou bem acima (ou abaixo) do que vinha antes e
 * do que veio depois?" Se a diferença for maior que o sobe e desce comum de um mês para o outro, ela vale para a previsão
 * (dezembro, férias, volta às aulas); se não for, o fator é 1 e nada muda. É de propósito que só entra o que se destacou
 * **nos dois anos anteriores**: com um ano só, o laboratório mostrou alarmes falsos que custavam mais do que a época rendia.
 */
internal object Season {
    /** 52 semanas: o mesmo dia da semana do ano passado. */
    private const val YEAR = 364
    private const val WINDOW = 28
    private const val SIDE = 91
    private const val MIN_SIDE_DAYS = 90
    private const val MIN_OTHERS = 8

    /** Cada um dos dois anos precisa passar disto ("desvios comuns" acima ou abaixo do normal)... */
    private const val Z_EACH = 1.5

    /** ...e os dois juntos, disto para começar a valer e disto para valer inteira. */
    private const val Z_FROM = 2.5
    private const val Z_FULL = 3.5

    /** Mesmo valendo inteira, a diferença entra com 80% (dois anos ainda são pouco). */
    private const val STRENGTH = 0.8

    /** Por quanto multiplicar a previsão dos [h] dias depois do último dia de [v] (1 = época comum). */
    fun factor(v: View, h: Int): Double {
        val t = v.t
        val len = maxOf(h, WINDOW)
        val a = t + 1 - YEAR
        val last = ratio(v, a, len)
        val before = ratio(v, a - YEAR, len)
        // Só vale o que se repetiu: a mesma época precisa ter se destacado nos dois anos anteriores, para o mesmo lado.
        if (!last.isFinite() || !before.isFinite()) return 1.0
        // O sobe e desce comum: a mesma conta em outros pedaços do passado, longe destes dias.
        val others = ArrayList<Double>(128)
        var start = a % 7
        while (start + len - 1 <= t) {
            if (abs(start - a) >= len + 7 && abs(start - (a - YEAR)) >= len + 7) {
                val r = ratio(v, start, len)
                if (r.isFinite()) others += r
            }
            start += 7
        }
        if (others.size < MIN_OTHERS) return 1.0
        val arr = others.toDoubleArray()
        val usual = Stats.median(arr)
        if (usual <= 0) return 1.0
        val spread = maxOf(1.4826 * Stats.mad(arr, usual), 0.05)
        val z1 = (last - usual) / spread
        val z2 = (before - usual) / spread
        if (z1 * z2 <= 0 || minOf(abs(z1), abs(z2)) < Z_EACH) return 1.0
        val diff = (last + before) / 2 / usual - 1
        val z = abs(z1 + z2) / sqrt(2.0)
        val w = ((z - Z_FROM) / (Z_FULL - Z_FROM)).coerceIn(0.0, 1.0)
        return (1 + w * STRENGTH * diff).coerceIn(0.75, 1.8)
    }

    /** O ritmo dos [len] dias a partir de [a], dividido pelo ritmo dos 3 meses de antes e de depois (NaN sem dados). */
    private fun ratio(v: View, a: Int, len: Int): Double {
        val b = a + len - 1
        if (a < 0 || b > v.t) return Double.NaN
        val lo = (a - SIDE).coerceAtLeast(0)
        val hi = (b + SIDE).coerceAtMost(v.t)
        val sideDays = (a - lo) + (hi - b)
        if (sideDays < MIN_SIDE_DAYS || a - lo < WINDOW) return Double.NaN
        val side = (v.sum(lo, a - 1) + v.sum(b + 1, hi)) / sideDays
        if (side <= 0) return Double.NaN
        return v.sum(a, b) / len / side
    }
}
