// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.domain

import io.github.h3yk0.cryo.R
import io.github.h3yk0.cryo.data.db.CategoryKind
import io.github.h3yk0.cryo.domain.engine.EngineReport
import io.github.h3yk0.cryo.domain.predict.MonthEnd
import java.time.temporal.ChronoUnit

/** RF-INI-19: a prioridade de um aviso (a cor dele na tela, DS-TOM). */
enum class InboxLevel { ALERT, ATTENTION, INFO }

/** Para onde o aviso leva (a tela decide a rota). */
sealed interface InboxTarget {
    data object Bills : InboxTarget
    data object Budgets : InboxTarget
    data object Insights : InboxTarget
    data object Recurring : InboxTarget
    data object Goals : InboxTarget
    data class Category(val id: Long) : InboxTarget
    data class Debt(val id: Long) : InboxTarget
    data class Card(val id: Long) : InboxTarget
    data class Tx(val id: Long) : InboxTarget
}

/** O tipo do aviso (o ícone dele na tela). */
enum class InboxKind { BILL, DEBT, INVOICE, BUDGET, MONTH, INSIGHT, ANOMALY, RECURRING }

/**
 * Um aviso da central. [key] é estável enquanto o aviso é o mesmo (a conta, o mês e o estado), sem texto da pessoa: só o
 * código HMAC dela fica guardado como "visto" (RF-SEG-02). [order] desempata dentro do nível (o mais urgente primeiro).
 */
data class InboxItem(
    val key: String,
    val level: InboxLevel,
    val kind: InboxKind,
    val text: String,
    val target: InboxTarget?,
    val positive: Boolean? = null,
    val order: Long = 0,
)

/**
 * RF-INI-19 e RF-INI-20 (pedido do Hanry, LA9): a central de avisos e o momento oportuno. Regras puras: o que entra, em que
 * grupo, em que ordem, quantos são novos e qual vira o destaque do Início.
 */
object Inbox {
    /** Contas e parcelas que vencem em até tantos dias entram em "Nos próximos dias". */
    const val SOON_DAYS = 3L

    /** Fatura fechada que vence em até tantos dias. */
    const val INVOICE_DAYS = 5L

    /** Quantas percepções do motor entram (as de maior nota, RF-MOT-11). */
    const val MAX_INSIGHTS = 5

