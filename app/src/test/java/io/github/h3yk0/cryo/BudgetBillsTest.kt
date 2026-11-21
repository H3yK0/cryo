// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo

import io.github.h3yk0.cryo.data.db.Bill
import io.github.h3yk0.cryo.data.db.Budget
import io.github.h3yk0.cryo.data.db.Category
import io.github.h3yk0.cryo.data.db.CategoryKind
import io.github.h3yk0.cryo.domain.BudgetBills
import io.github.h3yk0.cryo.domain.IncomePlanner
import io.github.h3yk0.cryo.domain.Ledger
import io.github.h3yk0.cryo.domain.PlanInputs
import io.github.h3yk0.cryo.domain.Snapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

/**
 * RF-ORC-09 (1.6, LA7-1): o orçamento conta com as contas fixas. Conta fixa maior que o limite gera a sugestão; aceitar
 * sobe o limite; a anual vira ajuste só no mês dela; sem orçamento, sugere criar; "Agora não" vale até as contas mudarem;
 * o orçamento pela renda respeita as contas fixas.
 */
class BudgetBillsTest {
    init { JvmStrings.install() }

    private val today = LocalDate.of(2026, 11, 9)
    private val nov = YearMonth.of(2026, 11)
    private val home = Category(1, "Moradia", CategoryKind.EXPENSE, "home", 0)
    private val car = Category(2, "Transporte", CategoryKind.EXPENSE, "car", 0)
    private val fun_ = Category(3, "Lazer", CategoryKind.EXPENSE, "celebration", 0)
    private val rent = Bill(id = 1, name = "Aluguel", amount = 100_000, dueDay = 5, categoryId = 1, startYm = 202601)
    private val condo = Bill(id = 2, name = "Condomínio", amount = 20_000, dueDay = 10, categoryId = 1, startYm = 202601)
    /** IPVA: todo ano, em janeiro (startYm 2027-01). */
    private val ipva = Bill(id = 3, name = "IPVA", amount = 120_000, dueDay = 20, categoryId = 2, startYm = 202701, every = 12)

    private fun l(budgets: List<Budget>, bills: List<Bill> = listOf(rent, condo, ipva)) =
        Ledger(Snapshot(categories = listOf(home, car, fun_), bills = bills, budgets = budgets), today)

    @Test fun contasMaioresQueOLimiteGeramASugestao() {
        val sg = BudgetBills.suggestion(l(listOf(Budget(1, 100_000))), 1, nov)
        assertNotNull(sg)
        assertEquals(120_000L, sg!!.monthly)
        assertEquals(100_000L, sg.current)
        assertEquals(120_000L, sg.suggested)
        assertTrue(sg.raisesLimit)
    }

    @Test fun quandoCabeNaoSugereNada() {
        assertNull(BudgetBills.suggestion(l(listOf(Budget(1, 150_000))), 1, nov))
    }

    @Test fun semOrcamentoSugereCriar() {
        val sg = BudgetBills.suggestion(l(emptyList()), 1, nov)!!
        assertTrue(sg.createsBudget)
        assertEquals(120_000L, sg.suggested)
        // Categoria sem conta fixa nenhuma: nada a sugerir.
        assertNull(BudgetBills.suggestion(l(emptyList()), 3, nov))
    }

    @Test fun anualViraAjusteSoNoMesDela() {
        val sg = BudgetBills.suggestion(l(listOf(Budget(2, 30_000))), 2, nov)!!
        // O limite de sempre não muda (não há conta mensal de transporte); janeiro ganha o IPVA a mais.
        assertEquals(30_000L, sg.suggested)
        assertEquals(listOf(YearMonth.of(2027, 1) to 90_000L), sg.monthOnly.map { it.ym to it.delta })
        assertEquals(listOf("IPVA"), sg.monthOnly.single().bills.map { it.name })
        // Com o ajuste de janeiro já feito, não sugere de novo.
        val adjusted = Ledger(
            Snapshot(
                categories = listOf(home, car, fun_), bills = listOf(ipva), budgets = listOf(Budget(2, 30_000)),
                budgetAdjustments = listOf(io.github.h3yk0.cryo.data.db.BudgetAdjustment(ym = 202701, categoryId = 2, amount = 90_000)),
            ),
            today,
        )
        assertNull(BudgetBills.suggestion(adjusted, 2, nov))
    }

    @Test fun agoraNaoValeAteAsContasMudarem() {
        val ledger = l(listOf(Budget(1, 100_000)))
        val sg = BudgetBills.suggestion(ledger, 1, nov)!!
        assertEquals(emptyList<BudgetBills.Suggestion>(), BudgetBills.suggestions(ledger, nov, mapOf(1L to sg.signature)).filter { it.categoryId == 1L })
        // O aluguel subiu: a marca muda e a sugestão volta.
        val later = l(listOf(Budget(1, 100_000)), listOf(rent.copy(amount = 110_000), condo, ipva))
        assertEquals(listOf(1L), BudgetBills.suggestions(later, nov, mapOf(1L to sg.signature)).filter { it.categoryId == 1L }.map { it.categoryId })
    }

    @Test fun orcamentoPelaRendaRespeitaAsContasFixas() {
        val plan = IncomePlanner.plan(l(emptyList()), PlanInputs(fixed = 200_000, variable = 0, useVariable = false))
        val moradia = plan.categories.first { it.categoryId == 1L }
        assertTrue("limite ${moradia.limit} abaixo das contas fixas", moradia.limit >= 120_000L)
    }
}
