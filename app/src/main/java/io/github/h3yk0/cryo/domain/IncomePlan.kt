// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.domain

import io.github.h3yk0.cryo.data.db.CategoryKind
import io.github.h3yk0.cryo.data.db.InvestmentKind
import io.github.h3yk0.cryo.data.db.TxType
import io.github.h3yk0.cryo.domain.engine.RecurrenceStatus
import io.github.h3yk0.cryo.domain.engine.Recurrences
import java.time.YearMonth

/*
 * RF-ORC-07/08 (1.6): o orçamento pela renda. A pessoa conta quanto ganha e o Cryo sugere quanto gastar em cada categoria
 * e quanto guardar. Tudo é sugestão (PRI-09): nada muda nos orçamentos até ela tocar em "Usar estes limites", e cada
 * número pode ser trocado. Regras puras, sem Android: a tela só mostra e guarda as escolhas (data/IncomePlanStore.kt).
 */

/** As duas partes do gasto na regra 50/30/20: o essencial e o estilo de vida. */
enum class Bucket { NEEDS, WANTS }

/**
 * O que o Cryo achou nos lançamentos: [fixed] = renda fixa por mês (contas fixas de entrada e o que se repete, como o
 * salário); a renda variável (o que entrou fora disso) nos [variableMonths] meses dos últimos 3 com movimento, com a média,
 * o mês mais fraco e o mais forte.
 */
data class IncomeGuess(
    val fixed: Long,
    val variableAvg: Long,
    val variableLow: Long,
    val variableHigh: Long,
    val variableMonths: Int,
)

/** O que a pessoa escolheu (ou o que o Cryo achou, enquanto ela não mudou). Porcentagens inteiras; guardar = o resto. */
data class PlanInputs(
    val fixed: Long,
    val variable: Long,
    val useVariable: Boolean,
    val needsPct: Int = 50,
    val wantsPct: Int = 30,
) {
    val savePct: Int get() = (100 - needsPct - wantsPct).coerceAtLeast(0)
}

/**
 * Uma categoria no plano: [typical] = quanto a pessoa costuma gastar por mês (a mediana dos últimos meses), [limit] = o
 * limite sugerido (o típico, ou menos quando a parte dela na renda não comporta; arredondado para cima até a unidade
 * da moeda), [current] = o limite que ela já tem hoje (null se não tem).
 */
data class PlanCategory(val categoryId: Long, val bucket: Bucket, val typical: Long, val limit: Long, val current: Long?)

/** Para onde vai o dinheiro de guardar (RF-ORC-08): só tipos de investimento, nunca produtos ou bancos. */
enum class SavingKind { RESERVE, GOALS, LONG_TERM }

data class SavingSlice(val kind: SavingKind, val amount: Long)

/**
 * O plano: [base] = a renda que entra no orçamento; [needs], [wants] e [save] = as três partes; [typicalNeeds] e
 * [typicalWants] = quanto a pessoa costuma gastar em cada parte; [fixedCosts] = as contas fixas de saída por mês;
 * [reserveTarget] = 6 meses do essencial; [reserveNow] = o que ela já tem em aplicações de resgate rápido; [saving] = como
 * dividir o que sobra para guardar; [reserveMonths] = em quantos meses a reserva fica completa (null se já está, ou se
 * não sobra nada para guardar).
 */
data class IncomePlan(
    val base: Long,
    val needs: Long,
    val wants: Long,
    val save: Long,
    val typicalNeeds: Long,
    val typicalWants: Long,
    val fixedCosts: Long,
    val categories: List<PlanCategory>,
    val reserveTarget: Long,
    val reserveNow: Long,
    val saving: List<SavingSlice>,
    val reserveMonths: Int?,
) {
    val reserveDone: Boolean get() = reserveTarget == 0L || reserveNow >= reserveTarget

    /** As contas fixas sozinhas já passam da parte do essencial: o plano avisa (RF-ORC-07). */
    val fixedCostsOver: Boolean get() = base > 0 && fixedCosts > needs
}

