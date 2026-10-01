// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo

import androidx.test.core.app.ApplicationProvider
import io.github.h3yk0.cryo.data.Backup
import io.github.h3yk0.cryo.data.db.Account
import io.github.h3yk0.cryo.data.db.Bill
import io.github.h3yk0.cryo.data.db.CreditCard
import io.github.h3yk0.cryo.data.db.Tx
import io.github.h3yk0.cryo.data.db.TxType
import io.github.h3yk0.cryo.domain.Ledger
import io.github.h3yk0.cryo.domain.key
import io.github.h3yk0.cryo.domain.ym
import io.github.h3yk0.cryo.notify.dueReminders
import io.github.h3yk0.cryo.notify.reminderTitle
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/** Testa o banco de verdade (Room) com as operações do app. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = TestApp::class)
class RepositoryTest {
    private lateinit var c: AppContainer
    private val today = LocalDate.of(2026, 10, 1)

    @Before fun setUp() {
        c = AppContainer(ApplicationProvider.getApplicationContext())
        runBlocking { c.repo.ensureSeeded() }
    }

    @Test fun compraParceladaEFaturas() = runBlocking {
        val r = c.repo
        val acc = r.saveAccount(Account(name = "Conta", initialBalance = 500000, color = 0))
        val cardId = r.saveCard(CreditCard(name = "Cartão", limitAmount = 300000, closingDay = 3, dueDay = 10, color = 0, payAccountId = acc))
        val card = r.loadOnce().cards.first { it.id == cardId }
        val first = r.saveCardPurchase(Tx(type = TxType.EXPENSE, amount = 100000, date = LocalDate.of(2026, 9, 2), description = "TV"), card, 3)
        assertNotNull(first.installmentGroup)
        var s = r.loadOnce()
        val parts = s.txs.filter { it.installmentGroup == first.installmentGroup }.sortedBy { it.installmentNumber }
        assertEquals(listOf(33334L, 33333L, 33333L), parts.map { it.amount })
        assertEquals(listOf(202609, 202610, 202611), parts.map { it.invoiceYm })
        val l = Ledger(s, today)
        assertEquals(100000L, l.cardOutstanding(cardId))
        assertEquals(200000L, l.availableLimit(card))

        r.payInvoice(card, java.time.YearMonth.of(2026, 9), 33334, acc, LocalDate.of(2026, 9, 10))
        s = r.loadOnce()
        assertEquals(500000L - 33334, Ledger(s, today).balance(acc))

        r.deleteTx(parts[1], wholeGroup = true)
        s = r.loadOnce()
        assertTrue(s.txs.none { it.installmentGroup == first.installmentGroup })
    }

    @Test fun contaFixaEOrcamento() = runBlocking {
        val r = c.repo
        val acc = r.saveAccount(Account(name = "Conta", initialBalance = 0, color = 0))
        val cats = r.loadOnce().categories
        val food = cats.first { it.name == "Alimentação" }.id
        r.saveBudget(food, 10000)
        r.saveTx(Tx(type = TxType.EXPENSE, amount = 7000, date = today, categoryId = food, accountId = acc))
        assertNull(r.budgetAlert(food, today, 7000))
        r.saveTx(Tx(type = TxType.EXPENSE, amount = 1500, date = today, categoryId = food, accountId = acc))
        assertTrue(r.budgetAlert(food, today, 1500)!!.contains("85%"))
        r.saveTx(Tx(type = TxType.EXPENSE, amount = 2000, date = today, categoryId = food, accountId = acc))
        assertTrue(r.budgetAlert(food, today, 2000)!!.startsWith("Orçamento de Alimentação estourado"))

        val billId = r.saveBill(Bill(name = "Internet", amount = 9990, dueDay = 2, accountId = acc, remindDaysBefore = 1, startYm = 202601))
        val bill = r.loadOnce().bills.first { it.id == billId }
        val reminders = dueReminders(Ledger(r.loadOnce(), today))
        assertEquals(1, reminders.size)
        assertEquals("Internet vence amanhã", reminderTitle(reminders.first(), today))
        r.markBillPaid(bill, today.ym(), 9990, today, acc, null)
        assertTrue(dueReminders(Ledger(r.loadOnce(), today)).isEmpty())
        r.unmarkBillPaid(bill, today.ym())
        assertEquals(1, dueReminders(Ledger(r.loadOnce(), today)).size)
        assertEquals(today.ym().key(), 202610)
    }

    @Test fun backupIdaEVolta() = runBlocking {
        seed(c, today)
        val before = c.repo.loadOnce()
        val json = Backup.export(before)
        c.repo.eraseAll()
        assertTrue(c.repo.loadOnce().txs.isEmpty())
        c.repo.replaceAll(Backup.import(json))
        val after = c.repo.loadOnce()
        assertEquals(before.txs.toSet(), after.txs.toSet())
        assertEquals(before.accounts, after.accounts)
        assertEquals(before.cards, after.cards)
        assertEquals(before.goals, after.goals)
        assertEquals(before.bills.toSet(), after.bills.toSet())
        assertEquals(before.budgets.toSet(), after.budgets.toSet())
        assertEquals(before.yields.toSet(), after.yields.toSet())
        assertEquals(Ledger(before, today).netWorth, Ledger(after, today).netWorth)
        val csv = Backup.csv(after, today)
        assertTrue(csv.lines()[0].contains("Data;Tipo;Descrição"))
        assertEquals(after.txs.size + 2, csv.lines().size) // cabeçalho + linhas + linha vazia final
        try { Backup.import("{\"app\":\"Outro\"}"); throw AssertionError("deveria falhar") } catch (e: IllegalArgumentException) { }
    }
}
