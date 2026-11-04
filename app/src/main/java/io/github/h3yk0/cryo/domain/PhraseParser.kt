// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.domain

import io.github.h3yk0.cryo.R
import io.github.h3yk0.cryo.data.db.Account
import io.github.h3yk0.cryo.data.db.AccountType
import io.github.h3yk0.cryo.data.db.Category
import io.github.h3yk0.cryo.data.db.CategoryKind
import io.github.h3yk0.cryo.data.db.CreditCard
import io.github.h3yk0.cryo.data.db.Debt
import io.github.h3yk0.cryo.data.db.DebtKind
import io.github.h3yk0.cryo.data.db.Goal
import io.github.h3yk0.cryo.data.db.Investment
import io.github.h3yk0.cryo.data.db.TxType
import io.github.h3yk0.cryo.data.DefaultData
import io.github.h3yk0.cryo.domain.PhraseLang.Writing
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.Normalizer
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale
import kotlin.math.max

data class ParseContext(
    val today: LocalDate,
    val accounts: List<Account>,
    val cards: List<CreditCard>,
    val categories: List<Category>,
    val investments: List<Investment> = emptyList(),
    val goals: List<Goal> = emptyList(),
    /** palavra/descrição normalizada -> categoria usada antes */
    val learned: Map<String, Long> = emptyMap(),
    val defaultAccountId: Long? = null,
    val debts: List<Debt> = emptyList(),
    /** Língua das frases (a do app; nos testes, a de cada caso). */
    val language: AppLanguage = AppLocale.language,
    /** Para a ordem de "11/03" (dia/mês ou mês/dia): o locale do app. */
    val locale: Locale = AppLocale.locale,
)

data class ParsedEntry(
    val type: TxType,
    val amount: Long?,
    val description: String,
    val categoryId: Long?,
    val accountId: Long?,
    val toAccountId: Long?,
    val cardId: Long?,
    val installments: Int,
    val date: LocalDate,
    val investmentId: Long?,
    val goalId: Long?,
    val debtId: Long? = null,
)

/**
 * Entende frases do dia a dia, como "mercado 120 no débito", "recebi 3000 de salário",
 * "tv 1200 em 10x no cartão" ou "transferi 200 para a poupança", nas 10 línguas do app (RF-REG-09):
 * "lunch 25 on debit", "コーヒー 500円", "점심 만원 카드로", "चाय 20 रुपये नकद"...
 *
 * Funciona 100% no aparelho, sem internet: são regras + o que o app aprendeu com o seu histórico.
 * As palavras de cada língua ficam em [PhraseLang]; aqui ficam as regras, iguais para todas.
 */
class PhraseParser(private val ctx: ParseContext) {

    private class Token(val orig: String, val norm: String, val start: Int, val end: Int, val suffix: String = "") {
        var used = false
    }

    private data class Cand(val cents: Long, val score: Int, val pos: Int, val range: IntRange)
    private data class EMatch(val id: Long, val score: Int, val pos: Int, val ranges: List<IntRange>)

    private val L = PhraseLang.forLanguage(ctx.language)

    /** Línguas sem espaço e o coreano: procura por pedaço de palavra, sem exigir o fim da palavra. */
    private val loose = L.writing != Writing.SPACED

    /** Como juntar as palavras da descrição: sem espaço em japonês e chinês. */
    private val joiner = if (L.writing == Writing.UNSPACED) "" else " "

    private lateinit var toks: List<Token>
    private lateinit var text: String

