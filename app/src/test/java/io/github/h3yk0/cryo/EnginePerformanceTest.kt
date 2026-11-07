// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo

import io.github.h3yk0.cryo.data.db.Account
import io.github.h3yk0.cryo.data.db.Bill
import io.github.h3yk0.cryo.data.db.CategoryKind
import io.github.h3yk0.cryo.data.db.CreditCard
import io.github.h3yk0.cryo.data.db.Debt
import io.github.h3yk0.cryo.data.db.DebtAdjustment
import io.github.h3yk0.cryo.data.db.Goal
import io.github.h3yk0.cryo.data.db.Investment
import io.github.h3yk0.cryo.data.db.InvestmentYield
import io.github.h3yk0.cryo.data.db.Tx
import io.github.h3yk0.cryo.data.db.TxType
import io.github.h3yk0.cryo.domain.CardMath
import io.github.h3yk0.cryo.domain.Ledger
import io.github.h3yk0.cryo.domain.Snapshot
import io.github.h3yk0.cryo.domain.engine.EngineOptions
import io.github.h3yk0.cryo.domain.engine.EngineReport
import io.github.h3yk0.cryo.domain.engine.EngineRunner
import io.github.h3yk0.cryo.domain.engine.FinanceEngine
import io.github.h3yk0.cryo.domain.engine.Outlook
import io.github.h3yk0.cryo.domain.engine.Projections
import io.github.h3yk0.cryo.domain.engine.Recurrences
import io.github.h3yk0.cryo.domain.engine.Rng
import io.github.h3yk0.cryo.domain.engine.Anomalies
import io.github.h3yk0.cryo.domain.key
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.lang.ref.WeakReference
import java.time.LocalDate
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.roundToLong

/*
 * RF-MOT-15: os 500 testes de garantia do motor, pedidos pelo Hanry (07/11/2026). 5 blocos de 100 casos, um tipo de teste
 * por bloco, cada caso com dados diferentes (semente, perfil e tamanho próprios):
 *   1–100 estabilidade · 101–200 escalabilidade · 201–300 desempenho · 301–400 economia de recursos · 401–500 segurança
 * (o bloco de segurança está em EngineSecurityTest, porque precisa do Android simulado).
 */

/* ================================ 201–300: desempenho ================================ */

@RunWith(Parameterized::class)
class EnginePerformanceTest(private val case: Int) {
    init { JvmStrings.install() }

    companion object {
        @JvmStatic @Parameterized.Parameters(name = "desempenho {0}")
        fun cases() = (201..300).map { arrayOf<Any>(it) }

        private val today = LocalDate.of(2026, 11, 7)

        init {
            JvmStrings.install()
            repeat(3) { FinanceEngine.run(Personas.generate(Personas.spec(Personas.Kind.STABLE), today, 1), today) }
        }
    }

    /**
     * Perfis realistas (até 5 anos, ~10 mil movimentações): o motor inteiro e cada parte dentro do limite (RF-MOT-13; na
     * JVM o limite é folgado), e o mesmo dado de novo vem do cache, na hora.
     */
    @Test fun dentroDoLimiteDeTempo() {
        val kind = Personas.Kind.entries[case % 4]
        val big = case % 2 == 0
        val spec = Personas.spec(kind).let { if (big) it.copy(months = 60, perDay = 4.5) else it.copy(months = 12 + case % 24) }
        val s = Personas.generate(spec, today.minusDays((case % 28).toLong()), case.toLong())
        val day = today.minusDays((case % 28).toLong())
        when (case % 5) {
            0 -> {
                val t = Synthetic.ms { FinanceEngine.run(s, day) }
                println("desempenho $case: motor inteiro com ${s.txs.size} movimentações em $t ms")
                assertTrue("caso $case: motor inteiro com ${s.txs.size} movimentações levou $t ms", t <= 1500)
            }
            1 -> {
                val l = Ledger(s, day)
                val rec = Recurrences.detect(s.txs, day)
                val t = Synthetic.ms { Outlook.month(l, rec, 1) }
                assertTrue("caso $case: previsão do mês levou $t ms", t <= 500)
            }
            2 -> {
                val l = Ledger(s, day)
                val rec = Recurrences.detect(s.txs, day)
                val t = Synthetic.ms { Projections.project(l, rec, 1) }
                assertTrue("caso $case: próximos 12 meses levou $t ms", t <= 700)
            }
            3 -> {
                val l = Ledger(s, day)
                val t = Synthetic.ms { Anomalies.find(l, Recurrences.detect(s.txs, day)) }
                assertTrue("caso $case: recorrências e fora do padrão levaram $t ms", t <= 600)
            }
            else -> runBlocking {
                val runner = EngineRunner()
                runner.run(s, day, EngineOptions())
                val t = Synthetic.ms { runBlocking { runner.run(s, day, EngineOptions()) } }
                assertEquals(1, runner.computations)
                assertTrue("caso $case: o cache levou $t ms", t <= 20)
            }
        }
    }
}
