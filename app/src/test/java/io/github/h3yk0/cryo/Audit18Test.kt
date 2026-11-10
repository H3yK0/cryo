// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo

import io.github.h3yk0.cryo.data.DefaultData
import io.github.h3yk0.cryo.data.db.Account
import io.github.h3yk0.cryo.data.db.CreditCard
import io.github.h3yk0.cryo.domain.AppLanguage
import io.github.h3yk0.cryo.domain.CardMath
import io.github.h3yk0.cryo.domain.Money
import io.github.h3yk0.cryo.domain.ParseContext
import io.github.h3yk0.cryo.domain.PhraseParser
import io.github.h3yk0.cryo.domain.Spoken
import io.github.h3yk0.cryo.importer.Values
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

/**
 * Auditoria da 1.8 (RNF-ROB): entradas hostis ou absurdas nunca derrubam o app nem viram um valor errado em silêncio.
 * Cada teste cobre uma falha confirmada na 1.7 (por sondagem ou pela leitura do código).
 */
class Audit18Test {
    private val today = LocalDate.of(2026, 11, 10)
    private val cats = DefaultData.categories().mapIndexed { i, c -> c.copy(id = (i + 1).toLong()) }
    private val ctx = ParseContext(
        today, listOf(Account(1, "Conta", color = 0)), listOf(CreditCard(1, "Roxo", 500000, 3, 10, 0)), cats, language = AppLanguage.PT_BR,
    )

    @Before fun strings() = JvmStrings.install()

    private val hostile = listOf(
        "tv 1200 em 99999999999x no cartão", "tv 1200 em 0x no cartão", "almoço há 99999999999 dias 30",
        "almoço 99999999999999999999999999", "almoço 9".repeat(400), "mercado 1e999", "uber 18,90 dia 99999999999",
        "almoço 30 em 12/13/99999999999", "x".repeat(5000), "（" + "9".repeat(60) + "）", "almoço 1万" + "9".repeat(40),
        "almoço 32 no débito dia 31/02", "almoço -0", "\u0000￿ 12", "almoço 12 em 4294967297x",
    )

    @Test fun fraseHostilNaoDerrubaNemInventaValor() {
        for (s in hostile) {
            val p = PhraseParser(ctx).parse(s)
            val a = p.amount
            assertTrue("valor de \"${s.take(40)}\": $a", a == null || a in 0..Money.MAX_CENTS)
            assertTrue("parcelas de \"${s.take(40)}\": ${p.installments}", p.installments in 1..48)
            Spoken.entries(s, ctx).forEach { e -> assertTrue(e.amount == null || e.amount!! in 0..Money.MAX_CENTS) }
        }
    }

    @Test fun valorEnormeNaoDaAVolta() {
        assertNull(Money.parse("99999999999999999999999"))
        assertNull(Money.parse("9".repeat(300)))
        assertEquals(123456L, Money.parse("1.234,56"))
        assertEquals(Money.MAX_CENTS / 100 * 100, Money.parse((Money.MAX_CENTS / 100).toString()))
        // Extratos: um valor absurdo é uma linha que não deu para ler, não um número qualquer.
        assertNull(Values.amount("99999999999999999999999,00"))
        // Expoente gigante: recusa na hora (antes, a conta tentava montar um número com um bilhão de casas).
        assertNull(Money.centsOrNull(java.math.BigDecimal("1E+999999999")))
        assertEquals(0L, Money.centsOrNull(java.math.BigDecimal("1E-999999999")))
        assertEquals(-1250L, Values.amount("-12,50"))
        assertNull(Values.centsOrNull(Double.NaN))
        assertNull(Values.centsOrNull(Double.POSITIVE_INFINITY))
        assertNull(Values.centsOrNull(1e300))
        assertEquals(1250L, Values.centsOrNull(12.5))
    }

    @Test fun parcelasNuncaDividemPorZero() {
        assertEquals(listOf(1000L), CardMath.split(1000, 0))
        assertEquals(listOf(1000L), CardMath.split(1000, -3))
        assertEquals(listOf(334L, 333L, 333L), CardMath.split(1000, 3))
        assertEquals(1000L, CardMath.split(1000, 7).sum())
    }
}
