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

/* ================================ 1–100: estabilidade ================================ */

@RunWith(Parameterized::class)
class EngineStabilityTest(private val case: Int) {
    init { JvmStrings.install() }

    companion object {
        @JvmStatic @Parameterized.Parameters(name = "estabilidade {0}")
        fun cases() = (1..100).map { arrayOf<Any>(it) }
    }

    /** Dados estranhos ou extremos: nunca quebra, nunca NaN, faixas em ordem e o mesmo dado dá o mesmo resultado. */
    @Test fun nuncaQuebraESempreDaOMesmoResultado() {
        val (s, today) = Synthetic.weird(case % 10, case * 7919L)
        val a = FinanceEngine.run(s, today)
        val problems = Synthetic.invariants(a)
        assertEquals("caso $case (tipo ${case % 10}): $problems", emptyList<String>(), problems)
        assertEquals("caso $case: o mesmo dado precisa dar o mesmo resultado", a, FinanceEngine.run(s, today))
    }
}
