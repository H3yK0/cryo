// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.ui.screens

import io.github.h3yk0.cryo.domain.GeneralBudget
import io.github.h3yk0.cryo.domain.predict.MonthEnds
import io.github.h3yk0.cryo.ui.components.Expandable
import io.github.h3yk0.cryo.ui.components.TileRow
import io.github.h3yk0.cryo.ui.components.TileButton
import io.github.h3yk0.cryo.ui.components.GroupTitle
import io.github.h3yk0.cryo.ui.components.CryoTile
import io.github.h3yk0.cryo.ui.components.ProgressRing
import io.github.h3yk0.cryo.ui.components.Tiles
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.LocalContentColor
import androidx.compose.ui.semantics.heading
import io.github.h3yk0.cryo.domain.BudgetBills
import io.github.h3yk0.cryo.domain.Num
import kotlin.math.roundToInt
import io.github.h3yk0.cryo.domain.engine.GoalOutlook
import io.github.h3yk0.cryo.domain.BillMatcher
import io.github.h3yk0.cryo.domain.BillReconciler
import io.github.h3yk0.cryo.domain.BillSuggestion
import io.github.h3yk0.cryo.domain.predict.CategoryOutlook
import io.github.h3yk0.cryo.ui.components.rememberEngine
import io.github.h3yk0.cryo.R
import io.github.h3yk0.cryo.domain.Res
import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EventRepeat
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.Unarchive
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.h3yk0.cryo.data.Palette
import io.github.h3yk0.cryo.data.db.Bill
import io.github.h3yk0.cryo.data.BillTemplates
import io.github.h3yk0.cryo.data.db.CategoryKind
import io.github.h3yk0.cryo.data.db.Goal
import io.github.h3yk0.cryo.data.db.Tx
import io.github.h3yk0.cryo.data.db.TxType
import io.github.h3yk0.cryo.domain.BillState
import io.github.h3yk0.cryo.domain.BillStatus
import io.github.h3yk0.cryo.domain.BudgetUsage
import io.github.h3yk0.cryo.domain.Dates
import io.github.h3yk0.cryo.domain.dayClamped
import io.github.h3yk0.cryo.domain.ymOf
import io.github.h3yk0.cryo.domain.capFirst
import io.github.h3yk0.cryo.domain.BillCycle
import io.github.h3yk0.cryo.domain.DebtInfo
import io.github.h3yk0.cryo.domain.DebtMath
import io.github.h3yk0.cryo.domain.Ledger
import io.github.h3yk0.cryo.domain.Money
import io.github.h3yk0.cryo.domain.key
import io.github.h3yk0.cryo.domain.ym
import io.github.h3yk0.cryo.notify.Reminders
import io.github.h3yk0.cryo.ui.LocalNav
import io.github.h3yk0.cryo.ui.Routes
import io.github.h3yk0.cryo.ui.components.AmountField
import io.github.h3yk0.cryo.ui.components.CategoryGrid
import io.github.h3yk0.cryo.ui.components.ColorChooser
import io.github.h3yk0.cryo.ui.components.ConfirmDialog
import io.github.h3yk0.cryo.ui.components.FlatItem
import io.github.h3yk0.cryo.ui.components.CryoTopBar
import io.github.h3yk0.cryo.ui.components.DatePickerDialogField
import io.github.h3yk0.cryo.ui.components.EmptyState
import io.github.h3yk0.cryo.ui.components.FieldLabel
import io.github.h3yk0.cryo.ui.components.IconBadge
import io.github.h3yk0.cryo.ui.components.LocalContainer
import io.github.h3yk0.cryo.ui.components.MonthSelector
import io.github.h3yk0.cryo.ui.components.LocalSettings
import io.github.h3yk0.cryo.ui.components.MoneyText
import io.github.h3yk0.cryo.ui.components.Segmented
import io.github.h3yk0.cryo.ui.components.Source
import io.github.h3yk0.cryo.ui.components.SourceChips
import io.github.h3yk0.cryo.ui.components.Stepper
import io.github.h3yk0.cryo.ui.components.SwitchRow
import io.github.h3yk0.cryo.ui.components.UsageBar
import io.github.h3yk0.cryo.ui.components.categoryIcon
import io.github.h3yk0.cryo.ui.components.rememberLedger
import io.github.h3yk0.cryo.ui.theme.CryoTheme
import java.time.LocalDate
import io.github.h3yk0.cryo.ui.tx.LoadingBox
import java.time.temporal.ChronoUnit
import kotlin.math.max
import io.github.h3yk0.cryo.ui.components.CryoButton
import io.github.h3yk0.cryo.ui.components.depthCard
import io.github.h3yk0.cryo.ui.components.CryoSheet
import io.github.h3yk0.cryo.ui.components.ArrangeGroup
import io.github.h3yk0.cryo.ui.components.ArrangeSheet
import io.github.h3yk0.cryo.ui.components.Block
import io.github.h3yk0.cryo.ui.components.OrganizeButton
import io.github.h3yk0.cryo.ui.components.arranged

@Composable
fun PlanScreen(initialTab: Int) {
    val l = rememberLedger() ?: return LoadingBox()
    val tabState = rememberSaveable(initialTab) { mutableIntStateOf(initialTab.coerceIn(0, 2)) }
    var tab by tabState
    // RF-GES-02 (1.7, LA7): arrastar para o lado anda entre as partes antes de ir para a aba vizinha.
    val step = remember(tabState) { { dir: Int -> val t = tabState.intValue + dir; if (t in 0..2) { tabState.intValue = t; true } else false } }
    io.github.h3yk0.cryo.ui.components.TabSwipeInner(step)
    Scaffold(topBar = { CryoTopBar(Res.s(R.string.tab_plan)) }, contentWindowInsets = WindowInsets(0, 0, 0, 0)) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            PrimaryTabRow(selectedTabIndex = tab) {
                listOf(Res.s(R.string.home_shortcut_budgets), Res.s(R.string.home_shortcut_bills), Res.s(R.string.home_shortcut_goals)).forEachIndexed { i, t ->
                    Tab(selected = tab == i, onClick = { tab = i }, text = { Text(t, maxLines = 1) })
                }
            }
            // DS-MOV-09: a parte nova entra pelo lado do passo (para a frente ou para trás).
            androidx.compose.animation.AnimatedContent(tab, transitionSpec = { with(io.github.h3yk0.cryo.ui.components.Motion) { steps() } }, label = "planTab") { t ->
                when (t) {
                    0 -> BudgetsTab(l)
                    1 -> BillsTab(l)
                    else -> GoalsTab(l)
                }
            }
        }
    }
}

/* ============================== Orçamentos ============================== */

@Composable
private fun BudgetsTab(l: Ledger) {
    val c = LocalContainer.current
    val nav = LocalNav.current
    val hide = LocalSettings.current.hideValues
    val ym = l.today.ym()
    val usages = l.budgetUsages(ym)
    var editing by remember { mutableStateOf<Pair<Long?, Long>?>(null) } // categoria, valor sugerido
    // RF-MOT-07: limites sugeridos e ritmo de cada categoria vêm do motor (enquanto ele calcula, a sugestão simples).
    val engine = rememberEngine()
    val suggestions = engine?.budgetSuggestions?.mapNotNull { sg -> l.category[sg.categoryId]?.let { it to sg.amount } } ?: remember(l) { budgetSuggestions(l) }
    val pace = engine?.pace?.associateBy { it.categoryId }.orEmpty()
    // RF-PRV-06: como cada categoria deve fechar o mês (só enquanto o mês não acabou).
    val forecasts = if (l.today.dayOfMonth < ym.lengthOfMonth()) engine?.categories?.associateBy { it.categoryId }.orEmpty() else emptyMap()
    // RF-ORC-09 (1.6): as contas fixas que não cabem no orçamento (o "Agora não" vale até elas mudarem).
    val bbVersion by c.budgetBillsVersion.collectAsState()
    val billSuggestions = remember(l, bbVersion) { BudgetBills.suggestions(l, ym, c.budgetBills.dismissed()) }
    var seeingBills by remember { mutableStateOf<BudgetBills.Suggestion?>(null) }

    // RF-ORC-10: o Geral (o quanto se pode gastar da renda no mês) e a sugestão pela renda fixa (RF-PRV-10).
    val general = GeneralBudget.usage(l, ym)
    val generalSuggestion = remember(l) { GeneralBudget.suggestion(MonthEnds.of(l)) }
    var editingGeneral by remember { mutableStateOf(false) }

    val layout = LocalSettings.current.layout(io.github.h3yk0.cryo.domain.Layouts.BUDGETS)
    var organizing by rememberSaveable { mutableStateOf(false) }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
      // RF-ORG (1.7, LA8): as partes de Orçamentos são blocos que a pessoa arruma; o botão de novo orçamento fica no fim.
      arranged(layout, listOfNotNull(
        Block("geral", Res.s(R.string.budget_general_title)) { item(key = "geral") { GeneralBudgetCard(general, generalSuggestion, hide) { editingGeneral = true } } },

        // RF-ORC-11: as sugestões das contas fixas juntas num cartão só, fechadas com setinha (DS-EXP).
        if (billSuggestions.isEmpty()) null else Block("bb", Res.s(R.string.org_block_bill_suggestions)) {
            item(key = "bb") {
                FlatItem {
                    Expandable(
                        Res.s(R.string.bb_group_title, billSuggestions.size),
                        summary = budgetBillText(l, billSuggestions.first(), hide),
                    ) {
                        billSuggestions.forEachIndexed { i, sg ->
                            if (i > 0) HorizontalDivider(Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            BudgetBillItem(
                                l, sg, hide,
                                onApply = { c.launch { applyBudgetBills(c, sg); c.message(Res.s(R.string.bb_done, l.category[sg.categoryId]?.name.orEmpty())) } },
                                onNotNow = { c.budgetBills.dismiss(sg.categoryId, sg.signature); c.budgetBillsVersion.value++ },
                                onSee = { seeingBills = sg },
                            )
                        }
                    }
                }
            }
        },

        // DS-BLOCO-02: planejar pela renda (RF-ORC-07) e o painel do mês (RF-ORC-06) como botões-bloco.
        Block("acoes", Res.s(R.string.org_block_budget_actions)) { item(key = "acoes") {
            TileRow(
                first = { m -> TileButton(Res.s(R.string.plan_by_income_title), Icons.Rounded.AccountBalanceWallet, m, subtitle = Res.s(R.string.plan_by_income_short), stacked = usages.isNotEmpty()) { nav.go(Routes.BUDGET_INCOME) } },
                second = if (usages.isEmpty()) null else { m ->
                    TileButton(Res.s(R.string.moment_panel), Icons.Rounded.Dashboard, m, subtitle = Res.s(R.string.budget_panel_short), stacked = true) { nav.go(Routes.BUDGET_MONTH) }
                },
            )
        } },

        Block("categorias", Res.s(R.string.budget_by_category)) {
        if (usages.isEmpty()) {
            item {
                EmptyState(
                    Icons.Rounded.PieChart, Res.s(R.string.plan_defina_limites_por_categoria),
                    Res.s(R.string.plan_cryo_avisa_quando_voce),
                )
            }
        } else {
            item(key = "cat") { GroupTitle(Res.s(R.string.budget_by_category)) }
            // RF-ORC-11: cada categoria num bloco, em duas colunas.
            usages.chunked(2).forEachIndexed { i, row ->
                item(key = "linha$i-" + row.joinToString("-") { it.category.id.toString() }) {
                    TileRow(
                        first = { m -> BudgetTile(row[0], hide, pace[row[0].category.id]?.crossDay, forecasts[row[0].category.id], m) { editing = row[0].category.id to row[0].limit } },
                        second = row.getOrNull(1)?.let { u -> { m -> BudgetTile(u, hide, pace[u.category.id]?.crossDay, forecasts[u.category.id], m) { editing = u.category.id to u.limit } } },
                    )
                }
            }
        }
        },
        // RF-ORC-03: as sugestões pela média, fechadas com setinha.
        if (suggestions.isEmpty()) null else Block("media", Res.s(R.string.plan_sugestoes_pela_sua_media)) {
            item(key = "media") {
                FlatItem {
                    Expandable(Res.s(R.string.plan_sugestoes_pela_sua_media), summary = suggestions.joinToString(", ") { it.first.name }) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            suggestions.forEach { (cat, v) ->
                                AssistChip(onClick = { editing = cat.id to v }, label = { Text("${cat.name} · ${if (hide) Money.HIDDEN else Money.format(v)}") })
                            }
                        }
                    }
                }
            }
        },
      ))
        item(key = "novo") {
            CryoButton(onClick = { editing = null to 0L }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Icon(Icons.Rounded.Add, null); Spacer(Modifier.width(8.dp)); Text(Res.s(R.string.plan_novo_orcamento))
            }
        }
        item(key = "organizar") { OrganizeButton { organizing = true } }
    }
    if (organizing) {
        ArrangeSheet(listOf(ArrangeGroup(io.github.h3yk0.cryo.domain.Layouts.BUDGETS, null, listOf(
            "geral" to Res.s(R.string.budget_general_title),
            "bb" to Res.s(R.string.org_block_bill_suggestions),
            "acoes" to Res.s(R.string.org_block_budget_actions),
            "categorias" to Res.s(R.string.budget_by_category),
            "media" to Res.s(R.string.plan_sugestoes_pela_sua_media),
        ))), onDismiss = { organizing = false })
    }

    if (editingGeneral) {
        GeneralBudgetSheet(
            initial = l.budgetOf(GeneralBudget.ID)?.limitAmount ?: generalSuggestion ?: 0L,
            suggestion = generalSuggestion, exists = general != null,
            onDismiss = { editingGeneral = false },
            onSave = { v -> editingGeneral = false; c.launch { c.repo.saveBudget(GeneralBudget.ID, v); c.message(Res.s(R.string.plan_orcamento_salvo)) } },
            onDelete = { editingGeneral = false; c.launch { c.repo.deleteBudget(GeneralBudget.ID); c.message(Res.s(R.string.plan_orcamento_removido)) } },
        )
    }
    seeingBills?.let { sg -> BudgetBillsSheet(l, sg, onDismiss = { seeingBills = null }) }
    editing?.let { (catId, suggested) ->
        BudgetSheet(
            l, catId, if (catId != null) l.budgetOf(catId)?.limitAmount ?: suggested else suggested,
            onDismiss = { editing = null },
            onSave = { id, v -> editing = null; c.launch { c.repo.saveBudget(id, v); c.message(Res.s(R.string.plan_orcamento_salvo)) } },
            onDelete = { id -> editing = null; c.launch { c.repo.deleteBudget(id); c.message(Res.s(R.string.plan_orcamento_removido)) } },
        )
    }
}

