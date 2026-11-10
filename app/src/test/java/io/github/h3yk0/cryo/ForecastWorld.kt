// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo

import io.github.h3yk0.cryo.data.db.Account
import io.github.h3yk0.cryo.data.db.AccountType
import io.github.h3yk0.cryo.data.db.Bill
import io.github.h3yk0.cryo.data.db.CreditCard
import io.github.h3yk0.cryo.data.db.Investment
import io.github.h3yk0.cryo.data.db.InvestmentYield
import io.github.h3yk0.cryo.data.db.Tx
import io.github.h3yk0.cryo.data.db.TxType
import io.github.h3yk0.cryo.domain.CardMath
import io.github.h3yk0.cryo.domain.Snapshot
import io.github.h3yk0.cryo.domain.dayClamped
import io.github.h3yk0.cryo.domain.engine.Rng
import io.github.h3yk0.cryo.domain.key
import io.github.h3yk0.cryo.domain.ym
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.exp
import kotlin.math.roundToLong

/**
 * "Mundos" inventados para medir a previsão (RF-PRV): vidas financeiras com os comportamentos que mais atrapalham quem
 * prevê. Nada vem de extrato de verdade (RNF-PRIV-04); tudo é sorteado com semente, então o mesmo teste gera os mesmos
 * dados. São mais difíceis que os [Personas] da 1.5 de propósito: uma previsão que só acerta no caso fácil não serve.
 */
object ForecastWorld {
    enum class Kind {
        /** Salário fixo, gasto estável. */
        STEADY,
        /** Gasta bem mais nos dias depois do pagamento e aperta no fim do mês. */
        PAYDAY,
        /** O custo de vida sobe um pouco todo mês (inflação, padrão de vida). */
        TREND,
        /** Mudança de vida há alguns meses (mudou de casa, filho): o gasto pulou de patamar. */
        SHIFT,
        /** Dezembro, janeiro e julho bem mais caros (festas, férias). */
        SEASONAL,
        /** Uma compra grande de mercado todo sábado e quase nada nos outros dias. */
        WEEKLY,
        /** Poucas compras, valores muito diferentes (gasto intermitente). */
        LUMPY,
        /** Renda irregular (trabalhos), gasto que acompanha o que entrou. */
        FREELANCER,
        /** Cortou gastos há poucos meses (mudança para baixo). */
        SAVER,
    }

    data class Spec(
        val kind: Kind,
        val months: Int = 26,
        val salary: Long = 520000,
        val salaryDay: Int = 5,
        val ticket: Double = 4200.0,
        val sigma: Double = 0.55,
        val perDay: Double = 1.4,
        val bigEvery: Int = 75,
    )

    fun spec(kind: Kind) = when (kind) {
        Kind.STEADY -> Spec(kind)
        Kind.PAYDAY -> Spec(kind, perDay = 1.5)
        Kind.TREND -> Spec(kind)
        Kind.SHIFT -> Spec(kind)
        Kind.SEASONAL -> Spec(kind)
        Kind.WEEKLY -> Spec(kind, perDay = 0.45, ticket = 3200.0, bigEvery = 120)
        Kind.LUMPY -> Spec(kind, perDay = 0.22, ticket = 16000.0, sigma = 0.9, bigEvery = 45)
        Kind.FREELANCER -> Spec(kind, salary = 0, ticket = 5200.0, sigma = 0.75, perDay = 1.6, bigEvery = 40)
        Kind.SAVER -> Spec(kind, perDay = 1.7)
    }

    private val shops = listOf(
        Triple("Padaria Pão Quente", 1L, 0.35), Triple("Restaurante Sabor Caseiro", 1L, 1.2), Triple("Lanchonete da Esquina", 1L, 0.6),
        Triple("Supermercado Bom Preço", 2L, 2.6), Triple("Mercadinho Vila Nova", 2L, 1.1), Triple("Hortifruti Verde", 2L, 0.8),
        Triple("Uber Viagem", 3L, 0.7), Triple("Posto Avenida", 3L, 2.2), Triple("Cinema Shopping Centro", 4L, 1.3),
        Triple("Bar do Zé", 4L, 1.4), Triple("Livraria Leitura", 9L, 1.6), Triple("Farmácia Saúde Já", 9L, 0.9),
    )

