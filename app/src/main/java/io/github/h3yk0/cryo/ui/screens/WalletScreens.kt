// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.RequestQuote
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Unarchive
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.h3yk0.cryo.data.Palette
import io.github.h3yk0.cryo.data.db.Account
import io.github.h3yk0.cryo.data.db.AccountType
import io.github.h3yk0.cryo.data.db.CreditCard
import io.github.h3yk0.cryo.data.db.Investment
import io.github.h3yk0.cryo.data.db.InvestmentKind
import io.github.h3yk0.cryo.data.db.InvestmentYield
import io.github.h3yk0.cryo.data.db.Tx
import io.github.h3yk0.cryo.data.db.TxType
import io.github.h3yk0.cryo.domain.Dates
import io.github.h3yk0.cryo.domain.InvoiceStatus
import io.github.h3yk0.cryo.domain.Ledger
import io.github.h3yk0.cryo.domain.Money
import io.github.h3yk0.cryo.ui.LocalNav
import io.github.h3yk0.cryo.ui.Routes
import io.github.h3yk0.cryo.ui.components.AccountChips
import io.github.h3yk0.cryo.ui.components.AmountField
import io.github.h3yk0.cryo.ui.components.ColorChooser
import io.github.h3yk0.cryo.ui.components.ConfirmDialog
import io.github.h3yk0.cryo.ui.components.CryoCard
import io.github.h3yk0.cryo.ui.components.CryoTopBar
import io.github.h3yk0.cryo.ui.components.DateChooser
import io.github.h3yk0.cryo.ui.components.EmptyState
import io.github.h3yk0.cryo.ui.components.FieldLabel
import io.github.h3yk0.cryo.ui.components.IconBadge
import io.github.h3yk0.cryo.ui.components.LocalContainer
import io.github.h3yk0.cryo.ui.components.LocalSettings
import io.github.h3yk0.cryo.ui.components.MoneyText
import io.github.h3yk0.cryo.ui.components.MonthSelector
import io.github.h3yk0.cryo.ui.components.Pill
import io.github.h3yk0.cryo.ui.components.SectionTitle
import io.github.h3yk0.cryo.ui.components.Stepper
import io.github.h3yk0.cryo.ui.components.SwitchRow
import io.github.h3yk0.cryo.ui.components.TxRow
import io.github.h3yk0.cryo.ui.components.UsageBar
import io.github.h3yk0.cryo.ui.components.accountIcon
import io.github.h3yk0.cryo.ui.components.rememberLedger
import io.github.h3yk0.cryo.ui.theme.CryoTheme
import io.github.h3yk0.cryo.ui.tx.LoadingBox
import java.time.LocalDate
import java.time.YearMonth

/* =============================== Carteira =============================== */

