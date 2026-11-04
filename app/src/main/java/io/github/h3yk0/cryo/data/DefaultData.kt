// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.data

import io.github.h3yk0.cryo.R
import io.github.h3yk0.cryo.domain.Res
import io.github.h3yk0.cryo.data.db.Category
import io.github.h3yk0.cryo.data.db.CategoryKind

/** Paleta usada para categorias, contas, cartões, metas e investimentos (boa leitura no claro e no escuro). */
object Palette {
    val colors: List<Int> = listOf(
        0xFFE53935, 0xFFD81B60, 0xFF8E24AA, 0xFF5E35B1, 0xFF3949AB, 0xFF1E88E5, 0xFF039BE5, 0xFF00ACC1,
        0xFF00897B, 0xFF43A047, 0xFF7CB342, 0xFFC0CA33, 0xFFFDD835, 0xFFFFB300, 0xFFFB8C00, 0xFFF4511E,
        0xFF6D4C41, 0xFF757575, 0xFF546E7A, 0xFF26A69A,
    ).map { it.toInt() }

    fun pick(i: Int): Int = colors[((i % colors.size) + colors.size) % colors.size]
}

object DefaultData {
    private fun c(name: String, kind: CategoryKind, icon: String, color: Long, keywords: String, order: Int) =
        Category(name = name, kind = kind, icon = icon, color = color.toInt(), keywords = keywords, sortOrder = order)

    const val DEBT_ICON = "debt"

    /** Categoria usada nos pagamentos de dívidas (também criada na atualização para a versão 1.1). */
    fun debtCategory() = c(
        Res.s(R.string.cat_debt_name), CategoryKind.EXPENSE, DEBT_ICON, 0xFFB23A48,
        Res.s(R.string.cat_debt_keywords),
        16,
    )

    fun categories(): List<Category> {
        val e = CategoryKind.EXPENSE
        val i = CategoryKind.INCOME
        return listOf(
            c(Res.s(R.string.cat_restaurant_name), e, "restaurant", 0xFFF4511E, Res.s(R.string.cat_restaurant_keywords), 0),
            c(Res.s(R.string.cat_cart_name), e, "cart", 0xFF43A047, Res.s(R.string.cat_cart_keywords), 1),
            c(Res.s(R.string.cat_car_name), e, "car", 0xFF1E88E5, Res.s(R.string.cat_car_keywords), 2),
            c(Res.s(R.string.cat_home_name), e, "home", 0xFF6D4C41, Res.s(R.string.cat_home_keywords), 3),
            c(Res.s(R.string.cat_bolt_name), e, "bolt", 0xFFFFB300, Res.s(R.string.cat_bolt_keywords), 4),
            c(Res.s(R.string.cat_health_name), e, "health", 0xFFE53935, Res.s(R.string.cat_health_keywords), 5),
            c(Res.s(R.string.cat_school_name), e, "school", 0xFF3949AB, Res.s(R.string.cat_school_keywords), 6),
            c(Res.s(R.string.cat_movie_name), e, "movie", 0xFF8E24AA, Res.s(R.string.cat_movie_keywords), 7),
            c(Res.s(R.string.cat_shopping_name), e, "shopping", 0xFFD81B60, Res.s(R.string.cat_shopping_keywords), 8),
            c(Res.s(R.string.cat_subscriptions_name), e, "subscriptions", 0xFF5E35B1, Res.s(R.string.cat_subscriptions_keywords), 9),
            c(Res.s(R.string.cat_spa_name), e, "spa", 0xFFEC407A, Res.s(R.string.cat_spa_keywords), 10),
            c(Res.s(R.string.cat_pets_name), e, "pets", 0xFF7CB342, Res.s(R.string.cat_pets_keywords), 11),
            c(Res.s(R.string.cat_volunteer_name), e, "volunteer", 0xFF00897B, Res.s(R.string.cat_volunteer_keywords), 12),
            c(Res.s(R.string.cat_gift_name), e, "gift", 0xFFFB8C00, Res.s(R.string.cat_gift_keywords), 13),
            c(Res.s(R.string.cat_flight_name), e, "flight", 0xFF039BE5, Res.s(R.string.cat_flight_keywords), 14),
            c(Res.s(R.string.cat_receipt_name), e, "receipt", 0xFF546E7A, Res.s(R.string.cat_receipt_keywords), 15),
            debtCategory(),
            c(Res.s(R.string.cat_more_expense_name), e, "more", 0xFF757575, Res.s(R.string.cat_more_expense_keywords), 17),

            c(Res.s(R.string.cat_work_name), i, "work", 0xFF2E7D32, Res.s(R.string.cat_work_keywords), 20),
            c(Res.s(R.string.cat_laptop_name), i, "laptop", 0xFF00ACC1, Res.s(R.string.cat_laptop_keywords), 21),
            c(Res.s(R.string.cat_sell_name), i, "sell", 0xFFC0CA33, Res.s(R.string.cat_sell_keywords), 22),
            c(Res.s(R.string.cat_trending_name), i, "trending", 0xFF3949AB, Res.s(R.string.cat_trending_keywords), 23),
            c(Res.s(R.string.cat_undo_name), i, "undo", 0xFF8E24AA, Res.s(R.string.cat_undo_keywords), 24),
            c(Res.s(R.string.cat_redeem_name), i, "redeem", 0xFFF4511E, Res.s(R.string.cat_redeem_keywords), 25),
            c(Res.s(R.string.cat_more_income_name), i, "more", 0xFF757575, Res.s(R.string.cat_more_income_keywords), 26),
        )
    }

