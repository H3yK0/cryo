// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.domain

import io.github.h3yk0.cryo.data.db.Account
import io.github.h3yk0.cryo.data.db.Bill
import io.github.h3yk0.cryo.data.db.Budget
import io.github.h3yk0.cryo.data.db.Category
import io.github.h3yk0.cryo.data.db.CategoryKind
import io.github.h3yk0.cryo.data.db.CreditCard
import io.github.h3yk0.cryo.data.db.Debt
import io.github.h3yk0.cryo.data.db.DebtAdjustment
import io.github.h3yk0.cryo.data.db.Goal
import io.github.h3yk0.cryo.data.db.Investment
import io.github.h3yk0.cryo.data.db.InvestmentYield
import io.github.h3yk0.cryo.data.db.Tx
import io.github.h3yk0.cryo.data.db.TxType
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import kotlin.math.max

/** Fotografia completa dos dados num instante. Todas as contas são feitas a partir dela. */
data class Snapshot(
    val accounts: List<Account> = emptyList(),
    val cards: List<CreditCard> = emptyList(),
    val categories: List<Category> = emptyList(),
    val txs: List<Tx> = emptyList(),
    val investments: List<Investment> = emptyList(),
    val yields: List<InvestmentYield> = emptyList(),
    val goals: List<Goal> = emptyList(),
    val bills: List<Bill> = emptyList(),
    val budgets: List<Budget> = emptyList(),
    val debts: List<Debt> = emptyList(),
    val debtAdjustments: List<DebtAdjustment> = emptyList(),
)

/* ----------------------------- Cartão ----------------------------- */

object CardMath {
    /** Mês de vencimento da fatura em que cai uma compra feita em [date]. Compras no dia do fechamento ou depois vão para a próxima. */
    fun invoiceFor(card: CreditCard, date: LocalDate): YearMonth {
        val closingThisMonth = date.ym().dayClamped(card.closingDay)
        val closingMonth = if (date.isBefore(closingThisMonth)) date.ym() else date.ym().plusMonths(1)
        return if (card.dueDay > card.closingDay) closingMonth else closingMonth.plusMonths(1)
    }

    fun closingDate(card: CreditCard, dueYm: YearMonth): LocalDate {
        val closingMonth = if (card.dueDay > card.closingDay) dueYm else dueYm.minusMonths(1)
        return closingMonth.dayClamped(card.closingDay)
    }

    fun dueDate(card: CreditCard, dueYm: YearMonth): LocalDate = dueYm.dayClamped(card.dueDay)

    /** Divide um total em [n] parcelas; os centavos que sobram vão na primeira. */
    fun split(total: Long, n: Int): List<Long> {
        val base = total / n
        val rest = total - base * n
        return List(n) { i -> if (i == 0) base + rest else base }
    }
}

enum class InvoiceStatus { OPEN, CLOSED, PAID, OVERDUE, EMPTY }

data class Invoice(
    val card: CreditCard,
    val ym: YearMonth,
    val closing: LocalDate,
    val due: LocalDate,
    val items: List<Tx>,
    val total: Long,
    val paid: Long,
) {
    val remaining: Long get() = max(0, total - paid)

    fun status(today: LocalDate): InvoiceStatus = when {
        today.isBefore(closing) -> InvoiceStatus.OPEN
        total <= 0 -> InvoiceStatus.EMPTY
        remaining == 0L -> InvoiceStatus.PAID
        today.isAfter(due) -> InvoiceStatus.OVERDUE
        else -> InvoiceStatus.CLOSED
    }
}

/* -------------------------- Contas fixas -------------------------- */

enum class BillState { PAID, OVERDUE, DUE_SOON, UPCOMING }

data class BillStatus(
    val bill: Bill,
    val ym: YearMonth,
    val due: LocalDate,
    val payment: Tx?,
    val state: BillState,
    val daysUntil: Long,
)

data class BudgetUsage(val category: Category, val limit: Long, val spent: Long) {
    val ratio: Float get() = if (limit > 0) spent.toFloat() / limit else 0f
    val remaining: Long get() = limit - spent
}

data class CategoryTotal(val category: Category?, val total: Long)

data class MonthSummary(
    val income: Long,
    val expense: Long,
    val invested: Long,
    val saved: Long,
    /** Dinheiro emprestado que entrou (não é receita). */
    val borrowed: Long = 0,
) {
    val result: Long get() = income - expense
}

/**
 * Livro-caixa: recebe a fotografia dos dados e calcula saldos, faturas, totais do mês,
 * orçamentos e contas fixas. É puro Kotlin (sem Android), por isso é testável.
 */
