// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.domain

import io.github.h3yk0.cryo.data.db.TxType
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/*
 * RF-GUI: o guia "Aprender o Cryo". Regras puras, sem Android e sem sorteio: com os mesmos dados e o mesmo histórico de
 * dicas, a resposta é sempre a mesma. Nada aqui é "IA": são condições simples, uma por lição, que dá para ler e testar.
 */

/** Os seis degraus, do primeiro lançamento ao que só serve para quem já usa bastante. */
enum class GuideLevel { START, ORGANIZE, PLAN, UNDERSTAND, PROTECT, BEYOND }

/** A aba em que a pessoa está (a dica só aparece nas abas; a que fala da aba aberta ganha pontos). */
enum class GuidePlace { HOME, TXS, PLAN, INSIGHTS }

/** RF-GUI-02: as lições. [place] é a aba onde o assunto mora; [spot] = há um ponto fixo na tela para destacar. */
enum class Lesson(val level: GuideLevel, val place: GuidePlace?, val spot: Boolean = false) {
    PHRASE(GuideLevel.START, GuidePlace.HOME, spot = true),
    VOICE(GuideLevel.START, GuidePlace.HOME, spot = true),
    EDIT(GuideLevel.START, GuidePlace.TXS),
    CATEGORIES(GuideLevel.ORGANIZE, GuidePlace.TXS),
    CARD(GuideLevel.ORGANIZE, GuidePlace.HOME),
    BILLS(GuideLevel.ORGANIZE, GuidePlace.PLAN),
    BUDGETS(GuideLevel.PLAN, GuidePlace.PLAN),
    MONTH(GuideLevel.PLAN, GuidePlace.PLAN),
    GOALS(GuideLevel.PLAN, GuidePlace.PLAN),
    DEBTS(GuideLevel.PLAN, GuidePlace.HOME),
    FORECAST(GuideLevel.UNDERSTAND, GuidePlace.INSIGHTS),
    HEALTH(GuideLevel.UNDERSTAND, GuidePlace.INSIGHTS),
    RECURRING(GuideLevel.UNDERSTAND, GuidePlace.INSIGHTS),
    BACKUP(GuideLevel.PROTECT, null),
    LOCK(GuideLevel.PROTECT, null),
    IMPORT(GuideLevel.BEYOND, GuidePlace.TXS),
    SHOPPING(GuideLevel.BEYOND, GuidePlace.HOME),
    INVEST(GuideLevel.BEYOND, GuidePlace.HOME),
}

/** As únicas coisas que o guia conta por conta própria (RF-GUI-01): o que não dá para deduzir dos dados. */
object GuideCount {
    const val PHRASE = "phrase"
    const val VOICE = "voice"
    const val EDIT = "edit"
    const val CATEGORIES = "categories"
    const val MONTH = "month"
    const val DEBTS = "debts"
    const val INSIGHTS = "insights"
    const val HEALTH = "health"
    const val RECURRING = "recurring"
    const val IMPORT = "import"
    const val SHOPPING = "shopping"
}

/**
 * RF-GUI-01: o que o guia olha, tirado dos próprios dados e ajustes. Só **quantidades e sim/não**: nenhum valor, nome ou
 * descrição chega aqui.
 */
data class GuideFacts(
    val txCount: Int = 0,
    /** Despesas sem categoria. */
    val uncategorized: Int = 0,
    /** Dias entre o lançamento mais antigo e hoje. */
    val historyDays: Int = 0,
    val cards: Int = 0,
    val cardPurchases: Int = 0,
    val cardPayments: Int = 0,
    val bills: Int = 0,
    val budgets: Int = 0,
    val goals: Int = 0,
    val debts: Int = 0,
    val investments: Int = 0,
    /** Quantas vezes a pessoa tocou em "Atualizar valor" (rendimentos lançados). */
    val investUpdates: Int = 0,
    /** Dias desde a última vez que um investimento foi mexido (aporte, resgate ou atualização); null = sem investimento. */
    val investStaleDays: Int? = null,
    val imported: Int = 0,
    /** O que se repete e ainda não é conta fixa (do motor; 0 quando o motor ainda não respondeu). */
    val recurringOpen: Int = 0,
    val backupOn: Boolean = false,
    val lockOn: Boolean = false,
    /** Minutos desde a primeira vez que o app abriu neste aparelho. */
    val minutesSinceFirstOpen: Long = Long.MAX_VALUE,
) {
    companion object {
        fun of(s: Snapshot, today: LocalDate, backupOn: Boolean, lockOn: Boolean, recurringOpen: Int, minutesSinceFirstOpen: Long): GuideFacts {
            var uncategorized = 0; var purchases = 0; var payments = 0; var imported = 0
            var first: LocalDate? = null
            var investLast: LocalDate? = null
            for (t in s.txs) {
                if (first == null || t.date.isBefore(first)) first = t.date
                if (t.type == TxType.EXPENSE && t.categoryId == null) uncategorized++
                if (t.type == TxType.EXPENSE && t.cardId != null) purchases++
                if (t.type == TxType.CARD_PAYMENT) payments++
                if (t.importKey != null) imported++
                if ((t.type == TxType.INVEST_IN || t.type == TxType.INVEST_OUT) && (investLast == null || t.date.isAfter(investLast))) investLast = t.date
            }
            for (y in s.yields) if (investLast == null || y.date.isAfter(investLast)) investLast = y.date
            val investments = s.investments.count { !it.archived }
            return GuideFacts(
                txCount = s.txs.size,
                uncategorized = uncategorized,
                historyDays = first?.let { ChronoUnit.DAYS.between(it, today).toInt().coerceAtLeast(0) } ?: 0,
                cards = s.cards.count { !it.archived },
                cardPurchases = purchases,
                cardPayments = payments,
                bills = s.bills.size,
                budgets = s.budgets.size,
                goals = s.goals.size,
                debts = s.debts.size,
                investments = investments,
                investUpdates = s.yields.size,
                investStaleDays = if (investments == 0) null else investLast?.let { ChronoUnit.DAYS.between(it, today).toInt().coerceAtLeast(0) } ?: Int.MAX_VALUE,
                imported = imported,
                recurringOpen = recurringOpen,
                backupOn = backupOn,
                lockOn = lockOn,
                minutesSinceFirstOpen = minutesSinceFirstOpen,
            )
        }
    }
}

