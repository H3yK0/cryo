// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.data

import io.github.h3yk0.cryo.R
import io.github.h3yk0.cryo.domain.Res
import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.withTransaction
import io.github.h3yk0.cryo.data.db.Account
import io.github.h3yk0.cryo.data.db.Bill
import io.github.h3yk0.cryo.data.db.Budget
import io.github.h3yk0.cryo.data.db.BudgetAdjustment
import io.github.h3yk0.cryo.data.db.NoSpendDay
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
    /** RF-PRF-01: a bolinha do perfil: "" (letra), "art:<chave>", "emoji:<grafema>" ou "photo:<carimbo>". */
    val avatar: String = "",
    /** A cor de fundo da bolinha (0 = a do tema). */
    val avatarColor: Int = 0,
    /** RF-LAN-10: o último dia usado num lançamento (fica guardado para a próxima vez). */
    val lastTxDate: LocalDate? = null,
    /** RF-ANA-09 (1.7): a vista do fluxo do dinheiro em Análises (0 = Barras, 1 = Funil); a escolha fica guardada (PRI-09). */
    val flowView: Int = 0,
    /** RF-BLQ-02 (1.7, LA2): segundos fora do app até pedir o desbloqueio de novo (0 = na hora; `LockRules.CHOICES`). */
    val lockAfter: Int = io.github.h3yk0.cryo.domain.LockRules.DEFAULT,
    /** RF-BLQ-07 (1.7, LA2 e LA6): o modo privacidade (prints e gravações pretos), só com o bloqueio ligado. */
    val privacyMode: Boolean = false,
    /** RF-ORG-04 (1.7, LA8): a arrumação de cada tela (o texto de `ScreenLayout`), pela tela (`Layouts`). */
    val layouts: Map<String, String> = emptyMap(),
) {
    /** A arrumação de uma tela (a padrão, se a pessoa nunca mexeu). */
    fun layout(screen: String): io.github.h3yk0.cryo.domain.ScreenLayout = io.github.h3yk0.cryo.domain.ScreenLayout.decode(layouts[screen])
}

private val Context.dataStore by preferencesDataStore("settings")

/** Ajustes do modo de teste (modo desenvolvedor): separados dos de verdade. */
private val Context.testDataStore by preferencesDataStore("settings_test")

/** RF-ORG-04: o começo do nome de cada arrumação guardada ("layout_home", "layout_insights"...). */
private const val LAYOUT = "layout_"

class SettingsRepository(context: Context, sandbox: Boolean = false) {
    private val store = if (sandbox) context.testDataStore else context.dataStore

    private object K {
        val theme = stringPreferencesKey("theme")
        val dynamic = booleanPreferencesKey("dynamic")
        val lock = booleanPreferencesKey("lock")
        val hide = booleanPreferencesKey("hide")
        val defaultAccount = longPreferencesKey("default_account")
        val onboarding = booleanPreferencesKey("onboarding")
        val name = stringPreferencesKey("name")
        val reminders = booleanPreferencesKey("reminders")
        val avatar = stringPreferencesKey("avatar")
        val avatarColor = intPreferencesKey("avatar_color")
        val lastTxDate = longPreferencesKey("last_tx_date")
        val flowView = intPreferencesKey("flow_view")
        val lockAfter = intPreferencesKey("lock_after")
        val privacy = booleanPreferencesKey("privacy")
    }

