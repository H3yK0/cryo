// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.domain.predict

import io.github.h3yk0.cryo.data.db.Tx
import io.github.h3yk0.cryo.domain.engine.Stats
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/*
 * RF-PRV-01: a base do previsor. Um fluxo de dinheiro (o gasto do dia a dia, uma categoria, as entradas avulsas) vira uma
 * série com um valor por dia; todos os modelos trabalham sobre ela. O calendário (dia da semana, dia do mês, mês) é
 * calculado uma vez, para as contas não criarem datas a cada passo.
 */

/** Uma série diária, do dia [start] em diante (um valor por dia, zero nos dias sem movimento). */
class DailySeries(val start: LocalDate, val raw: DoubleArray) {
    val n: Int get() = raw.size

    /** Dia da semana de cada dia (segunda = 0). */
    val dow = IntArray(raw.size)

    /** Dia do mês de cada dia (1 a 31). */
    val dom = IntArray(raw.size)

    /** Número do mês (ano × 12 + mês − 1): cresce de 1 em 1, bom para comparar meses. */
    val month = IntArray(raw.size)

    /** Quantos dias tem o mês de cada dia. */
    val monthLen = IntArray(raw.size)

    init {
        var d = start
        for (i in raw.indices) {
            dow[i] = d.dayOfWeek.value - 1
            dom[i] = d.dayOfMonth
            month[i] = d.year * 12 + d.monthValue - 1
            monthLen[i] = d.lengthOfMonth()
            d = d.plusDays(1)
        }
    }

    fun date(i: Int): LocalDate = start.plusDays(i.toLong())
    fun indexOf(d: LocalDate): Int = ChronoUnit.DAYS.between(start, d).toInt()

    /** O que se via no dia de índice [t]: só o passado (os modelos nunca enxergam o que veio depois). */
    fun view(t: Int): View = View(this, t.coerceIn(-1, n - 1))

    companion object {
        /** Três anos: dá para ver a mesma época em dois anos anteriores, e a conta continua leve. */
        const val MAX_DAYS = 1100L

        fun of(txs: List<Tx>, end: LocalDate, include: (Tx) -> Boolean): DailySeries {
            var first: LocalDate? = null
            for (t in txs) if (!t.date.isAfter(end) && include(t)) {
                if (first == null || t.date.isBefore(first)) first = t.date
            }
            val start = maxOf(first ?: end.plusDays(1), end.minusDays(MAX_DAYS - 1))
            if (start.isAfter(end)) return DailySeries(end.plusDays(1), DoubleArray(0))
            val n = (ChronoUnit.DAYS.between(start, end) + 1).toInt()
            val v = DoubleArray(n)
            for (t in txs) {
                if (t.date.isBefore(start) || t.date.isAfter(end) || !include(t)) continue
                v[ChronoUnit.DAYS.between(start, t.date).toInt()] += t.amount.toDouble()
            }
            return DailySeries(start, v)
        }
    }
}

/**
 * A série vista num dia do passado ([t] = último dia conhecido). Os valores vêm **sem os extremos**: o que passa do teto
 * (percentil 97,5 dos dias vistos até ali, e nunca abaixo do que é uma compra comum) vira o teto. Uma TV comprada à vista
 * não muda o que é "normal"; o teto é calculado só com o passado daquele dia, para o teste contra o passado ser honesto.
 */
class View(val s: DailySeries, val t: Int) {
    val size: Int get() = t + 1
    val cap: Double
    val x: DoubleArray
    private val prefix: DoubleArray

    init {
        val m = t + 1
        var c = Double.MAX_VALUE
        if (m >= 20) {
            // Uma cópia ordenada só (RF-MOT-15, economia de recursos): os dias com movimento são o fim dela.
            val sorted = s.raw.copyOf(m).also { it.sort() }
            c = quantile(sorted, 0, m, 0.975)
            var z = 0
            while (z < m && sorted[z] <= 0.0) z++
            if (z < m) c = maxOf(c, quantile(sorted, z, m, 0.80))
            if (c <= 0.0) c = Double.MAX_VALUE
        }
        cap = c
        x = DoubleArray(m) { minOf(s.raw[it], c) }
        prefix = DoubleArray(m + 1)
        for (i in 0 until m) prefix[i + 1] = prefix[i] + x[i]
    }

    /** O quantil [q] do pedaço [from]..[to) de um vetor ordenado (a mesma conta de [Stats.quantileSorted]). */
    private fun quantile(sorted: DoubleArray, from: Int, to: Int, q: Double): Double {
        val pos = q * (to - from - 1)
        val lo = pos.toInt()
        val hi = minOf(lo + 1, to - from - 1)
        return sorted[from + lo] + (sorted[from + hi] - sorted[from + lo]) * (pos - lo)
    }

    /** Soma (sem os extremos) dos dias [a]..[b], inclusive; fora do que se conhece conta zero. */
    fun sum(a: Int, b: Int): Double {
        val lo = a.coerceAtLeast(0)
        val hi = b.coerceAtMost(t)
        return if (hi < lo) 0.0 else prefix[hi + 1] - prefix[lo]
    }

    /** Média por dia dos últimos [w] dias conhecidos (ou de quantos houver). */
    fun pace(w: Int, at: Int = t): Double {
        val a = (at - w + 1).coerceAtLeast(0)
        val days = at - a + 1
        return if (days <= 0) 0.0 else sum(a, at) / days
    }

    /** Dia da semana do dia que fica [j] dias depois do último conhecido. */
    fun dowAhead(j: Int): Int = if (t >= 0) (s.dow[t] + j) % 7 else 0
}
