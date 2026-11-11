// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.notify

import io.github.h3yk0.cryo.R
import io.github.h3yk0.cryo.data.db.CategoryKind
import io.github.h3yk0.cryo.data.db.TxType
import io.github.h3yk0.cryo.domain.CardMath
import io.github.h3yk0.cryo.domain.Dates
import io.github.h3yk0.cryo.domain.Ledger
import io.github.h3yk0.cryo.domain.Money
import io.github.h3yk0.cryo.domain.Num
import io.github.h3yk0.cryo.domain.Res
import io.github.h3yk0.cryo.domain.engine.AnomalyKind
import io.github.h3yk0.cryo.domain.engine.EngineReport
import io.github.h3yk0.cryo.domain.engine.RecurrencePeriod
import io.github.h3yk0.cryo.domain.engine.RecurrenceStatus
import io.github.h3yk0.cryo.domain.key
import io.github.h3yk0.cryo.domain.ym
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * RF-AVI-01: cada situação do dinheiro que merece um aviso. [urgent] = contas e dívidas: sempre avisam (no dia certo) e não
 * contam no limite de um aviso comum por dia (RF-AVI-04). [importance] decide qual aviso comum sai quando há vários.
 */
enum class AlertKind(val group: AlertGroup, val urgent: Boolean = false, val importance: Double = 0.5) {
    BILL_DUE(AlertGroup.BILLS, urgent = true),
    DEBT_DUE(AlertGroup.BILLS, urgent = true),
    BUDGET_OVER(AlertGroup.BUDGET, importance = 0.95),
    NEGATIVE(AlertGroup.WATCH, importance = 0.9),
    BUDGET_PACE(AlertGroup.BUDGET, importance = 0.85),
    SALARY_LATE(AlertGroup.RECURRING, importance = 0.8),
    DUPLICATE(AlertGroup.WATCH, importance = 0.8),
    INVOICE_CLOSES(AlertGroup.CARDS, importance = 0.75),
    SALARY_DAY(AlertGroup.RECURRING, importance = 0.74),
    BUDGET_80(AlertGroup.BUDGET, importance = 0.7),
    PRICE_UP(AlertGroup.RECURRING, importance = 0.68),
    OUTLIER(AlertGroup.WATCH, importance = 0.65),
    MONTH_CLOSE(AlertGroup.SUMMARIES, importance = 0.6),
    LAST_INSTALLMENT(AlertGroup.GOALS, importance = 0.55),
    GOAL_NEAR(AlertGroup.GOALS, importance = 0.54),
    HEALTH_UP(AlertGroup.GOALS, importance = 0.5),
    WEEK(AlertGroup.SUMMARIES),
    DAILY(AlertGroup.DAILY),
}

/** RF-AVI-03: o que dá para fazer na própria notificação. */
enum class AlertAction { PAID, SNOOZE, NO_SPEND, LOG, REGISTER, OPEN }

/** Um aviso pronto para ir para a notificação (o texto já na língua e no tom da pessoa). */
data class Alert(
    val kind: AlertKind,
    val key: String,
    val title: String,
    val text: String,
    val actions: List<AlertAction> = emptyList(),
    /** Para onde o toque leva (o mesmo "open" dos lembretes: bills, debts, budgets, goals, insights, wallet). */
    val open: String = "home",
    val billId: Long? = null,
    val ym: Int? = null,
    val amount: Long? = null,
    val accountId: Long? = null,
    val categoryId: Long? = null,
    val label: String? = null,
    /** Os 7 dias da semana para o gráfico do resumo (RF-AVI-07). */
    val week: List<Long>? = null,
    val weekNormal: Long? = null,
)

/** Os horários em que o Cryo fala (RF-AVI-04: só dois; nada de aviso a qualquer hora). */
enum class AlertSlot { MORNING, EVENING }

/**
 * RF-AVI-01/02/04: monta os avisos de um horário. Puro Kotlin (sem Android): os testes conferem cada situação e cada regra
 * com dados inventados e um relógio fixo.
 */
class AlertPlanner(private val l: Ledger, private val report: EngineReport?, private val state: AlertState) {
    private val today = l.today

    fun plan(slot: AlertSlot): List<Alert> = when (slot) {
        AlertSlot.MORNING -> urgent() + listOfNotNull(bestCommon())
        AlertSlot.EVENING -> listOfNotNull(evening())
    }

