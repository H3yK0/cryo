// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.importer

import io.github.h3yk0.cryo.data.db.Category
import io.github.h3yk0.cryo.data.db.CategoryKind
import io.github.h3yk0.cryo.data.db.CreditCard
import io.github.h3yk0.cryo.data.db.Tx
import io.github.h3yk0.cryo.data.db.TxType
import io.github.h3yk0.cryo.domain.CardMath
import io.github.h3yk0.cryo.domain.PhraseParser
import io.github.h3yk0.cryo.domain.Snapshot
import io.github.h3yk0.cryo.domain.key
import io.github.h3yk0.cryo.domain.normalized
import java.security.MessageDigest
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs

/**
 * Para onde vão as movimentações: uma conta ou um cartão.
 * [invoiceYm] (só cartão): todas na mesma fatura (ano*100 + mês); nulo = cada compra na fatura da sua data.
 */
data class ImportTarget(val accountId: Long? = null, val cardId: Long? = null, val invoiceYm: Int? = null) {
    val isCard: Boolean get() = cardId != null
}

enum class RowFlag {
    NONE,
    /** Já veio numa importação anterior (mesmo identificador). */
    IMPORTED_BEFORE,
    /** Parece com algo que você já lançou à mão. */
    LOOKS_DUPLICATE,
    /** Pagamento recebido na fatura do cartão: ele sai da conta, então entra pelo extrato da conta (ou em Pagar fatura). */
    CARD_CREDIT,
    /** Pagamento de fatura saindo da conta: no Cryo ele é registrado no cartão, para o gasto não contar duas vezes. */
    INVOICE_PAYMENT,
}

/** Uma linha na tela de conferência. */
data class ImportRow(
    val index: Int,
    val line: StatementLine,
    val type: TxType,
    /** Sempre positivo, em centavos. */
    val amount: Long,
    val categoryId: Long?,
    val key: String,
    val flag: RowFlag,
    /** A movimentação que já existe e parece ser a mesma. */
    val match: Tx? = null,
    val selected: Boolean = flag == RowFlag.NONE,
    /**
     * Pagamento de fatura: o outro lado do pagamento (o cartão pago, quando o extrato é da conta; a conta de onde o
     * dinheiro saiu, quando o extrato é do cartão). Com ele, a linha vira um "pagamento de fatura", e não um gasto.
     */
    val pairedId: Long? = null,
) {
    val signed: Long get() = if (type == TxType.INCOME) amount else -amount

    /** Pode virar pagamento de fatura (saída na conta ou crédito no cartão). */
    val canBeInvoicePayment: Boolean get() = flag == RowFlag.INVOICE_PAYMENT || flag == RowFlag.CARD_CREDIT
}

/** Sugere a categoria pela descrição: o que você já usou antes e as palavras-chave de cada categoria. */
class CategoryGuesser(categories: List<Category>, private val learned: Map<String, Long>) {
    private class NameRule(val category: Category, val regex: Regex, val score: Double)
    private class KeywordRule(val category: Category, val keyword: String, val regex: Regex)

    private val active = categories.filter { !it.archived }

    // Expressões montadas uma vez só (um extrato pode ter milhares de linhas).
    private val names = active.map { c ->
        val n = c.name.normalized()
        NameRule(c, Regex("\\b${Regex.escape(n)}\\b"), 8.0 + n.length / 100.0)
    }
    private val keywords = active.flatMap { c ->
        c.keywords.split(',').map { it.trim().normalized() }.filter { it.isNotEmpty() }
            .map { kw -> KeywordRule(c, kw, Regex("\\b${Regex.escape(kw)}(?:s|es)?\\b")) }
    }

    fun guess(kind: CategoryKind, description: String): Long? {
        val cats = active.filter { it.kind == kind }
        if (cats.isEmpty()) return null
        val ids = cats.mapTo(HashSet()) { it.id }
        val text = description.normalized().replace(NOT_WORD, " ").replace(SPACES, " ").trim()
        var bestId: Long? = null
        var best = -1.0
        fun offer(id: Long, score: Double) {
            if (score > best && id in ids) { best = score; bestId = id }
        }
        if (text.isNotEmpty()) learned[description.normalized().trim()]?.let { offer(it, 10.0) }
        for (r in names) if (r.category.kind == kind && r.regex.containsMatchIn(text)) offer(r.category.id, r.score)
        text.split(' ').filter { it.length >= 3 && it !in PhraseParser.STOP_WORDS }.forEach { w -> learned[w]?.let { offer(it, 6.0) } }
        for (r in keywords) {
            if (r.category.kind != kind) continue
            val kw = r.keyword
            when {
                r.regex.containsMatchIn(text) -> offer(r.category.id, (if (kw.contains(' ')) 9.0 else 4.0) + kw.length / 100.0)
                // "PADARIAPAODEOURO": banco cola as palavras; só vale para palavras longas.
                kw.length >= 6 && !kw.contains(' ') && text.contains(kw) -> offer(r.category.id, 3.0 + kw.length / 100.0)
            }
        }
        return bestId ?: cats.firstOrNull { it.icon == "more" }?.id
    }

