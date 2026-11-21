// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.domain

import io.github.h3yk0.cryo.data.db.TxType
import io.github.h3yk0.cryo.domain.predict.EndLine
import io.github.h3yk0.cryo.domain.predict.MonthEnd
import java.time.YearMonth

/**
 * RF-ORC-10 (pedido do Hanry, LA12-1): o orçamento **Geral**, o quanto se pode gastar da renda no mês. DAD-17: é o orçamento
 * de código [ID] na mesma tabela dos outros (nenhuma categoria tem esse número), então vai e volta no backup sem migração.
 * Os orçamentos das categorias seguem o Geral: [Usage.divided] diz quanto dele já foi dividido entre elas. Regras puras.
 */
object GeneralBudget {
    const val ID = 0L

    /** [limit] do mês, [spent] = todas as saídas do mês, [divided] = a soma dos limites das categorias, [daysLeft] contando hoje. */
    data class Usage(val limit: Long, val spent: Long, val divided: Long, val daysLeft: Int) {
        val remaining: Long get() = limit - spent
        val ratio: Float get() = if (limit <= 0) 0f else spent.toFloat() / limit
        val perDay: Long get() = if (remaining > 0 && daysLeft > 0) remaining / daysLeft else 0L
        /** As categorias somam mais que o Geral. */
        val overDivided: Boolean get() = divided > limit
    }

    /**
     * O que conta no Geral: todas as saídas do mês (contas fixas, dívidas, parcelas e despesas variáveis, inclusive no
     * cartão), como nos orçamentos das categorias (RF-ORC-01). Transferências, pagamento de fatura, investimentos e metas não
     * são saídas (EXPENSE) e ficam fora.
     */
    fun spent(l: Ledger, ym: YearMonth): Long = l.monthTxs(ym).filter { it.type == TxType.EXPENSE }.sumOf { it.amount }

    /** O uso do Geral no mês, ou null se a pessoa não definiu um. */
    fun usage(l: Ledger, ym: YearMonth): Usage? {
        if (l.budgetOf(ID) == null) return null
        val divided = l.s.budgets.filter { it.categoryId != ID && l.category[it.categoryId] != null }.sumOf { l.monthLimit(it.categoryId, ym) }
        val daysLeft = if (ym == l.today.ym()) ym.lengthOfMonth() - l.today.dayOfMonth + 1 else 0
        return Usage(l.monthLimit(ID, ym), spent(l, ym), divided, daysLeft)
    }

    /**
     * RF-ORC-10: a sugestão do Geral = a renda fixa do mês (RF-PRV-10) menos o que vai para as metas e investimentos no mês,
     * arredondada para baixo na unidade da moeda. Null sem renda fixa (a pessoa digita o valor).
     */
    fun suggestion(end: MonthEnd): Long? {
        val fixed = end.amount(EndLine.IN_FIXED)
        if (fixed <= 0) return null
        val goals = -end.amount(EndLine.OUT_GOALS).coerceAtMost(0L)
        return (fixed - goals).coerceAtLeast(0L) / 100 * 100
    }
}
