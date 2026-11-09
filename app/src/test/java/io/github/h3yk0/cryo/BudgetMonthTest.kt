// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo

import androidx.test.core.app.ApplicationProvider
import io.github.h3yk0.cryo.data.Backup
import io.github.h3yk0.cryo.data.db.Budget
import io.github.h3yk0.cryo.data.db.Category
import io.github.h3yk0.cryo.data.db.CategoryKind
import io.github.h3yk0.cryo.data.db.Goal
import io.github.h3yk0.cryo.data.db.Tx
import io.github.h3yk0.cryo.data.db.TxType
import io.github.h3yk0.cryo.domain.GoalArt
import io.github.h3yk0.cryo.domain.Ledger
import io.github.h3yk0.cryo.domain.key
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.YearMonth

/**
 * DAD-12..14 (banco versão 4): remanejar o orçamento só no mês, dias sem gastos e a ilustração das metas, inclusive no
 * backup (e backups antigos, só com o emoji, ganham a ilustração).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = TestApp::class)
class BudgetMonthTest {
    private val today = LocalDate.of(2026, 11, 9)
    private val nov = YearMonth.of(2026, 11)

    private fun container() = AppContainer(ApplicationProvider.getApplicationContext(), sandbox = true).also {
        runBlocking { it.repo.eraseAll() }
    }

    @Test fun remanejarValeSoNoMes() = runBlocking {
        val c = container()
        val food = c.repo.saveCategory(Category(name = "Comida", kind = CategoryKind.EXPENSE, icon = "restaurant", color = 0))
        val fun_ = c.repo.saveCategory(Category(name = "Lazer", kind = CategoryKind.EXPENSE, icon = "celebration", color = 0))
        c.repo.saveBudget(food, 100_000)
        c.repo.saveBudget(fun_, 30_000)
        val group = c.repo.moveBudget(from = fun_, to = food, amount = 8_000, ym = nov.key())
        val l = Ledger(c.repo.loadOnce(), today)
        assertEquals(108_000L, l.monthLimit(food, nov))
        assertEquals(22_000L, l.monthLimit(fun_, nov))
        // Os outros meses continuam com o limite de sempre.
        assertEquals(100_000L, l.monthLimit(food, nov.plusMonths(1)))
        assertEquals(108_000L, l.budgetUsages(nov).single { it.category.id == food }.limit)
        // Desfazer tira as duas pontas.
        c.repo.undoBudgetMove(group)
        val back = Ledger(c.repo.loadOnce(), today)
        assertEquals(100_000L, back.monthLimit(food, nov))
        assertEquals(30_000L, back.monthLimit(fun_, nov))
        // Limite só deste mês, nunca abaixo de zero.
        c.repo.adjustMonthLimit(fun_, nov.key(), -50_000)
        assertEquals(0L, Ledger(c.repo.loadOnce(), today).monthLimit(fun_, nov))
    }

    @Test fun alertaAoSalvarUsaOLimiteDoMes() = runBlocking {
        val c = container()
        val food = c.repo.saveCategory(Category(name = "Comida", kind = CategoryKind.EXPENSE, icon = "restaurant", color = 0))
        val other = c.repo.saveCategory(Category(name = "Lazer", kind = CategoryKind.EXPENSE, icon = "celebration", color = 0))
        c.repo.saveBudget(food, 10_000)
        c.repo.saveBudget(other, 10_000)
        c.repo.moveBudget(from = other, to = food, amount = 5_000, ym = nov.key())
        val acc = c.repo.saveAccount(io.github.h3yk0.cryo.data.db.Account(name = "Conta", color = 0))
        c.repo.saveTx(Tx(type = TxType.EXPENSE, amount = 11_000, date = today, categoryId = food, accountId = acc))
        // 11.000 de 15.000 = 73%: sem alerta (sem o remanejamento seria "passou do limite").
        assertEquals(null, c.repo.budgetAlert(food, today, 11_000))
    }

    @Test fun backupLevaOsDadosNovosEAntigosGanhamIlustracao() = runBlocking {
        val c = container()
        val food = c.repo.saveCategory(Category(name = "Comida", kind = CategoryKind.EXPENSE, icon = "restaurant", color = 0))
        val fun_ = c.repo.saveCategory(Category(name = "Lazer", kind = CategoryKind.EXPENSE, icon = "celebration", color = 0))
        c.repo.saveBudget(food, 100_000); c.repo.saveBudget(fun_, 30_000)
        c.repo.moveBudget(fun_, food, 8_000, nov.key())
        c.repo.markNoSpend(today.minusDays(1))
        c.repo.saveGoal(Goal(name = "Praia", target = 400_000, color = 0, art = GoalArt.BEACH.key))
        val json = Backup.export(c.repo.loadOnce(), exportedAt = 1L)
        val back = Backup.import(json).snapshot
        assertEquals(2, back.budgetAdjustments.size)
        assertEquals(listOf(today.minusDays(1)), back.noSpendDays.map { it.date })
        assertEquals("beach", back.goals.single().art)
        assertTrue("o emoji continua para versões antigas", back.goals.single().emoji.isNotEmpty())
        // Um backup da 1.6, só com o emoji.
        val old = json.replace(Regex("\"art\":\"beach\",?"), "")
        assertEquals("beach", Backup.import(old).snapshot.goals.single().art)
        // Sem nada novo, o backup não ganha os campos novos (DAD-10).
        c.repo.eraseAll()
        val plain = Backup.export(c.repo.loadOnce(), exportedAt = 1L)
        assertTrue(!plain.contains("budgetAdjustments") && !plain.contains("noSpendDays"))
    }
}
