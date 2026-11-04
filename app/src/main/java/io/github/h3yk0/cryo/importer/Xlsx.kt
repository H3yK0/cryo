// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.importer

import io.github.h3yk0.cryo.R
import io.github.h3yk0.cryo.domain.Res
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.math.BigDecimal
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Planilhas do Excel (.xlsx). Um .xlsx é um arquivo .zip com textos em XML dentro; aqui ele é lido sem bibliotecas
 * de fora, para o app continuar pequeno.
 */
object Xlsx {
    private const val MAX_ENTRY = 30L * 1024 * 1024
    private const val MAX_TOTAL = 60L * 1024 * 1024

    fun looksLike(bytes: ByteArray): Boolean =
        bytes.size > 4 && bytes[0] == 'P'.code.toByte() && bytes[1] == 'K'.code.toByte() && bytes[2] == 3.toByte() && bytes[3] == 4.toByte()

    fun read(bytes: ByteArray): Table {
        val files = unzip(bytes)
        val sheets = sheetPaths(files)
        if (sheets.isEmpty()) throw StatementException(Res.s(R.string.importer_essa_planilha_nao_tem))
        val strings = files["xl/sharedStrings.xml"]?.let(::sharedStrings) ?: emptyList()
        val dates = files["xl/styles.xml"]?.let(::dateStyles) ?: emptySet()
        // A primeira aba com conteúdo de verdade (algumas planilhas têm uma capa antes).
        var best: Table = emptyList()
        for (path in sheets) {
            val table = sheet(files[path] ?: continue, strings, dates)
            if (table.count { row -> row.count { !it.isBlank } >= 2 } >= 2) return table
            if (table.size > best.size) best = table
        }
        return best
    }

