// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo

import io.github.h3yk0.cryo.data.db.Category
import io.github.h3yk0.cryo.data.db.CategoryKind
import io.github.h3yk0.cryo.domain.AppLanguage
import io.github.h3yk0.cryo.domain.AppLocale
import io.github.h3yk0.cryo.domain.Snapshot
import io.github.h3yk0.cryo.importer.ImportPlanner
import io.github.h3yk0.cryo.importer.ImportTarget
import io.github.h3yk0.cryo.importer.RowFlag
import io.github.h3yk0.cryo.importer.Statement
import io.github.h3yk0.cryo.importer.StatementReader
import io.github.h3yk0.cryo.importer.StatementReader.Result
import io.github.h3yk0.cryo.importer.Values
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.charset.Charset
import java.time.LocalDate
import java.util.Locale

/**
 * Extratos de bancos de outros países (PRD: RF-IMP-21), todos inventados: colunas na língua de cada um, datas com o mês
 * antes do dia, valores "1,234.56", sem centavos (iene, won), arquivos em Shift_JIS, CP949 e GB18030.
 */
class ImportCountriesTest {
    init { JvmStrings.install() }

    private val today = LocalDate.of(2026, 10, 3)

    @After fun back() = AppLocale.reset()

    private fun app(lang: AppLanguage, locale: Locale, currency: String) = AppLocale.set(lang, locale, currency)

    private fun read(text: String, charset: Charset = Charsets.UTF_8): Statement {
        val r = StatementReader.read(text.toByteArray(charset), today, false) { _, _ -> error("não é PDF") }
        assertTrue("esperava sucesso, veio $r", r is Result.Ok)
        return (r as Result.Ok).statement
    }

    private fun pdf(vararg pages: String): Statement {
        val r = StatementReader.read("%PDF-1.4 inventado".toByteArray(), today, false) { _, _ -> pages.toList() }
        assertTrue("esperava sucesso, veio $r", r is Result.Ok)
        return (r as Result.Ok).statement
    }

    private fun Statement.rows() = lines.map { Triple(it.date, it.description, it.amount) }

    @Test fun estadosUnidosMesAntesDoDia() {
        app(AppLanguage.EN, Locale.US, "USD")
        val st = read(
            """
            Details,Posting Date,Description,Amount,Type,Balance,Check or Slip #
            DEBIT,10/01/2026,"STARBUCKS STORE 123",-5.75,DEBIT_CARD,"1,234.56",,
            CREDIT,09/30/2026,"PAYROLL ACME INC","2,500.00",ACH_CREDIT,"1,240.31",,
            DEBIT,09/13/2026,"RENT PAYMENT",-1500.00,ACH_DEBIT,-1259.69,,
            """.trimIndent(),
        )
        assertEquals(
            listOf(
                Triple(LocalDate.of(2026, 10, 1), "Starbucks Store 123", -575L),
                Triple(LocalDate.of(2026, 9, 30), "Payroll Acme INC", 250_000L),
                Triple(LocalDate.of(2026, 9, 13), "Rent Payment", -150_000L),
            ),
            st.rows(),
        )
    }

    @Test fun reinoUnidoDiaAntesDoMes() {
        app(AppLanguage.EN, Locale.UK, "GBP")
        val st = read("Date,Description,Amount\n15/09/2026,TESCO STORES,-23.50\n01/10/2026,SALARY,1800.00\n")
        assertEquals(LocalDate.of(2026, 9, 15), st.lines[0].date)
        assertEquals(LocalDate.of(2026, 10, 1), st.lines[1].date)
        assertEquals(180_000L, st.lines[1].amount)
    }

    @Test fun alemanha() {
        app(AppLanguage.DE, Locale.GERMANY, "EUR")
        val st = read("Buchungstag;Verwendungszweck;Betrag (EUR)\n30.09.2026;REWE SAGT DANKE;-45,90\n01.10.2026;Gehalt Oktober;2.500,00\n")
        assertEquals(
            listOf(
                Triple(LocalDate.of(2026, 9, 30), "Rewe Sagt Danke", -4590L),
                Triple(LocalDate.of(2026, 10, 1), "Gehalt Oktober", 250_000L),
            ),
            st.rows(),
        )
    }

