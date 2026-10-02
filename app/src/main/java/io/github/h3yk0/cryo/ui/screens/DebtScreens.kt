// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.ui.screens

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
import androidx.compose.foundation.selection.selectable
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
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.semantics.Role
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
import io.github.h3yk0.cryo.ui.components.ColorChooser
import io.github.h3yk0.cryo.ui.components.ConfirmDialog
import io.github.h3yk0.cryo.ui.components.CryoCard
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
    DebtKind.LOAN -> "Empréstimo"
    DebtKind.FINANCING -> "Financiamento"
    DebtKind.CARD -> "Cartão ou cheque especial"
    DebtKind.AGREEMENT -> "Acordo"
    DebtKind.PERSON -> "Pessoa (amigo ou família)"
    DebtKind.OTHER -> "Outra"
}

fun debtKindShort(k: DebtKind) = when (k) {
    DebtKind.LOAN -> "Empréstimo"
    DebtKind.FINANCING -> "Financiamento"
    DebtKind.CARD -> "Cartão/cheque especial"
    DebtKind.AGREEMENT -> "Acordo"
    DebtKind.PERSON -> "Pessoa"
    DebtKind.OTHER -> "Outra"
}

fun debtKindIcon(k: DebtKind): ImageVector = when (k) {
    DebtKind.LOAN -> Icons.Rounded.RequestQuote
    DebtKind.FINANCING -> Icons.Rounded.Key
    DebtKind.CARD -> Icons.Rounded.CreditCard
    DebtKind.AGREEMENT -> Icons.Rounded.Handshake
    DebtKind.PERSON -> Icons.Rounded.People
    DebtKind.OTHER -> Icons.Rounded.MoneyOff
}

private fun plural(n: Int, one: String, many: String) = if (n == 1) "1 $one" else "$n $many"

/** Mês por extenso, curto: "mar/2029". */
fun shortMonthYear(ym: YearMonth) = "${Dates.monthShort(ym).lowercase()}/${ym.year}"

data class StatusLook(val text: String, val container: Color, val content: Color)

