// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.domain

import io.github.h3yk0.cryo.data.db.CategoryKind
import io.github.h3yk0.cryo.data.db.TxType
import java.time.YearMonth
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToLong

/* ===================== Previsão do fim do mês ===================== */

data class ForecastPoint(val day: Int, val actual: Long?, val projected: Long?)

data class MonthForecast(
    val ym: YearMonth,
    val dayOfMonth: Int,
    val daysInMonth: Int,
    val incomeSoFar: Long,
    val expenseSoFar: Long,
    val scheduledIncome: Long,
    val scheduledExpense: Long,
    val pendingBillsIncome: Long,
    val pendingBillsExpense: Long,
    val dailyRate: Long,
    val projectedVariable: Long,
    val usedHistory: Boolean,
    val series: List<ForecastPoint>,
) {
    val projectedIncome: Long get() = incomeSoFar + scheduledIncome + pendingBillsIncome
    val projectedExpense: Long get() = expenseSoFar + scheduledExpense + pendingBillsExpense + projectedVariable
    val projectedResult: Long get() = projectedIncome - projectedExpense
    val currentResult: Long get() = incomeSoFar - expenseSoFar
    val remainingDays: Int get() = daysInMonth - dayOfMonth

    /** Quanto dá para gastar por dia (incluindo hoje) e ainda terminar o mês no zero a zero. */
    val dailyAllowance: Long
        get() {
            val free = projectedIncome - expenseSoFar - scheduledExpense - pendingBillsExpense
            return free / (remainingDays + 1)
        }
}

private fun isVariable(t: io.github.h3yk0.cryo.data.db.Tx) =
    t.type == TxType.EXPENSE && t.billId == null && t.installmentTotal <= 1

fun Ledger.forecast(): MonthForecast {
    val ym = today.ym()
    val days = ym.lengthOfMonth()
    val d = today.dayOfMonth
    val txs = monthTxs(ym)
    val past = txs.filter { !it.date.isAfter(today) }
    val future = txs.filter { it.date.isAfter(today) }

    val incomeSoFar = past.filter { it.type == TxType.INCOME }.sumOf { it.amount }
    val expenseSoFar = past.filter { it.type == TxType.EXPENSE }.sumOf { it.amount }
    val schedInc = future.filter { it.type == TxType.INCOME }.sumOf { it.amount }
    val schedExp = future.filter { it.type == TxType.EXPENSE }.sumOf { it.amount }

    val pending = billStatuses(ym).filter { it.payment == null }
    val pendExp = pending.filter { it.bill.kind == CategoryKind.EXPENSE }.sumOf { it.bill.amount }
    val pendInc = pending.filter { it.bill.kind == CategoryKind.INCOME }.sumOf { it.bill.amount }

    // Ritmo de gastos do dia a dia (sem contas fixas e sem parcelas).
    val variableSoFar = past.filter(::isVariable).sumOf { it.amount }
    val currentRate = variableSoFar.toDouble() / d
    val hist = (1..3).map { ym.minusMonths(it.toLong()) }
        .map { m -> monthTxs(m).filter(::isVariable).sumOf { it.amount }.toDouble() / m.lengthOfMonth() }
        .filter { it > 0 }
    val histRate = if (hist.isEmpty()) null else hist.average()
    val usedHistory = histRate != null && d < 10
    val rate = when {
        histRate == null -> currentRate
        d >= 10 -> currentRate
        else -> { val w = d / 10.0; w * currentRate + (1 - w) * histRate }
    }
    val remaining = days - d
    val projectedVariable = (rate * remaining).roundToLong()

    // Série diária: real até hoje, projetada depois.
    val net = LongArray(days + 1)
    for (t in past) when (t.type) {
        TxType.INCOME -> net[t.date.dayOfMonth] += t.amount
        TxType.EXPENSE -> net[t.date.dayOfMonth] -= t.amount
        else -> {}
    }
    val points = ArrayList<ForecastPoint>(days)
    var cum = 0L
    for (day in 1..d) {
        cum += net[day]
        points += ForecastPoint(day, cum, if (day == d) cum else null)
    }
    if (d < days) {
        val proj = DoubleArray(days + 1)
        for (t in future) when (t.type) {
            TxType.INCOME -> proj[t.date.dayOfMonth] += t.amount.toDouble()
            TxType.EXPENSE -> proj[t.date.dayOfMonth] -= t.amount.toDouble()
            else -> {}
        }
        for (p in pending) {
            val day = max(p.due.dayOfMonth, d + 1).coerceAtMost(days)
            proj[day] += if (p.bill.kind == CategoryKind.INCOME) p.bill.amount.toDouble() else -p.bill.amount.toDouble()
        }
        var pc = cum.toDouble()
        for (day in d + 1..days) {
            pc += proj[day] - rate
            points += ForecastPoint(day, null, pc.roundToLong())
        }
    } else if (pending.isNotEmpty()) {
        // Último dia do mês: o que ainda está pendente entra direto no ponto final.
        val pendNet = pending.sumOf { if (it.bill.kind == CategoryKind.INCOME) it.bill.amount else -it.bill.amount }
        points += ForecastPoint(days, null, cum + pendNet)
    }

    return MonthForecast(
        ym = ym, dayOfMonth = d, daysInMonth = days,
        incomeSoFar = incomeSoFar, expenseSoFar = expenseSoFar,
        scheduledIncome = schedInc, scheduledExpense = schedExp,
        pendingBillsIncome = pendInc, pendingBillsExpense = pendExp,
        dailyRate = rate.roundToLong(), projectedVariable = projectedVariable,
        usedHistory = usedHistory, series = points,
    )
}

