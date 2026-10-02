// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.data.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.AutoMigration
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.AutoMigrationSpec
import androidx.sqlite.db.SupportSQLiteDatabase
import io.github.h3yk0.cryo.data.DefaultData
import io.github.h3yk0.cryo.domain.normalized
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class Converters {
    @TypeConverter fun dateToLong(d: LocalDate?): Long? = d?.toEpochDay()
    @TypeConverter fun longToDate(v: Long?): LocalDate? = v?.let(LocalDate::ofEpochDay)
}

@Dao
interface CryoDao {
    // ---------- leitura reativa (a tela se atualiza sozinha) ----------
    @Query("SELECT * FROM accounts ORDER BY sortOrder, id") fun accounts(): Flow<List<Account>>
    @Query("SELECT * FROM cards ORDER BY id") fun cards(): Flow<List<CreditCard>>
    @Query("SELECT * FROM categories ORDER BY sortOrder, id") fun categories(): Flow<List<Category>>
    @Query("SELECT * FROM transactions ORDER BY date DESC, id DESC") fun txs(): Flow<List<Tx>>
    @Query("SELECT * FROM investments ORDER BY id") fun investments(): Flow<List<Investment>>
    @Query("SELECT * FROM investment_yields ORDER BY date DESC, id DESC") fun yields(): Flow<List<InvestmentYield>>
    @Query("SELECT * FROM goals ORDER BY id") fun goals(): Flow<List<Goal>>
    @Query("SELECT * FROM bills ORDER BY dueDay, name") fun bills(): Flow<List<Bill>>
    @Query("SELECT * FROM budgets") fun budgets(): Flow<List<Budget>>
    @Query("SELECT * FROM debts ORDER BY id") fun debts(): Flow<List<Debt>>
    @Query("SELECT * FROM debt_adjustments ORDER BY date DESC, id DESC") fun debtAdjustments(): Flow<List<DebtAdjustment>>

    // ---------- leitura única (lembretes e backup) ----------
    @Query("SELECT * FROM accounts ORDER BY sortOrder, id") suspend fun accountsOnce(): List<Account>
    @Query("SELECT * FROM cards ORDER BY id") suspend fun cardsOnce(): List<CreditCard>
    @Query("SELECT * FROM categories ORDER BY sortOrder, id") suspend fun categoriesOnce(): List<Category>
    @Query("SELECT * FROM transactions ORDER BY date DESC, id DESC") suspend fun txsOnce(): List<Tx>
    @Query("SELECT * FROM investments ORDER BY id") suspend fun investmentsOnce(): List<Investment>
    @Query("SELECT * FROM investment_yields ORDER BY date DESC, id DESC") suspend fun yieldsOnce(): List<InvestmentYield>
    @Query("SELECT * FROM goals ORDER BY id") suspend fun goalsOnce(): List<Goal>
    @Query("SELECT * FROM bills ORDER BY dueDay, name") suspend fun billsOnce(): List<Bill>
    @Query("SELECT * FROM budgets") suspend fun budgetsOnce(): List<Budget>
    @Query("SELECT * FROM debts ORDER BY id") suspend fun debtsOnce(): List<Debt>
    @Query("SELECT * FROM debt_adjustments ORDER BY date DESC, id DESC") suspend fun debtAdjustmentsOnce(): List<DebtAdjustment>

