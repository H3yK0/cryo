// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.domain.engine

import io.github.h3yk0.cryo.data.db.Goal
import io.github.h3yk0.cryo.data.db.TxType
import io.github.h3yk0.cryo.domain.Ledger
import io.github.h3yk0.cryo.domain.ym
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.pow
import kotlin.math.roundToLong

/* ===================== Métodos numéricos com plano B ===================== */

object Solver {
    /**
     * Acha o zero de [f] entre [lo] e [hi] (onde [f] muda de sinal) pelo método de Newton, com a bissecção de reserva:
     * se o passo de Newton sai do intervalo ou anda pouco, divide o intervalo ao meio. Nunca diverge e nunca dá NaN.
     * Devolve null se [f] não muda de sinal no intervalo.
     */
    fun root(lo0: Double, hi0: Double, tol: Double = 1e-12, maxIter: Int = 200, f: (Double) -> Double): Double? {
        var lo = lo0; var hi = hi0
        var flo = f(lo); val fhi = f(hi)
        if (!flo.isFinite() || !fhi.isFinite()) return null
        if (flo == 0.0) return lo
        if (fhi == 0.0) return hi
        if ((flo > 0) == (fhi > 0)) return null
        var x = (lo + hi) / 2
        repeat(maxIter) {
            val fx = f(x)
            if (!fx.isFinite()) return null
            if (abs(fx) < 1e-12 || hi - lo < tol) return x
            if ((fx > 0) == (flo > 0)) { lo = x; flo = fx } else hi = x
            // Derivada numérica (diferença central): basta para as funções suaves daqui (juros e rendimento).
            val h = maxOf(1e-9, abs(x) * 1e-7)
            val d = (f(x + h) - f(x - h)) / (2 * h)
            val newton = if (d != 0.0 && d.isFinite()) x - fx / d else Double.NaN
            x = if (newton.isFinite() && newton > lo && newton < hi) newton else (lo + hi) / 2
        }
        return x
    }
}

/* ========================== Dívidas (RF-MOT-08) ========================== */

/** Juros de uma dívida, ao mês e ao ano. [known] = falso quando não dá para saber (sem parcelas e sem juros lançados). */
data class DebtRate(val debtId: Long, val monthly: Double, val known: Boolean) {
    val yearly: Double get() = (1 + monthly).pow(12) - 1
}

enum class PayoffStrategy { AVALANCHE, SNOWBALL }

/**
 * Plano para quitar: com [extra] a mais por mês, todas as dívidas acabam em [months] meses (null = nunca, no ritmo atual),
 * pagando [interest] de juros. [baselineMonths]/[baselineInterest] = pagando só as parcelas.
 */
data class PayoffPlan(
    val strategy: PayoffStrategy,
    val extra: Long,
    val months: Int?,
    val interest: Long,
    val freeYm: YearMonth?,
    val order: List<Long>,
    val baselineMonths: Int?,
    val baselineInterest: Long,
) {
    val savedInterest: Long get() = (baselineInterest - interest).coerceAtLeast(0)
    val monthsSaved: Int? get() = if (months != null && baselineMonths != null) (baselineMonths - months).coerceAtLeast(0) else null
}

/** Antecipar as últimas [installments] parcelas: valem [nominal], mas o justo hoje é [fair] (desconto dos juros). */
data class Anticipation(val debtId: Long, val installments: Int, val nominal: Long, val fair: Long) {
    val saving: Long get() = (nominal - fair).coerceAtLeast(0)
}

object DebtPlanner {
    const val MAX_MONTHS = 600

    /** Valor hoje de [n] parcelas de [pmt] com juros [i] ao mês (Tabela Price). */
    fun presentValue(pmt: Double, n: Int, i: Double): Double =
        if (abs(i) < 1e-12) pmt * n else pmt * (1 - (1 + i).pow(-n)) / i

