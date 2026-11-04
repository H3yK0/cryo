// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.importer

import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import io.github.h3yk0.cryo.domain.Money
import io.github.h3yk0.cryo.domain.normalized
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.abs

/**
 * Extratos de exemplo (inventados) em cada formato, para testar a importação no modo desenvolvedor sem usar
 * arquivos de banco de verdade. As datas acompanham o dia de hoje.
 */
object SampleFiles {
    class Sample(val name: String, val mime: String, val label: String, val bytes: ByteArray)

    private data class Move(val daysAgo: Long, val description: String, val cents: Long)

    /** Conta corrente: salário, Pix, compras, uma conta de luz, o pagamento da fatura e um rendimento. */
    private val ACCOUNT = listOf(
        Move(28, "SALÁRIO ACME LTDA", 420000),
        Move(27, "PIX ENVIADO MARIA SILVA", -15000),
        Move(25, "PADARIA PÃO DE OURO", -1850),
        Move(24, "SUPERMERCADO BOM PREÇO", -31247),
        Move(20, "UBER *TRIP", -2390),
        Move(18, "CONTA DE LUZ ENERGIA", -18932),
        Move(15, "PAGAMENTO FATURA CARTÃO", -125000),
        Move(12, "FARMÁCIA SÃO JOÃO", -4280),
        Move(10, "PIX RECEBIDO JOÃO PEREIRA", 8000),
        Move(7, "POSTO BR COMBUSTÍVEL", -20000),
        Move(5, "NETFLIX.COM", -3990),
        Move(3, "RENDIMENTO POUPANÇA", 1234),
        Move(1, "IFOOD *RESTAURANTE", -5490),
    )

    /** Fatura do cartão: compras positivas (como os bancos exportam), o pagamento e um estorno negativos. */
    private val CARD = listOf(
        Move(26, "Mercado Livre", 12990),
        Move(22, "Spotify", 2190),
        Move(19, "Uber *Trip", 1840),
        Move(16, "Amazon - Parcela 2/5", 8980),
        Move(14, "Pagamento recebido", -125000),
        Move(9, "iFood", 4750),
        Move(6, "Estorno Mercado Livre", -1500),
        Move(2, "Cinema Centro", 6400),
    )

    private const val OPENING = 123456L

    private fun plain(s: String) = s.normalized().replace(Regex("[^a-z0-9]+"), " ").trim()
    /**
     * Descrição e valor de cada exemplo. Só a descrição não basta: "Uber *Trip", "Spotify" e "iFood" também aparecem
     * nas faturas de verdade, e um mês com quase só isso seria tomado por exemplo (e recusado).
     */
    private val SAMPLE_KEYS: Set<Pair<String, Long>> by lazy { (ACCOUNT + CARD).map { plain(it.description) to abs(it.cents) }.toSet() }

    /**
     * O extrato é um dos exemplos inventados? (Eles só podem ser importados no modo de teste: assim, um exemplo salvo
     * numa pasta nunca vai parar nas contas de verdade.)
     */
    fun isSample(st: Statement): Boolean {
        if (st.lines.size < 5) return false
        return st.lines.count { (plain(it.description) to abs(it.amount)) in SAMPLE_KEYS } >= st.lines.size * 0.8
    }

    private val BR = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    private val OFX_DATE = DateTimeFormatter.ofPattern("yyyyMMdd")

    /** "-18.50" (ponto nos centavos, como no OFX e no CSV do Nubank). */
    private fun dot(cents: Long): String = String.format(java.util.Locale.ROOT, "%.2f", cents / 100.0)

    private fun brl(cents: Long): String = Money.format(abs(cents), symbol = false).let { if (cents < 0) "-$it" else it }

    fun all(today: LocalDate): List<Sample> = listOf(
        ofx(today), csvAccount(today), csvCard(today), xlsx(today), json(today),
    ) + listOfNotNull(runCatching { pdf(today) }.getOrNull())

    /** OFX antigo (SGML) em Windows-1252, como muitos bancos ainda mandam. */
    fun ofx(today: LocalDate): Sample {
        val items = ACCOUNT.mapIndexed { i, m ->
            val d = today.minusDays(m.daysAgo).format(OFX_DATE)
            """
            <STMTTRN>
            <TRNTYPE>${if (m.cents < 0) "DEBIT" else "CREDIT"}
            <DTPOSTED>${d}120000[-3:BRT]
            <TRNAMT>${dot(m.cents)}
            <FITID>EX${today.year}${(i + 1).toString().padStart(4, '0')}
            <MEMO>${m.description}
            </STMTTRN>
            """.trimIndent()
        }
        val balance = OPENING + ACCOUNT.sumOf { it.cents }
        val text = buildString {
            append("OFXHEADER:100\r\nDATA:OFXSGML\r\nVERSION:102\r\nSECURITY:NONE\r\nENCODING:USASCII\r\nCHARSET:1252\r\n")
            append("COMPRESSION:NONE\r\nOLDFILEUID:NONE\r\nNEWFILEUID:NONE\r\n\r\n")
            append("<OFX>\r\n<SIGNONMSGSRSV1><SONRS><STATUS><CODE>0<SEVERITY>INFO</STATUS><DTSERVER>${today.format(OFX_DATE)}<LANGUAGE>POR")
            append("<FI><ORG>Banco Exemplo<FID>999</FI></SONRS></SIGNONMSGSRSV1>\r\n")
            append("<BANKMSGSRSV1><STMTTRNRS><TRNUID>1<STATUS><CODE>0<SEVERITY>INFO</STATUS><STMTRS><CURDEF>BRL\r\n")
            append("<BANKACCTFROM><BANKID>999<ACCTID>12345-6<ACCTTYPE>CHECKING</BANKACCTFROM>\r\n")
            append("<BANKTRANLIST><DTSTART>${today.minusDays(30).format(OFX_DATE)}<DTEND>${today.format(OFX_DATE)}\r\n")
            items.forEach { append(it.replace("\n", "\r\n")).append("\r\n") }
            append("</BANKTRANLIST>\r\n<LEDGERBAL><BALAMT>${dot(balance)}")
            append("<DTASOF>${today.format(OFX_DATE)}</LEDGERBAL>\r\n</STMTRS></STMTTRNRS></BANKMSGSRSV1>\r\n</OFX>\r\n")
        }
        return Sample("cryo-exemplo-conta.ofx", "application/octet-stream", "OFX de conta", text.toByteArray(charset("windows-1252")))
    }

