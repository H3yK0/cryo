// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.domain.engine

import io.github.h3yk0.cryo.data.db.Tx
import io.github.h3yk0.cryo.data.db.TxType
import io.github.h3yk0.cryo.domain.dayClamped
import io.github.h3yk0.cryo.domain.ym
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/** De quanto em quanto tempo algo se repete, com a folga aceita (dias) e o mínimo de vezes para acreditar. */
enum class RecurrencePeriod(val days: Double, val tolerance: Int, val minCount: Int) {
    WEEKLY(7.0, 2, 4),
    BIWEEKLY(14.0, 3, 3),
    MONTHLY(30.44, 5, 3),
    YEARLY(365.25, 15, 2),
}

/** ACTIVE: em dia · LATE: passou da data esperada e ainda não veio · ENDED: parou de vir. */
enum class RecurrenceStatus { ACTIVE, LATE, ENDED }

/**
 * Algo que se repete (salário, aluguel, assinatura), descoberto sozinho a partir das movimentações (RF-MOT-02).
 * [key] é a chave da descrição: fica só na memória (o que vai para o disco é o hash dela, RF-SEG-02).
 */
data class Recurrence(
    val key: String,
    val type: TxType,
    val label: String,
    val categoryId: Long?,
    val accountId: Long?,
    val cardId: Long?,
    val period: RecurrencePeriod,
    /** Valor esperado da próxima vez (o preço novo, se mudou). */
    val amount: Long,
    val lastAmount: Long,
    /** O valor muda de uma vez para outra (conta de luz), mas a data não. */
    val variable: Boolean,
    val count: Int,
    val first: LocalDate,
    val last: LocalDate,
    val next: LocalDate,
    val status: RecurrenceStatus,
    /** Diferença do último valor para o de antes (positivo = ficou mais cara). 0 = não mudou. */
    val priceChange: Long,
    /** Conta fixa ligada (os pagamentos dela já são conhecidos pelo app). */
    val billId: Long?,
    /** De 0 a 1: quanto o motor acredita nesta recorrência. */
    val confidence: Double,
    /** Dia típico do mês (só nas mensais). */
    val anchorDay: Int,
    val txIds: Set<Long>,
) {
    /** Quanto isto pesa por mês (uma assinatura anual vira 1/12 por mês). */
    val monthlyAmount: Long get() = (amount * (30.44 / period.days)).roundToLong()

    /** As próximas datas esperadas depois de [after] até [until] (inclusive). */
    fun occurrences(after: LocalDate, until: LocalDate, max: Int = 400): List<LocalDate> {
        val out = ArrayList<LocalDate>()
        var d = next
        var guard = 0
        while (!d.isAfter(until) && guard++ < max) {
            if (d.isAfter(after)) out += d
            d = Recurrences.step(d, period, anchorDay)
        }
        return out
    }
}

object Recurrences {
    /** Quanto tempo para trás olhar: dois anos e pouco, para achar também o que é anual. */
    const val LOOKBACK_DAYS = 800L

    /** Gasto do dia a dia: entra no ritmo do mês (não é conta fixa, parcela nem pagamento de dívida). */
    fun isVariable(t: Tx) = t.type == TxType.EXPENSE && t.billId == null && t.debtId == null && t.installmentTotal <= 1

    fun detect(txs: List<Tx>, today: LocalDate): List<Recurrence> {
        val from = today.minusDays(LOOKBACK_DAYS)
        val groups = HashMap<String, MutableList<Tx>>()
        for (t in txs) {
            if (t.type != TxType.EXPENSE && t.type != TxType.INCOME) continue
            if (t.date.isAfter(today) || t.date.isBefore(from)) continue
            // Parcelas e dívidas já são conhecidas pelo app (cada uma tem a sua data e o seu valor).
            if (t.installmentTotal > 1 || t.debtId != null || t.amount <= 0) continue
            val k = TxKey.of(t.description)
            if (k.isEmpty()) continue
            groups.getOrPut(t.type.ordinal.toString() + "|" + k) { ArrayList() } += t
        }
        val out = ArrayList<Recurrence>()
        for ((gk, list) in groups) analyze(gk.substringAfter('|'), list, today)?.let { out += it }
        return out.sortedWith(compareByDescending<Recurrence> { it.monthlyAmount }.thenBy { it.key })
    }