    @Test fun francaComEspacoNoMilhar() {
        app(AppLanguage.FR, Locale.FRANCE, "EUR")
        val st = read("Date;Libellé;Montant\n30/09/2026;CARTE X1234 CARREFOUR;-52,30\n01/10/2026;VIR SEPA SALAIRE;2 450,00\n")
        assertEquals(listOf(-5230L, 245_000L), st.lines.map { it.amount })
    }

    @Test fun japaoEmShiftJisSemCentavos() {
        app(AppLanguage.JA, Locale.JAPAN, "JPY")
        val st = read(
            "日付,摘要,出金,入金,残高\n2026/09/30,セブンイレブン,580,,\"100,000\"\n2026/10/01,給与,,\"250,000\",\"350,000\"\n2026/10/02,家賃,\"85,000\",,\"265,000\"\n",
            Charset.forName("windows-31j"),
        )
        assertEquals(
            listOf(
                Triple(LocalDate.of(2026, 9, 30), "セブンイレブン", -58_000L),
                Triple(LocalDate.of(2026, 10, 1), "給与", 25_000_000L),
                Triple(LocalDate.of(2026, 10, 2), "家賃", -8_500_000L),
            ),
            st.rows(),
        )
    }

    @Test fun coreiaEmCp949() {
        app(AppLanguage.KO, Locale.KOREA, "KRW")
        val st = read(
            "거래일시,적요,출금액,입금액,잔액\n2026.09.30 12:30,스타벅스,\"5,000\",0,\"995,000\"\n2026.10.01 09:00,급여,0,\"3,000,000\",\"3,995,000\"\n",
            Charset.forName("x-windows-949"),
        )
        assertEquals(
            listOf(
                Triple(LocalDate.of(2026, 9, 30), "스타벅스", -500_000L),
                Triple(LocalDate.of(2026, 10, 1), "급여", 300_000_000L),
            ),
            st.rows(),
        )
    }

    @Test fun chinaEmGb18030() {
        app(AppLanguage.ZH, Locale.CHINA, "CNY")
        val st = read("交易日期,交易摘要,收入,支出,余额\n2026-09-30,美团外卖,,35.50,1000.00\n2026-10-01,工资,8000.00,,9000.00\n", Charset.forName("GB18030"))
        assertEquals(listOf(-3550L, 800_000L), st.lines.map { it.amount })
        assertEquals("美团外卖", st.lines[0].description)
    }

    @Test fun indiaComMilharIndiano() {
        app(AppLanguage.HI, Locale.forLanguageTag("hi-IN"), "INR")
        val st = read(
            "Txn Date,Narration,Withdrawal Amt.,Deposit Amt.,Closing Balance\n30/09/26,UPI-SWIGGY-ORDER,450.00,,\"1,23,456.78\"\n01/10/26,SALARY OCT,,\"1,20,000.00\",\"2,43,006.78\"\n",
        )
        assertEquals(
            listOf(
                Triple(LocalDate.of(2026, 9, 30), "UPI-Swiggy-Order", -45_000L),
                Triple(LocalDate.of(2026, 10, 1), "Salary Oct", 12_000_000L),
            ),
            st.rows(),
        )
    }

    @Test fun moedaEstrangeiraNaColunaFicaDeFora() {
        // Fatura em reais com a coluna do valor em dólar: vale a de reais. Num app em dólar, a de dólar é a certa.
        val csv = "Data;Descrição;Valor em US$;Valor em R$\n01/10/2026;AMAZON US;10,00;55,00\n"
        assertEquals(5500L, read(csv).lines.single().amount)
        app(AppLanguage.EN, Locale.US, "USD")
        assertEquals(listOf(-1500L), read("Date,Description,Amount (USD)\n10/01/2026,UBER,-15.00\n").lines.map { it.amount })
    }

