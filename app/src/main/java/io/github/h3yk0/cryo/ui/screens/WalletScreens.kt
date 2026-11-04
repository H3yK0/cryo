// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.ui.screens

import io.github.h3yk0.cryo.R
import io.github.h3yk0.cryo.domain.Res
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
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Remove
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
import io.github.h3yk0.cryo.ui.components.CryoListRow
import io.github.h3yk0.cryo.ui.components.CryoSection
import io.github.h3yk0.cryo.ui.components.HeroBlock
import io.github.h3yk0.cryo.ui.components.SectionPadding
import io.github.h3yk0.cryo.ui.components.ShortcutItem
import io.github.h3yk0.cryo.ui.components.Shortcuts
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

/**
 * TELA-CARTEIRA: a Carteira completa (RF-INI-14). Na 1.4 deixou de ser aba: abre pelo patrimônio e pelas seções do Início.
 * Direção 2.0: o patrimônio em destaque e seções planas com linhas (DS-DIR-01).
 */
@Composable
fun WalletScreen() {
    val l = rememberLedger() ?: return LoadingBox()
    val nav = LocalNav.current
    val hide = LocalSettings.current.hideValues
    Scaffold(topBar = { CryoTopBar(Res.s(R.string.wallet_carteira), onBack = nav::back) }, contentWindowInsets = WindowInsets(0, 0, 0, 0)) { pad ->
        LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(bottom = 32.dp)) {
            item {
                Column(Modifier.padding(start = SectionPadding, end = SectionPadding, top = 4.dp, bottom = 16.dp)) {
                    Text(Res.s(R.string.home_net_worth), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    MoneyText(l.netWorth, style = MaterialTheme.typography.displaySmall)
                    Spacer(Modifier.height(12.dp))
                    WorthLine(Res.s(R.string.home_accounts), l.totalBalance)
                    WorthLine(Res.s(R.string.home_investments), l.totalInvestments)
                    WorthLine(Res.s(R.string.home_goals), l.totalGoals)
                    WorthLine(Res.s(R.string.wallet_cartoes_pagar), -l.s.cards.sumOf { l.cardOutstanding(it.id) })
                    if (l.totalDebts != 0L) WorthLine(Res.s(R.string.home_shortcut_debts), -l.totalDebts)
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
            }

            item {
                CryoSection(Res.s(R.string.home_accounts), action = { AddButton(Res.s(R.string.wallet_nova)) { nav.go(Routes.accountEdit()) } }) {
                    if (l.activeAccounts.isEmpty()) {
                        Text(Res.s(R.string.wallet_nenhuma_conta_ainda), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    l.activeAccounts.forEach { a ->
                        val b = l.balance(a.id)
                        CryoListRow(
                            a.name,
                            subtitle = accountTypeName(a.type) + if (!a.includeInTotal) Res.s(R.string.wallet_fora_total) else "",
                            leading = { IconBadge(accountIcon(a.type), Color(a.color)) },
                            trailing = { MoneyText(b, style = MaterialTheme.typography.titleSmall, color = if (b < 0) CryoTheme.colors.expense else Color.Unspecified) },
                            onClick = { nav.go(Routes.account(a.id)) },
                        )
                    }
                    if (l.activeAccounts.size >= 2) {
                        TextButton(onClick = { nav.go(Routes.txNew(TxType.TRANSFER)) }, contentPadding = PaddingValues(horizontal = 0.dp)) {
                            Icon(Icons.Rounded.SwapHoriz, null); Spacer(Modifier.width(8.dp)); Text(Res.s(R.string.wallet_transferir_entre_contas))
                        }
                    }
                }
            }

            item {
                CryoSection(Res.s(R.string.wallet_cartoes_credito), action = { AddButton(Res.s(R.string.wallet_novo)) { nav.go(Routes.cardEdit()) } }) {
                    if (l.activeCards.isEmpty()) {
                        Text(Res.s(R.string.wallet_nenhum_cartao_cadastrado), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    l.activeCards.forEach { c ->
                        val inv = l.focusInvoice(c)
                        val st = inv.status(l.today)
                        CryoListRow(
                            c.name,
                            subtitle = invoiceStatusText(st, inv.due, l),
                            leading = { IconBadge(Icons.Rounded.CreditCard, Color(c.color)) },
                            trailing = { MoneyText(if (st == InvoiceStatus.OPEN) inv.total else inv.remaining, style = MaterialTheme.typography.titleSmall) },
                            onClick = { nav.go(Routes.card(c.id)) },
                        ) {
                            Spacer(Modifier.height(8.dp))
                            UsageBar((c.limitAmount - l.availableLimit(c)).toFloat() / c.limitAmount.coerceAtLeast(1), height = 6.dp, baseColor = Color(c.color))
                        }
                    }
                }
            }

            item {
                CryoSection(
                    Res.s(R.string.home_shortcut_debts),
                    onClick = if (l.s.debts.isNotEmpty()) ({ nav.go(Routes.DEBTS) }) else null,
                    action = if (l.s.debts.isEmpty()) ({ AddButton(Res.s(R.string.wallet_nova)) { nav.go(Routes.debtEdit()) } }) else null,
                ) {
                    if (l.debtOverview.open.isEmpty()) {
                        Text(
                            if (l.s.debts.isEmpty()) {
                                Res.s(R.string.wallet_emprestimos_financiamentos_acordos_ou)
                            } else {
                                Res.s(R.string.wallet_nenhuma_divida_aberto_veja)
                            },
                            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        DebtSummaryContent(l, hide)
                    }
                }
            }

            item {
                CryoSection(Res.s(R.string.home_investments), action = { AddButton(Res.s(R.string.wallet_novo)) { nav.go(Routes.investmentEdit()) } }) {
                    if (l.activeInvestments.isEmpty()) {
                        Text(Res.s(R.string.wallet_cadastre_onde_seu_dinheiro), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(bottom = 8.dp)) {
                            Column(Modifier.weight(1f)) {
                                Text(Res.s(R.string.wallet_total_investido_hoje), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                MoneyText(l.totalInvestments, style = MaterialTheme.typography.headlineSmall)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(Res.s(R.string.home_yield), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                MoneyText(
                                    l.totalYield, signed = true, style = MaterialTheme.typography.titleMedium,
                                    color = if (l.totalYield >= 0) CryoTheme.colors.income else CryoTheme.colors.expense,
                                )
                            }
                        }
                    }
                    l.activeInvestments.forEach { i ->
                        val y = l.yieldOf(i.id)
                        CryoListRow(
                            i.name,
                            subtitle = investmentKindName(i.kind),
                            leading = { IconBadge(Icons.AutoMirrored.Rounded.TrendingUp, Color(i.color)) },
                            trailing = {
                                Column(horizontalAlignment = Alignment.End) {
                                    MoneyText(l.investmentValue(i.id), style = MaterialTheme.typography.titleSmall)
                                    if (y != 0L) MoneyText(y, signed = true, style = MaterialTheme.typography.bodySmall, color = if (y > 0) CryoTheme.colors.income else CryoTheme.colors.expense)
                                }
                            },
                            onClick = { nav.go(Routes.investment(i.id)) },
                        )
                    }
                }
            }

            val archived = l.s.accounts.count { it.archived } + l.s.cards.count { it.archived } + l.s.investments.count { it.archived }
            if (archived > 0) {
                item {
                    CryoSection(Res.s(R.string.wallet_arquivados_2, archived), onClick = { nav.go(Routes.WALLET_ARCHIVE) }, divider = false) {
                        Text(
                            Res.s(R.string.wallet_contas_cartoes_investimentos_que_2),
                            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

/** "+ Nova" / "+ Novo" no título de uma seção. */
@Composable
private fun AddButton(label: String, onClick: () -> Unit) {
    TextButton(onClick = onClick) { Icon(Icons.Rounded.Add, null); Spacer(Modifier.width(4.dp)); Text(label) }
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

    Scaffold(topBar = { CryoTopBar(Res.s(R.string.wallet_arquivados), onBack = nav::back) }, contentWindowInsets = WindowInsets(0, 0, 0, 0)) { pad ->
        LazyColumn(
            Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (accounts.isEmpty() && cards.isEmpty() && investments.isEmpty()) {
                item {
                    EmptyState(
                        Icons.Rounded.Inventory2, Res.s(R.string.wallet_nada_arquivado),
                        Res.s(R.string.wallet_contas_cartoes_investimentos_que),
                    )
                }
                return@LazyColumn
            }
            item {
                Text(
                    Res.s(R.string.wallet_itens_arquivados_nao_aparecem),
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                )
            }
            if (accounts.isNotEmpty()) {
                item { SectionTitle(Res.s(R.string.home_accounts)) }
                items(accounts, key = { "a${it.id}" }) { a ->
                    ArchivedRow(
                        icon = accountIcon(a.type), color = Color(a.color), title = a.name, subtitle = accountTypeName(a.type),
                        value = l.balance(a.id), onClick = { nav.go(Routes.account(a.id)) },
                        onRestore = {
                            c.launch {
                                c.repo.saveAccount(a.copy(archived = false))
                                c.message(Res.s(R.string.wallet_conta_restaurada, a.name), Res.s(R.string.wallet_desfazer)) { c.repo.saveAccount(a.copy(archived = true)) }
                            }
                        },
                    )
                }
            }
            if (cards.isNotEmpty()) {
                item { SectionTitle(Res.s(R.string.wallet_cartoes_credito)) }
                items(cards, key = { "c${it.id}" }) { card ->
                    val owed = l.cardOutstanding(card.id)
                    ArchivedRow(
                        icon = Icons.Rounded.CreditCard, color = Color(card.color), title = card.name,
                        subtitle = if (owed > 0) Res.s(R.string.wallet_ainda_ha_fatura_pagar) else Res.s(R.string.wallet_sem_fatura_aberto),
                        value = if (owed > 0) owed else null, onClick = { nav.go(Routes.card(card.id)) },
                        onRestore = {
                            c.launch {
                                c.repo.saveCard(card.copy(archived = false))
                                c.message(Res.s(R.string.wallet_cartao_restaurado, card.name), Res.s(R.string.wallet_desfazer)) { c.repo.saveCard(card.copy(archived = true)) }
                            }
                        },
                    )
                }
            }
            if (investments.isNotEmpty()) {
                item { SectionTitle(Res.s(R.string.home_investments)) }
                items(investments, key = { "i${it.id}" }) { i ->
                    ArchivedRow(
                        icon = Icons.AutoMirrored.Rounded.TrendingUp, color = Color(i.color), title = i.name,
                        subtitle = investmentKindName(i.kind), value = l.investmentValue(i.id),
                        onClick = { nav.go(Routes.investment(i.id)) },
                        onRestore = {
                            c.launch {
                                c.repo.saveInvestment(i.copy(archived = false))
                                c.message(Res.s(R.string.wallet_restaurado, i.name), Res.s(R.string.wallet_desfazer)) { c.repo.saveInvestment(i.copy(archived = true)) }
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
    CryoListRow(
        title, Modifier.padding(horizontal = SectionPadding), subtitle = subtitle,
        leading = { IconBadge(icon, color.copy(alpha = 0.6f)) },
        trailing = {
            FilledTonalButton(onClick = onRestore, contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)) {
                Icon(Icons.Rounded.Unarchive, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text(Res.s(R.string.wallet_restaurar))
            }
        },
        onClick = onClick,
    ) { if (value != null) MoneyText(value, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 52.dp)) }
}

@Composable
private fun WorthLine(label: String, value: Long) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        MoneyText(value, style = MaterialTheme.typography.bodyMedium)
    }
}

fun accountTypeName(t: AccountType) = when (t) {
    AccountType.CHECKING -> Res.s(R.string.wallet_conta_corrente)
    AccountType.SAVINGS -> Res.s(R.string.wallet_poupanca)
    AccountType.CASH -> Res.s(R.string.wallet_dinheiro)
    AccountType.BENEFIT -> Res.s(R.string.wallet_vale_refeicao_alimentacao)
    AccountType.OTHER -> Res.s(R.string.wallet_outra)
}

fun investmentKindName(k: InvestmentKind) = when (k) {
    InvestmentKind.SAVINGS -> Res.s(R.string.wallet_poupanca)
    InvestmentKind.FIXED_INCOME -> Res.s(R.string.wallet_renda_fixa_cdb_lci)
    InvestmentKind.TREASURY -> Res.s(R.string.wallet_tesouro_direto)
    InvestmentKind.STOCKS -> Res.s(R.string.wallet_acoes)
    InvestmentKind.REITS -> Res.s(R.string.wallet_fundos_imobiliarios)
    InvestmentKind.FUNDS -> Res.s(R.string.wallet_fundos_investimento)
    InvestmentKind.CRYPTO -> Res.s(R.string.wallet_criptomoedas)
    InvestmentKind.PENSION -> Res.s(R.string.wallet_previdencia)
    InvestmentKind.OTHER -> Res.s(R.string.wallet_outro)
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
    amountLabel: String = Res.s(R.string.amount_label),
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
            FieldLabel(Res.s(R.string.wallet_data))
            DateChooser(date, l.today, { date = it })
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = { onConfirm(amount, acc, date) },
                enabled = amount > 0 && (accountLabel == null || acc != null),
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) { Icon(Icons.Rounded.Check, null); Spacer(Modifier.width(8.dp)); Text(confirmLabel) }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text(Res.s(R.string.wallet_cancelar)) }
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
                IconButton(onClick = { nav.go(Routes.import(account = id)) }) { Icon(Icons.Rounded.UploadFile, Res.s(R.string.wallet_importar_extrato_desta_conta)) }
                IconButton(onClick = { nav.go(Routes.accountEdit(id)) }) { Icon(Icons.Rounded.Edit, Res.s(R.string.wallet_editar_conta)) }
            })
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { pad ->
        LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(bottom = 32.dp)) {
            item {
                HeroBlock {
                    Text(Res.s(R.string.wallet_saldo_atual), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    MoneyText(l.balance(id), style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text(accountTypeName(a.type), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
            item {
                Shortcuts(
                    listOf(
                        ShortcutItem(Res.s(R.string.wallet_despesa), Icons.Rounded.Remove) { nav.go(Routes.txNew(TxType.EXPENSE, account = id)) },
                        ShortcutItem(Res.s(R.string.wallet_receita), Icons.Rounded.Add) { nav.go(Routes.txNew(TxType.INCOME, account = id)) },
                        ShortcutItem(Res.s(R.string.home_shortcut_transfer), Icons.Rounded.SwapHoriz) { nav.go(Routes.txNew(TxType.TRANSFER, account = id)) },
                        ShortcutItem(Res.s(R.string.home_shortcut_import), Icons.Rounded.UploadFile) { nav.go(Routes.import(account = id)) },
                    ),
                    Modifier.padding(vertical = 12.dp),
                )
            }
            item { SectionTitle(Res.s(R.string.wallet_movimentacoes), Modifier.padding(horizontal = 16.dp)) }
            if (list.isEmpty()) item { Text(Res.s(R.string.wallet_nada_por_aqui_ainda), Modifier.padding(horizontal = 20.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) }
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
        title = if (existing == null) Res.s(R.string.wallet_nova_conta) else Res.s(R.string.wallet_editar_conta),
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
            name, { name = it }, label = { Text(Res.s(R.string.wallet_nome_conta)) }, singleLine = true, placeholder = { Text(Res.s(R.string.wallet_ex_nubank_carteira_caixa)) },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            modifier = Modifier.fillMaxWidth(),
        )
        FieldLabel(Res.s(R.string.wallet_tipo))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AccountType.entries.forEach { t ->
                FilterChip(selected = type == t, onClick = { type = t }, label = { Text(accountTypeName(t)) })
            }
        }
        FieldLabel(if (existing == null) Res.s(R.string.wallet_saldo_atual) else Res.s(R.string.wallet_saldo_atual_corrigir))
        AmountField(balance, { balance = it }, label = if (negative) Res.s(R.string.wallet_saldo_negativo) else Res.s(R.string.wallet_saldo))
        SwitchRow(Res.s(R.string.wallet_saldo_esta_negativo), negative, { negative = it }, Res.s(R.string.wallet_use_se_conta_estiver))
        FieldLabel(Res.s(R.string.wallet_cor))
        ColorChooser(color) { color = it }
        Spacer(Modifier.height(8.dp))
        SwitchRow(Res.s(R.string.wallet_somar_saldo_total), inTotal, { inTotal = it }, Res.s(R.string.wallet_desligue_para_contas_que))
        SwitchRow(Res.s(R.string.wallet_conta_principal), isDefault, { isDefault = it }, Res.s(R.string.wallet_usada_quando_voce_nao))
        if (existing != null) {
            SwitchRow(Res.s(R.string.wallet_arquivar_conta), existing.archived, { v ->
                c.launch { c.repo.saveAccount(existing.copy(archived = v)) }
            }, Res.s(R.string.wallet_some_das_listas_mas))
        }
    }
    if (confirmDelete && existing != null) {
        ConfirmDialog(
            Res.s(R.string.wallet_apagar_conta), Res.s(R.string.wallet_so_possivel_apagar_contas),
            Res.s(R.string.wallet_apagar), onConfirm = {
                confirmDelete = false
                c.launch {
                    if (c.repo.deleteAccount(existing)) c.message(Res.s(R.string.wallet_conta_apagada)) else c.message(Res.s(R.string.wallet_esta_conta_tem_movimentacoes))
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
    saveLabel: String = Res.s(R.string.wallet_salvar),
    content: @Composable () -> Unit,
) {
    val nav = LocalNav.current
    Scaffold(
        topBar = {
            CryoTopBar(title, onBack = nav::back, actions = {
                if (onDelete != null) TextButton(onClick = onDelete) { Text(Res.s(R.string.wallet_apagar), color = MaterialTheme.colorScheme.error) }
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
            CryoTopBar(Res.s(R.string.home_card_title, card.name), onBack = nav::back, actions = {
                IconButton(onClick = { nav.go(Routes.import(card = id)) }) { Icon(Icons.Rounded.UploadFile, Res.s(R.string.wallet_importar_fatura_deste_cartao)) }
                IconButton(onClick = { nav.go(Routes.cardEdit(id)) }) { Icon(Icons.Rounded.Edit, Res.s(R.string.wallet_editar_cartao)) }
            })
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { pad ->
        LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(bottom = 32.dp)) {
            item {
                MonthSelector(
                    Res.s(R.string.wallet_fatura_2, Dates.monthYearInSentence(ym, l.today)),
                    onPrev = { ymKey = ym.minusMonths(1).let { it.year * 100 + it.monthValue } },
                    onNext = { ymKey = ym.plusMonths(1).let { it.year * 100 + it.monthValue } },
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
            }
            item {
                HeroBlock {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(Res.s(R.string.wallet_total_fatura), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.weight(1f))
                        val (pc, pt) = when (st) {
                            InvoiceStatus.OPEN -> MaterialTheme.colorScheme.secondary to Res.s(R.string.wallet_aberta)
                            InvoiceStatus.CLOSED -> CryoTheme.colors.warning to Res.s(R.string.wallet_fechada)
                            InvoiceStatus.PAID -> CryoTheme.colors.income to Res.s(R.string.wallet_paga)
                            InvoiceStatus.OVERDUE -> CryoTheme.colors.expense to Res.s(R.string.wallet_vencida)
                            InvoiceStatus.EMPTY -> MaterialTheme.colorScheme.outline to Res.s(R.string.wallet_sem_gastos)
                        }
                        Pill(pt, pc, MaterialTheme.colorScheme.surface)
                    }
                    MoneyText(inv.total, style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text(
                        Res.s(R.string.wallet_fecha_vence, Dates.short(inv.closing), Dates.short(inv.due)),
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    if (inv.paid > 0) {
                        Text(
                            Res.s(R.string.wallet_pago, Money.format(inv.paid)) + if (inv.remaining > 0) Res.s(R.string.wallet_falta, Money.format(inv.remaining)) else "",
                            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                    if (inv.remaining > 0 && st != InvoiceStatus.OPEN || (st == InvoiceStatus.OPEN && inv.total > inv.paid)) {
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { paying = true }, modifier = Modifier.fillMaxWidth()) { Text(Res.s(R.string.wallet_pagar_fatura)) }
                    }
                }
            }
            item {
                Column(Modifier.padding(horizontal = SectionPadding, vertical = 12.dp)) {
                    val used = card.limitAmount - l.availableLimit(card)
                    Row {
                        Text(Res.s(R.string.wallet_limite_usado), style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                        MoneyText(used, style = MaterialTheme.typography.labelLarge)
                        Text(Res.s(R.string.amount_of_total, if (LocalSettings.current.hideValues) Money.HIDDEN else Money.format(card.limitAmount)), style = MaterialTheme.typography.labelLarge)
                    }
                    Spacer(Modifier.height(8.dp))
                    UsageBar(used.toFloat() / card.limitAmount.coerceAtLeast(1), baseColor = Color(card.color))
                    Text(
                        Res.s(R.string.wallet_disponivel_parcelas_futuras_ja, if (LocalSettings.current.hideValues) Money.HIDDEN else Money.format(l.availableLimit(card))),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
            item {
                Row(Modifier.padding(horizontal = 16.dp)) {
                    FilledTonalButton(onClick = { nav.go(Routes.txNew(TxType.EXPENSE, card = id)) }) {
                        Icon(Icons.Rounded.Add, null); Spacer(Modifier.width(6.dp)); Text(Res.s(R.string.wallet_nova_compra))
                    }
                }
            }
            item { SectionTitle(Res.s(R.string.wallet_compras_desta_fatura), Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) }
            if (inv.items.isEmpty()) {
                item { Text(Res.s(R.string.wallet_nenhuma_compra_nesta_fatura), Modifier.padding(horizontal = 20.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            items(inv.items.sortedByDescending { it.date }, key = { it.id }) { t -> TxRow(t, l, onClick = { nav.go(Routes.txEdit(t.id)) }, showDate = true) }
            val payments = l.s.txs.filter { it.type == TxType.CARD_PAYMENT && it.cardId == id && it.invoiceYm == ymKey }
            if (payments.isNotEmpty()) {
                item { SectionTitle(Res.s(R.string.wallet_pagamentos), Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) }
                items(payments, key = { "p${it.id}" }) { t -> TxRow(t, l, onClick = { nav.go(Routes.txEdit(t.id)) }, showDate = true) }
            }
        }
    }
    if (paying) {
        MoneyActionSheet(
            title = Res.s(R.string.wallet_pagar_fatura), subtitle = Res.s(R.string.wallet_fatura, Dates.monthInSentence(ym), card.name),
            initialAmount = (inv.total - inv.paid).coerceAtLeast(0), l = l, accountLabel = Res.s(R.string.wallet_pagar_com_conta),
            initialAccount = card.payAccountId ?: LocalSettings.current.defaultAccountId, confirmLabel = Res.s(R.string.wallet_confirmar_pagamento),
            onDismiss = { paying = false },
            onConfirm = { amount, acc, date ->
                paying = false
                c.launch {
                    c.repo.payInvoice(card, ym, amount, acc!!, date)
                    c.message(Res.s(R.string.wallet_pagamento_registrado, Money.format(amount)))
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
        title = if (existing == null) Res.s(R.string.wallet_novo_cartao) else Res.s(R.string.wallet_editar_cartao),
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
            name, { name = it }, label = { Text(Res.s(R.string.wallet_nome_cartao)) }, singleLine = true, placeholder = { Text(Res.s(R.string.wallet_ex_nubank_inter)) },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words), modifier = Modifier.fillMaxWidth(),
        )
        FieldLabel(Res.s(R.string.wallet_limite_total))
        AmountField(limit, { limit = it }, label = Res.s(R.string.wallet_limite))
        FieldLabel(Res.s(R.string.wallet_datas_fatura))
        Stepper(closing, { closing = it }, 1..31, Res.s(R.string.wallet_dia_que_fecha))
        Stepper(due, { due = it }, 1..31, Res.s(R.string.wallet_dia_que_vence))
        Text(
            Res.s(R.string.wallet_compras_feitas_partir_dia),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FieldLabel(Res.s(R.string.wallet_conta_usada_para_pagar))
        AccountChips(l.activeAccounts, payAcc, { payAcc = it })
        FieldLabel(Res.s(R.string.wallet_cor))
        ColorChooser(color) { color = it }
        if (existing != null) {
            Spacer(Modifier.height(8.dp))
            SwitchRow(Res.s(R.string.wallet_arquivar_cartao), existing.archived, { v -> c.launch { c.repo.saveCard(existing.copy(archived = v)) } }, Res.s(R.string.wallet_para_cartoes_cancelados))
        }
    }
    if (confirmDelete && existing != null) {
        ConfirmDialog(
            Res.s(R.string.wallet_apagar_cartao), Res.s(R.string.wallet_so_possivel_apagar_cartoes),
            Res.s(R.string.wallet_apagar), onConfirm = {
                confirmDelete = false
                c.launch { if (c.repo.deleteCard(existing)) c.message(Res.s(R.string.wallet_cartao_apagado)) else c.message(Res.s(R.string.wallet_este_cartao_tem_compras)) }
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
                IconButton(onClick = { nav.go(Routes.investmentEdit(id)) }) { Icon(Icons.Rounded.Edit, Res.s(R.string.wallet_editar_investimento)) }
            })
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { pad ->
        LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(bottom = 32.dp)) {
            item {
                HeroBlock {
                    Text(Res.s(R.string.wallet_valor_atual), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    MoneyText(value, style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Spacer(Modifier.height(6.dp))
                    Row {
                        Column(Modifier.weight(1f)) {
                            Text(Res.s(R.string.wallet_voce_colocou), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            MoneyText(invested, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        Column(Modifier.weight(1f)) {
                            Text(Res.s(R.string.home_yield), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                MoneyText(y, signed = true, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                if (invested > 0) {
                                    Text(
                                        "  (${io.github.h3yk0.cryo.domain.Num.decimal(y * 100.0 / invested)}%)",
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
                    FilledTonalButton(onClick = { action = "in" }) { Text(Res.s(R.string.wallet_aportar)) }
                    FilledTonalButton(onClick = { action = "out" }, enabled = value > 0) { Text(Res.s(R.string.wallet_resgatar)) }
                    FilledTonalButton(onClick = { action = "update" }) { Text(Res.s(R.string.wallet_atualizar_valor)) }
                }
            }
            item {
                Text(
                    Res.s(R.string.wallet_dica_vez_quando_olhe),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }
            item { SectionTitle(Res.s(R.string.wallet_historico), Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) }
            val hist = l.investmentHistory(id)
            if (hist.isEmpty()) item { Text(Res.s(R.string.wallet_sem_movimentacoes_ainda), Modifier.padding(horizontal = 20.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) }
            items(hist.size) { i ->
                when (val h = hist[i]) {
                    is Tx -> TxRow(h, l, onClick = { nav.go(Routes.txEdit(h.id)) }, showDate = true)
                    is InvestmentYield -> YieldRow(h, onDelete = {
                        c.launch { c.repo.deleteYield(h); c.message(Res.s(R.string.wallet_atualizacao_removida)) }
                    })
                }
            }
        }
    }
    when (action) {
        "in", "out" -> MoneyActionSheet(
            title = if (action == "in") Res.s(R.string.wallet_aportar_2, inv.name) else Res.s(R.string.wallet_resgatar_2, inv.name),
            subtitle = if (action == "in") Res.s(R.string.wallet_dinheiro_sai_conta_escolhida) else Res.s(R.string.wallet_dinheiro_volta_para_conta),
            initialAmount = 0, l = l, accountLabel = if (action == "in") Res.s(R.string.wallet_sai_conta) else Res.s(R.string.wallet_entra_conta),
            initialAccount = LocalSettings.current.defaultAccountId, confirmLabel = if (action == "in") Res.s(R.string.wallet_aportar) else Res.s(R.string.wallet_resgatar),
            onDismiss = { action = null },
            onConfirm = { amount, acc, date ->
                val type = if (action == "in") TxType.INVEST_IN else TxType.INVEST_OUT
                action = null
                c.launch {
                    c.repo.saveTx(Tx(type = type, amount = amount, date = date, description = if (type == TxType.INVEST_IN) Res.s(R.string.wallet_aporte) else Res.s(R.string.wallet_resgate), accountId = acc, investmentId = id))
                    c.message(if (type == TxType.INVEST_IN) Res.s(R.string.wallet_aporte_registrado) else Res.s(R.string.wallet_resgate_registrado))
                }
            },
        )
        "update" -> MoneyActionSheet(
            title = Res.s(R.string.wallet_atualizar_valor), subtitle = Res.s(R.string.wallet_quanto_vale_hoje_veja),
            initialAmount = value.coerceAtLeast(0), l = l, accountLabel = null, initialAccount = null,
            confirmLabel = Res.s(R.string.wallet_atualizar), amountLabel = Res.s(R.string.wallet_valor_atual),
            onDismiss = { action = null },
            onConfirm = { amount, _, date ->
                action = null
                c.launch {
                    c.repo.updateInvestmentValue(inv, value, amount, date)
                    val diff = amount - value
                    c.message(if (diff >= 0) Res.s(R.string.wallet_rendimento_registrado, Money.format(diff)) else Res.s(R.string.wallet_desvalorizacao_registrada, Money.format(-diff)))
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
            Text(if (y.amount >= 0) Res.s(R.string.wallet_rendimento) else Res.s(R.string.wallet_desvalorizacao), style = MaterialTheme.typography.bodyLarge)
            Text(Dates.full(y.date), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        MoneyText(y.amount, signed = true, style = MaterialTheme.typography.titleSmall, color = if (y.amount >= 0) CryoTheme.colors.income else CryoTheme.colors.expense)
        TextButton(onClick = { ask = true }) { Text(Res.s(R.string.wallet_remover)) }
    }
    if (ask) ConfirmDialog(Res.s(R.string.wallet_remover_atualizacao), Res.s(R.string.wallet_valor_investimento_volta_ao), Res.s(R.string.wallet_remover), { ask = false; onDelete() }, { ask = false })
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
        title = if (existing == null) Res.s(R.string.wallet_novo_investimento) else Res.s(R.string.wallet_editar_investimento),
        canSave = name.isNotBlank(),
        onSave = {
            val i = (existing ?: Investment(name = name, color = color)).copy(name = name.trim(), kind = kind, color = color)
            c.launch {
                val newId = c.repo.saveInvestment(i)
                if (existing == null && initial > 0) {
                    c.repo.saveTx(Tx(type = TxType.INVEST_IN, amount = initial, date = l.today, description = Res.s(R.string.wallet_saldo_inicial), investmentId = newId))
                }
            }
            nav.back()
        },
        onDelete = existing?.let { { confirmDelete = true } },
    ) {
        OutlinedTextField(
            name, { name = it }, label = { Text(Res.s(R.string.wallet_nome)) }, singleLine = true, placeholder = { Text(Res.s(R.string.wallet_ex_tesouro_selic_cdb)) },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words), modifier = Modifier.fillMaxWidth(),
        )
        FieldLabel(Res.s(R.string.wallet_tipo))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            InvestmentKind.entries.forEach { k -> FilterChip(selected = kind == k, onClick = { kind = k }, label = { Text(investmentKindName(k)) }) }
        }
        if (existing == null) {
            FieldLabel(Res.s(R.string.wallet_quanto_ja_tem_investido))
            AmountField(initial, { initial = it }, label = Res.s(R.string.wallet_valor_atual))
            Text(
                Res.s(R.string.wallet_esse_valor_nao_sai),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        FieldLabel(Res.s(R.string.wallet_cor))
        ColorChooser(color) { color = it }
        if (existing != null) {
            Spacer(Modifier.height(8.dp))
            SwitchRow(Res.s(R.string.wallet_arquivar), existing.archived, { v -> c.launch { c.repo.saveInvestment(existing.copy(archived = v)) } }, Res.s(R.string.wallet_para_investimentos_encerrados))
        }
    }
    if (confirmDelete && existing != null) {
        ConfirmDialog(
            Res.s(R.string.wallet_apagar_investimento),
            Res.s(R.string.wallet_aportes_resgates_dele_tambem),
            Res.s(R.string.wallet_apagar), onConfirm = {
                confirmDelete = false
                c.launch { c.repo.deleteInvestment(existing); c.message(Res.s(R.string.wallet_investimento_apagado)) }
                nav.back(); nav.back()
            }, onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
fun EmptyWallet() {
    EmptyState(Icons.Rounded.Savings, Res.s(R.string.wallet_nada_aqui), Res.s(R.string.wallet_cadastre_suas_contas_para))
}
