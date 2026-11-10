// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * PRC-08: a biblioteca de erros já corrigidos (`docs/ERROS.md`) é lei. Este teste confere que ela continua inteira: todo
 * erro tem a sua guarda, toda guarda existe no código e não foi desligada, e os documentos de trabalho apontam para ela.
 * Apagar ou renomear um teste-guarda sem atualizar a tabela faz este teste falhar.
 */
class KnownIssuesTest {
    private val root = File("..").canonicalFile
    private val rows = File(root, "docs/ERROS.md").readLines().filter { it.startsWith("| ERR-") }.map { line -> line.trim('|').split('|').map { it.trim() } }

    @Test fun todoErroTemIdLeiEGuarda() {
        assertTrue("a biblioteca encolheu: ${rows.size} linhas", rows.size >= 38)
        val ids = rows.map { it[0] }
        assertEquals("ID repetido", ids.size, ids.toSet().size)
        assertEquals("os IDs seguem a ordem, sem buracos", (1..rows.size).map { "ERR-%03d".format(it) }, ids)
        for (r in rows) {
            assertEquals("${r[0]}: 6 colunas", 6, r.size)
            assertTrue("${r[0]}: severidade", r[1] in setOf("S1", "S2", "S3"))
            assertTrue("${r[0]}: sem a lei", r[4].length >= 20)
            assertTrue("${r[0]}: sem guarda", GUARD.containsMatchIn(r[5]))
        }
    }

    @Test fun todaGuardaExisteENaoFoiDesligada() {
        val tests = File(root, "app/src/test/java/io/github/h3yk0/cryo")
        for (r in rows) for (m in GUARD.findAll(r[5])) {
            val (cls, method) = m.destructured
            val file = File(tests, "$cls.kt")
            assertTrue("${r[0]}: não existe $cls.kt", file.isFile)
            val text = file.readText()
            val at = Regex("""@Test\s+fun\s+$method\s*\(""").find(text)
            assertTrue("${r[0]}: o teste-guarda $cls.$method sumiu", at != null)
            // Nem o teste nem a classe podem estar desligados.
            val before = text.substring(maxOf(0, at!!.range.first - 200), at.range.first)
            assertTrue("${r[0]}: $cls.$method está com @Ignore", !before.substringAfterLast('}').contains("@Ignore"))
            assertTrue("${r[0]}: $cls está com @Ignore", !Regex("""@Ignore[^\n]*\n(@[^\n]*\n)*class\s+$cls\b""").containsMatchIn(text))
        }
    }

    @Test fun osDocumentosApontamParaABiblioteca() {
        for (doc in listOf("CLAUDE.md", "docs/PRD.md")) {
            assertTrue("$doc precisa mandar ler docs/ERROS.md", File(root, doc).readText().contains("docs/ERROS.md"))
        }
    }

    private companion object {
        val GUARD = Regex("""`([A-Z][A-Za-z0-9]+)\.([a-zA-Z0-9_]+)`""")
    }
}
