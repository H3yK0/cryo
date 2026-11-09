// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.domain

import io.github.h3yk0.cryo.data.db.TxType

/**
 * RF-VOZ-05/06/07: do jeito que a pessoa FALA para o jeito que o [PhraseParser] entende. O Whisper escreve o que ouviu
 * ("Uber, 18 e 90.", "trinta e dois e cinquenta", "almoço 30 e uber 15", "32, não, 35"); aqui isso vira uma ou mais
 * frases limpas ("uber 18,90", "32,50", duas frases, "35"). Regras exatas e testáveis, sem modelo de linguagem (PRI-05):
 * com dinheiro, nada pode ser inventado.
 *
 * Português primeiro (pedido do Hanry, 09/11/2026); nas outras línguas o Whisper já escreve os números com algarismos e
 * aqui fica a limpeza (ruído, repetições, pontuação) e a divisão em vários lançamentos.
 */
object Spoken {

    /** A fala inteira → os lançamentos para conferir (um ou mais). Vazio se não houve fala. */
    fun entries(heard: String, ctx: ParseContext): List<ParsedEntry> =
        share(phrases(heard, ctx.language, vocabulary(ctx)), { PhraseParser(ctx).parse(it) }, ctx.today, ctx.language)
            .filter { it.amount != null || it.description.isNotBlank() }

    /** As palavras dos nomes da pessoa (contas, cartões, categorias, metas, investimentos), já normalizadas. */
    fun vocabulary(ctx: ParseContext): Set<String> =
        (ctx.accounts.map { it.name } + ctx.cards.map { it.name } + ctx.categories.map { it.name } + ctx.goals.map { it.name } +
            ctx.investments.map { it.name }).flatMap { it.normalized().split(Regex("[^\\p{L}\\p{N}]+")) }.filter { it.length >= 3 }.toSet()

    /** O que vira cada frase falada: o texto para o [PhraseParser]. */
    fun phrases(heard: String, language: AppLanguage = AppLocale.language, vocabulary: Set<String> = emptySet()): List<String> {
        var t = clean(heard)
        if (t.isEmpty()) return emptyList()
        if (language == AppLanguage.PT_BR) {
            t = PtNumbers.convert(t)
            t = correctionsPt(t)
            t = diminutivesPt(t, vocabulary)
        }
        return split(t, language)
    }

    /* ------------------------------------------------ limpeza ------------------------------------------------ */

    /**
     * Frases que o Whisper "inventa" em silêncio ou ruído (aprendidas de legendas de vídeos). Se tudo o que ele ouviu for
     * isso, não houve fala.
     */
    private val HALLUCINATIONS = listOf(
        "amara.org", "legendas pela comunidade", "legenda adriana", "legendado por", "obrigado por assistir", "obrigada por assistir",
        "inscreva-se", "se inscreva", "deixe seu like", "ative o sininho", "thank you for watching", "thanks for watching",
        "subtitles by", "gracias por ver", "suscríbete", "sous-titrage", "sous-titres", "untertitel", "merci d'avoir regardé",
        "grazie per la visione", "ご視聴ありがとうございました", "チャンネル登録", "시청해 주셔서 감사합니다", "구독과 좋아요",
        "请不吝点赞", "字幕由", "感谢观看", "देखने के लिए धन्यवाद",
    )

    private val FILLERS = Regex("^(?:(?:é|eh|ah|ahn|hã|hum|hmm|uhm|uh|tipo|então|entao|aí|olha)[,.!]?\\s+)+", RegexOption.IGNORE_CASE)

    /** Tira ruído, repetições ("meu e o meu e o meu"), pontuação de fim e espaços. Vazio se não sobrou fala. */
    fun clean(heard: String): String {
        var t = heard.replace(Regex("[\\[(（【][^\\])）】]*[\\])）】]"), " ")   // [Música], (risos), (inaudível)
            .replace('\n', ' ').replace(Regex("\\s+"), " ").trim()
        // Frase a frase: o que for "legenda de vídeo" sai.
        val sentences = t.split(Regex("(?<=[.!?])\\s+|(?<=[。！？])")).filter { s ->
            val low = s.lowercase()
            s.isNotBlank() && HALLUCINATIONS.none { low.contains(it) }
        }
        t = sentences.joinToString(" ").trim()
        t = collapseRepeats(t)
        t = t.trimStart('-', '–', '—', ' ', '.', ',', '…').replace(FILLERS, "")
        t = t.trimEnd('.', '!', '?', '…', ' ', ',', '。', '！', '？').trim()
        if (t.none { it.isLetterOrDigit() }) return ""
        return t
    }

