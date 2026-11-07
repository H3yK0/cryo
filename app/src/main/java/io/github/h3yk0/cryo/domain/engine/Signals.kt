// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.domain.engine

import io.github.h3yk0.cryo.data.db.CategoryKind
import io.github.h3yk0.cryo.data.db.TxType
import io.github.h3yk0.cryo.domain.Ledger
import io.github.h3yk0.cryo.domain.ym
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import kotlin.math.ceil
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToLong

/* ============================ Fora do padrão (RF-MOT-05) ============================ */

enum class AnomalyKind { OUTLIER, DUPLICATE, PRICE_UP, CATEGORY_HIGH }

/**
 * Algo fora do padrão. [usual] é o normal para comparar (a mediana da categoria, o preço de antes...). [score] ordena:
 * quanto maior, mais chama a atenção.
 */
data class Anomaly(
    val kind: AnomalyKind,
    val date: LocalDate,
    val amount: Long,
    val usual: Long,
    val label: String,
    val categoryId: Long?,
    val txId: Long? = null,
    val otherTxId: Long? = null,
    val score: Double,
)

object Anomalies {
    /** Quantos desvios típicos (na escala dos logaritmos) para um gasto ser "fora da curva". */
    private const val OUTLIER_Z = 4.0
    private const val MIN_IN_CATEGORY = 8

    fun find(l: Ledger, recurrences: List<Recurrence>): List<Anomaly> {
        val today = l.today
        val out = ArrayList<Anomaly>()
        outliers(l, today, out)
        duplicates(l, today, out)
        for (r in recurrences) {
            if (r.priceChange > 0 && r.status != RecurrenceStatus.ENDED && !r.last.isBefore(today.minusDays(45))) {
                out += Anomaly(
                    AnomalyKind.PRICE_UP, r.last, r.lastAmount, r.lastAmount - r.priceChange, r.label, r.categoryId,
                    score = 2.0 + r.priceChange.toDouble() / maxOf(1L, r.lastAmount - r.priceChange),
                )
            }
        }
        categoryHigh(l, today, out)
        return out.sortedByDescending { it.score }
    }

    /** Um gasto muito acima do normal da categoria (na escala dos logaritmos: gastos têm "cauda longa"). */
    private fun outliers(l: Ledger, today: LocalDate, out: MutableList<Anomaly>) {
        val from = today.minusDays(365)
        val recent = today.minusDays(30)
        val byCat = HashMap<Long, MutableList<io.github.h3yk0.cryo.data.db.Tx>>()
        for (t in l.s.txs) {
            if (!Recurrences.isVariable(t) || t.amount <= 0 || t.date.isBefore(from) || t.date.isAfter(today)) continue
            val c = t.categoryId ?: continue
            byCat.getOrPut(c) { ArrayList() } += t
        }
        for ((cat, list) in byCat) {
            if (list.size < MIN_IN_CATEGORY) continue
            val logs = DoubleArray(list.size) { ln(list[it].amount.toDouble()) }
            val med = Stats.median(logs)
            val mad = Stats.mad(logs, med)
            val medAmount = Stats.median(DoubleArray(list.size) { list[it].amount.toDouble() })
            for (t in list) {
                if (t.date.isBefore(recent)) continue
                val z = Stats.robustZ(ln(t.amount.toDouble()), med, mad, floor = 0.25)
                if (z > OUTLIER_Z && t.amount > 2 * medAmount) {
                    out += Anomaly(AnomalyKind.OUTLIER, t.date, t.amount, medAmount.roundToLong(), t.description.trim().take(60), cat, t.id, score = z)
                }
            }
        }
    }

