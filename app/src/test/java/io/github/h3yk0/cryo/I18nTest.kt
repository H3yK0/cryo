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
        // País fora da lista (Rússia): a moeda do país principal da língua.
        assertEquals("USD", defaultCurrencyFor(Locale.forLanguageTag("en-RU"), AppLanguage.EN))
        // Língua fora das 10: inglês.
        assertEquals(AppLanguage.EN, AppLanguage.forLocale(Locale.forLanguageTag("ru-RU")))
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
