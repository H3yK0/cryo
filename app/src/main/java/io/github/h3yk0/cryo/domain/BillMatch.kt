// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.domain

import io.github.h3yk0.cryo.data.db.Bill
import io.github.h3yk0.cryo.data.db.CategoryKind
import io.github.h3yk0.cryo.data.db.Tx
import io.github.h3yk0.cryo.data.db.TxType
import java.text.Normalizer
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.abs

/** O quanto uma movimentação parece o pagamento de uma conta fixa. */
enum class MatchLevel { LIKELY, CERTAIN }

/** Uma movimentação (ou uma linha de extrato ainda não importada) vista pelo casador. */
data class PayLine(val date: LocalDate, val amount: Long, val description: String, val expense: Boolean)

/** A linha é (ou parece ser) o pagamento de [bill] no mês [ym]. [score] só serve para desempatar. */
data class BillMatch(val bill: Bill, val ym: YearMonth, val level: MatchLevel, val score: Double)

/**
 * RF-CFX-01: o casador. Diz se uma movimentação é o pagamento de uma conta fixa, olhando três sinais: o **nome** (as
 * palavras do nome da conta fixa estão na descrição: "netflix" em "NETFLIX.COM"), o **valor** (igual, ou parecido quando a
 * conta varia, como a luz) e a **data** (perto do vencimento). Regra pura, sem Android: a importação, o livro-caixa, o
 * "Já paguei" e a tela de organizar usam a mesma conta.
 *
 * - CERTA: nome e valor igual; ou um apelido que a pessoa já confirmou (RF-CFX-04) com valor parecido.
 * - PROVÁVEL: nome com valor parecido; ou valor igual a até 5 dias do vencimento, sem o nome. Só vale depois de perguntar.
 */
object BillMatcher {
    /**
     * Em [Tx.billYm], sem [Tx.billId]: a pessoa disse que esta movimentação **não é** pagamento de conta fixa. Fica no próprio
     * lançamento (vai no backup, e qualquer gravação refaz as contas), sem mudar o banco de dados.
     */
    const val NOT_A_BILL = 0

    /** Pagar adiantado, até quando ainda é "deste mês"; e atrasado. */
    private const val EARLY_DAYS = 15L
    private const val LATE_DAYS = 25L
    private const val NEAR_DAYS = 5L

    /** Diferença de valor que ainda é "igual" (centavos de arredondamento) e a que ainda é "a conta varia". */
    private const val EQUAL = 0.01
    private const val NEAR = 0.30

    /** Só gasto ou entrada comum, ainda sem conta fixa: parcela, dívida e pagamento de fatura nunca casam. */
    fun eligible(t: Tx): Boolean =
        (t.type == TxType.EXPENSE || t.type == TxType.INCOME) && t.billId == null && t.billYm != NOT_A_BILL &&
            t.installmentTotal <= 1 && t.debtId == null

    fun line(t: Tx) = PayLine(t.date, t.amount, t.description, t.type == TxType.EXPENSE)

    /** As palavras de um texto, sem acento, sem números e sem pontuação ("NETFLIX.COM 12/11" → netflix, com). */
    fun words(text: String): List<String> {
        val plain = Normalizer.normalize(text.take(200).lowercase(Locale.ROOT), Normalizer.Form.NFD)
        val out = ArrayList<String>()
        val sb = StringBuilder()
        for (ch in plain) {
            when {
                Character.getType(ch) == Character.NON_SPACING_MARK.toInt() -> {}
                Character.isLetter(ch) -> sb.append(ch)
                else -> if (sb.isNotEmpty()) { out += sb.toString(); sb.setLength(0) }
            }
        }
        if (sb.isNotEmpty()) out += sb.toString()
        return out
    }

    /**
     * Todas as palavras do nome da conta fixa (as de 3 letras ou mais) aparecem na descrição. Palavra de 4 letras ou mais
     * também vale pelo começo ("energ" do banco = "energia" da conta); as curtas ("luz", "gas") só inteiras.
     */
    fun sameName(billName: String, description: String): Boolean {
        val wanted = words(billName).filter { it.length >= 3 }
        if (wanted.isEmpty()) return false
        val have = words(description)
        return wanted.all { w ->
            have.any { h -> h == w || (w.length >= 4 && h.length >= 4 && (h.startsWith(w) || w.startsWith(h))) }
        }
    }

