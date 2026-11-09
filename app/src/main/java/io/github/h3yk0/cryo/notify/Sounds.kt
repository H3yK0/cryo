// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.notify

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import io.github.h3yk0.cryo.R

/** RF-SOM-01: o kit de sons do Cryo (cristal de gelo), sintetizado do zero por `tools/sons/make_sounds.py`. */
enum class Sound(val res: Int) {
    SAVE(R.raw.som_salvar),
    INCOME(R.raw.som_entrada),
    ATTENTION(R.raw.som_atencao),
    LIMIT(R.raw.som_limite),
    ACHIEVEMENT(R.raw.som_conquista),
    STREAK(R.raw.som_sequencia),
    VOICE_START(R.raw.som_voz_inicio),
    VOICE_END(R.raw.som_voz_fim),
}

/**
 * Toca os sons do app (RF-SOM-02): só com o celular no modo normal (no silencioso e no vibrar, nada) e com "Sons do Cryo"
 * ligado; volume baixo; nunca em repetição. Os sons são carregados uma vez, no primeiro uso.
 */
object Sounds {
    /** Nos testes não há áudio: tocar vira só anotar o que tocaria. */
    @Volatile var testLog: MutableList<Sound>? = null

    private var pool: SoundPool? = null
    private val ids = HashMap<Sound, Int>()
    private val ready = HashSet<Int>()

    @Synchronized
    private fun load(ctx: Context): SoundPool {
        pool?.let { return it }
        val attrs = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
        val p = SoundPool.Builder().setMaxStreams(2).setAudioAttributes(attrs).build()
        p.setOnLoadCompleteListener { _, id, status -> if (status == 0) synchronized(this) { ready += id } }
        Sound.entries.forEach { ids[it] = p.load(ctx.applicationContext, it.res, 1) }
        pool = p
        return p
    }

    fun play(ctx: Context, s: Sound) {
        runCatching {
            if (!AlertState(ctx).sounds) return
            val am = ctx.getSystemService(AudioManager::class.java)
            if (am != null && am.ringerMode != AudioManager.RINGER_MODE_NORMAL) return
            testLog?.let { it += s; return }
            val p = load(ctx)
            val id = ids[s] ?: return
            if (id in ready) p.play(id, 0.6f, 0.6f, 1, 0, 1f)
        }
    }

    /** Carrega os sons antes (a primeira vez que um som é pedido ele ainda não estaria pronto). */
    fun warmUp(ctx: Context) {
        runCatching { if (AlertState(ctx).sounds && testLog == null) load(ctx) }
    }
}