    /** Mesma descrição, mesmo valor, mesma conta ou cartão, em até 2 dias (e não é parcela): pode ser cobrança em dobro. */
    private fun duplicates(l: Ledger, today: LocalDate, out: MutableList<Anomaly>) {
        val from = today.minusDays(45)
        val list = l.s.txs.filter {
            it.type == TxType.EXPENSE && it.installmentGroup == null && it.installmentTotal <= 1 && it.amount > 0 &&
                !it.date.isBefore(from) && !it.date.isAfter(today)
        }
        if (list.size < 2) return
        // Só valores que importam: pelo menos o gasto típico (dois cafés iguais no mesmo dia não são problema).
        val typical = Stats.median(DoubleArray(list.size) { list[it].amount.toDouble() })
        val groups = HashMap<String, MutableList<io.github.h3yk0.cryo.data.db.Tx>>()
        for (t in list) {
            if (t.amount < typical) continue
            val k = TxKey.of(t.description)
            if (k.isEmpty()) continue
            groups.getOrPut(k + "|" + t.amount + "|" + t.accountId + "|" + t.cardId) { ArrayList() } += t
        }
        for (g in groups.values) {
            if (g.size < 2) continue
            val sorted = g.sortedWith(compareBy({ it.date }, { it.id }))
            for (i in 1 until sorted.size) {
                val a = sorted[i - 1]; val b = sorted[i]
                if (ChronoUnit.DAYS.between(a.date, b.date) <= 2) {
                    out += Anomaly(AnomalyKind.DUPLICATE, b.date, b.amount, b.amount, b.description.trim().take(60), b.categoryId, b.id, a.id, score = 5.0)
                }
            }
        }
    }

    /**
     * Categoria que, até hoje, já gastou mais que em qualquer mês anterior até o mesmo dia, e pelo menos 60% acima do normal
     * (com pelo menos 6 meses de comparação).
     */
    private fun categoryHigh(l: Ledger, today: LocalDate, out: MutableList<Anomaly>) {
        val ym = today.ym()
        val d = today.dayOfMonth
        val cats = l.categories(CategoryKind.EXPENSE)
        for (c in cats) {
            val now = l.monthTxs(ym).filter { it.type == TxType.EXPENSE && it.categoryId == c.id && !it.date.isAfter(today) }.sumOf { it.amount }
            if (now <= 0) continue
            val past = (1..12).map { ym.minusMonths(it.toLong()) }.map { m ->
                val until = minOf(d, m.lengthOfMonth())
                l.monthTxs(m).filter { it.type == TxType.EXPENSE && it.categoryId == c.id && it.date.dayOfMonth <= until }.sumOf { it.amount }.toDouble()
            }
            val active = past.count { it > 0 }
            if (active < 6) continue
            val arr = past.toDoubleArray()
            val med = Stats.median(arr)
            if (now > arr.max() && now >= 1.6 * med && med > 0) {
                out += Anomaly(AnomalyKind.CATEGORY_HIGH, today, now, med.roundToLong(), c.name, c.id, score = 1.0 + now / med)
            }
        }
    }
}

/* ===================== Ritmo das categorias e orçamento inteligente (RF-MOT-07) ===================== */

/** No ritmo atual, a categoria deve fechar o mês em [projected]; passa do limite por volta do dia [crossDay]. */
data class PaceAlert(val categoryId: Long, val limit: Long, val spent: Long, val projected: Long, val crossDay: Int)

data class BudgetSuggestion(val categoryId: Long, val amount: Long, val usual: Long)

object Pacing {
    private const val GRID = 40

    /**
     * Como a categoria costuma andar no mês: a parte do total gasta até cada ponto do mês (0 a 1), mediana dos meses
     * anteriores. Mercado que concentra no começo do mês chega a 0,8 no dia 5; lazer anda devagar.
     */
    fun curve(l: Ledger, categoryId: Long, ym: YearMonth, months: Int = 6): DoubleArray? {
        val shares = ArrayList<DoubleArray>()
        for (k in 1..months) {
            val m = ym.minusMonths(k.toLong())
            val txs = l.monthTxs(m).filter { it.type == TxType.EXPENSE && it.categoryId == categoryId }
            val total = txs.sumOf { it.amount }.toDouble()
            if (total <= 0) continue
            val len = m.lengthOfMonth()
            val g = DoubleArray(GRID + 1)
            for (t in txs) {
                val pos = ((t.date.dayOfMonth - 1).toDouble() / len * GRID).toInt().coerceIn(0, GRID)
                for (i in pos + 1..GRID) g[i] += t.amount / total
            }
            g[GRID] = 1.0
            shares += g
        }
        if (shares.size < 3) return null
        return DoubleArray(GRID + 1) { i -> Stats.median(DoubleArray(shares.size) { shares[it][i] }) }.also { it[GRID] = 1.0 }
    }