/* ------------------------- RF-ORC-09: orçamento × contas fixas ------------------------- */

private fun bbMoney(v: Long, hide: Boolean) = if (hide) Money.HIDDEN else Money.format(v)

/** A frase principal da sugestão (criar o orçamento, subir o limite ou só os meses das contas que não são mensais). */
fun budgetBillText(l: Ledger, sg: BudgetBills.Suggestion, hide: Boolean = false): String {
    val name = l.category[sg.categoryId]?.name.orEmpty()
    return when {
        sg.createsBudget -> Res.s(R.string.bb_create, name, bbMoney(sg.monthly, hide))
        sg.raisesLimit -> Res.s(R.string.bb_raise, name, bbMoney(sg.monthly, hide), bbMoney(sg.current ?: 0L, hide))
        else -> sg.monthOnly.first().let { m -> monthOnlyText(m, hide) }
    }
}

private fun monthOnlyText(m: BudgetBills.MonthOnly, hide: Boolean) =
    Res.s(R.string.bb_month_only, Dates.monthYearInSentence(m.ym), m.bills.joinToString(", ") { it.name }, bbMoney(m.delta, hide))

fun budgetBillAction(sg: BudgetBills.Suggestion, hide: Boolean = false): String = when {
    sg.createsBudget -> Res.s(R.string.bb_apply_create, bbMoney(sg.suggested, hide))
    sg.raisesLimit -> Res.s(R.string.bb_apply_raise, bbMoney(sg.suggested, hide))
    else -> Res.s(R.string.bb_apply_months)
}

/** Aceitar: o limite de sempre (ou o orçamento novo) e os ajustes só nos meses das contas que não são mensais (DAD-12). */
suspend fun applyBudgetBills(c: io.github.h3yk0.cryo.AppContainer, sg: BudgetBills.Suggestion) {
    if (sg.current == null || sg.suggested > sg.current) c.repo.saveBudget(sg.categoryId, sg.suggested)
    for (m in sg.monthOnly) c.repo.adjustMonthLimit(sg.categoryId, m.ym.key(), m.delta)
}

@Composable
private fun BudgetBillItem(l: Ledger, sg: BudgetBills.Suggestion, hide: Boolean, onApply: () -> Unit, onNotNow: () -> Unit, onSee: () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.Top) {
            Icon(Icons.Rounded.EventRepeat, null, Modifier.size(20.dp), tint = CryoTheme.colors.warning)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(budgetBillText(l, sg, hide), style = MaterialTheme.typography.bodyLarge)
                // As contas que não são mensais, mês a mês (a primeira já está na frase quando é só ela).
                val extra = if (!sg.createsBudget && !sg.raisesLimit) sg.monthOnly.drop(1) else sg.monthOnly
                extra.forEach { m ->
                    Text(monthOnlyText(m, hide), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
            FilledTonalButton(onClick = onApply) { Text(budgetBillAction(sg, hide)) }
            TextButton(onClick = onNotNow) { Text(Res.s(R.string.bb_not_now)) }
            TextButton(onClick = onSee) { Text(Res.s(R.string.bb_see_bills)) }
        }
    }
}

/** As contas fixas da categoria; cada uma pode trocar de categoria (ou ir para uma categoria nova). */
@Composable
private fun BudgetBillsSheet(l: Ledger, sg: BudgetBills.Suggestion, onDismiss: () -> Unit) {
    val c = LocalContainer.current
    val nav = LocalNav.current
    val hide = LocalSettings.current.hideValues
    var moving by remember { mutableStateOf<io.github.h3yk0.cryo.data.db.Bill?>(null) }
    CryoSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
            val bill = moving
            if (bill == null) {
                Text(Res.s(R.string.bb_sheet_title, l.category[sg.categoryId]?.name.orEmpty()), style = MaterialTheme.typography.titleLarge)
                Text(Res.s(R.string.bb_sheet_text), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 8.dp))
                sg.bills.forEach { b ->
                    FlatItem {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(b.name, style = MaterialTheme.typography.titleSmall)
                                Text(bbMoney(b.amount, hide), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            TextButton(onClick = { moving = b }) { Text(Res.s(R.string.bb_change_category)) }
                        }
                    }
                }
            } else {
                Text(Res.s(R.string.bb_pick_category), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 8.dp))
                l.categories(CategoryKind.EXPENSE).filter { it.id != bill.categoryId }.forEach { cat ->
                    Row(
                        Modifier.fillMaxWidth().clickable {
                            onDismiss()
                            c.launch { c.repo.saveBill(bill.copy(categoryId = cat.id)); c.message(Res.s(R.string.bb_moved, bill.name, cat.name)) }
                        }.padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconBadge(categoryIcon(cat.icon), Color(cat.color), 32.dp)
                        Spacer(Modifier.width(12.dp))
                        Text(cat.name, style = MaterialTheme.typography.bodyLarge)
                    }
                }
                TextButton(onClick = { onDismiss(); nav.go(Routes.categoryEdit(kind = CategoryKind.EXPENSE)) }) {
                    Icon(Icons.Rounded.Add, null); Spacer(Modifier.width(6.dp)); Text(Res.s(R.string.bb_new_category))
                }
            }
        }
    }
}

/**
 * RF-ORC-10: o bloco largo do Geral, na cor do estado (DS-TOM: bom; atenção a partir de 80%; alerta ao passar). Sem o Geral
 * definido, o convite com a sugestão pela renda fixa (a pessoa muda, PRI-09).
 */
@Composable
private fun GeneralBudgetCard(u: GeneralBudget.Usage?, suggestion: Long?, hide: Boolean, onEdit: () -> Unit) {
    val tones = CryoTheme.tones
    val tone = when {
        u == null -> null
        u.ratio >= 1f -> tones.alert
        u.ratio >= 0.8f -> tones.attention
        else -> tones.good
    }
    val fg = tone?.onContainer ?: MaterialTheme.colorScheme.onSurface
    Column(
        Modifier.fillMaxWidth().depthCard(shape = Tiles.Shape, color = tone?.container ?: CryoTheme.depth.card)
            .clickable(role = androidx.compose.ui.semantics.Role.Button, onClick = onEdit).padding(18.dp),
    ) {
        CompositionLocalProvider(LocalContentColor provides fg) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(Icons.Rounded.AccountBalanceWallet, tone?.accent ?: MaterialTheme.colorScheme.primary, 36.dp)
                Spacer(Modifier.width(10.dp))
                Text(Res.s(R.string.budget_general), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f).semantics { heading() })
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = fg.copy(alpha = 0.6f))
            }
            Spacer(Modifier.height(10.dp))
            if (u == null) {
                Text(Res.s(R.string.budget_general_hint), style = MaterialTheme.typography.bodyMedium)
                suggestion?.let {
                    Text(
                        Res.s(R.string.budget_general_suggest, bbMoney(it, hide)), style = MaterialTheme.typography.bodySmall,
                        color = fg.copy(alpha = 0.75f), modifier = Modifier.padding(top = 4.dp),
                    )
                }
                FilledTonalButton(onClick = onEdit, modifier = Modifier.padding(top = 10.dp)) { Text(Res.s(R.string.budget_general_set)) }
                return@CompositionLocalProvider
            }
            Text(
                if (u.remaining >= 0) Res.s(R.string.budget_left, bbMoney(u.remaining, hide)) else Res.s(R.string.budget_over, bbMoney(-u.remaining, hide)),
                style = MaterialTheme.typography.headlineSmall, color = tone?.accent ?: fg,
            )
            Text(Res.s(R.string.goal_progress, bbMoney(u.spent, hide), bbMoney(u.limit, hide)), style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(10.dp))
            UsageBar(u.ratio, height = 10.dp, baseColor = tone?.accent ?: MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            if (u.perDay > 0) Text(Res.s(R.string.budget_general_per_day, bbMoney(u.perDay, hide)), style = MaterialTheme.typography.bodySmall)
            if (u.divided > 0) {
                Text(
                    if (u.overDivided) Res.s(R.string.budget_general_over_divided, bbMoney(u.divided, hide))
                    else Res.s(R.string.budget_general_divided, bbMoney(u.divided, hide), bbMoney(u.limit, hide)),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (u.overDivided) tones.alert.accent else fg.copy(alpha = 0.75f),
                )
            }
        }
    }
}

