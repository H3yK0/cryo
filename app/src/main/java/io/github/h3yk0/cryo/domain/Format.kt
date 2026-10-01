// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.domain

import java.text.Normalizer
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.abs

val LocaleBR: Locale = Locale.forLanguageTag("pt-BR")

/** Formatação de dinheiro no padrão brasileiro: R$ 1.234,56 */
object Money {
    private const val NBSP = ' '

    private fun group(v: Long): String {
        val s = v.toString()
        val sb = StringBuilder()
        for (i in s.indices) {
            if (i > 0 && (s.length - i) % 3 == 0) sb.append('.')
            sb.append(s[i])
        }
        return sb.toString()
    }

    /** 123456 -> "R$ 1.234,56" */
    fun format(cents: Long, symbol: Boolean = true): String {
        val a = abs(cents)
        val body = "${group(a / 100)},${(a % 100).toString().padStart(2, '0')}"
        val s = if (symbol) "R$$NBSP$body" else body
        return if (cents < 0) "-$s" else s
    }

    /** Com sinal explícito: "+ R$ 10,00" / "− R$ 10,00" */
    fun signed(cents: Long): String = when {
        cents > 0 -> "+$NBSP${format(cents)}"
        cents < 0 -> "−$NBSP${format(-cents)}"
        else -> format(0)
    }

    /** Versão curta para gráficos: "R$ 1,2 mil", "R$ 3,4 mi" */
    fun compact(cents: Long): String {
        val v = abs(cents) / 100.0
        val sign = if (cents < 0) "-" else ""
        fun dec(x: Double) = String.format(LocaleBR, if (x >= 100) "%.0f" else "%.1f", x).replace(",0", "")
        return when {
            v >= 1_000_000 -> "${sign}R$$NBSP${dec(v / 1_000_000)}${NBSP}mi"
            v >= 1_000 -> "${sign}R$$NBSP${dec(v / 1_000)}${NBSP}mil"
            else -> "${sign}R$$NBSP${String.format(LocaleBR, "%.0f", v)}"
        }
    }

    /** Valor escondido (modo privacidade). */
    const val HIDDEN = "R$ ••••"

    /** Converte "1234,5" / "1.234,56" / "12.5" em centavos. Retorna null se inválido. */
    fun parse(text: String): Long? {
        val t = text.trim().replace("R$", "").replace(" ", "").replace(NBSP.toString(), "")
        if (t.isEmpty()) return null
        val normalized = when {
            t.contains(',') -> t.replace(".", "").replace(',', '.')
            t.count { it == '.' } == 1 && t.substringAfter('.').length <= 2 -> t
            else -> t.replace(".", "")
        }
        val d = normalized.toBigDecimalOrNull() ?: return null
        return d.movePointRight(2).setScale(0, java.math.RoundingMode.HALF_UP).toLong()
    }
}

fun YearMonth.key(): Int = year * 100 + monthValue
fun ymOf(key: Int): YearMonth = YearMonth.of(key / 100, key % 100)
fun LocalDate.ym(): YearMonth = YearMonth.from(this)
fun YearMonth.dayClamped(day: Int): LocalDate = atDay(day.coerceIn(1, lengthOfMonth()))

fun String.capFirst(): String = replaceFirstChar { if (it.isLowerCase()) it.titlecase(LocaleBR) else it.toString() }

/** Datas por extenso em português. */
object Dates {
    private val dayMonth = DateTimeFormatter.ofPattern("d 'de' MMMM", LocaleBR)
    private val weekdayDayMonth = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", LocaleBR)
    private val short = DateTimeFormatter.ofPattern("dd/MM", LocaleBR)
    private val full = DateTimeFormatter.ofPattern("dd/MM/yyyy", LocaleBR)

    fun monthName(ym: YearMonth): String = ym.month.getDisplayName(TextStyle.FULL, LocaleBR).capFirst()
    fun monthShort(ym: YearMonth): String =
        ym.month.getDisplayName(TextStyle.SHORT, LocaleBR).replace(".", "").capFirst()

    fun monthYear(ym: YearMonth, today: LocalDate = LocalDate.now()): String =
        if (ym.year == today.year) monthName(ym) else "${monthName(ym)} de ${ym.year}"

    fun dayHeader(d: LocalDate, today: LocalDate): String = when (ChronoUnit.DAYS.between(d, today)) {
        0L -> "Hoje"
        1L -> "Ontem"
        -1L -> "Amanhã"
        else -> d.format(weekdayDayMonth).capFirst() + if (d.year != today.year) " de ${d.year}" else ""
    }

    fun relative(d: LocalDate, today: LocalDate): String = when (val diff = ChronoUnit.DAYS.between(today, d)) {
        0L -> "hoje"
        1L -> "amanhã"
        -1L -> "ontem"
        in 2..6 -> "em $diff dias"
        in -6..-2 -> "há ${-diff} dias"
        else -> d.format(dayMonth)
    }

    fun dayMonth(d: LocalDate): String = d.format(dayMonth)
    fun longDate(d: LocalDate): String = d.format(weekdayDayMonth).capFirst()
    fun short(d: LocalDate): String = d.format(short)
    fun full(d: LocalDate): String = d.format(full)
}

/** Texto em minúsculas e sem acentos, para comparações tolerantes ("Almoço" == "almoco"). */
fun String.normalized(): String =
    Normalizer.normalize(lowercase(LocaleBR), Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "")
