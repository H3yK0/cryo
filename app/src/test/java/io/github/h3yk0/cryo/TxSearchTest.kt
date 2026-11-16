// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo

import io.github.h3yk0.cryo.data.db.Tx
import io.github.h3yk0.cryo.data.db.TxType
import io.github.h3yk0.cryo.domain.SearchScope
import io.github.h3yk0.cryo.domain.TxSearch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

/** RF-EXT-07: a busca do Extrato com período (tudo, mês, ano) e por valor. JVM puro. */
class TxSearchTest {
    private fun tx(id: Long, cents: Long, date: LocalDate, text: String, note: String = "") =
        Tx(id, TxType.EXPENSE, cents, date, text, note = note)

    private val all = listOf(
        tx(1, 3250, LocalDate.of(2026, 11, 5), "Almoço no Centro"),
        tx(2, 13200, LocalDate.of(2026, 11, 20), "Mercado"),
        tx(3, 120000, LocalDate.of(2026, 3, 1), "Aluguel", note = "com condomínio"),
        tx(4, 3200, LocalDate.of(2025, 11, 5), "Farmácia"),
    )

    @Test fun oPeriodoEscolheOndeProcurar() {
        val nov26 = YearMonth.of(2026, 11)
        assertEquals(4, TxSearch.inScope(all, SearchScope.ALL, nov26).size)
        assertEquals(listOf(1L, 2L), TxSearch.inScope(all, SearchScope.MONTH, nov26).map { it.id })
        assertEquals(listOf(1L, 2L, 3L), TxSearch.inScope(all, SearchScope.YEAR, nov26).map { it.id })
        assertEquals(listOf(4L), TxSearch.inScope(all, SearchScope.YEAR, YearMonth.of(2025, 1)).map { it.id })
    }

    @Test fun procuraPorNomeObservacaoECategoria() {
        fun found(q: String, cat: String? = null) = all.filter { TxSearch.matches(it, q, cat) }.map { it.id }
        assertEquals(listOf(1L, 2L, 3L, 4L), found("  "))
        assertEquals(listOf(1L), found("almoco")) // sem acento e sem maiúscula
        assertEquals(listOf(3L), found("Condomínio"))
        assertEquals(listOf(1L, 2L, 3L, 4L), found("casa", cat = "Casa e contas"))
    }

    @Test fun procuraPorValor() {
        assertTrue(TxSearch.amountMatches(3250, "32,50"))
        assertTrue(TxSearch.amountMatches(3250, "32.50"))
        assertTrue(TxSearch.amountMatches(3250, "32,5"))
        assertTrue("o valor inteiro vale para qualquer centavo", TxSearch.amountMatches(3250, "32"))
        assertTrue(TxSearch.amountMatches(3200, "32"))
        assertFalse("32 não é 132", TxSearch.amountMatches(13200, "32"))
        assertFalse(TxSearch.amountMatches(3250, "3"))
        assertTrue(TxSearch.amountMatches(120000, "1.200"))
        assertTrue(TxSearch.amountMatches(120000, "r$ 1.200,00"))
        assertFalse(TxSearch.amountMatches(120000, "1.201"))
        assertFalse("texto com número não é valor", TxSearch.amountMatches(3250, "sala 32"))
        // Na busca inteira: "32" acha o almoço de 32,50 e a farmácia de 32,00, não o mercado de 132,00.
        assertEquals(listOf(1L, 4L), all.filter { TxSearch.matches(it, "32", null) }.map { it.id })
    }
}