    private companion object {
        val NOT_WORD = Regex("[^a-z0-9 ]")
        val SPACES = Regex("\\s+")
    }
}

object ImportPlanner {
    private val INVOICE_PAYMENT = Regex("""\b(pagamento|pagto|pgto|pag)\b.*\b(fatura|cartao)\b|\bfatura\b.*\b(pagamento|pagto|pgto|cartao)\b""")

    /** Crédito na fatura que é pagamento (e não estorno, cashback ou ajuste, que entram como crédito no cartão). */
    private val CARD_PAYMENT = Regex("""\b(pagamento|pagto|pgto|pag)\b""")

    /** Faturas de cartão costumam listar as compras como valores positivos: aí o sinal precisa virar. */
    fun suggestInvert(st: Statement, target: ImportTarget): Boolean {
        if (!target.isCard && !st.isCard) return false
        val positive = st.lines.count { it.amount > 0 }
        return target.isCard && positive > st.lines.size - positive
    }

    /**
     * Fatura paga por um pagamento feito em [date]: a última que já tinha fechado
     * (fecha dia 3 e vence dia 10: um pagamento em 05/10 é da fatura que vence em outubro).
     */
    fun paidInvoice(card: CreditCard, date: LocalDate): Int = CardMath.invoiceFor(card, date).minusMonths(1).key()

    /** Fatura em que cairia a compra mais recente (o padrão ao importar uma fatura). */
    fun defaultInvoice(st: Statement, card: CreditCard): Int? =
        st.lines.maxOfOrNull { it.date }?.let { CardMath.invoiceFor(card, it).key() }

    /** De qual extrato veio: da conta ou do cartão de destino ("a3", "c7"). */
    fun scopeOf(target: ImportTarget): String = target.cardId?.let { "c$it" } ?: "a${target.accountId}"