    /* ------------------------------------------- regras ------------------------------------------- */

    private fun tone() = state.tone

    /** O texto da vez: no tom Amigo, um dos jeitos de dizer, sem repetir o último; no Discreto, o curto. */
    private fun say(kind: AlertKind, friendly: List<Int>, discreet: Int, vararg args: Any?): String {
        val id = if (tone() == AlertTone.DISCREET || friendly.isEmpty()) discreet else friendly[state.nextVariant(kind, friendly.size)]
        return Res.s(id, *args)
    }

    /** Pessoa nova: nos primeiros dias o app só lembra contas (nada de opinar sem conhecer os gastos). */
    private val settled: Boolean by lazy { l.s.txs.any { ChronoUnit.DAYS.between(it.date, today) >= 7 } }

    private fun cooldownOk(kind: AlertKind, days: Long): Boolean {
        val last = state.lastOfKind(kind) ?: return true
        val wait = if (state.ignored(kind) >= 3) days * 2 else days
        return ChronoUnit.DAYS.between(last, today) >= wait
    }

    private fun allowed(a: Alert): Boolean {
        if (!state.enabled(a.kind.group) || state.shown(a.key)) return false
        return when (a.kind) {
            AlertKind.OUTLIER, AlertKind.PRICE_UP, AlertKind.DUPLICATE, AlertKind.NEGATIVE, AlertKind.HEALTH_UP, AlertKind.GOAL_NEAR ->
                cooldownOk(a.kind, 7)
            else -> if (state.ignored(a.kind) >= 3) cooldownOk(a.kind, 2) else true
        }
    }

    /* ------------------------------------------- contas ------------------------------------------- */

    private fun urgent(): List<Alert> {
        if (!state.enabled(AlertGroup.BILLS)) return emptyList()
        val out = ArrayList<Alert>()
        for (s in dueReminders(l)) {
            val key = "bill:${s.bill.id}:${s.ym.key()}:${s.daysUntil}"
            if (state.shown(key) || state.snoozed("bill:${s.bill.id}:${s.ym.key()}", today)) continue
            out += Alert(
                AlertKind.BILL_DUE, key, "${reminderTitle(s, today)} · ${Money.format(s.bill.amount)}",
                say(AlertKind.BILL_DUE, listOf(R.string.alert_bill_a1, R.string.alert_bill_a2), R.string.alert_bill_d),
                actions = listOf(AlertAction.PAID, AlertAction.SNOOZE), open = "bills", billId = s.bill.id, ym = s.ym.key(),
                amount = s.bill.amount, accountId = s.bill.accountId,
            )
        }
        for (r in debtReminders(l)) {
            val key = "debt:${r.info.debt.id}:${r.due}:${r.daysUntil}"
            if (state.shown(key)) continue
            out += Alert(AlertKind.DEBT_DUE, key, debtReminderTitle(r, today), debtReminderText(r), open = "debts")
        }
        return out
    }

    /* ---------------------------------------- avisos comuns ---------------------------------------- */

    private fun bestCommon(): Alert? {
        if (!settled || state.commonShownOn(today)) return null
        return candidates().filter(::allowed).maxByOrNull { it.kind.importance }
    }

    /** Todas as situações de hoje (as regras escolhem no máximo uma). */
    fun candidates(): List<Alert> = buildList {
        addAll(budget())
        addAll(invoices())
        report?.let { r ->
            addAll(recurring(r))
            addAll(watch(r))
            health(r)?.let(::add)
        }
        addAll(goals())
        addAll(lastInstallments())
        monthClose()?.let(::add)
    }

