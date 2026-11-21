// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.ui.components

import io.github.h3yk0.cryo.domain.engine.EngineRunner
import io.github.h3yk0.cryo.domain.engine.EngineReport
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import androidx.compose.runtime.produceState
import io.github.h3yk0.cryo.R
import io.github.h3yk0.cryo.domain.Res
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.composed
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
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.rounded.Schedule

/* ============================ Acesso global ============================ */

val LocalContainer = staticCompositionLocalOf<AppContainer> { error("AppContainer ausente") }
val LocalSettings = staticCompositionLocalOf { AppSettings() }

/**
 * ERR-046: quanto o botão flutuante da tela precisa subir agora. A barra de mensagem ("Desfazer") é do app inteiro e
 * mora no Scaffold de fora; o botão "+" é de cada tela. Sem isto, a barra cobre o botão e a pessoa não consegue lançar
 * outra coisa enquanto o desfazer está na tela. O valor é a altura medida da barra (0 quando não há nenhuma).
 */
val LocalFabLift = staticCompositionLocalOf { 0.dp }

/** Levanta o botão flutuante enquanto a barra de mensagem estiver à vista. */
fun Modifier.fabLift(): Modifier = composed { padding(bottom = LocalFabLift.current) }

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

/**
 * RF-MOT-13: o resultado do motor financeiro para os dados e o dia de agora. Calcula fora da linha principal e, enquanto
 * calcula, devolve o último resultado (ou null na primeira vez). Todas as telas dividem o mesmo cálculo (cache).
 */
