// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import io.github.h3yk0.cryo.data.DefaultData
import io.github.h3yk0.cryo.data.db.CategoryKind
import io.github.h3yk0.cryo.data.db.CryoDatabase
import io.github.h3yk0.cryo.data.db.Debt
import io.github.h3yk0.cryo.domain.debtCategory
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.time.LocalDate

/**
 * Quem atualiza da 1.0 para a 1.1 não pode perder nada: monta um banco exatamente como o da 1.0
 * (a partir do esquema salvo em app/schemas/.../1.json) e abre com a versão atual.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = TestApp::class)
class MigrationTest {
    private val ctx = ApplicationProvider.getApplicationContext<Context>()

    private fun createVersion1(file: File) {
        val schema = JSONObject(File("schemas/io.github.h3yk0.cryo.data.db.CryoDatabase/1.json").readText()).getJSONObject("database")
        assertEquals(1, schema.getInt("version"))
        file.parentFile?.mkdirs()
        val db = SQLiteDatabase.openOrCreateDatabase(file, null)
        val entities = schema.getJSONArray("entities")
        for (i in 0 until entities.length()) {
            val e = entities.getJSONObject(i)
            val table = e.getString("tableName")
            db.execSQL(e.getString("createSql").replace("\${TABLE_NAME}", table))
            val idx = e.optJSONArray("indices") ?: continue
            for (j in 0 until idx.length()) db.execSQL(idx.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}", table))
        }
        val setup = schema.getJSONArray("setupQueries")
        for (i in 0 until setup.length()) db.execSQL(setup.getString(i))

        // Dados de quem usava a 1.0: categorias padrão daquela versão (sem "Dívidas") e um gasto.
        DefaultData.categories().filter { it.icon != DefaultData.DEBT_ICON }.forEach { c ->
            db.insert(
                "categories", null,
                ContentValues().apply {
                    put("name", c.name); put("kind", c.kind.name); put("icon", c.icon); put("color", c.color)
                    put("keywords", c.keywords); put("archived", 0)
                    // na 1.0, "Outros gastos" vinha logo depois de "Impostos e taxas"
                    put("sortOrder", if (c.kind == CategoryKind.EXPENSE && c.sortOrder > 16) c.sortOrder - 1 else c.sortOrder)
                },
            )
        }
        db.insert(
            "accounts", null,
            ContentValues().apply {
                put("name", "Conta"); put("type", "CHECKING"); put("initialBalance", 100_000); put("color", 0)
                put("includeInTotal", 1); put("archived", 0); put("sortOrder", 0)
            },
        )
        db.insert(
            "transactions", null,
            ContentValues().apply {
                put("type", "EXPENSE"); put("amount", 2_500); put("date", LocalDate.of(2026, 9, 1).toEpochDay())
                put("description", "Almoço"); put("accountId", 1); put("installmentNumber", 0); put("installmentTotal", 0)
                put("note", ""); put("createdAt", 0)
            },
        )
        db.version = 1
        db.close()
    }

    @Test fun atualizaDa10Para11SemPerderNada() = runBlocking {
        val file = ctx.getDatabasePath("cryo.db")
        file.delete()
        createVersion1(file)

        val room = CryoDatabase.build(ctx)
        val dao = room.dao()
        val tx = dao.txById(1)
        assertNotNull(tx)
        assertEquals(2_500L, tx!!.amount)
        assertEquals("Almoço", tx.description)
        assertNull(tx.debtId)
        assertEquals(1, dao.accountsOnce().size)

        // a categoria de dívidas foi criada, uma vez só, antes de "Outros gastos"
        val cats = dao.categoriesOnce()
        val debtCat = cats.debtCategory()
        assertNotNull(debtCat)
        assertEquals(1, cats.count { it.icon == DefaultData.DEBT_ICON })
        val expense = cats.filter { it.kind == CategoryKind.EXPENSE }.sortedBy { it.sortOrder }
        assertEquals("Outros gastos", expense.last().name)
        assertEquals(debtCat!!.id, expense[expense.size - 2].id)

        // as tabelas novas funcionam
        val id = dao.putDebt(Debt(name = "Teste", color = 0, initialBalance = 1_000, startYm = 202610))
        assertEquals("Teste", dao.debtsOnce().single { it.id == id }.name)
        room.close()

        // abrir de novo não duplica a categoria
        val again = CryoDatabase.build(ctx)
        assertEquals(1, again.dao().categoriesOnce().count { it.icon == DefaultData.DEBT_ICON })
        again.close()
    }
}