@Composable
fun WalletScreen() {
    val l = rememberLedger() ?: return LoadingBox()
    val nav = LocalNav.current
    Scaffold(topBar = { CryoTopBar("Carteira") }, contentWindowInsets = WindowInsets(0, 0, 0, 0)) { pad ->
        LazyColumn(
            Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                CryoCard(color = MaterialTheme.colorScheme.primaryContainer) {
                    Text("Patrimônio", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    MoneyText(l.netWorth, style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Spacer(Modifier.height(10.dp))
                    WorthLine("Contas", l.totalBalance)
                    WorthLine("Investimentos", l.totalInvestments)
                    WorthLine("Metas e caixinhas", l.totalGoals)
                    WorthLine("Cartões (a pagar)", -l.s.cards.sumOf { l.cardOutstanding(it.id) })
                    if (l.totalDebts != 0L) WorthLine("Dívidas", -l.totalDebts)
                }
            }

            item { SectionTitle("Contas") { TextButton(onClick = { nav.go(Routes.accountEdit()) }) { Icon(Icons.Rounded.Add, null); Text("Nova") } } }
            if (l.activeAccounts.isEmpty()) {
                item { Text("Nenhuma conta ainda.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            items(l.activeAccounts, key = { "a${it.id}" }) { a ->
                CryoCard(onClick = { nav.go(Routes.account(a.id)) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(accountIcon(a.type), Color(a.color))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(a.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                accountTypeName(a.type) + if (!a.includeInTotal) " · fora do total" else "",
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        val b = l.balance(a.id)
                        MoneyText(b, style = MaterialTheme.typography.titleMedium, color = if (b < 0) CryoTheme.colors.expense else Color.Unspecified)
                    }
                }
            }
            if (l.activeAccounts.size >= 2) {
                item {
                    OutlinedButton(onClick = { nav.go(Routes.txNew(TxType.TRANSFER)) }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Rounded.SwapHoriz, null); Spacer(Modifier.width(8.dp)); Text("Transferir entre contas")
                    }
                }
            }

            item { SectionTitle("Cartões de crédito") { TextButton(onClick = { nav.go(Routes.cardEdit()) }) { Icon(Icons.Rounded.Add, null); Text("Novo") } } }
            if (l.activeCards.isEmpty()) {
                item { Text("Nenhum cartão cadastrado.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            items(l.activeCards, key = { "c${it.id}" }) { c ->
                val inv = l.focusInvoice(c)
                val st = inv.status(l.today)
                CryoCard(onClick = { nav.go(Routes.card(c.id)) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(Icons.Rounded.CreditCard, Color(c.color))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(c.name, style = MaterialTheme.typography.titleSmall)
                            Text(invoiceStatusText(st, inv.due, l), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        MoneyText(if (st == InvoiceStatus.OPEN) inv.total else inv.remaining, style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(Modifier.height(8.dp))
                    UsageBar((c.limitAmount - l.availableLimit(c)).toFloat() / c.limitAmount.coerceAtLeast(1), height = 6.dp, baseColor = Color(c.color))
                }
            }

            item { SectionTitle("Dívidas") { TextButton(onClick = { nav.go(Routes.debtEdit()) }) { Icon(Icons.Rounded.Add, null); Text("Nova") } } }
            if (l.debtOverview.open.isEmpty()) {
                item {
                    CryoCard(onClick = { nav.go(Routes.DEBTS) }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconBadge(Icons.Rounded.RequestQuote, MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(12.dp))
                            Text(
                                if (l.s.debts.isEmpty()) {
                                    "Empréstimos, financiamentos, acordos ou dinheiro que você deve a alguém ficam aqui, separados do resto."
                                } else {
                                    "Nenhuma dívida em aberto. Veja as quitadas em Dívidas."
                                },
                                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            } else {
                item { DebtSummaryCard(l, LocalSettings.current.hideValues, onClick = { nav.go(Routes.DEBTS) }) }
            }

            item {
                SectionTitle("Investimentos") { TextButton(onClick = { nav.go(Routes.investmentEdit()) }) { Icon(Icons.Rounded.Add, null); Text("Novo") } }
            }
            if (l.activeInvestments.isEmpty()) {
                item { Text("Cadastre onde seu dinheiro está investido para acompanhar o rendimento.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } else {
                item {
                    CryoCard(color = MaterialTheme.colorScheme.secondaryContainer) {
                        Row {
                            Column(Modifier.weight(1f)) {
                                Text("Total investido hoje", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                MoneyText(l.totalInvestments, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSecondaryContainer)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Rendeu", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                MoneyText(
                                    l.totalYield, signed = true, style = MaterialTheme.typography.titleMedium,
                                    color = if (l.totalYield >= 0) CryoTheme.colors.income else CryoTheme.colors.expense,
                                )
                            }
                        }
                    }
                }
            }
            items(l.activeInvestments, key = { "i${it.id}" }) { i ->
                CryoCard(onClick = { nav.go(Routes.investment(i.id)) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(Icons.AutoMirrored.Rounded.TrendingUp, Color(i.color))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(i.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(investmentKindName(i.kind), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            MoneyText(l.investmentValue(i.id), style = MaterialTheme.typography.titleMedium)
                            val y = l.yieldOf(i.id)
                            if (y != 0L) MoneyText(y, signed = true, style = MaterialTheme.typography.bodySmall, color = if (y > 0) CryoTheme.colors.income else CryoTheme.colors.expense)
                        }
                    }
                }
            }
            val archived = l.s.accounts.count { it.archived } + l.s.cards.count { it.archived } + l.s.investments.count { it.archived }
            if (archived > 0) {
                item {
                    OutlinedButton(
                        onClick = { nav.go(Routes.WALLET_ARCHIVE) },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(52.dp),
                    ) {
                        Icon(Icons.Rounded.Inventory2, null); Spacer(Modifier.width(8.dp))
                        Text("Arquivados ($archived)")
                    }
                }
            }
        }
    }
}

/* ============================ Itens arquivados ============================ */

/** Contas, cartões e investimentos arquivados: continuam com o histórico e podem voltar quando quiser. */
@Composable
fun WalletArchiveScreen() {
    val l = rememberLedger() ?: return LoadingBox()
    val nav = LocalNav.current
    val c = LocalContainer.current
    val accounts = l.s.accounts.filter { it.archived }
    val cards = l.s.cards.filter { it.archived }
    val investments = l.s.investments.filter { it.archived }

    Scaffold(topBar = { CryoTopBar("Arquivados", onBack = nav::back) }, contentWindowInsets = WindowInsets(0, 0, 0, 0)) { pad ->
        LazyColumn(
            Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (accounts.isEmpty() && cards.isEmpty() && investments.isEmpty()) {
                item {
                    EmptyState(
                        Icons.Rounded.Inventory2, "Nada arquivado",
                        "Contas, cartões e investimentos que você arquivar ficam aqui, com todo o histórico. Dá para restaurar quando quiser.",
                    )
                }
                return@LazyColumn
            }
            item {
                Text(
                    "Itens arquivados não aparecem nas listas nem na hora de lançar, mas o histórico continua no extrato.",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                )
            }
            if (accounts.isNotEmpty()) {
                item { SectionTitle("Contas") }
                items(accounts, key = { "a${it.id}" }) { a ->
                    ArchivedRow(
                        icon = accountIcon(a.type), color = Color(a.color), title = a.name, subtitle = accountTypeName(a.type),
                        value = l.balance(a.id), onClick = { nav.go(Routes.account(a.id)) },
                        onRestore = {
                            c.launch {
                                c.repo.saveAccount(a.copy(archived = false))
                                c.message("Conta ${a.name} restaurada", "Desfazer") { c.repo.saveAccount(a.copy(archived = true)) }
                            }
                        },
                    )
                }
            }
            if (cards.isNotEmpty()) {
                item { SectionTitle("Cartões de crédito") }
                items(cards, key = { "c${it.id}" }) { card ->
                    val owed = l.cardOutstanding(card.id)
                    ArchivedRow(
                        icon = Icons.Rounded.CreditCard, color = Color(card.color), title = card.name,
                        subtitle = if (owed > 0) "Ainda há fatura a pagar" else "Sem fatura em aberto",
                        value = if (owed > 0) owed else null, onClick = { nav.go(Routes.card(card.id)) },
                        onRestore = {
                            c.launch {
                                c.repo.saveCard(card.copy(archived = false))
                                c.message("Cartão ${card.name} restaurado", "Desfazer") { c.repo.saveCard(card.copy(archived = true)) }
                            }
                        },
                    )
                }
            }
            if (investments.isNotEmpty()) {
                item { SectionTitle("Investimentos") }
                items(investments, key = { "i${it.id}" }) { i ->
                    ArchivedRow(
                        icon = Icons.AutoMirrored.Rounded.TrendingUp, color = Color(i.color), title = i.name,
                        subtitle = investmentKindName(i.kind), value = l.investmentValue(i.id),
                        onClick = { nav.go(Routes.investment(i.id)) },
                        onRestore = {
                            c.launch {
                                c.repo.saveInvestment(i.copy(archived = false))
                                c.message("${i.name} restaurado", "Desfazer") { c.repo.saveInvestment(i.copy(archived = true)) }
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ArchivedRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    title: String,
    subtitle: String,
    value: Long?,
    onClick: () -> Unit,
    onRestore: () -> Unit,
) {
    CryoCard(onClick = onClick, color = MaterialTheme.colorScheme.surfaceContainerLowest) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(icon, color.copy(alpha = 0.6f))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (value != null) MoneyText(value, style = MaterialTheme.typography.bodyMedium)
            }
            FilledTonalButton(onClick = onRestore, contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)) {
                Icon(Icons.Rounded.Unarchive, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Restaurar")
            }
        }
    }
}

@Composable
private fun WorthLine(label: String, value: Long) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.weight(1f))
        MoneyText(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
    }
}

fun accountTypeName(t: AccountType) = when (t) {
    AccountType.CHECKING -> "Conta corrente"
    AccountType.SAVINGS -> "Poupança"
    AccountType.CASH -> "Dinheiro"
    AccountType.BENEFIT -> "Vale (refeição/alimentação)"
    AccountType.OTHER -> "Outra"
}

fun investmentKindName(k: InvestmentKind) = when (k) {
    InvestmentKind.SAVINGS -> "Poupança"
    InvestmentKind.FIXED_INCOME -> "Renda fixa (CDB, LCI, LCA)"
    InvestmentKind.TREASURY -> "Tesouro Direto"
    InvestmentKind.STOCKS -> "Ações"
    InvestmentKind.REITS -> "Fundos imobiliários"
    InvestmentKind.FUNDS -> "Fundos de investimento"
    InvestmentKind.CRYPTO -> "Criptomoedas"
    InvestmentKind.PENSION -> "Previdência"
    InvestmentKind.OTHER -> "Outro"
}

/* ========================= Folha de valor genérica ========================= */

/** Folha simples para ações com dinheiro: pagar fatura, aportar, resgatar, guardar... */
@Composable
fun MoneyActionSheet(
    title: String,
    subtitle: String?,
    initialAmount: Long,
    l: Ledger,
    accountLabel: String?,
    initialAccount: Long?,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (amount: Long, accountId: Long?, date: LocalDate) -> Unit,
    amountLabel: String = "Valor",
) {
    var amount by remember { mutableLongStateOf(initialAmount) }
    var acc by remember { mutableStateOf(initialAccount ?: l.activeAccounts.firstOrNull()?.id) }
    var date by remember { mutableStateOf(l.today) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).imePadding().padding(horizontal = 20.dp).padding(bottom = 28.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(16.dp))
            AmountField(amount, { amount = it }, label = amountLabel)
            if (accountLabel != null) {
                FieldLabel(accountLabel)
                AccountChips(l.activeAccounts, acc, { acc = it })
            }
            FieldLabel("Data")
            DateChooser(date, l.today, { date = it })
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = { onConfirm(amount, acc, date) },
                enabled = amount > 0 && (accountLabel == null || acc != null),
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) { Icon(Icons.Rounded.Check, null); Spacer(Modifier.width(8.dp)); Text(confirmLabel) }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Cancelar") }
        }
    }
}

/* ================================ Conta ================================ */

@Composable
fun AccountDetailScreen(id: Long) {
    val l = rememberLedger() ?: return LoadingBox()
    val nav = LocalNav.current
    val a = l.account[id] ?: return LoadingBox()
    val list = remember(l) { l.s.txs.filter { it.accountId == id || it.toAccountId == id }.take(200) }
    Scaffold(
        topBar = {
            CryoTopBar(a.name, onBack = nav::back, actions = {
                IconButton(onClick = { nav.go(Routes.accountEdit(id)) }) { Icon(Icons.Rounded.Edit, "Editar conta") }
            })
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { pad ->
        LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(bottom = 32.dp)) {
            item {
                CryoCard(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                    Text("Saldo atual", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    MoneyText(l.balance(id), style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text(accountTypeName(a.type), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
            item {
                FlowRow(
                    Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilledTonalButton(onClick = { nav.go(Routes.txNew(TxType.EXPENSE, account = id)) }) { Text("Despesa") }
                    FilledTonalButton(onClick = { nav.go(Routes.txNew(TxType.INCOME, account = id)) }) { Text("Receita") }
                    FilledTonalButton(onClick = { nav.go(Routes.txNew(TxType.TRANSFER, account = id)) }) { Text("Transferir") }
                }
            }
            item { SectionTitle("Movimentações", Modifier.padding(horizontal = 16.dp)) }
            if (list.isEmpty()) item { Text("Nada por aqui ainda.", Modifier.padding(horizontal = 20.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) }
            items(list, key = { it.id }) { t -> TxRow(t, l, onClick = { nav.go(Routes.txEdit(t.id)) }, showDate = true) }
        }
    }
}

@Composable
fun AccountEditorScreen(id: Long?) {
    val l = rememberLedger() ?: return LoadingBox()
    val c = LocalContainer.current
    val nav = LocalNav.current
    val settings = LocalSettings.current
    val existing = id?.let { l.account[it] }
    val currentBalance = existing?.let { l.balance(it.id) } ?: 0L
    var name by rememberSaveable { mutableStateOf(existing?.name ?: "") }
    var type by rememberSaveable { mutableStateOf(existing?.type ?: AccountType.CHECKING) }
    var balance by rememberSaveable { mutableLongStateOf(kotlin.math.abs(currentBalance)) }
    var negative by rememberSaveable { mutableStateOf(currentBalance < 0) }
    var color by rememberSaveable { mutableIntStateOf(existing?.color ?: Palette.pick(l.s.accounts.size + 5)) }
    var inTotal by rememberSaveable { mutableStateOf(existing?.includeInTotal ?: true) }
    var isDefault by rememberSaveable { mutableStateOf(existing != null && settings.defaultAccountId == existing.id) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    EditorScaffold(
        title = if (existing == null) "Nova conta" else "Editar conta",
        canSave = name.isNotBlank(),
        onSave = {
            val target = if (negative) -balance else balance
            val initial = if (existing == null) target else existing.initialBalance + (target - currentBalance)
            val acc = (existing ?: Account(name = name, color = color)).copy(
                name = name.trim(), type = type, initialBalance = initial, color = color, includeInTotal = inTotal,
            )
            c.launch {
                val newId = c.repo.saveAccount(acc)
                if (isDefault) c.settings.setDefaultAccount(newId) else if (settings.defaultAccountId == existing?.id) c.settings.setDefaultAccount(null)
            }
            nav.back()
        },
        onDelete = existing?.let { { confirmDelete = true } },
    ) {
        OutlinedTextField(
            name, { name = it }, label = { Text("Nome da conta") }, singleLine = true, placeholder = { Text("Ex.: Nubank, Carteira, Caixa") },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            modifier = Modifier.fillMaxWidth(),
        )
        FieldLabel("Tipo")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AccountType.entries.forEach { t ->
                FilterChip(selected = type == t, onClick = { type = t }, label = { Text(accountTypeName(t)) })
            }
        }
        FieldLabel(if (existing == null) "Saldo atual" else "Saldo atual (corrigir)")
        AmountField(balance, { balance = it }, label = if (negative) "Saldo negativo" else "Saldo")
        SwitchRow("Saldo está negativo", negative, { negative = it }, "Use se a conta estiver no cheque especial")
        FieldLabel("Cor")
        ColorChooser(color) { color = it }
        Spacer(Modifier.height(8.dp))
        SwitchRow("Somar no saldo total", inTotal, { inTotal = it }, "Desligue para contas que você não quer misturar")
        SwitchRow("Conta principal", isDefault, { isDefault = it }, "Usada quando você não diz a conta no registro rápido")
        if (existing != null) {
            SwitchRow("Arquivar conta", existing.archived, { v ->
                c.launch { c.repo.saveAccount(existing.copy(archived = v)) }
            }, "Some das listas, mas o histórico continua")
        }
    }
    if (confirmDelete && existing != null) {
        ConfirmDialog(
            "Apagar conta?", "Só é possível apagar contas sem movimentações. Se ela já tem histórico, use “Arquivar”.",
            "Apagar", onConfirm = {
                confirmDelete = false
                c.launch {
                    if (c.repo.deleteAccount(existing)) c.message("Conta apagada") else c.message("Esta conta tem movimentações. Arquive em vez de apagar.")
                }
                nav.back()
            }, onDismiss = { confirmDelete = false },
        )
    }
}

/** Estrutura padrão das telas de cadastro: barra superior, conteúdo rolável e botão Salvar fixo embaixo. */
@Composable
fun EditorScaffold(
    title: String,
    canSave: Boolean,
    onSave: () -> Unit,
    onDelete: (() -> Unit)? = null,
    saveLabel: String = "Salvar",
    content: @Composable () -> Unit,
) {
    val nav = LocalNav.current
    Scaffold(
        topBar = {
            CryoTopBar(title, onBack = nav::back, actions = {
                if (onDelete != null) TextButton(onClick = onDelete) { Text("Apagar", color = MaterialTheme.colorScheme.error) }
            })
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Button(
                    onClick = onSave, enabled = canSave,
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(16.dp).height(56.dp),
                ) { Icon(Icons.Rounded.Check, null); Spacer(Modifier.width(8.dp)); Text(saveLabel) }
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            content()
            Spacer(Modifier.height(24.dp))
        }
    }
}

/* ================================ Cartão ================================ */

@Composable
fun CardDetailScreen(id: Long) {
    val l = rememberLedger() ?: return LoadingBox()
    val nav = LocalNav.current
    val c = LocalContainer.current
    val card = l.card[id] ?: return LoadingBox()
    var ymKey by rememberSaveable { mutableStateOf(l.focusInvoice(card).ym.let { it.year * 100 + it.monthValue }) }
    val ym = YearMonth.of(ymKey / 100, ymKey % 100)
    val inv = l.invoice(card, ym)
    val st = inv.status(l.today)
    var paying by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CryoTopBar("Cartão ${card.name}", onBack = nav::back, actions = {
                IconButton(onClick = { nav.go(Routes.cardEdit(id)) }) { Icon(Icons.Rounded.Edit, "Editar cartão") }
            })
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { pad ->
        LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(bottom = 32.dp)) {
            item {
                MonthSelector(
                    "Fatura de ${Dates.monthYear(ym, l.today).lowercase()}",
                    onPrev = { ymKey = ym.minusMonths(1).let { it.year * 100 + it.monthValue } },
                    onNext = { ymKey = ym.plusMonths(1).let { it.year * 100 + it.monthValue } },
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
            }
            item {
                CryoCard(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Total da fatura", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.weight(1f))
                        val (pc, pt) = when (st) {
                            InvoiceStatus.OPEN -> MaterialTheme.colorScheme.secondary to "Aberta"
                            InvoiceStatus.CLOSED -> CryoTheme.colors.warning to "Fechada"
                            InvoiceStatus.PAID -> CryoTheme.colors.income to "Paga"
                            InvoiceStatus.OVERDUE -> CryoTheme.colors.expense to "Vencida"
                            InvoiceStatus.EMPTY -> MaterialTheme.colorScheme.outline to "Sem gastos"
                        }
                        Pill(pt, pc, MaterialTheme.colorScheme.surface)
                    }
                    MoneyText(inv.total, style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text(
                        "Fecha em ${Dates.short(inv.closing)} · vence em ${Dates.short(inv.due)}",
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    if (inv.paid > 0) {
                        Text(
                            "Pago: ${Money.format(inv.paid)}" + if (inv.remaining > 0) " · falta ${Money.format(inv.remaining)}" else "",
                            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                    if (inv.remaining > 0 && st != InvoiceStatus.OPEN || (st == InvoiceStatus.OPEN && inv.total > inv.paid)) {
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { paying = true }, modifier = Modifier.fillMaxWidth()) { Text("Pagar fatura") }
                    }
                }
            }
            item {
                CryoCard(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    val used = card.limitAmount - l.availableLimit(card)
                    Row {
                        Text("Limite usado", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                        MoneyText(used, style = MaterialTheme.typography.labelLarge)
                        Text(" de ${if (LocalSettings.current.hideValues) Money.HIDDEN else Money.format(card.limitAmount)}", style = MaterialTheme.typography.labelLarge)
                    }
                    Spacer(Modifier.height(8.dp))
                    UsageBar(used.toFloat() / card.limitAmount.coerceAtLeast(1), baseColor = Color(card.color))
                    Text(
                        "Disponível: ${if (LocalSettings.current.hideValues) Money.HIDDEN else Money.format(l.availableLimit(card))} (parcelas futuras já descontadas)",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
            item {
                Row(Modifier.padding(horizontal = 16.dp)) {
                    FilledTonalButton(onClick = { nav.go(Routes.txNew(TxType.EXPENSE, card = id)) }) {
                        Icon(Icons.Rounded.Add, null); Spacer(Modifier.width(6.dp)); Text("Nova compra")
                    }
                }
            }
            item { SectionTitle("Compras desta fatura", Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) }
            if (inv.items.isEmpty()) {
                item { Text("Nenhuma compra nesta fatura.", Modifier.padding(horizontal = 20.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            items(inv.items.sortedByDescending { it.date }, key = { it.id }) { t -> TxRow(t, l, onClick = { nav.go(Routes.txEdit(t.id)) }, showDate = true) }
            val payments = l.s.txs.filter { it.type == TxType.CARD_PAYMENT && it.cardId == id && it.invoiceYm == ymKey }
            if (payments.isNotEmpty()) {
                item { SectionTitle("Pagamentos", Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) }
                items(payments, key = { "p${it.id}" }) { t -> TxRow(t, l, onClick = { nav.go(Routes.txEdit(t.id)) }, showDate = true) }
            }
        }
    }
    if (paying) {
        MoneyActionSheet(
            title = "Pagar fatura", subtitle = "Fatura de ${Dates.monthName(ym).lowercase()} do ${card.name}",
            initialAmount = (inv.total - inv.paid).coerceAtLeast(0), l = l, accountLabel = "Pagar com a conta",
            initialAccount = card.payAccountId ?: LocalSettings.current.defaultAccountId, confirmLabel = "Confirmar pagamento",
            onDismiss = { paying = false },
            onConfirm = { amount, acc, date ->
                paying = false
                c.launch {
                    c.repo.payInvoice(card, ym, amount, acc!!, date)
                    c.message("Pagamento de ${Money.format(amount)} registrado")
                }
            },
        )
    }
}

@Composable
fun CardEditorScreen(id: Long?) {
    val l = rememberLedger() ?: return LoadingBox()
    val c = LocalContainer.current
    val nav = LocalNav.current
    val existing = id?.let { l.card[it] }
    var name by rememberSaveable { mutableStateOf(existing?.name ?: "") }
    var limit by rememberSaveable { mutableLongStateOf(existing?.limitAmount ?: 0L) }
    var closing by rememberSaveable { mutableIntStateOf(existing?.closingDay ?: 3) }
    var due by rememberSaveable { mutableIntStateOf(existing?.dueDay ?: 10) }
    var color by rememberSaveable { mutableIntStateOf(existing?.color ?: Palette.pick(l.s.cards.size + 2)) }
    var payAcc by rememberSaveable { mutableStateOf(existing?.payAccountId ?: l.activeAccounts.firstOrNull()?.id) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    EditorScaffold(
        title = if (existing == null) "Novo cartão" else "Editar cartão",
        canSave = name.isNotBlank() && limit > 0,
        onSave = {
            val card = (existing ?: CreditCard(name = name, limitAmount = limit, closingDay = closing, dueDay = due, color = color))
                .copy(name = name.trim(), limitAmount = limit, closingDay = closing, dueDay = due, color = color, payAccountId = payAcc)
            c.launch { c.repo.saveCard(card) }
            nav.back()
        },
        onDelete = existing?.let { { confirmDelete = true } },
    ) {
        OutlinedTextField(
            name, { name = it }, label = { Text("Nome do cartão") }, singleLine = true, placeholder = { Text("Ex.: Nubank, Inter") },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words), modifier = Modifier.fillMaxWidth(),
        )
        FieldLabel("Limite total")
        AmountField(limit, { limit = it }, label = "Limite")
        FieldLabel("Datas da fatura")
        Stepper(closing, { closing = it }, 1..31, "Dia que fecha")
        Stepper(due, { due = it }, 1..31, "Dia que vence")
        Text(
            "Compras feitas a partir do dia que fecha vão para a fatura seguinte.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FieldLabel("Conta usada para pagar")
        AccountChips(l.activeAccounts, payAcc, { payAcc = it })
        FieldLabel("Cor")
        ColorChooser(color) { color = it }
        if (existing != null) {
            Spacer(Modifier.height(8.dp))
            SwitchRow("Arquivar cartão", existing.archived, { v -> c.launch { c.repo.saveCard(existing.copy(archived = v)) } }, "Para cartões cancelados")
        }
    }
    if (confirmDelete && existing != null) {
        ConfirmDialog(
            "Apagar cartão?", "Só é possível apagar cartões sem compras. Se ele já tem histórico, use “Arquivar”.",
            "Apagar", onConfirm = {
                confirmDelete = false
                c.launch { if (c.repo.deleteCard(existing)) c.message("Cartão apagado") else c.message("Este cartão tem compras. Arquive em vez de apagar.") }
                nav.back()
            }, onDismiss = { confirmDelete = false },
        )
    }
}

/* ============================= Investimento ============================= */

@Composable
fun InvestmentDetailScreen(id: Long) {
    val l = rememberLedger() ?: return LoadingBox()
    val nav = LocalNav.current
    val c = LocalContainer.current
    val inv = l.investment[id] ?: return LoadingBox()
    var action by rememberSaveable { mutableStateOf<String?>(null) }
    val value = l.investmentValue(id)
    val invested = l.invested(id)
    val y = l.yieldOf(id)
    Scaffold(
        topBar = {
            CryoTopBar(inv.name, onBack = nav::back, actions = {
                IconButton(onClick = { nav.go(Routes.investmentEdit(id)) }) { Icon(Icons.Rounded.Edit, "Editar investimento") }
            })
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { pad ->
        LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(bottom = 32.dp)) {
            item {
                CryoCard(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                    Text("Valor atual", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    MoneyText(value, style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Spacer(Modifier.height(6.dp))
                    Row {
                        Column(Modifier.weight(1f)) {
                            Text("Você colocou", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            MoneyText(invested, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        Column(Modifier.weight(1f)) {
                            Text("Rendeu", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                MoneyText(y, signed = true, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                if (invested > 0) {
                                    Text(
                                        "  (${String.format(io.github.h3yk0.cryo.domain.LocaleBR, "%.1f", y * 100.0 / invested)}%)",
                                        style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    )
                                }
                            }
                        }
                    }
                }
            }
            item {
                FlowRow(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(onClick = { action = "in" }) { Text("Aportar") }
                    FilledTonalButton(onClick = { action = "out" }, enabled = value > 0) { Text("Resgatar") }
                    FilledTonalButton(onClick = { action = "update" }) { Text("Atualizar valor") }
                }
            }
            item {
                Text(
                    "Dica: de vez em quando, olhe o valor no app do banco e use “Atualizar valor”. A diferença vira rendimento.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }
            item { SectionTitle("Histórico", Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) }
            val hist = l.investmentHistory(id)
            if (hist.isEmpty()) item { Text("Sem movimentações ainda.", Modifier.padding(horizontal = 20.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) }
            items(hist.size) { i ->
                when (val h = hist[i]) {
                    is Tx -> TxRow(h, l, onClick = { nav.go(Routes.txEdit(h.id)) }, showDate = true)
                    is InvestmentYield -> YieldRow(h, onDelete = {
                        c.launch { c.repo.deleteYield(h); c.message("Atualização removida") }
                    })
                }
            }
        }
    }
    when (action) {
        "in", "out" -> MoneyActionSheet(
            title = if (action == "in") "Aportar em ${inv.name}" else "Resgatar de ${inv.name}",
            subtitle = if (action == "in") "O dinheiro sai da conta escolhida" else "O dinheiro volta para a conta escolhida",
            initialAmount = 0, l = l, accountLabel = if (action == "in") "Sai da conta" else "Entra na conta",
            initialAccount = LocalSettings.current.defaultAccountId, confirmLabel = if (action == "in") "Aportar" else "Resgatar",
            onDismiss = { action = null },
            onConfirm = { amount, acc, date ->
                val type = if (action == "in") TxType.INVEST_IN else TxType.INVEST_OUT
                action = null
                c.launch {
                    c.repo.saveTx(Tx(type = type, amount = amount, date = date, description = if (type == TxType.INVEST_IN) "Aporte" else "Resgate", accountId = acc, investmentId = id))
                    c.message(if (type == TxType.INVEST_IN) "Aporte registrado" else "Resgate registrado")
                }
            },
        )
        "update" -> MoneyActionSheet(
            title = "Atualizar valor", subtitle = "Quanto vale hoje? (veja no app do banco ou corretora)",
            initialAmount = value.coerceAtLeast(0), l = l, accountLabel = null, initialAccount = null,
            confirmLabel = "Atualizar", amountLabel = "Valor atual",
            onDismiss = { action = null },
            onConfirm = { amount, _, date ->
                action = null
                c.launch {
                    c.repo.updateInvestmentValue(inv, value, amount, date)
                    val diff = amount - value
                    c.message(if (diff >= 0) "Rendimento de ${Money.format(diff)} registrado" else "Desvalorização de ${Money.format(-diff)} registrada")
                }
            },
        )
    }
}

@Composable
private fun YieldRow(y: InvestmentYield, onDelete: () -> Unit) {
    var ask by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBadge(Icons.Rounded.Savings, if (y.amount >= 0) CryoTheme.colors.income else CryoTheme.colors.expense)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(if (y.amount >= 0) "Rendimento" else "Desvalorização", style = MaterialTheme.typography.bodyLarge)
            Text(Dates.full(y.date), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        MoneyText(y.amount, signed = true, style = MaterialTheme.typography.titleSmall, color = if (y.amount >= 0) CryoTheme.colors.income else CryoTheme.colors.expense)
        TextButton(onClick = { ask = true }) { Text("Remover") }
    }
    if (ask) ConfirmDialog("Remover atualização?", "O valor do investimento volta ao que era antes dela.", "Remover", { ask = false; onDelete() }, { ask = false })
}

@Composable
fun InvestmentEditorScreen(id: Long?) {
    val l = rememberLedger() ?: return LoadingBox()
    val c = LocalContainer.current
    val nav = LocalNav.current
    val existing = id?.let { l.investment[it] }
    var name by rememberSaveable { mutableStateOf(existing?.name ?: "") }
    var kind by rememberSaveable { mutableStateOf(existing?.kind ?: InvestmentKind.FIXED_INCOME) }
    var color by rememberSaveable { mutableIntStateOf(existing?.color ?: Palette.pick(l.s.investments.size + 4)) }
    var initial by rememberSaveable { mutableLongStateOf(0L) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    EditorScaffold(
        title = if (existing == null) "Novo investimento" else "Editar investimento",
        canSave = name.isNotBlank(),
        onSave = {
            val i = (existing ?: Investment(name = name, color = color)).copy(name = name.trim(), kind = kind, color = color)
            c.launch {
                val newId = c.repo.saveInvestment(i)
                if (existing == null && initial > 0) {
                    c.repo.saveTx(Tx(type = TxType.INVEST_IN, amount = initial, date = l.today, description = "Saldo inicial", investmentId = newId))
                }
            }
            nav.back()
        },
        onDelete = existing?.let { { confirmDelete = true } },
    ) {
        OutlinedTextField(
            name, { name = it }, label = { Text("Nome") }, singleLine = true, placeholder = { Text("Ex.: Tesouro Selic, CDB Nubank") },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words), modifier = Modifier.fillMaxWidth(),
        )
        FieldLabel("Tipo")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            InvestmentKind.entries.forEach { k -> FilterChip(selected = kind == k, onClick = { kind = k }, label = { Text(investmentKindName(k)) }) }
        }
        if (existing == null) {
            FieldLabel("Quanto já tem investido (opcional)")
            AmountField(initial, { initial = it }, label = "Valor atual")
            Text(
                "Esse valor não sai de nenhuma conta: serve para começar a acompanhar o que você já tem.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        FieldLabel("Cor")
        ColorChooser(color) { color = it }
        if (existing != null) {
            Spacer(Modifier.height(8.dp))
            SwitchRow("Arquivar", existing.archived, { v -> c.launch { c.repo.saveInvestment(existing.copy(archived = v)) } }, "Para investimentos encerrados")
        }
    }
    if (confirmDelete && existing != null) {
        ConfirmDialog(
            "Apagar investimento?",
            "Os aportes e resgates dele também serão apagados, e as contas voltam a ter esse dinheiro. Para encerrar sem mexer no histórico, resgate tudo e arquive.",
            "Apagar", onConfirm = {
                confirmDelete = false
                c.launch { c.repo.deleteInvestment(existing); c.message("Investimento apagado") }
                nav.back(); nav.back()
            }, onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
fun EmptyWallet() {
    EmptyState(Icons.Rounded.Savings, "Nada aqui", "Cadastre suas contas para começar.")
}
