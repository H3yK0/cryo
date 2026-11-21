// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * ERR-057 (1.6): o F-Droid compila do código-fonte e o scanner dele recusa binários prontos no repositório. Até a 1.5 o
 * modelo do Whisper (57 MB de pesos) ficava em `app/src/main/assets/`; desde a 1.6 a pessoa baixa e escolhe o arquivo
 * (RF-VOZ-15). Este teste garante que nenhum binário desse tipo volte para o código.
 */
class SourceTreeTest {
    private val root = File("..").canonicalFile

    /** Pastas que não vão para o git (compilação, imagens dos testes, a chave) ou que não são do projeto. */
    private val skipDirs = setOf(".git", ".gradle", "build", ".idea", "keystore", "screenshots", ".cxx", ".kotlin")

    /** Extensões de binários prontos: bibliotecas, pacotes, código compilado, arquivos compactados e pesos de modelos de IA. */
    private val binaryExt = setOf(
        "bin", "so", "a", "o", "dylib", "dll", "exe", "jar", "aar", "apk", "aab", "dex", "class", "zip", "gz", "tgz", "xz",
        "7z", "rar", "tar", "gguf", "ggml", "onnx", "tflite", "pt", "pth", "ckpt", "safetensors", "npy", "npz", "pb",
    )

    /** O único binário que o F-Droid aceita no código: o "gradle wrapper" (ele confere contra os oficiais). */
    private val allowed = setOf("gradle/wrapper/gradle-wrapper.jar")

    private fun files(): List<File> = root.walkTopDown()
        .onEnter { it == root || it.name !in skipDirs }
        .filter { it.isFile }
        .toList()

    private fun rel(f: File) = f.relativeTo(root).invariantSeparatorsPath

    @Test fun nenhumBinarioProntoNoCodigo() {
        val bad = files().filter { it.extension.lowercase() in binaryExt && rel(it) !in allowed }.map { rel(it) }
        assertEquals("binários no código (o F-Droid recusa): $bad", emptyList<String>(), bad)
    }

    @Test fun nenhumArquivoGigante() {
        // O maior arquivo de verdade do projeto (o whisper.cpp tem fontes grandes) fica bem abaixo disso; 57 MB não passa.
        val big = files().filter { it.length() > 8L * 1024 * 1024 }.map { "${rel(it)} (${it.length() / (1024 * 1024)} MB)" }
        assertEquals("arquivos grandes demais no código: $big", emptyList<String>(), big)
    }

    @Test fun modeloNaoVoltaParaDentroDoApp() {
        assertFalse("o modelo não vem mais no APK (RF-VOZ-15)", File(root, "app/src/main/assets/whisper").exists())
        // A ponte nativa abre o arquivo que a pessoa instalou (e não precisa mais da biblioteca "android" para ler assets).
        val bridge = File(root, "app/src/main/cpp/cryo_whisper.cpp").readText()
        assertTrue(bridge.contains("nativeOpenFile"))
        assertFalse(bridge.contains("AAssetManager"))
        val cmake = File(root, "app/src/main/cpp/CMakeLists.txt").readText()
        assertFalse(cmake.contains(Regex("target_link_libraries\\(cryo_whisper PRIVATE[^)]*\\bandroid\\b")))
    }
}
