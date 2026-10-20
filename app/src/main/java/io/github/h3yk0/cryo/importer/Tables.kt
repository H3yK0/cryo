// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.importer

import io.github.h3yk0.cryo.domain.normalized
import java.time.LocalDate
import kotlin.math.abs

/** Uma célula de tabela (CSV ou planilha). Planilhas já trazem número e data prontos. */
data class Cell(val text: String, val number: Double? = null, val date: LocalDate? = null) {
    val isBlank: Boolean get() = text.isBlank() && number == null && date == null
}

typealias Table = List<List<Cell>>

/** O que cada coluna significa. Índices começam em 0; -1 = a planilha não tem cabeçalho. */
data class ColumnMap(
    val headerRow: Int,
    val date: Int,
    val description: Int,
    /** Coluna única com o valor (com sinal). */
    val amount: Int? = null,
    /** Ou duas colunas: entradas e saídas. */
    val credit: Int? = null,
    val debit: Int? = null,
    /** Coluna "C/D", "Tipo" (crédito/débito). */
    val kind: Int? = null,
    val id: Int? = null,
    /** Outras colunas de texto que completam a descrição (ex.: "Lançamento" + "Detalhes"). */
    val extra: List<Int> = emptyList(),
) {
    val isValid: Boolean get() = date >= 0 && description >= 0 && (amount != null || credit != null || debit != null)
}

/* ====================================== CSV ====================================== */

object Csv {
    /** Lê o CSV descobrindo o separador (; , tab |) e respeitando aspas. */
    fun read(text: String): Pair<Table, Char> {
        val candidates = listOf(';', ',', '\t', '|')
        val sample = text.lineSequence().filter { it.isNotBlank() }.take(40).joinToString("\n")
        val best = candidates.maxBy { sep ->
            val counts = split(sample, sep).map { it.size }.filter { it > 1 }
            if (counts.isEmpty()) 0 else {
                val mode = counts.groupingBy { it }.eachCount().maxBy { it.value }
                mode.value * 10 + mode.key
            }
        }
        return split(text, best).map { row -> row.map { Cell(it.trim()) } } to best
    }

    private fun split(text: String, sep: Char): List<List<String>> {
        val rows = ArrayList<List<String>>()
        var row = ArrayList<String>()
        val cell = StringBuilder()
        var quoted = false
        var i = 0
        while (i < text.length) {
            val ch = text[i]
            when {
                quoted && ch == '"' && i + 1 < text.length && text[i + 1] == '"' -> { cell.append('"'); i++ }
                ch == '"' && (quoted || cell.isBlank()) -> { quoted = !quoted; if (!quoted) Unit else cell.setLength(0) }
                !quoted && ch == sep -> { row += cell.toString(); cell.setLength(0) }
                !quoted && (ch == '\n' || ch == '\r') -> {
                    if (ch == '\r' && i + 1 < text.length && text[i + 1] == '\n') i++
                    row += cell.toString(); cell.setLength(0)
                    // Linhas em branco continuam contando: assim "linha 5" no relatório é a linha 5 do arquivo.
                    rows += row
                    row = ArrayList()
                }
                else -> cell.append(ch)
            }
            i++
        }
        row += cell.toString()
        if (row.any { it.isNotBlank() }) rows += row
        return rows
    }
}

/* ================================ colunas e linhas ================================ */

object Columns {
    private enum class Role { DATE, DESCRIPTION, AMOUNT, CREDIT, DEBIT, BALANCE, KIND, ID }

    private val NAMES: List<Pair<Role, List<String>>> = listOf(
        Role.DATE to listOf("data", "date", "dt", "data lancamento", "data de lancamento", "data da transacao", "data mov",
            "data movimento", "data do movimento", "data compra", "data da compra", "dia"),
        Role.DESCRIPTION to listOf("descricao", "historico", "description", "title", "titulo", "lancamento", "lancamentos",
            "estabelecimento", "detalhes", "detalhe", "memo", "nome", "transacao", "movimentacao", "descricao do lancamento"),
        Role.AMOUNT to listOf("valor", "amount", "valor (r$)", "valor r$", "valor em r$", "quantia", "montante", "valor rs",
            "valor da transacao", "valor do lancamento"),
        Role.CREDIT to listOf("credito", "creditos", "entrada", "entradas", "valor credito", "credito (r$)", "entradas (r$)"),
        Role.DEBIT to listOf("debito", "debitos", "saida", "saidas", "valor debito", "debito (r$)", "saidas (r$)"),
        Role.BALANCE to listOf("saldo", "balance", "saldo (r$)", "saldo do dia"),
        Role.KIND to listOf("c/d", "d/c", "tipo", "natureza", "cd", "dc"),
        Role.ID to listOf("identificador", "id", "fitid", "codigo da transacao"),
    )

