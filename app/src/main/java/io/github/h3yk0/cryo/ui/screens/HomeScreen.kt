// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.EventRepeat
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.RequestQuote
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.h3yk0.cryo.R
import io.github.h3yk0.cryo.data.db.TxType
import io.github.h3yk0.cryo.domain.Dates
import io.github.h3yk0.cryo.domain.DebtState
import io.github.h3yk0.cryo.domain.Inbox
import io.github.h3yk0.cryo.domain.InvoiceStatus
import io.github.h3yk0.cryo.domain.Ledger
import io.github.h3yk0.cryo.domain.Money
import io.github.h3yk0.cryo.domain.Num
import io.github.h3yk0.cryo.domain.Res
import io.github.h3yk0.cryo.domain.predict.MonthEnds
import io.github.h3yk0.cryo.ui.LocalNav
import io.github.h3yk0.cryo.ui.Routes
import io.github.h3yk0.cryo.ui.components.CardPadding
import io.github.h3yk0.cryo.ui.components.CryoSection
import io.github.h3yk0.cryo.ui.components.CryoTile
import io.github.h3yk0.cryo.ui.components.Depth
import io.github.h3yk0.cryo.ui.components.HomeHeader
import io.github.h3yk0.cryo.ui.components.InfoCard
import io.github.h3yk0.cryo.ui.components.LocalContainer
import io.github.h3yk0.cryo.ui.components.LocalSettings
import io.github.h3yk0.cryo.ui.components.MoneyText
import io.github.h3yk0.cryo.ui.components.SectionPadding
import io.github.h3yk0.cryo.ui.components.ShortcutItem
import io.github.h3yk0.cryo.ui.components.Shortcuts
import io.github.h3yk0.cryo.ui.components.TileGap
import io.github.h3yk0.cryo.ui.components.TileRow
import io.github.h3yk0.cryo.ui.components.ToneNotice
import io.github.h3yk0.cryo.ui.components.TxRow
import io.github.h3yk0.cryo.ui.components.accountIcon
import io.github.h3yk0.cryo.ui.components.depthCard
import io.github.h3yk0.cryo.ui.components.guideTarget
import io.github.h3yk0.cryo.ui.components.rememberEngine
import io.github.h3yk0.cryo.ui.components.rememberLedger
import io.github.h3yk0.cryo.ui.theme.CryoTheme
import io.github.h3yk0.cryo.ui.tx.LoadingBox
import io.github.h3yk0.cryo.ui.tx.QuickEntryBar
import io.github.h3yk0.cryo.ui.tx.QuickEntryHint

/**
 * TELA-INICIO (PRD 6.1). Direção visual 2.0: cabeçalho na cor do Cryo com o saldo (RF-INI-11), atalhos (RF-INI-12) e
 * registro rápido. Reforma visual da 1.6 (LA9, LA10): o sino da central de avisos no cabeçalho (RF-INI-19), no máximo um
 * destaque, o mais importante e novo (RF-INI-20), o mês num cartão largo na cor do resultado e o resto em blocos de duas
 * colunas (RF-INI-21). Nada de lista de avisos ou de percepções no meio da tela (DS-PSI-06).
 */
