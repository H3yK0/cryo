// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.withTransaction
import io.github.h3yk0.cryo.data.db.Account
import io.github.h3yk0.cryo.data.db.Bill
import io.github.h3yk0.cryo.data.db.Budget
import io.github.h3yk0.cryo.data.db.Category
import io.github.h3yk0.cryo.data.db.CategoryKind
import io.github.h3yk0.cryo.data.db.CreditCard
import io.github.h3yk0.cryo.data.db.CryoDatabase
import io.github.h3yk0.cryo.data.db.Goal
import io.github.h3yk0.cryo.data.db.Investment
import io.github.h3yk0.cryo.data.db.InvestmentYield
import io.github.h3yk0.cryo.data.db.Tx
import io.github.h3yk0.cryo.data.db.TxType
import io.github.h3yk0.cryo.domain.CardMath
import io.github.h3yk0.cryo.domain.Money
import io.github.h3yk0.cryo.domain.Snapshot
import io.github.h3yk0.cryo.domain.key
import io.github.h3yk0.cryo.domain.ym
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID
import kotlin.math.roundToInt

/* ============================== Ajustes ============================== */

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val lockEnabled: Boolean = false,
    val hideValues: Boolean = false,
    val defaultAccountId: Long? = null,
    val onboardingDone: Boolean = false,
    val userName: String = "",
    val remindersEnabled: Boolean = true,
)

private val Context.dataStore by preferencesDataStore("settings")

class SettingsRepository(private val context: Context) {
    private object K {
        val theme = stringPreferencesKey("theme")
        val dynamic = booleanPreferencesKey("dynamic")
        val lock = booleanPreferencesKey("lock")
        val hide = booleanPreferencesKey("hide")
        val defaultAccount = longPreferencesKey("default_account")
        val onboarding = booleanPreferencesKey("onboarding")
        val name = stringPreferencesKey("name")
        val reminders = booleanPreferencesKey("reminders")
    }

    val flow: Flow<AppSettings> = context.dataStore.data.map { p ->
        AppSettings(
            themeMode = p[K.theme]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM,
            dynamicColor = p[K.dynamic] ?: true,
            lockEnabled = p[K.lock] ?: false,
            hideValues = p[K.hide] ?: false,
            defaultAccountId = p[K.defaultAccount],
            onboardingDone = p[K.onboarding] ?: false,
            userName = p[K.name] ?: "",
            remindersEnabled = p[K.reminders] ?: true,
        )
    }

    suspend fun current(): AppSettings = flow.first()
    suspend fun setTheme(m: ThemeMode) = context.dataStore.edit { it[K.theme] = m.name }
    suspend fun setDynamic(v: Boolean) = context.dataStore.edit { it[K.dynamic] = v }
    suspend fun setLock(v: Boolean) = context.dataStore.edit { it[K.lock] = v }
    suspend fun setHideValues(v: Boolean) = context.dataStore.edit { it[K.hide] = v }
    suspend fun setOnboardingDone(v: Boolean) = context.dataStore.edit { it[K.onboarding] = v }
    suspend fun setName(v: String) = context.dataStore.edit { it[K.name] = v.trim() }
    suspend fun setReminders(v: Boolean) = context.dataStore.edit { it[K.reminders] = v }
    suspend fun setDefaultAccount(id: Long?) = context.dataStore.edit {
        if (id == null) it.remove(K.defaultAccount) else it[K.defaultAccount] = id
    }
}

/* ============================ Repositório ============================ */

private data class Part1(
    val a: List<Account>, val c: List<CreditCard>, val cat: List<Category>, val t: List<Tx>, val i: List<Investment>,
)

private data class Part2(val y: List<InvestmentYield>, val g: List<Goal>, val b: List<Bill>, val bu: List<Budget>)

class FinanceRepository(private val db: CryoDatabase, scope: CoroutineScope) {
    private val dao = db.dao()

    /** Fotografia sempre atualizada de todos os dados (qualquer gravação dispara uma nova). */
    val snapshot: StateFlow<Snapshot?> = combine(
        combine(dao.accounts(), dao.cards(), dao.categories(), dao.txs(), dao.investments(), ::Part1),
        combine(dao.yields(), dao.goals(), dao.bills(), dao.budgets(), ::Part2),
    ) { p1, p2 -> Snapshot(p1.a, p1.c, p1.cat, p1.t, p1.i, p2.y, p2.g, p2.b, p2.bu) }
        .stateIn(scope, SharingStarted.Eagerly, null)

