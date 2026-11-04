// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.h3yk0.cryo.CryoApp
import io.github.h3yk0.cryo.R
import io.github.h3yk0.cryo.domain.Res
import io.github.h3yk0.cryo.data.LocaleSettings
import io.github.h3yk0.cryo.domain.AppLanguage
import io.github.h3yk0.cryo.domain.AppLocale
import io.github.h3yk0.cryo.domain.SupportedCurrencies
import io.github.h3yk0.cryo.domain.defaultCurrencyFor
import io.github.h3yk0.cryo.ui.components.ChoiceRow
import io.github.h3yk0.cryo.ui.components.ConfirmDialog
import java.util.Currency

/*
 * Idioma e moeda (PRD: RF-AJU-09, RF-AJU-10, RF-BOA-07). Usado nos Ajustes e na apresentação inicial.
 * Trocar aplica na hora: a tela é recriada com a língua e a moeda novas, sem fechar o app.
 */

/** Aplica a língua e a moeda escolhidas e recria a tela. */
fun applyLocaleChange(ctx: Context) {
    (ctx.applicationContext as? CryoApp)?.applyLocale()
    ctx.findActivity()?.recreate()
}

/** "Português (Brasil)" ou "Do celular (English)". */
fun languageSummary(ctx: Context): String {
    val chosen = LocaleSettings.chosenLanguage(ctx)
    return chosen?.nativeName ?: ctx.getString(R.string.locale_from_device, AppLocale.language.nativeName)
}

/** "Real brasileiro (R$)" ou "Do país do celular (Dólar americano, $)". */
fun currencySummary(ctx: Context): String {
    val name = currencyName(AppLocale.currency.currencyCode)
    return if (LocaleSettings.chosenCurrency(ctx) == null) ctx.getString(R.string.currency_from_device, name) else name
}

private fun currencyName(code: String): String {
    val c = Currency.getInstance(code)
    return "${c.getDisplayName(AppLocale.locale).replaceFirstChar { it.titlecase(AppLocale.locale) }} (${c.getSymbol(AppLocale.locale)})"
}

@Composable
fun LanguageSheet(onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val chosen = LocaleSettings.chosenLanguage(ctx)
    val device = AppLanguage.forLocale(LocaleSettings.systemLocale(ctx.applicationContext))
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 24.dp).navigationBarsPadding()) {
            Text(stringResource(R.string.locale_language_title), style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(12.dp))
            ChoiceRow(
                stringResource(R.string.locale_language_device), device.nativeName, selected = chosen == null,
                onClick = { pick(ctx, chosen, null, onDismiss) },
            )
            AppLanguage.entries.forEach { lang ->
                ChoiceRow(lang.nativeName, "", selected = chosen == lang, onClick = { pick(ctx, chosen, lang, onDismiss) })
            }
        }
    }
}

private fun pick(ctx: Context, current: AppLanguage?, lang: AppLanguage?, onDismiss: () -> Unit) {
    onDismiss()
    if (current == lang) return
    LocaleSettings.setLanguage(ctx, lang)
    applyLocaleChange(ctx)
}

@Composable
fun CurrencySheet(onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val chosen = LocaleSettings.chosenCurrency(ctx)
    val deviceCode = defaultCurrencyFor(LocaleSettings.systemLocale(ctx.applicationContext), AppLocale.language)
    var confirm by remember { mutableStateOf<String?>(null) }
    // RF-MOE-03: trocar não converte; com dados já lançados, um aviso antes.
    fun choose(code: String?) {
        val effective = code ?: deviceCode
        if (effective == AppLocale.currency.currencyCode && code == chosen) return onDismiss()
        confirm = code ?: ""
    }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 24.dp).navigationBarsPadding()) {
            Text(stringResource(R.string.locale_currency_title), style = MaterialTheme.typography.titleLarge)
            Text(
                stringResource(R.string.locale_currency_hint), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
            )
            ChoiceRow(stringResource(R.string.locale_currency_device), currencyName(deviceCode), selected = chosen == null, onClick = { choose(null) })
            SupportedCurrencies.forEach { code ->
                ChoiceRow(currencyName(code), code, selected = chosen == code, onClick = { choose(code) })
            }
        }
    }
    confirm?.let { code ->
        ConfirmDialog(
            stringResource(R.string.locale_currency_confirm_title),
            stringResource(R.string.locale_currency_confirm_text),
            stringResource(R.string.locale_currency_confirm_button),
            onConfirm = {
                confirm = null
                onDismiss()
                LocaleSettings.setCurrency(ctx, code.ifEmpty { null })
                applyLocaleChange(ctx)
            },
            onDismiss = { confirm = null },
        )
    }
}
