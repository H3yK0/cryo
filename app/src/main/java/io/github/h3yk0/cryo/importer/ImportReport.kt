// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.importer

import io.github.h3yk0.cryo.R
import io.github.h3yk0.cryo.domain.Res
import io.github.h3yk0.cryo.domain.Money
import io.github.h3yk0.cryo.domain.normalized
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.abs

/**
 * Como o último arquivo foi lido (modo desenvolvedor): formato, colunas, cada movimentação e cada linha que ficou de fora.
 *
 * A versão anônima serve para mandar a quem cuida do Cryo quando um extrato não é lido direito: troca as letras por "x"
 * e os números por "9", mas mantém o formato de cada linha (separadores, sinais, tamanho), que é o que importa para
 * corrigir a leitura. Palavras comuns de extrato ("saldo", "pix", "pagamento"...) continuam, porque explicam o que a
 * linha é sem dizer nada sobre a pessoa.
 */
data class ImportReport(
    val fileName: String,
    val size: Long,
    val at: LocalDateTime,
    /** O extrato lido, só com as primeiras linhas (o relatório não precisa guardar um arquivo enorme na memória). */
    val statement: Statement? = null,
    /** O que deu errado, ou por que o Cryo precisou perguntar algo (colunas, senha). */
    val message: String? = null,
    /** Quantas entraram no Cryo depois da conferência. */
    val imported: Int? = null,
    /** Quantas movimentações e linhas de fora o arquivo tinha ao todo. */
    val totalLines: Int = statement?.lines?.size ?: 0,
    val totalSkipped: Int = statement?.skipped?.size ?: 0,
) {
    fun text(anonymous: Boolean, appVersion: String): String = buildString {
        fun a(s: String) = if (anonymous) anonymize(s) else s
        fun money(cents: Long): String {
            val sign = if (cents < 0) "−" else "+"
            return "$sign R$ " + a(Money.format(abs(cents), symbol = false))
        }
        // As notas são do Cryo, mas citam entre aspas o que veio do arquivo (nomes de colunas): isso também é trocado.
        fun note(s: String) = if (anonymous) QUOTED.replace(s) { m -> "\"" + anonymize(m.groupValues[1]) + "\"" } else s

        appendLine(Res.s(R.string.report_relatorio_leitura_extrato_cryo, appVersion))
        appendLine(Res.s(R.string.report_gerado, at.format(STAMP)))
        if (anonymous) appendLine(Res.s(R.string.report_versao_anonima_letras_viram))
        appendLine()
        appendLine(Res.s(R.string.report_arquivo_2, if (anonymous) anonymousName(fileName) else fileName, sizeText(size)))
        val st = statement
        if (st != null) {
            appendLine(Res.s(R.string.report_formato, st.format.label))
            appendLine(Res.s(R.string.report_tipo) + if (st.isCard) Res.s(R.string.report_fatura_cartao) else Res.s(R.string.report_extrato_conta))
            st.accountHint?.let { appendLine(Res.s(R.string.report_conta_escrita_arquivo, a(it))) }
            st.balance?.let { appendLine(Res.s(R.string.report_saldo_fim_extrato, money(it))) }
            if (st.notes.isNotEmpty()) {
                appendLine(Res.s(R.string.report_como_foi_lido))
                st.notes.forEach { appendLine("  · ${note(it)}") }
            }
            appendLine()
            appendLine(Res.s(R.string.report_movimentacoes_encontradas, totalLines))
            st.lines.take(MAX_LINES).forEachIndexed { i, l ->
                val id = l.id?.let { Res.s(R.string.report_id, a(it.take(MAX_FIELD))) }.orEmpty()
                appendLine("${i + 1}. ${a(l.date.toString())} · ${money(l.amount)} · ${a(l.description.take(MAX_FIELD))}$id")
                if (l.source.isNotBlank()) appendLine(Res.s(R.string.report_veio, a(l.source.take(MAX_SOURCE))))
            }
            if (totalLines > MAX_LINES) appendLine(Res.s(R.string.report_mais, totalLines - MAX_LINES))
            appendLine()
            appendLine(Res.s(R.string.report_linhas_que_ficaram_fora, totalSkipped))
            st.skipped.take(MAX_LINES).forEach { appendLine("  · ${it.reason}: ${a(it.source.take(MAX_SOURCE))}") }
            if (totalSkipped > MAX_LINES) appendLine(Res.s(R.string.report_mais, totalSkipped - MAX_LINES))
        }
        message?.let {
            appendLine()
            appendLine(Res.s(R.string.report_resultado, it))
        }
        imported?.let {
            appendLine()
            appendLine(Res.s(R.string.report_importadas_depois_conferencia, it))
        }
    }

    companion object {
        /** Linhas mostradas no relatório (e guardadas na memória). */
        const val MAX_LINES = 500
        private const val MAX_SOURCE = 200
        private const val MAX_FIELD = 120
        private val QUOTED = Regex("\"([^\"]*)\"")

        /** Monta o relatório guardando só as primeiras linhas do extrato. */
        fun of(fileName: String, size: Long, at: LocalDateTime, statement: Statement?, message: String?): ImportReport =
            ImportReport(
                fileName = fileName, size = size, at = at,
                statement = statement?.copy(lines = statement.lines.take(MAX_LINES), skipped = statement.skipped.take(MAX_LINES)),
                message = message,
                totalLines = statement?.lines?.size ?: 0,
                totalSkipped = statement?.skipped?.size ?: 0,
            )

        /** Extensões que podem aparecer no relatório anônimo (qualquer outra coisa depois do ponto pode ser um nome). */
        private val EXTENSIONS = setOf("ofx", "qfx", "csv", "txt", "tsv", "xlsx", "xls", "pdf", "json", "html", "htm", "xml")
        private val STAMP: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")

        /** Palavras de extrato que não dizem nada sobre a pessoa e ajudam a entender cada linha. */
        private val KEEP = setOf(
            "saldo", "total", "totais", "anterior", "disponivel", "final", "dia", "do", "da", "de", "dos", "das", "em", "no",
            "na", "para", "por", "com", "e", "a", "o", "r", "rs", "pix", "ted", "doc", "tef", "compra", "compras", "pagamento",
            "pagamentos", "pagto", "pgto", "pag", "fatura", "cartao", "credito", "creditos", "debito", "debitos", "transferencia",
            "transf", "enviada", "enviado", "recebida", "recebido", "recebimento", "estorno", "tarifa", "juros", "iof", "multa",
            "rendimento", "rendimentos", "aplicacao", "resgate", "boleto", "parcela", "parcelado", "deposito", "saque",
            "salario", "entrada", "entradas", "saida", "saidas", "data", "descricao", "valor", "historico", "lancamento",
            "lancamentos", "documento", "cashback", "anuidade", "seguro", "encargos", "limite", "minimo", "vencimento",
            "automatico", "conta", "agencia", "banco", "extrato", "periodo", "c", "d", "cd", "dc", "id", "title", "amount",
            "date", "description", "brl", "qrcode", "qr", "code", "online", "internacional", "nacional", "tipo", "natureza",
        )

        /** "PIX ENVIADO JOAO 12,50" -> "PIX ENVIADO XXXX 99,99". */
        fun anonymize(text: String): String {
            val out = StringBuilder(text.length)
            var i = 0
            while (i < text.length) {
                val ch = text[i]
                when {
                    ch.isLetter() -> {
                        var j = i
                        while (j < text.length && text[j].isLetter()) j++
                        val word = text.substring(i, j)
                        if (word.normalized() in KEEP) out.append(word)
                        else word.forEach { out.append(if (it.isUpperCase()) 'X' else 'x') }
                        i = j
                        continue
                    }
                    ch.isDigit() -> out.append('9')
                    else -> out.append(ch)
                }
                i++
            }
            return out.toString()
        }

        /** O nome do arquivo pode ter o nome da pessoa: fica só a extensão. */
        fun anonymousName(name: String): String {
            val ext = name.substringAfterLast('.', "").lowercase().takeIf { it in EXTENSIONS }
            return if (ext != null) Res.s(R.string.report_arquivo, ext) else "arquivo"
        }

        fun sizeText(bytes: Long): String = when {
            bytes < 1024 -> "$bytes bytes"
            bytes < 1024 * 1024 -> String.format(java.util.Locale.ROOT, "%.1f KB", bytes / 1024.0).replace('.', ',')
            else -> String.format(java.util.Locale.ROOT, "%.1f MB", bytes / (1024.0 * 1024)).replace('.', ',')
        }
    }
}
