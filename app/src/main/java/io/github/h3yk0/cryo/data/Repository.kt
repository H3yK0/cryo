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
import io.github.h3yk0.cryo.data.db.Debt
import io.github.h3yk0.cryo.data.db.DebtAdjustment
import io.github.h3yk0.cryo.data.db.Goal
import io.github.h3yk0.cryo.data.db.Investment
import io.github.h3yk0.cryo.data.db.InvestmentYield
import io.github.h3yk0.cryo.data.db.Tx
import io.github.h3yk0.cryo.data.db.TxType
import io.github.h3yk0.cryo.domain.CardMath
import io.github.h3yk0.cryo.domain.Money
import io.github.h3yk0.cryo.domain.Snapshot
import io.github.h3yk0.cryo.domain.debtCategory
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

private data class Part2(
    val y: List<InvestmentYield>, val g: List<Goal>, val b: List<Bill>, val bu: List<Budget>, val d: List<Debt>,
)

/** O que foi gravado numa ação, para o botão "Desfazer". */
data class Undo(val txs: List<Tx> = emptyList(), val adjustments: List<DebtAdjustment> = emptyList())

class FinanceRepository(private val db: CryoDatabase, scope: CoroutineScope) {
    private val dao = db.dao()

    /** Fotografia sempre atualizada de todos os dados (qualquer gravação dispara uma nova). */
    val snapshot: StateFlow<Snapshot?> = combine(
        combine(dao.accounts(), dao.cards(), dao.categories(), dao.txs(), dao.investments(), ::Part1),
        combine(dao.yields(), dao.goals(), dao.bills(), dao.budgets(), dao.debts(), ::Part2),
        dao.debtAdjustments(),
    ) { p1, p2, adj -> Snapshot(p1.a, p1.c, p1.cat, p1.t, p1.i, p2.y, p2.g, p2.b, p2.bu, p2.d, adj) }
        .stateIn(scope, SharingStarted.Eagerly, null)

    /** Fotografia de todos os dados, lida de uma vez só (numa transação, para não pegar nada pela metade). */
    suspend fun loadOnce(): Snapshot = db.withTransaction {
        Snapshot(
            dao.accountsOnce(), dao.cardsOnce(), dao.categoriesOnce(), dao.txsOnce(), dao.investmentsOnce(),
            dao.yieldsOnce(), dao.goalsOnce(), dao.billsOnce(), dao.budgetsOnce(), dao.debtsOnce(), dao.debtAdjustmentsOnce(),
        )
    }

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

    /* ------------------------------- dívidas ------------------------------ */

    suspend fun saveDebt(d: Debt): Long = dao.putDebt(d)

    /** Cadastra a dívida. Se o dinheiro do empréstimo entrou numa conta agora, registra essa entrada. */
    suspend fun createDebt(d: Debt, received: Long, accountId: Long?, date: LocalDate): Long = db.withTransaction {
        val id = dao.putDebt(d)
        if (received > 0 && accountId != null) {
            dao.putTx(
                Tx(
                    type = TxType.DEBT_IN, amount = received, date = date, description = "Empréstimo · ${d.name}",
                    accountId = accountId, debtId = id,
                ),
            )
        }
        id
    }

    /** Apaga a dívida e os ajustes dela. Os pagamentos continuam no extrato (o dinheiro saiu de verdade). */
    suspend fun deleteDebt(d: Debt) = db.withTransaction {
        dao.unlinkDebt(d.id); dao.deleteAdjustmentsOf(d.id); dao.deleteDebt(d.id)
    }

    suspend fun debtCategoryId(): Long? = dao.categoriesOnce().debtCategory()?.id

    suspend fun ensureDebtCategory() {
        if (dao.categoriesOnce().debtCategory() != null) return
        val existing = dao.categoriesOnce()
        val cat = DefaultData.debtCategory()
        dao.putCategories(existing.filter { it.kind == CategoryKind.EXPENSE && it.sortOrder >= cat.sortOrder }.map { it.copy(sortOrder = it.sortOrder + 1) })
        dao.putCategory(cat)
    }