    suspend fun loadOnce(): Snapshot = Snapshot(
        dao.accountsOnce(), dao.cardsOnce(), dao.categoriesOnce(), dao.txsOnce(), dao.investmentsOnce(),
        dao.yieldsOnce(), dao.goalsOnce(), dao.billsOnce(), dao.budgetsOnce(),
    )

    suspend fun ensureSeeded() {
        if (dao.categoryCount() == 0) dao.putCategories(DefaultData.categories())
    }

    /* ---------------------------- lançamentos ---------------------------- */

    suspend fun saveTx(tx: Tx): Long = dao.putTx(tx)

    /** Compra no cartão, opcionalmente parcelada: cria uma movimentação por parcela, cada uma na sua fatura. */
    suspend fun saveCardPurchase(base: Tx, card: CreditCard, installments: Int): Tx {
        val first = CardMath.invoiceFor(card, base.date)
        if (installments <= 1) {
            val t = base.copy(cardId = card.id, accountId = null, invoiceYm = first.key())
            return t.copy(id = dao.putTx(t))
        }
        val group = UUID.randomUUID().toString()
        val parts = CardMath.split(base.amount, installments)
        var firstTx: Tx? = null
        db.withTransaction {
            parts.forEachIndexed { i, v ->
                val t = base.copy(
                    id = 0, amount = v, date = base.date.plusMonths(i.toLong()), cardId = card.id, accountId = null,
                    invoiceYm = first.plusMonths(i.toLong()).key(), installmentGroup = group,
                    installmentNumber = i + 1, installmentTotal = installments,
                )
                val id = dao.putTx(t)
                if (i == 0) firstTx = t.copy(id = id)
            }
        }
        return firstTx!!
    }

    suspend fun deleteTx(tx: Tx, wholeGroup: Boolean = false) {
        if (wholeGroup && tx.installmentGroup != null) dao.deleteTxGroup(tx.installmentGroup) else dao.deleteTx(tx.id)
    }

    /** Recoloca uma movimentação apagada (botão "Desfazer"). */
    suspend fun restore(txs: List<Tx>) = dao.putTxs(txs)

    suspend fun txById(id: Long): Tx? = dao.txById(id)

    /** Aviso de orçamento depois de registrar um gasto (80% e 100%). */
    suspend fun budgetAlert(categoryId: Long?, date: LocalDate, added: Long): String? {
        if (categoryId == null || added <= 0) return null
        val budget = dao.budgetsOnce().firstOrNull { it.categoryId == categoryId } ?: return null
        if (budget.limitAmount <= 0) return null
        val cat = dao.categoriesOnce().firstOrNull { it.id == categoryId } ?: return null
        val ym = date.ym()
        val spent = dao.categorySpent(categoryId, ym.atDay(1), ym.atEndOfMonth())
        val before = spent - added
        val ratio = spent.toDouble() / budget.limitAmount
        val beforeRatio = before.toDouble() / budget.limitAmount
        return when {
            beforeRatio < 1.0 && ratio >= 1.0 ->
                "Orçamento de ${cat.name} estourado: ${Money.format(spent)} de ${Money.format(budget.limitAmount)}"
            beforeRatio < 0.8 && ratio >= 0.8 ->
                "Atenção: você já usou ${(ratio * 100).roundToInt()}% do orçamento de ${cat.name}"
            else -> null
        }
    }

    /* ------------------------------- contas ------------------------------ */

    suspend fun saveAccount(a: Account): Long = dao.putAccount(a)

    /** Só apaga se a conta não tiver movimentações (senão o histórico ficaria quebrado). */
    suspend fun deleteAccount(a: Account): Boolean {
        if (dao.countTxForAccount(a.id) > 0) return false
        dao.deleteAccount(a.id)
        return true
    }

    /* ------------------------------ cartões ------------------------------ */

    suspend fun saveCard(c: CreditCard): Long = dao.putCard(c)

    suspend fun deleteCard(c: CreditCard): Boolean {
        if (dao.countTxForCard(c.id) > 0) return false
        dao.deleteCard(c.id)
        return true
    }

