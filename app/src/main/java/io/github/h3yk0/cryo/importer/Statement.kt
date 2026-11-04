// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.importer

import io.github.h3yk0.cryo.R
import io.github.h3yk0.cryo.domain.Res
import java.time.LocalDate

/*
 * Importação de extratos.
 *
 * Cada formato (OFX, CSV, planilha XLSX, PDF e o JSON de testes) vira a mesma coisa: um [Statement], a lista de
 * movimentações que o arquivo traz. Depois a pessoa confere tudo numa tela antes de qualquer coisa entrar no Cryo.
 * Nada sai do celular: o arquivo é lido aqui mesmo.
 */

/** O nome vem na hora de mostrar, na língua do app (OFX, CSV e PDF são iguais em todas). */
enum class StatementFormat(private val labelRes: Int?, private val fixed: String = "") {
    OFX(null, "OFX"),
    CSV(null, "CSV"),
    XLSX(R.string.format_xlsx),
    HTML(R.string.format_html),
    PDF(null, "PDF"),
    JSON(R.string.format_json);

    val label: String get() = labelRes?.let { Res.s(it) } ?: fixed
}

/**
 * Uma movimentação do extrato.
 * [amount] em centavos, com sinal: negativo = o dinheiro saiu; positivo = entrou.
 */
data class StatementLine(
    val date: LocalDate,
    val description: String,
    val amount: Long,
    /** Identificador que o banco dá à movimentação (FITID do OFX, "Identificador" do CSV...), quando existe. */
    val id: String? = null,
    /** O trecho do arquivo de onde a linha saiu (para o relatório do modo desenvolvedor). */
    val source: String = "",
)

/** Um pedaço do arquivo que não virou movimentação, e por quê (para o relatório). */
data class SkippedLine(val source: String, val reason: String)

data class Statement(
    val format: StatementFormat,
    val lines: List<StatementLine>,
    val skipped: List<SkippedLine> = emptyList(),
    /** O arquivo é de cartão de crédito (fatura)? */
    val isCard: Boolean = false,
    /** Banco e conta escritos no arquivo, quando ele diz (ex.: "Banco 260 · conta 1234-5"). */
    val accountHint: String? = null,
    /** Saldo no fim do extrato, quando o arquivo traz. */
    val balance: Long? = null,
    /** Como o arquivo foi lido (separador, codificação, colunas...). Aparece no relatório do modo desenvolvedor. */
    val notes: List<String> = emptyList(),
)

/** Erro com mensagem pronta para mostrar à pessoa. */
class StatementException(message: String) : Exception(message)