    @Test fun pdfDosEstadosUnidos() {
        app(AppLanguage.EN, Locale.US, "USD")
        val st = pdf(
            """
            Example Bank
            Account Statement - Checking
            Opening Balance 1,000.00
            09/28 STARBUCKS 5.75
            09/30 PAYROLL DEPOSIT 2,500.00
            10/01 SHELL OIL 40.00
            Closing Balance 3,454.25
            """.trimIndent(),
        )
        assertEquals(
            listOf(
                Triple(LocalDate.of(2026, 9, 28), "STARBUCKS", -575L),
                Triple(LocalDate.of(2026, 9, 30), "PAYROLL DEPOSIT", 250_000L),
                Triple(LocalDate.of(2026, 10, 1), "SHELL OIL", -4000L),
            ),
            st.lines.map { Triple(it.date, it.description.uppercase(), it.amount) },
        )
    }

    @Test fun pdfDoJapaoEmIenes() {
        app(AppLanguage.JA, Locale.JAPAN, "JPY")
        val st = pdf("ご利用明細\n2026年9月28日 セブンイレブン 580円\n2026年9月30日 アマゾン 3,980円\n")
        assertEquals(
            listOf(
                Triple(LocalDate.of(2026, 9, 28), "セブンイレブン", -58_000L),
                Triple(LocalDate.of(2026, 9, 30), "アマゾン", -398_000L),
            ),
            st.rows(),
        )
    }

    @Test fun pdfDaAlemanhaComMesPorExtenso() {
        app(AppLanguage.DE, Locale.GERMANY, "EUR")
        val st = pdf("Kontoauszug\n28. Sep. 2026 Lastschrift Stadtwerke -89,90\n30. Sep. 2026 Gutschrift Gehalt 2.500,00\n")
        assertEquals(listOf(-8990L, 250_000L), st.lines.map { it.amount })
        assertEquals(LocalDate.of(2026, 9, 28), st.lines[0].date)
    }

    @Test fun datasEValoresDeVariosPaises() {
        assertEquals(LocalDate.of(2026, 10, 1), Values.date("Oct 1, 2026"))
        assertEquals(LocalDate.of(2026, 9, 15), Values.date("15-Sep-2026"))
        assertEquals(LocalDate.of(2026, 10, 1), Values.date("1er octobre 2026"))
        assertEquals(LocalDate.of(2026, 9, 15), Values.date("15. September 2026"))
        assertEquals(LocalDate.of(2026, 9, 15), Values.date("2026. 9. 15."))
        assertEquals(LocalDate.of(2026, 9, 15), Values.date("2026年9月15日"))
        assertEquals(LocalDate.of(2026, 9, 15), Values.date("9月15日", reference = today))
        assertEquals(LocalDate.of(2026, 9, 15), Values.date("9월 15일", reference = today))
        assertEquals(LocalDate.of(2026, 3, 4), Values.date("03/04/2026", monthFirst = true))
        assertEquals(LocalDate.of(2026, 4, 3), Values.date("03/04/2026"))
        // Mês 13 não existe: a ordem pedida não vale e fica a outra.
        assertEquals(LocalDate.of(2026, 9, 30), Values.date("30/09/2026", monthFirst = true))
        assertEquals(LocalDate.of(2026, 1, 15), Values.date("15 ene 2026"))
        assertEquals(LocalDate.of(2026, 7, 4), Values.date("4 luglio 2026"))
        assertEquals(LocalDate.of(2026, 6, 4), Values.date("4 juin 2026"))
        assertEquals(LocalDate.of(2026, 7, 4), Values.date("4 juillet 2026"))

        assertEquals(123_456L, Values.amount("$1,234.56"))
        assertEquals(123_456L, Values.amount("1.234,56 €"))
        assertEquals(12_345_600L, Values.amount("₹1,23,456.00"))
        assertEquals(-50_000L, Values.amount("500.00 Dr"))
        assertEquals(50_000L, Values.amount("500.00 Cr"))
        assertEquals(123_450L, Values.amount("1'234.50"))
        assertEquals(123_456L, Values.amount("1 234,56"))
        assertEquals(150L, Values.amount("1,500")) // em português, "1,500" é um real e cinquenta
        app(AppLanguage.JA, Locale.JAPAN, "JPY")
        assertEquals(150_000L, Values.amount("¥1,500"))
        assertEquals(150_000L, Values.amount("1,500"))
        assertEquals(-58_000L, Values.amount("-580円"))
    }

