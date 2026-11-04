// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.data

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import android.text.format.DateFormat
import io.github.h3yk0.cryo.domain.AppLanguage
import io.github.h3yk0.cryo.domain.AppLocale
import io.github.h3yk0.cryo.domain.Res
import io.github.h3yk0.cryo.domain.SupportedCurrencies
import io.github.h3yk0.cryo.domain.defaultCurrencyFor
import java.util.Locale

/**
 * Língua e moeda do app (PRD: RF-LNG-02, RF-MOE-02).
 *
 * Ficam num arquivo próprio, lido na hora de abrir o app (antes de qualquer tela), e fora do backup e da transferência para
 * outro celular: no celular novo, o padrão vem do país dele. Vazio = seguir o celular.
 */
object LocaleSettings {
    private const val FILE = "cryo_locale"
    private const val KEY_LANGUAGE = "language"
    private const val KEY_CURRENCY = "currency"

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    /** Língua escolhida em Ajustes, ou null = a do celular. */
    fun chosenLanguage(ctx: Context): AppLanguage? = AppLanguage.forTag(prefs(ctx).getString(KEY_LANGUAGE, null))

    fun setLanguage(ctx: Context, language: AppLanguage?) {
        prefs(ctx).edit().putString(KEY_LANGUAGE, language?.tag.orEmpty()).commit()
    }

    /** Moeda escolhida em Ajustes, ou null = a do país do celular. */
    fun chosenCurrency(ctx: Context): String? = prefs(ctx).getString(KEY_CURRENCY, null)?.takeIf { it in SupportedCurrencies }

    fun setCurrency(ctx: Context, code: String?) {
        prefs(ctx).edit().putString(KEY_CURRENCY, code.orEmpty()).commit()
    }

    /** O que vale agora: a língua do app, o locale para valores e datas, a moeda e o locale das traduções. */
    data class Effective(val language: AppLanguage, val locale: Locale, val currency: String, val resources: Locale)

    /**
     * [system]: o locale do celular (ou o idioma do app escolhido no Android 13+, que o sistema já aplica no contexto).
     * Valores e datas usam o país do celular quando a língua é a mesma (inglês dos EUA ou do Reino Unido, espanhol do México
     * ou da Espanha); as traduções usam a pasta da língua (`values-zh-rCN` para o chinês, `values/` para o português).
     */
    fun effective(ctx: Context, system: Locale): Effective {
        val language = chosenLanguage(ctx) ?: AppLanguage.forLocale(system)
        val sameLanguage = system.language == language.code
        val locale = if (sameLanguage) system else language.locale
        val resources = when (language) {
            AppLanguage.PT_BR -> if (sameLanguage) system else language.locale
            AppLanguage.ZH -> language.locale
            else -> locale
        }
        val currency = chosenCurrency(ctx) ?: defaultCurrencyFor(system, language)
        return Effective(language, locale, currency, resources)
    }

    /** O locale do celular, como o Android entrega neste contexto (antes de o Cryo trocar a língua). */
    fun systemLocale(base: Context): Locale = base.resources.configuration.locales[0] ?: Locale.getDefault()

    /** Um contexto com a língua do app: usado pela Application e pela Activity ao serem criadas. */
    fun wrap(base: Context): Context {
        val e = effective(base, systemLocale(base))
        val config = Configuration(base.resources.configuration)
        config.setLocales(LocaleList(e.resources))
        return base.createConfigurationContext(config)
    }

    /**
     * Aplica a língua e a moeda às regras sem Android (valores, datas e textos por [Res]) e devolve um contexto com a língua do
     * app. Ao abrir o app e quando a pessoa troca nos Ajustes.
     */
    fun install(base: Context): Context {
        val e = effective(base, systemLocale(base))
        AppLocale.set(e.language, e.locale, e.currency)
        val ctx = wrap(base)
        Res.text = { id, args -> if (args.isEmpty()) ctx.getString(id) else ctx.getString(id, *args) }
        Res.plural = { id, n, args -> if (args.isEmpty()) ctx.resources.getQuantityString(id, n) else ctx.resources.getQuantityString(id, n, *args) }
        AppLocale.datePattern = { skeleton, locale -> DateFormat.getBestDateTimePattern(locale, skeleton) }
        return ctx
    }
}
