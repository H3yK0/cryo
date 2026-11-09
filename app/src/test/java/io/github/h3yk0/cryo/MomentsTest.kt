// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo

import io.github.h3yk0.cryo.data.db.Account
import io.github.h3yk0.cryo.data.db.Budget
import io.github.h3yk0.cryo.data.db.BudgetAdjustment
import io.github.h3yk0.cryo.data.db.Category
import io.github.h3yk0.cryo.data.db.CategoryKind
import io.github.h3yk0.cryo.data.db.Goal
import io.github.h3yk0.cryo.data.db.NoSpendDay
import io.github.h3yk0.cryo.data.db.Tx
import io.github.h3yk0.cryo.data.db.TxType
import io.github.h3yk0.cryo.domain.Ledger
import io.github.h3yk0.cryo.domain.Snapshot
import io.github.h3yk0.cryo.notify.ShoppingSession
import io.github.h3yk0.cryo.ui.components.Moment
import io.github.h3yk0.cryo.ui.components.MomentBus
import io.github.h3yk0.cryo.ui.components.Source
import io.github.h3yk0.cryo.ui.tx.TxDraft
import io.github.h3yk0.cryo.ui.tx.momentAfterSave
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/**
 * RF-MOM-01..03 e RF-ORC-04: o momento certo depois de salvar (meta completa, orçamento em 80% e estourado com a sugestão
 * de onde cobrir, marcos da sequência), um por vez; e RF-AVI-10: a conta da ida às compras.
 */
class MomentsTest {
    private val today = LocalDate.of(2026, 11, 9)
    private val acc = Account(id = 1, name = "Conta", color = 0)
    private val food = Category(id = 10, name = "Mercado", kind = CategoryKind.EXPENSE, icon = "shopping_cart", color = 0)
    private val fun_ = Category(id = 11, name = "Lazer", kind = CategoryKind.EXPENSE, icon = "celebration", color = 0)

    @Before fun strings() = JvmStrings.install()

    private fun ledger(txs: List<Tx> = emptyList(), goals: List<Goal> = emptyList(), adjustments: List<BudgetAdjustment> = emptyList(), noSpend: List<NoSpendDay> = emptyList()) = Ledger(
        Snapshot(
            accounts = listOf(acc), categories = listOf(food, fun_), txs = txs, goals = goals,
            budgets = listOf(Budget(food.id, 50_000), Budget(fun_.id, 30_000)), budgetAdjustments = adjustments, noSpendDays = noSpend,
        ),
        today,
    )

    private fun expense(amount: Long, cat: Long = food.id, date: LocalDate = today) =
        TxDraft(type = TxType.EXPENSE, amount = amount, categoryId = cat, source = Source.Acc(acc.id), date = date)

    private fun spent(amount: Long, cat: Long = food.id, date: LocalDate = today.minusDays(3)) =
        Tx(type = TxType.EXPENSE, amount = amount, date = date, categoryId = cat, accountId = acc.id)

    @Test fun orcamentoEm80PorCento() {
        val m = momentAfterSave(expense(5_000), ledger(listOf(spent(36_000))), 5_000, isNew = true) as Moment.Budget
        assertEquals(false, m.over)
        assertEquals(41_000L, m.spent)
        assertEquals(50_000L, m.limit)
        assertNull(m.move)
        assertEquals(21, m.daysLeft)
    }

    @Test fun orcamentoEstouradoSugereDeOndeCobrir() {
        val m = momentAfterSave(expense(8_000), ledger(listOf(spent(45_000), spent(5_000, fun_.id))), 8_000, isNew = true) as Moment.Budget
        assertTrue(m.over)
        // Passou 3.000; a sugestão arredonda para cima de 10 em 10 (na moeda) e vem de quem tem folga (Lazer: 25.000 livres).
        val mv = m.move!!
        assertEquals(fun_.id, mv.fromId)
        assertEquals(25_000L, mv.free)
        assertEquals(3_000L, mv.amount)
    }