    /** CSV de conta no jeito do Nubank: vírgula separando colunas e ponto nos centavos. */
    fun csvAccount(today: LocalDate): Sample {
        val text = buildString {
            append("Data,Valor,Identificador,Descrição\n")
            ACCOUNT.forEachIndexed { i, m ->
                append("${today.minusDays(m.daysAgo).format(BR)},${dot(m.cents)},ex-${i + 1},\"${m.description.lowercase().replaceFirstChar { it.titlecase() }}\"\n")
            }
        }
        return Sample("cryo-exemplo-conta.csv", "text/csv", "CSV de conta", text.toByteArray(Charsets.UTF_8))
    }

    /** CSV de fatura no jeito do Nubank: compras positivas, pagamento e estorno negativos. */
    fun csvCard(today: LocalDate): Sample {
        val text = buildString {
            append("date,title,amount\n")
            CARD.forEach { m ->
                append("${today.minusDays(m.daysAgo)},${m.description},${dot(m.cents)}\n")
            }
        }
        return Sample("cryo-exemplo-fatura.csv", "text/csv", "CSV de fatura", text.toByteArray(Charsets.UTF_8))
    }

    /** Planilha com o nome do banco e o período antes da tabela, e linhas de saldo no meio (como muitos bancos fazem). */
    fun xlsx(today: LocalDate): Sample {
        var balance = OPENING
        val rows = ArrayList<List<Any?>>()
        rows += listOf("Banco Exemplo S.A.")
        rows += listOf("Extrato de conta corrente")
        rows += listOf("Período: ${today.minusDays(30).format(BR)} a ${today.format(BR)}")
        rows += emptyList<Any?>()
        rows += listOf("Data", "Lançamento", "Detalhes", "Valor (R$)", "Saldo (R$)")
        rows += listOf(today.minusDays(30), "Saldo anterior", null, null, OPENING / 100.0)
        ACCOUNT.forEach { m ->
            balance += m.cents
            val (title, detail) = m.description.split(' ', limit = 2).let { it[0] to it.getOrElse(1) { "" } }
            rows += listOf(today.minusDays(m.daysAgo), title, detail, m.cents / 100.0, balance / 100.0)
        }
        rows += listOf(today, "Saldo do dia", null, null, balance / 100.0)
        return Sample(
            "cryo-exemplo-planilha.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "Planilha XLSX",
            Xlsx.write(rows),
        )
    }

    /** JSON de testes (formato documentado em docs/IMPORTACAO.md). */
    fun json(today: LocalDate): Sample {
        val lines = ACCOUNT.mapIndexed { i, m ->
            StatementLine(today.minusDays(m.daysAgo), m.description, m.cents, "json-${i + 1}")
        }
        val text = JsonStatement.write(lines, account = "Banco de teste", card = false)
        return Sample("cryo-exemplo-teste.json", "application/json", "JSON de teste", text.toByteArray(Charsets.UTF_8))
    }

    /** PDF de extrato com texto de verdade (como o que o banco gera), para testar a leitura de PDF. */
    fun pdf(today: LocalDate): Sample {
        val lines = pdfLines(today)
        val doc = PdfDocument()
        try {
            val page = doc.startPage(PdfDocument.PageInfo.Builder(595, 842, 1).create())
            val paint = Paint().apply { textSize = 10f; typeface = Typeface.MONOSPACE; isAntiAlias = true }
            var y = 48f
            for (line in lines) {
                page.canvas.drawText(line, 40f, y, paint)
                y += 16f
            }
            doc.finishPage(page)
            val out = ByteArrayOutputStream()
            doc.writeTo(out)
            return Sample("cryo-exemplo-extrato.pdf", "application/pdf", "PDF de conta", out.toByteArray())
        } finally {
            doc.close()
        }
    }

    /** O texto do PDF de exemplo, linha por linha. */
    fun pdfLines(today: LocalDate): List<String> {
        val lines = ArrayList<String>()
        lines += "Banco Exemplo S.A."
        lines += "Extrato de conta corrente"
        lines += "Período: ${today.minusDays(30).format(BR)} a ${today.format(BR)}"
        lines += ""
        lines += "Data        Descrição                              Valor (R$)"
        lines += "${today.minusDays(30).format(BR)}  SALDO ANTERIOR  ${brl(OPENING)}"
        ACCOUNT.forEach { m -> lines += "${today.minusDays(m.daysAgo).format(BR)}  ${m.description}  ${brl(m.cents)}" }
        lines += "${today.format(BR)}  SALDO DO DIA  ${brl(OPENING + ACCOUNT.sumOf { it.cents })}"
        return lines
    }
}
