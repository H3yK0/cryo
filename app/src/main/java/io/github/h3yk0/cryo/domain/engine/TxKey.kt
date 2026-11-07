// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.domain.engine

import java.text.Normalizer
import java.util.Locale

/**
 * RF-MOT-02: a "chave" de uma descrição, para reconhecer a mesma coisa escrita de jeitos diferentes:
 * "NETFLIX.COM 12/03", "Netflix.com" e "netflix com" viram "netflix". Tira números (datas, "parcela 3/10", códigos),
 * acentos das letras latinas, pontuação e palavras genéricas de banco ("PIX", "COMPRA", "DEBITO").
 *
 * Feito com um laço simples sobre os caracteres, sem expressões regulares: o tempo é sempre proporcional ao texto (nenhum
 * texto malicioso consegue travar o motor) e o texto de entrada é cortado em [MAX_INPUT] caracteres.
 */
object TxKey {
    const val MAX_INPUT = 160
    private const val MAX_TOKENS = 4

    /** Palavras que os bancos e as maquininhas põem em quase tudo, em várias línguas: não dizem o que é a compra. */
    private val GENERIC = setOf(
        // português (Brasil)
        "pix", "ted", "doc", "tef", "compra", "compras", "debito", "credito", "pagamento", "pagto", "pgto", "pag", "pg",
        "transf", "transferencia", "enviado", "enviada", "recebido", "recebida", "cartao", "parc", "parcela", "deb", "cred",
        "elo", "visa", "master", "mastercard", "online", "com", "www", "ltda", "eireli", "me", "sa", "br", "bra", "brasil",
        "de", "da", "do", "das", "dos", "e",
        // outras línguas do app
        "card", "purchase", "payment", "pos", "debit", "credit", "the", "inc", "llc", "ltd", "co", "us", "usa",
        "tarjeta", "pago", "compra", "achat", "paiement", "carte", "kauf", "zahlung", "karte", "acquisto", "pagamento",
    )

    /** A chave da descrição, ou "" quando não sobra nada que identifique (só números, só palavras genéricas). */
    fun of(description: String): String {
        if (description.isEmpty()) return ""
        val cut = if (description.length > MAX_INPUT) description.substring(0, MAX_INPUT) else description
        val decomposed = Normalizer.normalize(cut.lowercase(Locale.ROOT), Normalizer.Form.NFD)
        val sb = StringBuilder(decomposed.length)
        var prevLatin = false
        for (ch in decomposed) {
            val type = Character.getType(ch)
            when {
                type == Character.NON_SPACING_MARK.toInt() -> {
                    // Acento de letra latina sai ("ç" → "c"); em hindi e outras escritas, o sinal faz parte da palavra.
                    if (!prevLatin) sb.append(ch)
                }
                Character.isLetter(ch) -> {
                    sb.append(ch)
                    prevLatin = Character.UnicodeScript.of(ch.code) == Character.UnicodeScript.LATIN
                }
                else -> {
                    if (sb.isNotEmpty() && sb[sb.length - 1] != ' ') sb.append(' ')
                    prevLatin = false
                }
            }
        }
        val out = StringBuilder()
        var count = 0
        for (token in sb.split(' ')) {
            if (token.isEmpty() || token in GENERIC) continue
            // Uma letra latina sozinha ("x", "s") não identifica nada; nas escritas sem espaço, um caractere já é uma palavra.
            if (token.length < 2 && Character.UnicodeScript.of(token[0].code) == Character.UnicodeScript.LATIN) continue
            if (out.isNotEmpty()) out.append(' ')
            out.append(token)
            if (++count == MAX_TOKENS) break
        }
        return Normalizer.normalize(out.toString(), Normalizer.Form.NFC)
    }

    /** As palavras da chave (para a categoria aprendida). */
    fun tokens(description: String): List<String> = of(description).split(' ').filter { it.isNotEmpty() }
}
