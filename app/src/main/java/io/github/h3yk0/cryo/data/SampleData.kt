// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.data

import io.github.h3yk0.cryo.data.db.Account
import io.github.h3yk0.cryo.data.db.AccountType
import io.github.h3yk0.cryo.data.db.Bill
import io.github.h3yk0.cryo.data.db.CategoryKind
import io.github.h3yk0.cryo.data.db.CreditCard
import io.github.h3yk0.cryo.data.db.Debt
import io.github.h3yk0.cryo.data.db.DebtKind
import io.github.h3yk0.cryo.data.db.Goal
import io.github.h3yk0.cryo.data.db.Investment
import io.github.h3yk0.cryo.data.db.InvestmentKind
import io.github.h3yk0.cryo.data.db.Tx
import io.github.h3yk0.cryo.data.db.TxType
import io.github.h3yk0.cryo.domain.Ledger
import io.github.h3yk0.cryo.domain.key
import io.github.h3yk0.cryo.domain.ym
import java.time.LocalDate
import kotlin.random.Random

/**
 * Dados de exemplo (inventados): usados no modo de teste do modo desenvolvedor e nas imagens da loja.
 * Sempre os mesmos (a "sorte" é fixa), para os testes e as imagens não mudarem de uma vez para outra.
 */
object SampleData {
    /** 4 meses de vida financeira: contas, cartão parcelado, contas fixas, metas, investimentos, orçamentos e dívidas. */
    suspend fun fill(r: FinanceRepository, today: LocalDate) {
        r.ensureSeeded()
        val cats = r.loadOnce().categories
        fun cat(n: String) = cats.first { it.name == n }.id
        val rnd = Random(42)

        val principal = r.saveAccount(Account(name = "Conta principal", type = AccountType.CHECKING, initialBalance = 150000, color = Palette.pick(3)))
        val carteira = r.saveAccount(Account(name = "Carteira", type = AccountType.CASH, initialBalance = 15000, color = Palette.pick(9), sortOrder = 1))
        r.saveAccount(Account(name = "Poupança", type = AccountType.SAVINGS, initialBalance = 120000, color = Palette.pick(5), sortOrder = 2))
        r.saveCard(CreditCard(name = "Roxo", limitAmount = 350000, closingDay = 3, dueDay = 10, color = Palette.pick(2), payAccountId = principal))
        val card = r.loadOnce().cards.first()

        val tesouro = r.saveInvestment(Investment(name = "Tesouro Selic 2029", kind = InvestmentKind.TREASURY, color = Palette.pick(4)))
        r.saveTx(Tx(type = TxType.INVEST_IN, amount = 200000, date = today.minusMonths(4), description = "Saldo inicial", investmentId = tesouro))
        r.saveTx(Tx(type = TxType.INVEST_IN, amount = 30000, date = today.minusDays(20), description = "Aporte", accountId = principal, investmentId = tesouro))
        val cdb = r.saveInvestment(Investment(name = "CDB 110% do CDI", kind = InvestmentKind.FIXED_INCOME, color = Palette.pick(14)))
        r.saveTx(Tx(type = TxType.INVEST_IN, amount = 100000, date = today.minusMonths(3), description = "Saldo inicial", investmentId = cdb))
        val invs = r.loadOnce().investments
        r.updateInvestmentValue(invs.first { it.id == tesouro }, 230000, 238530, today.minusDays(3))
        r.updateInvestmentValue(invs.first { it.id == cdb }, 100000, 101240, today.minusDays(3))

        val viagem = r.saveGoal(Goal(name = "Viagem para a praia", target = 400000, deadline = today.plusMonths(8), emoji = "🏖️", color = Palette.pick(7)))
        r.saveTx(Tx(type = TxType.GOAL_IN, amount = 125000, date = today.minusMonths(2), description = "Guardado", accountId = principal, goalId = viagem))
        val reserva = r.saveGoal(Goal(name = "Reserva de emergência", target = 1000000, emoji = "🛡️", color = Palette.pick(8)))
        r.saveTx(Tx(type = TxType.GOAL_IN, amount = 320000, date = today.minusMonths(3), description = "Saldo inicial", goalId = reserva))

        val start = today.ym().minusMonths(3).key()
        val ids = listOf(
            Bill(name = "Salário", amount = 420000, dueDay = 5, kind = CategoryKind.INCOME, categoryId = cat("Salário"), accountId = principal, remind = false, startYm = start),
            Bill(name = "Aluguel", amount = 90000, dueDay = 5, categoryId = cat("Moradia"), accountId = principal, startYm = start),
            Bill(name = "Conta de luz", amount = 18000, dueDay = 10, categoryId = cat("Contas da casa"), accountId = principal, startYm = start),
            Bill(name = "Internet", amount = 9990, dueDay = 15, categoryId = cat("Contas da casa"), accountId = principal, startYm = start),
            Bill(name = "Streaming", amount = 3990, dueDay = 20, categoryId = cat("Assinaturas"), cardId = card.id, startYm = start),
            Bill(name = "Academia", amount = 8990, dueDay = 28, categoryId = cat("Saúde"), accountId = principal, startYm = start),
            Bill(name = "Celular", amount = 4990, dueDay = 3, categoryId = cat("Contas da casa"), accountId = principal, startYm = start),
        ).map { it to r.saveBill(it) }
        val bills = ids.map { (b, id) -> b.copy(id = id) }

        data class T(val desc: String, val cat: String, val min: Int, val max: Int, val cash: Boolean = false, val onCard: Boolean = false)
        val templates = listOf(
            T("Almoço", "Alimentação", 1800, 3500), T("Padaria", "Alimentação", 600, 1800, cash = true),
            T("Mercado", "Mercado", 6000, 22000), T("Corrida de app", "Transporte", 1200, 3200, onCard = true),
            T("Gasolina", "Transporte", 8000, 15000), T("Farmácia", "Saúde", 1500, 6000),
            T("Delivery", "Alimentação", 3500, 7000, onCard = true), T("Cinema", "Lazer", 3000, 6000, onCard = true),
            T("Açaí", "Alimentação", 1200, 2500, cash = true), T("Roupa", "Compras", 8000, 18000, onCard = true),
        )

        for (back in 3 downTo 0) {
            val ym = today.ym().minusMonths(back.toLong())
            val lastDay = if (back == 0) today.dayOfMonth else ym.lengthOfMonth()
            for (b in bills) {
                if (b.dueDay > lastDay) continue
                if (back == 0 && b.name == "Academia") continue // fica vencida, para mostrar o alerta
                val amount = if (b.name == "Conta de luz") 15000L + rnd.nextInt(6000) else b.amount
                r.markBillPaid(b, ym, amount, ym.atDay(b.dueDay), b.accountId, if (b.cardId != null) card else null)
            }
            for (d in 1..lastDay) {
                repeat(if (rnd.nextInt(100) < 55) 1 else 0) {
                    val t = templates[rnd.nextInt(templates.size)]
                    val base = Tx(
                        type = TxType.EXPENSE, amount = (t.min + rnd.nextInt(t.max - t.min)).toLong(), date = ym.atDay(d),
                        description = t.desc, categoryId = cat(t.cat), accountId = if (t.cash) carteira else principal,
                    )
                    if (t.onCard) r.saveCardPurchase(base, card, 1) else r.saveTx(base)
                }
            }
            if (back % 2 == 1) r.saveTx(Tx(type = TxType.INCOME, amount = 60000, date = ym.atDay(18), description = "Freela de design", categoryId = cat("Extras e freelas"), accountId = principal))
            r.saveTx(Tx(type = TxType.EXPENSE, amount = 32000, date = ym.atDay(minOf(lastDay, 7)), description = "Dízimo", categoryId = cat("Dízimo e doações"), accountId = principal))
        }
        r.saveCardPurchase(
            Tx(type = TxType.EXPENSE, amount = 180000, date = today.minusMonths(2).withDayOfMonth(12), description = "Celular novo", categoryId = cat("Compras")),
            card, 10,
        )
        r.saveTx(Tx(type = TxType.TRANSFER, amount = 20000, date = today.minusDays(4), description = "Transferência", accountId = principal, toAccountId = carteira))

        r.saveBudget(cat("Alimentação"), 60000)
        r.saveBudget(cat("Mercado"), 80000)
        r.saveBudget(cat("Transporte"), 30000)
        r.saveBudget(cat("Lazer"), 20000)

        // paga as faturas que já venceram
        val l = Ledger(r.loadOnce(), today)
        for (m in l.invoiceMonths(card)) {
            val inv = l.invoice(card, m)
            if (inv.due.isBefore(today) && inv.remaining > 0) r.payInvoice(card, m, inv.remaining, principal, inv.due)
        }

        fillDebts(r, today, principal)

        // Arquivados: uma meta concluída (com o dinheiro ainda guardado), uma já usada e uma conta antiga
        val show = r.saveGoal(Goal(name = "Show de rock", target = 80000, emoji = "🎸", color = Palette.pick(11), archived = true))
        r.saveTx(Tx(type = TxType.GOAL_IN, amount = 80000, date = today.minusMonths(3), description = "Guardado", accountId = principal, goalId = show))
        val note = r.saveGoal(Goal(name = "Notebook novo", target = 350000, emoji = "💻", color = Palette.pick(6), archived = true))
        r.saveTx(Tx(type = TxType.GOAL_IN, amount = 350000, date = today.minusMonths(4), description = "Saldo inicial", goalId = note))
        r.saveTx(Tx(type = TxType.GOAL_OUT, amount = 350000, date = today.minusMonths(3), description = "Retirado", accountId = principal, goalId = note))
        r.saveAccount(Account(name = "Conta antiga", type = AccountType.CHECKING, initialBalance = 0, color = Palette.pick(12), sortOrder = 3, archived = true))
    }

