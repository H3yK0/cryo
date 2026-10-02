// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo

import io.github.h3yk0.cryo.data.DefaultData
import io.github.h3yk0.cryo.data.db.Account
import io.github.h3yk0.cryo.data.db.AccountType
import io.github.h3yk0.cryo.data.db.CreditCard
import io.github.h3yk0.cryo.data.db.Debt
import io.github.h3yk0.cryo.data.db.DebtKind
import io.github.h3yk0.cryo.data.db.Goal
import io.github.h3yk0.cryo.data.db.Investment
import io.github.h3yk0.cryo.data.db.TxType
import io.github.h3yk0.cryo.domain.ParseContext
import io.github.h3yk0.cryo.domain.ParsedEntry
import io.github.h3yk0.cryo.domain.PhraseParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class PhraseParserTest {
    private val today = LocalDate.of(2026, 9, 30) // quarta-feira
    private val cats = DefaultData.categories().mapIndexed { i, c -> c.copy(id = (i + 1).toLong()) }
    private fun cat(name: String) = cats.first { it.name == name }.id

    private val accounts = listOf(
        Account(id = 1, name = "Nubank", type = AccountType.CHECKING, color = 0),
        Account(id = 2, name = "Poupança", type = AccountType.SAVINGS, color = 0),
        Account(id = 3, name = "Carteira", type = AccountType.CASH, color = 0),
        Account(id = 4, name = "Inter", type = AccountType.CHECKING, color = 0),
        Account(id = 5, name = "Vale refeição", type = AccountType.BENEFIT, color = 0),
    )
    private val cards = listOf(
        CreditCard(id = 1, name = "Nubank", limitAmount = 500_000, closingDay = 3, dueDay = 10, color = 0),
        CreditCard(id = 2, name = "Inter Black", limitAmount = 300_000, closingDay = 28, dueDay = 5, color = 0),
    )
    private val investments = listOf(
        Investment(id = 1, name = "Tesouro Selic 2029", color = 0),
        Investment(id = 2, name = "CDB Banco Inter", color = 0),
    )
    private val goals = listOf(
        Goal(id = 1, name = "Viagem para o Chile", target = 1_000_000, color = 0),
        Goal(id = 2, name = "Reserva de emergência", target = 2_000_000, color = 0),
    )
    private val ctx = ParseContext(
        today = today, accounts = accounts, cards = cards, categories = cats,
        investments = investments, goals = goals,
        learned = mapOf("padaria" to cat("Mercado")), defaultAccountId = 1,
    )

    private fun p(s: String): ParsedEntry = PhraseParser(ctx).parse(s)

    @Test fun mercadoNoDebito() {
        val r = p("mercado 120 no débito")
        assertEquals(TxType.EXPENSE, r.type)
        assertEquals(12000L, r.amount)
        assertEquals(cat("Mercado"), r.categoryId)
        assertEquals(1L, r.accountId)
        assertNull(r.cardId)
        assertEquals("Mercado", r.description)
        assertEquals(today, r.date)
    }

    @Test fun almocoComCentavos() {
        val r = p("almoço 32,50")
        assertEquals(3250L, r.amount)
        assertEquals(cat("Alimentação"), r.categoryId)
        assertEquals("Almoço", r.description)
    }

    @Test fun uberNubankUsaConta() {
        val r = p("uber 23,90 nubank")
        assertEquals(2390L, r.amount)
        assertEquals(1L, r.accountId)
        assertNull(r.cardId)
        assertEquals(cat("Transporte"), r.categoryId)
        assertEquals("Uber", r.description)
    }

    @Test fun parceladoNoCartao() {
        val r = p("tv 1200 em 10x no cartão")
        assertEquals(TxType.EXPENSE, r.type)
        assertEquals(120000L, r.amount)
        assertEquals(1L, r.cardId)
        assertEquals(10, r.installments)
        assertEquals(cat("Compras"), r.categoryId)
        assertEquals("TV", r.description)
    }

    @Test fun salario() {
        val r = p("recebi 3000 de salário")
        assertEquals(TxType.INCOME, r.type)
        assertEquals(300000L, r.amount)
        assertEquals(cat("Salário"), r.categoryId)
        assertEquals(1L, r.accountId)
        assertEquals("Salário", r.description)
    }

    @Test fun transferenciaParaPoupanca() {
        val r = p("transferi 200 para poupança")
        assertEquals(TxType.TRANSFER, r.type)
        assertEquals(20000L, r.amount)
        assertEquals(1L, r.accountId)
        assertEquals(2L, r.toAccountId)
        assertEquals("Transferência", r.description)
    }

    @Test fun transferenciaEntreContasCitadas() {
        val r = p("transferi 100 da nubank para inter")
        assertEquals(TxType.TRANSFER, r.type)
        assertEquals(1L, r.accountId)
        assertEquals(4L, r.toAccountId)
        val r2 = p("passei 100 para o inter da nubank")
        assertEquals(1L, r2.accountId)
        assertEquals(4L, r2.toAccountId)
    }

    @Test fun ontemFarmacia() {
        val r = p("gastei 50 com farmácia ontem")
        assertEquals(5000L, r.amount)
        assertEquals(cat("Saúde"), r.categoryId)
        assertEquals(today.minusDays(1), r.date)
        assertEquals("Farmácia", r.description)
    }

    @Test fun contaDeLuz() {
        val r = p("paguei a luz 180,75")
        assertEquals(18075L, r.amount)
        assertEquals(cat("Contas da casa"), r.categoryId)
        assertEquals("Luz", r.description)
        val r2 = p("conta de luz 230")
        assertEquals(23000L, r2.amount)
        assertEquals(cat("Contas da casa"), r2.categoryId)
    }

    @Test fun investimento() {
        val r = p("investi 500 no tesouro")
        assertEquals(TxType.INVEST_IN, r.type)
        assertEquals(50000L, r.amount)
        assertEquals(1L, r.investmentId)
        assertEquals(1L, r.accountId)
        val r2 = p("resgatei 200 do cdb")
        assertEquals(TxType.INVEST_OUT, r2.type)
        assertEquals(2L, r2.investmentId)
        val r3 = p("investi 1000 no cdb do inter")
        assertEquals(2L, r3.investmentId)
        assertEquals(1L, r3.accountId)
    }

    @Test fun metas() {
        val r = p("guardei 100 na caixinha viagem")
        assertEquals(TxType.GOAL_IN, r.type)
        assertEquals(10000L, r.amount)
        assertEquals(1L, r.goalId)
        val r2 = p("tirei 300 da reserva")
        assertEquals(TxType.GOAL_OUT, r2.type)
        assertEquals(2L, r2.goalId)
    }

    @Test fun valoresEscritosDeFormasDiferentes() {
        assertEquals(125000L, p("R$ 1.250,00 aluguel").amount)
        assertEquals(200000L, p("2 mil de freela").amount)
        assertEquals(TxType.INCOME, p("2 mil de freela").type)
        assertEquals(5050L, p("ganhei 50 reais e 50 centavos").amount)
        assertEquals(350000L, p("salario 3.500").amount)
        assertEquals(150000L, p("1500 aluguel").amount)
        assertEquals(700L, p("café 7").amount)
        assertNull(p("almoço").amount)
    }

    @Test fun escolheONumeroCerto() {
        val r = p("comprei 2 pizzas por 80")
        assertEquals(8000L, r.amount)
        assertEquals(cat("Alimentação"), r.categoryId)
        assertEquals("2 pizzas", r.description)
        val r2 = p("uber 99 25")
        assertEquals(2500L, r2.amount)
        assertEquals(cat("Transporte"), r2.categoryId)
    }

    @Test fun datas() {
        assertEquals(LocalDate.of(2026, 9, 5), p("pizza 45 dia 5").date)
        val r = p("luz 180 dia 10/09")
        assertEquals(LocalDate.of(2026, 9, 10), r.date)
        assertEquals("Luz", r.description)
        assertEquals(LocalDate.of(2026, 9, 25), p("sexta passada lanche 20").date)
        assertEquals(LocalDate.of(2026, 8, 31), p("mercado 300 dia 31").date)
    }

    @Test fun aprendeComHistorico() {
        assertEquals(cat("Mercado"), p("padaria 12").categoryId)
    }

    @Test fun creditoEPalavrasCompostas() {
        val r = p("netflix 55,90 no crédito")
        assertEquals(1L, r.cardId)
        assertEquals(cat("Assinaturas"), r.categoryId)
        val r2 = p("mercado livre 150 em 3x")
        assertEquals(cat("Compras"), r2.categoryId)
        assertEquals(3, r2.installments)
        assertEquals(1L, r2.cardId)
    }

    @Test fun formasDePagamento() {
        val r = p("uber 15 ontem pix")
        assertEquals(1L, r.accountId)
        assertEquals(today.minusDays(1), r.date)
        assertEquals(5L, p("almoço 25 no vale").accountId)
        assertEquals(3L, p("pastel 8 em dinheiro").accountId)
        assertEquals(2L, p("pastel 8 no inter black").cardId)
    }

    @Test fun fatura() {
        val r = p("paguei a fatura do nubank 850")
        assertEquals(TxType.CARD_PAYMENT, r.type)
        assertEquals(85000L, r.amount)
        assertEquals(1L, r.cardId)
        assertEquals(1L, r.accountId)
    }

    @Test fun dizimo() {
        val r = p("dízimo 300")
        assertEquals(cat("Dízimo e doações"), r.categoryId)
        assertEquals(30000L, r.amount)
    }

    /* ------------------------------ dívidas ------------------------------ */

    private val debts = listOf(
        Debt(
            id = 1, name = "Financiamento da moto", creditor = "Banco", kind = DebtKind.FINANCING, color = 0,
            initialBalance = 600_000, installmentAmount = 45_000, installmentCount = 24, dueDay = 15,
            startYm = 202609, accountId = 4,
        ),
        Debt(id = 2, name = "Devo ao João", creditor = "João", kind = DebtKind.PERSON, color = 0, initialBalance = 80_000, startYm = 202609),
    )
    private val debtCat = cats.first { it.icon == DefaultData.DEBT_ICON }.id
    private fun pd(s: String, list: List<Debt> = debts) = PhraseParser(ctx.copy(debts = list)).parse(s)

    @Test fun parcelaDeDivida() {
        val r = pd("paguei a parcela da moto")
        assertEquals(TxType.EXPENSE, r.type)
        assertEquals(1L, r.debtId)
        assertEquals(45_000L, r.amount) // sem valor na frase: usa o valor da parcela
        assertEquals(debtCat, r.categoryId)
        assertEquals(4L, r.accountId) // a conta cadastrada para pagar a dívida
        assertNull(r.cardId)
        assertEquals("Parcela · Financiamento da moto", r.description)
        val r2 = pd("paguei 460 da prestação do financiamento pelo nubank")
        assertEquals(1L, r2.debtId)
        assertEquals(46_000L, r2.amount)
        assertEquals(1L, r2.accountId)
    }

    @Test fun pagamentoParaPessoa() {
        val r = pd("paguei 200 pro joão")
        assertEquals(TxType.EXPENSE, r.type)
        assertEquals(2L, r.debtId)
        assertEquals(20_000L, r.amount)
        assertEquals(debtCat, r.categoryId)
        assertEquals("Pagamento · Devo ao João", r.description)
    }

    @Test fun pegueiEmprestado() {
        val r = pd("peguei 300 emprestado com o joão")
        assertEquals(TxType.DEBT_IN, r.type)
        assertEquals(2L, r.debtId)
        assertEquals(30_000L, r.amount)
        assertEquals(1L, r.accountId)
        assertNull(r.categoryId) // não é receita
        val r2 = p("peguei 500 emprestado")
        assertEquals(TxType.DEBT_IN, r2.type)
        assertNull(r2.debtId)
        assertEquals(TxType.DEBT_IN, pd("a Ana me emprestou 150", emptyList()).type)
    }

    @Test fun gastoComAMotoNaoEDivida() {
        val r = pd("gasolina da moto 50")
        assertEquals(TxType.EXPENSE, r.type)
        assertNull(r.debtId)
        assertEquals(cat("Transporte"), r.categoryId)
        val r2 = pd("paguei a gasolina da moto 50")
        assertNull(r2.debtId)
        assertEquals(cat("Transporte"), r2.categoryId)
        assertEquals(5_000L, r2.amount)
        // compra parcelada no cartão continua sendo compra no cartão
        val r3 = pd("comprei uma tv em 10 parcelas no cartão 2000")
        assertNull(r3.debtId)
        assertEquals(1L, r3.cardId)
        assertEquals(10, r3.installments)
        // com uma só dívida, "paguei a parcela" vai para ela, mas não se a frase fala de outra coisa
        val one = debts.take(1)
        assertEquals(1L, pd("paguei a parcela 450", one).debtId)
        assertNull(pd("paguei a parcela do curso de inglês 300", one).debtId)
    }

    @Test fun dividaArquivadaNaoEntra() {
        val r = pd("paguei a parcela da moto", debts.map { it.copy(archived = true) })
        assertNull(r.debtId)
    }
}