    fun items(l: Ledger, report: EngineReport?, monthEnd: MonthEnd?, hide: Boolean): List<InboxItem> {
        val out = ArrayList<InboxItem>()
        val today = l.today
        val ym = today.ym()
        fun money(v: Long) = if (hide) Money.HIDDEN else Money.format(v)
        fun days(d: java.time.LocalDate) = ChronoUnit.DAYS.between(today, d)

        for (s in l.billStatuses(ym)) {
            if (s.bill.kind != CategoryKind.EXPENSE) continue
            when (s.state) {
                BillState.OVERDUE -> out += InboxItem(
                    "bill:${s.bill.id}:${ym.key()}:late", InboxLevel.ALERT, InboxKind.BILL,
                    Res.s(R.string.alert_bill_overdue, s.bill.name, Dates.relative(s.due, today), money(s.bill.amount)), InboxTarget.Bills, order = days(s.due),
                )
                BillState.DUE_SOON -> if (days(s.due) <= SOON_DAYS) out += InboxItem(
                    "bill:${s.bill.id}:${ym.key()}:soon", InboxLevel.ATTENTION, InboxKind.BILL,
                    Res.s(R.string.alert_bill_due, s.bill.name, Dates.relative(s.due, today), money(s.bill.amount)), InboxTarget.Bills, order = days(s.due),
                )
                else -> Unit
            }
        }
        for (i in l.debtOverview.open) {
            val due = i.nextDue ?: continue
            when (i.state) {
                DebtState.OVERDUE -> out += InboxItem(
                    "debt:${i.debt.id}:$due:late", InboxLevel.ALERT, InboxKind.DEBT,
                    if (i.hasInstallments) Res.s(R.string.alert_debt_overdue, i.debt.name, Dates.short(due), money(i.nextAmount))
                    else Res.s(R.string.alert_debt_deadline, i.debt.name, Dates.short(due)),
                    InboxTarget.Debt(i.debt.id), order = days(due),
                )
                DebtState.DUE_SOON -> out += InboxItem(
                    "debt:${i.debt.id}:$due:soon", InboxLevel.ATTENTION, InboxKind.DEBT,
                    Res.s(R.string.alert_debt_due, i.debt.name, Dates.relative(due, today), money(i.nextAmount)), InboxTarget.Debt(i.debt.id), order = days(due),
                )
                else -> Unit
            }
        }
        for (card in l.activeCards) {
            val inv = l.focusInvoice(card)
            when (inv.status(today)) {
                InvoiceStatus.OVERDUE -> out += InboxItem(
                    "inv:${card.id}:${inv.ym.key()}:late", InboxLevel.ALERT, InboxKind.INVOICE,
                    Res.s(R.string.alert_invoice_overdue, card.name, money(inv.remaining)), InboxTarget.Card(card.id), order = days(inv.due),
                )
                InvoiceStatus.CLOSED -> if (days(inv.due) <= INVOICE_DAYS) out += InboxItem(
                    "inv:${card.id}:${inv.ym.key()}:soon", InboxLevel.ATTENTION, InboxKind.INVOICE,
                    Res.s(R.string.alert_invoice_due, card.name, Dates.relative(inv.due, today), money(inv.remaining)), InboxTarget.Card(card.id), order = days(inv.due),
                )
                else -> Unit
            }
        }
        for (b in l.budgetUsages(ym)) {
            if (b.ratio >= 1f) out += InboxItem(
                "budget:${b.category.id}:${ym.key()}:over", InboxLevel.ALERT, InboxKind.BUDGET,
                Res.s(R.string.alert_budget_over, b.category.name), InboxTarget.Budgets, order = -(b.ratio * 100).toLong(),
            ) else if (b.ratio >= 0.8f) out += InboxItem(
                "budget:${b.category.id}:${ym.key()}:near", InboxLevel.ATTENTION, InboxKind.BUDGET,
                Res.s(R.string.alert_budget_near, b.category.name, Num.percent((b.ratio * 100).toInt())), InboxTarget.Budgets, order = -(b.ratio * 100).toLong(),
            )
        }
        // RF-PRV-10: pelo que já é certo, o mês fecha negativo (o número real, não um cenário).
        if (monthEnd != null && monthEnd.total < 0) out += InboxItem(
            "month:${ym.key()}:neg", InboxLevel.ALERT, InboxKind.MONTH,
            Res.s(R.string.inbox_month_negative, money(-monthEnd.total)), InboxTarget.Insights, order = monthEnd.total,
        )

        if (report != null) {
            // RF-MOT-11: as percepções de maior nota; o que saiu do padrão (RF-MOT-05) já vem entre elas. Ficam fora a faixa
            // do mês (cenários: o fim do mês da tela é o real, RF-PRV-10) e o que já é aviso aqui (conta vencida, orçamento
            // estourado), para a central não dizer a mesma coisa duas vezes.
            report.insights.filter { it.topic != "month" && !it.topic.startsWith("alert:") }.take(MAX_INSIGHTS).forEachIndexed { i, r ->
                val (kind, target) = insightTarget(r.topic)
                out += InboxItem("ins:${r.topic}:${ym.key()}", InboxLevel.INFO, kind, r.text, target, r.positive, order = i.toLong())
            }
            // RF-MOT-02: "O que se repete" sempre tem a sua porta de entrada (a tela saiu de Análises).
            if (report.recurrences.isNotEmpty() && out.none { it.target == InboxTarget.Recurring }) out += InboxItem(
                "rec:${ym.key()}", InboxLevel.INFO, InboxKind.RECURRING, Res.s(R.string.inbox_recurring), InboxTarget.Recurring, order = 200L,
            )
        }
        return out.sortedWith(compareBy<InboxItem>({ it.level.ordinal }, { it.order }))
    }

    /** O ícone e a tela de cada percepção, pelo assunto dela (RF-MOT-11). */
    private fun insightTarget(topic: String): Pair<InboxKind, InboxTarget?> = when {
        topic.startsWith("dup:") || topic.startsWith("out:") -> InboxKind.ANOMALY to topic.substringAfter(':').toLongOrNull()?.let { InboxTarget.Tx(it) }
        topic.startsWith("cat:") -> InboxKind.ANOMALY to topic.substringAfter(':').toLongOrNull()?.let { InboxTarget.Category(it) }
        topic == "subs" || topic.startsWith("rec:") -> InboxKind.RECURRING to InboxTarget.Recurring
        topic.startsWith("goal:") -> InboxKind.INSIGHT to InboxTarget.Goals
        topic == "future" -> InboxKind.INSIGHT to InboxTarget.Insights
        else -> InboxKind.INSIGHT to null
    }

    /** Os avisos de um nível, na ordem da tela. */
    fun group(items: List<InboxItem>, level: InboxLevel): List<InboxItem> = items.filter { it.level == level }

    /** RF-INI-19: o número do sino = os avisos importantes (atenção e alerta) ainda não vistos. */
    fun unseenImportant(items: List<InboxItem>, seen: (String) -> Boolean): Int =
        items.count { it.level != InboxLevel.INFO && !seen(it.key) }

    /** RF-INI-19: há percepção nova (o ponto no sino, sem número)? */
    fun hasNewInsight(items: List<InboxItem>, seen: (String) -> Boolean): Boolean =
        items.any { it.level == InboxLevel.INFO && !seen(it.key) }

    /**
     * RF-INI-20: o destaque do Início, o aviso mais importante ainda não visto (atenção ou alerta); null = nada aparece. O fim
     * do mês negativo não vira destaque: o cartão do mês, logo abaixo, já mostra o número na cor do resultado.
     */
    fun highlight(items: List<InboxItem>, seen: (String) -> Boolean): InboxItem? =
        items.firstOrNull { it.level != InboxLevel.INFO && it.kind != InboxKind.MONTH && !seen(it.key) }
}
