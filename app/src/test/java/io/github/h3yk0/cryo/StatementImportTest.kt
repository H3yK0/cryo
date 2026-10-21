// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo

import io.github.h3yk0.cryo.data.DefaultData
import io.github.h3yk0.cryo.data.db.Account
import io.github.h3yk0.cryo.data.db.CreditCard
import io.github.h3yk0.cryo.data.db.Tx
import io.github.h3yk0.cryo.data.db.TxType
import io.github.h3yk0.cryo.domain.Snapshot
import io.github.h3yk0.cryo.importer.Columns
import io.github.h3yk0.cryo.importer.ImportPlanner
import io.github.h3yk0.cryo.importer.ImportReport
import io.github.h3yk0.cryo.importer.ImportTarget
import io.github.h3yk0.cryo.importer.JsonStatement
import io.github.h3yk0.cryo.importer.PdfStatement
import io.github.h3yk0.cryo.importer.PdfText
import io.github.h3yk0.cryo.importer.RowFlag
import io.github.h3yk0.cryo.importer.SampleFiles
import io.github.h3yk0.cryo.importer.Statement
import io.github.h3yk0.cryo.importer.StatementFormat
import io.github.h3yk0.cryo.importer.StatementLine
import io.github.h3yk0.cryo.importer.StatementReader
import io.github.h3yk0.cryo.importer.StatementReader.Result
import io.github.h3yk0.cryo.importer.Values
import io.github.h3yk0.cryo.importer.Xlsx
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/** Leitura de extratos de cada formato, com arquivos parecidos com os dos bancos brasileiros. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = TestApp::class)
class StatementImportTest {
    private val today = LocalDate.of(2026, 10, 3)
    private fun d(day: Int, month: Int = 10, year: Int = 2026) = LocalDate.of(year, month, day)

    private fun read(text: String, allowJson: Boolean = false, charset: java.nio.charset.Charset = Charsets.UTF_8): Result =
        StatementReader.read(text.toByteArray(charset), today, allowJson) { _, _ -> error("não é PDF") }

    private fun ok(r: Result): Statement {
        assertTrue("esperava sucesso, veio $r", r is Result.Ok)
        return (r as Result.Ok).statement
    }

    /* ------------------------------------ valores ------------------------------------ */

    @Test fun valoresNoJeitoBrasileiro() {
        assertEquals(123456L, Values.amount("R$ 1.234,56"))
        assertEquals(-123456L, Values.amount("-1.234,56"))
        assertEquals(-123456L, Values.amount("1.234,56-"))
        assertEquals(-123456L, Values.amount("(1.234,56)"))
        assertEquals(-1250L, Values.amount("12,50 D"))
        assertEquals(1250L, Values.amount("12,50 C"))
        assertEquals(-1000L, Values.amount("−R$ 10,00"))
        assertEquals(-1000L, Values.amount("R$ -10,00"))
        assertEquals(123456L, Values.amount("1234.56"))
        assertEquals(123400L, Values.amount("1.234")) // milhar
        assertEquals(150L, Values.amount("1.5"))
        assertEquals(123456L, Values.amount("1,234.56"))
        assertEquals(123400L, Values.amount("1,234", decimalComma = false))
        assertNull(Values.amount("abc"))
        assertNull(Values.amount(""))
        assertEquals(true, Values.guessDecimalComma(listOf("1.234,56", "-12,50", "300")))
        assertEquals(false, Values.guessDecimalComma(listOf("1234.56", "-12.50")))
    }

    @Test fun datasNoJeitoBrasileiro() {
        assertEquals(d(1), Values.date("01/10/2026"))
        assertEquals(d(1), Values.date("1/10/26"))
        assertEquals(d(1), Values.date("2026-10-01"))
        assertEquals(d(1), Values.date("2026-10-01T12:30:00"))
        assertEquals(d(1), Values.date("01.10.2026"))
        assertEquals(d(1), Values.date("01/10", reference = today))
        assertNull(Values.date("01/10"))
        assertEquals(d(1), Values.date("01 OUT 2026"))
        assertEquals(d(1), Values.date("1 de outubro de 2026"))
        assertEquals(d(5, 2), Values.date("05 FEV", reference = today))
        assertNull(Values.date("31/02/2026"))
        assertNull(Values.date("10.00"))
        assertEquals(d(1), Values.ofxDate("20261001120000[-3:BRT]"))
        assertEquals(LocalDate.of(2026, 10, 1), Values.excelDate(46296.0))
    }

    @Test fun textoUtf8OuWindows1252() {
        assertEquals("Água" to "UTF-8", Values.decode("Água".toByteArray(Charsets.UTF_8)))
        assertEquals("Água" to "UTF-8", Values.decode(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) + "Água".toByteArray()))
        assertEquals("Água" to "Windows-1252", Values.decode("Água".toByteArray(Charsets.ISO_8859_1)))
        assertEquals("Compra Padaria Pao", Values.prettify("COMPRA  PADARIA PAO"))
        assertEquals("PIX Enviado", Values.prettify("PIX ENVIADO"))
        assertEquals("Uber *Trip", Values.prettify("Uber *Trip")) // já tem minúsculas: fica como está
        assertEquals("Padaria Pão de Ouro", Values.prettify("PADARIA PÃO DE OURO"))
        assertEquals("Uber *Trip", Values.prettify("UBER *TRIP"))
        assertEquals("De Olho no Preço", Values.prettify("DE OLHO NO PREÇO"))
        assertTrue(Values.isBalanceOrTotal("SALDO ANTERIOR"))
        assertTrue(Values.isBalanceOrTotal("Total da fatura"))
        assertFalse(Values.isBalanceOrTotal("Total Distribuidora Ltda"))
    }

    /* -------------------------------------- OFX -------------------------------------- */

    private val ofxSgml = """
        OFXHEADER:100
        DATA:OFXSGML
        VERSION:102
        ENCODING:USASCII
        CHARSET:1252

        <OFX>
        <SIGNONMSGSRSV1><SONRS><STATUS><CODE>0<SEVERITY>INFO</STATUS><DTSERVER>20261003120000[-3:BRT]<LANGUAGE>POR<FI><ORG>Banco Teste<FID>001</FI></SONRS></SIGNONMSGSRSV1>
        <BANKMSGSRSV1><STMTTRNRS><TRNUID>1<STATUS><CODE>0<SEVERITY>INFO</STATUS>
        <STMTRS><CURDEF>BRL<BANKACCTFROM><BANKID>0001<ACCTID>12345-6<ACCTTYPE>CHECKING</BANKACCTFROM>
        <BANKTRANLIST><DTSTART>20261001<DTEND>20261003
        <STMTTRN><TRNTYPE>DEBIT<DTPOSTED>20261001120000[-3:BRT]<TRNAMT>-25.90<FITID>A1<CHECKNUM>1<MEMO>COMPRA PADARIA PAO DOURADO</STMTTRN>
        <STMTTRN><TRNTYPE>CREDIT<DTPOSTED>20261002<TRNAMT>3500.00<FITID>A2<MEMO>SALARIO EMPRESA X</STMTTRN>
        <STMTTRN><TRNTYPE>DEBIT<DTPOSTED>20261003<TRNAMT>150,00<FITID>A3<NAME>PIX ENVIADO<MEMO>PIX ENVIADO MARIA ÁGUA</STMTTRN>
        <STMTTRN><TRNTYPE>DEBIT<TRNAMT>-1.00<FITID>A4<MEMO>SEM DATA</STMTTRN>
        </BANKTRANLIST>
        <LEDGERBAL><BALAMT>1234.56<DTASOF>20261003</LEDGERBAL>
        </STMTRS></STMTTRNRS></BANKMSGSRSV1></OFX>
    """.trimIndent()

    @Test fun ofxAntigoDeConta() {
        val st = ok(read(ofxSgml, charset = Charsets.ISO_8859_1)) // bancos gravam em Windows-1252
        assertEquals(StatementFormat.OFX, st.format)
        assertFalse(st.isCard)
        assertEquals(listOf(-2590L, 350000L, -15000L), st.lines.map { it.amount })
        assertEquals(listOf(d(1), d(2), d(3)), st.lines.map { it.date })
        assertEquals(listOf("A1", "A2", "A3"), st.lines.map { it.id })
        assertEquals("Compra Padaria Pao Dourado", st.lines[0].description)
        assertEquals("PIX Enviado Maria Água", st.lines[2].description)
        assertEquals(1, st.skipped.size)
        assertEquals("Banco Teste · conta 12345-6", st.accountHint)
        assertEquals(123456L, st.balance)
        assertTrue(st.notes.any { it.contains("Windows-1252") })
    }

    @Test fun ofxNovoDeCartao() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <?OFX OFXHEADER="200" VERSION="220" SECURITY="NONE" OLDFILEUID="NONE" NEWFILEUID="NONE"?>
            <OFX><CREDITCARDMSGSRSV1><CCSTMTTRNRS><CCSTMTRS><CURDEF>BRL</CURDEF><CCACCTFROM><ACCTID>4111</ACCTID></CCACCTFROM>
            <BANKTRANLIST>
            <STMTTRN><TRNTYPE>DEBIT</TRNTYPE><DTPOSTED>20260915</DTPOSTED><TRNAMT>-89.90</TRNAMT><FITID>c1</FITID><MEMO>Netflix</MEMO></STMTTRN>
            <STMTTRN><TRNTYPE>CREDIT</TRNTYPE><DTPOSTED>20260920</DTPOSTED><TRNAMT>500.00</TRNAMT><FITID>c2</FITID><NAME>Pagamento &amp; recebido</NAME></STMTTRN>
            </BANKTRANLIST></CCSTMTRS></CCSTMTTRNRS></CREDITCARDMSGSRSV1></OFX>
        """.trimIndent()
        val st = ok(read(xml))
        assertTrue(st.isCard)
        assertEquals(listOf(-8990L, 50000L), st.lines.map { it.amount })
        assertEquals("Pagamento & recebido", st.lines[1].description)
        assertTrue(st.notes.first().contains("XML"))
    }

    /* -------------------------------------- CSV -------------------------------------- */

    @Test fun csvDaContaNubank() {
        val csv = """
            Data,Valor,Identificador,Descrição
            01/10/2026,-25.90,6512a0c1-0000,Compra no débito - PADARIA
            02/10/2026,3500.00,6512a0c1-0001,"Transferência recebida pelo Pix - EMPRESA X, LTDA"
        """.trimIndent()
        val st = ok(read(csv))
        assertEquals(StatementFormat.CSV, st.format)
        assertEquals(listOf(-2590L, 350000L), st.lines.map { it.amount })
        assertEquals("6512a0c1-0000", st.lines[0].id)
        assertEquals("Transferência recebida pelo Pix - EMPRESA X, LTDA", st.lines[1].description)
        assertTrue(st.notes.any { it.contains("vírgula") })
    }

    @Test fun csvDaFaturaNubank() {
        val csv = "date,title,amount\n2026-09-15,Netflix,39.90\n2026-09-20,Uber *Trip,25.50\n2026-09-25,Pagamento recebido,-500.00\n"
        val st = ok(read(csv))
        assertTrue(st.isCard)
        assertEquals(listOf(3990L, 2550L, -50000L), st.lines.map { it.amount })
        // compras positivas: ao importar num cartão, o Cryo sugere inverter os sinais
        assertTrue(ImportPlanner.suggestInvert(st, ImportTarget(cardId = 1)))
        assertFalse(ImportPlanner.suggestInvert(st, ImportTarget(accountId = 1)))
    }

    @Test fun csvComCabecalhoDepoisDoNomeDoBanco() {
        val csv = """
            Extrato de conta corrente
            Agência: 1234-5;Conta: 98765-4
            Período: 01/10/2026 a 03/10/2026

            Data;Lançamento;Detalhes;Nº documento;Valor;Tipo Lançamento
            01/10/2026;Saldo Anterior;;;1.000,00;C
            01/10/2026;Pix - Enviado;Maria;123;150,00;D
            02/10/2026;Pix - Recebido;João;124;1.234,56;C
            03/10/2026;Tarifa;;125;12,90;D
        """.trimIndent()
        val st = ok(read(csv, charset = Charsets.ISO_8859_1))
        assertEquals(listOf(-15000L, 123456L, -1290L), st.lines.map { it.amount })
        assertEquals("Pix - Enviado Maria", st.lines[0].description)
        assertEquals(1, st.skipped.size) // saldo anterior
        assertTrue(st.notes.any { it.contains("ponto e vírgula") })
        assertTrue(st.notes.any { it.startsWith("Cabeçalho na linha 5") })
    }

    @Test fun csvComEntradasESaidasSeparadas() {
        val csv = "Data;Descrição;Entrada;Saída;Saldo\n01/10/2026;Salário;3.000,00;;3.000,00\n02/10/2026;Mercado;;250,30;2.749,70\n"
        val st = ok(read(csv))
        assertEquals(listOf(300000L, -25030L), st.lines.map { it.amount })
    }

    @Test fun csvSemCabecalho() {
        val st = ok(read("01/10/2026;PADARIA;-12,50\n02/10/2026;SALARIO;3.000,00\n"))
        assertEquals(listOf(-1250L, 300000L), st.lines.map { it.amount })
        assertEquals("Padaria", st.lines[0].description)
    }

    @Test fun csvQueOCryoNaoEntendePedeAsColunas() {
        val r = read("nome;quantidade\nbanana;3\nmaçã;5\n")
        assertTrue(r is Result.NeedsColumns)
        r as Result.NeedsColumns
        assertEquals("nome", r.table[0][0].text)
        assertEquals(listOf("Coluna 1", "Coluna 2"), Columns.headers(r.table, null))
    }

    @Test fun colunasEscolhidasPelaPessoa() {
        // a pessoa diz qual coluna é o quê: vale o que ela escolheu
        val csv = "quando;o que;quanto\n01/10/2026;Padaria;-12,50\n"
        val map = io.github.h3yk0.cryo.importer.ColumnMap(headerRow = 0, date = 0, description = 1, amount = 2)
        val st = ok(StatementReader.read(csv.toByteArray(), today, false, columns = map) { _, _ -> emptyList() })
        assertEquals(-1250L, st.lines.single().amount)
    }

    /* ------------------------------------- planilha ------------------------------------- */

    @Test fun planilhaXlsx() {
        val bytes = Xlsx.write(
            listOf(
                listOf("Banco Teste - Extrato"),
                listOf("Período: outubro de 2026"),
                emptyList(),
                listOf("Data", "Histórico", "Valor (R$)", "Saldo (R$)"),
                listOf(d(1), "COMPRA MERCADO EXTRA", -230.45, 769.55),
                listOf(d(2), "PIX RECEBIDO JOAO", 100, 869.55),
                listOf(d(3), "Saldo do dia", null, 869.55),
            ),
        )
        assertTrue(Xlsx.looksLike(bytes))
        val st = ok(StatementReader.read(bytes, today, false) { _, _ -> emptyList() })
        assertEquals(StatementFormat.XLSX, st.format)
        assertEquals(listOf(-23045L, 10000L), st.lines.map { it.amount })
        assertEquals(listOf(d(1), d(2)), st.lines.map { it.date })
        assertEquals("Compra Mercado Extra", st.lines[0].description)
        // o mesmo conteúdo gera sempre o mesmo arquivo
        assertTrue(bytes.contentEquals(Xlsx.write(listOf(listOf("Banco Teste - Extrato"), listOf("Período: outubro de 2026"), emptyList(),
            listOf("Data", "Histórico", "Valor (R$)", "Saldo (R$)"), listOf(d(1), "COMPRA MERCADO EXTRA", -230.45, 769.55),
            listOf(d(2), "PIX RECEBIDO JOAO", 100, 869.55), listOf(d(3), "Saldo do dia", null, 869.55)))))
    }

    @Test fun planilhaAntigaEPaginaDaInternet() {
        val xls = byteArrayOf(0xD0.toByte(), 0xCF.toByte(), 0x11, 0xE0.toByte(), 0xA1.toByte(), 0xB1.toByte(), 0x1A, 0xE1.toByte(), 0, 0)
        val r = StatementReader.read(xls, today, false) { _, _ -> emptyList() }
        assertTrue((r as Result.Failed).message.contains(".xls"))
        assertTrue((read("<html><table><tr><td>1</td></tr></table></html>") as Result.Failed).message.contains("Não encontrei uma tabela"))
        assertTrue((read("<html><body>Acesse o internet banking</body></html>") as Result.Failed).message.contains("página da internet"))
        assertTrue((read("") as Result.Failed).message.contains("vazio"))
    }

    /* --------------------------------------- JSON --------------------------------------- */

    @Test fun jsonDeTestes() {
        val json = """{"cryoImport":1,"card":true,"account":"Cartão de teste","transactions":[
            {"date":"2026-10-01","description":"Padaria","amount":-12.5,"id":"t1"},
            {"date":"02/10/2026","description":"Estorno","amount":"30,00"},
            {"date":"ontem","description":"Data ruim","amount":-1},
            {"date":"2026-10-03","description":"Sem valor"}
        ]}"""
        assertTrue((read(json) as Result.Failed).message.contains("não parece um extrato")) // só no modo desenvolvedor
        val st = ok(read(json, allowJson = true))
        assertEquals(StatementFormat.JSON, st.format)
        assertTrue(st.isCard)
        assertEquals("Cartão de teste", st.accountHint)
        assertEquals(listOf(-1250L, 3000L), st.lines.map { it.amount })
        assertEquals(2, st.skipped.size)
        // o que o Cryo gera é lido de volta igual
        val again = JsonStatement.parse(JsonStatement.write(st.lines, "Cartão de teste", true))
        assertEquals(st.lines.map { Triple(it.date, it.amount, it.id) }, again.lines.map { Triple(it.date, it.amount, it.id) })
        assertTrue((read("""{"app":"Cryo","version":2}""", allowJson = true) as Result.Failed).message.contains("Restaurar backup"))
    }

    /* ---------------------------------------- PDF ---------------------------------------- */

    @Test fun pdfDeContaComSinais() {
        val text = """
            Extrato de conta - Outubro de 2026
            Data Histórico Valor Saldo
            01/10 SALDO ANTERIOR 1.000,00
            01/10 PIX ENVIADO MARIA -150,00 850,00
            02/10 SALARIO EMPRESA X 3.000,00 3.850,00
            03/10 COMPRA CARTAO PADARIA -12,50 3.837,50
        """.trimIndent()
        val st = PdfStatement.parse(listOf(text), today)
        assertEquals(listOf(-15000L, 300000L, -1250L), st.lines.map { it.amount })
        assertEquals(listOf(d(1), d(2), d(3)), st.lines.map { it.date })
        assertEquals("PIX Enviado Maria", st.lines[0].description)
        assertFalse(st.isCard)
    }

    @Test fun pdfAgrupadoPorDia() {
        val text = """
            01 OUT 2026 Total de entradas + 100,00
            Transferência recebida pelo Pix
            FULANO DE TAL
            100,00
            Total de saídas - 50,00
            Compra no débito
            PADARIA PAO
            50,00
            02 OUT 2026 Total de saídas - 20,00
            Pagamento de boleto
            CONTA DE LUZ
            20,00
        """.trimIndent()
        val st = PdfStatement.parse(listOf(text), today)
        assertEquals(listOf(10000L, -5000L, -2000L), st.lines.map { it.amount })
        assertEquals(listOf(d(1), d(1), d(2)), st.lines.map { it.date })
        assertEquals("Transferência recebida pelo Pix FULANO DE TAL", st.lines[0].description)
    }

    @Test fun pdfDeFaturaSemSinais() {
        val text = """
            Fatura do cartão - vencimento 10/10/2026
            15/09 NETFLIX 39,90
            20/09 UBER TRIP 25,50
            22/09 PAGAMENTO RECEBIDO 500,00
            Total da fatura 65,40
        """.trimIndent()
        val st = PdfStatement.parse(listOf(text), today)
        assertTrue(st.isCard)
        assertEquals(listOf(-3990L, -2550L, 50000L), st.lines.map { it.amount })
        assertEquals(d(15, 9), st.lines[0].date)
    }

    @Test fun pdfComSenhaEPdfSemTexto() {
        val bytes = "%PDF-1.7 ...".toByteArray()
        val pages: (ByteArray, String?) -> List<String> = { _, pw -> if (pw != "1234") throw PdfText.PasswordNeeded() else listOf("01/10/2026 PADARIA -12,50") }
        assertEquals(Result.NeedsPassword, StatementReader.read(bytes, today, false, pdfPages = pages))
        assertEquals(Result.NeedsPassword, StatementReader.read(bytes, today, false, password = "errada", pdfPages = pages))
        assertEquals(-1250L, ok(StatementReader.read(bytes, today, false, password = "1234", pdfPages = pages)).lines.single().amount)
        val empty = StatementReader.read(bytes, today, false) { _, _ -> listOf("   ") }
        assertTrue((empty as Result.Failed).message.contains("foto"))
    }

    /* ----------------------------------- conferência ----------------------------------- */

    private val categories = DefaultData.categories().mapIndexed { i, c -> c.copy(id = i + 1L) }
    private fun catId(name: String) = categories.first { it.name == name }.id
    private val account = Account(id = 1, name = "Conta", color = 0)
    private val card = CreditCard(id = 7, name = "Roxo", limitAmount = 500000, closingDay = 3, dueDay = 10, color = 0)

    @Test fun conferenciaComCategoriasERepetidas() {
        val st = ok(read(ofxSgml, charset = Charsets.ISO_8859_1))
        val existing = listOf(
            // lançado à mão um dia depois: parece repetida
            Tx(id = 50, type = TxType.EXPENSE, amount = 2590, date = d(2), description = "padaria", accountId = 1),
            // já importada antes (mesmo identificador do banco)
            Tx(id = 51, type = TxType.INCOME, amount = 350000, date = d(2), description = "Salário", accountId = 1, importKey = "a1|id:A2"),
        )
        val snap = Snapshot(accounts = listOf(account), categories = categories, txs = existing)
        val rows = ImportPlanner.plan(st, ImportTarget(accountId = 1), snap, emptyMap(), invert = false)
        assertEquals(listOf(RowFlag.LOOKS_DUPLICATE, RowFlag.IMPORTED_BEFORE, RowFlag.NONE), rows.map { it.flag })
        assertEquals(listOf(false, false, true), rows.map { it.selected })
        assertEquals(50L, rows[0].match?.id)
        assertEquals(catId("Alimentação"), rows[0].categoryId)
        assertEquals(catId("Salário"), rows[1].categoryId)
        assertEquals(listOf(TxType.EXPENSE, TxType.INCOME, TxType.EXPENSE), rows.map { it.type })
        assertEquals("a1|id:A3", rows[2].key)

        val tx = ImportPlanner.toTx(rows[2], ImportTarget(accountId = 1), null, now = 0)
        assertEquals(1L, tx.accountId)
        assertEquals(15000L, tx.amount)
        assertEquals("a1|id:A3", tx.importKey)
    }

    @Test fun faturaNoCartao() {
        val csv = "date,title,amount\n2026-09-15,Netflix,39.90\n2026-09-20,Uber *Trip,25.50\n2026-09-25,Pagamento recebido,-500.00\n2026-09-25,Pagamento recebido,-500.00\n"
        val st = ok(read(csv))
        val invoice = ImportPlanner.defaultInvoice(st, card)
        assertEquals(202610, invoice) // fecha dia 3, vence dia 10: compras até 02/10 vencem em outubro
        val target = ImportTarget(cardId = 7, invoiceYm = invoice)
        val snap = Snapshot(cards = listOf(card), categories = categories)
        val rows = ImportPlanner.plan(st, target, snap, emptyMap(), invert = true)
        assertEquals(listOf(TxType.EXPENSE, TxType.EXPENSE, TxType.INCOME, TxType.INCOME), rows.map { it.type })
        assertEquals(listOf(RowFlag.NONE, RowFlag.NONE, RowFlag.CARD_CREDIT, RowFlag.CARD_CREDIT), rows.map { it.flag })
        assertEquals(catId("Assinaturas"), rows[0].categoryId)
        assertEquals(catId("Transporte"), rows[1].categoryId)
        assertNotEquals(rows[2].key, rows[3].key) // duas linhas iguais continuam sendo duas
        val tx = ImportPlanner.toTx(rows[0], target.copy(invoiceYm = 202611), card, now = 0)
        assertEquals(7L, tx.cardId)
        assertNull(tx.accountId)
        assertEquals(202611, tx.invoiceYm) // a fatura escolhida vale para todas
        // sem fatura escolhida: cada compra vai para a fatura da sua data (15/09 fecha em 03/10, vence em outubro)
        val byDate = ImportPlanner.toTx(rows[0], target.copy(invoiceYm = null), card, now = 0)
        assertEquals(202610, byDate.invoiceYm)
    }

    @Test fun pagamentoDeFaturaNaConta() {
        val csv = "Data;Descrição;Valor\n05/10/2026;PAGAMENTO FATURA CARTAO;-500,00\n06/10/2026;PAGTO FATURA NUBANK;-300,00\n"
        val st = ok(read(csv))
        // o pagamento já registrado no Cryo (fatura do cartão) é reconhecido
        val paid = Tx(id = 60, type = TxType.CARD_PAYMENT, amount = 50000, date = d(5), accountId = 1, cardId = 7, invoiceYm = 202610)
        val snap = Snapshot(accounts = listOf(account), cards = listOf(card), categories = categories, txs = listOf(paid))
        val rows = ImportPlanner.plan(st, ImportTarget(accountId = 1), snap, emptyMap(), invert = false)
        assertEquals(listOf(RowFlag.LOOKS_DUPLICATE, RowFlag.INVOICE_PAYMENT), rows.map { it.flag })
        assertTrue(rows.none { it.selected })
    }

    @Test fun estornoNoCartaoEntraEPagamentoFicaDeFora() {
        val csv = "date,title,amount\n2026-09-15,Netflix,39.90\n2026-09-20,Estorno Netflix,-39.90\n2026-09-25,Pagamento recebido,-500.00\n"
        val st = ok(read(csv))
        val rows = ImportPlanner.plan(st, ImportTarget(cardId = 7, invoiceYm = 202610), Snapshot(cards = listOf(card), categories = categories), emptyMap(), invert = true)
        assertEquals(listOf(TxType.EXPENSE, TxType.INCOME, TxType.INCOME), rows.map { it.type })
        assertEquals(listOf(RowFlag.NONE, RowFlag.NONE, RowFlag.CARD_CREDIT), rows.map { it.flag })
        assertEquals(listOf(true, true, false), rows.map { it.selected })
        assertEquals(catId("Reembolsos"), rows[1].categoryId)
    }

    @Test fun pagamentoDeFaturaViraPagamentoDoCartao() {
        // fecha dia 3, vence dia 10: pagar em 05/10 quita a fatura de outubro; em 02/10 (antes de fechar), a de setembro
        assertEquals(202610, ImportPlanner.paidInvoice(card, d(5)))
        assertEquals(202609, ImportPlanner.paidInvoice(card, d(2)))
        // fecha dia 25 e vence dia 5 do mês seguinte: pagar em 03/11 quita a que vence em novembro
        val late = card.copy(closingDay = 25, dueDay = 5)
        assertEquals(202611, ImportPlanner.paidInvoice(late, d(3, 11)))

        val conta = ok(read("Data;Descrição;Valor\n05/10/2026;PAGAMENTO FATURA CARTAO;-500,00\n"))
        val row = ImportPlanner.plan(conta, ImportTarget(accountId = 1), Snapshot(accounts = listOf(account), cards = listOf(card), categories = categories), emptyMap(), false)
            .single().copy(pairedId = 7, selected = true)
        val tx = ImportPlanner.toTx(row, ImportTarget(accountId = 1), null, now = 0, cards = mapOf(7L to card))
        assertEquals(TxType.CARD_PAYMENT, tx.type)
        assertEquals(1L, tx.accountId)
        assertEquals(7L, tx.cardId)
        assertEquals(202610, tx.invoiceYm)
        assertNull(tx.categoryId)

        val fatura = ok(read("date,title,amount\n2026-10-05,Pagamento recebido,-500.00\n"))
        val credit = ImportPlanner.plan(fatura, ImportTarget(cardId = 7, invoiceYm = 202611), Snapshot(cards = listOf(card), categories = categories), emptyMap(), invert = true)
            .single().copy(pairedId = 1)
        val pay = ImportPlanner.toTx(credit, ImportTarget(cardId = 7, invoiceYm = 202611), card, now = 0)
        assertEquals(TxType.CARD_PAYMENT, pay.type)
        assertEquals(1L, pay.accountId)
        assertEquals(202610, pay.invoiceYm) // o pagamento é da fatura que fechou antes dele, não da fatura do arquivo
        // sem dizer de onde saiu, continua sendo só um crédito no cartão
        assertEquals(TxType.INCOME, ImportPlanner.toTx(credit.copy(pairedId = null), ImportTarget(cardId = 7, invoiceYm = 202611), card, now = 0).type)
    }

    @Test fun relatorioAnonimoNaoMostraNadaDaPessoa() {
        assertEquals("PIX ENVIADO XXXX XXXXX 99/99/9999 -9.999,99", ImportReport.anonymize("PIX ENVIADO JOAO SILVA 01/10/2026 -1.234,56"))
        assertEquals("Saldo anterior", ImportReport.anonymize("Saldo anterior"))
        assertEquals("arquivo.ofx", ImportReport.anonymousName("Extrato Joao Silva 2026.ofx"))
        assertEquals("arquivo", ImportReport.anonymousName("extrato"))

        val st = Statement(
            StatementFormat.OFX,
            listOf(StatementLine(d(1), "PADARIA DO JOAO", -1250, "ABC123", "<MEMO>PADARIA DO JOAO")),
            listOf(io.github.h3yk0.cryo.importer.SkippedLine("SALDO ANTERIOR 1.000,00", "linha de saldo ou total")),
            accountHint = "Banco 260 · conta 1234-5", balance = 100000, notes = listOf("Formato OFX (SGML)"),
        )
        val r = ImportReport("joao-silva.ofx", 2048, java.time.LocalDateTime.of(2026, 10, 3, 10, 0), st, imported = 1)
        val anon = r.text(anonymous = true, appVersion = "1.3")
        listOf("JOAO", "joao", "1234-5", "12,50", "ABC123", "1.000,00").forEach { assertFalse("vazou: $it", anon.contains(it)) }
        assertTrue(anon.contains("arquivo.ofx"))
        assertTrue(anon.contains("− R$ 99,99 · XXXXXXX DO XXXX · id XXX999"))
        assertTrue(anon.contains("linha de saldo ou total: SALDO ANTERIOR 9.999,99"))
        assertTrue(anon.contains("Formato OFX (SGML)"))
        val full = r.text(anonymous = false, appVersion = "1.3")
        assertTrue(full.contains("PADARIA DO JOAO"))
        assertTrue(full.contains("joao-silva.ofx"))
        assertTrue(full.contains("Importadas depois da conferência: 1"))
    }

    @Test fun arquivosDeExemploSaoLidosDoJeitoCerto() {
        val ofx = ok(StatementReader.read(SampleFiles.ofx(today).bytes, today, false) { _, _ -> emptyList() })
        assertEquals(13, ofx.lines.size)
        assertFalse(ofx.isCard)
        assertEquals(123456L + ofx.lines.sumOf { it.amount }, ofx.balance)
        assertEquals("Padaria Pão de Ouro", ofx.lines[2].description) // Windows-1252 com acento
        assertEquals(13, ok(StatementReader.read(SampleFiles.csvAccount(today).bytes, today, false) { _, _ -> emptyList() }).lines.size)
        val card = ok(StatementReader.read(SampleFiles.csvCard(today).bytes, today, false) { _, _ -> emptyList() })
        assertEquals(8, card.lines.size)
        assertTrue(card.isCard)
        val xlsx = ok(StatementReader.read(SampleFiles.xlsx(today).bytes, today, false) { _, _ -> emptyList() })
        assertEquals(ofx.lines.map { it.amount }, xlsx.lines.map { it.amount })
        assertEquals(ofx.lines.map { it.date }, xlsx.lines.map { it.date })
        assertEquals(2, xlsx.skipped.count { it.reason.contains("saldo") })
        val json = ok(StatementReader.read(SampleFiles.json(today).bytes, today, allowJson = true) { _, _ -> emptyList() })
        assertEquals(ofx.lines.map { it.amount }, json.lines.map { it.amount })
        val pdf = PdfStatement.parse(listOf(SampleFiles.pdfLines(today).joinToString("\n")), today)
        assertEquals(ofx.lines.map { it.amount }, pdf.lines.map { it.amount })
        assertEquals(ofx.lines.map { it.date }, pdf.lines.map { it.date })
    }

    @Test fun chaveDaImportacaoEDeCadaContaOuCartao() {
        val csv = "Data;Descrição;Valor;Identificador\n01/10/2026;TARIFA;-10,00;000000\n02/10/2026;TARIFA;-10,00;000000\n"
        val st = ok(read(csv))
        val rows = ImportPlanner.plan(st, ImportTarget(accountId = 1), Snapshot(categories = categories), emptyMap(), false)
        // o banco repetiu o identificador: as duas linhas continuam sendo duas
        assertEquals(listOf("a1|id:000000", "a1|id:000000#1"), rows.map { it.key })
        // o mesmo identificador em outra conta não é "já importada"
        val other = Tx(id = 70, type = TxType.EXPENSE, amount = 99900, date = d(20, 8), description = "Outra coisa", accountId = 2, importKey = "a2|id:000000")
        val again = ImportPlanner.plan(st, ImportTarget(accountId = 1), Snapshot(categories = categories, txs = listOf(other)), emptyMap(), false)
        assertEquals(listOf(RowFlag.NONE, RowFlag.NONE), again.map { it.flag })
        // na mesma conta, sim (as duas, cada uma pela sua chave)
        val mine = rows.mapIndexed { i, r -> ImportPlanner.toTx(r, ImportTarget(accountId = 1), null, now = 0).copy(id = 80L + i) }
        val third = ImportPlanner.plan(st, ImportTarget(accountId = 1), Snapshot(categories = categories, txs = mine), emptyMap(), false)
        assertEquals(listOf(RowFlag.IMPORTED_BEFORE, RowFlag.IMPORTED_BEFORE), third.map { it.flag })
    }

    @Test fun pagamentoImportadoPelaFaturaNaoEntraDeNovoPeloExtratoDaConta() {
        // pela fatura do cartão, "Pagamento recebido" virou pagamento saindo da conta 1
        val fatura = ok(read("date,title,amount\n2026-10-05,Pagamento recebido,-500.00\n"))
        val credit = ImportPlanner.plan(fatura, ImportTarget(cardId = 7, invoiceYm = 202611), Snapshot(cards = listOf(card), categories = categories), emptyMap(), invert = true)
            .single().copy(pairedId = 1)
        val paid = ImportPlanner.toTx(credit, ImportTarget(cardId = 7, invoiceYm = 202611), card, now = 0).copy(id = 90)
        // depois, o extrato da conta traz o mesmo pagamento (um dia depois): parece repetido
        val conta = ok(read("Data;Descrição;Valor\n06/10/2026;PAGAMENTO FATURA CARTAO;-500,00\n"))
        val row = ImportPlanner.plan(conta, ImportTarget(accountId = 1), Snapshot(accounts = listOf(account), cards = listOf(card), categories = categories, txs = listOf(paid)), emptyMap(), false).single()
        assertEquals(RowFlag.LOOKS_DUPLICATE, row.flag)
        assertEquals(90L, row.match?.id)
        assertFalse(row.selected)
    }

    @Test fun mesmoPeriodoEmOutroFormatoPareceRepetidoMasOCafezinhoDeTodoDiaNao() {
        val ofx = ok(read(ofxSgml, charset = Charsets.ISO_8859_1))
        val target = ImportTarget(accountId = 1)
        val first = ImportPlanner.plan(ofx, target, Snapshot(categories = categories), emptyMap(), false)
        val saved = first.mapIndexed { i, r -> ImportPlanner.toTx(r, target, null, now = 0).copy(id = 100L + i) }
        // o mesmo extrato, agora em CSV (sem identificador): cada linha parece repetida
        val csv = ofx.lines.joinToString("\n", prefix = "Data;Descrição;Valor\n") {
            "${it.date.dayOfMonth.toString().padStart(2, '0')}/${it.date.monthValue.toString().padStart(2, '0')}/${it.date.year};${it.description};${io.github.h3yk0.cryo.domain.Money.format(it.amount, symbol = false)}"
        }
        val fromCsv = ok(read(csv))
        val rows = ImportPlanner.plan(fromCsv, target, Snapshot(categories = categories, txs = saved), emptyMap(), false)
        assertTrue(rows.all { it.flag == RowFlag.LOOKS_DUPLICATE })
        // OFX do dia seguinte, com um café igual ao de ontem: identificadores diferentes, então é outro café
        val cafe = { day: Int, id: String -> "<STMTTRN><TRNTYPE>DEBIT<DTPOSTED>202610${day.toString().padStart(2, '0')}<TRNAMT>-5.00<FITID>$id<MEMO>CAFE DA ESQUINA</STMTTRN>" }
        val day1 = ok(read("OFXHEADER:100\n<OFX><BANKMSGSRSV1><STMTTRNRS><STMTRS><BANKTRANLIST>${cafe(1, "C1")}</BANKTRANLIST></STMTRS></STMTTRNRS></BANKMSGSRSV1></OFX>"))
        val day2 = ok(read("OFXHEADER:100\n<OFX><BANKMSGSRSV1><STMTTRNRS><STMTRS><BANKTRANLIST>${cafe(2, "C2")}</BANKTRANLIST></STMTRS></STMTTRNRS></BANKMSGSRSV1></OFX>"))
        val cafe1 = ImportPlanner.toTx(ImportPlanner.plan(day1, target, Snapshot(categories = categories), emptyMap(), false).single(), target, null, now = 0).copy(id = 200)
        assertEquals(RowFlag.NONE, ImportPlanner.plan(day2, target, Snapshot(categories = categories, txs = listOf(cafe1)), emptyMap(), false).single().flag)
    }

    @Test fun datasSemAnoNaViradaDoAno() {
        val jan = LocalDate.of(2027, 1, 5)
        // fatura de janeiro com compras de dezembro: o ano vem do vencimento e dezembro é do ano anterior
        val pdf = PdfStatement.parse(listOf("Fatura do cartão - vencimento 10/01/2027\nPagamento mínimo 50,00\n05/12 LOJA CENTRO 100,00\n03/01 PADARIA 20,00"), jan)
        assertEquals(listOf(LocalDate.of(2026, 12, 5), LocalDate.of(2027, 1, 3)), pdf.lines.map { it.date })
        // CSV sem ano nenhum, importado no começo de janeiro
        val st = ok(StatementReader.read("Data;Descrição;Valor\n28/12;MERCADO;-50,00\n02/01;FARMACIA;-30,00\n".toByteArray(), jan, false) { _, _ -> emptyList() })
        assertEquals(listOf(LocalDate.of(2026, 12, 28), LocalDate.of(2027, 1, 2)), st.lines.map { it.date })
        assertEquals(LocalDate.of(2026, 12, 28), Values.date("28/12", jan))
        assertEquals(LocalDate.of(2027, 1, 20), Values.date("20/01", jan)) // pouco depois da referência: mesmo ano
    }

    @Test fun linhaDeResumoNaoViraCabecalho() {
        val csv = "Data: 03/10/2026;Créditos: R\$ 5.230,00;Débitos: R\$ 4.100,00\n" +
            "Data;Descrição;Valor\n01/10/2026;PIX RECEBIDO ANA;50,00\n02/10/2026;PADARIA;-12,50\n"
        val st = ok(read(csv))
        assertEquals(listOf(5000L, -1250L), st.lines.map { it.amount })
        assertEquals(listOf("PIX Recebido Ana", "Padaria"), st.lines.map { it.description })
        val anon = ImportReport.of("Extrato Maria.Souza", 100, java.time.LocalDateTime.of(2026, 10, 3, 9, 0), st, null).text(true, "1.3")
        assertFalse(anon.contains("Souza"))
        assertFalse(anon.contains("5.230"))
        assertTrue(anon.contains("arquivo"))
    }

    @Test fun notasDoRelatorioAnonimoNaoMostramOQueVeioDoArquivo() {
        val st = Statement(StatementFormat.CSV, emptyList(), notes = listOf("Descrição: \"Pagamentos da Maria\"", "Datas sem ano: ano tirado de \"05/10/2026\""))
        val anon = ImportReport.of("a.csv", 1, java.time.LocalDateTime.of(2026, 10, 3, 9, 0), st, null).text(true, "1.3")
        assertTrue(anon.contains("Descrição: \"Pagamentos da Xxxxx\""))
        assertTrue(anon.contains("\"99/99/9999\""))
    }

    @Test fun pdfDeContaSemSinaisNaoConfundeFaturaNemCompraNoCredito() {
        val text = """
            Extrato do período
            01/10/2026 PIX RECEBIDO ANA 50,00
            02/10/2026 PAGAMENTO DE FATURA CARTAO 1.250,00
            03/10/2026 COMPRA CREDITO LOJA 100,00
            04/10/2026 PAGAMENTO RECEBIDO 10,00
        """.trimIndent()
        val st = PdfStatement.parse(listOf(text), today)
        assertEquals(listOf(5000L, -125000L, -10000L, 1000L), st.lines.map { it.amount })
        assertFalse(st.isCard)
    }

    @Test fun pdfQueMarcaSoAsEntradasTrataOResto() {
        // Banco que põe "+" (ou "C") só nas entradas: o que vem sem sinal é saída, mesmo sem palavra de compra.
        val plus = """
            Extrato de conta - Outubro de 2026
            01/10 SALARIO EMPRESA X +3.000,00
            02/10 PADARIA PAO 12,50
            03/10 TED JOAO +200,00
            04/10 MERCADO BOM 80,00
        """.trimIndent()
        val st = PdfStatement.parse(listOf(plus), today)
        assertEquals(listOf(300000L, -1250L, 20000L, -8000L), st.lines.map { it.amount })
        assertTrue(st.notes.contains("Só as entradas têm sinal no PDF (o resto é saída)"))

        val credit = "Extrato de conta\n01/10/2026 SALARIO EMPRESA X 3.000,00 C\n02/10/2026 PADARIA PAO 12,50"
        assertEquals(listOf(300000L, -1250L), PdfStatement.parse(listOf(credit), today).lines.map { it.amount })

        // Com saídas marcadas no mesmo PDF, continua valendo a regra de antes: sem sinal é entrada.
        val both = "Extrato de conta\n01/10/2026 PIX ENVIADO MARIA -150,00\n02/10/2026 TED JOAO +200,00\n03/10/2026 REEMBOLSO LOJA 30,00"
        assertEquals(listOf(-15000L, 20000L, 3000L), PdfStatement.parse(listOf(both), today).lines.map { it.amount })
    }

    @Test fun xlsQueNaVerdadeEPaginaComTabela() {
        val html = """
            <html><head><meta charset="utf-8"><style>td { font-size: 10px }</style></head><body>
            <table>
              <tr><td colspan="4">Banco Exemplo - Extrato</td></tr>
              <tr><th>Data</th><th>Hist&oacute;rico</th><th>Valor</th><th>Saldo</th></tr>
              <tr><td>01/10/2026</td><td>PIX RECEBIDO&nbsp;ANA</td><td>50,00</td><td>1.050,00</td></tr>
              <tr><td>02/10/2026</td><td>COMPRA <b>PADARIA</b></td><td>-12,50</td><td>1.037,50</td>
              <tr><td>02/10/2026</td><td>SALDO DO DIA</td><td></td><td>1.037,50</td></tr>
            </table></body></html>
        """.trimIndent()
        val st = ok(read(html))
        assertEquals(StatementFormat.HTML, st.format)
        assertEquals(listOf(5000L, -1250L), st.lines.map { it.amount })
        assertEquals(listOf("PIX Recebido Ana", "Compra Padaria"), st.lines.map { it.description })
    }

    @Test fun extratoDeExemploEReconhecido() {
        assertTrue(SampleFiles.isSample(ok(StatementReader.read(SampleFiles.ofx(today).bytes, today, false) { _, _ -> emptyList() })))
        assertTrue(SampleFiles.isSample(ok(StatementReader.read(SampleFiles.csvCard(today).bytes, today, false) { _, _ -> emptyList() })))
        assertTrue(SampleFiles.isSample(ok(StatementReader.read(SampleFiles.xlsx(today).bytes, today, false) { _, _ -> emptyList() })))
        assertFalse(SampleFiles.isSample(ok(read(ofxSgml, charset = Charsets.ISO_8859_1))))
    }

    @Test fun copiaDoModoDeTesteFicaMarcada() {
        val snap = Snapshot(accounts = listOf(account), categories = categories)
        assertTrue(io.github.h3yk0.cryo.data.Backup.import(io.github.h3yk0.cryo.data.Backup.export(snap, exportedAt = 1, testMode = true)).testMode)
        assertFalse(io.github.h3yk0.cryo.data.Backup.import(io.github.h3yk0.cryo.data.Backup.export(snap, exportedAt = 1)).testMode)
    }

    @Test fun categoriaAprendidaComVoce() {
        val st = ok(read("Data;Descrição;Valor\n01/10/2026;LOJA DO ZE;-40,00\n"))
        val learned = mapOf("loja" to catId("Pets"))
        val rows = ImportPlanner.plan(st, ImportTarget(accountId = 1), Snapshot(categories = categories), learned, invert = false)
        assertEquals(catId("Pets"), rows.single().categoryId)
        assertEquals(catId("Compras"), ImportPlanner.plan(st, ImportTarget(accountId = 1), Snapshot(categories = categories), emptyMap(), false).single().categoryId)
        val other = ok(read("Data;Descrição;Valor\n01/10/2026;ABC XYZ 123;-40,00\n"))
        val unknown = ImportPlanner.plan(other, ImportTarget(accountId = 1), Snapshot(categories = categories), emptyMap(), invert = false)
        assertEquals(catId("Outros gastos"), unknown.single().categoryId)
    }
}
