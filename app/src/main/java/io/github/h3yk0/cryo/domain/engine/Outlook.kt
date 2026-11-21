// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.domain.engine

import io.github.h3yk0.cryo.data.db.Tx
import io.github.h3yk0.cryo.data.db.TxType
import io.github.h3yk0.cryo.domain.Ledger
import io.github.h3yk0.cryo.domain.MonthForecast
import io.github.h3yk0.cryo.domain.forecast
import io.github.h3yk0.cryo.domain.ym
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.exp
import kotlin.math.roundToLong
import kotlin.math.sqrt

/**
 * RF-MOT-03: o gasto do dia a dia aprendido no histórico. Guarda cada dia (com o que foi gasto nele) para a simulação
 * sortear dias parecidos: mesmo dia da semana e mais peso no que é recente (meia-vida de 90 dias). As compras grandes
 * continuam no sorteio, na frequência em que acontecem; o "nível" do gasto é medido sem elas (corte no 97,5º percentil).
 */
class SpendingModel private constructor(
    private val pool: DoubleArray,
    private val pickers: Array<WeightedPicker>,
    private val all: WeightedPicker,
    /** Peso de cada dia da semana (segunda = 0): 1,2 = gasta 20% a mais que a média naquele dia. */
    val weekdayFactor: DoubleArray,
    /** Gasto típico por dia (sem os extremos). */
    val level: Double,
    /** Limite usado para cortar os extremos. */
    val cap: Double,
    /** Quanto o nível costuma variar de um mês para o outro (fora o sobe e desce dos dias). */
    val sigmaLevel: Double,
    val historyDays: Int,
    val fullMonths: Int,
) {
    val isEmpty: Boolean get() = all.total <= 0.0

    /** Sorteia o gasto de um dia com o mesmo dia da semana. */
    fun draw(rng: Rng, weekday: Int): Double {
        val p = pickers[weekday]
        val i = if (p.total > 0.0) p.pick(rng) else all.pick(rng)
        return if (i < 0) 0.0 else pool[i]
    }

    companion object {
        const val HALF_LIFE = 90.0
        const val MAX_DAYS = 365L

        fun weekday(d: LocalDate) = d.dayOfWeek.value - 1

        /** Gasto do dia a dia, sem as movimentações de [exclude] (as que são recorrências). */
        fun spending(exclude: Set<Long>): (Tx) -> Boolean = { Recurrences.isVariable(it) && it.id !in exclude }

        /** Entradas avulsas (trabalhos, vendas): não são recorrência nem conta fixa a receber. */
        fun extraIncome(exclude: Set<Long>): (Tx) -> Boolean = { it.type == TxType.INCOME && it.billId == null && it.id !in exclude }

        /** Monta o modelo com o que [include] escolhe, de [start] a [end] (no máximo um ano). */
        fun build(txs: List<Tx>, include: (Tx) -> Boolean, end: LocalDate, ref: LocalDate, minStart: LocalDate? = null): SpendingModel {
            var first: LocalDate? = null
            for (t in txs) if (!t.date.isAfter(end) && include(t)) {
                if (first == null || t.date.isBefore(first)) first = t.date
            }
            var start = maxOf(first ?: end.plusDays(1), end.minusDays(MAX_DAYS - 1))
            if (minStart != null && start.isBefore(minStart)) start = minStart
            if (start.isAfter(end)) return empty()
            val n = (ChronoUnit.DAYS.between(start, end) + 1).toInt()
            val daily = DoubleArray(n)
            for (t in txs) {
                if (t.date.isBefore(start) || t.date.isAfter(end) || !include(t)) continue
                daily[ChronoUnit.DAYS.between(start, t.date).toInt()] += t.amount.toDouble()
            }
            val weights = DoubleArray(n) { Stats.decay(ChronoUnit.DAYS.between(start.plusDays(it.toLong()), ref), HALF_LIFE) }
            val cap = if (n >= 20) Stats.quantile(daily, 0.975) else daily.max()
            val wins = DoubleArray(n) { minOf(daily[it], cap) }
            val level = Stats.weightedMean(wins, weights)

            val wd = IntArray(n) { weekday(start.plusDays(it.toLong())) }
            val factor = DoubleArray(7) { 1.0 }
            if (level > 0) {
                for (k in 0 until 7) {
                    var sw = 0.0; var s = 0.0; var c = 0
                    for (i in 0 until n) if (wd[i] == k) { sw += weights[i]; s += weights[i] * wins[i]; c++ }
                    val ratio = if (sw > 0) (s / sw) / level else 1.0
                    // Poucas semanas: o fator fica perto de 1 (encolhimento, como quem ainda não tem certeza).
                    factor[k] = (c * ratio + 8.0) / (c + 8.0)
                }
                val avg = factor.average()
                if (avg > 0) for (k in 0 until 7) factor[k] /= avg
            }
            val pickers = Array(7) { k -> WeightedPicker(DoubleArray(n) { if (wd[it] == k) weights[it] else 0.0 }) }

            // Variação de um mês para o outro: a dos meses completos, tirando o que o sobe e desce dos dias já explica.
            val months = LinkedHashMap<Int, DoubleArray>() // mês -> (soma, dias)
            for (i in 0 until n) {
                val day = start.plusDays(i.toLong())
                val k = day.year * 100 + day.monthValue
                val acc = months.getOrPut(k) { DoubleArray(2) }
                acc[0] += wins[i]; acc[1] += 1.0
            }
            val full = months.filter { (k, v) -> v[1] >= java.time.YearMonth.of(k / 100, k % 100).lengthOfMonth() }.values
                .map { it[0] / it[1] * 30.44 }
            val sigma = if (full.size >= 3) {
                val m = full.average()
                val vm = full.sumOf { (it - m) * (it - m) } / (full.size - 1)
                val md = wins.average()
                val vd = wins.sumOf { (it - md) * (it - md) } / maxOf(1, n - 1)
                if (m > 0) (sqrt(maxOf(0.0, vm - 30.44 * vd)) / m).coerceIn(0.03, 0.6) else 0.2
            } else if (n >= 28) 0.2 else 0.35
            return SpendingModel(daily, pickers, WeightedPicker(weights), factor, level, cap, sigma, n, full.size)
        }

        private fun empty() = SpendingModel(
            DoubleArray(0), Array(7) { WeightedPicker(DoubleArray(0)) }, WeightedPicker(DoubleArray(0)), DoubleArray(7) { 1.0 },
            0.0, Double.MAX_VALUE, 0.35, 0, 0,
        )
    }
}

