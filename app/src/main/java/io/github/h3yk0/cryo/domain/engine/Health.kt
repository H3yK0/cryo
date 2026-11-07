// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.domain.engine

import io.github.h3yk0.cryo.data.db.InvestmentKind
import io.github.h3yk0.cryo.data.db.TxType
import io.github.h3yk0.cryo.domain.BillState
import io.github.h3yk0.cryo.domain.InvoiceStatus
import io.github.h3yk0.cryo.domain.Ledger
import io.github.h3yk0.cryo.domain.ym
import java.time.temporal.ChronoUnit
import kotlin.math.max
import kotlin.math.roundToInt

/** As 6 partes da nota (RF-MOT-06), com o máximo de pontos de cada uma. */
enum class HealthKind(val max: Int) { SAVINGS(25), RESERVE(25), COMMITMENTS(20), CARD(10), ON_TIME(10), STABILITY(10) }

enum class HealthLevel { ATTENTION, BUILDING, GOOD, GREAT }

/** Uma parte da nota. [value] é o número que deu os pontos (ex.: 0,12 = sobra de 12%; 3,5 = 3,5 meses de reserva). */
data class HealthPart(val kind: HealthKind, val points: Double, val value: Double) {
    val ratio: Double get() = points / kind.max
}

/**
 * RF-MOT-06: a saúde financeira, de 0 a 100. Pesos fixos e publicados (PRD), sem caixa-preta. [ready] = falso quando ainda
 * não há dados suficientes (menos de 60 dias ou nenhuma renda): a tela mostra "Ainda conhecendo você".
 */
data class Health(val ready: Boolean, val score: Int, val level: HealthLevel, val parts: List<HealthPart>, val months: Int) {
    /** A parte que mais tira pontos (a primeira coisa a melhorar). */
    val weakest: HealthPart? get() = parts.filter { it.kind.max - it.points >= 0.5 }.maxByOrNull { it.kind.max - it.points }

    companion object {
        val NOT_READY = Health(false, 0, HealthLevel.BUILDING, emptyList(), 0)
    }
}

object HealthScore {
    const val MIN_DAYS = 60L

    fun of(l: Ledger): Health {
        val today = l.today
        val first = l.s.txs.minOfOrNull { it.date } ?: return Health.NOT_READY
        if (ChronoUnit.DAYS.between(first, today) < MIN_DAYS) return Health.NOT_READY
        val ym = today.ym()
        // Meses completos desde que a pessoa começou (até 6).
        val months = (1..6).map { ym.minusMonths(it.toLong()) }.filter { !it.atEndOfMonth().isBefore(first) }
        if (months.size < 2) return Health.NOT_READY
        val sums = months.map { l.monthSummary(it) }
        val incomes = sums.map { it.income.toDouble() }.toDoubleArray()
        val expenses = sums.map { it.expense.toDouble() }.toDoubleArray()
        val income = Stats.median(incomes)
        if (income <= 0) return Health.NOT_READY

        // 1. Sobra do mês: 20% ou mais = nota cheia.
        val rates = sums.filter { it.income > 0 }.map { (it.income - it.expense).toDouble() / it.income }.toDoubleArray()
        val rate = Stats.median(rates)
        val savings = HealthPart(HealthKind.SAVINGS, HealthKind.SAVINGS.max * Stats.ramp(rate, 0.0, 0.20, 0.0, 1.0), rate)

        // 2. Reserva de emergência: dinheiro que dá para usar logo (contas, caixinhas e investimentos fora da previdência).
        val liquid = max(0L, l.totalBalance) + max(0L, l.totalGoals) +
            l.s.investments.filter { !it.archived && it.kind != InvestmentKind.PENSION }.sumOf { max(0L, l.investmentValue(it.id)) }
        val monthlyExpense = Stats.median(expenses)
        val reserveMonths = if (monthlyExpense <= 0) 12.0 else liquid / monthlyExpense
        val reserve = HealthPart(HealthKind.RESERVE, HealthKind.RESERVE.max * Stats.ramp(reserveMonths, 0.0, 6.0, 0.0, 1.0), reserveMonths)

        // 3. Compromissos: parcelas de dívidas e do cartão ÷ renda. Até 10% = nota cheia; 50% ou mais = zero.
        val cardInstallments = l.s.txs.filter {
            it.type == TxType.EXPENSE && it.installmentTotal > 1 && it.date.isAfter(today) && !it.date.isAfter(today.plusDays(30))
        }.sumOf { it.amount }
        val commitments = (l.debtOverview.monthlyInstallments + cardInstallments) / income
        val commit = HealthPart(HealthKind.COMMITMENTS, HealthKind.COMMITMENTS.max * Stats.ramp(commitments, 0.10, 0.50, 1.0, 0.0), commitments)

        // 4. Cartão: quanto do limite está usado. Até 30% = nota cheia; 90% ou mais = zero. Sem cartão, nota cheia.
        val cards = l.activeCards.filter { it.limitAmount > 0 }
        val usage = if (cards.isEmpty()) 0.0 else cards.sumOf { max(0L, l.cardOutstanding(it.id)) }.toDouble() / cards.sumOf { it.limitAmount }
        val card = HealthPart(HealthKind.CARD, HealthKind.CARD.max * Stats.ramp(usage, 0.30, 0.90, 1.0, 0.0), usage)

        // 5. Em dia: contas fixas, dívidas e faturas atrasadas (dos últimos 3 meses). Cada atraso tira 4 pontos.
        var late = 0
        for (k in 0..2) late += l.billStatuses(ym.minusMonths(k.toLong())).count { it.state == BillState.OVERDUE }
        late += l.debtOverview.overdueCount
        late += l.activeCards.count { c ->
            val f = l.focusInvoice(c)
            f.status(today) == InvoiceStatus.OVERDUE && !f.due.isBefore(today.minusDays(45))
        }
        val onTime = HealthPart(HealthKind.ON_TIME, max(0.0, HealthKind.ON_TIME.max - 4.0 * late), late.toDouble())

        // 6. Estabilidade: quanto o gasto varia de um mês para o outro (10% = nota cheia; 50% = zero).
        val cv = Stats.robustCv(expenses)
        val stability = HealthPart(HealthKind.STABILITY, HealthKind.STABILITY.max * Stats.ramp(cv, 0.10, 0.50, 1.0, 0.0), cv)

        val parts = listOf(savings, reserve, commit, card, onTime, stability)
        val score = parts.sumOf { it.points }.roundToInt().coerceIn(0, 100)
        val level = when {
            score >= 80 -> HealthLevel.GREAT
            score >= 60 -> HealthLevel.GOOD
            score >= 40 -> HealthLevel.BUILDING
            else -> HealthLevel.ATTENTION
        }
        return Health(true, score, level, parts, months.size)
    }
}