    /** "meu e o meu e o meu e o meu cartão" → "meu cartão": o Whisper às vezes entra em repetição. */
    private fun collapseRepeats(t: String): String {
        var words = t.split(' ').filter { it.isNotEmpty() }
        var changed = true
        while (changed) {
            changed = false
            loop@ for (n in 1..4) {
                var i = 0
                while (i + 2 * n <= words.size) {
                    var reps = 1
                    while (i + (reps + 1) * n <= words.size && words.subList(i, i + n).map(::low) == words.subList(i + reps * n, i + (reps + 1) * n).map(::low)) reps++
                    // Uma palavra repetida duas vezes pode ser de verdade ("bem bem"); três ou mais é repetição.
                    if (reps >= 3 || (reps >= 2 && n >= 2)) {
                        words = words.subList(0, i + n) + words.subList(i + reps * n, words.size)
                        changed = true
                        break@loop
                    }
                    i++
                }
            }
        }
        return words.joinToString(" ")
    }

    private fun low(s: String) = s.lowercase().trim(',', '.', '!', '?')

    /* ------------------------------------------------ correções ------------------------------------------------ */

    private const val PT_FIX = "(?:não|nao|quer dizer|digo|ou melhor|aliás|alias|na verdade|perdão|perdao|opa)"
    private val PT_FIX_NUMBER = Regex("(\\d+(?:[.,]\\d+)?)\\s*,?\\s*$PT_FIX\\s*,?\\s*(?:é\\s+|foi\\s+)?(\\d+(?:[.,]\\d+)?)", RegexOption.IGNORE_CASE)
    private val PT_FIX_PLACE = Regex("\\b(n[oa]|em|com o|com a|pelo|pela|de|do|da)\\s+(\\S+(?:\\s+\\S+)?)\\s*,?\\s*$PT_FIX\\s*,?\\s*\\1\\s+", RegexOption.IGNORE_CASE)

    /** "32, não, 35" → "35"; "no débito, não, no crédito" → "no crédito". */
    internal fun correctionsPt(t: String): String {
        var s = t
        repeat(3) { s = PT_FIX_NUMBER.replace(s) { it.groupValues[2] } }
        repeat(3) { s = PT_FIX_PLACE.replace(s) { it.groupValues[1] + " " } }
        return s.replace(Regex("\\s+"), " ").trim()
    }

    /* ------------------------------------------------ diminutivos ------------------------------------------------ */

    /**
     * "com o roxinho" → "com o roxo" quando "roxo" é uma palavra dos nomes da pessoa (contas, cartões, categorias,
     * metas). Só troca nesse caso: "salgadinho" continua "salgadinho".
     */
    internal fun diminutivesPt(t: String, vocabulary: Set<String>): String {
        if (vocabulary.isEmpty()) return t
        return Regex("[\\p{L}]+").replace(t) { m ->
            val w = m.value
            val n = w.normalized()
            val bases = buildList {
                for ((suf, rep) in listOf("zinhos" to "s", "zinhas" to "s", "zinho" to "", "zinha" to "", "inhos" to "os", "inhas" to "as", "inho" to "o", "inha" to "a")) {
                    if (n.length > suf.length + 1 && n.endsWith(suf)) {
                        val stem = n.dropLast(suf.length)
                        add(stem + rep); add(stem); add(stem + "o"); add(stem + "a")
                    }
                }
            }
            bases.firstOrNull { it in vocabulary } ?: w
        }
    }

    /* ------------------------------------------------ vários lançamentos ------------------------------------------------ */

