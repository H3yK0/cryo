// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo

import io.github.h3yk0.cryo.data.db.Account
import io.github.h3yk0.cryo.data.db.Bill
import io.github.h3yk0.cryo.data.db.CategoryKind
import io.github.h3yk0.cryo.data.db.CreditCard
import io.github.h3yk0.cryo.data.db.Debt
import io.github.h3yk0.cryo.data.db.DebtAdjustment
import io.github.h3yk0.cryo.data.db.Goal
import io.github.h3yk0.cryo.data.db.Investment
import io.github.h3yk0.cryo.data.db.InvestmentYield
import io.github.h3yk0.cryo.data.db.Tx
import io.github.h3yk0.cryo.data.db.TxType
import io.github.h3yk0.cryo.domain.CardMath
import io.github.h3yk0.cryo.domain.Ledger
import io.github.h3yk0.cryo.domain.Snapshot
import io.github.h3yk0.cryo.domain.engine.EngineOptions
import io.github.h3yk0.cryo.domain.engine.EngineReport
import io.github.h3yk0.cryo.domain.engine.EngineRunner
import io.github.h3yk0.cryo.domain.engine.FinanceEngine
import io.github.h3yk0.cryo.domain.engine.Outlook
import io.github.h3yk0.cryo.domain.engine.Projections
import io.github.h3yk0.cryo.domain.engine.Recurrences
import io.github.h3yk0.cryo.domain.engine.Rng
import io.github.h3yk0.cryo.domain.engine.Anomalies
import io.github.h3yk0.cryo.domain.key
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.lang.ref.WeakReference
import java.time.LocalDate
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.roundToLong

/** Históricos inventados e estranhos para os testes de garantia. */
object Synthetic {
    private val merchants = (1..200).map { "Loja ${('A' + it % 26)}${('a' + (it * 7) % 26)}${('a' + (it * 3) % 26)} ${it % 17}" }

    /** [n] movimentações espalhadas em [years] anos até [today], em [accounts] contas, com cartão, contas fixas e dívidas. */
    fun random(n: Int, accounts: Int, years: Int, seed: Long, today: LocalDate): Snapshot {
        val rng = Rng(seed)
        val accs = (1..accounts).map { Account(it.toLong(), "Conta $it", initialBalance = 100_000L * it, color = 0) }
        val card = CreditCard(1, "Cartão", limitAmount = 2_000_000, closingDay = 1 + rng.nextInt(28), dueDay = 1 + rng.nextInt(28), color = 0, payAccountId = 1)
        val days = years * 365
        val txs = ArrayList<Tx>(n + 64)
        for (i in 0 until n) {
            val date = today.minusDays(rng.nextInt(days).toLong())
            val r = rng.nextDouble()
            val amount = (500 + 20_000 * exp(rng.nextGaussian())).roundToLong()
            val t = when {
                r < 0.08 -> Tx(type = TxType.INCOME, amount = amount * 8, date = date, description = merchants[rng.nextInt(10)], categoryId = 8, accountId = 1L + rng.nextInt(accounts))
                r < 0.45 -> Tx(type = TxType.EXPENSE, amount = amount, date = date, description = merchants[rng.nextInt(merchants.size)], categoryId = 1L + rng.nextInt(7), cardId = 1, invoiceYm = CardMath.invoiceFor(card, date).key())
                r < 0.95 -> Tx(type = TxType.EXPENSE, amount = amount, date = date, description = merchants[rng.nextInt(merchants.size)], categoryId = 1L + rng.nextInt(7), accountId = 1L + rng.nextInt(accounts))
                else -> Tx(type = TxType.TRANSFER, amount = amount, date = date, accountId = 1L + rng.nextInt(accounts), toAccountId = 1L + rng.nextInt(accounts))
            }
            txs += t.copy(id = i + 1L)
        }
        // Algumas recorrências de verdade no meio do barulho.
        var id = n + 1L
        for (m in 0 until minOf(years * 12, 120)) {
            val d = today.minusMonths(m.toLong()).withDayOfMonth(5)
            if (!d.isAfter(today)) txs += Tx(id++, TxType.INCOME, 450_000, d, "Salário Empresa", categoryId = 8, accountId = 1)
            val s = today.minusMonths(m.toLong()).withDayOfMonth(15)
            if (!s.isAfter(today)) txs += Tx(id++, TxType.EXPENSE, 3_990, s, "Streaming Plus", categoryId = 5, accountId = 1)
        }
        return Snapshot(
            accounts = accs, cards = listOf(card), categories = Personas.categories, txs = txs,
            bills = listOf(Bill(1, "Aluguel", 120_000, dueDay = 10, accountId = 1, startYm = today.minusYears(years.toLong()).let { it.year * 100 + it.monthValue })),
            debts = listOf(Debt(1, "Empréstimo", color = 0, initialBalance = 500_000, installmentAmount = 30_000, installmentCount = 24, dueDay = 12, startYm = today.year * 100 + today.monthValue)),
            goals = listOf(Goal(1, "Meta", target = 1_000_000, deadline = today.plusYears(1), color = 0)),
            investments = listOf(Investment(1, "Fundo", color = 0)),
        )
    }

