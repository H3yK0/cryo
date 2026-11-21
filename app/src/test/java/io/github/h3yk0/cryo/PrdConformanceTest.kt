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
        assertEquals(
            setOf("android.permission.POST_NOTIFICATIONS", "android.permission.USE_BIOMETRIC", "android.permission.RECORD_AUDIO", "android.permission.POST_PROMOTED_NOTIFICATIONS"),
            declared,
        )
        assertTrue("O app não pode ter permissão de internet", !manifest.contains("android.permission.INTERNET"))
        assertTrue("allowBackup precisa ser false", manifest.contains("""android:allowBackup="false""""))
    }

    @Test fun vozSemRedeECompilacaoReproduzivel() { // RF-VOZ-10, RNF-PRIV-01
        // O código de voz (Kotlin e a ponte nativa) não fala com a rede nem grava o áudio em arquivo.
        val voice = kotlinFiles(File(main, "voice")) + listOf(File(app, "src/main/cpp/cryo_whisper.cpp"), File(app, "src/main/cpp/cryo_whisper_stub.cpp"))
        val net = Regex("""\b(?:java\.net|okhttp|HttpURLConnection|URL\(|Socket|socket\(|connect\(|curl)""")
        val file = Regex("""\b(?:FileOutputStream|fopen|ofstream|MediaRecorder\.OutputFormat)""")
        fun code(f: File) = f.readText().lines().filterNot { it.trim().startsWith("//") || it.trim().startsWith("*") }.joinToString("\n")
        voice.forEach { f ->
            assertTrue("rede em ${f.name}", !net.containsMatchIn(code(f)))
            // RF-VOZ-15 (1.6): o único arquivo que a voz grava é o modelo que a pessoa escolheu, conferido, na pasta dele.
            if (f.name != "WhisperModelStore.kt") assertTrue("arquivo em ${f.name}", !file.containsMatchIn(code(f)))
        }
        val store = code(File(main, "voice/WhisperModelStore.kt"))
        assertTrue("o modelo fica em noBackupFilesDir/whisper", store.contains("File(app.noBackupFilesDir, \"whisper\")"))
        assertTrue("o guardião do modelo não mexe com áudio", !Regex("""AudioRecord|MicInput|FloatArray|pcm""").containsMatchIn(store))
        // Compilação nativa igual em qualquer máquina.
        val cmake = File(app, "src/main/cpp/CMakeLists.txt").readText()
        listOf("GGML_NATIVE OFF", "GGML_CPU_KLEIDIAI OFF", "GGML_OPENMP OFF", "-ffile-prefix-map=\${CMAKE_SOURCE_DIR}", "-ffile-prefix-map=\${ANDROID_NDK}", "-g0", "CRYO_GGML_COMMIT")
            .forEach { assertTrue("falta $it no CMakeLists.txt", cmake.contains(it)) }
        val gradle = File(app, "build.gradle.kts").readText()
        assertTrue("NDK fixo", Regex("""ndkVersion = "\d+\.\d+\.\d+"""").containsMatchIn(gradle))
        assertTrue("CMake fixo", Regex("""version = "\d+\.\d+\.\d+"""").containsMatchIn(gradle))
    }

    @Test fun nenhumEmojiDeTeclado() { // RF-ILU-01
        // Emoji = símbolos coloridos do teclado (rostos, objetos, "festa"...) e o seletor que pede a versão colorida (FE0F).
        // Setas e o "☰" (botão de menu de outros apps, citado num texto) não são emoji.
        fun isEmoji(cp: Int) = cp in 0x1F000..0x1FAFF || cp == 0xFE0F || (cp in 0x2600..0x27BF && cp != 0x2630 && cp != 0x2713)
        val files = File(app, "src/main/res").walkTopDown()
            .filter { it.isFile && it.name == "strings.xml" }.toList() + kotlinFiles(main)
        val found = files.flatMap { f ->
            f.readLines().mapIndexedNotNull { i, line ->
                val cps = line.codePoints().toArray()
                if (cps.any(::isEmoji)) "${f.name}:${i + 1}" else null
            }
        }
        assertEquals("Emoji no app (RF-ILU-01: use as ilustrações)", emptyList<String>(), found)
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
        // RF-LNG-09: o CHANGELOG.md (inglês, o da página inicial) e a cópia em português têm a seção da versão.
        for (log in listOf("CHANGELOG.md", "docs/CHANGELOG.pt-BR.md")) {
            assertTrue("$log sem a seção ## $name", Regex("""(?m)^## ${Regex.escape(name)}\b""").containsMatchIn(File(root, log).readText()))
        }
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
        assertEquals("Pastas de texto: a base (pt-BR) e as 13 línguas do RF-LNG-01 e RF-LNG-11", APP_LANGS, folders)

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
        assertEquals("Textos da loja (F-Droid) nas 14 línguas", emptyList<String>(), problems)
    }

    @Test fun semAnimacaoInfinita() { // DS-MOV-10
        val ui = File(main, "ui")
        val found = kotlinFiles(ui).flatMap { f ->
            f.readLines().mapIndexedNotNull { i, line ->
                if (Regex("""rememberInfiniteTransition|infiniteRepeatable""").containsMatchIn(line)) "${f.relativeTo(root).path}:${i + 1}" else null
            }
        }
        assertEquals("Animação infinita numa tela (DS-MOV-10)", emptyList<String>(), found)
    }

    @Test fun botaoPrincipalComVolume() { // DS-PROF-05, DS-COMP-24
        val ui = File(main, "ui")
        val found = kotlinFiles(ui).filter { it.name != "Depth.kt" }.flatMap { f ->
            f.readLines().mapIndexedNotNull { i, line ->
                if (Regex("""(?<![A-Za-z.])Button\(""").containsMatchIn(line)) "${f.relativeTo(root).path}:${i + 1}" else null
            }
        }
        assertEquals("Botão principal sem volume: use CryoButton (DS-PROF-05)", emptyList<String>(), found)
    }

    /**
     * ERR-059 (DS-PAD-03): toda folha que sobe de baixo é a `CryoSheet`. O `ModalBottomSheet` direto deixa a folha entrar
     * embaixo da barra de status, e com o conteúdo quase enchendo a tela ela tremia sem parar (a lista de idiomas na 1.6).
     */
    @Test fun folhasPelaCryoSheet() {
        val found = kotlinFiles(File(main, "ui")).filter { it.name != "Sheet.kt" }.flatMap { f ->
            f.readLines().mapIndexedNotNull { i, line ->
                if (Regex("""(?<![A-Za-z.])ModalBottomSheet\(""").containsMatchIn(line)) "${f.relativeTo(root).path}:${i + 1}" else null
            }
        }
        assertEquals("Folha sem a CryoSheet: use CryoSheet (DS-PAD-03, ERR-059)", emptyList<String>(), found)
        // A CryoSheet tira a barra de status por fora da folha: é isso que corta a conta que fazia a folha tremer.
        val sheet = File(main, "ui/components/Sheet.kt").readText()
        assertTrue("CryoSheet precisa manter a folha abaixo da barra de status", sheet.contains("windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))"))
    }

    @Test fun nadaEmLogs() { // RF-SEG-07
        val bad = kotlinFiles(main).flatMap { f ->
            f.readLines().mapIndexedNotNull { i, line ->
                val code = line.substringBefore("//")
                if (Regex("""(?<![A-Za-z])(Log\.[vdiwe]\(|println\(|printStackTrace\()""").containsMatchIn(code)) "${f.relativeTo(root).path}:${i + 1}" else null
            }
        }
        assertEquals("O app não escreve dados no registro do sistema", emptyList<String>(), bad)
    }

    @Test fun motorSemAndroidESemRede() { // RF-MOT, RF-SEG-02, RNF-PRIV-01
        val engine = File(main, "domain/engine")
        assertTrue(engine.isDirectory)
        val bad = kotlinFiles(engine).flatMap { f ->
            f.readLines().filter { it.startsWith("import android.") || it.startsWith("import java.net") || it.startsWith("import javax.net") || it.contains("URL(") }
                .map { "${f.name}: $it" }
        }
        assertEquals("O motor é puro Kotlin, sem Android e sem rede", emptyList<String>(), bad)
    }

    @Test fun iconeSemEstrela() { // DS-ICO-02, DS-ICO-03
        val drawable = File(app, "src/main/res/drawable")
        val star = "M54,44.5"
        val withStar = listOf("ic_launcher_foreground", "ic_launcher_monochrome", "ic_cryo_badge", "ic_cryo_mark")
            .filter { File(drawable, "$it.xml").readText().contains(star) }
        assertEquals("A estrela do ícone antigo voltou (DS-ICO-02)", emptyList<String>(), withStar)
    }

    @Test fun telasComAsTransicoesDoMotion() { // DS-MOV-02, DS-MOV-03
        val nav = File(main, "ui/Nav.kt").readText()
        val hosts = Regex("""NavHost\(""").findAll(nav).count()
        assertEquals("Um NavHost só, em Nav.kt", 1, hosts)
        listOf("Motion.forwardEnter()", "Motion.forwardExit()", "Motion.backEnter()", "Motion.backExit()", "Motion.tabEnter()", "Motion.tabExit()")
            .forEach { assertTrue("Nav.kt sem $it (DS-MOV-02/03)", it in nav) }
        val others = kotlinFiles(main).filter { it.name != "Nav.kt" && "NavHost(" in it.readText() }
        assertEquals("NavHost fora de Nav.kt", emptyList<File>(), others)
    }

    @Test fun readmeNasDezLinguas() { // RF-LNG-09
        val pages = listOf(File(root, "README.md")) + README_LANGS.map { File(root, "docs/readme/README.$it.md") }
        val names = listOf("English", "Português", "Español", "Français", "Deutsch", "Italiano", "日本語", "한국어", "中文", "हिन्दी", "Русский", "Українська", "العربية", "עברית")
        val link = Regex("""(?:href|src)="([^"]+)"|\]\(([^)\s]+)\)""")
        val problems = pages.flatMap { f ->
            val name = f.relativeTo(root).path
            if (!f.exists()) return@flatMap listOf("falta $name")
            val text = f.readText()
            val missingLang = names.filter { it !in text }.map { "$name sem a língua $it na linha de línguas" }
            val broken = link.findAll(text).map { it.groupValues[1].ifEmpty { it.groupValues[2] } }
                .filter { !it.startsWith("http") && !it.startsWith("#") }
                .map { it.substringBefore('#') }
                .filter { !File(f.parentFile, it).exists() }
                .map { "$name aponta para $it, que não existe" }
            missingLang + broken
        }
        assertEquals("README nas 14 línguas (RF-LNG-09)", emptyList<String>(), problems)
        assertTrue("README.md (a página principal) em inglês", "## What it does" in File(root, "README.md").readText())
    }

    @Test fun imagensDasTelasNasDezLinguas() { // RF-LNG-10
        val base = File(root, "fastlane/metadata/android")
        val missing = STORE_LANGS.flatMap { lang -> (1..6).map { "$lang/images/phoneScreenshots/$it.png" } }.filter { !File(base, it).exists() }
        assertEquals("Imagens das telas da loja em cada língua (StoreScreenshotsTest)", emptyList<String>(), missing)
    }

    private companion object {
        // RF-LNG-11 (1.6): russo, ucraniano, árabe e hebraico. O hebraico fica em values-iw e iw-IL (o código antigo, que é
        // o que o Android e o F-Droid procuram); os READMEs usam o código de hoje ("he").
        val README_LANGS = listOf("pt-BR", "es", "fr", "de", "it", "ja", "ko", "zh-CN", "hi", "ru", "uk", "ar", "he")
        val APP_LANGS = setOf(
            "values", "values-en", "values-es", "values-fr", "values-de", "values-it", "values-ja", "values-ko", "values-zh-rCN", "values-hi",
            "values-ru", "values-uk", "values-ar", "values-iw",
        )
        val STORE_LANGS = listOf("pt-BR", "en-US", "es-ES", "fr-FR", "de-DE", "it-IT", "ja-JP", "ko-KR", "zh-CN", "hi-IN", "ru-RU", "uk", "ar", "iw-IL")
    }
}
