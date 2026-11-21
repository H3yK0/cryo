// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.domain.engine

import io.github.h3yk0.cryo.data.db.Goal
import io.github.h3yk0.cryo.domain.Ledger
import io.github.h3yk0.cryo.domain.ym
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import kotlin.math.ceil

/** RF-MOT-16: como a meta fica no ritmo de agora. */
enum class GoalFit {
    /** Já chegou lá. */
    DONE,
    /** Cabe até o prazo. */
    ON_TIME,
    /** No ritmo, chega depois do prazo. */
    LATE,
    /** Sem prazo: chega lá em [GoalForecastItem.eta]. */
    NO_DEADLINE,
    /** No ritmo de agora, não sobra para ela. */
    NO_ROOM,
}

/**
 * Uma meta na previsão. [need] = o que falta; [eta] = o mês em que chega lá com a sobra reservada para ela (null = não
 * sobra); [monthsLate] = quantos meses depois do prazo; [shortMonthly] = quanto por mês faltaria para chegar no prazo.
 */
data class GoalForecastItem(
    val goal: Goal,
    val need: Long,
    val fit: GoalFit,
    val eta: YearMonth?,
    val monthsLate: Int,
    val neededMonthly: Long,
    val shortMonthly: Long,
)

/** [neededMonthly] = quanto as metas com prazo pedem por mês; [surplusMonthly] = quanto deve sobrar por mês. */
data class GoalForecast(val items: List<GoalForecastItem>, val neededMonthly: Long, val surplusMonthly: Long)

/**
 * RF-MOT-16 (1.6, pedido do Hanry, LA8-2; no lugar de "Próximos meses"): no ritmo de agora, as metas cabem? A sobra vem da
 * projeção do saldo (RF-MOT-17: a renda, as contas fixas, os orçamentos planejados, as faturas, as parcelas, as dívidas e o
 * gasto do dia a dia): o que o saldo mais provável cresce até cada data. Ela é repartida entre as metas, primeiro as de
 * prazo mais perto; as sem prazo ficam com o que sobra depois. Depois do fim da projeção, a sobra média por mês continua.
 */
object GoalForecasts {
    fun of(l: Ledger, p: Projection): GoalForecast {
        val today = l.today
        val goals = l.activeGoals.filter { it.target > 0 }
        // A sobra por mês: o quanto o saldo mais provável cresce, em média, por mês da projeção.
        val months = (p.horizonDays / 30.4375).coerceAtLeast(1.0)
        val surplusMonthly = ((p.mid[p.horizonDays] - p.mid[0]) / months).toLong()

        /** A sobra acumulada até a data [d] (dentro da projeção, pelo saldo; depois, a média por mês continua). */
        fun surplusUntil(d: LocalDate): Long {
            val i = ChronoUnit.DAYS.between(p.start, d).toInt()
            if (i <= 0) return 0L
            if (i <= p.horizonDays) return (p.mid[i] - p.mid[0]).coerceAtLeast(0L)
            val extraMonths = (i - p.horizonDays) / 30.4375
            return (p.mid[p.horizonDays] - p.mid[0] + (surplusMonthly * extraMonths).toLong()).coerceAtLeast(0L)
        }

        /** O primeiro mês em que a sobra acumulada passa de [amount] (até 50 anos; null = não chega). */
        fun monthReaching(amount: Long): YearMonth? {
            if (amount <= 0) return today.ym()
            var ym = today.ym()
            repeat(600) {
                if (surplusUntil(ym.atEndOfMonth()) >= amount) return ym
                ym = ym.plusMonths(1)
            }
            return null
        }

        val ordered = goals.sortedWith(compareBy<Goal, LocalDate?>(nullsLast()) { it.deadline }.thenBy { it.id })
        var reserved = 0L
        var neededMonthly = 0L
        val items = ArrayList<GoalForecastItem>()
        for (g in ordered) {
            val need = (g.target - l.goalSaved(g.id)).coerceAtLeast(0L)
            if (need == 0L) {
                items += GoalForecastItem(g, 0L, GoalFit.DONE, today.ym(), 0, 0L, 0L)
                continue
            }
            val eta = monthReaching(reserved + need)
            val deadline = g.deadline
            if (deadline == null) {
                items += GoalForecastItem(g, need, if (eta == null) GoalFit.NO_ROOM else GoalFit.NO_DEADLINE, eta, 0, 0L, 0L)
                reserved += need
                continue
            }
            val monthsLeft = ChronoUnit.MONTHS.between(today.ym(), deadline.ym()).toInt().coerceAtLeast(1)
            val perMonth = ceil(need.toDouble() / monthsLeft).toLong()
            neededMonthly += perMonth
            val available = (surplusUntil(deadline) - reserved).coerceAtLeast(0L)
            val fit = when {
                available >= need -> GoalFit.ON_TIME
                eta == null || surplusMonthly <= 0 -> GoalFit.NO_ROOM
                else -> GoalFit.LATE
            }
            val late = if (fit == GoalFit.LATE && eta != null) ChronoUnit.MONTHS.between(deadline.ym(), eta).toInt().coerceAtLeast(1) else 0
            val short = if (fit == GoalFit.ON_TIME) 0L else ceil((need - available).toDouble() / monthsLeft).toLong()
            items += GoalForecastItem(g, need, fit, eta, late, perMonth, short)
            reserved += need
        }
        return GoalForecast(items, neededMonthly, surplusMonthly)
    }
}
