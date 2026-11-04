// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.domain

import java.util.Currency
import java.util.Locale

/*
 * Língua e moeda do app (PRD: RF-LNG, RF-MOE), para as regras sem Android (valores, datas, textos das análises).
 * O app define tudo isso ao abrir e quando a pessoa troca nos Ajustes; nos testes de regras fica o padrão (português e real).
 */

/** As 10 línguas do Cryo (RF-LNG-01). A primeira é a base (`values/`). */
enum class AppLanguage(val tag: String, val nativeName: String, val defaultCurrency: String) {
    PT_BR("pt-BR", "Português (Brasil)", "BRL"),
    EN("en", "English", "USD"),
    ES("es", "Español", "EUR"),
    FR("fr", "Français", "EUR"),
    DE("de", "Deutsch", "EUR"),
    IT("it", "Italiano", "EUR"),
    JA("ja", "日本語", "JPY"),
    KO("ko", "한국어", "KRW"),
    ZH("zh-CN", "中文（简体）", "CNY"),
    HI("hi", "हिन्दी", "INR");

    val locale: Locale get() = Locale.forLanguageTag(tag)
    val code: String get() = tag.substringBefore('-')

    companion object {
        /** A língua do Cryo para um locale do celular: a mesma língua, ou inglês quando ela não está entre as 10 (RF-LNG-02). */
        fun forLocale(l: Locale): AppLanguage = entries.firstOrNull { it.code == l.language } ?: EN
        fun forTag(tag: String?): AppLanguage? = tag?.takeIf { it.isNotBlank() }?.let { t -> entries.firstOrNull { it.tag == t } }
    }
}

/** Moedas que dá para escolher (RF-MOE-07), na ordem da lista. */
val SupportedCurrencies: List<String> = listOf(
    "BRL", "USD", "EUR", "GBP", "CAD", "AUD", "MXN", "ARS", "CLP", "COP", "PEN", "UYU", "CHF",
    "KRW", "INR", "CNY", "JPY", "TWD", "HKD", "SGD",
)

/**
 * Moeda padrão (RF-MOE-02): a do país do celular, se estiver na lista; senão, a do país principal da língua do app.
 * Ex.: inglês nos EUA → dólar; no Reino Unido → libra; espanhol no México → peso; na Espanha → euro.
 */
fun defaultCurrencyFor(device: Locale, language: AppLanguage): String {
    val byCountry = device.country.takeIf { it.length == 2 }?.let { runCatching { Currency.getInstance(Locale("", it)).currencyCode }.getOrNull() }
    return byCountry?.takeIf { it in SupportedCurrencies } ?: language.defaultCurrency
}

object AppLocale {
    /** Locale usado para valores e datas (a língua do app com o país do celular, quando combinam). */
    @Volatile var locale: Locale = LocaleBR
        private set
    @Volatile var language: AppLanguage = AppLanguage.PT_BR
        private set
    @Volatile var currency: Currency = Currency.getInstance("BRL")
        private set

    /**
     * Padrão de data para um "esqueleto" ("EEEEdMMMM" → "EEEE, d 'de' MMMM" em português, "EEEE, MMMM d" em inglês).
     * No app vem do Android (que conhece o jeito de cada língua); nos testes de regras, os padrões do português.
     */
    @Volatile var datePattern: (skeleton: String, locale: Locale) -> String = { skeleton, _ -> PT_PATTERNS[skeleton] ?: skeleton }

    fun set(language: AppLanguage, locale: Locale, currencyCode: String) {
        this.language = language
        this.locale = locale
        this.currency = runCatching { Currency.getInstance(currencyCode) }.getOrDefault(Currency.getInstance(language.defaultCurrency))
    }

    /** Casas decimais da moeda (iene e won: 0). */
    val fractionDigits: Int get() = currency.defaultFractionDigits.coerceAtLeast(0)

    /** Volta ao padrão (português e real): para testes. */
    fun reset() = set(AppLanguage.PT_BR, LocaleBR, "BRL")

    private val PT_PATTERNS = mapOf(
        "EEEEdMMMM" to "EEEE, d 'de' MMMM",
        "dMMMM" to "d 'de' MMMM",
        "dMMMMyyyy" to "d 'de' MMMM 'de' yyyy",
        "ddMM" to "dd/MM",
        "ddMMyyyy" to "dd/MM/yyyy",
        "MMMMyyyy" to "MMMM 'de' yyyy",
        "MMMyyyy" to "MMM/yyyy",
        "EEEEdMMMMyyyy" to "EEEE, d 'de' MMMM 'de' yyyy",
    )
}

/**
 * Textos das regras (análises, datas relativas, lembretes) sem depender do Android: as regras chamam [s] e [p] com o id do
 * `strings.xml`, e o app diz como achar o texto na língua dele. Nos testes de regras, um leitor do `values/strings.xml`.
 */
object Res {
    @Volatile var text: (id: Int, args: Array<out Any?>) -> String = { id, _ -> "#$id" }
    @Volatile var plural: (id: Int, count: Int, args: Array<out Any?>) -> String = { id, n, _ -> "#$id:$n" }

    fun s(id: Int, vararg args: Any?): String = text(id, args)
    fun p(id: Int, count: Int, vararg args: Any?): String = plural(id, count, args)
}
