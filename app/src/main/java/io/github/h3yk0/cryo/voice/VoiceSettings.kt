// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.voice

import android.content.Context
import io.github.h3yk0.cryo.R
import io.github.h3yk0.cryo.domain.ParseContext
import io.github.h3yk0.cryo.domain.Res

/**
 * Qual ouvido usar (Ajustes › Reconhecimento de voz). É uma preferência do aparelho, não um dado da pessoa: fica fora do
 * backup e vale igual com o modo de teste.
 */
class VoiceSettings(ctx: Context) {
    private val prefs = ctx.getSharedPreferences("cryo_voice", Context.MODE_PRIVATE)

    /** O Whisper quando o celular aguenta (padrão); senão, sempre o reconhecedor do celular. */
    var engine: VoiceEngineKind
        get() = if (!WhisperEngine.supported) VoiceEngineKind.PHONE
        else prefs.getString(KEY, null)?.let { runCatching { VoiceEngineKind.valueOf(it) }.getOrNull() } ?: VoiceEngineKind.WHISPER
        set(v) { prefs.edit().putString(KEY, v.name).apply() }

    private companion object { const val KEY = "engine" }
}

/**
 * RF-VOZ-04: a "dica" que o Whisper recebe antes de ouvir. Ele imita o jeito de escrever da dica (valores com algarismos
 * e vírgula: "32,50", não "trinta e dois e cinquenta") e passa a reconhecer os nomes da pessoa ("Cartão Roxo", "Pet").
 * Cabe em ~100 pedaços de palavra (o Whisper aceita até 224).
 */
object VoicePrompt {
    fun build(ctx: ParseContext): String {
        val names = (ctx.cards.filter { !it.archived }.map { it.name } + ctx.accounts.filter { !it.archived }.map { it.name } +
            ctx.goals.filter { !it.archived }.map { it.name } + ctx.categories.filter { !it.archived }.map { it.name })
            .map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        val sb = StringBuilder(Res.s(R.string.voice_prompt_example))
        var first = true
        for (n in names) {
            if (sb.length + n.length + 2 > MAX_CHARS) break
            sb.append(if (first) " " else ", ").append(n)
            first = false
        }
        if (!first) sb.append('.')
        return sb.toString()
    }

    private const val MAX_CHARS = 420
}
