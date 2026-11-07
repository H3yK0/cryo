// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.domain.engine

import io.github.h3yk0.cryo.data.db.Tx
import io.github.h3yk0.cryo.data.db.TxType
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.log2

/** Uma categoria sugerida e a certeza (0 a 1). */
data class CategoryGuess(val categoryId: Long, val probability: Double)

/**
 * RF-MOT-12: a categoria aprendida. Um classificador bayesiano simples ("naive Bayes") treinado só com as movimentações da
 * própria pessoa: as palavras da descrição, a conta ou o cartão e a faixa de valor. O que é recente pesa mais (meia-vida de
 * um ano). Fica só na memória (RF-SEG-02): é refeito a partir dos dados quando precisa.
 *
 * Só sugere quando a certeza é de pelo menos [MIN_PROBABILITY] e alguma palavra da descrição já foi vista antes: nunca
 * "chuta" pela categoria mais comum.
 */
class CategoryModel private constructor(private val byType: Map<TxType, Part>) {

    private class Part {
        val classWeight = HashMap<Long, Double>()
        val featureTotal = HashMap<Long, Double>()
        val counts = HashMap<String, HashMap<Long, Double>>()
        var total = 0.0
    }

    fun guess(type: TxType, description: String, amount: Long? = null, accountId: Long? = null, cardId: Long? = null): CategoryGuess? {
        val part = byType[type] ?: return null
        if (part.classWeight.isEmpty()) return null
        val words = TxKey.tokens(description).map { "w:$it" }
        if (words.none { it in part.counts }) return null
        val feats = words + context(amount, accountId, cardId)
        val vocab = part.counts.size.toDouble()
        val scores = HashMap<Long, Double>()
        for ((cat, cw) in part.classWeight) {
            var s = ln(cw / part.total)
            val denom = (part.featureTotal[cat] ?: 0.0) + ALPHA * vocab
            for (f in feats) {
                val c = part.counts[f]?.get(cat) ?: 0.0
                s += ln((c + ALPHA) / denom)
            }
            scores[cat] = s
        }
        val best = scores.maxByOrNull { it.value } ?: return null
        var z = 0.0
        for (v in scores.values) z += exp(v - best.value)
        val p = 1.0 / z
        return if (p >= MIN_PROBABILITY) CategoryGuess(best.key, p) else null
    }

    companion object {
        const val MIN_PROBABILITY = 0.6
        private const val ALPHA = 1.0
        private const val HALF_LIFE = 365.0
        private const val MAX_AGE_DAYS = 730L

        private fun context(amount: Long?, accountId: Long?, cardId: Long?): List<String> = buildList {
            if (cardId != null) add("c:$cardId") else if (accountId != null) add("a:$accountId")
            if (amount != null && amount > 0) add("v:" + log2(amount / 100.0 + 1).toInt())
        }

        fun train(txs: List<Tx>, today: LocalDate): CategoryModel {
            val parts = mapOf(TxType.EXPENSE to Part(), TxType.INCOME to Part())
            for (t in txs) {
                val part = parts[t.type] ?: continue
                val cat = t.categoryId ?: continue
                if (t.debtId != null || t.description.contains(" · ")) continue // textos criados pelo próprio app
                val age = ChronoUnit.DAYS.between(t.date, today)
                if (age > MAX_AGE_DAYS) continue
                val words = TxKey.tokens(t.description)
                if (words.isEmpty()) continue
                val w = Stats.decay(age, HALF_LIFE)
                part.classWeight[cat] = (part.classWeight[cat] ?: 0.0) + w
                part.total += w
                for (f in words.map { "w:$it" } + context(t.amount, t.accountId, t.cardId)) {
                    val m = part.counts.getOrPut(f) { HashMap() }
                    m[cat] = (m[cat] ?: 0.0) + w
                    part.featureTotal[cat] = (part.featureTotal[cat] ?: 0.0) + w
                }
            }
            return CategoryModel(parts)
        }
    }
}
