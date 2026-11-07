// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo

import io.github.h3yk0.cryo.data.db.Account
import io.github.h3yk0.cryo.data.db.AccountType
import io.github.h3yk0.cryo.data.db.Bill
import io.github.h3yk0.cryo.data.db.Category
import io.github.h3yk0.cryo.data.db.CategoryKind
import io.github.h3yk0.cryo.data.db.CreditCard
import io.github.h3yk0.cryo.data.db.Debt
import io.github.h3yk0.cryo.data.db.Goal
import io.github.h3yk0.cryo.data.db.Investment
import io.github.h3yk0.cryo.data.db.InvestmentYield
import io.github.h3yk0.cryo.data.db.Tx
import io.github.h3yk0.cryo.data.db.TxType
import io.github.h3yk0.cryo.domain.CardMath
import io.github.h3yk0.cryo.domain.Snapshot
import io.github.h3yk0.cryo.domain.dayClamped
import io.github.h3yk0.cryo.domain.key
import io.github.h3yk0.cryo.domain.ym
import io.github.h3yk0.cryo.domain.engine.Rng
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.exp
import kotlin.math.roundToLong

/**
 * Históricos inventados para os testes do motor (RF-MOT). Nada aqui vem de extrato de verdade (RNF-PRIV-04): pessoas,
 * lojas e valores são sorteados com uma semente, então o mesmo teste gera sempre os mesmos dados.
 */
object Personas {
    enum class Kind { STABLE, IRREGULAR, NEWCOMER, INDEBTED }

    data class Spec(
        val kind: Kind,
        val months: Int = 24,
        val salary: Long = 520000,
        val salaryDay: Int = 5,
        /** Gasto médio por compra do dia a dia (centavos). */
        val ticket: Double = 4200.0,
        val sigma: Double = 0.55,
        /** Compras por dia (média). */
        val perDay: Double = 1.4,
        val bigEvery: Int = 75,
        val card: Boolean = true,
        val accounts: Int = 2,
        val netflixRaiseMonth: Int? = null,
    )

    fun spec(kind: Kind) = when (kind) {
        Kind.STABLE -> Spec(kind, netflixRaiseMonth = 20)
        Kind.IRREGULAR -> Spec(kind, salary = 0, ticket = 5200.0, sigma = 0.75, perDay = 1.6, bigEvery = 40)
        Kind.NEWCOMER -> Spec(kind, months = 2)
        Kind.INDEBTED -> Spec(kind, salary = 380000, ticket = 4800.0, perDay = 1.6, bigEvery = 50)
    }

    val categories = listOf(
        Category(1, "Alimentação", CategoryKind.EXPENSE, "restaurant", 0),
        Category(2, "Mercado", CategoryKind.EXPENSE, "cart", 0),
        Category(3, "Transporte", CategoryKind.EXPENSE, "car", 0),
        Category(4, "Lazer", CategoryKind.EXPENSE, "movie", 0),
        Category(5, "Assinaturas", CategoryKind.EXPENSE, "subscriptions", 0),
        Category(6, "Moradia", CategoryKind.EXPENSE, "home", 0),
        Category(7, "Contas da casa", CategoryKind.EXPENSE, "bolt", 0),
        Category(8, "Salário", CategoryKind.INCOME, "work", 0),
        Category(9, "Compras", CategoryKind.EXPENSE, "shopping", 0),
        Category(10, "Dívidas", CategoryKind.EXPENSE, "debt", 0),
        Category(11, "Trabalhos", CategoryKind.INCOME, "laptop", 0),
    )

    /** Lojas do dia a dia: (descrição, categoria, peso no valor). */
    private val shops = listOf(
        Triple("Padaria Pão Quente", 1L, 0.35), Triple("Restaurante Sabor Caseiro", 1L, 1.2), Triple("Lanchonete da Esquina", 1L, 0.6),
        Triple("Supermercado Bom Preço", 2L, 2.6), Triple("Mercadinho Vila Nova", 2L, 1.1), Triple("Hortifruti Verde", 2L, 0.8),
        Triple("Uber Viagem", 3L, 0.7), Triple("Posto Avenida", 3L, 2.2), Triple("Cinema Shopping Centro", 4L, 1.3),
        Triple("Bar do Zé", 4L, 1.4), Triple("Livraria Leitura", 9L, 1.6), Triple("Farmácia Saúde Já", 9L, 0.9),
    )

