// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.importer

import io.github.h3yk0.cryo.R
import io.github.h3yk0.cryo.domain.AppLocale
import io.github.h3yk0.cryo.domain.Res
import io.github.h3yk0.cryo.domain.normalized
import java.time.LocalDate
import kotlin.math.abs

/** Uma célula de tabela (CSV ou planilha). Planilhas já trazem número e data prontos. */
data class Cell(val text: String, val number: Double? = null, val date: LocalDate? = null) {
    val isBlank: Boolean get() = text.isBlank() && number == null && date == null
}

typealias Table = List<List<Cell>>

/** O que cada coluna significa. Índices começam em 0; -1 = a planilha não tem cabeçalho. */
data class ColumnMap(
    val headerRow: Int,
    val date: Int,
    val description: Int,
    /** Coluna única com o valor (com sinal). */
    val amount: Int? = null,
    /** Ou duas colunas: entradas e saídas. */
    val credit: Int? = null,
    val debit: Int? = null,
    /** Coluna "C/D", "Tipo" (crédito/débito). */
    val kind: Int? = null,
    val id: Int? = null,
    /** Outras colunas de texto que completam a descrição (ex.: "Lançamento" + "Detalhes"). */
    val extra: List<Int> = emptyList(),
    /** Coluna "Parcela" ("2/3") das faturas que guardam a parcela à parte da descrição. */
    val installment: Int? = null,
) {
    val isValid: Boolean get() = date >= 0 && description >= 0 && (amount != null || credit != null || debit != null)
}

/* ====================================== CSV ====================================== */

object Csv {
    /** Lê o CSV descobrindo o separador (; , tab |) e respeitando aspas. */
    fun read(text: String): Pair<Table, Char> {
        val candidates = listOf(';', ',', '\t', '|')
        val sample = text.lineSequence().filter { it.isNotBlank() }.take(40).joinToString("\n")
        val best = candidates.maxBy { sep ->
            val counts = split(sample, sep).map { it.size }.filter { it > 1 }
            if (counts.isEmpty()) 0 else {
                val mode = counts.groupingBy { it }.eachCount().maxBy { it.value }
                mode.value * 10 + mode.key
            }
        }
        return split(text, best).map { row -> row.map { Cell(it.trim()) } } to best
    }

    private fun split(text: String, sep: Char): List<List<String>> {
        val rows = ArrayList<List<String>>()
        var row = ArrayList<String>()
        val cell = StringBuilder()
        var quoted = false
        var i = 0
        while (i < text.length) {
            val ch = text[i]
            when {
                quoted && ch == '"' && i + 1 < text.length && text[i + 1] == '"' -> { cell.append('"'); i++ }
                ch == '"' && (quoted || cell.isBlank()) -> { quoted = !quoted; if (!quoted) Unit else cell.setLength(0) }
                !quoted && ch == sep -> { row += cell.toString(); cell.setLength(0) }
                !quoted && (ch == '\n' || ch == '\r') -> {
                    if (ch == '\r' && i + 1 < text.length && text[i + 1] == '\n') i++
                    row += cell.toString(); cell.setLength(0)
                    // Linhas em branco continuam contando: assim "linha 5" no relatório é a linha 5 do arquivo.
                    rows += row
                    row = ArrayList()
                }
                else -> cell.append(ch)
            }
            i++
        }
        row += cell.toString()
        if (row.any { it.isNotBlank() }) rows += row
        return rows
    }
}

/* ================================ colunas e linhas ================================ */

object Columns {
    private enum class Role { DATE, DESCRIPTION, AMOUNT, CREDIT, DEBIT, BALANCE, KIND, ID, INSTALLMENT }

