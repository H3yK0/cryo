// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.importer

import io.github.h3yk0.cryo.R
import io.github.h3yk0.cryo.domain.Res
import org.json.JSONArray
import org.json.JSONObject

/**
 * Formato JSON de testes (modo desenvolvedor). Documentado em docs/IMPORTACAO.md:
 *
 * {
 *   "cryoImport": 1,
 *   "card": false,
 *   "account": "Banco de teste",
 *   "transactions": [
 *     { "date": "2026-10-01", "description": "Padaria", "amount": -12.50, "id": "t1" }
 *   ]
 * }
 *
 * "amount" em reais: negativo = saiu, positivo = entrou. Também aceita só a lista de movimentações.
 */
object JsonStatement {
    const val VERSION = 1

    fun looksLike(text: String): Boolean = text.trimStart().let { it.startsWith("{") || it.startsWith("[") }

    fun parse(text: String): Statement {
        val trimmed = text.trim()
        val root: JSONObject?
        val items: JSONArray
        try {
            if (trimmed.startsWith("[")) {
                root = null
                items = JSONArray(trimmed)
            } else {
                root = JSONObject(trimmed)
                if (root.optString("app") == "Cryo") {
                    throw StatementException(Res.s(R.string.importer_este_arquivo_backup_cryo))
                }
                items = root.optJSONArray("transactions")
                    ?: throw StatementException(Res.s(R.string.importer_json_precisa_ter_lista))
            }
        } catch (e: StatementException) {
            throw e
        } catch (e: Exception) {
            throw StatementException(Res.s(R.string.importer_esse_json_nao_esta))
        }
        val version = root?.optInt("cryoImport", VERSION) ?: VERSION
        if (version > VERSION) throw StatementException(Res.s(R.string.importer_esse_json_versao_mais))

        val lines = ArrayList<StatementLine>()
        val skipped = ArrayList<SkippedLine>()
        for (i in 0 until items.length()) {
            val o = items.optJSONObject(i)
            if (o == null) { skipped += SkippedLine(items.opt(i).toString(), Res.s(R.string.importer_nao_objeto)); continue }
            val source = o.toString()
            val date = o.optString("date").let { Values.date(it) }
            val raw = o.opt("amount")
            val cents = when (raw) {
                is Number -> Values.cents(raw.toDouble())
                is String -> Values.amount(raw)
                else -> null
            }
            when {
                date == null -> skipped += SkippedLine(source, Res.s(R.string.importer_date_ausente_ou_invalida))
                cents == null -> skipped += SkippedLine(source, Res.s(R.string.importer_amount_ausente_ou_invalido))
                cents == 0L -> skipped += SkippedLine(source, Res.s(R.string.importer_valor_zero))
                else -> lines += StatementLine(
                    date, o.optString("description").trim().ifBlank { Res.s(R.string.importer_movimentacao) }, cents,
                    o.optString("id").trim().takeIf { it.isNotEmpty() }, source,
                )
            }
        }
        return Statement(
            StatementFormat.JSON, lines, skipped,
            isCard = root?.optBoolean("card", false) ?: false,
            accountHint = root?.optString("account")?.takeIf { it.isNotBlank() },
            notes = listOf(Res.s(R.string.importer_formato_testes_cryo_versao, version)),
        )
    }

    /** Monta um JSON de testes (usado pelos arquivos de exemplo do modo desenvolvedor). */
    fun write(lines: List<StatementLine>, account: String, card: Boolean): String {
        val arr = JSONArray()
        lines.forEach { l ->
            arr.put(
                JSONObject().put("date", l.date.toString()).put("description", l.description)
                    .put("amount", l.amount / 100.0).apply { l.id?.let { put("id", it) } },
            )
        }
        return JSONObject().put("cryoImport", VERSION).put("card", card).put("account", account)
            .put("transactions", arr).toString(2)
    }
}