    private fun analyze(key: String, raw: List<Tx>, today: LocalDate): Recurrence? {
        if (raw.size < 2) return null
        // Duas no mesmo dia contam como uma vez (a outra pode ser uma cobrança repetida: RF-MOT-05 cuida disso).
        val occ = raw.sortedWith(compareBy<Tx>({ it.date }, { it.id })).distinctBy { it.date }
        if (occ.size < 2) return null
        val intervals = DoubleArray(occ.size - 1) { ChronoUnit.DAYS.between(occ[it].date, occ[it + 1].date).toDouble() }
        val m = Stats.median(intervals)
        val period = RecurrencePeriod.entries.firstOrNull { abs(m - it.days) <= it.tolerance } ?: return null
        if (occ.size < period.minCount) return null

        // Intervalos que batem com o período (um mês pulado também vale: 2 períodos com o dobro da folga).
        var ok = 0
        for (iv in intervals) {
            if (abs(iv - period.days) <= period.tolerance || abs(iv - 2 * period.days) <= 2 * period.tolerance) ok++
        }
        val regularity = ok.toDouble() / intervals.size
        if (regularity < 0.75) return null

        val amounts = DoubleArray(occ.size) { occ[it].amount.toDouble() }
        val med = Stats.median(amounts)
        if (med <= 0.0) return null
        val madRatio = Stats.mad(amounts, med) / med
        val variable = madRatio > 0.08
        if (madRatio > 0.30) return null

        val last = occ.last()
        // Preço novo: as cobranças de antes eram iguais entre si e as últimas (no máximo 2: depois disso não é mais
        // novidade) têm outro valor, pelo menos 3% e 1 unidade da moeda diferente.
        var priceChange = 0L
        var run = 1
        while (run < occ.size && abs(occ[occ.size - 1 - run].amount - last.amount) <= last.amount * 0.01) run++
        if (run <= 2 && occ.size - run >= 2) {
            val prev = DoubleArray(occ.size - run) { occ[it].amount.toDouble() }
            val prevMed = Stats.median(prev)
            val prevStable = prevMed > 0 && Stats.mad(prev, prevMed) / prevMed <= 0.02
            val diff = last.amount - prevMed
            if (prevStable && abs(diff) >= maxOf(0.03 * prevMed, 100.0)) priceChange = diff.roundToLong()
        }
        // Valor fixo: a próxima cobrança costuma ser igual à última (preço novo continua). Valor que varia: a mediana.
        val expected = if (!variable) last.amount else med.roundToLong()

        val anchor = if (period == RecurrencePeriod.MONTHLY) anchorDay(occ.map { it.date }) else last.date.dayOfMonth
        val next = step(last.date, period, anchor)
        val late = ChronoUnit.DAYS.between(next, today)
        val status = when {
            late <= period.tolerance -> RecurrenceStatus.ACTIVE
            late <= period.days -> RecurrenceStatus.LATE
            else -> RecurrenceStatus.ENDED
        }
        // Parou há muito tempo: não interessa mais.
        if (late > 3 * period.days) return null

        val billIds = occ.mapNotNull { it.billId }
        val billId = billIds.groupingBy { it }.eachCount().maxByOrNull { it.value }?.takeIf { it.value * 2 >= occ.size }?.key
        val confidence = (minOf(1.0, occ.size / 6.0) * regularity * (1.0 - minOf(madRatio, 0.3))).coerceIn(0.0, 1.0)

        return Recurrence(
            key = key, type = last.type, label = last.description.trim().take(60), categoryId = last.categoryId,
            accountId = last.accountId, cardId = last.cardId, period = period, amount = expected, lastAmount = last.amount,
            variable = variable, count = occ.size, first = occ.first().date, last = last.date, next = next, status = status,
            priceChange = priceChange, billId = billId, confidence = confidence, anchorDay = anchor,
            txIds = raw.mapTo(HashSet()) { it.id },
        )
    }

    /** Dia típico do mês: a mediana dos dias; quem cai sempre no fim do mês fica no último dia. */
    private fun anchorDay(dates: List<LocalDate>): Int {
        val endOfMonth = dates.count { it.dayOfMonth >= it.lengthOfMonth() - 1 } * 2 > dates.size && dates.all { it.dayOfMonth >= 27 }
        if (endOfMonth) return 31
        return Stats.median(DoubleArray(dates.size) { dates[it].dayOfMonth.toDouble() }).roundToInt().coerceIn(1, 31)
    }

    /** A data seguinte de uma recorrência. Nas mensais, volta para o dia típico (o salário que caiu antes por causa do fim de semana). */
    fun step(from: LocalDate, period: RecurrencePeriod, anchorDay: Int): LocalDate = when (period) {
        RecurrencePeriod.MONTHLY -> {
            var candidate = from.plusDays(30).ym().dayClamped(anchorDay)
            if (ChronoUnit.DAYS.between(from, candidate) < 20) candidate = candidate.ym().plusMonths(1).dayClamped(anchorDay)
            if (ChronoUnit.DAYS.between(from, candidate) > 45) candidate = candidate.ym().minusMonths(1).dayClamped(anchorDay)
            candidate
        }
        RecurrencePeriod.YEARLY -> from.plusYears(1)
        else -> from.plusDays(period.days.roundToLong())
    }
}