    /**
     * Nomes de coluna nas 10 línguas do app (RF-IMP-21), já normalizados (minúsculas, sem acento nas letras latinas).
     * Ex.: "Data"/"Date"/"Fecha"/"Datum"/"日付"/"날짜"/"日期"/"तारीख".
     */
    private val NAMES: List<Pair<Role, List<String>>> = listOf(
        Role.DATE to listOf(
            "data", "date", "dt", "data lancamento", "data de lancamento", "data da transacao", "data mov",
            "data movimento", "data do movimento", "data compra", "data da compra", "dia",
            "transaction date", "posting date", "posted date", "post date", "booking date", "value date", "trans date",
            "txn date", "tran date", "value dt", "date posted", "fecha", "fecha operacion", "fecha de operacion", "fecha valor",
            "f. operacion", "fecha movimiento", "fecha de la transaccion", "date operation", "date de l'operation",
            "date valeur", "date comptable", "datum", "buchungstag", "buchungsdatum", "valuta", "wertstellung",
            "data operazione", "data contabile", "data valuta", "日付", "取引日", "利用日", "ご利用日", "お取引日", "年月日",
            "날짜", "거래일", "거래일자", "거래일시", "이용일", "이용일자", "日期", "交易日期", "记账日期", "交易时间", "入账日期",
            "तारीख", "दिनांक", "तिथि",
        ),
        Role.DESCRIPTION to listOf(
            "descricao", "historico", "description", "title", "titulo", "lancamento", "lancamentos",
            "estabelecimento", "detalhes", "detalhe", "memo", "nome", "transacao", "movimentacao", "descricao do lancamento",
            "details", "narration", "payee", "merchant", "transaction details", "particulars", "transaction description",
            "name", "concepto", "descripcion", "detalle", "movimiento", "comercio", "beneficiario", "libelle",
            "libelle operation", "libelle de l'operation", "detail", "beneficiaire", "verwendungszweck", "buchungstext",
            "beschreibung", "empfanger", "auftraggeber/empfanger", "begunstigter/zahlungspflichtiger", "descrizione",
            "causale", "dettagli", "esercente", "摘要", "内容", "ご利用店名", "利用店名", "お取引内容", "取引内容", "明細",
            "ご利用先", "내용", "적요", "거래내용", "가맹점", "가맹점명", "이용가맹점", "기재내용", "交易摘要", "说明", "备注",
            "商户", "交易对方", "商品说明", "对方户名", "विवरण", "ब्योरा", "नैरेशन",
        ),
        Role.AMOUNT to listOf(
            "valor", "amount", "valor (r$)", "valor r$", "valor em r$", "quantia", "montante", "valor rs",
            "valor da transacao", "valor do lancamento", "transaction amount", "value", "importe", "monto", "cantidad",
            "montant", "betrag", "umsatz", "importo", "金額", "ご利用金額", "利用金額", "お取引金額", "금액", "거래금액",
            "이용금액", "결제금액", "金额", "交易金额", "发生额", "राशि", "रकम",
        ),
        Role.CREDIT to listOf(
            "credito", "creditos", "entrada", "entradas", "valor credito", "credito (r$)", "entradas (r$)",
            "credit", "credits", "deposit", "deposits", "money in", "paid in", "credit amount", "deposit amt",
            "deposit amount", "abono", "abonos", "ingreso", "ingresos", "haber", "recettes", "entree", "haben",
            "gutschrift", "eingang", "accrediti", "avere", "entrate", "入金", "お預入れ", "預入金額", "入金額", "입금",
            "입금액", "입금금액", "맡기신금액", "收入", "贷方", "收入金额", "存入", "जमा", "क्रेडिट",
        ),
        Role.DEBIT to listOf(
            "debito", "debitos", "saida", "saidas", "valor debito", "debito (r$)", "saidas (r$)",
            "debit", "debits", "withdrawal", "withdrawals", "money out", "paid out", "debit amount", "withdrawal amt",
            "withdrawal amount", "cargo", "cargos", "retiro", "retiros", "debe", "egreso", "egresos", "depenses", "sortie",
            "soll", "belastung", "ausgang", "addebiti", "dare", "uscite", "出金", "お引出し", "引出金額", "出金額",
            "お支払金額", "출금", "출금액", "찾으신금액", "출금금액", "支出", "借方", "支出金额", "निकासी", "डेबिट",
        ),
        Role.BALANCE to listOf(
            "saldo", "balance", "saldo (r$)", "saldo do dia", "running balance", "closing balance", "available balance",
            "solde", "kontostand", "残高", "差引残高", "잔액", "거래후잔액", "余额", "账户余额", "शेष", "बैलेंस",
        ),
        Role.KIND to listOf(
            "c/d", "d/c", "tipo", "natureza", "cd", "dc", "dr/cr", "cr/dr", "type", "transaction type", "sens", "soll/haben",
            "s/h", "segno", "区分", "구분", "收/支", "收支", "प्रकार",
        ),
        Role.ID to listOf(
            "identificador", "id", "fitid", "codigo da transacao", "transaction id", "ref no", "ref no.", "reference number",
            "chq/ref no", "referencia", "reference", "referenz", "riferimento", "取引番号", "거래번호", "交易单号", "流水号",
        ),
        Role.INSTALLMENT to listOf(
            "parcela", "parcelas", "parcelamento", "numero da parcela", "n parcela", "installment", "cuota", "echeance",
            "rata", "支払回数", "分割回数", "할부", "할부개월", "分期", "किस्त",
        ),
    ).map { (role, names) -> role to names.map { it.normalized() } }