    /** A linha, sem o destino: o identificador do banco ou um resumo de data, valor e descrição. */
    fun baseKeyOf(line: StatementLine): String {
        line.id?.let { return "id:$it" }
        val raw = "${line.date}|${line.amount}|${line.description.normalized()}"
        val digest = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray(Charsets.UTF_8))
        return "h:" + digest.take(16).joinToString("") { "%02x".format(it) }
    }

    /**
     * Chave que reconhece a mesma linha numa próxima importação: "a3|ofx.id:123", "c7|csv.h:9f3a...", e "#1", "#2"...
     * quando a mesma linha aparece mais de uma vez no arquivo (duas compras iguais no mesmo dia continuam sendo duas,
     * e bancos que repetem o identificador não escondem movimentações novas). O formato vem junto porque o mesmo
     * período em outro formato (PDF e depois CSV) traz as descrições escritas de outro jeito: a chave muda, e então
     * essas linhas passam pela busca de repetidas (veja [comparable]) em vez de entrar de novo sem aviso.
     */
    fun keyOf(line: StatementLine, occurrence: Int, target: ImportTarget, format: StatementFormat): String =
        "${scopeOf(target)}|${format.name.lowercase()}.${baseKeyOf(line)}" + if (occurrence > 0) "#$occurrence" else ""

    private fun scopeOfKey(key: String): String = if (key.contains('|')) key.substringBefore('|') else ""

    /** Formato e tipo da chave ("ofx.id", "csv.h"). */
    private fun kindOfKey(key: String): String = key.substringAfter('|').substringBefore(':')

    /** Efeito de uma movimentação já existente no saldo do destino (para achar repetidas). */
    private fun effect(t: Tx, target: ImportTarget): Long? = if (target.isCard) {
        if (t.cardId != target.cardId) null else when (t.type) {
            TxType.EXPENSE -> -t.amount
            TxType.INCOME, TxType.CARD_PAYMENT -> t.amount
            else -> null
        }
    } else {
        val a = target.accountId
        when {
            t.type == TxType.TRANSFER && t.toAccountId == a -> t.amount
            t.accountId != a || t.cardId != null && t.type != TxType.CARD_PAYMENT -> null
            t.type in setOf(TxType.INCOME, TxType.INVEST_OUT, TxType.GOAL_OUT, TxType.DEBT_IN) -> t.amount
            else -> -t.amount
        }
    }

    fun plan(
        st: Statement,
        target: ImportTarget,
        snapshot: Snapshot,
        learned: Map<String, Long>,
        invert: Boolean,
    ): List<ImportRow> {
        val guesser = CategoryGuesser(snapshot.categories, learned)
        val imported = snapshot.txs.filter { it.importKey != null }.groupBy { it.importKey!! }
        val candidates = snapshot.txs.mapNotNull { t -> effect(t, target)?.let { t to it } }
        val scope = scopeOf(target)
        val card = target.cardId?.let { id -> snapshot.cards.firstOrNull { it.id == id } }
        val used = HashSet<Long>()
        val seen = HashMap<String, Int>()

        return st.lines.mapIndexed { i, line ->
            val base = baseKeyOf(line)
            val occurrence = seen.merge(base, 1, Int::plus)!! - 1
            val key = keyOf(line, occurrence, target, st.format)
            val lineKind = kindOfKey(key)
            val signed = if (invert) -line.amount else line.amount
            val type = if (signed < 0) TxType.EXPENSE else TxType.INCOME
            val category = guesser.guess(if (type == TxType.EXPENSE) CategoryKind.EXPENSE else CategoryKind.INCOME, line.description)

            // No cartão, a mesma linha numa fatura diferente é outra cobrança: muitos bancos repetem a data, a descrição e
            // o valor da compra original em cada parcela. O pagamento da fatura fica fora disso (ele vai para a fatura
            // que pagou, não para a escolhida).
            val invoice = card?.let { target.invoiceYm ?: CardMath.invoiceFor(it, line.date).key() }
            val before = imported[key]?.firstOrNull { invoice == null || it.type == TxType.CARD_PAYMENT || it.invoiceYm == invoice }
            val similar = if (before != null) null else candidates
                .filter { (t, eff) ->
                    t.id !in used && eff == signed && comparable(t, scope, lineKind) && (
                        abs(ChronoUnit.DAYS.between(t.date, line.date)) <= 3 ||
                            target.invoiceYm != null && t.invoiceYm == target.invoiceYm
                        )
                }
                .minByOrNull { (t, _) -> abs(ChronoUnit.DAYS.between(t.date, line.date)) }?.first
            similar?.let { used += it.id }
            val desc = line.description.normalized()
            val flag = when {
                before != null -> RowFlag.IMPORTED_BEFORE
                similar != null -> RowFlag.LOOKS_DUPLICATE
                target.isCard && signed > 0 && CARD_PAYMENT.containsMatchIn(desc) && !desc.contains("estorno") -> RowFlag.CARD_CREDIT
                !target.isCard && signed < 0 && INVOICE_PAYMENT.containsMatchIn(desc) -> RowFlag.INVOICE_PAYMENT
                else -> RowFlag.NONE
            }
            ImportRow(i, line, type, abs(signed), category, key, flag, before ?: similar)
        }
    }

    /**
     * Uma movimentação que já existe entra na busca por repetidas quando foi lançada à mão, quando veio do extrato de
     * outra conta ou cartão (o pagamento da fatura importado pela fatura do cartão aparece de novo no extrato da conta),
     * ou quando veio deste mesmo extrato num formato diferente (OFX e depois CSV, ou PDF e depois planilha). Já uma importação
     * anterior do mesmo extrato e no mesmo formato tem a chave da linha: se a chave é outra, a movimentação é outra
     * (o cafezinho de todo dia não vira "repetida" do de ontem).
     */
    private fun comparable(t: Tx, scope: String, lineKind: String): Boolean {
        val k = t.importKey ?: return true
        return scopeOfKey(k) != scope || kindOfKey(k) != lineKind
    }

    /**
     * A movimentação que vai para o Cryo. [card]: o cartão de destino (extrato de cartão).
     * [cards]: todos os cartões, para os pagamentos de fatura que saem da conta.
     */
    fun toTx(
        row: ImportRow,
        target: ImportTarget,
        card: CreditCard?,
        now: Long = System.currentTimeMillis(),
        cards: Map<Long, CreditCard> = emptyMap(),
    ): Tx {
        val base = Tx(
            type = row.type, amount = row.amount, date = row.line.date, description = row.line.description.take(120),
            categoryId = row.categoryId, importKey = row.key, createdAt = now,
        )
        val paired = row.pairedId?.takeIf { row.canBeInvoicePayment }
        return when {
            // Crédito na fatura que é o pagamento dela: sai da conta escolhida e entra no cartão.
            paired != null && target.isCard && card != null && row.type == TxType.INCOME -> base.copy(
                type = TxType.CARD_PAYMENT, categoryId = null, accountId = paired, cardId = card.id,
                invoiceYm = paidInvoice(card, row.line.date),
            )
            // Pagamento de fatura saindo da conta: entra no cartão escolhido.
            paired != null && !target.isCard && row.type == TxType.EXPENSE && cards[paired] != null -> base.copy(
                type = TxType.CARD_PAYMENT, categoryId = null, accountId = target.accountId, cardId = paired,
                invoiceYm = paidInvoice(cards.getValue(paired), row.line.date),
            )
            target.isCard && card != null ->
                base.copy(cardId = card.id, invoiceYm = target.invoiceYm ?: CardMath.invoiceFor(card, row.line.date).key())
            else -> base.copy(accountId = target.accountId)
        }
    }
}
