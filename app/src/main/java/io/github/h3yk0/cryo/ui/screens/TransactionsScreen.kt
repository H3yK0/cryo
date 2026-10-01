// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.h3yk0.cryo.data.db.Tx
import io.github.h3yk0.cryo.data.db.TxType
import io.github.h3yk0.cryo.domain.Dates
import io.github.h3yk0.cryo.domain.Ledger
import io.github.h3yk0.cryo.domain.key
import io.github.h3yk0.cryo.domain.normalized
import io.github.h3yk0.cryo.domain.ym
import io.github.h3yk0.cryo.domain.ymOf
import io.github.h3yk0.cryo.ui.LocalNav
import io.github.h3yk0.cryo.ui.Routes
import io.github.h3yk0.cryo.ui.components.CryoCard
import io.github.h3yk0.cryo.ui.components.CryoTopBar
import io.github.h3yk0.cryo.ui.components.EmptyState
import io.github.h3yk0.cryo.ui.components.HeatmapCalendar
import io.github.h3yk0.cryo.ui.components.MoneyText
import io.github.h3yk0.cryo.ui.components.MonthSelector
import io.github.h3yk0.cryo.ui.components.Segmented
import io.github.h3yk0.cryo.ui.components.TxRow
import io.github.h3yk0.cryo.ui.components.rememberLedger
import io.github.h3yk0.cryo.ui.components.txSignedAmount
import io.github.h3yk0.cryo.ui.theme.CryoTheme
import io.github.h3yk0.cryo.ui.tx.LoadingBox
import java.time.LocalDate

private enum class TxFilter(val label: String) { ALL("Tudo"), OUT("Saídas"), IN("Entradas"), OTHER("Transferências e outros") }

