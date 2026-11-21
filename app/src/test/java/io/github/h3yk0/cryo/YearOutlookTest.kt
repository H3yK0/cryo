// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo

import io.github.h3yk0.cryo.data.db.Bill
import io.github.h3yk0.cryo.data.db.Budget
import io.github.h3yk0.cryo.data.db.Category
import io.github.h3yk0.cryo.data.db.CategoryKind
import io.github.h3yk0.cryo.domain.Ledger
import io.github.h3yk0.cryo.domain.Snapshot
import io.github.h3yk0.cryo.domain.engine.Certainty
import io.github.h3yk0.cryo.domain.engine.ChartWindow
import io.github.h3yk0.cryo.domain.engine.FinanceEngine
import io.github.h3yk0.cryo.domain.engine.Projection
import io.github.h3yk0.cryo.domain.engine.SignalKind
import io.github.h3yk0.cryo.domain.engine.YearOutlooks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * RF-MOT-17 (1.6, LA8-3): "Como vou fechar o ano?". Até quando vai, os sinais vermelhos, a janela do zoom da primeira vista
 * (contém os sinais) e o arrastar e aproximar (mudam a janela sem sair do período).
 */
class YearOutlookTest {
    init { JvmStrings.install() }

    private val today = LocalDate.of(2026, 11, 20)

    /** Uma projeção inventada: o saldo [start] muda [perDay] por dia, com um tombo de [drop] no dia [dropAt]. */
    private fun projection(days: Int, start: Long, perDay: Long, dropAt: Int = -1, drop: Long = 0): Projection {
        val mid = (0..days).map { i -> start + perDay * i - (if (dropAt in 1..i) drop else 0L) }
        return Projection(
            today, days, start, mid.map { it - 10_000 }, mid, mid.map { it + 10_000 }, emptyList(),
            today, mid.min(), 0.0, null, 1, Certainty.HIGH,
        )
    }

    @Test fun ateOFimDoAnoOuDoQueVem() {
        assertEquals(LocalDate.of(2027, 12, 31), YearOutlooks.until(today))
        assertTrue(YearOutlooks.hasNextYear(today))
        assertEquals(LocalDate.of(2026, 12, 31), YearOutlooks.until(LocalDate.of(2026, 3, 1)))
        assertFalse(YearOutlooks.hasNextYear(LocalDate.of(2026, 3, 1)))
    }

    @Test fun sinaisDoAno() {
        val ipva = Bill(id = 1, name = "IPVA", amount = 150_000, dueDay = 20, startYm = 202701, every = 12)
        val l = Ledger(Snapshot(bills = listOf(ipva)), today)
        // Saldo de R$ 1.000 que cai R$ 15 por dia: fica negativo no dia 67.
        val p = projection(406, 100_000, -1_500)
        val days = YearOutlooks.daysUntilEndOf(p, 2026)
        assertEquals(41, days)
        val thisYear = YearOutlooks.signals(l, p, days)
        assertEquals(listOf(SignalKind.TIGHTEST), thisYear.map { it.kind })
        val all = YearOutlooks.signals(l, p, p.horizonDays)
        assertTrue(all.any { it.kind == SignalKind.FIRST_NEGATIVE && it.date == today.plusDays(67) })
        assertTrue(all.any { it.kind == SignalKind.BIG_BILL && it.name == "IPVA" && it.date == LocalDate.of(2027, 1, 20) && it.amount == 150_000L })
        assertTrue(all.any { it.kind == SignalKind.TIGHTEST })
    }

    @Test fun zoomDaPrimeiraVistaContemOsSinais() {
        val p = projection(406, 100_000, 500, dropAt = 120, drop = 200_000)
        val l = Ledger(Snapshot(), today)
        val signals = YearOutlooks.signals(l, p, p.horizonDays)
        val w = YearOutlooks.zoomWindow(p, signals, p.horizonDays)
        for (s in signals) {
            val x = YearOutlooks.index(p, s.date).toFloat()
            assertTrue("$s fora de $w", x >= w.from && x <= w.to)
        }
        assertTrue(w.span >= YearOutlooks.MIN_WINDOW)
        assertTrue("o zoom aproxima", w.span < p.horizonDays)
        // Sem sinais, o período inteiro (sem zoom).
        assertEquals(ChartWindow(0f, 406f), YearOutlooks.zoomWindow(p, emptyList(), 406))
    }

    @Test fun arrastarEAproximarFicamDentroDoPeriodo() {
        val w = ChartWindow(100f, 160f)
        assertEquals(ChartWindow(130f, 190f), YearOutlooks.pan(w, 30f, 406))
        assertEquals(ChartWindow(346f, 406f), YearOutlooks.pan(w, 1_000f, 406))
        assertEquals(ChartWindow(0f, 60f), YearOutlooks.pan(w, -1_000f, 406))
        val closer = YearOutlooks.zoom(w, 2f, 130f, 406)
        assertEquals(30f, closer.span, 0.01f)
        assertTrue(closer.from >= 100f && closer.to <= 160f)
        // Não aproxima mais que o mínimo, nem afasta além do período.
        assertEquals(YearOutlooks.MIN_WINDOW, YearOutlooks.zoom(w, 100f, 130f, 406).span, 0.01f)
        assertEquals(ChartWindow(0f, 406f), YearOutlooks.zoom(w, 0.01f, 130f, 406))
    }

    @Test fun oMotorVaiAteOFimDoAnoComOOrcamento() {
        val s = Personas.generate(Personas.spec(Personas.Kind.STABLE), today, 21)
        val r = FinanceEngine.run(s, today)
        val year = r.year!!
        assertEquals(LocalDate.of(2027, 12, 31), year.dateAt(year.horizonDays))
        // RF-PRV-11 nos meses seguintes: um orçamento novo (sem histórico) conta inteiro em cada mês.
        val travel = Category(id = 900, name = "Viagem", kind = CategoryKind.EXPENSE, icon = "flight", color = 0)
        val withBudget = s.copy(categories = s.categories + travel, budgets = s.budgets + Budget(900, 50_000))
        val r2 = FinanceEngine.run(withBudget, today)
        val diff = year.mid[year.horizonDays] - r2.year!!.mid[year.horizonDays]
        // De amanhã até 31/12/2027 são 13 meses e pouco: perto de 13 × R$ 500 (a faixa de sorteio muda um pouco).
        assertTrue("diferença $diff", diff in 600_000L..720_000L)
    }
}
