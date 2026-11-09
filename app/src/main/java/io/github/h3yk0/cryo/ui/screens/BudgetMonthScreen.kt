// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.h3yk0.cryo.R
import io.github.h3yk0.cryo.data.db.BudgetAdjustment
import io.github.h3yk0.cryo.data.db.CategoryKind
import io.github.h3yk0.cryo.domain.BudgetUsage
import io.github.h3yk0.cryo.domain.Dates
import io.github.h3yk0.cryo.domain.Ledger
import io.github.h3yk0.cryo.domain.Money
import io.github.h3yk0.cryo.domain.Res
import io.github.h3yk0.cryo.domain.key
import io.github.h3yk0.cryo.domain.ym
import io.github.h3yk0.cryo.ui.LocalNav
import io.github.h3yk0.cryo.ui.components.AmountField
import io.github.h3yk0.cryo.ui.components.CryoButton
import io.github.h3yk0.cryo.ui.components.CryoTopBar
import io.github.h3yk0.cryo.ui.components.EmptyState
import io.github.h3yk0.cryo.ui.components.FieldLabel
import io.github.h3yk0.cryo.ui.components.FlatItem
import io.github.h3yk0.cryo.ui.components.IconBadge
import io.github.h3yk0.cryo.ui.components.LocalContainer
import io.github.h3yk0.cryo.ui.components.LocalSettings
import io.github.h3yk0.cryo.ui.components.MoneyText
import io.github.h3yk0.cryo.ui.components.UsageBar
import io.github.h3yk0.cryo.ui.components.categoryIcon
import io.github.h3yk0.cryo.ui.components.depthCard
import io.github.h3yk0.cryo.ui.components.rememberLedger
import io.github.h3yk0.cryo.ui.theme.CryoTheme
import io.github.h3yk0.cryo.ui.tx.LoadingBox

/** "+R$ 50" / "−R$ 30": quanto o limite mudou só neste mês. */
private fun signed(v: Long) = (if (v >= 0) "+" else "−") + Money.format(kotlin.math.abs(v))

/**
 * RF-ORC-06 (TELA-ORCAMENTO-MES): o painel geral dos gastos do mês. Todas as categorias com orçamento numa tela só, já com
 * o que foi remanejado neste mês (DAD-12), as que não têm orçamento mas tiveram gastos, e os remanejamentos com desfazer. A
 * ação principal ("Remanejar") fica embaixo, ao alcance do polegar (RF-ALC-03).
 */
