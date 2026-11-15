// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo

import androidx.test.core.app.ApplicationProvider
import io.github.h3yk0.cryo.data.db.Account
import io.github.h3yk0.cryo.data.db.CreditCard
import io.github.h3yk0.cryo.data.db.Tx
import io.github.h3yk0.cryo.data.db.TxType
import io.github.h3yk0.cryo.domain.Ledger
import io.github.h3yk0.cryo.domain.Snapshot
import io.github.h3yk0.cryo.ui.components.Source
import io.github.h3yk0.cryo.ui.tx.TxDraft
import io.github.h3yk0.cryo.ui.tx.refundOf
import io.github.h3yk0.cryo.ui.tx.saveDraft
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.YearMonth

/**
 * Cartão de crédito, a partir do pedido de quem usa (issue #3, 12/11/2026): "não consigo lançar estorno no cartão" e
 * "o total da fatura não bate com o do banco, mesmo depois de importar e corrigir tudo". RF-CAR-07, 10 e 11; ERR-043 e 044.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = TestApp::class)
class CardFixTest {
    private val today = LocalDate.of(2026, 11, 20)
    private val ctx get() = ApplicationProvider.getApplicationContext<android.app.Application>()
    private val account = Account(1, "Nubank", color = 0)

    /** Fecha dia 3 e vence dia 10: compra em 02/11 cai na fatura de novembro; em 04/11, na de dezembro. */
    private val card = CreditCard(1, "Cartão Roxo", limitAmount = 500000, closingDay = 3, dueDay = 10, color = 0, payAccountId = 1)
    private val other = CreditCard(2, "Cartão Azul", limitAmount = 300000, closingDay = 20, dueDay = 27, color = 0, payAccountId = 1)
    private val nov = YearMonth.of(2026, 11)
    private val dec = YearMonth.of(2026, 12)

    private fun day(d: Int) = LocalDate.of(2026, 11, d)
    private fun container(vararg txs: Tx) = AppContainer(ctx, sandbox = true).also {
        runBlocking {
            it.repo.eraseAll()
            it.repo.replaceAll(Snapshot(accounts = listOf(account), cards = listOf(card, other), txs = txs.toList()))
        }
    }
    private suspend fun AppContainer.ledger() = Ledger(repo.loadOnce(), today)

    /**
     * RF-LAN-09 e ERR-053: "Estornar" cria o contrário (hoje, mesma conta ou cartão, na fatura de hoje) e não leva o que
     * é só do original: categoria (a de gasto não serve para receita), importação, conta fixa e parcelas.
     */
    @Test fun estornarCriaOContrarioSemLevarACategoria() = runBlocking {
        val c = container(
            Tx(1, TxType.EXPENSE, 20000, day(4), "MERCADO", categoryId = 7, accountId = 1, billId = 3, billYm = 202611, importKey = "a1|ofx.id:1"),
            Tx(2, TxType.EXPENSE, 9000, day(5), "LOJA", categoryId = 7, cardId = 1, invoiceYm = 202612, installmentGroup = "g", installmentNumber = 1, installmentTotal = 3),
            Tx(3, TxType.INCOME, 5000, day(6), "Venda", categoryId = 9, accountId = 1),
        )
        var l = c.ledger()
        val antes = l.balance(1)
        for (id in 1L..3L) c.repo.saveTx(refundOf(l.s.txs.single { it.id == id }, l))
        l = c.ledger()
        val novos = l.s.txs.filter { it.id > 3 }.sortedBy { it.id }
        assertEquals(listOf(TxType.INCOME, TxType.INCOME, TxType.EXPENSE), novos.map { it.type })
        assertEquals("Estorno · MERCADO", novos[0].description)
        for (t in novos) {
            assertEquals(today, t.date)
            assertNull("a categoria do outro tipo não vai junto", t.categoryId)
            assertNull(t.importKey); assertNull(t.billId); assertNull(t.billYm); assertNull(t.installmentGroup)
        }
        // Conta: o gasto voltou (+200) e a venda saiu (-50).
        assertEquals(antes + 20000 - 5000, l.balance(1))
        // Cartão: o estorno fica no cartão, na fatura de hoje (20/11 fecha em dezembro), e zera a compra.
        assertEquals(1L, novos[1].cardId)
        assertNull(novos[1].accountId)
        assertEquals(202612, novos[1].invoiceYm)
        assertEquals(0L, l.invoice(card, dec).total)
    }

    /** RF-CAR-07: o estorno lançado à mão entra no cartão, diminui a fatura e não mexe no saldo de conta nenhuma. */
    @Test fun estornoNoCartaoAMao() = runBlocking {
        val c = container(Tx(1, TxType.EXPENSE, 20000, day(12), "Loja", cardId = 1, invoiceYm = 202612))
        val d = TxDraft(type = TxType.INCOME, amount = 5000, description = "Estorno da loja", source = Source.Card(1), date = day(14))
        assertNull("receita num cartão é estorno: dá para salvar", d.problem())
        saveDraft(d, c.repo, c.ledger())
        val l = c.ledger()
        val refund = l.s.txs.single { it.type == TxType.INCOME }
        assertEquals(1L, refund.cardId)
        assertNull(refund.accountId)
        assertEquals(202612, refund.invoiceYm)
        assertEquals(15000L, l.invoice(card, dec).total)
        assertEquals(15000L, l.cardOutstanding(1))
        assertEquals(0L, l.balance(1))
        // Sem conta nem cartão, a receita continua pedindo onde entrou.
        assertEquals(false, TxDraft(type = TxType.INCOME, amount = 5000).problem() == null)
    }

    /** ERR-043: editar um estorno importado (trocar o nome, a categoria) não o tira do cartão nem da fatura dele. */
    @Test fun editarEstornoImportadoFicaNoCartao() = runBlocking {
        val c = container(
            Tx(1, TxType.EXPENSE, 20000, day(1), "LOJA", cardId = 1, invoiceYm = 202611),
            Tx(2, TxType.INCOME, 5000, day(14), "ESTORNO LOJA", cardId = 1, invoiceYm = 202611, importKey = "c1|ofx.id:9"),
        )
        val d = TxDraft.fromTx(c.repo.loadOnce().txs.single { it.id == 2L })
        assertEquals(Source.Card(1), d.source)
        assertNull("não pode exigir uma conta para salvar", d.problem())
        d.description = "Estorno da loja"
        saveDraft(d, c.repo, c.ledger())
        val l = c.ledger()
        val t = l.s.txs.single { it.id == 2L }
        assertEquals("Estorno da loja", t.description)
        assertEquals(1L, t.cardId)
        assertNull(t.accountId)
        assertEquals(202611, t.invoiceYm)
        assertEquals("c1|ofx.id:9", t.importKey)
        assertEquals(15000L, l.invoice(card, nov).total)
        assertEquals(0L, l.balance(1))
    }

    /**
     * ERR-044: a compra importada fica na fatura do arquivo do banco. Corrigir o nome ou a categoria não pode recalcular a
     * fatura pela data (o banco fecha em dias que variam); só mudar a data ou o cartão faz a fatura seguir a data de novo.
     */
    @Test fun editarCompraNaoMudaAFatura() = runBlocking {
        // 04/11 cairia em dezembro pela regra do Cryo, mas veio na fatura de novembro do banco.
        val c = container(Tx(1, TxType.EXPENSE, 20000, day(4), "MERCADO", cardId = 1, invoiceYm = 202611, importKey = "c1|csv.h:ab"))
        val d = TxDraft.fromTx(c.repo.loadOnce().txs.single())
        d.description = "Mercado do mês"
        d.amount = 21000
        saveDraft(d, c.repo, c.ledger())
        var l = c.ledger()
        assertEquals(202611, l.s.txs.single().invoiceYm)
        assertEquals(21000L, l.invoice(card, nov).total)
        assertEquals(0L, l.invoice(card, dec).total)

        // Mudou a data: agora a fatura é a da data nova.
        val moved = TxDraft.fromTx(l.s.txs.single())
        moved.date = day(25)
        saveDraft(moved, c.repo, l)
        l = c.ledger()
        assertEquals(202612, l.s.txs.single().invoiceYm)

        // Mudou de cartão (fecha dia 20): 25/11 cai na fatura de dezembro do outro cartão.
        val swapped = TxDraft.fromTx(l.s.txs.single())
        swapped.invoiceYm = 202611
        swapped.source = Source.Card(2)
        assertNull("trocar o cartão desfaz a escolha da fatura", swapped.invoiceYm)
        saveDraft(swapped, c.repo, l)
        val t = c.ledger().s.txs.single()
        assertEquals(2L, t.cardId)
        assertEquals(202612, t.invoiceYm)
    }

    /** RF-CAR-10: a pessoa escolhe a fatura da compra (as parcelas seguem dela) e a fatura que um pagamento quita. */
    @Test fun escolherAFaturaDaCompraEDoPagamento() = runBlocking {
        val c = container(Tx(1, TxType.CARD_PAYMENT, 9000, day(8), "Fatura", accountId = 1, cardId = 1, invoiceYm = 202611))
        // Compra em 02/11: o Cryo sugere novembro; a pessoa viu no banco que entrou em dezembro.
        val buy = TxDraft(type = TxType.EXPENSE, amount = 9000, description = "Farmácia", source = Source.Card(1), date = day(2))
        assertEquals(nov, buy.invoiceFor(card))
        buy.invoiceYm = 202612
        saveDraft(buy, c.repo, c.ledger())
        assertEquals(202612, c.ledger().s.txs.single { it.description == "Farmácia" }.invoiceYm)

        // Parcelada em 3x a partir da fatura escolhida.
        val parcels = TxDraft(type = TxType.EXPENSE, amount = 30000, description = "Sofá", source = Source.Card(1), installments = 3, date = day(2), invoiceYm = 202612)
        saveDraft(parcels, c.repo, c.ledger())
        assertEquals(listOf(202612, 202701, 202702), c.ledger().s.txs.filter { it.description == "Sofá" }.sortedBy { it.installmentNumber }.map { it.invoiceYm })

        // O pagamento: mudar a data não troca a fatura que ele quita; a pessoa pode escolher outra.
        val pay = TxDraft.fromTx(c.ledger().s.txs.single { it.type == TxType.CARD_PAYMENT })
        assertEquals(202611, pay.invoiceYm)
        pay.date = day(9)
        assertEquals(202611, pay.invoiceYm)
        pay.invoiceYm = 202612
        saveDraft(pay, c.repo, c.ledger())
        val l = c.ledger()
        val paid = l.s.txs.single { it.type == TxType.CARD_PAYMENT }
        assertEquals(202612, paid.invoiceYm)
        assertEquals(1L, paid.cardId)
        assertEquals(1L, paid.accountId)
        assertEquals(9000L, l.invoice(card, dec).paid)
        assertEquals(0L, l.invoice(card, nov).paid)
    }

    /** RF-IMP-23: as importações viram grupos (destino + hora), para dar para apagar uma inteira. */
    @Test fun importacoesViramGruposParaApagar() = runBlocking {
        val c = container(
            Tx(1, TxType.EXPENSE, 1000, day(2), "A", accountId = 1, importKey = "a1|ofx.id:1", createdAt = 1000),
            Tx(2, TxType.INCOME, 5000, day(3), "B", accountId = 1, importKey = "a1|ofx.id:2", createdAt = 1000),
            Tx(3, TxType.EXPENSE, 2000, day(4), "C", cardId = 1, invoiceYm = 202611, importKey = "c1|csv.h:ab", createdAt = 2000),
            Tx(4, TxType.EXPENSE, 700, day(5), "À mão", accountId = 1),
        )
        val batches = io.github.h3yk0.cryo.domain.ImportBatches.of(c.repo.loadOnce().txs)
        assertEquals(2, batches.size)
        val cartao = batches.first { it.cardId == 1L }
        val conta = batches.first { it.accountId == 1L }
        assertEquals(2, conta.count)
        assertEquals(4000L, conta.net)
        assertEquals(1, cartao.count)
        // Apagar a importação da conta tira as duas linhas dela e deixa o que foi lançado à mão.
        c.repo.undoImport(conta.txs.map { it.id })
        val after = c.repo.loadOnce().txs
        assertEquals(2, after.size)
        assertTrue(after.any { it.description == "À mão" })
        assertTrue(after.any { it.importKey == "c1|csv.h:ab" })
        // E desfazer devolve tudo, com os mesmos ids.
        c.repo.importTxs(conta.txs)
        assertEquals(setOf(1L, 2L, 3L, 4L), c.repo.loadOnce().txs.map { it.id }.toSet())
    }

    /** RF-CAR-11: "ajustar total" lança a diferença como um ajuste na fatura (para mais ou para menos) e apagar o ajuste desfaz. */
    @Test fun ajustarOTotalDaFatura() = runBlocking {
        val c = container(Tx(1, TxType.EXPENSE, 10000, day(5), "Compra", cardId = 1, invoiceYm = 202612))
        val up = c.repo.adjustInvoice(card, dec, c.ledger().invoice(card, dec).total, 13000, today)!!
        assertEquals(TxType.EXPENSE, up.type)
        assertEquals(3000L, up.amount)
        assertEquals(202612, up.invoiceYm)
        assertEquals(13000L, c.ledger().invoice(card, dec).total)

        val down = c.repo.adjustInvoice(card, dec, 13000, 9000, today)!!
        assertEquals(TxType.INCOME, down.type)
        assertEquals(4000L, down.amount)
        assertEquals(1L, down.cardId)
        assertEquals(9000L, c.ledger().invoice(card, dec).total)
        assertEquals(9000L, c.ledger().cardOutstanding(1))

        assertNull("o total já é esse: não cria ajuste vazio", c.repo.adjustInvoice(card, dec, 9000, 9000, today))
        c.repo.deleteTx(down)
        assertEquals(13000L, c.ledger().invoice(card, dec).total)
        // Nenhuma conta foi tocada: o ajuste é do cartão.
        assertEquals(0L, c.ledger().balance(1))
    }
}