class Ledger(val s: Snapshot, val today: LocalDate) {
    val account = s.accounts.associateBy { it.id }
    val card = s.cards.associateBy { it.id }
    val category = s.categories.associateBy { it.id }
    val investment = s.investments.associateBy { it.id }
    val goal = s.goals.associateBy { it.id }
    val bill = s.bills.associateBy { it.id }
    val debt = s.debts.associateBy { it.id }

    val activeAccounts get() = s.accounts.filter { !it.archived }
    val activeCards get() = s.cards.filter { !it.archived }
    val activeInvestments get() = s.investments.filter { !it.archived }
    val activeGoals get() = s.goals.filter { !it.archived }
    val activeDebts get() = s.debts.filter { !it.archived }
    fun categories(kind: CategoryKind) = s.categories.filter { it.kind == kind && !it.archived }

    /* ------------------------------ contas ------------------------------ */

    val balances: Map<Long, Long> by lazy {
        val m = HashMap<Long, Long>()
        s.accounts.forEach { m[it.id] = it.initialBalance }
        fun add(id: Long?, v: Long) { if (id != null) m[id] = (m[id] ?: 0L) + v }
        for (t in s.txs) {
            if (t.date.isAfter(today)) continue
            when (t.type) {
                TxType.EXPENSE -> add(t.accountId, -t.amount)
                TxType.INCOME -> add(t.accountId, t.amount)
                TxType.TRANSFER -> { add(t.accountId, -t.amount); add(t.toAccountId, t.amount) }
                TxType.CARD_PAYMENT, TxType.INVEST_IN, TxType.GOAL_IN -> add(t.accountId, -t.amount)
                TxType.INVEST_OUT, TxType.GOAL_OUT, TxType.DEBT_IN -> add(t.accountId, t.amount)
            }
        }
        m
    }

    fun balance(accountId: Long): Long = balances[accountId] ?: 0L

    val totalBalance: Long by lazy {
        s.accounts.filter { it.includeInTotal && !it.archived }.sumOf { balance(it.id) }
    }

    /* ------------------------------ cartões ----------------------------- */

    private val cardTxs: Map<Long, List<Tx>> by lazy { s.txs.filter { it.cardId != null }.groupBy { it.cardId!! } }

    /** Quanto se deve no cartão (inclui parcelas futuras). Negativo = crédito a favor. */
    fun cardOutstanding(cardId: Long): Long = cardTxs[cardId].orEmpty().sumOf {
        when (it.type) {
            TxType.EXPENSE -> it.amount
            TxType.INCOME, TxType.CARD_PAYMENT -> -it.amount
            else -> 0L
        }
    }

    fun availableLimit(c: CreditCard): Long = c.limitAmount - max(0, cardOutstanding(c.id))

    private val invoiceCache = HashMap<Pair<Long, Int>, Invoice>()

    fun invoice(c: CreditCard, ym: YearMonth): Invoice = invoiceCache.getOrPut(c.id to ym.key()) {
        val key = ym.key()
        val all = cardTxs[c.id].orEmpty()
        val items = all.filter { it.invoiceYm == key && (it.type == TxType.EXPENSE || it.type == TxType.INCOME) }
        val total = items.sumOf { if (it.type == TxType.EXPENSE) it.amount else -it.amount }
        val paid = all.filter { it.type == TxType.CARD_PAYMENT && it.invoiceYm == key }.sumOf { it.amount }
        Invoice(c, ym, CardMath.closingDate(c, ym), CardMath.dueDate(c, ym), items, total, paid)
    }

    fun openInvoiceYm(c: CreditCard): YearMonth = CardMath.invoiceFor(c, today)

    fun invoiceMonths(c: CreditCard): List<YearMonth> =
        (cardTxs[c.id].orEmpty().mapNotNull { it.invoiceYm }.map(::ymOf) + openInvoiceYm(c)).distinct().sorted()

    /** A fatura que pede atenção: a mais antiga já fechada e não paga; se não houver, a aberta. */
    fun focusInvoice(c: CreditCard): Invoice {
        val open = openInvoiceYm(c)
        return invoiceMonths(c).filter { it < open }.map { invoice(c, it) }.firstOrNull { it.remaining > 0 }
            ?: invoice(c, open)
    }

    val totalCardDebt: Long by lazy { s.cards.sumOf { max(0, cardOutstanding(it.id)) } }

    /* --------------------------- investimentos -------------------------- */

    private val invTxs by lazy { s.txs.filter { it.investmentId != null }.groupBy { it.investmentId!! } }
    private val invYields by lazy { s.yields.groupBy { it.investmentId } }

    fun invested(id: Long): Long = invTxs[id].orEmpty().sumOf {
        when (it.type) {
            TxType.INVEST_IN -> it.amount
            TxType.INVEST_OUT -> -it.amount
            else -> 0L
        }
    }

