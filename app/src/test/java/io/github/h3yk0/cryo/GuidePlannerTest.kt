// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo

import io.github.h3yk0.cryo.data.db.Tx
import io.github.h3yk0.cryo.data.db.TxType
import io.github.h3yk0.cryo.domain.GuideCount
import io.github.h3yk0.cryo.domain.GuideFacts
import io.github.h3yk0.cryo.domain.GuideLog
import io.github.h3yk0.cryo.domain.GuidePlace
import io.github.h3yk0.cryo.domain.GuidePlanner
import io.github.h3yk0.cryo.domain.Lesson
import io.github.h3yk0.cryo.domain.Snapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Modifier
import java.time.LocalDate

/** RF-GUI-01..04: as regras do guia "Aprender o Cryo", com dados e datas inventados (JVM puro, sem Android). */
class GuidePlannerTest {
    private val day = LocalDate.of(2026, 11, 10)

    /** Alguém que já anotou algumas coisas e abriu o app há horas. */
    private val base = GuideFacts(txCount = 12, historyDays = 10, minutesSinceFirstOpen = 500)

    /** Alguém com meses de uso, cartão e investimento, que ainda não explorou nada. */
    private val rich = GuideFacts(
        txCount = 25, historyDays = 120, cards = 1, cardPurchases = 4, investments = 1, investStaleDays = 70, minutesSinceFirstOpen = 90_000,
    )

    private fun counted(vararg keys: String, times: Int = 1): GuideLog {
        var g = GuideLog()
        for (k in keys) repeat(times) { g = GuidePlanner.counted(g, k) }
        return g
    }

    /** RF-GUI-03: o dia zero é do passeio. Sem lançamento, ou nos dois primeiros minutos, o guia não fala. */
    @Test fun nadaAntesDoPrimeiroLancamentoNemNosPrimeirosMinutos() {
        assertNull(GuidePlanner.next(base.copy(txCount = 0), GuideLog(), day, GuidePlace.HOME))
        assertNull(GuidePlanner.next(base.copy(minutesSinceFirstOpen = 1), GuideLog(), day, GuidePlace.HOME))
        assertNotNull(GuidePlanner.next(base.copy(minutesSinceFirstOpen = 2), GuideLog(), day, GuidePlace.HOME))
    }

    /** RF-GUI-03: no máximo uma dica por dia e três em sete dias. */
    @Test fun umaPorDiaETresPorSemana() {
        var g = GuideLog()
        for (i in 0L..2L) {
            val d = day.plusDays(i)
            val l = GuidePlanner.next(base, g, d, null)!!
            g = GuidePlanner.shown(g, l, d)
            assertNull("segunda dica no mesmo dia", GuidePlanner.next(base, g, d, null))
        }
        // Três em três dias: o resto da semana fica em silêncio; a quarta só quando a primeira completa 7 dias.
        for (i in 3L..6L) assertNull("dia $i", GuidePlanner.next(base, g, day.plusDays(i), null))
        assertNotNull(GuidePlanner.next(base, g, day.plusDays(7), null))
    }

    /** RF-GUI-03: a lição dispensada volta 7 dias depois; dispensada pela segunda vez, só fica na lista. */
    @Test fun aLicaoVoltaEmSeteDiasEDepoisSoFicaNaLista() {
        assertEquals(Lesson.PHRASE, GuidePlanner.next(base, GuideLog(), day, null))
        var g = GuidePlanner.shown(GuideLog(), Lesson.PHRASE, day)
        assertNotEquals(Lesson.PHRASE, GuidePlanner.next(base, g, day.plusDays(1), null))
        assertNotEquals(Lesson.PHRASE, GuidePlanner.next(base, g, day.plusDays(6), null))
        assertEquals(Lesson.PHRASE, GuidePlanner.next(base, g, day.plusDays(7), null))
        g = GuidePlanner.shown(g, Lesson.PHRASE, day.plusDays(7))
        for (i in listOf(14L, 30L, 400L)) assertNotEquals(Lesson.PHRASE, GuidePlanner.next(base, g, day.plusDays(i), null))
        // Na lista ela continua "a aprender": só tocar nela (ou usar o recurso) a dá como conhecida.
        assertFalse(GuidePlanner.known(Lesson.PHRASE, base, g))
        assertTrue(GuidePlanner.known(Lesson.PHRASE, base, GuidePlanner.seen(g, Lesson.PHRASE)))
    }

