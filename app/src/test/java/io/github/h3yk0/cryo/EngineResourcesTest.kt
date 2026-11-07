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

/* ============================ 301–400: economia de recursos ============================ */

@RunWith(Parameterized::class)
class EngineResourcesTest(private val case: Int) {
    init { JvmStrings.install() }

    companion object {
        @JvmStatic @Parameterized.Parameters(name = "recursos {0}")
        fun cases() = (301..400).map { arrayOf<Any>(it) }

        private val today = LocalDate.of(2026, 11, 7)

        init {
            JvmStrings.install()
            repeat(2) { FinanceEngine.run(Personas.generate(Personas.spec(Personas.Kind.STABLE), today, 1), today) }
        }

        // A compilação dos testes usa a biblioteca do Android (sem java.lang.management): a medida vem por reflexão, na JVM.
        private val bean = Class.forName("java.lang.management.ManagementFactory").getMethod("getThreadMXBean").invoke(null)
        private val allocatedOf = Class.forName("com.sun.management.ThreadMXBean").getMethod("getThreadAllocatedBytes", Long::class.javaPrimitiveType)

        private fun allocated(block: () -> Unit): Long {
            val id = Thread.currentThread().id
            val before = allocatedOf.invoke(bean, id) as Long
            block()
            return (allocatedOf.invoke(bean, id) as Long) - before
        }
    }

    @Test fun usaPoucosRecursos() {
        val spec = Personas.spec(Personas.Kind.entries[case % 4]).copy(months = 6 + case % 30)
        val s = Personas.generate(spec, today, case.toLong())
        when (case % 4) {
            // Memória por cálculo: no máximo ~15 KB por movimentação mais 40 MB fixos (as simulações).
            0 -> {
                val bytes = allocated { FinanceEngine.run(s, today) }
                println("recursos $case: ${bytes / 1024 / 1024} MB alocados para ${s.txs.size} movimentações")
                val limit = 40L * 1024 * 1024 + s.txs.size * 15L * 1024
                assertTrue("caso $case: ${bytes / 1024 / 1024} MB para ${s.txs.size} movimentações", bytes <= limit)
            }
            // Nada fica preso na memória: depois de muitos cálculos, os resultados antigos podem ser recolhidos.
            1 -> {
                val refs = ArrayList<WeakReference<EngineReport>>()
                val runner = EngineRunner()
                repeat(12) { i -> refs += WeakReference(runBlocking { runner.run(s.copy(), today.minusDays(i.toLong()), EngineOptions()) }) }
                repeat(5) { System.gc(); Thread.sleep(20) }
                val alive = refs.count { it.get() != null }
                assertTrue("caso $case: $alive resultados ainda na memória", alive <= 1)
            }
            // Número de cenários fixo, seja qual for o tamanho dos dados.
            2 -> {
                val r = FinanceEngine.run(s, today)
                assertEquals(Outlook.SCENARIOS, r.outlook.scenarios)
                assertEquals(Projections.SCENARIOS, r.projection.scenarios)
                assertTrue(r.projection.horizonDays <= 366)
                assertTrue(r.outlook.band.size <= 32)
            }
            // Nenhuma thread nova e um cache que não cresce: só o último resultado fica guardado.
            else -> {
                val before = Thread.activeCount()
                val runner = EngineRunner()
                runBlocking {
                    val first = s.copy()
                    runner.run(first, today, EngineOptions())
                    repeat(5) { runner.run(s.copy(), today, EngineOptions()) }
                    runner.run(first, today, EngineOptions())
                }
                assertEquals("caso $case: o cache guarda só o último (recalcula o primeiro)", 7, runner.computations)
                assertTrue("caso $case: threads ${Thread.activeCount()} (antes $before)", Thread.activeCount() <= before)
            }
        }
    }
}
