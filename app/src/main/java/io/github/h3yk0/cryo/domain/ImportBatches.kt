// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.domain

import io.github.h3yk0.cryo.data.db.Tx
import java.time.LocalDate

/**
 * RF-IMP-23: as importações já feitas, para a pessoa poder apagar uma inteira (pedido do Hanry, 14/11/2026: importou um
 * extrato, o resultado não ficou bom e só dava para apagar lançamento por lançamento). Cada importação é reconhecida
 * pela conta/cartão de destino e pela hora em que foi feita: todas as movimentações de uma mesma importação nascem com
 * o mesmo `createdAt` (`ImportModel`) e com a mesma parte inicial da chave (`a3|…`, `c7|…`). Regra pura.
 */
data class ImportBatch(
    /** "a3" (conta 3) ou "c7" (cartão 7). */
    val scope: String,
    val createdAt: Long,
    val txs: List<Tx>,
) {
    val count: Int get() = txs.size
    val accountId: Long? get() = scope.takeIf { it.startsWith("a") }?.drop(1)?.toLongOrNull()
    val cardId: Long? get() = scope.takeIf { it.startsWith("c") }?.drop(1)?.toLongOrNull()
    val first: LocalDate get() = txs.minOf { it.date }
    val last: LocalDate get() = txs.maxOf { it.date }

    /** O que a importação mexeu no total (entradas menos saídas), para a pessoa reconhecer qual é qual. */
    val net: Long get() = txs.sumOf {
        when (it.type) {
            io.github.h3yk0.cryo.data.db.TxType.INCOME -> it.amount
            io.github.h3yk0.cryo.data.db.TxType.EXPENSE -> -it.amount
            else -> 0L
        }
    }
}

object ImportBatches {
    fun of(txs: List<Tx>): List<ImportBatch> = txs
        .filter { it.importKey != null }
        .groupBy { it.importKey!!.substringBefore('|') to it.createdAt }
        .map { (k, list) -> ImportBatch(k.first, k.second, list.sortedBy { it.date }) }
        .sortedByDescending { it.createdAt }
}