    private fun budget(): List<Alert> = buildList {
        val ym = today.ym()
        val left = ym.lengthOfMonth() - today.dayOfMonth
        for (u in l.budgetUsages(ym)) {
            if (u.limit <= 0) continue
            val name = u.category.name
            when {
                u.spent >= u.limit -> add(
                    Alert(
                        AlertKind.BUDGET_OVER, "budget:${u.category.id}:${ym.key()}:100", Res.s(R.string.alert_over_title, name),
                        say(AlertKind.BUDGET_OVER, listOf(R.string.alert_over_a1, R.string.alert_over_a2), R.string.alert_over_d, name, Money.format(u.spent), Money.format(u.limit)),
                        listOf(AlertAction.OPEN), open = "budgets", categoryId = u.category.id,
                    ),
                )
                u.spent * 10 >= u.limit * 8 -> add(
                    Alert(
                        AlertKind.BUDGET_80, "budget:${u.category.id}:${ym.key()}:80",
                        Res.s(R.string.alert_80_title, name, Num.percent((u.spent * 100 / u.limit).toInt())),
                        say(AlertKind.BUDGET_80, listOf(R.string.alert_80_a1, R.string.alert_80_a2), R.string.alert_80_d, today.dayOfMonth, Money.format(u.limit - u.spent), left),
                        listOf(AlertAction.OPEN), open = "budgets", categoryId = u.category.id,
                    ),
                )
            }
        }
        for (p in report?.pace.orEmpty()) {
            if (p.crossDay <= today.dayOfMonth) continue
            val name = l.category[p.categoryId]?.name ?: continue
            add(
                Alert(
                    AlertKind.BUDGET_PACE, "pace:${p.categoryId}:${ym.key()}", Res.s(R.string.alert_pace_title, name),
                    say(AlertKind.BUDGET_PACE, listOf(R.string.alert_pace_a1, R.string.alert_pace_a2), R.string.alert_pace_d, name, p.crossDay),
                    listOf(AlertAction.OPEN), open = "budgets", categoryId = p.categoryId,
                ),
            )
        }
    }

    /** A fatura fecha amanhã: compra a partir de amanhã só vence no mês seguinte (o "melhor dia de compra"). */
    private fun invoices(): List<Alert> = l.s.cards.filter { !it.archived }.mapNotNull { card ->
        val tomorrow = today.plusDays(1)
        val closes = CardMath.invoiceFor(card, tomorrow) != CardMath.invoiceFor(card, today)
        val used = l.s.txs.any { it.cardId == card.id && ChronoUnit.DAYS.between(it.date, today) in 0..60 }
        if (!closes || !used) return@mapNotNull null
        val next = Dates.monthInSentence(CardMath.invoiceFor(card, tomorrow))
        Alert(
            AlertKind.INVOICE_CLOSES, "invoice:${card.id}:${tomorrow}", Res.s(R.string.alert_invoice_title, card.name),
            say(AlertKind.INVOICE_CLOSES, listOf(R.string.alert_invoice_a1, R.string.alert_invoice_a2), R.string.alert_invoice_d, next),
            open = "wallet",
        )
    }

    private fun recurring(r: EngineReport): List<Alert> = buildList {
        val ym = today.ym()
        for (rec in r.recurrences) {
            if (rec.type != TxType.INCOME || rec.period != RecurrencePeriod.MONTHLY || rec.confidence < 0.6) continue
            val receivedThisMonth = rec.last.ym() == ym
            when {
                rec.status == RecurrenceStatus.LATE && !receivedThisMonth -> add(
                    Alert(
                        AlertKind.SALARY_LATE, "late:${rec.key}:${ym.key()}", Res.s(R.string.alert_late_title, rec.label),
                        say(AlertKind.SALARY_LATE, listOf(R.string.alert_late_a1, R.string.alert_late_a2), R.string.alert_late_d, rec.anchorDay),
                        listOf(AlertAction.REGISTER), open = "home", amount = rec.amount, accountId = rec.accountId, categoryId = rec.categoryId, label = rec.label,
                    ),
                )
                rec.status == RecurrenceStatus.ACTIVE && !receivedThisMonth && rec.next == today -> add(
                    Alert(
                        AlertKind.SALARY_DAY, "salary:${rec.key}:${ym.key()}", Res.s(R.string.alert_salary_title, rec.label),
                        say(AlertKind.SALARY_DAY, listOf(R.string.alert_salary_a1, R.string.alert_salary_a2), R.string.alert_salary_d, Money.format(rec.amount)),
                        listOf(AlertAction.REGISTER), open = "home", amount = rec.amount, accountId = rec.accountId, categoryId = rec.categoryId, label = rec.label,
                    ),
                )
            }
        }
    }