    /** RF-GUI-03: três "Agora não" seguidos e o guia descansa 14 dias; um "Me mostra" no meio zera a conta. */
    @Test fun tresAgoraNaoSeguidosPausamDuasSemanas() {
        var g = GuideLog()
        repeat(3) { g = GuidePlanner.later(g, day) }
        assertEquals(day.toEpochDay() + 14, g.pausedUntil)
        assertNull(GuidePlanner.next(base, g, day.plusDays(13), null))
        assertNotNull(GuidePlanner.next(base, g, day.plusDays(14), null))

        var h = GuidePlanner.later(GuidePlanner.later(GuideLog(), day), day)
        h = GuidePlanner.later(GuidePlanner.seen(h, Lesson.VOICE), day)
        assertEquals(0L, h.pausedUntil)
        assertEquals(1, h.laterStreak)
    }

    /** RF-GUI-03: chave desligada = silêncio; no dia de um momento (meta completa, orçamento estourado) também. */
    @Test fun chaveDesligadaEDiaDeMomento() {
        assertNull(GuidePlanner.next(base, GuideLog(enabled = false), day, GuidePlace.HOME))
        val g = GuideLog(momentDay = day.toEpochDay())
        assertNull(GuidePlanner.next(base, g, day, GuidePlace.HOME))
        assertNotNull(GuidePlanner.next(base, g, day.plusDays(1), GuidePlace.HOME))
    }

    /** RF-GUI-04: degrau mais baixo primeiro; a aba aberta e o que os dados pedem passam na frente. */
    @Test fun aOrdemDosDegrausEOsPontosDosDados() {
        // Sem nada especial: a primeira lição do primeiro degrau.
        assertEquals(Lesson.PHRASE, GuidePlanner.next(base, GuideLog(), day, null))
        // Na aba Extrato, a lição do Extrato do mesmo degrau passa na frente.
        assertEquals(Lesson.EDIT, GuidePlanner.next(base, GuideLog(), day, GuidePlace.TXS))
        // Mais de 30 lançamentos sem cópia nenhuma: proteger passa na frente de tudo, em qualquer aba.
        val many = base.copy(txCount = 31)
        assertEquals(Lesson.BACKUP, GuidePlanner.next(many, GuideLog(), day, GuidePlace.HOME))
        assertEquals(Lesson.PHRASE, GuidePlanner.next(many.copy(backupOn = true), GuideLog(), day, GuidePlace.HOME))
        // Com o primeiro degrau feito: 10 gastos sem categoria puxam as categorias (e não as contas fixas, do mesmo degrau).
        val started = counted(GuideCount.PHRASE, GuideCount.VOICE, GuideCount.EDIT)
        assertEquals(Lesson.CATEGORIES, GuidePlanner.next(base.copy(uncategorized = 10), started, day, GuidePlace.PLAN))
        assertEquals(Lesson.BILLS, GuidePlanner.next(base.copy(uncategorized = 2), started, day, GuidePlace.PLAN))
        // Cartão com compras e nenhuma fatura paga pelo app.
        assertTrue(GuidePlanner.score(Lesson.CARD, rich, null) > GuidePlanner.score(Lesson.CARD, rich.copy(cardPayments = 2), null))
        // Três meses de histórico: a previsão; investimento parado há 60 dias; o que se repete sem conta fixa.
        assertTrue(GuidePlanner.score(Lesson.FORECAST, rich, null) > GuidePlanner.score(Lesson.FORECAST, rich.copy(historyDays = 40), null))
        assertTrue(GuidePlanner.score(Lesson.INVEST, rich, null) > GuidePlanner.score(Lesson.INVEST, rich.copy(investStaleDays = 10), null))
        assertTrue(GuidePlanner.score(Lesson.RECURRING, rich.copy(recurringOpen = 2), null) > GuidePlanner.score(Lesson.RECURRING, rich, null))
    }