    private fun unzip(bytes: ByteArray): Map<String, ByteArray> {
        val out = HashMap<String, ByteArray>()
        var total = 0L
        try {
            ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
                while (true) {
                    val e = zip.nextEntry ?: break
                    if (e.isDirectory || !e.name.startsWith("xl/") || !e.name.endsWith(".xml") && !e.name.endsWith(".rels")) continue
                    val buf = ByteArrayOutputStream()
                    val chunk = ByteArray(16 * 1024)
                    var size = 0L
                    while (true) {
                        val n = zip.read(chunk)
                        if (n < 0) break
                        size += n; total += n
                        if (size > MAX_ENTRY || total > MAX_TOTAL) throw StatementException(Res.s(R.string.importer_essa_planilha_grande_demais))
                        buf.write(chunk, 0, n)
                    }
                    out[e.name] = buf.toByteArray()
                }
            }
        } catch (e: StatementException) {
            throw e
        } catch (e: Exception) {
            throw StatementException(Res.s(R.string.importer_nao_consegui_abrir_essa))
        }
        return out
    }

    private fun parser(xml: ByteArray): XmlPullParser =
        XmlPullParserFactory.newInstance().apply { isNamespaceAware = false }.newPullParser().apply {
            setInput(ByteArrayInputStream(xml), "UTF-8")
        }

    /** As abas na ordem da planilha. */
    private fun sheetPaths(files: Map<String, ByteArray>): List<String> {
        val rels = HashMap<String, String>()
        files["xl/_rels/workbook.xml.rels"]?.let { xml ->
            val p = parser(xml)
            while (p.next() != XmlPullParser.END_DOCUMENT) {
                if (p.eventType == XmlPullParser.START_TAG && p.name == "Relationship") {
                    val id = p.getAttributeValue(null, "Id") ?: continue
                    val target = p.getAttributeValue(null, "Target") ?: continue
                    rels[id] = if (target.startsWith("/")) target.removePrefix("/") else "xl/" + target.removePrefix("./")
                }
            }
        }
        val ordered = ArrayList<String>()
        files["xl/workbook.xml"]?.let { xml ->
            val p = parser(xml)
            while (p.next() != XmlPullParser.END_DOCUMENT) {
                if (p.eventType == XmlPullParser.START_TAG && p.name == "sheet") {
                    val rid = (0 until p.attributeCount).firstOrNull { p.getAttributeName(it).endsWith(":id") || p.getAttributeName(it) == "id" }
                        ?.let { p.getAttributeValue(it) }
                    rels[rid]?.let { if (it in files) ordered += it }
                }
            }
        }
        if (ordered.isNotEmpty()) return ordered
        return files.keys.filter { it.startsWith("xl/worksheets/") && it.endsWith(".xml") }.sorted()
    }

    private fun sharedStrings(xml: ByteArray): List<String> {
        val out = ArrayList<String>()
        val p = parser(xml)
        val sb = StringBuilder()
        var inSi = false
        var inT = false
        var inPhonetic = false
        while (p.next() != XmlPullParser.END_DOCUMENT) {
            when (p.eventType) {
                XmlPullParser.START_TAG -> when (p.name) {
                    "si" -> { inSi = true; sb.setLength(0) }
                    "t" -> inT = inSi
                    "rPh" -> inPhonetic = true
                }
                XmlPullParser.TEXT -> if (inT && !inPhonetic) sb.append(p.text)
                XmlPullParser.END_TAG -> when (p.name) {
                    "si" -> { out += sb.toString(); inSi = false }
                    "t" -> inT = false
                    "rPh" -> inPhonetic = false
                }
            }
        }
        return out
    }

    /** Índices dos estilos de célula que mostram data. */
    private fun dateStyles(xml: ByteArray): Set<Int> {
        val custom = HashMap<Int, String>()
        val xfs = ArrayList<Int>()
        val p = parser(xml)
        var inCellXfs = false
        while (p.next() != XmlPullParser.END_DOCUMENT) {
            when (p.eventType) {
                XmlPullParser.START_TAG -> when (p.name) {
                    "numFmt" -> {
                        val id = p.getAttributeValue(null, "numFmtId")?.toIntOrNull()
                        val code = p.getAttributeValue(null, "formatCode")
                        if (id != null && code != null) custom[id] = code
                    }
                    "cellXfs" -> inCellXfs = true
                    "xf" -> if (inCellXfs) xfs += p.getAttributeValue(null, "numFmtId")?.toIntOrNull() ?: 0
                }
                XmlPullParser.END_TAG -> if (p.name == "cellXfs") inCellXfs = false
            }
        }
        return xfs.withIndex().filter { (_, id) -> isDateFormat(id, custom[id]) }.map { it.index }.toSet()
    }

    private fun isDateFormat(id: Int, code: String?): Boolean {
        if (id in 14..17 || id == 22 || id in 27..36 || id in 50..58) return true
        val c = code?.lowercase()?.replace(Regex("\"[^\"]*\"|\\[[^]]*]"), "") ?: return false
        return c.contains('d') || c.contains('y')
    }

    private fun column(ref: String): Int {
        var n = 0
        for (ch in ref) {
            if (!ch.isLetter()) break
            n = n * 26 + (ch.uppercaseChar() - 'A' + 1)
        }
        return n - 1
    }

    private fun sheet(xml: ByteArray, strings: List<String>, dates: Set<Int>): Table {
        val rows = ArrayList<List<Cell>>()
        val p = parser(xml)
        var row: MutableMap<Int, Cell>? = null
        var ref: String? = null
        var type: String? = null
        var style = 0
        val value = StringBuilder()
        var inValue = false
        var nextCol = 0
        var rowNumber = 0

        fun finishCell() {
            val col = ref?.let(::column)?.takeIf { it >= 0 } ?: nextCol
            nextCol = col + 1
            val v = value.toString()
            val cell = when (type) {
                "s" -> Cell(strings.getOrNull(v.trim().toIntOrNull() ?: -1).orEmpty())
                "inlineStr", "str" -> Cell(v)
                "b" -> Cell(if (v.trim() == "1") "VERDADEIRO" else "FALSO")
                "e" -> Cell("")
                "d" -> Cell(v, date = Values.date(v))
                else -> {
                    val num = v.trim().toDoubleOrNull()
                    when {
                        num == null -> Cell(v)
                        style in dates -> Values.excelDate(num)?.let { Cell(it.toString(), date = it) } ?: Cell(v)
                        else -> Cell(BigDecimal(v.trim()).stripTrailingZeros().toPlainString(), number = num)
                    }
                }
            }
            row?.set(col, cell)
        }

        while (p.next() != XmlPullParser.END_DOCUMENT) {
            when (p.eventType) {
                XmlPullParser.START_TAG -> when (p.name) {
                    "row" -> {
                        row = HashMap()
                        nextCol = 0
                        val r = p.getAttributeValue(null, "r")?.toIntOrNull()
                        // Linhas vazias puladas no arquivo continuam existindo (o cabeçalho pode estar na linha 5).
                        if (r != null) while (rowNumber < r - 1) { rows.add(emptyList()); rowNumber++ }
                    }
                    "c" -> {
                        ref = p.getAttributeValue(null, "r")
                        type = p.getAttributeValue(null, "t")
                        style = p.getAttributeValue(null, "s")?.toIntOrNull() ?: 0
                        value.setLength(0)
                    }
                    "v", "t" -> inValue = true
                }
                XmlPullParser.TEXT -> if (inValue) value.append(p.text)
                XmlPullParser.END_TAG -> when (p.name) {
                    "v", "t" -> inValue = false
                    "c" -> finishCell()
                    "row" -> {
                        val cells = row.orEmpty()
                        val width = (cells.keys.maxOrNull() ?: -1) + 1
                        rows.add(List(width) { cells[it] ?: Cell("") })
                        rowNumber++
                        row = null
                    }
                }
            }
        }
        return rows
    }

    /* ------------------------------ gravar (exemplos) ------------------------------ */

    /** Monta um .xlsx simples. Datas viram datas de verdade do Excel; números, números. */
    fun write(rows: List<List<Any?>>): ByteArray {
        fun esc(s: String) = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
        fun colName(i: Int): String {
            var n = i + 1
            val sb = StringBuilder()
            while (n > 0) { val r = (n - 1) % 26; sb.insert(0, 'A' + r); n = (n - 1) / 26 }
            return sb.toString()
        }
        val sheet = StringBuilder()
        sheet.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        sheet.append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetData>""")
        rows.forEachIndexed { r, row ->
            sheet.append("<row r=\"${r + 1}\">")
            row.forEachIndexed { c, v ->
                val ref = "${colName(c)}${r + 1}"
                when (v) {
                    null -> Unit
                    is LocalDate -> sheet.append("<c r=\"$ref\" s=\"1\"><v>${ChronoUnit.DAYS.between(LocalDate.of(1899, 12, 30), v)}</v></c>")
                    is Number -> sheet.append("<c r=\"$ref\"><v>$v</v></c>")
                    else -> sheet.append("<c r=\"$ref\" t=\"inlineStr\"><is><t>${esc(v.toString())}</t></is></c>")
                }
            }
            sheet.append("</row>")
        }
        sheet.append("</sheetData></worksheet>")

        val files = linkedMapOf(
            "[Content_Types].xml" to """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/><Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/><Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/></Types>""",
            "_rels/.rels" to """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/></Relationships>""",
            "xl/workbook.xml" to """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><sheets><sheet name="Extrato" sheetId="1" r:id="rId1"/></sheets></workbook>""",
            "xl/_rels/workbook.xml.rels" to """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/><Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/></Relationships>""",
            "xl/styles.xml" to """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><numFmts count="1"><numFmt numFmtId="164" formatCode="dd/mm/yyyy"/></numFmts><fonts count="1"><font><sz val="11"/><name val="Calibri"/></font></fonts><fills count="1"><fill><patternFill patternType="none"/></fill></fills><borders count="1"><border/></borders><cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs><cellXfs count="2"><xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/><xf numFmtId="164" fontId="0" fillId="0" borderId="0" xfId="0" applyNumberFormat="1"/></cellXfs></styleSheet>""",
            "xl/worksheets/sheet1.xml" to sheet.toString(),
        )
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            for ((name, content) in files) {
                // Data fixa nas entradas do zip: o mesmo conteúdo gera sempre o mesmo arquivo.
                zip.putNextEntry(ZipEntry(name).apply { time = 315532800000L })
                zip.write(content.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
        }
        return out.toByteArray()
    }
}