    fun generate(spec: Spec, end: LocalDate, seed: Long): Snapshot {
        val rng = Rng(seed)
        val start = end.ym().minusMonths((spec.months - 1).toLong()).atDay(1)
        val txs = ArrayList<Tx>()
        var id = 1L
        fun add(t: Tx) { txs += t.copy(id = id++) }
        val accounts = (1..spec.accounts).map {
            Account(it.toLong(), if (it == 1) "Conta principal" else "Conta $it", if (it == 2) AccountType.CASH else AccountType.CHECKING,
                initialBalance = if (it == 1) 300000 else 20000, color = 0, sortOrder = it)
        }
        val card = CreditCard(1, "Cartão Roxo", limitAmount = 800000, closingDay = 3, dueDay = 10, color = 0, payAccountId = 1)
        val cards = if (spec.card) listOf(card) else emptyList()
        val rent = Bill(1, "Aluguel", 140000, dueDay = 10, categoryId = 6, accountId = 1, startYm = start.ym().key())
        val bills = listOf(rent)
        val debts = ArrayList<Debt>()
        if (spec.kind == Kind.INDEBTED) {
            debts += Debt(1, "Empréstimo pessoal", creditor = "Banco", color = 0, initialBalance = 36L * 62000, installmentAmount = 62000,
                installmentCount = 36, dueDay = 15, startYm = start.ym().key(), accountId = 1)
        }
        fun lognormal(mean: Double, sigma: Double) = mean * exp(sigma * rng.nextGaussian() - sigma * sigma / 2)
        fun payDay(ym: YearMonth, day: Int): LocalDate {
            var d = ym.dayClamped(day)
            // Salário no fim de semana cai na sexta antes.
            while (d.dayOfWeek == DayOfWeek.SATURDAY || d.dayOfWeek == DayOfWeek.SUNDAY) d = d.minusDays(1)
            return d
        }
        var ym = start.ym()
        var monthIndex = 0
        while (!ym.isAfter(end.ym())) {
            if (spec.salary > 0) {
                val d = payDay(ym, spec.salaryDay)
                if (!d.isAfter(end)) add(Tx(type = TxType.INCOME, amount = spec.salary, date = d, description = "Salário ACME Ltda", categoryId = 8, accountId = 1))
            } else {
                // Renda irregular: 2 a 4 trabalhos por mês, valores bem diferentes.
                repeat(2 + rng.nextInt(3)) {
                    val d = ym.atDay(1 + rng.nextInt(ym.lengthOfMonth()))
                    if (!d.isAfter(end)) add(Tx(type = TxType.INCOME, amount = lognormal(190000.0, 0.5).roundToLong(), date = d, description = "Trabalho cliente " + (1 + rng.nextInt(40)), categoryId = 11, accountId = 1))
                }
            }
            // Aluguel: conta fixa marcada como paga no vencimento.
            val rentDay = ym.dayClamped(10)
            if (!rentDay.isAfter(end)) add(Tx(type = TxType.EXPENSE, amount = rent.amount, date = rentDay, description = "Aluguel", categoryId = 6, accountId = 1, billId = 1, billYm = ym.key()))
            // Assinaturas e contas que se repetem (sem conta fixa: o motor precisa descobrir).
            val netflix = if (spec.netflixRaiseMonth != null && monthIndex >= spec.netflixRaiseMonth) 4490L else 3990L
            fun sub(day: Int, desc: String, amount: Long, cat: Long, onCard: Boolean) {
                val d = ym.dayClamped(day)
                if (d.isAfter(end)) return
                if (onCard && spec.card) add(Tx(type = TxType.EXPENSE, amount = amount, date = d, description = desc, categoryId = cat, cardId = 1, invoiceYm = CardMath.invoiceFor(card, d).key()))
                else add(Tx(type = TxType.EXPENSE, amount = amount, date = d, description = desc, categoryId = cat, accountId = 1))
            }
            sub(15, "NETFLIX.COM", netflix, 5, true)
            sub(20, "Spotify Premium", 2190, 5, true)
            sub(3, "Academia Corpo Forte", 11990, 4, false)
            sub(12, "Conta de luz Energia SA", lognormal(18000.0, 0.20).roundToLong(), 7, false)
            if (spec.kind == Kind.INDEBTED) {
                val d = ym.dayClamped(15)
                if (!d.isAfter(end)) add(Tx(type = TxType.EXPENSE, amount = 62000, date = d, description = "Parcela · Empréstimo pessoal", categoryId = 10, accountId = 1, debtId = 1))
            }
            // Fatura: paga no vencimento, inteira.
            if (spec.card) {
                val due = CardMath.dueDate(card, ym)
                if (!due.isAfter(end)) {
                    val total = txs.filter { it.cardId == 1L && it.invoiceYm == ym.key() && it.type == TxType.EXPENSE }.sumOf { it.amount }
                    if (total > 0) add(Tx(type = TxType.CARD_PAYMENT, amount = total, date = due, description = "Pagamento da fatura", accountId = 1, cardId = 1, invoiceYm = ym.key()))
                }
            }
            ym = ym.plusMonths(1); monthIndex++
        }
        // Gasto do dia a dia.
        var d = start
        var day = 0
        while (!d.isAfter(end)) {
            val weekend = d.dayOfWeek == DayOfWeek.SATURDAY || d.dayOfWeek == DayOfWeek.SUNDAY
            val lambda = spec.perDay * (if (weekend) 1.5 else 0.85)
            var k = 0
            var p = exp(-lambda); var cum = p; val u = rng.nextDouble()
            while (u > cum && k < 12) { k++; p *= lambda / k; cum += p }
            repeat(k) {
                val (desc, cat, w) = shops[rng.nextInt(shops.size)]
                val amount = lognormal(spec.ticket * w, spec.sigma).roundToLong().coerceAtLeast(150)
                val onCard = spec.card && rng.nextDouble() < 0.55
                add(
                    if (onCard) Tx(type = TxType.EXPENSE, amount = amount, date = d, description = desc, categoryId = cat, cardId = 1, invoiceYm = CardMath.invoiceFor(card, d).key())
                    else Tx(type = TxType.EXPENSE, amount = amount, date = d, description = desc, categoryId = cat, accountId = if (spec.accounts > 1 && rng.nextDouble() < 0.2) 2 else 1),
                )
            }
            if (spec.bigEvery > 0 && rng.nextInt(spec.bigEvery) == 0) {
                add(Tx(type = TxType.EXPENSE, amount = lognormal(spec.ticket * 14, 0.4).roundToLong(), date = d, description = "Loja Eletro Mais", categoryId = 9, accountId = 1))
            }
            d = d.plusDays(1); day++
        }
        val sorted = txs.sortedWith(compareBy<Tx>({ it.date }, { it.id }))
        val goals = listOf(Goal(1, "Viagem", target = 600000, deadline = end.plusMonths(8), color = 0, createdAt = start.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()))
        val goalTxs = (0 until spec.months).mapNotNull { i ->
            val dd = start.ym().plusMonths(i.toLong()).dayClamped(6)
            if (dd.isAfter(end)) null else Tx(type = TxType.GOAL_IN, amount = 30000, date = dd, description = "Viagem", accountId = 1, goalId = 1)
        }
        var gid = id
        val inv = listOf(Investment(1, "Tesouro Selic", color = 0))
        val invTxs = (0 until spec.months).mapNotNull { i ->
            val dd = start.ym().plusMonths(i.toLong()).dayClamped(7)
            if (dd.isAfter(end)) null else Tx(type = TxType.INVEST_IN, amount = 50000, date = dd, description = "Aporte", accountId = 1, investmentId = 1)
        }
        val all = (sorted + goalTxs + invTxs).map { if (it.id == 0L) it.copy(id = gid++) else it }
        val yields = (1 until spec.months).mapNotNull { i ->
            val dd = start.ym().plusMonths(i.toLong()).atDay(1)
            if (dd.isAfter(end)) null else InvestmentYield(i.toLong(), 1, dd, (50000L * i) / 100)
        }
        return Snapshot(
            accounts = accounts, cards = cards, categories = categories, txs = all, investments = inv, yields = yields,
            goals = goals, bills = bills, debts = debts,
        )
    }

    /** Os mesmos dados vistos num dia do passado: só o que aconteceu até [day]. */
    fun asOf(s: Snapshot, day: LocalDate): Snapshot = s.copy(
        txs = s.txs.filter { !it.date.isAfter(day) },
        yields = s.yields.filter { !it.date.isAfter(day) },
    )
}
