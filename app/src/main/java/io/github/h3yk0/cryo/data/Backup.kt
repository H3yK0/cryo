// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.data

import io.github.h3yk0.cryo.data.db.Account
import io.github.h3yk0.cryo.data.db.AccountType
import io.github.h3yk0.cryo.data.db.Bill
import io.github.h3yk0.cryo.data.db.Budget
import io.github.h3yk0.cryo.data.db.Category
import io.github.h3yk0.cryo.data.db.CategoryKind
import io.github.h3yk0.cryo.data.db.CreditCard
import io.github.h3yk0.cryo.data.db.Goal
import io.github.h3yk0.cryo.data.db.Investment
import io.github.h3yk0.cryo.data.db.InvestmentKind
import io.github.h3yk0.cryo.data.db.InvestmentYield
import io.github.h3yk0.cryo.data.db.Tx
import io.github.h3yk0.cryo.data.db.TxType
import io.github.h3yk0.cryo.domain.Ledger
import io.github.h3yk0.cryo.domain.Snapshot
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/** Cópia de segurança em JSON (tudo) e exportação em CSV (planilhas). */
object Backup {
    private const val VERSION = 1

    private fun JSONObject.optLongOrNull(k: String): Long? = if (isNull(k) || !has(k)) null else getLong(k)
    private fun JSONObject.optIntOrNull(k: String): Int? = if (isNull(k) || !has(k)) null else getInt(k)
    private fun JSONObject.optStr(k: String): String? = if (isNull(k) || !has(k)) null else getString(k)
    private fun <T> JSONArray.mapObj(f: (JSONObject) -> T): List<T> = (0 until length()).map { f(getJSONObject(it)) }
    private fun <T> List<T>.toJson(f: (T) -> JSONObject) = JSONArray().also { arr -> forEach { arr.put(f(it)) } }
    private fun Any?.orNull(): Any = this ?: JSONObject.NULL

    fun export(s: Snapshot): String {
        val root = JSONObject()
        root.put("app", "Cryo")
        root.put("version", VERSION)
        root.put("exportedAt", System.currentTimeMillis())
        root.put("accounts", s.accounts.toJson {
            JSONObject().put("id", it.id).put("name", it.name).put("type", it.type.name)
                .put("initialBalance", it.initialBalance).put("color", it.color).put("includeInTotal", it.includeInTotal)
                .put("archived", it.archived).put("sortOrder", it.sortOrder)
        })
        root.put("cards", s.cards.toJson {
            JSONObject().put("id", it.id).put("name", it.name).put("limitAmount", it.limitAmount)
                .put("closingDay", it.closingDay).put("dueDay", it.dueDay).put("color", it.color)
                .put("payAccountId", it.payAccountId.orNull()).put("archived", it.archived)
        })
        root.put("categories", s.categories.toJson {
            JSONObject().put("id", it.id).put("name", it.name).put("kind", it.kind.name).put("icon", it.icon)
                .put("color", it.color).put("keywords", it.keywords).put("archived", it.archived).put("sortOrder", it.sortOrder)
        })
        root.put("transactions", s.txs.toJson {
            JSONObject().put("id", it.id).put("type", it.type.name).put("amount", it.amount)
                .put("date", it.date.toString()).put("description", it.description)
                .put("categoryId", it.categoryId.orNull()).put("accountId", it.accountId.orNull())
                .put("toAccountId", it.toAccountId.orNull()).put("cardId", it.cardId.orNull())
                .put("invoiceYm", it.invoiceYm.orNull()).put("installmentGroup", it.installmentGroup.orNull())
                .put("installmentNumber", it.installmentNumber).put("installmentTotal", it.installmentTotal)
                .put("investmentId", it.investmentId.orNull()).put("goalId", it.goalId.orNull())
                .put("billId", it.billId.orNull()).put("billYm", it.billYm.orNull()).put("note", it.note)
                .put("createdAt", it.createdAt)
        })
        root.put("investments", s.investments.toJson {
            JSONObject().put("id", it.id).put("name", it.name).put("kind", it.kind.name).put("color", it.color)
                .put("archived", it.archived).put("createdAt", it.createdAt)
        })
        root.put("yields", s.yields.toJson {
            JSONObject().put("id", it.id).put("investmentId", it.investmentId).put("date", it.date.toString())
                .put("amount", it.amount).put("note", it.note)
        })
        root.put("goals", s.goals.toJson {
            JSONObject().put("id", it.id).put("name", it.name).put("target", it.target)
                .put("deadline", it.deadline?.toString().orNull()).put("emoji", it.emoji).put("color", it.color)
                .put("archived", it.archived).put("createdAt", it.createdAt)
        })
        root.put("bills", s.bills.toJson {
            JSONObject().put("id", it.id).put("name", it.name).put("amount", it.amount).put("dueDay", it.dueDay)
                .put("kind", it.kind.name).put("categoryId", it.categoryId.orNull()).put("accountId", it.accountId.orNull())
                .put("cardId", it.cardId.orNull()).put("remind", it.remind).put("remindDaysBefore", it.remindDaysBefore)
                .put("active", it.active).put("startYm", it.startYm).put("createdAt", it.createdAt)
        })
        root.put("budgets", s.budgets.toJson {
            JSONObject().put("categoryId", it.categoryId).put("limitAmount", it.limitAmount)
        })
        return root.toString(1)
    }

