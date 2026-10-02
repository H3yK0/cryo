// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.ChildFriendly
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.DirectionsBus
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Laptop
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.LocalGasStation
import androidx.compose.material.icons.rounded.MedicalServices
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material.icons.rounded.RequestQuote
import androidx.compose.material.icons.rounded.Redeem
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Sell
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Subscriptions
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.VolunteerActivism
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.h3yk0.cryo.AppContainer
import io.github.h3yk0.cryo.data.AppSettings
import io.github.h3yk0.cryo.data.db.Tx
import io.github.h3yk0.cryo.data.db.TxType
import io.github.h3yk0.cryo.domain.Dates
import io.github.h3yk0.cryo.domain.Ledger
import io.github.h3yk0.cryo.domain.Money
import io.github.h3yk0.cryo.ui.theme.CryoTheme
import java.time.LocalDate

/* ============================ Acesso global ============================ */

val LocalContainer = staticCompositionLocalOf<AppContainer> { error("AppContainer ausente") }
val LocalSettings = staticCompositionLocalOf { AppSettings() }

/** Permite fixar a data "de hoje". Usado só para gerar as imagens da loja com dados de exemplo. */
val LocalFixedToday = staticCompositionLocalOf<LocalDate?> { null }

/** Data de hoje, atualizada sempre que o app volta para a tela (caso vire o dia). */
@Composable
fun rememberToday(): LocalDate {
    LocalFixedToday.current?.let { return it }
    var today by remember { mutableStateOf(LocalDate.now()) }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) today = LocalDate.now() }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }
    return today
}

/** O "livro-caixa" com todos os cálculos, recalculado quando os dados mudam. */
@Composable
fun rememberLedger(): Ledger? {
    val c = LocalContainer.current
    val snap by c.repo.snapshot.collectAsStateWithLifecycle()
    val today = rememberToday()
    val s = snap ?: return null
    return remember(s, today) { Ledger(s, today) }
}

/* ================================ Ícones ================================ */

fun categoryIcon(key: String): ImageVector = when (key) {
    "restaurant" -> Icons.Rounded.Restaurant
    "cart" -> Icons.Rounded.ShoppingCart
    "car" -> Icons.Rounded.DirectionsCar
    "bus" -> Icons.Rounded.DirectionsBus
    "home" -> Icons.Rounded.Home
    "bolt" -> Icons.Rounded.Bolt
    "water" -> Icons.Rounded.WaterDrop
    "wifi" -> Icons.Rounded.Wifi
    "phone" -> Icons.Rounded.PhoneAndroid
    "health" -> Icons.Rounded.MedicalServices
    "fitness" -> Icons.Rounded.FitnessCenter
    "school" -> Icons.Rounded.School
    "movie" -> Icons.Rounded.Movie
    "game" -> Icons.Rounded.SportsEsports
    "shopping" -> Icons.Rounded.ShoppingBag
    "subscriptions" -> Icons.Rounded.Subscriptions
    "spa" -> Icons.Rounded.Spa
    "pets" -> Icons.Rounded.Pets
    "volunteer" -> Icons.Rounded.VolunteerActivism
    "gift" -> Icons.Rounded.CardGiftcard
    "flight" -> Icons.Rounded.Flight
    "receipt" -> Icons.Rounded.Receipt
    "coffee" -> Icons.Rounded.LocalCafe
    "gas" -> Icons.Rounded.LocalGasStation
    "baby" -> Icons.Rounded.ChildFriendly
    "build" -> Icons.Rounded.Build
    "work" -> Icons.Rounded.Work
    "laptop" -> Icons.Rounded.Laptop
    "sell" -> Icons.Rounded.Sell
    "trending" -> Icons.AutoMirrored.Rounded.TrendingUp
    "undo" -> Icons.AutoMirrored.Rounded.Undo
    "redeem" -> Icons.Rounded.Redeem
    "savings" -> Icons.Rounded.Savings
    "money" -> Icons.Rounded.Payments
    "star" -> Icons.Rounded.Star
    "debt" -> Icons.Rounded.RequestQuote
    else -> Icons.Rounded.MoreHoriz
}

