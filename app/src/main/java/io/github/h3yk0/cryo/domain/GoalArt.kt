// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.domain

import io.github.h3yk0.cryo.data.db.Goal

/**
 * RF-ILU-03: as ilustrações das metas (o desenho fica em `ui/components/Art.kt`). Até a 1.6 a meta guardava um emoji; ele
 * continua no banco só para backups lidos por versões antigas, e aqui vira a ilustração equivalente. Os emojis antigos estão
 * escritos como códigos (\\u...) de propósito: o app não mostra nem escreve emoji (RF-ILU-01).
 */
enum class GoalArt(val key: String, val legacy: String) {
    TARGET("target", "\uD83C\uDFAF"), // alvo
    HOUSE("house", "\uD83C\uDFE0"), // casa
    PLANE("plane", "\u2708\uFE0F"), // avião
    CAR("car", "\uD83D\uDE97"), // carro
    GRADUATION("graduation", "\uD83C\uDF93"), // formatura
    RING("ring", "\uD83D\uDC8D"), // aliança
    SHIELD("shield", "\uD83D\uDEE1\uFE0F"), // escudo (reserva)
    PHONE("phone", "\uD83D\uDCF1"), // celular
    LAPTOP("laptop", "\uD83D\uDCBB"), // notebook
    GAME("game", "\uD83C\uDFAE"), // videogame
    BEACH("beach", "\uD83C\uDFD6\uFE0F"), // praia
    BABY("baby", "\uD83D\uDC76"), // bebê
    PET("pet", "\uD83D\uDC36"), // pet
    GUITAR("guitar", "\uD83C\uDFB8"), // violão
    MOTO("moto", "\uD83C\uDFCD\uFE0F"), // moto
    SAVINGS("savings", "\uD83D\uDCB0"), // cofre
    GIFT("gift", "\uD83C\uDF81"), // presente
    CHURCH("church", "\u26EA"), // igreja
    GYM("gym", "\uD83C\uDFCB\uFE0F"), // academia
    BOOKS("books", "\uD83D\uDCDA"); // livros

    companion object {
        private fun plain(s: String) = s.replace("\uFE0F", "").trim()

        fun of(key: String?): GoalArt = entries.firstOrNull { it.key == key } ?: TARGET

        /** A ilustração de uma meta antiga, pelo emoji que ela tinha (desconhecido = o alvo). */
        fun fromLegacy(emoji: String?): GoalArt = emoji?.let { e -> entries.firstOrNull { plain(it.legacy) == plain(e) } } ?: TARGET

        fun forGoal(g: Goal): GoalArt = g.art?.let { k -> entries.firstOrNull { it.key == k } } ?: fromLegacy(g.emoji)
    }
}