    private val DIGIT = Regex("\\d")
    private val LETTER = Regex("\\p{L}{2,}")
    private val CENTS = Regex("^\\d{1,2}\\s*(?:centavos?|cents?|céntimos?|centimes?|centesimi|cent)\\b", RegexOption.IGNORE_CASE)

    /**
     * "almoço 30 e uber 15" → ["almoço 30", "uber 15"]. Separa no "e", na vírgula e no ponto, mas só fica separado o que
     * tem valor E palavra: "Almoço, 32,50, no débito" continua uma frase só.
     */
    internal fun split(t: String, language: AppLanguage): List<String> {
        val sep = when (language) {
            AppLanguage.PT_BR -> "\\s+(?:e também|e depois|e|mais|depois)\\s+"
            AppLanguage.EN -> "\\s+(?:and then|and also|and|plus|then)\\s+"
            AppLanguage.ES -> "\\s+(?:y luego|y también|y|e|más|luego)\\s+"
            AppLanguage.FR -> "\\s+(?:et puis|et aussi|et|plus|puis)\\s+"
            AppLanguage.DE -> "\\s+(?:und dann|und auch|und|plus|dann)\\s+"
            AppLanguage.IT -> "\\s+(?:e poi|e anche|e|più|poi)\\s+"
            AppLanguage.HI -> "\\s+(?:और फिर|और|फिर)\\s+"
            else -> "(?!)"
        }
        // Vírgula ou ponto seguidos de espaço separam; "32,50" (sem espaço) é um valor.
        val pieces = t.split(Regex("$sep|[,;]\\s+|[.。、，]\\s*(?=\\D)|[.。]$", RegexOption.IGNORE_CASE)).map { it.trim() }.filter { it.isNotEmpty() }
        val groups = ArrayList<StringBuilder>()
        for (p in pieces) {
            val last = groups.lastOrNull()
            // Fica separado só quando os dois lados têm valor e palavra; o resto completa a frase de antes.
            // "32 reais e 50 centavos" é um valor só.
            val cents = CENTS.containsMatchIn(p)
            if (last != null && (cents || !(complete(last) && complete(p)))) last.append(if (cents) " e " else " ").append(p) else groups += StringBuilder(p)
        }
        val joined = if (language == AppLanguage.JA || language == AppLanguage.ZH) groups.map { it.toString().replace(" ", "") } else groups.map { it.toString() }
        return joined.map { it.replace(Regex("\\s+"), " ").trim() }.filter { it.isNotEmpty() }
    }

    private fun complete(s: CharSequence) = DIGIT.containsMatchIn(s) && LETTER.containsMatchIn(s)

    /**
     * Ajustes entre as frases de uma mesma fala: a data, a conta ou o cartão e o verbo ditos uma vez valem para todas
     * ("mercado 120 e farmácia 30 no cartão" = as duas no cartão; "recebi 200 do João e 100 da Maria" = duas entradas).
     */
    fun share(parts: List<String>, parse: (String) -> ParsedEntry, today: java.time.LocalDate, language: AppLanguage = AppLocale.language): List<ParsedEntry> {
        if (parts.size <= 1) return parts.map(parse)
        val L = PhraseLang.forLanguage(language)
        // O verbo do começo ("recebi", "paguei") vale para as frases que não têm verbo.
        val verbs = L.verbs.toSet()
        val firstWord = parts.first().split(' ').first()
        val texts = parts.mapIndexed { i, p ->
            val hasVerb = p.normalized().split(Regex("[^\\p{L}\\p{N}]+")).any { it in verbs }
            if (i > 0 && !hasVerb && firstWord.normalized() in verbs) "$firstWord $p" else p
        }
        val parsed = texts.map(parse)
        // A data diferente de hoje, se só uma data foi dita, vale para todas.
        val dated = parsed.map { it.date }.filter { it != today }.distinct()
        // A conta que o leitor escolhe quando nada é dito (a mesma para toda frase sem conta).
        val fallback = parse("1").accountId
        val plain = { e: ParsedEntry -> e.type == TxType.EXPENSE && e.cardId == null && e.accountId == fallback }
        val specific = parsed.filter { it.type == TxType.EXPENSE && !plain(it) }.map { it.cardId to it.accountId }.distinct()
        return parsed.map { e ->
            var r = e
            if (dated.size == 1 && e.date == today) r = r.copy(date = dated[0])
            if (specific.size == 1 && plain(e)) r = r.copy(cardId = specific[0].first, accountId = specific[0].second)
            r
        }
    }
}