    /**
     * Colunas que não são a descrição nem o valor em reais, mesmo começando com "valor" ou "nome": o valor em dólar e a
     * cotação (faturas com compras internacionais) e o nome ou o final do cartão de quem fez a compra.
     */
    private val IGNORED = Regex(
        """cotacao|nome (?:no|do) cartao|final do cartao|titular|portador|exchange rate|""" +
            """tipo de cambio|taux de change|wechselkurs|tasso di cambio|為替|환율|汇率|card member|cardholder|""" +
            """name on card|card no|card number|kartennummer|numero de tarjeta|カード番号|카드번호|卡号""",
    )

    private val CURRENCY_WORDS = listOf(
        Regex("""us\$|u\$|\busd\b|dolar|dollar""") to "USD", Regex("""r\$|\bbrl\b|\breais\b""") to "BRL",
        Regex("""€|\beur\b|\beuros?\b""") to "EUR", Regex("""£|\bgbp\b""") to "GBP", Regex("""円|\bjpy\b""") to "JPY",
        Regex("""원|\bkrw\b""") to "KRW", Regex("""元|\bcny\b|\brmb\b""") to "CNY", Regex("""₹|\binr\b""") to "INR",
    )

    /** A moeda que o nome da coluna cita ("Valor em US$" → USD, "Amount (EUR)" → EUR), se citar. */
    private fun headerCurrency(n: String): String? = CURRENCY_WORDS.firstOrNull { it.first.containsMatchIn(n) }?.second

    private val INSTALLMENT_TEXT = Regex("""^(\d{1,3})\s*(?:/|de)\s*(\d{1,3})$""")

    private fun roleOf(header: String): Role? {
        val n = header.normalized().replace(Regex("\\s+"), " ").trim().trimEnd(':')
        // "Data: 03/10/2026", "Créditos: R$ 5.230,00": é um resumo com valores, não o nome de uma coluna.
        if (n.isEmpty() || n.any { it.isDigit() } || IGNORED.containsMatchIn(n)) return null
        // "Valor em US$" numa fatura em reais: é o valor na moeda da compra, não o que vai para a conta.
        if (headerCurrency(n)?.let { it != AppLocale.currency.currencyCode } == true) return null
        // "Amount (USD)", "金額(円)", "Betrag (EUR)": o nome sem a moeda entre parênteses.
        val bare = n.replace(Regex("\\s*[(（][^)）]*[)）]$"), "").trim()
        for (name in listOf(n, bare)) {
            if (name.isEmpty()) continue
            NAMES.firstOrNull { (_, names) -> name in names }?.let { return it.first }
        }
        return NAMES.firstOrNull { (_, names) -> names.any { it.length >= minPrefix(it) && n.startsWith(it) } }?.first
    }