    fun yieldOf(id: Long): Long = invYields[id].orEmpty().sumOf { it.amount }
    fun investmentValue(id: Long): Long = invested(id) + yieldOf(id)
    fun investmentHistory(id: Long): List<Any> =
        (invTxs[id].orEmpty() + invYields[id].orEmpty()).sortedByDescending {
            when (it) { is Tx -> it.date; is InvestmentYield -> it.date; else -> LocalDate.MIN }
        }

    val totalInvestments: Long by lazy { s.investments.sumOf { investmentValue(it.id) } }
    val totalInvestedPrincipal: Long by lazy { s.investments.sumOf { invested(it.id) } }
    val totalYield: Long by lazy { s.investments.sumOf { yieldOf(it.id) } }

    /* ------------------------------- metas ------------------------------ */

    private val goalTxs by lazy { s.txs.filter { it.goalId != null }.groupBy { it.goalId!! } }

    fun goalSaved(id: Long): Long = goalTxs[id].orEmpty().sumOf {
        when (it.type) {
            TxType.GOAL_IN -> it.amount
            TxType.GOAL_OUT -> -it.amount
            else -> 0L
        }
    }

    fun goalHistory(id: Long): List<Tx> = goalTxs[id].orEmpty()

    /** Quanto guardar por mês para atingir a meta no prazo (null se sem prazo ou já atingida). */
    fun goalMonthlyNeeded(g: Goal): Long? {
        val deadline = g.deadline ?: return null
        val missing = g.target - goalSaved(g.id)
        if (missing <= 0) return null
        val months = ChronoUnit.MONTHS.between(today.ym(), deadline.ym()).coerceAtLeast(0) + 1
        return (missing + months - 1) / months
    }

    val totalGoals: Long by lazy { s.goals.sumOf { goalSaved(it.id) } }

    /* ------------------------------ dívidas ------------------------------ */

    private val debtTxs by lazy { s.txs.filter { it.debtId != null }.groupBy { it.debtId!! } }
    private val debtAdj by lazy { s.debtAdjustments.groupBy { it.debtId } }
    private val debtCache = HashMap<Long, DebtInfo>()

    fun debtInfo(d: Debt): DebtInfo = debtCache.getOrPut(d.id) {
        DebtMath.info(d, debtTxs[d.id].orEmpty(), debtAdj[d.id].orEmpty(), today)
    }

    /** Pagamentos, empréstimos e ajustes de uma dívida, do mais novo para o mais antigo. */
    fun debtHistory(id: Long): List<Any> =
        (debtTxs[id].orEmpty() + debtAdj[id].orEmpty()).sortedByDescending {
            when (it) { is Tx -> it.date; is DebtAdjustment -> it.date; else -> LocalDate.MIN }
        }

    val debtOverview: DebtOverview by lazy {
        val infos = activeDebts.map { debtInfo(it) }
        val open = infos.filter { !it.isPaidOff }
        val ends = open.map { it.payoffYm }
        DebtOverview(
            total = open.sumOf { it.remaining },
            paidAll = open.sumOf { it.paidAll },
            monthlyInstallments = open.filter { it.hasInstallments }.sumOf { it.debt.installmentAmount },
            pendingThisMonth = open.sumOf { it.pendingThisMonth },
            overdueCount = open.sumOf { it.overdueCount },
            freeOfDebtYm = if (open.isNotEmpty() && ends.all { it != null }) ends.filterNotNull().max() else null,
            open = open.sortedWith(compareBy<DebtInfo>({ it.state.ordinal }, { it.nextDue ?: LocalDate.MAX })),
        )
    }

    val totalDebts: Long get() = debtOverview.total

    /** Média do que entrou por mês nos últimos 3 meses completos (ou neste mês, se ainda não há histórico). */
    val averageIncome: Long by lazy {
        val months = (1..3).map { monthSummary(today.ym().minusMonths(it.toLong())).income }.filter { it > 0 }
        if (months.isEmpty()) monthSummary(today.ym()).income else months.sum() / months.size
    }

    /** Patrimônio: contas + investimentos + caixinhas − cartões − dívidas. */
    val netWorth: Long by lazy {
        totalBalance + totalInvestments + totalGoals - s.cards.sumOf { cardOutstanding(it.id) } - totalDebts
    }

    /* --------------------------- totais do mês -------------------------- */

    private val monthCache = HashMap<Int, List<Tx>>()

    fun monthTxs(ym: YearMonth): List<Tx> = monthCache.getOrPut(ym.key()) {
        val start = ym.atDay(1)
        val end = ym.atEndOfMonth()
        s.txs.filter { !it.date.isBefore(start) && !it.date.isAfter(end) }
    }

