// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.ui.screens

import androidx.compose.material.icons.rounded.EventRepeat
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import io.github.h3yk0.cryo.ui.components.LocalContainer
import io.github.h3yk0.cryo.ui.components.LocalSettings
import io.github.h3yk0.cryo.domain.Money
import io.github.h3yk0.cryo.R
import io.github.h3yk0.cryo.domain.Res
import io.github.h3yk0.cryo.domain.AccountStatement
import io.github.h3yk0.cryo.domain.TxSource
import io.github.h3yk0.cryo.domain.SearchScope
import io.github.h3yk0.cryo.domain.TxSearch
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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Check
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import io.github.h3yk0.cryo.ui.components.CryoSheet
import io.github.h3yk0.cryo.ui.components.SectionTitle
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
import androidx.compose.ui.semantics.contentDescription
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
import io.github.h3yk0.cryo.ui.components.FlatItem
import io.github.h3yk0.cryo.ui.components.CryoTopBar
import io.github.h3yk0.cryo.ui.components.fabLift
import io.github.h3yk0.cryo.ui.components.EmptyState
import io.github.h3yk0.cryo.ui.components.HeatmapCalendar
import io.github.h3yk0.cryo.ui.components.MoneyText
import io.github.h3yk0.cryo.ui.components.MonthSelector
import io.github.h3yk0.cryo.ui.components.Segmented
import io.github.h3yk0.cryo.ui.components.TxRow
import io.github.h3yk0.cryo.ui.components.groupedRow
import io.github.h3yk0.cryo.ui.components.rememberLedger
import io.github.h3yk0.cryo.ui.components.txSignedAmount
import io.github.h3yk0.cryo.ui.theme.CryoTheme
import io.github.h3yk0.cryo.ui.tx.LoadingBox
import java.time.LocalDate
import io.github.h3yk0.cryo.ui.components.noTabSwipe

/** O nome vem na hora de mostrar (não na criação do enum), para acompanhar a troca de idioma. */
private enum class TxFilter(private val labelRes: Int) {
    ALL(R.string.txs_tudo), OUT(R.string.txs_saidas), IN(R.string.txs_entradas), OTHER(R.string.txs_transferencias_outros);

    val label: String get() = Res.s(labelRes)
}

