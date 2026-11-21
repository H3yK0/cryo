// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo

import io.github.h3yk0.cryo.data.db.Account
import io.github.h3yk0.cryo.data.db.Budget
import io.github.h3yk0.cryo.data.db.Category
import io.github.h3yk0.cryo.data.db.CategoryKind
import io.github.h3yk0.cryo.data.db.CreditCard
import io.github.h3yk0.cryo.data.db.Tx
import io.github.h3yk0.cryo.data.db.TxType
import io.github.h3yk0.cryo.domain.GeneralBudget
import io.github.h3yk0.cryo.domain.Ledger
import io.github.h3yk0.cryo.domain.Snapshot
import io.github.h3yk0.cryo.domain.predict.BudgetPlan
import io.github.h3yk0.cryo.domain.predict.EndItem
import io.github.h3yk0.cryo.domain.predict.EndLine
import io.github.h3yk0.cryo.domain.predict.MonthEnd
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

/** RF-ORC-10 e DAD-17 (reforma visual da 1.6, LA12-1): o orçamento Geral, o quanto se pode gastar da renda no mês. */
class GeneralBudgetTest {
    @Before fun strings() = JvmStrings.install()

    private val today = LocalDate.of(2026, 11, 21)
    private val ym = YearMonth.of(2026, 11)
    private val cats = listOf(Category(1, "Mercado", CategoryKind.EXPENSE, "cart", 0), Category(2, "Lazer", CategoryKind.EXPENSE, "movie", 0))
    private val card = CreditCard(1, "Roxo", limitAmount = 500_000, closingDay = 3, dueDay = 10, color = 0, payAccountId = 1)

    private fun d(day: Int) = LocalDate.of(2026, 11, day)

    private fun ledger(budgets: List<Budget> = emptyList()) = Ledger(
        Snapshot(
            accounts = listOf(Account(1, "Conta", initialBalance = 500_000, color = 0)), cards = listOf(card), categories = cats, budgets = budgets,
            txs = listOf(
                Tx(1, TxType.EXPENSE, 30_000, d(2), "Mercado", accountId = 1, categoryId = 1),
                Tx(2, TxType.EXPENSE, 12_000, d(5), "Cinema", cardId = 1, invoiceYm = 202612, categoryId = 2),
                Tx(3, TxType.EXPENSE, 9_000, d(10), "Internet", accountId = 1, billId = 9, billYm = 202611),
                Tx(4, TxType.TRANSFER, 50_000, d(6), "Poupança", accountId = 1, toAccountId = 1),
                Tx(5, TxType.CARD_PAYMENT, 40_000, d(10), "Fatura", accountId = 1, cardId = 1, invoiceYm = 202611),
                Tx(6, TxType.INVEST_IN, 70_000, d(7), "Aporte", accountId = 1, investmentId = 1),
                Tx(7, TxType.GOAL_IN, 20_000, d(8), "Viagem", accountId = 1, goalId = 1),
                Tx(8, TxType.INCOME, 450_000, d(5), "Salário", accountId = 1),
            ),
        ),
        today,
    )

    @Test fun oGastoQueContaSaoTodasAsSaidas() {
        // Mercado, cinema no cartão e a conta fixa; transferência, fatura, aporte e meta ficam fora.
        assertEquals(30_000L + 12_000L + 9_000L, GeneralBudget.spent(ledger(), ym))
    }

    @Test fun usoDoGeralEODividido() {
        assertNull("sem o Geral definido, nada", GeneralBudget.usage(ledger(), ym))
        val u = GeneralBudget.usage(ledger(listOf(Budget(GeneralBudget.ID, 350_000), Budget(1, 120_000), Budget(2, 80_000))), ym)!!
        assertEquals(350_000L, u.limit)
        assertEquals(51_000L, u.spent)
        assertEquals(299_000L, u.remaining)
        assertEquals(200_000L, u.divided)
        assertFalse(u.overDivided)
        assertEquals(10, u.daysLeft)
        assertEquals(29_900L, u.perDay)
        val over = GeneralBudget.usage(ledger(listOf(Budget(GeneralBudget.ID, 150_000), Budget(1, 120_000), Budget(2, 80_000))), ym)!!
        assertTrue("as categorias somam mais que o Geral", over.overDivided)
    }

    @Test fun sugestaoPelaRendaFixaMenosAsMetas() {
        fun end(vararg items: EndItem) = MonthEnd(ym, items.toList(), items.sumOf { it.amount }, 0, 0, 0, 9)
        assertEquals(400_000L, GeneralBudget.suggestion(end(EndItem(EndLine.IN_FIXED, 450_000), EndItem(EndLine.OUT_GOALS, -50_000))))
        // Arredonda para baixo na unidade da moeda.
        assertEquals(399_900L, GeneralBudget.suggestion(end(EndItem(EndLine.IN_FIXED, 450_000), EndItem(EndLine.OUT_GOALS, -50_050))))
        assertNull("sem renda fixa, a pessoa digita", GeneralBudget.suggestion(end(EndItem(EndLine.IN_OTHER, 90_000))))
    }

    /** DAD-17: o Geral é o orçamento de código 0; as contas por categoria não o veem como categoria. */
    @Test fun oGeralNaoViraCategoria() {
        val l = ledger(listOf(Budget(GeneralBudget.ID, 350_000), Budget(1, 120_000)))
        assertEquals(listOf(1L), l.budgetUsages(ym).map { it.category.id })
        assertEquals(setOf(1L), BudgetPlan.thisMonth(l, ym, emptyMap()).keys)
        assertFalse(BudgetPlan.thisMonth(l, ym, emptyMap()).containsKey(GeneralBudget.ID))
    }
}