/** RF-ORC-10: definir, mudar ou tirar o Geral; a sugestão é um toque (e a pessoa digita o que quiser). */
@Composable
private fun GeneralBudgetSheet(initial: Long, suggestion: Long?, exists: Boolean, onDismiss: () -> Unit, onSave: (Long) -> Unit, onDelete: () -> Unit) {
    val hide = LocalSettings.current.hideValues
    var amount by remember { mutableLongStateOf(initial) }
    CryoSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.verticalScroll(rememberScrollState()).imePadding().padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
            Text(Res.s(R.string.budget_general_title), style = MaterialTheme.typography.titleLarge)
            Text(Res.s(R.string.budget_general_hint), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
            AmountField(amount, { amount = it }, label = Res.s(R.string.plan_limite_por_mes))
            if (suggestion != null && suggestion != amount) {
                AssistChip(onClick = { amount = suggestion }, label = { Text(Res.s(R.string.budget_general_use, bbMoney(suggestion, hide))) })
            }
            if (suggestion != null) {
                Text(Res.s(R.string.budget_general_how), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(20.dp))
            CryoButton(onClick = { onSave(amount) }, enabled = amount > 0, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                Text(Res.s(R.string.plan_salvar_orcamento))
            }
            if (exists) {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = onDelete, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                    Text(Res.s(R.string.plan_remover_orcamento), color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

/** Média dos últimos 3 meses, arredondada para cima (de R$ 10 em R$ 10), das maiores categorias sem orçamento. */
private fun budgetSuggestions(l: Ledger): List<Pair<io.github.h3yk0.cryo.data.db.Category, Long>> {
    val has = l.s.budgets.map { it.categoryId }.toSet()
    val months = (1..3).map { l.today.ym().minusMonths(it.toLong()) }
    return l.categories(CategoryKind.EXPENSE).filter { it.id !in has }.mapNotNull { cat ->
        val avg = months.sumOf { l.categorySpent(it, cat.id) } / 3
        if (avg < 5000) null else cat to ((avg + 999) / 1000) * 1000
    }.sortedByDescending { it.second }.take(3)
}

/**
 * RF-ORC-11: o bloco de uma categoria: o ícone, o quanto resta, a barra na cor do estado e, enquanto o mês não acabou, como
 * deve fechar (RF-PRV-06) ou o dia em que o ritmo passa do limite (RF-MOT-07). A cor pastel só quando pede atenção (DS-TOM-02).
 */
@Composable
private fun BudgetTile(u: BudgetUsage, hide: Boolean, crossDay: Int?, forecast: CategoryOutlook?, modifier: Modifier, onClick: () -> Unit) {
    val tones = CryoTheme.tones
    val chance = forecast?.let { (it.chanceOver * 100).roundToInt() } ?: 0
    val tone = when {
        u.ratio >= 1f -> tones.alert
        u.ratio >= 0.8f || crossDay != null || chance >= 50 -> tones.attention
        else -> null
    }
    val lines = buildList {
        if (forecast != null && u.remaining >= 0) {
            add(Res.s(R.string.budget_tile_close, bbMoney(forecast.mid, hide)))
            if (chance >= 5) add(Res.s(R.string.budget_tile_risk, Num.percent(chance)))
        } else if (crossDay != null) {
            add(Res.s(R.string.budget_tile_pace, crossDay))
        } else {
            add(Res.s(R.string.goal_progress, bbMoney(u.spent, hide), bbMoney(u.limit, hide)))
        }
    }
    CryoTile(
        u.category.name, modifier, icon = categoryIcon(u.category.icon), iconColor = Color(u.category.color),
        subtitle = lines.joinToString("\n"), tone = tone, progress = u.ratio, progressColor = Color(u.category.color), onClick = onClick,
        valueContent = {
            Text(
                if (u.remaining >= 0) Res.s(R.string.budget_left, bbMoney(u.remaining, hide)) else Res.s(R.string.budget_over, bbMoney(-u.remaining, hide)),
                style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis,
                color = if (u.remaining < 0) tones.alert.accent else LocalContentColor.current,
            )
        },
    )
}

@Composable
private fun BudgetSheet(
    l: Ledger,
    categoryId: Long?,
    initial: Long,
    onDismiss: () -> Unit,
    onSave: (Long, Long) -> Unit,
    onDelete: (Long) -> Unit,
) {
    var cat by remember { mutableStateOf(categoryId) }
    var amount by remember { mutableLongStateOf(initial) }
    val existing = categoryId?.let { l.budgetOf(it) }
    val options = l.categories(CategoryKind.EXPENSE).filter { it.id == categoryId || l.budgetOf(it.id) == null }
    CryoSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.verticalScroll(rememberScrollState()).imePadding().padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
            Text(if (existing != null) Res.s(R.string.plan_editar_orcamento) else Res.s(R.string.plan_novo_orcamento), style = MaterialTheme.typography.titleLarge)
            Text(Res.s(R.string.plan_quanto_voce_quer_gastar), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
            AmountField(amount, { amount = it }, label = Res.s(R.string.plan_limite_por_mes))
            if (existing == null) {
                FieldLabel(Res.s(R.string.plan_categoria))
                CategoryGrid(options, cat, { cat = it.id })
            } else {
                FieldLabel(Res.s(R.string.plan_categoria_2, l.category[categoryId]?.name))
            }
            Spacer(Modifier.height(20.dp))
            CryoButton(
                onClick = { cat?.let { onSave(it, amount) } }, enabled = cat != null && amount > 0,
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) { Text(Res.s(R.string.plan_salvar_orcamento)) }
            if (existing != null) {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { onDelete(existing.categoryId) }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                    Text(Res.s(R.string.plan_remover_orcamento), color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

/* ============================== Contas fixas ============================== */

/** RF-FIX-12 (1.6): pula a conta fixa no mês e oferece o desfazer. Usado em Planejar e nas Previstas do Extrato. */
fun skipBillWithUndo(c: io.github.h3yk0.cryo.AppContainer, bill: io.github.h3yk0.cryo.data.db.Bill, ym: java.time.YearMonth) {
    c.launch {
        c.repo.skipBill(bill.id, ym)
        c.message(Res.s(R.string.bill_skipped_msg, bill.name, Dates.monthYearInSentence(ym)), Res.s(R.string.wallet_desfazer)) {
            c.repo.unskipBill(bill.id, ym)
        }
    }
}

@Composable
private fun BillsTab(l: Ledger) {
    val nav = LocalNav.current
    val c = LocalContainer.current
    val hide = LocalSettings.current.hideValues
    var ymKey by rememberSaveable { mutableIntStateOf(l.today.ym().key()) }
    val ym = java.time.YearMonth.of(ymKey / 100, ymKey % 100)
    val statuses = l.billStatuses(ym)
    val pay = statuses.filter { it.bill.kind == CategoryKind.EXPENSE }
    val receive = statuses.filter { it.bill.kind == CategoryKind.INCOME }
    val installments = l.activeDebts.mapNotNull { DebtMath.installmentIn(l.debtInfo(it), ym) }.sortedBy { it.due }
    var paying by remember { mutableStateOf<BillStatus?>(null) }
    var undoing by remember { mutableStateOf<BillStatus?>(null) }
    var payingDebt by remember { mutableStateOf<DebtInfo?>(null) }
    // RF-CFX-05: o que já está no app e parece pagamento de conta fixa (o reconhecido com certeza e o que só parece).
    val found = remember(l) { BillReconciler.suggestions(l.s, l.today) { d -> c.engineStore.billAlias(d) } }
    // Qual conta fixa a folha de sugestões mostra: -1 = todas; [thenPay] = a pessoa tocou "Já paguei" e o Cryo achou um candidato.
    var reviewing by remember { mutableStateOf<Long?>(null) }
    var thenPay by remember { mutableStateOf<BillStatus?>(null) }
    val justSaved by c.billReview.collectAsState()
    LaunchedEffect(justSaved, found) {
        // RF-CFX-08: a conta fixa acabou de ser criada ou mudada e já existem movimentações que parecem ela.
        val id = justSaved ?: return@LaunchedEffect
        if (l.bill[id] == null) return@LaunchedEffect
        if (found.any { it.bill.id == id }) reviewing = id
        c.billReview.value = null
    }

    val billLayout = LocalSettings.current.layout(io.github.h3yk0.cryo.domain.Layouts.BILLS)
    var organizingBills by rememberSaveable { mutableStateOf(false) }
    val skipped = l.skippedBills(ym)
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            io.github.h3yk0.cryo.ui.components.MonthSelector(
                Dates.monthYear(ym, l.today), { ymKey = ym.minusMonths(1).key() }, { ymKey = ym.plusMonths(1).key() },
            )
        }
        if (statuses.isEmpty() && installments.isEmpty()) {
            item {
                EmptyState(
                    Icons.Rounded.EventRepeat, Res.s(R.string.plan_nenhuma_conta_fixa),
                    Res.s(R.string.plan_cadastre_aluguel_internet_assinaturas),
                )
            }
        }
        if (found.isNotEmpty()) {
            item {
                Column(Modifier.fillMaxWidth().depthCard().padding(16.dp)) {
                    Text(Res.p(R.plurals.cfx_card_title, found.size, found.size), style = MaterialTheme.typography.bodyLarge)
                    TextButton(onClick = { reviewing = -1L }, contentPadding = PaddingValues(0.dp)) { Text(Res.s(R.string.cfx_card_button)) }
                }
            }
        }
        // RF-FIX-13: três blocos no topo; as que faltam primeiro; as pagas (e as puladas) fechadas com setinha (DS-EXP).
        val onPayBill: (BillStatus) -> Unit = { s -> if (found.any { it.bill.id == s.bill.id && it.ym == s.ym }) { thenPay = s; reviewing = s.bill.id } else paying = s }
        val onUndoBill: (BillStatus) -> Unit = { s -> if (s.inferred) reviewing = s.bill.id else undoing = s }
      // RF-ORG (1.7, LA8): as partes de Contas fixas são blocos que a pessoa arruma; o mês fica no alto e os botões de
      // nova conta fixa, no fim.
      arranged(billLayout, listOfNotNull(
        if (pay.isEmpty()) null else Block("pagar", Res.s(R.string.plan_pagar)) {
            val open = pay.filter { it.payment == null }
            val paid = pay.filter { it.payment != null }
            item(key = "resumo") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    BillStat(Res.s(R.string.plan_pagar), open.sumOf { it.bill.amount }, hide, Modifier.weight(1f), open.any { it.state == BillState.OVERDUE })
                    BillStat(Res.s(R.string.bills_paid), paid.sumOf { it.payment?.amount ?: it.bill.amount }, hide, Modifier.weight(1f))
                    BillStat(Res.s(R.string.bills_total), pay.sumOf { it.payment?.amount ?: it.bill.amount }, hide, Modifier.weight(1f))
                }
            }
            items(open, key = { "p${it.bill.id}" }) { s ->
                // RF-CFX-06: antes de criar um pagamento, o Cryo olha se ele já está no Extrato.
                BillRow(s, l, hide, onPay = { onPayBill(s) }, onUndo = { onUndoBill(s) }, onSkip = { skipBillWithUndo(c, s.bill, s.ym) })
            }
            if (paid.isNotEmpty()) {
                item(key = "pagas") {
                    Expandable(Res.s(R.string.bills_paid_group, paid.size), Modifier.padding(horizontal = 4.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            paid.forEach { s -> BillRow(s, l, hide, onPay = { onPayBill(s) }, onUndo = { onUndoBill(s) }, onSkip = null) }
                        }
                    }
                }
            }
        },
        if (installments.isEmpty()) null else Block("dividas", Res.s(R.string.plan_parcelas_dividas)) {
            item(key = "dividas") {
                SummaryLine(
                    Res.s(R.string.plan_parcelas_dividas), Res.s(R.string.plan_pagas, installments.count { it.paid }, installments.size),
                    installments.sumOf { it.info.debt.installmentAmount }, hide,
                )
            }
            items(installments.filter { !it.paid }, key = { "d${it.info.debt.id}" }) { m -> DebtInstallmentRow(m, l, hide, onPay = { payingDebt = m.info }) }
            val paidDebts = installments.filter { it.paid }
            if (paidDebts.isNotEmpty()) {
                item(key = "dpagas") {
                    Expandable(Res.s(R.string.bills_debts_paid_group, paidDebts.size), Modifier.padding(horizontal = 4.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            paidDebts.forEach { m -> DebtInstallmentRow(m, l, hide, onPay = { payingDebt = m.info }) }
                        }
                    }
                }
            }
        },
        if (receive.isEmpty()) null else Block("receber", Res.s(R.string.plan_receber)) {
            item(key = "receber") {
                val total = receive.sumOf { it.bill.amount }
                val got = receive.count { it.payment != null }
                SummaryLine(Res.s(R.string.plan_receber), Res.s(R.string.plan_recebidos, got, receive.size), total, hide)
            }
            items(receive.filter { it.payment == null }, key = { "r${it.bill.id}" }) { s ->
                BillRow(s, l, hide, onPay = { onPayBill(s) }, onUndo = { onUndoBill(s) }, onSkip = { skipBillWithUndo(c, s.bill, s.ym) })
            }
            val received = receive.filter { it.payment != null }
            if (received.isNotEmpty()) {
                item(key = "recebidas") {
                    Expandable(Res.s(R.string.bills_received_group, received.size), Modifier.padding(horizontal = 4.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            received.forEach { s -> BillRow(s, l, hide, onPay = { onPayBill(s) }, onUndo = { onUndoBill(s) }, onSkip = null) }
                        }
                    }
                }
            }
        },
        // RF-FIX-12: as puladas neste mês, com "Voltar a contar" (fechadas com setinha).
        if (skipped.isEmpty()) null else Block("puladas", Res.s(R.string.org_block_skipped)) {
            item(key = "puladas") {
                Expandable(Res.s(R.string.bills_skipped_group, skipped.size), Modifier.padding(horizontal = 4.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        skipped.forEach { b ->
                            FlatItem {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(b.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(
                                            if (hide) Money.HIDDEN else Money.format(b.amount), style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    TextButton(onClick = { c.launch { c.repo.unskipBill(b.id, ym) } }) { Text(Res.s(R.string.bill_unskip)) }
                                }
                            }
                        }
                    }
                }
            }
        },
        // RF-FIX-13, DS-BLOCO-02: "O que se repete" (RF-MOT-02) num botão-bloco.
        Block("repete", Res.s(R.string.engine_recurring_title)) {
            item(key = "repete") {
                TileButton(Res.s(R.string.engine_recurring_title), Icons.Rounded.Autorenew, subtitle = Res.s(R.string.bills_recurring_sub)) { nav.go(Routes.RECURRING) }
            }
        },
      ))
        item(key = "nova") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                CryoButton(onClick = { nav.go(Routes.billEdit(kind = CategoryKind.EXPENSE)) }, modifier = Modifier.weight(1f).height(52.dp)) {
                    Icon(Icons.Rounded.Add, null); Spacer(Modifier.width(6.dp)); Text(Res.s(R.string.plan_conta_pagar))
                }
                OutlinedButton(onClick = { nav.go(Routes.billEdit(kind = CategoryKind.INCOME)) }, modifier = Modifier.weight(1f).height(52.dp)) {
                    Text(Res.s(R.string.plan_receber))
                }
            }
        }
        item(key = "organizar") { OrganizeButton { organizingBills = true } }
    }
    if (organizingBills) {
        ArrangeSheet(listOf(ArrangeGroup(io.github.h3yk0.cryo.domain.Layouts.BILLS, null, listOf(
            "pagar" to Res.s(R.string.plan_pagar),
            "dividas" to Res.s(R.string.plan_parcelas_dividas),
            "receber" to Res.s(R.string.plan_receber),
            "puladas" to Res.s(R.string.org_block_skipped),
            "repete" to Res.s(R.string.engine_recurring_title),
        ))), onDismiss = { organizingBills = false })
    }

    paying?.let { s ->
        val b = s.bill
        val card = b.cardId?.let { l.card[it] }
        MoneyActionSheet(
            title = if (b.kind == CategoryKind.EXPENSE) Res.s(R.string.plan_marcar_como_paga, b.name) else Res.s(R.string.plan_marcar_como_recebido, b.name),
            subtitle = if (card != null) Res.s(R.string.plan_sera_lancada_cartao, card.name) else Res.s(R.string.plan_confira_valor_pode_mudar),
            initialAmount = b.amount, l = l,
            accountLabel = if (card != null) null else if (b.kind == CategoryKind.EXPENSE) Res.s(R.string.plan_pago_com) else Res.s(R.string.plan_recebido_2),
            initialAccount = b.accountId ?: LocalSettings.current.defaultAccountId,
            confirmLabel = Res.s(R.string.plan_confirmar),
            onDismiss = { paying = null },
            // RF-FIX-11: pago por fora (em dinheiro que o Cryo não acompanha): a conta fixa fica paga e o lançamento
            // nasce sem conta e sem cartão — conta no mês e na categoria, mas não mexe no saldo de conta nenhuma.
            offLabel = Res.s(R.string.plan_fora_do_saldo),
            offHint = Res.s(R.string.plan_fora_do_saldo_hint),
            onConfirmOff = { amount, date ->
                paying = null
                c.launch {
                    c.repo.markBillPaid(b, s.ym, amount, date, null, null)
                    c.message(if (b.kind == CategoryKind.EXPENSE) Res.s(R.string.plan_paga, b.name) else Res.s(R.string.plan_recebido_3, b.name))
                }
            },
            onConfirm = { amount, acc, date ->
                paying = null
                c.launch {
                    c.repo.markBillPaid(b, s.ym, amount, date, acc, card)
                    val alert = if (b.kind == CategoryKind.EXPENSE) c.repo.budgetAlert(b.categoryId, date, amount) else null
                    c.message(alert ?: if (b.kind == CategoryKind.EXPENSE) Res.s(R.string.plan_paga, b.name) else Res.s(R.string.plan_recebido_3, b.name))
                }
            },
        )
    }
    undoing?.let { s ->
        val tx = s.payment
        CryoSheet(onDismissRequest = { undoing = null }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
            Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
                Text(Res.s(R.string.plan_desmarcar_pagamento), style = MaterialTheme.typography.titleLarge)
                Text(
                    Res.s(R.string.cfx_undo_text, s.bill.name), style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 8.dp),
                )
                // A movimentação continua no Extrato e a conta fixa volta a ficar em aberto.
                CryoButton(
                    onClick = { undoing = null; if (tx != null) c.launch { c.repo.unlinkBillPayment(tx.id, never = true) } },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) { Text(Res.s(R.string.cfx_undo_keep)) }
                // Apagar só o que nasceu do botão "Já paguei": o que veio do extrato se apaga no Extrato, de propósito.
                if (tx?.importKey == null) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { undoing = null; c.launch { c.repo.unmarkBillPaid(s.bill, s.ym) } },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                    ) { Text(Res.s(R.string.cfx_undo_delete)) }
                }
            }
        }
    }
    reviewing?.let { id ->
        val list = found.filter { id == -1L || it.bill.id == id }
        val pending = thenPay
        if (list.isEmpty()) {
            // Tudo resolvido (ou nada a mostrar): fecha; se a pessoa queria pagar e ainda está em aberto, segue para o pagamento.
            LaunchedEffect(list) {
                reviewing = null
                thenPay = null
                if (pending != null && l.billStatuses(pending.ym).any { it.bill.id == pending.bill.id && it.payment == null }) paying = pending
            }
        } else {
            BillSuggestionsSheet(
                list, l,
                onOther = pending?.let { p -> { reviewing = null; thenPay = null; paying = p } },
                onDismiss = { reviewing = null; thenPay = null },
            )
        }
    }
    payingDebt?.let { info -> DebtPaySheet(info, l, payoff = false, onDismiss = { payingDebt = null }) }
}

