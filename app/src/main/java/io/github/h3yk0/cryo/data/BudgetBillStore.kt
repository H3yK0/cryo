// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.data

import android.content.Context

/**
 * RF-ORC-09 (1.6): o "Agora não" das sugestões do orçamento com as contas fixas. Guarda, por categoria, a marca das contas
 * fixas que estava na hora ([io.github.h3yk0.cryo.domain.BudgetBills.signature]): quando elas mudam, a sugestão volta. Só
 * números (ids e marcas); no modo de teste, um arquivo separado.
 */
class BudgetBillStore(ctx: Context, sandbox: Boolean) {
    private val prefs = ctx.getSharedPreferences(if (sandbox) "cryo_budget_bills_teste" else "cryo_budget_bills", Context.MODE_PRIVATE)

    fun dismissed(): Map<Long, Int> = prefs.all.mapNotNull { (k, v) -> k.toLongOrNull()?.let { id -> (v as? Int)?.let { id to it } } }.toMap()

    fun dismiss(categoryId: Long, signature: Int) = prefs.edit().putInt(categoryId.toString(), signature).apply()

    fun clear() = prefs.edit().clear().apply()
}
