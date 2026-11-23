// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo

import io.github.h3yk0.cryo.data.DefaultData
import io.github.h3yk0.cryo.data.db.Account
import io.github.h3yk0.cryo.data.db.AccountType
import io.github.h3yk0.cryo.data.db.CreditCard
import io.github.h3yk0.cryo.data.db.TxType
import io.github.h3yk0.cryo.domain.AppLanguage
import io.github.h3yk0.cryo.domain.ParseContext
import io.github.h3yk0.cryo.domain.Spoken
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * RF-VOZ-05/06/07: o que o Whisper escreve → o que o Cryo entende. As frases vêm do teste de 09/11/2026 (as que a regra
 * da 1.5 errava) e do jeito que o Whisper costuma escrever português falado.
 */
class SpokenTest {
    init { JvmStrings.install() }

    private val today = LocalDate.of(2026, 11, 9)
    private val cats = DefaultData.categories().mapIndexed { i, c -> c.copy(id = (i + 1).toLong()) }
    private fun cat(name: String) = cats.first { it.name == name }.id
    private val ctx = ParseContext(
        today = today,
        accounts = listOf(Account(1, "Conta principal", type = AccountType.CHECKING, color = 0), Account(2, "Carteira", type = AccountType.CASH, color = 0)),
        cards = listOf(CreditCard(1, "Cartão Roxo", 500_000, 3, 10, 0)),
        categories = cats, defaultAccountId = 1, language = AppLanguage.PT_BR,
    )

    private fun phrases(s: String) = Spoken.phrases(s, AppLanguage.PT_BR, Spoken.vocabulary(ctx))
    private fun one(s: String) = Spoken.entries(s, ctx).also { assertEquals("uma frase em \"$s\": $it", 1, it.size) }.single()

    @Test fun centavosFalados() {
        assertEquals(1890L, one("Uber 18 e 90").amount)
        assertEquals(1890L, one("uber dezoito e noventa").amount)
        assertEquals(3250L, one("almoço trinta e dois e cinquenta").amount)
        assertEquals(1850L, one("padaria 18 e 50 reais").amount)
        assertEquals(3250L, one("almoço 32 reais e 50 centavos").amount)
    }

    @Test fun numerosPorExtenso() {
        assertEquals(12000L, one("mercado cento e vinte reais").amount)
        assertEquals(300000L, one("aluguel três mil").amount)
        assertEquals(120000L, one("tv mil e duzentos").amount)
        assertEquals(250000L, one("notebook dois mil e quinhentos").amount)
        assertEquals(1500L, one("farmácia quinze reais").amount)
        assertEquals(350000L, one("aluguel 3 mil e 500").amount)
        assertEquals(1000L, one("lanche dez conto").amount)
    }

    @Test fun pequenosSoltosSaoQuantidade() {
        assertEquals(listOf("um café 5 reais"), phrases("um café 5 reais"))
        assertEquals(listOf("dois pães 3,50"), phrases("dois pães 3,50"))
        val tv = one("tv 1200 em dez vezes no cartão roxo")
        assertEquals(10, tv.installments)
        assertEquals(120000L, tv.amount)
    }

    @Test fun apelidoDoCartao() {
        val r = one("pizza 60 com o roxinho")
        assertEquals(1L, r.cardId)
        assertEquals(6000L, r.amount)
    }

    @Test fun variosLancamentos() {
        val r = Spoken.entries("almoço 30 e uber 15", ctx)
        assertEquals(2, r.size)
        assertEquals(3000L, r[0].amount)
        assertEquals(1500L, r[1].amount)
        assertEquals(cat("Transporte"), r[1].categoryId)
        // A forma de pagamento dita no fim vale para as duas.
        val c = Spoken.entries("mercado 120 e farmácia 30 no cartão roxo", ctx)
        assertEquals(listOf(1L, 1L), c.map { it.cardId })
        // A data dita uma vez também.
        val d = Spoken.entries("ontem almoço 30, uber 15", ctx)
        assertEquals(listOf(today.minusDays(1), today.minusDays(1)), d.map { it.date })
        // O verbo do começo vale para as outras.
        val i = Spoken.entries("recebi 200 do João e 100 da Maria", ctx)
        assertEquals(listOf(TxType.INCOME, TxType.INCOME), i.map { it.type })
    }

    @Test fun pontuacaoDoWhisperNaoDivide() {
        assertEquals(listOf("Almoço 32,50 no débito"), phrases("Almoço, 32,50, no débito."))
        assertEquals(listOf("Mercado R$ 1.200,00"), phrases("Mercado, R$ 1.200,00."))
        assertEquals(120000L, one("Mercado, R$ 1.200,00.").amount)
    }

    @Test fun correcaoFalada() {
        val r = one("almoço 32, não, 35")
        assertEquals(3500L, r.amount)
        assertEquals("Almoço", r.description)
        assertEquals(4000L, one("gasolina 50 quer dizer 40").amount)
        val m = one("farmácia 20 no débito, não, na carteira")
        assertEquals(2L, m.accountId)
    }

    @Test fun ruidoEAlucinacoes() {
        assertEquals(emptyList<String>(), phrases("Legendas pela comunidade Amara.org"))
        assertEquals(emptyList<String>(), phrases("Obrigado por assistir!"))
        assertEquals(emptyList<String>(), phrases("[Música]"))
        assertEquals(emptyList<String>(), phrases("..."))
        assertEquals(listOf("Uber 15"), phrases("Uber 15. Obrigado por assistir."))
        assertEquals("Televisão meu e o meu cartão", Spoken.clean("Televisão meu e o meu e o meu e o meu e o meu e o meu cartão"))
        assertEquals("almoço 30", Spoken.clean("é, tipo, almoço 30"))
    }

    /**
     * ERR-069: um separador de linha que não é o "\\n" comum (U+2028, U+2029, U+0085) chegava inteiro até a leitura dos
     * números, e a expressão que separa a pontuação não achava nada: o app fechava no meio da voz.
     */
    @Test fun separadorDeLinhaIncomumNaoFechaOApp() {
        for (sep in listOf("\u2028", "\u2029", "\u0085", "\r", "\u000B")) {
            val heard = "almoço trinta e dois${sep}no débito"
            val out = Spoken.phrases(heard, AppLanguage.PT_BR)
            assertEquals(listOf("almoço 32 no débito"), out.map { it.lowercase() })
            assertTrue(Spoken.entries(heard, ctx).isNotEmpty())
        }
    }

    @Test fun outrasLinguasSoLimpaEDivide() {
        assertEquals(listOf("lunch 12.50", "uber 8"), Spoken.phrases("Lunch 12.50 and uber 8.", AppLanguage.EN).map { it.lowercase() })
        assertTrue(Spoken.phrases("Thank you for watching.", AppLanguage.EN).isEmpty())
    }
}