    private fun roleOf(header: String): Role? {
        val n = header.normalized().replace(Regex("\\s+"), " ").trim().trimEnd(':')
        // "Data: 03/10/2026", "Créditos: R$ 5.230,00": é um resumo com valores, não o nome de uma coluna.
        if (n.isEmpty() || n.any { it.isDigit() }) return null
        NAMES.firstOrNull { (_, names) -> n in names }?.let { return it.first }
        return NAMES.firstOrNull { (_, names) -> names.any { it.length >= 4 && n.startsWith(it) } }?.first
    }

    /** Descobre as colunas: pelo cabeçalho (que pode vir depois de algumas linhas com nome do banco e período) ou pelo conteúdo. */
    fun detect(rows: Table): ColumnMap? {
        for (r in 0 until minOf(rows.size, 40)) {
            val roles = rows[r].map { roleOf(it.text) }
            val date = roles.indexOf(Role.DATE)
            if (date < 0) continue
            val amount = roles.indexOf(Role.AMOUNT).takeIf { it >= 0 }
            val credit = roles.indexOf(Role.CREDIT).takeIf { it >= 0 }
            val debit = roles.indexOf(Role.DEBIT).takeIf { it >= 0 }
            if (amount == null && credit == null && debit == null) continue
            val texts = roles.withIndex().filter { it.value == Role.DESCRIPTION }.map { it.index }
            val description = texts.firstOrNull()
                ?: longestText(rows.drop(r + 1).take(30), setOfNotNull(date, amount, credit, debit)) ?: continue
            return ColumnMap(
                headerRow = r, date = date, description = description, amount = amount, credit = credit, debit = debit,
                kind = roles.indexOf(Role.KIND).takeIf { it >= 0 && it != description && isKindColumn(rows.drop(r + 1).take(60), it) },
                id = roles.indexOf(Role.ID).takeIf { it >= 0 },
                extra = texts.drop(1),
            )
        }
        return byContent(rows)
    }

    private val DEBIT_WORDS = setOf("d", "db", "deb", "debito", "debit", "saida", "-")
    private val CREDIT_WORDS = setOf("c", "cr", "cred", "credito", "credit", "entrada", "+")

    /** -1 = débito, 1 = crédito, 0 = não diz. */
    private fun kindSign(text: String): Int {
        val n = text.normalized().trim().trimEnd('.')
        return when (n) {
            in DEBIT_WORDS -> -1
            in CREDIT_WORDS -> 1
            else -> 0
        }
    }

    /** Uma coluna "Tipo" só vale como crédito/débito se quase todos os valores forem C/D (e não "PIX", "Compra"...). */
    private fun isKindColumn(rows: Table, col: Int): Boolean {
        val values = rows.mapNotNull { it.getOrNull(col)?.text?.takeIf { t -> t.isNotBlank() } }
        return values.isNotEmpty() && values.count { kindSign(it) != 0 } >= values.size * 0.8
    }

    /** Sem cabeçalho: a coluna que mais parece data, a que mais parece valor e o texto mais longo. */
    private fun byContent(rows: Table): ColumnMap? {
        val sample = rows.filter { r -> r.any { !it.isBlank } }.take(60)
        val width = sample.maxOfOrNull { it.size } ?: return null
        fun ratio(col: Int, test: (Cell) -> Boolean): Double {
            val cells = sample.mapNotNull { it.getOrNull(col) }.filter { !it.isBlank }
            return if (cells.isEmpty()) 0.0 else cells.count(test).toDouble() / sample.size
        }
        val date = (0 until width).maxByOrNull { c -> ratio(c) { it.date != null || Values.date(it.text, Values.ANY_YEAR) != null } } ?: return null
        if (ratio(date) { it.date != null || Values.date(it.text, Values.ANY_YEAR) != null } < 0.5) return null
        val amount = (0 until width).filter { it != date }
            .maxByOrNull { c -> ratio(c) { it.number != null || (Values.amount(it.text) != null && Regex("[.,]\\d{1,2}\\b").containsMatchIn(it.text)) } }
            ?: return null
        if (ratio(amount) { it.number != null || Values.amount(it.text) != null } < 0.5) return null
        val description = longestText(sample, setOf(date, amount)) ?: return null
        return ColumnMap(headerRow = -1, date = date, description = description, amount = amount)
    }

    private fun longestText(rows: Table, skip: Set<Int>): Int? {
        val width = rows.maxOfOrNull { it.size } ?: return null
        return (0 until width).filter { it !in skip }
            .maxByOrNull { c -> rows.sumOf { r -> r.getOrNull(c)?.text?.takeIf { t -> t.any(Char::isLetter) }?.length ?: 0 } }
    }

