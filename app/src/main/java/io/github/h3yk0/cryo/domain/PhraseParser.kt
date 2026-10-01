// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.domain

import io.github.h3yk0.cryo.data.db.Account
import io.github.h3yk0.cryo.data.db.AccountType
import io.github.h3yk0.cryo.data.db.Category
import io.github.h3yk0.cryo.data.db.CategoryKind
import io.github.h3yk0.cryo.data.db.CreditCard
import io.github.h3yk0.cryo.data.db.Goal
import io.github.h3yk0.cryo.data.db.Investment
import io.github.h3yk0.cryo.data.db.TxType
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.DayOfWeek
import java.time.LocalDate
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
)

/**
 * Entende frases do dia a dia, como "mercado 120 no débito", "recebi 3000 de salário",
 * "tv 1200 em 10x no cartão" ou "transferi 200 para a poupança".
 *
 * Funciona 100% no aparelho, sem internet: são regras + o que o app aprendeu com o seu histórico.
 */
class PhraseParser(private val ctx: ParseContext) {

    private class Token(val orig: String, val norm: String, val start: Int, val end: Int) {
        var used = false
    }

    private data class Cand(val cents: Long, val score: Int, val pos: Int, val range: IntRange)
    private data class EMatch(val id: Long, val score: Int, val pos: Int, val ranges: List<IntRange>)

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

        val goalWord = has("\\b(caixinha|cofrinho|meta)\\b")
        val goalOutVerb = has("\\b(tirei|retirei|resgatei|saquei|usei|peguei)\\b")
        val goalInVerb = has("\\b(guardei|separei|reservei|poupei|coloquei|depositei)\\b")
        val investVerb = has("\\b(investi|apliquei|aportei|aporte|aplicacao|aplicar|investir)\\b")
        val redeemVerb = has("\\b(resgatei|resgate|saquei|retirei)\\b")
        val payVerb = has("\\b(paguei|pagar|pagamento|quitei|pago)\\b")
        val transferVerb = has("\\b(transferi|transferencia|transferir|passei|movi|mandei)\\b")
        val incomeVerb = has("\\b(recebi|ganhei|entrou|caiu|vendi|receber|recebido|recebimento)\\b")
        val expenseVerb = has("\\b(gastei|paguei|comprei|gasto|pago|gastar|comprar)\\b")
        val incomeNoun = has(
            "\\b(salario|freela|freelance|bico|rendimento|rendimentos|reembolso|estorno|dividendos|cashback|" +
                "bonus|comissao|mesada|13o|decimo terceiro|plr|adiantamento|venda|vendas|deposito)\\b",
        )

        val type = when {
            goalMatches.isNotEmpty() && (goalInVerb || goalOutVerb || goalWord) ->
                if (goalOutVerb) TxType.GOAL_OUT else TxType.GOAL_IN
            goalWord && (goalInVerb || goalOutVerb) && ctx.goals.any { !it.archived } ->
                if (goalOutVerb) TxType.GOAL_OUT else TxType.GOAL_IN
            invMatches.isNotEmpty() && (investVerb || redeemVerb) -> if (redeemVerb) TxType.INVEST_OUT else TxType.INVEST_IN
            investVerb -> TxType.INVEST_IN
            has("\\bfatura\\b") && (payVerb || cardMatches.isNotEmpty()) -> TxType.CARD_PAYMENT
            (transferVerb || goalInVerb) && accMatches.isNotEmpty() -> TxType.TRANSFER
            goalInVerb && ctx.goals.any { !it.archived } -> TxType.GOAL_IN
            incomeVerb -> TxType.INCOME
            expenseVerb -> TxType.EXPENSE
            incomeNoun -> TxType.INCOME
            else -> TxType.EXPENSE
        }