enum class Certainty { LOW, MEDIUM, HIGH }

/** Um ponto da faixa: o resultado do mês acumulado até o dia [day] (P10, P50 e P90 dos cenários). */
data class BandPoint(val day: Int, val low: Long, val mid: Long, val high: Long)

/** Uma recorrência que deve acontecer antes do fim do mês e ainda não foi lançada. */
data class ExpectedItem(val recurrence: Recurrence, val date: LocalDate, val probability: Double)

/**
 * RF-MOT-03: a previsão do mês com faixa. [low], [mid] e [high] são o resultado do mês (entradas − saídas) em 10%, 50% e
 * 90% dos cenários: "entre [low] e [high], 8 em cada 10 vezes".
 */
data class MonthOutlook(
    val base: MonthForecast,
    val low: Long,
    val mid: Long,
    val high: Long,
    val chanceNegative: Double,
    /** Gasto do dia a dia previsto para o resto do mês (P50). */
    val variableMid: Long,
    /** Gasto típico por dia agora (nível do histórico misturado com o deste mês). */
    val dailyRate: Long,
    val expected: List<ExpectedItem>,
    val band: List<BandPoint>,
    val certainty: Certainty,
    val historyDays: Int,
    val scenarios: Int,
    /** RF-PRV-05: a conferência no passado da própria pessoa (null enquanto não há meses bastantes). */
    val calibration: io.github.h3yk0.cryo.domain.predict.Calibration? = null,
    /** RF-PRV-08: quanto cada modelo pesou na previsão do gasto do dia a dia. */
    val models: List<io.github.h3yk0.cryo.domain.predict.ModelShare> = emptyList(),
    /** RF-PRV-03: a época do ano aplicada ao gasto do dia a dia (1 = época comum; 1,4 = estes dias costumam custar 40% a mais). */
    val season: Double = 1.0,
    /** RF-PRV-10 (1.6): as entradas avulsas previstas até o fim do mês (P50), já contadas em [mid]; 0 quando desligadas. */
    val extraIncomeMid: Long = 0,
    /** RF-PRV-10: as entradas avulsas entram na conta (escolha da pessoa, ou o Cryo sozinho: [extraIncomeAuto]). */
    val extraIncomeOn: Boolean = false,
    /** O que o Cryo escolheria sozinho: houve entradas avulsas em pelo menos 2 dos 3 últimos meses ([extraIncomeMonths]). */
    val extraIncomeAuto: Boolean = false,
    val extraIncomeMonths: Int = 0,
    /** RF-PRV-05: o acerto conferido no passado (o erro que se repete para o mesmo lado), já somado em [mid]. */
    val pastShift: Long = 0,
) {
    val expectedIncome: Long get() = expected.filter { it.recurrence.type == TxType.INCOME }.sumOf { (it.recurrence.amount * it.probability).roundToLong() }
    val expectedExpense: Long get() = expected.filter { it.recurrence.type == TxType.EXPENSE }.sumOf { (it.recurrence.amount * it.probability).roundToLong() }

    /**
     * Quanto dá para gastar por dia (incluindo hoje) e terminar o mês no zero a zero, contando também o que se repete e
     * ainda deve entrar ou sair (RF-INI-05 com o motor).
     */
    val allowance: Long
        get() {
            val free = base.projectedIncome + expectedIncome -
                (base.expenseSoFar + base.scheduledExpense + base.pendingBillsExpense + base.pendingDebts + expectedExpense)
            return free / (base.remainingDays + 1)
        }
}

