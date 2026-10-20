// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Modo desenvolvedor: ligado com 7 toques na versão do app (Ajustes › Sobre), como no Android.
 *
 * Fica num arquivo próprio (lido na hora de abrir o app, antes do banco) e fora do backup e da transferência
 * para outro celular. No "modo de teste", as telas usam outro banco de dados e outros ajustes: os dados de verdade
 * ficam guardados, sem ninguém mexer, até voltar.
 */
class DevSettings(context: Context) {
    private val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    var enabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, false)
        set(v) {
            // Desligar o modo desenvolvedor também sai do modo de teste.
            prefs.edit().putBoolean(KEY_ENABLED, v).also { if (!v) it.putBoolean(KEY_SANDBOX, false) }.commit()
        }

    /** Usando os dados de teste (só vale com o modo desenvolvedor ligado). */
    var sandbox: Boolean
        get() = enabled && prefs.getBoolean(KEY_SANDBOX, false)
        set(v) {
            prefs.edit().putBoolean(KEY_SANDBOX, v).commit()
        }

    /** O modo desenvolvedor, acompanhando as mudanças. */
    val enabledFlow: Flow<Boolean>
        get() = callbackFlow {
            val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                if (key == KEY_ENABLED || key == null) trySend(enabled)
            }
            trySend(enabled)
            prefs.registerOnSharedPreferenceChangeListener(listener)
            awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
        }.conflate().distinctUntilChanged()

    companion object {
        /** Também listado em data_extraction_rules.xml (fica fora do backup do Android). */
        const val FILE = "cryo_dev"
        private const val KEY_ENABLED = "enabled"
        private const val KEY_SANDBOX = "sandbox"
    }
}
