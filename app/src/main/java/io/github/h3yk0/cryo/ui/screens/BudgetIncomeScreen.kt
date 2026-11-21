// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
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
import io.github.h3yk0.cryo.domain.Bucket
import io.github.h3yk0.cryo.domain.IncomePlan
import io.github.h3yk0.cryo.domain.IncomePlanner
import io.github.h3yk0.cryo.domain.Money
import io.github.h3yk0.cryo.domain.Num
import io.github.h3yk0.cryo.domain.PlanCategory
import io.github.h3yk0.cryo.domain.PlanInputs
import io.github.h3yk0.cryo.domain.Res
import io.github.h3yk0.cryo.domain.SavingKind
import io.github.h3yk0.cryo.ui.LocalNav
import io.github.h3yk0.cryo.ui.components.AmountField
import io.github.h3yk0.cryo.ui.components.CryoButton
import io.github.h3yk0.cryo.ui.components.CryoTopBar
import io.github.h3yk0.cryo.ui.components.FlatItem
import io.github.h3yk0.cryo.ui.components.IconBadge
import io.github.h3yk0.cryo.ui.components.LocalContainer
import io.github.h3yk0.cryo.ui.components.LocalSettings
import io.github.h3yk0.cryo.ui.components.MoneyText
import io.github.h3yk0.cryo.ui.components.SectionTitle
import io.github.h3yk0.cryo.ui.components.Stepper
import io.github.h3yk0.cryo.ui.components.SwitchRow
import io.github.h3yk0.cryo.ui.components.categoryIcon
import io.github.h3yk0.cryo.ui.components.depthCard
import io.github.h3yk0.cryo.ui.components.rememberLedger
import io.github.h3yk0.cryo.ui.theme.CryoTheme
import io.github.h3yk0.cryo.ui.tx.LoadingBox

/**
 * RF-ORC-07/08 (TELA-ORC-RENDA, rota `plan/budgets/income`): o orçamento pela renda. A pessoa conta quanto ganha (o Cryo
 * já preenche com o que achou nos lançamentos), escolhe como dividir (50/30/20 é só o ponto de partida) e vê os limites
 * sugeridos para cada categoria e para onde vai o que sobra. Nada muda nos orçamentos até "Usar estes limites" (PRI-09).
 */