/**
 * RF-CFX-05, 06 e 08: as movimentações que já estão no app e parecem pagamentos de contas fixas. Cada uma com "É este"
 * (liga, sem criar nem mudar valor), "Não é" (o Cryo não pergunta de novo) ou, quando está pago em dobro, "Juntar".
 */
@Composable
private fun BillSuggestionsSheet(list: List<BillSuggestion>, l: Ledger, onOther: (() -> Unit)?, onDismiss: () -> Unit) {
    val c = LocalContainer.current
    val hide = LocalSettings.current.hideValues
    val undo = Res.s(R.string.wallet_desfazer)
    CryoSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
            Text(Res.s(R.string.cfx_sheet_title), style = MaterialTheme.typography.titleLarge)
            Text(
                Res.s(R.string.cfx_sheet_text), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
            )
            list.forEach { s ->
                val tx = s.tx
                val where = if (tx.cardId != null) l.cardName(tx.cardId) else l.accountName(tx.accountId)
                FlatItem {
                    Text(s.bill.name + " · " + Dates.monthName(s.ym), style = MaterialTheme.typography.titleSmall)
                    Text(
                        listOf(tx.description.ifBlank { s.bill.name }, Dates.short(tx.date), where, if (hide) Money.HIDDEN else Money.format(tx.amount)).joinToString(" · "),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    if (s.duplicateOf != null) {
                        Text(Res.s(R.string.cfx_double_text), style = MaterialTheme.typography.bodySmall, color = CryoTheme.colors.warning)
                    }
                    Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { c.launch { c.repo.unlinkBillPayment(tx.id, never = true) } }) { Text(Res.s(R.string.cfx_no)) }
                        Spacer(Modifier.width(8.dp))
                        val manual = s.duplicateOf
                        FilledTonalButton(onClick = {
                            c.launch {
                                if (manual != null) {
                                    c.repo.mergeBillPayment(tx.id, manual)
                                    c.message(Res.s(R.string.cfx_merged, s.bill.name), undo) { c.repo.unlinkBillPayment(tx.id, never = false); c.repo.restore(listOf(manual)) }
                                } else if (c.repo.linkBillPayment(tx.id, s.bill, s.ym)) {
                                    // RF-CFX-04: o nome que o banco usa passa a valer para esta conta fixa.
                                    if (!BillMatcher.sameName(s.bill.name, tx.description)) c.engineStore.learnBill(tx.description, s.bill.id)
                                    c.message(Res.s(R.string.cfx_linked, s.bill.name), undo) { c.repo.unlinkBillPayment(tx.id, never = false) }
                                }
                            }
                        }) { Text(Res.s(if (manual != null) R.string.cfx_merge else R.string.cfx_yes)) }
                    }
                }
            }
            if (onOther != null) {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = onOther, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text(Res.s(R.string.cfx_other)) }
            }
        }
    }
}