object IncomePlanner {
    /**
     * Ícones das categorias que contam como essenciais quando a pessoa ainda não escolheu (mercado, transporte, moradia,
     * contas da casa, saúde, escola, filhos, pets, dívidas). As outras vão para o estilo de vida. Ela pode trocar cada uma.
     */
    val NEEDS_ICONS: Set<String> = setOf(
        "cart", "car", "bus", "home", "bolt", "water", "wifi", "phone", "health", "school", "baby", "gas", "receipt", "pets",
        "build", "debt",
    )

    /** O padrão (regra 50/30/20). */
    const val NEEDS_PCT = 50
    const val WANTS_PCT = 30

    /** Meses da reserva de emergência (do essencial). */
    const val RESERVE_MONTHS = 6

    /** As aplicações que contam como reserva: as de baixo risco e resgate rápido. */
    private val RESERVE_KINDS = setOf(InvestmentKind.SAVINGS, InvestmentKind.FIXED_INCOME, InvestmentKind.TREASURY)

    /** Os 3 meses mais recentes, antes deste, que têm algum lançamento (dos últimos 12). */
    fun months(l: Ledger): List<YearMonth> {
        val ym = l.today.ym()
        return (1..12).map { ym.minusMonths(it.toLong()) }.filter { l.monthTxs(it).isNotEmpty() }.take(3)
    }

    /** A parte de cada mês de uma conta fixa que não é mensal (a cada 3 meses = um terço; IPVA em 3x no ano = 3/12). */
    private fun monthly(amount: Long, every: Int, installments: Int): Long {
        val e = every.coerceAtLeast(1)
        return amount * installments.coerceIn(1, e) / e
    }

    /** O que o Cryo acha da renda da pessoa (RF-ORC-07). */
    fun guess(l: Ledger): IncomeGuess {
        val today = l.today
        val recurrences = Recurrences.detect(l.s.txs, today)
        val incomeBills = l.s.bills.filter { it.active && it.kind == CategoryKind.INCOME && (it.endYm == 0 || it.endYm >= today.ym().key()) }
        val billIds = incomeBills.mapTo(HashSet()) { it.id }
        val fromBills = incomeBills.sumOf { monthly(it.amount, it.every, it.installments) }
        // O que se repete e não é uma conta fixa já contada (o salário que o Cryo descobriu sozinho).
        val fromRepeats = recurrences
            .filter { it.type == TxType.INCOME && it.status != RecurrenceStatus.ENDED && (it.billId == null || it.billId !in billIds) }
            .sumOf { it.monthlyAmount }
        val repeated = HashSet<Long>().also { set -> recurrences.filter { it.type == TxType.INCOME }.forEach { set += it.txIds } }
        // Renda variável: o que entrou em cada mês fora das contas fixas e do que se repete.
        val perMonth = months(l).map { ym ->
            l.monthTxs(ym).filter { it.type == TxType.INCOME && it.billId == null && it.id !in repeated }.sumOf { it.amount }
        }
        return IncomeGuess(
            fixed = fromBills + fromRepeats,
            variableAvg = if (perMonth.isEmpty()) 0L else Math.round(perMonth.average()),
            variableLow = perMonth.minOrNull() ?: 0L,
            variableHigh = perMonth.maxOrNull() ?: 0L,
            variableMonths = perMonth.count { it > 0 },
        )
    }

    /** A parte da categoria: a escolha da pessoa, ou o padrão pelo ícone. */
    fun bucketOf(icon: String, chosen: Bucket?): Bucket = chosen ?: if (icon in NEEDS_ICONS) Bucket.NEEDS else Bucket.WANTS