    /**
     * Os juros embutidos: a taxa que faz [n] parcelas de [pmt] valerem [pv] hoje. 0 quando as parcelas somam o saldo (sem
     * juros, ou o saldo cadastrado já inclui os juros).
     */
    fun impliedMonthlyRate(pv: Double, pmt: Double, n: Int): Double {
        if (n <= 0 || pmt <= 0 || pv <= 0) return 0.0
        if (pmt * n <= pv + n) return 0.0 // diferença de centavos: arredondamento
        return Solver.root(1e-12, 1.0) { i -> presentValue(pmt, n, i) - pv } ?: 1.0
    }

    fun rates(l: Ledger): List<DebtRate> = l.activeDebts.mapNotNull { d ->
        val info = l.debtInfo(d)
        if (info.isPaidOff) return@mapNotNull null
        if (info.hasInstallments && info.installmentsLeft > 0) {
            DebtRate(d.id, impliedMonthlyRate(info.remaining.toDouble(), d.installmentAmount.toDouble(), contractLeft(info)), true)
        } else {
            // Sem parcelas: os juros lançados como ajuste nos últimos 6 meses, por mês, sobre o saldo.
            val since = l.today.minusDays(180)
            val adj = l.s.debtAdjustments.filter { it.debtId == d.id && it.amount > 0 && !it.date.isBefore(since) && !it.date.isAfter(l.today) }
            if (adj.isEmpty() || info.remaining <= 0) DebtRate(d.id, 0.0, false) else {
                val first = adj.minOf { it.date }
                val months = maxOf(1.0, ChronoUnit.DAYS.between(first, l.today) / 30.44)
                DebtRate(d.id, (adj.sumOf { it.amount } / months / info.remaining).coerceIn(0.0, 1.0), true)
            }
        }
    }

    /**
     * Parcelas que faltam pelo contrato (total − pagas). Quem cadastra o saldo para quitar que o banco mostra (menor que a
     * soma das parcelas) tem mais parcelas pelo contrato do que "saldo ÷ parcela": a diferença são os juros embutidos.
     */
    fun contractLeft(info: io.github.h3yk0.cryo.domain.DebtInfo): Int {
        val byContract = info.debt.installmentCount - info.installmentsPaid
        return if (info.debt.installmentCount > 0 && byContract > info.installmentsLeft) byContract else info.installmentsLeft
    }

    private class Item(val id: Long, var balance: Double, val rate: Double, val minimum: Double)

    fun plan(l: Ledger, extra: Long, strategy: PayoffStrategy): PayoffPlan? {
        val rates = rates(l).associateBy { it.debtId }
        val open = l.activeDebts.map { l.debtInfo(it) }.filter { !it.isPaidOff && it.remaining > 0 }
        if (open.isEmpty()) return null
        fun items() = open.map {
            val min = when {
                it.hasInstallments -> it.debt.installmentAmount.toDouble()
                it.monthlyNeeded != null -> it.monthlyNeeded.toDouble()
                else -> 0.0
            }
            Item(it.debt.id, it.remaining.toDouble(), rates[it.debt.id]?.monthly ?: 0.0, min)
        }
        val order = items().sortedWith(
            if (strategy == PayoffStrategy.AVALANCHE) compareByDescending<Item> { it.rate }.thenBy { it.balance }
            else compareBy<Item> { it.balance }.thenByDescending { it.rate },
        ).map { it.id }
        val (bm, bi) = simulate(items(), 0.0, order, rollover = false)
        val (m, i) = simulate(items(), extra.toDouble(), order, rollover = true)
        return PayoffPlan(strategy, extra, m, i.roundToLong(), m?.let { l.today.ym().plusMonths(it.toLong()) }, order, bm, bi.roundToLong())
    }

