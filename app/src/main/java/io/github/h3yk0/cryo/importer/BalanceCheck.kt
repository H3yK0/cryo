// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.importer

import io.github.h3yk0.cryo.domain.Snapshot
import io.github.h3yk0.cryo.domain.Ledger
import java.time.LocalDate

/**
 * RF-IMP-24: o saldo do banco no fim do extrato contra o saldo do Cryo **no mesmo dia** (pedido do Hanry, 15/11/2026:
 * importou o mês e o Cryo mostrava menos que o banco — exatamente o que já estava na conta antes do dia 1º).
 * O extrato só traz o que se mexeu no período; o que havia antes é o saldo inicial da conta. Regra pura.
 */
object BalanceCheck {
    /** O último dia com movimentação no extrato: o saldo final do banco vale para o fim desse dia. */
    fun endDate(st: Statement): LocalDate? = st.lines.maxOfOrNull { it.date }

    /** O saldo do Cryo no fim de [day] (o que foi lançado depois não entra na comparação). */
    fun cryoBalance(s: Snapshot, accountId: Long, day: LocalDate): Long = Ledger(s, day).balance(accountId)

    /**
     * O acerto automático (pedido do Hanry, 16/11/2026: "todo o cálculo seja feito automaticamente"). Quando o extrato
     * **diz** quanto havia na conta antes da primeira movimentação, o Cryo parte do mesmo ponto: devolve quanto somar
     * ao saldo inicial da conta para a véspera do extrato ficar igual à do banco. Não depende de quais linhas a pessoa
     * escolheu importar. Null quando o arquivo não diz: o OFX só traz o saldo do dia em que foi baixado, que pode ser
     * bem depois da última linha — aí o Cryo mostra a diferença e a pessoa decide com um toque.
     */
    fun autoShift(st: Statement, s: Snapshot, accountId: Long): Long? {
        if (st.isCard) return null
        val bank = st.opening ?: return null
        val first = st.lines.minOfOrNull { it.date } ?: return null
        return bank - cryoBalance(s, accountId, first.minusDays(1))
    }

    /**
     * Quanto falta no Cryo (positivo) ou sobra (negativo) para ficar igual ao banco. Null quando não dá para comparar:
     * o arquivo não traz o saldo ou é uma fatura de cartão.
     */
    fun gap(st: Statement, s: Snapshot, accountId: Long, today: LocalDate): Long? {
        if (st.isCard) return null
        val bank = st.balance ?: return null
        return bank - cryoBalance(s, accountId, endDate(st) ?: today)
    }
}