@Composable
fun TransactionsScreen(categoryFilter: Long?) {
    val l = rememberLedger() ?: return LoadingBox()
    val nav = LocalNav.current
    var ymKey by rememberSaveable { mutableStateOf(l.today.ym().key()) }
    val ym = ymOf(ymKey)
    var mode by rememberSaveable { mutableStateOf(0) }
    var filter by rememberSaveable { mutableStateOf(TxFilter.ALL) }
    var searching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable(categoryFilter) { mutableStateOf(categoryFilter) }
    var selectedDay by rememberSaveable(ymKey) { mutableStateOf(if (ym == l.today.ym()) l.today.dayOfMonth else null as Int?) }

    val txs = remember(l, ymKey, filter, query, category) {
        val q = query.trim().normalized()
        val base = if (q.isNotEmpty()) l.s.txs else l.monthTxs(ym)
        base.filter { t ->
            val okType = when (filter) {
                TxFilter.ALL -> true
                TxFilter.OUT -> t.type == TxType.EXPENSE
                TxFilter.IN -> t.type == TxType.INCOME
                TxFilter.OTHER -> t.type != TxType.EXPENSE && t.type != TxType.INCOME
            }
            val okCat = category == null || t.categoryId == category
            val okQ = q.isEmpty() || t.description.normalized().contains(q) ||
                (t.categoryId?.let { l.category[it]?.name?.normalized()?.contains(q) } == true)
            okType && okCat && okQ
        }
    }
    val summary = remember(l, ymKey) { l.monthSummary(ym) }

    Scaffold(
        topBar = {
            CryoTopBar(
                title = "Extrato",
                actions = {
                    IconButton(onClick = { searching = !searching; if (!searching) query = "" }) {
                        Icon(if (searching) Icons.Rounded.Close else Icons.Rounded.Search, if (searching) "Fechar busca" else "Buscar")
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { nav.go(Routes.txNew()) },
                icon = { Icon(Icons.Rounded.Add, null) }, text = { Text("Lançar") },
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { pad ->
        LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(bottom = 96.dp)) {
            if (searching) {
                item {
                    OutlinedTextField(
                        value = query, onValueChange = { query = it }, singleLine = true,
                        label = { Text("Buscar em todos os meses") },
                        leadingIcon = { Icon(Icons.Rounded.Search, null) },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }
            }
            if (query.isBlank()) {
                item {
                    MonthSelector(
                        Dates.monthYear(ym, l.today),
                        onPrev = { ymKey = ym.minusMonths(1).key() },
                        onNext = { ymKey = ym.plusMonths(1).key() },
                        modifier = Modifier.padding(horizontal = 8.dp),
                    )
                }
                item {
                    CryoCard(Modifier.padding(horizontal = 16.dp)) {
                        Row(Modifier.fillMaxWidth()) {
                            SummaryCell("Entrou", summary.income, CryoTheme.colors.income, Modifier.weight(1f))
                            SummaryCell("Saiu", summary.expense, CryoTheme.colors.expense, Modifier.weight(1f))
                            SummaryCell(
                                "Resultado", summary.result,
                                if (summary.result >= 0) CryoTheme.colors.income else CryoTheme.colors.expense, Modifier.weight(1f), signed = true,
                            )
                        }
                    }
                }
                item {
                    Segmented(
                        listOf(0 to "Lista", 1 to "Calendário"), mode, { mode = it },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                }
            }

            if (mode == 1 && query.isBlank()) {
                item {
                    CryoCard(Modifier.padding(horizontal = 16.dp)) {
                        Text("Calendário de gastos", style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
                        Text(
                            "Quanto mais forte a cor, mais você gastou no dia. Toque em um dia para ver os detalhes.",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(12.dp))
                        HeatmapCalendar(ym, l.dailyExpenses(ym), l.today, selectedDay, onSelect = { selectedDay = it })
                    }
                }
                val day = selectedDay
                if (day != null && day <= ym.lengthOfMonth()) {
                    val date = ym.atDay(day)
                    val list = l.monthTxs(ym).filter { it.date == date }
                    item { DayHeader(date, list, l) }
                    if (list.isEmpty()) {
                        item {
                            Text(
                                "Nenhuma movimentação neste dia.", style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                            )
                        }
                    }
                    items(list, key = { it.id }) { t -> TxRow(t, l, onClick = { nav.go(Routes.txEdit(t.id)) }) }
                }
            } else {
                item {
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        category?.let { id ->
                            InputChip(
                                selected = true, onClick = { category = null },
                                label = { Text(l.category[id]?.name ?: "Categoria") },
                                trailingIcon = { Icon(Icons.Rounded.Close, "Remover filtro de categoria") },
                            )
                        }
                        TxFilter.entries.forEach { f ->
                            FilterChip(selected = filter == f, onClick = { filter = f }, label = { Text(f.label) })
                        }
                    }
                }
                if (txs.isEmpty()) {
                    item {
                        EmptyState(
                            Icons.AutoMirrored.Rounded.ReceiptLong,
                            if (query.isNotBlank()) "Nada encontrado" else "Nenhuma movimentação",
                            if (query.isNotBlank()) "Tente outra palavra." else "Use o botão Lançar ou o registro rápido na tela inicial.",
                        )
                    }
                } else {
                    val groups = txs.groupBy { it.date }.toSortedMap(compareByDescending { it })
                    groups.forEach { (date, list) ->
                        item(key = "h$date") { DayHeader(date, list, l) }
                        items(list, key = { it.id }) { t -> TxRow(t, l, onClick = { nav.go(Routes.txEdit(t.id)) }) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryCell(label: String, value: Long, color: androidx.compose.ui.graphics.Color, modifier: Modifier, signed: Boolean = false) {
    Column(modifier, horizontalAlignment = Alignment.Start) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        MoneyText(value, style = MaterialTheme.typography.titleSmall, color = color, signed = signed)
    }
}

@Composable
private fun DayHeader(date: LocalDate, list: List<Tx>, l: Ledger) {
    val net = list.sumOf { txSignedAmount(it) }
    Row(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(start = 20.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            Dates.dayHeader(date, l.today), style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f).semantics { heading() },
        )
        if (net != 0L) {
            MoneyText(net, signed = true, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
