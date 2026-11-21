// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.domain.predict

import io.github.h3yk0.cryo.data.db.CategoryKind
import io.github.h3yk0.cryo.domain.BillCycle
import io.github.h3yk0.cryo.domain.Ledger
import io.github.h3yk0.cryo.domain.ym
import java.time.YearMonth
import kotlin.math.roundToLong

/**
 * RF-PRV-11 (1.6, pedido do Hanry, LA8-1: "com tantas contas, obrigações e planejamentos, como vou fechar o mês?"): o que a
 * pessoa planejou nos orçamentos e o ritmo ainda não mostra. Regras puras.
 *
 * Planejado = o limite × o quanto a pessoa costuma usar dele (a mediana dos últimos 6 meses completos, de 0% a 100%; com
 * menos de 2 meses com gasto na categoria, o limite inteiro) + o ajuste feito só no mês, inteiro (DAD-12).
 */
object BudgetPlan {
    const val MONTHS = 6

    /** Os meses completos que contam (os 6 antes de [ym], a partir do primeiro mês com algum lançamento). */
    private fun pastMonths(l: Ledger, ym: YearMonth): List<YearMonth> {
        val first = l.s.txs.minOfOrNull { it.date }?.ym() ?: return emptyList()
        return (1..MONTHS).map { ym.minusMonths(it.toLong()) }.filter { !it.isBefore(first) }
    }

    /** O quanto a pessoa costuma usar do limite [base] (0 a 1); null = menos de 2 meses com gasto (conta o limite inteiro). */
    fun usage(l: Ledger, categoryId: Long, base: Long, ym: YearMonth): Double? {
        if (base <= 0) return null
        val spent = pastMonths(l, ym).map { l.categorySpent(it, categoryId) }
        if (spent.count { it > 0 } < 2) return null
        val ratios = spent.map { (it.toDouble() / base).coerceIn(0.0, 1.0) }.sorted()
        val n = ratios.size
        return if (n % 2 == 1) ratios[n / 2] else (ratios[n / 2 - 1] + ratios[n / 2]) / 2
    }

    /** O ajuste só no mês (DAD-12: remanejamentos e limites daquele mês), inteiro. */
    private fun monthAdjustment(l: Ledger, categoryId: Long, ym: YearMonth): Long =
        l.budgetAdjustments(ym).filter { it.categoryId == categoryId }.sumOf { it.amount }

    /** O planejado da categoria no mês. */
    fun planned(l: Ledger, categoryId: Long, ym: YearMonth): Long {
        val base = l.budgetOf(categoryId)?.limitAmount ?: return 0L
        val use = usage(l, categoryId, base, ym) ?: 1.0
        return ((base * use).roundToLong() + monthAdjustment(l, categoryId, ym)).coerceAtLeast(0L)
    }

    /**
     * Este mês: por categoria com orçamento, quanto o planejado passa do que ela deve fechar no ritmo ([pace]: o total do mês
     * pela previsão da categoria, RF-PRV-06; sem previsão, o que já saiu). Orçamento estourado não muda nada.
     */
    fun thisMonth(l: Ledger, ym: YearMonth, pace: Map<Long, Long>): Map<Long, Long> {
        val out = HashMap<Long, Long>()
        val expense = l.categories(CategoryKind.EXPENSE).mapTo(HashSet()) { it.id }
        for (b in l.s.budgets) {
            if (b.categoryId !in expense) continue
            val spent = l.categorySpent(ym, b.categoryId)
            if (spent >= l.monthLimit(b.categoryId, ym)) continue
            val expected = pace[b.categoryId] ?: spent
            val extra = planned(l, b.categoryId, ym) - expected
            if (extra > 0) out[b.categoryId] = extra
        }
        return out
    }

    /**
     * Os meses seguintes (RF-MOT-16, RF-MOT-17): orçamento sem histórico conta inteiro; os outros, só o que o planejado passa
     * do que a pessoa costuma gastar na categoria. As contas fixas da categoria que vencem no mês e não estão no costume (as
     * que não são mensais, ou todas quando não há histórico) já entram na projeção e são descontadas.
     */
    fun futureMonth(l: Ledger, ym: YearMonth): Long {
        var total = 0L
        val expense = l.categories(CategoryKind.EXPENSE).mapTo(HashSet()) { it.id }
        val now = l.today.ym()
        for (b in l.s.budgets) {
            val cat = b.categoryId
            if (cat !in expense || b.limitAmount <= 0) continue
            val use = usage(l, cat, b.limitAmount, now)
            val bills = l.s.bills.filter { it.active && it.kind == CategoryKind.EXPENSE && it.categoryId == cat && BillCycle.partOf(it, ym) != null }
            val planned = (if (use == null) b.limitAmount else (b.limitAmount * use).roundToLong()) + monthAdjustment(l, cat, ym)
            val expected = if (use == null) {
                bills.sumOf { it.amount }
            } else {
                val months = pastMonths(l, now).map { l.categorySpent(it, cat) }.sorted()
                val typical = if (months.isEmpty()) 0L else months[months.size / 2]
                typical + bills.filter { it.every > 1 }.sumOf { it.amount }
            }
            total += (planned - expected).coerceAtLeast(0L)
        }
        return total
    }
}