        // Forma de pagamento citada na frase
        val wantsCredit = consumeFirst("\\b(?:n[oa] )?(credito|cartao)\\b")
        val wantsDebit = consumeFirst("\\b(?:n[oa] )?(debito|pix)\\b|\\bna conta\\b")
        val wantsCash = consumeFirst("\\b(?:em )?(dinheiro|especie|cash)\\b|\\bem maos\\b")
        val wantsBenefit = consumeFirst(
            "\\b(?:n[oa] )?(vale refeicao|vale alimentacao|vr|va|ticket|alelo|sodexo|pluxee|caju|vale)\\b",
        )

        var accountId: Long? = null
        var toAccountId: Long? = null
        var cardId: Long? = null
        var investmentId: Long? = null
        var goalId: Long? = null

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
                val toWords = setOf("para", "pra", "pro", "na", "no", "em")
                val fromWords = setOf("de", "da", "do", "dos", "das")
                when (ordered.size) {
                    0 -> {}
                    1 -> {
                        val a = ordered[0]
                        if (precededBy(a.pos, fromWords) && !precededBy(a.pos, toWords)) accountId = a.id else toAccountId = a.id
                    }
                    else -> {
                        val a = ordered[0]; val b = ordered[1]
                        if (precededBy(a.pos, toWords) && !precededBy(b.pos, toWords)) {
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
            TxType.EXPENSE -> {
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
                    else -> wantsCredit || installments > 1
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
        val descTokens = toks.filter { !it.used && it.norm !in STOP_WORDS && it.norm.any(Char::isLetterOrDigit) }
        val descNorm = descTokens.joinToString(" ") { it.norm }
        val categoryId = if (type == TxType.EXPENSE || type == TxType.INCOME) {
            guessCategory(if (type == TxType.INCOME) CategoryKind.INCOME else CategoryKind.EXPENSE, descTokens, descNorm)
        } else null

        var description = descTokens.joinToString(" ") { cleanOrig(it.orig) }.trim()
        if (description.isEmpty()) {
            description = when (type) {
                TxType.EXPENSE, TxType.INCOME -> ctx.categories.firstOrNull { it.id == categoryId }?.name ?: ""
                TxType.TRANSFER -> "Transferência"
                TxType.CARD_PAYMENT -> "Pagamento de fatura"
                TxType.INVEST_IN -> "Aporte"
                TxType.INVEST_OUT -> "Resgate"
                TxType.GOAL_IN -> "Dinheiro guardado"
                TxType.GOAL_OUT -> "Dinheiro retirado"
            }
        }

        description = description.split(' ').joinToString(" ") { w -> if (w.normalized() in ACRONYMS) w.uppercase(LocaleBR) else w }

        return ParsedEntry(
            type = type,
            amount = amount,
            description = description.capFirst(),
            categoryId = categoryId,
            accountId = accountId,
            toAccountId = toAccountId,
            cardId = cardId,
            installments = if (cardId != null) installments else 1,
            date = date,
            investmentId = investmentId,
            goalId = goalId,
        )
    }

    /* ------------------------------------------------------------------ */

    private fun tokenize(input: String): List<Token> {
        val out = ArrayList<Token>()
        var pos = 0
        for (r in input.trim().split(Regex("\\s+"))) {
            if (r.isEmpty()) continue
            val n = normToken(r)
            if (n.isEmpty()) continue
            out += Token(r, n, pos, pos + n.length)
            pos += n.length + 1
        }
        return out
    }

    private fun normToken(r: String): String {
        var t = r.normalized().trim('(', ')', '[', ']', '"', '\'', '!', '?', ';', ':', '“', '”', '-')
        while (t.isNotEmpty() && (t.last() == '.' || t.last() == ',')) t = t.dropLast(1)
        return t
    }

    private fun cleanOrig(s: String) =
        s.trim('(', ')', '[', ']', '"', '\'', '!', '?', ';', ':', '“', '”', ',', '.', '-')

    private fun overlapping(r: IntRange) = toks.filter { it.start <= r.last && it.end > r.first }
    private fun free(r: IntRange) = overlapping(r).none { it.used }
    private fun consume(r: IntRange) = overlapping(r).forEach { it.used = true }
    private fun consume(m: EMatch) = m.ranges.forEach { consume(it) }
    private fun has(rx: String) = Regex(rx).containsMatchIn(text)

    private fun consumeFirst(rx: String): Boolean {
        val m = Regex(rx).findAll(text).firstOrNull { free(it.range) } ?: return false
        consume(m.range)
        return true
    }

    private fun precededBy(pos: Int, words: Set<String>): Boolean {
        val idx = toks.indexOfFirst { it.start == pos }
        if (idx <= 0) return false
        return toks.subList(max(0, idx - 2), idx).any { it.norm in words }
    }

    /* ------------------------------ datas ------------------------------ */

    private fun parseDate(): LocalDate {
        val today = ctx.today
        Regex("\\b(hoje|ontem|anteontem|amanha)\\b").find(text)?.let { m ->
            consume(m.range)
            return when (m.groupValues[1]) {
                "ontem" -> today.minusDays(1)
                "anteontem" -> today.minusDays(2)
                "amanha" -> today.plusDays(1)
                else -> today
            }
        }
        Regex("\\b(?:(?:n[oa]|em) )?(?:dia )?(\\d{1,2})/(\\d{1,2})(?:/(\\d{2,4}))?\\b").find(text)?.let { m ->
            val day = m.groupValues[1].toInt()
            val month = m.groupValues[2].toInt()
            val yearTxt = m.groupValues[3]
            var year = when {
                yearTxt.isEmpty() -> today.year
                yearTxt.length == 2 -> 2000 + yearTxt.toInt()
                else -> yearTxt.toInt()
            }
            val d = runCatching { LocalDate.of(year, month, day) }.getOrNull()
            if (d != null) {
                consume(m.range)
                if (yearTxt.isEmpty() && d.isAfter(today.plusDays(31))) year -= 1
                return LocalDate.of(year, month, day.coerceAtMost(java.time.YearMonth.of(year, month).lengthOfMonth()))
            }
        }
        val months = listOf(
            "janeiro", "fevereiro", "marco", "abril", "maio", "junho",
            "julho", "agosto", "setembro", "outubro", "novembro", "dezembro",
        )
        Regex("\\b(?:(?:n[oa]|em) )?(?:dia )?(\\d{1,2}) de (${months.joinToString("|")})\\b").find(text)?.let { m ->
            val day = m.groupValues[1].toInt()
            val month = months.indexOf(m.groupValues[2]) + 1
            var d = runCatching { LocalDate.of(today.year, month, day) }.getOrNull()
            if (d != null) {
                consume(m.range)
                if (d.isAfter(today.plusDays(31))) d = d.minusYears(1)
                return d
            }
        }
        Regex("\\b(?:(?:n[oa]|em) )?dia (\\d{1,2})\\b").find(text)?.let { m ->
            val day = m.groupValues[1].toInt()
            if (day in 1..31) {
                consume(m.range)
                val ym = if (day <= today.dayOfMonth) today.ym() else today.ym().minusMonths(1)
                return ym.dayClamped(day)
            }
        }
        val weekdays = mapOf(
            "segunda" to DayOfWeek.MONDAY, "terca" to DayOfWeek.TUESDAY, "quarta" to DayOfWeek.WEDNESDAY,
            "quinta" to DayOfWeek.THURSDAY, "sexta" to DayOfWeek.FRIDAY, "sabado" to DayOfWeek.SATURDAY,
            "domingo" to DayOfWeek.SUNDAY,
        )
        Regex("\\b(?:n[oa] )?(segunda|terca|quarta|quinta|sexta|sabado|domingo)(?:-feira| feira)?( passad[oa])?\\b")
            .find(text)?.let { m ->
                consume(m.range)
                val target = weekdays.getValue(m.groupValues[1])
                var back = (today.dayOfWeek.value - target.value + 7) % 7
                if (back == 0 && m.groupValues[2].isNotEmpty()) back = 7
                return today.minusDays(back.toLong())
            }
        return today
    }

    /* ----------------------------- parcelas ---------------------------- */

    private fun parseInstallments(): Int {
        consumeFirst("\\ba vista\\b")
        val patterns = listOf(
            "\\b(?:em )?(\\d{1,2}) ?(?:x|vezes)\\b",
            "\\bparcelad[oa] (?:em )?(\\d{1,2})(?: ?(?:x|vezes|parcelas))?\\b",
            "\\b(?:em )?(\\d{1,2}) parcelas\\b",
        )
        for (p in patterns) {
            val m = Regex(p).findAll(text).firstOrNull { free(it.range) } ?: continue
            val n = m.groupValues[1].toInt()
            if (n in 1..48) {
                consume(m.range)
                return n
            }
        }
        return 1
    }

    /* ------------------------------- valor ----------------------------- */

    private fun toCents(num: String, mil: Boolean): Long? {
        val v: BigDecimal = when {
            num.contains(',') -> num.replace(".", "").replace(',', '.').toBigDecimalOrNull()
            Regex("\\d{1,3}(\\.\\d{3})+").matches(num) -> num.replace(".", "").toBigDecimalOrNull()
            else -> num.toBigDecimalOrNull()
        } ?: return null
        val x = if (mil) v.multiply(BigDecimal(1000)) else v
        return x.movePointRight(2).setScale(0, RoundingMode.HALF_UP).toLong()
    }

    private fun parseAmount(): Long? {
        val cands = ArrayList<Cand>()
        fun scan(rx: String, score: (MatchResult) -> Int, value: (MatchResult) -> Long?) {
            Regex(rx).findAll(text).forEach { m ->
                if (free(m.range)) value(m)?.let { cands += Cand(it, score(m), m.range.first, m.range) }
            }
        }
        // "50 reais e 50 centavos"
        scan("\\b$NUM ?(?:reais|real)? e (\\d{1,2}) centavos\\b", { 6 }) { m ->
            toCents(m.groupValues[1], false)?.plus(m.groupValues[2].toLong())
        }
        // "R$ 32,50" / "R$ 2 mil"
        scan("r\\$ ?$NUM( ?mil\\b)?", { 5 }) { m -> toCents(m.groupValues[1], m.groupValues[2].isNotBlank()) }
        // "120 reais"
        scan("\\b$NUM( ?mil)? ?(?:reais|real|conto|contos|pila|pilas)\\b", { 5 }) { m ->
            toCents(m.groupValues[1], m.groupValues[2].isNotBlank())
        }
        // "gastei 50", "por 80"
        scan(
            "\\b(?:gastei|paguei|recebi|ganhei|custou|deu|foi|por|valor|total|investi|apliquei|guardei|transferi|resgatei|separei)( de)? $NUM( ?mil\\b)?(?![\\p{L}\\d])",
            { 4 },
        ) { m -> toCents(m.groupValues[2], m.groupValues[3].isNotBlank()) }
        // número solto
        scan("(?<![\\d/])\\b$NUM( ?mil\\b)?(?![\\p{L}\\d/])", { m -> if (m.groupValues[1].contains(',') || Regex("\\.\\d{1,2}$").containsMatchIn(m.groupValues[1])) 2 else 1 }) { m ->
            toCents(m.groupValues[1], m.groupValues[2].isNotBlank())
        }
        // "mil reais" sem número
        scan("\\bmil\\b", { 0 }) { 100_000L }

        val best = cands.filter { it.cents in 1..99_999_999_999L }
            .maxWithOrNull(compareBy<Cand>({ it.score }, { it.pos })) ?: return null
        consume(best.range)
        return best.cents
    }

    /* ---------------------------- entidades ---------------------------- */

    private fun matchNames(items: List<Pair<Long, String>>): List<EMatch> = items.mapNotNull { (id, name) ->
        val norm = name.normalized()
        var words = norm.split(Regex("[^a-z0-9]+"))
            .filter { it.length >= 2 && it !in GENERIC_NAME_WORDS && !it.all(Char::isDigit) }
        if (words.isEmpty()) words = listOf(norm.trim()).filter { it.isNotEmpty() }
        val ranges = words.mapNotNull { w ->
            Regex("\\b${Regex.escape(w)}\\b").findAll(text).firstOrNull { free(it.range) }?.range
        }
        if (ranges.isEmpty()) null else EMatch(id, ranges.size, ranges.minOf { it.first }, ranges)
    }.sortedWith(compareByDescending<EMatch> { it.score }.thenBy { it.pos })

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
            val m = Regex("\\b${Regex.escape(n)}\\b").find(text)
            if (m != null && free(m.range)) offer(c.id, 8.0 + n.length / 100.0)
        }
        // 3. Palavra que você já usou antes com uma categoria
        descTokens.forEach { t -> if (t.norm.length >= 3) ctx.learned[t.norm]?.let { offer(it, 6.0) } }
        // 4. Palavras-chave da categoria (as compostas são mais específicas)
        for (c in cats) {
            for (kw in c.keywords.split(',').map { it.trim().normalized() }.filter { it.isNotEmpty() }) {
                val m = Regex("\\b${Regex.escape(kw)}(?:s|es)?\\b").find(text) ?: continue
                if (!free(m.range)) continue
                offer(c.id, (if (kw.contains(' ')) 9.0 else 4.0) + kw.length / 100.0)
            }
        }
        return bestId ?: cats.firstOrNull { it.icon == "more" }?.id
    }

    companion object {
        private const val NUM = "(\\d{1,3}(?:\\.\\d{3})+(?:,\\d{1,2})?|\\d+(?:,\\d{1,2})?|\\d+\\.\\d{1,2})"

        private val GENERIC_NAME_WORDS = setOf(
            "conta", "cartao", "de", "do", "da", "dos", "das", "banco", "credito", "debito", "corrente",
            "minha", "meu", "o", "a", "e", "para", "pra", "caixinha", "meta", "investimento", "em", "no", "na",
        )

        private val ACRONYMS = setOf(
            "tv", "ipva", "iptu", "iof", "das", "darf", "inss", "cdb", "lci", "lca", "fgts", "irpf", "cpf", "cnh",
            "vr", "va", "pc", "dvd", "sus", "plr", "mei", "cdi", "fii", "fiis",
        )

        val STOP_WORDS = setOf(
            "gastei", "gasto", "gastos", "gastar", "paguei", "pago", "pagar", "pagamento", "comprei", "compra",
            "comprar", "recebi", "receber", "recebido", "recebimento", "ganhei", "entrou", "caiu", "transferi",
            "transferencia", "transferir", "passei", "movi", "mandei", "guardei", "separei", "reservei", "poupei",
            "coloquei", "depositei", "investi", "apliquei", "aportei", "aporte", "aplicacao", "resgatei", "resgate",
            "tirei", "retirei", "saquei", "usei", "peguei", "quitei", "com", "no", "na", "nos", "nas", "de", "do",
            "da", "dos", "das", "em", "um", "uma", "uns", "umas", "o", "a", "os", "as", "e", "pra", "para", "pro",
            "por", "pelo", "pela", "reais", "real", "r$", "conto", "contos", "pila", "pilas", "foi", "fiz", "deu",
            "custou", "valor", "total", "hoje", "ontem", "anteontem", "amanha", "meu", "minha", "meus", "minhas",
            "ao", "aos", "que", "via", "la", "aqui", "fatura", "caixinha", "cofrinho", "meta", "cartao", "conta",
            "investimento", "eu", "agora", "tambem",
        )
    }
}