@Composable
fun HomeScreen() {
    val l = rememberLedger() ?: return LoadingBox()
    val nav = LocalNav.current
    val c = LocalContainer.current
    val settings = LocalSettings.current
    val hide = settings.hideValues
    val month = remember(l) { l.monthSummary(l.today.ym()) }
    val engine = rememberEngine()
    // RF-PRV-10: o fim do mês real, o mesmo de Análises (renda × despesas, nada estimado).
    val monthEnd = remember(l) { MonthEnds.of(l) }
    // RF-INI-19/20: os avisos, o que já foi visto e o destaque.
    val inboxVersion by c.inbox.version.collectAsState()
    val inbox = remember(l, engine, hide, monthEnd) { Inbox.items(l, engine, monthEnd, hide) }
    val seen = remember(inboxVersion) { c.inbox.seen() }
    val unseen = remember(inbox, seen) { Inbox.unseenImportant(inbox, seen) }
    val newInsight = remember(inbox, seen) { Inbox.hasNewInsight(inbox, seen) }
    // DS-PSI-06: depois do × (ou do toque) no destaque, nenhum outro aparece nesta visita; o sino guarda o resto.
    var quiet by rememberSaveable { mutableStateOf(false) }
    val highlight = remember(inbox, seen, quiet) { if (quiet) null else Inbox.highlight(inbox, seen) }

    var shopping by remember { mutableStateOf(false) }
    if (shopping) ShoppingStartSheet(onDismiss = { shopping = false })
    val tiles = homeTiles(l, nav, hide)

    // RF-INI-16: sem botão flutuante; "Lançar" é o primeiro atalho.
    Scaffold(contentWindowInsets = WindowInsets(0, 0, 0, 0)) { pad ->
        LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(bottom = 24.dp)) {
            item {
                HomeHeader(
                    name = settings.userName, subtitle = Dates.longDate(l.today), hideValues = hide,
                    onToggleHide = { c.launch { c.settings.setHideValues(!hide) } },
                    onProfile = { nav.go(Routes.PROFILE) },
                    avatar = { io.github.h3yk0.cryo.ui.components.ProfileAvatar(settings, 40.dp, onHeader = true) },
                    bell = { InboxBell(unseen, newInsight) { nav.go(Routes.INBOX) } },
                ) {
                    // RF-INI-18 (1.6): a saúde financeira ao lado do saldo; só quando a nota existe.
                    BalanceBlock(l, health = engine?.health?.takeIf { it.ready }, onClick = { nav.go(Routes.WALLET) }, onHealth = { nav.go(Routes.HEALTH) })
                }
            }

            item { Shortcuts(homeShortcuts(l, nav, onShopping = if (c.sandbox) null else ({ shopping = true })), Modifier.padding(top = 16.dp)) }

            // RF-AVI-10: a ida às compras em andamento (sempre os dados de verdade).
            if (!c.sandbox) item { ShoppingBanner(Modifier.padding(start = SectionPadding, end = SectionPadding, top = 12.dp)) }

            item {
                Column(Modifier.padding(start = SectionPadding, end = SectionPadding, top = 12.dp)) {
                    QuickEntryBar(l, Modifier.guideTarget(io.github.h3yk0.cryo.domain.Lesson.PHRASE))
                    QuickEntryHint(Modifier.padding(start = 16.dp, end = 16.dp, top = 6.dp))
                }
            }

            // RF-INI-20: no máximo um destaque, o mais importante ainda não visto; "×" ou o toque o dão por visto.
            highlight?.let { h ->
                item(key = "destaque") {
                    ToneNotice(
                        inboxIcon(h.kind), h.text, toneOf(h, CryoTheme.tones),
                        modifier = Modifier.padding(start = SectionPadding, end = SectionPadding, top = 12.dp),
                        onClick = {
                            quiet = true
                            c.inbox.markSeen(listOf(h.key), l.today.toEpochDay())
                            h.target?.let { openTarget(nav, it) } ?: nav.go(Routes.INBOX)
                        },
                        onDismiss = { quiet = true; c.inbox.markSeen(listOf(h.key), l.today.toEpochDay()) },
                    )
                }
            }

            item { Spacer(Modifier.height(10.dp)) }

            item { MonthCard(l, month.income, month.expense, monthEnd, hide) { nav.tab(Routes.INSIGHTS) } }

            // RF-INI-21: os blocos, em duas colunas (DS-BLOCO).
            tiles.chunked(2).forEachIndexed { i, row ->
                item(key = "blocos$i") {
                    TileRow(
                        Modifier.padding(horizontal = Depth.Margin, vertical = TileGap / 2),
                        first = row[0], second = row.getOrNull(1),
                    )
                }
            }

            val recent = l.s.txs.filter { !it.date.isAfter(l.today) }.take(MAX_ROWS)
            item {
                Spacer(Modifier.height(6.dp))
                CryoSection(stringResource(R.string.home_recent), onClick = { nav.tab(Routes.txs()) }, headerOnly = true) {
                    if (recent.isEmpty()) {
                        InfoCard(
                            Icons.Rounded.AutoAwesome,
                            stringResource(R.string.home_empty_hint),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    } else {
                        recent.forEachIndexed { i, t ->
                            if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            TxRow(t, l, onClick = { nav.go(Routes.txEdit(t.id)) }, modifier = Modifier.padding(horizontal = 0.dp))
                        }
                    }
                }
            }
        }
    }
}

/**
 * RF-INI-19: o sino. O número conta os avisos importantes ainda não vistos; só percepção nova acende um ponto. O leitor de
 * tela lê "Abrir os avisos" (e quantos são novos).
 */
