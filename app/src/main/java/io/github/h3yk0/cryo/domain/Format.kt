// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.domain

import io.github.h3yk0.cryo.R
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.Normalizer
import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Currency
import java.util.Locale
import kotlin.math.abs

val LocaleBR: Locale = Locale.forLanguageTag("pt-BR")

/**
 * Valores na moeda e no jeito da língua do app (RF-MOE-04): "R$ 1.234,56", "$1,234.56", "1.234,56 €", "￥1,235".
 * Guardamos sempre centésimos da unidade (DAD-01); em moedas sem centavos, o valor aparece sem casas.
 */
object Money {
    private const val NBSP = ' '

    private class Formats(val locale: Locale, val currency: Currency, val withSymbol: NumberFormat, val plain: NumberFormat)

    @Volatile private var cache: Formats? = null

    private fun formats(): Formats {
        val l = AppLocale.locale
        val c = AppLocale.currency
        cache?.let { if (it.locale == l && it.currency == c) return it }
        val digits = AppLocale.fractionDigits
        fun NumberFormat.setup() = apply {
            minimumFractionDigits = digits
            maximumFractionDigits = digits
            roundingMode = RoundingMode.HALF_UP
        }
        val withSymbol = NumberFormat.getCurrencyInstance(l).apply { currency = c }.setup()
        val plain = NumberFormat.getNumberInstance(l).setup()
        return Formats(l, c, withSymbol, plain).also { cache = it }
    }

    private fun units(cents: Long): BigDecimal = BigDecimal.valueOf(cents).movePointLeft(2)

    /** 123456 -> "R$ 1.234,56" (em pt-BR com real). [symbol] = false: só o número ("1.234,56"). */
    fun format(cents: Long, symbol: Boolean = true): String {
        val f = formats()
        val nf = if (symbol) f.withSymbol else f.plain
        return synchronized(nf) { nf.format(units(cents)) }
    }

    /** Com sinal explícito: "+ R$ 10,00" / "− R$ 10,00" (menos de verdade e espaço que não quebra, TXT-07). */
    fun signed(cents: Long): String = when {
        cents > 0 -> "+$NBSP${format(cents)}"
        cents < 0 -> "−$NBSP${format(-cents)}"
        else -> format(0)
    }

    /** Onde fica o número no formato da moeda: "R$ ⟨1.234,56⟩", "⟨1.234,56⟩ €". */
    private val NUMBER_PART = Regex("""[0-9](?:[0-9.,'   ]*[0-9])?""")

    /** O formato da moeda com outra coisa no lugar do número ("R$ ••••", "•••• €"). */
    private fun around(inner: String): String {
        val zero = format(0)
        val m = NUMBER_PART.find(zero) ?: return inner
        return zero.substring(0, m.range.first) + inner + zero.substring(m.range.last + 1)
    }

    /** Valor escondido (modo privacidade): o símbolo da moeda e "••••". */
    val HIDDEN: String get() = around("••••")

    /**
     * Versão curta para gráficos (RF-MOE-06), do jeito de cada língua: "R$ 1,2 mil", "$1.2K", "1,2 Tsd. €", "￥1.2万", "₹1.2 लाख".
     */
    fun compact(cents: Long): String {
        val v = abs(cents) / 100.0
        val sign = if (cents < 0) "-" else ""
        val units = COMPACT[AppLocale.language] ?: COMPACT.getValue(AppLanguage.EN)
        val unit = units.lastOrNull { v >= it.first }
        val text = if (unit == null) {
            synchronized(formats().plain) {
                NumberFormat.getIntegerInstance(AppLocale.locale).apply { roundingMode = RoundingMode.HALF_UP }.format(v)
            }
        } else {
            val x = v / unit.first
            val nf = NumberFormat.getNumberInstance(AppLocale.locale).apply {
                maximumFractionDigits = if (x >= 100) 0 else 1
                minimumFractionDigits = 0
                roundingMode = RoundingMode.HALF_UP
            }
            nf.format(x) + unit.second
        }
        return sign + around(text)
    }

