// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.importer

import io.github.h3yk0.cryo.domain.engine.CategoryModel
import io.github.h3yk0.cryo.data.db.Category
import io.github.h3yk0.cryo.data.db.CategoryKind
import io.github.h3yk0.cryo.data.db.CreditCard
import io.github.h3yk0.cryo.data.db.Tx
import io.github.h3yk0.cryo.data.db.TxType
import io.github.h3yk0.cryo.domain.BillMatch
import io.github.h3yk0.cryo.domain.BillMatcher
import io.github.h3yk0.cryo.domain.MatchLevel
import io.github.h3yk0.cryo.domain.PayLine
import io.github.h3yk0.cryo.domain.CardMath
import io.github.h3yk0.cryo.domain.PhraseLang
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
    /** RF-CFX-02: a conta fixa (e o mês) de que esta linha parece ser o pagamento. */
    val bill: BillMatch? = null,
    /** Entra já ligada à conta fixa. Com certeza vem marcado; quando só parece, a pessoa decide. */
    val payBill: Boolean = bill?.level == MatchLevel.CERTAIN,
) {
    val signed: Long get() = if (type == TxType.INCOME) amount else -amount

    /** Pode virar pagamento de fatura (saída na conta ou crédito no cartão). */
    val canBeInvoicePayment: Boolean get() = flag == RowFlag.INVOICE_PAYMENT || flag == RowFlag.CARD_CREDIT
}

/** Sugere a categoria pela descrição: o que você já usou antes e as palavras-chave de cada categoria. */
class CategoryGuesser(
    categories: List<Category>,
    private val learned: Map<String, Long>,
    /** RF-MOT-12: a categoria aprendida, usada só quando nenhuma regra acha a categoria. */
    private val model: ((CategoryKind, String) -> Long?)? = null,
) {
    private class NameRule(val category: Category, val regex: Regex, val score: Double)
    private class KeywordRule(val category: Category, val keyword: String, val regex: Regex)

    private val active = categories.filter { !it.archived }

    // Expressões montadas uma vez só (um extrato pode ter milhares de linhas).
    private val names = active.map { c ->
        val n = c.name.normalized()
        NameRule(c, PhraseParser.wordRegex(n), 8.0 + n.length / 100.0)
    }
    private val keywords = active.flatMap { c ->
        c.keywords.split(',').map { it.trim().normalized() }.filter { it.isNotEmpty() }
            .map { kw -> KeywordRule(c, kw, PhraseParser.wordRegex(kw, PhraseLang.current.plural, PhraseLang.current.prefix)) }
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
        val stop = PhraseParser.STOP_WORDS
        text.split(' ').filter { it.length >= PhraseParser.minLearned(it) && it !in stop }.forEach { w -> learned[w]?.let { offer(it, 6.0) } }
        for (r in keywords) {
            if (r.category.kind != kind) continue
            val kw = r.keyword
            when {
                r.regex.containsMatchIn(text) -> offer(r.category.id, (if (kw.contains(' ')) 9.0 else 4.0) + kw.length / 100.0)
                // "PADARIAPAODEOURO": banco cola as palavras; só vale para palavras longas.
                kw.length >= 6 && !kw.contains(' ') && text.contains(kw) -> offer(r.category.id, 3.0 + kw.length / 100.0)
            }
        }
        if (bestId == null) model?.invoke(kind, description)?.let { offer(it, 1.0) }
        return bestId ?: cats.firstOrNull { it.icon == "more" }?.id
    }

    private companion object {
        val NOT_WORD = Regex("[^\\p{L}\\p{M}\\p{N} ]")
        val SPACES = Regex("\\s+")
    }
}

object ImportPlanner {
    /**
     * Pagamento de fatura saindo da conta, nas línguas do app: "PAGAMENTO FATURA", "CHASE CREDIT CRD AUTOPAY",
     * "PAGO TARJETA", "PAIEMENT CARTE", "KREDITKARTENABRECHNUNG", "カード代金", "카드대금", "信用卡还款"...
     */
    private val INVOICE_PAYMENT = Regex(
        """\b(pagamento|pagto|pgto|pag)\b.*\b(fatura|cartao)\b|\bfatura\b.*\b(pagamento|pagto|pgto|cartao)\b|""" +
            """\b(payment|pmt|autopay|epay|e-payment|epayment)\b.*\b(card|crd|credit|amex|visa|mastercard|discover)\b|""" +
            """\b(card|crd|amex|discover|capital one)\b.*\b(payment|pmt|autopay|epay|e-payment|epayment)\b|""" +
            """\bpago\b.*\b(tarjeta|tdc)\b|\btarjeta\b.*\bpago\b|\b(paiement|reglement)\b.*\bcarte\b|""" +
            """kreditkartenabrechnung|kartenabrechnung|\bpagamento\b.*\bcarta\b|""" +
            """カード代金|カード引落|カードご利用代金|クレジットカード支払|카드대금|카드결제대금|카드값|信用卡还款|还信用卡|""" +
            """क्रेडिट कार्ड भुगतान|कार्ड बिल""",
    )