    /** Um dos 10 tipos de dados estranhos (estabilidade). */
    fun weird(kind: Int, seed: Long): Pair<Snapshot, LocalDate> {
        val rng = Rng(seed)
        val today = LocalDate.of(2024 + rng.nextInt(6), 1 + rng.nextInt(12), 1).let { it.withDayOfMonth(1 + rng.nextInt(it.lengthOfMonth())) }
        val base = Personas.generate(Personas.spec(Personas.Kind.entries[rng.nextInt(4)]).copy(months = 1 + rng.nextInt(30)), today, seed)
        return when (kind) {
            0 -> Snapshot() to today
            1 -> Snapshot(accounts = listOf(Account(1, "A", color = 0)), txs = listOf(Tx(1, TxType.entries[rng.nextInt(TxType.entries.size)], 1 + rng.nextInt(1_000_000).toLong(), today.minusDays(rng.nextInt(40).toLong()), "x", accountId = 1))) to today
            // Valores enormes (até 100 bilhões).
            2 -> base.copy(txs = base.txs.mapIndexed { i, t -> if (i % 7 == 0) t.copy(amount = 10.0.pow(6 + rng.nextInt(8)).toLong()) else t }) to today
            // Ano bissexto: hoje é 29 de fevereiro e há compras nesse dia.
            3 -> {
                val leap = LocalDate.of(listOf(2024, 2028, 2032)[rng.nextInt(3)], 2, 29)
                val s = Personas.generate(Personas.spec(Personas.Kind.STABLE).copy(months = 14), leap, seed)
                s.copy(txs = s.txs + Tx(9_999_001, TxType.EXPENSE, 12_345, leap, "Bissexto", accountId = 1)) to leap
            }
            // Tudo no vermelho.
            4 -> base.copy(accounts = base.accounts.map { it.copy(initialBalance = -5_000_000) }, txs = base.txs.filter { it.type != TxType.INCOME }) to today
            // Sem nenhuma renda.
            5 -> base.copy(txs = base.txs.filter { it.type != TxType.INCOME }) to today
            // Textos de todo tipo: outras escritas, emoji, inversão de escrita, controle, vazio, enorme.
            6 -> {
                val odd = listOf("", "スターバックス 12/03", "🍕🍕🍕", "‮evil‬", "\u0000\u0001\u0007", "a".repeat(5_000), "مطعم الشام", "बाज़ार", "%s %n %d %1\$s", "   ")
                base.copy(txs = base.txs.mapIndexed { i, t -> t.copy(description = odd[(i + rng.nextInt(10)) % odd.size]) }) to today
            }
            // Tudo no mesmo dia.
            7 -> base.copy(txs = base.txs.map { it.copy(date = today) }) to today
            8 -> base to today
            // Estruturas no limite: fechamento 31, vencimento 1, contas no dia 31, dívida sem parcelas com prazo vencido,
            // tudo arquivado, meta de 1 centavo, investimento com prejuízo e resgate maior que o aporte.
            else -> base.copy(
                cards = listOf(CreditCard(1, "C", limitAmount = 0, closingDay = 31, dueDay = 1, color = 0, payAccountId = 99)),
                bills = base.bills + Bill(7, "Dia 31", 9_999, dueDay = 31, kind = CategoryKind.INCOME, startYm = 202001),
                debts = listOf(Debt(5, "Livre", color = 0, initialBalance = 300_000, deadline = today.minusDays(3), startYm = 202001)),
                debtAdjustments = listOf(DebtAdjustment(1, 5, today.minusDays(10), 9_000)),
                accounts = base.accounts.map { it.copy(archived = true) },
                goals = listOf(Goal(3, "Um centavo", target = 1, color = 0)),
                investments = listOf(Investment(2, "Ações", color = 0)),
                txs = base.txs + listOf(
                    Tx(9_999_101, TxType.INVEST_IN, 100_000, today.minusDays(400), investmentId = 2, accountId = 1),
                    Tx(9_999_102, TxType.INVEST_OUT, 300_000, today.minusDays(100), investmentId = 2, accountId = 1),
                ),
                yields = listOf(InvestmentYield(9, 2, today.minusDays(5), -50_000)),
            ) to today
        }
    }

