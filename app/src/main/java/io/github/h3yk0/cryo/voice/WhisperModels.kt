// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.voice

import androidx.annotation.StringRes
import io.github.h3yk0.cryo.R

/**
 * RF-VOZ-15 (1.6): um modelo Whisper que o Cryo aceita. Até a 1.5 o modelo "base" vinha dentro do APK; o F-Droid só
 * publica o que ele mesmo compila do código-fonte, e um arquivo de 57 MB de pesos não sai de código nenhum. Desde a 1.6 a
 * pessoa baixa o modelo uma vez pelo navegador (o Cryo continua sem internet) e escolhe o arquivo: o Cryo só aceita os
 * arquivos oficiais, conferidos pelo tamanho e pelo SHA-256 (lista e licença em docs/VOZ-MODELOS.md).
 */
data class WhisperModel(
    val id: String,
    /** O nome do arquivo na origem oficial; é também o nome com que ele fica guardado no celular. */
    val file: String,
    /** Tamanho exato em bytes: a primeira conferência (e a que acha um download cortado no meio). */
    val size: Long,
    /** SHA-256 do arquivo oficial, em hexadecimal minúsculo. */
    val sha256: String,
    @StringRes val nameRes: Int,
    @StringRes val descRes: Int,
    /** O modelo sugerido: o mesmo "base" de 5 bits que ia dentro do Cryo até a 1.5. */
    val recommended: Boolean = false,
) {
    /** A origem oficial (Hugging Face, ggerganov/whisper.cpp). Quem abre é o navegador. */
    val officialUrl: String get() = OFFICIAL + file

    /**
     * A cópia na Release `voice-model-1` do GitHub do Cryo (só do recomendado): o mesmo arquivo, com o mesmo SHA-256. Fica
     * numa Release à parte, presa ao código da 1.4, para o robô do F-Droid nunca confundir essa etiqueta com uma versão.
     */
    val cryoUrl: String? get() = if (recommended) CRYO_RELEASE + file else null

    companion object {
        const val OFFICIAL = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/"
        const val CRYO_RELEASE = "https://github.com/H3yK0/cryo/releases/download/voice-model-1/"
    }
}

/** Os modelos aceitos, do mais leve ao mais pesado. Tamanhos e SHA-256 conferidos nos arquivos oficiais (RF-VOZ-15). */
object WhisperModels {
    val ALL: List<WhisperModel> = listOf(
        WhisperModel(
            "tiny-q5_1", "ggml-tiny-q5_1.bin", 32_152_673L,
            "818710568da3ca15689e31a743197b520007872ff9576237bda97bd1b469c3d7",
            R.string.voice_model_tiny, R.string.voice_model_tiny_desc,
        ),
        WhisperModel(
            "tiny-q8_0", "ggml-tiny-q8_0.bin", 43_537_433L,
            "c2085835d3f50733e2ff6e4b41ae8a2b8d8110461e18821b09a15c40c42d1cca",
            R.string.voice_model_tiny8, R.string.voice_model_tiny8_desc,
        ),
        WhisperModel(
            "base-q5_1", "ggml-base-q5_1.bin", 59_707_625L,
            "422f1ae452ade6f30a004d7e5c6a43195e4433bc370bf23fac9cc591f01a8898",
            R.string.voice_model_base, R.string.voice_model_base_desc, recommended = true,
        ),
        WhisperModel(
            "base-q8_0", "ggml-base-q8_0.bin", 81_768_585L,
            "c577b9a86e7e048a0b7eada054f4dd79a56bbfa911fbdacf900ac5b567cbb7d9",
            R.string.voice_model_base8, R.string.voice_model_base8_desc,
        ),
        WhisperModel(
            "small-q5_1", "ggml-small-q5_1.bin", 190_085_487L,
            "ae85e4a935d7a567bd102fe55afc16bb595bdb618e11b2fc7591bc08120411bb",
            R.string.voice_model_small, R.string.voice_model_small_desc,
        ),
        WhisperModel(
            "small-q8_0", "ggml-small-q8_0.bin", 264_464_607L,
            "49c8fb02b65e6049d5fa6c04f81f53b867b5ec9540406812c643f177317f779f",
            R.string.voice_model_small8, R.string.voice_model_small8_desc,
        ),
    )

    val recommended: WhisperModel get() = ALL.first { it.recommended }
}