    private fun shareAt(curve: DoubleArray?, day: Int, len: Int): Double {
        if (curve == null) return (day.toDouble() / len).coerceIn(0.0, 1.0)
        val x = day.toDouble() / len * GRID
        val i = x.toInt().coerceIn(0, GRID - 1)
        val f = x - i
        return (curve[i] + (curve[i + 1] - curve[i]) * f).coerceIn(0.0, 1.0)
    }

    fun alerts(l: Ledger): List<PaceAlert> {
        val today = l.today
        val ym = today.ym()
        val d = today.dayOfMonth
        val len = ym.lengthOfMonth()
        if (d < 3) return emptyList()
        val out = ArrayList<PaceAlert>()
        for (b in l.s.budgets) {
            if (b.limitAmount <= 0) continue
            val cat = l.category[b.categoryId] ?: continue
            val spent = l.monthTxs(ym).filter { it.type == TxType.EXPENSE && it.categoryId == cat.id && !it.date.isAfter(today) }.sumOf { it.amount }
            if (spent >= b.limitAmount) continue // já passou: o app já mostra
            val c = curve(l, cat.id, ym)
            val share = shareAt(c, d, len)
            val history = (1..6).map { l.categorySpent(ym.minusMonths(it.toLong()), cat.id).toDouble() }.filter { it > 0 }
            val usual = if (history.isEmpty()) 0.0 else Stats.median(history.toDoubleArray())
            // Quanto mais o mês anda, mais vale o ritmo deste mês; no começo, vale mais o normal da categoria.
            val byPace = if (share >= 0.05) spent / share else spent.toDouble()
            val byUsual = spent + usual * (1 - share)
            val projected = maxOf(spent.toDouble(), share * byPace + (1 - share) * byUsual)
            if (projected <= b.limitAmount * 1.02) continue
            var cross = len
            for (k in d + 1..len) {
                val sk = shareAt(c, k, len)
                val est = if (share >= 1.0) spent.toDouble() else spent + (projected - spent) * (sk - share) / (1 - share)
                if (est >= b.limitAmount) { cross = k; break }
            }
            out += PaceAlert(cat.id, b.limitAmount, spent, projected.roundToLong(), cross)
        }
        return out.sortedBy { it.crossDay }
    }

    /**
     * Limites sugeridos: o 60º percentil dos últimos 6 meses (o normal, com folga), mais a tendência (no máximo ±20%),
     * arredondado para cima com 2 algarismos ("287,43" → "290").
     */
    fun suggestions(l: Ledger, max: Int = 3): List<BudgetSuggestion> {
        val ym = l.today.ym()
        val has = l.s.budgets.mapTo(HashSet()) { it.categoryId }
        val months = (1..6).map { ym.minusMonths(it.toLong()) }
        val totals = months.map { m -> l.monthSummary(m).expense.toDouble() }.filter { it > 0 }
        val monthly = if (totals.isEmpty()) 0.0 else Stats.median(totals.toDoubleArray())
        val out = ArrayList<BudgetSuggestion>()
        for (c in l.categories(CategoryKind.EXPENSE)) {
            if (c.id in has) continue
            val vals = months.map { l.categorySpent(it, c.id).toDouble() }
            if (vals.count { it > 0 } < 3) continue
            val arr = vals.toDoubleArray()
            val p60 = Stats.quantile(arr, 0.6)
            val xs = DoubleArray(arr.size) { -(it + 1).toDouble() } // meses para trás
            val slope = Stats.theilSen(xs, arr)
            val next = p60 + (slope).coerceIn(-0.2 * p60, 0.2 * p60)
            if (next <= 0 || next < 0.02 * monthly) continue
            out += BudgetSuggestion(c.id, niceCeil(next), Stats.median(arr).roundToLong())
        }
        return out.sortedByDescending { it.amount }.take(max)
    }

    /** Arredonda para cima com 2 algarismos significativos, em unidades inteiras da moeda (o valor está em centavos). */
    fun niceCeil(cents: Double): Long {
        val units = cents / 100.0
        if (units <= 1) return 100
        val step = 10.0.pow((log10(units).toInt() - 1).coerceAtLeast(0))
        return (ceil(units / step - 1e-9) * step * 100).roundToLong()
    }
}
