// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo

import io.github.h3yk0.cryo.data.db.Account
import io.github.h3yk0.cryo.data.db.AccountType
import io.github.h3yk0.cryo.data.db.Category
import io.github.h3yk0.cryo.data.db.CategoryKind
import io.github.h3yk0.cryo.data.db.CreditCard
import io.github.h3yk0.cryo.data.db.TxType
import io.github.h3yk0.cryo.domain.AppLanguage
import io.github.h3yk0.cryo.domain.ParseContext
import io.github.h3yk0.cryo.domain.ParsedEntry
import io.github.h3yk0.cryo.domain.PhraseParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.util.Locale

/**
 * Registro por frase nas outras 9 línguas (PRD: RF-REG-09). As categorias aqui são do próprio teste (com as
 * palavras-chave de cada língua), para o teste não depender das traduções.
 */
class PhraseLanguagesTest {
    init { JvmStrings.install() }

    private val today = LocalDate.of(2026, 9, 30) // quarta-feira
    private val yesterday = today.minusDays(1)

    private fun cats(food: String, transport: String, home: String, salary: String) = listOf(
        Category(id = 1, name = "food", kind = CategoryKind.EXPENSE, icon = "food", color = 0, keywords = food),
        Category(id = 2, name = "transport", kind = CategoryKind.EXPENSE, icon = "car", color = 0, keywords = transport),
        Category(id = 3, name = "home", kind = CategoryKind.EXPENSE, icon = "home", color = 0, keywords = home),
        Category(id = 4, name = "more", kind = CategoryKind.EXPENSE, icon = "more", color = 0),
        Category(id = 5, name = "salary", kind = CategoryKind.INCOME, icon = "salary", color = 0, keywords = salary),
        Category(id = 6, name = "more in", kind = CategoryKind.INCOME, icon = "more", color = 0),
    )

    private fun accounts(main: String, savings: String, cash: String) = listOf(
        Account(id = 1, name = main, type = AccountType.CHECKING, color = 0),
        Account(id = 2, name = savings, type = AccountType.SAVINGS, color = 0),
        Account(id = 3, name = cash, type = AccountType.CASH, color = 0),
    )

    private val card = listOf(CreditCard(id = 1, name = "Visa", limitAmount = 500_000, closingDay = 3, dueDay = 10, color = 0))

    private fun parser(lang: AppLanguage, locale: Locale, accounts: List<Account>, cats: List<Category>) = { s: String ->
        PhraseParser(
            ParseContext(
                today = today, accounts = accounts, cards = card, categories = cats, defaultAccountId = 1,
                language = lang, locale = locale,
            ),
        ).parse(s)
    }

    private fun ParsedEntry.check(type: TxType, cents: Long?, date: LocalDate = today) {
        assertEquals(type, this.type)
        assertEquals(cents, amount)
        assertEquals(date, this.date)
    }

    @Test fun ingles() {
        val p = parser(
            AppLanguage.EN, Locale.US, accounts("Checking", "Savings", "Wallet"),
            cats("lunch,coffee,groceries,dinner", "gas,uber", "rent,electricity", "salary,paycheck"),
        )
        p("lunch 25 on debit").run {
            check(TxType.EXPENSE, 2500); assertEquals(1L, accountId); assertNull(cardId); assertEquals("Lunch", description)
            assertEquals(1L, categoryId)
        }
        p("tv 1200 in 10 installments on credit card").run {
            check(TxType.EXPENSE, 120_000); assertEquals(1L, cardId); assertEquals(10, installments); assertEquals("TV", description)
        }
        p("received 3,000 salary").run { check(TxType.INCOME, 300_000); assertEquals(5L, categoryId) }
        p("transferred 200 from Checking to Savings").run {
            check(TxType.TRANSFER, 20_000); assertEquals(1L, accountId); assertEquals(2L, toAccountId)
        }
        p("moved 50 to savings from checking").run { assertEquals(1L, accountId); assertEquals(2L, toAccountId) }
        p("coffee $4.50 yesterday").check(TxType.EXPENSE, 450, yesterday)
        p("groceries 1,234.56 on 9/15").check(TxType.EXPENSE, 123_456, LocalDate.of(2026, 9, 15))
        p("gas 40 bucks last friday").run { check(TxType.EXPENSE, 4000, LocalDate.of(2026, 9, 25)); assertEquals(2L, categoryId) }
        p("uber 15 cash").run { assertEquals(3L, accountId); assertEquals(1500L, amount) }
        p("paid the card statement 850").run { check(TxType.CARD_PAYMENT, 85_000); assertEquals(1L, cardId) }
        p("5k bonus").check(TxType.INCOME, 500_000)
        p("dinner 30 dollars and 50 cents").check(TxType.EXPENSE, 3050)
        p("rent 1500 on september 5th").check(TxType.EXPENSE, 150_000, LocalDate.of(2026, 9, 5))
        p("pizza 20 on debit card").run { assertNull(cardId); assertEquals(1L, accountId) }
    }

