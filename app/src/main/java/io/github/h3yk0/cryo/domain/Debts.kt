// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.domain

import io.github.h3yk0.cryo.data.DefaultData
import io.github.h3yk0.cryo.data.db.Category
import io.github.h3yk0.cryo.data.db.CategoryKind
import io.github.h3yk0.cryo.data.db.Debt
import io.github.h3yk0.cryo.data.db.DebtAdjustment
import io.github.h3yk0.cryo.data.db.Tx
import io.github.h3yk0.cryo.data.db.TxType
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import kotlin.math.max
import kotlin.math.min

/**
 * Situação de uma dívida:
 * - PAID_OFF: quitada
 * - OVERDUE: tem parcela vencida (ou passou do prazo combinado)
 * - DUE_SOON: a próxima parcela vence em até 3 dias
 * - UPCOMING: a próxima parcela ainda está longe
 * - PAID_THIS_MONTH: a parcela deste mês já foi paga
 * - FREE: sem parcelas fixas (paga quando puder)
 */
enum class DebtState { PAID_OFF, OVERDUE, DUE_SOON, UPCOMING, PAID_THIS_MONTH, FREE }

data class DebtInfo(
    val debt: Debt,
    /** Quanto falta pagar (pode ser ≤ 0 se quitada). */
    val outstanding: Long,
    /** Quanto foi pago pelo Cryo. */
    val paid: Long,
    /** Dinheiro emprestado depois do cadastro (DEBT_IN). */
    val borrowed: Long,
    /** Juros/encargos (+) e descontos (−). */
    val adjusted: Long,
    val installmentsPaid: Int,
    val installmentsTotal: Int,
    val installmentsLeft: Int,
    /** Vencimento da próxima parcela em aberto (ou o prazo combinado, nas dívidas sem parcelas). */
    val nextDue: LocalDate?,
    /** Quanto falta da próxima parcela. */
    val nextAmount: Long,
    val state: DebtState,
    /** Parcelas já vencidas e não pagas. */
    val overdueCount: Int,
    /** O que deveria ser pago até o fim deste mês (inclui atrasadas) e ainda não foi. */
    val pendingThisMonth: Long,
    /** Mês previsto da última parcela (ou do prazo). */
    val payoffYm: YearMonth?,
    /** Dívidas sem parcelas, com prazo: quanto pagar por mês para quitar a tempo. */
    val monthlyNeeded: Long?,
    val lastPayment: LocalDate?,
) {
    val hasInstallments: Boolean get() = debt.installmentAmount > 0
    val isPaidOff: Boolean get() = state == DebtState.PAID_OFF
    val remaining: Long get() = max(0, outstanding)

    /** Quanto já foi pago, contando as parcelas pagas antes de usar o Cryo. */
    val paidAll: Long get() = paid + debt.paidBefore.toLong() * debt.installmentAmount

    val progress: Float
        get() = when {
            isPaidOff -> 1f
            hasInstallments && installmentsTotal > 0 -> installmentsPaid.toFloat() / installmentsTotal
            paid + remaining > 0 -> (paid.toFloat() / (paid + remaining)).coerceIn(0f, 1f)
            else -> 0f
        }
}

object DebtMath {
    /** Diferença de até R$ 0,50 é tratada como arredondamento. */
    const val TOLERANCE = 50L