    val flow: Flow<AppSettings> = store.data.map { p ->
        AppSettings(
            themeMode = p[K.theme]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM,
            dynamicColor = p[K.dynamic] ?: true,
            lockEnabled = p[K.lock] ?: false,
            hideValues = p[K.hide] ?: false,
            defaultAccountId = p[K.defaultAccount],
            onboardingDone = p[K.onboarding] ?: false,
            userName = p[K.name] ?: "",
            remindersEnabled = p[K.reminders] ?: true,
            avatar = p[K.avatar] ?: "",
            avatarColor = p[K.avatarColor] ?: 0,
            lastTxDate = p[K.lastTxDate]?.let { runCatching { LocalDate.ofEpochDay(it) }.getOrNull() },
            flowView = (p[K.flowView] ?: 0).coerceIn(0, 1),
            lockAfter = io.github.h3yk0.cryo.domain.LockRules.normalize(p[K.lockAfter] ?: io.github.h3yk0.cryo.domain.LockRules.DEFAULT),
            privacyMode = p[K.privacy] ?: false,
            layouts = p.asMap().entries
                .mapNotNull { (k, v) -> if (k.name.startsWith(LAYOUT) && v is String) k.name.removePrefix(LAYOUT) to v else null }
                .toMap().let { io.github.h3yk0.cryo.domain.Layouts.clean(it) },
        )
    }

    suspend fun current(): AppSettings = flow.first()
    suspend fun setTheme(m: ThemeMode) = store.edit { it[K.theme] = m.name }
    suspend fun setDynamic(v: Boolean) = store.edit { it[K.dynamic] = v }
    suspend fun setLock(v: Boolean) = store.edit { it[K.lock] = v }
    suspend fun setHideValues(v: Boolean) = store.edit { it[K.hide] = v }
    suspend fun setOnboardingDone(v: Boolean) = store.edit { it[K.onboarding] = v }
    suspend fun setName(v: String) = store.edit { it[K.name] = v.trim() }
    suspend fun setAvatar(value: String, color: Int) = store.edit { it[K.avatar] = value.take(64); it[K.avatarColor] = color }
    suspend fun setReminders(v: Boolean) = store.edit { it[K.reminders] = v }
    suspend fun setLastTxDate(d: LocalDate) = store.edit { it[K.lastTxDate] = d.toEpochDay() }
    suspend fun setFlowView(v: Int) = store.edit { it[K.flowView] = v.coerceIn(0, 1) }
    suspend fun setLockAfter(seconds: Int) = store.edit { it[K.lockAfter] = io.github.h3yk0.cryo.domain.LockRules.normalize(seconds) }
    suspend fun setPrivacyMode(v: Boolean) = store.edit { it[K.privacy] = v }

    /** RF-ORG-02: guarda a arrumação de uma tela (a padrão apaga o que estava guardado). */
    suspend fun setLayout(screen: String, layout: io.github.h3yk0.cryo.domain.ScreenLayout) = store.edit {
        if (screen !in io.github.h3yk0.cryo.domain.Layouts.ALL) return@edit
        val key = stringPreferencesKey(LAYOUT + screen)
        if (layout.isDefault) it.remove(key) else it[key] = layout.encode()
    }

    /** RF-ORG-04: a arrumação que veio num backup (só telas conhecidas; as outras ficam como estão). */
    suspend fun setLayouts(raw: Map<String, String>) = store.edit {
        io.github.h3yk0.cryo.domain.Layouts.clean(raw).forEach { (screen, text) -> it[stringPreferencesKey(LAYOUT + screen)] = text }
    }
    /** Volta todos os ajustes ao padrão. */
    suspend fun clear() = store.edit { it.clear() }