    fun parse(input: String): ParsedEntry {
        toks = tokenize(input)
        text = toks.joinToString(" ") { it.norm }

        val date = parseDate()
        val installments = parseInstallments()
        val amount = parseAmount()

        val goalMatches = matchNames(ctx.goals.filter { !it.archived }.map { it.id to it.name })
        val invMatches = matchNames(ctx.investments.filter { !it.archived }.map { it.id to it.name })
        val accMatches = matchNames(ctx.accounts.filter { !it.archived }.map { it.id to it.name })
        val cardMatches = matchNames(ctx.cards.filter { !it.archived }.map { it.id to it.name })
        val activeDebts = ctx.debts.filter { !it.archived }
        val debtMatches = matchNames(activeDebts.map { it.id to "${it.name} ${it.creditor}" }, L.debtGenericN)
        val debtMatch = debtMatches.firstOrNull()

        // Dívidas: "paguei a parcela da moto", "paguei 200 pro joão", "peguei 300 emprestado com o joão"
        val borrowVerb = has(L.borrow)
        val installmentWord = re(words(L.installmentWord)).findAll(text).any { free(it.range) }
        val debtNoun = installmentWord || has(L.debtNoun)

        val goalWord = has(L.goalWord)
        val goalOutVerb = has(L.goalOut)
        val goalInVerb = has(L.goalIn)
        val investVerb = has(L.invest)
        val redeemVerb = has(L.redeem)
        val payVerb = has(L.pay)
        val transferVerb = has(L.transfer)
        val incomeVerb = has(L.income)
        val expenseVerb = has(L.expense)
        val incomeNoun = has(L.incomeNoun)

        // Sem o nome da dívida na frase: a única que existe, ou a única do tipo citado ("o financiamento").
        val kindHint = when {
            has(L.financing) -> DebtKind.FINANCING
            has(L.loan) -> DebtKind.LOAN
            has(L.agreement) -> DebtKind.AGREEMENT
            else -> null
        }
        val guessedDebt = activeDebts.singleOrNull() ?: kindHint?.let { k -> activeDebts.singleOrNull { it.kind == k } }
        val payDebt: Debt? = when {
            borrowVerb -> null
            debtMatch != null && (payVerb || installmentWord) && (debtNoun || !keywordOutside(debtMatch.ranges)) ->
                activeDebts.first { it.id == debtMatch.id }
            // Só assume uma dívida não citada se nada na frase indicar outra categoria.
            debtMatch == null && (payVerb || installmentWord) && debtNoun && !keywordOutside(emptyList()) -> guessedDebt
            else -> null
        }

        val type = when {
            goalMatches.isNotEmpty() && (goalInVerb || goalOutVerb || goalWord) ->
                if (goalOutVerb) TxType.GOAL_OUT else TxType.GOAL_IN
            goalWord && (goalInVerb || goalOutVerb) && ctx.goals.any { !it.archived } ->
                if (goalOutVerb) TxType.GOAL_OUT else TxType.GOAL_IN
            invMatches.isNotEmpty() && (investVerb || redeemVerb) -> if (redeemVerb) TxType.INVEST_OUT else TxType.INVEST_IN
            investVerb -> TxType.INVEST_IN
            has(L.invoice) && (payVerb || cardMatches.isNotEmpty()) -> TxType.CARD_PAYMENT
            (transferVerb || goalInVerb) && accMatches.isNotEmpty() -> TxType.TRANSFER
            goalInVerb && ctx.goals.any { !it.archived } -> TxType.GOAL_IN
            borrowVerb -> TxType.DEBT_IN
            payDebt != null -> TxType.EXPENSE
            incomeVerb -> TxType.INCOME
            expenseVerb -> TxType.EXPENSE
            incomeNoun -> TxType.INCOME
            else -> TxType.EXPENSE
        }

        // Forma de pagamento citada na frase. O débito vem antes: "debit card" e "cartão de débito" também têm "cartão".
        val wantsDebit = consumeMethod(L.debit)
        val wantsCredit = consumeMethod(L.credit)
        val wantsCash = consumeMethod(L.cash)
        val wantsBenefit = consumeMethod(L.benefit)

        var accountId: Long? = null
        var toAccountId: Long? = null
        var cardId: Long? = null
        var investmentId: Long? = null
        var goalId: Long? = null
        var debtId: Long? = null

        fun methodAccount(): Long? = when {
            wantsCash -> accountOfType(AccountType.CASH)
            wantsBenefit -> accountOfType(AccountType.BENEFIT)
            else -> null
        } ?: defaultAccount()

        when (type) {
            TxType.GOAL_IN, TxType.GOAL_OUT -> {
                val g = goalMatches.firstOrNull()
                g?.let { consume(it) }
                goalId = g?.id ?: ctx.goals.singleOrNull { !it.archived }?.id
                val acc = accMatches.firstOrNull { a -> g == null || a.ranges.none { it in g.ranges } }
                acc?.let { consume(it) }
                accountId = acc?.id ?: methodAccount()
            }
            TxType.INVEST_IN, TxType.INVEST_OUT -> {
                val i = invMatches.firstOrNull()
                i?.let { consume(it) }
                investmentId = i?.id ?: ctx.investments.singleOrNull { !it.archived }?.id
                val acc = accMatches.firstOrNull { a -> i == null || a.ranges.none { it in i.ranges } }
                acc?.let { consume(it) }
                accountId = acc?.id ?: methodAccount()
            }
            TxType.CARD_PAYMENT -> {
                val c = cardMatches.firstOrNull()
                c?.let { consume(it) }
                cardId = c?.id ?: defaultCard()
                val acc = accMatches.firstOrNull { a -> c == null || a.ranges.none { it in c.ranges } }
                acc?.let { consume(it) }
                accountId = acc?.id ?: ctx.cards.firstOrNull { it.id == cardId }?.payAccountId ?: methodAccount()
            }
            TxType.TRANSFER -> {
                val ordered = accMatches.sortedBy { it.pos }.distinctBy { it.id }
                when (ordered.size) {
                    0 -> {}
                    1 -> {
                        val a = ordered[0]
                        if (isFrom(a) && !isTo(a)) accountId = a.id else toAccountId = a.id
                    }
                    else -> {
                        val a = ordered[0]; val b = ordered[1]
                        if ((isTo(a) && !isTo(b)) || (L.postpositional && isFrom(b) && !isFrom(a))) {
                            toAccountId = a.id; accountId = b.id
                        } else {
                            accountId = a.id; toAccountId = b.id
                        }
                    }
                }
                ordered.take(2).forEach { consume(it) }
                if (accountId == null) accountId = defaultAccount()?.takeIf { it != toAccountId }
                    ?: ctx.accounts.firstOrNull { !it.archived && it.id != toAccountId }?.id
            }
            TxType.DEBT_IN -> {
                debtMatch?.let { consume(it) }
                debtId = debtMatch?.id ?: guessedDebt?.id
                val acc = accMatches.firstOrNull { a -> debtMatch == null || a.ranges.none { it in debtMatch.ranges } }
                acc?.let { consume(it) }
                accountId = acc?.id ?: methodAccount()
            }
            TxType.EXPENSE -> if (payDebt != null) {
                // Pagamento de dívida: sempre sai de uma conta.
                debtMatch?.let { consume(it) }
                debtId = payDebt.id
                val acc = accMatches.firstOrNull { a -> debtMatch == null || a.ranges.none { it in debtMatch.ranges } }
                acc?.let { consume(it) }
                accountId = acc?.id ?: payDebt.accountId?.takeIf { id -> ctx.accounts.any { it.id == id && !it.archived } }
                    ?: methodAccount()
            } else {
                val c = cardMatches.firstOrNull()
                val a = accMatches.firstOrNull()
                val sameWords = c != null && a != null && c.ranges.any { it in a.ranges }
                val useCard = when {
                    c != null && a != null && sameWords -> when {
                        wantsCredit || installments > 1 -> true
                        wantsDebit -> false
                        else -> c.score > a.score // o nome mais completo vence ("inter black" = cartão)
                    }
                    c != null -> !wantsDebit
                    a != null -> false
                    else -> (wantsCredit && !wantsDebit) || installments > 1
                }
                if (useCard) {
                    cardId = c?.id ?: defaultCard()
                    c?.let { consume(it) }
                    if (sameWords) a?.let { consume(it) }
                }
                if (cardId == null) {
                    a?.let { consume(it) }
                    accountId = a?.id ?: methodAccount()
                }
            }
            TxType.INCOME -> {
                val a = accMatches.firstOrNull()
                a?.let { consume(it) }
                accountId = a?.id ?: methodAccount()
            }
        }

        // Categoria e descrição (com o que sobrou da frase)
        val descTokens = toks.filter { !it.used && !isStop(it.norm) && it.norm.any(Char::isLetterOrDigit) }
        val descNorm = descTokens.joinToString(joiner) { it.norm }
        val isDebtPayment = payDebt != null && type == TxType.EXPENSE
        val categoryId = when {
            isDebtPayment -> ctx.categories.debtCategory()?.id
            type == TxType.EXPENSE || type == TxType.INCOME ->
                guessCategory(if (type == TxType.INCOME) CategoryKind.INCOME else CategoryKind.EXPENSE, descTokens, descNorm)
            else -> null
        }

        var description = descTokens.joinToString(joiner) { cleanOrig(it.orig) }.trim()
        val debt = activeDebts.firstOrNull { it.id == debtId }
        if (debt != null) {
            description = when {
                type == TxType.DEBT_IN -> Res.s(R.string.phrase_emprestimo, debt.name)
                debt.installmentAmount > 0 -> Res.s(R.string.phrase_parcela, debt.name)
                else -> Res.s(R.string.phrase_pagamento, debt.name)
            }
        }
        if (description.isEmpty()) {
            description = when (type) {
                TxType.EXPENSE, TxType.INCOME -> ctx.categories.firstOrNull { it.id == categoryId }?.name ?: ""
                TxType.TRANSFER -> Res.s(R.string.common_transferencia)
                TxType.CARD_PAYMENT -> Res.s(R.string.common_pagamento_fatura)
                TxType.INVEST_IN -> Res.s(R.string.wallet_aporte)
                TxType.INVEST_OUT -> Res.s(R.string.wallet_resgate)
                TxType.GOAL_IN -> Res.s(R.string.phrase_dinheiro_guardado)
                TxType.GOAL_OUT -> Res.s(R.string.phrase_dinheiro_retirado)
                TxType.DEBT_IN -> Res.s(R.string.common_emprestimo_recebido)
            }
        }

        description = description.split(' ').joinToString(" ") { w -> if (w.normalized() in ACRONYMS) w.uppercase(Locale.ROOT) else w }

        return ParsedEntry(
            type = type,
            amount = amount ?: if (isDebtPayment) payDebt?.installmentAmount?.takeIf { it > 0 } else null,
            description = description.capFirst(),
            categoryId = categoryId,
            accountId = accountId,
            toAccountId = toAccountId,
            cardId = cardId,
            installments = if (cardId != null) installments else 1,
            date = date,
            investmentId = investmentId,
            goalId = goalId,
            debtId = debtId,
        )
    }