    @Test fun pagamentoDeFaturaECategoriaEmOutrasLinguas() {
        app(AppLanguage.EN, Locale.US, "USD")
        val cats = listOf(
            Category(id = 1, name = "Groceries", kind = CategoryKind.EXPENSE, icon = "cart", color = 0, keywords = "whole foods,market"),
            Category(id = 2, name = "Other", kind = CategoryKind.EXPENSE, icon = "more", color = 0),
        )
        val st = read("Date,Description,Amount\n10/01/2026,CHASE CREDIT CRD AUTOPAY,-850.00\n10/02/2026,WHOLE FOODS MKT 123,-80.00\n")
        val rows = ImportPlanner.plan(st, ImportTarget(accountId = 1), Snapshot(categories = cats), emptyMap(), invert = false)
        assertEquals(RowFlag.INVOICE_PAYMENT, rows[0].flag)
        assertEquals(1L, rows[1].categoryId)

        app(AppLanguage.JA, Locale.JAPAN, "JPY")
        val ja = listOf(
            Category(id = 1, name = "食費", kind = CategoryKind.EXPENSE, icon = "food", color = 0, keywords = "セブンイレブン,ローソン"),
            Category(id = 2, name = "その他", kind = CategoryKind.EXPENSE, icon = "more", color = 0),
        )
        val st2 = read("日付,摘要,金額\n2026/10/01,セブンイレブン新宿店,-580\n2026/10/02,カード代金,\"-45,000\"\n")
        val rows2 = ImportPlanner.plan(st2, ImportTarget(accountId = 1), Snapshot(categories = ja), emptyMap(), invert = false)
        assertEquals(1L, rows2[0].categoryId)
        assertEquals(RowFlag.INVOICE_PAYMENT, rows2[1].flag)
    }

    /* ---------------- 1.6 (RF-LNG-11): Rússia, Ucrânia, países árabes e Israel ---------------- */

