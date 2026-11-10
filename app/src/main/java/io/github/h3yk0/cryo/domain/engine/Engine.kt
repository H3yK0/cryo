// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.domain.engine

import io.github.h3yk0.cryo.R
import io.github.h3yk0.cryo.data.db.TxType
import io.github.h3yk0.cryo.domain.Dates
import io.github.h3yk0.cryo.domain.Insight
import io.github.h3yk0.cryo.domain.Ledger
import io.github.h3yk0.cryo.domain.Money
import io.github.h3yk0.cryo.domain.Num
import io.github.h3yk0.cryo.domain.Res
import io.github.h3yk0.cryo.domain.Snapshot
import io.github.h3yk0.cryo.domain.insights
import io.github.h3yk0.cryo.domain.ym
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.roundToInt

/** O que a pessoa escolheu nas telas do motor (não muda os dados, só a conta). */
data class EngineOptions(
    val debtExtra: Long = 0,
    val strategy: PayoffStrategy = PayoffStrategy.AVALANCHE,
    /** Rendimento ao mês usado nas metas (0 = sem rendimento). */
    val goalYield: Double = 0.0,
    /** Chaves (hash, RF-SEG-02) das recorrências que a pessoa disse que não são recorrentes. */
    val dismissed: Set<String> = emptySet(),
)

/** Uma percepção com nota de importância (RF-MOT-11). [topic] evita duas frases sobre a mesma coisa. */
data class RankedInsight(val text: String, val positive: Boolean?, val score: Double, val topic: String) {
    fun toInsight() = Insight(text, positive)
}

/** Tudo o que o motor calculou para um dia e uma fotografia dos dados. */
data class EngineReport(
    val today: LocalDate,
    val recurrences: List<Recurrence>,
    val outlook: MonthOutlook,
    val projection: Projection,
    val anomalies: List<Anomaly>,
    val health: Health,
    val pace: List<PaceAlert>,
    val budgetSuggestions: List<BudgetSuggestion>,
    val debtRates: List<DebtRate>,
    val payoff: PayoffPlan?,
    val returns: Map<Long, InvestReturn>,
    val portfolio: InvestReturn?,
    val goals: Map<Long, GoalOutlook>,
    val insights: List<RankedInsight>,
) {
    /** Assinaturas e outras saídas que se repetem, ativas, por mês. */
    val monthlyRecurringExpense: Long
        get() = recurrences.filter { it.type == TxType.EXPENSE && it.status != RecurrenceStatus.ENDED }.sumOf { it.monthlyAmount }
}

/**
 * RF-MOT: o motor financeiro. Puro Kotlin, sem Android e sem rede (RF-SEG): recebe a fotografia dos dados e devolve tudo
 * calculado. Os sorteios usam uma semente tirada dos próprios dados: os mesmos dados dão sempre o mesmo resultado.
 */
object FinanceEngine {
    fun seedOf(s: Snapshot, today: LocalDate): Long {
        var h = Rng.mix(0x43727930L, today.toEpochDay())
        for (t in s.txs) {
            h = Rng.mix(h, t.id)
            h = Rng.mix(h, t.amount)
            h = Rng.mix(h, t.date.toEpochDay())
        }
        h = Rng.mix(h, s.accounts.sumOf { it.initialBalance })
        return Rng.mix(h, (s.bills.size * 31 + s.debts.size * 7 + s.cards.size).toLong())
    }