@Composable
fun BudgetMonthScreen() {
    val nav = LocalNav.current
    val c = LocalContainer.current
    val hide = LocalSettings.current.hideValues
    val l = rememberLedger() ?: return LoadingBox()
    val ym = l.today.ym()
    val usages = l.budgetUsages(ym).sortedBy { it.category.name.lowercase() }
    val adjustments = l.budgetAdjustments(ym)
    val withBudget = usages.map { it.category.id }.toSet()
    val others = l.byCategory(ym, CategoryKind.EXPENSE).filter { it.category != null && it.category.id !in withBudget && it.total > 0 }
    var moving by remember { mutableStateOf<Long?>(null) }   // categoria de destino (0 = a pessoa escolhe)
    var opened by remember { mutableStateOf<Long?>(null) }
    fun money(v: Long) = if (hide) Money.HIDDEN else Money.format(v)

    Scaffold(
        topBar = { CryoTopBar(Res.s(R.string.budget_month_title, Dates.monthInSentence(ym)), onBack = nav::back) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (usages.size >= 2) Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp)) {
                CryoButton(onClick = { moving = 0L }, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                    Icon(Icons.Rounded.SwapHoriz, null); Spacer(Modifier.width(8.dp)); Text(Res.s(R.string.budget_month_move))
                }
            }
        },
    ) { pad ->
        LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (usages.isEmpty()) item {
                EmptyState(Icons.Rounded.PieChart, Res.s(R.string.plan_defina_limites_por_categoria), Res.s(R.string.plan_cryo_avisa_quando_voce))
            } else item {
                val limit = usages.sumOf { it.limit }
                val spent = usages.sumOf { it.spent }
                val daysLeft = ym.lengthOfMonth() - l.today.dayOfMonth + 1
                Column(Modifier.fillMaxWidth().depthCard().padding(18.dp)) {
                    Text(Res.s(R.string.plan_total_planejado, Dates.monthInSentence(ym)), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(verticalAlignment = Alignment.Bottom) {
                        MoneyText(spent, style = MaterialTheme.typography.headlineSmall)
                        Text(Res.s(R.string.amount_of_total, money(limit)), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.height(8.dp))
                    UsageBar(spent.toFloat() / limit.coerceAtLeast(1))
                    Spacer(Modifier.height(6.dp))
                    Text(
                        if (limit - spent >= 0) Res.s(R.string.plan_restam_cerca_por_dia, money(limit - spent), money((limit - spent) / daysLeft))
                        else Res.s(R.string.plan_voce_passou_total_planejado, money(spent - limit)),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            items(usages, key = { "b" + it.category.id }) { u ->
                val base = l.budgetOf(u.category.id)?.limitAmount ?: 0L
                MonthRow(u, if (u.limit != base) u.limit - base else null, ::money) { opened = u.category.id }
            }
            if (others.isNotEmpty()) {
                item { SectionLabel(Res.s(R.string.budget_month_no_budget)) }
                items(others, key = { "o" + it.category!!.id }) { t ->
                    FlatItem {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconBadge(categoryIcon(t.category!!.icon), Color(t.category.color), 32.dp)
                            Spacer(Modifier.width(12.dp))
                            Text(t.category.name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(money(t.total), style = MaterialTheme.typography.titleSmall)
                        }
                    }
                }
            }
            if (adjustments.isNotEmpty()) {
                item { SectionLabel(Res.s(R.string.budget_month_moves)) }
                items(adjustmentRows(l, adjustments), key = { "a" + it.first.id }) { (a, text) ->
                    FlatItem {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.SwapHoriz, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(12.dp))
                            Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                            IconButton(onClick = { c.launch { c.repo.deleteBudgetAdjustment(a) } }) {
                                Icon(Icons.AutoMirrored.Rounded.Undo, Res.s(R.string.wallet_desfazer))
                            }
                        }
                    }
                }
            }
        }
    }

    opened?.let { id ->
        val u = usages.firstOrNull { it.category.id == id }
        if (u == null) opened = null else CategoryMonthSheet(l, u, onDismiss = { opened = null }, onBring = { opened = null; moving = id })
    }
    moving?.let { to -> MoveSheet(l, to.takeIf { it > 0 }, onDismiss = { moving = null }) }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 12.dp, start = 4.dp))
}

/** Um remanejamento vira uma linha só ("R$ 50 de Lazer para Mercado"); um limite só do mês, outra. */
private fun adjustmentRows(l: Ledger, list: List<BudgetAdjustment>): List<Pair<BudgetAdjustment, String>> {
    val out = ArrayList<Pair<BudgetAdjustment, String>>()
    val seen = HashSet<String>()
    for (a in list.sortedByDescending { it.createdAt }) {
        val name = l.category[a.categoryId]?.name ?: continue
        val g = a.moveGroup
        if (g == null) { out += a to Res.s(R.string.budget_month_limit_row, name, signed(a.amount)); continue }
        if (!seen.add(g)) continue
        val pair = list.filter { it.moveGroup == g }
        val from = pair.firstOrNull { it.amount < 0 } ?: continue
        val to = pair.firstOrNull { it.amount > 0 } ?: continue
        out += a to Res.s(R.string.budget_month_move_row, Money.format(to.amount), l.category[from.categoryId]?.name.orEmpty(), l.category[to.categoryId]?.name.orEmpty())
    }
    return out
}

