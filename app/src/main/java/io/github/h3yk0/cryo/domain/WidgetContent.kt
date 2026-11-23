// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.domain

import io.github.h3yk0.cryo.R
import io.github.h3yk0.cryo.data.db.CategoryKind
import io.github.h3yk0.cryo.domain.predict.MonthEnds
import java.time.YearMonth

/** RF-WID-02 (1.7, LA9): o número grande do widget. */
enum class WidgetMain { MONTH_END, BALANCE, GENERAL, NET_WORTH }

/** RF-WID-02: o que pode aparecer embaixo do número. */
enum class WidgetLine { BILLS, BUDGETS, GOALS, ENTRY }

/** RF-WID-02: a escolha de um widget. Sem escolha: o fim do mês, as próximas contas e o botão "Lançar". */
data class WidgetConfig(
    val main: WidgetMain = WidgetMain.MONTH_END,
    val lines: Set<WidgetLine> = setOf(WidgetLine.BILLS, WidgetLine.ENTRY),
) {
    fun encode(): String = main.name + "|" + WidgetLine.entries.filter { it in lines }.joinToString(",") { it.name }

    companion object {
        fun decode(text: String?): WidgetConfig {
            if (text.isNullOrBlank() || '|' !in text) return WidgetConfig()
            val (m, l) = text.split('|', limit = 2)
            val main = WidgetMain.entries.firstOrNull { it.name == m } ?: return WidgetConfig()
            val lines = l.split(',').mapNotNull { n -> WidgetLine.entries.firstOrNull { it.name == n } }.toSet()
            return WidgetConfig(main, lines)
        }
    }
}

/** Uma linha do widget: o nome, um detalhe curto (quando vence) e o valor. */
data class WidgetRow(val name: String, val detail: String, val amount: Long)

/** Uma barra do widget (orçamento ou meta): o nome, quanto já foi (0 a 1, pode passar de 1) e o detalhe. */
data class WidgetBar(val name: String, val ratio: Float, val detail: String, val color: Int)

/** RF-WID-01..05: o que o widget mostra, já com os textos na língua do app. */
data class WidgetContent(
    val mainLabel: String,
    /** null = não há o que mostrar (por exemplo, sem orçamento Geral). */
    val mainValue: Long?,
    /** 1 = bom (verde), -1 = ruim (vermelho), 0 = neutro. */
    val mainSign: Int,
    val mainHint: String?,
    val bills: List<WidgetRow>,
    val budgets: List<WidgetBar>,
    val goals: List<WidgetBar>,
    val config: WidgetConfig,
    /** Ainda não há dados no Cryo. */
    val empty: Boolean,
)

/**
 * RF-WID-03: o conteúdo do widget, com as mesmas contas do app (o fim do mês é o `MonthEnds.of`, RF-PRV-10). Regra pura:
 * mesmos dados, mesmo widget. RF-WID-04: os valores aparecem sempre (pôr o widget na tela é escolher a conveniência).
 */
object WidgetContents {
    /** Quantas linhas cabem em cada lista. */
    const val MAX_ROWS = 3

    fun of(l: Ledger, config: WidgetConfig): WidgetContent {
        val ym = YearMonth.from(l.today)
        val empty = l.s.accounts.isEmpty() && l.s.txs.isEmpty()
        val (label, value, sign, hint) = when (config.main) {
            WidgetMain.MONTH_END -> {
                val end = MonthEnds.of(l)
                Main(
                    Res.s(R.string.month_end), end.total, if (end.total >= 0) 1 else -1,
                    if (end.total > 0 && end.perDay > 0) Res.s(R.string.widget_per_day, Money.format(end.perDay)) else null,
                )
            }
            WidgetMain.BALANCE -> Main(Res.s(R.string.home_balance), l.totalBalance, if (l.totalBalance < 0) -1 else 0, null)
            WidgetMain.GENERAL -> {
                val u = GeneralBudget.usage(l, ym)
                if (u == null) Main(Res.s(R.string.budget_general_title), null, 0, Res.s(R.string.widget_no_general))
                else Main(Res.s(R.string.budget_general_title), u.remaining, if (u.remaining < 0) -1 else 1, Res.s(R.string.widget_left_of, Money.format(u.limit)))
            }
            WidgetMain.NET_WORTH -> Main(Res.s(R.string.home_net_worth), l.netWorth, if (l.netWorth < 0) -1 else 0, null)
        }
        // As próximas contas a pagar: as que faltam deste mês e, se sobrar lugar, as do mês que vem.
        val bills = if (WidgetLine.BILLS !in config.lines) emptyList() else
            (l.billStatuses(ym) + l.billStatuses(ym.plusMonths(1)))
                .filter { it.payment == null && it.bill.kind == CategoryKind.EXPENSE }
                .sortedBy { it.due }
                .take(MAX_ROWS)
                .map { WidgetRow(it.bill.name, Dates.relative(it.due, l.today), it.bill.amount) }
        val budgets = if (WidgetLine.BUDGETS !in config.lines) emptyList() else
            l.budgetUsages(ym).filter { it.limit > 0 }.sortedByDescending { it.ratio }.take(MAX_ROWS)
                .map { WidgetBar(it.category.name, it.ratio, Num.percent((it.ratio * 100).toInt()), it.category.color) }
        val goals = if (WidgetLine.GOALS !in config.lines) emptyList() else
            l.activeGoals.filter { it.target > 0 }.take(MAX_ROWS).map { g ->
                val r = l.goalSaved(g.id).toFloat() / g.target
                WidgetBar(g.name, r, Num.percent((r * 100).toInt()), g.color)
            }
        return WidgetContent(label, value, sign, hint, bills, budgets, goals, config, empty)
    }

    private data class Main(val label: String, val value: Long?, val sign: Int, val hint: String?)
}