    /** Mês a mês: juros, parcelas e o extra (mais as parcelas das que acabaram) na dívida da vez. */
    private fun simulate(items: List<Item>, extra: Double, order: List<Long>, rollover: Boolean): Pair<Int?, Double> {
        val byId = items.associateBy { it.id }
        val ordered = order.mapNotNull { byId[it] }
        var interest = 0.0
        for (month in 1..MAX_MONTHS) {
            var pool = extra
            for (it in ordered) {
                if (it.balance <= 0.5) { if (rollover) pool += it.minimum; continue }
                val j = it.balance * it.rate
                it.balance += j; interest += j
                val pay = minOf(it.minimum, it.balance)
                it.balance -= pay
                if (rollover) pool += it.minimum - pay
            }
            for (it in ordered) {
                if (pool <= 0) break
                if (it.balance <= 0.5) continue
                val pay = minOf(pool, it.balance)
                it.balance -= pay; pool -= pay
            }
            if (ordered.all { it.balance <= 0.5 }) return month to interest
            // Juros maiores que as parcelas: a dívida cresce sem parar (para antes de virar um número sem sentido).
            if (ordered.any { it.balance > 1e15 }) return null to minOf(interest, 1e15)
            // Sem pagamento nenhum e com saldo: nunca acaba.
            if (ordered.none { it.minimum > 0 } && extra <= 0) return null to interest
        }
        return null to minOf(interest, 1e15)
    }

    fun anticipate(l: Ledger, debtId: Long, installments: Int): Anticipation? {
        val d = l.debt[debtId] ?: return null
        val info = l.debtInfo(d)
        if (!info.hasInstallments || info.installmentsLeft <= 0 || installments <= 0) return null
        val n = contractLeft(info)
        val k = installments.coerceAtMost(n)
        val i = impliedMonthlyRate(info.remaining.toDouble(), d.installmentAmount.toDouble(), n)
        val pmt = d.installmentAmount.toDouble()
        var fair = 0.0
        for (t in n - k + 1..n) fair += pmt / (1 + i).pow(t)
        return Anticipation(debtId, k, (k * pmt).roundToLong(), fair.roundToLong())
    }
}

/* ======================= Investimentos (RF-MOT-09) ======================= */

/** Rendimento: [annual] = ao ano (null com menos de 30 dias); [total] = no período todo. */
data class InvestReturn(val investmentId: Long?, val annual: Double?, val total: Double, val days: Long, val putIn: Long, val value: Long) {
    fun real(inflation: Double): Double? = annual?.let { (1 + it) / (1 + inflation) - 1 }
}

object Returns {
    const val MIN_DAYS = 30L

    /**
     * Taxa interna de retorno com datas (XIRR): a taxa ao ano que zera o valor presente de todos os aportes (negativos),
     * resgates e o valor de hoje (positivos).
     */
    fun xirr(flows: List<Pair<LocalDate, Double>>): Double? {
        if (flows.size < 2 || flows.none { it.second < 0 } || flows.none { it.second > 0 }) return null
        val t0 = flows.minOf { it.first }
        val ts = flows.map { ChronoUnit.DAYS.between(t0, it.first) / 365.0 }
        fun npv(r: Double): Double {
            var s = 0.0
            for (k in flows.indices) s += flows[k].second / (1 + r).pow(ts[k])
            return s
        }
        return Solver.root(-0.9999, 100.0, tol = 1e-10) { npv(it) }
    }

    fun of(l: Ledger, investmentId: Long?): InvestReturn? {
        val ids = if (investmentId == null) l.s.investments.filter { !it.archived }.map { it.id }.toSet() else setOf(investmentId)
        val flows = ArrayList<Pair<LocalDate, Double>>()
        var putIn = 0L
        for (t in l.s.txs) {
            if (t.investmentId !in ids || t.date.isAfter(l.today)) continue
            when (t.type) {
                TxType.INVEST_IN -> { flows += t.date to -t.amount.toDouble(); putIn += t.amount }
                TxType.INVEST_OUT -> flows += t.date to t.amount.toDouble()
                else -> {}
            }
        }
        if (flows.isEmpty()) return null
        val value = ids.sumOf { l.investmentValue(it) }
        val first = flows.minOf { it.first }
        val days = ChronoUnit.DAYS.between(first, l.today)
        val outs = flows.filter { it.second > 0 }.sumOf { it.second }
        val total = if (putIn > 0) (value + outs - putIn) / putIn else 0.0
        val annual = if (days < MIN_DAYS) null else xirr(flows + (l.today to value.toDouble()))
        return InvestReturn(investmentId, annual, total, days, putIn, value)
    }
}