    fun monthSummary(ym: YearMonth, untilDay: Int? = null): MonthSummary {
        var inc = 0L; var exp = 0L; var inv = 0L; var sav = 0L; var bor = 0L
        for (t in monthTxs(ym)) {
            if (untilDay != null && t.date.dayOfMonth > untilDay) continue
            when (t.type) {
                TxType.INCOME -> inc += t.amount
                TxType.EXPENSE -> exp += t.amount
                TxType.INVEST_IN -> inv += t.amount
                TxType.INVEST_OUT -> inv -= t.amount
                TxType.GOAL_IN -> sav += t.amount
                TxType.GOAL_OUT -> sav -= t.amount
                TxType.DEBT_IN -> bor += t.amount
                else -> {}
            }
        }
        return MonthSummary(inc, exp, inv, sav, bor)
    }

    fun byCategory(ym: YearMonth, kind: CategoryKind, untilDay: Int? = null): List<CategoryTotal> {
        val type = if (kind == CategoryKind.EXPENSE) TxType.EXPENSE else TxType.INCOME
        return monthTxs(ym)
            .filter { it.type == type && (untilDay == null || it.date.dayOfMonth <= untilDay) }
            .groupBy { it.categoryId }
            .map { (id, list) -> CategoryTotal(id?.let { category[it] }, list.sumOf { it.amount }) }
            .sortedByDescending { it.total }
    }

    fun categorySpent(ym: YearMonth, categoryId: Long): Long =
        monthTxs(ym).filter { it.type == TxType.EXPENSE && it.categoryId == categoryId }.sumOf { it.amount }

    fun dailyExpenses(ym: YearMonth): LongArray {
        val arr = LongArray(ym.lengthOfMonth() + 1)
        monthTxs(ym).filter { it.type == TxType.EXPENSE }.forEach { arr[it.date.dayOfMonth] += it.amount }
        return arr
    }

    /* ----------------------------- orçamentos ---------------------------- */

    fun budgetUsages(ym: YearMonth): List<BudgetUsage> = s.budgets.mapNotNull { b ->
        category[b.categoryId]?.let { BudgetUsage(it, b.limitAmount, categorySpent(ym, it.id)) }
    }.sortedByDescending { it.ratio }

    fun budgetOf(categoryId: Long): Budget? = s.budgets.firstOrNull { it.categoryId == categoryId }

    /* ---------------------------- contas fixas --------------------------- */

    private val billPayments: Map<Pair<Long, Int>, Tx> by lazy {
        s.txs.filter { it.billId != null && it.billYm != null }.associateBy { it.billId!! to it.billYm!! }
    }

    fun billStatuses(ym: YearMonth): List<BillStatus> = s.bills
        .filter { it.active && it.startYm <= ym.key() }
        .map { b ->
            val due = ym.dayClamped(b.dueDay)
            val pay = billPayments[b.id to ym.key()]
            val days = ChronoUnit.DAYS.between(today, due)
            val state = when {
                pay != null -> BillState.PAID
                days < 0 -> BillState.OVERDUE
                days <= 3 -> BillState.DUE_SOON
                else -> BillState.UPCOMING
            }
            BillStatus(b, ym, due, pay, state, days)
        }
        .sortedBy { it.due }

    /* ------------------------------ histórico ---------------------------- */

    /** Palavra da descrição -> categoria usada por último. Faz o registro por frase "aprender" com você. */
    val learnedCategories: Map<String, Long> by lazy {
        val m = HashMap<String, Long>()
        for (t in s.txs) {
            if ((t.type != TxType.EXPENSE && t.type != TxType.INCOME) || t.categoryId == null) continue
            // Descrições criadas pelo próprio app ("Parcela · Financiamento") não ensinam nada.
            if (t.debtId != null || t.description.contains(" · ")) continue
            val norm = t.description.normalized().trim()
            if (norm.isEmpty()) continue
            m.putIfAbsent(norm, t.categoryId)
            norm.split(Regex("[^a-z0-9]+")).firstOrNull { it.length >= 3 && it !in PhraseParser.STOP_WORDS }
                ?.let { m.putIfAbsent(it, t.categoryId) }
        }
        m
    }

    /** Descrições recentes (para sugestões ao digitar). */
    fun recentDescriptions(type: TxType, limit: Int = 30): List<Tx> =
        s.txs.asSequence().filter { it.type == type && it.description.isNotBlank() && it.debtId == null && !it.description.contains(" · ") }
            .distinctBy { it.description.normalized() }.take(limit).toList()

    fun accountName(id: Long?): String = id?.let { account[it]?.name } ?: "—"
    fun cardName(id: Long?): String = id?.let { card[it]?.name } ?: "—"
}