    private fun watch(r: EngineReport): List<Alert> = buildList {
        for (a in r.anomalies) {
            if (ChronoUnit.DAYS.between(a.date, today) !in 0..3) continue
            when (a.kind) {
                AnomalyKind.PRICE_UP -> add(
                    Alert(
                        AlertKind.PRICE_UP, "price:${a.txId ?: a.label}:${a.date}", Res.s(R.string.alert_price_title, a.label),
                        say(AlertKind.PRICE_UP, listOf(R.string.alert_price_a1, R.string.alert_price_a2), R.string.alert_price_d, Money.format(abs(a.amount - a.usual))),
                        open = "insights",
                    ),
                )
                AnomalyKind.DUPLICATE -> add(
                    Alert(
                        AlertKind.DUPLICATE, "dup:${a.txId}:${a.otherTxId}", Res.s(R.string.alert_dup_title, a.label),
                        say(AlertKind.DUPLICATE, listOf(R.string.alert_dup_a1, R.string.alert_dup_a2), R.string.alert_dup_d, Money.format(a.amount)),
                        open = "insights",
                    ),
                )
                AnomalyKind.OUTLIER -> add(
                    Alert(
                        AlertKind.OUTLIER, "out:${a.txId ?: a.label}:${a.date}", Res.s(R.string.alert_out_title, a.label),
                        say(AlertKind.OUTLIER, listOf(R.string.alert_out_a1, R.string.alert_out_a2), R.string.alert_out_d, Money.format(a.amount), Money.format(a.usual)),
                        open = "insights",
                    ),
                )
                AnomalyKind.CATEGORY_HIGH -> Unit
            }
        }
        // Saldo que vai ficar negativo nos próximos 30 dias (a projeção do motor).
        val p = r.projection
        val days = minOf(30, p.horizonDays - 1)
        val firstNeg = (0..days).firstOrNull { p.mid.getOrNull(it)?.let { v -> v < 0 } == true }
        if (firstNeg != null && p.startBalance >= 0) {
            val d = p.dateAt(firstNeg)
            add(
                Alert(
                    AlertKind.NEGATIVE, "neg:${today.ym().key()}:${d.ym().key()}", Res.s(R.string.alert_neg_title),
                    say(AlertKind.NEGATIVE, listOf(R.string.alert_neg_a1, R.string.alert_neg_a2), R.string.alert_neg_d, Dates.dayMonth(d)),
                    open = "insights",
                ),
            )
        }
    }

    private fun health(r: EngineReport): Alert? {
        val h = r.health
        if (!h.ready) return null
        val before = state.lastHealth
        if (before < 0) { state.lastHealth = h.score; return null }
        if (h.score < before + 5) return null
        return Alert(
            AlertKind.HEALTH_UP, "health:${h.score}:${today.ym().key()}", Res.s(R.string.alert_health_title),
            say(AlertKind.HEALTH_UP, listOf(R.string.alert_health_a1, R.string.alert_health_a2), R.string.alert_health_d, h.score, before, h.score - before),
            open = "insights",
        )
    }

    private fun goals(): List<Alert> = l.activeGoals.mapNotNull { g ->
        val saved = l.goalSaved(g.id)
        if (g.target <= 0 || saved * 10 < g.target * 9 || saved >= g.target) return@mapNotNull null
        Alert(
            AlertKind.GOAL_NEAR, "goal:${g.id}:90", Res.s(R.string.alert_goal_title, g.name),
            say(AlertKind.GOAL_NEAR, listOf(R.string.alert_goal_a1, R.string.alert_goal_a2), R.string.alert_goal_d, Money.format(g.target - saved)),
            open = "goals",
        )
    }

    private fun lastInstallments(): List<Alert> = l.s.txs.filter {
        it.installmentTotal > 1 && it.installmentNumber == it.installmentTotal && it.installmentGroup != null &&
            (it.invoiceYm == today.ym().key() || (it.cardId == null && it.date.ym() == today.ym()))
    }.map { t ->
        Alert(
            AlertKind.LAST_INSTALLMENT, "last:${t.installmentGroup}", Res.s(R.string.alert_last_title, t.description),
            say(AlertKind.LAST_INSTALLMENT, listOf(R.string.alert_last_a1, R.string.alert_last_a2), R.string.alert_last_d, t.description, Money.format(t.amount)),
            open = "wallet",
        )
    }

