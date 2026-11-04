// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.importer

import io.github.h3yk0.cryo.domain.AppLanguage
import io.github.h3yk0.cryo.domain.AppLocale
import io.github.h3yk0.cryo.domain.PhraseLang
import io.github.h3yk0.cryo.domain.normalized
import java.math.BigDecimal
import java.math.RoundingMode
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction
import java.time.LocalDate

/**
 * Leitura de datas, valores e textos do jeito que os bancos escrevem: os brasileiros ("01/10/2026", "1.234,56") e os de
 * outros países (RF-IMP-21: "10/01/2026" nos EUA, "1,234.56", "2026年10月1日", "₹1,23,456", "15-Sep-2026").
 */
object Values {
    private val WINDOWS_1252: Charset = Charset.forName("windows-1252")

    /**
     * Texto do arquivo: UTF-8 (com ou sem BOM), UTF-16 (com BOM) ou, se não for, a codificação antiga mais comum na
     * língua do app: Shift_JIS no Japão, CP949/EUC-KR na Coreia, GB18030 na China; no resto, Windows-1252 (o "ANSI" de
     * muitos bancos).
     */
    fun decode(bytes: ByteArray, language: AppLanguage = AppLocale.language): Pair<String, String> {
        if (bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte()) {
            return String(bytes, 2, bytes.size - 2, Charsets.UTF_16LE) to "UTF-16"
        }
        if (bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte()) {
            return String(bytes, 2, bytes.size - 2, Charsets.UTF_16BE) to "UTF-16"
        }
        val start = if (bytes.size >= 3 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte()) 3 else 0
        strict(bytes, start, Charsets.UTF_8)?.let { return it to "UTF-8" }
        val local = when (language) {
            AppLanguage.JA -> listOf("windows-31j", "Shift_JIS", "EUC-JP")
            AppLanguage.KO -> listOf("x-windows-949", "EUC-KR")
            AppLanguage.ZH -> listOf("GB18030", "GBK", "Big5")
            else -> emptyList()
        }
        for (name in local) {
            val cs = runCatching { Charset.forName(name) }.getOrNull() ?: continue
            strict(bytes, start, cs)?.let { return it to cs.name() }
        }
        return String(bytes, start, bytes.size - start, WINDOWS_1252) to "Windows-1252"
    }