    /* ------------------------------ palavras ---------------------------- */

    /**
     * Expressão regular com "\b" valendo para todas as escritas: no Android (ICU) e no Java o "\b" padrão trata letras
     * fora do latim de jeitos diferentes, e as vogais do hindi (sinais combinantes) partiriam a palavra.
     * Em coreano, japonês e chinês o fim da palavra não é exigido (as terminações vêm grudadas).
     */
    private fun re(p: String): Regex = Regex(p.replace("\\b", if (loose) "" else B))

    /** Alternativas de uma lista, as mais longas primeiro (para "debit card" ganhar de "debit"). */
    private fun alt(list: Collection<String>): String =
        list.map { L.n(it) }.filter { it.isNotEmpty() }.distinct().sortedByDescending { it.length }.joinToString("|")

    private fun words(list: Collection<String>): String = "\\b(?:${alt(list)})\\b"

    private fun has(list: List<String>): Boolean = list.isNotEmpty() && re(words(list)).containsMatchIn(text)

    private fun isStop(norm: String): Boolean {
        if (norm in L.stopN || norm in STOP_SYMBOLS) return true
        // Japonês, chinês e coreano: o verbo vem conjugado e grudado ("買いました", "샀어요").
        return loose && L.verbs.any { norm.contains(it) }
    }

    /* ------------------------------ tokens ------------------------------ */

    private fun tokenize(input: String): List<Token> {
        val raw = when (L.writing) {
            Writing.SPACED -> spacedChunks(input)
            Writing.AGGLUTINATIVE -> spacedChunks(input).flatMap { (o, n) -> splitDigits(o, n) }.map { particle(it) }
            Writing.UNSPACED -> unspacedChunks(input)
        }
        val out = ArrayList<Token>()
        var pos = 0
        for (r in raw) {
            val n = r.second
            if (n.isEmpty()) continue
            out += Token(r.first, n, pos, pos + n.length, r.third)
            pos += n.length + 1
        }
        return out
    }

    private fun spacedChunks(input: String): List<Triple<String, String, String>> =
        input.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.map { r ->
            var orig = r
            var n = normToken(r)
            L.elision?.let { e ->
                e.find(n)?.let { m -> n = n.substring(m.range.last + 1) }
                e.find(orig.lowercase().replace('’', '\''))?.let { m -> orig = orig.substring(m.range.last + 1) }
            }
            Triple(orig, n, "")
        }

    /** Coreano: "5000원" → "5000" + "원", "3만5천원" → "3만5천" + "원". */
    private fun splitDigits(orig: String, norm: String): List<Triple<String, String, String>> {
        if (norm.none { it.isDigit() } || norm.all { it.isDigit() || it in ".,/-" }) return listOf(Triple(orig, norm, ""))
        val o = if (orig.length == norm.length) orig else norm
        return runs(norm).map { r -> Triple(o.substring(r.first, r.last + 1), norm.substring(r.first, r.last + 1), "") }
    }