object Outlook {
    const val SCENARIOS = 2000

    /** Quanto o mês atual pesa contra o histórico: no dia [d], d / (d + 10). */
    private const val CURRENT_WEIGHT_DAYS = 10.0

    fun month(l: Ledger, recurrences: List<Recurrence>, seed: Long, scenarios: Int = SCENARIOS): MonthOutlook {
        val base = l.forecast()
        val today = l.today
        val ym = today.ym()
        val d = today.dayOfMonth
        val days = ym.lengthOfMonth()
        val monthEnd = ym.atEndOfMonth()

        val exclude = HashSet<Long>()
        val excludeIncome = HashSet<Long>()
        for (r in recurrences) if (r.type == TxType.EXPENSE) exclude += r.txIds else excludeIncome += r.txIds
        val history = SpendingModel.build(l.s.txs, SpendingModel.spending(exclude), end = ym.atDay(1).minusDays(1), ref = today)
        // Entradas avulsas (freelas, vendas): aprendidas do histórico até ontem, sorteadas nos dias que faltam.
        val incomeModel = SpendingModel.build(l.s.txs, SpendingModel.extraIncome(excludeIncome), end = today, ref = today)
        val hasHistory = history.historyDays >= 28 && !history.isEmpty

        // O que já foi gasto no dia a dia neste mês (sem recorrências), com os extremos cortados pelo limite do histórico.
        val cur = DoubleArray(d + 1)
        for (t in l.monthTxs(ym)) if (Recurrences.isVariable(t) && t.id !in exclude && !t.date.isAfter(today)) cur[t.date.dayOfMonth] += t.amount.toDouble()
        val cap = if (hasHistory) maxOf(history.cap, 1.0) else Double.MAX_VALUE
        var sCur = 0.0; var eCur = 0.0
        for (day in 1..d) {
            sCur += minOf(cur[day], cap)
            eCur += history.weekdayFactor[SpendingModel.weekday(ym.atDay(day))]
        }
        val levelCur = if (eCur > 0) sCur / eCur else 0.0
        val lambda = if (hasHistory) d / (d + CURRENT_WEIGHT_DAYS) else 1.0
        val level = lambda * levelCur + (1 - lambda) * history.level

        // Sem histórico, o próprio mês é o histórico.
        val model = if (hasHistory) history else SpendingModel.build(l.s.txs, SpendingModel.spending(exclude), end = today, ref = today, minStart = ym.atDay(1))
        val scale = when {
            model.isEmpty || model.level <= 0.0 -> 0.0
            hasHistory -> level / model.level
            else -> 1.0
        }
        val sigma = model.sigmaLevel * (1 - lambda).coerceAtLeast(if (hasHistory) 0.0 else 0.5)

        val expected = expectedItems(l, recurrences, today, monthEnd)

        val remaining = days - d
        val baseResult = base.currentResult
        val planned = LongArray(days + 1).also { arr -> base.plannedNet.forEachIndexed { i, v -> if (i <= days) arr[i] = v } }
        val rng = Rng(seed)
        val n = scenarios.coerceAtLeast(1)
        val results = DoubleArray(n)
        val variables = DoubleArray(n)
        val paths = Array(remaining) { DoubleArray(n) }
        val recNet = DoubleArray(days + 2)
        for (sc in 0 until n) {
            val lvl = if (sigma > 0) exp(sigma * rng.nextGaussian() - sigma * sigma / 2) else 1.0
            java.util.Arrays.fill(recNet, 0.0)
            for (e in expected) {
                if (rng.nextDouble() >= e.probability) continue
                val r = e.recurrence
                val amt = if (r.variable) r.amount * (1.0 + 0.08 * rng.nextGaussian()).coerceIn(0.5, 1.5) else r.amount.toDouble()
                recNet[e.date.dayOfMonth] += if (r.type == TxType.INCOME) amt else -amt
            }
            var cum = baseResult.toDouble()
            var variable = 0.0
            val ilvl = if (incomeModel.isEmpty) 0.0 else exp(incomeModel.sigmaLevel * rng.nextGaussian() - incomeModel.sigmaLevel * incomeModel.sigmaLevel / 2)
            for (k in 0 until remaining) {
                val day = d + 1 + k
                val wd = SpendingModel.weekday(ym.atDay(day))
                val v = if (scale > 0) model.draw(rng, wd) * scale * lvl else 0.0
                val extra = if (ilvl > 0) incomeModel.draw(rng, wd) * ilvl else 0.0
                variable += v
                cum += planned[day] + recNet[day] - v + extra
                paths[k][sc] = cum
            }
            variables[sc] = variable
            results[sc] = if (remaining > 0) cum else baseResult.toDouble() + planned.sum()
        }
        results.sort()
        variables.sort()
        val band = ArrayList<BandPoint>(remaining + 1)
        val actualToday = base.series.firstOrNull { it.day == d }?.actual ?: baseResult
        band += BandPoint(d, actualToday, actualToday, actualToday)
        for (k in 0 until remaining) {
            val col = paths[k].also { it.sort() }
            band += BandPoint(
                d + 1 + k, Stats.quantileSorted(col, 0.1).roundToLong(), Stats.quantileSorted(col, 0.5).roundToLong(),
                Stats.quantileSorted(col, 0.9).roundToLong(),
            )
        }
        val negatives = results.count { it < 0.0 }
        return MonthOutlook(
            base = base,
            low = Stats.quantileSorted(results, 0.1).roundToLong(),
            mid = Stats.quantileSorted(results, 0.5).roundToLong(),
            high = Stats.quantileSorted(results, 0.9).roundToLong(),
            chanceNegative = negatives.toDouble() / n,
            variableMid = Stats.quantileSorted(variables, 0.5).roundToLong(),
            dailyRate = level.roundToLong(),
            expected = expected,
            band = band,
            certainty = when {
                history.historyDays >= 90 && history.fullMonths >= 3 -> Certainty.HIGH
                hasHistory -> Certainty.MEDIUM
                else -> Certainty.LOW
            },
            historyDays = history.historyDays,
            scenarios = n,
        )
    }

