// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.domain.engine

import io.github.h3yk0.cryo.data.db.Bill
import io.github.h3yk0.cryo.data.db.CategoryKind
import io.github.h3yk0.cryo.domain.BillCycle
import io.github.h3yk0.cryo.domain.Ledger
import io.github.h3yk0.cryo.domain.dayClamped
import io.github.h3yk0.cryo.domain.ym
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

/** RF-MOT-17: o que o gráfico do ano marca em vermelho. */
enum class SignalKind { FIRST_NEGATIVE, TIGHTEST, BIG_BILL }

/** Um sinal: o dia, o tipo, o valor (o saldo, ou o valor da conta) e, para a conta, o nome dela. */
data class YearSignal(val date: LocalDate, val kind: SignalKind, val amount: Long, val name: String? = null)

/** A janela que o gráfico mostra, em dias contados a partir de hoje ([from] a [to], inclusive). */
data class ChartWindow(val from: Float, val to: Float) {
    val span: Float get() = to - from
}

/**
 * RF-MOT-17 (1.6, pedido do Hanry, LA8-3): "Como vou fechar o ano?". O saldo das contas previsto até 31 de dezembro (e, de
 * outubro em diante, até o fim do ano que vem), com a faixa de 80%: as contas que não são mensais, as faturas, as parcelas,
 * as dívidas, os orçamentos planejados (RF-PRV-11) e o ritmo. Regras puras, para a tela e os testes.
 */
object YearOutlooks {
    /** A partir de outubro, a pessoa pode olhar também o ano que vem. */
    const val NEXT_YEAR_FROM_MONTH = 10

    /** O menor pedaço que o zoom mostra (dias) e a folga em volta dos sinais. */
    const val MIN_WINDOW = 21f
    private const val PADDING = 10f

    /** Até quando o motor projeta: 31/12 deste ano, ou do ano que vem a partir de outubro. */
    fun until(today: LocalDate): LocalDate =
        LocalDate.of(if (today.monthValue >= NEXT_YEAR_FROM_MONTH) today.year + 1 else today.year, 12, 31)

    fun hasNextYear(today: LocalDate): Boolean = today.monthValue >= NEXT_YEAR_FROM_MONTH

    /** Quantos dias da projeção cobrem até 31/12 do ano [year] (limitado ao que foi projetado). */
    fun daysUntilEndOf(p: Projection, year: Int): Int =
        ChronoUnit.DAYS.between(p.start, LocalDate.of(year, 12, 31)).toInt().coerceIn(1, p.horizonDays)

    /** As contas grandes que não são mensais (IPVA, IPTU, seguro...) que vencem entre amanhã e [end]. */
    fun bigBills(l: Ledger, end: LocalDate): List<Pair<Bill, LocalDate>> {
        val out = ArrayList<Pair<Bill, LocalDate>>()
        var m = l.today.ym()
        while (!m.isAfter(end.ym())) {
            for (s in l.billStatuses(m)) {
                val b = s.bill
                if (b.kind != CategoryKind.EXPENSE || b.every <= 1 || s.payment != null) continue
                val due = m.dayClamped(b.dueDay)
                if (due.isAfter(l.today) && !due.isAfter(end)) out += b to due
            }
            m = m.plusMonths(1)
        }
        return out.sortedBy { it.second }
    }

    /** Os sinais dentro dos primeiros [days] dias: o primeiro dia negativo, o dia mais apertado e as contas grandes. */
    fun signals(l: Ledger, p: Projection, days: Int): List<YearSignal> {
        val out = ArrayList<YearSignal>()
        val end = p.dateAt(days)
        p.firstNegativeWithin(days)?.let { d -> out += YearSignal(d, SignalKind.FIRST_NEGATIVE, p.mid[index(p, d)]) }
        val (tDate, tValue) = p.tightestWithin(days)
        if (tDate.isAfter(p.start) && out.none { it.date == tDate }) out += YearSignal(tDate, SignalKind.TIGHTEST, tValue)
        for ((b, due) in bigBills(l, end)) out += YearSignal(due, SignalKind.BIG_BILL, b.amount, b.name)
        return out.sortedBy { it.date }
    }

    fun index(p: Projection, d: LocalDate): Int = ChronoUnit.DAYS.between(p.start, d).toInt().coerceIn(0, p.horizonDays)

    /** O período inteiro (de hoje até o último dia mostrado). */
    fun full(days: Int): ChartWindow = ChartWindow(0f, days.toFloat())

    /**
     * Para onde o gráfico dá o zoom na primeira vista: a janela que contém todos os sinais, com uma folga, de no mínimo
     * [MIN_WINDOW] dias. Sem sinais, o período inteiro (sem zoom).
     */
    fun zoomWindow(p: Projection, signals: List<YearSignal>, days: Int): ChartWindow {
        if (signals.isEmpty()) return full(days)
        val xs = signals.map { index(p, it.date).toFloat() }
        var from = (xs.min() - PADDING).coerceAtLeast(0f)
        var to = (xs.max() + PADDING).coerceAtMost(days.toFloat())
        if (to - from < MIN_WINDOW) {
            val mid = (from + to) / 2
            from = (mid - MIN_WINDOW / 2).coerceAtLeast(0f)
            to = (from + MIN_WINDOW).coerceAtMost(days.toFloat())
            from = (to - MIN_WINDOW).coerceAtLeast(0f)
        }
        return ChartWindow(from, to)
    }

    /** Arrastar: a janela anda [deltaDays] (positivo = para o futuro), sem sair do período. */
    fun pan(w: ChartWindow, deltaDays: Float, days: Int): ChartWindow {
        val max = days.toFloat()
        val d = deltaDays.coerceIn(-w.from, max - w.to)
        return ChartWindow(w.from + d, w.to + d)
    }

    /**
     * Aproximar ou afastar com os dedos: [factor] > 1 aproxima (a janela encolhe), em volta de [focus] (um dia do período).
     * A janela fica entre [MIN_WINDOW] dias e o período inteiro, sem sair dele.
     */
    fun zoom(w: ChartWindow, factor: Float, focus: Float, days: Int): ChartWindow {
        val max = days.toFloat()
        val span = (w.span / factor.coerceAtLeast(0.01f)).coerceIn(minOf(MIN_WINDOW, max), max)
        val f = focus.coerceIn(w.from, w.to)
        val ratio = if (w.span > 0) (f - w.from) / w.span else 0.5f
        var from = f - span * ratio
        from = from.coerceIn(0f, max - span)
        return ChartWindow(from, from + span)
    }
}
