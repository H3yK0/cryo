// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo

import androidx.test.core.app.ApplicationProvider
import io.github.h3yk0.cryo.data.Backup
import io.github.h3yk0.cryo.data.DefaultData
import io.github.h3yk0.cryo.data.db.Account
import io.github.h3yk0.cryo.data.db.Bill
import io.github.h3yk0.cryo.data.db.CreditCard
import io.github.h3yk0.cryo.data.db.Debt
import io.github.h3yk0.cryo.data.db.DebtKind
import io.github.h3yk0.cryo.data.db.Tx
import io.github.h3yk0.cryo.data.db.TxType
import io.github.h3yk0.cryo.domain.Ledger
import io.github.h3yk0.cryo.domain.debtCategory
import io.github.h3yk0.cryo.domain.key
import io.github.h3yk0.cryo.domain.ym
import org.json.JSONArray
import org.json.JSONObject
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
        assertTrue(before.debts.isNotEmpty() && before.debtAdjustments.isNotEmpty())
        val json = Backup.export(before)
        c.repo.eraseAll()
        assertTrue(c.repo.loadOnce().txs.isEmpty())
        val restored = Backup.import(json)
        assertEquals(Backup.VERSION, restored.version)
        c.repo.replaceAll(restored.snapshot)
        val after = c.repo.loadOnce()
        assertEquals(before.txs.toSet(), after.txs.toSet())
        assertEquals(before.accounts, after.accounts)
        assertEquals(before.cards, after.cards)
        assertEquals(before.categories, after.categories)
        assertEquals(before.goals, after.goals)
        assertEquals(before.bills.toSet(), after.bills.toSet())
        assertEquals(before.budgets.toSet(), after.budgets.toSet())
        assertEquals(before.yields.toSet(), after.yields.toSet())
        assertEquals(before.debts, after.debts)
        assertEquals(before.debtAdjustments.toSet(), after.debtAdjustments.toSet())
        assertEquals(Ledger(before, today).netWorth, Ledger(after, today).netWorth)
        val csv = Backup.csv(after, today)
        assertTrue(csv.lines()[0].contains("Data;Tipo;Descrição"))
        assertTrue(csv.lines()[0].endsWith(";Dívida"))
        assertEquals(after.txs.size + 2, csv.lines().size) // cabeçalho + linhas + linha vazia final
        try { Backup.import("{\"app\":\"Outro\"}"); throw AssertionError("deveria falhar") } catch (e: IllegalArgumentException) { }
    }

    /** Backup feito na 1.0 (sem dívidas) abre na 1.1 e ganha a categoria de dívidas. */
    @Test fun backupDaVersao10() = runBlocking {
        val r = c.repo
        val acc = r.saveAccount(Account(name = "Conta", initialBalance = 1_000, color = 0))
        r.saveTx(Tx(type = TxType.EXPENSE, amount = 500, date = today, accountId = acc, description = "Pão"))
        val root = JSONObject(Backup.export(r.loadOnce()))
        root.put("version", 1)
        root.remove("debts")
        root.remove("debtAdjustments")
        val cats = root.getJSONArray("categories")
        val old = JSONArray()
        for (i in 0 until cats.length()) cats.getJSONObject(i).let { if (it.getString("icon") != DefaultData.DEBT_ICON) old.put(it) }
        root.put("categories", old)
        val txs = root.getJSONArray("transactions")
        for (i in 0 until txs.length()) txs.getJSONObject(i).remove("debtId")

        val restored = Backup.import(root.toString())
        assertEquals(1, restored.version)
        assertNull(restored.snapshot.categories.debtCategory())
        r.replaceAll(restored.snapshot, fromOldVersion = true)
        val s = r.loadOnce()
        assertNotNull(s.categories.debtCategory())
        assertEquals("Pão", s.txs.single().description)
        assertTrue(s.debts.isEmpty())

        root.put("version", Backup.VERSION + 1)
        try { Backup.import(root.toString()); throw AssertionError("deveria recusar backup mais novo") } catch (e: IllegalArgumentException) { }
    }

    @Test fun dividasNoBanco() = runBlocking {
        val r = c.repo
        val acc = r.saveAccount(Account(name = "Conta", initialBalance = 100_000, color = 0))
        val id = r.createDebt(
            Debt(
                name = "Empréstimo", kind = DebtKind.LOAN, color = 0, initialBalance = 0, installmentAmount = 25_000,
                installmentCount = 4, dueDay = 10, startYm = 202610, accountId = acc,
            ),
            received = 100_000, accountId = acc, date = today,
        )
        var l = Ledger(r.loadOnce(), today)
        val d = l.debt[id]!!
        assertEquals(200_000L, l.balance(acc)) // o dinheiro do empréstimo entrou na conta
        assertEquals(0L, l.monthSummary(today.ym()).income) // mas não é receita
        assertEquals(100_000L, l.debtInfo(d).outstanding)
        assertEquals(4, l.debtInfo(d).installmentsLeft)

        // parcela paga com R$ 10 de multa: a multa sai da conta mas não abate a dívida
        val u = r.payDebt(d, 25_000, acc, today, installment = true, extra = 1_000)
        l = Ledger(r.loadOnce(), today)
        assertEquals(75_000L, l.debtInfo(d).outstanding)
        assertEquals(200_000L - 26_000, l.balance(acc))
        val parcela = l.s.txs.single { it.debtId == id && it.type == TxType.EXPENSE }
        assertEquals("Parcela · Empréstimo", parcela.description)
        assertEquals(r.debtCategoryId(), parcela.categoryId)
        assertTrue(l.s.txs.any { it.description == "Juros/multa · Empréstimo" && it.debtId == null })
        assertEquals(26_000L, l.monthSummary(today.ym()).expense)

        r.undo(u)
        l = Ledger(r.loadOnce(), today)
        assertEquals(100_000L, l.debtInfo(d).outstanding)
        assertEquals(200_000L, l.balance(acc))

        // correção do saldo e quitação com desconto
        r.adjustDebt(d, 5_000, today, "Juros e encargos")
        assertEquals(105_000L, Ledger(r.loadOnce(), today).debtInfo(d).outstanding)
        r.payDebt(d, 95_000, acc, today, installment = false, discount = 10_000)
        l = Ledger(r.loadOnce(), today)
        assertTrue(l.debtInfo(d).isPaidOff)
        assertTrue(l.debtOverview.open.isEmpty())

        // apagar a dívida mantém os pagamentos no extrato (o dinheiro saiu de verdade)
        r.deleteDebt(d)
        val s = r.loadOnce()
        assertTrue(s.debts.isEmpty())
        assertTrue(s.debtAdjustments.isEmpty())
        assertTrue(s.txs.any { it.amount == 95_000L && it.debtId == null })
    }
}
