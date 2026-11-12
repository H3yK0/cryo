// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.data

import android.content.Context
import android.content.SharedPreferences
import io.github.h3yk0.cryo.domain.GuideLog
import io.github.h3yk0.cryo.domain.GuidePlanner
import io.github.h3yk0.cryo.domain.Lesson
import io.github.h3yk0.cryo.domain.LessonLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalDate

/**
 * RF-GUI-01: o caderninho do guia, só deste aparelho (fora do backup e da troca de celular, como o dos avisos). Guarda
 * **só contagens, dias e sim/não**: nunca valores, nomes ou descrições. Um arquivo à parte, para dar para apagar ou conferir.
 */
class GuideStore(private val prefs: SharedPreferences) {
    constructor(ctx: Context) : this(ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE))

    private val changes = MutableStateFlow(0)

    /** Muda a cada escrita (as telas observam para se redesenhar). */
    val version: StateFlow<Int> = changes

    /** A chave "Dicas do Cryo" (RF-GUI-03): ligada no começo. */
    var enabled: Boolean
        get() = prefs.getBoolean("enabled", true)
        set(v) { prefs.edit().putBoolean("enabled", v).apply(); changes.value++ }

    /** Minutos desde a primeira vez que o app abriu (a primeira leitura marca a hora). */
    fun minutesSinceFirstOpen(now: Long = System.currentTimeMillis()): Long {
        val first = prefs.getLong("first", 0L)
        if (first == 0L) { prefs.edit().putLong("first", now).apply(); return 0 }
        return ((now - first) / 60_000L).coerceAtLeast(0)
    }

    fun log(): GuideLog {
        val lessons = HashMap<Lesson, LessonLog>()
        for (l in Lesson.entries) {
            val shown = prefs.getInt("n_${l.name}", 0)
            val seen = prefs.getBoolean("s_${l.name}", false)
            if (shown > 0 || seen) lessons[l] = LessonLog(shown, prefs.getLong("d_${l.name}", 0L), seen)
        }
        val counts = HashMap<String, Int>()
        for ((k, v) in prefs.all) if (k.startsWith("c_") && v is Int) counts[k.substring(2)] = v
        return GuideLog(
            enabled = enabled,
            lessons = lessons,
            tipDays = (prefs.getString("tips", "") ?: "").split(',').mapNotNull { it.toLongOrNull() },
            laterStreak = prefs.getInt("streak", 0),
            pausedUntil = prefs.getLong("paused", 0L),
            momentDay = prefs.getLong("moment", 0L),
            counts = counts,
        )
    }

    private fun save(g: GuideLog) {
        val e = prefs.edit()
        for ((l, x) in g.lessons) {
            e.putInt("n_${l.name}", x.shown).putLong("d_${l.name}", x.day).putBoolean("s_${l.name}", x.seen)
        }
        for ((k, v) in g.counts) e.putInt("c_$k", v)
        e.putString("tips", g.tipDays.joinToString(","))
        e.putInt("streak", g.laterStreak).putLong("paused", g.pausedUntil).putLong("moment", g.momentDay)
        e.apply()
        changes.value++
    }

    fun markShown(l: Lesson, today: LocalDate) = save(GuidePlanner.shown(log(), l, today))
    fun markLater(today: LocalDate) = save(GuidePlanner.later(log(), today))
    fun markSeen(l: Lesson) = save(GuidePlanner.seen(log(), l))
    fun markMoment(today: LocalDate) { if (prefs.getLong("moment", 0L) != today.toEpochDay()) save(log().copy(momentDay = today.toEpochDay())) }

    /** Soma um ao contador [key] (uma das chaves de [io.github.h3yk0.cryo.domain.GuideCount]). */
    fun count(key: String) = save(GuidePlanner.counted(log(), key))

    companion object {
        const val FILE = "cryo_guide"
    }
}