    /** Lê um backup. Lança exceção com mensagem amigável se o arquivo não for do Cryo. */
    fun import(json: String): Snapshot {
        val root = try { JSONObject(json) } catch (e: Exception) { throw IllegalArgumentException("O arquivo não é um backup válido.") }
        if (root.optString("app") != "Cryo") throw IllegalArgumentException("Este arquivo não é um backup do Cryo.")
        return Snapshot(
            accounts = root.getJSONArray("accounts").mapObj {
                Account(
                    id = it.getLong("id"), name = it.getString("name"), type = AccountType.valueOf(it.getString("type")),
                    initialBalance = it.getLong("initialBalance"), color = it.getInt("color"),
                    includeInTotal = it.optBoolean("includeInTotal", true), archived = it.optBoolean("archived"),
                    sortOrder = it.optInt("sortOrder"),
                )
            },
            cards = root.getJSONArray("cards").mapObj {
                CreditCard(
                    id = it.getLong("id"), name = it.getString("name"), limitAmount = it.getLong("limitAmount"),
                    closingDay = it.getInt("closingDay"), dueDay = it.getInt("dueDay"), color = it.getInt("color"),
                    payAccountId = it.optLongOrNull("payAccountId"), archived = it.optBoolean("archived"),
                )
            },
            categories = root.getJSONArray("categories").mapObj {
                Category(
                    id = it.getLong("id"), name = it.getString("name"), kind = CategoryKind.valueOf(it.getString("kind")),
                    icon = it.getString("icon"), color = it.getInt("color"), keywords = it.optString("keywords"),
                    archived = it.optBoolean("archived"), sortOrder = it.optInt("sortOrder"),
                )
            },
            txs = root.getJSONArray("transactions").mapObj {
                Tx(
                    id = it.getLong("id"), type = TxType.valueOf(it.getString("type")), amount = it.getLong("amount"),
                    date = LocalDate.parse(it.getString("date")), description = it.optString("description"),
                    categoryId = it.optLongOrNull("categoryId"), accountId = it.optLongOrNull("accountId"),
                    toAccountId = it.optLongOrNull("toAccountId"), cardId = it.optLongOrNull("cardId"),
                    invoiceYm = it.optIntOrNull("invoiceYm"), installmentGroup = it.optStr("installmentGroup"),
                    installmentNumber = it.optInt("installmentNumber"), installmentTotal = it.optInt("installmentTotal"),
                    investmentId = it.optLongOrNull("investmentId"), goalId = it.optLongOrNull("goalId"),
                    billId = it.optLongOrNull("billId"), billYm = it.optIntOrNull("billYm"), note = it.optString("note"),
                    createdAt = it.optLong("createdAt"),
                )
            },
            investments = root.getJSONArray("investments").mapObj {
                Investment(
                    id = it.getLong("id"), name = it.getString("name"), kind = InvestmentKind.valueOf(it.getString("kind")),
                    color = it.getInt("color"), archived = it.optBoolean("archived"), createdAt = it.optLong("createdAt"),
                )
            },
            yields = root.getJSONArray("yields").mapObj {
                InvestmentYield(
                    id = it.getLong("id"), investmentId = it.getLong("investmentId"),
                    date = LocalDate.parse(it.getString("date")), amount = it.getLong("amount"), note = it.optString("note"),
                )
            },
            goals = root.getJSONArray("goals").mapObj {
                Goal(
                    id = it.getLong("id"), name = it.getString("name"), target = it.getLong("target"),
                    deadline = it.optStr("deadline")?.let(LocalDate::parse), emoji = it.optString("emoji", "🎯"),
                    color = it.getInt("color"), archived = it.optBoolean("archived"), createdAt = it.optLong("createdAt"),
                )
            },
            bills = root.getJSONArray("bills").mapObj {
                Bill(
                    id = it.getLong("id"), name = it.getString("name"), amount = it.getLong("amount"),
                    dueDay = it.getInt("dueDay"), kind = CategoryKind.valueOf(it.getString("kind")),
                    categoryId = it.optLongOrNull("categoryId"), accountId = it.optLongOrNull("accountId"),
                    cardId = it.optLongOrNull("cardId"), remind = it.optBoolean("remind", true),
                    remindDaysBefore = it.optInt("remindDaysBefore", 1), active = it.optBoolean("active", true),
                    startYm = it.getInt("startYm"), createdAt = it.optLong("createdAt"),
                )
            },
            budgets = root.getJSONArray("budgets").mapObj {
                Budget(categoryId = it.getLong("categoryId"), limitAmount = it.getLong("limitAmount"))
            },
        )
    }