    /** RF-GUI-04: mesma situação, mesma lição (não há sorteio). */
    @Test fun mesmoEstadoMesmaLicao() {
        val g = GuidePlanner.shown(counted(GuideCount.PHRASE), Lesson.VOICE, day.minusDays(9))
        val first = GuidePlanner.next(rich, g, day, GuidePlace.INSIGHTS)
        repeat(50) { assertEquals(first, GuidePlanner.next(rich, g, day, GuidePlace.INSIGHTS)) }
    }

    /** O jeito de cada lição ser dada como conhecida sem aparecer (a pessoa já faz sozinha). */
    private fun knownCase(l: Lesson): Pair<GuideFacts, GuideLog> = when (l) {
        Lesson.PHRASE -> rich to counted(GuideCount.PHRASE)
        Lesson.VOICE -> rich to counted(GuideCount.VOICE)
        Lesson.EDIT -> rich to counted(GuideCount.EDIT)
        Lesson.CATEGORIES -> rich to counted(GuideCount.CATEGORIES)
        Lesson.CARD -> rich.copy(cardPayments = 1) to GuideLog()
        Lesson.BILLS -> rich.copy(bills = 1) to GuideLog()
        Lesson.BUDGETS -> rich.copy(budgets = 1) to GuideLog()
        Lesson.MONTH -> rich.copy(budgets = 1) to counted(GuideCount.MONTH)
        Lesson.GOALS -> rich.copy(goals = 1) to GuideLog()
        Lesson.DEBTS -> rich.copy(debts = 1) to GuideLog()
        Lesson.FORECAST -> rich to counted(GuideCount.INSIGHTS, times = 3)
        Lesson.HEALTH -> rich to counted(GuideCount.HEALTH)
        Lesson.RECURRING -> rich to counted(GuideCount.RECURRING)
        Lesson.BACKUP -> rich.copy(backupOn = true) to GuideLog()
        Lesson.LOCK -> rich.copy(lockOn = true) to GuideLog()
        Lesson.IMPORT -> rich.copy(imported = 3) to GuideLog()
        Lesson.SHOPPING -> rich to counted(GuideCount.SHOPPING)
        Lesson.INVEST -> rich.copy(investUpdates = 1, investStaleDays = 2) to GuideLog()
    }

    /** RF-GUI-02: um caso por lição. Ela pode aparecer para quem ainda não usa e some quando a pessoa já faz sozinha. */
    @Test fun cadaLicaoSomeQuandoAPessoaJaFazSozinha() {
        for (l in Lesson.entries) {
            val before = if (l == Lesson.MONTH) rich.copy(budgets = 1) else rich
            assertFalse("$l conhecida cedo demais", GuidePlanner.known(l, before, GuideLog()))
            assertTrue("$l devia poder aparecer", GuidePlanner.eligible(l, before, GuideLog(), day))
            val (f, g) = knownCase(l)
            assertTrue("$l devia estar conhecida", GuidePlanner.known(l, f, g))
            assertFalse("$l não pode mais aparecer", GuidePlanner.eligible(l, f, g, day))
            assertEquals(GuidePlanner.progress(before, GuideLog()) + 1, GuidePlanner.progress(f, g))
        }
        // Abrir Análises uma ou duas vezes ainda não é "conhecer a previsão" (a aba abre até sem querer).
        assertFalse(GuidePlanner.known(Lesson.FORECAST, rich, counted(GuideCount.INSIGHTS, times = 2)))
    }

