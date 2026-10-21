// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.importer

import io.github.h3yk0.cryo.domain.normalized
import java.time.LocalDate
import kotlin.math.abs

/**
 * Extrato em PDF: cada banco desenha o seu de um jeito, então aqui o Cryo procura padrões no texto:
 * uma data (no começo da linha ou como título de um grupo de dias), a descrição e um valor no formato brasileiro.
 * Por isso a tela de conferência é tão importante para PDF.
 */
object PdfStatement {
    private val MONEY = Regex("""(?<![\d,.])([-−+]\s*)?(R\$\s*)?([-−]\s*)?(\d{1,3}(?:\.\d{3})*,\d{2})(\s*[-−])?(\s*[DC](?![A-Za-z]))?""")
    private val LEAD_DATE = Regex(
        """^(\d{1,2}[/.-]\d{1,2}(?:[/.-]\d{2,4})?|\d{1,2}\s+(?:de\s+)?(?:jan|fev|mar|abr|mai|jun|jul|ago|set|out|nov|dez)[a-zç]*\.?(?:\s+(?:de\s+)?\d{4})?)(?=\s|$)""",
        RegexOption.IGNORE_CASE,
    )
    /** Datas completas no texto (com o ano), para descobrir o ano das datas que vêm sem ele. */
    private val FULL_DATE = Regex(
        """\b\d{1,2}[/.-]\d{1,2}[/.-]\d{4}\b|\b\d{4}-\d{2}-\d{2}\b|\b\d{1,2}\s+(?:de\s+)?(?:jan|fev|mar|abr|mai|jun|jul|ago|set|out|nov|dez)[a-z]*\.?\s+(?:de\s+)?\d{4}\b""",
    )

    // Para PDFs sem sinal nos valores: palavras que indicam saída ou entrada. Vale a mais específica (a mais longa):
    // "pagamento recebido" é entrada, "pagamento de fatura" é saída, "compra credito" (no crédito) é saída.
    private val OUT_WORDS = listOf(
        "compra", "pagamento", "pgto", "pagto", "pag ", "pix enviado", "enviado", "enviada", "transferencia enviada", "saque",
        "tarifa", "debito", "boleto", "fatura", "juros", "iof", "anuidade", "seguro", "parcela", "aplicacao",
    )
    private val IN_WORDS = listOf(
        "recebid", "deposito", "salario", "estorno", "estornad", "rendimento", "reembolso", "devolucao", "pix recebido",
        "transferencia recebida", "resgate", "cashback", "pagamento recebido", "credito em conta", "credito de salario",
    )

    private fun longest(words: List<String>, text: String): Int = words.filter { text.contains(it) }.maxOfOrNull { it.length } ?: 0

    /** Fatura de cartão ou extrato de conta, pelo que o texto traz (um extrato de conta também fala em "fatura" e "cartão"). */
    private fun looksLikeCard(n: String): Boolean {
        val card = listOf("fatura" to 1, "vencimento" to 1, "pagamento minimo" to 2, "limite" to 1, "total a pagar" to 1, "total da fatura" to 2)
            .sumOf { (w, p) -> if (n.contains(w)) p else 0 }
        val account = listOf(
            "saldo anterior" to 2, "saldo do dia" to 2, "saldo disponivel" to 1, "saldo final" to 1, "extrato de conta" to 2,
            "conta corrente" to 1, "agencia" to 1,
        ).sumOf { (w, p) -> if (n.contains(w)) p else 0 }
        return card >= 3 && card > account
    }

