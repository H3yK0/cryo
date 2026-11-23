// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.domain

/**
 * RF-ORG-01..05 (1.7, LA8): a arrumação de uma tela: a ordem dos blocos e os que a pessoa escondeu. Regra pura (sem
 * Android). Guardada como texto curto ("saude,-ano,fluxo": o "-" marca o escondido), por tela.
 */
data class ScreenLayout(val order: List<String> = emptyList(), val hidden: Set<String> = emptySet()) {
    val isDefault: Boolean get() = order.isEmpty() && hidden.isEmpty()

    /**
     * A ordem da pessoa aplicada aos blocos que existem agora ([defaults], na ordem padrão). Os que ela já arrumou vêm na
     * ordem dela; um bloco que ela nunca viu (de uma versão nova, ou que só existe agora, como um cartão novo) entra no lugar
     * padrão: logo depois do bloco que vem antes dele no padrão (RF-ORG-04).
     */
    fun arrange(defaults: List<String>): List<String> {
        val out = order.filter { it in defaults }.distinct().toMutableList()
        defaults.forEachIndexed { i, key ->
            if (key in out) return@forEachIndexed
            val before = defaults.subList(0, i).lastOrNull { it in out }
            out.add(if (before == null) 0 else out.indexOf(before) + 1, key)
        }
        return out
    }

    /** O que aparece na tela: a ordem da pessoa sem os escondidos. Nunca vazio (RF-ORG-05): sem nada, o primeiro bloco. */
    fun visible(defaults: List<String>): List<String> =
        arrange(defaults).filter { it !in hidden }.ifEmpty { defaults.take(1) }

    /** Leva o bloco da posição [from] para a [to] (posições na ordem arrumada de agora). */
    fun moved(defaults: List<String>, from: Int, to: Int): ScreenLayout {
        val list = arrange(defaults).toMutableList()
        if (from !in list.indices || to !in list.indices || from == to) return this
        list.add(to, list.removeAt(from))
        return copy(order = list)
    }

    /** Esconde ou mostra [key]. Esconder o último bloco à vista não vale (RF-ORG-05): devolve null. */
    fun toggled(defaults: List<String>, key: String): ScreenLayout? {
        if (key in hidden) return copy(hidden = hidden - key)
        val shown = arrange(defaults).filter { it !in hidden }
        if (shown.size <= 1 && key in shown) return null
        return copy(hidden = hidden + key)
    }

    fun encode(): String = order.joinToString(",") { if (it in hidden) "-$it" else it } +
        hidden.filter { it !in order }.joinToString("") { ",-$it" }

    companion object {
        /** Só letras minúsculas, números, ":" e "_" (as chaves são do app, nunca texto da pessoa). */
        private val KEY = Regex("^[a-z0-9:_]{1,40}$")

        /** O texto mais longo que uma arrumação pode ter (protege a leitura de um backup mexido). */
        const val MAX_TEXT = 2_000

        fun decode(text: String?): ScreenLayout {
            if (text.isNullOrBlank() || text.length > MAX_TEXT) return ScreenLayout()
            val order = ArrayList<String>()
            val hidden = HashSet<String>()
            text.split(',').forEach { raw ->
                val isHidden = raw.startsWith("-")
                val key = raw.removePrefix("-")
                if (!KEY.matches(key) || key in order) return@forEach
                order += key
                if (isHidden) hidden += key
            }
            return ScreenLayout(order, hidden)
        }
    }
}

/** RF-ORG: as telas que têm arrumação (o nome de cada uma no que fica guardado e no backup). */
object Layouts {
    const val HOME = "home"
    const val HOME_TILES = "home_tiles"
    const val INSIGHTS = "insights"
    const val BUDGETS = "budgets"
    const val BILLS = "bills"
    const val GOALS = "goals"
    const val WALLET = "wallet"
    val ALL = setOf(HOME, HOME_TILES, INSIGHTS, BUDGETS, BILLS, GOALS, WALLET)

    /** Só as telas conhecidas e textos que viram uma arrumação de verdade (para o backup e a leitura dos ajustes). */
    fun clean(raw: Map<String, String>): Map<String, String> =
        raw.filterKeys { it in ALL }.mapValues { (_, v) -> ScreenLayout.decode(v).encode() }.filterValues { it.isNotEmpty() }
}
