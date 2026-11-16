// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.domain

import io.github.h3yk0.cryo.data.db.Tx
import java.time.YearMonth

/** RF-EXT-07: onde a busca do Extrato procura. */
enum class SearchScope { ALL, MONTH, YEAR }

/**
 * RF-EXT-07 (pedido do Hanry, 17/11/2026): a busca do Extrato escolhe o período (tudo, um mês, um ano) e entende valor
 * além de nome e categoria. Regra pura.
 */
object TxSearch {
    fun inScope(txs: List<Tx>, scope: SearchScope, ym: YearMonth): List<Tx> = when (scope) {
        SearchScope.ALL -> txs
        SearchScope.MONTH -> txs.filter { it.date.year == ym.year && it.date.monthValue == ym.monthValue }
        SearchScope.YEAR -> txs.filter { it.date.year == ym.year }
    }

    /** Busca vazia aceita tudo. Texto: descrição, observação ou nome da categoria. Número: o valor. */
    fun matches(t: Tx, query: String, categoryName: String?): Boolean {
        val q = query.trim().normalized()
        if (q.isEmpty()) return true
        if (t.description.normalized().contains(q) || t.note.normalized().contains(q)) return true
        if (categoryName != null && categoryName.normalized().contains(q)) return true
        return amountMatches(t.amount, q)
    }

    /**
     * "32,50", "32.50", "R$ 1.200" ou só "32": o valor inteiro vale para qualquer centavo ("32" acha 32,00 e 32,50, mas
     * não 132,00); com centavos, o começo precisa bater ("32,5" acha 32,50).
     */
    fun amountMatches(cents: Long, query: String): Boolean {
        var q = query.filter { it.isDigit() || it == ',' || it == '.' }
        if (q.none { it.isDigit() } || query.any { it.isLetter() && it !in "r$" }) return false
        // O ponto seguido de três dígitos é separador de milhar; qualquer outro ponto é o decimal.
        q = Regex("""\.(?=\d{3}(\D|$))""").replace(q, "").replace('.', ',')
        val plain = "${cents / 100},${(cents % 100).toString().padStart(2, '0')}"
        return if (q.contains(',')) plain.startsWith(q) else plain.startsWith("$q,")
    }
}