    /** Partícula do fim da palavra, separada (para a descrição e para "de onde"/"para onde"). */
    private fun particle(t: Triple<String, String, String>): Triple<String, String, String> {
        val (orig, norm, _) = t
        if (norm.any { it.isDigit() } || orig.length != norm.length) return t
        for (p in L.particlesN) {
            if (norm.length > p.length && norm.endsWith(p) && norm.length - p.length >= (if (p.length == 1) 2 else 1)) {
                return Triple(orig.dropLast(p.length), norm.dropLast(p.length), p)
            }
        }
        return t
    }

    /** Pedaços de números: dígitos (com . , / -) e as unidades grudadas depois deles (万, 千, 만, 천). */
    private fun runs(s: String): List<IntRange> {
        val out = ArrayList<IntRange>()
        var i = 0
        while (i < s.length) {
            val start = i
            if (s[i].isDigit()) {
                while (i < s.length && (s[i].isDigit() || (s[i] in ".,/-" && i + 1 < s.length && s[i + 1].isDigit()) ||
                        (s[i] in CJK_UNITS && i > start))
                ) i++
            } else {
                while (i < s.length && !s[i].isDigit()) i++
            }
            out += start until i
        }
        return out
    }

    /**
     * Japonês e chinês: sem espaços, a frase é separada pelas palavras que o app conhece (as da língua, os nomes das
     * contas, cartões, metas e categorias e as palavras-chave), pelos números e pela troca de escrita (katakana, latim).
     */
    private fun unspacedChunks(input: String): List<Triple<String, String, String>> {
        val dict = HashSet<String>(L.vocabulary)
        fun add(s: String) { s.normalized().trim().takeIf { it.isNotEmpty() && it.length <= 24 }?.let { dict += it } }
        ctx.accounts.forEach { add(it.name) }
        ctx.cards.forEach { add(it.name) }
        ctx.goals.forEach { add(it.name) }
        ctx.investments.forEach { add(it.name) }
        ctx.debts.forEach { add(it.name); add(it.creditor) }
        ctx.categories.forEach { c -> add(c.name); c.keywords.split(',').forEach { add(it) } }
        ctx.learned.keys.forEach { add(it) }
        val maxLen = dict.maxOfOrNull { it.length } ?: 1

        val text = Normalizer.normalize(input, Normalizer.Form.NFKC).replace(Regex("[。、，！？「」（）・]"), " ")
        val out = ArrayList<Triple<String, String, String>>()
        for (chunk in text.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }) {
            val low = String(CharArray(chunk.length) { Character.toLowerCase(chunk[it]) })
            var pending = -1
            fun flush(end: Int) {
                if (pending >= 0 && end > pending) {
                    val o = chunk.substring(pending, end)
                    out += Triple(o, normToken(o), "")
                }
                pending = -1
            }
            var i = 0
            while (i < low.length) {
                val c = low[i]
                if (c.isDigit()) {
                    flush(i)
                    val r = runs(low.substring(i)).first()
                    val o = chunk.substring(i, i + r.last + 1)
                    out += Triple(o, normToken(o), "")
                    i += r.last + 1
                    continue
                }
                var len = 0
                for (n in minOf(maxLen, low.length - i) downTo 1) {
                    val w = low.substring(i, i + n)
                    if (w.normalized() !in dict) continue
                    // Partícula de uma letra só separa depois de kanji/katakana ("スタバで"), não no meio de "おにぎり".
                    if (n == 1 && !singleOk(low, i)) continue
                    len = n; break
                }
                if (len > 0) {
                    flush(i)
                    val o = chunk.substring(i, i + len)
                    out += Triple(o, normToken(o), "")
                    i += len
                    continue
                }
                if (pending >= 0 && script(low[i - 1]) != script(c)) flush(i)
                if (pending < 0) pending = i
                i++
            }
            flush(low.length)
        }
        return out
    }

    private fun singleOk(s: String, i: Int): Boolean = when (L.language) {
        AppLanguage.JA -> i > 0 && !isHiragana(s[i - 1])
        AppLanguage.ZH -> s[i] in ZH_SINGLE
        else -> true
    }

    private fun isHiragana(c: Char) = c in 'ぁ'..'ゟ'

    /** Escrita de cada letra, para separar "スタバ" de "コーヒー" mesmo sem as conhecer. */
    private fun script(c: Char): Int = when {
        c == 'ー' || c in '゠'..'ヿ' -> 1
        c in '぀'..'ゟ' || Character.UnicodeScript.of(c.code) == Character.UnicodeScript.HAN -> 2
        Character.UnicodeScript.of(c.code) == Character.UnicodeScript.HANGUL -> 3
        c.isLetter() -> 4
        else -> 5
    }

    private fun normToken(r: String): String {
        val digits = r.map { c ->
            when {
                c.isDigit() && c !in '0'..'9' -> ('0' + Character.digit(c, 10))
                c == '’' || c == '‘' -> '\''
                c == '，' -> ','
                c == '．' -> '.'
                else -> c
            }
        }.joinToString("")
        var t = digits.normalized().trim('(', ')', '[', ']', '"', '\'', '!', '?', ';', ':', '“', '”', '-')
        while (t.isNotEmpty() && (t.last() == '.' || t.last() == ',')) t = t.dropLast(1)
        return t
    }

    private fun cleanOrig(s: String) =
        s.trim('(', ')', '[', ']', '"', '\'', '!', '?', ';', ':', '“', '”', ',', '.', '-')

    private fun overlapping(r: IntRange) = toks.filter { it.start <= r.last && it.end > r.first }
    private fun free(r: IntRange) = overlapping(r).none { it.used }
    private fun consume(r: IntRange) = overlapping(r).forEach { it.used = true }
    private fun consume(m: EMatch) = m.ranges.forEach { consume(it) }

    private fun consumeFirst(rx: String): Boolean {
        val m = re(rx).findAll(text).firstOrNull { free(it.range) } ?: return false
        consume(m.range)
        return true
    }

    /** Forma de pagamento, com a preposição ou posposição junto ("no débito", "カードで", "कार्ड से"). */
    private fun consumeMethod(list: List<String>): Boolean {
        if (list.isEmpty()) return false
        val pre = if (L.methodPrep.isNotEmpty()) "(?:(?:${L.methodPrep}) )?" else ""
        val post = if (L.methodPost.isNotEmpty()) "(?: ?(?:${L.methodPost}))?" else ""
        return consumeFirst("\\b$pre(?:${alt(list)})$post\\b")
    }

    private fun precededBy(pos: Int, words: Set<String>): Boolean {
        val idx = toks.indexOfFirst { it.start == pos }
        if (idx <= 0) return false
        return toks.subList(max(0, idx - 2), idx).any { it.norm in words }
    }

    /** A palavra logo depois do nome ("楽天 から", "SBI से") ou a partícula grudada nele ("국민은행에서"). */
    private fun followedBy(m: EMatch, words: Set<String>): Boolean {
        val end = m.ranges.maxOf { it.last }
        val idx = toks.indexOfFirst { it.start <= end && it.end > end }
        if (idx < 0) return false
        val t = toks[idx]
        if (t.suffix.isNotEmpty()) return t.suffix in words
        val rest = t.norm.substring((end - t.start + 1).coerceAtMost(t.norm.length))
        if (rest.isNotEmpty()) return rest in words
        return toks.getOrNull(idx + 1)?.norm?.let { it in words } == true
    }

    private fun isTo(m: EMatch) = if (L.postpositional) followedBy(m, L.toN) else precededBy(m.pos, L.toN)
    private fun isFrom(m: EMatch) = if (L.postpositional) followedBy(m, L.fromN) else precededBy(m.pos, L.fromN)

    /* ------------------------------ datas ------------------------------ */

    /**
     * Modelo com partes ({D}, {M}, {NUM}...) virando uma expressão regular com grupos numerados. (Grupos com nome
     * dependem de uma função que o Android só tem a partir da versão 13.) Cada parte que começa com "(" é um grupo.
     */
    private class Tpl(val rx: Regex, private val index: Map<String, Int>) {
        fun get(m: MatchResult, key: String): String? = index[key]?.let { m.groups[it]?.value }
    }

    private fun tpl(template: String, parts: Map<String, String>): Tpl {
        val sb = StringBuilder()
        val index = HashMap<String, Int>()
        var groups = 0
        var last = 0
        for (m in PLACEHOLDER.findAll(template)) {
            val before = template.substring(last, m.range.first)
            sb.append(before); groups += countGroups(before)
            val key = m.groupValues[1]
            val part = parts[key] ?: error("parte {$key} sem valor")
            if (part.startsWith("(") && !part.startsWith("(?")) index[key] = groups + 1
            sb.append(part); groups += countGroups(part)
            last = m.range.last + 1
        }
        val tail = template.substring(last)
        sb.append(tail)
        return Tpl(re(sb.toString()), index)
    }

    private fun countGroups(s: String): Int {
        var n = 0
        var i = 0
        while (i < s.length) {
            when {
                s[i] == '\\' -> i++
                s[i] == '(' && (i + 1 >= s.length || s[i + 1] != '?') -> n++
            }
            i++
        }
        return n
    }

    private fun parseDate(): LocalDate {
        val today = ctx.today
        if (L.relDaysN.isNotEmpty()) {
            re(words(L.relDaysN.keys)).find(text)?.let { m ->
                consume(m.range)
                return today.plusDays((L.relDaysN[m.value] ?: 0).toLong())
            }
        }
        for (p in L.daysAgo) {
            re("\\b$p\\b").find(text)?.let { m ->
                val n = m.groupValues[1].toInt()
                if (n in 0..366) {
                    consume(m.range)
                    return today.minusDays(n.toLong())
                }
            }
        }
        // 2026-11-03 (ou 2026/11/03): igual em toda língua.
        re("\\b(\\d{4})[-/.](\\d{1,2})[-/.](\\d{1,2})\\b").find(text)?.let { m ->
            val d = runCatching { LocalDate.of(m.groupValues[1].toInt(), m.groupValues[2].toInt(), m.groupValues[3].toInt()) }.getOrNull()
            if (d != null) {
                consume(m.range)
                return d
            }
        }
        val monthFirst = L.monthFirst(ctx.locale)
        re("\\b${L.datePrefix}(\\d{1,2})/(\\d{1,2})(?:/(\\d{2,4}))?\\b").find(text)?.let { m ->
            var a = m.groupValues[1].toInt()
            var b = m.groupValues[2].toInt()
            if (monthFirst) { val x = a; a = b; b = x }
            // "25/12" num app em inglês dos EUA: só pode ser dia/mês.
            if (b > 12 && a <= 12) { val x = a; a = b; b = x }
            dated(a, b, m.groupValues[3])?.let { consume(m.range); return it }
        }
        if (!monthFirst) {
            // 03.11.2026 (alemão e outros): com o ano, para não confundir com valor.
            re("\\b(\\d{1,2})\\.(\\d{1,2})\\.(\\d{4})\\b").find(text)?.let { m ->
                dated(m.groupValues[1].toInt(), m.groupValues[2].toInt(), m.groupValues[3])?.let { consume(m.range); return it }
            }
        }
        val monthAlt = alt(L.monthIndex.keys)
        for (pattern in L.monthPatterns) {
            if (pattern.contains("{M}") && monthAlt.isEmpty()) continue
            val t = tpl(
                "\\b$pattern\\b",
                mapOf("P" to L.datePrefix, "D" to "(\\d{1,2})", "MN" to "(\\d{1,2})", "M" to "($monthAlt)", "Y" to "(\\d{4})"),
            )
            for (m in t.rx.findAll(text)) {
                if (!free(m.range)) continue
                val day = t.get(m, "D")?.toIntOrNull() ?: continue
                val month = t.get(m, "MN")?.toIntOrNull() ?: t.get(m, "M")?.let { L.monthIndex[it] } ?: continue
                val year = t.get(m, "Y")
                var d = runCatching { LocalDate.of(year?.toInt() ?: today.year, month, day) }.getOrNull() ?: continue
                consume(m.range)
                if (year == null && d.isAfter(today.plusDays(31))) d = d.minusYears(1)
                return d
            }
        }
        for (p in L.dayOnly) {
            re("\\b$p\\b").find(text)?.let { m ->
                val day = m.groupValues[1].toInt()
                if (day in 1..31) {
                    consume(m.range)
                    val ym = if (day <= today.dayOfMonth) today.ym() else today.ym().minusMonths(1)
                    return ym.dayClamped(day)
                }
            }
        }
        if (L.weekdayIndex.isNotEmpty()) {
            val last = "(?:${alt(L.lastWords)})"
            val t = tpl("\\b${L.weekdayPattern}\\b", mapOf("W" to "(${alt(L.weekdayIndex.keys)})", "LB" to "($last)", "LA" to "($last)"))
            t.rx.find(text)?.let { m ->
                val target = L.weekdayIndex[t.get(m, "W")] ?: return@let
                consume(m.range)
                var back = (today.dayOfWeek.value - target.value + 7) % 7
                if (back == 0 && (t.get(m, "LB") != null || t.get(m, "LA") != null)) back = 7
                return today.minusDays(back.toLong())
            }
        }
        return today
    }

    /** Dia e mês (e o ano, se veio). Sem ano, uma data mais de 31 dias no futuro é do ano passado. */
    private fun dated(day: Int, month: Int, yearTxt: String): LocalDate? {
        val today = ctx.today
        var year = when {
            yearTxt.isEmpty() -> today.year
            yearTxt.length == 2 -> 2000 + yearTxt.toInt()
            else -> yearTxt.toInt()
        }
        if (runCatching { LocalDate.of(year, month, day) }.getOrNull() == null) return null
        if (yearTxt.isEmpty() && LocalDate.of(year, month, day).isAfter(today.plusDays(31))) year -= 1
        return LocalDate.of(year, month, day.coerceAtMost(YearMonth.of(year, month).lengthOfMonth()))
    }

    /* ----------------------------- parcelas ---------------------------- */

    private fun parseInstallments(): Int {
        if (L.upfront.isNotEmpty()) consumeFirst(words(L.upfront))
        for (p in L.installments + UNIVERSAL_INSTALLMENTS) {
            val m = re("\\b$p\\b").findAll(text).firstOrNull { free(it.range) } ?: continue
            val n = m.groupValues[1].toInt()
            if (n in 1..48) {
                consume(m.range)
                return n
            }
        }
        return 1
    }

    /* ------------------------------- valor ----------------------------- */

    private fun parseAmount(): Long? {
        val cands = ArrayList<Cand>()
        val currency = buildString {
            append("(?:")
            if (L.currencyWords.isNotEmpty()) append("(?:${alt(L.currencyWords)})\\b|")
            append(SUFFIX_SYMBOLS)
            append(")")
        }
        val parts = mapOf(
            "NUM" to NUM,
            "K" to if (L.multipliersN.isEmpty()) "(?!)" else "(${alt(L.multipliersN.keys)})",
            "C" to "(\\d{1,2})",
            "CUR" to currency,
            "CJK" to "((?:\\d+(?:\\.\\d+)? ?[$CJK_UNITS] ?)+\\d*)",
            "CURG" to "($currency)",
            "U" to "([万萬만千천])",
        )
        fun scan(template: String, score: (Tpl, MatchResult) -> Int, value: (Tpl, MatchResult) -> Long?) {
            val t = tpl(template, parts)
            t.rx.findAll(text).forEach { m ->
                if (free(m.range)) value(t, m)?.let { cands += Cand(it, score(t, m), m.range.first, m.range) }
            }
        }
        fun value(t: Tpl, m: MatchResult): Long? =
            t.get(m, "NUM")?.let { toCents(it, t.get(m, "K")?.let { k -> L.multipliersN[k] } ?: 1L) }
        val mult = "(?: ?{K}\\b)?"

        // "50 reais e 50 centavos", "12 dollars and 50 cents"
        for (c in L.cents) {
            scan("\\b$c\\b", { _, _ -> 6 }) { t, m ->
                t.get(m, "NUM")?.let { toCents(it, 1) }?.plus(t.get(m, "C")!!.toLong())
            }
        }
        // "R$ 32,50", "$25", "€ 2 mil", "₹500"
        scan("(?:\\b(?:$ALPHA_SYMBOLS) ?|(?:$PREFIX_SYMBOLS) ?){NUM}$mult", { _, _ -> 5 }, ::value)
        // "120 reais", "20 euros", "500 円", "20€"
        scan("(?<![\\d.,$CJK_UNITS])\\b{NUM}(?: ?{K})? ?{CUR}", { _, _ -> 5 }, ::value)
        // "gastei 50", "paid 50", "für 20"
        if (L.amountVerbs.isNotEmpty()) {
            val link = if (L.amountLink.isNotEmpty()) "(?: (?:${L.amountLink}))?" else ""
            scan("\\b(?:${alt(L.amountVerbs)})$link ?{NUM}$mult(?![\\p{L}\\d$CJK_UNITS])", { _, _ -> 4 }, ::value)
        }
        // Japonês, chinês e coreano: "1万5千円", "3만 원", "2千"
        if (L.writing != Writing.SPACED) {
            scan("(?<![\\d.]){CJK}(?: ?{CURG})?", { t, m -> if (t.get(m, "CURG") != null) 5 else 3 }) { t, m -> cjkCents(t.get(m, "CJK")!!) }
            scan("(?<![\\d]){U} ?{CUR}", { _, _ -> 4 }) { t, m -> cjkCents("1" + t.get(m, "U")) }
        }
        // número solto
        scan("(?<![\\d/$CJK_UNITS])\\b{NUM}$mult(?![\\p{L}\\d/])", { t, m ->
            val n = t.get(m, "NUM").orEmpty()
            if (n.contains(',') || Regex("\\.\\d{1,2}$").containsMatchIn(n)) 2 else 1
        }, ::value)
        // "mil reais" sem número
        if (L.bareThousand.isNotEmpty()) scan(words(L.bareThousand), { _, _ -> 0 }) { _, _ -> 100_000L }

        val best = cands.filter { it.cents in 1..99_999_999_999L }
            .maxWithOrNull(compareBy<Cand>({ it.score }, { it.pos })) ?: return null
        consume(best.range)
        return best.cents
    }

    /* ---------------------------- entidades ---------------------------- */

    private fun kw(k: String) = re("\\b${Regex.escape(k)}${L.plural}\\b")

    /** Alguma palavra-chave de categoria (fora da dívida) aparece na frase? Ex.: "gasolina da moto". */
    private fun keywordOutside(ranges: List<IntRange>): Boolean =
        ctx.categories.filter { it.kind == CategoryKind.EXPENSE && !it.archived && it.icon != DefaultData.DEBT_ICON }.any { c ->
            c.keywords.split(',').map { it.trim().normalized() }.filter { it.isNotEmpty() }.any { k ->
                kw(k).findAll(text).any { m ->
                    ranges.none { r -> r.first <= m.range.last && m.range.first <= r.last }
                }
            }
        }

    private fun matchNames(items: List<Pair<Long, String>>, extraGeneric: Set<String> = emptySet()): List<EMatch> = items.mapNotNull { (id, name) ->
        val norm = name.normalized().trim()
        var words = norm.split(NON_WORD)
            .filter { it.length >= 2 && it !in L.genericNameN && it !in extraGeneric && !it.all(Char::isDigit) }
        if (words.isEmpty()) words = listOf(norm.trim()).filter { it.isNotEmpty() }
        val ranges = words.mapNotNull { w ->
            re("\\b${Regex.escape(w)}\\b").findAll(text).firstOrNull { free(it.range) }?.range
                ?: if (L.writing == Writing.UNSPACED) prefixOf(w) else null
        }
        if (ranges.isEmpty()) null else EMatch(id, ranges.size, ranges.minOf { it.first }, ranges)
    }.sortedWith(compareByDescending<EMatch> { it.score }.thenBy { it.pos })

    /** Sem espaços, a pessoa costuma falar só o começo do nome ("楽天" para "楽天銀行"). */
    private fun prefixOf(w: String): IntRange? {
        for (n in w.length - 1 downTo 2) {
            val m = Regex(Regex.escape(w.substring(0, n))).findAll(text).firstOrNull { free(it.range) } ?: continue
            return m.range
        }
        return null
    }

    private fun defaultAccount(): Long? {
        val active = ctx.accounts.filter { !it.archived }
        return ctx.defaultAccountId?.takeIf { id -> active.any { it.id == id } }
            ?: active.firstOrNull { it.type == AccountType.CHECKING }?.id
            ?: active.firstOrNull()?.id
    }

    private fun defaultCard(): Long? = ctx.cards.firstOrNull { !it.archived }?.id
    private fun accountOfType(t: AccountType): Long? = ctx.accounts.firstOrNull { !it.archived && it.type == t }?.id

    /* ----------------------------- categoria --------------------------- */

    private fun guessCategory(kind: CategoryKind, descTokens: List<Token>, descNorm: String): Long? {
        val cats = ctx.categories.filter { it.kind == kind && !it.archived }
        if (cats.isEmpty()) return null
        var bestId: Long? = null
        var best = -1.0
        fun offer(id: Long, score: Double) {
            if (score > best && cats.any { it.id == id }) { best = score; bestId = id }
        }
        // 1. Mesma descrição usada antes (o app aprendeu)
        if (descNorm.isNotEmpty()) ctx.learned[descNorm]?.let { offer(it, 10.0) }
        // 2. Nome da categoria escrito na frase
        for (c in cats) {
            val n = c.name.normalized()
            if (n.isEmpty()) continue
            val m = re("\\b${Regex.escape(n)}\\b").find(text)
            if (m != null && free(m.range)) offer(c.id, 8.0 + n.length / 100.0)
        }
        // 3. Palavra que você já usou antes com uma categoria
        descTokens.forEach { t -> if (t.norm.length >= minLearned(t.norm)) ctx.learned[t.norm]?.let { offer(it, 6.0) } }
        // 4. Palavras-chave da categoria (as compostas são mais específicas)
        for (c in cats) {
            for (k in c.keywords.split(',').map { it.trim().normalized() }.filter { it.isNotEmpty() }) {
                val m = kw(k).find(text) ?: continue
                if (!free(m.range)) continue
                offer(c.id, (if (k.contains(' ')) 9.0 else 4.0) + k.length / 100.0)
            }
        }
        return bestId ?: cats.firstOrNull { it.icon == "more" }?.id
    }

    companion object {
        /** Valor: "1.234,56", "1,234.56", "1,23,456" (Índia), "12,50", "12.50", "1200". */
        private const val NUM =
            "(\\d{1,3}(?:[.,]\\d{3})+(?:[.,]\\d{1,2})?|\\d{1,2}(?:,\\d{2})+,\\d{3}(?:\\.\\d{1,2})?|\\d+(?:[.,]\\d{1,2})?)"

        /** Fim de palavra que vale para todas as escritas (letras, sinais combinantes e números). */
        private const val W = "[\\p{L}\\p{M}\\p{N}_]"
        private const val B = "(?:(?<=$W)(?!$W)|(?<!$W)(?=$W))"

        private val NON_WORD = Regex("[^\\p{L}\\p{M}\\p{N}]+")
        private val PLACEHOLDER = Regex("(?<!\\\\p)\\{([A-Z]+)\\}") // não pega o "\\p{L}" das expressões

        private const val CJK_UNITS = "億亿万萬만千천百백"
        private val ZH_SINGLE = setOf('了', '的', '在', '给', '从', '我', '用', '和', '跟')

        /** Símbolo antes do valor: "R$ 10", "$10", "€10", "₹10", "Rs. 10". */
        private const val PREFIX_SYMBOLS = "r\\$|us\\$|u\\\$s|c\\$|a\\$|mx\\$|\\$|€|£|¥|￥|₩|₹"
        private const val ALPHA_SYMBOLS = "rs\\.?|chf|eur|usd|inr|brl|gbp"
        private const val SUFFIX_SYMBOLS = "€|\\$|£|¥|￥|₩|₹"
        private val STOP_SYMBOLS = setOf("r$", "$", "us$", "€", "£", "¥", "￥", "₩", "₹")

        /** "10x" em qualquer língua. */
        private val UNIVERSAL_INSTALLMENTS = listOf("(\\d{1,2}) ?x")

        private val ACRONYMS = setOf(
            "tv", "ipva", "iptu", "iof", "das", "darf", "inss", "cdb", "lci", "lca", "fgts", "irpf", "cpf", "cnh",
            "vr", "va", "pc", "dvd", "sus", "plr", "mei", "cdi", "fii", "fiis", "atm", "usd", "eur", "emi", "upi", "sip",
            "etf", "ira", "nisa", "kfc", "gps",
        )

        /** Palavras sem significado para a categoria, na língua do app (para o que o app aprende com o histórico). */
        val STOP_WORDS: Set<String> get() = PhraseLang.current.stopN

        /** Palavras de extrato que não dizem nada da categoria ("pix", "transfer", "virement"...), na língua do app. */
        val BANK_WORDS: Set<String> get() = PhraseLang.current.bankN

        /** Tamanho mínimo de uma palavra aprendida: 2 letras em japonês, chinês e coreano ("咖啡"), 3 nas outras. */
        fun minLearned(word: String): Int = if (word.any { Character.UnicodeScript.of(it.code) in CJK_SCRIPTS }) 2 else 3

        private val CJK_SCRIPTS = setOf(
            Character.UnicodeScript.HAN, Character.UnicodeScript.HIRAGANA, Character.UnicodeScript.KATAKANA,
            Character.UnicodeScript.HANGUL,
        )

        /**
         * Palavra inteira em qualquer escrita ("\\b" que vale igual no Android e no Java, também para o hindi); em
         * japonês, chinês e coreano, pedaço de palavra (não há espaços, ou as partículas vêm grudadas).
         */
        fun wordRegex(word: String, plural: String = ""): Regex =
            if (word.any { Character.UnicodeScript.of(it.code) in CJK_SCRIPTS }) Regex(Regex.escape(word))
            else Regex("$B${Regex.escape(word)}$plural$B")

        /** Separa uma descrição em palavras (qualquer escrita). */
        fun wordsOf(norm: String): List<String> = norm.split(NON_WORD).filter { it.isNotEmpty() }

        /**
         * Número em centavos. Ponto e vírgula valem como separador de milhar ou de centavos, conforme o que vem depois:
         * três dígitos = milhar ("1.200", "1,200"), um ou dois = centavos ("12,50", "12.50"); com os dois, o último manda.
         */
        internal fun toCents(num: String, factor: Long): Long? {
            val lastDot = num.lastIndexOf('.')
            val lastComma = num.lastIndexOf(',')
            val plain: String = when {
                lastDot >= 0 && lastComma >= 0 -> {
                    val dec = if (lastDot > lastComma) '.' else ','
                    val other = if (dec == '.') ',' else '.'
                    num.replace(other.toString(), "").replace(dec, '.')
                }
                lastDot >= 0 || lastComma >= 0 -> {
                    val sep = if (lastDot >= 0) '.' else ','
                    val tail = num.substringAfterLast(sep)
                    if (tail.length == 3) num.replace(sep.toString(), "")
                    else num.substringBeforeLast(sep).replace(sep.toString(), "") + "." + tail
                }
                else -> num
            }
            val v = plain.toBigDecimalOrNull() ?: return null
            return v.multiply(BigDecimal(factor)).movePointRight(2).setScale(0, RoundingMode.HALF_UP).toLong()
        }

        /** "1万5千" → 15.000, "3億2000万" → 320.000.000, "3만5천" → 35.000 (em centavos). */
        internal fun cjkCents(s: String): Long? {
            val units = mapOf(
                '億' to 100_000_000L, '亿' to 100_000_000L, '万' to 10_000L, '萬' to 10_000L, '만' to 10_000L,
                '千' to 1000L, '천' to 1000L, '百' to 100L, '백' to 100L,
            )
            val compact = s.replace(" ", "")
            var total = BigDecimal.ZERO
            var last = 0
            for (m in Regex("(\\d+(?:\\.\\d+)?)([$CJK_UNITS])").findAll(compact)) {
                total += m.groupValues[1].toBigDecimal().multiply(BigDecimal(units.getValue(m.groupValues[2][0])))
                last = m.range.last + 1
            }
            if (last == 0) return null
            compact.substring(last).toBigDecimalOrNull()?.let { total += it }
            return total.movePointRight(2).setScale(0, RoundingMode.HALF_UP).toLong()
        }
    }
}