/** O que houve com uma lição: quantas vezes apareceu como dica, em que dia foi a última e se a pessoa já a viu ("Me mostra"). */
data class LessonLog(val shown: Int = 0, val day: Long = 0, val seen: Boolean = false)

/** RF-GUI-01: tudo o que o guia guarda. Só contagens e dias (dia = número de dias desde 1970). */
data class GuideLog(
    val enabled: Boolean = true,
    val lessons: Map<Lesson, LessonLog> = emptyMap(),
    /** Os dias das últimas dicas (no máximo as 3 mais recentes). */
    val tipDays: List<Long> = emptyList(),
    /** Quantos "Agora não" seguidos. */
    val laterStreak: Int = 0,
    /** O guia está em pausa até este dia (0 = sem pausa). */
    val pausedUntil: Long = 0,
    /** O dia do último momento (RF-MOM): nesse dia o guia fica quieto. */
    val momentDay: Long = 0,
    val counts: Map<String, Int> = emptyMap(),
)

object GuidePlanner {
    /** RF-GUI-03: os limites do "nunca chato". */
    const val MAX_PER_WEEK = 3
    const val COME_BACK_DAYS = 7L
    const val MAX_SHOWN = 2
    const val STREAK_TO_PAUSE = 3
    const val PAUSE_DAYS = 14L
    const val FIRST_MINUTES = 2L

    val total: Int get() = Lesson.entries.size

    /** A pessoa já faz isso sozinha (pelos dados) ou já viu a lição: aí ela não aparece como dica. */
    fun known(l: Lesson, f: GuideFacts, g: GuideLog): Boolean {
        if (g.lessons[l]?.seen == true) return true
        fun n(key: String) = g.counts[key] ?: 0
        return when (l) {
            Lesson.PHRASE -> n(GuideCount.PHRASE) > 0
            Lesson.VOICE -> n(GuideCount.VOICE) > 0
            Lesson.EDIT -> n(GuideCount.EDIT) > 0
            Lesson.CATEGORIES -> n(GuideCount.CATEGORIES) > 0
            Lesson.CARD -> f.cardPayments > 0
            Lesson.BILLS -> f.bills > 0
            Lesson.BUDGETS -> f.budgets > 0
            Lesson.MONTH -> n(GuideCount.MONTH) > 0
            Lesson.GOALS -> f.goals > 0
            Lesson.DEBTS -> f.debts > 0 || n(GuideCount.DEBTS) > 0
            // A aba Análises abre até sem querer: três visitas é quem foi lá para olhar.
            Lesson.FORECAST -> n(GuideCount.INSIGHTS) >= 3
            Lesson.HEALTH -> n(GuideCount.HEALTH) > 0
            Lesson.RECURRING -> n(GuideCount.RECURRING) > 0
            Lesson.BACKUP -> f.backupOn
            Lesson.LOCK -> f.lockOn
            Lesson.IMPORT -> f.imported > 0 || n(GuideCount.IMPORT) > 0
            Lesson.SHOPPING -> n(GuideCount.SHOPPING) > 0
            Lesson.INVEST -> f.investUpdates > 0
        }
    }

