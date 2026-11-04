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

    @Test fun changelogsDaVersaoAtual() { // PRC-04, RF-LNG-07
        val gradle = File(app, "build.gradle.kts").readText()
        val code = Regex("""versionCode = (\d+)""").find(gradle)!!.groupValues[1]
        val name = Regex("""versionName = "([^"]+)"""").find(gradle)!!.groupValues[1]
        assertTrue("CHANGELOG.md sem a seção ## $name", Regex("""(?m)^## ${Regex.escape(name)}\b""").containsMatchIn(File(root, "CHANGELOG.md").readText()))
        for (lang in STORE_LANGS) {
            val dir = File(root, "fastlane/metadata/android/$lang/changelogs")
            assertTrue("Falta o changelog $lang/$code.txt", File(dir, "$code.txt").exists())
            dir.listFiles()!!.forEach { f ->
                val n = f.readText().trim().length
                assertTrue("${f.relativeTo(root).path} tem $n caracteres (máximo 500)", n <= 500)
            }
        }
    }

    /* ------------------------------------ línguas ------------------------------------ */

    private val res = File(app, "src/main/res")

    /** Textos de um strings.xml: chave (plurais como "chave#quantidade") -> texto. */
    private fun strings(file: File): Map<String, String> {
        val doc = javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        val out = LinkedHashMap<String, String>()
        val nodes = doc.documentElement.childNodes
        for (i in 0 until nodes.length) {
            val e = nodes.item(i) as? org.w3c.dom.Element ?: continue
            val name = e.getAttribute("name")
            when (e.tagName) {
                "string" -> if (e.getAttribute("translatable") != "false") out[name] = e.textContent
                "plurals" -> {
                    val items = e.getElementsByTagName("item")
                    for (j in 0 until items.length) {
                        val it = items.item(j) as org.w3c.dom.Element
                        out["$name#${it.getAttribute("quantity")}"] = it.textContent
                    }
                }
            }
        }
        return out
    }

    private val placeholder = Regex("""%(\d+\$)?[-#+0,(]*\d*(?:\.\d+)?[sdfxXc]""")
    private fun marks(text: String) = placeholder.findAll(text).map { it.value }.sorted().toList()

    @Test fun asDezLinguasTemTodosOsTextos() { // RF-LNG-01, RF-LNG-04
        val folders = res.listFiles()!!.filter { it.isDirectory && it.name.startsWith("values") && File(it, "strings.xml").exists() }
            .map { it.name }.toSet()
        assertEquals("Pastas de texto: a base (pt-BR) e as 9 línguas do RF-LNG-01", APP_LANGS, folders)

        val base = strings(File(res, "values/strings.xml"))
        val baseKeys = base.keys.map { it.substringBefore('#') }.toSet()
        val problems = ArrayList<String>()
        for (folder in folders - "values") {
            val tr = strings(File(res, "$folder/strings.xml"))
            val keys = tr.keys.map { it.substringBefore('#') }.toSet()
            (baseKeys - keys).forEach { problems += "$folder: falta $it" }
            (keys - baseKeys).forEach { problems += "$folder: sobra $it" }
            for ((k, v) in tr) {
                // Japonês, coreano e chinês só têm a forma "other" dos plurais: compara com a "other" da base.
                val ref = base[k] ?: base[k.substringBefore('#') + "#other"] ?: continue
                if (marks(ref) != marks(v)) problems += "$folder: marcadores diferentes em $k (${marks(ref)} x ${marks(v)})"
            }
            base.keys.filter { it.endsWith("#other") }.forEach { if (it !in tr) problems += "$folder: plural sem \"other\" em $it" }
        }
        assertEquals("Traduções incompletas ou com marcadores trocados", emptyList<String>(), problems.take(40))
    }

    @Test fun nenhumTextoEscritoNoCodigo() { // RF-LNG-03
        // Telas e notificações: todo texto vem do strings.xml. Um literal com palavras (letras e espaço) é texto escrito à mão.
        val literal = Regex(""""((?:[^"\\]|\\.)*)"""")
        val words = Regex("""\p{L}{2,}\s+\p{L}{2,}""")
        val template = Regex("""\$\{[^}]*\}|\$\w+""")
        val ignore = Regex("""Regex\(|label = "|error\(|check\(|require\(|Log\.|const val ROUTE|SPDX|"\$\{|\.putExtra\("""")
        val dirs = listOf(File(main, "ui"), File(main, "notify"))
        val found = dirs.flatMap { kotlinFiles(it) }.flatMap { f ->
            f.readLines().mapIndexedNotNull { i, raw ->
                val line = raw.substringBefore("//").trim()
                if (line.startsWith("*") || line.startsWith("/*") || ignore.containsMatchIn(line)) return@mapIndexedNotNull null
                literal.findAll(line).map { it.groupValues[1] }.firstOrNull { words.containsMatchIn(it.replace(template, " ")) }
                    ?.let { "${f.relativeTo(root).path}:${i + 1}: ${it.take(60)}" }
            }
        }
        assertEquals("Texto escrito no código (leve para res/values/strings.xml e traduza)", emptyList<String>(), found)
    }

    @Test fun lojaNasDezLinguas() { // RF-LNG-07
        val base = File(root, "fastlane/metadata/android")
        val problems = STORE_LANGS.flatMap { lang ->
            listOf("title.txt", "short_description.txt", "full_description.txt").mapNotNull { f ->
                val file = File(base, "$lang/$f")
                when {
                    !file.exists() -> "falta $lang/$f"
                    f == "title.txt" && file.readText().trim().length > 50 -> "$lang/$f com mais de 50 caracteres"
                    f == "short_description.txt" && file.readText().trim().length > 80 -> "$lang/$f com mais de 80 caracteres"
                    else -> null
                }
            }
        }
        assertEquals("Textos da loja (F-Droid) nas 10 línguas", emptyList<String>(), problems)
    }

    private companion object {
        val APP_LANGS = setOf("values", "values-en", "values-es", "values-fr", "values-de", "values-it", "values-ja", "values-ko", "values-zh-rCN", "values-hi")
        val STORE_LANGS = listOf("pt-BR", "en-US", "es-ES", "fr-FR", "de-DE", "it-IT", "ja-JP", "ko-KR", "zh-CN", "hi-IN")
    }
}
