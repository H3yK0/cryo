// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.data

import android.content.Context
import io.github.h3yk0.cryo.domain.Bucket
import io.github.h3yk0.cryo.domain.IncomeGuess
import io.github.h3yk0.cryo.domain.IncomePlanner
import io.github.h3yk0.cryo.domain.PlanInputs

/**
 * RF-ORC-07 (1.6): as escolhas da pessoa no orçamento pela renda (PRI-09: o que ela muda fica guardado e as contas seguem).
 * Enquanto ela não mexe, valem os números que o Cryo achou ([IncomeGuess]) e a regra 50/30/20. Só números e ids de
 * categoria; no modo de teste, um arquivo separado.
 */
class IncomePlanStore(ctx: Context, sandbox: Boolean) {
    private val prefs = ctx.getSharedPreferences(if (sandbox) "cryo_income_plan_teste" else "cryo_income_plan", Context.MODE_PRIVATE)

    /** As escolhas, com o que o Cryo achou no lugar do que ela ainda não informou. A renda variável começa no mês mais fraco. */
    fun inputs(guess: IncomeGuess): PlanInputs = PlanInputs(
        fixed = prefs.getLong(FIXED, -1L).takeIf { it >= 0 } ?: guess.fixed,
        variable = prefs.getLong(VARIABLE, -1L).takeIf { it >= 0 } ?: guess.variableLow,
        useVariable = prefs.getBoolean(USE_VARIABLE, false),
        needsPct = prefs.getInt(NEEDS, IncomePlanner.NEEDS_PCT),
        wantsPct = prefs.getInt(WANTS, IncomePlanner.WANTS_PCT),
    )

    fun setFixed(v: Long) = prefs.edit().putLong(FIXED, v.coerceAtLeast(0)).apply()
    fun setVariable(v: Long) = prefs.edit().putLong(VARIABLE, v.coerceAtLeast(0)).apply()
    fun setUseVariable(v: Boolean) = prefs.edit().putBoolean(USE_VARIABLE, v).apply()

    /** As porcentagens do essencial e do estilo de vida (o guardar é o que sobra até 100%). */
    fun setSplit(needsPct: Int, wantsPct: Int) {
        val n = needsPct.coerceIn(0, 100)
        prefs.edit().putInt(NEEDS, n).putInt(WANTS, wantsPct.coerceIn(0, 100 - n)).apply()
    }

    /** "Usar o que o Cryo achou": a renda volta a ser a dos lançamentos. */
    fun resetIncome() = prefs.edit().remove(FIXED).remove(VARIABLE).apply()

    /** "Voltar para 50/30/20". */
    fun resetSplit() = prefs.edit().remove(NEEDS).remove(WANTS).apply()

    /** As categorias que a pessoa trocou de parte (essencial ou estilo de vida). */
    fun buckets(): Map<Long, Bucket> = prefs.getStringSet(BUCKETS, emptySet()).orEmpty().mapNotNull { e ->
        val id = e.substringBefore(':').toLongOrNull() ?: return@mapNotNull null
        id to if (e.endsWith(":N")) Bucket.NEEDS else Bucket.WANTS
    }.toMap()

    fun setBucket(categoryId: Long, bucket: Bucket) {
        val others = prefs.getStringSet(BUCKETS, emptySet()).orEmpty().filter { it.substringBefore(':') != categoryId.toString() }
        val tag = if (bucket == Bucket.NEEDS) "N" else "W"
        prefs.edit().putStringSet(BUCKETS, (others + "$categoryId:$tag").toSet()).apply()
    }

    fun clear() = prefs.edit().clear().apply()

    private companion object {
        const val FIXED = "fixed"
        const val VARIABLE = "variable"
        const val USE_VARIABLE = "use_variable"
        const val NEEDS = "needs_pct"
        const val WANTS = "wants_pct"
        const val BUCKETS = "buckets"
    }
}
