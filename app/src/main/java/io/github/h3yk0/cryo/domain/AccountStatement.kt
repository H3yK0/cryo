// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.domain

import io.github.h3yk0.cryo.data.db.Tx
import io.github.h3yk0.cryo.data.db.TxType
import java.time.LocalDate
import java.time.YearMonth

/** RF-EXT-09 (1.6): de onde é o que o Extrato mostra quando a pessoa escolhe uma conta ou um cartão. */
sealed interface TxSource {
    data class Account(val id: Long) : TxSource
    data class Card(val id: Long) : TxSource
}

/**
 * RF-CON-08 e RF-EXT-09 (1.6, pedidos de quem usa, LA2 e LA3): o extrato de uma conta, como o do banco, e o filtro por
 * conta ou cartão do Extrato. Regras puras. O saldo segue exatamente a mesma conta de [Ledger.balances] (saldo inicial da
 * conta + o efeito de cada lançamento), para o saldo depois do último lançamento de hoje ser o saldo atual da conta.
 */
object AccountStatement {
    /** Um lançamento do extrato da conta e o saldo da conta logo depois dele (previsto, quando o lançamento é agendado). */
    data class Line(val tx: Tx, val effect: Long, val balanceAfter: Long)

    /** Um dia do extrato: os lançamentos (do mais novo para o mais antigo, como na tela) e o saldo no fim do dia. */
    data class Day(val date: LocalDate, val lines: List<Line>, val endBalance: Long)

    /** Quanto [t] muda o saldo da conta [accountId] (0 = não mexe nela). Igual a [Ledger.balances]. */
    fun effect(t: Tx, accountId: Long): Long {
        var v = 0L
        when (t.type) {
            TxType.EXPENSE, TxType.CARD_PAYMENT, TxType.INVEST_IN, TxType.GOAL_IN -> if (t.accountId == accountId) v -= t.amount
            TxType.INCOME, TxType.INVEST_OUT, TxType.GOAL_OUT, TxType.DEBT_IN -> if (t.accountId == accountId) v += t.amount
            TxType.TRANSFER -> {
                if (t.accountId == accountId) v -= t.amount
                if (t.toAccountId == accountId) v += t.amount
            }
        }
        return v
    }

    /**
     * Quanto [t] mexe no cartão [cardId], do ponto de vista de quem paga: a compra é saída, o estorno e o pagamento da
     * fatura são entrada (diminuem o que se deve).
     */
    fun cardEffect(t: Tx, cardId: Long): Long = when {
        t.cardId != cardId -> 0L
        t.type == TxType.EXPENSE -> -t.amount
        t.type == TxType.INCOME || t.type == TxType.CARD_PAYMENT -> t.amount
        else -> 0L
    }

    /** O lançamento é da conta ou do cartão? Transferência conta para as duas contas. */
    fun touches(t: Tx, source: TxSource): Boolean = when (source) {
        is TxSource.Account -> t.accountId == source.id || t.toAccountId == source.id
        is TxSource.Card -> t.cardId == source.id
    }

    /** O efeito do lançamento no que foi escolhido (conta ou cartão). */
    fun effectOn(t: Tx, source: TxSource): Long = when (source) {
        is TxSource.Account -> effect(t, source.id)
        is TxSource.Card -> cardEffect(t, source.id)
    }

    /** O resumo do topo do Extrato com uma conta ou um cartão escolhido: o que entrou e saiu dela (transferências também). */
    data class Summary(val income: Long, val expense: Long) {
        val result: Long get() = income - expense
    }

    fun summary(txs: List<Tx>, source: TxSource): Summary {
        var inc = 0L; var out = 0L
        for (t in txs) {
            val e = effectOn(t, source)
            if (e > 0) inc += e else out -= e
        }
        return Summary(inc, out)
    }

    /** Ordem do banco: do mais antigo ao mais novo; no mesmo dia, na ordem em que foram lançados. */
    private val bankOrder = compareBy<Tx>({ it.date }, { it.id })

    /** Saldo da conta antes do dia [day] (saldo inicial + tudo o que veio antes). */
    fun balanceBefore(l: Ledger, accountId: Long, day: LocalDate): Long {
        var b = l.account[accountId]?.initialBalance ?: 0L
        for (t in l.s.txs) if (t.date.isBefore(day)) b += effect(t, accountId)
        return b
    }

    /**
     * O extrato da conta no mês [ym]: os dias do mais novo para o mais antigo, cada lançamento com o saldo depois dele, e
     * o saldo no fim de cada dia. Agendados entram com o saldo previsto.
     */
    fun month(l: Ledger, accountId: Long, ym: YearMonth): List<Day> {
        val start = ym.atDay(1)
        var running = balanceBefore(l, accountId, start)
        val lines = ArrayList<Line>()
        for (t in l.s.txs.filter { it.date.year == ym.year && it.date.monthValue == ym.monthValue && touches(it, TxSource.Account(accountId)) }.sortedWith(bankOrder)) {
            val e = effect(t, accountId)
            running += e
            lines += Line(t, e, running)
        }
        return lines.groupBy { it.tx.date }.map { (date, ls) -> Day(date, ls.asReversed(), ls.last().balanceAfter) }
            .sortedByDescending { it.date }
    }
}