@Composable
private fun MonthRow(u: BudgetUsage, changed: Long?, money: (Long) -> String, onClick: () -> Unit) {
    FlatItem(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(categoryIcon(u.category.icon), Color(u.category.color), 36.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(u.category.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(Res.s(R.string.goal_progress, money(u.spent), money(u.limit)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                if (u.remaining >= 0) money(u.remaining) else "−" + money(-u.remaining),
                style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold,
                color = if (u.remaining >= 0) MaterialTheme.colorScheme.onSurface else CryoTheme.colors.expense,
            )
        }
        Spacer(Modifier.height(8.dp))
        UsageBar(u.ratio)
        if (changed != null) Text(
            Res.s(R.string.budget_month_adjusted, signed(changed)), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 4.dp),
        )
    }
}

/** Tocar numa categoria: o limite de sempre, o deste mês, e mudar só neste mês ou trazer de outra. */
@Composable
private fun CategoryMonthSheet(l: Ledger, u: BudgetUsage, onDismiss: () -> Unit, onBring: () -> Unit) {
    val c = LocalContainer.current
    val ym = l.today.ym()
    val base = l.budgetOf(u.category.id)?.limitAmount ?: 0L
    var amount by remember { mutableLongStateOf(u.limit) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.verticalScroll(rememberScrollState()).imePadding().padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(categoryIcon(u.category.icon), Color(u.category.color), 40.dp)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(u.category.name, style = MaterialTheme.typography.titleLarge)
                    Text(Res.s(R.string.budget_cat_base, Money.format(base)), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(16.dp))
            AmountField(amount, { amount = it }, label = Res.s(R.string.budget_cat_new_limit))
            Spacer(Modifier.height(16.dp))
            CryoButton(
                onClick = {
                    onDismiss()
                    c.launch { c.repo.adjustMonthLimit(u.category.id, ym.key(), amount - u.limit) }
                },
                enabled = amount != u.limit, modifier = Modifier.fillMaxWidth().height(54.dp),
            ) { Text(Res.s(R.string.budget_cat_save)) }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onBring, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text(Res.s(R.string.budget_cat_bring)) }
        }
    }
}

/** RF-ORC-04: remanejar só neste mês (de uma categoria com folga para outra). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun MoveSheet(l: Ledger, presetTo: Long?, onDismiss: () -> Unit) {
    val c = LocalContainer.current
    val ym = l.today.ym()
    val usages = l.budgetUsages(ym)
    var to by remember { mutableStateOf(presetTo ?: usages.firstOrNull { it.remaining < 0 }?.category?.id) }
    var from by remember { mutableStateOf(usages.filter { it.category.id != to && it.remaining > 0 }.maxByOrNull { it.remaining }?.category?.id) }
    val need = usages.firstOrNull { it.category.id == to }?.let { -it.remaining }?.coerceAtLeast(0) ?: 0L
    val free = usages.firstOrNull { it.category.id == from }?.remaining?.coerceAtLeast(0) ?: 0L
    var amount by remember(from, to) { mutableLongStateOf(if (need > 0) ((minOf(need, free) + 999) / 1000 * 1000).coerceAtMost(free) else 0L) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.verticalScroll(rememberScrollState()).imePadding().padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
            Text(Res.s(R.string.budget_move_title), style = MaterialTheme.typography.titleLarge)
            Text(Res.s(R.string.budget_move_text), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            FieldLabel(Res.s(R.string.budget_move_from))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                usages.filter { it.category.id != to }.forEach { u ->
                    FilterChip(
                        selected = from == u.category.id, onClick = { from = u.category.id },
                        label = { Text(Res.s(R.string.budget_move_free, u.category.name, Money.format(u.remaining.coerceAtLeast(0)))) },
                    )
                }
            }
            FieldLabel(Res.s(R.string.budget_move_to))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                usages.filter { it.category.id != from }.forEach { u ->
                    FilterChip(selected = to == u.category.id, onClick = { to = u.category.id }, label = { Text(u.category.name) })
                }
            }
            Spacer(Modifier.height(12.dp))
            AmountField(amount, { amount = it })
            Spacer(Modifier.height(16.dp))
            val fromLimit = from?.let { l.monthLimit(it, ym) } ?: 0L
            CryoButton(
                onClick = {
                    val f = from ?: return@CryoButton
                    val t = to ?: return@CryoButton
                    onDismiss()
                    c.launch {
                        val group = c.repo.moveBudget(f, t, amount, ym.key())
                        c.message(
                            Res.s(R.string.moment_moved, Money.format(amount), l.category[f]?.name.orEmpty(), l.category[t]?.name.orEmpty()),
                            Res.s(R.string.wallet_desfazer),
                        ) { c.repo.undoBudgetMove(group) }
                    }
                },
                enabled = from != null && to != null && from != to && amount in 1..fromLimit,
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) { Text(Res.s(R.string.budget_move_button, Money.format(amount))) }
        }
    }
}
