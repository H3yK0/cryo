// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.importer

import io.github.h3yk0.cryo.R
import io.github.h3yk0.cryo.domain.AppLanguage
import io.github.h3yk0.cryo.domain.AppLocale
import io.github.h3yk0.cryo.domain.Dates
import io.github.h3yk0.cryo.domain.PhraseLang
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
    /** Como o PDF escreve os valores: "1.234,56" (Brasil, Europa), "1,234.56" (EUA, Ásia) ou sem centavos ("1,500円"). */
    private enum class Style { COMMA, DOT, WHOLE }

    /**
     * Valor com o sinal ao redor. O "-" depois do valor ("12,50-") é dele; um "-" separado por espaço e colado no
     * número seguinte é do próximo valor (o saldo negativo da coluna ao lado: "PIX RECEBIDO 50,00 -1.150,00").
     * Grupos: 1 sinal antes, 2 moeda, 3 sinal depois da moeda, 4 número, 5 "-" depois, 6 D/C/DR/CR.
     */
    private fun money(style: Style): Regex {
        val number = when (style) {
            Style.COMMA -> """\d{1,3}(?:\.\d{3})*,\d{2}|\d{1,3}(?:[ \u00A0\u202F]\d{3})+,\d{2}"""
            Style.DOT -> """\d{1,2}(?:,\d{2})+,\d{3}\.\d{2}|\d{1,3}(?:,\d{3})*\.\d{2}"""
            Style.WHOLE -> """\d{1,3}(?:,\d{3})+(?![\d.])|\d{1,7}(?=\s*[円원元])"""
        }
        val symbol = """R\$|US\$|\$|€|£|¥|￥|₩|₹|Rs\.?"""
        return Regex(
            """(?<![\d,.])([-−+]\s*)?((?:$symbol)\s*)?([-−]\s*)?($number)(?:\s*(?:€|円|원|元|EUR\b))?""" +
                """([-−](?!\d)|\s+[-−](?!\s*(?:(?:$symbol)\s*)?\d))?(\s*(?:CR|DR|Cr|Dr|[DC])(?![A-Za-z]))?""",
        )
    }

    private const val MONTHS = "jan|fev|feb|mar|abr|apr|avr|mai|may|mag|jun|jui|giu|jul|lug|ago|aug|aou|set|sep|out|oct|okt|ott|nov|dez|dec|dic|ene|gen"

    /** Data no começo da linha: "05/10", "05/10/2026", "5 out", "5 de outubro", "2026-10-05", "10月5日", "Oct 5" (inglês). */
    private fun leadDate(monthFirstText: Boolean) = Regex(
        """^(\d{4}[/.-]\d{1,2}[/.-]\d{1,2}|\d{1,2}[/.-]\d{1,2}(?:[/.-]\d{2,4})?|""" +
            """\d{1,2}(?:st|nd|rd|th|er|º)?\.?\s*(?:de\s+)?(?:$MONTHS)[a-zçéûä]*\.?(?:\s+(?:de\s+)?\d{4})?|""" +
            """(?:\d{4}\s*[年년]\s*)?\d{1,2}\s*[月월]\s*\d{1,2}\s*[日일号]?""" +
            (if (monthFirstText) """|(?:$MONTHS)[a-z]*\.?\s+\d{1,2}(?:st|nd|rd|th)?,?(?:\s+\d{4})?""" else "") +
            """)(?=\s|$)""",
        RegexOption.IGNORE_CASE,
    )

    /** Datas completas no texto (com o ano), para descobrir o ano das datas que vêm sem ele. */
    private val FULL_DATE = Regex(
        """\b\d{1,2}[/.-]\d{1,2}[/.-]\d{4}\b|\b\d{4}-\d{2}-\d{2}\b|\b\d{1,2}\s+(?:de\s+)?(?:jan|fev|mar|abr|mai|jun|jul|ago|set|out|nov|dez)[a-z]*\.?\s+(?:de\s+)?\d{4}\b|""" +
            """\b\d{1,2}[\s-](?:$MONTHS)[a-z]*\.?[\s-]\d{4}\b|\b(?:$MONTHS)[a-z]*\.?\s+\d{1,2},?\s+\d{4}\b|""" +
            """\d{4}\s*[年년]\s*\d{1,2}\s*[月월]\s*\d{1,2}|\b\d{4}\.\s?\d{1,2}\.\s?\d{1,2}\b""",
    )

    // Para PDFs sem sinal nos valores: palavras que indicam saída ou entrada. Vale a mais específica (a mais longa):
    // "pagamento recebido" é entrada, "pagamento de fatura" é saída, "compra credito" (no crédito) é saída.
    private val OUT_WORDS = listOf(
        "compra", "pagamento", "pgto", "pagto", "pag ", "pix enviado", "enviado", "enviada", "transferencia enviada", "saque",
        "tarifa", "debito", "boleto", "fatura", "juros", "iof", "anuidade", "seguro", "parcela", "aplicacao",
        // outras línguas
        "purchase", "payment", "withdrawal", "debit", "fee", "charge", "transfer to", "sent", "pos ", "atm", "check ",
        "compra", "pago", "retiro", "cargo", "comision", "transferencia emitida", "achat", "paiement", "prelevement",
        "retrait", "frais", "virement emis", "lastschrift", "kartenzahlung", "zahlung", "abhebung", "entgelt",
        "uberweisung an", "acquisto", "prelievo", "addebito", "commissione", "支払", "引落", "出金", "購入", "출금",
        "결제", "지급", "支出", "消费", "支付", "निकासी", "भुगतान",
    ).map { it.normalized() }
    private val IN_WORDS = listOf(
        "recebid", "deposito", "salario", "estorno", "estornad", "rendimento", "reembolso", "devolucao", "pix recebido",
        "transferencia recebida", "resgate", "cashback", "pagamento recebido", "credito em conta", "credito de salario",
        // outras línguas
        "deposit", "credit", "refund", "salary", "payroll", "interest", "transfer from", "received", "reversal",
        "payment received", "payment thank you", "abono", "ingreso", "nomina", "devolucion", "transferencia recibida",
        "virement recu", "remboursement", "salaire", "avoir", "gutschrift", "gehalt", "erstattung", "eingang", "zinsen",
        "accredito", "stipendio", "rimborso", "bonifico ricevuto", "versamento", "入金", "給与", "返金", "振込入金", "입금",
        "급여", "환급", "收入", "存入", "工资", "退款", "जमा", "वेतन",
    ).map { it.normalized() }

    private val IN_SECTIONS = listOf(
        "deposits", "deposits and additions", "deposits and other credits", "credits", "abonos", "ingresos", "credits",
        "gutschriften", "accrediti", "entrate",
    )
    private val OUT_SECTIONS = listOf(
        "withdrawals", "withdrawals and other debits", "checks paid", "electronic withdrawals", "debits", "cargos",
        "retiros", "lastschriften", "addebiti", "uscite",
    )

    private fun longest(words: List<String>, text: String): Int = words.filter { text.contains(it) }.maxOfOrNull { it.length } ?: 0

    /** Fatura de cartão ou extrato de conta, pelo que o texto traz (um extrato de conta também fala em "fatura" e "cartão"). */
    private val CARD_WORDS = listOf(
        "fatura" to 1, "vencimento" to 1, "pagamento minimo" to 2, "limite" to 1, "total a pagar" to 1, "total da fatura" to 2,
        "statement balance" to 1, "minimum payment" to 2, "payment due date" to 1, "credit limit" to 1, "new balance" to 1,
        "pago minimo" to 2, "fecha limite de pago" to 1, "limite de credito" to 1, "paiement minimum" to 2,
        "date d'echeance" to 1, "mindestzahlung" to 2, "kreditkartenabrechnung" to 2, "kreditlimit" to 1,
        "pagamento minimo" to 2, "ご利用明細" to 2, "お支払金額" to 1, "お支払日" to 1, "결제예정금액" to 2, "청구금액" to 1,
        "결제일" to 1, "最低还款额" to 2, "本期应还" to 2, "信用卡" to 1, "न्यूनतम देय" to 2,
    ).map { (w, p) -> w.normalized() to p }
    private val ACCOUNT_WORDS = listOf(
        "saldo anterior" to 2, "saldo do dia" to 2, "saldo disponivel" to 1, "saldo final" to 1, "extrato de conta" to 2,
        "conta corrente" to 1, "agencia" to 1, "opening balance" to 2, "closing balance" to 2, "account statement" to 1,
        "checking" to 1, "savings account" to 1, "sort code" to 1, "iban" to 1, "saldo inicial" to 2, "cuenta corriente" to 1,
        "solde precedent" to 2, "releve de compte" to 2, "compte courant" to 1, "kontoauszug" to 2, "girokonto" to 1,
        "alter kontostand" to 2, "conto corrente" to 1, "saldo iniziale" to 2, "普通預金" to 2, "残高" to 1, "입출금" to 1,
        "잔액" to 1, "통장" to 1, "账户明细" to 2, "余额" to 1, "खाता विवरण" to 2,
    ).map { (w, p) -> w.normalized() to p }

    /** Fatura de cartão ou extrato de conta, pelo que o texto traz (um extrato de conta também fala em "fatura" e "cartão"). */
    private fun looksLikeCard(n: String): Boolean {
        val card = CARD_WORDS.sumOf { (w, p) -> if (n.contains(w)) p else 0 }
        val account = ACCOUNT_WORDS.sumOf { (w, p) -> if (n.contains(w)) p else 0 }
        return card >= 3 && card > account
    }

    /** Estilo dos valores do documento: o que mais aparece; empate, o costume da língua (e da moeda, sem centavos). */
    private fun styleOf(text: String): Style {
        val comma = Regex("""\d,\d{2}(?![\d])""").findAll(text).count()
        val dot = Regex("""\d\.\d{2}(?![\d])""").findAll(text).count()
        return when {
            comma > dot -> Style.COMMA
            dot > comma -> Style.DOT
            AppLocale.fractionDigits == 0 -> Style.WHOLE
            PhraseLang.current.language in setOf(AppLanguage.PT_BR, AppLanguage.ES, AppLanguage.FR, AppLanguage.DE, AppLanguage.IT) -> Style.COMMA
            else -> Style.DOT
        }
    }

    fun parse(pages: List<String>, today: LocalDate): Statement {
        val text = pages.joinToString("\n")
        val rawLines = text.lines().map { it.replace(' ', ' ').replace(Regex("[ \\t]+"), " ").trim() }.filter { it.isNotEmpty() }
        if (rawLines.isEmpty()) {
            throw StatementException(Res.s(R.string.importer_nao_encontrei_texto_nesse))
        }
        val style = styleOf(text)
        val moneyRx = money(style)
        val leadRx = leadDate(PhraseLang.current.language == AppLanguage.EN)
        // "10/31" (EUA) ou "31/10": decide pelas datas do próprio PDF (ou pelo costume da língua do app).
        val monthFirst = Values.monthFirst(rawLines.asSequence().mapNotNull { leadRx.find(it)?.value })
        val reference = Values.reference(
            FULL_DATE.findAll(text.normalized()).mapNotNull { Values.date(it.value, monthFirst = monthFirst) },
            today,
        )

        // 1ª passada: as linhas que viram movimentação, ainda sem decidir o sinal de quem não tem.
        class Found(val line: String, val date: LocalDate, val description: String, val value: MatchResult, val cents: Long, val section: Int)
        val found = ArrayList<Found>()
        val skipped = ArrayList<SkippedLine>()
        var currentDate: LocalDate? = null
        var section = 0 // 1 = entradas, -1 = saídas (extratos que agrupam assim)
        val pending = ArrayList<String>()

        for (line in rawLines) {
            var rest = line
            leadRx.find(line)?.let { m ->
                val d = Values.date(m.value, reference, monthFirst)
                if (d != null) {
                    currentDate = d
                    rest = line.substring(m.range.last + 1).trim()
                    pending.clear()
                }
            }
            val norm = rest.normalized()
            when {
                norm.contains("total de entradas") || norm == "entradas" || norm.startsWith("creditos") ||
                    IN_SECTIONS.any { norm == it || norm.startsWith("total $it") } -> section = 1
                norm.contains("total de saidas") || norm == "saidas" || norm.startsWith("debitos") ||
                    OUT_SECTIONS.any { norm == it || norm.startsWith("total $it") } -> section = -1
            }
            val money = moneyRx.findAll(rest).toList()
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
            val cents = Values.amount(first.value.replace("−", "-"), decimalComma = style == Style.COMMA)
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
            Res.s(R.string.importer_datas_sem_ano_ano, Dates.full(reference)),
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
        return g[1].trim().let { it.startsWith("-") || it.startsWith("−") } || g[3].isNotBlank() || g[5].isNotBlank() ||
            g[6].trim().uppercase() in setOf("D", "DR")
    }

    /** Valor marcado como entrada: "+12,50", "12,50 C", "12.50 CR". */
    private fun marksIn(m: MatchResult): Boolean =
        m.groupValues[1].trim().startsWith("+") || m.groupValues[6].trim().uppercase() in setOf("C", "CR")

    private val SUMMARY = listOf(
        "minimum payment", "payment due", "credit limit", "available credit", "previous balance", "new balance",
        "statement balance", "pago minimo", "limite de credito", "fecha limite", "paiement minimum", "mindestzahlung",
        "kreditlimit", "pagamento minimo", "最低还款", "본期", "결제예정", "お支払",
    ).map { it.normalized() }

    private fun isSummary(description: String): Boolean {
        val n = description.normalized()
        return n.startsWith("limite") || n.startsWith("pagamento minimo") || n.startsWith("vencimento") ||
            n.startsWith("valor da fatura") || n.startsWith("fatura anterior") || n.startsWith("encargos") && n.contains("proxima") ||
            SUMMARY.any { n.startsWith(it) }
    }
}
