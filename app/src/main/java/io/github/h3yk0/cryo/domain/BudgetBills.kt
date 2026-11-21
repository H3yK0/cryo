// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.domain

import io.github.h3yk0.cryo.data.db.Bill
import io.github.h3yk0.cryo.data.db.CategoryKind
import java.time.YearMonth

/**
 * RF-ORC-09 (1.6, pedido do Hanry, LA7-1): o orçamento conta com as contas fixas. Para cada categoria, as contas fixas de
 * saída que vencem no mês são comparadas com o limite do orçamento; quando não cabem, o Cryo sugere — nunca muda sozinho
 * (PRI-09). A conta mensal sobe o limite de sempre; a que não é mensal (anual, semestral...) entra só nos meses em que
 * vence, como um ajuste do limite só daquele mês (DAD-12). Regras puras.
 */
object BudgetBills {
    /** Quantos meses à frente o Cryo olha as contas que não são mensais (o mês de agora e os 11 seguintes). */
    const val HORIZON = 12

    /** Um ajuste só no mês [ym]: o limite daquele mês sobe [delta] por causa das contas que vencem nele ([bills]). */
    data class MonthOnly(val ym: YearMonth, val delta: Long, val bills: List<Bill>)

    /**
     * A sugestão de uma categoria. [monthly] = as contas fixas mensais dela por mês; [current] = o limite de sempre (null =
     * sem orçamento); [suggested] = o limite de sempre sugerido; [monthOnly] = os ajustes só nos meses das contas que não
     * são mensais; [signature] = a "marca" das contas fixas da categoria (o "Agora não" vale até ela mudar).
     */
    data class Suggestion(
        val categoryId: Long,
        val monthly: Long,
        val current: Long?,
        val suggested: Long,
        val monthOnly: List<MonthOnly>,
        val bills: List<Bill>,
        val signature: Int,
    ) {
        val createsBudget: Boolean get() = current == null
        val raisesLimit: Boolean get() = current != null && suggested > current
    }

    /** As contas fixas de saída da categoria, ativas. */
    fun billsOf(l: Ledger, categoryId: Long): List<Bill> =
        l.s.bills.filter { it.active && it.kind == CategoryKind.EXPENSE && it.categoryId == categoryId }

    /** As contas fixas mensais da categoria que vencem em [ym] (o valor de cada mês). */
    fun monthlyIn(l: Ledger, categoryId: Long, ym: YearMonth): Long =
        billsOf(l, categoryId).filter { it.every <= 1 && BillCycle.partOf(it, ym) != null }.sumOf { it.amount }

    /** RF-ORC-07: o piso do orçamento pela renda numa categoria (as contas fixas mensais de agora). */
    fun floorFor(l: Ledger, categoryId: Long): Long = monthlyIn(l, categoryId, l.today.ym())

    /** A marca das contas fixas da categoria: muda quando alguma entra, sai ou muda de valor, ritmo ou prazo. */
    fun signature(bills: List<Bill>): Int =
        bills.sortedBy { it.id }.fold(17) { h, b ->
            var x = h * 31 + b.id.hashCode()
            x = x * 31 + b.amount.hashCode()
            x = x * 31 + b.every
            x = x * 31 + b.installments
            x = x * 31 + b.startYm
            x * 31 + b.endYm
        }

    /**
     * As sugestões a partir de [from]. [dismissed] = categoria → a marca que estava quando a pessoa tocou "Agora não". Só
     * categorias de saída; sem orçamento, só quando há contas fixas mensais (com só uma conta anual, não sugere criar).
     */
    fun suggestions(l: Ledger, from: YearMonth, dismissed: Map<Long, Int> = emptyMap()): List<Suggestion> =
        l.categories(CategoryKind.EXPENSE).mapNotNull { cat -> suggestion(l, cat.id, from) }
            .filter { dismissed[it.categoryId] != it.signature }

    /** A sugestão de uma categoria (null = as contas fixas cabem, ou não há o que sugerir). */
    fun suggestion(l: Ledger, categoryId: Long, from: YearMonth): Suggestion? {
        val bills = billsOf(l, categoryId)
        if (bills.isEmpty()) return null
        val current = l.budgetOf(categoryId)?.limitAmount
        val monthly = monthlyIn(l, categoryId, from)
        if (current == null && monthly <= 0) return null
        val base = maxOf(current ?: 0L, monthly)
        val monthOnly = ArrayList<MonthOnly>()
        for (k in 0 until HORIZON) {
            val ym = from.plusMonths(k.toLong())
            val others = bills.filter { it.every > 1 && BillCycle.partOf(it, ym) != null }
            if (others.isEmpty()) continue
            val due = monthlyIn(l, categoryId, ym) + others.sumOf { it.amount }
            // O limite daquele mês com o limite de sempre sugerido e os ajustes que já existem nele.
            val adj = l.budgetAdjustments(ym).filter { it.categoryId == categoryId }.sumOf { it.amount }
            val limit = (base + adj).coerceAtLeast(0L)
            if (due > limit) monthOnly += MonthOnly(ym, due - limit, others)
        }
        if (current != null && monthly <= current && monthOnly.isEmpty()) return null
        return Suggestion(categoryId, monthly, current, base, monthOnly, bills, signature(bills))
    }
}