    fun parse(pages: List<String>, today: LocalDate): Statement {
        val text = pages.joinToString("\n")
        val rawLines = text.lines().map { it.replace(' ', ' ').replace(Regex("[ \\t]+"), " ").trim() }.filter { it.isNotEmpty() }
        if (rawLines.isEmpty()) {
            throw StatementException("Não encontrei texto nesse PDF. Se ele for uma foto ou digitalização, o Cryo não consegue ler.")
        }
        val reference = Values.reference(FULL_DATE.findAll(text.normalized()).mapNotNull { Values.date(it.value) }, today)
        // O PDF marca as saídas com sinal (- ou D)? Então o que não tem sinal é entrada. Se marca só as entradas
        // (+ ou C), é o contrário: o que não tem sinal é saída.
        val marks = rawLines.flatMap { MONEY.findAll(it).toList() }
        val docMarksOut = marks.any(::marksOut)
        val docMarksIn = !docMarksOut && marks.any(::marksIn)

        val lines = ArrayList<StatementLine>()
        val skipped = ArrayList<SkippedLine>()
        var currentDate: LocalDate? = null
        var section = 0 // 1 = entradas, -1 = saídas (extratos que agrupam assim)
        val pending = ArrayList<String>()

        for (line in rawLines) {
            var rest = line
            LEAD_DATE.find(line)?.let { m ->
                val d = Values.date(m.value, reference)
                if (d != null) {
                    currentDate = d
                    rest = line.substring(m.range.last + 1).trim()
                    pending.clear()
                }
            }
            val norm = rest.normalized()
            when {
                norm.contains("total de entradas") || norm == "entradas" || norm.startsWith("creditos") -> section = 1
                norm.contains("total de saidas") || norm == "saidas" || norm.startsWith("debitos") -> section = -1
            }
            val money = MONEY.findAll(rest).toList()
            if (money.isEmpty()) {
                if (rest.isNotEmpty() && rest.any { it.isLetter() }) {
                    pending += rest
                    if (pending.size > 3) pending.removeAt(0)
                }
                continue
            }
            val first = money.first()
            var description = rest.substring(0, first.range.first).trim().trimEnd('-', '–', ':').trim()
            if (description.isEmpty()) description = pending.joinToString(" ").trim()
            val date = currentDate
            when {
                date == null -> { skipped += SkippedLine(line, "sem data antes"); continue }
                description.isEmpty() -> { skipped += SkippedLine(line, "valor sem descrição"); continue }
                Values.isBalanceOrTotal(description) || isSummary(description) -> {
                    skipped += SkippedLine(line, "linha de saldo, total ou resumo"); pending.clear(); continue
                }
            }
            val token = first.value
            var cents = Values.amount(token.replace("−", "-"), decimalComma = true)
            if (cents == null) { skipped += SkippedLine(line, "valor ilegível"); continue }
            if (!marksOut(first) && !marksIn(first)) {
                val d = description.normalized()
                cents = when {
                    section != 0 -> section * abs(cents)
                    docMarksOut -> abs(cents)
                    docMarksIn -> -abs(cents)
                    longest(IN_WORDS, d) > longest(OUT_WORDS, d) -> abs(cents)
                    else -> -abs(cents) // fatura de cartão: tudo positivo, e quase tudo é compra
                }
            }
            if (cents == 0L) { skipped += SkippedLine(line, "valor zero"); continue }
            lines += StatementLine(date!!, Values.prettify(description), cents, null, line)
            pending.clear()
        }

        val isCard = looksLikeCard(text.normalized())
        val notes = listOf(
            "${pages.size} página(s), ${rawLines.size} linhas de texto",
            "Datas sem ano: ano tirado de \"${reference.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))}\"",
            if (isCard) "Parece fatura de cartão" else "Parece extrato de conta",
            when {
                docMarksOut -> "As saídas têm sinal no PDF (o resto é entrada)"
                docMarksIn -> "Só as entradas têm sinal no PDF (o resto é saída)"
                else -> "Sem sinais no PDF: entradas e saídas pelas palavras"
            },
        )
        return Statement(StatementFormat.PDF, lines, skipped, isCard = isCard, notes = notes)
    }

    /** Valor marcado como saída: "-12,50", "R$ -12,50", "12,50-", "12,50 D". */
    private fun marksOut(m: MatchResult): Boolean {
        val g = m.groupValues
        return g[1].trim().let { it.startsWith("-") || it.startsWith("−") } || g[3].isNotBlank() || g[5].isNotBlank() || g[6].trim() == "D"
    }

    /** Valor marcado como entrada: "+12,50", "12,50 C". */
    private fun marksIn(m: MatchResult): Boolean = m.groupValues[1].trim().startsWith("+") || m.groupValues[6].trim() == "C"

    private fun isSummary(description: String): Boolean {
        val n = description.normalized()
        return n.startsWith("limite") || n.startsWith("pagamento minimo") || n.startsWith("vencimento") ||
            n.startsWith("valor da fatura") || n.startsWith("fatura anterior") || n.startsWith("encargos") && n.contains("proxima")
    }
}