    suspend fun setDefaultAccount(id: Long?) = store.edit {
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

private data class Part3(
    val adj: List<DebtAdjustment>, val badj: List<io.github.h3yk0.cryo.data.db.BudgetAdjustment>, val nsd: List<NoSpendDay>,
    val skips: List<io.github.h3yk0.cryo.data.db.BillSkip>,
)

/** O que foi gravado numa ação, para o botão "Desfazer". */
data class Undo(val txs: List<Tx> = emptyList(), val adjustments: List<DebtAdjustment> = emptyList())

class FinanceRepository(private val db: CryoDatabase, scope: CoroutineScope) {
    private val dao = db.dao()

    /** Fotografia sempre atualizada de todos os dados (qualquer gravação dispara uma nova). */
    val snapshot: StateFlow<Snapshot?> = combine(
        combine(dao.accounts(), dao.cards(), dao.categories(), dao.txs(), dao.investments(), ::Part1),
        combine(dao.yields(), dao.goals(), dao.bills(), dao.budgets(), dao.debts(), ::Part2),
        combine(dao.debtAdjustments(), dao.budgetAdjustments(), dao.noSpendDays(), dao.billSkips(), ::Part3),
    ) { p1, p2, p3 ->
        Snapshot(
            p1.a, p1.c, p1.cat, p1.t, p1.i, p2.y, p2.g, p2.b, p2.bu, p2.d, p3.adj,
            budgetAdjustments = p3.badj, noSpendDays = p3.nsd, billSkips = p3.skips,
        )
    }
        .stateIn(scope, SharingStarted.Eagerly, null)

    /** Fotografia de todos os dados, lida de uma vez só (numa transação, para não pegar nada pela metade). */
    suspend fun loadOnce(): Snapshot = db.withTransaction {
        Snapshot(
            dao.accountsOnce(), dao.cardsOnce(), dao.categoriesOnce(), dao.txsOnce(), dao.investmentsOnce(),
            dao.yieldsOnce(), dao.goalsOnce(), dao.billsOnce(), dao.budgetsOnce(), dao.debtsOnce(), dao.debtAdjustmentsOnce(),
            budgetAdjustments = dao.budgetAdjustmentsOnce(), noSpendDays = dao.noSpendDaysOnce(), billSkips = dao.billSkipsOnce(),
        )
    }

    /** Categorias padrão na primeira vez. Numa transação: nunca cria as categorias em dobro, mesmo junto com outra gravação. */
    suspend fun ensureSeeded() = db.withTransaction {
        if (dao.categoryCount() == 0) dao.putCategories(DefaultData.categories())
    }

    /* ---------------------------- lançamentos ---------------------------- */

    suspend fun saveTx(tx: Tx): Long = dao.putTx(tx)

    /** Compra no cartão, opcionalmente parcelada: cria uma movimentação por parcela, cada uma na sua fatura. */
    suspend fun saveCardPurchase(base: Tx, card: CreditCard, installments: Int, firstInvoice: YearMonth? = null): Tx {
        // RF-CAR-10: a primeira fatura pode vir escolhida pela pessoa; as parcelas seguem dela em diante.
        val first = firstInvoice ?: CardMath.invoiceFor(card, base.date)
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

    /** Várias gravações que entram juntas ou não entram (se algo falhar no meio, nada muda). */
    suspend fun <T> inTransaction(block: suspend () -> T): T = db.withTransaction(block)

    /** Grava as movimentações de um extrato importado, todas de uma vez. Devolve os ids (para desfazer). */
    suspend fun importTxs(txs: List<Tx>): List<Long> = db.withTransaction { txs.map { dao.putTx(it) } }

    /** Desfaz uma importação. */
    suspend fun undoImport(ids: List<Long>) = db.withTransaction { ids.chunked(500).forEach { dao.deleteTxs(it) } }

    /** Recoloca uma movimentação apagada (botão "Desfazer"). */
    suspend fun restore(txs: List<Tx>) = dao.putTxs(txs)

    suspend fun txById(id: Long): Tx? = dao.txById(id)

    /** Aviso de orçamento depois de registrar um gasto (80% e 100%). */
    suspend fun budgetAlert(categoryId: Long?, date: LocalDate, added: Long): String? {
        if (categoryId == null || added <= 0) return null
        val budget = dao.budgetsOnce().firstOrNull { it.categoryId == categoryId } ?: return null
        val ym = date.ym()
        // DAD-12: o limite deste mês, já com os remanejamentos.
        val key = ym.year * 100 + ym.monthValue
        val limit = (budget.limitAmount + dao.budgetAdjustmentsOnce().filter { it.ym == key && it.categoryId == categoryId }.sumOf { it.amount })
            .coerceAtLeast(0L)
        if (limit <= 0) return null
        val cat = dao.categoriesOnce().firstOrNull { it.id == categoryId } ?: return null
        val spent = dao.categorySpent(categoryId, ym.atDay(1), ym.atEndOfMonth())
        val before = spent - added
        val ratio = spent.toDouble() / limit
        val beforeRatio = before.toDouble() / limit
        return when {
            beforeRatio < 1.0 && ratio >= 1.0 ->
                Res.s(R.string.repo_orcamento_estourado, cat.name, Money.format(spent), Money.format(limit))
            beforeRatio < 0.8 && ratio >= 0.8 ->
                Res.s(R.string.repo_atencao_voce_ja_usou, (ratio * 100).roundToInt(), cat.name)
            else -> null
        }
    }

    /* ------------------------------- contas ------------------------------ */

    suspend fun saveAccount(a: Account): Long = dao.putAccount(a)

    /**
     * RF-IMP-24: acerta o saldo de uma conta somando [delta] ao **saldo inicial** dela (o que já havia antes do primeiro
     * lançamento). Não cria movimentação: o acerto não vira receita nem gasto de mês nenhum, e a previsão não o vê.
     * Devolve a conta como estava, para o desfazer; null se a conta não existe mais.
     */
    suspend fun shiftBalance(accountId: Long, delta: Long): Account? = db.withTransaction {
        val before = dao.accountsOnce().firstOrNull { it.id == accountId } ?: return@withTransaction null
        dao.putAccount(before.copy(initialBalance = before.initialBalance + delta))
        before
    }

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

    /**
     * RF-CAR-11: acerta o total da fatura com o do banco. A diferença entra como um lançamento de ajuste nessa fatura
     * (compra se faltava, crédito se sobrava), à vista no Extrato e apagável; nada é escondido nem reescrito.
     * Devolve o ajuste criado, ou null se o total já era esse.
     */
    suspend fun adjustInvoice(card: CreditCard, ym: YearMonth, currentTotal: Long, bankTotal: Long, date: LocalDate): Tx? {
        val diff = bankTotal - currentTotal
        if (diff == 0L || bankTotal < 0) return null
        val t = Tx(
            type = if (diff > 0) TxType.EXPENSE else TxType.INCOME, amount = kotlin.math.abs(diff), date = date,
            description = Res.s(R.string.card_adjust_name), cardId = card.id, invoiceYm = ym.key(),
        )
        return t.copy(id = saveTx(t))
    }

    suspend fun payInvoice(card: CreditCard, ym: YearMonth, amount: Long, accountId: Long, date: LocalDate) {
        dao.putTx(
            Tx(
                type = TxType.CARD_PAYMENT, amount = amount, date = date, description = Res.s(R.string.repo_fatura, card.name),
                accountId = accountId, cardId = card.id, invoiceYm = ym.key(),
            ),
        )
    }

    /* ----------------------------- categorias ---------------------------- */

    suspend fun saveCategory(c: Category): Long = dao.putCategory(c)

    /** Se a categoria já foi usada, ela é arquivada (some das listas, mas o histórico continua certo). */
    suspend fun deleteCategory(c: Category): Boolean = db.withTransaction {
        dao.deleteBudget(c.id)
        dao.deleteBudgetAdjustmentsOf(c.id) // senão os remanejamentos do mês ficavam soltos, sem orçamento
        if (dao.countTxForCategory(c.id) > 0) {
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

    /** DAD-14: guarda a ilustração e, para backups lidos por versões antigas, o emoji equivalente (o app não o mostra). */
    suspend fun saveGoal(g: Goal): Long {
        val art = io.github.h3yk0.cryo.domain.GoalArt.forGoal(g)
        return dao.putGoal(g.copy(art = art.key, emoji = art.legacy))
    }

    suspend fun deleteGoal(g: Goal) = db.withTransaction { dao.deleteTxsOfGoal(g.id); dao.deleteGoal(g.id) }

    /* ----------------------------- contas fixas -------------------------- */

    suspend fun saveBill(b: Bill): Long = dao.putBill(b)

    // DAD-16: apagar a conta fixa leva os meses pulados dela.
    suspend fun deleteBill(b: Bill) = db.withTransaction { dao.unlinkBill(b.id); dao.deleteBillSkipsOf(b.id); dao.deleteBill(b.id) }

    /** RF-FIX-12 (1.6): pula a conta fixa no mês (sem lançamento nenhum). */
    suspend fun skipBill(billId: Long, ym: YearMonth) = dao.putBillSkip(io.github.h3yk0.cryo.data.db.BillSkip(billId, ym.key()))

    /** RF-FIX-12: "Voltar a contar" (e o desfazer do pular). */
    suspend fun unskipBill(billId: Long, ym: YearMonth) = dao.deleteBillSkip(billId, ym.key())

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
        // Conferido e gravado juntos: dois toques seguidos (ou o botão da notificação junto com a tela) dão um pagamento só.
        return db.withTransaction { dao.billPaymentId(bill.id, ym.key()) ?: dao.putTx(tx) }
    }

    /**
     * RF-CFX-06 (ERR-045): marcar uma conta fixa como paga **olhando antes** o que já existe. Se houver uma movimentação
     * que o casador reconhece com certeza (mesmo nome e valor, perto do vencimento), ela é ligada à conta fixa; só
     * quando não há nenhuma é que nasce um lançamento novo. Todo caminho do app passa por aqui.
     */
    suspend fun markBillPaidLinking(bill: Bill, ym: YearMonth, amount: Long, date: LocalDate, accountId: Long?, card: CreditCard?): Long {
        val snap = loadOnce()
        val paid = snap.txs.filter { it.billId != null && it.billYm != null }.mapTo(HashSet()) { it.billId!! to it.billYm!! }
        val found = io.github.h3yk0.cryo.domain.BillMatcher.certain(listOf(bill), snap.txs, paid)[bill.id to ym.key()]
        if (found != null && linkBillPayment(found.id, bill, ym)) return found.id
        return markBillPaid(bill, ym, amount, date, accountId, card)
    }

    suspend fun unmarkBillPaid(bill: Bill, ym: YearMonth) = dao.deleteBillPayment(bill.id, ym.key())

    /**
     * RF-CFX: liga uma movimentação que já existe (veio do extrato ou foi digitada) como o pagamento da conta fixa no mês.
     * Nada é criado nem muda de conta. Conferido e gravado juntos: se o mês já tem pagamento, não liga outro.
     */
    suspend fun linkBillPayment(txId: Long, bill: Bill, ym: YearMonth): Boolean = db.withTransaction {
        if (dao.billPaymentId(bill.id, ym.key()) != null) false else { dao.setBill(txId, bill.id, ym.key()); true }
    }

    /** Desfaz a ligação e deixa a movimentação onde está. [never]: a pessoa disse que ela não é pagamento de conta fixa. */
    suspend fun unlinkBillPayment(txId: Long, never: Boolean) =
        dao.setBill(txId, null, if (never) io.github.h3yk0.cryo.domain.BillMatcher.NOT_A_BILL else null)

    /** RF-CFX-05, paga em dobro: fica a movimentação do extrato ([keepId]) como o pagamento e sai a marcada à mão. */
    suspend fun mergeBillPayment(keepId: Long, manual: Tx) = db.withTransaction {
        dao.deleteTxs(listOf(manual.id))
        dao.setBill(keepId, manual.billId, manual.billYm)
    }

    /* ----------------------------- orçamentos ---------------------------- */

    suspend fun saveBudget(categoryId: Long, limit: Long) = dao.putBudget(Budget(categoryId, limit))
    suspend fun deleteBudget(categoryId: Long) = db.withTransaction {
        dao.deleteBudget(categoryId)
        dao.deleteBudgetAdjustmentsOf(categoryId)
    }

    /**
     * RF-ORC-04, DAD-12: passa [amount] do orçamento de [from] para [to] só no mês [ym]. Devolve o grupo (para desfazer com
     * [undoBudgetMove]).
     */
    suspend fun moveBudget(from: Long, to: Long, amount: Long, ym: Int): String {
        require(amount > 0 && from != to)
        val group = java.util.UUID.randomUUID().toString()
        dao.putBudgetAdjustments(
            listOf(
                BudgetAdjustment(ym = ym, categoryId = from, amount = -amount, moveGroup = group),
                BudgetAdjustment(ym = ym, categoryId = to, amount = amount, moveGroup = group),
            ),
        )
        return group
    }

    /** Muda o limite de uma categoria só no mês [ym] (soma [delta]), sem mexer nos outros meses. */
    suspend fun adjustMonthLimit(categoryId: Long, ym: Int, delta: Long) {
        if (delta != 0L) dao.putBudgetAdjustments(listOf(BudgetAdjustment(ym = ym, categoryId = categoryId, amount = delta)))
    }

    suspend fun undoBudgetMove(group: String) = dao.deleteBudgetMove(group)

    suspend fun deleteBudgetAdjustment(a: BudgetAdjustment) =
        a.moveGroup?.let { dao.deleteBudgetMove(it) } ?: dao.deleteBudgetAdjustment(a.id)

    /* ---------------------------- dias sem gastos ---------------------------- */

    /** DAD-13, RF-AVI-03: "Dia sem gastos ✓". */
    suspend fun markNoSpend(date: java.time.LocalDate) = dao.putNoSpendDay(NoSpendDay(date))

    suspend fun unmarkNoSpend(date: java.time.LocalDate) = dao.deleteNoSpendDay(date)

    /* ------------------------------- dívidas ------------------------------ */

    suspend fun saveDebt(d: Debt): Long = dao.putDebt(d)

    /** Cadastra a dívida. Se o dinheiro do empréstimo entrou numa conta agora, registra essa entrada. */
    suspend fun createDebt(d: Debt, received: Long, accountId: Long?, date: LocalDate): Long = db.withTransaction {
        val id = dao.putDebt(d)
        if (received > 0 && accountId != null) {
            dao.putTx(
                Tx(
                    type = TxType.DEBT_IN, amount = received, date = date, description = Res.s(R.string.phrase_emprestimo, d.name),
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
                    description = (if (installment) Res.s(R.string.repo_parcela) else Res.s(R.string.repo_pagamento)) + debt.name,
                    categoryId = cat, accountId = accountId, debtId = debt.id,
                )
                txs += t.copy(id = dao.putTx(t))
            }
            if (extra > 0) {
                val t = Tx(
                    type = TxType.EXPENSE, amount = extra, date = date, description = Res.s(R.string.repo_juros_multa, debt.name),
                    categoryId = cat, accountId = accountId,
                )
                txs += t.copy(id = dao.putTx(t))
            }
            if (discount > 0) {
                val a = DebtAdjustment(debtId = debt.id, date = date, amount = -discount, note = Res.s(R.string.repo_desconto_quitacao))
                adjs += a.copy(id = dao.putDebtAdjustment(a))
            }
        }
        return Undo(txs, adjs)
    }

    /** Pegou mais dinheiro emprestado: entra na conta e aumenta a dívida. */
    suspend fun borrowMore(debt: Debt, amount: Long, accountId: Long, date: LocalDate): Undo {
        val t = Tx(
            type = TxType.DEBT_IN, amount = amount, date = date, description = Res.s(R.string.phrase_emprestimo, debt.name),
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
        dao.putBudgetAdjustments(s.budgetAdjustments); dao.putNoSpendDays(s.noSpendDays); dao.putBillSkips(s.billSkips)
        if (fromOldVersion) ensureDebtCategory()
    }

    suspend fun eraseAll() = db.withTransaction {
        clearTables()
        dao.putCategories(DefaultData.categories())
    }

    private suspend fun clearTables() {
        dao.clearTxs(); dao.clearAccounts(); dao.clearCards(); dao.clearCategories(); dao.clearInvestments()
        dao.clearYields(); dao.clearGoals(); dao.clearBills(); dao.clearBudgets(); dao.clearDebts()
        dao.clearDebtAdjustments(); dao.clearBudgetAdjustments(); dao.clearNoSpendDays(); dao.clearBillSkips()
    }
}