    /**
     * Pagamento de dívida: vira um gasto normal (categoria "Dívidas e empréstimos") ligado à dívida.
     * [extra] é juros ou multa pagos junto: sai da conta, mas não diminui a dívida.
     * [discount] é desconto ao quitar: diminui a dívida sem sair dinheiro.
     */
    suspend fun payDebt(
        debt: Debt, amount: Long, accountId: Long, date: LocalDate, installment: Boolean,
        extra: Long = 0, discount: Long = 0,
    ): Undo {
        val cat = debtCategoryId()
        val txs = ArrayList<Tx>()
        val adjs = ArrayList<DebtAdjustment>()
        db.withTransaction {
            if (amount > 0) {
                val t = Tx(
                    type = TxType.EXPENSE, amount = amount, date = date,
                    description = (if (installment) "Parcela · " else "Pagamento · ") + debt.name,
                    categoryId = cat, accountId = accountId, debtId = debt.id,
                )
                txs += t.copy(id = dao.putTx(t))
            }
            if (extra > 0) {
                val t = Tx(
                    type = TxType.EXPENSE, amount = extra, date = date, description = "Juros/multa · ${debt.name}",
                    categoryId = cat, accountId = accountId,
                )
                txs += t.copy(id = dao.putTx(t))
            }
            if (discount > 0) {
                val a = DebtAdjustment(debtId = debt.id, date = date, amount = -discount, note = "Desconto na quitação")
                adjs += a.copy(id = dao.putDebtAdjustment(a))
            }
        }
        return Undo(txs, adjs)
    }

    /** Pegou mais dinheiro emprestado: entra na conta e aumenta a dívida. */
    suspend fun borrowMore(debt: Debt, amount: Long, accountId: Long, date: LocalDate): Undo {
        val t = Tx(
            type = TxType.DEBT_IN, amount = amount, date = date, description = "Empréstimo · ${debt.name}",
            accountId = accountId, debtId = debt.id,
        )
        return Undo(txs = listOf(t.copy(id = dao.putTx(t))))
    }

    /** Corrige quanto falta pagar: a diferença vira juros/encargos (+) ou desconto (−). */
    suspend fun adjustDebt(debt: Debt, diff: Long, date: LocalDate, note: String): Undo {
        if (diff == 0L) return Undo()
        val a = DebtAdjustment(debtId = debt.id, date = date, amount = diff, note = note)
        return Undo(adjustments = listOf(a.copy(id = dao.putDebtAdjustment(a))))
    }

    suspend fun deleteDebtAdjustment(a: DebtAdjustment) = dao.deleteDebtAdjustment(a.id)

    suspend fun undo(u: Undo) = db.withTransaction {
        u.txs.forEach { dao.deleteTx(it.id) }
        u.adjustments.forEach { dao.deleteDebtAdjustment(it.id) }
    }

    /* ------------------------------- backup ------------------------------ */

    /** [fromOldVersion]: backup feito antes da versão 1.1 (sem dívidas): ganha a categoria de dívidas. */
    suspend fun replaceAll(s: Snapshot, fromOldVersion: Boolean = false) = db.withTransaction {
        clearTables()
        dao.putAccounts(s.accounts); dao.putCards(s.cards); dao.putCategories(s.categories); dao.putTxs(s.txs)
        dao.putInvestments(s.investments); dao.putYields(s.yields); dao.putGoals(s.goals); dao.putBills(s.bills)
        dao.putBudgets(s.budgets); dao.putDebts(s.debts); dao.putDebtAdjustments(s.debtAdjustments)
        if (fromOldVersion) ensureDebtCategory()
    }

    suspend fun eraseAll() = db.withTransaction {
        clearTables()
        dao.putCategories(DefaultData.categories())
    }

    private suspend fun clearTables() {
        dao.clearTxs(); dao.clearAccounts(); dao.clearCards(); dao.clearCategories(); dao.clearInvestments()
        dao.clearYields(); dao.clearGoals(); dao.clearBills(); dao.clearBudgets(); dao.clearDebts()
        dao.clearDebtAdjustments()
    }
}
