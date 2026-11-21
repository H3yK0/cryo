// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo

import androidx.test.core.app.ApplicationProvider
import io.github.h3yk0.cryo.data.Backup
import io.github.h3yk0.cryo.data.db.Bill
import io.github.h3yk0.cryo.data.db.BillSkip
import io.github.h3yk0.cryo.data.db.CategoryKind
import io.github.h3yk0.cryo.domain.Ledger
import io.github.h3yk0.cryo.domain.Snapshot
import io.github.h3yk0.cryo.domain.forecast
import io.github.h3yk0.cryo.domain.key
import io.github.h3yk0.cryo.notify.dueReminders
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.YearMonth

/**
 * RF-FIX-12 e RF-EXT-11 (1.6, LA5): pular um mês da conta fixa. O mês pulado sai da lista, das previstas, da previsão e
 * dos avisos; o mês seguinte volta normal; desfazer devolve; o backup leva os pulos (DAD-16).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = TestApp::class)
class BillSkipTest {
    private val today = LocalDate.of(2026, 11, 9)
    private val nov = YearMonth.of(2026, 11)
    private val gym = Bill(id = 1, name = "Academia", amount = 12_000, dueDay = 10, startYm = 202601, remindDaysBefore = 1)
    private val rent = Bill(id = 2, name = "Aluguel", amount = 150_000, dueDay = 15, startYm = 202601)

    private fun ledger(skips: List<BillSkip>) = Ledger(Snapshot(bills = listOf(gym, rent), billSkips = skips), today)

    @Test fun mesPuladoSaiDeTudo() {
        val before = ledger(emptyList())
        val after = ledger(listOf(BillSkip(1, nov.key())))
        // Lista do mês e previstas.
        assertEquals(listOf(1L, 2L), before.billStatuses(nov).map { it.bill.id })
        assertEquals(listOf(2L), after.billStatuses(nov).map { it.bill.id })
        assertEquals(listOf(1L), after.skippedBills(nov).map { it.id })
        // Previsão do fim do mês: a academia não entra mais como conta a pagar.
        assertEquals(before.forecast().pendingBillsExpense - 12_000, after.forecast().pendingBillsExpense)
        // Avisos: amanhã vence a academia; pulada, não avisa.
        assertTrue(dueReminders(before).any { it.bill.id == 1L })
        assertFalse(dueReminders(after).any { it.bill.id == 1L })
    }

    @Test fun mesSeguinteVoltaNormal() {
        val after = ledger(listOf(BillSkip(1, nov.key())))
        assertEquals(listOf(1L, 2L), after.billStatuses(nov.plusMonths(1)).map { it.bill.id })
        assertEquals(emptyList<Bill>(), after.skippedBills(nov.plusMonths(1)))
    }

    @Test fun pularDesfazerEBackup() = runBlocking {
        val c = AppContainer(ApplicationProvider.getApplicationContext(), sandbox = true)
        c.repo.eraseAll()
        val id = c.repo.saveBill(gym.copy(id = 0))
        c.repo.skipBill(id, nov)
        assertEquals(listOf(BillSkip(id, nov.key())).map { it.billId to it.ym }, c.repo.loadOnce().billSkips.map { it.billId to it.ym })
        val json = Backup.export(c.repo.loadOnce(), exportedAt = 1L)
        assertTrue(json.contains("billSkips"))
        assertEquals(listOf(id to nov.key()), Backup.import(json).snapshot.billSkips.map { it.billId to it.ym })
        // Desfazer ("Voltar a contar").
        c.repo.unskipBill(id, nov)
        assertEquals(0, c.repo.loadOnce().billSkips.size)
        // Sem pulos, o backup não ganha o campo (DAD-10); apagar a conta fixa leva os pulos dela.
        assertFalse(Backup.export(c.repo.loadOnce(), exportedAt = 1L).contains("billSkips"))
        c.repo.skipBill(id, nov)
        c.repo.deleteBill(c.repo.loadOnce().bills.single())
        assertEquals(0, c.repo.loadOnce().billSkips.size)
    }

    @Test fun contaFixaDeReceberTambemPula() {
        val salary = Bill(id = 3, name = "Salário", amount = 400_000, dueDay = 5, kind = CategoryKind.INCOME, startYm = 202601)
        val l = Ledger(Snapshot(bills = listOf(salary), billSkips = listOf(BillSkip(3, nov.key()))), today)
        assertEquals(0, l.billStatuses(nov).size)
        assertEquals(0L, l.forecast().pendingBillsIncome)
    }
}
