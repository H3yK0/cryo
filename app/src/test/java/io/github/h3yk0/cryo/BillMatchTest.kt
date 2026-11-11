// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo

import io.github.h3yk0.cryo.data.db.Bill
import io.github.h3yk0.cryo.data.db.CategoryKind
import io.github.h3yk0.cryo.data.db.Tx
import io.github.h3yk0.cryo.data.db.TxType
import io.github.h3yk0.cryo.domain.BillMatcher
import io.github.h3yk0.cryo.domain.BillState
import io.github.h3yk0.cryo.domain.Ledger
import io.github.h3yk0.cryo.domain.MatchLevel
import io.github.h3yk0.cryo.domain.PayLine
import io.github.h3yk0.cryo.domain.Snapshot
import io.github.h3yk0.cryo.domain.forecast
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

/** RF-CFX-01 e 07: o casador de contas fixas e o livro-caixa que não conta em dobro. Tudo com dados inventados. */
class BillMatchTest {
    private val nov = YearMonth.of(2026, 11)
    private val netflix = Bill(1, "Netflix", 5999, dueDay = 12, accountId = 1, startYm = 202601)
    private val luz = Bill(2, "Conta de luz", 18000, dueDay = 10, accountId = 1, startYm = 202601)
    private val aluguel = Bill(3, "Aluguel", 140000, dueDay = 5, accountId = 1, startYm = 202601)
    private val salario = Bill(4, "Salário", 420000, dueDay = 5, kind = CategoryKind.INCOME, accountId = 1, startYm = 202601)

    @Before fun strings() = JvmStrings.install()

    private fun out(day: Int, amount: Long, desc: String) = PayLine(LocalDate.of(2026, 11, day), amount, desc, expense = true)

    @Test fun nomeEValorIguaisECerta() {
        val m = BillMatcher.rate(netflix, nov, out(12, 5999, "NETFLIX.COM 12/11 ASS"))!!
        assertEquals(MatchLevel.CERTAIN, m.level)
        // Paga dois dias depois, com centavos de diferença: continua certa.
        assertEquals(MatchLevel.CERTAIN, BillMatcher.rate(netflix, nov, out(14, 6000, "Netflix"))!!.level)
        // Acentos, maiúsculas e abreviação do banco não atrapalham.
        assertTrue(BillMatcher.sameName("Conta de luz", "DEB AUT CONTA LUZ 0042"))
        assertTrue(BillMatcher.sameName("Energia", "CIA ENERG PAULISTA"))
        assertFalse(BillMatcher.sameName("Luz", "LUZIA BOUTIQUE"))
        assertFalse(BillMatcher.sameName("Gás", "POSTO GASOLINA AVENIDA"))
    }

    @Test fun contaQueVariaEProvavelEValorSemNomeSoPertoDoVencimento() {
        // Luz de 180 que veio 205: o nome bate, o valor varia: pergunta.
        assertEquals(MatchLevel.LIKELY, BillMatcher.rate(luz, nov, out(11, 20500, "CONTA DE LUZ ENERGIA SA"))!!.level)
        // Mais de 30% de diferença: não é a mesma conta.
        assertNull(BillMatcher.rate(luz, nov, out(11, 30000, "CONTA DE LUZ ENERGIA SA")))
        // Aluguel pago por Pix, sem o nome: valor igual a 2 dias do vencimento = provável; a 12 dias = nada.
        assertEquals(MatchLevel.LIKELY, BillMatcher.rate(aluguel, nov, out(7, 140000, "PIX ENVIADO JOAO SILVA"))!!.level)
        assertNull(BillMatcher.rate(aluguel, nov, out(17, 140000, "PIX ENVIADO JOAO SILVA")))
        // Com o apelido que a pessoa já confirmou (RF-CFX-04), vira certa mesmo com o valor variando.
        assertEquals(MatchLevel.CERTAIN, BillMatcher.rate(luz, nov, out(11, 20500, "DEB AUT CIA ENERG"), alias = true)!!.level)
    }

    @Test fun tipoMesEJanelaPrecisamBater() {
        // Entrada não paga conta de saída, e o salário (conta fixa de entrada) casa com a entrada.
        assertNull(BillMatcher.rate(netflix, nov, PayLine(LocalDate.of(2026, 11, 12), 5999, "Estorno Netflix", expense = false)))
        assertEquals(MatchLevel.CERTAIN, BillMatcher.rate(salario, nov, PayLine(LocalDate.of(2026, 11, 5), 420000, "SALARIO ACME LTDA", expense = false))!!.level)
        // Antes de a conta fixa começar, e longe demais do vencimento: não casa.
        assertNull(BillMatcher.rate(netflix.copy(startYm = 202612), nov, out(12, 5999, "Netflix")))
        assertNull(BillMatcher.rate(netflix, YearMonth.of(2026, 9), out(12, 5999, "Netflix")))
        // Paga em 31 de outubro a conta que vence dia 5: só pode ser a de novembro (a de outubro venceu há 26 dias).
        assertEquals(listOf(nov), BillMatcher.months(aluguel, LocalDate.of(2026, 10, 31)))
        // Em 30 de outubro as duas janelas valem, e a mais perto do vencimento ganha: novembro.
        val early = PayLine(LocalDate.of(2026, 10, 30), 140000, "Aluguel", expense = true)
        assertEquals(nov, BillMatcher.assign(mapOf(1 to early), listOf(aluguel), emptySet()).getValue(1).ym)
    }