/* ========================== Metas (RF-MOT-10) ========================== */

data class GoalOutlook(
    val goalId: Long,
    val saved: Long,
    val target: Long,
    /** Depósito típico por mês (mediana dos últimos 6 meses). */
    val pace: Long,
    val etaYm: YearMonth?,
    /** Chance de chegar lá até o prazo (null sem prazo). */
    val chanceByDeadline: Double?,
    val neededMonthly: Long?,
) {
    val done: Boolean get() = saved >= target
}

object GoalPlanner {
    const val SCENARIOS = 1000

    fun of(l: Ledger, g: Goal, seed: Long, monthlyYield: Double = 0.0): GoalOutlook {
        val saved = l.goalSaved(g.id)
        val ym = l.today.ym()
        val created = java.time.Instant.ofEpochMilli(g.createdAt).atZone(ZoneId.systemDefault()).toLocalDate()
        val firstTx = l.goalHistory(g.id).minOfOrNull { it.date }
        val start = listOfNotNull(firstTx, created).minOrNull()?.ym() ?: ym
        val months = (0..11).map { ym.minusMonths(it.toLong()) }.filter { !it.isBefore(start) }
        val perMonth = months.associateWith { 0.0 }.toMutableMap()
        for (t in l.goalHistory(g.id)) {
            if (t.date.isAfter(l.today)) continue
            val m = t.date.ym()
            if (m !in perMonth) continue
            perMonth[m] = perMonth.getValue(m) + when (t.type) { TxType.GOAL_IN -> t.amount.toDouble(); TxType.GOAL_OUT -> -t.amount.toDouble(); else -> 0.0 }
        }
        val recent = months.take(6).map { perMonth.getValue(it) }.toDoubleArray()
        val med = Stats.median(recent)
        val pace = when {
            med > 0 -> med
            recent.average() > 0 -> recent.average()
            else -> 0.0
        }
        val missing = g.target - saved
        val eta = when {
            missing <= 0 -> ym
            pace <= 0 -> null
            else -> ym.plusMonths(minOf(ceil(missing / pace).toLong(), 600L))
        }
        val deadline = g.deadline
        var chance: Double? = null
        var needed: Long? = null
        if (deadline != null) {
            val n = (ChronoUnit.MONTHS.between(ym, deadline.ym()) + 1).toInt().coerceAtLeast(1)
            needed = when {
                missing <= 0 -> 0L
                monthlyYield <= 0.0 -> (missing + n - 1) / n
                else -> {
                    val gr = (1 + monthlyYield).pow(n)
                    ((g.target - saved * gr) * monthlyYield / (gr - 1)).coerceAtLeast(0.0).roundToLong()
                }
            }
            chance = when {
                missing <= 0 -> 1.0
                perMonth.values.none { it > 0 } -> 0.0
                else -> {
                    val sample = perMonth.values.toDoubleArray()
                    val rng = Rng(Rng.mix(seed, g.id))
                    var ok = 0
                    repeat(SCENARIOS) {
                        var s = saved.toDouble()
                        for (k in 0 until n) s = s * (1 + monthlyYield) + sample[rng.nextInt(sample.size)]
                        if (s >= g.target) ok++
                    }
                    ok.toDouble() / SCENARIOS
                }
            }
        }
        return GoalOutlook(g.id, saved, g.target, pace.roundToLong(), eta, chance, needed)
    }
}
