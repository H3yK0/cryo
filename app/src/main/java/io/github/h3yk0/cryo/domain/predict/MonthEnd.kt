// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.domain.predict

import io.github.h3yk0.cryo.data.db.CategoryKind
import io.github.h3yk0.cryo.data.db.Tx
import io.github.h3yk0.cryo.data.db.TxType
import io.github.h3yk0.cryo.domain.Ledger
import io.github.h3yk0.cryo.domain.engine.TxKey
import io.github.h3yk0.cryo.domain.ym
import java.time.YearMonth
import java.time.temporal.ChronoUnit

/** RF-PRV-10 (reforma de 21/11/2026): as linhas de "Como cheguei nesse número", na ordem da tela. */
enum class EndLine {
    /** Renda fixa: contas fixas de entrada e o que entra todo mês (o salário), as que já entraram e as que ainda vêm. */
    IN_FIXED,
    /** Outras entradas: o que já entrou e não é fixo (uma venda, um estorno). Nada é previsto aqui. */
    IN_OTHER,
    /** Contas fixas de saída: as pagas e as que ainda vencem no mês (inclusive as atrasadas). */
    OUT_BILLS,
    /** Metas e investimentos: o guardado e o aplicado no mês (menos o resgatado) e o que falta guardar para as metas com prazo. */
    OUT_GOALS,
    /** Dívidas e parcelas: as parcelas de dívidas (pagas e a pagar) e as parcelas de compras no cartão do mês. */
    OUT_DEBTS,
    /** Despesas variáveis: o que já saiu e não é fixo (inclusive no cartão). */
    OUT_VARIABLE,
    /** Saídas agendadas para depois de hoje que não são de conta fixa nem de dívida. */
    OUT_SCHEDULED,
}

/** Uma linha: [amount] com sinal (entrada positiva, saída negativa) e, dentro dele, [pending] = o que ainda vem ou falta pagar. */
data class EndItem(val line: EndLine, val amount: Long, val pending: Long = 0L)

/**
 * RF-PRV-10 (pedido do Hanry, 21/11/2026, LA11-1): o fim do mês **real**. [total] = renda − despesas do mês, sempre a soma de
 * [items] (ERR-056), sem estimativa nenhuma (ERR-064: nada de orçamento planejado, de dia a dia previsto, de entrada avulsa
 * prevista, de acerto do passado): só o que já aconteceu e o que é fixo. [income] e [expenses] são as duas barras da tela;
 * [perDay] = quanto da sobra dá para cada dia que falta (contando hoje), 0 quando não sobra.
 */
data class MonthEnd(
    val ym: YearMonth,
    val items: List<EndItem>,
    val total: Long,
    val income: Long,
    val expenses: Long,
    val perDay: Long,
    val remainingDays: Int,
) {
    fun amount(line: EndLine): Long = items.firstOrNull { it.line == line }?.amount ?: 0L
    fun item(line: EndLine): EndItem? = items.firstOrNull { it.line == line }
}

/**
 * O que entra todo mês sem ser conta fixa (o salário, a pensão): a mesma descrição nos 3 meses completos antes deste, sem
 * ligação com conta fixa. [amount] é o valor do meio dos três; [day], o dia de costume.
 */
data class MonthlyIncome(val key: String, val amount: Long, val day: Int)

object MonthEnds {
    /** Quantos meses seguidos o mesmo dinheiro precisa ter entrado para contar como renda fixa. */
    const val FIXED_MONTHS = 3