    /**
     * As recorrências que ainda devem acontecer até [until] e não estão no app de outro jeito: sem conta fixa (a conta fixa
     * já entra na previsão) e sem a ocorrência que um lançamento agendado com a mesma descrição já cobre.
     *
     * ERR-060 (auditoria da 1.6): cada agendado substitui **só a ocorrência mais perto dele** (até um período de distância),
     * nunca a recorrência inteira. Antes, o salário de dezembro agendado apagava todos os outros meses de salário da
     * projeção de 12 meses e de "Como vou fechar o ano?". Na previsão do mês nada muda: lá só cabe uma ocorrência, e o
     * agendado (até 7 dias depois do fim do período, como antes) continua tomando o lugar dela.
     */
    fun expectedItems(l: Ledger, recurrences: List<Recurrence>, today: LocalDate, until: LocalDate): List<ExpectedItem> {
        val billKeys = l.s.bills.filter { it.active }.mapTo(HashSet()) { TxKey.of(it.name) }
        val scheduled = HashMap<String, MutableList<LocalDate>>()
        for (t in l.s.txs) if (t.date.isAfter(today) && !t.date.isAfter(until.plusDays(7))) {
            scheduled.getOrPut(t.type.ordinal.toString() + "|" + TxKey.of(t.description)) { ArrayList() } += t.date
        }
        val out = ArrayList<ExpectedItem>()
        for (r in recurrences) {
            if (r.billId != null || r.key in billKeys || r.status == RecurrenceStatus.ENDED) continue
            val p = (0.6 + 0.4 * r.confidence).coerceAtMost(0.98)
            val items = ArrayList<ExpectedItem>()
            if (r.status == RecurrenceStatus.LATE) {
                // Atrasou (o salário que não veio): metade da chance de vir ainda neste período, amanhã.
                val tomorrow = today.plusDays(1)
                if (!tomorrow.isAfter(until)) items += ExpectedItem(r, tomorrow, 0.5)
                for (date in r.occurrences(tomorrow, until)) items += ExpectedItem(r, date, p)
            } else {
                for (date in r.occurrences(today, until)) items += ExpectedItem(r, date, p)
            }
            // Cada agendado tira uma ocorrência só: a mais perto dele, se estiver a até um período (o que está mais longe
            // que isso não é esta ocorrência, é outra coisa com o mesmo nome).
            val reach = kotlin.math.ceil(r.period.days).toLong()
            scheduled[r.type.ordinal.toString() + "|" + r.key]?.sorted()?.forEach { date ->
                val nearest = items.minByOrNull { kotlin.math.abs(ChronoUnit.DAYS.between(it.date, date)) } ?: return@forEach
                if (kotlin.math.abs(ChronoUnit.DAYS.between(nearest.date, date)) <= reach) items.remove(nearest)
            }
            out += items
        }
        return out.sortedBy { it.date }
    }
}
