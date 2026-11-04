// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.domain

import java.time.DayOfWeek
import java.util.Locale

/**
 * As palavras de cada língua para o registro por frase (PRD: RF-REG-09). O [PhraseParser] tem as regras (o que é valor,
 * data, parcela, conta, cartão); aqui fica só o vocabulário: verbos de gastar, receber, transferir, guardar, investir,
 * as formas de pagamento, as datas e as moedas faladas.
 *
 * Tudo já vem normalizado como o texto da frase ([normalized]: minúsculas e sem acento nas letras latinas).
 * Uma entrada pode ser uma expressão regular simples (ex.: "emprestad[oa]s?"); as de japonês e chinês são palavras
 * comuns, porque também servem para separar a frase em palavras (essas línguas não usam espaço).
 */
internal class PhraseLang(
    val language: AppLanguage,
    val writing: Writing = Writing.SPACED,
    /** "De onde" e "para onde" vêm depois do nome: "楽天から", "국민은행에서", "SBI से". */
    val postpositional: Boolean = false,
    val borrow: List<String>,
    /** Parcela de dívida ("paguei a parcela da moto"). */
    val installmentWord: List<String>,
    val debtNoun: List<String>,
    val financing: List<String>,
    val loan: List<String>,
    val agreement: List<String>,
    val goalWord: List<String>,
    val goalOut: List<String>,
    val goalIn: List<String>,
    val invest: List<String>,
    val redeem: List<String>,
    val pay: List<String>,
    val transfer: List<String>,
    val income: List<String>,
    val expense: List<String>,
    val incomeNoun: List<String>,
    /** Fatura do cartão ("paguei a fatura"). */
    val invoice: List<String>,
    val credit: List<String>,
    val debit: List<String>,
    val cash: List<String>,
    val benefit: List<String>,
    /** Preposição antes da forma de pagamento ("no débito", "on credit"), consumida junto. */
    val methodPrep: String = "",
    /** Posposição depois da forma de pagamento ("カードで", "कार्ड से"). */
    val methodPost: String = "",
    val toWords: Set<String>,
    val fromWords: Set<String>,
    /** "ontem" → -1, "amanhã" → +1. */
    val relDays: Map<String, Int>,
    /** "há 3 dias", "3 days ago": grupo 1 = número de dias. */
    val daysAgo: List<String>,
    /** O que pode vir antes de uma data e sai junto ("no dia", "on the", "am"). */
    val datePrefix: String = "",
    /** "11/03": dia/mês, ou mês/dia (inglês dos EUA, japonês, coreano, chinês). */
    val monthFirst: (Locale) -> Boolean = { false },
    /** Nomes de cada mês (janeiro primeiro), com as abreviações. */
    val months: List<List<String>> = emptyList(),
    /** Modelos de data com o mês: {P} prefixo, {D} dia, {M} mês por extenso, {MN} mês em número, {Y} ano. */
    val monthPatterns: List<String> = emptyList(),
    /** "dia 5", "the 5th", "5日": grupo 1 = dia. */
    val dayOnly: List<String> = emptyList(),
    /** Dias da semana (segunda primeiro). */
    val weekdays: List<List<String>>,
    /** Modelo do dia da semana: {W} o dia, {LB}/{LA} a palavra "passado" antes/depois. */
    val weekdayPattern: String,
    val lastWords: List<String>,
    /** "à vista": não é parcelado. */
    val upfront: List<String>,
    /** Parcelas da compra ("em 10x", "in 10 installments"): grupo 1 = número. */
    val installments: List<String>,
    /** Moeda falada depois do número ("120 reais", "20 euros", "500円"). */
    val currencyWords: List<String>,
    /** "2 mil", "5k", "3 lakh". */
    val multipliers: Map<String, Long>,
    /** Só a palavra, sem número ("mil reais"). */
    val bareThousand: List<String> = emptyList(),
    /** Verbos que costumam vir logo antes do valor ("gastei 50", "paid 50", "für 20"). */
    val amountVerbs: List<String> = emptyList(),
    /** O que pode ligar o verbo ao valor ("gastei de 50"? não; "foi de 50"). */
    val amountLink: String = "",
    /** Valor com centavos por extenso: {NUM} o valor, {C} os centavos. */
    val cents: List<String> = emptyList(),
    val debtGeneric: Set<String>,
    val genericName: Set<String>,
    val stop: Set<String>,
    /** Palavras de extrato que não dizem nada da categoria (para aprender pela próxima palavra). */
    val bankWords: Set<String>,
    /** Plural nas palavras-chave das categorias. */
    val plural: String = "",
    /** Artigo grudado com apóstrofo que sai da descrição ("l'épicerie" → "épicerie"). */
    val elision: Regex? = null,
    /** Partículas grudadas no fim da palavra (coreano: "카드로" = "카드" + "로"). */
    val particles: List<String> = emptyList(),
) {
    enum class Writing {
        /** Palavras separadas por espaço (português, inglês, hindi...). */
        SPACED,
        /** Com espaços, mas com partículas e terminações grudadas (coreano): procura por pedaço de palavra. */
        AGGLUTINATIVE,
        /** Sem espaços (japonês, chinês): separa pelas palavras conhecidas e procura por pedaço. */
        UNSPACED,
    }

    /** Palavras conhecidas, para separar frases sem espaço. */
    val vocabulary: Set<String> by lazy {
        val plain = PLAIN
        listOf(
            borrow, installmentWord, debtNoun, financing, loan, agreement, goalWord, goalOut, goalIn, invest, redeem, pay,
            transfer, income, expense, incomeNoun, invoice, credit, debit, cash, benefit, upfront, currencyWords, lastWords,
        ).flatten().plus(toWords).plus(fromWords).plus(relDays.keys).plus(months.flatten()).plus(weekdays.flatten())
            .plus(stop).plus(debtGeneric)
            .map { it.normalized() }.filter { it.isNotEmpty() && plain.matches(it) }.toSet()
    }

    /** Verbos (para tirar da descrição em línguas onde eles aparecem conjugados e grudados). */
    val verbs: List<String> by lazy {
        (borrow + goalOut + goalIn + invest + redeem + pay + transfer + income + expense).filter { PLAIN.matches(it) }
            .map { it.normalized() }.filter { it.isNotEmpty() }
    }

    /** Entrada normalizada como o texto da frase (as expressões regulares ficam como estão). */
    fun n(e: String): String = if (PLAIN.matches(e)) e.normalized() else e

    val stopN: Set<String> by lazy { stop.map { it.normalized() }.toSet() }
    val debtGenericN: Set<String> by lazy { debtGeneric.map { it.normalized() }.toSet() }
    val genericNameN: Set<String> by lazy { genericName.map { it.normalized() }.toSet() }
    val bankN: Set<String> by lazy { bankWords.map { it.normalized() }.toSet() }
    val toN: Set<String> by lazy { toWords.map { it.normalized() }.toSet() }
    val fromN: Set<String> by lazy { fromWords.map { it.normalized() }.toSet() }
    val relDaysN: Map<String, Int> by lazy { relDays.mapKeys { it.key.normalized() } }
    val multipliersN: Map<String, Long> by lazy { multipliers.mapKeys { it.key.normalized() } }
    val particlesN: List<String> by lazy { particles.map { it.normalized() }.sortedByDescending { it.length } }
    val monthIndex: Map<String, Int> by lazy {
        months.flatMapIndexed { i, names -> names.map { it.normalized() to i + 1 } }.toMap()
    }
    val weekdayIndex: Map<String, DayOfWeek> by lazy {
        weekdays.flatMapIndexed { i, names -> names.map { it.normalized() to DayOfWeek.of(i + 1) } }.toMap()
    }

    companion object {
        /** Palavra comum (sem sinais de expressão regular). */
        private val PLAIN = Regex("^[^\\\\()\\[\\]?*+|.{}$]+$")

        private fun w(s: String): List<String> = s.split('|').map { it.trim() }.filter { it.isNotEmpty() }
        private fun ws(s: String): Set<String> = w(s).toSet()
        private fun days(vararg d: String): List<List<String>> = d.map { w(it) }

        fun forLanguage(l: AppLanguage): PhraseLang = when (l) {
            AppLanguage.PT_BR -> PT
            AppLanguage.EN -> EN
            AppLanguage.ES -> ES
            AppLanguage.FR -> FR
            AppLanguage.DE -> DE
            AppLanguage.IT -> IT
            AppLanguage.JA -> JA
            AppLanguage.KO -> KO
            AppLanguage.ZH -> ZH
            AppLanguage.HI -> HI
        }

        val current: PhraseLang get() = forLanguage(AppLocale.language)

        /* ------------------------------ português ------------------------------ */
        val PT = PhraseLang(
            language = AppLanguage.PT_BR,
            borrow = listOf(
                "(?:peguei|pedi|tomei|recebi|consegui) (?:\\S+ ){0,3}emprestad[oa]s?",
                "me emprest(?:ou|aram)",
                "(?:peguei|fiz|tomei|contratei) (?:um |o )?emprestimo",
            ),
            installmentWord = w("parcela|parcelas|prestacao|prestacoes"),
            debtNoun = w("divida|dividas|emprestimo|financiamento|acordo|carne|crediario|consignado"),
            financing = w("financiamento"),
            loan = w("emprestimo|consignado"),
            agreement = w("acordo"),
            goalWord = w("caixinha|cofrinho|meta"),
            goalOut = w("tirei|retirei|resgatei|saquei|usei|peguei"),
            goalIn = w("guardei|separei|reservei|poupei|coloquei|depositei"),
            invest = w("investi|apliquei|aportei|aporte|aplicacao|aplicar|investir"),
            redeem = w("resgatei|resgate|saquei|retirei"),
            pay = w("paguei|pagar|pagamento|quitei|pago"),
            transfer = w("transferi|transferencia|transferir|passei|movi|mandei"),
            income = w("recebi|ganhei|entrou|caiu|vendi|receber|recebido|recebimento"),
            expense = w("gastei|paguei|comprei|gasto|pago|gastar|comprar"),
            incomeNoun = w(
                "salario|freela|freelance|bico|rendimento|rendimentos|reembolso|estorno|dividendos|cashback|bonus|comissao|" +
                    "mesada|13o|decimo terceiro|plr|adiantamento|venda|vendas|deposito",
            ),
            invoice = w("fatura"),
            credit = w("credito|cartao"),
            debit = w("cartao de debito|debito|pix|na conta"),
            cash = w("dinheiro|especie|cash|em maos"),
            benefit = w("vale refeicao|vale alimentacao|vr|va|ticket|alelo|sodexo|pluxee|caju|vale"),
            methodPrep = "n[oa]|em",
            toWords = ws("para|pra|pro|na|no|em"),
            fromWords = ws("de|da|do|dos|das"),
            relDays = mapOf("hoje" to 0, "ontem" to -1, "anteontem" to -2, "amanha" to 1),
            daysAgo = listOf("ha (\\d{1,2}) dias", "(\\d{1,2}) dias atras"),
            datePrefix = "(?:(?:n[oa]|em) )?(?:dia )?",
            months = days("janeiro", "fevereiro", "marco", "abril", "maio", "junho", "julho", "agosto", "setembro", "outubro", "novembro", "dezembro"),
            monthPatterns = listOf("{P}{D} de {M}(?: de {Y})?"),
            dayOnly = listOf("(?:(?:n[oa]|em) )?dia (\\d{1,2})"),
            weekdays = days("segunda", "terca", "quarta", "quinta", "sexta", "sabado", "domingo"),
            weekdayPattern = "(?:n[oa] )?{W}(?:-feira| feira)?(?: {LA})?",
            lastWords = w("passado|passada"),
            upfront = w("a vista"),
            installments = listOf(
                "(?:em )?(\\d{1,2}) ?(?:x|vezes)",
                "parcelad[oa] (?:em )?(\\d{1,2})(?: ?(?:x|vezes|parcelas))?",
                "(?:em )?(\\d{1,2}) parcelas",
            ),
            currencyWords = w("reais|real|conto|contos|pila|pilas"),
            multipliers = mapOf("mil" to 1000L),
            bareThousand = w("mil"),
            amountVerbs = w("gastei|paguei|recebi|ganhei|custou|deu|foi|por|valor|total|investi|apliquei|guardei|transferi|resgatei|separei"),
            amountLink = "de",
            cents = listOf("{NUM} ?(?:reais|real)? e {C} centavos"),
            debtGeneric = ws(
                "divida|dividas|emprestimo|financiamento|parcela|parcelas|acordo|devo|ao|aos|com|pro|loja|carne|crediario|consignado",
            ),
            genericName = ws(
                "conta|cartao|de|do|da|dos|das|banco|credito|debito|corrente|minha|meu|o|a|e|para|pra|caixinha|meta|investimento|em|no|na",
            ),
            stop = ws(
                "gastei|gasto|gastos|gastar|paguei|pago|pagar|pagamento|comprei|compra|comprar|recebi|receber|recebido|" +
                    "recebimento|ganhei|entrou|caiu|transferi|transferencia|transferir|passei|movi|mandei|guardei|separei|" +
                    "reservei|poupei|coloquei|depositei|investi|apliquei|aportei|aporte|aplicacao|resgatei|resgate|tirei|retirei|" +
                    "saquei|usei|peguei|quitei|com|no|na|nos|nas|de|do|da|dos|das|em|um|uma|uns|umas|o|a|os|as|e|pra|para|pro|" +
                    "por|pelo|pela|reais|real|r$|conto|contos|pila|pilas|foi|fiz|deu|custou|valor|total|hoje|ontem|anteontem|" +
                    "amanha|meu|minha|meus|minhas|ao|aos|que|via|la|aqui|fatura|caixinha|cofrinho|meta|cartao|conta|" +
                    "investimento|eu|agora|tambem",
            ),
            bankWords = ws(
                "pix|ted|doc|tef|enviado|enviada|recebida|debito|credito|boleto|tarifa|saque|deposito|pagto|pgto|transf|" +
                    "compras|estorno|parcela|automatico|cartao|elo|visa|master|mastercard",
            ),
            plural = "(?:s|es)?",
        )

        /* ------------------------------- inglês -------------------------------- */
        val EN = PhraseLang(
            language = AppLanguage.EN,
            borrow = listOf("borrowed|borrow", "(?:lent|loaned) me", "took (?:out )?a loan", "got a loan"),
            installmentWord = w("installment|installments|instalment|instalments"),
            debtNoun = w("debt|debts|loan|loans|financing|mortgage|car payment|settlement|payment plan"),
            financing = w("financing|mortgage|car payment|car loan"),
            loan = w("loan|loans|personal loan"),
            agreement = w("settlement|payment plan|agreement"),
            goalWord = w("goal|savings goal|piggy bank|jar|pot"),
            goalOut = w("took out|withdrew|pulled|used"),
            goalIn = w("saved|set aside|put aside|stashed|deposited"),
            invest = w("invested|invest|investing|contributed|contribution"),
            redeem = w("redeemed|withdrew|cashed out|sold"),
            pay = w("paid|pay|payment|paying|settled|paid off"),
            transfer = w("transferred|transfer|moved|move|sent|wired"),
            income = w("received|receive|earned|got paid|sold|refunded|income"),
            expense = w("spent|spend|paid|bought|buy|purchased|purchase|cost"),
            incomeNoun = w(
                "salary|paycheck|payday|wage|wages|freelance|gig|bonus|commission|refund|reimbursement|dividend|dividends|" +
                    "cashback|allowance|tips|tax refund|interest earned",
            ),
            invoice = w("card statement|statement|card bill|credit card bill|card balance"),
            credit = w("credit card|credit|card"),
            debit = w("debit card|debit"),
            cash = w("cash|in cash"),
            benefit = w("meal voucher|meal card|food stamps|ebt|snap"),
            methodPrep = "on|with|by|via|using",
            toWords = ws("to|into|in|on"),
            fromWords = ws("from|out"),
            relDays = mapOf("today" to 0, "yesterday" to -1, "day before yesterday" to -2, "tomorrow" to 1),
            daysAgo = listOf("(\\d{1,2}) days? ago"),
            datePrefix = "(?:on )?(?:the )?",
            monthFirst = { it.country.isEmpty() || it.country in setOf("US", "PH", "FM", "MH", "PW") },
            months = days(
                "january|jan", "february|feb", "march|mar", "april|apr", "may", "june|jun",
                "july|jul", "august|aug", "september|sep|sept", "october|oct", "november|nov", "december|dec",
            ),
            monthPatterns = listOf(
                "{P}{D}(?:st|nd|rd|th)?(?: of)? {M}(?:,? {Y})?",
                "(?:on )?{M} (?:the )?{D}(?:st|nd|rd|th)?(?:,? {Y})?",
            ),
            dayOnly = listOf("(?:on )?the (\\d{1,2})(?:st|nd|rd|th)", "on the (\\d{1,2})"),
            weekdays = days("monday|mon", "tuesday|tue|tues", "wednesday|wed", "thursday|thu|thurs", "friday|fri", "saturday|sat", "sunday|sun"),
            weekdayPattern = "(?:on )?(?:{LB} )?{W}",
            lastWords = w("last|past|this past"),
            upfront = w("upfront|up front|in full|paid in full|one payment"),
            installments = listOf(
                "(?:in )?(\\d{1,2}) (?:installments|instalments|payments|monthly payments)",
                "(\\d{1,2})-month (?:installments|plan)",
            ),
            currencyWords = w("dollars|dollar|bucks|buck|usd|euros|euro|eur|pounds|pound|quid|gbp|cad|aud"),
            multipliers = mapOf("k" to 1000L, "thousand" to 1000L, "grand" to 1000L),
            bareThousand = w("a thousand|a grand"),
            amountVerbs = w("spent|paid|received|earned|cost|costs|was|for|total|invested|saved|transferred|withdrew|sent|got"),
            cents = listOf("{NUM} (?:dollars?|bucks?|euros?|pounds?) and {C} cents?"),
            debtGeneric = ws("debt|debts|loan|financing|installment|installments|agreement|owe|to|the|with|store|mortgage|my"),
            genericName = ws("account|card|of|the|bank|credit|debit|checking|my|a|and|to|for|goal|investment|in|on|at"),
            stop = ws(
                "spent|spend|paid|pay|payment|paying|bought|buy|purchase|purchased|received|receive|earned|got|transferred|" +
                    "transfer|moved|move|sent|wired|saved|set|aside|put|stashed|deposited|invested|invest|contributed|withdrew|" +
                    "took|out|used|pulled|with|on|in|at|of|from|to|for|the|a|an|my|and|dollars|dollar|bucks|buck|usd|cost|" +
                    "costs|was|total|today|yesterday|tomorrow|day|before|this|that|via|card|account|statement|goal|jar|" +
                    "investment|i|me|just|also|some|off|by|into|last|ago|days|is|it|using|$",
            ),
            bankWords = ws(
                "transfer|payment|purchase|pos|card|debit|credit|atm|withdrawal|deposit|fee|zelle|venmo|ach|online|recurring|" +
                    "visa|mastercard|amex|check|paypal|autopay",
            ),
            plural = "(?:s|es)?",
        )

        /* ------------------------------- espanhol ------------------------------ */
        val ES = PhraseLang(
            language = AppLanguage.ES,
            borrow = listOf(
                "(?:pedi|tome|saque|recibi|consegui) (?:\\S+ ){0,3}prestad[oa]s?",
                "me prest(?:o|aron)",
                "(?:pedi|saque|tome|contrate) (?:un |el )?prestamo",
            ),
            installmentWord = w("cuota|cuotas|mensualidad|mensualidades"),
            debtNoun = w("deuda|deudas|prestamo|prestamos|financiamiento|financiacion|hipoteca|credito hipotecario|acuerdo|convenio|credito personal"),
            financing = w("financiamiento|financiacion|hipoteca|credito hipotecario"),
            loan = w("prestamo|prestamos|credito personal"),
            agreement = w("acuerdo|convenio"),
            goalWord = w("meta|alcancia|hucha|objetivo|apartado"),
            goalOut = w("saque|retire|use|tome|quite"),
            goalIn = w("ahorre|guarde|separe|aparte|reserve|deposite"),
            invest = w("inverti|invertir|invierto|aporte|aportacion"),
            redeem = w("rescate|retire|vendi|saque"),
            pay = w("pague|pagar|pago|pagado|liquide|abone"),
            transfer = w("transferi|transferencia|transferir|pase|movi|mande|envie"),
            income = w("recibi|recibir|gane|cobre|vendi|entro|cayo|me pagaron|ingreso|ingrese"),
            expense = w("gaste|gasto|gastar|pague|compre|comprar|costo"),
            incomeNoun = w(
                "salario|sueldo|nomina|freelance|chamba|bono|aguinaldo|comision|reembolso|devolucion|dividendos|cashback|" +
                    "mesada|propina|propinas|venta|ventas|deposito|rendimiento|rendimientos|intereses",
            ),
            invoice = w("resumen de la tarjeta|resumen|estado de cuenta|factura de la tarjeta"),
            credit = w("tarjeta de credito|credito|tarjeta"),
            debit = w("tarjeta de debito|debito|bizum"),
            cash = w("en efectivo|efectivo|cash|en mano"),
            benefit = w("vales de despensa|vale de despensa|vales|ticket restaurante|tarjeta restaurante|edenred|sodexo|pluxee|cheque gourmet"),
            methodPrep = "con|en|a|por|al",
            toWords = ws("a|al|para|en|hacia"),
            fromWords = ws("de|del|desde"),
            relDays = mapOf(
                "hoy" to 0, "esta manana" to 0, "ayer" to -1, "anteayer" to -2, "antier" to -2, "antes de ayer" to -2,
                "manana" to 1, "pasado manana" to 2,
            ),
            daysAgo = listOf("hace (\\d{1,2}) dias"),
            datePrefix = "(?:el )?(?:dia )?",
            months = days(
                "enero|ene", "febrero|feb", "marzo|mar", "abril|abr", "mayo|may", "junio|jun",
                "julio|jul", "agosto|ago", "septiembre|setiembre|sep|sept", "octubre|oct", "noviembre|nov", "diciembre|dic",
            ),
            monthPatterns = listOf("{P}{D}(?:º|°|o)? (?:de )?{M}(?: (?:de |del )?{Y})?"),
            dayOnly = listOf("(?:el )?dia (\\d{1,2})"),
            weekdays = days("lunes", "martes", "miercoles", "jueves", "viernes", "sabado", "domingo"),
            weekdayPattern = "(?:el )?{W}(?: {LA})?",
            lastWords = w("pasado|pasada"),
            upfront = w("de contado|al contado|en una cuota|en un pago|en una exhibicion"),
            installments = listOf(
                "(?:en )?(\\d{1,2}) (?:cuotas|pagos|mensualidades|meses sin intereses|meses)",
                "(\\d{1,2}) ?msi",
            ),
            currencyWords = w("pesos|peso|euros|euro|eur|dolares|dolar|soles|pavos|usd|mxn"),
            multipliers = mapOf("mil" to 1000L, "k" to 1000L),
            bareThousand = w("mil"),
            amountVerbs = w("gaste|pague|recibi|gane|costo|fue|por|valor|total|inverti|ahorre|transferi|cobre|saque"),
            amountLink = "de",
            cents = listOf("{NUM} (?:pesos?|euros?|dolares?) (?:con|y) {C} (?:centavos|centimos)"),
            debtGeneric = ws("deuda|deudas|prestamo|financiamiento|cuota|cuotas|acuerdo|debo|al|a|con|para|tienda|hipoteca|mi"),
            genericName = ws("cuenta|tarjeta|de|del|la|el|banco|credito|debito|corriente|mi|mis|y|para|meta|inversion|en|con"),
            stop = ws(
                "gaste|gasto|gastos|gastar|pague|pago|pagar|pagado|compre|compra|comprar|recibi|recibir|gane|cobre|entro|cayo|" +
                    "transferi|transferencia|transferir|pase|movi|mande|envie|ahorre|guarde|separe|aparte|reserve|deposite|" +
                    "inverti|invertir|aporte|rescate|retire|saque|use|tome|con|en|de|del|la|el|los|las|un|una|unos|unas|y|a|al|" +
                    "para|por|pesos|peso|euros|euro|dolares|dolar|costo|fue|valor|total|hoy|ayer|anteayer|manana|mi|mis|que|" +
                    "via|aqui|meta|tarjeta|cuenta|inversion|yo|ahora|tambien|me|se|lo|hace|dias|$",
            ),
            bankWords = ws(
                "transferencia|pago|compra|tarjeta|cajero|retiro|comision|bizum|spei|domiciliacion|recibo|cargo|abono|" +
                    "debito|credito|visa|mastercard|pos",
            ),
            plural = "(?:s|es)?",
        )

        /* ------------------------------- francês ------------------------------- */
        val FR = PhraseLang(
            language = AppLanguage.FR,
            borrow = listOf("emprunte|emprunter|empruntee", "(?:a|ont) prete", "pret recu"),
            installmentWord = w("mensualite|mensualites|echeance|echeances"),
            debtNoun = w("dette|dettes|pret|emprunt|financement|credit immobilier|credit auto|credit conso|hypotheque|accord|echeancier"),
            financing = w("financement|credit immobilier|credit auto|hypotheque"),
            loan = w("pret|emprunt|credit conso"),
            agreement = w("accord|echeancier"),
            goalWord = w("objectif|cagnotte|tirelire"),
            goalOut = w("retire|sorti|pris|utilise|preleve"),
            goalIn = w("economise|mis de cote|epargne|depose|verse"),
            invest = w("investi|investir|placement"),
            redeem = w("rachat|rachete|vendu|retire"),
            pay = w("paye|payer|paiement|regle|rembourse"),
            transfer = w("vire|virement|transfere|transfert|envoye|bouge|deplace"),
            income = w("recu|gagne|touche|vendu|encaisse|rentre"),
            expense = w("depense|depenser|paye|achete|acheter|achat|coute"),
            incomeNoun = w(
                "salaire|paie|prime|freelance|remboursement|dividendes|cashback|argent de poche|commission|vente|ventes|" +
                    "interets|pourboire|pourboires|bonus|depot|treizieme mois",
            ),
            invoice = w("releve de carte|releve|facture de la carte|debit differe"),
            credit = w("carte de credit|credit"),
            debit = w("carte bancaire|carte bleue|cb|carte de debit|debit|carte"),
            cash = w("en especes|especes|liquide|en liquide|cash"),
            benefit = w("ticket resto|tickets resto|ticket restaurant|titre restaurant|titres restaurant|carte resto|swile|edenred|sodexo|pluxee"),
            methodPrep = "par|en|avec|a|au",
            toWords = ws("a|au|aux|vers|sur|dans|pour"),
            fromWords = ws("de|du|des|depuis"),
            relDays = mapOf("aujourd'hui" to 0, "hier" to -1, "avant-hier" to -2, "avant hier" to -2, "demain" to 1),
            daysAgo = listOf("il y a (\\d{1,2}) jours"),
            datePrefix = "(?:le )?",
            months = days(
                "janvier|janv", "fevrier|fevr|fev", "mars", "avril|avr", "mai", "juin",
                "juillet|juil", "aout", "septembre|sept", "octobre|oct", "novembre|nov", "decembre|dec",
            ),
            monthPatterns = listOf("{P}{D}(?:er)? {M}(?: {Y})?"),
            dayOnly = listOf("le (\\d{1,2})(?:er)?(?! ?(?:euros?|€|eur|e)(?![\\p{L}]))"),
            weekdays = days("lundi", "mardi", "mercredi", "jeudi", "vendredi", "samedi", "dimanche"),
            weekdayPattern = "(?:ce |le )?{W}(?: {LA})?",
            lastWords = w("dernier|passe"),
            upfront = w("au comptant|comptant|en une fois"),
            installments = listOf("(?:en )?(\\d{1,2}) ?(?:fois|mensualites|versements)"),
            currencyWords = w("euros|euro|eur|balles|dollars|dollar|francs|franc|chf"),
            multipliers = mapOf("mille" to 1000L, "k" to 1000L),
            bareThousand = w("mille"),
            amountVerbs = w("depense|paye|recu|gagne|coute|pour|total|investi|economise|vire|envoye|retire|touche"),
            amountLink = "de",
            cents = listOf("{NUM} euros? (?:et )?{C} (?:centimes|cts)"),
            debtGeneric = ws("dette|dettes|pret|emprunt|financement|mensualite|mensualites|accord|dois|au|aux|a|avec|pour|magasin|credit|mon|ma"),
            genericName = ws("compte|carte|de|du|des|la|le|les|banque|credit|debit|courant|mon|ma|mes|et|pour|objectif|cagnotte|investissement|en|au"),
            stop = ws(
                "depense|depenser|paye|payer|paiement|achete|acheter|achat|recu|gagne|touche|encaisse|vendu|vire|virement|" +
                    "transfere|transfert|envoye|economise|epargne|mis|cote|depose|verse|investi|investir|retire|sorti|pris|" +
                    "utilise|avec|en|de|du|des|la|le|les|l|un|une|et|a|au|aux|pour|par|sur|dans|euros|euro|eur|balles|coute|" +
                    "total|aujourd'hui|hier|demain|mon|ma|mes|que|via|ici|objectif|cagnotte|carte|compte|investissement|ai|je|" +
                    "moi|aussi|il|y|jours|ce|cette|suis|est|€",
            ),
            bankWords = ws(
                "virement|paiement|carte|prelevement|retrait|cb|achat|frais|cotisation|sepa|depot|remise|cheque|visa|" +
                    "mastercard|dab|inst|prlv|vir",
            ),
            plural = "(?:s|x)?",
            elision = Regex("^(?:l|d|j|qu|n|s|m|t|c|jusqu)'(?=\\p{L})"),
        )

        /* ------------------------------- alemão -------------------------------- */
        val DE = PhraseLang(
            language = AppLanguage.DE,
            borrow = listOf("geliehen|ausgeliehen|geborgt", "kredit aufgenommen|darlehen aufgenommen"),
            installmentWord = w("rate|raten|monatsrate|tilgung"),
            debtNoun = w("schulden|schuld|kredit|darlehen|finanzierung|hypothek|ratenkredit|vergleich|ratenzahlung|autokredit|baufinanzierung"),
            financing = w("finanzierung|hypothek|baufinanzierung|autokredit"),
            loan = w("kredit|darlehen|ratenkredit"),
            agreement = w("vergleich|ratenzahlung"),
            goalWord = w("sparziel|ziel|spardose|sparschwein|spartopf|topf"),
            goalOut = w("entnommen|abgehoben|genommen|rausgenommen|geholt"),
            goalIn = w("gespart|zuruckgelegt|beiseitegelegt|eingezahlt|angespart"),
            invest = w("investiert|investieren|angelegt|anlegen|sparplan"),
            redeem = w("verkauft|ausgezahlt|aufgelost|entnommen"),
            pay = w("bezahlt|zahlen|zahlung|gezahlt|beglichen|abbezahlt|getilgt"),
            transfer = w("uberwiesen|uberweisung|umgebucht|umbuchung|verschoben|geschickt|gesendet|transferiert"),
            income = w("erhalten|bekommen|verdient|verkauft|eingegangen|eingang|gutschrift"),
            expense = w("ausgegeben|bezahlt|gekauft|kauf|kaufen|gezahlt|kostet|gekostet"),
            incomeNoun = w(
                "gehalt|lohn|bonus|provision|erstattung|ruckerstattung|dividende|dividenden|cashback|taschengeld|nebenjob|" +
                    "verkauf|zinsen|trinkgeld|weihnachtsgeld|urlaubsgeld|honorar",
            ),
            invoice = w("kreditkartenabrechnung|kartenabrechnung|kreditkartenrechnung|abrechnung"),
            credit = w("kreditkarte|kredit"),
            debit = w("ec-karte|ec karte|girocard|debitkarte|ec|lastschrift|karte"),
            cash = w("bar|bargeld|in bar|cash"),
            benefit = w("essensgutschein|essensmarke|sachbezug|sodexo|pluxee|edenred"),
            methodPrep = "mit|per|auf|in",
            toWords = ws("auf|an|zu|zum|zur|nach|in|ins|aufs"),
            fromWords = ws("von|vom|aus"),
            relDays = mapOf("heute" to 0, "gestern" to -1, "vorgestern" to -2, "morgen" to 1, "ubermorgen" to 2),
            daysAgo = listOf("vor (\\d{1,2}) tagen"),
            datePrefix = "(?:am )?",
            months = days(
                "januar|janner|jan", "februar|feb", "marz|mar", "april|apr", "mai", "juni|jun",
                "juli|jul", "august|aug", "september|sep|sept", "oktober|okt", "november|nov", "dezember|dez",
            ),
            monthPatterns = listOf("{P}{D} {M}(?: {Y})?", "am {D}\\.{MN}(?:\\.{Y})?"),
            dayOnly = listOf("am (\\d{1,2})(?! ?(?:euro|€|eur)(?![\\p{L}]))"),
            weekdays = days("montag", "dienstag", "mittwoch", "donnerstag", "freitag", "samstag|sonnabend", "sonntag"),
            weekdayPattern = "(?:am )?(?:{LB} )?{W}",
            lastWords = w("letzten|letzte|letzter|vergangenen|vergangene"),
            upfront = w("auf einmal|in einer rate|einmalzahlung"),
            installments = listOf("(?:in )?(\\d{1,2}) (?:raten|monatsraten|monatlichen raten)"),
            currencyWords = w("euro|euros|eur|franken|chf|dollar"),
            multipliers = mapOf("tausend" to 1000L, "tsd" to 1000L, "k" to 1000L),
            bareThousand = w("tausend"),
            amountVerbs = w("ausgegeben|bezahlt|erhalten|bekommen|verdient|kostet|gekostet|fur|total|gesamt|investiert|gespart|uberwiesen|abgehoben"),
            amountLink = "",
            cents = listOf("{NUM} euro (?:und )?{C} (?:cent|ct)"),
            debtGeneric = ws("schulden|schuld|kredit|darlehen|finanzierung|rate|raten|vergleich|an|bei|mit|fur|laden|mein|meine"),
            genericName = ws("konto|karte|von|der|die|das|des|bank|kredit|debit|giro|girokonto|mein|meine|und|fur|ziel|sparziel|depot|in|im|auf"),
            stop = ws(
                "ausgegeben|bezahlt|zahlen|zahlung|gezahlt|gekauft|kauf|kaufen|erhalten|bekommen|verdient|eingegangen|" +
                    "uberwiesen|uberweisung|umgebucht|geschickt|gespart|zuruckgelegt|eingezahlt|investiert|angelegt|entnommen|" +
                    "abgehoben|genommen|mit|im|in|am|an|auf|von|vom|der|die|das|den|dem|des|ein|eine|einen|einem|und|fur|zu|" +
                    "zum|zur|nach|bei|euro|euros|eur|kostet|gekostet|gesamt|heute|gestern|vorgestern|morgen|mein|meine|" +
                    "meinem|meinen|ich|habe|hab|hat|furs|auch|jetzt|vor|tagen|karte|konto|ziel|sparziel|ins|aus|beim|€",
            ),
            bankWords = ws(
                "uberweisung|lastschrift|kartenzahlung|zahlung|gutschrift|ec|girocard|abbuchung|dauerauftrag|sepa|" +
                    "bargeldauszahlung|geldautomat|entgelt|gebuhr|visa|mastercard|paypal|einzug|basislastschrift",
            ),
            plural = "(?:e|en|n|s|er)?",
        )

        /* ------------------------------- italiano ------------------------------ */
        val IT = PhraseLang(
            language = AppLanguage.IT,
            borrow = listOf("(?:mi ha|mi hanno) prestato", "pres[oi] in prestito", "prestito ricevuto", "chiesto un prestito"),
            installmentWord = w("rata|rate|mensilita"),
            debtNoun = w("debiti|prestito|prestiti|finanziamento|mutuo|accordo|rateizzazione|cessione del quinto"),
            financing = w("finanziamento|mutuo"),
            loan = w("prestito|prestiti|cessione del quinto"),
            agreement = w("accordo|rateizzazione"),
            goalWord = w("obiettivo|salvadanaio"),
            goalOut = w("prelevato|tolto|preso|ritirato|usato"),
            goalIn = w("risparmiato|messo da parte|accantonato|versato|depositato"),
            invest = w("investito|investire|piano di accumulo|pac"),
            redeem = w("riscattato|disinvestito|venduto|riscatto|prelevato"),
            pay = w("pagato|pagare|pagamento|saldato"),
            transfer = w("trasferito|trasferimento|bonifico|girato|giroconto|spostato|mandato|inviato"),
            income = w("ricevuto|guadagnato|incassato|venduto|entrato|entrati|accreditato"),
            expense = w("speso|spesi|pagato|comprato|spendere|comprare|acquisto|costato"),
            incomeNoun = w(
                "stipendio|bonus|tredicesima|quattordicesima|commissione|rimborso|dividendi|cashback|paghetta|vendita|" +
                    "mancia|mance|interessi|provvigione|accredito",
            ),
            invoice = w("estratto conto|addebito carta|saldo della carta|rendiconto"),
            credit = w("carta di credito|credito|carta"),
            debit = w("carta di debito|bancomat|pagobancomat|debito|satispay"),
            cash = w("in contanti|contanti|cash"),
            benefit = w("buono pasto|buoni pasto|ticket restaurant|ticket|edenred|pluxee|sodexo"),
            methodPrep = "con|col|in|a|al|via",
            toWords = ws("a|al|alla|allo|su|sul|sulla|in|nel|nella|verso|per"),
            fromWords = ws("da|dal|dalla|dallo|dai"),
            relDays = mapOf(
                "oggi" to 0, "ieri" to -1, "altro ieri" to -2, "altroieri" to -2, "avantieri" to -2, "domani" to 1,
                "dopodomani" to 2,
            ),
            daysAgo = listOf("(\\d{1,2}) giorni fa"),
            datePrefix = "(?:il )?(?:giorno )?",
            months = days(
                "gennaio|gen", "febbraio|feb", "marzo|mar", "aprile|apr", "maggio|mag", "giugno|giu",
                "luglio|lug", "agosto|ago", "settembre|set", "ottobre|ott", "novembre|nov", "dicembre|dic",
            ),
            monthPatterns = listOf("{P}{D}(?:º|°)? {M}(?: {Y})?"),
            dayOnly = listOf("(?:il )?giorno (\\d{1,2})", "il (\\d{1,2})(?! ?(?:euro|€|eur)(?![\\p{L}]))"),
            weekdays = days("lunedi", "martedi", "mercoledi", "giovedi", "venerdi", "sabato", "domenica"),
            weekdayPattern = "(?:lo |la |il )?{W}(?: {LA})?",
            lastWords = w("scorso|scorsa"),
            upfront = w("unica soluzione|in unica soluzione|unica rata"),
            installments = listOf("(?:in )?(\\d{1,2}) (?:rate|mensilita)"),
            currencyWords = w("euro|eur|franchi|chf|dollari|dollaro"),
            multipliers = mapOf("mila" to 1000L, "mille" to 1000L, "k" to 1000L),
            bareThousand = w("mille"),
            amountVerbs = w("speso|pagato|ricevuto|guadagnato|costato|costa|per|totale|investito|risparmiato|trasferito|incassato|prelevato"),
            amountLink = "",
            cents = listOf("{NUM} euro e {C} (?:centesimi|cent)"),
            debtGeneric = ws("debito|debiti|prestito|finanziamento|mutuo|rata|rate|accordo|devo|a|al|con|per|negozio|mio|mia"),
            genericName = ws("conto|carta|di|del|della|la|il|lo|banca|credito|debito|corrente|mio|mia|miei|e|per|obiettivo|salvadanaio|investimento|in|sul|sulla"),
            stop = ws(
                "speso|spesi|spendere|pagato|pagare|pagamento|comprato|comprare|acquisto|ricevuto|guadagnato|incassato|venduto|" +
                    "entrato|accreditato|trasferito|trasferimento|bonifico|girato|spostato|mandato|inviato|risparmiato|messo|" +
                    "parte|accantonato|versato|depositato|investito|investire|prelevato|tolto|preso|ritirato|usato|con|col|in|" +
                    "di|del|della|dello|dei|delle|la|il|lo|i|gli|le|l|un|una|uno|e|a|al|alla|per|su|sul|sulla|nel|nella|euro|" +
                    "eur|costato|costa|totale|oggi|ieri|domani|mio|mia|miei|mie|che|via|qui|obiettivo|carta|conto|" +
                    "investimento|ho|io|anche|adesso|ora|fa|giorni|da|dal|dalla|€",
            ),
            bankWords = ws(
                "bonifico|pagamento|carta|prelievo|addebito|pos|acquisto|commissione|accredito|sdd|rid|giroconto|bancomat|" +
                    "satispay|visa|mastercard|paypal|canone",
            ),
            plural = "",
            elision = Regex("^(?:l|d|un|dell|all|nell|sull|dall|c|po)'(?=\\p{L})"),
        )

        /* ------------------------------- japonês ------------------------------- */
        val JA = PhraseLang(
            language = AppLanguage.JA,
            writing = Writing.UNSPACED,
            postpositional = true,
            borrow = w("借りた|借りました|借りて|借金した|借入|ローンを組んだ|貸してもらった"),
            installmentWord = w("返済|月賦"),
            debtNoun = w("借金|ローン|住宅ローン|自動車ローン|カーローン|奨学金|借入|示談"),
            financing = w("住宅ローン|自動車ローン|カーローン"),
            loan = w("借金|借入|キャッシング|ローン"),
            agreement = w("示談|和解"),
            goalWord = w("目標|貯金箱|へそくり"),
            goalOut = w("引き出した|引き出し|取り崩した|取り崩し|下ろした|おろした"),
            goalIn = w("貯金した|貯金|貯めた|積み立てた|積み立て"),
            invest = w("投資|つみたて|nisa"),
            redeem = w("売却|解約|換金"),
            pay = w("払った|払いました|支払った|支払い|支払|返済|返した|納付"),
            transfer = w("振り込んだ|振込|振替|送金|移動|移した|移しました|送った"),
            income = w("受け取った|もらった|貰った|入金|収入|売った|稼いだ|振り込まれた"),
            expense = w("買った|買いました|買い|購入|使った|使いました|支出|かかった|買"),
            incomeNoun = w("給料|給与|ボーナス|賞与|副業|返金|配当|配当金|お小遣い|小遣い|キャッシュバック|売上|利息|報酬|手当"),
            invoice = w("カード代|カードの請求|カード請求|カードの支払い"),
            credit = w("クレジットカード|クレジット|クレカ|カード"),
            debit = w("デビットカード|デビット|口座払い"),
            cash = w("現金"),
            benefit = w("食事券|食券|チケットレストラン"),
            methodPost = "で|払い",
            toWords = ws("に|へ|まで"),
            fromWords = ws("から|より"),
            relDays = mapOf(
                "今日" to 0, "きょう" to 0, "本日" to 0, "昨日" to -1, "きのう" to -1, "一昨日" to -2, "おととい" to -2,
                "明日" to 1, "あした" to 1, "あす" to 1,
            ),
            daysAgo = listOf("(\\d{1,2}) ?日 ?前"),
            monthFirst = { true },
            monthPatterns = listOf("(?:{Y} ?年 ?)?{MN} ?月 ?{D} ?日"),
            dayOnly = listOf("(\\d{1,2}) ?日(?! ?前)"),
            weekdays = days("月曜", "火曜", "水曜", "木曜", "金曜", "土曜", "日曜"),
            weekdayPattern = "(?:{LB} ?(?:の )?)?{W}(?: ?日)?",
            lastWords = w("先週"),
            upfront = w("一括払い|一括|一回払い"),
            installments = listOf("(\\d{1,2}) ?回 ?(?:払い|払|分割)?", "分割 ?(\\d{1,2}) ?回"),
            currencyWords = w("円|えん|ドル|ユーロ"),
            multipliers = mapOf("千" to 1000L, "万" to 10_000L),
            debtGeneric = ws("借金|ローン|返済|の|に"),
            genericName = ws("口座|カード|銀行|の|クレジット|デビット|目標|投資"),
            stop = ws(
                "は|が|を|に|で|と|の|へ|から|まで|も|や|より|て|た|です|ます|ました|でした|した|しました|いました|して|った|" +
                    "円|えん|今日|昨日|明日|本日|一昨日|カード|口座|現金|目標|投資|貯金|約|くらい|ぐらい|ほど|よ|ね|けど|ので|" +
                    "私|僕|俺|払い|代金",
            ),
            bankWords = ws("振込|振替|引落|引き落とし|atm|手数料|入金|出金|デビット|カード|口座振替"),
        )

        /* ------------------------------- coreano ------------------------------- */
        val KO = PhraseLang(
            language = AppLanguage.KO,
            writing = Writing.AGGLUTINATIVE,
            postpositional = true,
            borrow = w("빌렸|빌림|빌려 받|대출받|대출 받|차입"),
            installmentWord = w("할부금|상환|원리금|월납"),
            debtNoun = w("빚|대출|부채|주택담보|전세대출|카드론|융자"),
            financing = w("주택담보|전세대출|오토론"),
            loan = w("대출|신용대출|카드론|융자"),
            agreement = w("합의"),
            goalWord = w("목표|저금통|적금|비상금"),
            goalOut = w("꺼냈|뺐|인출|찾았"),
            goalIn = w("저축|모았|적립|저금"),
            invest = w("투자|적립식|매수"),
            redeem = w("매도|환매|인출|팔았"),
            pay = w("냈|납부|갚았|지불|결제했|상환"),
            transfer = w("이체|송금|보냈|옮겼"),
            income = w("받았|벌었|입금|들어왔|팔았|수입|받음"),
            expense = w("샀|썼|지출|결제|구매|구입"),
            incomeNoun = w("월급|급여|보너스|상여금|성과급|부업|알바비|환급|배당|배당금|용돈|캐시백|이자|수당|판매"),
            invoice = w("카드값|카드 대금|카드대금|결제대금|결제 대금"),
            credit = w("신용카드|신용|카드"),
            debit = w("체크카드|체크|계좌|직불"),
            cash = w("현금"),
            benefit = w("식권|식대카드|복지카드|식비카드"),
            toWords = ws("로|으로|에|에게|한테|까지"),
            fromWords = ws("에서|부터|에게서|한테서"),
            relDays = mapOf("오늘" to 0, "어제" to -1, "그제" to -2, "그저께" to -2, "엊그제" to -2, "내일" to 1),
            daysAgo = listOf("(\\d{1,2}) ?일 ?전"),
            monthFirst = { true },
            monthPatterns = listOf("(?:{Y} ?년 ?)?{MN} ?월 ?{D} ?일"),
            dayOnly = listOf("(\\d{1,2}) ?일(?! ?전)"),
            weekdays = days("월요일", "화요일", "수요일", "목요일", "금요일", "토요일", "일요일"),
            weekdayPattern = "(?:{LB} ?)?{W}",
            lastWords = w("지난주|저번주|지난|저번"),
            upfront = w("일시불"),
            installments = listOf("(\\d{1,2}) ?개월 ?할부", "할부 ?(\\d{1,2}) ?개월"),
            currencyWords = w("원|달러|엔|유로"),
            multipliers = mapOf("천" to 1000L, "만" to 10_000L),
            debtGeneric = ws("빚|대출|할부|상환"),
            genericName = ws("계좌|카드|은행|통장|신용카드|체크카드|목표|투자"),
            stop = ws(
                "원|오늘|어제|내일|그제|카드|계좌|현금|목표|투자|저축|나|내가|저는|제가|했어|했다|했어요|함|요|그리고|좀|" +
                    "정도|쯤|약",
            ),
            bankWords = ws("이체|송금|입금|출금|체크|카드|결제|수수료|atm|카카오페이|네이버페이|토스"),
            particles = w("에게서|한테서|에서|으로|에게|한테|까지|부터|이랑|하고|로|에|을|를|은|는|이|가|도|의|와|과|랑"),
        )

        /* ------------------------------- chinês -------------------------------- */
        val ZH = PhraseLang(
            language = AppLanguage.ZH,
            writing = Writing.UNSPACED,
            borrow = w("借了|借来|借入|借钱|贷款了|贷了"),
            installmentWord = w("还款|月供|分期还款"),
            debtNoun = w("债务|欠款|贷款|房贷|车贷|借款|欠债"),
            financing = w("房贷|车贷|按揭"),
            loan = w("贷款|借款"),
            agreement = w("协议|和解"),
            goalWord = w("目标|存钱罐|小金库|储蓄目标"),
            goalOut = w("取出|取了|拿出|提取|转出"),
            goalIn = w("存了|存入|攒了|存钱|储蓄"),
            invest = w("投资|定投|买基金|买股票|理财"),
            redeem = w("赎回|卖出|提现"),
            pay = w("付了|支付|还了|缴了|缴费|交了|付款|还款"),
            transfer = w("转账|转了|转到|汇款|转给|转入"),
            income = w("收到|赚了|入账|到账|卖了|进账|收入|领了"),
            expense = w("花了|付了|消费|支出|刷了|买了|买"),
            incomeNoun = w("工资|薪水|薪资|奖金|年终奖|兼职|退款|分红|红包|零花钱|返现|利息|报销|提成|佣金"),
            invoice = w("信用卡账单|信用卡还款|还信用卡"),
            credit = w("信用卡|刷卡|信用"),
            debit = w("储蓄卡|借记卡|银行卡"),
            cash = w("现金|现钞"),
            benefit = w("饭卡|餐券|餐卡"),
            methodPrep = "用|刷",
            toWords = ws("到|给|入|进|往|至|转到"),
            fromWords = ws("从|自"),
            relDays = mapOf("今天" to 0, "今日" to 0, "昨天" to -1, "昨日" to -1, "前天" to -2, "明天" to 1),
            daysAgo = listOf("(\\d{1,2}) ?天 ?前"),
            monthFirst = { true },
            monthPatterns = listOf("(?:{Y} ?年 ?)?{MN} ?月 ?{D} ?[日号]"),
            dayOnly = listOf("(\\d{1,2}) ?[日号](?! ?前)"),
            weekdays = days(
                "星期一|周一|礼拜一", "星期二|周二|礼拜二", "星期三|周三|礼拜三", "星期四|周四|礼拜四", "星期五|周五|礼拜五",
                "星期六|周六|礼拜六", "星期日|星期天|周日|周天|礼拜天|礼拜日",
            ),
            weekdayPattern = "(?:{LB} ?)?{W}",
            lastWords = w("上个|上"),
            upfront = w("一次性付清|一次付清|全款|一次性"),
            installments = listOf("分 ?(\\d{1,2}) ?期", "(\\d{1,2}) ?期"),
            currencyWords = w("块钱|块|元|圆|人民币|美元|欧元"),
            multipliers = mapOf("千" to 1000L, "万" to 10_000L, "k" to 1000L),
            amountVerbs = w("花了|付了|收到|买了|赚了|一共|共|总共"),
            debtGeneric = ws("债务|贷款|借款|欠款|还款|的|给"),
            genericName = ws("账户|卡|银行|的|信用卡|储蓄卡|目标|投资"),
            stop = ws(
                "了|的|在|给|到|从|用|元|块|块钱|人民币|今天|昨天|前天|明天|我|是|和|跟|一共|共|总共|信用卡|现金|账户|卡|" +
                    "目标|去|又|也|个|吃了|刷",
            ),
            bankWords = ws("转账|支付|消费|取现|手续费|微信|支付宝|财付通|网银|快捷支付|pos|atm"),
        )

        /* -------------------------------- hindi -------------------------------- */
        val HI = PhraseLang(
            language = AppLanguage.HI,
            postpositional = true,
            borrow = w("उधार लिया|उधार लिए|उधार लिये|कर्ज लिया|कर्ज़ लिया|लोन लिया|उधार मिला"),
            installmentWord = w("किस्त|किश्त|किस्तें|ईएमआई|emi"),
            debtNoun = w("कर्ज|कर्ज़|कर्जा|लोन|ऋण|उधार"),
            financing = w("होम लोन|कार लोन|गृह ऋण"),
            loan = w("लोन|ऋण|पर्सनल लोन"),
            agreement = w("समझौता"),
            goalWord = w("लक्ष्य|गुल्लक"),
            goalOut = w("निकाले|निकाला|निकाली"),
            goalIn = w("बचाए|बचाया|बचत की|जमा किए|जमा किया"),
            invest = w("निवेश|एसआईपी|sip|इन्वेस्ट"),
            redeem = w("बेचे|बेचा|रिडीम"),
            pay = w("भुगतान|चुकाया|चुकाए|चुकाई|भरा|भरे|भरी|पेमेंट|दिया|दिए"),
            transfer = w("ट्रांसफर|ट्रान्सफर|भेजे|भेजा|भेजी"),
            income = w("मिले|मिला|मिली|कमाए|कमाया|बेचा|बेचे|प्राप्त|आए|आया"),
            expense = w("खर्च|खर्चे|खरीदा|खरीदे|खरीदी"),
            incomeNoun = w("वेतन|तनख्वाह|सैलरी|बोनस|रिफंड|लाभांश|कैशबैक|ब्याज|कमीशन|बिक्री"),
            invoice = w("क्रेडिट कार्ड बिल|कार्ड बिल|कार्ड का बिल"),
            credit = w("क्रेडिट कार्ड|क्रेडिट|कार्ड|credit card|card"),
            debit = w("डेबिट कार्ड|डेबिट|यूपीआई|upi|फोनपे|phonepe|पेटीएम|paytm|gpay|गूगल पे"),
            cash = w("नकद|नगद|कैश|cash"),
            benefit = w("मील कूपन|सोडेक्सो|sodexo|pluxee"),
            methodPost = "से|द्वारा",
            toWords = ws("में|को|पर"),
            fromWords = ws("से"),
            relDays = mapOf("आज" to 0, "कल" to -1, "परसों" to -2),
            daysAgo = listOf("(\\d{1,2}) दिन पहले"),
            months = days(
                "जनवरी", "फ़रवरी|फरवरी", "मार्च", "अप्रैल|अप्रेल", "मई", "जून",
                "जुलाई", "अगस्त", "सितंबर|सितम्बर", "अक्टूबर|अक्तूबर", "नवंबर|नवम्बर", "दिसंबर|दिसम्बर",
            ),
            monthPatterns = listOf("{D} {M}(?: {Y})?"),
            dayOnly = listOf("(\\d{1,2}) तारीख"),
            weekdays = days("सोमवार", "मंगलवार", "बुधवार", "गुरुवार|बृहस्पतिवार", "शुक्रवार", "शनिवार", "रविवार"),
            weekdayPattern = "(?:{LB} )?{W}(?: को)?",
            lastWords = w("पिछले|पिछला"),
            upfront = w("एकमुश्त|एक बार में|पूरा भुगतान"),
            installments = listOf("(\\d{1,2}) (?:किस्तों|किस्त|किश्तों|किश्त|emi|ईएमआई)"),
            currencyWords = w("रुपये|रुपए|रुपया|रुपयों|रु|rs|inr|डॉलर"),
            multipliers = mapOf("हज़ार" to 1000L, "हजार" to 1000L, "लाख" to 100_000L, "करोड़" to 10_000_000L, "करोड" to 10_000_000L, "k" to 1000L),
            bareThousand = w("हज़ार|हजार"),
            debtGeneric = ws("कर्ज|कर्ज़|लोन|ऋण|उधार|किस्त|को|का|की|के|से"),
            genericName = ws("खाता|कार्ड|बैंक|का|की|के|मेरा|मेरी|क्रेडिट|डेबिट|लक्ष्य|निवेश"),
            stop = ws(
                "खर्च|खर्चे|किए|किया|किये|की|के|का|को|में|से|पर|ने|है|हैं|था|थे|और|रुपये|रुपए|रुपया|रु|rs|आज|कल|परसों|" +
                    "मैंने|मेरा|मेरी|मेरे|दिए|दिया|मिले|मिला|भेजे|भेजा|खरीदा|खरीदे|भुगतान|कार्ड|खाता|नकद|लिए|लिया|लिये|पे|" +
                    "द्वारा|₹",
            ),
            bankWords = ws("upi|neft|imps|rtgs|atm|emi|पेमेंट|ट्रांसफर|pos|nach|ecs|चार्ज"),
        )
    }
}