    /**
     * [hasher] transforma a chave de uma recorrência no código guardado quando a pessoa a esconde (no app, HMAC com a chave do
     * cofre: RF-SEG-02); [EngineOptions.dismissed] traz esses códigos.
     */
    fun run(s: Snapshot, today: LocalDate, options: EngineOptions = EngineOptions(), hasher: (String) -> String = { it }): EngineReport {
        // O motor usa o seu próprio livro-caixa: o das telas guarda cálculos em mapas que não são feitos para duas threads.
        val l = Ledger(s, today)
        val seed = seedOf(s, today)
        val all = Recurrences.detect(s.txs, today)
        val recurrences = if (options.dismissed.isEmpty()) all else all.filter { hasher(it.key) !in options.dismissed }
        // RF-PRV: a previsão do mês vem do previsor novo (1.8); a da 1.5 ([Outlook.month]) fica como régua nos testes.
        val outlook = io.github.h3yk0.cryo.domain.predict.Forecaster.month(l, recurrences, seed)
        val projection = Projections.project(l, recurrences, Rng.mix(seed, 1))
        val anomalies = Anomalies.find(l, recurrences)
        val health = HealthScore.of(l)
        val pace = Pacing.alerts(l)
        val suggestions = Pacing.suggestions(l)
        val rates = DebtPlanner.rates(l)
        val payoff = DebtPlanner.plan(l, options.debtExtra, options.strategy)
        val returns = l.s.investments.filter { !it.archived }.mapNotNull { inv -> Returns.of(l, inv.id)?.let { inv.id to it } }.toMap()
        val portfolio = Returns.of(l, null)
        val goals = l.activeGoals.associate { it.id to GoalPlanner.of(l, it, Rng.mix(seed, 2), options.goalYield) }
        val report = EngineReport(
            today, recurrences, outlook, projection, anomalies, health, pace, suggestions, rates, payoff, returns, portfolio, goals,
            insights = emptyList(),
        )
        return report.copy(insights = Ranking.rank(l, report))
    }
}

/**
 * RF-MOT-13: guarda o último resultado. Com os mesmos dados (o mesmo objeto), o mesmo dia e as mesmas escolhas, devolve o
 * que já calculou; senão calcula de novo (fora da linha principal: quem chama decide a thread). Uma conta por vez.
 */
class EngineRunner(private val hasher: (String) -> String = { it }) {
    private val mutex = Mutex()
    @Volatile private var lastKey: Triple<Snapshot, LocalDate, EngineOptions>? = null
    @Volatile private var lastReport: EngineReport? = null

    /** Quantas vezes o motor calculou de verdade (os testes conferem o cache). */
    @Volatile var computations: Int = 0
        private set

    /** O último resultado, se for para os mesmos dados; senão null (a tela mostra o anterior enquanto recalcula). */
    fun cached(s: Snapshot, today: LocalDate, options: EngineOptions): EngineReport? {
        val k = lastKey ?: return null
        return if (k.first === s && k.second == today && k.third == options) lastReport else null
    }

    /** O último resultado, de quaisquer dados. */
    val latest: EngineReport? get() = lastReport

    suspend fun run(s: Snapshot, today: LocalDate, options: EngineOptions): EngineReport = mutex.withLock { runLocked(s, today, options) }

    /** Para os testes de tela, que precisam do resultado já na primeira composição. */
    fun runBlocking(s: Snapshot, today: LocalDate, options: EngineOptions): EngineReport = synchronized(this) { runLocked(s, today, options) }

    private fun runLocked(s: Snapshot, today: LocalDate, options: EngineOptions): EngineReport {
        cached(s, today, options)?.let { return it }
        val r = FinanceEngine.run(s, today, options, hasher)
        computations++
        lastReport = r
        lastKey = Triple(s, today, options)
        return r
    }

    companion object {
        /** Nos testes de tela (Robolectric), o motor roda na hora, para as imagens já saírem com o resultado. */
        @Volatile var inline: Boolean = false
    }
}

/** RF-MOT-11: as percepções candidatas, cada uma com uma nota (dinheiro envolvido × certeza × novidade). */
internal object Ranking {
    private fun money(c: Long) = Money.format(c)

    /** Nota: log do valor (em unidades da moeda) × certeza × novidade. */
    private fun score(impact: Long, certainty: Double, novelty: Double = 1.0) = log10(1.0 + abs(impact) / 100.0) * certainty * novelty

