// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.github.h3yk0.cryo.data.LocaleSettings
import io.github.h3yk0.cryo.domain.AppLanguage
import io.github.h3yk0.cryo.domain.AppLocale
import io.github.h3yk0.cryo.domain.Dates
import io.github.h3yk0.cryo.domain.Money
import io.github.h3yk0.cryo.domain.Res
import io.github.h3yk0.cryo.domain.defaultCurrencyFor
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.util.Locale

/**
 * Valores e datas no jeito de cada língua e moeda (RF-MOE-04, RF-MOE-06, RF-LNG-05), com o mesmo Android (ICU) do celular.
 * Os valores são guardados em centésimos (DAD-01): 123456 = 1.234,56 em qualquer moeda.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = TestApp::class)
class I18nTest {
    private val nb = ' '
    /** Algarismos arábico-índicos (٠–٩) e os persas (۰–۹). */
    private val ARABIC_INDIC = ('\u0660'..'\u0669') + ('\u06F0'..'\u06F9')
    private val app get() = ApplicationProvider.getApplicationContext<Context>()

    private fun use(tag: String, currency: String) =
        AppLocale.set(AppLanguage.forLocale(Locale.forLanguageTag(tag)), Locale.forLanguageTag(tag), currency)

    @After fun back() {
        LocaleSettings.setLanguage(app, null)
        LocaleSettings.setCurrency(app, null)
        LocaleSettings.install(app)
    }

    @Test fun valoresEmCadaMoeda() {
        use("pt-BR", "BRL")
        assertEquals("R$${nb}1.234,56", Money.format(123456))
        assertEquals("-R$${nb}10,00", Money.format(-1000))
        assertEquals("+${nb}R$${nb}10,00", Money.signed(1000))
        assertEquals("R$${nb}••••", Money.HIDDEN)
        assertEquals("R$${nb}1,2${nb}mil", Money.compact(123456))

        use("en-US", "USD")
        assertEquals("$1,234.56", Money.format(123456))
        assertEquals("$1.2K", Money.compact(123456))
        assertEquals("$••••", Money.HIDDEN)

        use("de-DE", "EUR")
        assertEquals("1.234,56${nb}€", Money.format(123456))
        assertEquals("1,2${nb}Tsd.${nb}€", Money.compact(123456))
        assertEquals("••••${nb}€", Money.HIDDEN)

        // Iene e won: sem centavos (12345678 centésimos = 123.456,78 → ¥123,457).
        use("ja-JP", "JPY")
        assertTrue(Money.format(12345678).endsWith("123,457"))
        assertTrue(Money.compact(12345678).endsWith("12.3万"))
        use("ko-KR", "KRW")
        assertTrue(Money.format(12345678).contains("123,457"))

        // Rúpia: no celular, agrupamento indiano (₹1,23,456.78); o simulador dos testes agrupa de 3 em 3.
        use("hi-IN", "INR")
        assertTrue(Money.format(12345678).let { it.startsWith("₹") && it.endsWith("456.78") })
        assertEquals("₹1.2${nb}लाख", Money.compact(12345678))
    }

    @Test fun moedaPadraoPeloPais() {
        assertEquals("USD", defaultCurrencyFor(Locale.US, AppLanguage.EN))
        assertEquals("GBP", defaultCurrencyFor(Locale.UK, AppLanguage.EN))
        assertEquals("MXN", defaultCurrencyFor(Locale.forLanguageTag("es-MX"), AppLanguage.ES))
        assertEquals("EUR", defaultCurrencyFor(Locale.forLanguageTag("es-ES"), AppLanguage.ES))
        assertEquals("JPY", defaultCurrencyFor(Locale.JAPAN, AppLanguage.JA))
        assertEquals("BRL", defaultCurrencyFor(Locale.forLanguageTag("pt-BR"), AppLanguage.PT_BR))
        // País fora da lista (Cazaquistão, tenge): a moeda do país principal da língua.
        assertEquals("USD", defaultCurrencyFor(Locale.forLanguageTag("en-KZ"), AppLanguage.EN))
        // Língua fora das 14: inglês.
        assertEquals(AppLanguage.EN, AppLanguage.forLocale(Locale.forLanguageTag("tr-TR")))
        // 1.6 (RF-LNG-11): as 4 línguas novas e as moedas delas.
        assertEquals(AppLanguage.RU, AppLanguage.forLocale(Locale.forLanguageTag("ru-RU")))
        assertEquals("RUB", defaultCurrencyFor(Locale.forLanguageTag("ru-RU"), AppLanguage.RU))
        assertEquals("UAH", defaultCurrencyFor(Locale.forLanguageTag("uk-UA"), AppLanguage.UK))
        assertEquals("SAR", defaultCurrencyFor(Locale.forLanguageTag("ar-SA"), AppLanguage.AR))
        assertEquals("AED", defaultCurrencyFor(Locale.forLanguageTag("ar-AE"), AppLanguage.AR))
        assertEquals("EGP", defaultCurrencyFor(Locale.forLanguageTag("ar-EG"), AppLanguage.AR))
        // O Android ainda entrega o hebraico como "iw"; o Cryo entende "iw" e "he" como a mesma língua.
        assertEquals(AppLanguage.HE, AppLanguage.forLocale(Locale("iw", "IL")))
        assertEquals(AppLanguage.HE, AppLanguage.forLocale(Locale.forLanguageTag("he-IL")))
        assertEquals("ILS", defaultCurrencyFor(Locale("iw", "IL"), AppLanguage.HE))
        // País árabe cuja moeda o Cryo não tem (Marrocos): a moeda padrão do árabe.
        assertEquals(AppLanguage.AR.defaultCurrency, defaultCurrencyFor(Locale.forLanguageTag("ar-MA"), AppLanguage.AR))
    }

    /** RF-LNG-11 (1.6): rublo, grívnia, as três moedas árabes e o shekel, no jeito de cada país. */
    @Test fun moedasDasLinguasNovas() {
        use("ru-RU", "RUB")
        assertEquals("1${nb}234,56${nb}₽", Money.format(123456))
        assertEquals("123${nb}тыс.${nb}₽", Money.compact(12345678))
        use("uk-UA", "UAH")
        assertEquals("1${nb}234,56${nb}₴", Money.format(123456))
        assertEquals("123${nb}тис.${nb}₴", Money.compact(12345678))
        for ((tag, cur, sym) in listOf(Triple("ar-SA", "SAR", "ر.س."), Triple("ar-AE", "AED", "د.إ."), Triple("ar-EG", "EGP", "ج.م."))) {
            val l = Locale.forLanguageTag(tag)
            AppLocale.set(AppLanguage.AR, LocaleSettings.latinDigits(AppLanguage.AR, l), cur)
            val v = Money.format(123456)
            assertTrue("$tag: $v", v.contains("1,234.56") && v.contains(sym) && v.none { it in ARABIC_INDIC })
            assertTrue("$tag: compacto", Money.compact(12345678).contains("123${nb}ألف"))
        }
        use("he-IL", "ILS")
        assertTrue(Money.format(123456).let { it.contains("1,234.56") && it.contains("₪") })
        assertTrue(Money.compact(12345678).contains("123K"))
    }

    /**
     * ERR-058: no árabe, os números do app (valores, datas e os que vão dentro dos textos, como "3 dias") saem com os
     * algarismos 0–9, os mesmos dos extratos e do teclado. O Android tira o "-u-nu-latn" do locale dos recursos; por isso
     * o Cryo formata os textos com o próprio locale.
     */
    @Test fun arabeComAlgarismosLatinos() {
        LocaleSettings.setLanguage(app, AppLanguage.AR)
        LocaleSettings.setCurrency(app, "SAR")
        LocaleSettings.install(app)
        assertEquals("latn", AppLocale.locale.getUnicodeLocaleType("nu"))
        assertTrue(Money.format(123456).contains("1,234.56"))
        val date = Dates.short(LocalDate.of(2026, 10, 5))
        assertTrue(date, date.contains("05") && date.contains("10") && date.none { it in ARABIC_INDIC })
        // As outras línguas ficam como estão.
        assertEquals(Locale.forLanguageTag("hi-IN"), LocaleSettings.latinDigits(AppLanguage.HI, Locale.forLanguageTag("hi-IN")))
    }

    /** ERR-058: os números dentro das frases ("3 ديون", "+ 2 حساب", "بعد 2 يوم") também saem com 0–9. */
    @Test fun arabeComAlgarismosLatinosNasFrases() {
        LocaleSettings.setLanguage(app, AppLanguage.AR)
        LocaleSettings.setCurrency(app, "SAR")
        LocaleSettings.install(app)
        val texts = listOf(
            Res.p(R.plurals.count_days, 3, 3),
            Res.p(R.plurals.debt_count, 3, 3),
            Res.p(R.plurals.home_more_accounts, 2, 2),
            Res.p(R.plurals.date_rel_in_days, 2, 2),
            Res.s(R.string.voice_mb, "57"),
        )
        for (t in texts) assertTrue(t, t.any { it.isDigit() && it in '0'..'9' } && t.none { it in ARABIC_INDIC })
    }

    @Test fun datasEmCadaLingua() {
        val d = LocalDate.of(2026, 10, 5)
        LocaleSettings.install(app)
        use("pt-BR", "BRL")
        assertEquals("Segunda-feira, 5 de outubro", Dates.longDate(d))
        assertEquals("05/10", Dates.short(d))
        use("en-US", "USD")
        assertEquals("Monday, October 5", Dates.longDate(d))
        assertEquals("10/05", Dates.short(d))
        use("de-DE", "EUR")
        assertEquals("05.10.", Dates.short(d))
        use("ja-JP", "JPY")
        assertTrue(Dates.longDate(d).startsWith("10月5日"))
    }

    @Test fun escolherLinguaEMoedaNosAjustes() {
        LocaleSettings.setLanguage(app, AppLanguage.DE)
        LocaleSettings.setCurrency(app, "CHF")
        LocaleSettings.install(app)
        assertEquals(AppLanguage.DE, AppLocale.language)
        assertEquals("CHF", AppLocale.currency.currencyCode)
        assertEquals("de", LocaleSettings.wrap(app).resources.configuration.locales[0].language)
    }
}
