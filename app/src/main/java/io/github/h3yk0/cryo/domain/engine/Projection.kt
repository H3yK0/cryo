// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.domain.engine

import io.github.h3yk0.cryo.data.db.CategoryKind
import io.github.h3yk0.cryo.data.db.CreditCard
import io.github.h3yk0.cryo.data.db.TxType
import io.github.h3yk0.cryo.domain.CardMath
import io.github.h3yk0.cryo.domain.Ledger
import io.github.h3yk0.cryo.domain.dayClamped
import io.github.h3yk0.cryo.domain.ym
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import kotlin.math.exp
import kotlin.math.roundToLong

/** Saldo no fim de um mês da projeção (P10, P50 e P90 dos cenários). */
data class MonthEnd(val ym: YearMonth, val low: Long, val mid: Long, val high: Long)

/**
 * RF-MOT-04: o saldo das contas (as que entram no total) dia a dia nos próximos meses. Índice 0 = hoje.
 * [chanceNegative] = parte dos cenários em que o saldo fica negativo em algum dia.
 */
data class Projection(
    val start: LocalDate,
    val horizonDays: Int,
    val startBalance: Long,
    val low: List<Long>,
    val mid: List<Long>,
    val high: List<Long>,
    val monthEnds: List<MonthEnd>,
    val tightestDate: LocalDate,
    val tightestMid: Long,
    val chanceNegative: Double,
    val firstNegativeMonth: YearMonth?,
    val scenarios: Int,
    val certainty: Certainty,
) {
    fun dateAt(i: Int): LocalDate = start.plusDays(i.toLong())

    /** Os primeiros [months] meses (para mostrar 3, 6 ou 12 meses sem calcular de novo). */
    fun monthsAhead(months: Int): Int = ChronoUnit.DAYS.between(start, start.plusMonths(months.toLong())).toInt().coerceAtMost(horizonDays)

    /** O primeiro dia, dentro dos primeiros [days], em que o saldo mais provável fica abaixo de zero (ou null). */
    fun firstNegativeWithin(days: Int): LocalDate? {
        for (i in 1..days.coerceAtMost(horizonDays)) if (mid[i] < 0) return dateAt(i)
        return null
    }

    /** O dia mais apertado dentro dos primeiros [days] dias. */
    fun tightestWithin(days: Int): Pair<LocalDate, Long> {
        var bi = 0
        for (i in 1..days.coerceAtMost(horizonDays)) if (mid[i] < mid[bi]) bi = i
        return dateAt(bi) to mid[bi]
    }
}

object Projections {
    const val SCENARIOS = 400
    const val MAX_MONTHS = 12

    /** RF-MOT-17: até o fim do ano que vem, no máximo (de outubro em diante a pessoa pode escolher o ano que vem). */
    const val MAX_UNTIL_MONTHS = 27L

    /** Faturas vencidas há mais tempo que isto provavelmente foram pagas fora do app: não entram na conta. */
    private const val OLD_INVOICE_DAYS = 45L