    /** Do dia 1 ao 3: como o mês passado fechou. */
    private fun monthClose(): Alert? {
        if (today.dayOfMonth > 3) return null
        val prev = today.ym().minusMonths(1)
        val sum = l.monthSummary(prev)
        if (sum.income == 0L && sum.expense == 0L) return null
        val month = Dates.monthName(prev)
        val positive = sum.result >= 0
        return Alert(
            AlertKind.MONTH_CLOSE, "month:${prev.key()}",
            Res.s(if (positive) R.string.alert_month_title_plus else R.string.alert_month_title_minus, month),
            if (tone() == AlertTone.DISCREET) Res.s(R.string.alert_month_d, Money.format(sum.result))
            else Res.s(if (positive) R.string.alert_month_a_plus else R.string.alert_month_a_minus, Money.format(abs(sum.result))),
            open = "insights",
        )
    }

    /* --------------------------------------------- noite --------------------------------------------- */

    /** RF-AVI-11 e o resumo de domingo: um aviso só, no horário que a pessoa escolheu. */
    private fun evening(): Alert? {
        daily()?.let { if (allowed(it)) return it }
        if (today.dayOfWeek == DayOfWeek.SUNDAY) week()?.let { if (allowed(it)) return it }
        return null
    }

    /** Dias seguidos (antes de hoje) com algo anotado ou marcados como sem gastos. */
    fun streak(): Int {
        val logged = l.s.txs.mapTo(HashSet()) { it.date } + l.s.noSpendDays.map { it.date }
        var n = 0
        var d = today.minusDays(1)
        while (d in logged) { n++; d = d.minusDays(1) }
        return n
    }

    private fun daily(): Alert? {
        if (state.reminderMinutes == null) return null
        val loggedToday = l.s.txs.any { it.date == today } || l.s.noSpendDays.any { it.date == today }
        if (loggedToday) return null
        val s = streak()
        val text = when {
            tone() == AlertTone.DISCREET -> Res.s(R.string.alert_daily_d)
            s >= 2 -> Res.s(R.string.alert_daily_streak, s)
            else -> say(AlertKind.DAILY, listOf(R.string.alert_daily_a1, R.string.alert_daily_a2), R.string.alert_daily_d)
        }
        return Alert(
            AlertKind.DAILY, "daily:$today", Res.s(R.string.alert_daily_title), text,
            listOf(AlertAction.NO_SPEND, AlertAction.LOG), open = "voice",
        )
    }

    private fun week(): Alert? {
        val days = (6 downTo 0).map { today.minusDays(it.toLong()) }
        val values = days.map { d -> l.s.txs.filter { it.type == TxType.EXPENSE && it.date == d }.sumOf { it.amount } }
        val total = values.sum()
        if (total == 0L) return null
        val weeks = (1..8).map { w ->
            val end = today.minusDays(7L * w)
            l.s.txs.filter { it.type == TxType.EXPENSE && !it.date.isAfter(end) && it.date.isAfter(end.minusDays(7)) }.sumOf { it.amount }
        }.filter { it > 0 }.sorted()
        val normal = if (weeks.isEmpty()) 0L else weeks[weeks.size / 2]
        val top = days[values.indexOf(values.max())]
        val dayName = top.dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL, io.github.h3yk0.cryo.domain.AppLocale.locale)
        val text = when {
            tone() == AlertTone.DISCREET || normal == 0L -> Res.s(R.string.alert_week_d, dayName)
            total <= normal -> Res.s(R.string.alert_week_less, Num.percent(((normal - total) * 100.0 / normal).roundToInt()), dayName)
            else -> Res.s(R.string.alert_week_more, Num.percent(((total - normal) * 100.0 / normal).roundToInt()), dayName)
        }
        return Alert(
            AlertKind.WEEK, "week:${today}", Res.s(R.string.alert_week_title, Money.format(total)), text,
            open = "insights", week = values, weekNormal = normal / 7,
        )
    }

    companion object {
        /** A conta fixa ainda não foi paga neste mês (para "Já paguei" não pagar duas vezes). */
        fun billOpen(l: Ledger, billId: Long, ymKey: Int): Boolean =
            // RF-CFX-07: pelo livro-caixa, que também reconhece o pagamento que veio do extrato sem a ligação.
            l.billStatuses(io.github.h3yk0.cryo.domain.ymOf(ymKey)).any { it.bill.id == billId && it.payment == null && it.bill.kind == CategoryKind.EXPENSE }
    }
}