    /** Unidades dos valores resumidos de cada língua: a partir de quanto e o que vem depois do número. */
    private val COMPACT: Map<AppLanguage, List<Pair<Double, String>>> = mapOf(
        AppLanguage.PT_BR to listOf(1e3 to "${NBSP}mil", 1e6 to "${NBSP}mi", 1e9 to "${NBSP}bi"),
        AppLanguage.EN to listOf(1e3 to "K", 1e6 to "M", 1e9 to "B"),
        AppLanguage.ES to listOf(1e3 to "${NBSP}mil", 1e6 to "${NBSP}M", 1e9 to "${NBSP}mil${NBSP}M"),
        AppLanguage.FR to listOf(1e3 to "${NBSP}k", 1e6 to "${NBSP}M", 1e9 to "${NBSP}Md"),
        AppLanguage.DE to listOf(1e3 to "${NBSP}Tsd.", 1e6 to "${NBSP}Mio.", 1e9 to "${NBSP}Mrd."),
        AppLanguage.IT to listOf(1e6 to "${NBSP}Mln", 1e9 to "${NBSP}Mrd"),
        AppLanguage.JA to listOf(1e4 to "万", 1e8 to "億"),
        AppLanguage.ZH to listOf(1e4 to "万", 1e8 to "亿"),
        AppLanguage.KO to listOf(1e4 to "만", 1e8 to "억"),
        AppLanguage.HI to listOf(1e3 to "${NBSP}हज़ार", 1e5 to "${NBSP}लाख", 1e7 to "${NBSP}करोड़"),
    )

    /** Converte "1234,5" / "1.234,56" / "12.5" / "$1,234" em centésimos. Retorna null se inválido. */
    fun parse(text: String): Long? {
        val t = text.trim().filter { it.isDigit() || it == ',' || it == '.' }
        if (t.isEmpty()) return null
        val normalized = when {
            t.contains(',') && t.contains('.') -> if (t.lastIndexOf(',') > t.lastIndexOf('.')) t.replace(".", "").replace(',', '.') else t.replace(",", "")
            t.contains(',') -> if (t.substringAfterLast(',').length <= 2 && t.count { it == ',' } == 1) t.replace(',', '.') else t.replace(",", "")
            t.count { it == '.' } == 1 && t.substringAfter('.').length <= 2 -> t
            else -> t.replace(".", "")
        }
        val d = normalized.toBigDecimalOrNull() ?: return null
        return d.movePointRight(2).setScale(0, RoundingMode.HALF_UP).toLong()
    }
}

/** Números e porcentagens no jeito da língua do app ("80%" em português, "80 %" em francês e alemão). */
object Num {
    fun percent(p: Int): String = NumberFormat.getPercentInstance(AppLocale.locale).format(p / 100.0)
    fun int(n: Long): String = NumberFormat.getIntegerInstance(AppLocale.locale).format(n)
    fun decimal(x: Double, digits: Int = 1): String = NumberFormat.getNumberInstance(AppLocale.locale).apply {
        minimumFractionDigits = digits
        maximumFractionDigits = digits
    }.format(x)
}

fun YearMonth.key(): Int = year * 100 + monthValue
fun ymOf(key: Int): YearMonth = YearMonth.of(key / 100, key % 100)
fun LocalDate.ym(): YearMonth = YearMonth.from(this)
fun YearMonth.dayClamped(day: Int): LocalDate = atDay(day.coerceIn(1, lengthOfMonth()))

fun String.capFirst(): String = replaceFirstChar { if (it.isLowerCase()) it.titlecase(AppLocale.locale) else it.toString() }

/** Datas no jeito da língua do app (RF-LNG-05, TXT-08). */
object Dates {
    @Volatile private var cache: Pair<Locale, MutableMap<String, DateTimeFormatter>>? = null

