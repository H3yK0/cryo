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
 * **HMAC-SHA256** da chave (nunca a descrição), a inflação que ela informou para comparar o rendimento e (1.6, RF-PRV-10)
 * as duas chaves da previsão do fim do mês. O resto do motor fica só na memória. No modo de teste, um arquivo separado.
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
     * As entradas avulsas na previsão por cenários do motor. Null = o Cryo decide sozinho (liga com avulsas em 2 dos 3
     * últimos meses). Desde 21/11/2026 o fim do mês da tela é o real (RF-PRV-10, sem cenários) e esta escolha saiu da tela;
     * fica só para o motor e para os testes.
     */
    var extraIncome: Boolean?
        get() = if (prefs.contains(EXTRA)) prefs.getBoolean(EXTRA, false) else null
        set(v) {
            prefs.edit().apply { if (v == null) remove(EXTRA) else putBoolean(EXTRA, v) }.apply()
            _options.value = read()
        }

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

    private fun read(set: Set<String> = prefs.getStringSet(DISMISSED, emptySet()).orEmpty()) =
        EngineOptions(dismissed = HashSet(set), extraIncome = if (prefs.contains(EXTRA)) prefs.getBoolean(EXTRA, false) else null)

    fun clear() {
        prefs.edit().clear().apply()
        _options.value = EngineOptions()
        _inflation.value = null
    }

    private companion object {
        const val DISMISSED = "dismissed"
        const val INFLATION = "inflation"
        const val EXTRA = "extra_income"
        const val PREFIX = "recorrencia:"
        const val BILL = "bill."
        const val BILL_PREFIX = "contafixa:"
    }
}
