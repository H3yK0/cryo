// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.data

import android.content.Context
import io.github.h3yk0.cryo.domain.engine.EngineOptions
import io.github.h3yk0.cryo.domain.engine.TxKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * O pouco que o motor financeiro guarda (RF-SEG-02): as recorrências que a pessoa disse que não são recorrentes, como
 * **HMAC-SHA256** da chave (nunca a descrição), e a inflação que ela informou para comparar o rendimento. O resto do motor
 * fica só na memória. No modo de teste, um arquivo separado.
 */
class EngineStore(ctx: Context, sandbox: Boolean, private val vault: Vault) {
    private val prefs = ctx.getSharedPreferences(if (sandbox) "cryo_engine_teste" else "cryo_engine", Context.MODE_PRIVATE)

    private val _options = MutableStateFlow(read())
    val options: StateFlow<EngineOptions> = _options

    /** O código guardado para uma recorrência (o mesmo que o motor confere ao filtrar). */
    fun hash(key: String): String = vault.hmac(PREFIX + key)

    fun dismiss(key: String) = update(prefs.getStringSet(DISMISSED, emptySet()).orEmpty() + hash(key))

    fun undismiss(key: String) = update(prefs.getStringSet(DISMISSED, emptySet()).orEmpty() - hash(key))

    /** Inflação ao ano informada pela pessoa (0,045 = 4,5%), ou null. */
    var inflation: Double?
        get() = prefs.getFloat(INFLATION, -1f).takeIf { it >= 0f }?.toDouble()
        set(v) {
            prefs.edit().apply { if (v == null) remove(INFLATION) else putFloat(INFLATION, v.toFloat()) }.apply()
            _inflation.value = v
        }

    private val _inflation = MutableStateFlow(inflation)
    val inflationFlow: StateFlow<Double?> = _inflation

    /**
     * RF-CFX-04: a conta fixa que a pessoa já confirmou para uma descrição de extrato ("DEB AUT CIA ENERG" = Luz). Como nas
     * recorrências, fica só o HMAC da descrição normalizada, nunca o texto.
     */
    fun billAlias(description: String): Long? {
        val key = TxKey.of(description)
        if (key.isEmpty()) return null
        return prefs.getLong(BILL + vault.hmac(BILL_PREFIX + key), -1L).takeIf { it >= 0 }
    }

    fun learnBill(description: String, billId: Long) {
        val key = TxKey.of(description)
        if (key.isNotEmpty()) prefs.edit().putLong(BILL + vault.hmac(BILL_PREFIX + key), billId).apply()
    }

    private fun update(set: Set<String>) {
        prefs.edit().putStringSet(DISMISSED, set).apply()
        _options.value = read(set)
    }

    private fun read(set: Set<String> = prefs.getStringSet(DISMISSED, emptySet()).orEmpty()) = EngineOptions(dismissed = HashSet(set))

    fun clear() {
        prefs.edit().clear().apply()
        _options.value = EngineOptions()
        _inflation.value = null
    }

    private companion object {
        const val DISMISSED = "dismissed"
        const val INFLATION = "inflation"
        const val PREFIX = "recorrencia:"
        const val BILL = "bill."
        const val BILL_PREFIX = "contafixa:"
    }
}
