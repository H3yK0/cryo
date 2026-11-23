// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.data

import android.content.Context
import io.github.h3yk0.cryo.domain.WidgetConfig

/**
 * RF-WID-02 (1.7): a escolha de cada widget (o número grande e as linhas), pelo número que o Android deu a ele. Só guarda
 * escolhas de tela, nunca dados (SharedPreferences `cryo_widgets`, fora do backup como os outros ajustes do aparelho).
 */
class WidgetPrefs(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("cryo_widgets", Context.MODE_PRIVATE)

    fun config(appWidgetId: Int): WidgetConfig = WidgetConfig.decode(prefs.getString(key(appWidgetId), null))

    fun save(appWidgetId: Int, config: WidgetConfig) {
        prefs.edit().putString(key(appWidgetId), config.encode()).apply()
    }

    /** O widget saiu da tela: a escolha dele vai junto. */
    fun remove(ids: IntArray) {
        prefs.edit().apply { ids.forEach { remove(key(it)) } }.apply()
    }

    private fun key(id: Int) = "w$id"
}
