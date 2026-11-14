// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.data

import androidx.annotation.StringRes
import io.github.h3yk0.cryo.R
import io.github.h3yk0.cryo.domain.Res
import java.time.LocalDate
import java.time.YearMonth

/**
 * RF-CFX-12: modelos de conta fixa, para quem está começando não ter de pensar em tudo sozinho. São poucos e básicos; o
 * nome de cada um vem dos textos, então cada língua traz o seu (no Brasil "IPVA"; em inglês "Vehicle registration"; em
 * japonês 自動車税). O modelo só preenche nome, ritmo e dia: o valor e a categoria continuam com a pessoa.
 */
object BillTemplates {
    /** [every]: de quantos em quantos meses vence. [month]: o mês do ano das anuais (1 = janeiro; 0 = tanto faz). */
    data class Template(@StringRes val name: Int, val every: Int, val dueDay: Int, val month: Int = 0)

    val all = listOf(
        Template(R.string.bill_tpl_rent, every = 1, dueDay = 5),
        Template(R.string.bill_tpl_power, every = 1, dueDay = 10),
        Template(R.string.bill_tpl_water, every = 1, dueDay = 10),
        Template(R.string.bill_tpl_internet, every = 1, dueDay = 15),
        Template(R.string.bill_tpl_phone, every = 1, dueDay = 15),
        Template(R.string.bill_tpl_streaming, every = 1, dueDay = 20),
        Template(R.string.bill_tpl_health, every = 1, dueDay = 5),
        Template(R.string.bill_tpl_car_insurance, every = 12, dueDay = 10),
        Template(R.string.bill_tpl_vehicle_tax, every = 12, dueDay = 10, month = 1),
        Template(R.string.bill_tpl_property_tax, every = 12, dueDay = 10, month = 1),
    )

    fun label(t: Template): String = Res.s(t.name)

    /** O primeiro vencimento que o modelo sugere: este mês, ou o próximo mês certo do ano quando a conta é anual. */
    fun firstDue(t: Template, today: LocalDate): YearMonth {
        val now = YearMonth.of(today.year, today.monthValue)
        if (t.month !in 1..12) return now
        val thisYear = YearMonth.of(today.year, t.month)
        return if (thisYear.isBefore(now)) thisYear.plusYears(1) else thisYear
    }
}