    /** Um nome que começa o cabeçalho: 4 letras no latim ("valor ..."), 2 em japonês, coreano e chinês ("金額..."). */
    private fun minPrefix(name: String): Int = if (name.any { it.code >= 0x3000 }) 2 else 4

    /** Descobre as colunas: pelo cabeçalho (que pode vir depois de algumas linhas com nome do banco e período) ou pelo conteúdo. */
    fun detect(rows: Table): ColumnMap? {
        for (r in 0 until minOf(rows.size, 40)) {
            val roles = rows[r].map { roleOf(it.text) }
            val date = roles.indexOf(Role.DATE)
            if (date < 0) continue
            val amount = roles.indexOf(Role.AMOUNT).takeIf { it >= 0 }
            val credit = roles.indexOf(Role.CREDIT).takeIf { it >= 0 }
            val debit = roles.indexOf(Role.DEBIT).takeIf { it >= 0 }
            if (amount == null && credit == null && debit == null) continue
            val body = rows.drop(r + 1).take(60)
            // Mais de uma coluna de texto ("Lançamento" + "Detalhes"): juntas, na ordem do arquivo. Uma que só traz
            // "DEBIT"/"CREDIT" ("Details" no Chase) diz o sinal, não o nome.
            val textCols = roles.withIndex().filter { it.value == Role.DESCRIPTION }.map { it.index }
            val kindLike = textCols.filter { isKindColumn(body, it) }
            val texts = (textCols - kindLike.toSet()).ifEmpty { textCols }
            val description = texts.firstOrNull()
                ?: longestText(rows.drop(r + 1).take(30), setOfNotNull(date, amount, credit, debit)) ?: continue
            val kind = roles.indexOf(Role.KIND).takeIf { it >= 0 && it != description && isKindColumn(body, it) }
                ?: kindLike.firstOrNull { it != description }
            return ColumnMap(
                headerRow = r, date = date, description = description, amount = amount, credit = credit, debit = debit,
                kind = kind,
                id = roles.indexOf(Role.ID).takeIf { it >= 0 && it != description },
                extra = texts.drop(1),
                installment = roles.indexOf(Role.INSTALLMENT).takeIf { it >= 0 },
            )
        }
        return byContent(rows)
    }

    private val DEBIT_WORDS = setOf(
        "d", "db", "deb", "debito", "debit", "saida", "-", "dr", "withdrawal", "s", "soll", "cargo", "addebito", "dare",
        "出金", "支出", "출금", "निकासी", "डेबिट",
    )
    private val CREDIT_WORDS = setOf(
        "c", "cr", "cred", "credito", "credit", "entrada", "+", "deposit", "h", "haben", "abono", "accredito", "avere",
        "入金", "收入", "입금", "जमा", "क्रेडिट",
    )

    /** -1 = débito, 1 = crédito, 0 = não diz. */
    private fun kindSign(text: String): Int {
        val n = text.normalized().trim().trimEnd('.')
        return when (n) {
            in DEBIT_WORDS -> -1
            in CREDIT_WORDS -> 1
            else -> 0
        }
    }

    /** Uma coluna "Tipo" só vale como crédito/débito se quase todos os valores forem C/D (e não "PIX", "Compra"...). */
    private fun isKindColumn(rows: Table, col: Int): Boolean {
        val values = rows.mapNotNull { it.getOrNull(col)?.text?.takeIf { t -> t.isNotBlank() } }
        return values.isNotEmpty() && values.count { kindSign(it) != 0 } >= values.size * 0.8
    }