    fun info(d: Debt, txs: List<Tx>, adjustments: List<DebtAdjustment>, today: LocalDate): DebtInfo {
        val past = txs.filter { !it.date.isAfter(today) }
        val payments = past.filter { it.type == TxType.EXPENSE }
        val paid = payments.sumOf { it.amount }
        val borrowed = past.filter { it.type == TxType.DEBT_IN }.sumOf { it.amount }
        val adjusted = adjustments.filter { !it.date.isAfter(today) }.sumOf { it.amount }
        val outstanding = d.initialBalance + borrowed + adjusted - paid
        val paidOff = outstanding <= TOLERANCE
        val lastPayment = payments.maxOfOrNull { it.date }
        val a = d.installmentAmount

        if (a <= 0) {
            val deadline = d.deadline
            val monthly = if (deadline == null || paidOff) null else {
                val months = ChronoUnit.MONTHS.between(today.ym(), deadline.ym()).coerceAtLeast(0) + 1
                (outstanding + months - 1) / months
            }
            val state = when {
                paidOff -> DebtState.PAID_OFF
                deadline != null && deadline.isBefore(today) -> DebtState.OVERDUE
                else -> DebtState.FREE
            }
            return DebtInfo(
                debt = d, outstanding = outstanding, paid = paid, borrowed = borrowed, adjusted = adjusted,
                installmentsPaid = 0, installmentsTotal = 0, installmentsLeft = 0,
                nextDue = if (paidOff) null else deadline, nextAmount = if (paidOff) 0 else monthly ?: max(0, outstanding),
                state = state, overdueCount = if (state == DebtState.OVERDUE) 1 else 0, pendingThisMonth = 0,
                payoffYm = if (paidOff) null else deadline?.ym(), monthlyNeeded = monthly, lastPayment = lastPayment,
            )
        }

        // Parcelas cobertas pelos pagamentos (o dinheiro pago quita sempre a parcela mais antiga primeiro).
        val kPaid = ((paid + TOLERANCE) / a).toInt().coerceAtLeast(0)
        val left = if (paidOff) 0 else ((outstanding - TOLERANCE + a - 1) / a).toInt().coerceAtLeast(1)
        val start = ymOf(d.startYm)
        val nextDue = if (paidOff) null else start.plusMonths(kPaid.toLong()).dayClamped(d.dueDay)
        val partial = (paid - kPaid.toLong() * a).coerceAtLeast(0)
        val nextAmount = if (paidOff) 0 else min(outstanding, a - partial).coerceAtLeast(0)

        val cur = today.ym()
        val dueThroughMonth = if (cur < start) 0L else ChronoUnit.MONTHS.between(start, cur) + 1
        val dueCount = min(dueThroughMonth, (kPaid + left).toLong())
        val pending = if (paidOff) 0 else (dueCount * a - paid).coerceIn(0, outstanding)

        var overdue = 0
        var k = kPaid
        while (k < kPaid + left && start.plusMonths(k.toLong()).dayClamped(d.dueDay).isBefore(today)) {
            overdue++; k++
        }
        val state = when {
            paidOff -> DebtState.PAID_OFF
            overdue > 0 -> DebtState.OVERDUE
            nextDue!!.ym() > cur && cur >= start -> DebtState.PAID_THIS_MONTH
            ChronoUnit.DAYS.between(today, nextDue) <= 3 -> DebtState.DUE_SOON
            else -> DebtState.UPCOMING
        }
        // Quitada (às vezes com desconto, pagando menos que as parcelas): conta todas as parcelas como pagas.
        val installmentsPaid = if (paidOff) max(d.paidBefore + kPaid, d.installmentCount) else d.paidBefore + kPaid
        return DebtInfo(
            debt = d, outstanding = outstanding, paid = paid, borrowed = borrowed, adjusted = adjusted,
            installmentsPaid = installmentsPaid, installmentsTotal = installmentsPaid + left, installmentsLeft = left,
            nextDue = nextDue, nextAmount = nextAmount, state = state, overdueCount = overdue, pendingThisMonth = pending,
            payoffYm = nextDue?.ym()?.plusMonths((left - 1).toLong()), monthlyNeeded = null, lastPayment = lastPayment,
        )
    }

    /** Parcela de uma dívida que vence no mês [ym] (para a lista de contas fixas). */
    data class MonthInstallment(val info: DebtInfo, val number: Int, val due: LocalDate, val paid: Boolean, val payable: Boolean)

    fun installmentIn(info: DebtInfo, ym: YearMonth): MonthInstallment? {
        val d = info.debt
        if (d.installmentAmount <= 0) return null
        val start = ymOf(d.startYm)
        if (ym < start) return null
        // Quitada antes do fim: não mostra parcelas nos meses depois da quitação.
        if (info.isPaidOff && ym > (info.lastPayment?.ym() ?: start)) return null
        val k = ChronoUnit.MONTHS.between(start, ym).toInt()
        val kPaid = info.installmentsPaid - d.paidBefore
        if (k >= kPaid + info.installmentsLeft) return null
        return MonthInstallment(
            info = info, number = d.paidBefore + k + 1, due = ym.dayClamped(d.dueDay),
            paid = k < kPaid, payable = k == kPaid,
        )
    }

    /** Primeira parcela a pagar sugerida no cadastro: se o vencimento deste mês já passou, a do mês que vem. */
    fun suggestedStart(today: LocalDate, dueDay: Int): YearMonth =
        if (today.dayOfMonth > dueDay) today.ym().plusMonths(1) else today.ym()
}

/** Categoria usada nos pagamentos de dívidas. */
fun List<Category>.debtCategory(): Category? =
    firstOrNull { it.kind == CategoryKind.EXPENSE && !it.archived && it.icon == DefaultData.DEBT_ICON }
        ?: firstOrNull { it.kind == CategoryKind.EXPENSE && !it.archived && it.name.normalized().contains("divida") }

/** Visão geral de todas as dívidas em aberto. */
data class DebtOverview(
    val total: Long,
    val paidAll: Long,
    val monthlyInstallments: Long,
    val pendingThisMonth: Long,
    val overdueCount: Int,
    val freeOfDebtYm: YearMonth?,
    val open: List<DebtInfo>,
) {
    val progress: Float get() = if (paidAll + total > 0) paidAll.toFloat() / (paidAll + total) else 0f
}