/** Parcela de uma dívida no mês escolhido (vem da área de Dívidas, não é uma conta fixa). */
@Composable
private fun DebtInstallmentRow(m: DebtMath.MonthInstallment, l: Ledger, hide: Boolean, onPay: () -> Unit) {
    val nav = LocalNav.current
    val info = m.info
    val d = info.debt
    val amount = if (m.payable) info.nextAmount else d.installmentAmount
    val (status, statusColor) = when {
        m.paid -> Res.s(R.string.wallet_paga) to CryoTheme.colors.income
        m.due.isBefore(l.today) -> Res.s(R.string.bill_was_due, Dates.relative(m.due, l.today)) to CryoTheme.colors.expense
        ChronoUnit.DAYS.between(l.today, m.due) <= 3 -> Res.s(R.string.bill_due, Dates.relative(m.due, l.today)) to CryoTheme.colors.warning
        else -> Res.s(R.string.plan_vence_dia, m.due.dayOfMonth) to MaterialTheme.colorScheme.onSurfaceVariant
    }
    FlatItem(onClick = { nav.go(Routes.debt(d.id)) }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(debtKindIcon(d.kind), Color(d.color))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(d.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    Res.s(R.string.plan_parcela, if (hide) Money.HIDDEN else Money.format(amount), m.number, info.installmentsTotal),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(status, style = MaterialTheme.typography.bodySmall, color = statusColor)
            }
            Spacer(Modifier.width(8.dp))
            when {
                m.paid -> Row(
                    Modifier.padding(horizontal = 12.dp).semantics { contentDescription = Res.s(R.string.plan_parcela_paga) },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.CheckCircle, null, Modifier.size(20.dp), tint = CryoTheme.colors.income)
                    Spacer(Modifier.width(6.dp))
                    Text(Res.s(R.string.wallet_paga), style = MaterialTheme.typography.labelLarge, color = CryoTheme.colors.income)
                }
                m.payable -> FilledTonalButton(onClick = onPay, contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
                    Text(Res.s(R.string.debt_paguei))
                }
            }
        }
        if (!m.paid && !m.payable) {
            Text(
                Res.s(R.string.plan_antes_desta_falta_pagar, info.installmentsPaid + 1),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

/** RF-FIX-13: um dos três blocos do topo das contas fixas (A pagar, Pagas, Total do mês). */
@Composable
private fun BillStat(label: String, value: Long, hide: Boolean, modifier: Modifier, alert: Boolean = false) {
    val tone = if (alert) CryoTheme.tones.alert else null
    Column(
        modifier.depthCard(shape = MaterialTheme.shapes.large, color = tone?.container ?: CryoTheme.depth.card).padding(horizontal = 12.dp, vertical = 12.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = tone?.onContainer ?: MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(4.dp))
        Text(
            if (hide) Money.HIDDEN else Money.format(value), style = MaterialTheme.typography.titleSmall,
            color = tone?.accent ?: MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun SummaryLine(title: String, sub: String, total: Long, hide: Boolean) {
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(if (hide) Money.HIDDEN else Money.format(total), style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun BillRow(s: BillStatus, l: Ledger, hide: Boolean, onPay: () -> Unit, onUndo: () -> Unit, onSkip: (() -> Unit)? = null) {
    val nav = LocalNav.current
    val b = s.bill
    val cat = b.categoryId?.let { l.category[it] }
    val income = b.kind == CategoryKind.INCOME
    val value = if (hide) Money.HIDDEN else Money.format(s.payment?.amount ?: b.amount)
    val (status, statusColor) = when (s.state) {
        BillState.PAID ->
            if (s.inferred) Res.s(R.string.cfx_found_on, Dates.short(s.payment!!.date)) to MaterialTheme.colorScheme.primary
            else Res.s(if (income) R.string.bill_received_on else R.string.bill_paid_on, Dates.short(s.payment!!.date)) to CryoTheme.colors.income
        BillState.OVERDUE -> Res.s(if (income) R.string.bill_late_since else R.string.bill_was_due, Dates.relative(s.due, l.today)) to CryoTheme.colors.expense
        BillState.DUE_SOON -> Res.s(if (income) R.string.bill_arrives else R.string.bill_due, Dates.relative(s.due, l.today)) to CryoTheme.colors.warning
        BillState.UPCOMING -> Res.s(if (income) R.string.bill_arrives_day else R.string.bill_due_day, s.due.dayOfMonth) to MaterialTheme.colorScheme.onSurfaceVariant
    }
    FlatItem(onClick = { nav.go(Routes.billEdit(b.id, b.kind)) }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (cat != null) IconBadge(categoryIcon(cat.icon), Color(cat.color)) else IconBadge(Icons.Rounded.EventRepeat, MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(b.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    value, style = MaterialTheme.typography.bodyMedium,
                    color = if (income) CryoTheme.colors.income else MaterialTheme.colorScheme.onSurface,
                )
                Text(status, style = MaterialTheme.typography.bodySmall, color = statusColor)
                // RF-CFX-09/10: só quando não é a conta mensal de sempre, para não poluir a lista.
                if (b.every > 1 || s.parts > 1) {
                    val freq = Res.s(
                        when (b.every) {
                            2 -> R.string.bill_every_2; 3 -> R.string.bill_every_3
                            6 -> R.string.bill_every_6; 12 -> R.string.bill_every_12
                            else -> R.string.bill_every_1
                        },
                    )
                    val part = if (s.parts > 1) " · " + Res.s(R.string.bill_part_of, s.part, s.parts) else ""
                    Text(freq + part, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                // RF-FIX-12 (1.6): pular o mês (sem lançamento nenhum), com desfazer.
                if (onSkip != null && s.payment == null) {
                    TextButton(onClick = onSkip, contentPadding = PaddingValues(0.dp)) { Text(Res.s(R.string.bill_skip)) }
                }
            }
            Spacer(Modifier.width(8.dp))
            if (s.payment == null) {
                FilledTonalButton(onClick = onPay, contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
                    Text(if (income) Res.s(R.string.plan_recebi) else Res.s(R.string.debt_paguei))
                }
            } else {
                if (s.inferred) {
                // RF-CFX-07: o Cryo reconheceu o pagamento no Extrato; a pessoa só confirma (ou diz que não é).
                FilledTonalButton(onClick = onUndo, contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) { Text(Res.s(R.string.cfx_confirm)) }
                } else TextButton(
                    onClick = onUndo,
                    modifier = Modifier.semantics { contentDescription = if (income) Res.s(R.string.plan_recebido_toque_para_desmarcar) else Res.s(R.string.plan_paga_toque_para_desmarcar) },
                ) {
                    Icon(Icons.Rounded.CheckCircle, null, Modifier.size(20.dp), tint = CryoTheme.colors.income)
                    Spacer(Modifier.width(6.dp))
                    Text(if (income) Res.s(R.string.plan_recebido) else Res.s(R.string.wallet_paga), color = CryoTheme.colors.income)
                }
            }
        }
    }
}

/**
 * RF-CFX-09..11: o ritmo da conta fixa (todo mês, a cada 2, 3, 6 meses ou uma vez por ano), em quantas parcelas cada
 * ciclo é pago, o preço à vista (para mostrar os juros) e até quando ela vale.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BillCycleFields(
    every: Int,
    onEvery: (Int) -> Unit,
    parts: Int,
    onParts: (Int) -> Unit,
    cash: Long,
    onCash: (Long) -> Unit,
    startKey: Int,
    onStart: (Int) -> Unit,
    endKey: Int,
    onEnd: (Int) -> Unit,
    dueDay: Int,
    amount: Long,
    today: LocalDate,
) {
    val options = listOf(
        1 to R.string.bill_every_1, 2 to R.string.bill_every_2, 3 to R.string.bill_every_3,
        6 to R.string.bill_every_6, 12 to R.string.bill_every_12,
    )
    FieldLabel(Res.s(R.string.bill_every))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (n, label) ->
            FilterChip(selected = every == n, onClick = { onEvery(n) }, label = { Text(Res.s(label)) })
        }
    }
    if (every > 1) {
        // O primeiro vencimento importa quando a conta não é mensal: o IPVA é em janeiro, não "daqui a um mês".
        FieldLabel(Res.s(R.string.bill_first))
        val start = ymOf(startKey)
        MonthSelector(
            Dates.monthYearInSentence(start, today).capFirst(),
            onPrev = { onStart(start.minusMonths(1).key()) },
            onNext = { onStart(start.plusMonths(1).key()) },
        )
        FieldLabel(Res.s(R.string.bill_installments))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            (1..minOf(every, 12)).forEach { n ->
                FilterChip(
                    selected = parts == n, onClick = { onParts(n) },
                    label = { Text(if (n == 1) Res.s(R.string.bill_in_cash) else Res.s(R.string.bill_installments_n, n)) },
                )
            }
        }
    }
    if (every > 1 && parts > 1) {
        FieldLabel(Res.s(R.string.bill_cash_price))
        AmountField(cash, onCash)
    }
    FieldLabel(Res.s(R.string.bill_end))
    Row(verticalAlignment = Alignment.CenterVertically) {
        FilterChip(selected = endKey == 0, onClick = { onEnd(0) }, label = { Text(Res.s(R.string.bill_end_never)) })
        Spacer(Modifier.width(8.dp))
        val end = if (endKey == 0) ymOf(startKey).plusMonths(11) else ymOf(endKey)
        FilterChip(
            selected = endKey != 0, onClick = { onEnd(end.key()) },
            label = { Text(Dates.monthYearInSentence(end, today).capFirst()) },
        )
        if (endKey != 0) {
            IconButton(onClick = { onEnd(maxOf(end.minusMonths(1), ymOf(startKey)).key()) }) {
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, Res.s(R.string.common_mes_anterior))
            }
            IconButton(onClick = { onEnd(end.plusMonths(1).key()) }) {
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, Res.s(R.string.common_proximo_mes))
            }
        }
    }
    // O resumo em uma frase: o que se paga por ciclo e, se houver, os juros do parcelamento.
    val bill = Bill(name = "", amount = amount, dueDay = dueDay, startYm = startKey, every = every, installments = parts, cashAmount = cash, endYm = endKey)
    if (parts > 1 && amount > 0) {
        Text(
            Res.s(R.string.bill_cycle_total, parts, Money.format(amount), Money.format(BillCycle.cycleTotal(bill))),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp),
        )
        if (cash > 0) {
            val extra = BillCycle.interest(bill)
            Text(
                if (extra > 0) Res.s(R.string.bill_interest, Money.format(extra)) else Res.s(R.string.bill_interest_none),
                style = MaterialTheme.typography.bodySmall,
                color = if (extra > 0) CryoTheme.colors.warning else CryoTheme.colors.income,
            )
        }
    }
    if (every > 1) {
        val next = generateSequence(maxOf(ymOf(startKey), today.ym())) { it.plusMonths(1) }
            .take(24).firstOrNull { BillCycle.partOf(bill, it) != null }
        if (next != null) {
            Text(
                Res.s(R.string.bill_next_due, Dates.dayMonthYear(next.dayClamped(dueDay))),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun BillEditorScreen(id: Long?, kindArg: CategoryKind) {
    val l = rememberLedger() ?: return LoadingBox()
    val c = LocalContainer.current
    val hideNow = LocalSettings.current.hideValues
    val nav = LocalNav.current
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val existing = id?.let { l.bill[it] }
    var kind by rememberSaveable { mutableStateOf(existing?.kind ?: kindArg) }
    var name by rememberSaveable { mutableStateOf(existing?.name ?: "") }
    var amount by rememberSaveable { mutableLongStateOf(existing?.amount ?: 0L) }
    var day by rememberSaveable { mutableIntStateOf(existing?.dueDay ?: l.today.dayOfMonth.coerceAtMost(28)) }
    var cat by rememberSaveable { mutableStateOf(existing?.categoryId) }
    var source by remember {
        mutableStateOf<Source?>(existing?.cardId?.let { Source.Card(it) } ?: (existing?.accountId ?: l.activeAccounts.firstOrNull()?.id)?.let { Source.Acc(it) })
    }
    var remind by rememberSaveable { mutableStateOf(existing?.remind ?: (kindArg == CategoryKind.EXPENSE)) }
    var before by rememberSaveable { mutableIntStateOf(existing?.remindDaysBefore ?: 1) }
    var active by rememberSaveable { mutableStateOf(existing?.active ?: true) }
    // RF-CFX-09..11: ritmo, parcelas, preço à vista e prazo.
    var every by rememberSaveable { mutableIntStateOf(existing?.every ?: 1) }
    var parts by rememberSaveable { mutableIntStateOf(existing?.installments ?: 1) }
    var cash by rememberSaveable { mutableLongStateOf(existing?.cashAmount ?: 0L) }
    var startKey by rememberSaveable { mutableIntStateOf(existing?.startYm ?: l.today.ym().key()) }
    var endKey by rememberSaveable { mutableIntStateOf(existing?.endYm ?: 0) }
    var alreadyPaid by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    EditorScaffold(
        title = if (existing == null) (if (kind == CategoryKind.EXPENSE) Res.s(R.string.plan_nova_conta_fixa) else Res.s(R.string.plan_novo_recebimento_fixo)) else Res.s(R.string.plan_editar),
        canSave = name.isNotBlank() && amount > 0,
        onSave = {
            val b = (existing ?: Bill(name = name, amount = amount, dueDay = day, startYm = startKey)).copy(
                name = name.trim(), amount = amount, dueDay = day, kind = kind, categoryId = cat,
                accountId = (source as? Source.Acc)?.id, cardId = if (kind == CategoryKind.EXPENSE) (source as? Source.Card)?.id else null,
                remind = remind, remindDaysBefore = before, active = active,
                every = every, installments = parts.coerceIn(1, every), cashAmount = if (parts > 1) cash else 0,
                startYm = startKey, endYm = endKey,
            )
            if (remind && Build.VERSION.SDK_INT >= 33 && !Reminders.canNotify(ctx)) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
            val payNow = alreadyPaid
            val payYm = ymOf(startKey)
            val payCard = b.cardId?.let { l.card[it] }
            c.launch {
                val saved = c.repo.saveBill(b)
                // RF-FIX-10: "já paguei o deste ciclo" liga ao pagamento que já existe; só lança um novo se não houver.
                if (payNow) {
                    c.repo.markBillPaidLinking(b.copy(id = saved), payYm, amount, payYm.dayClamped(day), (source as? Source.Acc)?.id, payCard)
                }
                c.billReview.value = if (b.id != 0L) b.id else saved
                // RF-ORC-09 (1.6): as contas fixas da categoria não cabem no orçamento: a mensagem oferece ajustar.
                val sg = b.categoryId?.let { cat ->
                    BudgetBills.suggestion(Ledger(c.repo.loadOnce(), l.today), cat, l.today.ym())
                        ?.takeIf { c.budgetBills.dismissed()[cat] != it.signature }
                }
                if (sg != null) {
                    c.message(budgetBillText(l, sg, hideNow), budgetBillAction(sg, hideNow)) { applyBudgetBills(c, sg) }
                } else {
                    c.message(Res.s(R.string.plan_salvo))
                }
            }
            nav.back()
        },
        onDelete = existing?.let { { confirmDelete = true } },
    ) {
        Segmented(
            listOf(CategoryKind.EXPENSE to Res.s(R.string.plan_pagar), CategoryKind.INCOME to Res.s(R.string.plan_receber)), kind,
            { kind = it; cat = null; if (it == CategoryKind.INCOME && source is Source.Card) source = l.activeAccounts.firstOrNull()?.let { a -> Source.Acc(a.id) } },
        )
        Spacer(Modifier.height(12.dp))
        // RF-CFX-12: modelos, só ao criar uma conta a pagar. Preenchem o nome, o ritmo e o dia; o valor é de cada um.
        if (existing == null && kind == CategoryKind.EXPENSE) {
            FieldLabel(Res.s(R.string.bill_templates))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BillTemplates.all.forEach { t ->
                    val label = BillTemplates.label(t)
                    AssistChip(
                        onClick = {
                            name = label; every = t.every; day = t.dueDay; parts = 1
                            startKey = BillTemplates.firstDue(t, l.today).key()
                        },
                        label = { Text(label) },
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        }
        OutlinedTextField(
            name, { name = it }, label = { Text(Res.s(R.string.wallet_nome)) }, singleLine = true,
            placeholder = { Text(if (kind == CategoryKind.EXPENSE) Res.s(R.string.plan_ex_aluguel_internet_netflix) else Res.s(R.string.plan_ex_salario)) },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences), modifier = Modifier.fillMaxWidth(),
        )
        // O rótulo muda com o ritmo: "todo mês" só vale para a conta mensal; na anual em 3x é "o valor de cada parcela".
        FieldLabel(
            when {
                every == 1 -> Res.s(R.string.plan_valor_todo_mes_aproximado)
                parts > 1 -> Res.s(R.string.bill_amount_part)
                else -> Res.s(R.string.bill_amount_each)
            },
        )
        AmountField(amount, { amount = it }, color = if (kind == CategoryKind.INCOME) CryoTheme.colors.income else MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(8.dp))
        Stepper(
            day, { day = it }, 1..31,
            when {
                every > 1 -> Res.s(if (kind == CategoryKind.EXPENSE) R.string.bill_due_on else R.string.bill_in_on)
                kind == CategoryKind.EXPENSE -> Res.s(R.string.debt_vence_todo_dia)
                else -> Res.s(R.string.plan_cai_todo_dia)
            },
        )
        BillCycleFields(
            every, { every = it; if (parts > it) parts = it }, parts, { parts = it }, cash, { cash = it },
            startKey, { startKey = it }, endKey, { endKey = it }, day, amount, l.today,
        )
        // RF-FIX-10: só faz sentido ao criar uma conta cujo primeiro vencimento já passou (ou é deste mês).
        if (existing == null && ymOf(startKey) <= l.today.ym()) {
            Spacer(Modifier.height(8.dp))
            SwitchRow(
                Res.s(R.string.bill_already_paid, Dates.monthYearInSentence(ymOf(startKey), l.today)),
                alreadyPaid, { alreadyPaid = it }, Res.s(R.string.bill_already_paid_hint),
            )
        }
        FieldLabel(Res.s(R.string.plan_categoria))
        CategoryGrid(l.categories(kind), cat, { cat = it.id })
        FieldLabel(if (kind == CategoryKind.EXPENSE) Res.s(R.string.debt_paga_com) else Res.s(R.string.wallet_entra_conta))
        SourceChips(l.activeAccounts, if (kind == CategoryKind.EXPENSE) l.activeCards else emptyList(), source, { source = it })
        if (kind == CategoryKind.EXPENSE) {
            Spacer(Modifier.height(8.dp))
            SwitchRow(Res.s(R.string.debt_lembrar_vencimento), remind, { remind = it }, Res.s(R.string.debt_notificacao_celular))
            if (remind) {
                Stepper(before, { before = it }, 0..7, Res.s(R.string.debt_avisar_antes), format = { if (it == 0) Res.s(R.string.debt_dia) else Res.p(R.plurals.count_days, it, it) })
            }
        }
        if (existing != null) SwitchRow(Res.s(R.string.plan_ativa), active, { active = it }, Res.s(R.string.plan_desligue_quando_conta_deixar))
    }
    if (confirmDelete && existing != null) {
        ConfirmDialog(
            Res.s(R.string.plan_apagar, existing.name), Res.s(R.string.plan_pagamentos_ja_registrados_continuam), Res.s(R.string.wallet_apagar),
            onConfirm = { confirmDelete = false; c.launch { c.repo.deleteBill(existing); c.message(Res.s(R.string.plan_conta_fixa_apagada)) }; nav.back() },
            onDismiss = { confirmDelete = false },
        )
    }
}

/* ================================= Metas ================================= */

/**
 * RF-MET-07 (reforma visual da 1.6, LA12): o total guardado no topo (com a previsão das metas, RF-MOT-16) e cada meta num
 * bloco, em duas colunas; tocar abre a folha da meta com Guardar, Retirar, Editar e Arquivar.
 */
@Composable
private fun GoalsTab(l: Ledger) {
    val nav = LocalNav.current
    val hide = LocalSettings.current.hideValues
    val archived = l.s.goals.filter { it.archived }
    val archivedSaved = archived.sumOf { l.goalSaved(it.id) }
    var action by remember { mutableStateOf<Pair<Goal, Boolean>?>(null) } // meta, guardar?
    var opened by remember { mutableStateOf<Goal?>(null) }
    val engine = rememberEngine()
    val forecast = engine?.goalForecast
    val goalLayout = LocalSettings.current.layout(io.github.h3yk0.cryo.domain.Layouts.GOALS)
    var organizingGoals by rememberSaveable { mutableStateOf(false) }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (l.activeGoals.isEmpty()) {
            item {
                if (archived.isEmpty()) {
                    EmptyState(
                        Icons.Rounded.Flag, Res.s(R.string.plan_crie_sua_primeira_meta),
                        Res.s(R.string.plan_viagem_reserva_emergencia_celular),
                    )
                } else {
                    EmptyState(
                        Icons.Rounded.Flag, Res.s(R.string.plan_nenhuma_meta_ativa),
                        Res.s(R.string.plan_suas_metas_arquivadas_continuam),
                    )
                }
            }
        } else {
          // RF-ORG (1.7, LA8): o total e as metas são blocos que a pessoa arruma (com as arquivadas, no fim).
          arranged(goalLayout, listOfNotNull(
            Block("total", Res.s(R.string.org_block_goals_total)) {
            item(key = "total") {
                FlatItem {
                    Text(Res.s(R.string.plan_guardado_metas), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    MoneyText(l.activeGoals.sumOf { l.goalSaved(it.id) }, style = MaterialTheme.typography.headlineSmall)
                    if (archivedSaved > 0) {
                        Text(
                            Res.s(R.string.plan_mais_metas_arquivadas, if (hide) Money.HIDDEN else Money.format(archivedSaved)),
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    // RF-MOT-16: a previsão das metas (antes em Análises) mora aqui; a explicação, fechada com setinha.
                    if (forecast != null && forecast.neededMonthly > 0) {
                        Text(
                            Res.s(R.string.gf_top, if (hide) Money.HIDDEN else Money.format(forecast.neededMonthly), if (hide) Money.HIDDEN else Money.format(forecast.surplusMonthly.coerceAtLeast(0))),
                            style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp),
                        )
                        Expandable(Res.s(R.string.insights_como_cheguei_nesse_numero)) {
                            Text(Res.s(R.string.gf_how), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            },
            Block("metas", Res.s(R.string.home_goals_short)) {
            l.activeGoals.chunked(2).forEachIndexed { i, row ->
                item(key = "metas$i-" + row.joinToString("-") { it.id.toString() }) {
                    TileRow(
                        first = { m -> GoalTile(row[0], l, hide, forecast?.items?.firstOrNull { it.goal.id == row[0].id }, m) { opened = row[0] } },
                        second = row.getOrNull(1)?.let { g -> { m -> GoalTile(g, l, hide, forecast?.items?.firstOrNull { it.goal.id == g.id }, m) { opened = g } } },
                    )
                }
            }
            },
            if (archived.isEmpty()) null else Block("arquivadas", Res.s(R.string.org_block_goals_archived)) {
                item(key = "arquivadas") {
                    TileButton(Res.s(R.string.plan_metas_arquivadas_2, archived.size), Icons.Rounded.Inventory2) { nav.go(Routes.GOALS_ARCHIVE) }
                }
            },
          ))
        }
        item(key = "nova") {
            CryoButton(onClick = { nav.go(Routes.goalEdit()) }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Icon(Icons.Rounded.Add, null); Spacer(Modifier.width(8.dp)); Text(Res.s(R.string.plan_nova_meta))
            }
        }
        // Sem metas ativas, as arquivadas continuam à vista (não há o que arrumar).
        if (l.activeGoals.isEmpty() && archived.isNotEmpty()) {
            item(key = "arquivadas") {
                TileButton(Res.s(R.string.plan_metas_arquivadas_2, archived.size), Icons.Rounded.Inventory2) { nav.go(Routes.GOALS_ARCHIVE) }
            }
        }
        if (l.activeGoals.isNotEmpty()) item(key = "organizar") { OrganizeButton { organizingGoals = true } }
    }
    if (organizingGoals) {
        ArrangeSheet(listOf(ArrangeGroup(io.github.h3yk0.cryo.domain.Layouts.GOALS, null, listOf(
            "total" to Res.s(R.string.org_block_goals_total),
            "metas" to Res.s(R.string.home_goals_short),
            "arquivadas" to Res.s(R.string.org_block_goals_archived),
        ))), onDismiss = { organizingGoals = false })
    }
    opened?.let { g ->
        GoalSheet(
            g, l, hide, forecast?.items?.firstOrNull { it.goal.id == g.id }, engine?.goals?.get(g.id),
            onDismiss = { opened = null },
            onMoney = { deposit -> opened = null; action = g to deposit },
            onEdit = { opened = null; nav.go(Routes.goalEdit(g.id)) },
        )
    }
    action?.let { (g, deposit) -> GoalMoneySheet(g, deposit, l, onDismiss = { action = null }) }
}

/** RF-MET-07: o bloco de uma meta: a ilustração, o anel do progresso, o nome, o guardado e a frase curta da previsão. */
@Composable
private fun GoalTile(g: Goal, l: Ledger, hide: Boolean, forecast: io.github.h3yk0.cryo.domain.engine.GoalForecastItem?, modifier: Modifier, onClick: () -> Unit) {
    val saved = l.goalSaved(g.id)
    val ratio = saved.toFloat() / g.target.coerceAtLeast(1)
    val line = when {
        saved >= g.target -> Res.s(R.string.plan_meta_alcancada)
        forecast != null -> goalForecastText(forecast)
        else -> Res.s(R.string.goal_tile_of, if (hide) Money.HIDDEN else Money.format(g.target))
    }
    val late = forecast != null && saved < g.target &&
        (forecast.fit == io.github.h3yk0.cryo.domain.engine.GoalFit.LATE || forecast.fit == io.github.h3yk0.cryo.domain.engine.GoalFit.NO_ROOM)
    CryoTile(
        g.name, modifier, subtitle = line, onClick = onClick,
        tone = if (late) CryoTheme.tones.attention else null,
        leading = { io.github.h3yk0.cryo.ui.components.GoalBadge(g, 40.dp) },
        trailing = { ProgressRing(ratio, Color(g.color)) },
        valueContent = { MoneyText(saved, style = MaterialTheme.typography.titleLarge) },
    )
}

/** RF-MET-07: a folha da meta: o progresso, quanto guardar por mês, a previsão e as quatro ações. */
@Composable
private fun GoalSheet(
    g: Goal, l: Ledger, hide: Boolean,
    forecast: io.github.h3yk0.cryo.domain.engine.GoalForecastItem?, outlook: GoalOutlook?,
    onDismiss: () -> Unit, onMoney: (deposit: Boolean) -> Unit, onEdit: () -> Unit,
) {
    val c = LocalContainer.current
    val saved = l.goalSaved(g.id)
    CryoSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                io.github.h3yk0.cryo.ui.components.GoalBadge(g, 48.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(g.name, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(
                        Res.s(R.string.goal_sheet_progress, if (hide) Money.HIDDEN else Money.format(saved), if (hide) Money.HIDDEN else Money.format(g.target)),
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                ProgressRing(saved.toFloat() / g.target.coerceAtLeast(1), Color(g.color), size = 52.dp)
            }
            Spacer(Modifier.height(12.dp))
            GoalDetails(g, l, hide, forecast, outlook)
            Spacer(Modifier.height(16.dp))
            CryoButton(onClick = { onMoney(true) }, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text(Res.s(R.string.plan_guardar)) }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { onMoney(false) }, enabled = saved > 0, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text(Res.s(R.string.plan_retirar)) }
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = onEdit) {
                    Icon(Icons.Rounded.Edit, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text(Res.s(R.string.plan_editar))
                }
                TextButton(onClick = { onDismiss(); archiveGoal(c, g) }) {
                    Icon(Icons.Rounded.Archive, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text(Res.s(R.string.wallet_arquivar))
                }
            }
        }
    }
}

private fun archiveGoal(c: io.github.h3yk0.cryo.AppContainer, g: Goal) {
    c.launch {
        c.repo.saveGoal(g.copy(archived = true))
        c.message(Res.s(R.string.plan_meta_arquivada_ela_fica), Res.s(R.string.wallet_desfazer)) { c.repo.saveGoal(g.copy(archived = false)) }
    }
}

/** Cartão de uma meta, com barra de progresso e quanto guardar por mês. */
@Composable
private fun GoalCard(
    g: Goal,
    l: Ledger,
    hide: Boolean,
    onClick: () -> Unit,
    faded: Boolean = false,
    outlook: GoalOutlook? = null,
    /** RF-MOT-16 (1.6): a resposta da previsão de metas (no lugar do ritmo dos depósitos). */
    forecast: io.github.h3yk0.cryo.domain.engine.GoalForecastItem? = null,
    actions: @Composable androidx.compose.foundation.layout.FlowRowScope.(saved: Long) -> Unit,
) {
    val saved = l.goalSaved(g.id)
    val ratio = saved.toFloat() / g.target.coerceAtLeast(1)
    FlatItem(onClick = onClick, faded = faded) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            io.github.h3yk0.cryo.ui.components.GoalBadge(g, 48.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(g.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    Res.s(R.string.goal_progress, if (hide) Money.HIDDEN else Money.format(saved), if (hide) Money.HIDDEN else Money.format(g.target)),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text("${(ratio * 100).toInt().coerceAtLeast(0)}%", style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(10.dp))
        UsageBar(ratio, baseColor = Color(g.color), height = 12.dp, warn = false)
        GoalDetails(g, l, hide, forecast, outlook)
        FlowRow(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) { actions(saved) }
    }
}

/** Quanto guardar por mês, a previsão (RF-MOT-16) ou o ritmo dos depósitos (RF-MOT-10): no cartão das arquivadas e na folha da meta. */
@Composable
private fun GoalDetails(g: Goal, l: Ledger, hide: Boolean, forecast: io.github.h3yk0.cryo.domain.engine.GoalForecastItem?, outlook: GoalOutlook?) {
    val saved = l.goalSaved(g.id)
    val need = l.goalMonthlyNeeded(g)
    val info = when {
        saved >= g.target -> Res.s(R.string.plan_meta_alcancada)
        g.archived && saved <= 0 -> Res.s(R.string.plan_arquivada_sem_dinheiro_guardado)
        g.archived -> Res.s(R.string.debt_arquivada_faltavam, if (hide) Money.HIDDEN else Money.format(max(0, g.target - saved)))
        need != null && g.deadline != null -> {
            val months = ChronoUnit.MONTHS.between(l.today.ym(), g.deadline.ym()) + 1
            Res.s(
                R.string.goal_save_per_month, if (hide) Money.HIDDEN else Money.format(need),
                Dates.monthYearInSentence(g.deadline.ym(), l.today), Res.p(R.plurals.count_months, months.toInt(), months.toInt()),
            )
        }
        else -> Res.s(R.string.plan_faltam, if (hide) Money.HIDDEN else Money.format(max(0, g.target - saved)))
    }
    Text(info, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp))
    // RF-MOT-16 (1.6): a resposta da previsão de metas (a sobra prevista do ano, pelo prazo mais perto).
    if (forecast != null && !g.archived && forecast.fit != io.github.h3yk0.cryo.domain.engine.GoalFit.DONE) {
        Text(
            goalForecastText(forecast), style = MaterialTheme.typography.bodySmall,
            color = when (forecast.fit) {
                io.github.h3yk0.cryo.domain.engine.GoalFit.LATE -> CryoTheme.colors.warning
                io.github.h3yk0.cryo.domain.engine.GoalFit.NO_ROOM -> CryoTheme.colors.expense
                else -> MaterialTheme.colorScheme.primary
            },
            modifier = Modifier.padding(top = 2.dp),
        )
        if (forecast.shortMonthly > 0) {
            Text(
                Res.s(R.string.gf_short, if (hide) Money.HIDDEN else Money.format(forecast.shortMonthly)),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    // RF-MOT-10: sem a previsão (motor ainda calculando), o ritmo dos depósitos.
    val eta = outlook?.etaYm
    if (forecast == null && outlook != null && !g.archived && !outlook.done && eta != null && outlook.pace > 0) {
        val chance = outlook.chanceByDeadline?.let { " · " + Res.s(R.string.engine_goal_chance, Num.percent((it * 100).toInt())) }.orEmpty()
        Text(
            Res.s(R.string.engine_goal_eta, if (hide) Money.HIDDEN else Money.format(outlook.pace), Dates.monthYearInSentence(eta, l.today)) + chance,
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 2.dp),
        )
    }
}

/** Guardar dinheiro numa meta ou retirar dela. */
@Composable
private fun GoalMoneySheet(g: Goal, deposit: Boolean, l: Ledger, onDismiss: () -> Unit) {
    val c = LocalContainer.current
    MoneyActionSheet(
        title = if (deposit) Res.s(R.string.plan_guardar_2, g.name) else Res.s(R.string.plan_retirar_2, g.name),
        subtitle = if (deposit) Res.s(R.string.plan_dinheiro_sai_conta_escolhida) else Res.s(R.string.wallet_dinheiro_volta_para_conta),
        initialAmount = when {
            deposit -> l.goalMonthlyNeeded(g) ?: 0
            g.archived -> l.goalSaved(g.id).coerceAtLeast(0)
            else -> 0
        },
        l = l,
        accountLabel = if (deposit) Res.s(R.string.wallet_sai_conta) else Res.s(R.string.plan_volta_para_conta),
        initialAccount = LocalSettings.current.defaultAccountId, confirmLabel = if (deposit) Res.s(R.string.plan_guardar) else Res.s(R.string.plan_retirar),
        onDismiss = onDismiss,
        onConfirm = { amount, acc, date ->
            onDismiss()
            c.launch {
                c.repo.saveTx(
                    Tx(
                        type = if (deposit) TxType.GOAL_IN else TxType.GOAL_OUT, amount = amount, date = date,
                        description = if (deposit) Res.s(R.string.plan_guardado, g.name) else Res.s(R.string.plan_retirado, g.name), accountId = acc, goalId = g.id,
                    ),
                )
                c.message(if (deposit) Res.s(R.string.plan_guardados, Money.format(amount), g.name) else Res.s(R.string.plan_retirados, Money.format(amount), g.name))
            }
        },
    )
}

/* ============================ Metas arquivadas ============================ */

@Composable
fun GoalsArchiveScreen() {
    val l = rememberLedger() ?: return LoadingBox()
    val nav = LocalNav.current
    val c = LocalContainer.current
    val hide = LocalSettings.current.hideValues
    val archived = l.s.goals.filter { it.archived }.sortedBy { it.name.lowercase() }
    val stillSaved = archived.sumOf { l.goalSaved(it.id).coerceAtLeast(0) }
    var withdrawing by remember { mutableStateOf<Goal?>(null) }

    Scaffold(topBar = { CryoTopBar(Res.s(R.string.plan_metas_arquivadas), onBack = nav::back) }, contentWindowInsets = WindowInsets(0, 0, 0, 0)) { pad ->
        LazyColumn(
            Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (archived.isEmpty()) {
                item {
                    EmptyState(
                        Icons.Rounded.Inventory2, Res.s(R.string.plan_nenhuma_meta_arquivada),
                        Res.s(R.string.plan_quando_voce_arquivar_meta),
                    )
                }
            } else {
                item {
                    Text(
                        if (stillSaved > 0) {
                            Res.s(R.string.plan_ainda_ha_guardados_nestas, if (hide) Money.HIDDEN else Money.format(stillSaved)) +
                                Res.s(R.string.plan_esse_dinheiro_continua_seu)
                        } else {
                            Res.s(R.string.plan_metas_arquivadas_nao_aparecem)
                        },
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                    )
                }
            }
            items(archived, key = { it.id }) { g ->
                GoalCard(g, l, hide, faded = true, onClick = { nav.go(Routes.goalEdit(g.id)) }) { saved ->
                    FilledTonalButton(onClick = {
                        c.launch {
                            c.repo.saveGoal(g.copy(archived = false))
                            c.message(Res.s(R.string.plan_voltou_para_metas, g.name), Res.s(R.string.wallet_desfazer)) { c.repo.saveGoal(g.copy(archived = true)) }
                        }
                    }) { Icon(Icons.Rounded.Unarchive, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text(Res.s(R.string.wallet_restaurar)) }
                    if (saved > 0) OutlinedButton(onClick = { withdrawing = g }) { Text(Res.s(R.string.plan_retirar)) }
                }
            }
        }
    }
    withdrawing?.let { g -> GoalMoneySheet(g, deposit = false, l = l, onDismiss = { withdrawing = null }) }
}

@Composable
fun GoalEditorScreen(id: Long?) {
    val l = rememberLedger() ?: return LoadingBox()
    val c = LocalContainer.current
    val nav = LocalNav.current
    val existing = id?.let { l.goal[it] }
    var name by rememberSaveable { mutableStateOf(existing?.name ?: "") }
    var art by rememberSaveable { mutableStateOf((existing?.let { io.github.h3yk0.cryo.domain.GoalArt.forGoal(it) } ?: io.github.h3yk0.cryo.domain.GoalArt.TARGET).key) }
    var target by rememberSaveable { mutableLongStateOf(existing?.target ?: 0L) }
    var deadline by rememberSaveable { mutableStateOf(existing?.deadline?.toEpochDay()) }
    var color by rememberSaveable { mutableIntStateOf(existing?.color ?: Palette.pick(l.s.goals.size + 8)) }
    var initial by rememberSaveable { mutableLongStateOf(0L) }
    var pickDate by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    EditorScaffold(
        title = when {
            existing == null -> Res.s(R.string.plan_nova_meta)
            existing.archived -> Res.s(R.string.plan_meta_arquivada)
            else -> Res.s(R.string.plan_editar_meta)
        },
        canSave = name.isNotBlank() && target > 0,
        onSave = {
            val g = (existing ?: Goal(name = name, target = target, color = color)).copy(
                name = name.trim(), art = art, target = target, color = color,
                deadline = deadline?.let { java.time.LocalDate.ofEpochDay(it) },
            )
            c.launch {
                val newId = c.repo.saveGoal(g)
                if (existing == null && initial > 0) {
                    c.repo.saveTx(Tx(type = TxType.GOAL_IN, amount = initial, date = l.today, description = Res.s(R.string.wallet_saldo_inicial), goalId = newId))
                }
            }
            nav.back()
        },
        onDelete = existing?.let { { confirmDelete = true } },
    ) {
        if (existing?.archived == true) {
            io.github.h3yk0.cryo.ui.components.Banner(
                Icons.Rounded.Inventory2, Res.s(R.string.plan_esta_meta_esta_arquivada),
                MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer,
                Modifier.padding(bottom = 12.dp),
            )
        }
        OutlinedTextField(
            name, { name = it }, label = { Text(Res.s(R.string.plan_nome_meta)) }, singleLine = true, placeholder = { Text(Res.s(R.string.plan_ex_viagem_reserva_emergencia)) },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences), modifier = Modifier.fillMaxWidth(),
        )
        FieldLabel(Res.s(R.string.plan_icone))
        io.github.h3yk0.cryo.ui.components.GoalArtChooser(io.github.h3yk0.cryo.domain.GoalArt.of(art), Color(color)) { art = it.key }
        FieldLabel(Res.s(R.string.plan_quanto_quer_juntar))
        AmountField(target, { target = it }, label = Res.s(R.string.plan_valor_meta))
        FieldLabel(Res.s(R.string.plan_prazo_opcional))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                deadline?.let { Dates.full(java.time.LocalDate.ofEpochDay(it)) } ?: Res.s(R.string.debt_sem_prazo),
                style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f).clickable { pickDate = true },
            )
            TextButton(onClick = { pickDate = true }) { Text(if (deadline == null) Res.s(R.string.debt_definir) else Res.s(R.string.debt_mudar)) }
            if (deadline != null) TextButton(onClick = { deadline = null }) { Text(Res.s(R.string.debt_tirar)) }
        }
        if (existing == null) {
            FieldLabel(Res.s(R.string.plan_ja_tem_algo_guardado))
            AmountField(initial, { initial = it }, label = Res.s(R.string.plan_ja_guardado))
        }
        FieldLabel(Res.s(R.string.wallet_cor))
        ColorChooser(color) { color = it }
        if (existing != null) {
            Spacer(Modifier.height(20.dp))
            if (existing.archived) {
                OutlinedButton(
                    onClick = {
                        c.launch { c.repo.saveGoal(existing.copy(archived = false)); c.message(Res.s(R.string.plan_voltou_para_metas, existing.name)) }
                        nav.back()
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) { Icon(Icons.Rounded.Unarchive, null); Spacer(Modifier.width(8.dp)); Text(Res.s(R.string.plan_restaurar_meta)) }
            } else {
                OutlinedButton(
                    onClick = { archiveGoal(c, existing); nav.back() },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) { Icon(Icons.Rounded.Archive, null); Spacer(Modifier.width(8.dp)); Text(Res.s(R.string.plan_arquivar_meta)) }
                Text(
                    Res.s(R.string.plan_para_metas_concluidas_ou),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
    if (pickDate) {
        DatePickerDialogField(
            deadline?.let { java.time.LocalDate.ofEpochDay(it) } ?: l.today.plusMonths(6),
            onDismiss = { pickDate = false }, onPick = { deadline = it.toEpochDay(); pickDate = false },
        )
    }
    if (confirmDelete && existing != null) {
        ConfirmDialog(
            Res.s(R.string.plan_apagar_meta), Res.s(R.string.plan_dinheiro_guardado_nela_volta), Res.s(R.string.wallet_apagar),
            onConfirm = { confirmDelete = false; c.launch { c.repo.deleteGoal(existing); c.message(Res.s(R.string.plan_meta_apagada)) }; nav.back() },
            onDismiss = { confirmDelete = false },
        )
    }
}
