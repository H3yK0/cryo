// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.ui.screens

import androidx.annotation.PluralsRes
import io.github.h3yk0.cryo.R
import io.github.h3yk0.cryo.domain.Res
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Celebration
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Handshake
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.MoneyOff
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.RequestQuote
import androidx.compose.material.icons.rounded.Unarchive
import androidx.compose.material3.Button
import androidx.compose.material3.ExtendedFloatingActionButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.h3yk0.cryo.data.Palette
import io.github.h3yk0.cryo.data.db.Debt
import io.github.h3yk0.cryo.data.db.DebtAdjustment
import io.github.h3yk0.cryo.data.db.DebtKind
import io.github.h3yk0.cryo.data.db.Tx
import io.github.h3yk0.cryo.domain.DebtInfo
import io.github.h3yk0.cryo.domain.DebtMath
import io.github.h3yk0.cryo.domain.DebtOverview
import io.github.h3yk0.cryo.domain.DebtState
import io.github.h3yk0.cryo.domain.Dates
import io.github.h3yk0.cryo.domain.Ledger
import io.github.h3yk0.cryo.domain.Money
import io.github.h3yk0.cryo.domain.dayClamped
import io.github.h3yk0.cryo.domain.key
import io.github.h3yk0.cryo.domain.ym
import io.github.h3yk0.cryo.domain.ymOf
import io.github.h3yk0.cryo.ui.LocalNav
import io.github.h3yk0.cryo.ui.Routes
import io.github.h3yk0.cryo.ui.components.AccountChips
import io.github.h3yk0.cryo.ui.components.AmountField
import io.github.h3yk0.cryo.ui.components.Banner
import io.github.h3yk0.cryo.ui.components.ChoiceRow
import io.github.h3yk0.cryo.ui.components.ColorChooser
import io.github.h3yk0.cryo.ui.components.ConfirmDialog
import io.github.h3yk0.cryo.ui.components.CryoCard
import io.github.h3yk0.cryo.ui.components.FlatItem
import io.github.h3yk0.cryo.ui.components.HeroBlock
import io.github.h3yk0.cryo.ui.components.CryoTopBar
import io.github.h3yk0.cryo.ui.components.DateChooser
import io.github.h3yk0.cryo.ui.components.DatePickerDialogField
import io.github.h3yk0.cryo.ui.components.EmptyState
import io.github.h3yk0.cryo.ui.components.FieldLabel
import io.github.h3yk0.cryo.ui.components.IconBadge
import io.github.h3yk0.cryo.ui.components.LocalContainer
import io.github.h3yk0.cryo.ui.components.LocalSettings
import io.github.h3yk0.cryo.ui.components.MoneyText
import io.github.h3yk0.cryo.ui.components.MonthSelector
import io.github.h3yk0.cryo.ui.components.Pill
import io.github.h3yk0.cryo.ui.components.SectionTitle
import io.github.h3yk0.cryo.ui.components.Segmented
import io.github.h3yk0.cryo.ui.components.Stepper
import io.github.h3yk0.cryo.ui.components.SwitchRow
import io.github.h3yk0.cryo.ui.components.TxRow
import io.github.h3yk0.cryo.ui.components.UsageBar
import io.github.h3yk0.cryo.ui.components.rememberLedger
import io.github.h3yk0.cryo.ui.theme.CryoTheme
import io.github.h3yk0.cryo.ui.tx.LoadingBox
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.max
import kotlin.math.roundToInt

/* ============================== Textos e ícones ============================== */

fun debtKindName(k: DebtKind) = when (k) {
    DebtKind.LOAN -> Res.s(R.string.debt_emprestimo)
    DebtKind.FINANCING -> Res.s(R.string.debt_financiamento)
    DebtKind.CARD -> Res.s(R.string.debt_cartao_ou_cheque_especial)
    DebtKind.AGREEMENT -> Res.s(R.string.debt_acordo)
    DebtKind.PERSON -> Res.s(R.string.debt_pessoa_amigo_ou_familia)
    DebtKind.OTHER -> Res.s(R.string.wallet_outra)
}

fun debtKindShort(k: DebtKind) = when (k) {
    DebtKind.LOAN -> Res.s(R.string.debt_emprestimo)
    DebtKind.FINANCING -> Res.s(R.string.debt_financiamento)
    DebtKind.CARD -> Res.s(R.string.debt_cartao_cheque_especial)
    DebtKind.AGREEMENT -> Res.s(R.string.debt_acordo)
    DebtKind.PERSON -> Res.s(R.string.debt_pessoa)
    DebtKind.OTHER -> Res.s(R.string.wallet_outra)
}

fun debtKindIcon(k: DebtKind): ImageVector = when (k) {
    DebtKind.LOAN -> Icons.Rounded.RequestQuote
    DebtKind.FINANCING -> Icons.Rounded.Key
    DebtKind.CARD -> Icons.Rounded.CreditCard
    DebtKind.AGREEMENT -> Icons.Rounded.Handshake
    DebtKind.PERSON -> Icons.Rounded.People
    DebtKind.OTHER -> Icons.Rounded.MoneyOff
}

/** "1 parcela" / "3 parcelas", com as regras de plural de cada língua (RF-LNG-04). */
internal fun plural(@PluralsRes id: Int, n: Int) = Res.p(id, n, n)

/** Mês por extenso, curto: "mar/2029". */
fun shortMonthYear(ym: YearMonth) = Dates.monthShortYear(ym)

data class StatusLook(val text: String, val container: Color, val content: Color)

@Composable
fun debtStatusLook(info: DebtInfo, today: LocalDate): StatusLook {
    val c = CryoTheme.colors
    val neutral = MaterialTheme.colorScheme.surfaceContainerHighest
    val onNeutral = MaterialTheme.colorScheme.onSurface
    val due = info.nextDue
    return when (info.state) {
        DebtState.PAID_OFF -> StatusLook(Res.s(R.string.debt_quitada), c.incomeContainer, c.onIncomeContainer)
        DebtState.OVERDUE -> StatusLook(
            if (info.hasInstallments) plural(R.plurals.debt_overdue_installments, info.overdueCount) else Res.s(R.string.debt_prazo_vencido),
            c.expenseContainer, c.onExpenseContainer,
        )
        DebtState.DUE_SOON -> StatusLook(Res.s(R.string.debt_vence_2, due?.let { Dates.relative(it, today) }), c.warningContainer, c.onWarningContainer)
        DebtState.PAID_THIS_MONTH -> StatusLook(Res.s(R.string.debt_parcela_mes_paga), c.incomeContainer, c.onIncomeContainer)
        DebtState.UPCOMING -> StatusLook(Res.s(R.string.debt_proxima, due?.let { Dates.short(it) }), neutral, onNeutral)
        DebtState.FREE -> StatusLook(if (due != null) Res.s(R.string.debt_prazo, Dates.short(due)) else Res.s(R.string.debt_sem_prazo), neutral, onNeutral)
    }
}