    suspend fun payInvoice(card: CreditCard, ym: YearMonth, amount: Long, accountId: Long, date: LocalDate) {
        dao.putTx(
            Tx(
                type = TxType.CARD_PAYMENT, amount = amount, date = date, description = "Fatura ${card.name}",
                accountId = accountId, cardId = card.id, invoiceYm = ym.key(),
            ),
        )
    }

    /* ----------------------------- categorias ---------------------------- */

    suspend fun saveCategory(c: Category): Long = dao.putCategory(c)

    /** Se a categoria já foi usada, ela é arquivada (some das listas, mas o histórico continua certo). */
    suspend fun deleteCategory(c: Category): Boolean {
        dao.deleteBudget(c.id)
        return if (dao.countTxForCategory(c.id) > 0) {
            dao.putCategory(c.copy(archived = true)); false
        } else {
            dao.deleteCategory(c.id); true
        }
    }

    /* --------------------------- investimentos --------------------------- */

    suspend fun saveInvestment(i: Investment): Long = dao.putInvestment(i)

    suspend fun deleteInvestment(i: Investment) = db.withTransaction {
        dao.deleteTxsOfInvestment(i.id); dao.deleteYieldsOf(i.id); dao.deleteInvestment(i.id)
    }

    /** Atualiza o valor atual: a diferença vira um registro de rendimento (ou perda). */
    suspend fun updateInvestmentValue(i: Investment, currentValue: Long, newValue: Long, date: LocalDate) {
        val diff = newValue - currentValue
        if (diff != 0L) dao.putYield(InvestmentYield(investmentId = i.id, date = date, amount = diff))
    }

    suspend fun deleteYield(y: InvestmentYield) = dao.deleteYield(y.id)

    /* -------------------------------- metas ------------------------------ */

    suspend fun saveGoal(g: Goal): Long = dao.putGoal(g)

    suspend fun deleteGoal(g: Goal) = db.withTransaction { dao.deleteTxsOfGoal(g.id); dao.deleteGoal(g.id) }

    /* ----------------------------- contas fixas -------------------------- */

    suspend fun saveBill(b: Bill): Long = dao.putBill(b)

    suspend fun deleteBill(b: Bill) = db.withTransaction { dao.unlinkBill(b.id); dao.deleteBill(b.id) }

    suspend fun markBillPaid(
        bill: Bill, ym: YearMonth, amount: Long, date: LocalDate, accountId: Long?, card: CreditCard?,
    ): Long {
        val tx = Tx(
            type = if (bill.kind == CategoryKind.EXPENSE) TxType.EXPENSE else TxType.INCOME,
            amount = amount, date = date, description = bill.name, categoryId = bill.categoryId,
            accountId = if (card == null) accountId else null, cardId = card?.id,
            invoiceYm = card?.let { CardMath.invoiceFor(it, date).key() },
            billId = bill.id, billYm = ym.key(),
        )
        return dao.putTx(tx)
    }

    suspend fun unmarkBillPaid(bill: Bill, ym: YearMonth) = dao.deleteBillPayment(bill.id, ym.key())

    /* ----------------------------- orçamentos ---------------------------- */

    suspend fun saveBudget(categoryId: Long, limit: Long) = dao.putBudget(Budget(categoryId, limit))
    suspend fun deleteBudget(categoryId: Long) = dao.deleteBudget(categoryId)

    /* ------------------------------- backup ------------------------------ */

    suspend fun replaceAll(s: Snapshot) = db.withTransaction {
        clearTables()
        dao.putAccounts(s.accounts); dao.putCards(s.cards); dao.putCategories(s.categories); dao.putTxs(s.txs)
        dao.putInvestments(s.investments); dao.putYields(s.yields); dao.putGoals(s.goals); dao.putBills(s.bills)
        dao.putBudgets(s.budgets)
    }

    suspend fun eraseAll() = db.withTransaction {
        clearTables()
        dao.putCategories(DefaultData.categories())
    }

    private suspend fun clearTables() {
        dao.clearTxs(); dao.clearAccounts(); dao.clearCards(); dao.clearCategories(); dao.clearInvestments()
        dao.clearYields(); dao.clearGoals(); dao.clearBills(); dao.clearBudgets()
    }
}
