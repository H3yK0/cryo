// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * PRC-09: as regras do PRD (docs/PRD.md) que dá para conferir sozinho. Se este teste falha, o app (ou o PRD) saiu do que
 * foi combinado: corrija o código ou atualize o PRD, nunca o teste para "passar".
 *
 * Os testes rodam de dentro da pasta `app/`, por isso os caminhos começam em "..".
 */
class PrdConformanceTest {
    private val root = File("..").canonicalFile
    private val app = File(root, "app")
    private val main = File(app, "src/main/java/io/github/h3yk0/cryo")
    private val prd by lazy { File(root, "docs/PRD.md").readText() }

    private fun kotlinFiles(dir: File) = dir.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()

    /** Arquivos que estão no git (o que vai para o GitHub). Sem git (ex.: um zip do projeto), a regra não é conferida. */
    private fun trackedFiles(): List<String>? = runCatching {
        val p = ProcessBuilder("git", "ls-files").directory(root).redirectErrorStream(true).start()
        val out = p.inputStream.bufferedReader().readLines()
        if (!p.waitFor(30, TimeUnit.SECONDS) || p.exitValue() != 0) null else out
    }.getOrNull()

    @Test fun licencaAgplEmTodoArquivo() { // RNF-LIC-01
        val license = File(root, "LICENSE").readText()
        assertTrue("LICENSE precisa ser a GNU AGPL v3", license.contains("GNU AFFERO GENERAL PUBLIC LICENSE") && license.contains("Version 3, 19 November 2007"))
        val wrong = kotlinFiles(File(app, "src")).filter { it.useLines { l -> l.firstOrNull() } != "// SPDX-License-Identifier: AGPL-3.0-or-later" }
        assertEquals("Arquivos sem o cabeçalho SPDX da AGPL na primeira linha", emptyList<String>(), wrong.map { it.relativeTo(root).path })
    }