    fun rank(l: Ledger, r: EngineReport, max: Int = 5): List<RankedInsight> {
        val out = ArrayList<RankedInsight>()
        val o = r.outlook
        val today = l.today

        if (o.chanceNegative >= 0.3 && o.base.dayOfMonth < o.base.daysInMonth) {
            val pct = (o.chanceNegative * 100).roundToInt()
            out += RankedInsight(Res.s(R.string.engine_ins_chance_red, Num.percent(pct)), false, score(abs(o.low), o.chanceNegative), "month")
        } else if (o.chanceNegative <= 0.1 && o.low > 0 && o.base.dayOfMonth >= 10 && o.certainty != Certainty.LOW) {
            out += RankedInsight(Res.s(R.string.engine_ins_blue, money(o.low), money(o.high)), true, score(o.mid, 0.6, 0.7), "month")
        }
        for (rec in r.recurrences) {
            if (rec.priceChange > 0 && rec.status != RecurrenceStatus.ENDED && !rec.last.isBefore(today.minusDays(45))) {
                out += RankedInsight(
                    Res.s(R.string.engine_ins_price_up, rec.label, money(rec.lastAmount - rec.priceChange), money(rec.lastAmount)),
                    false, score(rec.priceChange * 12, 0.9), "rec:" + rec.key,
                )
            }
            if (rec.type == TxType.INCOME && rec.status == RecurrenceStatus.LATE && rec.billId == null) {
                out += RankedInsight(Res.s(R.string.engine_ins_income_late, rec.label, rec.anchorDay), false, score(rec.amount, 0.6), "rec:" + rec.key)
            }
        }
        for (a in r.anomalies) when (a.kind) {
            AnomalyKind.DUPLICATE -> out += RankedInsight(Res.s(R.string.engine_ins_duplicate, a.label, money(a.amount)), false, score(a.amount, 0.6), "dup:" + a.txId)
            AnomalyKind.OUTLIER -> out += RankedInsight(Res.s(R.string.engine_ins_outlier, a.label, money(a.amount), money(a.usual)), null, score(a.amount - a.usual, 0.6), "out:" + a.txId)
            AnomalyKind.CATEGORY_HIGH -> out += RankedInsight(Res.s(R.string.engine_ins_category_high, a.label, money(a.amount), money(a.usual)), false, score(a.amount - a.usual, 0.7), "cat:" + a.categoryId)
            AnomalyKind.PRICE_UP -> {} // já entrou pelas recorrências
        }
        for (p in r.pace) {
            val name = l.category[p.categoryId]?.name ?: continue
            out += RankedInsight(Res.s(R.string.engine_ins_pace, name, p.crossDay), false, score(p.projected - p.limit, 0.7), "cat:" + p.categoryId)
        }
        r.projection.firstNegativeMonth?.let { m ->
            if (m != today.ym()) out += RankedInsight(Res.s(R.string.engine_ins_negative_month, Dates.monthInSentence(m)), false, score(abs(r.projection.tightestMid), 0.6, 0.8), "future")
        }
        for ((id, g) in r.goals) {
            val goal = l.goal[id] ?: continue
            val deadline = goal.deadline ?: continue
            val eta = g.etaYm ?: continue
            if (!g.done && eta.isAfter(deadline.ym()) && (g.chanceByDeadline ?: 1.0) < 0.5) {
                out += RankedInsight(
                    Res.s(R.string.engine_ins_goal_late, goal.name, Dates.monthInSentence(eta), Dates.monthInSentence(deadline.ym())),
                    false, score(g.target - g.saved, 0.5, 0.7), "goal:$id",
                )
            }
        }
        val subs = r.monthlyRecurringExpense
        if (subs > 0 && r.recurrences.count { it.type == TxType.EXPENSE && it.status != RecurrenceStatus.ENDED } >= 2) {
            out += RankedInsight(Res.s(R.string.engine_ins_recurring_total, money(subs)), null, score(subs, 0.4, 0.5), "subs")
        }
        // As percepções de antes (comparação com o mês passado) continuam, com nota menor.
        for ((i, ins) in l.insights().withIndex()) out += RankedInsight(ins.text, ins.positive, 0.5 - i * 0.05, "classic:$i")

        return out.sortedByDescending { it.score }.distinctBy { it.topic }.take(max)
    }
}