    // ---------- gravação ----------
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putAccount(a: Account): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putAccounts(a: List<Account>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putCard(c: CreditCard): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putCards(c: List<CreditCard>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putCategory(c: Category): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putCategories(c: List<Category>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putTx(t: Tx): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putTxs(t: List<Tx>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putInvestment(i: Investment): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putInvestments(i: List<Investment>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putYield(y: InvestmentYield): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putYields(y: List<InvestmentYield>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putGoal(g: Goal): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putGoals(g: List<Goal>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putBill(b: Bill): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putBills(b: List<Bill>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putBudget(b: Budget)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putBudgets(b: List<Budget>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putDebt(d: Debt): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putDebts(d: List<Debt>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putDebtAdjustment(a: DebtAdjustment): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putDebtAdjustments(a: List<DebtAdjustment>)

    // ---------- exclusão ----------
    @Query("DELETE FROM transactions WHERE id = :id") suspend fun deleteTx(id: Long)
    @Query("DELETE FROM transactions WHERE installmentGroup = :group") suspend fun deleteTxGroup(group: String)
    @Query("DELETE FROM transactions WHERE billId = :billId AND billYm = :ym") suspend fun deleteBillPayment(billId: Long, ym: Int)
    @Query("DELETE FROM accounts WHERE id = :id") suspend fun deleteAccount(id: Long)
    @Query("DELETE FROM cards WHERE id = :id") suspend fun deleteCard(id: Long)
    @Query("DELETE FROM categories WHERE id = :id") suspend fun deleteCategory(id: Long)
    @Query("DELETE FROM investments WHERE id = :id") suspend fun deleteInvestment(id: Long)
    @Query("DELETE FROM investment_yields WHERE investmentId = :id") suspend fun deleteYieldsOf(id: Long)
    @Query("DELETE FROM investment_yields WHERE id = :id") suspend fun deleteYield(id: Long)
    @Query("DELETE FROM transactions WHERE investmentId = :id") suspend fun deleteTxsOfInvestment(id: Long)
    @Query("DELETE FROM goals WHERE id = :id") suspend fun deleteGoal(id: Long)
    @Query("DELETE FROM transactions WHERE goalId = :id") suspend fun deleteTxsOfGoal(id: Long)
    @Query("DELETE FROM bills WHERE id = :id") suspend fun deleteBill(id: Long)
    @Query("UPDATE transactions SET billId = NULL, billYm = NULL WHERE billId = :id") suspend fun unlinkBill(id: Long)
    @Query("DELETE FROM budgets WHERE categoryId = :id") suspend fun deleteBudget(id: Long)
    @Query("DELETE FROM debts WHERE id = :id") suspend fun deleteDebt(id: Long)
    @Query("DELETE FROM debt_adjustments WHERE debtId = :id") suspend fun deleteAdjustmentsOf(id: Long)
    @Query("DELETE FROM debt_adjustments WHERE id = :id") suspend fun deleteDebtAdjustment(id: Long)
    @Query("UPDATE transactions SET debtId = NULL WHERE debtId = :id") suspend fun unlinkDebt(id: Long)

    // ---------- consultas auxiliares ----------
    @Query("SELECT COUNT(*) FROM transactions WHERE accountId = :id OR toAccountId = :id") suspend fun countTxForAccount(id: Long): Int
    @Query("SELECT COUNT(*) FROM transactions WHERE cardId = :id") suspend fun countTxForCard(id: Long): Int
    @Query("SELECT COUNT(*) FROM transactions WHERE categoryId = :id") suspend fun countTxForCategory(id: Long): Int
    @Query("SELECT COUNT(*) FROM categories") suspend fun categoryCount(): Int
    @Query("SELECT * FROM transactions WHERE id = :id") suspend fun txById(id: Long): Tx?
    @Query(
        "SELECT COALESCE(SUM(amount), 0) FROM transactions " +
            "WHERE type = 'EXPENSE' AND categoryId = :cat AND date BETWEEN :start AND :end",
    )
    suspend fun categorySpent(cat: Long, start: LocalDate, end: LocalDate): Long

    @Query("DELETE FROM accounts") suspend fun clearAccounts()
    @Query("DELETE FROM cards") suspend fun clearCards()
    @Query("DELETE FROM categories") suspend fun clearCategories()
    @Query("DELETE FROM transactions") suspend fun clearTxs()
    @Query("DELETE FROM investments") suspend fun clearInvestments()
    @Query("DELETE FROM investment_yields") suspend fun clearYields()
    @Query("DELETE FROM goals") suspend fun clearGoals()
    @Query("DELETE FROM bills") suspend fun clearBills()
    @Query("DELETE FROM budgets") suspend fun clearBudgets()
    @Query("DELETE FROM debts") suspend fun clearDebts()
    @Query("DELETE FROM debt_adjustments") suspend fun clearDebtAdjustments()
}

/**
 * Versão 1 → 2 (Cryo 1.1): o Room cria as tabelas de dívidas e a coluna debtId sozinho
 * (AutoMigration, comparando os esquemas salvos em app/schemas). Depois disso, aqui
 * adicionamos a categoria "Dívidas e empréstimos" para quem já usava o app.
 */
class Migration1To2 : AutoMigrationSpec {
    override fun onPostMigrate(db: SupportSQLiteDatabase) = DebtCategorySeed.ensure(db)
}

object DebtCategorySeed {
    /** Adiciona a categoria de dívidas se ainda não existir nenhuma parecida. */
    fun ensure(db: SupportSQLiteDatabase) {
        var exists = false
        db.query("SELECT name, icon FROM categories WHERE kind = 'EXPENSE'").use { c ->
            while (c.moveToNext()) {
                if (c.getString(1) == DefaultData.DEBT_ICON || c.getString(0).normalized().contains("divida")) exists = true
            }
        }
        if (exists) return
        val cat = DefaultData.debtCategory()
        db.execSQL("UPDATE categories SET sortOrder = sortOrder + 1 WHERE kind = 'EXPENSE' AND sortOrder >= ${cat.sortOrder}")
        db.insert(
            "categories", SQLiteDatabase.CONFLICT_NONE,
            ContentValues().apply {
                put("name", cat.name)
                put("kind", cat.kind.name)
                put("icon", cat.icon)
                put("color", cat.color)
                put("keywords", cat.keywords)
                put("archived", 0)
                put("sortOrder", cat.sortOrder)
            },
        )
    }
}

@Database(
    entities = [
        Account::class, CreditCard::class, Category::class, Tx::class, Investment::class,
        InvestmentYield::class, Goal::class, Bill::class, Budget::class, Debt::class, DebtAdjustment::class,
    ],
    version = 2,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2, spec = Migration1To2::class)],
)
@TypeConverters(Converters::class)
abstract class CryoDatabase : RoomDatabase() {
    abstract fun dao(): CryoDao

    companion object {
        fun build(context: Context): CryoDatabase =
            Room.databaseBuilder(context, CryoDatabase::class.java, "cryo.db").build()
    }
}