@Composable
private fun InboxBell(unseen: Int, newInsight: Boolean, onClick: () -> Unit) {
    val desc = if (unseen > 0) Res.s(R.string.inbox_open_count, Num.int(unseen.toLong())) else Res.s(R.string.inbox_open)
    IconButton(onClick = onClick, modifier = Modifier.semantics { contentDescription = desc }) {
        BadgedBox(
            badge = {
                when {
                    unseen > 0 -> Badge { Text(if (unseen > 9) "9+" else Num.int(unseen.toLong())) }
                    newInsight -> Badge()
                }
            },
        ) { Icon(Icons.Rounded.Notifications, null) }
    }
}

/**
 * RF-INI-05, RF-PRV-10: o mês num cartão largo, na cor do resultado (DS-TOM: menta quando sobra, rosa quando falta):
 * entrou, saiu, o fim do mês real e a frase de quanto dá por dia.
 */
@Composable
private fun MonthCard(l: Ledger, income: Long, expense: Long, end: io.github.h3yk0.cryo.domain.predict.MonthEnd, hide: Boolean, onClick: () -> Unit) {
    val tone = if (end.total >= 0) CryoTheme.tones.good else CryoTheme.tones.alert
    Column(
        Modifier.fillMaxWidth().padding(horizontal = Depth.Margin, vertical = 6.dp)
            .depthCard(color = tone.container)
            .clickable(role = androidx.compose.ui.semantics.Role.Button, onClick = onClick)
            .padding(horizontal = CardPadding, vertical = 16.dp),
    ) {
        androidx.compose.runtime.CompositionLocalProvider(LocalContentColor provides tone.onContainer) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    Dates.monthName(l.today.ym()), style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f).semantics { heading() },
                )
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = tone.onContainer.copy(alpha = 0.6f))
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth().height(androidx.compose.foundation.layout.IntrinsicSize.Min)) {
                Stat(stringResource(R.string.month_in), income, CryoTheme.colors.income, Icons.AutoMirrored.Rounded.TrendingUp, Modifier.weight(1f))
                Stat(stringResource(R.string.month_out), expense, CryoTheme.colors.expense, Icons.AutoMirrored.Rounded.TrendingDown, Modifier.weight(1f))
                Stat(stringResource(R.string.month_end), end.total, tone.accent, Icons.Rounded.AutoAwesome, Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
            Text(
                when {
                    end.total > 0 -> stringResource(R.string.home_month_left, fmt(end.total, hide), fmt(end.perDay, hide))
                    end.total < 0 -> stringResource(R.string.home_month_short, fmt(-end.total, hide))
                    else -> stringResource(R.string.home_month_even)
                },
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

/** RF-INI-21: um bloco do Início = a função que desenha ele com o modificador da grade. */
private typealias TileSlot = @Composable (Modifier) -> Unit

/** RF-INI-21: os blocos do Início, na ordem do dia a dia; cada um só aparece quando existe o que mostrar. */
@Composable
private fun homeTiles(l: Ledger, nav: io.github.h3yk0.cryo.ui.Navigator, hide: Boolean): List<TileSlot> {
    val out = ArrayList<TileSlot>()
    val tones = CryoTheme.tones
    for (card in l.activeCards) {
        val inv = l.focusInvoice(card)
        val st = inv.status(l.today)
        val used = (card.limitAmount - l.availableLimit(card)).toFloat() / card.limitAmount.coerceAtLeast(1)
        out += { m ->
            CryoTile(
                card.name, m, icon = Icons.Rounded.CreditCard, iconColor = Color(card.color),
                subtitle = invoiceStatusText(st, inv.due, l), progress = used, progressColor = Color(card.color),
                tone = if (st == InvoiceStatus.OVERDUE) tones.alert else null,
                onClick = { nav.go(Routes.card(card.id)) },
                valueContent = { MoneyText(if (st == InvoiceStatus.OPEN) inv.total else inv.remaining, style = MaterialTheme.typography.titleLarge) },
            )
        }
    }
    val accounts = l.activeAccounts
    if (accounts.isNotEmpty()) {
        val main = accounts.firstOrNull { it.id == LocalSettings.current.defaultAccountId } ?: accounts.maxBy { l.balance(it.id) }
        out += { m ->
            CryoTile(
                stringResource(R.string.home_accounts), m, icon = accountIcon(main.type), iconColor = Color(main.color),
                subtitle = if (accounts.size > 1) stringResource(R.string.home_tile_accounts_sub, main.name, accounts.size - 1) else main.name,
                onClick = { nav.go(Routes.WALLET) },
                valueContent = { MoneyText(l.balance(main.id), style = MaterialTheme.typography.titleLarge) },
            )
        }
    }
    val budget = io.github.h3yk0.cryo.domain.GeneralBudget.usage(l, l.today.ym())
    val usages = l.budgetUsages(l.today.ym())
    if (budget != null || usages.isNotEmpty()) {
        out += { m ->
            val ratio = budget?.ratio ?: (usages.sumOf { it.spent }.toFloat() / usages.sumOf { it.limit }.coerceAtLeast(1))
            CryoTile(
                stringResource(R.string.home_budgets_short), m, icon = Icons.Rounded.PieChart,
                subtitle = if (budget != null) stringResource(R.string.home_tile_budget_general, fmt(budget.limit, hide))
                else stringResource(R.string.home_tile_budget_used, Num.percent((ratio * 100).toInt())),
                progress = ratio,
                tone = when {
                    ratio >= 1f -> tones.alert
                    ratio >= 0.8f -> tones.attention
                    else -> null
                },
                onClick = { nav.open(Routes.plan(0)) },
                valueContent = {
                    val left = budget?.remaining ?: (usages.sumOf { it.limit } - usages.sumOf { it.spent })
                    Text(
                        if (left >= 0) stringResource(R.string.budget_left, fmt(left, hide)) else stringResource(R.string.budget_over, fmt(-left, hide)),
                        style = MaterialTheme.typography.titleMedium, maxLines = 2,
                    )
                },
            )
        }
    }
    if (l.debtOverview.open.isNotEmpty()) {
        val ov = l.debtOverview
        val next = ov.open.filter { it.nextDue != null && it.hasInstallments }.minByOrNull { it.nextDue!! }
        out += { m ->
            CryoTile(
                stringResource(R.string.home_debts), m, icon = Icons.Rounded.RequestQuote, iconColor = CryoTheme.colors.expense,
                subtitle = next?.let { stringResource(R.string.home_tile_debt_next, it.debt.name, Dates.short(it.nextDue!!)) },
                tone = if (ov.open.any { it.state == DebtState.OVERDUE }) tones.alert else null,
                onClick = { nav.go(Routes.DEBTS) },
                valueContent = { MoneyText(ov.total, style = MaterialTheme.typography.titleLarge) },
            )
        }
    }
    if (l.activeInvestments.isNotEmpty()) {
        out += { m ->
            CryoTile(
                stringResource(R.string.home_investments), m, icon = Icons.Rounded.Savings, iconColor = CryoTheme.colors.income,
                subtitle = stringResource(R.string.home_tile_yield, if (hide) Money.HIDDEN else Money.signed(l.totalYield)),
                onClick = { nav.go(Routes.WALLET) },
                valueContent = { MoneyText(l.totalInvestments, style = MaterialTheme.typography.titleLarge) },
            )
        }
    }
    if (l.activeGoals.isNotEmpty()) {
        val saved = l.activeGoals.sumOf { l.goalSaved(it.id) }
        val target = l.activeGoals.sumOf { it.target }
        out += { m ->
            CryoTile(
                stringResource(R.string.home_goals_short), m, icon = Icons.Rounded.Flag, iconColor = Color(l.activeGoals.first().color),
                subtitle = stringResource(R.string.home_tile_goals_sub, l.activeGoals.size),
                progress = saved.toFloat() / target.coerceAtLeast(1), progressColor = Color(l.activeGoals.first().color),
                onClick = { nav.open(Routes.plan(2)) },
                valueContent = { MoneyText(saved, style = MaterialTheme.typography.titleLarge) },
            )
        }
    }
    return out
}

/** RF-INI-21: quantas movimentações recentes o Início mostra (o resto fica no Extrato). */
private const val MAX_ROWS = 4

/** RF-INI-12: as ações mais usadas, na ordem do dia a dia. "Fatura" só aparece com cartão. */
private fun homeShortcuts(l: Ledger, nav: io.github.h3yk0.cryo.ui.Navigator, onShopping: (() -> Unit)?): List<ShortcutItem> = listOfNotNull(
    ShortcutItem(Res.s(R.string.home_shortcut_new), Icons.Rounded.Add) { nav.go(Routes.txNew()) },
    ShortcutItem(Res.s(R.string.home_shortcut_import), Icons.Rounded.UploadFile) { nav.go(Routes.import()) },
    ShortcutItem(Res.s(R.string.home_shortcut_transfer), Icons.Rounded.SwapHoriz) { nav.go(Routes.txNew(TxType.TRANSFER)) },
    // RF-AVI-10: "Vou às compras" (o modo compras ao vivo).
    onShopping?.let { ShortcutItem(Res.s(R.string.home_shortcut_shopping), Icons.Rounded.ShoppingCart, onClick = it) },
    l.activeCards.takeIf { it.isNotEmpty() }?.let { cards ->
        ShortcutItem(Res.s(R.string.home_shortcut_invoice), Icons.Rounded.CreditCard) { if (cards.size == 1) nav.go(Routes.card(cards.single().id)) else nav.go(Routes.WALLET) }
    },
    ShortcutItem(Res.s(R.string.home_shortcut_bills), Icons.Rounded.EventRepeat) { nav.open(Routes.plan(1)) },
    ShortcutItem(Res.s(R.string.home_shortcut_goals), Icons.Rounded.Savings) { nav.open(Routes.plan(2)) },
    ShortcutItem(Res.s(R.string.home_shortcut_budgets), Icons.Rounded.PieChart) { nav.open(Routes.plan(0)) },
    ShortcutItem(Res.s(R.string.home_shortcut_debts), Icons.Rounded.RequestQuote) { nav.go(Routes.DEBTS) },
)

private fun fmt(v: Long, hide: Boolean) = if (hide) Money.HIDDEN else Money.format(v)

private fun java.time.LocalDate.ym() = java.time.YearMonth.from(this)

fun invoiceStatusText(st: InvoiceStatus, due: java.time.LocalDate, l: Ledger): String = when (st) {
    InvoiceStatus.OPEN -> Res.s(R.string.invoice_status_open, Dates.short(due))
    InvoiceStatus.CLOSED -> Res.s(R.string.invoice_status_closed, Dates.relative(due, l.today))
    InvoiceStatus.PAID -> Res.s(R.string.invoice_status_paid)
    InvoiceStatus.OVERDUE -> Res.s(R.string.invoice_status_overdue, Dates.short(due))
    InvoiceStatus.EMPTY -> Res.s(R.string.invoice_status_empty)
}

/** RF-INI-02/03: o saldo em contas, o maior número da tela, e o patrimônio logo abaixo (toque leva à Carteira). */
@Composable
private fun BalanceBlock(
    l: Ledger, onClick: () -> Unit,
    health: io.github.h3yk0.cryo.domain.engine.Health? = null, onHealth: () -> Unit = {},
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        BalanceColumn(l, onClick, Modifier.weight(1f))
        if (health != null) {
            io.github.h3yk0.cryo.ui.components.HealthRing(
                health.score, levelColor(health.level), levelLabel(health.level), onHealth,
                Modifier.padding(start = 8.dp),
            )
        }
    }
}

@Composable
private fun BalanceColumn(l: Ledger, onClick: () -> Unit, modifier: Modifier) {
    // Sem recorte de canto: ele cortava a primeira letra de "Saldo" e "Patrimônio", que ficam rente à margem.
    Column(modifier.clickable(onClick = onClick)) {
        Text(stringResource(R.string.home_balance), style = MaterialTheme.typography.labelLarge, color = LocalContentColor.current.copy(alpha = 0.85f))
        MoneyText(l.totalBalance, style = MaterialTheme.typography.displaySmall, color = LocalContentColor.current, animate = true)
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.home_net_worth), style = MaterialTheme.typography.bodyMedium, color = LocalContentColor.current.copy(alpha = 0.85f))
            Spacer(Modifier.width(6.dp))
            MoneyText(l.netWorth, style = MaterialTheme.typography.titleSmall, color = LocalContentColor.current, animate = true)
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, Modifier.size(18.dp))
        }
    }
}

/**
 * Uma das três colunas do mês. A legenda pode quebrar em duas linhas ("End of month", "Конец месяца"); os valores ficam
 * alinhados embaixo nas três colunas.
 */
@Composable
private fun Stat(label: String, value: Long, color: Color, icon: ImageVector, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
        Row(verticalAlignment = Alignment.Top) {
            Icon(icon, null, tint = color, modifier = Modifier.padding(top = 1.dp).size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = LocalContentColor.current.copy(alpha = 0.75f), maxLines = 2)
        }
        MoneyText(value, style = MaterialTheme.typography.titleSmall, color = color)
    }
}