    /** Sem cabeçalho: a coluna que mais parece data, a que mais parece valor e o texto mais longo. */
    private fun byContent(rows: Table): ColumnMap? {
        val sample = rows.filter { r -> r.any { !it.isBlank } }.take(60)
        val width = sample.maxOfOrNull { it.size } ?: return null
        fun ratio(col: Int, test: (Cell) -> Boolean): Double {
            val cells = sample.mapNotNull { it.getOrNull(col) }.filter { !it.isBlank }
            return if (cells.isEmpty()) 0.0 else cells.count(test).toDouble() / sample.size
        }
        val date = (0 until width).maxByOrNull { c -> ratio(c) { it.date != null || Values.date(it.text, Values.ANY_YEAR) != null } }
            ?: return null
        if (ratio(date) { it.date != null || Values.date(it.text, Values.ANY_YEAR) != null } < 0.5) return null
        val amount = (0 until width).filter { it != date }
            .maxByOrNull { c -> ratio(c) { it.number != null || (Values.amount(it.text) != null && Regex("[.,]\\d{1,2}\\b").containsMatchIn(it.text)) } }
            ?: return null
        if (ratio(amount) { it.number != null || Values.amount(it.text) != null } < 0.5) return null
        val description = longestText(sample, setOf(date, amount)) ?: return null
        return ColumnMap(headerRow = -1, date = date, description = description, amount = amount)
    }

    private fun longestText(rows: Table, skip: Set<Int>): Int? {
        val width = rows.maxOfOrNull { it.size } ?: return null
        return (0 until width).filter { it !in skip }
            .maxByOrNull { c -> rows.sumOf { r -> r.getOrNull(c)?.text?.takeIf { t -> t.any(Char::isLetter) }?.length ?: 0 } }
    }

    /** Os nomes das colunas (para a tela de escolher colunas). */
    fun headers(rows: Table, map: ColumnMap?): List<String> {
        val width = rows.maxOfOrNull { it.size } ?: 0
        val header = map?.headerRow?.takeIf { it >= 0 }?.let { rows.getOrNull(it) }
        return (0 until width).map { c -> header?.getOrNull(c)?.text?.takeIf { it.isNotBlank() } ?: Res.s(R.string.importer_coluna, c + 1) }
    }