    /** Formatador para um "esqueleto" de data, com o padrão da língua ("EEEEdMMMM" → "Segunda-feira, 5 de outubro"). */
    private fun fmt(skeleton: String): DateTimeFormatter {
        val l = AppLocale.locale
        val c = cache?.takeIf { it.first == l } ?: (l to java.util.concurrent.ConcurrentHashMap<String, DateTimeFormatter>()).also { cache = it }
        return c.second.getOrPut(skeleton) { DateTimeFormatter.ofPattern(AppLocale.datePattern(skeleton, l), l) }
    }

    fun monthName(ym: YearMonth): String = ym.month.getDisplayName(TextStyle.FULL_STANDALONE, AppLocale.locale).capFirst()
    fun monthShort(ym: YearMonth): String =
        ym.month.getDisplayName(TextStyle.SHORT_STANDALONE, AppLocale.locale).replace(".", "").capFirst()

    fun monthYear(ym: YearMonth, today: LocalDate = LocalDate.now()): String =
        if (ym.year == today.year) monthName(ym) else ym.atDay(1).format(fmt("MMMMyyyy")).capFirst()

    /**
     * O mês no meio de uma frase, do jeito de cada língua: "fatura de outubro" (minúsculo em português, espanhol, francês,
     * italiano), "October invoice" e "Oktober" (maiúsculo em inglês e alemão).
     */
    fun monthInSentence(ym: YearMonth): String = ym.month.getDisplayName(TextStyle.FULL_STANDALONE, AppLocale.locale)

    fun monthYearInSentence(ym: YearMonth, today: LocalDate = LocalDate.now()): String =
        if (ym.year == today.year) monthInSentence(ym) else ym.atDay(1).format(fmt("MMMMyyyy"))

    /** Mês curto com ano: "out. de 2027", "Oct 2027", "2027年10月". */
    fun monthShortYear(ym: YearMonth): String = ym.atDay(1).format(fmt("MMMyyyy"))

    fun dayHeader(d: LocalDate, today: LocalDate): String = when (ChronoUnit.DAYS.between(d, today)) {
        0L -> Res.s(R.string.date_today)
        1L -> Res.s(R.string.date_yesterday)
        -1L -> Res.s(R.string.date_tomorrow)
        else -> d.format(fmt(if (d.year != today.year) "EEEEdMMMMyyyy" else "EEEEdMMMM")).capFirst()
    }

    fun relative(d: LocalDate, today: LocalDate): String = when (val diff = ChronoUnit.DAYS.between(today, d)) {
        0L -> Res.s(R.string.date_rel_today)
        1L -> Res.s(R.string.date_rel_tomorrow)
        -1L -> Res.s(R.string.date_rel_yesterday)
        in 2..6 -> Res.p(R.plurals.date_rel_in_days, diff.toInt(), diff.toInt())
        in -6..-2 -> Res.p(R.plurals.date_rel_days_ago, (-diff).toInt(), (-diff).toInt())
        else -> d.format(fmt("dMMMM"))
    }

    fun dayMonth(d: LocalDate): String = d.format(fmt("dMMMM"))
    fun longDate(d: LocalDate): String = d.format(fmt("EEEEdMMMM")).capFirst()
    fun short(d: LocalDate): String = d.format(fmt("ddMM"))
    fun full(d: LocalDate): String = d.format(fmt("ddMMyyyy"))
}

/**
 * Texto em minúsculas e sem acentos, para comparações tolerantes ("Almoço" == "almoco", "Café" == "cafe"). Só tira os acentos
 * das letras latinas: em hindi, japonês e coreano, os sinais fazem parte da palavra.
 */
fun String.normalized(): String {
    val decomposed = Normalizer.normalize(lowercase(Locale.ROOT), Normalizer.Form.NFD).replace(LATIN_MARKS, "")
    return Normalizer.normalize(decomposed, Normalizer.Form.NFC)
}

private val LATIN_MARKS = Regex("(?<=\\p{IsLatin})\\p{Mn}+")
