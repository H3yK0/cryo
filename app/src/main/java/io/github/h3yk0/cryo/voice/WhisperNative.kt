// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.voice

import android.content.res.AssetManager
import android.os.Build
import java.io.File

/**
 * RF-VOZ-01: a ponte com o whisper.cpp (`src/main/cpp/cryo_whisper.cpp`). Só existe aqui para o resto do app não saber
 * de JNI. A biblioteca só é carregada no primeiro uso do microfone e só em processadores que aguentam (veja [supported]).
 */
internal object WhisperNative {
    /**
     * O Whisper foi compilado para ARM de 64 bits com fp16 e dotprod (2017 para cá). Num processador sem elas, carregar
     * a biblioteca derrubaria o app (instrução desconhecida): por isso a conferência vem antes, lendo o /proc/cpuinfo.
     */
    val supported: Boolean by lazy {
        runCatching {
            if (Build.SUPPORTED_ABIS.firstOrNull() != "arm64-v8a") return@runCatching false
            val features = File("/proc/cpuinfo").useLines { lines ->
                lines.firstOrNull { it.startsWith("Features") }?.substringAfter(':')?.trim()?.split(' ')?.toSet()
            } ?: return@runCatching false
            "asimdhp" in features && "asimddp" in features
        }.getOrDefault(false)
    }

    /** Carrega a biblioteca (uma vez). False se não deu: aí fica o reconhecedor do celular. */
    val loaded: Boolean by lazy {
        supported && runCatching { System.loadLibrary("cryo_whisper"); nativeAvailable() }.getOrDefault(false)
    }

    external fun nativeAvailable(): Boolean
    external fun nativeOpen(assets: AssetManager, path: String, flash: Boolean): Long
    external fun nativeTranscribe(
        handle: Long, pcm: FloatArray, samples: Int, language: String, prompt: String?,
        threads: Int, greedy: Boolean, audioCtx: Int, maxTokens: Int,
    ): ByteArray?
    external fun nativeNoSpeech(handle: Long): Float
    external fun nativeAbort(handle: Long)
    external fun nativeClose(handle: Long)
    external fun nativeInfo(): String
}