/** Bolinha colorida com ícone (categorias, contas, cartões). */
@Composable
fun IconBadge(icon: ImageVector, color: Color, size: Dp = 40.dp, contentDescription: String? = null) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(color.copy(alpha = if (CryoTheme.colors.isDark) 0.28f else 0.16f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription, tint = color.forBadge(), modifier = Modifier.size(size * 0.55f))
    }
}

/** Ajusta a cor do ícone para ficar legível no claro e no escuro. */
@Composable
fun Color.forBadge(): Color {
    val dark = CryoTheme.colors.isDark
    return if (dark) lerpTo(Color.White, 0.25f) else lerpTo(Color.Black, 0.12f)
}

fun Color.lerpTo(other: Color, t: Float) = Color(
    red = red + (other.red - red) * t,
    green = green + (other.green - green) * t,
    blue = blue + (other.blue - blue) * t,
    alpha = alpha,
)

@Composable
fun EmojiBadge(emoji: String, color: Color, size: Dp = 40.dp) {
    Box(
        Modifier.size(size).clip(CircleShape).background(color.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center,
    ) { Text(emoji, style = MaterialTheme.typography.titleMedium) }
}

/* ================================ Dinheiro ================================ */

/** Valor em R$. [hideable] respeita o modo "esconder valores". */
@Composable
fun MoneyText(
    cents: Long,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
    color: Color = Color.Unspecified,
    signed: Boolean = false,
    hideable: Boolean = true,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign? = null,
) {
    val hide = hideable && LocalSettings.current.hideValues
    val text = when {
        hide -> Money.HIDDEN
        signed -> Money.signed(cents)
        else -> Money.format(cents)
    }
    val c = color.takeOrElse { style.color.takeOrElse { LocalContentColor.current } }
    val max = if (style.fontSize.isSpecified) style.fontSize else 16.sp
    BasicText(
        text,
        modifier = modifier.semantics { if (hide) contentDescription = "Valor escondido" },
        style = style.merge(
            TextStyle(color = c, fontWeight = fontWeight, textAlign = textAlign ?: TextAlign.Unspecified),
        ),
        maxLines = 1,
        softWrap = false,
        autoSize = TextAutoSize.StepBased(minFontSize = 10.sp, maxFontSize = max, stepSize = 0.5.sp),
    )
}

@Composable
fun incomeColor() = CryoTheme.colors.income

@Composable
fun expenseColor() = CryoTheme.colors.expense

/* ============================ Movimentações ============================ */

fun typeLabel(t: TxType): String = when (t) {
    TxType.EXPENSE -> "Despesa"
    TxType.INCOME -> "Receita"
    TxType.TRANSFER -> "Transferência"
    TxType.CARD_PAYMENT -> "Pagamento de fatura"
    TxType.INVEST_IN -> "Aporte"
    TxType.INVEST_OUT -> "Resgate"
    TxType.GOAL_IN -> "Guardado em meta"
    TxType.GOAL_OUT -> "Retirado de meta"
    TxType.DEBT_IN -> "Empréstimo recebido"
}

fun typeIcon(t: TxType): ImageVector = when (t) {
    TxType.TRANSFER -> Icons.Rounded.SwapHoriz
    TxType.CARD_PAYMENT -> Icons.Rounded.CreditCard
    TxType.INVEST_IN, TxType.INVEST_OUT -> Icons.AutoMirrored.Rounded.TrendingUp
    TxType.GOAL_IN, TxType.GOAL_OUT -> Icons.Rounded.Flag
    TxType.INCOME -> Icons.Rounded.Payments
    TxType.EXPENSE -> Icons.Rounded.MoreHoriz
    TxType.DEBT_IN -> Icons.Rounded.RequestQuote
}

/** Efeito no bolso: + entrada, − saída, 0 movimentação interna. */
fun txSignedAmount(t: Tx): Long = when (t.type) {
    TxType.EXPENSE -> -t.amount
    TxType.INCOME -> t.amount
    else -> 0L
}

fun txTitle(t: Tx, l: Ledger): String = t.description.ifBlank {
    t.categoryId?.let { l.category[it]?.name } ?: typeLabel(t.type)
}

fun txSubtitle(t: Tx, l: Ledger): String {
    val parts = ArrayList<String>()
    when (t.type) {
        TxType.EXPENSE, TxType.INCOME -> {
            t.categoryId?.let { l.category[it]?.name }?.let { parts += it }
            if (t.cardId != null) parts += "Cartão ${l.cardName(t.cardId)}" else t.accountId?.let { parts += l.accountName(it) }
            if (t.installmentTotal > 1) parts += "parcela ${t.installmentNumber}/${t.installmentTotal}"
        }
        TxType.TRANSFER -> parts += "${l.accountName(t.accountId)} → ${l.accountName(t.toAccountId)}"
        TxType.CARD_PAYMENT -> parts += "${l.accountName(t.accountId)} → cartão ${l.cardName(t.cardId)}"
        TxType.INVEST_IN -> parts += "${l.accountName(t.accountId)} → ${t.investmentId?.let { l.investment[it]?.name } ?: "investimento"}"
        TxType.INVEST_OUT -> parts += "${t.investmentId?.let { l.investment[it]?.name } ?: "investimento"} → ${l.accountName(t.accountId)}"
        TxType.GOAL_IN -> parts += "${l.accountName(t.accountId)} → ${t.goalId?.let { l.goal[it]?.name } ?: "meta"}"
        TxType.GOAL_OUT -> parts += "${t.goalId?.let { l.goal[it]?.name } ?: "meta"} → ${l.accountName(t.accountId)}"
        TxType.DEBT_IN -> parts += "${t.debtId?.let { l.debt[it]?.name } ?: "dívida"} → ${l.accountName(t.accountId)}"
    }
    if (t.billId != null) parts += "conta fixa"
    return parts.joinToString(" · ")
}

@Composable
fun TxLeadingBadge(t: Tx, l: Ledger, size: Dp = 40.dp) {
    val cat = t.categoryId?.let { l.category[it] }
    if ((t.type == TxType.EXPENSE || t.type == TxType.INCOME) && cat != null) {
        IconBadge(categoryIcon(cat.icon), Color(cat.color), size)
    } else {
        IconBadge(typeIcon(t.type), MaterialTheme.colorScheme.primary, size)
    }
}

/** Linha de uma movimentação. [showDate]: põe a data no começo (listas sem cabeçalho de dia, como históricos). */
@Composable
fun TxRow(t: Tx, l: Ledger, onClick: () -> Unit, modifier: Modifier = Modifier, showDate: Boolean = false) {
    val signed = txSignedAmount(t)
    val future = t.date.isAfter(l.today)
    val datePart = if (!showDate) "" else (if (t.date.year == l.today.year) Dates.short(t.date) else Dates.full(t.date)) + " · "
    val amountColor = when {
        signed > 0 -> incomeColor()
        signed < 0 -> MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TxLeadingBadge(t, l)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(txTitle(t, l), style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                datePart + txSubtitle(t, l) + if (future) " · agendado" else "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        MoneyText(
            if (signed == 0L) t.amount else signed,
            signed = signed != 0L,
            style = MaterialTheme.typography.titleSmall,
            color = amountColor,
        )
    }
}

/* ================================ Layout ================================ */

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, action: (@Composable RowScope.() -> Unit)? = null) {
    Row(
        modifier.fillMaxWidth().padding(start = 4.dp, top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text, style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f).semantics { heading() },
        )
        action?.invoke(this)
    }
}