    @Test fun semInternetEPermissoesSoAsCombinadas() { // RNF-PRIV-01, RNF-PRIV-03
        val manifest = File(app, "src/main/AndroidManifest.xml").readText()
        val declared = Regex("""<uses-permission[^>]*android:name="([^"]+)"""").findAll(manifest).map { it.groupValues[1] }.toSet()
        assertEquals(setOf("android.permission.POST_NOTIFICATIONS", "android.permission.USE_BIOMETRIC"), declared)
        assertTrue("O app não pode ter permissão de internet", !manifest.contains("android.permission.INTERNET"))
        assertTrue("allowBackup precisa ser false", manifest.contains("""android:allowBackup="false""""))
    }

    @Test fun semExtratosNemChaveNoGit() { // RNF-PRIV-04, RNF-SEG-01
        val ignore = File(root, ".gitignore").readLines().map { it.trim() }
        assertTrue("keystore/ precisa estar no .gitignore", "keystore/" in ignore)
        val tracked = trackedFiles()
        assumeTrue("sem git aqui: não dá para conferir os arquivos versionados", tracked != null)
        val forbidden = Regex("""\.(ofx|qfx|csv|xlsx|xls|pdf|jks|keystore|p12)$""", RegexOption.IGNORE_CASE)
        val found = tracked!!.filter { forbidden.containsMatchIn(it) || it.startsWith("keystore/") || it.endsWith("keystore.properties") }
        assertEquals("Extratos ou chaves no git", emptyList<String>(), found)
    }

    @Test fun nenhumaCorFixaNasTelas() { // DS-COR-02
        val ui = File(main, "ui")
        val files = kotlinFiles(ui).filter { "theme" !in it.relativeTo(ui).path.split(File.separatorChar) }
        val found = files.flatMap { f ->
            f.readLines().mapIndexedNotNull { i, line -> if (line.contains("Color(0x")) "${f.relativeTo(root).path}:${i + 1}" else null }
        }
        assertEquals("Cor fixa numa tela: use MaterialTheme.colorScheme ou CryoTheme.colors", emptyList<String>(), found)
    }

    @Test fun todaTelaEstaNoCatalogo() { // PRC-02, PRD 5.2
        val nav = File(main, "ui/Nav.kt").readText()
        val routes = nav.substringAfter("object Routes {").substringBefore("\n}")
            .let { Regex("""const val \w+ = "([^"]+)"""").findAll(it).map { m -> m.groupValues[1] }.toList() }
        assertTrue("Não achei as rotas em Nav.kt", routes.size > 10)
        val missing = routes.filter { "`$it`" !in prd }
        assertEquals("Rotas que faltam no catálogo de telas do PRD (seção 5.2)", emptyList<String>(), missing)
    }

    @Test fun caminhosEIdsDoPrdBatem() { // PRC-02
        val tokens = Regex("`([^`\\s]+)`").findAll(prd).map { it.groupValues[1] }.toSet()
        // Caminhos com pasta: os de código são relativos ao pacote (ui/, domain/...), os outros à raiz do projeto.
        val packageDirs = setOf("ui", "domain", "data", "importer", "notify")
        val paths = tokens.filter { '/' in it && '<' !in it && "..." !in it && '{' !in it && '?' !in it && Regex("""^[\w./-]+$""").matches(it) }
            .filter { it.substringBefore('/') in packageDirs || File(root, it.substringBefore('/')).exists() || it.endsWith(".kt") || it.endsWith(".md") }
        val missingPaths = paths.filter { p ->
            val base = if (p.substringBefore('/') in packageDirs) main else root
            !File(base, p.trimEnd('/')).exists()
        }
        assertEquals("Arquivos citados no PRD que não existem", emptyList<String>(), missingPaths)

        val tests = File(app, "src/test/java/io/github/h3yk0/cryo")
        val missingTests = tokens.filter { Regex("""^[A-Z]\w+Test$""").matches(it) }.filter { !File(tests, "$it.kt").exists() }
        assertEquals("Testes citados no PRD que não existem", emptyList<String>(), missingTests)

        val idPattern = Regex("""^\| ((?:PRI|PER|JOR|ESC|TELA|RF|RNF|DAD|DS|TXT|ACS)-[A-Z0-9-]+) \|""", RegexOption.MULTILINE)
        val ids = idPattern.findAll(prd).map { it.groupValues[1] }.toList()
        assertTrue("Não achei os IDs do PRD", ids.size > 100)
        assertEquals("IDs repetidos no PRD", emptyList<String>(), ids.groupBy { it }.filter { it.value.size > 1 }.keys.toList())
    }

    @Test fun schemaDoBancoAtualExportado() { // DAD-03
        val db = File(main, "data/db/Database.kt").readText()
        val version = Regex("""version = (\d+),""").find(db)?.groupValues?.get(1)
        assertTrue("Não achei a versão do banco", version != null)
        assertTrue("Falta app/schemas/.../$version.json", File(app, "schemas/io.github.h3yk0.cryo.data.db.CryoDatabase/$version.json").exists())
    }

    @Test fun changelogsDaVersaoAtual() { // PRC-04
        val gradle = File(app, "build.gradle.kts").readText()
        val code = Regex("""versionCode = (\d+)""").find(gradle)!!.groupValues[1]
        val name = Regex("""versionName = "([^"]+)"""").find(gradle)!!.groupValues[1]
        assertTrue("CHANGELOG.md sem a seção ## $name", Regex("""(?m)^## ${Regex.escape(name)}\b""").containsMatchIn(File(root, "CHANGELOG.md").readText()))
        for (lang in listOf("pt-BR", "en-US")) {
            val dir = File(root, "fastlane/metadata/android/$lang/changelogs")
            assertTrue("Falta o changelog $lang/$code.txt", File(dir, "$code.txt").exists())
            dir.listFiles()!!.forEach { f ->
                val n = f.readText().trim().length
                assertTrue("${f.relativeTo(root).path} tem $n caracteres (máximo 500)", n <= 500)
            }
        }
    }
}
