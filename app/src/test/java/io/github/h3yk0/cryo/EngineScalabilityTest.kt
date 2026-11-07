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

/* =============================== 101–200: escalabilidade =============================== */

@RunWith(Parameterized::class)
class EngineScalabilityTest(private val case: Int) {
    init { JvmStrings.install() }

    companion object {
        @JvmStatic @Parameterized.Parameters(name = "escalabilidade {0}")
        fun cases() = (101..200).map { arrayOf<Any>(it) }

        private val today = LocalDate.of(2026, 11, 7)

        init {
            JvmStrings.install()
            // Aquece a JVM uma vez, para o tempo medido ser o do motor e não o de carregar classes.
            repeat(3) { FinanceEngine.run(Synthetic.random(3_000, 3, 2, 1, today), today) }
        }
    }

    /**
     * De 100 a 50 mil movimentações, de 1 a 40 contas, de 1 a 10 anos: o tempo cresce no máximo na proporção dos dados.
     * Mede com n e com n/2: crescimento linear dá ~2×; quadrático daria ~4×.
     */
    @Test fun cresceNaProporcaoDosDados() {
        val k = case - 101
        val n = (100 * 500.0.pow(k / 99.0)).toInt()
        val accounts = 1 + k % 40
        val years = 1 + k % 10
        val full = Synthetic.random(n, accounts, years, case.toLong(), today)
        val half = Synthetic.random(n / 2, accounts, years, case.toLong(), today)
        var report: EngineReport? = null
        val tFull = minOf(Synthetic.ms { report = FinanceEngine.run(full, today) }, Synthetic.ms { FinanceEngine.run(full, today) })
        val tHalf = minOf(Synthetic.ms { FinanceEngine.run(half, today) }, Synthetic.ms { FinanceEngine.run(half, today) })
        val problems = Synthetic.invariants(report!!)
        println("escalabilidade $case: $n movimentações, $accounts contas, $years anos: $tFull ms (metade: $tHalf ms)")
        assertEquals("caso $case (n=$n): $problems", emptyList<String>(), problems)
        assertTrue("caso $case: n=$n levou $tFull ms (n/2: $tHalf ms)", tFull <= 3.5 * tHalf + 60)
        assertTrue("caso $case: n=$n levou $tFull ms", tFull <= 300 + n * 0.05)
    }
}