/**
 * Números falados em português: "trinta e dois" → 32, "cento e vinte" → 120, "dois mil e quinhentos" → 2500, e o jeito
 * de falar centavos: "dezoito e noventa" / "18 e 90" → "18,90". Palavras pequenas soltas ("um café", "dois pães") ficam
 * como estão: podem ser quantidade, não valor.
 */
internal object PtNumbers {
    private enum class Kind { UNIT, TEEN, TEN, HUNDRED, THOUSAND, MILLION }

    private val WORDS: Map<String, Pair<Long, Kind>> = buildMap {
        listOf("um" to 1, "uma" to 1, "dois" to 2, "duas" to 2, "tres" to 3, "quatro" to 4, "cinco" to 5, "seis" to 6, "sete" to 7, "oito" to 8, "nove" to 9)
            .forEach { put(it.first, it.second.toLong() to Kind.UNIT) }
        listOf("dez" to 10, "onze" to 11, "doze" to 12, "treze" to 13, "catorze" to 14, "quatorze" to 14, "quinze" to 15, "dezesseis" to 16,
            "dezasseis" to 16, "dezessete" to 17, "dezassete" to 17, "dezoito" to 18, "dezenove" to 19, "dezanove" to 19)
            .forEach { put(it.first, it.second.toLong() to Kind.TEEN) }
        listOf("vinte" to 20, "trinta" to 30, "quarenta" to 40, "cinquenta" to 50, "cincoenta" to 50, "sessenta" to 60, "setenta" to 70, "oitenta" to 80, "noventa" to 90)
            .forEach { put(it.first, it.second.toLong() to Kind.TEN) }
        listOf("cem" to 100, "cento" to 100, "duzentos" to 200, "duzentas" to 200, "trezentos" to 300, "trezentas" to 300, "quatrocentos" to 400,
            "quatrocentas" to 400, "quinhentos" to 500, "quinhentas" to 500, "seiscentos" to 600, "seiscentas" to 600, "setecentos" to 700,
            "setecentas" to 700, "oitocentos" to 800, "oitocentas" to 800, "novecentos" to 900, "novecentas" to 900)
            .forEach { put(it.first, it.second.toLong() to Kind.HUNDRED) }
        put("mil", 1000L to Kind.THOUSAND)
        put("milhao", 1_000_000L to Kind.MILLION)
        put("milhoes", 1_000_000L to Kind.MILLION)
    }

    /** O que vem depois de um valor (deixa "um", "dois"... virar número). */
    private val MONEY = setOf("real", "reais", "conto", "contos", "pila", "pilas", "pau", "paus", "pratas", "centavo", "centavos", "mil")
    /** Contagens: "dez vezes" vira "10 vezes", e "N e M" seguido delas não é centavo. */
    private val COUNTS = setOf("x", "vezes", "parcelas", "parcela", "dias", "meses", "anos", "horas", "%")
    /** Depois destas palavras, um número pequeno é valor: "paguei dez", "dia cinco". */
    private val BEFORE_VALUE = setOf("dia", "por", "custou", "deu", "foi", "gastei", "paguei", "recebi", "ganhei", "total", "valor", "r$", "ha")

    private class Tok(val orig: String, val word: String, val punct: String)

    private fun tokens(t: String): List<Tok> = t.split(' ').filter { it.isNotEmpty() }.map { raw ->
        val m = Regex("^(.*?)([,.!?;:]*)$").find(raw)!!
        val core = m.groupValues[1].ifEmpty { raw }
        val punct = if (m.groupValues[1].isEmpty()) "" else m.groupValues[2]
        Tok(core, core.normalized(), punct)
    }

    /** Uma sequência de números falados a partir de [start]: valor, onde termina e quantas palavras usou. */
    private data class Run(val value: Long, val end: Int, val words: Int, val digits: Boolean)