    /**
     * [until]: o último dia da projeção (RF-MOT-17: 31 de dezembro, deste ano ou do que vem); sem ele, [months] meses.
     * [budgetPace]: com ele, entra também o orçamento planejado (RF-PRV-11): neste mês, o que o planejado passa do ritmo de
     * cada categoria (o mapa é o total do mês previsto por categoria); nos seguintes, a regra dos meses seguintes.
     */
    fun project(
        l: Ledger, recurrences: List<Recurrence>, seed: Long, months: Int = MAX_MONTHS, scenarios: Int = SCENARIOS,
        until: LocalDate? = null, budgetPace: Map<Long, Long>? = null,
    ): Projection {
        val today = l.today
        val end = until?.takeIf { it.isAfter(today) }?.let { minOf(it, today.plusMonths(MAX_UNTIL_MONTHS)) }
            ?: today.plusMonths(months.coerceIn(1, MAX_MONTHS).toLong())
        val h = ChronoUnit.DAYS.between(today, end).toInt().coerceAtLeast(1)
        val included = l.s.accounts.filter { it.includeInTotal && !it.archived }.mapTo(HashSet()) { it.id }
        fun inc(id: Long?) = id != null && id in included
        fun index(date: LocalDate): Int = ChronoUnit.DAYS.between(today, date).toInt().coerceAtLeast(1)

        val det = DoubleArray(h + 2)
        fun add(date: LocalDate, v: Double) { val i = index(date); if (i <= h) det[i] += v }

        // 1. O que já está lançado para depois de hoje.
        for (t in l.s.txs) {
            if (!t.date.isAfter(today) || t.date.isAfter(end)) continue
            val a = t.amount.toDouble()
            when (t.type) {
                TxType.EXPENSE, TxType.CARD_PAYMENT, TxType.INVEST_IN, TxType.GOAL_IN -> if (inc(t.accountId)) add(t.date, -a)
                TxType.INCOME, TxType.INVEST_OUT, TxType.GOAL_OUT, TxType.DEBT_IN -> if (inc(t.accountId)) add(t.date, a)
                TxType.TRANSFER -> { if (inc(t.accountId)) add(t.date, -a); if (inc(t.toAccountId)) add(t.date, a) }
            }
        }

        // 2. Faturas dos cartões (pagas no vencimento, pela conta do cartão).
        val cards = l.activeCards.filter { it.payAccountId == null || it.payAccountId in included }
        val cardById = cards.associateBy { it.id }
        for (c in cards) {
            for (ym in l.invoiceMonths(c)) {
                val inv = l.invoice(c, ym)
                if (inv.remaining <= 0 || inv.due.isAfter(end)) continue
                if (inv.due.isBefore(today.minusDays(OLD_INVOICE_DAYS))) continue
                add(inv.due, -inv.remaining.toDouble())
            }
        }
        fun cardDueDate(c: CreditCard, purchase: LocalDate) = CardMath.dueDate(c, CardMath.invoiceFor(c, purchase))

        // 3. Contas fixas ainda não pagas (no cartão, entram na fatura).
        var m = today.ym()
        while (!m.isAfter(end.ym())) {
            for (bs in l.billStatuses(m)) {
                if (bs.payment != null || bs.due.isAfter(end)) continue
                if (bs.due.isBefore(today) && m != today.ym()) continue
                val b = bs.bill
                val sign = if (b.kind == CategoryKind.INCOME) 1.0 else -1.0
                val card = b.cardId?.let { cardById[it] }
                when {
                    card != null && b.kind == CategoryKind.EXPENSE -> add(cardDueDate(card, maxOf(bs.due, today.plusDays(1))), -b.amount.toDouble())
                    b.cardId != null -> {}
                    b.accountId == null || inc(b.accountId) -> add(bs.due, sign * b.amount)
                }
            }
            m = m.plusMonths(1)
        }

        // 4. Parcelas de dívidas (menos o que já está agendado para elas). ERR-061 (auditoria da 1.6): só conta o pagamento
        // agendado DENTRO do período (o de depois não tira dinheiro daqui), e cada pagamento cobre primeiro a parcela de
        // vencimento mais perto dele; o que sobra vai para as seguintes e, depois da última, para as anteriores. Antes, um
        // pagamento de março "pagava" a parcela de dezembro (o saldo de dezembro a fevereiro saía maior do que vai ser) e um
        // pagamento depois do fim do período tirava uma parcela de dentro dele. A soma não muda: nada conta duas vezes.
        for (d in l.activeDebts) {
            val info = l.debtInfo(d)
            if (info.isPaidOff || !info.hasInstallments || info.nextDue == null) continue
            if (d.accountId != null && !inc(d.accountId)) continue
            val n = info.installmentsLeft
            val dues = Array(n) { k -> if (k == 0) info.nextDue else info.nextDue.ym().plusMonths(k.toLong()).dayClamped(d.dueDay) }
            val open = LongArray(n)
            var left = info.remaining
            for (k in 0 until n) {
                open[k] = minOf(if (k == 0) info.nextAmount else d.installmentAmount, left)
                left -= open[k]
            }
            val pays = l.s.txs.filter { it.debtId == d.id && it.type == TxType.EXPENSE && it.date.isAfter(today) && !it.date.isAfter(end) }
                .sortedWith(compareBy({ it.date }, { it.id }))
            for (pay in pays) {
                var money = pay.amount
                var near = 0
                for (k in 1 until n) {
                    if (kotlin.math.abs(ChronoUnit.DAYS.between(dues[k], pay.date)) < kotlin.math.abs(ChronoUnit.DAYS.between(dues[near], pay.date))) near = k
                }
                for (k in (near until n) + (near - 1 downTo 0)) {
                    if (money <= 0) break
                    val use = minOf(money, open[k])
                    open[k] -= use
                    money -= use
                }
            }
            for (k in 0 until n) {
                if (dues[k].isAfter(end)) break
                if (open[k] > 0) add(dues[k], -open[k].toDouble())
            }
        }

        // 4b. RF-PRV-11: o orçamento planejado que o ritmo não mostra, espalhado pelos dias que faltam de cada mês.
        if (budgetPace != null) {
            var bm = today.ym()
            while (!bm.isAfter(end.ym())) {
                val extra = if (bm == today.ym()) io.github.h3yk0.cryo.domain.predict.BudgetPlan.thisMonth(l, bm, budgetPace).values.sum()
                else io.github.h3yk0.cryo.domain.predict.BudgetPlan.futureMonth(l, bm)
                if (extra > 0) {
                    val first = maxOf(bm.atDay(1), today.plusDays(1))
                    val last = minOf(bm.atEndOfMonth(), end)
                    val days = ChronoUnit.DAYS.between(first, last).toInt() + 1
                    if (days > 0) {
                        var d = first
                        while (!d.isAfter(last)) { add(d, -extra.toDouble() / days); d = d.plusDays(1) }
                    }
                }
                bm = bm.plusMonths(1)
            }
        }

        // 5. Recorrências (salário, assinaturas) que ainda não estão no app de outro jeito.
        val expected = Outlook.expectedItems(l, recurrences, today, end)
        val recIdx = IntArray(expected.size)
        val recAmt = DoubleArray(expected.size)
        for ((i, e) in expected.withIndex()) {
            val r = e.recurrence
            val card = r.cardId?.let { cardById[it] }
            recIdx[i] = when {
                r.type == TxType.EXPENSE && card != null -> index(cardDueDate(card, e.date))
                r.cardId != null -> Int.MAX_VALUE
                r.accountId == null || inc(r.accountId) -> index(e.date)
                else -> Int.MAX_VALUE
            }
            recAmt[i] = if (r.type == TxType.INCOME) r.amount.toDouble() else -r.amount.toDouble()
        }

        // 6. Gasto do dia a dia: a parte no cartão vai para a fatura do cartão mais usado.
        val exclude = HashSet<Long>()
        val excludeIncome = HashSet<Long>()
        for (r in recurrences) if (r.type == TxType.EXPENSE) exclude += r.txIds else excludeIncome += r.txIds
        val model = SpendingModel.build(l.s.txs, SpendingModel.spending(exclude), end = today, ref = today)
        val incomeModel = SpendingModel.build(l.s.txs, SpendingModel.extraIncome(excludeIncome), end = today, ref = today)
        val incomeAccount = l.s.txs.lastOrNull { it.type == TxType.INCOME && it.billId == null && it.id !in excludeIncome }?.accountId
        val incomeCounts = incomeAccount == null || inc(incomeAccount)
        val since = today.minusDays(180)
        var onCard = 0.0; var total = 0.0
        val perCard = HashMap<Long, Double>()
        for (t in l.s.txs) {
            if (!Recurrences.isVariable(t) || t.id in exclude || t.date.isBefore(since) || t.date.isAfter(today)) continue
            total += t.amount
            val cid = t.cardId
            if (cid != null && cid in cardById) { onCard += t.amount; perCard[cid] = (perCard[cid] ?: 0.0) + t.amount }
        }
        val mainCard = perCard.maxByOrNull { it.value }?.key?.let { cardById[it] }
        val cardShare = if (total > 0 && mainCard != null) onCard / total else 0.0
        val dueIdx = IntArray(h + 1) { i -> if (i == 0 || mainCard == null) 0 else index(cardDueDate(mainCard, today.plusDays(i.toLong()))) }
        val monthOf = IntArray(h + 1) { i -> val dt = today.plusDays(i.toLong()); (dt.year - today.year) * 12 + dt.monthValue - today.monthValue }
        val weekdays = IntArray(h + 1) { SpendingModel.weekday(today.plusDays(it.toLong())) }

        val n = scenarios.coerceAtLeast(1)
        val start = l.totalBalance
        val paths = Array(h + 1) { DoubleArray(n) }
        val rng = Rng(seed)
        val cardDue = DoubleArray(h + 2)
        val recDelta = DoubleArray(h + 2)
        val levels = DoubleArray(monthOf[h] + 1)
        val incomeLevels = DoubleArray(monthOf[h] + 1)
        val sigma = model.sigmaLevel
        val sigmaIncome = incomeModel.sigmaLevel
        var negatives = 0
        for (sc in 0 until n) {
            java.util.Arrays.fill(cardDue, 0.0)
            java.util.Arrays.fill(recDelta, 0.0)
            for (k in levels.indices) levels[k] = if (sigma > 0) exp(sigma * rng.nextGaussian() - sigma * sigma / 2) else 1.0
            for (k in incomeLevels.indices) incomeLevels[k] = exp(sigmaIncome * rng.nextGaussian() - sigmaIncome * sigmaIncome / 2)
            for (i in expected.indices) {
                if (rng.nextDouble() >= expected[i].probability) continue
                val at = recIdx[i]
                if (at in 1..h) recDelta[at] += recAmt[i]
            }
            var bal = start.toDouble()
            var min = bal
            paths[0][sc] = bal
            for (i in 1..h) {
                val v = if (model.isEmpty) 0.0 else model.draw(rng, weekdays[i]) * levels[monthOf[i]]
                val extra = if (incomeModel.isEmpty || !incomeCounts) 0.0 else incomeModel.draw(rng, weekdays[i]) * incomeLevels[monthOf[i]]
                val viaCard = v * cardShare
                val due = dueIdx[i]
                if (viaCard > 0 && due in (i + 1)..h) cardDue[due] += viaCard
                bal += det[i] + recDelta[i] - (v - viaCard) - cardDue[i] + extra
                paths[i][sc] = bal
                if (bal < min) min = bal
            }
            if (min < 0) negatives++
        }

        val low = ArrayList<Long>(h + 1); val mid = ArrayList<Long>(h + 1); val high = ArrayList<Long>(h + 1)
        for (i in 0..h) {
            val col = paths[i].also { it.sort() }
            low += Stats.quantileSorted(col, 0.1).roundToLong()
            mid += Stats.quantileSorted(col, 0.5).roundToLong()
            high += Stats.quantileSorted(col, 0.9).roundToLong()
        }
        val monthEnds = ArrayList<MonthEnd>()
        var ym = today.ym()
        while (!ym.isAfter(end.ym())) {
            val i = ChronoUnit.DAYS.between(today, minOf(ym.atEndOfMonth(), end)).toInt().coerceIn(0, h)
            monthEnds += MonthEnd(ym, low[i], mid[i], high[i])
            ym = ym.plusMonths(1)
        }
        var ti = 0
        for (i in 1..h) if (mid[i] < mid[ti]) ti = i
        return Projection(
            start = today, horizonDays = h, startBalance = start, low = low, mid = mid, high = high, monthEnds = monthEnds,
            tightestDate = today.plusDays(ti.toLong()), tightestMid = mid[ti],
            chanceNegative = negatives.toDouble() / n,
            firstNegativeMonth = monthEnds.firstOrNull { it.mid < 0 }?.ym,
            scenarios = n,
            certainty = when {
                model.historyDays >= 90 && model.fullMonths >= 3 -> Certainty.HIGH
                model.historyDays >= 28 -> Certainty.MEDIUM
                else -> Certainty.LOW
            },
        )
    }
}
