// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.importer

import io.github.h3yk0.cryo.R
import io.github.h3yk0.cryo.domain.Res
import io.github.h3yk0.cryo.domain.normalized
import java.time.LocalDate
import kotlin.math.abs

/**
 * Extrato em PDF: cada banco desenha o seu de um jeito, então aqui o Cryo procura padrões no texto:
 * uma data (no começo da linha ou como título de um grupo de dias), a descrição e um valor no formato brasileiro.
 * Por isso a tela de conferência é tão importante para PDF.
 */
object PdfStatement {
    // O "-" depois do valor ("12,50-") é dele; um "-" separado por espaço e colado no número seguinte é do próximo valor
    // (o saldo negativo da coluna ao lado: "PIX RECEBIDO 50,00 -1.150,00").
    private val MONEY = Regex("""(?<![\d,.])([-−+]\s*)?(R\$\s*)?([-−]\s*)?(\d{1,3}(?:\.\d{3})*,\d{2})([-−](?!\d)|\s+[-−](?!\s*(?:R\$\s*)?\d))?(\s*[DC](?![A-Za-z]))?""")
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
            throw StatementException(Res.s(R.string.importer_nao_encontrei_texto_nesse))
        }
        val reference = Values.reference(FULL_DATE.findAll(text.normalized()).mapNotNull { Values.date(it.value) }, today)

        // 1ª passada: as linhas que viram movimentação, ainda sem decidir o sinal de quem não tem.
        class Found(val line: String, val date: LocalDate, val description: String, val value: MatchResult, val cents: Long, val section: Int)
        val found = ArrayList<Found>()
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
                date == null -> { skipped += SkippedLine(line, Res.s(R.string.importer_sem_data_antes)); continue }
                description.isEmpty() -> { skipped += SkippedLine(line, Res.s(R.string.importer_valor_sem_descricao)); continue }
                Values.isBalanceOrTotal(description) || isSummary(description) -> {
                    skipped += SkippedLine(line, Res.s(R.string.importer_linha_saldo_total_ou)); pending.clear(); continue
                }
            }
            val cents = Values.amount(first.value.replace("−", "-"), decimalComma = true)
            if (cents == null) { skipped += SkippedLine(line, Res.s(R.string.importer_valor_ilegivel)); continue }
            if (cents == 0L) { skipped += SkippedLine(line, Res.s(R.string.importer_valor_zero)); continue }
            found += Found(line, date!!, description, first, cents, section)
            pending.clear()
        }

        // O PDF marca as saídas com sinal (- ou D)? Então o que não tem sinal é entrada. Se marca só as entradas
        // (+ ou C), é o contrário: o que não tem sinal é saída. Conta só o valor de cada movimentação: um saldo negativo
        // (cheque especial) ou um "Total de saídas -3.000,00" não dizem como o banco marca as movimentações.
        val docMarksOut = found.any { marksOut(it.value) }
        val docMarksIn = !docMarksOut && found.any { marksIn(it.value) }

        // 2ª passada: o sinal de quem não tem.
        val lines = found.map { f ->
            val cents = if (marksOut(f.value) || marksIn(f.value)) f.cents else {
                val d = f.description.normalized()
                when {
                    f.section != 0 -> f.section * abs(f.cents)
                    docMarksOut -> abs(f.cents)
                    docMarksIn -> -abs(f.cents)
                    longest(IN_WORDS, d) > longest(OUT_WORDS, d) -> abs(f.cents)
                    else -> -abs(f.cents) // fatura de cartão: tudo positivo, e quase tudo é compra
                }
            }
            StatementLine(f.date, Values.prettify(f.description), cents, null, f.line)
        }

        val isCard = looksLikeCard(text.normalized())
        val notes = listOf(
            Res.s(R.string.importer_pagina_linhas_texto, pages.size, rawLines.size),
            Res.s(R.string.importer_datas_sem_ano_ano, reference.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))),
            if (isCard) Res.s(R.string.importer_parece_fatura_cartao) else Res.s(R.string.importer_parece_extrato_conta),
            when {
                docMarksOut -> Res.s(R.string.importer_saidas_tem_sinal_pdf)
                docMarksIn -> Res.s(R.string.importer_so_entradas_tem_sinal)
                else -> Res.s(R.string.importer_sem_sinais_pdf_entradas)
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