    private fun run(toks: List<Tok>, start: Int): Run? {
        val first = toks[start]
        // Algarismos: "18", "3", "1.200", "32,50" (este último já é um valor completo).
        if (first.word.matches(Regex("\\d{1,3}(?:\\.\\d{3})+|\\d+"))) {
            var value = first.word.replace(".", "").toLongOrNull() ?: return null
            var end = start + 1
            var words = 1
            // "3 mil e 500"
            if (first.punct.isEmpty() && end < toks.size && toks[end].word == "mil") {
                value *= 1000; end++; words++
                if (toks[end - 1].punct.isEmpty() && end + 1 < toks.size && toks[end].word == "e") {
                    val nxt = run(toks, end + 1)
                    if (nxt != null && nxt.value in 1..999) { value += nxt.value; end = nxt.end; words += 1 + nxt.words }
                }
            }
            return Run(value, end, words, true)
        }
        if (first.word !in WORDS) return null
        var total = 0L
        var cur = 0L
        var last: Kind? = null
        var i = start
        var used = 0
        while (i < toks.size) {
            if (used > 0 && toks[i - 1].punct.isNotEmpty()) break   // vírgula ou ponto encerram o número
            var j = i
            if (used > 0 && toks[i].word == "e") {                  // "trinta E dois"
                if (toks[i].punct.isNotEmpty()) break
                j = i + 1
            }
            if (j >= toks.size) break
            val (v, k) = WORDS[toks[j].word] ?: break
            val ok = when (k) {
                Kind.HUNDRED -> cur == 0L && (last == null || last == Kind.THOUSAND || last == Kind.MILLION)
                Kind.TEN, Kind.TEEN -> cur % 100 == 0L && (last == null || last == Kind.THOUSAND || last == Kind.MILLION || last == Kind.HUNDRED)
                Kind.UNIT -> cur % 10 == 0L && last != Kind.UNIT && last != Kind.TEEN
                Kind.THOUSAND -> last != Kind.THOUSAND && total % 1_000_000 == 0L
                Kind.MILLION -> last != Kind.MILLION && last != Kind.THOUSAND && total == 0L
            }
            if (!ok) break
            when (k) {
                Kind.THOUSAND -> { total += (if (cur == 0L) 1 else cur) * 1000; cur = 0 }
                Kind.MILLION -> { total += (if (cur == 0L) 1 else cur) * 1_000_000; cur = 0 }
                else -> cur += v
            }
            last = k
            used++
            i = j + 1
        }
        if (used == 0) return null
        return Run(total + cur, i, used, false)
    }

    fun convert(t: String): String {
        val toks = tokens(t)
        val out = ArrayList<String>()
        var i = 0
        while (i < toks.size) {
            val r = run(toks, i)
            if (r == null) { out += toks[i].orig + toks[i].punct; i++; continue }
            val endPunct = toks[r.end - 1].punct
            val prev = toks.getOrNull(i - 1)?.word
            // Centavos falados: "18 e 90", "dezoito e noventa", "trinta e dois e cinquenta".
            if (endPunct.isEmpty() && prev != "dia" && r.end + 1 < toks.size && toks[r.end].word == "e" && toks[r.end].punct.isEmpty()) {
                val c = run(toks, r.end + 1)
                val after = toks.getOrNull(c?.end ?: -1)?.word
                if (c != null && c.value in 10..99 && (!c.digits || toks[r.end + 1].word.length == 2) && after !in COUNTS) {
                    out += "${r.value},${c.value}" + toks[c.end - 1].punct
                    i = c.end
                    continue
                }
            }
            val next = toks.getOrNull(r.end)?.word
            val keep = !r.digits && r.words == 1 && r.value <= 10 && next !in MONEY && next !in COUNTS && prev !in BEFORE_VALUE
            when {
                keep -> out += toks[i].orig + toks[i].punct
                r.digits && r.words == 1 -> out += toks[i].orig + toks[i].punct
                else -> out += r.value.toString() + endPunct
            }
            i = r.end
        }
        return out.joinToString(" ")
    }
}