    /** Dívidas de exemplo: financiamento, empréstimo (com o dinheiro que entrou), dívida com pessoa e uma já quitada. */
    suspend fun fillDebts(r: FinanceRepository, today: LocalDate, account: Long) {
        val ym0 = today.ym()
        suspend fun create(d: Debt, received: Long = 0, at: LocalDate = today): Debt =
            d.copy(id = r.createDebt(d, received, if (received > 0) account else null, at))

        // 36 parcelas de R$ 380; 26 já pagas antes de começar a usar o Cryo.
        val moto = create(
            Debt(
                name = "Financiamento da moto", creditor = "Banco", kind = DebtKind.FINANCING, color = Palette.pick(1),
                initialBalance = 10L * 38_000, installmentAmount = 38_000, installmentCount = 36, paidBefore = 26, dueDay = 15,
                startYm = ym0.minusMonths(3).key(), accountId = account,
            ),
        )
        for (back in 3 downTo 0) {
            val due = ym0.minusMonths(back.toLong()).atDay(15)
            if (!due.isAfter(today)) r.payDebt(moto, 38_000, account, due, installment = true)
        }

        // Pegou R$ 1.900 e vai devolver 12 × R$ 180 (R$ 2.160): a diferença são os juros.
        val loan = create(
            Debt(
                name = "Empréstimo pessoal", creditor = "Banco", kind = DebtKind.LOAN, color = Palette.pick(10),
                initialBalance = 216_000 - 190_000, installmentAmount = 18_000, installmentCount = 12, dueDay = 5,
                startYm = ym0.minusMonths(1).key(), accountId = account,
            ),
            received = 190_000, at = ym0.minusMonths(2).atDay(20),
        )
        for (back in 1 downTo 0) {
            val due = ym0.minusMonths(back.toLong()).atDay(5)
            if (due.isBefore(today)) r.payDebt(loan, 18_000, account, due, installment = true)
        }

        val ana = create(
            Debt(
                name = "Devo à Ana", creditor = "Ana", kind = DebtKind.PERSON, color = Palette.pick(13), initialBalance = 60_000,
                startYm = ym0.minusMonths(2).key(), deadline = today.plusMonths(3), accountId = account,
            ),
            at = today.minusMonths(2),
        )
        r.payDebt(ana, 20_000, account, today.minusMonths(1), installment = false)

        val carne = create(
            Debt(
                name = "Carnê da loja", creditor = "Loja", kind = DebtKind.OTHER, color = Palette.pick(0), initialBalance = 30_000,
                installmentAmount = 15_000, installmentCount = 6, paidBefore = 4, dueDay = 20, startYm = ym0.minusMonths(2).key(),
                accountId = account,
            ),
        )
        r.payDebt(carne, 15_000, account, ym0.minusMonths(2).atDay(20), installment = true)
        r.payDebt(carne, 13_000, account, ym0.minusMonths(1).atDay(20), installment = false, discount = 2_000)
    }
}