    /** Monta as movimentações a partir das colunas escolhidas. */
    fun build(rows: Table, map: ColumnMap, format: StatementFormat, today: LocalDate, notes: List<String>): Statement {
        val data = rows.drop(map.headerRow + 1)
        val amountCols = listOfNotNull(map.amount, map.credit, map.debit)
        val decimalComma = Values.guessDecimalComma(data.flatMap { r -> amountCols.mapNotNull { r.getOrNull(it)?.takeIf { c -> c.number == null }?.text } })
        // "10/31/2026" (EUA) ou "31/10/2026": decide pelas datas do arquivo (ou pelo costume da língua do app).
        val monthFirst = Values.monthFirst(data.asSequence().mapNotNull { r -> r.getOrNull(map.date)?.takeIf { it.date == null }?.text })
        // Datas sem ano ("05/10"): o ano vem da data completa mais recente da coluna (ou de hoje).
        val reference = Values.reference(
            data.asSequence().mapNotNull { it.getOrNull(map.date)?.let { c -> c.date ?: Values.date(c.text, monthFirst = monthFirst) } },
            today,
        )
        val lines = ArrayList<StatementLine>()
        val skipped = ArrayList<SkippedLine>()

        fun money(c: Cell?): Long? = when {
            c == null || c.isBlank -> null
            c.number != null -> Values.cents(c.number)
            else -> Values.amount(c.text, decimalComma)
        }

        for (row in data) {
            if (row.all { it.isBlank }) continue
            val source = row.joinToString(" | ") { it.text }
            val dc = row.getOrNull(map.date)
            val date = dc?.date ?: dc?.text?.let { Values.date(it, reference, monthFirst) }
            val description = (listOf(map.description) + map.extra).mapNotNull { row.getOrNull(it)?.text?.trim()?.takeIf { t -> t.isNotEmpty() } }
                .joinToString(" ")
                .ifBlank { row.withIndex().filter { it.index !in amountCols && it.index != map.date && it.value.text.any(Char::isLetter) }.joinToString(" ") { it.value.text.trim() } }
            if (date == null) { skipped += SkippedLine(source, Res.s(R.string.importer_sem_data)); continue }
            if (Values.isBalanceOrTotal(description)) { skipped += SkippedLine(source, Res.s(R.string.importer_linha_saldo_ou_total)); continue }
            var cents: Long? = when {
                map.amount != null -> money(row.getOrNull(map.amount))
                else -> {
                    val credit = money(map.credit?.let { row.getOrNull(it) })
                    val debit = money(map.debit?.let { row.getOrNull(it) })
                    if (credit == null && debit == null) null else abs(credit ?: 0) - abs(debit ?: 0)
                }
            }
            if (cents == null) { skipped += SkippedLine(source, Res.s(R.string.importer_sem_valor)); continue }
            map.kind?.let { k ->
                when (kindSign(row.getOrNull(k)?.text.orEmpty())) {
                    -1 -> cents = -abs(cents!!)
                    1 -> cents = abs(cents!!)
                }
            }
            if (cents == 0L) { skipped += SkippedLine(source, Res.s(R.string.importer_valor_zero)); continue }
            val id = map.id?.let { row.getOrNull(it)?.text?.trim() }?.takeIf { it.isNotEmpty() }
            lines += StatementLine(date, Values.prettify(description.ifBlank { Res.s(R.string.importer_movimentacao) }) + installment(row, map, description), cents!!, id, source)
        }

        val headers = headers(rows, map)
        fun name(i: Int?) = i?.let { "\"${headers.getOrNull(it)}\"" }
        val colNotes = listOfNotNull(
            if (map.headerRow >= 0) Res.s(R.string.importer_cabecalho_linha, map.headerRow + 1) else Res.s(R.string.importer_sem_cabecalho_colunas_pelo),
            Res.s(R.string.importer_data, name(map.date)), Res.s(R.string.importer_descricao) + (listOf(map.description) + map.extra).joinToString(" + ") { name(it).orEmpty() },
            map.amount?.let { Res.s(R.string.importer_valor, name(it)) }, map.credit?.let { Res.s(R.string.importer_entradas, name(it)) }, map.debit?.let { Res.s(R.string.importer_saidas, name(it)) },
            map.kind?.let { Res.s(R.string.importer_credito_debito, name(it)) }, map.id?.let { Res.s(R.string.importer_identificador, name(it)) },
            map.installment?.let { Res.s(R.string.importer_parcela_2, name(it)) },
            when (decimalComma) { true -> Res.s(R.string.importer_centavos_depois_virgula); false -> Res.s(R.string.importer_centavos_depois_ponto); null -> null },
            if (monthFirst) Res.s(R.string.importer_mes_antes_do_dia) else null,
        )
        return Statement(format, lines, skipped, isCard = looksLikeCard(rows, map), notes = notes + colNotes)
    }

    /**
     * " - Parcela 2/3" quando a parcela vem numa coluna própria. Sem isso, a parcela 3/3 de uma compra (mesma data da
     * compra, mesma descrição, mesmo valor da 2/3) pareceria a mesma linha do mês anterior.
     */
    private fun installment(row: List<Cell>, map: ColumnMap, description: String): String {
        val m = map.installment?.let { row.getOrNull(it)?.text?.trim() }?.let { INSTALLMENT_TEXT.find(it.normalized()) } ?: return ""
        val (n, of) = m.groupValues[1].toInt() to m.groupValues[2].toInt()
        if (of <= 1 || description.normalized().contains("parc")) return ""
        return Res.s(R.string.importer_parcela, n, of)
    }

    private val CARD_HEADERS = listOf(
        "title", "estabelecimento", "merchant", "comercio", "commercant", "esercente", "ご利用店名", "利用店名", "가맹점",
        "이용가맹점", "商户",
    ).map { it.normalized() }

    /** Faturas de cartão costumam ter "title"/"estabelecimento"/"merchant" e nenhuma coluna de saldo. */
    private fun looksLikeCard(rows: Table, map: ColumnMap): Boolean {
        val row = rows.getOrNull(map.headerRow) ?: return false
        val header = row.map { it.text.normalized() }
        return header.any { h -> CARD_HEADERS.any { h == it || h.startsWith(it) } } &&
            row.none { roleOf(it.text) == Role.BALANCE }
    }
}