    private fun strict(bytes: ByteArray, start: Int, cs: Charset): String? = try {
        cs.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(bytes, start, bytes.size - start)).toString()
    } catch (e: CharacterCodingException) {
        null
    }

    /* ------------------------------------ datas ------------------------------------ */

    /** Meses abreviados em português, inglês, espanhol, francês, alemão e italiano (as 3 primeiras letras). */
    private val MONTHS = mapOf(
        "jan" to 1, "fev" to 2, "feb" to 2, "mar" to 3, "abr" to 4, "apr" to 4, "mai" to 5, "may" to 5, "jun" to 6,
        "jul" to 7, "ago" to 8, "aug" to 8, "set" to 9, "sep" to 9, "out" to 10, "oct" to 10, "nov" to 11,
        "dez" to 12, "dec" to 12, "ene" to 1, "gen" to 1, "avr" to 4, "mag" to 5, "giu" to 6, "lug" to 7, "aou" to 8,
        "okt" to 10, "ott" to 10, "dic" to 12,
    )

    /** Francês: "juin" e "juillet" começam igual. */
    private val MONTHS4 = mapOf("juin" to 6, "juil" to 7)

    /** O número do mês por um nome ("outubro", "Oct", "févr.", "Juli"). */
    fun month(word: String): Int? {
        val w = word.normalized().trimEnd('.')
        if (w.length < 3) return null
        return MONTHS4[w.take(4)] ?: MONTHS[w.take(3)]
    }

    private const val ORD = "(?:st|nd|rd|th|er|º|°|o)?"
    private val ISO = Regex("""^(\d{4})[-/.](\d{1,2})[-/.](\d{1,2})\.?(?:[tT ].*)?$""")
    private val DMY = Regex("""^(\d{1,2})[/.-](\d{1,2})[/.-](\d{2}|\d{4})(?:\s.*)?$""")
    private val DM = Regex("""^(\d{1,2})[/.-](\d{1,2})\.?$""")
    private val TEXT_MONTH =
        Regex("""^(\d{1,2})$ORD\.?[\s-]*(?:de\s+)?([a-z]{3,9})\.?(?:[\s-]*(?:de\s+)?(\d{4}|\d{2}))?(?:\s+\d{1,2}:\d{2}.*)?$""")
    private val MONTH_TEXT = Regex("""^([a-z]{3,9})\.?[\s-]+(\d{1,2})$ORD,?(?:[\s-]+(\d{4}|\d{2}))?(?:\s+\d{1,2}:\d{2}.*)?$""")
    private val CJK_DATE = Regex("""[年월月년일日号]""")

    /**
     * "01/10/2026", "01/10/26", "2026-10-01", "01.10.2026", "01 OUT 2026", "1 de outubro de 2026", "Oct 1, 2026",
     * "15-Sep-2026", "2026年10月1日", "2026. 10. 1.", e também sem o ano ("01/10", "1 de outubro", "10月1日"), quando há
     * uma [reference]: o ano é o dela, ou o anterior se a data cairia mais de um mês depois dela (numa fatura de janeiro,
     * "05/12" é dezembro do ano passado). [monthFirst]: "10/01" é 1º de outubro (EUA); senão, dia antes do mês (Brasil).
     */
    fun date(text: String, reference: LocalDate? = null, monthFirst: Boolean = false): LocalDate? {
        var t = text.trim().normalized().replace(Regex("\\s+"), " ")
        if (t.isEmpty()) return null
        var mf = monthFirst
        if (CJK_DATE.containsMatchIn(t)) {
            // 2026年10月1日, 10월 1일, 10月1号: sempre ano, mês, dia.
            t = t.replace(Regex("\\s*[年년]\\s*"), "-").replace(Regex("\\s*[月월]\\s*"), "-")
                .replace(Regex("\\s*[日일号]"), "").trim('-', ' ')
            mf = true
        }
        // "2026. 10. 1." (coreano) e "01 / 10 / 2026"
        t = t.replace(Regex("\\s*([/.-])\\s*(?=\\d)"), "$1")
        ISO.matchEntire(t)?.let { m -> return safeDate(m.groupValues[1].toInt(), m.groupValues[2].toInt(), m.groupValues[3].toInt()) }
        DMY.matchEntire(t)?.let { m ->
            val (d, mo) = order(m.groupValues[1].toInt(), m.groupValues[2].toInt(), mf)
            return safeDate(year(m.groupValues[3]), mo, d)
        }
        DM.matchEntire(t)?.let { m ->
            val (d, mo) = order(m.groupValues[1].toInt(), m.groupValues[2].toInt(), mf)
            return reference?.let { withoutYear(mo, d, it) }
        }
        TEXT_MONTH.matchEntire(t)?.let { m ->
            val month = month(m.groupValues[2]) ?: return null
            return textDate(m.groupValues[1].toInt(), month, m.groupValues[3], reference)
        }
        MONTH_TEXT.matchEntire(t)?.let { m ->
            val month = month(m.groupValues[1]) ?: return null
            return textDate(m.groupValues[2].toInt(), month, m.groupValues[3], reference)
        }
        return null
    }

    /** Dia e mês na ordem certa; se a ordem pedida der mês 13 ou mais, só pode ser a outra. */
    private fun order(a: Int, b: Int, monthFirst: Boolean): Pair<Int, Int> {
        val (d, m) = if (monthFirst) b to a else a to b
        return if (m > 12 && d <= 12) m to d else d to m
    }

    private fun textDate(day: Int, month: Int, yearText: String, reference: LocalDate?): LocalDate? {
        val y = yearText.takeIf { it.isNotEmpty() }?.let(::year) ?: return reference?.let { withoutYear(month, day, it) }
        return safeDate(y, month, day)
    }

    /**
     * As datas de um arquivo vêm com o mês antes do dia ("10/31/2026")? Decide pelas que não deixam dúvida (um número
     * maior que 12); sem nenhuma, vale o costume da língua do app (mês antes no inglês dos EUA, japonês, coreano, chinês).
     */
    fun monthFirst(samples: Sequence<String>): Boolean {
        var dayFirst = 0
        var monthFirst = 0
        val rx = Regex("""^\s*(\d{1,2})[/.-](\d{1,2})(?:[/.-]\d{2,4})?\b""")
        for (s in samples.take(400)) {
            val m = rx.find(s) ?: continue
            val a = m.groupValues[1].toInt()
            val b = m.groupValues[2].toInt()
            if (a > 12 && b <= 12) dayFirst++
            if (b > 12 && a <= 12) monthFirst++
        }
        return when {
            dayFirst > monthFirst -> false
            monthFirst > dayFirst -> true
            else -> PhraseLang.current.monthFirst(AppLocale.locale)
        }
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

    /** Símbolos e códigos de moeda que saem do valor ("R$", "US$", "€", "₹", "Rs.", "EUR", "円"...). */
    private val CURRENCY = Regex(
        """(?i)R\$|US\$|U\${'$'}S|C\$|A\$|MX\$|\$|€|£|¥|￥|₩|₹|\bRs\.?|\b(?:BRL|USD|EUR|GBP|INR|JPY|KRW|CNY|RMB|CHF|MXN|CAD|AUD|ARS|CLP|COP|PEN|UYU)\b|元|円|원""",
    )

    /** Línguas em que a vírgula separa os centavos (no resto, "1,500" é mil e quinhentos). */
    private val COMMA_DECIMAL = setOf(AppLanguage.PT_BR, AppLanguage.ES, AppLanguage.FR, AppLanguage.DE, AppLanguage.IT)

    /**
     * Valor em centavos, com sinal. Entende "R$ 1.234,56", "-1.234,56", "1.234,56-", "(1.234,56)", "1.234,56 D",
     * "12,50 C", "−R$ 10,00", "1234.56", "$1,234.56", "1.234,56 €", "1 234,56" (França), "1'234.50" (Suíça),
     * "₹1,23,456.00", "500.00 Dr", "¥1,500".
     * [decimalComma]: true = vírgula separa os centavos (padrão brasileiro); false = ponto; null = descobre sozinho.
     */
    fun amount(text: String?, decimalComma: Boolean? = null): Long? {
        var t = text?.trim()?.replace('−', '-')?.replace('\u00A0', ' ')?.replace('\u202F', ' ')?.replace('\u2009', ' ') ?: return null
        if (t.isEmpty()) return null
        var negative = false
        if (t.startsWith("(") && t.endsWith(")")) { negative = true; t = t.substring(1, t.length - 1).trim() }
        Regex("""\s*(CR|DR|Cr|Dr|cr|dr|[DdCc])\.?$""").find(t)?.let { m ->
            if (m.groupValues[1].startsWith("d", true)) negative = !negative
            t = t.substring(0, m.range.first).trim()
        }
        t = t.replace(CURRENCY, "").replace(" ", "").replace("'", "").replace("’", "")
        while (t.startsWith("-") || t.startsWith("+")) { if (t[0] == '-') negative = !negative; t = t.drop(1) }
        if (t.endsWith("-")) { negative = !negative; t = t.dropLast(1) }
        if (t.isEmpty() || !NUMBER.matches(t)) return null

        val lastComma = t.lastIndexOf(',')
        val lastDot = t.lastIndexOf('.')
        val decimal: Char? = when {
            lastComma >= 0 && lastDot >= 0 -> if (lastComma > lastDot) ',' else '.'
            lastComma >= 0 -> {
                // "1,500": milhar nos EUA e na Ásia; centavos no Brasil e na Europa. "1,23,456": milhar (Índia).
                val threeDigits = t.length - lastComma - 1 == 3
                val commaDecimal = decimalComma ?: (PhraseLang.current.language in COMMA_DECIMAL)
                if (threeDigits && (!commaDecimal || t.count { it == ',' } > 1)) null else ','
            }
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

    /**
     * Os valores de uma coluna usam vírgula para os centavos? Olha todos: ",50" no fim decide. Sem centavos em nenhum
     * (iene, won), o separador de milhar responde: "1,500" = ponto nos centavos; "1.500" = vírgula.
     */
    fun guessDecimalComma(samples: List<String>): Boolean? {
        var comma = 0
        var dot = 0
        var commaThousands = 0
        var dotThousands = 0
        for (s in samples) {
            val t = s.replace(CURRENCY, "").trim().trimEnd('-', 'D', 'd', 'C', 'c', 'R', 'r', ' ', ')', '.')
            when {
                Regex(""",\d{1,2}$""").containsMatchIn(t) -> comma++
                Regex("""\.\d{1,2}$""").containsMatchIn(t) -> dot++
                Regex("""\d,\d{3}$""").containsMatchIn(t) -> commaThousands++
                Regex("""\d\.\d{3}$""").containsMatchIn(t) -> dotThousands++
            }
        }
        return when {
            comma > 0 || dot > 0 -> comma >= dot
            commaThousands > 0 && dotThousands == 0 -> false
            dotThousands > 0 && commaThousands == 0 -> true
            else -> null
        }
    }

    /* ------------------------------------ textos ------------------------------------ */

    private val KEEP_UPPER = setOf(
        "PIX", "TED", "DOC", "IOF", "CPF", "CNPJ", "TEF", "BB", "CEF", "INSS", "IPVA", "IPTU", "FGTS", "DAS", "DARF",
        "LTDA", "ME", "MEI", "EIRELI", "SA", "S.A.", "PJ", "PF", "RS", "SP", "RJ", "MG", "GO", "DF", "BA", "PR", "SC",
        "ATM", "POS", "ACH", "UPI", "NEFT", "IMPS", "RTGS", "EMI", "SEPA", "CB", "EC", "USA", "UK", "LLC", "INC", "LTD",
        "GMBH", "AG", "KG", "SL", "SAS", "SARL", "SPA", "SRL", "BV", "NV", "PLC", "SPEI", "OXXO", "IVA", "VAT", "TV",
    )

    /** Palavras que ficam minúsculas no meio do nome ("Pão de Queijo", não "Pão De Queijo"; "Bank of America"). */
    private val SMALL = setOf(
        "de", "da", "do", "das", "dos", "e", "em", "na", "no", "nas", "nos", "para", "por", "com", "a", "o", "as", "os",
        "of", "the", "and", "at", "on", "in", "for", "del", "la", "el", "y", "con", "du", "des", "le", "les", "et", "au",
        "und", "der", "die", "von", "im", "am", "di", "della", "il",
    )

    /** "PADARIA PAO DE OURO" -> "Padaria Pao de Ouro" (só quando o banco escreve tudo em maiúsculas). */
    fun prettify(text: String): String {
        val t = text.replace(Regex("\\s+"), " ").trim()
        val letters = t.filter { it.isLetter() }
        if (letters.isEmpty() || letters.any { it.isLowerCase() }) return t
        return t.split(' ').mapIndexed { i, w ->
            when {
                w.uppercase() in KEEP_UPPER || w.any { it.isDigit() } -> w
                i > 0 && w.lowercase() in SMALL -> w.lowercase()
                // "UPI-SWIGGY" -> "UPI-Swiggy": cada pedaço entre hífens e barras do seu jeito.
                else -> w.split(PARTS).joinToString("") { part -> if (part.uppercase() in KEEP_UPPER) part else titled(part) }
            }
        }.joinToString(" ")
    }

    private val PARTS = Regex("(?<=[-/])|(?=[-/])")

    /** "*TRIP" -> "*Trip": maiúscula na primeira letra, mesmo depois de um símbolo. */
    private fun titled(w: String): String = w.lowercase().let { lw ->
        val first = lw.indexOfFirst { it.isLetter() }
        if (first < 0) lw else lw.substring(0, first) + lw[first].titlecase() + lw.substring(first + 1)
    }

    /** "Total", "Total da fatura", "Total de compras" (mas não "Total Distribuidora", que é nome de loja). */
    private val TOTAL = Regex(
        """^(?:total|totale|summe|gesamt|gesamtsumme|合計|合计|总计|합계|총계|कुल)(?: (?:de|da|do|dos|das|geral|a pagar|no periodo|""" +
            """for|of|del|des|du|der|di|della)\b|:|$)""",
    )

    /** Começo das linhas de saldo nas outras línguas ("Opening balance", "Solde précédent", "残高", "잔액"...). */
    private val BALANCE_START = listOf(
        "opening balance", "closing balance", "previous balance", "new balance", "available balance",
        "beginning balance", "ending balance", "balance brought forward", "balance carried forward", "brought forward",
        "carried forward", "b/f", "c/f", "solde", "kontostand", "anfangssaldo", "endsaldo", "alter saldo", "neuer saldo",
        "残高", "繰越", "前月繰越", "잔액", "이월", "余额", "期初余额", "期末余额", "上期余额", "शेष", "प्रारंभिक शेष",
    ).map { it.normalized() }

    /** Linhas de saldo e totais não são movimentações. */
    fun isBalanceOrTotal(description: String): Boolean {
        val n = description.normalized().replace(Regex("\\s+"), " ").trim()
        return n.startsWith("saldo") || n.contains("saldo anterior") || n.contains("saldo do dia") ||
            n.contains("saldo final") || n.contains("saldo disponivel") || TOTAL.containsMatchIn(n) ||
            n.contains("total de entradas") || n.contains("total de saidas") || n == "s a l d o" ||
            BALANCE_START.any { n == it || n.startsWith("$it ") || n.startsWith("$it:") } ||
            n.contains("opening balance") || n.contains("closing balance") || n.contains("solde precedent") ||
            n.contains("nouveau solde") || n.contains("saldo inicial") || n.contains("saldo precedente")
    }
}