    fun of(l: Ledger): MonthEnd {
        val today = l.today
        val ym = today.ym()
        val monthEnd = ym.atEndOfMonth()
        val txs = l.monthTxs(ym)
        val monthly = monthlyIncome(l, ym)
        val monthlyKeys = monthly.mapTo(HashSet()) { it.key }

        var inFixed = 0L; var inFixedPending = 0L; var inOther = 0L; var inOtherPending = 0L
        var bills = 0L; var billsPending = 0L
        var goals = 0L; var goalsPending = 0L
        var debts = 0L; var debtsPending = 0L
        var variable = 0L; var scheduled = 0L
        val scheduledDebt = HashMap<Long, Long>()
        val receivedKeys = HashSet<String>()

        for (t in txs) {
            val future = t.date.isAfter(today)
            when (t.type) {
                TxType.INCOME -> {
                    val key = TxKey.of(t.description)
                    val fixed = t.billId != null || key in monthlyKeys
                    if (fixed) {
                        inFixed += t.amount
                        if (future) inFixedPending += t.amount
                        receivedKeys += key
                    } else {
                        // Entrada agendada que não é fixa: a pessoa disse que vem (é o combinado, não um palpite).
                        inOther += t.amount
                        if (future) inOtherPending += t.amount
                    }
                }
                TxType.EXPENSE -> when {
                    t.billId != null && t.billYm != 0 -> { bills += t.amount; if (future) billsPending += t.amount }
                    t.debtId != null -> {
                        debts += t.amount
                        if (future) { debtsPending += t.amount; scheduledDebt.merge(t.debtId, t.amount, Long::plus) }
                    }
                    t.installmentTotal > 1 -> { debts += t.amount; if (future) debtsPending += t.amount }
                    future -> scheduled += t.amount
                    else -> variable += t.amount
                }
                TxType.GOAL_IN, TxType.INVEST_IN -> { goals += t.amount; if (future) goalsPending += t.amount }
                TxType.GOAL_OUT, TxType.INVEST_OUT -> { goals -= t.amount; if (future) goalsPending -= t.amount }
                // Transferência entre contas, pagamento de fatura (as compras já contaram) e empréstimo que entrou: fora.
                TxType.TRANSFER, TxType.CARD_PAYMENT, TxType.DEBT_IN -> Unit
            }
        }

        // Contas fixas ainda sem pagamento neste mês (as puladas já saíram de billStatuses, RF-FIX-12).
        for (s in l.billStatuses(ym)) {
            if (s.payment != null) continue
            if (s.bill.kind == CategoryKind.INCOME) { inFixed += s.bill.amount; inFixedPending += s.bill.amount }
            else { bills += s.bill.amount; billsPending += s.bill.amount }
        }

        // O que entra todo mês e ainda não veio (nem foi agendado com a mesma descrição, ERR-060).
        for (m in monthly) {
            if (m.key in receivedKeys) continue
            inFixed += m.amount; inFixedPending += m.amount
        }

        // Parcelas de dívidas do mês que ainda faltam (o que já está agendado para elas já entrou acima).
        for (d in l.activeDebts) {
            val info = l.debtInfo(d)
            val left = (info.pendingThisMonth - (scheduledDebt[d.id] ?: 0L)).coerceAtLeast(0L)
            if (left > 0) { debts += left; debtsPending += left }
        }

        // Metas com prazo: o que falta guardar neste mês para chegar lá no prazo (o compromisso que a pessoa fez).
        for (g in l.activeGoals) {
            val left = goalLeftThisMonth(l, g, txs)
            if (left > 0) { goals += left; goalsPending += left }
        }

        val items = listOf(
            EndItem(EndLine.IN_FIXED, inFixed, inFixedPending),
            EndItem(EndLine.IN_OTHER, inOther, inOtherPending),
            EndItem(EndLine.OUT_BILLS, -bills, -billsPending),
            EndItem(EndLine.OUT_GOALS, -goals, -goalsPending),
            EndItem(EndLine.OUT_DEBTS, -debts, -debtsPending),
            EndItem(EndLine.OUT_VARIABLE, -variable),
            EndItem(EndLine.OUT_SCHEDULED, -scheduled, -scheduled),
        ).filter { it.amount != 0L }
        val total = items.sumOf { it.amount }
        val income = items.filter { it.amount > 0 }.sumOf { it.amount }
        val expenses = -items.filter { it.amount < 0 }.sumOf { it.amount }
        val remaining = ChronoUnit.DAYS.between(today, monthEnd).toInt().coerceAtLeast(0)
        val perDay = if (total > 0) total / (remaining + 1) else 0L
        return MonthEnd(ym, items, total, income, expenses, perDay, remaining)
    }

    /**
     * O que entra todo mês sem ser conta fixa: a mesma descrição (sem números e datas, [TxKey]) nos [FIXED_MONTHS] meses
     * completos antes de [ym], sem ligação com conta fixa e sem ser o nome de uma conta fixa de entrada (essa já conta pela
     * conta fixa). Uma passada só pelos meses, sem ordenar todas as movimentações.
     */
    fun monthlyIncome(l: Ledger, ym: YearMonth): List<MonthlyIncome> {
        val billKeys = l.s.bills.filter { it.active && it.kind == CategoryKind.INCOME }.mapTo(HashSet()) { TxKey.of(it.name) }
        val perMonth = (1..FIXED_MONTHS).map { k ->
            val m = ym.minusMonths(k.toLong())
            val byKey = HashMap<String, MutableList<Tx>>()
            for (t in l.monthTxs(m)) {
                if (t.type != TxType.INCOME || t.billId != null || t.cardId != null) continue
                val key = TxKey.of(t.description)
                if (key.isBlank() || key in billKeys) continue
                byKey.getOrPut(key) { ArrayList() } += t
            }
            byKey
        }
        val out = ArrayList<MonthlyIncome>()
        for (key in perMonth[0].keys) {
            if (perMonth.any { key !in it }) continue
            // Um valor por mês (a soma do mês, se veio em partes) e o valor do meio dos três: um mês diferente não muda tudo.
            val amounts = perMonth.map { m -> m.getValue(key).sumOf { it.amount } }.sorted()
            val days = perMonth.map { m -> m.getValue(key).minOf { it.date.dayOfMonth } }.sorted()
            out += MonthlyIncome(key, amounts[amounts.size / 2], days[days.size / 2])
        }
        return out.sortedByDescending { it.amount }
    }

    /**
     * Quanto ainda falta guardar neste mês para a meta [g] chegar lá no prazo: o plano do mês (o que faltava no começo do
     * mês ÷ os meses até o prazo, contando este) menos o que já foi guardado nela neste mês. 0 sem prazo, já cumprida ou já
     * guardada a parte do mês.
     */
    fun goalLeftThisMonth(l: Ledger, g: io.github.h3yk0.cryo.data.db.Goal, @Suppress("UNUSED_PARAMETER") monthTxs: List<Tx>): Long {
        val deadline = g.deadline ?: return 0L
        val start = l.today.ym().atDay(1)
        fun net(t: Tx) = when (t.type) {
            TxType.GOAL_IN -> t.amount
            TxType.GOAL_OUT -> -t.amount
            else -> 0L
        }
        // O que já estava guardado no começo do mês e o que entrou nela neste mês (o agendado também: ele já conta acima).
        val history = l.goalHistory(g.id)
        val savedAtStart = history.filter { it.date.isBefore(start) }.sumOf(::net)
        val thisMonth = history.filter { !it.date.isBefore(start) && !it.date.isAfter(l.today.ym().atEndOfMonth()) }.sumOf(::net)
        val missingAtStart = g.target - savedAtStart
        if (missingAtStart <= 0) return 0L
        val months = ChronoUnit.MONTHS.between(l.today.ym(), deadline.ym()).coerceAtLeast(0) + 1
        val plan = (missingAtStart + months - 1) / months
        return (plan - thisMonth).coerceAtLeast(0L)
    }
}