@Composable
fun TransactionsScreen(categoryFilter: Long?, initialMonth: Int? = null, onlyOut: Boolean = false) {
    val l = rememberLedger() ?: return LoadingBox()
    val nav = LocalNav.current
    // RF-EXT-12: outra tela (o fluxo do dinheiro, RF-ANA-10) pode abrir o Extrato já no mês dela e só nas saídas.
    var ymKey by rememberSaveable(initialMonth) { mutableStateOf(initialMonth ?: l.today.ym().key()) }
    val ym = ymOf(ymKey)
    var mode by rememberSaveable { mutableStateOf(0) }
    var filter by rememberSaveable(onlyOut) { mutableStateOf(if (onlyOut) TxFilter.OUT else TxFilter.ALL) }
    var searching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable(categoryFilter) { mutableStateOf(categoryFilter) }
    // RF-EXT-09: a conta ou o cartão escolhido ("a12", "c3"; vazio = todas as contas).
    var sourceKey by rememberSaveable { mutableStateOf("") }
    val source = remember(sourceKey, l) { sourceOf(sourceKey, l) }
    var pickingSource by rememberSaveable { mutableStateOf(false) }
    // RF-EXT-11 (1.6): o filtro "Previstas" mostra as contas fixas do mês ainda em aberto.
    var planned by rememberSaveable { mutableStateOf(false) }
    var selectedDay by rememberSaveable(ymKey) { mutableStateOf(if (ym == l.today.ym()) l.today.dayOfMonth else null as Int?) }

    // RF-EXT-07: com a busca aberta, o período é o que a pessoa escolheu (tudo, o mês ou o ano do seletor).
    var scope by rememberSaveable { mutableStateOf(SearchScope.ALL) }
    val txs = remember(l, ymKey, filter, query, category, searching, scope, source) {
        val base = if (searching) TxSearch.inScope(l.s.txs, scope, ym) else l.monthTxs(ym)
        base.filter { t ->
            val okType = when (filter) {
                TxFilter.ALL -> true
                TxFilter.OUT -> t.type == TxType.EXPENSE
                TxFilter.IN -> t.type == TxType.INCOME
                TxFilter.OTHER -> t.type != TxType.EXPENSE && t.type != TxType.INCOME
            }
            val okCat = category == null || t.categoryId == category
            val okSource = source == null || AccountStatement.touches(t, source)
            val okQ = TxSearch.matches(t, query, t.categoryId?.let { l.category[it]?.name })
            okType && okCat && okSource && okQ
        }
    }
    // RF-EXT-09: com uma conta escolhida, o resumo do topo é o dela (o que entrou e saiu dela, transferências também).
    val summary = remember(l, ymKey, source) {
        if (source == null) l.monthSummary(ym).let { AccountStatement.Summary(it.income, it.expense) }
        else AccountStatement.summary(l.monthTxs(ym).filter { AccountStatement.touches(it, source) }, source)
    }

    Scaffold(
        topBar = {
            CryoTopBar(
                title = Res.s(R.string.tab_transactions),
                actions = {
                    IconButton(onClick = { searching = !searching; if (!searching) query = "" }) {
                        Icon(if (searching) Icons.Rounded.Close else Icons.Rounded.Search, if (searching) Res.s(R.string.txs_fechar_busca) else Res.s(R.string.txs_buscar))
                    }
                },
            )
        },
        floatingActionButton = {
            val fabLabel = Res.s(R.string.home_shortcut_new)
            ExtendedFloatingActionButton(
                // ERR-046 sobe o botão quando a barra de desfazer aparece; ERR-047 dá nome a ele para o leitor de tela
                // (o Material esconde o texto de dentro do botão estendido).
                modifier = Modifier.fabLift().semantics { contentDescription = fabLabel },
                onClick = { nav.go(Routes.txNew()) },
                icon = { Icon(Icons.Rounded.Add, null) }, text = { Text(Res.s(R.string.home_shortcut_new)) },
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { pad ->
        LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(bottom = 96.dp)) {
            if (searching) {
                item {
                    OutlinedTextField(
                        value = query, onValueChange = { query = it }, singleLine = true,
                        label = { Text(Res.s(R.string.txs_buscar_nome_valor)) },
                        leadingIcon = { Icon(Icons.Rounded.Search, null) },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }
                // RF-EXT-07: onde procurar. No mês e no ano, as setas andam de um em um.
                item {
                    Segmented(
                        listOf(
                            SearchScope.ALL to Res.s(R.string.txs_tudo), SearchScope.MONTH to Res.s(R.string.txs_escopo_mes),
                            SearchScope.YEAR to Res.s(R.string.txs_escopo_ano),
                        ),
                        scope, { scope = it }, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
                if (scope != SearchScope.ALL) {
                    item {
                        if (scope == SearchScope.YEAR) {
                            MonthSelector(
                                ym.year.toString(),
                                onPrev = { ymKey = ym.minusYears(1).key() }, onNext = { ymKey = ym.plusYears(1).key() },
                                modifier = Modifier.padding(horizontal = 8.dp),
                                prevLabel = Res.s(R.string.txs_ano_anterior), nextLabel = Res.s(R.string.txs_proximo_ano),
                            )
                        } else {
                            MonthSelector(
                                Dates.monthYear(ym, l.today),
                                onPrev = { ymKey = ym.minusMonths(1).key() }, onNext = { ymKey = ym.plusMonths(1).key() },
                                modifier = Modifier.padding(horizontal = 8.dp),
                            )
                        }
                    }
                }
                item {
                    val found = remember(txs, source) {
                        if (source != null) AccountStatement.summary(txs, source).let { Triple(txs.size, it.income, it.expense) }
                        else Triple(txs.size, txs.filter { it.type == TxType.INCOME }.sumOf { it.amount }, txs.filter { it.type == TxType.EXPENSE }.sumOf { it.amount })
                    }
                    FlatItem(Modifier.padding(horizontal = 16.dp)) {
                        Text(Res.p(R.plurals.imports_count, found.first, found.first), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                            SummaryCell(Res.s(R.string.month_in), found.second, CryoTheme.colors.income, Modifier.weight(1f))
                            SummaryCell(Res.s(R.string.month_out), found.third, CryoTheme.colors.expense, Modifier.weight(1f))
                        }
                    }
                }
            }
            if (!searching) {
                item {
                    MonthSelector(
                        Dates.monthYear(ym, l.today),
                        onPrev = { ymKey = ym.minusMonths(1).key() },
                        onNext = { ymKey = ym.plusMonths(1).key() },
                        modifier = Modifier.padding(horizontal = 8.dp),
                    )
                }
                item {
                    FlatItem(Modifier.padding(horizontal = 16.dp)) {
                        Row(Modifier.fillMaxWidth()) {
                            SummaryCell(Res.s(R.string.month_in), summary.income, CryoTheme.colors.income, Modifier.weight(1f))
                            SummaryCell(Res.s(R.string.month_out), summary.expense, CryoTheme.colors.expense, Modifier.weight(1f))
                            SummaryCell(
                                Res.s(R.string.txs_resultado), summary.result,
                                if (summary.result >= 0) CryoTheme.colors.income else CryoTheme.colors.expense, Modifier.weight(1f), signed = true,
                            )
                        }
                    }
                }
                item {
                    Segmented(
                        listOf(0 to Res.s(R.string.txs_lista), 1 to Res.s(R.string.txs_calendario)), mode, { mode = it },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                }
            }

            if (mode == 1 && !searching) {
                item {
                    FlatItem(Modifier.padding(horizontal = 16.dp)) {
                        Text(Res.s(R.string.txs_calendario_gastos), style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
                        Text(
                            Res.s(R.string.txs_quanto_mais_forte_cor),
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
                                Res.s(R.string.txs_nenhuma_movimentacao_neste_dia), style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                            )
                        }
                    }
                    itemsIndexed(list, key = { _, t -> t.id }) { i, t -> TxRow(t, l, onClick = { nav.go(Routes.txEdit(t.id)) }, modifier = Modifier.animateItem().groupedRow(i, list.size)) }
                }
            } else {
                item {
                    Row(
                        Modifier.fillMaxWidth().noTabSwipe().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        SourceChip(source, l, onOpen = { pickingSource = true }, onClear = { sourceKey = "" })
                        category?.let { id ->
                            InputChip(
                                selected = true, onClick = { category = null },
                                label = { Text(l.category[id]?.name ?: Res.s(R.string.plan_categoria)) },
                                trailingIcon = { Icon(Icons.Rounded.Close, Res.s(R.string.txs_remover_filtro_categoria)) },
                            )
                        }
                        TxFilter.entries.forEach { f ->
                            FilterChip(selected = !planned && filter == f, onClick = { filter = f; planned = false }, label = { Text(f.label) })
                        }
                        if (!searching) {
                            FilterChip(
                                selected = planned, onClick = { planned = !planned },
                                label = { Text(Res.s(R.string.txs_previstas)) },
                                leadingIcon = { Icon(Icons.Rounded.EventRepeat, null, Modifier.size(18.dp)) },
                            )
                        }
                    }
                }
                if (planned && !searching) {
                    plannedBills(l, ym, source)
                } else if (txs.isEmpty()) {
                    item {
                        EmptyState(
                            Icons.AutoMirrored.Rounded.ReceiptLong,
                            if (query.isNotBlank()) Res.s(R.string.txs_nada_encontrado) else Res.s(R.string.txs_nenhuma_movimentacao),
                            if (query.isNotBlank()) Res.s(R.string.txs_tente_outra_palavra) else Res.s(R.string.txs_use_botao_lancar_ou),
                        )
                    }
                } else {
                    val groups = txs.groupBy { it.date }.toSortedMap(compareByDescending { it })
                    groups.forEach { (date, list) ->
                        // DS-MOV-07: o que entra, sai ou muda de dia desliza e aparece com fade.
                        item(key = "h$date") { Box(Modifier.animateItem()) { DayHeader(date, list, l) } }
                        itemsIndexed(list, key = { _, t -> t.id }) { i, t -> TxRow(t, l, onClick = { nav.go(Routes.txEdit(t.id)) }, modifier = Modifier.animateItem().groupedRow(i, list.size)) }
                    }
                }
            }
        }
    }
    if (planned) PlannedBillSheets(l)
    if (pickingSource) {
        SourcePickerSheet(l, source, onPick = { sourceKey = keyOf(it); pickingSource = false }, onDismiss = { pickingSource = false })
    }
}

/** RF-EXT-11: o que as Previstas estão fazendo agora (a folha de ajustar). */
private val adjusting = androidx.compose.runtime.mutableStateOf<io.github.h3yk0.cryo.domain.BillStatus?>(null)

/**
 * RF-EXT-11 (1.6, LA5-2): as contas fixas do mês ainda não pagas (ou recebidas), com o dia, o valor e o total, e as três
 * ações: Já paguei (o caminho de sempre, que liga ao lançamento que já existe), Ajustar (outro valor, conta ou dia) e
 * Pular este mês (RF-FIX-12). São as mesmas do fim do mês previsto (RF-PRV-10). Com uma conta escolhida (RF-EXT-09), só as dela.
 */
private fun androidx.compose.foundation.lazy.LazyListScope.plannedBills(l: Ledger, ym: java.time.YearMonth, source: TxSource?) {
    val open = l.billStatuses(ym).filter { it.payment == null }.filter { s ->
        when (source) {
            null -> true
            is TxSource.Account -> s.bill.cardId == null && s.bill.accountId == source.id
            is TxSource.Card -> s.bill.cardId == source.id
        }
    }
    item {
        FlatItem(Modifier.padding(horizontal = 16.dp)) {
            Text(Res.s(R.string.txs_previstas_titulo), style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
            val pay = open.filter { it.bill.kind == io.github.h3yk0.cryo.data.db.CategoryKind.EXPENSE }.sumOf { it.bill.amount }
            val get = open.filter { it.bill.kind == io.github.h3yk0.cryo.data.db.CategoryKind.INCOME }.sumOf { it.bill.amount }
            val hide = LocalSettings.current.hideValues
            fun m(v: Long) = if (hide) Money.HIDDEN else Money.format(v)
            Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                Text(Res.s(R.string.txs_previstas_pagar, m(pay)), style = MaterialTheme.typography.bodyMedium, color = CryoTheme.colors.expense, modifier = Modifier.weight(1f))
                if (get > 0) Text(Res.s(R.string.txs_previstas_receber, m(get)), style = MaterialTheme.typography.bodyMedium, color = CryoTheme.colors.income)
            }
            Text(Res.s(R.string.txs_previstas_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    if (open.isEmpty()) {
        item {
            Text(
                Res.s(R.string.txs_previstas_nenhuma), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            )
        }
    }
    items(open, key = { "pb${it.bill.id}" }) { s -> PlannedBillRow(s, l) }
}

@Composable
private fun PlannedBillRow(s: io.github.h3yk0.cryo.domain.BillStatus, l: Ledger) {
    val c = LocalContainer.current
    val settings = LocalSettings.current
    val b = s.bill
    val income = b.kind == io.github.h3yk0.cryo.data.db.CategoryKind.INCOME
    val card = b.cardId?.let { l.card[it] }
    FlatItem(Modifier.padding(horizontal = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(b.name, style = MaterialTheme.typography.titleSmall)
                Text(
                    Res.s(if (income) R.string.bill_arrives_day else R.string.bill_due_day, s.due.dayOfMonth),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (s.state == io.github.h3yk0.cryo.domain.BillState.OVERDUE) CryoTheme.colors.expense else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            MoneyText(if (income) b.amount else -b.amount, signed = true, style = MaterialTheme.typography.titleSmall)
        }
        Row(Modifier.fillMaxWidth().noTabSwipe().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Já paguei: o mesmo caminho do aviso e de Planejar (liga ao pagamento que já está no Extrato, se houver).
            FilledTonalButton(onClick = {
                val before = l.s.txs.mapTo(HashSet()) { it.id }
                c.launch {
                    val id = c.repo.markBillPaidLinking(b, s.ym, b.amount, l.today, if (card == null) b.accountId ?: settings.defaultAccountId else null, card)
                    c.message(Res.s(if (income) R.string.plan_recebido_3 else R.string.plan_paga, b.name), Res.s(R.string.wallet_desfazer)) {
                        if (id in before) c.repo.unlinkBillPayment(id, never = false) else c.repo.unmarkBillPaid(b, s.ym)
                    }
                }
            }) { Text(Res.s(if (income) R.string.plan_recebi else R.string.debt_paguei)) }
            OutlinedButton(onClick = { adjusting.value = s }) { Text(Res.s(R.string.bill_adjust)) }
            TextButton(onClick = { skipBillWithUndo(c, b, s.ym) }) { Text(Res.s(R.string.bill_skip)) }
        }
    }
}

/** RF-EXT-11: "Ajustar" = pagar com outro valor, outra conta ou outro dia (a folha de sempre, já preenchida). */
@Composable
private fun PlannedBillSheets(l: Ledger) {
    val c = LocalContainer.current
    val s = adjusting.value ?: return
    val b = s.bill
    val card = b.cardId?.let { l.card[it] }
    val expense = b.kind == io.github.h3yk0.cryo.data.db.CategoryKind.EXPENSE
    MoneyActionSheet(
        title = if (expense) Res.s(R.string.plan_marcar_como_paga, b.name) else Res.s(R.string.plan_marcar_como_recebido, b.name),
        subtitle = if (card != null) Res.s(R.string.plan_sera_lancada_cartao, card.name) else Res.s(R.string.plan_confira_valor_pode_mudar),
        initialAmount = b.amount, l = l,
        accountLabel = if (card != null) null else if (expense) Res.s(R.string.plan_pago_com) else Res.s(R.string.plan_recebido_2),
        initialAccount = b.accountId ?: LocalSettings.current.defaultAccountId,
        confirmLabel = Res.s(R.string.plan_confirmar),
        onDismiss = { adjusting.value = null },
        offLabel = Res.s(R.string.plan_fora_do_saldo),
        offHint = Res.s(R.string.plan_fora_do_saldo_hint),
        onConfirmOff = { amount, date ->
            adjusting.value = null
            c.launch { c.repo.markBillPaid(b, s.ym, amount, date, null, null); c.message(if (expense) Res.s(R.string.plan_paga, b.name) else Res.s(R.string.plan_recebido_3, b.name)) }
        },
        onConfirm = { amount, acc, date ->
            adjusting.value = null
            c.launch { c.repo.markBillPaid(b, s.ym, amount, date, acc, card); c.message(if (expense) Res.s(R.string.plan_paga, b.name) else Res.s(R.string.plan_recebido_3, b.name)) }
        },
    )
}

/** RF-EXT-09: "a12" = conta 12, "c3" = cartão 3; o que não existe mais vira "todas as contas". */
private fun sourceOf(key: String, l: Ledger): TxSource? {
    val id = key.drop(1).toLongOrNull() ?: return null
    return when (key.firstOrNull()) {
        'a' -> TxSource.Account(id).takeIf { l.account[id] != null }
        'c' -> TxSource.Card(id).takeIf { l.card[id] != null }
        else -> null
    }
}

private fun keyOf(s: TxSource?): String = when (s) {
    null -> ""
    is TxSource.Account -> "a${s.id}"
    is TxSource.Card -> "c${s.id}"
}

private fun sourceName(s: TxSource, l: Ledger): Pair<String, Int?> = when (s) {
    is TxSource.Account -> l.accountName(s.id) to l.account[s.id]?.color
    is TxSource.Card -> l.cardName(s.id) to l.card[s.id]?.color
}

/** RF-EXT-09: o botão "Todas as contas" ao lado dos filtros; escolhida uma conta, mostra o nome dela com a bolinha. */
@Composable
private fun SourceChip(source: TxSource?, l: Ledger, onOpen: () -> Unit, onClear: () -> Unit) {
    if (source == null) {
        FilterChip(
            selected = false, onClick = onOpen, label = { Text(Res.s(R.string.txs_todas_contas)) },
            leadingIcon = { Icon(Icons.Rounded.AccountBalanceWallet, null, Modifier.size(18.dp)) },
            trailingIcon = { Icon(Icons.Rounded.ArrowDropDown, null, Modifier.size(18.dp)) },
        )
    } else {
        val (name, color) = sourceName(source, l)
        InputChip(
            selected = true, onClick = onOpen, label = { Text(name) },
            leadingIcon = { SourceDot(color) },
            trailingIcon = {
                IconButton(onClick = onClear, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Rounded.Close, Res.s(R.string.txs_mostrar_todas_contas), Modifier.size(18.dp))
                }
            },
        )
    }
}

@Composable
private fun SourceDot(color: Int?) {
    Box(
        Modifier.size(10.dp).clip(CircleShape)
            .background(color?.let { androidx.compose.ui.graphics.Color(it) } ?: MaterialTheme.colorScheme.outline),
    )
}

/** RF-EXT-09: a lista das contas e dos cartões, para ver o Extrato só de um deles. */
@Composable
private fun SourcePickerSheet(l: Ledger, current: TxSource?, onPick: (TxSource?) -> Unit, onDismiss: () -> Unit) {
    CryoSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
            Text(Res.s(R.string.txs_ver_extrato_de), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
            SourceOption(Res.s(R.string.txs_todas_contas), null, current == null) { onPick(null) }
            if (l.activeAccounts.isNotEmpty()) {
                SectionTitle(Res.s(R.string.home_accounts), Modifier.padding(horizontal = 20.dp))
                l.activeAccounts.forEach { a -> SourceOption(a.name, a.color, current == TxSource.Account(a.id)) { onPick(TxSource.Account(a.id)) } }
            }
            if (l.activeCards.isNotEmpty()) {
                SectionTitle(Res.s(R.string.txs_cartoes), Modifier.padding(horizontal = 20.dp))
                l.activeCards.forEach { c -> SourceOption(c.name, c.color, current == TxSource.Card(c.id)) { onPick(TxSource.Card(c.id)) } }
            }
        }
    }
}

@Composable
private fun SourceOption(name: String, color: Int?, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (color != null) SourceDot(color) else Icon(Icons.Rounded.AccountBalanceWallet, null, Modifier.size(18.dp))
        Spacer(Modifier.width(14.dp))
        Text(name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        if (selected) Icon(Icons.Rounded.Check, null, tint = MaterialTheme.colorScheme.primary)
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
