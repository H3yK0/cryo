// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.importer

import io.github.h3yk0.cryo.R
import io.github.h3yk0.cryo.domain.Res
import java.time.LocalDate

/** Descobre o formato do arquivo pelo conteúdo (não pelo nome) e lê as movimentações. */
object StatementReader {
    const val MAX_BYTES = 20 * 1024 * 1024

    sealed interface Result {
        data class Ok(val statement: Statement) : Result

        /** CSV ou planilha em que o Cryo não teve certeza das colunas: a pessoa escolhe. */
        data class NeedsColumns(val table: Table, val format: StatementFormat, val guess: ColumnMap?, val notes: List<String>) : Result

        /** PDF com senha. */
        data object NeedsPassword : Result

        data class Failed(val message: String) : Result
    }

    /**
     * [pdfPages]: tira o texto de cada página de um PDF (no app, o leitor do Android; nos testes, um texto pronto).
     * [columns]: colunas escolhidas pela pessoa (CSV e planilha).
     */
    fun read(
        bytes: ByteArray,
        today: LocalDate,
        allowJson: Boolean,
        password: String? = null,
        columns: ColumnMap? = null,
        pdfPages: (ByteArray, String?) -> List<String>,
    ): Result = try {
        when {
            bytes.isEmpty() -> Result.Failed(Res.s(R.string.importer_arquivo_esta_vazio))
            bytes.size > MAX_BYTES -> Result.Failed(Res.s(R.string.importer_arquivo_grande_demais_mais))
            isPdf(bytes) -> try {
                Result.Ok(PdfStatement.parse(pdfPages(bytes, password), today))
            } catch (e: PdfText.PasswordNeeded) {
                Result.NeedsPassword
            }
            Xlsx.looksLike(bytes) -> table(Xlsx.read(bytes), StatementFormat.XLSX, listOf(Res.s(R.string.format_xlsx)), columns, today)
            isOldExcel(bytes) -> Result.Failed(
                Res.s(R.string.importer_planilhas_formato_antigo_xls),
            )
            else -> text(bytes, today, allowJson, columns)
        }
    } catch (e: StatementException) {
        Result.Failed(e.message ?: Res.s(R.string.importer_nao_consegui_ler_esse))
    } catch (e: OutOfMemoryError) {
        Result.Failed(Res.s(R.string.importer_arquivo_grande_demais_para))
    } catch (e: Exception) {
        Result.Failed(Res.s(R.string.importer_nao_consegui_ler_esse))
    }

    private fun isPdf(bytes: ByteArray): Boolean = String(bytes, 0, minOf(bytes.size, 1024), Charsets.ISO_8859_1).contains("%PDF-")

    private fun isOldExcel(bytes: ByteArray): Boolean =
        bytes.size > 8 && bytes[0] == 0xD0.toByte() && bytes[1] == 0xCF.toByte() && bytes[2] == 0x11.toByte() && bytes[3] == 0xE0.toByte()

    private fun text(bytes: ByteArray, today: LocalDate, allowJson: Boolean, columns: ColumnMap?): Result {
        val (text, encoding) = Values.decode(bytes)
        if (text.isBlank()) return Result.Failed(Res.s(R.string.importer_arquivo_esta_vazio))
        if (Ofx.looksLike(text)) return Result.Ok(Ofx.parse(text, encoding))
        if (JsonStatement.looksLike(text)) {
            if (text.contains("\"app\"") && text.contains("\"Cryo\"")) {
                return Result.Failed(Res.s(R.string.importer_este_arquivo_backup_cryo))
            }
            return if (allowJson) Result.Ok(JsonStatement.parse(text))
            else Result.Failed(Res.s(R.string.importer_esse_arquivo_nao_parece))
        }
        if (HtmlTable.hasTable(text)) {
            // O ".xls" de muitos bancos é, por dentro, uma página com uma tabela.
            return table(HtmlTable.read(text), StatementFormat.HTML, listOf(Res.s(R.string.importer_tabela_html_excel_muitos), Res.s(R.string.importer_codificacao, encoding)), columns, today)
        }
        if (text.trimStart().startsWith("<")) {
            return Result.Failed(Res.s(R.string.importer_esse_arquivo_pagina_internet))
        }
        val (table, sep) = Csv.read(text)
        val sepName = when (sep) { ';' -> Res.s(R.string.importer_ponto_virgula); ',' -> Res.s(R.string.importer_virgula); '\t' -> Res.s(R.string.importer_tabulacao); else -> "\"$sep\"" }
        return table(table, StatementFormat.CSV, listOf(Res.s(R.string.importer_csv_separado_por, sepName), Res.s(R.string.importer_codificacao, encoding)), columns, today)
    }

    private fun table(table: Table, format: StatementFormat, notes: List<String>, columns: ColumnMap?, today: LocalDate): Result {
        if (table.none { row -> row.count { !it.isBlank } >= 2 }) return Result.Failed(Res.s(R.string.importer_nao_encontrei_tabela_movimentacoes))
        val map = columns ?: Columns.detect(table)
        if (map == null || !map.isValid) return Result.NeedsColumns(table, format, map, notes)
        val st = Columns.build(table, map, format, today, notes)
        // Colunas adivinhadas que não renderam nada: melhor a pessoa escolher.
        if (columns == null && st.lines.isEmpty()) return Result.NeedsColumns(table, format, map, notes)
        return Result.Ok(st)
    }
}