    /**
     * O plano (RF-ORC-07/08). [buckets] = as categorias que a pessoa trocou de parte. Os limites sugeridos são o que ela
     * costuma gastar; quando a soma de uma parte passa do que a renda comporta, todos encolhem juntos, na mesma proporção
     * (ninguém zera e a ordem entre eles continua a mesma).
     */
    fun plan(l: Ledger, inputs: PlanInputs, buckets: Map<Long, Bucket> = emptyMap()): IncomePlan {
        val fixed = inputs.fixed.coerceAtLeast(0)
        val variable = if (inputs.useVariable) inputs.variable.coerceAtLeast(0) else 0L
        val base = fixed + variable
        val needs = base * inputs.needsPct / 100
        val wants = base * inputs.wantsPct / 100
        val save = (base - needs - wants).coerceAtLeast(0)

        // Quanto a pessoa costuma gastar em cada categoria: a mediana dos últimos meses (um mês fora do comum não manda).
        val months = months(l)
        val expense = l.categories(CategoryKind.EXPENSE)
        val typical = HashMap<Long, Long>()
        for (c in expense) {
            val v = if (months.isNotEmpty()) {
                median(months.map { l.categorySpent(it, c.id) })
            } else {
                // Nenhum mês completo ainda: o mês de agora, esticado até o fim.
                val ym = l.today.ym()
                Math.round(l.categorySpent(ym, c.id).toDouble() * ym.lengthOfMonth() / l.today.dayOfMonth)
            }
            if (v > 0) typical[c.id] = v
        }
        val current = l.s.budgets.associate { it.categoryId to it.limitAmount }

        val unit = 100L   // os limites sugeridos ficam em valores redondos (a unidade da moeda)
        val categories = ArrayList<PlanCategory>()
        // RF-ORC-09 (1.6): o limite sugerido nunca fica abaixo das contas fixas mensais da categoria.
        val floors = expense.associate { it.id to BudgetBills.floorFor(l, it.id) }
        for (bucket in Bucket.entries) {
            val members = expense.filter { c -> bucketOf(c.icon, buckets[c.id]) == bucket && ((typical[c.id] ?: 0L) > 0 || (floors[c.id] ?: 0L) > 0) }
            val sum = members.sumOf { typical[it.id] ?: 0L }
            val target = if (bucket == Bucket.NEEDS) needs else wants
            for (c in members) {
                val t = typical[c.id] ?: 0L
                val raw = if (sum <= target || sum == 0L) t else (t.toDouble() * target / sum).toLong()
                val limit = (maxOf(raw, floors[c.id] ?: 0L) + unit - 1) / unit * unit
                categories += PlanCategory(c.id, bucket, t, limit, current[c.id])
            }
        }
        categories.sortWith(compareBy<PlanCategory> { it.bucket }.thenByDescending { it.typical })

        val typicalNeeds = categories.filter { it.bucket == Bucket.NEEDS }.sumOf { it.typical }
        val typicalWants = categories.filter { it.bucket == Bucket.WANTS }.sumOf { it.typical }
        val fixedCosts = l.s.bills
            .filter { it.active && it.kind == CategoryKind.EXPENSE && (it.endYm == 0 || it.endYm >= l.today.ym().key()) }
            .sumOf { monthly(it.amount, it.every, it.installments) }

        // RF-ORC-08: primeiro a reserva de emergência (6 meses do essencial); completa, o guardar vai para objetivos e longo prazo.
        val reserveTarget = RESERVE_MONTHS * typicalNeeds
        val reserveNow = l.activeInvestments.filter { it.kind in RESERVE_KINDS }.sumOf { l.investmentValue(it.id) }
        val done = reserveTarget == 0L || reserveNow >= reserveTarget
        val saving = when {
            save <= 0 -> emptyList()
            !done -> listOf(SavingSlice(SavingKind.RESERVE, save))
            else -> listOf(SavingSlice(SavingKind.GOALS, save / 2), SavingSlice(SavingKind.LONG_TERM, save - save / 2))
        }
        val reserveMonths = if (!done && save > 0) ((reserveTarget - reserveNow + save - 1) / save).toInt() else null
        return IncomePlan(
            base, needs, wants, save, typicalNeeds, typicalWants, fixedCosts, categories, reserveTarget, reserveNow, saving, reserveMonths,
        )
    }

    private fun median(xs: List<Long>): Long {
        if (xs.isEmpty()) return 0L
        val s = xs.sorted()
        val n = s.size
        return if (n % 2 == 1) s[n / 2] else (s[n / 2 - 1] + s[n / 2]) / 2
    }
}
