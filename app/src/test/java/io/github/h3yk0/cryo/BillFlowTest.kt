// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo

import androidx.test.core.app.ApplicationProvider
import io.github.h3yk0.cryo.data.Backup
import io.github.h3yk0.cryo.data.db.Account
import io.github.h3yk0.cryo.data.db.Bill
import io.github.h3yk0.cryo.data.db.CreditCard
import io.github.h3yk0.cryo.data.db.Tx
import io.github.h3yk0.cryo.data.db.TxType
import io.github.h3yk0.cryo.domain.BillMatcher
import io.github.h3yk0.cryo.domain.BillReconciler
import io.github.h3yk0.cryo.domain.BillState
import io.github.h3yk0.cryo.domain.Ledger
import io.github.h3yk0.cryo.domain.MatchLevel
import io.github.h3yk0.cryo.domain.Snapshot
import io.github.h3yk0.cryo.importer.ImportPlanner
import io.github.h3yk0.cryo.importer.ImportTarget
import io.github.h3yk0.cryo.importer.RowFlag
import io.github.h3yk0.cryo.importer.Statement
import io.github.h3yk0.cryo.importer.StatementFormat
import io.github.h3yk0.cryo.importer.StatementLine
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.YearMonth

/**
 * RF-CFX-02..08: contas fixas que se acertam com o extrato, de ponta a ponta (conferência da importação, banco de dados,
 * organizador e backup). Dados inventados.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = TestApp::class)
class BillFlowTest {
    private val today = LocalDate.of(2026, 11, 20)
    private val nov = YearMonth.of(2026, 11)
    private val ctx get() = ApplicationProvider.getApplicationContext<android.app.Application>()
    private val account = Account(1, "Nubank", color = 0)
    private val card = CreditCard(1, "Cartão Roxo", limitAmount = 500000, closingDay = 3, dueDay = 10, color = 0, payAccountId = 1)
    private val netflix = Bill(1, "Netflix", 5999, dueDay = 12, accountId = 1, startYm = 202601)
    private val luz = Bill(2, "Luz", 18000, dueDay = 10, accountId = 1, startYm = 202601)

    private fun container() = AppContainer(ctx, sandbox = true).also { runBlocking { it.repo.eraseAll() } }
    private fun statement(vararg lines: StatementLine) = Statement(StatementFormat.OFX, lines.toList())
    private fun line(day: Int, desc: String, amount: Long, id: String) = StatementLine(LocalDate.of(2026, 11, day), desc, amount, id)

    /** ERR-040: a linha do extrato entra já ligada à conta fixa: uma movimentação só, e a conta fixa fica paga. */
    @Test fun importarOExtratoDeixaAContaFixaPagaComUmaMovimentacaoSo() = runBlocking {
        val c = container()
        c.repo.replaceAll(Snapshot(accounts = listOf(account), bills = listOf(netflix, luz)))
        val snap = c.repo.loadOnce()
        val st = statement(line(12, "NETFLIX.COM", -5999, "a"), line(11, "DEB AUT CIA ENERG", -20500, "b"), line(13, "PADARIA", -1800, "c"))
        val rows = ImportPlanner.plan(st, ImportTarget(accountId = 1), snap, emptyMap(), invert = false)
        // Netflix: certa, já marcada como pagamento. A luz com outro nome e outro valor não casa sozinha. A padaria, nada.
        assertEquals(MatchLevel.CERTAIN, rows[0].bill!!.level)
        assertTrue(rows[0].payBill)
        assertNull(rows[1].bill)
        assertNull(rows[2].bill)
        c.repo.importTxs(rows.map { ImportPlanner.toTx(it, ImportTarget(accountId = 1), null) })
        val after = c.repo.loadOnce()
        assertEquals(3, after.txs.size)
        val l = Ledger(after, today)
        val status = l.billStatuses(nov).first { it.bill.id == 1L }
        assertEquals(BillState.PAID, status.state)
        assertFalse(status.inferred)
        assertEquals(1L, status.payment!!.accountId) // o pagamento fica na conta do extrato
        // Importar o mesmo extrato de novo não liga nada outra vez (a conta já está paga e a linha já veio).
        val again = ImportPlanner.plan(st, ImportTarget(accountId = 1), after, emptyMap(), invert = false)
        assertEquals(RowFlag.IMPORTED_BEFORE, again[0].flag)
        assertNull(again[0].bill)
    }

    /** RF-CFX-04: depois de a pessoa dizer uma vez que "DEB AUT CIA ENERG" é a Luz, o apelido basta (só o código fica guardado). */
    @Test fun apelidoAprendidoCasaSozinhoNaProximaVez() = runBlocking {
        val c = container()
        assertNull(c.engineStore.billAlias("DEB AUT CIA ENERG 0042"))
        c.engineStore.learnBill("DEB AUT CIA ENERG 0042", 2)
        assertEquals(2L, c.engineStore.billAlias("DEB AUT CIA ENERG 0107")) // o número muda todo mês; as palavras não
        val snap = Snapshot(accounts = listOf(account), bills = listOf(luz))
        val rows = ImportPlanner.plan(statement(line(11, "DEB AUT CIA ENERG 0107", -20500, "b")), ImportTarget(accountId = 1), snap, emptyMap(), false) { d -> c.engineStore.billAlias(d) }
        assertEquals(MatchLevel.CERTAIN, rows[0].bill!!.level)
        val prefs = ctx.getSharedPreferences("cryo_engine_teste", 0).all
        assertTrue(prefs.keys.none { it.contains("energ", ignoreCase = true) } && prefs.values.none { it.toString().contains("ENERG") })
        c.engineStore.clear()
    }

    /** ERR-041: conta fixa marcada como paga na conta, e a cobrança chega pelo extrato do cartão: não entra em dobro. */
    @Test fun pagaAMaoNaContaEExtratoDoCartaoNaoEntraEmDobro() {
        val manual = Tx(1, TxType.EXPENSE, 5999, LocalDate.of(2026, 11, 12), "Netflix", accountId = 1, billId = 1, billYm = 202611)
        val snap = Snapshot(accounts = listOf(account), cards = listOf(card), bills = listOf(netflix), txs = listOf(manual))
        val rows = ImportPlanner.plan(statement(line(13, "NETFLIX.COM", -5999, "x")), ImportTarget(cardId = 1), snap, emptyMap(), invert = false)
        assertEquals(RowFlag.LOOKS_DUPLICATE, rows[0].flag)
        assertFalse(rows[0].selected)
        assertEquals(1L, rows[0].match!!.id)
    }

    /** ERR-042: "desmarcar" o pagamento que veio do extrato não pode apagar a movimentação; ligar duas vezes vale uma. */
    @Test fun ligarDesligarEJuntarNoBanco() = runBlocking {
        val c = container()
        val imported = Tx(1, TxType.EXPENSE, 5999, LocalDate.of(2026, 11, 12), "NETFLIX.COM", accountId = 1, importKey = "a1|ofx.id:a")
        val other = Tx(2, TxType.EXPENSE, 5999, LocalDate.of(2026, 11, 13), "NETFLIX.COM", accountId = 1, importKey = "a1|ofx.id:z")
        c.repo.replaceAll(Snapshot(accounts = listOf(account), bills = listOf(netflix), txs = listOf(imported, other)))
        assertTrue(c.repo.linkBillPayment(1, netflix, nov))
        assertFalse("o mês já tem pagamento", c.repo.linkBillPayment(2, netflix, nov))
        assertEquals(1L, c.repo.loadOnce().txs.single { it.billId == 1L }.id)
        // "Não é": a movimentação continua no Extrato, a conta fixa volta a ficar em aberto e o Cryo não a reconhece de novo.
        c.repo.unlinkBillPayment(1, never = true)
        val snap = c.repo.loadOnce()
        assertEquals(2, snap.txs.size)
        assertEquals(BillMatcher.NOT_A_BILL, snap.txs.first { it.id == 1L }.billYm)
        assertTrue(BillReconciler.suggestions(snap, today).none { it.tx.id == 1L })
        // Pago em dobro: o "Já paguei" e a mesma coisa vinda do extrato. Juntar fica com a do extrato; desfazer volta tudo.
        val manualId = c.repo.markBillPaid(netflix, nov, 5999, LocalDate.of(2026, 11, 12), 1, null)
        val doubled = BillReconciler.suggestions(c.repo.loadOnce(), today).single()
        assertEquals(manualId, doubled.duplicateOf!!.id)
        assertEquals(2L, doubled.tx.id)
        c.repo.mergeBillPayment(doubled.tx.id, doubled.duplicateOf!!)
        val merged = c.repo.loadOnce()
        assertEquals(2, merged.txs.size)
        assertEquals(2L, merged.txs.single { it.billId == 1L }.id)
        c.repo.unlinkBillPayment(2, never = false); c.repo.restore(listOf(doubled.duplicateOf!!))
        assertEquals(manualId, c.repo.loadOnce().txs.single { it.billId == 1L }.id)
    }

    /** RF-CFX-08: a conta fixa criada depois encontra os pagamentos que já estavam no app, mês a mês. */
    @Test fun contaFixaCriadaDepoisEncontraOsPagamentosQueJaExistem() {
        val txs = listOf(
            Tx(1, TxType.EXPENSE, 7999, LocalDate.of(2026, 10, 5), "NETFLIX.COM", accountId = 1, importKey = "a1|ofx.id:o"),
            Tx(2, TxType.EXPENSE, 7999, LocalDate.of(2026, 11, 5), "NETFLIX.COM", accountId = 1, importKey = "a1|ofx.id:n"),
            Tx(3, TxType.EXPENSE, 7999, LocalDate.of(2026, 11, 6), "Posto Avenida", accountId = 1),
        )
        val bill = Bill(7, "Netflix", 7999, dueDay = 5, accountId = 1, startYm = 202609)
        val snap = Snapshot(accounts = listOf(account), bills = listOf(bill), txs = txs)
        val found = BillReconciler.suggestions(snap, today, only = bill)
        assertEquals(listOf(202611 to 2L, 202610 to 1L), found.map { it.ym.year * 100 + it.ym.monthValue to it.tx.id })
        assertTrue(found.all { it.level == MatchLevel.CERTAIN && it.duplicateOf == null })
        // E, antes mesmo de a pessoa confirmar, o livro-caixa já não cobra a conta de novo (RF-CFX-07).
        assertTrue(Ledger(snap, today).billStatuses(nov).single().inferred)
    }

    /** O "não é conta fixa" vai no backup e volta (e um mês impossível continua sendo recusado). */
    @Test fun naoEContaFixaSobreviveAoBackup() {
        val t = Tx(1, TxType.EXPENSE, 5999, LocalDate.of(2026, 11, 12), "NETFLIX.COM", accountId = 1, billYm = BillMatcher.NOT_A_BILL)
        val json = Backup.export(Snapshot(accounts = listOf(account), bills = listOf(netflix), txs = listOf(t)))
        assertEquals(BillMatcher.NOT_A_BILL, Backup.import(json).snapshot.txs.single().billYm)
        val broken = json.replace(Regex("\"billYm\":\\s*0"), "\"billYm\":209913")
        assertTrue(broken != json)
        assertTrue(runCatching { Backup.import(broken) }.isFailure)
    }
}