    /** Os nomes das colunas (para a tela de escolher colunas). */
    fun headers(rows: Table, map: ColumnMap?): List<String> {
        val width = rows.maxOfOrNull { it.size } ?: 0
        val header = map?.headerRow?.takeIf { it >= 0 }?.let { rows.getOrNull(it) }
        return (0 until width).map { c -> header?.getOrNull(c)?.text?.takeIf { it.isNotBlank() } ?: "Coluna ${c + 1}" }
    }

    /** Monta as movimentações a partir das colunas escolhidas. */
    fun build(rows: Table, map: ColumnMap, format: StatementFormat, today: LocalDate, notes: List<String>): Statement {
        val data = rows.drop(map.headerRow + 1)
        val amountCols = listOfNotNull(map.amount, map.credit, map.debit)
        val decimalComma = Values.guessDecimalComma(data.flatMap { r -> amountCols.mapNotNull { r.getOrNull(it)?.takeIf { c -> c.number == null }?.text } })
        // Datas sem ano ("05/10"): o ano vem da data completa mais recente da coluna (ou de hoje).
        val reference = Values.reference(data.asSequence().mapNotNull { it.getOrNull(map.date)?.let { c -> c.date ?: Values.date(c.text) } }, today)
        val lines = ArrayList<StatementLine>()
        val skipped = ArrayList<SkippedLine>()

        fun money(c: Cell?): Long? = when {
            c == null || c.isBlank -> null
            c.number != null -> Values.cents(c.number)
            else -> Values.amount(c.text, decimalComma)
        }

        for (row in data) {
            if (row.all { it.isBlank }) continue
            val source = row.joinToString(" | ") { it.text }
            val dc = row.getOrNull(map.date)
            val date = dc?.date ?: dc?.text?.let { Values.date(it, reference) }
            val description = (listOf(map.description) + map.extra).mapNotNull { row.getOrNull(it)?.text?.trim()?.takeIf { t -> t.isNotEmpty() } }
                .joinToString(" ")
                .ifBlank { row.withIndex().filter { it.index !in amountCols && it.index != map.date && it.value.text.any(Char::isLetter) }.joinToString(" ") { it.value.text.trim() } }
            if (date == null) { skipped += SkippedLine(source, "sem data"); continue }
            if (Values.isBalanceOrTotal(description)) { skipped += SkippedLine(source, "linha de saldo ou total"); continue }
            var cents: Long? = when {
                map.amount != null -> money(row.getOrNull(map.amount))
                else -> {
                    val credit = money(map.credit?.let { row.getOrNull(it) })
                    val debit = money(map.debit?.let { row.getOrNull(it) })
                    if (credit == null && debit == null) null else abs(credit ?: 0) - abs(debit ?: 0)
                }
            }
            if (cents == null) { skipped += SkippedLine(source, "sem valor"); continue }
            map.kind?.let { k ->
                when (kindSign(row.getOrNull(k)?.text.orEmpty())) {
                    -1 -> cents = -abs(cents!!)
                    1 -> cents = abs(cents!!)
                }
            }
            if (cents == 0L) { skipped += SkippedLine(source, "valor zero"); continue }
            val id = map.id?.let { row.getOrNull(it)?.text?.trim() }?.takeIf { it.isNotEmpty() }
            lines += StatementLine(date, Values.prettify(description.ifBlank { "Movimentação" }), cents!!, id, source)
        }

        val headers = headers(rows, map)
        fun name(i: Int?) = i?.let { "\"${headers.getOrNull(it)}\"" }
        val colNotes = listOfNotNull(
            if (map.headerRow >= 0) "Cabeçalho na linha ${map.headerRow + 1}" else "Sem cabeçalho (colunas pelo conteúdo)",
            "Data: ${name(map.date)}", "Descrição: " + (listOf(map.description) + map.extra).joinToString(" + ") { name(it).orEmpty() },
            map.amount?.let { "Valor: ${name(it)}" }, map.credit?.let { "Entradas: ${name(it)}" }, map.debit?.let { "Saídas: ${name(it)}" },
            map.kind?.let { "Crédito/débito: ${name(it)}" }, map.id?.let { "Identificador: ${name(it)}" },
            when (decimalComma) { true -> "Centavos depois da vírgula"; false -> "Centavos depois do ponto"; null -> null },
        )
        return Statement(format, lines, skipped, isCard = looksLikeCard(rows, map), notes = notes + colNotes)
    }

    /** Faturas de cartão costumam ter "title"/"estabelecimento" e nenhuma coluna de saldo. */
    private fun looksLikeCard(rows: Table, map: ColumnMap): Boolean {
        val header = rows.getOrNull(map.headerRow)?.map { it.text.normalized() } ?: return false
        return header.any { it == "title" || it.startsWith("estabelecimento") } && header.none { it.startsWith("saldo") }
    }
}