@Composable
fun CryoCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    color: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = CardDefaults.cardColors(containerColor = color)
    if (onClick != null) {
        Card(onClick = onClick, modifier = modifier.fillMaxWidth(), colors = colors, shape = MaterialTheme.shapes.large) {
            Column(Modifier.padding(16.dp), content = content)
        }
    } else {
        Card(modifier = modifier.fillMaxWidth(), colors = colors, shape = MaterialTheme.shapes.large) {
            Column(Modifier.padding(16.dp), content = content)
        }
    }
}

@Composable
fun EmptyState(icon: ImageVector, title: String, message: String, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    Column(
        modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(72.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(36.dp)) }
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(
            message, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (action != null) {
            Spacer(Modifier.height(16.dp))
            action()
        }
    }
}

/** Barra de progresso que muda de cor: normal, atenção (80%) e estourado (100%). */
@Composable
fun UsageBar(
    ratio: Float,
    modifier: Modifier = Modifier,
    height: Dp = 10.dp,
    baseColor: Color = MaterialTheme.colorScheme.primary,
    warn: Boolean = true,
) {
    val color = when {
        !warn -> baseColor
        ratio >= 1f -> CryoTheme.colors.expense
        ratio >= 0.8f -> CryoTheme.colors.warning
        else -> baseColor
    }
    LinearProgressIndicator(
        progress = { ratio.coerceIn(0f, 1f) },
        modifier = modifier.fillMaxWidth().height(height),
        color = color,
        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        strokeCap = StrokeCap.Round,
        gapSize = 0.dp,
        drawStopIndicator = {},
    )
}