    /** Confere tudo o que precisa ser verdade em qualquer resultado do motor. Devolve a lista de problemas. */
    fun invariants(r: EngineReport): List<String> {
        val bad = ArrayList<String>()
        fun check(ok: Boolean, what: String) { if (!ok) bad += what }
        val o = r.outlook
        check(o.low <= o.mid && o.mid <= o.high, "faixa do mês fora de ordem")
        check(o.chanceNegative in 0.0..1.0, "chance do mês fora de 0..1")
        check(o.band.all { it.low <= it.mid && it.mid <= it.high }, "faixa diária fora de ordem")
        check(o.band.size <= 32, "faixa do mês grande demais")
        val p = r.projection
        check(p.mid.size == p.horizonDays + 1 && p.low.size == p.mid.size && p.high.size == p.mid.size, "tamanho da projeção")
        check(p.mid.indices.all { p.low[it] <= p.mid[it] && p.mid[it] <= p.high[it] }, "projeção fora de ordem")
        check(p.chanceNegative in 0.0..1.0, "chance da projeção")
        val h = r.health
        check(h.score in 0..100, "nota fora de 0..100")
        check(h.parts.all { it.points.isFinite() && it.points >= 0 && it.points <= it.kind.max + 1e-9 && it.value.isFinite() }, "parte da nota inválida")
        check(r.recurrences.all { it.amount > 0 && it.confidence in 0.0..1.0 && it.next.isAfter(it.last) }, "recorrência inválida")
        check(r.debtRates.all { it.monthly.isFinite() && it.monthly >= 0 }, "juros inválidos")
        check(r.payoff?.let { it.interest >= 0 && it.baselineInterest >= 0 } ?: true, "plano de dívidas inválido")
        check(r.returns.values.all { (it.annual?.isFinite() ?: true) && it.total.isFinite() }, "rendimento inválido")
        check(r.goals.values.all { (it.chanceByDeadline ?: 0.0) in 0.0..1.0 && it.pace >= 0 }, "meta inválida")
        check(r.insights.size <= 5 && r.insights.map { it.topic }.toSet().size == r.insights.size && r.insights.all { it.text.isNotBlank() && it.score.isFinite() }, "percepções inválidas")
        check(r.anomalies.all { it.score.isFinite() }, "fora do padrão inválido")
        return bad
    }

    fun ms(block: () -> Unit): Long {
        val t0 = System.nanoTime()
        block()
        return (System.nanoTime() - t0) / 1_000_000
    }
}