    /** Crédito na fatura que é pagamento (e não estorno, cashback ou ajuste, que entram como crédito no cartão). */
    private val CARD_PAYMENT = Regex(
        """\b(pagamento|pagto|pgto|pag|payment|pmt|autopay|pago|paiement|reglement|zahlung|ausgleich)\b|お支払|入金|결제|입금|还款|भुगतान""",
    )

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

    /** Uma linha do extrato na conferência, antes da busca por parecidas: a chave, o valor com sinal, a fatura e o já importado. */
    private class PlanLine(val index: Int, val line: StatementLine, val key: String, val signed: Long, val invoice: Int?, val before: Tx?)

    /** Uma linha do extrato e um lançamento do Cryo que podem ser a mesma movimentação (ERR-062). */
    private class SimilarPair(val line: Int, val tx: Tx, val sameInvoice: Boolean, val days: Long)

    fun plan(
        st: Statement,
        target: ImportTarget,
        snapshot: Snapshot,
        learned: Map<String, Long>,
        invert: Boolean,
        model: CategoryModel? = null,
        /** RF-CFX-04: a conta fixa que a pessoa já confirmou para uma descrição (só o código dela fica guardado). */
        alias: (String) -> Long? = { null },
    ): List<ImportRow> {
        val guesser = CategoryGuesser(snapshot.categories, learned, model?.let { m ->
            { kind: CategoryKind, desc: String ->
                m.guess(if (kind == CategoryKind.INCOME) TxType.INCOME else TxType.EXPENSE, desc, accountId = target.accountId, cardId = target.cardId)?.categoryId
            }
        })
        val imported = snapshot.txs.filter { it.importKey != null }.groupBy { it.importKey!! }
        val candidates = snapshot.txs.mapNotNull { t -> effect(t, target)?.let { t to it } }
        val scope = scopeOf(target)
        val card = target.cardId?.let { id -> snapshot.cards.firstOrNull { it.id == id } }
        val seen = HashMap<String, Int>()

        // Primeira passada: a chave de cada linha e o que já veio numa importação anterior.
        val lines = st.lines.mapIndexed { i, line ->
            val base = baseKeyOf(line)
            val occurrence = seen.merge(base, 1, Int::plus)!! - 1
            val key = keyOf(line, occurrence, target, st.format)
            val signed = if (invert) -line.amount else line.amount
            // No cartão, a mesma linha numa fatura diferente é outra cobrança: muitos bancos repetem a data, a descrição e
            // o valor da compra original em cada parcela. O pagamento da fatura fica fora disso (ele vai para a fatura
            // que pagou, não para a escolhida).
            val invoice = card?.let { target.invoiceYm ?: CardMath.invoiceFor(it, line.date).key() }
            val before = imported[key]?.firstOrNull { invoice == null || it.type == TxType.CARD_PAYMENT || it.invoiceYm == invoice }
            PlanLine(i, line, key, signed, invoice, before)
        }

        // ERR-062 (auditoria da 1.6): as parecidas. Mesmo valor e até 3 dias; no cartão, também a PARCELA do Cryo na mesma
        // fatura escolhida (o banco repete a data da compra em cada parcela). A mesma fatura sozinha não basta: duas compras
        // de R$ 80 em dias diferentes são duas compras. Cada linha fica com uma parecida só e cada lançamento com uma linha
        // só, escolhidos primeiro pela mesma fatura e depois pela data mais perto, nunca pela ordem do arquivo (senão a
        // Farmácia de 05/11 "levava" o Restaurante de 19/11 lançado à mão, e o Restaurante entrava em dobro).
        val pairs = ArrayList<SimilarPair>()
        for (p in lines) {
            if (p.before != null) continue
            val lineKind = kindOfKey(p.key)
            for ((t, eff) in candidates) {
                if (eff != p.signed || !comparable(t, scope, lineKind)) continue
                val days = abs(ChronoUnit.DAYS.between(t.date, p.line.date))
                val sameInvoice = p.invoice != null && t.invoiceYm == p.invoice
                if (days <= 3 || target.invoiceYm != null && sameInvoice && t.installmentTotal > 1) pairs += SimilarPair(p.index, t, sameInvoice, days)
            }
        }
        pairs.sortWith(compareBy<SimilarPair>({ !it.sameInvoice }, { it.days }, { it.line }, { it.tx.id }))
        val similarOf = HashMap<Int, Tx>()
        val used = HashSet<Long>()
        for (c in pairs) if (c.line !in similarOf && c.tx.id !in used) {
            similarOf[c.line] = c.tx
            used += c.tx.id
        }

        val rows = lines.map { p ->
            val i = p.index
            val line = p.line
            val key = p.key
            val signed = p.signed
            val before = p.before
            val type = if (signed < 0) TxType.EXPENSE else TxType.INCOME
            val category = guesser.guess(if (type == TxType.EXPENSE) CategoryKind.EXPENSE else CategoryKind.INCOME, line.description)
            val similar = similarOf[i]
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
        return withBills(rows, snapshot, alias)
    }

    /**
     * RF-CFX-02 e 03: marca as linhas que são o pagamento de uma conta fixa em aberto, e as que repetem um pagamento que a
     * pessoa já marcou à mão ("Já paguei"), em qualquer conta ou cartão (a busca comum por repetidas só olha a conta de destino).
     */
    private fun withBills(rows: List<ImportRow>, snapshot: Snapshot, alias: (String) -> Long?): List<ImportRow> {
        val bills = snapshot.bills.filter { it.active }
        if (bills.isEmpty()) return rows
        val byId = bills.associateBy { it.id }
        val linked = snapshot.txs.filter { it.billId != null && it.billYm != null && it.billId in byId }
        val paid = linked.mapTo(HashSet()) { it.billId!! to it.billYm!! }
        val used = HashSet<Long>()
        fun plain(r: ImportRow) = r.flag == RowFlag.NONE && r.pairedId == null && (r.type == TxType.EXPENSE || r.type == TxType.INCOME)
        val checked = rows.map { r ->
            if (!plain(r)) return@map r
            val same = linked.firstOrNull { t ->
                val days = abs(ChronoUnit.DAYS.between(t.date, r.line.date))
                t.id !in used && t.type == r.type && abs(t.amount - r.amount) <= r.amount / 100 && (
                    days <= 3 || days <= 7 && byId.getValue(t.billId!!).let { b -> BillMatcher.sameName(b.name, r.line.description) || alias(r.line.description) == b.id }
                    )
            } ?: return@map r
            used += same.id
            r.copy(flag = RowFlag.LOOKS_DUPLICATE, match = same, selected = false)
        }
        // O que já está no app sem ligação, mas é o pagamento com certeza, também conta como pago.
        paid += BillMatcher.certain(bills, snapshot.txs, paid).keys
        val lines = LinkedHashMap<Int, PayLine>()
        for (r in checked) if (plain(r)) lines[r.index] = PayLine(r.line.date, r.amount, r.line.description, r.type == TxType.EXPENSE)
        val found = BillMatcher.assign(lines, bills, paid) { alias(it.description) }
        return if (found.isEmpty()) checked else checked.map { r -> found[r.index]?.let { r.copy(bill = it, payBill = it.level == MatchLevel.CERTAIN) } ?: r }
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
        val tx = when {
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
        // RF-CFX-02: o pagamento da conta fixa nasce ligado a ela, na conta ou no cartão do extrato (onde aconteceu).
        val m = row.bill
        return when {
            m == null || tx.type == TxType.CARD_PAYMENT -> tx
            row.payBill -> tx.copy(billId = m.bill.id, billYm = m.ym.key(), categoryId = tx.categoryId ?: m.bill.categoryId)
            // Era certo e a pessoa desmarcou: fica dito no lançamento, para o Cryo não reconhecer sozinho depois.
            m.level == MatchLevel.CERTAIN -> tx.copy(billYm = BillMatcher.NOT_A_BILL)
            else -> tx
        }
    }
}
