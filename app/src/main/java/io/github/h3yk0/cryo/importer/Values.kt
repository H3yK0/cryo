// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.importer

import io.github.h3yk0.cryo.domain.normalized
import java.math.BigDecimal
import java.math.RoundingMode
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction
import java.time.LocalDate

/** Leitura de datas, valores e textos no jeito que os bancos brasileiros escrevem. */
object Values {
    private val WINDOWS_1252: Charset = Charset.forName("windows-1252")

    /** Texto do arquivo: UTF-8 (com ou sem BOM) ou, se não for, Windows-1252 (o "ANSI" de muitos bancos). */
    fun decode(bytes: ByteArray): Pair<String, String> {
        val start = if (bytes.size >= 3 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte()) 3 else 0
        val buf = ByteBuffer.wrap(bytes, start, bytes.size - start)
        return try {
            val text = Charsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(buf).toString()
            text to "UTF-8"
        } catch (e: CharacterCodingException) {
            String(bytes, start, bytes.size - start, WINDOWS_1252) to "Windows-1252"
        }
    }

    /* ------------------------------------ datas ------------------------------------ */

    private val MONTHS = mapOf(
        "jan" to 1, "fev" to 2, "feb" to 2, "mar" to 3, "abr" to 4, "apr" to 4, "mai" to 5, "may" to 5, "jun" to 6,
        "jul" to 7, "ago" to 8, "aug" to 8, "set" to 9, "sep" to 9, "out" to 10, "oct" to 10, "nov" to 11,
        "dez" to 12, "dec" to 12,
    )

    private val ISO = Regex("""^(\d{4})[-/.](\d{1,2})[-/.](\d{1,2})(?:[tT ].*)?$""")
    private val DMY = Regex("""^(\d{1,2})[/.-](\d{1,2})[/.-](\d{2}|\d{4})(?:\s.*)?$""")
    private val DM = Regex("""^(\d{1,2})[/.-](\d{1,2})$""")
    private val TEXT_MONTH = Regex("""^(\d{1,2})\s*(?:de\s+)?([a-z]{3,9})\.?(?:\s*(?:de\s+)?(\d{4}|\d{2}))?$""")

    /**
     * "01/10/2026", "01/10/26", "2026-10-01", "01.10.2026", "01 OUT 2026", "1 de outubro de 2026", e também sem o ano
     * ("01/10", "1 de outubro"), quando há uma [reference]: o ano é o dela, ou o anterior se a data cairia mais de um
     * mês depois dela (numa fatura de janeiro, "05/12" é dezembro do ano passado). Sempre dia antes do mês, como no Brasil.
     */
    fun date(text: String, reference: LocalDate? = null): LocalDate? {
        val t = text.trim().normalized().replace(Regex("\\s+"), " ")
        if (t.isEmpty()) return null
        ISO.matchEntire(t)?.let { m -> return safeDate(m.groupValues[1].toInt(), m.groupValues[2].toInt(), m.groupValues[3].toInt()) }
        DMY.matchEntire(t)?.let { m -> return safeDate(year(m.groupValues[3]), m.groupValues[2].toInt(), m.groupValues[1].toInt()) }
        DM.matchEntire(t)?.let { m -> return reference?.let { withoutYear(m.groupValues[2].toInt(), m.groupValues[1].toInt(), it) } }
        TEXT_MONTH.matchEntire(t)?.let { m ->
            val month = MONTHS[m.groupValues[2].take(3)] ?: return null
            val day = m.groupValues[1].toInt()
            val y = m.groupValues[3].takeIf { it.isNotEmpty() }?.let(::year) ?: return reference?.let { withoutYear(month, day, it) }
            return safeDate(y, month, day)
        }
        return null
    }

    private fun withoutYear(month: Int, day: Int, reference: LocalDate): LocalDate? {
        val d = safeDate(reference.year, month, day) ?: return null
        return if (d.isAfter(reference.plusDays(31))) safeDate(reference.year - 1, month, day) else d
    }

    /**
     * Data de referência para as datas sem ano de um extrato: a mais recente das datas completas que ele traz
     * (ignorando datas absurdas, como uma validade lá em 2030), ou hoje.
     */
    fun reference(fullDates: Sequence<LocalDate>, today: LocalDate): LocalDate =
        fullDates.filter { !it.isAfter(today.plusDays(60)) && it.year >= today.year - 15 }.maxOrNull() ?: today

    /** Para só conferir se um texto é uma data (o ano não importa). */
    val ANY_YEAR: LocalDate = LocalDate.of(2000, 12, 31)

    private fun year(s: String): Int = if (s.length == 2) s.toInt().let { if (it >= 70) 1900 + it else 2000 + it } else s.toInt()

    private fun safeDate(y: Int, m: Int, d: Int): LocalDate? =
        if (y in 1970..2100 && m in 1..12 && d in 1..31) runCatching { LocalDate.of(y, m, d) }.getOrNull() else null

    /** Data do OFX: "20261001", "20261001120000", "20261001120000[-3:BRT]". */
    fun ofxDate(text: String?): LocalDate? {
        val digits = text?.trim()?.takeWhile { it.isDigit() } ?: return null
        if (digits.length < 8) return null
        return safeDate(digits.substring(0, 4).toInt(), digits.substring(4, 6).toInt(), digits.substring(6, 8).toInt())
    }

    /** Data das planilhas do Excel: dias desde 30/12/1899. */
    fun excelDate(serial: Double): LocalDate? =
        if (serial in 1.0..80000.0) LocalDate.of(1899, 12, 30).plusDays(serial.toLong()) else null

    /* ------------------------------------ valores ----------------------------------- */

    private val NUMBER = Regex("""^\d[\d.,]*$""")

