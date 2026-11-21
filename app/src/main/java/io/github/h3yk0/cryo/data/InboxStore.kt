// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * RF-INI-19 (pedido do Hanry, LA9): o que a central de avisos já mostrou. Guarda só o código HMAC da chave de cada aviso
 * (RF-SEG-02: nunca o texto, o valor ou o nome) e o dia em que ele foi visto, para esquecer o que é velho (mais de
 * [KEEP_DAYS] dias). Um arquivo por conjunto de dados: o modo de teste tem o seu. [version] muda a cada "visto", para as
 * telas recontarem o sino.
 */
class InboxStore(ctx: Context, sandbox: Boolean, private val hasher: (String) -> String) {
    private val prefs = ctx.getSharedPreferences(if (sandbox) "cryo_inbox_teste" else "cryo_inbox", Context.MODE_PRIVATE)
    private val _version = MutableStateFlow(0)
    val version: StateFlow<Int> = _version

    /** Os códigos vistos (lidos uma vez por chamada: a lista é pequena). */
    private fun seenCodes(): Map<String, Long> = prefs.getStringSet(SEEN, emptySet()).orEmpty().mapNotNull { e ->
        val i = e.lastIndexOf('|')
        if (i <= 0) null else e.substring(0, i) to (e.substring(i + 1).toLongOrNull() ?: 0L)
    }.toMap()

    /** Uma função "este aviso já foi visto?" para as regras ([io.github.h3yk0.cryo.domain.Inbox]). */
    fun seen(): (String) -> Boolean {
        val codes = seenCodes().keys
        return { key -> hasher(PREFIX + key) in codes }
    }

    /** Marca os avisos como vistos hoje ([today] em dias desde 1970) e esquece os vistos há mais de [KEEP_DAYS] dias. */
    fun markSeen(keys: Collection<String>, today: Long) {
        if (keys.isEmpty()) return
        val codes = seenCodes().filterValues { today - it <= KEEP_DAYS }.toMutableMap()
        for (k in keys) codes[hasher(PREFIX + k)] = today
        prefs.edit().putStringSet(SEEN, codes.map { (c, d) -> "$c|$d" }.toSet()).apply()
        _version.value++
    }

    /** "Apagar todos os dados" também esquece os avisos vistos. */
    fun clear() {
        prefs.edit().clear().apply()
        _version.value++
    }

    private companion object {
        const val SEEN = "seen"
        const val PREFIX = "inbox:"
        const val KEEP_DAYS = 90L
    }
}