    @Test fun limiteRemanejadoMudaOAlerta() {
        // Com 10.000 a mais neste mês, os mesmos 53.000 ficam abaixo de 80% de 60.000? Não: 88%. Passa a ser só "80%".
        val adj = listOf(BudgetAdjustment(ym = 202611, categoryId = food.id, amount = 10_000))
        val m = momentAfterSave(expense(8_000), ledger(listOf(spent(45_000)), adjustments = adj), 8_000, isNew = true) as Moment.Budget
        assertEquals(false, m.over)
        assertEquals(60_000L, m.limit)
    }

    @Test fun semOrcamentoNemMarcoNaoHaMomento() {
        assertNull(momentAfterSave(expense(1_000), ledger(), 1_000, isNew = true))
        // Já estava acima de 80%: não repete.
        assertNull(momentAfterSave(expense(1_000), ledger(listOf(spent(42_000))), 1_000, isNew = true))
    }

    @Test fun metaCompleta() {
        val goal = Goal(id = 3, name = "Viagem", target = 100_000, color = 0, createdAt = today.minusMonths(5).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli())
        val saved = Tx(type = TxType.GOAL_IN, amount = 90_000, date = today.minusDays(10), goalId = goal.id, accountId = acc.id)
        val d = TxDraft(type = TxType.GOAL_IN, amount = 10_000, goalId = goal.id, source = Source.Acc(acc.id), date = today)
        val m = momentAfterSave(d, ledger(listOf(saved), goals = listOf(goal)), 0, isNew = true) as Moment.GoalDone
        assertEquals("Viagem", m.name)
        assertEquals(100_000L, m.saved)
        assertEquals(5, m.months)
        // Editar não comemora de novo.
        assertNull(momentAfterSave(d, ledger(listOf(saved), goals = listOf(goal)), 0, isNew = false))
    }

    @Test fun sequenciaNosMarcos() {
        // 6 dias seguidos antes de hoje (um deles "sem gastos") + o primeiro de hoje = 7.
        val before = (1L..5L).map { spent(500, fun_.id, today.minusDays(it)) }
        val m = momentAfterSave(expense(500, fun_.id), ledger(before, noSpend = listOf(NoSpendDay(today.minusDays(6)))), 500, isNew = true)
        assertEquals(Moment.Streak(7), m)
        // O segundo lançamento do dia não comemora de novo.
        assertNull(momentAfterSave(expense(500, fun_.id), ledger(before + spent(500, fun_.id, today), noSpend = listOf(NoSpendDay(today.minusDays(6)))), 500, isNew = true))
        // 5 dias não é marco.
        assertNull(momentAfterSave(expense(500, fun_.id), ledger(before.take(4)), 500, isNew = true))
    }

    @Test fun umMomentoPorVez() {
        val bus = MomentBus()
        bus.show(Moment.Streak(3))
        bus.show(Moment.Streak(7))
        assertEquals(Moment.Streak(3), bus.current.value)
        bus.dismiss()
        assertNull(bus.current.value)
    }

    @Test fun idaAsComprasSomaSoODepoisETermina() {
        val start = today.atTime(15, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val s = ShoppingSession(cap = 30_000, startMillis = start)
        val before = spent(9_000, date = today).copy(createdAt = start - 60_000)
        val during = listOf(spent(4_500, date = today).copy(createdAt = start + 60_000), spent(2_000, date = today).copy(createdAt = start + 120_000))
        val income = Tx(type = TxType.INCOME, amount = 50_000, date = today, accountId = acc.id, createdAt = start + 1_000)
        assertEquals(6_500L, s.spent(Snapshot(txs = listOf(before, income) + during), today))
        // Começou às 15h: termina à meia-noite (antes das 6 horas acabarem).
        val midnight = today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        assertEquals(minOf(start + 6 * 3_600_000L, midnight), s.endsAt())
        val morning = ShoppingSession(10_000, today.atTime(9, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
        assertEquals(morning.startMillis + 6 * 3_600_000L, morning.endsAt())
    }
}