    @Test fun cadaContaComUmaLinhaECadaLinhaComUmaConta() {
        val outra = Bill(5, "Spotify", 5999, dueDay = 12, accountId = 1, startYm = 202601)
        val lines = mapOf(
            "a" to out(12, 5999, "NETFLIX.COM"), "b" to out(12, 5999, "SPOTIFY BR"), "c" to out(13, 5999, "NETFLIX.COM"),
            "d" to out(3, 2500, "PADARIA"),
        )
        val m = BillMatcher.assign(lines, listOf(netflix, outra), paid = emptySet())
        // Dois valores iguais: o nome decide. A segunda Netflix do mês (cobrança repetida) fica sem conta.
        assertEquals(1L, m.getValue("a").bill.id)
        assertEquals(5L, m.getValue("b").bill.id)
        assertFalse("c" in m || "d" in m)
        // Conta já paga no mês não entra de novo.
        assertTrue(BillMatcher.assign(lines, listOf(netflix), paid = setOf(1L to 202611)).isEmpty())
    }

    @Test fun parcelaDividaENaoEContaFixaNuncaCasam() {
        val base = Tx(1, TxType.EXPENSE, 5999, LocalDate.of(2026, 11, 12), "Netflix", accountId = 1)
        assertTrue(BillMatcher.eligible(base))
        assertFalse(BillMatcher.eligible(base.copy(installmentTotal = 10, installmentNumber = 3)))
        assertFalse(BillMatcher.eligible(base.copy(debtId = 2)))
        assertFalse(BillMatcher.eligible(base.copy(billId = 1, billYm = 202611)))
        assertFalse(BillMatcher.eligible(base.copy(billYm = BillMatcher.NOT_A_BILL)))
        assertFalse(BillMatcher.eligible(base.copy(type = TxType.CARD_PAYMENT)))
    }

    /**
     * ERR-040: a Netflix importada do extrato (sem ligação) e a conta fixa Netflix eram contadas duas vezes na previsão, e a
     * conta fixa ficava "atrasada". Agora o livro-caixa reconhece o pagamento certo e conta uma vez só.
     */
    @Test fun previsaoNaoContaEmDobroOQueJaEstaNoExtrato() {
        val today = LocalDate.of(2026, 11, 20)
        val imported = Tx(1, TxType.EXPENSE, 5999, LocalDate.of(2026, 11, 12), "NETFLIX.COM", accountId = 1, importKey = "a1|ofx.id:9")
        val with = Ledger(Snapshot(txs = listOf(imported), bills = listOf(netflix)), today)
        val status = with.billStatuses(nov).single()
        assertEquals(BillState.PAID, status.state)
        assertTrue(status.inferred)
        assertEquals(imported, status.payment)
        assertEquals(0L, with.forecast().pendingBillsExpense)
        // A mesma movimentação sem conta fixa dá a mesma previsão: nada em dobro.
        assertEquals(Ledger(Snapshot(txs = listOf(imported)), today).forecast().projectedResult, with.forecast().projectedResult)
        // Se a pessoa disse "não é", a conta fixa volta a ficar em aberto.
        val no = Ledger(Snapshot(txs = listOf(imported.copy(billYm = BillMatcher.NOT_A_BILL)), bills = listOf(netflix)), today)
        assertEquals(BillState.OVERDUE, no.billStatuses(nov).single().state)
        assertEquals(5999L, no.forecast().pendingBillsExpense)
        // Com a ligação feita, deixa de ser "reconhecido" e passa a ser o pagamento de fato.
        val linked = Ledger(Snapshot(txs = listOf(imported.copy(billId = 1, billYm = 202611)), bills = listOf(netflix)), today)
        assertFalse(linked.billStatuses(nov).single().inferred)
        // Valor parecido (provável) não é reconhecido sozinho: só depois de a pessoa dizer.
        val likely = Ledger(Snapshot(txs = listOf(imported.copy(amount = 6999)), bills = listOf(netflix)), today)
        assertEquals(BillState.OVERDUE, likely.billStatuses(nov).single().state)
    }
}