    /** Os meses em que uma movimentação desta data pode ser o pagamento de [bill]. */
    fun months(bill: Bill, date: LocalDate): List<YearMonth> =
        listOf(date.ym().minusMonths(1), date.ym(), date.ym().plusMonths(1)).filter { ym ->
            if (ym.key() < bill.startYm) return@filter false
            val delta = ChronoUnit.DAYS.between(ym.dayClamped(bill.dueDay), date)
            delta in -EARLY_DAYS..LATE_DAYS
        }

    /** A nota de [line] como pagamento de [bill] em [ym], ou null se não casa. [alias]: a pessoa já confirmou esta descrição. */
    fun rate(bill: Bill, ym: YearMonth, line: PayLine, alias: Boolean = false): BillMatch? {
        if ((bill.kind == CategoryKind.EXPENSE) != line.expense || ym.key() < bill.startYm || bill.amount <= 0) return null
        val delta = ChronoUnit.DAYS.between(ym.dayClamped(bill.dueDay), line.date)
        if (delta < -EARLY_DAYS || delta > LATE_DAYS) return null
        val diff = abs(line.amount - bill.amount).toDouble() / bill.amount
        val equal = diff <= EQUAL
        val near = diff <= NEAR
        val name = alias || sameName(bill.name, line.description)
        val level = when {
            name && equal -> MatchLevel.CERTAIN
            alias && near -> MatchLevel.CERTAIN
            name && near -> MatchLevel.LIKELY
            equal && abs(delta) <= NEAR_DAYS -> MatchLevel.LIKELY
            else -> return null
        }
        val score = (if (name) 3.0 else 0.0) + (if (equal) 2.0 else 1.0 - diff / NEAR) + (1.0 - abs(delta).toDouble() / LATE_DAYS)
        return BillMatch(bill, ym, level, score)
    }

    private class Pair3<K>(val key: K, val match: BillMatch)

    /**
     * Distribui as linhas pelas contas fixas em aberto: cada conta-mês fica com uma linha só (a de maior nota) e cada linha
     * com uma conta só. [paid]: as contas-mês que já têm pagamento (conta, mês). [alias]: a conta fixa que a pessoa já
     * confirmou para esta linha, se houver.
     */
    fun <K> assign(
        lines: Map<K, PayLine>, bills: List<Bill>, paid: Set<Pair<Long, Int>>, alias: (PayLine) -> Long? = { null },
    ): Map<K, BillMatch> {
        val active = bills.filter { it.active }
        if (active.isEmpty() || lines.isEmpty()) return emptyMap()
        val all = ArrayList<Pair3<K>>()
        for ((k, line) in lines) {
            val known = alias(line)
            for (b in active) for (ym in months(b, line.date)) {
                if ((b.id to ym.key()) in paid) continue
                rate(b, ym, line, alias = known == b.id)?.let { all += Pair3(k, it) }
            }
        }
        all.sortWith(compareByDescending<Pair3<K>> { it.match.level.ordinal }.thenByDescending { it.match.score })
        val out = LinkedHashMap<K, BillMatch>()
        val taken = HashSet<Pair<Long, Int>>()
        for (c in all) {
            val slot = c.match.bill.id to c.match.ym.key()
            if (c.key in out || slot in taken) continue
            out[c.key] = c.match
            taken += slot
        }
        return out
    }