@Composable
fun rememberEngine(): EngineReport? {
    val c = LocalContainer.current
    val snap by c.repo.snapshot.collectAsStateWithLifecycle()
    val today = rememberToday()
    val options by c.engineStore.options.collectAsStateWithLifecycle()
    val s = snap ?: return null
    if (EngineRunner.inline) return remember(s, today, options) { c.engine.runBlocking(s, today, options) }
    val report by produceState(c.engine.cached(s, today, options) ?: c.engine.latest, s, today, options) {
        value = withContext(Dispatchers.Default) { c.engine.run(s, today, options) }
    }
    return report
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

/** RF-ILU-03: a ilustração de uma meta (no lugar do emoji que existia até a 1.6). */
@Composable
fun GoalBadge(goal: io.github.h3yk0.cryo.data.db.Goal, size: Dp = 40.dp) =
    GoalImage(io.github.h3yk0.cryo.domain.GoalArt.forGoal(goal), Color(goal.color), size)

/* ================================ Dinheiro ================================ */

/**
 * DS-COMP-03: um valor na moeda do app. [hideable] respeita o modo "esconder valores". [animate]: conta até o novo valor
 * quando ele muda (DS-MOV-05), para os números de destaque (saldo do Início).
 */
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
    animate: Boolean = false,
) {
    val hide = hideable && LocalSettings.current.hideValues
    val shown = if (animate) animateMoney(cents, enabled = !hide) else cents
    val text = when {
        hide -> Money.HIDDEN
        signed -> Money.signed(shown)
        else -> Money.format(shown)
    }
    val c = color.takeOrElse { style.color.takeOrElse { LocalContentColor.current } }
    val max = if (style.fontSize.isSpecified) style.fontSize else 16.sp
    BasicText(
        text,
        modifier = modifier.semantics { if (hide) contentDescription = Res.s(R.string.common_valor_escondido) },
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
    TxType.EXPENSE -> Res.s(R.string.wallet_despesa)
    TxType.INCOME -> Res.s(R.string.wallet_receita)
    TxType.TRANSFER -> Res.s(R.string.common_transferencia)
    TxType.CARD_PAYMENT -> Res.s(R.string.common_pagamento_fatura)
    TxType.INVEST_IN -> Res.s(R.string.wallet_aporte)
    TxType.INVEST_OUT -> Res.s(R.string.wallet_resgate)
    TxType.GOAL_IN -> Res.s(R.string.common_guardado_meta)
    TxType.GOAL_OUT -> Res.s(R.string.common_retirado_meta)
    TxType.DEBT_IN -> Res.s(R.string.common_emprestimo_recebido)
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

/**
 * Um pedaço da linha de baixo de um lançamento. [color] marca uma conta ou um cartão (RF-EXT-08: a bolinha na cor dela,
 * com o nome em destaque); [before] é o que vem antes do pedaço (" · " entre informações, " → " de onde para onde).
 */
data class TxPart(val text: String, val color: Int? = null, val before: String = " · ")

fun txSubtitleParts(t: Tx, l: Ledger): List<TxPart> {
    val parts = ArrayList<TxPart>()
    fun acc(id: Long?, before: String = " · ") = TxPart(l.accountName(id), id?.let { l.account[it]?.color }, before)
    when (t.type) {
        TxType.EXPENSE, TxType.INCOME -> {
            t.categoryId?.let { l.category[it]?.name }?.let { parts += TxPart(it) }
            if (t.cardId != null) parts += TxPart(Res.s(R.string.home_card_title, l.cardName(t.cardId)), l.card[t.cardId]?.color)
            else if (t.accountId != null) parts += acc(t.accountId)
            if (t.installmentTotal > 1) parts += TxPart(Res.s(R.string.common_parcela, t.installmentNumber, t.installmentTotal))
        }
        TxType.TRANSFER -> { parts += acc(t.accountId); parts += acc(t.toAccountId, " → ") }
        TxType.CARD_PAYMENT -> {
            parts += acc(t.accountId)
            parts += TxPart(Res.s(R.string.home_card_title, l.cardName(t.cardId)), t.cardId?.let { l.card[it]?.color }, " → ")
        }
        TxType.INVEST_IN -> { parts += acc(t.accountId); parts += TxPart(t.investmentId?.let { l.investment[it]?.name } ?: "investimento", before = " → ") }
        TxType.INVEST_OUT -> { parts += TxPart(t.investmentId?.let { l.investment[it]?.name } ?: "investimento"); parts += acc(t.accountId, " → ") }
        TxType.GOAL_IN -> { parts += acc(t.accountId); parts += TxPart(t.goalId?.let { l.goal[it]?.name } ?: "meta", before = " → ") }
        TxType.GOAL_OUT -> { parts += TxPart(t.goalId?.let { l.goal[it]?.name } ?: "meta"); parts += acc(t.accountId, " → ") }
        TxType.DEBT_IN -> { parts += TxPart(t.debtId?.let { l.debt[it]?.name } ?: "dívida"); parts += acc(t.accountId, " → ") }
    }
    if (t.billId != null) parts += TxPart(Res.s(R.string.common_conta_fixa))
    return parts
}

fun txSubtitle(t: Tx, l: Ledger): String =
    txSubtitleParts(t, l).withIndex().joinToString("") { (i, p) -> (if (i > 0) p.before else "") + p.text }

@Composable
fun TxLeadingBadge(t: Tx, l: Ledger, size: Dp = 40.dp) {
    val cat = t.categoryId?.let { l.category[it] }
    if ((t.type == TxType.EXPENSE || t.type == TxType.INCOME) && cat != null) {
        IconBadge(categoryIcon(cat.icon), Color(cat.color), size)
    } else {
        IconBadge(typeIcon(t.type), MaterialTheme.colorScheme.primary, size)
    }
}

/**
 * Linha de uma movimentação. [showDate]: põe a data no começo (listas sem cabeçalho de dia, como históricos).
 * [showSource]: mostra a conta ou o cartão com a bolinha da cor dela (RF-EXT-08); no extrato de uma conta só, não precisa.
 * [balanceAfter]: o saldo da conta depois deste lançamento (RF-CON-08, extrato da conta).
 */
@Composable
fun TxRow(
    t: Tx, l: Ledger, onClick: () -> Unit, modifier: Modifier = Modifier, showDate: Boolean = false,
    showSource: Boolean = true, balanceAfter: Long? = null,
) {
    val signed = txSignedAmount(t)
    val future = t.date.isAfter(l.today)
    val datePart = if (!showDate) "" else (if (t.date.year == l.today.year) Dates.short(t.date) else Dates.full(t.date)) + " · "
    val amountColor = when {
        signed > 0 -> incomeColor()
        signed < 0 -> MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val strong = MaterialTheme.colorScheme.onSurface
    val parts = txSubtitleParts(t, l)
    val dots = HashMap<String, InlineTextContent>()
    val sub = buildAnnotatedString {
        append(datePart)
        parts.forEachIndexed { i, p ->
            if (i > 0) append(p.before)
            val c = p.color
            if (c != null && showSource) {
                // RF-EXT-08: a bolinha na cor da conta (ou do cartão) e o nome dela em destaque.
                val id = "dot$i"
                dots[id] = InlineTextContent(Placeholder(8.sp, 8.sp, PlaceholderVerticalAlign.TextCenter)) {
                    Box(Modifier.fillMaxSize().clip(CircleShape).background(Color(c)))
                }
                appendInlineContent(id, " ")
                append(" ")
                withStyle(SpanStyle(color = strong, fontWeight = FontWeight.Medium)) { append(p.text) }
            } else {
                append(p.text)
            }
        }
        if (future) append(Res.s(R.string.common_agendado))
    }
    Row(
        modifier
            .fillMaxWidth()
            // RF-EXT-10: o agendado tem fundo de outro tom (e um reloginho junto do valor).
            .then(if (future) Modifier.background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)) else Modifier)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TxLeadingBadge(t, l)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(txTitle(t, l), style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                sub,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2, overflow = TextOverflow.Ellipsis,
                inlineContent = dots,
            )
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (future) {
                    Icon(Icons.Rounded.Schedule, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(4.dp))
                }
                MoneyText(
                    if (signed == 0L) t.amount else signed,
                    signed = signed != 0L,
                    style = MaterialTheme.typography.titleSmall,
                    color = amountColor,
                )
            }
            if (balanceAfter != null) {
                val hide = LocalSettings.current.hideValues
                Text(
                    Res.s(R.string.acc_balance_after, if (hide) Money.HIDDEN else Money.format(balanceAfter)),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (balanceAfter < 0) CryoTheme.colors.expense else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
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

/** DS-COMP-01: destaque com fundo próprio. Na 1.5, com profundidade (DS-PROF-02): sombra suave e cantos 24. */
@Composable
fun CryoCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    color: Color = Color.Unspecified,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = color.takeOrElse { CryoTheme.depth.card }
    Column(
        modifier.fillMaxWidth().depthCard(shape = MaterialTheme.shapes.large, color = c)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(16.dp),
        content = content,
    )
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
    // DS-MOV-06: cresce ao aparecer e vai até o novo valor quando muda.
    val appear by rememberAppear()
    val value by animateFloatAsState(ratio.coerceIn(0f, 1f), tween(Motion.LONG, easing = Motion.Emphasized), label = "uso")
    LinearProgressIndicator(
        progress = { value * appear },
        modifier = modifier.fillMaxWidth().height(height),
        color = color,
        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        strokeCap = StrokeCap.Round,
        gapSize = 0.dp,
        drawStopIndicator = {},
    )
}

@Composable
fun MonthSelector(
    label: String, onPrev: () -> Unit, onNext: () -> Unit, modifier: Modifier = Modifier, nextEnabled: Boolean = true,
    /** O que as setas dizem ao leitor de tela (RF-EXT-07: na busca por ano elas andam de ano em ano). */
    prevLabel: String = Res.s(R.string.common_mes_anterior), nextLabel: String = Res.s(R.string.common_proximo_mes),
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
        IconButton(onClick = onPrev) { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, prevLabel) }
        Text(
            label, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f, fill = false).padding(horizontal = 8.dp),
        )
        IconButton(onClick = onNext, enabled = nextEnabled) { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, nextLabel) }
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
            if (onBack != null) IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, Res.s(R.string.onboarding_voltar)) }
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
    dismissLabel: String = Res.s(R.string.wallet_cancelar),
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