    /** Ícones disponíveis para categorias (chave -> descrição para leitores de tela). */
    val categoryIcons: List<Pair<String, String>> get() = listOf(
        "restaurant" to Res.s(R.string.icon_restaurant), "cart" to Res.s(R.string.icon_cart), "car" to Res.s(R.string.icon_car), "bus" to Res.s(R.string.icon_bus), "home" to Res.s(R.string.icon_home),
        "bolt" to Res.s(R.string.icon_bolt), "water" to Res.s(R.string.icon_water), "wifi" to Res.s(R.string.icon_wifi), "phone" to Res.s(R.string.icon_phone), "health" to Res.s(R.string.icon_health),
        "fitness" to Res.s(R.string.icon_fitness), "school" to Res.s(R.string.icon_school), "movie" to Res.s(R.string.icon_movie), "game" to Res.s(R.string.icon_game), "shopping" to Res.s(R.string.icon_shopping),
        "subscriptions" to Res.s(R.string.icon_subscriptions), "spa" to Res.s(R.string.icon_spa), "pets" to Res.s(R.string.icon_pets), "volunteer" to Res.s(R.string.icon_volunteer),
        "gift" to Res.s(R.string.icon_gift), "flight" to Res.s(R.string.icon_flight), "receipt" to Res.s(R.string.icon_receipt), "coffee" to Res.s(R.string.icon_coffee), "gas" to Res.s(R.string.icon_gas),
        "baby" to Res.s(R.string.icon_baby), "build" to Res.s(R.string.icon_build), "work" to Res.s(R.string.icon_work), "laptop" to Res.s(R.string.icon_laptop), "sell" to Res.s(R.string.icon_sell),
        "trending" to Res.s(R.string.icon_trending), "undo" to Res.s(R.string.icon_undo), "redeem" to Res.s(R.string.icon_redeem), "savings" to Res.s(R.string.icon_savings),
        DEBT_ICON to Res.s(R.string.icon_debt),
        "money" to Res.s(R.string.icon_money), "star" to Res.s(R.string.icon_star), "more" to Res.s(R.string.icon_more),
    )

    val goalEmojis = listOf("🎯", "🏠", "✈️", "🚗", "🎓", "💍", "🛡️", "📱", "💻", "🎮", "🏖️", "👶", "🐶", "🎸", "🏍️", "💰", "🎁", "⛪", "🏋️", "📚")
}
