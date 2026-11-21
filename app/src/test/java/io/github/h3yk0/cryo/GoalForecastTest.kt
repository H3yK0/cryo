// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo

import io.github.h3yk0.cryo.data.db.Goal
import io.github.h3yk0.cryo.data.db.Tx
import io.github.h3yk0.cryo.data.db.TxType
import io.github.h3yk0.cryo.domain.Ledger
import io.github.h3yk0.cryo.domain.Snapshot
import io.github.h3yk0.cryo.domain.engine.Certainty
import io.github.h3yk0.cryo.domain.engine.GoalFit
import io.github.h3yk0.cryo.domain.engine.GoalForecasts
import io.github.h3yk0.cryo.domain.engine.Projection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

/**
 * RF-MOT-16 (1.6, LA8-2): a previsão de metas. A sobra da projeção do saldo é repartida entre as metas, primeiro as de prazo
 * mais perto: sobra que cabe, sobra que não cabe, prioridade pelo prazo, meta sem prazo e sobra negativa.
 */
class GoalForecastTest {
    private val today = LocalDate.of(2026, 11, 20)

    /** Uma projeção em que o saldo cresce [perMonth] por mês (por dia, proporcional). */
    private fun projection(perMonth: Long, days: Int = 406): Projection {
        val mid = (0..days).map { i -> 100_000L + (perMonth * i / 30.4375).toLong() }
        return Projection(today, days, 100_000L, mid, mid, mid, emptyList(), today, mid.min(), 0.0, null, 1, Certainty.HIGH)
    }

    private fun ledger(vararg goals: Goal, saved: Map<Long, Long> = emptyMap()) = Ledger(
        Snapshot(
            goals = goals.toList(),
            txs = saved.map { (id, v) -> Tx(id = id, type = TxType.GOAL_IN, amount = v, date = today.minusMonths(1), goalId = id, accountId = 1) },
        ),
        today,
    )

    private fun goal(id: Long, target: Long, deadline: LocalDate?) = Goal(id = id, name = "Meta $id", target = target, deadline = deadline, color = 0)

    @Test fun sobraQueCabe() {
        val f = GoalForecasts.of(ledger(goal(1, 600_000, today.plusMonths(6))), projection(300_000))
        val g = f.items.single()
        assertEquals(GoalFit.ON_TIME, g.fit)
        assertTrue("chega em ${g.eta}", g.eta!! <= YearMonth.from(today).plusMonths(2))
        assertEquals(100_000L, g.neededMonthly)
        assertTrue("sobra ${f.surplusMonthly}", kotlin.math.abs(f.surplusMonthly - 300_000L) <= 2_000L)
    }

    @Test fun sobraQueNaoCabe() {
        val g = GoalForecasts.of(ledger(goal(1, 3_000_000, today.plusMonths(6))), projection(300_000)).items.single()
        assertEquals(GoalFit.LATE, g.fit)
        assertTrue(g.monthsLate >= 3)
        assertTrue(g.shortMonthly > 0)
    }

    @Test fun prioridadePeloPrazo() {
        val near = goal(1, 600_000, today.plusMonths(3))
        val far = goal(2, 900_000, today.plusMonths(4))
        val items = GoalForecasts.of(ledger(far, near), projection(300_000)).items
        assertEquals(listOf(1L, 2L), items.map { it.goal.id })
        assertEquals(GoalFit.ON_TIME, items[0].fit)
        // A de 4 meses fica com o que sobra depois da de 3 meses: não cabe toda.
        assertEquals(GoalFit.LATE, items[1].fit)
    }

    @Test fun metaSemPrazoFicaComODepois() {
        val items = GoalForecasts.of(ledger(goal(1, 600_000, today.plusMonths(3)), goal(2, 600_000, null)), projection(300_000)).items
        val open = items.first { it.goal.id == 2L }
        assertEquals(GoalFit.NO_DEADLINE, open.fit)
        assertTrue(open.eta!! > items.first { it.goal.id == 1L }.eta!!)
    }

    @Test fun sobraNegativaNaoCabeNada() {
        val g = GoalForecasts.of(ledger(goal(1, 100_000, today.plusMonths(6))), projection(-50_000)).items.single()
        assertEquals(GoalFit.NO_ROOM, g.fit)
    }

    @Test fun metaJaCompleta() {
        val g = GoalForecasts.of(ledger(goal(1, 100_000, today.plusMonths(6)), saved = mapOf(1L to 100_000L)), projection(10_000)).items.single()
        assertEquals(GoalFit.DONE, g.fit)
    }
}
