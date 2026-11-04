// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.importer

/**
 * Tabela de uma página HTML. Muitos bancos chamam de "Excel (.xls)" um arquivo que, por dentro, é uma página da
 * internet com uma tabela: o Cryo lê essa tabela como se fosse uma planilha.
 */
object HtmlTable {
    private val OPTS = setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    private val SCRIPT = Regex("<(script|style)\\b.*?</\\1\\s*>", OPTS)
    private val ROW_START = Regex("<tr\\b[^>]*>", RegexOption.IGNORE_CASE)
    private val ROW_END = Regex("</tr\\s*>|</table\\s*>|</tbody\\s*>|</thead\\s*>", RegexOption.IGNORE_CASE)
    private val CELL_START = Regex("<t[dh]\\b[^>]*>", RegexOption.IGNORE_CASE)
    private val CELL_END = Regex("</t[dh]\\s*>", RegexOption.IGNORE_CASE)
    private val BREAK = Regex("<br\\s*/?>", RegexOption.IGNORE_CASE)
    private val TAG = Regex("<[^>]*>")
    private val ENTITY = Regex("&(#\\d+|#x[0-9a-fA-F]+|[a-zA-Z]+);")
    private val NAMED = mapOf(
        "nbsp" to " ", "amp" to "&", "lt" to "<", "gt" to ">", "quot" to "\"", "apos" to "'", "ordm" to "º", "ordf" to "ª",
        "aacute" to "á", "agrave" to "à", "acirc" to "â", "atilde" to "ã", "eacute" to "é", "ecirc" to "ê", "iacute" to "í",
        "oacute" to "ó", "ocirc" to "ô", "otilde" to "õ", "uacute" to "ú", "uuml" to "ü", "ccedil" to "ç",
        "Aacute" to "Á", "Agrave" to "À", "Acirc" to "Â", "Atilde" to "Ã", "Eacute" to "É", "Ecirc" to "Ê", "Iacute" to "Í",
        "Oacute" to "Ó", "Ocirc" to "Ô", "Otilde" to "Õ", "Uacute" to "Ú", "Ccedil" to "Ç",
    )

    fun hasTable(text: String): Boolean = text.contains("<table", ignoreCase = true) && ROW_START.containsMatchIn(text)

    fun read(html: String): Table {
        val text = SCRIPT.replace(html, " ")
        val rows = ArrayList<List<Cell>>()
        var pos = 0
        while (true) {
            val start = ROW_START.find(text, pos) ?: break
            val bodyStart = start.range.last + 1
            val next = ROW_START.find(text, bodyStart)?.range?.first ?: text.length
            val end = ROW_END.find(text, bodyStart)?.range?.first?.takeIf { it < next } ?: next
            rows += cells(text.substring(bodyStart, end)).map { Cell(it) }
            pos = end
        }
        return rows
    }

    private fun cells(row: String): List<String> {
        val out = ArrayList<String>()
        var pos = 0
        while (true) {
            val start = CELL_START.find(row, pos) ?: break
            val bodyStart = start.range.last + 1
            val next = CELL_START.find(row, bodyStart)?.range?.first ?: row.length
            val end = CELL_END.find(row, bodyStart)?.range?.first?.takeIf { it < next } ?: next
            out += clean(row.substring(bodyStart, end))
            pos = end
        }
        return out
    }

    private fun clean(cell: String): String {
        val noTags = TAG.replace(BREAK.replace(cell, " "), " ")
        val decoded = ENTITY.replace(noTags) { m ->
            val e = m.groupValues[1]
            when {
                e.startsWith("#x") || e.startsWith("#X") -> e.drop(2).toIntOrNull(16)?.let { String(Character.toChars(it)) } ?: m.value
                e.startsWith("#") -> e.drop(1).toIntOrNull()?.let { String(Character.toChars(it)) } ?: m.value
                else -> NAMED[e] ?: m.value
            }
        }
        return decoded.replace(' ', ' ').replace(Regex("\\s+"), " ").trim()
    }
}