    @Test fun inglesBritanicoLeDiaAntesDoMes() {
        val p = parser(AppLanguage.EN, Locale.UK, accounts("Current", "Savings", "Cash"), cats("lunch", "petrol", "rent", "salary"))
        p("petrol 40 on 5/9").check(TxType.EXPENSE, 4000, LocalDate.of(2026, 9, 5))
        p("lunch £12.50").check(TxType.EXPENSE, 1250)
    }

    @Test fun espanhol() {
        val p = parser(
            AppLanguage.ES, Locale.forLanguageTag("es-ES"), accounts("BBVA", "Ahorros", "Cartera"),
            cats("almuerzo,cafe,supermercado", "gasolina,taxi", "alquiler,luz", "sueldo,nomina"),
        )
        p("almuerzo 12,50 con tarjeta de débito").run {
            check(TxType.EXPENSE, 1250); assertNull(cardId); assertEquals(1L, accountId); assertEquals("Almuerzo", description)
        }
        p("tele 600 en 12 cuotas con tarjeta").run { assertEquals(1L, cardId); assertEquals(12, installments) }
        p("recibí 1.500 de sueldo").run { check(TxType.INCOME, 150_000); assertEquals(5L, categoryId) }
        p("ayer gasolina 50 euros en efectivo").run { check(TxType.EXPENSE, 5000, yesterday); assertEquals(3L, accountId); assertEquals(2L, categoryId) }
        p("transferí 100 de BBVA a Ahorros").run { check(TxType.TRANSFER, 10_000); assertEquals(1L, accountId); assertEquals(2L, toAccountId) }
        p("café 3 el 5 de septiembre").check(TxType.EXPENSE, 300, LocalDate.of(2026, 9, 5))
        p("supermercado 80 el lunes pasado").check(TxType.EXPENSE, 8000, LocalDate.of(2026, 9, 28))
    }

    @Test fun frances() {
        val p = parser(
            AppLanguage.FR, Locale.FRANCE, accounts("Compte courant", "Livret A", "Portefeuille"),
            cats("dejeuner,courses,boulangerie", "essence,taxi", "loyer,electricite", "salaire"),
        )
        p("déjeuner 15 € par carte bancaire").run {
            check(TxType.EXPENSE, 1500); assertNull(cardId); assertEquals(1L, accountId); assertEquals("Déjeuner", description)
        }
        p("j'ai payé 30 euros d'essence hier").run {
            check(TxType.EXPENSE, 3000, yesterday); assertEquals(2L, categoryId); assertEquals("Essence", description)
        }
        p("reçu 2500 de salaire").run { check(TxType.INCOME, 250_000); assertEquals(5L, categoryId) }
        p("télé 800 en 4 fois avec la carte de crédit").run { assertEquals(1L, cardId); assertEquals(4, installments) }
        p("courses 85,40 le 12 septembre").check(TxType.EXPENSE, 8540, LocalDate.of(2026, 9, 12))
        p("loyer 900 en espèces").run { assertEquals(3L, accountId) }
    }

    @Test fun alemao() {
        val p = parser(
            AppLanguage.DE, Locale.GERMANY, accounts("Girokonto", "Tagesgeld", "Geldbörse"),
            cats("mittagessen,supermarkt,kaffee", "tanken,taxi", "miete,strom", "gehalt"),
        )
        p("Mittagessen 12,50 mit Karte").run { check(TxType.EXPENSE, 1250); assertNull(cardId); assertEquals(1L, accountId) }
        p("Fernseher 900 in 6 Raten mit Kreditkarte").run { assertEquals(1L, cardId); assertEquals(6, installments) }
        p("Gehalt 3.000 erhalten").run { check(TxType.INCOME, 300_000); assertEquals(5L, categoryId) }
        p("gestern Tanken 60 € bar").run { check(TxType.EXPENSE, 6000, yesterday); assertEquals(3L, accountId); assertEquals(2L, categoryId) }
        p("Supermarkt 45 am 3.9.").check(TxType.EXPENSE, 4500, LocalDate.of(2026, 9, 3))
        p("letzten Montag Kino 20").check(TxType.EXPENSE, 2000, LocalDate.of(2026, 9, 28))
        p("Miete 800 am 1. September").check(TxType.EXPENSE, 80_000, LocalDate.of(2026, 9, 1))
    }