    /** CSV com ";" (abre direto no Excel/Planilhas em português). */
    fun csv(s: Snapshot, today: LocalDate): String {
        val l = Ledger(s, today)
        fun esc(v: String) = if (v.contains(';') || v.contains('"') || v.contains('\n')) "\"" + v.replace("\"", "\"\"") + "\"" else v
        fun money(c: Long): String {
            val a = kotlin.math.abs(c)
            return (if (c < 0) "-" else "") + "${a / 100},${(a % 100).toString().padStart(2, '0')}"
        }
        val typeName = mapOf(
            TxType.EXPENSE to "Despesa", TxType.INCOME to "Receita", TxType.TRANSFER to "Transferência",
            TxType.CARD_PAYMENT to "Pagamento de fatura", TxType.INVEST_IN to "Aporte", TxType.INVEST_OUT to "Resgate",
            TxType.GOAL_IN to "Guardado em meta", TxType.GOAL_OUT to "Retirado de meta",
        )
        val sb = StringBuilder("﻿")
        sb.append("Data;Tipo;Descrição;Categoria;Conta;Destino;Cartão;Parcela;Valor;Observação\n")
        for (t in s.txs.sortedBy { it.date }) {
            val signed = when (t.type) {
                TxType.EXPENSE, TxType.CARD_PAYMENT, TxType.INVEST_IN, TxType.GOAL_IN -> -t.amount
                TxType.TRANSFER -> t.amount
                else -> t.amount
            }
            val dest = when (t.type) {
                TxType.TRANSFER -> l.accountName(t.toAccountId)
                TxType.INVEST_IN, TxType.INVEST_OUT -> t.investmentId?.let { l.investment[it]?.name } ?: ""
                TxType.GOAL_IN, TxType.GOAL_OUT -> t.goalId?.let { l.goal[it]?.name } ?: ""
                else -> ""
            }
            sb.append(
                listOf(
                    t.date.toString(), typeName.getValue(t.type), esc(t.description),
                    esc(t.categoryId?.let { l.category[it]?.name } ?: ""),
                    esc(t.accountId?.let { l.account[it]?.name } ?: ""), esc(dest),
                    esc(t.cardId?.let { l.card[it]?.name } ?: ""),
                    if (t.installmentTotal > 1) "${t.installmentNumber}/${t.installmentTotal}" else "",
                    money(signed), esc(t.note),
                ).joinToString(";"),
            ).append('\n')
        }
        return sb.toString()
    }
}