    /** Rússia: arquivo em windows-1251, milhar com espaço e duas colunas de valor (vale a da moeda do cartão). */
    @Test fun russiaEmWindows1251ComValorNaMoedaDoCartao() {
        app(AppLanguage.RU, Locale.forLanguageTag("ru-RU"), "RUB")
        val st = read(
            "Дата операции;Описание;Сумма операции;Сумма в валюте карты\n" +
                "30.09.2026;ПЯТЕРОЧКА 1234;-1 234,56;-1 234,56\n" +
                "01.10.2026;AMAZON.COM (USD);-10,00;-950,00\n" +
                "02.10.2026;Зарплата за сентябрь;80 000,00;80 000,00\n",
            Charset.forName("windows-1251"),
        )
        assertEquals(listOf(LocalDate.of(2026, 9, 30), LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 2)), st.lines.map { it.date })
        assertEquals(listOf(-123_456L, -95_000L, 8_000_000L), st.lines.map { it.amount })
    }

    /** Ucrânia: colunas em ucraniano, gastos e entradas separados, vírgula decimal. */
    @Test fun ucraniaComDebitoECreditoSeparados() {
        app(AppLanguage.UK, Locale.forLanguageTag("uk-UA"), "UAH")
        val st = read(
            "Дата операції;Опис операції;Списання;Зарахування;Залишок\n" +
                "30.09.2026;АТБ-маркет;245,50;;9 754,50\n" +
                "01.10.2026;Зарплата;;30 000,00;39 754,50\n",
            Charset.forName("windows-1251"),
        )
        assertEquals(listOf(-24_550L, 3_000_000L), st.lines.map { it.amount })
        assertEquals(LocalDate.of(2026, 10, 1), st.lines[1].date)
    }

    /**
     * Arábia Saudita: em UTF-8, algarismos arábico-índicos (٠–٩) e os separadores ٫ e ٬; em windows-1256 (que não tem
     * esses algarismos), os 0–9 de sempre.
     */
    @Test fun arabiaSauditaComAlgarismosArabes() {
        app(AppLanguage.AR, Locale.forLanguageTag("ar-SA-u-nu-latn"), "SAR")
        val st = read(
            "التاريخ,الوصف,مدين,دائن,الرصيد\n" +
                "٣٠/٠٩/٢٠٢٦,سوبرماركت الدانوب,١٥٠٫٧٥,,٢٬٣٤٩٫٢٥\n" +
                "٠١/١٠/٢٠٢٦,راتب شهر أكتوبر,,١٠٬٠٠٠٫٠٠,١٢٬٣٤٩٫٢٥\n",
        )
        assertEquals(listOf(LocalDate.of(2026, 9, 30), LocalDate.of(2026, 10, 1)), st.lines.map { it.date })
        assertEquals(listOf(-15_075L, 1_000_000L), st.lines.map { it.amount })
        val legacy = read(
            "التاريخ,الوصف,المبلغ\n30/09/2026,سوبرماركت الدانوب,-150.75\n01/10/2026,راتب,10000.00\n",
            Charset.forName("windows-1256"),
        )
        assertEquals(listOf(-15_075L, 1_000_000L), legacy.lines.map { it.amount })
        assertEquals("سوبرماركت الدانوب", legacy.lines[0].description)
    }

    /**
     * Israel: windows-1255, marcas invisíveis de direção (\u200F) em volta do valor, e a compra em dólar com o valor
     * cobrado em shekels na coluna "סכום חיוב" (vale a cobrada).
     */
    @Test fun israelEmWindows1255ComMarcasDeDirecao() {
        app(AppLanguage.HE, Locale.forLanguageTag("he-IL"), "ILS")
        val st = read(
            "תאריך עסקה,שם בית העסק,סכום העסקה,סכום חיוב\n" +
                "30/09/2026,שופרסל דיל,\u200F-120.50 ₪,\u200F-120.50 ₪\n" +
                "01/10/2026,NETFLIX.COM,-10.00 $,\u200F-37.20 ₪\n",
            Charset.forName("windows-1255"),
        )
        assertEquals(listOf(-12_050L, -3_720L), st.lines.map { it.amount })
        assertEquals(LocalDate.of(2026, 9, 30), st.lines[0].date)
    }

    @Test fun datasEValoresDasLinguasNovas() {
        assertEquals(LocalDate.of(2026, 9, 15), Values.date("15 сентября 2026 г."))
        assertEquals(LocalDate.of(2026, 9, 15), Values.date("15 сент. 2026"))
        assertEquals(LocalDate.of(2026, 9, 15), Values.date("15 вересня 2026 р."))
        assertEquals(LocalDate.of(2026, 9, 15), Values.date("15 سبتمبر 2026"))
        assertEquals(LocalDate.of(2026, 9, 15), Values.date("15 בספטמבר 2026"))
        app(AppLanguage.RU, Locale.forLanguageTag("ru-RU"), "RUB")
        assertEquals(123_456L, Values.amount("1 234,56 ₽"))
        assertEquals(123_456L, Values.amount("1 234,56 руб."))
        app(AppLanguage.UK, Locale.forLanguageTag("uk-UA"), "UAH")
        assertEquals(-24_550L, Values.amount("-245,50 грн"))
        app(AppLanguage.AR, Locale.forLanguageTag("ar-SA-u-nu-latn"), "SAR")
        assertEquals(234_925L, Values.amount("٢٬٣٤٩٫٢٥ ر.س"))
        app(AppLanguage.HE, Locale.forLanguageTag("he-IL"), "ILS")
        assertEquals(12_050L, Values.amount("\u200F₪ 120.50"))
    }
}