    @Test fun italiano() {
        val p = parser(
            AppLanguage.IT, Locale.ITALY, accounts("Conto corrente", "Risparmi", "Portafoglio"),
            cats("pranzo,spesa,caffe", "benzina,taxi", "affitto,luce", "stipendio"),
        )
        p("pranzo 14 con bancomat").run { check(TxType.EXPENSE, 1400); assertNull(cardId); assertEquals("Pranzo", description) }
        p("tv 1000 in 10 rate con carta di credito").run { assertEquals(1L, cardId); assertEquals(10, installments) }
        p("ricevuto lo stipendio 1.800").run { check(TxType.INCOME, 180_000); assertEquals(5L, categoryId) }
        p("ieri benzina 50 euro in contanti").run { check(TxType.EXPENSE, 5000, yesterday); assertEquals(3L, accountId) }
        p("spesa 70 il 15 settembre").check(TxType.EXPENSE, 7000, LocalDate.of(2026, 9, 15))
        p("caffè 1,20 dell'altro ieri").check(TxType.EXPENSE, 120, today.minusDays(2))
    }

    @Test fun japones() {
        val p = parser(
            AppLanguage.JA, Locale.JAPAN, accounts("三菱UFJ銀行", "貯金", "財布"),
            cats("コーヒー,ランチ,スーパー", "タクシー,ガソリン", "家賃,電気代", "給料"),
        )
        p("コーヒー500円").run { check(TxType.EXPENSE, 50_000); assertEquals("コーヒー", description); assertEquals(1L, categoryId) }
        p("昨日ランチ1200円現金で").run { check(TxType.EXPENSE, 120_000, yesterday); assertEquals(3L, accountId); assertEquals("ランチ", description) }
        p("テレビ10万円12回払いカードで").run { check(TxType.EXPENSE, 10_000_000); assertEquals(1L, cardId); assertEquals(12, installments) }
        p("給料30万円").run { check(TxType.INCOME, 30_000_000); assertEquals(5L, categoryId) }
        p("1万5千円 スーパー").check(TxType.EXPENSE, 1_500_000)
        p("9月15日 電気代 8000円").run { check(TxType.EXPENSE, 800_000, LocalDate.of(2026, 9, 15)); assertEquals("電気代", description) }
        p("タクシー２３００円").check(TxType.EXPENSE, 230_000)
    }

    @Test fun coreano() {
        val p = parser(
            AppLanguage.KO, Locale.KOREA, accounts("국민은행", "토스", "지갑"),
            cats("점심,커피,마트", "택시,주유", "월세,전기요금", "월급"),
        )
        p("점심 12000원 카드로").run { check(TxType.EXPENSE, 1_200_000); assertEquals(1L, cardId); assertEquals("점심", description) }
        p("어제 커피 5천원 현금").run { check(TxType.EXPENSE, 500_000, yesterday); assertEquals(3L, accountId); assertEquals(1L, categoryId) }
        p("월급 300만원 받았어").run { check(TxType.INCOME, 300_000_000); assertEquals(5L, categoryId) }
        p("TV 120만원 12개월 할부").run { check(TxType.EXPENSE, 120_000_000); assertEquals(1L, cardId); assertEquals(12, installments) }
        p("국민은행에서 토스로 5만원 이체").run { check(TxType.TRANSFER, 5_000_000); assertEquals(1L, accountId); assertEquals(2L, toAccountId) }
        p("토스로 국민은행에서 5만원 이체").run { assertEquals(1L, accountId); assertEquals(2L, toAccountId) }
        p("9월 15일 전기요금 5만원").check(TxType.EXPENSE, 5_000_000, LocalDate.of(2026, 9, 15))
        p("택시비 만원").check(TxType.EXPENSE, 1_000_000)
    }

    @Test fun chines() {
        val p = parser(
            AppLanguage.ZH, Locale.CHINA, accounts("招商银行", "支付宝", "钱包"),
            cats("午饭,咖啡,超市", "打车,加油", "房租,电费", "工资"),
        )
        p("午饭50元").run { check(TxType.EXPENSE, 5000); assertEquals("午饭", description); assertEquals(1L, categoryId) }
        p("昨天打车30块现金").run { check(TxType.EXPENSE, 3000, yesterday); assertEquals(3L, accountId); assertEquals(2L, categoryId) }
        p("电视3000元分12期信用卡").run { assertEquals(1L, cardId); assertEquals(12, installments); assertEquals(300_000L, amount) }
        p("工资8000").run { check(TxType.INCOME, 800_000); assertEquals(5L, categoryId) }
        p("从招商银行转到支付宝500元").run { check(TxType.TRANSFER, 50_000); assertEquals(1L, accountId); assertEquals(2L, toAccountId) }
        p("9月15日 电费 200元").check(TxType.EXPENSE, 20_000, LocalDate.of(2026, 9, 15))
        p("房租2万").check(TxType.EXPENSE, 2_000_000)
    }

