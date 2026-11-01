// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.importer

import kotlin.math.abs

/**
 * OFX: o formato de extrato que quase todo banco oferece ("Money", "Quicken", "OFX").
 * Entende as duas variantes: a antiga (SGML, sem fechar as tags) e a nova (XML).
 */
object Ofx {
    private val TOKEN = Regex("""<(/?)([A-Za-z0-9.]+)\s*>([^<]*)""")

    fun looksLike(text: String): Boolean {
        val head = text.take(2048).uppercase()
        return head.contains("OFXHEADER") || head.contains("<OFX>")
    }

    fun parse(text: String, encodingNote: String? = null): Statement {
        val lines = ArrayList<StatementLine>()
        val skipped = ArrayList<SkippedLine>()
        var current: MutableMap<String, String>? = null
        var isCard = false
        var bankId: String? = null
        var acctId: String? = null
        var org: String? = null
        var balance: Long? = null
        var inLedgerBal = false

        fun emit(m: Map<String, String>) {
            val source = m.entries.joinToString(" ") { "<${it.key}>${it.value}" }
            val date = Values.ofxDate(m["DTPOSTED"] ?: m["DTUSER"])
            val raw = m["TRNAMT"]
            var cents = Values.amount(raw, decimalComma = raw?.contains(',') == true && raw.contains('.').not())
            if (date == null || cents == null) {
                skipped += SkippedLine(source, if (date == null) "sem data" else "sem valor")
                return
            }
            // O sinal do valor manda; só corrige quando o tipo é claramente débito ou crédito.
            when (m["TRNTYPE"]?.uppercase()) {
                "DEBIT" -> cents = -abs(cents)
                "CREDIT" -> cents = abs(cents)
            }
            val name = m["NAME"]?.let(::unescape)?.trim().orEmpty()
            val memo = m["MEMO"]?.let(::unescape)?.trim().orEmpty()
            val description = when {
                memo.isEmpty() -> name
                name.isEmpty() -> memo
                memo.contains(name, ignoreCase = true) -> memo
                name.contains(memo, ignoreCase = true) -> name
                else -> "$name $memo"
            }.ifBlank { m["TRNTYPE"] ?: "Movimentação" }
            // Como nos outros formatos: alguns bancos põem o saldo como se fosse uma movimentação ("SALDO ANTERIOR",
            // "S A L D O"), às vezes com o valor do saldo; isso não é dinheiro entrando nem saindo.
            if (cents == 0L) { skipped += SkippedLine(source, "valor zero"); return }
            if (Values.isBalanceOrTotal(description)) { skipped += SkippedLine(source, "linha de saldo ou total"); return }
            lines += StatementLine(date, Values.prettify(description), cents, m["FITID"]?.trim()?.takeIf { it.isNotEmpty() }, source)
        }

        for (t in TOKEN.findAll(text)) {
            val closing = t.groupValues[1] == "/"
            val tag = t.groupValues[2].uppercase()
            val value = t.groupValues[3].trim()
            when {
                tag == "STMTTRN" && !closing -> {
                    current?.let(::emit) // OFX malformado: abriu outra sem fechar a anterior
                    current = HashMap()
                }
                tag == "STMTTRN" && closing -> { current?.let(::emit); current = null }
                closing -> if (tag == "LEDGERBAL") inLedgerBal = false
                tag == "CCSTMTRS" || tag == "CCACCTFROM" -> isCard = true
                tag == "LEDGERBAL" -> inLedgerBal = true
                current != null -> if (value.isNotEmpty()) current!![tag] = value
                value.isEmpty() -> Unit
                tag == "BANKID" -> bankId = value
                tag == "ACCTID" -> acctId = value
                tag == "ORG" -> org = unescape(value)
                tag == "BALAMT" && inLedgerBal -> balance = Values.amount(value, decimalComma = value.contains(',') && !value.contains('.'))
            }
        }
        current?.let(::emit)

        val hint = listOfNotNull(
            org?.takeIf { it.isNotBlank() } ?: bankId?.let { "Banco $it" },
            acctId?.let { "conta $it" },
        ).joinToString(" · ").ifBlank { null }
        val notes = listOfNotNull(
            "Formato OFX " + if (text.contains("<?xml", ignoreCase = true) || text.contains("</NAME>", ignoreCase = true)) "(XML)" else "(SGML)",
            encodingNote?.let { "Codificação: $it" },
            if (isCard) "Extrato de cartão de crédito" else "Extrato de conta",
        )
        return Statement(StatementFormat.OFX, lines, skipped, isCard, hint, balance, notes)
    }

    private fun unescape(s: String) = s.replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"")
        .replace("&apos;", "'").replace("&amp;", "&")
}