private fun fmt(v: Long, hide: Boolean) = if (hide) Money.HIDDEN else Money.format(v)

/* ================================ Lista ================================ */

@Composable
fun DebtsScreen() {
    val l = rememberLedger() ?: return LoadingBox()
    val nav = LocalNav.current
    val hide = LocalSettings.current.hideValues
    val ov = l.debtOverview
    val closed = l.s.debts.filter { it.archived || l.debtInfo(it).isPaidOff }.map { l.debtInfo(it) }
    var showClosed by rememberSaveable { mutableStateOf(false) }
    var paying by remember { mutableStateOf<DebtInfo?>(null) }

    Scaffold(
        topBar = { CryoTopBar(Res.s(R.string.home_shortcut_debts), onBack = nav::back) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { nav.go(Routes.debtEdit()) },
                icon = { Icon(Icons.Rounded.Add, null) }, text = { Text(Res.s(R.string.debt_nova_divida)) },
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { pad ->
        LazyColumn(
            Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (ov.open.isEmpty() && closed.isEmpty()) {
                item {
                    EmptyState(
                        Icons.Rounded.RequestQuote, Res.s(R.string.debt_organize_suas_dividas),
                        Res.s(R.string.debt_cadastre_emprestimos_financiamentos_acordos) +
                            Res.s(R.string.debt_cryo_mostra_quanto_falta),
                    ) { Button(onClick = { nav.go(Routes.debtEdit()) }) { Text(Res.s(R.string.debt_cadastrar_divida)) } }
                }
            } else {
                item { DebtHero(ov, hide) }
                if (ov.overdueCount > 0) {
                    item {
                        Banner(
                            Icons.Rounded.ErrorOutline,
                            Res.s(R.string.debt_overdue_hint, plural(R.plurals.debt_overdue_installments, ov.overdueCount)),
                            CryoTheme.colors.expenseContainer, CryoTheme.colors.onExpenseContainer,
                        )
                    }
                }
                if (ov.monthlyInstallments > 0) item { IncomeShareCard(ov, l.averageIncome, hide) }
                if (ov.open.isNotEmpty()) item { SectionTitle(Res.s(R.string.debt_aberto)) }
                items(ov.open, key = { it.debt.id }) { info -> DebtCard(info, l, hide, onPay = { paying = info }) }
                if (ov.open.isEmpty()) {
                    item {
                        CryoCard(color = CryoTheme.colors.incomeContainer) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Celebration, null, tint = CryoTheme.colors.onIncomeContainer)
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    Res.s(R.string.debt_nenhuma_divida_aberto_muito),
                                    style = MaterialTheme.typography.titleSmall, color = CryoTheme.colors.onIncomeContainer,
                                )
                            }
                        }
                    }
                }
                if (closed.isNotEmpty()) {
                    item {
                        TextButton(onClick = { showClosed = !showClosed }, modifier = Modifier.fillMaxWidth()) {
                            Icon(if (showClosed) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, null)
                            Spacer(Modifier.width(8.dp))
                            Text(Res.s(R.string.debt_quitadas_arquivadas, closed.size))
                        }
                    }
                    if (showClosed) items(closed, key = { "c${it.debt.id}" }) { info -> ClosedDebtRow(info, hide) }
                }
            }
        }
    }
    paying?.let { info -> DebtPaySheet(info, l, payoff = false, onDismiss = { paying = null }) }
}

@Composable
private fun DebtHero(ov: DebtOverview, hide: Boolean) {
    // Direção 2.0: o número em destaque sem fundo vermelho (calmo, PRI-06); o vermelho fica para o que está atrasado.
    val fg = MaterialTheme.colorScheme.onSurface
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(Res.s(R.string.debt_voce_deve), style = MaterialTheme.typography.labelLarge, color = fg)
        MoneyText(ov.total, style = MaterialTheme.typography.displaySmall, color = fg)
        Text(
            if (ov.open.isEmpty()) Res.s(R.string.debt_nenhuma_divida_aberto) else Res.s(R.string.debt_in_count, plural(R.plurals.debt_count, ov.open.size)),
            style = MaterialTheme.typography.bodyMedium, color = fg,
        )
        if (ov.paidAll > 0) {
            Spacer(Modifier.height(12.dp))
            UsageBar(ov.progress, baseColor = CryoTheme.colors.income, warn = false, height = 8.dp)
            Text(
                Res.s(R.string.debt_ja_pagou_por_cento_2, fmt(ov.paidAll, hide), (ov.progress * 100).roundToInt()),
                style = MaterialTheme.typography.bodySmall, color = fg, modifier = Modifier.padding(top = 4.dp),
            )
        }
        if (ov.monthlyInstallments > 0 || ov.freeOfDebtYm != null) {
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = fg.copy(alpha = 0.2f))
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth()) {
                if (ov.monthlyInstallments > 0) {
                    Column(Modifier.weight(1f)) {
                        Text(Res.s(R.string.debt_parcelas_por_mes), style = MaterialTheme.typography.labelMedium, color = fg)
                        MoneyText(ov.monthlyInstallments, style = MaterialTheme.typography.titleSmall, color = fg)
                    }
                }
                ov.freeOfDebtYm?.let { end ->
                    Column(Modifier.weight(1f)) {
                        Text(Res.s(R.string.debt_livre_das_dividas), style = MaterialTheme.typography.labelMedium, color = fg)
                        Text(shortMonthYear(end), style = MaterialTheme.typography.titleSmall, color = fg)
                    }
                }
            }
        }
    }
}