@Composable
fun BudgetIncomeScreen() {
    val nav = LocalNav.current
    val c = LocalContainer.current
    val hide = LocalSettings.current.hideValues
    val l = rememberLedger() ?: return LoadingBox()
    val store = c.incomePlan
    val guess = remember(l) { IncomePlanner.guess(l) }
    // As escolhas ficam guardadas a cada toque; [version] refaz o plano quando algo muda.
    var version by remember { mutableIntStateOf(0) }
    val inputs = remember(guess, version) { store.inputs(guess) }
    val buckets = remember(version) { store.buckets() }
    val plan = remember(l, inputs, buckets) { IncomePlanner.plan(l, inputs, buckets) }
    // Os limites que a pessoa editou nesta tela (antes de "Usar estes limites").
    val edited = remember { mutableStateMapOf<Long, Long>() }
    fun money(v: Long) = if (hide) Money.HIDDEN else Money.format(v)
    fun changed() { version++ }

    Scaffold(
        topBar = { CryoTopBar(Res.s(R.string.inc_title), onBack = nav::back) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (plan.categories.isNotEmpty() && plan.base > 0) Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp)) {
                CryoButton(
                    onClick = {
                        val limits = plan.categories.associate { it.categoryId to (edited[it.categoryId] ?: it.limit) }.filterValues { it > 0 }
                        c.launch {
                            for ((id, v) in limits) c.repo.saveBudget(id, v)
                            c.message(Res.p(R.plurals.inc_applied, limits.size, limits.size))
                        }
                        nav.back()
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                ) { Text(Res.s(R.string.inc_apply)) }
            }
        },
    ) { pad ->
        LazyColumn(
            Modifier.fillMaxSize().padding(pad).imePadding(),
            contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(Res.s(R.string.inc_intro), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item { IncomeCard(inputs, guess, plan, ::money, onFixed = { store.setFixed(it); changed() }, onVariable = { store.setVariable(it); changed() },
                onUseVariable = { store.setUseVariable(it); changed() }, onReset = { store.resetIncome(); changed() }) }
            if (plan.base <= 0) {
                item { Text(Res.s(R.string.inc_need_income), style = MaterialTheme.typography.bodyMedium, color = CryoTheme.colors.warning) }
                return@LazyColumn
            }
            item { SplitCard(inputs, plan, ::money, onSplit = { n, w -> store.setSplit(n, w); changed() }, onReset = { store.resetSplit(); changed() }) }
            item {
                SectionTitle(Res.s(R.string.inc_limits))
                Text(Res.s(R.string.inc_limit_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (plan.categories.isEmpty()) item {
                Text(Res.s(R.string.inc_limits_none), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            items(plan.categories, key = { "c" + it.categoryId }) { pc ->
                val cat = l.category[pc.categoryId] ?: return@items
                LimitRow(
                    pc, cat, edited[pc.categoryId] ?: pc.limit, ::money,
                    onLimit = { edited[pc.categoryId] = it },
                    onBucket = { b -> store.setBucket(pc.categoryId, b); edited.remove(pc.categoryId); changed() },
                )
            }
            item { SavingCard(plan, inputs, ::money) }
            item { Spacer(Modifier.height(72.dp)) }
        }
    }
}

@Composable
private fun IncomeCard(
    inputs: PlanInputs, guess: io.github.h3yk0.cryo.domain.IncomeGuess, plan: IncomePlan, money: (Long) -> String,
    onFixed: (Long) -> Unit, onVariable: (Long) -> Unit, onUseVariable: (Boolean) -> Unit, onReset: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().depthCard().padding(16.dp)) {
        Text(Res.s(R.string.inc_income), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        AmountField(inputs.fixed, onFixed, label = Res.s(R.string.inc_fixed))
        Text(
            Res.s(R.string.inc_fixed_hint, money(guess.fixed)), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp),
        )
        Spacer(Modifier.height(8.dp))
        SwitchRow(Res.s(R.string.inc_use_variable), inputs.useVariable, onUseVariable)
        if (inputs.useVariable) {
            AmountField(inputs.variable, onVariable, label = Res.s(R.string.inc_variable))
            Text(
                if (guess.variableMonths > 0) Res.s(R.string.inc_variable_range, money(guess.variableLow), money(guess.variableHigh), money(guess.variableAvg))
                else Res.s(R.string.inc_variable_none),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp),
            )
            // RF-ORC-07: a renda variável oscila; o aviso fica sempre à vista quando ela entra na conta.
            Text(
                Res.s(R.string.inc_variable_warning), style = MaterialTheme.typography.bodySmall,
                color = CryoTheme.colors.warning, modifier = Modifier.padding(top = 4.dp),
            )
        }
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(Res.s(R.string.inc_base), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                MoneyText(plan.base, style = MaterialTheme.typography.titleLarge)
            }
            TextButton(onClick = onReset) { Text(Res.s(R.string.inc_reset)) }
        }
    }
}

@Composable
private fun SplitCard(inputs: PlanInputs, plan: IncomePlan, money: (Long) -> String, onSplit: (Int, Int) -> Unit, onReset: () -> Unit) {
    Column(Modifier.fillMaxWidth().depthCard().padding(16.dp)) {
        Text(Res.s(R.string.inc_split), style = MaterialTheme.typography.titleMedium)
        Text(Res.s(R.string.inc_split_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        // De 5 em 5 por cento (o botão de menos ou de mais); o guardar é sempre o que sobra.
        fun step(old: Int, new: Int, max: Int) = (old + if (new > old) STEP else -STEP).coerceIn(0, max)
        Stepper(inputs.needsPct, { v -> val n = step(inputs.needsPct, v, 100); onSplit(n, inputs.wantsPct.coerceAtMost(100 - n)) }, 0..100, Res.s(R.string.inc_needs), format = { Num.percent(it) })
        PartLine(plan.needs, plan.typicalNeeds, money)
        Stepper(inputs.wantsPct, { v -> onSplit(inputs.needsPct, step(inputs.wantsPct, v, 100 - inputs.needsPct)) }, 0..(100 - inputs.needsPct), Res.s(R.string.inc_wants), format = { Num.percent(it) })
        PartLine(plan.wants, plan.typicalWants, money)
        Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(Res.s(R.string.inc_save_pct, inputs.savePct), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            MoneyText(plan.save, style = MaterialTheme.typography.titleMedium, color = CryoTheme.colors.income)
        }
        if (plan.fixedCostsOver) {
            Text(
                Res.s(R.string.inc_fixed_costs_over, money(plan.fixedCosts), money(plan.needs)), style = MaterialTheme.typography.bodySmall,
                color = CryoTheme.colors.warning, modifier = Modifier.padding(top = 6.dp),
            )
        }
        if (inputs.needsPct != IncomePlanner.NEEDS_PCT || inputs.wantsPct != IncomePlanner.WANTS_PCT) {
            TextButton(onClick = onReset) { Text(Res.s(R.string.inc_split_reset)) }
        }
    }
}

/** O valor da parte e quanto a pessoa costuma gastar nela hoje. */
@Composable
private fun PartLine(amount: Long, typical: Long, money: (Long) -> String) {
    Row(Modifier.fillMaxWidth().padding(bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            Res.s(R.string.inc_today_you_spend, money(typical)), style = MaterialTheme.typography.bodySmall,
            color = if (typical > amount) CryoTheme.colors.warning else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f),
        )
        MoneyText(amount, style = MaterialTheme.typography.titleSmall)
    }
}

@Composable
private fun LimitRow(
    pc: PlanCategory, cat: io.github.h3yk0.cryo.data.db.Category, limit: Long, money: (Long) -> String,
    onLimit: (Long) -> Unit, onBucket: (Bucket) -> Unit,
) {
    FlatItem {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(categoryIcon(cat.icon), Color(cat.color))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(cat.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(Res.s(R.string.inc_typical, money(pc.typical)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        // A parte de cada categoria (a pessoa troca quando o padrão não serve: PRI-09).
        Row(Modifier.padding(top = 6.dp)) {
            FilterChip(pc.bucket == Bucket.NEEDS, onClick = { if (pc.bucket != Bucket.NEEDS) onBucket(Bucket.NEEDS) }, label = { Text(Res.s(R.string.inc_needs_short)) })
            Spacer(Modifier.width(8.dp))
            FilterChip(pc.bucket == Bucket.WANTS, onClick = { if (pc.bucket != Bucket.WANTS) onBucket(Bucket.WANTS) }, label = { Text(Res.s(R.string.inc_wants_short)) })
        }
        AmountField(limit, onLimit, label = cat.name, modifier = Modifier.padding(top = 6.dp))
    }
}

@Composable
private fun SavingCard(plan: IncomePlan, inputs: PlanInputs, money: (Long) -> String) {
    if (plan.save <= 0) return
    Column(Modifier.fillMaxWidth().depthCard().padding(16.dp)) {
        Text(Res.s(R.string.inc_saving, money(plan.save)), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        Text(
            if (plan.reserveDone) Res.s(R.string.inc_reserve_done, money(plan.reserveNow))
            else Res.p(R.plurals.inc_reserve_progress, plan.reserveMonths ?: 0, money(plan.reserveNow), money(plan.reserveTarget), plan.reserveMonths ?: 0),
            style = MaterialTheme.typography.bodyMedium,
        )
        for (slice in plan.saving) {
            Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(Res.s(savingTitle(slice.kind)), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    Text(Res.s(savingDesc(slice.kind)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.width(8.dp))
                MoneyText(slice.amount, style = MaterialTheme.typography.titleSmall)
            }
        }
        // RF-ORC-08: só tipos de investimento, nunca produtos (o Cryo não indica bancos nem aplicações).
        Text(
            Res.s(R.string.inc_not_advice), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 10.dp),
        )
    }
}

/** Quanto cada toque muda a divisão (em pontos percentuais). */
private const val STEP = 5

private fun savingTitle(k: SavingKind) = when (k) {
    SavingKind.RESERVE -> R.string.inc_save_reserve
    SavingKind.GOALS -> R.string.inc_save_goals
    SavingKind.LONG_TERM -> R.string.inc_save_long
}

private fun savingDesc(k: SavingKind) = when (k) {
    SavingKind.RESERVE -> R.string.inc_save_reserve_desc
    SavingKind.GOALS -> R.string.inc_save_goals_desc
    SavingKind.LONG_TERM -> R.string.inc_save_long_desc
}