    /** Quanto o gasto do dia a dia muda naquele dia, por causa do comportamento do mundo. */
    private fun intensity(kind: Kind, d: LocalDate, start: LocalDate, end: LocalDate, salaryDay: Int, incomeThisMonth: Double): Double {
        val monthsIn = ChronoUnit.DAYS.between(start, d) / 30.44
        val monthsLeft = ChronoUnit.DAYS.between(d, end) / 30.44
        return when (kind) {
            Kind.PAYDAY -> {
                val since = ((d.dayOfMonth - salaryDay) + 31) % 31
                when { since <= 9 -> 1.65; since <= 18 -> 0.95; else -> 0.55 }
            }
            Kind.TREND -> exp(0.022 * monthsIn)
            Kind.SHIFT -> if (monthsLeft <= 5.5) 1.55 else 1.0
            Kind.SAVER -> if (monthsLeft <= 4.5) 0.62 else 1.0
            Kind.SEASONAL -> when (d.monthValue) { 12 -> 1.7; 1 -> 1.35; 7 -> 1.45; else -> 0.92 }
            Kind.FREELANCER -> (0.55 + 0.45 * incomeThisMonth / 520000.0).coerceIn(0.5, 1.8)
            else -> 1.0
        }
    }

    fun generate(spec: Spec, end: LocalDate, seed: Long): Snapshot {
        val rng = Rng(seed)
        val start = end.ym().minusMonths((spec.months - 1).toLong()).atDay(1)
        val txs = ArrayList<Tx>()
        var id = 1L
        fun add(t: Tx) { txs += t.copy(id = id++) }
        val accounts = listOf(
            Account(1, "Conta principal", AccountType.CHECKING, initialBalance = 400000, color = 0, sortOrder = 1),
            Account(2, "Carteira", AccountType.CASH, initialBalance = 20000, color = 0, sortOrder = 2),
        )
        val card = CreditCard(1, "Cartão Roxo", limitAmount = 900000, closingDay = 3, dueDay = 10, color = 0, payAccountId = 1)
        val rent = Bill(1, "Aluguel", 140000, dueDay = 10, categoryId = 6, accountId = 1, startYm = start.ym().key())
        fun lognormal(mean: Double, sigma: Double) = mean * exp(sigma * rng.nextGaussian() - sigma * sigma / 2)
        fun payDay(ym: java.time.YearMonth, day: Int): LocalDate {
            var d = ym.dayClamped(day)
            while (d.dayOfWeek == DayOfWeek.SATURDAY || d.dayOfWeek == DayOfWeek.SUNDAY) d = d.minusDays(1)
            return d
        }
        val incomeByMonth = HashMap<Int, Double>()
        var ym = start.ym()
        while (!ym.isAfter(end.ym())) {
            var income = 0.0
            if (spec.salary > 0) {
                val d = payDay(ym, spec.salaryDay)
                if (!d.isAfter(end)) add(Tx(type = TxType.INCOME, amount = spec.salary, date = d, description = "Salário ACME Ltda", categoryId = 8, accountId = 1))
                income = spec.salary.toDouble()
            } else {
                repeat(2 + rng.nextInt(3)) {
                    val d = ym.atDay(1 + rng.nextInt(ym.lengthOfMonth()))
                    val v = lognormal(190000.0, 0.5).roundToLong()
                    income += v
                    if (!d.isAfter(end)) add(Tx(type = TxType.INCOME, amount = v, date = d, description = "Trabalho cliente " + (1 + rng.nextInt(40)), categoryId = 11, accountId = 1))
                }
            }
            incomeByMonth[ym.key()] = income
            val rentDay = ym.dayClamped(10)
            if (!rentDay.isAfter(end)) add(Tx(type = TxType.EXPENSE, amount = rent.amount, date = rentDay, description = "Aluguel", categoryId = 6, accountId = 1, billId = 1, billYm = ym.key()))
            fun sub(day: Int, desc: String, amount: Long, cat: Long, onCard: Boolean) {
                val d = ym.dayClamped(day)
                if (d.isAfter(end)) return
                if (onCard) add(Tx(type = TxType.EXPENSE, amount = amount, date = d, description = desc, categoryId = cat, cardId = 1, invoiceYm = CardMath.invoiceFor(card, d).key()))
                else add(Tx(type = TxType.EXPENSE, amount = amount, date = d, description = desc, categoryId = cat, accountId = 1))
            }
            sub(15, "NETFLIX.COM", 3990, 5, true)
            sub(20, "Spotify Premium", 2190, 5, true)
            sub(3, "Academia Corpo Forte", 11990, 4, false)
            sub(12, "Conta de luz Energia SA", lognormal(18000.0, 0.20).roundToLong(), 7, false)
            val due = CardMath.dueDate(card, ym)
            if (!due.isAfter(end)) {
                val total = txs.filter { it.cardId == 1L && it.invoiceYm == ym.key() && it.type == TxType.EXPENSE }.sumOf { it.amount }
                if (total > 0) add(Tx(type = TxType.CARD_PAYMENT, amount = total, date = due, description = "Pagamento da fatura", accountId = 1, cardId = 1, invoiceYm = ym.key()))
            }
            ym = ym.plusMonths(1)
        }
        var d = start
        while (!d.isAfter(end)) {
            val weekend = d.dayOfWeek == DayOfWeek.SATURDAY || d.dayOfWeek == DayOfWeek.SUNDAY
            val k0 = intensity(spec.kind, d, start, end, spec.salaryDay, incomeByMonth[d.ym().key()] ?: 0.0)
            val lambda = spec.perDay * (if (weekend) 1.5 else 0.85) * k0
            var k = 0
            var p = exp(-lambda); var cum = p; val u = rng.nextDouble()
            while (u > cum && k < 14) { k++; p *= lambda / k; cum += p }
            repeat(k) {
                val (desc, cat, w) = shops[rng.nextInt(shops.size)]
                val amount = lognormal(spec.ticket * w, spec.sigma).roundToLong().coerceAtLeast(150)
                val onCard = rng.nextDouble() < 0.55
                add(
                    if (onCard) Tx(type = TxType.EXPENSE, amount = amount, date = d, description = desc, categoryId = cat, cardId = 1, invoiceYm = CardMath.invoiceFor(card, d).key())
                    else Tx(type = TxType.EXPENSE, amount = amount, date = d, description = desc, categoryId = cat, accountId = if (rng.nextDouble() < 0.2) 2 else 1),
                )
            }
            if (spec.kind == Kind.WEEKLY && d.dayOfWeek == DayOfWeek.SATURDAY) {
                add(Tx(type = TxType.EXPENSE, amount = lognormal(42000.0, 0.18).roundToLong(), date = d, description = "Supermercado Bom Preço", categoryId = 2, accountId = 1))
            }
            if (spec.bigEvery > 0 && rng.nextInt(spec.bigEvery) == 0) {
                add(Tx(type = TxType.EXPENSE, amount = lognormal(spec.ticket * 14, 0.4).roundToLong(), date = d, description = "Loja Eletro Mais", categoryId = 9, accountId = 1))
            }
            d = d.plusDays(1)
        }
        // Um investimento com aportes mensais e rendimento com sobe e desce (para a previsão de investimentos).
        val inv = Investment(1, "Fundo Horizonte", color = 0)
        var gid = id
        val invTxs = (0 until spec.months).mapNotNull { i ->
            val dd = start.ym().plusMonths(i.toLong()).dayClamped(7)
            if (dd.isAfter(end)) null else Tx(id = gid++, type = TxType.INVEST_IN, amount = 50000, date = dd, description = "Aporte", accountId = 1, investmentId = 1)
        }
        var value = 0.0
        val yields = (1 until spec.months).mapNotNull { i ->
            val dd = start.ym().plusMonths(i.toLong()).atDay(1)
            value += 50000.0
            val r = 0.008 + 0.02 * rng.nextGaussian()
            val y = (value * r).roundToLong()
            value += y
            if (dd.isAfter(end)) null else InvestmentYield(i.toLong(), 1, dd, y)
        }
        return Snapshot(
            accounts = accounts, cards = listOf(card), categories = Personas.categories,
            txs = txs.sortedWith(compareBy<Tx>({ it.date }, { it.id })) + invTxs, investments = listOf(inv), yields = yields, bills = listOf(rent),
        )
    }
}
