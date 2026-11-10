// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.domain.predict

import io.github.h3yk0.cryo.data.db.CategoryKind
import io.github.h3yk0.cryo.data.db.TxType
import io.github.h3yk0.cryo.domain.Ledger
import io.github.h3yk0.cryo.domain.engine.ExpectedItem
import io.github.h3yk0.cryo.domain.engine.Outlook
import io.github.h3yk0.cryo.domain.engine.Recurrence
import io.github.h3yk0.cryo.domain.engine.Recurrences
import io.github.h3yk0.cryo.domain.engine.Rng
import io.github.h3yk0.cryo.domain.engine.Stats
import io.github.h3yk0.cryo.domain.ym
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.roundToLong

/**
 * RF-PRV-06: como uma categoria deve fechar o mês. [spent] é o que já saiu até hoje; [low], [mid] e [high] são o total do
 * mês em 10%, 50% e 90% dos cenários; [chanceOver] é a chance de passar do [limit] (0 quando não há limite).
 */
data class CategoryOutlook(
    val categoryId: Long,
    val limit: Long,
    val spent: Long,
    val low: Long,
    val mid: Long,
    val high: Long,
    val chanceOver: Double,
)

/**
 * RF-PRV-06: a previsão do mês, categoria por categoria, com o mesmo conjunto de modelos da previsão geral (RF-PRV-03).
 * O total do mês = o que já saiu + o que já tem dia certo (lançado, conta fixa, o que se repete) + o que o conjunto prevê
 * para o gasto do dia a dia até o fim do mês.
 */
object CategoryForecasts {
    const val DRAWS = 400

    /** Só entra categoria com movimento nos últimos 3 meses (as outras não têm o que prever). */
    private const val ACTIVE_DAYS = 92L

    fun of(l: Ledger, recurrences: List<Recurrence>, seed: Long, draws: Int = DRAWS): List<CategoryOutlook> {
        val today = l.today
        val ym = today.ym()
        val remaining = ym.lengthOfMonth() - today.dayOfMonth
        val monthStart = ym.atDay(1)
        val monthEnd = ym.atEndOfMonth()
        val from = today.minusDays(DailySeries.MAX_DAYS - 1)
        val expense = l.categories(CategoryKind.EXPENSE).mapTo(HashSet()) { it.id }
        // O que tem dia e valor certos (assinaturas, contas fixas, parcelas) não é "ritmo": entra à parte, como na previsão geral.
        val fixed = HashSet<Long>()
        for (r in recurrences) if (r.type == TxType.EXPENSE) fixed += r.txIds

        // Uma passada só pelas movimentações: a série do gasto do dia a dia, o gasto do mês e o já lançado para a frente.
        val first = HashMap<Long, LocalDate>()
        val spent = HashMap<Long, Long>()
        val ahead = HashMap<Long, Long>()
        val lastSeen = HashMap<Long, LocalDate>()
        for (t in l.s.txs) {
            val c = t.categoryId ?: continue
            if (t.type != TxType.EXPENSE || c !in expense) continue
            if (t.date.isAfter(today)) {
                if (!t.date.isAfter(monthEnd)) ahead[c] = (ahead[c] ?: 0L) + t.amount
                continue
            }
            if (!t.date.isBefore(monthStart)) spent[c] = (spent[c] ?: 0L) + t.amount
            val seen = lastSeen[c]
            if (seen == null || t.date.isAfter(seen)) lastSeen[c] = t.date
            if (t.date.isBefore(from) || !Recurrences.isVariable(t) || t.id in fixed) continue
            val f = first[c]
            if (f == null || t.date.isBefore(f)) first[c] = t.date
        }
        val raw = HashMap<Long, DoubleArray>()
        for (t in l.s.txs) {
            val c = t.categoryId ?: continue
            val start = first[c] ?: continue
            if (t.type != TxType.EXPENSE || t.date.isAfter(today) || t.date.isBefore(start) || !Recurrences.isVariable(t) || t.id in fixed) continue
            val arr = raw.getOrPut(c) { DoubleArray(ChronoUnit.DAYS.between(start, today).toInt() + 1) }
            arr[ChronoUnit.DAYS.between(start, t.date).toInt()] += t.amount.toDouble()
        }
        // Contas fixas deste mês ainda sem pagamento, e o que se repete e ainda deve vir até o fim do mês.
        for (b in l.billStatuses(ym)) {
            val c = b.bill.categoryId ?: continue
            if (b.payment == null && b.bill.kind == CategoryKind.EXPENSE && c in expense) ahead[c] = (ahead[c] ?: 0L) + b.bill.amount
        }
        val expected = HashMap<Long, MutableList<ExpectedItem>>()
        if (remaining > 0) for (e in Outlook.expectedItems(l, recurrences, today, monthEnd)) {
            val c = e.recurrence.categoryId ?: continue
            if (e.recurrence.type == TxType.EXPENSE && c in expense) expected.getOrPut(c) { ArrayList() } += e
        }

        val out = ArrayList<CategoryOutlook>()
        for (c in expense.sorted()) {
            val limit = l.monthLimit(c, ym)
            val done = spent[c] ?: 0L
            val active = lastSeen[c]?.let { ChronoUnit.DAYS.between(it, today) <= ACTIVE_DAYS } == true
            val items = expected[c].orEmpty()
            if (!active && limit <= 0 && (ahead[c] ?: 0L) == 0L && items.isEmpty()) continue
            val known = (done + (ahead[c] ?: 0L)).toDouble()
            val arr = raw[c]
            val fc = if (arr != null && remaining > 0) DailySeries(first.getValue(c), arr).let { Ensemble.forecast(it, it.n - 1, remaining) } else null
            val rng = Rng(Rng.mix(seed, c))
            val n = draws.coerceAtLeast(1)
            val totals = DoubleArray(n) {
                var v = known
                if (fc != null) v += (fc.point * (1.0 + fc.drawError(rng))).coerceAtLeast(0.0)
                for (e in items) if (rng.nextDouble() < e.probability) {
                    v += if (e.recurrence.variable) e.recurrence.amount * (1.0 + 0.08 * rng.nextGaussian()).coerceIn(0.5, 1.5) else e.recurrence.amount.toDouble()
                }
                v
            }
            var over = 0
            if (limit > 0) for (v in totals) if (v > limit) over++
            totals.sort()
            // O centro é o número do conjunto (como na previsão geral) mais o que se espera das recorrências.
            val mid = known + (fc?.point ?: 0.0) + items.sumOf { it.recurrence.amount * it.probability }
            out += CategoryOutlook(
                categoryId = c, limit = limit, spent = done,
                low = minOf(Stats.quantileSorted(totals, 0.1), mid).roundToLong(),
                mid = mid.roundToLong(),
                high = maxOf(Stats.quantileSorted(totals, 0.9), mid).roundToLong(),
                chanceOver = over.toDouble() / n,
            )
        }
        return out
    }
}