@Composable
fun debtStatusLook(info: DebtInfo, today: LocalDate): StatusLook {
    val c = CryoTheme.colors
    val neutral = MaterialTheme.colorScheme.surfaceContainerHighest
    val onNeutral = MaterialTheme.colorScheme.onSurface
    val due = info.nextDue
    return when (info.state) {
        DebtState.PAID_OFF -> StatusLook("Quitada", c.incomeContainer, c.onIncomeContainer)
        DebtState.OVERDUE -> StatusLook(
            if (info.hasInstallments) plural(info.overdueCount, "parcela atrasada", "parcelas atrasadas") else "Prazo vencido",
            c.expenseContainer, c.onExpenseContainer,
        )
        DebtState.DUE_SOON -> StatusLook("Vence ${due?.let { Dates.relative(it, today) }}", c.warningContainer, c.onWarningContainer)
        DebtState.PAID_THIS_MONTH -> StatusLook("Parcela do mês paga", c.incomeContainer, c.onIncomeContainer)
        DebtState.UPCOMING -> StatusLook("Próxima: ${due?.let { Dates.short(it) }}", neutral, onNeutral)
        DebtState.FREE -> StatusLook(if (due != null) "Prazo: ${Dates.short(due)}" else "Sem prazo", neutral, onNeutral)
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
        topBar = { CryoTopBar("Dívidas", onBack = nav::back) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { nav.go(Routes.debtEdit()) },
                icon = { Icon(Icons.Rounded.Add, null) }, text = { Text("Nova dívida") },
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
                        Icons.Rounded.RequestQuote, "Organize suas dívidas",
                        "Cadastre empréstimos, financiamentos, acordos ou dinheiro que você deve a alguém. " +
                            "O Cryo mostra quanto falta, lembra dos vencimentos e mostra quando você fica livre.",
                    ) { Button(onClick = { nav.go(Routes.debtEdit()) }) { Text("Cadastrar dívida") } }
                }
            } else {
                item { DebtHero(ov, hide) }
                if (ov.overdueCount > 0) {
                    item {
                        Banner(
                            Icons.Rounded.ErrorOutline,
                            plural(ov.overdueCount, "parcela atrasada", "parcelas atrasadas") +
                                ". Pagar logo evita juros e multa.",
                            CryoTheme.colors.expenseContainer, CryoTheme.colors.onExpenseContainer,
                        )
                    }
                }
                if (ov.monthlyInstallments > 0) item { IncomeShareCard(ov, l.averageIncome, hide) }
                if (ov.open.isNotEmpty()) item { SectionTitle("Em aberto") }
                items(ov.open, key = { it.debt.id }) { info -> DebtCard(info, l, hide, onPay = { paying = info }) }
                if (ov.open.isEmpty()) {
                    item {
                        CryoCard(color = CryoTheme.colors.incomeContainer) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Celebration, null, tint = CryoTheme.colors.onIncomeContainer)
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    "Nenhuma dívida em aberto. Muito bem!",
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
                            Text("Quitadas e arquivadas (${closed.size})")
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
    val bg = CryoTheme.colors.expenseContainer
    val fg = CryoTheme.colors.onExpenseContainer
    CryoCard(color = bg) {
        Text("Você deve", style = MaterialTheme.typography.labelLarge, color = fg)
        MoneyText(ov.total, style = MaterialTheme.typography.displaySmall, color = fg)
        Text(
            if (ov.open.isEmpty()) "Nenhuma dívida em aberto" else "em ${plural(ov.open.size, "dívida", "dívidas")}",
            style = MaterialTheme.typography.bodyMedium, color = fg,
        )
        if (ov.paidAll > 0) {
            Spacer(Modifier.height(12.dp))
            UsageBar(ov.progress, baseColor = fg, warn = false, height = 8.dp)
            Text(
                "Já pagou ${fmt(ov.paidAll, hide)} (${(ov.progress * 100).roundToInt()}%)",
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
                        Text("Parcelas por mês", style = MaterialTheme.typography.labelMedium, color = fg)
                        MoneyText(ov.monthlyInstallments, style = MaterialTheme.typography.titleSmall, color = fg)
                    }
                }
                ov.freeOfDebtYm?.let { end ->
                    Column(Modifier.weight(1f)) {
                        Text("Livre das dívidas em", style = MaterialTheme.typography.labelMedium, color = fg)
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
                "Suas parcelas somam ${fmt(ov.monthlyInstallments, hide)} por mês. Registre o que você recebe para ver quanto isso pesa na sua renda.",
                style = MaterialTheme.typography.bodyMedium,
            )
            return@CryoCard
        }
        val ratio = ov.monthlyInstallments.toFloat() / avgIncome
        val pct = (ratio * 100).roundToInt()
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Quanto da sua renda vai para parcelas", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
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
            "${fmt(ov.monthlyInstallments, hide)} de parcelas por mês, de ${fmt(avgIncome, hide)} que você costuma receber.",
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
    CryoCard(onClick = { nav.go(Routes.debt(d.id)) }) {
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
                Text("falta pagar", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(10.dp))
        UsageBar(info.progress, baseColor = Color(d.color), warn = false, height = 8.dp)
        Text(
            if (info.hasInstallments) {
                "${info.installmentsPaid} de ${info.installmentsTotal} parcelas pagas · ${fmt(d.installmentAmount, hide)} por mês"
            } else {
                "Pagou ${fmt(info.paid, hide)}" + (info.monthlyNeeded?.let { " · ${fmt(it, hide)} por mês para quitar no prazo" } ?: "")
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
                    Text(if (info.hasInstallments && info.state != DebtState.PAID_THIS_MONTH) "Paguei" else "Pagar")
                }
            }
        }
    }
}

@Composable
private fun ClosedDebtRow(info: DebtInfo, hide: Boolean) {
    val nav = LocalNav.current
    val d = info.debt
    CryoCard(onClick = { nav.go(Routes.debt(d.id)) }, color = MaterialTheme.colorScheme.surfaceContainerLowest) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(debtKindIcon(d.kind), Color(d.color).copy(alpha = 0.5f), 36.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(d.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    when {
                        info.isPaidOff -> "Quitada" + (info.lastPayment?.let { " em ${Dates.full(it)}" } ?: "")
                        else -> "Arquivada · faltavam ${fmt(info.remaining, hide)}"
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
    val bg = if (paidOff) CryoTheme.colors.incomeContainer else CryoTheme.colors.expenseContainer
    val fg = if (paidOff) CryoTheme.colors.onIncomeContainer else CryoTheme.colors.onExpenseContainer

    Scaffold(
        topBar = {
            CryoTopBar(d.name, onBack = nav::back, actions = {
                IconButton(onClick = { nav.go(Routes.debtEdit(id)) }) { Icon(Icons.Rounded.Edit, "Editar dívida") }
            })
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { pad ->
        LazyColumn(
            Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(bottom = 32.dp),
        ) {
            item {
                CryoCard(Modifier.padding(horizontal = 16.dp), color = bg) {
                    if (paidOff) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Celebration, null, tint = fg, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Dívida quitada. Parabéns!", style = MaterialTheme.typography.labelLarge, color = fg)
                        }
                        MoneyText(info.paidAll, style = MaterialTheme.typography.displaySmall, color = fg)
                        Text(
                            (listOf("pagos no total") + listOf(debtKindShort(d.kind), d.creditor).filter { it.isNotBlank() }).joinToString(" · "),
                            style = MaterialTheme.typography.bodyMedium, color = fg,
                        )
                    } else {
                        Text("Falta pagar", style = MaterialTheme.typography.labelLarge, color = fg)
                        MoneyText(info.remaining, style = MaterialTheme.typography.displaySmall, color = fg)
                        Text(
                            listOf(debtKindShort(d.kind), d.creditor).filter { it.isNotBlank() }.joinToString(" · "),
                            style = MaterialTheme.typography.bodyMedium, color = fg,
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    UsageBar(info.progress, baseColor = fg, warn = false, height = 8.dp)
                    Text(
                        if (info.hasInstallments) {
                            "${info.installmentsPaid} de ${info.installmentsTotal} parcelas pagas · ${fmt(d.installmentAmount, hide)} cada"
                        } else {
                            "Já pagou ${fmt(info.paid, hide)}"
                        },
                        style = MaterialTheme.typography.bodySmall, color = fg, modifier = Modifier.padding(top = 4.dp),
                    )
                    if (!info.isPaidOff) {
                        val end = when {
                            info.hasInstallments -> info.payoffYm?.let {
                                "Termina em ${Dates.monthYear(it, l.today).lowercase()} · ${plural(info.installmentsLeft, "parcela restante", "parcelas restantes")}"
                            }
                            d.deadline != null -> "Prazo: ${Dates.full(d.deadline)}" +
                                (info.monthlyNeeded?.let { " · ${fmt(it, hide)} por mês" } ?: "")
                            else -> null
                        }
                        if (end != null) Text(end, style = MaterialTheme.typography.bodyMedium, color = fg, modifier = Modifier.padding(top = 6.dp))
                    }
                    if (paidOff && !d.archived) {
                        Spacer(Modifier.height(12.dp))
                        OutlinedButton(onClick = {
                            c.launch {
                                c.repo.saveDebt(d.copy(archived = true))
                                c.message("Dívida arquivada. Ela fica em Quitadas e arquivadas.", "Desfazer") { c.repo.saveDebt(d.copy(archived = false)) }
                            }
                            nav.back()
                        }) { Icon(Icons.Rounded.Archive, null, tint = fg); Spacer(Modifier.width(8.dp)); Text("Arquivar", color = fg) }
                    }
                }
            }
            if (d.archived) {
                item {
                    Banner(
                        Icons.Rounded.Archive, "Dívida arquivada: ela não entra nos totais nem na previsão.",
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
                            "${plural(info.overdueCount, "parcela atrasada", "parcelas atrasadas")} desde ${info.nextDue?.let { Dates.short(it) }}"
                        } else {
                            "O prazo combinado já passou"
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
                        Button(onClick = { action = "pay" }) { Text(if (info.hasInstallments) "Pagar parcela" else "Pagar") }
                        FilledTonalButton(onClick = { action = "payoff" }) { Text("Quitar") }
                    }
                    FilledTonalButton(onClick = { action = "adjust" }) { Text("Atualizar saldo") }
                    FilledTonalButton(onClick = { action = "borrow" }) { Text("Peguei mais") }
                    if (d.archived) {
                        OutlinedButton(onClick = { c.launch { c.repo.saveDebt(d.copy(archived = false)); c.message("Dívida restaurada") } }) {
                            Icon(Icons.Rounded.Unarchive, null); Spacer(Modifier.width(8.dp)); Text("Restaurar")
                        }
                    }
                }
            }
            if (info.hasInstallments && !info.isPaidOff) {
                item { SectionTitle("Próximas parcelas", Modifier.padding(horizontal = 16.dp)) }
                item { UpcomingInstallments(info, l, hide) }
            }
            item {
                CryoCard(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    InfoLine("Tipo", debtKindName(d.kind))
                    if (d.creditor.isNotBlank()) InfoLine("Com quem", d.creditor)
                    InfoLine("Paga com", d.accountId?.let { l.accountName(it) } ?: "—")
                    if (info.hasInstallments && !paidOff) {
                        InfoLine("Vencimento", "todo dia ${d.dueDay}")
                        InfoLine("Lembrete", if (d.remind) (if (d.remindDaysBefore == 0) "no dia" else "${plural(d.remindDaysBefore, "dia", "dias")} antes") else "desligado")
                    }
                    if (info.borrowed > 0) InfoLine("Pegou depois do cadastro", fmt(info.borrowed, hide))
                    if (info.adjusted != 0L) InfoLine(if (info.adjusted > 0) "Juros e encargos" else "Descontos", fmt(kotlin.math.abs(info.adjusted), hide))
                    if (d.note.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text(d.note, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            item { SectionTitle("Histórico", Modifier.padding(horizontal = 16.dp)) }
            val hist = l.debtHistory(id)
            if (hist.isEmpty()) {
                item {
                    Text(
                        "Nenhum pagamento registrado ainda.", Modifier.padding(horizontal = 20.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(hist.size) { i ->
                when (val h = hist[i]) {
                    is Tx -> TxRow(h, l, onClick = { nav.go(Routes.txEdit(h.id)) }, showDate = true)
                    is DebtAdjustment -> AdjustmentRow(h, hide, l.today, onDelete = {
                        c.launch { c.repo.deleteDebtAdjustment(h); c.message("Ajuste removido") }
                    })
                }
            }
        }
    }

    when (action) {
        "pay" -> DebtPaySheet(info, l, payoff = false, onDismiss = { action = null })
        "payoff" -> DebtPaySheet(info, l, payoff = true, onDismiss = { action = null })
        "adjust" -> MoneyActionSheet(
            title = "Atualizar quanto falta",
            subtitle = "Veja o valor no app do banco ou no contrato. Se aumentou, a diferença conta como juros e encargos. Se diminuiu, como desconto.",
            initialAmount = info.remaining, l = l, accountLabel = null, initialAccount = null,
            confirmLabel = "Atualizar", amountLabel = "Falta pagar hoje",
            onDismiss = { action = null },
            onConfirm = { amount, _, date ->
                action = null
                val diff = amount - info.outstanding
                c.launch {
                    val u = c.repo.adjustDebt(d, diff, date, if (diff > 0) "Juros e encargos" else "Desconto")
                    if (u.adjustments.isNotEmpty()) c.message("Saldo da dívida atualizado", "Desfazer") { c.repo.undo(u) }
                }
            },
        )
        "borrow" -> MoneyActionSheet(
            title = "Peguei mais emprestado",
            subtitle = "O dinheiro entra na conta e aumenta a dívida. Não conta como receita.",
            initialAmount = 0, l = l, accountLabel = "Entrou na conta",
            initialAccount = d.accountId ?: LocalSettings.current.defaultAccountId, confirmLabel = "Registrar",
            onDismiss = { action = null },
            onConfirm = { amount, acc, date ->
                action = null
                c.launch {
                    val u = c.repo.borrowMore(d, amount, acc!!, date)
                    c.message("${Money.format(amount)} somados à dívida", "Desfazer") { c.repo.undo(u) }
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
                due.isBefore(l.today) -> "Atrasada" to c.expense
                due.ym() == l.today.ym() -> "Este mês" to c.warning
                else -> "" to MaterialTheme.colorScheme.onSurfaceVariant
            }
            if (i > 0) HorizontalDivider(Modifier.padding(vertical = 6.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Parcela ${d.paidBefore + k + 1} de ${info.installmentsTotal}", style = MaterialTheme.typography.bodyLarge)
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
                "e mais ${info.installmentsLeft - shown}, até ${info.payoffYm?.let { Dates.monthYear(it, l.today).lowercase() }}",
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
    val title = a.note.ifBlank { if (up) "Juros e encargos" else "Desconto" }
    Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        IconBadge(if (up) Icons.AutoMirrored.Rounded.TrendingUp else Icons.AutoMirrored.Rounded.TrendingDown, if (up) CryoTheme.colors.expense else CryoTheme.colors.income)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                (if (a.date.year == today.year) Dates.short(a.date) else Dates.full(a.date)) +
                    if (up) " · aumentou a dívida" else " · diminuiu a dívida",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            if (hide) Money.HIDDEN else (if (up) "+ " else "− ") + Money.format(kotlin.math.abs(a.amount)),
            style = MaterialTheme.typography.titleSmall, color = if (up) CryoTheme.colors.expense else CryoTheme.colors.income,
        )
        IconButton(onClick = { ask = true }) { Icon(Icons.Rounded.Close, "Remover ajuste: $title") }
    }
    if (ask) ConfirmDialog("Remover ajuste?", "A dívida volta ao valor de antes deste ajuste.", "Remover", { ask = false; onDelete() }, { ask = false })
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
                    payoff -> "Quitar ${d.name}"
                    info.hasInstallments -> "Pagar parcela"
                    else -> "Pagar ${d.name}"
                },
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                when {
                    payoff -> "Falta pagar ${Money.format(remaining)}. Se o banco ou a pessoa deu desconto, coloque o valor que você pagou."
                    info.hasInstallments ->
                        "Parcela ${info.installmentsPaid + 1} de ${info.installmentsTotal} de ${d.name}" +
                            (info.nextDue?.let { " · vence ${Dates.short(it)}" } ?: "")
                    else -> "Falta pagar ${Money.format(remaining)}"
                },
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            AmountField(amount, { amount = it }, label = "Valor pago", color = CryoTheme.colors.expense)

            when {
                payoff && amount in 1 until remaining -> Hint("Desconto de ${Money.format(remaining - amount)}: a dívida fica quitada e o desconto fica registrado.")
                overTotal > 0 -> Hint("Você pagou ${Money.format(overTotal)} a mais do que falta: essa diferença conta como juros ou multa.")
                !payoff && amount == remaining && remaining > 0 -> Hint("Esse pagamento quita a dívida. 🎉")
                asksExtra -> {
                    FieldLabel("A diferença de ${Money.format(overInstallment)} foi:")
                    ChoiceRow("Juros ou multa por atraso", "Sai da conta, mas não diminui a dívida", extraIsInterest) { extraIsInterest = true }
                    ChoiceRow("Para adiantar a dívida", "Diminui quanto falta pagar", !extraIsInterest) { extraIsInterest = false }
                }
            }

            FieldLabel("Pago com a conta")
            AccountChips(l.activeAccounts, acc, { acc = it })
            FieldLabel("Data")
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
                        c.message(if (quitou) "Dívida quitada. Parabéns! 🎉" else "Pagamento registrado", "Desfazer") { c.repo.undo(u) }
                    }
                },
                enabled = amount > 0 && acc != null,
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) {
                Icon(Icons.Rounded.Check, null)
                Spacer(Modifier.width(8.dp))
                Text(if (payoff) "Quitar dívida" else "Registrar pagamento")
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Cancelar") }
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

@Composable
private fun ChoiceRow(title: String, subtitle: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(Modifier.width(10.dp))
        Column {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
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
        name.isBlank() -> "Dê um nome para a dívida"
        installments && amountEach <= 0 -> "Informe o valor da parcela"
        installments && paidBefore >= count -> "As parcelas pagas precisam ser menos que o total"
        creating && balance <= 0 -> "Informe quanto falta pagar"
        creating && received && (receivedAmount <= 0 || receivedAcc == null) -> "Informe quanto entrou e em qual conta"
        creating && received && receivedAmount > balance -> "O valor que entrou não pode ser maior que o total a pagar"
        else -> null
    }

    EditorScaffold(
        title = if (creating) "Nova dívida" else "Editar dívida",
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
                    c.message("Dívida cadastrada")
                } else {
                    val noHistory = info!!.paid == 0L && info.borrowed == 0L && info.adjusted == 0L
                    when {
                        !balanceTouched -> c.repo.saveDebt(base)
                        noHistory -> c.repo.saveDebt(base.copy(initialBalance = balance))
                        else -> {
                            c.repo.saveDebt(base)
                            val diff = balance - info.outstanding
                            if (diff != 0L) c.repo.adjustDebt(base, diff, l.today, "Correção do saldo")
                        }
                    }
                    c.message("Dívida atualizada")
                }
            }
            nav.back()
        },
        onDelete = existing?.let { { confirmDelete = true } },
    ) {
        OutlinedTextField(
            name, { name = it }, label = { Text("Nome da dívida") }, singleLine = true,
            placeholder = { Text("Ex.: Financiamento da moto, Devo à Ana") },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences), modifier = Modifier.fillMaxWidth(),
        )
        FieldLabel("Tipo")
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
            creditor, { creditor = it }, label = { Text("Com quem (opcional)") }, singleLine = true,
            placeholder = { Text("Ex.: banco, loja ou nome da pessoa") },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words), modifier = Modifier.fillMaxWidth(),
        )

        FieldLabel("Como você paga")
        Segmented(listOf(true to "Parcelas fixas", false to "Quando puder"), installments, { installments = it; modeTouched = true })

        if (installments) {
            FieldLabel("Valor de cada parcela")
            AmountField(amountEach, { amountEach = it }, label = "Parcela")
            Spacer(Modifier.height(8.dp))
            Stepper(count, { count = it; if (paidBefore >= it) paidBefore = it - 1 }, 1..600, "Total de parcelas")
            Stepper(paidBefore, { paidBefore = it }, 0..(count - 1).coerceAtLeast(0), "Já pagas")
            Stepper(dueDay, { dueDay = it }, 1..31, "Vence todo dia")
            FieldLabel("Próxima parcela a pagar")
            val start = ymOf(startKey)
            MonthSelector(
                "${start.dayClamped(dueDay).dayOfMonth} de ${Dates.monthYear(start, l.today).lowercase()}",
                onPrev = { startKey = start.minusMonths(1).key(); startTouched = true },
                onNext = { startKey = start.plusMonths(1).key(); startTouched = true },
            )
        } else {
            FieldLabel("Prazo para quitar (opcional)")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    deadline?.let { Dates.full(LocalDate.ofEpochDay(it)) } ?: "Sem prazo",
                    style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f).clickable { pickDate = true },
                )
                TextButton(onClick = { pickDate = true }) { Text(if (deadline == null) "Definir" else "Mudar") }
                if (deadline != null) TextButton(onClick = { deadline = null }) { Text("Tirar") }
            }
        }

        FieldLabel(if (creating) "Quanto falta pagar" else "Falta pagar hoje")
        AmountField(balance, { balance = it; balanceTouched = true }, label = "Falta pagar", color = CryoTheme.colors.expense)
        Text(
            when {
                !creating -> "Se você mudar este valor, a diferença fica registrada como juros ou desconto."
                installments && amountEach <= 0 -> "Preencha o valor da parcela: o total é calculado sozinho."
                installments && !balanceTouched ->
                    "Calculado: ${count - paidBefore} parcelas de ${Money.format(amountEach)}. Ajuste se o banco informou outro valor (por exemplo, com desconto para quitar)."
                installments -> "Valor informado por você (as parcelas somariam ${Money.format(computed)})."
                else -> "O total que você deve hoje."
            },
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
        if (installments && amountEach > 0 && count > paidBefore) {
            val left = count - paidBefore
            val end = ymOf(startKey).plusMonths((left - 1).toLong())
            Text(
                "Termina em ${Dates.monthYear(end, l.today).lowercase()} · ${plural(left, "parcela", "parcelas")} pela frente",
                style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 6.dp),
            )
        }

        if (creating) {
            Spacer(Modifier.height(8.dp))
            SwitchRow(
                "O dinheiro entrou numa conta agora", received, { received = it },
                "Use quando acabou de pegar o empréstimo. Não conta como receita.",
            )
            if (received) {
                AmountField(receivedAmount, { receivedAmount = it }, label = "Quanto entrou")
                FieldLabel("Entrou na conta")
                AccountChips(l.activeAccounts, receivedAcc, { receivedAcc = it })
            }
        }

        FieldLabel("Conta usada para pagar")
        AccountChips(l.activeAccounts, payAcc, { payAcc = it })
        if (installments) {
            Spacer(Modifier.height(8.dp))
            SwitchRow("Lembrar do vencimento", remind, { remind = it }, "Notificação no celular")
            if (remind) {
                Stepper(remindBefore, { remindBefore = it }, 0..7, "Avisar antes", format = { if (it == 0) "No dia" else "$it dia${if (it > 1) "s" else ""}" })
            }
            Text(
                "Não precisa criar uma conta fixa para esta parcela: ela já aparece em Planejar › Contas fixas e na previsão do mês.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        FieldLabel("Cor")
        ColorChooser(color) { color = it }
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            note, { note = it }, label = { Text("Observação (opcional)") }, minLines = 2,
            placeholder = { Text("Ex.: juros de 2% ao mês, número do contrato") }, modifier = Modifier.fillMaxWidth(),
        )
        if (problem != null && name.isNotBlank()) {
            Text(problem, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 12.dp))
        }
        if (existing != null) {
            Spacer(Modifier.height(16.dp))
            if (existing.archived) {
                OutlinedButton(onClick = {
                    c.launch { c.repo.saveDebt(existing.copy(archived = false)); c.message("Dívida restaurada") }
                    nav.back()
                }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Rounded.Unarchive, null); Spacer(Modifier.width(8.dp)); Text("Restaurar dívida") }
            } else {
                OutlinedButton(onClick = { confirmArchive = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.Archive, null); Spacer(Modifier.width(8.dp)); Text("Arquivar dívida")
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
            "Arquivar dívida?",
            if (info!!.remaining > 0) {
                "Ela ainda tem ${Money.format(info.remaining)} em aberto. Arquivada, ela sai dos totais, da previsão e dos lembretes. Você encontra em Dívidas › Quitadas e arquivadas."
            } else {
                "Ela vai para Dívidas › Quitadas e arquivadas."
            },
            "Arquivar",
            onConfirm = {
                confirmArchive = false
                c.launch {
                    c.repo.saveDebt(existing.copy(archived = true))
                    c.message("Dívida arquivada", "Desfazer") { c.repo.saveDebt(existing.copy(archived = false)) }
                }
                nav.back(); nav.back()
            },
            onDismiss = { confirmArchive = false },
        )
    }
    if (confirmDelete && existing != null) {
        ConfirmDialog(
            "Apagar dívida?",
            "Os pagamentos já feitos continuam no extrato, porque o dinheiro saiu de verdade. Para só esconder a dívida, use Arquivar.",
            "Apagar",
            onConfirm = {
                confirmDelete = false
                c.launch { c.repo.deleteDebt(existing); c.message("Dívida apagada") }
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
    val ov = l.debtOverview
    val next = ov.open.filter { it.nextDue != null && it.hasInstallments }.minByOrNull { it.nextDue!! }
    CryoCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(Icons.Rounded.RequestQuote, CryoTheme.colors.expense)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Você deve", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                MoneyText(ov.total, style = MaterialTheme.typography.titleLarge)
            }
            Text(plural(ov.open.size, "dívida", "dívidas"), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (ov.paidAll > 0) {
            Spacer(Modifier.height(10.dp))
            UsageBar(ov.progress, baseColor = CryoTheme.colors.income, warn = false, height = 6.dp)
            Text(
                "Já pagou ${(ov.progress * 100).roundToInt()}%" + (ov.freeOfDebtYm?.let { " · livre em ${shortMonthYear(it)}" } ?: ""),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        if (next != null) {
            Spacer(Modifier.height(10.dp))
            val look = debtStatusLook(next, l.today)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Próxima parcela", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