    /** A lição faz sentido **agora** para esta pessoa (não se fala de fatura para quem não tem cartão). */
    fun useful(l: Lesson, f: GuideFacts): Boolean = when (l) {
        Lesson.CARD -> f.cards > 0 && f.cardPurchases > 0
        Lesson.MONTH -> f.budgets > 0
        Lesson.FORECAST, Lesson.HEALTH -> f.historyDays >= 30
        Lesson.RECURRING -> f.historyDays >= 60 || f.recurringOpen > 0
        Lesson.INVEST -> f.investments > 0
        Lesson.BACKUP, Lesson.LOCK, Lesson.IMPORT -> f.txCount >= 5
        Lesson.DEBTS, Lesson.SHOPPING -> f.txCount >= 20
        else -> true
    }

    /** RF-GUI-04: quanto esta lição pede para ser a próxima. Degrau mais baixo vale mais; os dados e a aba aberta somam. */
    fun score(l: Lesson, f: GuideFacts, place: GuidePlace?): Int {
        var p = (GuideLevel.entries.size - l.level.ordinal) * 10
        if (place != null && l.place == place) p += 15
        p += when (l) {
            // Muita coisa anotada e nenhuma cópia: proteger passa na frente de tudo.
            Lesson.BACKUP -> if (f.txCount > 30) 60 else 0
            Lesson.CATEGORIES -> if (f.uncategorized >= 10) 25 else 0
            Lesson.CARD -> if (f.cardPayments == 0) 25 else 0
            Lesson.FORECAST -> if (f.historyDays >= 90) 25 else 0
            Lesson.RECURRING -> if (f.recurringOpen > 0) 25 else 0
            Lesson.INVEST -> if ((f.investStaleDays ?: 0) >= 60) 25 else 0
            else -> 0
        }
        return p
    }

    /** RF-GUI-03: pode sair uma dica hoje? */
    fun allowed(f: GuideFacts, g: GuideLog, today: LocalDate): Boolean {
        val day = today.toEpochDay()
        if (!g.enabled || f.txCount == 0 || f.minutesSinceFirstOpen < FIRST_MINUTES) return false
        if (day < g.pausedUntil || g.momentDay == day) return false
        if (g.tipDays.any { it == day }) return false
        return g.tipDays.count { day - it in 0..6 } < MAX_PER_WEEK
    }

    /** Esta lição ainda pode aparecer como dica (depois de duas vezes, só fica na lista). */
    fun eligible(l: Lesson, f: GuideFacts, g: GuideLog, today: LocalDate): Boolean {
        if (known(l, f, g) || !useful(l, f)) return false
        val log = g.lessons[l] ?: return true
        return log.shown < MAX_SHOWN && (log.shown == 0 || today.toEpochDay() - log.day >= COME_BACK_DAYS)
    }

    /** RF-GUI-04: a dica de agora, ou null. Empate = a que vem antes na lista (sem sorteio). */
    fun next(f: GuideFacts, g: GuideLog, today: LocalDate, place: GuidePlace?): Lesson? {
        if (!allowed(f, g, today)) return null
        var best: Lesson? = null
        var bestScore = Int.MIN_VALUE
        for (l in Lesson.entries) {
            if (!eligible(l, f, g, today)) continue
            val p = score(l, f, place)
            if (p > bestScore) { best = l; bestScore = p }
        }
        return best
    }

    /** Quantas lições a pessoa já conhece (para "7 de 18"). */
    fun progress(f: GuideFacts, g: GuideLog): Int = Lesson.entries.count { known(it, f, g) }

    /* ------------------------------- o que muda no histórico ------------------------------- */

    /** A dica apareceu: conta para o dia e para a semana na hora, mesmo que a pessoa feche o app sem tocar em nada. */
    fun shown(g: GuideLog, l: Lesson, today: LocalDate): GuideLog {
        val day = today.toEpochDay()
        val old = g.lessons[l] ?: LessonLog()
        return g.copy(lessons = g.lessons + (l to old.copy(shown = old.shown + 1, day = day)), tipDays = (g.tipDays + day).takeLast(MAX_PER_WEEK))
    }

    /** "Agora não" (ou Voltar): três seguidos e o guia descansa duas semanas. */
    fun later(g: GuideLog, today: LocalDate): GuideLog {
        val streak = g.laterStreak + 1
        return if (streak >= STREAK_TO_PAUSE) g.copy(laterStreak = 0, pausedUntil = today.toEpochDay() + PAUSE_DAYS) else g.copy(laterStreak = streak)
    }

    /** "Me mostra" (na dica ou na lista): a lição está vista. */
    fun seen(g: GuideLog, l: Lesson): GuideLog =
        g.copy(lessons = g.lessons + (l to (g.lessons[l] ?: LessonLog()).copy(seen = true)), laterStreak = 0)

    fun counted(g: GuideLog, key: String): GuideLog = g.copy(counts = g.counts + (key to ((g.counts[key] ?: 0) + 1).coerceAtMost(999)))
}