    /**
     * Valor em centavos, com sinal. Entende "R$ 1.234,56", "-1.234,56", "1.234,56-", "(1.234,56)", "1.234,56 D",
     * "12,50 C", "−R$ 10,00", "1234.56".
     * [decimalComma]: true = vírgula separa os centavos (padrão brasileiro); false = ponto; null = descobre sozinho.
     */
    fun amount(text: String?, decimalComma: Boolean? = null): Long? {
        var t = text?.trim()?.replace('−', '-')?.replace(' ', ' ') ?: return null
        if (t.isEmpty()) return null
        var negative = false
        if (t.startsWith("(") && t.endsWith(")")) { negative = true; t = t.substring(1, t.length - 1).trim() }
        Regex("""\s*([DdCc])$""").find(t)?.let { m ->
            if (m.groupValues[1].equals("d", true)) negative = !negative
            t = t.substring(0, m.range.first).trim()
        }
        t = t.replace(Regex("(?i)R\\$"), "").replace(" ", "")
        while (t.startsWith("-") || t.startsWith("+")) { if (t[0] == '-') negative = !negative; t = t.drop(1) }
        if (t.endsWith("-")) { negative = !negative; t = t.dropLast(1) }
        if (t.isEmpty() || !NUMBER.matches(t)) return null

        val lastComma = t.lastIndexOf(',')
        val lastDot = t.lastIndexOf('.')
        val decimal: Char? = when {
            lastComma >= 0 && lastDot >= 0 -> if (lastComma > lastDot) ',' else '.'
            lastComma >= 0 -> if (decimalComma == false && t.length - lastComma - 1 == 3) null else ','
            lastDot >= 0 -> when (decimalComma) {
                true -> null
                false -> '.'
                null -> if (t.length - lastDot - 1 == 3 && (t.count { it == '.' } > 1 || t.indexOf('.') <= 3)) null else '.'
            }
            else -> null
        }
        val plain = if (decimal == null) t.replace(".", "").replace(",", "")
        else t.substring(0, t.lastIndexOf(decimal)).replace(".", "").replace(",", "") + "." + t.substring(t.lastIndexOf(decimal) + 1)
        val value = plain.toBigDecimalOrNull() ?: return null
        val cents = value.movePointRight(2).setScale(0, RoundingMode.HALF_UP).toLong()
        return if (negative) -cents else cents
    }

    /** Valor que veio como número de verdade (planilha, JSON): 12.5 -> 1250. */
    fun cents(value: Double): Long = BigDecimal.valueOf(value).movePointRight(2).setScale(0, RoundingMode.HALF_UP).toLong()

    /** Os valores de uma coluna usam vírgula para os centavos? Olha todos: ",50" no fim decide. */
    fun guessDecimalComma(samples: List<String>): Boolean? {
        var comma = 0
        var dot = 0
        for (s in samples) {
            val t = s.trim().trimEnd('-', 'D', 'd', 'C', 'c', ' ', ')')
            if (Regex(""",\d{1,2}$""").containsMatchIn(t)) comma++
            else if (Regex("""\.\d{1,2}$""").containsMatchIn(t)) dot++
        }
        return when {
            comma == 0 && dot == 0 -> null
            comma >= dot -> true
            else -> false
        }
    }

    /* ------------------------------------ textos ------------------------------------ */

    private val KEEP_UPPER = setOf(
        "PIX", "TED", "DOC", "IOF", "CPF", "CNPJ", "TEF", "BB", "CEF", "INSS", "IPVA", "IPTU", "FGTS", "DAS", "DARF",
        "LTDA", "ME", "MEI", "EIRELI", "SA", "S.A.", "PJ", "PF", "RS", "SP", "RJ", "MG", "GO", "DF", "BA", "PR", "SC",
    )

    /** Palavras que ficam minúsculas no meio do nome ("Pão de Queijo", não "Pão De Queijo"). */
    private val SMALL = setOf("de", "da", "do", "das", "dos", "e", "em", "na", "no", "nas", "nos", "para", "por", "com", "a", "o", "as", "os")

    /** "PADARIA PAO DE OURO" -> "Padaria Pao de Ouro" (só quando o banco escreve tudo em maiúsculas). */
    fun prettify(text: String): String {
        val t = text.replace(Regex("\\s+"), " ").trim()
        val letters = t.filter { it.isLetter() }
        if (letters.isEmpty() || letters.any { it.isLowerCase() }) return t
        return t.split(' ').mapIndexed { i, w ->
            when {
                w.uppercase() in KEEP_UPPER || w.any { it.isDigit() } -> w
                i > 0 && w.lowercase() in SMALL -> w.lowercase()
                // "*TRIP" -> "*Trip": maiúscula na primeira letra, mesmo depois de um símbolo.
                else -> w.lowercase().let { lw ->
                    val first = lw.indexOfFirst { it.isLetter() }
                    if (first < 0) lw else lw.substring(0, first) + lw[first].titlecase() + lw.substring(first + 1)
                }
            }
        }.joinToString(" ")
    }

    /** "Total", "Total da fatura", "Total de compras" (mas não "Total Distribuidora", que é nome de loja). */
    private val TOTAL = Regex("""^total(?: (?:de|da|do|dos|das|geral|a pagar|no periodo)\b|:|$)""")

    /** Linhas de saldo e totais não são movimentações. */
    fun isBalanceOrTotal(description: String): Boolean {
        val n = description.normalized().replace(Regex("\\s+"), " ").trim()
        return n.startsWith("saldo") || n.contains("saldo anterior") || n.contains("saldo do dia") ||
            n.contains("saldo final") || n.contains("saldo disponivel") || TOTAL.containsMatchIn(n) ||
            n.contains("total de entradas") || n.contains("total de saidas") || n == "s a l d o"
    }
}