    /** RF-GUI-02: não se fala de fatura para quem não tem cartão, nem de painel do mês para quem não tem orçamento. */
    @Test fun licaoSoApareceQuandoFazSentido() {
        assertFalse(GuidePlanner.useful(Lesson.CARD, base))
        assertFalse(GuidePlanner.useful(Lesson.CARD, base.copy(cards = 1)))
        assertTrue(GuidePlanner.useful(Lesson.CARD, base.copy(cards = 1, cardPurchases = 1)))
        assertFalse(GuidePlanner.useful(Lesson.MONTH, base))
        assertFalse(GuidePlanner.useful(Lesson.INVEST, base))
        assertFalse(GuidePlanner.useful(Lesson.FORECAST, base))
        assertTrue(GuidePlanner.useful(Lesson.FORECAST, base.copy(historyDays = 30)))
        assertFalse(GuidePlanner.useful(Lesson.BACKUP, base.copy(txCount = 4)))
        // Com 60 dias de uso ou algo que se repete já encontrado pelo motor.
        assertFalse(GuidePlanner.useful(Lesson.RECURRING, base))
        assertTrue(GuidePlanner.useful(Lesson.RECURRING, base.copy(recurringOpen = 1)))
    }

    /** Quem nunca toca em nada vê cada lição útil uma vez, no ritmo de três por semana, e depois o guia se cala. */
    @Test fun todasAsLicoesUteisChegamUmaVezNoRitmoCerto() {
        var g = GuideLog()
        val seen = ArrayList<Lesson>()
        val days = ArrayList<Long>()
        for (i in 0L..200L) {
            val d = day.plusDays(i)
            val l = GuidePlanner.next(rich, g, d, GuidePlace.HOME) ?: continue
            g = GuidePlanner.seen(GuidePlanner.shown(g, l, d), l)
            seen += l; days += i
        }
        assertEquals("sem repetir", seen.size, seen.toSet().size)
        assertEquals(Lesson.entries.toSet() - Lesson.MONTH, seen.toSet())
        for (i in days) assertTrue("mais de 3 na semana do dia $i", days.count { it in i..i + 6 } <= GuidePlanner.MAX_PER_WEEK)
        assertEquals(GuidePlanner.total - 1, GuidePlanner.progress(rich, g))
        assertNull(GuidePlanner.next(rich, g, day.plusDays(300), GuidePlace.HOME))
    }

    /** RF-GUI-01: os fatos saem dos dados como quantidades e sim/não; nenhum campo guarda texto. */
    @Test fun osFatosSaoSoQuantidades() {
        val s = Snapshot(
            txs = listOf(
                Tx(1, TxType.EXPENSE, 3250, day.minusDays(40), "Almoço no centro", accountId = 1),
                Tx(2, TxType.EXPENSE, 9900, day.minusDays(3), "Mercado", accountId = 1, categoryId = 4, importKey = "a1|ofx.id:9"),
                Tx(3, TxType.EXPENSE, 12000, day.minusDays(2), "Loja", cardId = 2, invoiceYm = 202611),
                Tx(4, TxType.CARD_PAYMENT, 12000, day.minusDays(1), accountId = 1, cardId = 2, invoiceYm = 202611),
            ),
        )
        val f = GuideFacts.of(s, day, backupOn = true, lockOn = false, recurringOpen = 2, minutesSinceFirstOpen = 7)
        assertEquals(4, f.txCount)
        assertEquals(2, f.uncategorized)
        assertEquals(40, f.historyDays)
        assertEquals(1, f.cardPurchases)
        assertEquals(1, f.cardPayments)
        assertEquals(1, f.imported)
        assertEquals(0, f.investments)
        assertNull(f.investStaleDays)
        assertTrue(f.backupOn)
        val allowed = setOf<Class<*>>(Int::class.javaPrimitiveType!!, Long::class.javaPrimitiveType!!, Boolean::class.javaPrimitiveType!!, Integer::class.java)
        for (field in GuideFacts::class.java.declaredFields) {
            if (Modifier.isStatic(field.modifiers)) continue
            assertTrue("${field.name} não pode guardar ${field.type.simpleName}", field.type in allowed)
        }
    }
}