    /**
     * RF-CFX-07: os pagamentos que o Cryo reconhece sozinho (só os CERTOS: mesmo nome e mesmo valor), para a previsão e os
     * avisos não contarem em dobro antes de a pessoa confirmar. Procura pelo valor primeiro (busca binária), para não
     * comparar nomes em milhares de movimentações.
     */
    fun certain(bills: List<Bill>, txs: List<Tx>, paid: Set<Pair<Long, Int>>): Map<Pair<Long, Int>, Tx> {
        val active = bills.filter { it.active && it.amount > 0 }
        if (active.isEmpty()) return emptyMap()
        val open = txs.filter(::eligible).sortedBy { it.amount }
        if (open.isEmpty()) return emptyMap()
        val lines = LinkedHashMap<Long, PayLine>()
        val byId = HashMap<Long, Tx>()
        for (b in active) {
            val lo = (b.amount * (1 - EQUAL)).toLong()
            val hi = (b.amount * (1 + EQUAL)).toLong() + 1
            var i = open.binarySearchBy(lo) { it.amount }.let { if (it < 0) -it - 1 else it }
            while (i > 0 && open[i - 1].amount >= lo) i--
            while (i < open.size && open[i].amount <= hi) {
                val t = open[i++]
                if (sameName(b.name, t.description)) { lines[t.id] = line(t); byId[t.id] = t }
            }
        }
        val out = HashMap<Pair<Long, Int>, Tx>()
        for ((id, m) in assign(lines, active, paid)) if (m.level == MatchLevel.CERTAIN) out[m.bill.id to m.ym.key()] = byId.getValue(id)
        return out
    }
}

/**
 * RF-CFX-05: uma coisa que dá para arrumar. [tx] é a movimentação que já está no app e parece o pagamento de [bill] em
 * [ym]. Com [duplicateOf], a conta-mês já tem um pagamento marcado à mão e [tx] é a mesma coisa vinda do extrato: pago em dobro.
 */
data class BillSuggestion(val bill: Bill, val ym: YearMonth, val tx: Tx, val level: MatchLevel, val duplicateOf: Tx? = null)

/**
 * RF-CFX-05, 06 e 08: o que o Cryo encontra nas movimentações que já existem (mês atual e os 3 anteriores) para as contas
 * fixas: pagamentos sem a ligação e contas pagas em dobro. Só sugere: nada muda sem a pessoa tocar. Regra pura.
 */
object BillReconciler {
    const val MONTHS_BACK = 3L
    private const val DOUBLE_DAYS = 7L

    fun suggestions(s: Snapshot, today: LocalDate, only: Bill? = null, alias: (String) -> Long? = { null }): List<BillSuggestion> {
        val bills = (if (only != null) listOf(only) else s.bills).filter { it.active }
        if (bills.isEmpty()) return emptyList()
        val first = today.ym().minusMonths(MONTHS_BACK)
        val last = today.ym()
        val from = first.atDay(1).minusDays(20)
        val byId = bills.associateBy { it.id }
        val linked = s.txs.filter { it.billId in byId && it.billYm != null }
        val paid = linked.mapTo(HashSet()) { it.billId!! to it.billYm!! }
        val open = s.txs.filter { BillMatcher.eligible(it) && !it.date.isBefore(from) && !it.date.isAfter(today) }
        val out = ArrayList<BillSuggestion>()

        // Pagas em dobro: o pagamento marcado à mão e a mesma coisa que veio do extrato depois.
        val doubled = HashSet<Long>()
        for (p in linked) {
            val ym = ymOf(p.billYm!!)
            if (p.importKey != null || ym.isBefore(first) || ym.isAfter(last)) continue
            val b = byId.getValue(p.billId!!)
            val twin = open.firstOrNull { t ->
                val days = abs(ChronoUnit.DAYS.between(t.date, p.date))
                t.id !in doubled && t.type == p.type && abs(t.amount - p.amount) <= p.amount / 100 && days <= DOUBLE_DAYS &&
                    (BillMatcher.sameName(b.name, t.description) || alias(t.description) == b.id || t.importKey != null && days <= 3)
            } ?: continue
            doubled += twin.id
            out += BillSuggestion(b, ym, twin, MatchLevel.CERTAIN, duplicateOf = p)
        }

        val lines = LinkedHashMap<Long, PayLine>()
        val txById = HashMap<Long, Tx>()
        for (t in open) if (t.id !in doubled) { lines[t.id] = BillMatcher.line(t); txById[t.id] = t }
        for ((id, m) in BillMatcher.assign(lines, bills, paid) { alias(it.description) }) {
            if (m.ym.isBefore(first) || m.ym.isAfter(last)) continue
            out += BillSuggestion(m.bill, m.ym, txById.getValue(id), m.level)
        }
        return out.sortedWith(compareByDescending<BillSuggestion> { it.ym }.thenBy { it.bill.name }.thenBy { it.tx.id })
    }
}