@Composable
private fun IncomeShareCard(ov: DebtOverview, avgIncome: Long, hide: Boolean) {
    CryoCard {
        if (avgIncome <= 0) {
            Text(
                Res.s(R.string.debt_suas_parcelas_somam_por, fmt(ov.monthlyInstallments, hide)),
                style = MaterialTheme.typography.bodyMedium,
            )
            return@CryoCard
        }
        val ratio = ov.monthlyInstallments.toFloat() / avgIncome
        val pct = (ratio * 100).roundToInt()
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(Res.s(R.string.debt_quanto_sua_renda_vai), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            val c = CryoTheme.colors
            val (bg, fg) = when {
                ratio > 0.5f -> c.expenseContainer to c.onExpenseContainer
                ratio > 0.3f -> c.warningContainer to c.onWarningContainer
                else -> c.incomeContainer to c.onIncomeContainer
            }
            Pill("$pct%", bg, fg)
        }
        Spacer(Modifier.height(8.dp))
        UsageBar(ratio, warn = false, baseColor = MaterialTheme.colorScheme.primary)
        Text(
            Res.s(R.string.debt_parcelas_por_mes_que, fmt(ov.monthlyInstallments, hide), fmt(avgIncome, hide)),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
fun DebtCard(info: DebtInfo, l: Ledger, hide: Boolean, onPay: () -> Unit) {
    val nav = LocalNav.current
    val d = info.debt
    val look = debtStatusLook(info, l.today)
    FlatItem(onClick = { nav.go(Routes.debt(d.id)) }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(debtKindIcon(d.kind), Color(d.color))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(d.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    listOf(debtKindShort(d.kind), d.creditor).filter { it.isNotBlank() }.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                MoneyText(info.remaining, style = MaterialTheme.typography.titleMedium)
                Text(Res.s(R.string.debt_falta_pagar_3), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(10.dp))
        UsageBar(info.progress, baseColor = Color(d.color), warn = false, height = 8.dp)
        Text(
            if (info.hasInstallments) {
                Res.s(R.string.debt_parcelas_pagas_por_mes, info.installmentsPaid, info.installmentsTotal, fmt(d.installmentAmount, hide))
            } else {
                Res.s(R.string.debt_pagou, fmt(info.paid, hide)) + (info.monthlyNeeded?.let { Res.s(R.string.debt_por_mes_para_quitar, fmt(it, hide)) } ?: "")
            },
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Pill(look.text, look.container, look.content)
            Spacer(Modifier.weight(1f))
            if (!info.isPaidOff) {
                FilledTonalButton(onClick = onPay, contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
                    Text(if (info.hasInstallments && info.state != DebtState.PAID_THIS_MONTH) Res.s(R.string.debt_paguei) else Res.s(R.string.debt_pagar_2))
                }
            }
        }
    }
}

@Composable
private fun ClosedDebtRow(info: DebtInfo, hide: Boolean) {
    val nav = LocalNav.current
    val d = info.debt
    FlatItem(onClick = { nav.go(Routes.debt(d.id)) }, faded = true) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(debtKindIcon(d.kind), Color(d.color).copy(alpha = 0.5f), 36.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(d.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    when {
                        info.isPaidOff -> Res.s(R.string.debt_quitada) + (info.lastPayment?.let { " em ${Dates.full(it)}" } ?: "")
                        else -> Res.s(R.string.debt_arquivada_faltavam, fmt(info.remaining, hide))
                    },
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            MoneyText(info.paidAll, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/* ================================ Detalhe ================================ */

@Composable
fun DebtDetailScreen(id: Long) {
    val l = rememberLedger() ?: return LoadingBox()
    val nav = LocalNav.current
    val c = LocalContainer.current
    val hide = LocalSettings.current.hideValues
    val d = l.debt[id] ?: return LoadingBox()
    val info = l.debtInfo(d)
    var action by rememberSaveable { mutableStateOf<String?>(null) }
    val paidOff = info.isPaidOff
    val fg = MaterialTheme.colorScheme.onSurface

    Scaffold(
        topBar = {
            CryoTopBar(d.name, onBack = nav::back, actions = {
                IconButton(onClick = { nav.go(Routes.debtEdit(id)) }) { Icon(Icons.Rounded.Edit, Res.s(R.string.debt_editar_divida)) }
            })
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { pad ->
        LazyColumn(
            Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(bottom = 32.dp),
        ) {
            item {
                HeroBlock {
                    if (paidOff) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Celebration, null, tint = fg, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(Res.s(R.string.debt_divida_quitada_parabens_2), style = MaterialTheme.typography.labelLarge, color = fg)
                        }
                        MoneyText(info.paidAll, style = MaterialTheme.typography.displaySmall, color = fg)
                        Text(
                            (listOf(Res.s(R.string.debt_pagos_total)) + listOf(debtKindShort(d.kind), d.creditor).filter { it.isNotBlank() }).joinToString(" · "),
                            style = MaterialTheme.typography.bodyMedium, color = fg,
                        )
                    } else {
                        Text(Res.s(R.string.debt_falta_pagar), style = MaterialTheme.typography.labelLarge, color = fg)
                        MoneyText(info.remaining, style = MaterialTheme.typography.displaySmall, color = fg)
                        Text(
                            listOf(debtKindShort(d.kind), d.creditor).filter { it.isNotBlank() }.joinToString(" · "),
                            style = MaterialTheme.typography.bodyMedium, color = fg,
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    UsageBar(info.progress, baseColor = CryoTheme.colors.income, warn = false, height = 8.dp)
                    Text(
                        if (info.hasInstallments) {
                            Res.s(R.string.debt_parcelas_pagas_cada, info.installmentsPaid, info.installmentsTotal, fmt(d.installmentAmount, hide))
                        } else {
                            Res.s(R.string.debt_ja_pagou, fmt(info.paid, hide))
                        },
                        style = MaterialTheme.typography.bodySmall, color = fg, modifier = Modifier.padding(top = 4.dp),
                    )
                    if (!info.isPaidOff) {
                        val end = when {
                            info.hasInstallments -> info.payoffYm?.let {
                                Res.s(R.string.debt_termina, Dates.monthYearInSentence(it, l.today), plural(R.plurals.debt_installments_left, info.installmentsLeft))
                            }
                            d.deadline != null -> Res.s(R.string.debt_prazo, Dates.full(d.deadline)) +
                                (info.monthlyNeeded?.let { Res.s(R.string.debt_por_mes, fmt(it, hide)) } ?: "")
                            else -> null
                        }
                        if (end != null) Text(end, style = MaterialTheme.typography.bodyMedium, color = fg, modifier = Modifier.padding(top = 6.dp))
                    }
                    if (paidOff && !d.archived) {
                        Spacer(Modifier.height(12.dp))
                        OutlinedButton(onClick = {
                            c.launch {
                                c.repo.saveDebt(d.copy(archived = true))
                                c.message(Res.s(R.string.debt_divida_arquivada_ela_fica), Res.s(R.string.wallet_desfazer)) { c.repo.saveDebt(d.copy(archived = false)) }
                            }
                            nav.back()
                        }) { Icon(Icons.Rounded.Archive, null, tint = fg); Spacer(Modifier.width(8.dp)); Text(Res.s(R.string.wallet_arquivar), color = fg) }
                    }
                }
            }
            if (d.archived) {
                item {
                    Banner(
                        Icons.Rounded.Archive, Res.s(R.string.debt_divida_arquivada_ela_nao),
                        MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer,
                        Modifier.padding(16.dp),
                    )
                }
            }
            if (info.state == DebtState.OVERDUE) {
                item {
                    Banner(
                        Icons.Rounded.ErrorOutline,
                        if (info.hasInstallments) {
                            Res.s(R.string.debt_desde, plural(R.plurals.debt_overdue_installments, info.overdueCount), info.nextDue?.let { Dates.short(it) })
                        } else {
                            Res.s(R.string.debt_prazo_combinado_ja_passou)
                        },
                        CryoTheme.colors.expenseContainer, CryoTheme.colors.onExpenseContainer,
                        Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp),
                    )
                }
            }
            item {
                FlowRow(
                    Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (!info.isPaidOff) {
                        Button(onClick = { action = "pay" }) { Text(if (info.hasInstallments) Res.s(R.string.debt_pagar_parcela) else Res.s(R.string.debt_pagar_2)) }
                        FilledTonalButton(onClick = { action = "payoff" }) { Text(Res.s(R.string.debt_quitar_2)) }
                    }
                    FilledTonalButton(onClick = { action = "adjust" }) { Text(Res.s(R.string.debt_atualizar_saldo)) }
                    FilledTonalButton(onClick = { action = "borrow" }) { Text(Res.s(R.string.debt_peguei_mais)) }
                    if (d.archived) {
                        OutlinedButton(onClick = { c.launch { c.repo.saveDebt(d.copy(archived = false)); c.message(Res.s(R.string.debt_divida_restaurada)) } }) {
                            Icon(Icons.Rounded.Unarchive, null); Spacer(Modifier.width(8.dp)); Text(Res.s(R.string.wallet_restaurar))
                        }
                    }
                }
            }
            if (info.hasInstallments && !info.isPaidOff) {
                item { SectionTitle(Res.s(R.string.debt_proximas_parcelas), Modifier.padding(horizontal = 16.dp)) }
                item { UpcomingInstallments(info, l, hide) }
            }
            item {
                CryoCard(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    InfoLine(Res.s(R.string.wallet_tipo), debtKindName(d.kind))
                    if (d.creditor.isNotBlank()) InfoLine(Res.s(R.string.debt_com_quem), d.creditor)
                    InfoLine(Res.s(R.string.debt_paga_com), d.accountId?.let { l.accountName(it) } ?: "—")
                    if (info.hasInstallments && !paidOff) {
                        InfoLine(Res.s(R.string.debt_vencimento), Res.s(R.string.debt_todo_dia, d.dueDay))
                        InfoLine(Res.s(R.string.debt_lembrete), if (d.remind) (if (d.remindDaysBefore == 0) Res.s(R.string.debt_dia_2) else plural(R.plurals.days_before, d.remindDaysBefore)) else "desligado")
                    }
                    if (info.borrowed > 0) InfoLine(Res.s(R.string.debt_pegou_depois_cadastro), fmt(info.borrowed, hide))
                    if (info.adjusted != 0L) InfoLine(if (info.adjusted > 0) Res.s(R.string.debt_juros_encargos) else Res.s(R.string.debt_descontos), fmt(kotlin.math.abs(info.adjusted), hide))
                    if (d.note.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text(d.note, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            item { SectionTitle(Res.s(R.string.wallet_historico), Modifier.padding(horizontal = 16.dp)) }
            val hist = l.debtHistory(id)
            if (hist.isEmpty()) {
                item {
                    Text(
                        Res.s(R.string.debt_nenhum_pagamento_registrado_ainda), Modifier.padding(horizontal = 20.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(hist.size) { i ->
                when (val h = hist[i]) {
                    is Tx -> TxRow(h, l, onClick = { nav.go(Routes.txEdit(h.id)) }, showDate = true)
                    is DebtAdjustment -> AdjustmentRow(h, hide, l.today, onDelete = {
                        c.launch { c.repo.deleteDebtAdjustment(h); c.message(Res.s(R.string.debt_ajuste_removido)) }
                    })
                }
            }
        }
    }

    when (action) {
        "pay" -> DebtPaySheet(info, l, payoff = false, onDismiss = { action = null })
        "payoff" -> DebtPaySheet(info, l, payoff = true, onDismiss = { action = null })
        "adjust" -> MoneyActionSheet(
            title = Res.s(R.string.debt_atualizar_quanto_falta),
            subtitle = Res.s(R.string.debt_veja_valor_app_banco),
            initialAmount = info.remaining, l = l, accountLabel = null, initialAccount = null,
            confirmLabel = Res.s(R.string.wallet_atualizar), amountLabel = Res.s(R.string.debt_falta_pagar_hoje),
            onDismiss = { action = null },
            onConfirm = { amount, _, date ->
                action = null
                val diff = amount - info.outstanding
                c.launch {
                    val u = c.repo.adjustDebt(d, diff, date, if (diff > 0) Res.s(R.string.debt_juros_encargos) else Res.s(R.string.debt_desconto))
                    if (u.adjustments.isNotEmpty()) c.message(Res.s(R.string.debt_saldo_divida_atualizado), Res.s(R.string.wallet_desfazer)) { c.repo.undo(u) }
                }
            },
        )
        "borrow" -> MoneyActionSheet(
            title = Res.s(R.string.debt_peguei_mais_emprestado),
            subtitle = Res.s(R.string.debt_dinheiro_entra_conta_aumenta),
            initialAmount = 0, l = l, accountLabel = Res.s(R.string.debt_entrou_conta),
            initialAccount = d.accountId ?: LocalSettings.current.defaultAccountId, confirmLabel = Res.s(R.string.debt_registrar),
            onDismiss = { action = null },
            onConfirm = { amount, acc, date ->
                action = null
                c.launch {
                    val u = c.repo.borrowMore(d, amount, acc!!, date)
                    c.message(Res.s(R.string.debt_somados_divida, Money.format(amount)), Res.s(R.string.wallet_desfazer)) { c.repo.undo(u) }
                }
            },
        )
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun UpcomingInstallments(info: DebtInfo, l: Ledger, hide: Boolean) {
    val d = info.debt
    val start = ymOf(d.startYm)
    val kPaid = info.installmentsPaid - d.paidBefore
    val shown = minOf(4, info.installmentsLeft)
    CryoCard(Modifier.padding(horizontal = 16.dp)) {
        for (i in 0 until shown) {
            val k = kPaid + i
            val due = start.plusMonths(k.toLong()).dayClamped(d.dueDay)
            val amount = if (i == 0) info.nextAmount else minOf(d.installmentAmount, info.remaining - info.nextAmount - (i - 1L) * d.installmentAmount)
            val c = CryoTheme.colors
            val (text, color) = when {
                due.isBefore(l.today) -> Res.s(R.string.debt_atrasada) to c.expense
                due.ym() == l.today.ym() -> Res.s(R.string.debt_este_mes) to c.warning
                else -> "" to MaterialTheme.colorScheme.onSurfaceVariant
            }
            if (i > 0) HorizontalDivider(Modifier.padding(vertical = 6.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(Res.s(R.string.debt_parcela_3, d.paidBefore + k + 1, info.installmentsTotal), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        Dates.dayMonth(due) + if (due.year != l.today.year) " de ${due.year}" else "",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (text.isNotEmpty()) Text(text, style = MaterialTheme.typography.labelLarge, color = color, modifier = Modifier.padding(end = 12.dp))
                Text(fmt(max(0, amount), hide), style = MaterialTheme.typography.titleSmall)
            }
        }
        if (info.installmentsLeft > shown) {
            Text(
                Res.s(R.string.debt_mais_ate, info.installmentsLeft - shown, info.payoffYm?.let { Dates.monthYearInSentence(it, l.today) }),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun AdjustmentRow(a: DebtAdjustment, hide: Boolean, today: LocalDate, onDelete: () -> Unit) {
    var ask by remember { mutableStateOf(false) }
    val up = a.amount > 0
    val title = a.note.ifBlank { if (up) Res.s(R.string.debt_juros_encargos) else Res.s(R.string.debt_desconto) }
    Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        IconBadge(if (up) Icons.AutoMirrored.Rounded.TrendingUp else Icons.AutoMirrored.Rounded.TrendingDown, if (up) CryoTheme.colors.expense else CryoTheme.colors.income)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                (if (a.date.year == today.year) Dates.short(a.date) else Dates.full(a.date)) +
                    if (up) Res.s(R.string.debt_aumentou_divida) else Res.s(R.string.debt_diminuiu_divida),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            if (hide) Money.HIDDEN else (if (up) "+ " else "− ") + Money.format(kotlin.math.abs(a.amount)),
            style = MaterialTheme.typography.titleSmall, color = if (up) CryoTheme.colors.expense else CryoTheme.colors.income,
        )
        IconButton(onClick = { ask = true }) { Icon(Icons.Rounded.Close, Res.s(R.string.debt_remover_ajuste_2, title)) }
    }
    if (ask) ConfirmDialog(Res.s(R.string.debt_remover_ajuste), Res.s(R.string.debt_divida_volta_ao_valor), Res.s(R.string.wallet_remover), { ask = false; onDelete() }, { ask = false })
}

/* ============================== Pagamento ============================== */

/**
 * Pagar parcela/valor ou quitar.
 * - Pagou mais que a parcela: a pessoa escolhe se a diferença foi juros/multa ou se foi para adiantar a dívida.
 * - Quitar por menos do que falta: a diferença vira desconto.
 */
@Composable
fun DebtPaySheet(info: DebtInfo, l: Ledger, payoff: Boolean, onDismiss: () -> Unit) {
    val c = LocalContainer.current
    val settings = LocalSettings.current
    val d = info.debt
    val remaining = info.remaining
    val suggested = when {
        payoff -> remaining
        info.hasInstallments -> info.nextAmount
        else -> info.monthlyNeeded ?: 0L
    }
    var amount by remember { mutableLongStateOf(suggested) }
    var acc by remember {
        mutableStateOf(
            d.accountId?.takeIf { id -> l.activeAccounts.any { it.id == id } } ?: settings.defaultAccountId
                ?: l.activeAccounts.firstOrNull()?.id,
        )
    }
    var date by remember { mutableStateOf(l.today) }
    var extraIsInterest by remember { mutableStateOf(true) }
    val installmentPart = if (info.hasInstallments) info.nextAmount else remaining
    // Passou do total: a diferença é juros/multa. Passou só da parcela: a pessoa diz se foi juros ou adiantamento.
    val overTotal = amount - remaining
    val overInstallment = amount - minOf(installmentPart, remaining)
    val asksExtra = !payoff && info.hasInstallments && overInstallment > 0 && amount < remaining

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.verticalScroll(rememberScrollState()).imePadding().padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
            Text(
                when {
                    payoff -> Res.s(R.string.debt_quitar, d.name)
                    info.hasInstallments -> Res.s(R.string.debt_pagar_parcela)
                    else -> Res.s(R.string.debt_pagar, d.name)
                },
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                when {
                    payoff -> Res.s(R.string.debt_falta_pagar_se_banco, Money.format(remaining))
                    info.hasInstallments ->
                        Res.s(R.string.debt_parcela_2, info.installmentsPaid + 1, info.installmentsTotal, d.name) +
                            (info.nextDue?.let { Res.s(R.string.debt_vence, Dates.short(it)) } ?: "")
                    else -> Res.s(R.string.debt_falta_pagar_2, Money.format(remaining))
                },
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            AmountField(amount, { amount = it }, label = Res.s(R.string.debt_valor_pago), color = CryoTheme.colors.expense)

            when {
                payoff && amount in 1 until remaining -> Hint(Res.s(R.string.debt_desconto_divida_fica_quitada, Money.format(remaining - amount)))
                overTotal > 0 -> Hint(Res.s(R.string.debt_voce_pagou_mais_que, Money.format(overTotal)))
                !payoff && amount == remaining && remaining > 0 -> Hint(Res.s(R.string.debt_esse_pagamento_quita_divida))
                asksExtra -> {
                    FieldLabel(Res.s(R.string.debt_diferenca_foi, Money.format(overInstallment)))
                    ChoiceRow(Res.s(R.string.debt_juros_ou_multa_por), Res.s(R.string.debt_sai_conta_mas_nao), extraIsInterest) { extraIsInterest = true }
                    ChoiceRow(Res.s(R.string.debt_para_adiantar_divida), Res.s(R.string.debt_diminui_quanto_falta_pagar), !extraIsInterest) { extraIsInterest = false }
                }
            }

            FieldLabel(Res.s(R.string.debt_pago_com_conta))
            AccountChips(l.activeAccounts, acc, { acc = it })
            FieldLabel(Res.s(R.string.wallet_data))
            DateChooser(date, l.today, { date = it })
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = {
                    val account = acc ?: return@Button
                    onDismiss()
                    c.launch {
                        val u = when {
                            payoff && amount < remaining -> c.repo.payDebt(d, amount, account, date, installment = false, discount = remaining - amount)
                            amount > remaining -> c.repo.payDebt(d, remaining, account, date, installment = false, extra = amount - remaining)
                            asksExtra && extraIsInterest ->
                                c.repo.payDebt(d, amount - overInstallment, account, date, installment = true, extra = overInstallment)
                            else -> c.repo.payDebt(
                                d, amount, account, date,
                                installment = info.hasInstallments && !payoff && amount <= installmentPart,
                            )
                        }
                        val quitou = payoff || amount >= remaining
                        c.message(if (quitou) Res.s(R.string.debt_divida_quitada_parabens) else Res.s(R.string.debt_pagamento_registrado), Res.s(R.string.wallet_desfazer)) { c.repo.undo(u) }
                    }
                },
                enabled = amount > 0 && acc != null,
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) {
                Icon(Icons.Rounded.Check, null)
                Spacer(Modifier.width(8.dp))
                Text(if (payoff) Res.s(R.string.debt_quitar_divida) else Res.s(R.string.debt_registrar_pagamento))
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text(Res.s(R.string.wallet_cancelar)) }
        }
    }
}

@Composable
private fun Hint(text: String) {
    Text(
        text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 10.dp),
    )
}

/* ================================ Cadastro ================================ */

@Composable
fun DebtEditorScreen(id: Long?) {
    val l = rememberLedger() ?: return LoadingBox()
    val c = LocalContainer.current
    val nav = LocalNav.current
    val settings = LocalSettings.current
    val existing = id?.let { l.debt[it] }
    val info = existing?.let { l.debtInfo(it) }
    val creating = existing == null

    var name by rememberSaveable { mutableStateOf(existing?.name ?: "") }
    var creditor by rememberSaveable { mutableStateOf(existing?.creditor ?: "") }
    var kind by rememberSaveable { mutableStateOf(existing?.kind ?: DebtKind.LOAN) }
    var installments by rememberSaveable { mutableStateOf(existing?.let { it.installmentAmount > 0 } ?: true) }
    var modeTouched by rememberSaveable { mutableStateOf(!creating) }
    var amountEach by rememberSaveable { mutableLongStateOf(existing?.installmentAmount ?: 0L) }
    var count by rememberSaveable { mutableIntStateOf(existing?.installmentCount?.takeIf { it > 0 } ?: 12) }
    var paidBefore by rememberSaveable { mutableIntStateOf(existing?.paidBefore ?: 0) }
    var dueDay by rememberSaveable { mutableIntStateOf(existing?.dueDay ?: 10) }
    var startKey by rememberSaveable { mutableIntStateOf(existing?.startYm ?: DebtMath.suggestedStart(l.today, 10).key()) }
    var startTouched by rememberSaveable { mutableStateOf(!creating) }
    var deadline by rememberSaveable { mutableStateOf(existing?.deadline?.toEpochDay()) }
    var balance by rememberSaveable { mutableLongStateOf(info?.remaining ?: 0L) }
    // A pessoa mexeu em "quanto falta"? (No cadastro, até lá o valor acompanha as parcelas.)
    var balanceTouched by rememberSaveable { mutableStateOf(false) }
    var received by rememberSaveable { mutableStateOf(false) }
    var receivedAmount by rememberSaveable { mutableLongStateOf(0L) }
    val defaultAcc = settings.defaultAccountId?.takeIf { id -> l.activeAccounts.any { it.id == id } } ?: l.activeAccounts.firstOrNull()?.id
    var receivedAcc by rememberSaveable { mutableStateOf(defaultAcc) }
    var payAcc by rememberSaveable { mutableStateOf(existing?.accountId ?: defaultAcc) }
    var remind by rememberSaveable { mutableStateOf(existing?.remind ?: true) }
    var remindBefore by rememberSaveable { mutableIntStateOf(existing?.remindDaysBefore ?: 1) }
    var color by rememberSaveable { mutableIntStateOf(existing?.color ?: Palette.pick(l.s.debts.size * 3 + 1)) }
    var note by rememberSaveable { mutableStateOf(existing?.note ?: "") }
    var pickDate by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    var confirmArchive by rememberSaveable { mutableStateOf(false) }

    // No cadastro, "quanto falta" acompanha as parcelas até a pessoa mexer no valor.
    val computed = if (installments) (count - paidBefore).coerceAtLeast(0).toLong() * amountEach else balance
    if (creating && installments && !balanceTouched && balance != computed) balance = computed
    if (creating && !startTouched) {
        val s = DebtMath.suggestedStart(l.today, dueDay).key()
        if (s != startKey) startKey = s
    }

    val problem = when {
        name.isBlank() -> Res.s(R.string.debt_nome_para_divida)
        installments && amountEach <= 0 -> Res.s(R.string.debt_informe_valor_parcela)
        installments && paidBefore >= count -> Res.s(R.string.debt_parcelas_pagas_precisam_ser)
        creating && balance <= 0 -> Res.s(R.string.debt_informe_quanto_falta_pagar)
        creating && received && (receivedAmount <= 0 || receivedAcc == null) -> Res.s(R.string.debt_informe_quanto_entrou_qual)
        creating && received && receivedAmount > balance -> Res.s(R.string.debt_valor_que_entrou_nao)
        else -> null
    }

    EditorScaffold(
        title = if (creating) Res.s(R.string.debt_nova_divida) else Res.s(R.string.debt_editar_divida),
        canSave = problem == null,
        onSave = {
            val base = (existing ?: Debt(name = name, color = color, initialBalance = 0, startYm = startKey)).copy(
                name = name.trim(), creditor = creditor.trim(), kind = kind, color = color,
                installmentAmount = if (installments) amountEach else 0,
                installmentCount = if (installments) count else 0,
                paidBefore = if (installments) paidBefore else 0,
                dueDay = dueDay, startYm = startKey,
                deadline = if (!installments) deadline?.let { LocalDate.ofEpochDay(it) } else null,
                accountId = payAcc, remind = remind, remindDaysBefore = remindBefore, note = note.trim(),
            )
            c.launch {
                if (existing == null) {
                    val r = if (received) receivedAmount else 0L
                    c.repo.createDebt(base.copy(initialBalance = balance - r), r, receivedAcc, l.today)
                    c.message(Res.s(R.string.debt_divida_cadastrada))
                } else {
                    val noHistory = info!!.paid == 0L && info.borrowed == 0L && info.adjusted == 0L
                    when {
                        !balanceTouched -> c.repo.saveDebt(base)
                        noHistory -> c.repo.saveDebt(base.copy(initialBalance = balance))
                        else -> {
                            c.repo.saveDebt(base)
                            val diff = balance - info.outstanding
                            if (diff != 0L) c.repo.adjustDebt(base, diff, l.today, Res.s(R.string.debt_correcao_saldo))
                        }
                    }
                    c.message(Res.s(R.string.debt_divida_atualizada))
                }
            }
            nav.back()
        },
        onDelete = existing?.let { { confirmDelete = true } },
    ) {
        OutlinedTextField(
            name, { name = it }, label = { Text(Res.s(R.string.debt_nome_divida)) }, singleLine = true,
            placeholder = { Text(Res.s(R.string.debt_ex_financiamento_moto_devo)) },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences), modifier = Modifier.fillMaxWidth(),
        )
        FieldLabel(Res.s(R.string.wallet_tipo))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DebtKind.entries.forEach { k ->
                FilterChip(
                    selected = kind == k,
                    onClick = {
                        kind = k
                        if (!modeTouched) installments = k != DebtKind.PERSON && k != DebtKind.CARD
                    },
                    label = { Text(debtKindShort(k)) },
                    leadingIcon = { Icon(debtKindIcon(k), null, Modifier.size(18.dp)) },
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            creditor, { creditor = it }, label = { Text(Res.s(R.string.debt_com_quem_opcional)) }, singleLine = true,
            placeholder = { Text(Res.s(R.string.debt_ex_banco_loja_ou)) },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words), modifier = Modifier.fillMaxWidth(),
        )

        FieldLabel(Res.s(R.string.debt_como_voce_paga))
        Segmented(listOf(true to Res.s(R.string.debt_parcelas_fixas), false to Res.s(R.string.debt_quando_puder)), installments, { installments = it; modeTouched = true })

        if (installments) {
            FieldLabel(Res.s(R.string.debt_valor_cada_parcela))
            AmountField(amountEach, { amountEach = it }, label = Res.s(R.string.debt_parcela))
            Spacer(Modifier.height(8.dp))
            Stepper(count, { count = it; if (paidBefore >= it) paidBefore = it - 1 }, 1..600, Res.s(R.string.debt_total_parcelas))
            Stepper(paidBefore, { paidBefore = it }, 0..(count - 1).coerceAtLeast(0), Res.s(R.string.debt_ja_pagas))
            Stepper(dueDay, { dueDay = it }, 1..31, Res.s(R.string.debt_vence_todo_dia))
            FieldLabel(Res.s(R.string.debt_proxima_parcela_pagar))
            val start = ymOf(startKey)
            MonthSelector(
                "${start.dayClamped(dueDay).dayOfMonth} de ${Dates.monthYearInSentence(start, l.today)}",
                onPrev = { startKey = start.minusMonths(1).key(); startTouched = true },
                onNext = { startKey = start.plusMonths(1).key(); startTouched = true },
            )
        } else {
            FieldLabel(Res.s(R.string.debt_prazo_para_quitar_opcional))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    deadline?.let { Dates.full(LocalDate.ofEpochDay(it)) } ?: Res.s(R.string.debt_sem_prazo),
                    style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f).clickable { pickDate = true },
                )
                TextButton(onClick = { pickDate = true }) { Text(if (deadline == null) Res.s(R.string.debt_definir) else Res.s(R.string.debt_mudar)) }
                if (deadline != null) TextButton(onClick = { deadline = null }) { Text(Res.s(R.string.debt_tirar)) }
            }
        }

        FieldLabel(if (creating) Res.s(R.string.debt_quanto_falta_pagar) else Res.s(R.string.debt_falta_pagar_hoje))
        AmountField(balance, { balance = it; balanceTouched = true }, label = Res.s(R.string.debt_falta_pagar), color = CryoTheme.colors.expense)
        Text(
            when {
                !creating -> Res.s(R.string.debt_se_voce_mudar_este)
                installments && amountEach <= 0 -> Res.s(R.string.debt_preencha_valor_parcela_total)
                installments && !balanceTouched ->
                    Res.s(R.string.debt_calculado_parcelas_ajuste_se, count - paidBefore, Money.format(amountEach))
                installments -> Res.s(R.string.debt_valor_informado_por_voce, Money.format(computed))
                else -> Res.s(R.string.debt_total_que_voce_deve)
            },
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
        if (installments && amountEach > 0 && count > paidBefore) {
            val left = count - paidBefore
            val end = ymOf(startKey).plusMonths((left - 1).toLong())
            Text(
                Res.s(R.string.debt_termina, Dates.monthYearInSentence(end, l.today), plural(R.plurals.debt_installments_ahead, left)),
                style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 6.dp),
            )
        }

        if (creating) {
            Spacer(Modifier.height(8.dp))
            SwitchRow(
                Res.s(R.string.debt_dinheiro_entrou_numa_conta), received, { received = it },
                Res.s(R.string.debt_use_quando_acabou_pegar),
            )
            if (received) {
                AmountField(receivedAmount, { receivedAmount = it }, label = Res.s(R.string.debt_quanto_entrou))
                FieldLabel(Res.s(R.string.debt_entrou_conta))
                AccountChips(l.activeAccounts, receivedAcc, { receivedAcc = it })
            }
        }

        FieldLabel(Res.s(R.string.wallet_conta_usada_para_pagar))
        AccountChips(l.activeAccounts, payAcc, { payAcc = it })
        if (installments) {
            Spacer(Modifier.height(8.dp))
            SwitchRow(Res.s(R.string.debt_lembrar_vencimento), remind, { remind = it }, Res.s(R.string.debt_notificacao_celular))
            if (remind) {
                Stepper(remindBefore, { remindBefore = it }, 0..7, Res.s(R.string.debt_avisar_antes), format = { if (it == 0) Res.s(R.string.debt_dia) else Res.p(R.plurals.count_days, it, it) })
            }
            Text(
                Res.s(R.string.debt_nao_precisa_criar_conta),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        FieldLabel(Res.s(R.string.wallet_cor))
        ColorChooser(color) { color = it }
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            note, { note = it }, label = { Text(Res.s(R.string.debt_observacao_opcional)) }, minLines = 2,
            placeholder = { Text(Res.s(R.string.debt_ex_juros_ao_mes)) }, modifier = Modifier.fillMaxWidth(),
        )
        if (problem != null && name.isNotBlank()) {
            Text(problem, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 12.dp))
        }
        if (existing != null) {
            Spacer(Modifier.height(16.dp))
            if (existing.archived) {
                OutlinedButton(onClick = {
                    c.launch { c.repo.saveDebt(existing.copy(archived = false)); c.message(Res.s(R.string.debt_divida_restaurada)) }
                    nav.back()
                }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Rounded.Unarchive, null); Spacer(Modifier.width(8.dp)); Text(Res.s(R.string.debt_restaurar_divida)) }
            } else {
                OutlinedButton(onClick = { confirmArchive = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.Archive, null); Spacer(Modifier.width(8.dp)); Text(Res.s(R.string.debt_arquivar_divida_2))
                }
            }
        }
    }

    if (pickDate) {
        DatePickerDialogField(
            deadline?.let { LocalDate.ofEpochDay(it) } ?: l.today.plusMonths(3),
            onDismiss = { pickDate = false }, onPick = { deadline = it.toEpochDay(); pickDate = false },
        )
    }
    if (confirmArchive && existing != null) {
        ConfirmDialog(
            Res.s(R.string.debt_arquivar_divida),
            if (info!!.remaining > 0) {
                Res.s(R.string.debt_ela_ainda_tem_aberto, Money.format(info.remaining))
            } else {
                Res.s(R.string.debt_ela_vai_para_dividas)
            },
            Res.s(R.string.wallet_arquivar),
            onConfirm = {
                confirmArchive = false
                c.launch {
                    c.repo.saveDebt(existing.copy(archived = true))
                    c.message(Res.s(R.string.debt_divida_arquivada), Res.s(R.string.wallet_desfazer)) { c.repo.saveDebt(existing.copy(archived = false)) }
                }
                nav.back(); nav.back()
            },
            onDismiss = { confirmArchive = false },
        )
    }
    if (confirmDelete && existing != null) {
        ConfirmDialog(
            Res.s(R.string.debt_apagar_divida),
            Res.s(R.string.debt_pagamentos_ja_feitos_continuam),
            Res.s(R.string.wallet_apagar),
            onConfirm = {
                confirmDelete = false
                c.launch { c.repo.deleteDebt(existing); c.message(Res.s(R.string.debt_divida_apagada)) }
                nav.back(); nav.back()
            },
            onDismiss = { confirmDelete = false },
        )
    }
}

/* ======================= Resumo usado em outras telas ======================= */

/** Cartão compacto de dívidas (Início e Carteira). */
@Composable
fun DebtSummaryCard(l: Ledger, hide: Boolean, onClick: () -> Unit) {
    CryoCard(onClick = onClick) { DebtSummaryContent(l, hide) }
}

/** O resumo das dívidas (quanto deve, quanto já pagou, próxima parcela): no cartão da Carteira e na seção do Início. */
@Composable
fun DebtSummaryContent(l: Ledger, hide: Boolean) {
    val ov = l.debtOverview
    val next = ov.open.filter { it.nextDue != null && it.hasInstallments }.minByOrNull { it.nextDue!! }
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(Icons.Rounded.RequestQuote, CryoTheme.colors.expense)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(Res.s(R.string.debt_voce_deve), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                MoneyText(ov.total, style = MaterialTheme.typography.titleLarge)
            }
            Text(plural(R.plurals.debt_count, ov.open.size), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (ov.paidAll > 0) {
            Spacer(Modifier.height(10.dp))
            UsageBar(ov.progress, baseColor = CryoTheme.colors.income, warn = false, height = 6.dp)
            Text(
                Res.s(R.string.debt_ja_pagou_por_cento, (ov.progress * 100).roundToInt()) + (ov.freeOfDebtYm?.let { Res.s(R.string.debt_livre, shortMonthYear(it)) } ?: ""),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        if (next != null) {
            Spacer(Modifier.height(10.dp))
            val look = debtStatusLook(next, l.today)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(Res.s(R.string.debt_proxima_parcela), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "${next.debt.name} · ${fmt(next.nextAmount, hide)}",
                        style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.width(8.dp))
                Pill(look.text, look.container, look.content)
            }
        }
    }
}