/* ======================== Fluxo do dinheiro ======================== */

enum class FlowKind { CATEGORY, SURPLUS, FROM_BALANCE, INVEST, GOALS, OTHERS }

data class FlowNode(val label: String, val value: Long, val color: Int, val kind: FlowKind = FlowKind.CATEGORY)

data class MoneyFlow(val sources: List<FlowNode>, val destinations: List<FlowNode>) {
    val total: Long get() = max(sources.sumOf { it.value }, destinations.sumOf { it.value })
    val isEmpty: Boolean get() = total == 0L
}

fun Ledger.moneyFlow(ym: YearMonth, maxDestinations: Int = 6): MoneyFlow {
    val until = if (ym == today.ym()) today.dayOfMonth else null
    val sources = byCategory(ym, CategoryKind.INCOME, until).map {
        FlowNode(it.category?.name ?: "Sem categoria", it.total, it.category?.color ?: 0xFF8A9296.toInt())
    }.toMutableList()
    val expenses = byCategory(ym, CategoryKind.EXPENSE, until)
    val dest = expenses.take(maxDestinations).map {
        FlowNode(it.category?.name ?: "Sem categoria", it.total, it.category?.color ?: 0xFF8A9296.toInt())
    }.toMutableList()
    val others = expenses.drop(maxDestinations).sumOf { it.total }
    if (others > 0) dest += FlowNode("Outras categorias", others, 0xFF9E9E9E.toInt(), FlowKind.OTHERS)

    val summary = monthSummary(ym, until)
    if (summary.invested > 0) dest += FlowNode("Investimentos", summary.invested, 0xFF3F51B5.toInt(), FlowKind.INVEST)
    if (summary.saved > 0) dest += FlowNode("Metas e caixinhas", summary.saved, 0xFF00897B.toInt(), FlowKind.GOALS)

    val totalIn = sources.sumOf { it.value }
    val totalOut = dest.sumOf { it.value }
    if (totalIn > totalOut) dest += FlowNode("Sobrou", totalIn - totalOut, 0xFF2E7D32.toInt(), FlowKind.SURPLUS)
    if (totalOut > totalIn) sources += FlowNode("Saldo que você já tinha", totalOut - totalIn, 0xFF78909C.toInt(), FlowKind.FROM_BALANCE)
    return MoneyFlow(sources, dest)
}

/* ========================= Evolução mensal ========================= */

data class MonthBar(val ym: YearMonth, val income: Long, val expense: Long)

fun Ledger.lastMonths(n: Int = 6, end: YearMonth = today.ym()): List<MonthBar> =
    (n - 1 downTo 0).map { end.minusMonths(it.toLong()) }.map {
        val s = monthSummary(it)
        MonthBar(it, s.income, s.expense)
    }

/* ===================== Percepções automáticas ===================== */

data class Insight(val text: String, val positive: Boolean?)

/** Frases curtas que o app "percebe" sozinho, comparando com o mesmo período do mês anterior. */
fun Ledger.insights(): List<Insight> {
    val out = ArrayList<Insight>()
    val ym = today.ym()
    val d = today.dayOfMonth
    val prev = ym.minusMonths(1)
    val prevUntil = minOf(d, prev.lengthOfMonth())
    val now = monthSummary(ym, d)
    val before = monthSummary(prev, prevUntil)

    if (before.expense > 0 && now.expense > 0) {
        val pct = ((now.expense - before.expense) * 100.0 / before.expense).roundToLong()
        if (abs(pct) >= 5) {
            out += if (pct < 0) Insight("Até hoje você gastou ${abs(pct)}% menos que no mesmo período de ${Dates.monthName(prev).lowercase()}.", true)
            else Insight("Até hoje você gastou $pct% mais que no mesmo período de ${Dates.monthName(prev).lowercase()}.", false)
        }
    }

    val cats = byCategory(ym, CategoryKind.EXPENSE, d)
    val prevCats = byCategory(prev, CategoryKind.EXPENSE, prevUntil).associate { it.category?.id to it.total }
    cats.firstOrNull()?.let { top ->
        if (top.total > 0 && now.expense > 0) {
            val share = (top.total * 100.0 / now.expense).roundToLong()
            out += Insight("Seu maior gasto do mês é ${top.category?.name ?: "sem categoria"}: $share% do total.", null)
        }
    }
    cats.mapNotNull { c ->
        val p = prevCats[c.category?.id] ?: return@mapNotNull null
        if (p < 2000 || c.total < 2000) return@mapNotNull null
        val pct = ((c.total - p) * 100.0 / p).roundToLong()
        if (pct >= 30) c to pct else null
    }.maxByOrNull { it.second }?.let { (c, pct) ->
        out += Insight("${c.category?.name ?: "Uma categoria"} subiu $pct% em relação ao mesmo período do mês passado.", false)
    }

    budgetUsages(ym).firstOrNull { it.ratio >= 1f }?.let {
        out += Insight("O orçamento de ${it.category.name} já passou do limite em ${Money.format(-it.remaining)}.", false)
    }
    val overdue = billStatuses(ym).count { it.state == BillState.OVERDUE }
    if (overdue > 0) out += Insight(if (overdue == 1) "Há 1 conta fixa vencida." else "Há $overdue contas fixas vencidas.", false)

    if (now.income > 0 && now.expense < now.income && d >= 15) {
        val pct = ((now.income - now.expense) * 100.0 / now.income).roundToLong()
        if (pct >= 10) out += Insight("Você está guardando $pct% do que entrou neste mês. Continue assim!", true)
    }
    return out
}