@Composable
fun MonthSelector(label: String, onPrev: () -> Unit, onNext: () -> Unit, modifier: Modifier = Modifier, nextEnabled: Boolean = true) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
        IconButton(onClick = onPrev) { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, "Mês anterior") }
        Text(
            label, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f, fill = false).padding(horizontal = 8.dp),
        )
        IconButton(onClick = onNext, enabled = nextEnabled) { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, "Próximo mês") }
    }
}

@Composable
fun CryoTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    TopAppBar(
        title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        navigationIcon = {
            if (onBack != null) IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Voltar") }
        },
        actions = actions,
        scrollBehavior = scrollBehavior,
    )
}

@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    dismissLabel: String = "Cancelar",
    extra: (@Composable () -> Unit)? = null,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = { TextButton(onClick = { onConfirm() }) { Text(confirmLabel) } },
        dismissButton = {
            Row {
                extra?.invoke()
                TextButton(onClick = onDismiss) { Text(dismissLabel) }
            }
        },
    )
}

/** Faixa de aviso/dica. */
@Composable
fun Banner(icon: ImageVector, text: String, container: Color, content: Color, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    Surface(
        color = container, contentColor = content, shape = MaterialTheme.shapes.medium,
        modifier = modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Text(text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/** Pequena etiqueta arredondada (status). */
@Composable
fun Pill(text: String, container: Color, content: Color, modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(50))
            .background(container)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) { Text(text, style = MaterialTheme.typography.labelMedium, color = content, maxLines = 1) }
}

@Composable
fun OutlinedBox(modifier: Modifier = Modifier, selected: Boolean, content: @Composable () -> Unit) {
    Box(
        modifier
            .clip(MaterialTheme.shapes.medium)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                shape = MaterialTheme.shapes.medium,
            ),
    ) { content() }
}

@Composable
fun accountIcon(type: io.github.h3yk0.cryo.data.db.AccountType): ImageVector = when (type) {
    io.github.h3yk0.cryo.data.db.AccountType.CASH -> Icons.Rounded.Payments
    io.github.h3yk0.cryo.data.db.AccountType.SAVINGS -> Icons.Rounded.Savings
    io.github.h3yk0.cryo.data.db.AccountType.BENEFIT -> Icons.Rounded.Restaurant
    io.github.h3yk0.cryo.data.db.AccountType.CHECKING -> Icons.Rounded.AccountBalance
    io.github.h3yk0.cryo.data.db.AccountType.OTHER -> Icons.Rounded.AccountBalanceWallet
}

/** Cor do conteúdo atual (atalho). */
@Composable
fun contentColor() = LocalContentColor.current
