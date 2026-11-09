// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.notify

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalDate

/** RF-AVI-02: o jeito de falar dos avisos (o Hanry escolheu Amigo como padrão). */
enum class AlertTone { FRIEND, DISCREET }

/** RF-AVI-08: os grupos de avisos que a pessoa liga e desliga em Ajustes › Avisos. */
enum class AlertGroup { BILLS, BUDGET, CARDS, RECURRING, WATCH, GOALS, SUMMARIES, DAILY }

/**
 * O que os avisos guardam no aparelho (RF-AVI-04/05): as escolhas da pessoa e só **quando** cada aviso foi mostrado, nunca o
 * texto nem valores. Fica fora do backup (é do aparelho, como a escolha do ouvido da voz) e vale para os dados de verdade.
 */
class AlertState(private val prefs: SharedPreferences) {
    constructor(ctx: Context) : this(ctx.getSharedPreferences("cryo_alerts", Context.MODE_PRIVATE))

    private val changes = MutableStateFlow(0)

    /** Muda a cada ajuste (as telas observam para se redesenhar). */
    val version: StateFlow<Int> = changes

    private fun edit(block: SharedPreferences.Editor.() -> Unit) {
        prefs.edit().apply(block).apply()
        changes.value++
    }

    var tone: AlertTone
        get() = runCatching { AlertTone.valueOf(prefs.getString("tone", null) ?: "") }.getOrDefault(AlertTone.FRIEND)
        set(v) = edit { putString("tone", v.name) }

    /** RF-SOM-02: sons do Cryo (liga/desliga em Ajustes; o modo silencioso do celular sempre vale). */
    var sounds: Boolean
        get() = prefs.getBoolean("sounds", true)
        set(v) = edit { putBoolean("sounds", v) }

    fun enabled(g: AlertGroup): Boolean = prefs.getBoolean("on_${g.name}", true)
    fun setEnabled(g: AlertGroup, on: Boolean) = edit { putBoolean("on_${g.name}", on) }

    /** RF-AVI-11: o horário do lembrete do dia, em minutos desde a meia-noite; null = sem lembrete. */
    var reminderMinutes: Int?
        get() = prefs.getInt("reminder_min", DEFAULT_REMINDER).takeIf { it >= 0 && enabled(AlertGroup.DAILY) }
        set(v) = edit {
            if (v == null) putBoolean("on_${AlertGroup.DAILY.name}", false)
            else { putInt("reminder_min", v); putBoolean("on_${AlertGroup.DAILY.name}", true) }
        }

    /** Já perguntou o horário do lembrete (RF-AVI-11: pergunta uma vez só). */
    var reminderAsked: Boolean
        get() = prefs.getBoolean("reminder_asked", false)
        set(v) = edit { putBoolean("reminder_asked", v) }

    /* ------------------------------- o que já foi mostrado ------------------------------- */

    fun shown(key: String): Boolean = prefs.contains("k_$key")

    fun markShown(key: String, kind: AlertKind, day: LocalDate) = edit {
        putLong("k_$key", day.toEpochDay())
        putLong("t_${kind.name}", day.toEpochDay())
        if (!kind.urgent) putLong("common_day", day.toEpochDay())
    }

    fun lastOfKind(kind: AlertKind): LocalDate? = prefs.getLong("t_${kind.name}", Long.MIN_VALUE).takeIf { it != Long.MIN_VALUE }?.let(LocalDate::ofEpochDay)

    /** Já saiu um aviso comum hoje (RF-AVI-04: no máximo 1 por dia). */
    fun commonShownOn(day: LocalDate): Boolean = prefs.getLong("common_day", Long.MIN_VALUE) == day.toEpochDay()

    /** Quantas vezes seguidas a pessoa dispensou este tipo sem abrir (3 = passa a vir com metade da frequência). */
    fun ignored(kind: AlertKind): Int = prefs.getInt("ign_${kind.name}", 0)
    fun markIgnored(kind: AlertKind) = edit { putInt("ign_${kind.name}", ignored(kind) + 1) }
    fun markOpened(kind: AlertKind) = edit { putInt("ign_${kind.name}", 0) }

    /** Qual dos textos do tipo saiu da última vez (para não repetir). */
    fun nextVariant(kind: AlertKind, count: Int): Int {
        if (count <= 1) return 0
        val next = (prefs.getInt("v_${kind.name}", -1) + 1) % count
        edit { putInt("v_${kind.name}", next) }
        return next
    }

    /** A última nota de saúde avisada (para "subiu"). */
    var lastHealth: Int
        get() = prefs.getInt("health", -1)
        set(v) = edit { putInt("health", v) }

    /** Momentos do app que saem uma vez só (RF-MOM-04: o fechamento de cada mês). */
    fun momentShown(key: String): Boolean = prefs.contains("m_$key")
    fun markMoment(key: String) = edit { putBoolean("m_$key", true) }

    /** "Lembrar amanhã": a conta só volta a avisar depois deste dia. */
    fun snooze(key: String, until: LocalDate) = edit { putLong("z_$key", until.toEpochDay()) }
    fun snoozed(key: String, today: LocalDate): Boolean = prefs.getLong("z_$key", Long.MIN_VALUE) >= today.toEpochDay()

    companion object {
        const val DEFAULT_REMINDER = 20 * 60 + 30
    }
}