    @Test fun hindi() {
        val p = parser(
            AppLanguage.HI, Locale.forLanguageTag("hi-IN"), accounts("एसबीआई", "बचत", "बटुआ"),
            cats("चाय,खाना", "पेट्रोल,ऑटो", "किराया,बिजली", "वेतन"),
        )
        p("चाय 20 रुपये नकद").run { check(TxType.EXPENSE, 2000); assertEquals(3L, accountId); assertEquals("चाय", description) }
        p("कल पेट्रोल 500 रुपये").run { check(TxType.EXPENSE, 50_000, yesterday); assertEquals(2L, categoryId) }
        p("वेतन 50,000 मिला").run { check(TxType.INCOME, 5_000_000); assertEquals(5L, categoryId) }
        p("टीवी 30000 12 किस्तों में क्रेडिट कार्ड से").run { assertEquals(1L, cardId); assertEquals(12, installments); assertEquals(3_000_000L, amount) }
        p("₹1,23,456 फ्रिज").check(TxType.EXPENSE, 12_345_600)
        p("15 सितंबर बिजली 1200").check(TxType.EXPENSE, 120_000, LocalDate.of(2026, 9, 15))
        p("5 हज़ार किराया").check(TxType.EXPENSE, 500_000)
        p("चाय ५० रुपये").check(TxType.EXPENSE, 5000)
    }

    /** Qualquer frase, em qualquer língua, com o leitor de qualquer língua: nunca quebra e nunca inventa valor negativo. */
    @Test fun nenhumaLinguaQuebra() {
        val phrases = listOf(
            "mercado 120 no débito", "há 3 dias uber 20", "tv 1200 à vista", "guardei 100 na caixinha", "investi 500",
            "3 days ago lunch 12", "saved 100", "invested 300 in stocks", "borrowed 500 from John", "paid the loan 200",
            "hace 2 días taxi 15", "pagué 30 pesos con 50 centavos", "il y a 2 jours 12 euros et 30 centimes", "vor 3 Tagen 20 Euro und 5 Cent",
            "3 giorni fa 12 euro e 40 centesimi", "3日前 500円", "2026年9月1日 家賃 8万円", "3일 전 커피 4500원", "지난 월요일 만원",
            "3天前 咖啡 20元", "上周一 午饭 30块", "3 दिन पहले 200 रुपये", "पिछले सोमवार 2 लाख", "2026-09-01 rent 900",
            "01.09.2026 Miete 900", "R$ 1.234,56", "$1,234.56", "€ 12", "¥500", "₩5000", "", "   ", "!!!", "12/31/2026", "31/12",
            "1,23,45,678", "x x x 10x", "50k", "1万", "만원", "千円", "九月", "дом 100", "١٢٣ عربي", "😀 15",
        )
        val accts = accounts("Main", "Savings", "Cash")
        val c = cats("a", "b", "c", "d")
        for (lang in AppLanguage.entries) {
            val p = parser(lang, lang.locale, accts, c)
            for (ph in phrases) {
                val r = p(ph)
                assert(r.amount == null || r.amount!! > 0) { "$lang: \"$ph\" -> ${r.amount}" }
            }
        }
    }

    @Test fun diasAtrasEAVista() {
        val c = cats("a", "b", "c", "d")
        assertEquals(today.minusDays(3), parser(AppLanguage.EN, Locale.US, accounts("A", "B", "C"), c)("lunch 12 3 days ago").date)
        assertEquals(today.minusDays(2), parser(AppLanguage.ES, Locale.US, accounts("A", "B", "C"), c)("hace 2 días taxi 15").date)
        assertEquals(today.minusDays(3), parser(AppLanguage.JA, Locale.JAPAN, accounts("A", "B", "C"), c)("3日前 500円").date)
        assertEquals(today.minusDays(4), parser(AppLanguage.PT_BR, Locale.US, accounts("A", "B", "C"), c)("uber 20 há 4 dias").date)
        parser(AppLanguage.KO, Locale.KOREA, accounts("A", "B", "C"), c)("냉장고 100만원 일시불 카드").run {
            assertEquals(1, installments); assertEquals(1L, cardId)
        }
        parser(AppLanguage.FR, Locale.FRANCE, accounts("A", "B", "C"), c)("12 euros et 30 centimes").run { assertEquals(1230L, amount) }
    }
}
