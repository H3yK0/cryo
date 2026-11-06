// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.ui.screens

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.h3yk0.cryo.R
import io.github.h3yk0.cryo.data.Palette
import io.github.h3yk0.cryo.domain.AppLocale
import io.github.h3yk0.cryo.domain.Dates
import io.github.h3yk0.cryo.domain.Money
import io.github.h3yk0.cryo.domain.Num
import io.github.h3yk0.cryo.domain.Res
import io.github.h3yk0.cryo.ui.LocalNav
import io.github.h3yk0.cryo.ui.components.CryoTopBar
import io.github.h3yk0.cryo.ui.components.IconBadge
import io.github.h3yk0.cryo.ui.components.animationsOn
import io.github.h3yk0.cryo.ui.components.Motion
import io.github.h3yk0.cryo.ui.components.PageDots
import io.github.h3yk0.cryo.ui.components.categoryIcon
import io.github.h3yk0.cryo.ui.theme.CryoTheme
import kotlinx.coroutines.launch
import java.text.BreakIterator
import java.time.YearMonth
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.math.sin

/*
 * RF-TOUR: "Como usar o Cryo", um passeio de 5 páginas para quem nunca ouviu falar do app. Cada página tem uma animação curta
 * que mostra a função funcionando (a frase sendo digitada e virando um lançamento, a previsão se desenhando...).
 *
 * As animações são desenhadas com os próprios componentes do app, sem biblioteca nem arquivos de animação (PRC-07): pesam
 * nada no APK e seguem o tema, a língua e a moeda. Cada uma tem uma linha do tempo de 0 a 1 que anda quando a página aparece
 * e para no quadro final (RF-TOUR-06, DS-MOV-10).
 */

/** Exemplos do passeio na moeda do app, em valores que parecem de verdade em cada país (RF-TOUR-04). */
internal object TourExamples {
    /** Um almoço, em centésimos da moeda. */
    private val LUNCH = mapOf(
        "BRL" to 3_250L, "USD" to 1_250L, "EUR" to 1_250L, "GBP" to 950L, "CAD" to 1_600L, "AUD" to 1_800L,
        "MXN" to 15_000L, "ARS" to 1_500_000L, "CLP" to 800_000L, "COP" to 3_000_000L, "PEN" to 2_500L, "UYU" to 45_000L,
        "CHF" to 2_000L, "KRW" to 1_200_000L, "INR" to 25_000L, "CNY" to 3_500L, "JPY" to 120_000L, "TWD" to 15_000L,
        "HKD" to 8_000L, "SGD" to 1_200L,
    )

    fun lunch(currency: String = AppLocale.currency.currencyCode): Long = LUNCH[currency] ?: 1_250L

    /** Arredonda para 2 algarismos na unidade da moeda (980, 380, 48.000): os outros exemplos saem do almoço e ficam redondos. */
    fun nice(cents: Long): Long {
        val units = cents / 100.0
        if (units < 100) return cents
        val step = 10.0.pow(floor(log10(units)) - 1)
        return ((units / step).roundToLong() * step * 100).roundToLong()
    }

    fun lunchPhrase(): String = Res.s(R.string.tour_1_phrase, Money.typed(lunch()))
    fun tv(): Long = nice(lunch() * 40)
    fun tvPhrase(): String = Res.s(R.string.tour_3_phrase, Money.typed(tv()))
}

private class TourPage(@StringRes val title: Int, @StringRes val text: Int, val scene: @Composable (Float) -> Unit)

private val PAGES = listOf(
    TourPage(R.string.tour_1_title, R.string.tour_1_text) { SceneQuickEntry(it) },
    TourPage(R.string.onboarding_saiba_como_mes_vai, R.string.tour_2_text) { SceneForecast(it) },
    TourPage(R.string.tour_3_title, R.string.tour_3_text) { SceneCards(it) },
    TourPage(R.string.tour_4_title, R.string.tour_4_text) { ScenePlan(it) },
    TourPage(R.string.onboarding_seus_dados_sao_seus, R.string.tour_5_text) { ScenePrivacy(it) },
)

/** Quanto dura a animação de cada página (RF-TOUR-06: até cerca de 3 s). */
internal const val SCENE_MS = 2_800

/** Quantas páginas o passeio tem. */
internal val TOUR_PAGES: Int get() = PAGES.size

/**
 * O passeio. [lastLabel] é o botão da última página ("Vamos lá" no primeiro uso, "Entendi" nos Ajustes).
 * [onSkip]: mostra "Pular" no topo (primeiro uso). O Voltar do celular volta uma página; na primeira, chama [onBackFromFirst].
 */
@Composable
fun Tour(
    lastLabel: String,
    onFinish: () -> Unit,
    onBackFromFirst: () -> Unit,
    modifier: Modifier = Modifier,
    onSkip: (() -> Unit)? = null,
    initialPage: Int = 0,
) {
    val pager = rememberPagerState(initialPage) { PAGES.size }
    val scope = rememberCoroutineScope()
    val last = pager.currentPage == PAGES.lastIndex
    BackHandler {
        if (pager.currentPage > 0) scope.launch { pager.animateScrollToPage(pager.currentPage - 1) } else onBackFromFirst()
    }
    Column(modifier.fillMaxSize()) {
        Box(Modifier.fillMaxWidth().height(48.dp).padding(horizontal = 8.dp)) {
            if (onSkip != null && !last) {
                TextButton(onClick = onSkip, modifier = Modifier.align(Alignment.CenterEnd)) { Text(Res.s(R.string.onboarding_pular)) }
            }
        }
        HorizontalPager(pager, Modifier.weight(1f), verticalAlignment = Alignment.Top) { i ->
            val page = PAGES[i]
            val t = rememberTimeline(active = pager.settledPage == i)
            // Centralizado na altura quando cabe; rola quando a fonte do sistema é grande (ACS-06).
            BoxWithConstraints(Modifier.fillMaxSize()) {
            val viewport = maxHeight
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).heightIn(min = viewport).padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                // A ilustração é decorativa para o leitor de tela: o título e o texto dizem tudo (RF-TOUR-06).
                Box(Modifier.fillMaxWidth().height(320.dp).clearAndSetSemantics {}, contentAlignment = Alignment.Center) { page.scene(t) }
                Spacer(Modifier.height(20.dp))
                Text(
                    Res.s(page.title), style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().semantics { heading() },
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    Res.s(page.text), style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(16.dp))
            }
            }
        }
        PageDots(pager.currentPage, PAGES.size, Modifier.align(Alignment.CenterHorizontally).padding(vertical = 12.dp))
        Button(
            onClick = { if (last) onFinish() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } },
            modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 24.dp, top = 4.dp).height(56.dp),
        ) { Text(if (last) lastLabel else Res.s(R.string.tour_next)) }
    }
}

/** TELA-TOUR (RF-TOUR-03): o passeio aberto pelos Ajustes. */
@Composable
fun TourScreen() {
    val nav = LocalNav.current
    Scaffold(
        topBar = { CryoTopBar(Res.s(R.string.tour_how_to_use), onBack = nav::back) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { pad ->
        Tour(
            lastLabel = Res.s(R.string.tour_done), onFinish = nav::back, onBackFromFirst = nav::back,
            modifier = Modifier.padding(pad).navigationBarsPadding(),
        )
    }
}

/**
 * A linha do tempo da página: vai de 0 a 1 quando a página para na tela e recomeça ao voltar a ela. Com "Remover animações"
 * (escala 0), o Compose termina a animação na hora: aparece direto o quadro final (ACS-08).
 */
@Composable
private fun rememberTimeline(active: Boolean): Float {
    val enabled = animationsOn()
    val t = remember { Animatable(if (enabled) 0f else 1f) }
    LaunchedEffect(active) {
        if (active && enabled) {
            t.snapTo(0f)
            t.animateTo(1f, tween(SCENE_MS, easing = LinearEasing))
        }
    }
    return t.value
}

/** Quanto do trecho [a]..[b] da linha do tempo já passou (0 a 1), com a curva do Material. */
private fun seg(t: Float, a: Float, b: Float): Float = Motion.Emphasized.transform(((t - a) / (b - a)).coerceIn(0f, 1f))

/* ================================== Cenas ================================== */

/** A moldura das cenas: um cartão claro, como uma tela do app em miniatura. */
@Composable
private fun SceneCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow, shape = RoundedCornerShape(28.dp),
        modifier = Modifier.widthIn(max = 360.dp).fillMaxWidth(),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

/** Uma etiqueta que aparece crescendo ([p] de 0 a 1). */
@Composable
private fun SceneChip(icon: ImageVector?, text: String, p: Float) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.graphicsLayer { alpha = p; val s = 0.8f + 0.2f * p; scaleX = s; scaleY = s },
    ) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(icon, null, Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
            }
            Text(text, style = MaterialTheme.typography.labelLarge, maxLines = 1)
        }
    }
}

/** Uma barra de progresso desenhada pela própria cena (a [UsageBar] do app anima sozinha e atrasaria a linha do tempo). */
@Composable
private fun SceneBar(fraction: Float, color: Color, track: Color = MaterialTheme.colorScheme.surfaceContainerHighest) {
    Box(Modifier.fillMaxWidth().height(10.dp).background(track, CircleShape)) {
        Box(Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).height(10.dp).background(color, CircleShape))
    }
}

/** O texto até onde a pessoa já "digitou", sem cortar uma letra no meio (acentos do hindi, por exemplo). */
private fun typedPart(text: String, fraction: Float): String {
    if (fraction >= 1f) return text
    val bi = BreakIterator.getCharacterInstance(AppLocale.locale).apply { setText(text) }
    val bounds = generateSequence(bi.first()) { bi.next().takeIf { n -> n != BreakIterator.DONE } }.toList()
    val n = ((bounds.size - 1) * fraction).roundToInt()
    return text.substring(0, bounds[n])
}

/** Página 1 (RF-TOUR-01): a frase é digitada, o Cryo entende (etiquetas) e vira um lançamento. */
@Composable
private fun SceneQuickEntry(t: Float) {
    val lunch = TourExamples.lunch()
    val phrase = remember(lunch) { TourExamples.lunchPhrase() }
    val typing = ((t - 0.03f) / 0.37f).coerceIn(0f, 1f)
    val food = Res.s(R.string.cat_restaurant_name)
    SceneCard {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, shape = CircleShape, modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                // O cursor fica no fim enquanto a frase é digitada.
                Text(
                    typedPart(phrase, typing) + if (typing in 0.001f..0.999f) "▏" else "",
                    style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Box(
                    Modifier.size(40.dp).background(MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.Mic, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(20.dp)) }
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SceneChip(categoryIcon("restaurant"), food, seg(t, 0.44f, 0.58f))
            SceneChip(null, Money.format(lunch), seg(t, 0.51f, 0.65f))
            SceneChip(Icons.Rounded.AccountBalance, Res.s(R.string.wallet_conta_principal), seg(t, 0.58f, 0.72f))
        }
        val row = seg(t, 0.74f, 0.94f)
        Surface(
            color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().graphicsLayer { alpha = row; translationY = (1f - row) * 28.dp.toPx() },
        ) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                IconBadge(categoryIcon("restaurant"), Color(Palette.pick(15)))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(Res.s(R.string.tour_1_lunch), style = MaterialTheme.typography.bodyLarge, maxLines = 1)
                    Text(
                        "$food · ${Res.s(R.string.date_today)}", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1,
                    )
                }
                Text(Money.signed(-lunch), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, maxLines = 1)
            }
        }
    }
}

/** Página 2 (RF-TOUR-01): a linha do mês se desenha até hoje, a projeção segue até o fim do mês e o resultado aparece. */
@Composable
private fun SceneForecast(t: Float) {
    val l = TourExamples.lunch()
    val end = TourExamples.nice(l * 8)
    val perDay = TourExamples.nice(l * 2)
    val line = seg(t, 0.05f, 0.55f)
    val proj = seg(t, 0.55f, 0.82f)
    val dot = seg(t, 0.80f, 0.95f)
    val income = CryoTheme.colors.income
    val primary = MaterialTheme.colorScheme.primary
    val grid = MaterialTheme.colorScheme.outlineVariant
    // O mês da cena, em "almoços": o salário entra no dia 1, os gastos descem até hoje (dia 16) e a projeção vai até o dia 30.
    val actual = remember {
        val noise = floatArrayOf(0f, 0.8f, -0.4f, 1.1f, 0.2f, -0.6f, 0.9f, 0f, -0.3f, 0.7f, -0.2f, 0.5f, -0.5f, 0.3f, 0f, 0f)
        List(16) { d -> 40f - 1.5f * d + noise[d] }
    }
    val endUnits = end.toFloat() / l
    SceneCard {
        Text(
            Res.s(R.string.chart_fim_mes, Money.format((end * dot).roundToLong())),
            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = income,
            modifier = Modifier.graphicsLayer { alpha = 0.35f + 0.65f * dot },
        )
        Canvas(Modifier.fillMaxWidth().height(150.dp)) {
            val maxV = 42f
            fun x(day: Int) = (day - 1) / 29f * size.width
            fun y(v: Float) = size.height - 6.dp.toPx() - v / maxV * (size.height - 12.dp.toPx())
            drawLine(grid, Offset(0f, y(0f)), Offset(size.width, y(0f)), 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)))
            // Realizado: desenhado da esquerda para a direita (corte pela largura já desenhada).
            val path = Path()
            val area = Path()
            actual.forEachIndexed { i, v ->
                if (i == 0) { path.moveTo(x(1), y(v)); area.moveTo(x(1), y(0f)); area.lineTo(x(1), y(v)) } else { path.lineTo(x(i + 1), y(v)); area.lineTo(x(i + 1), y(v)) }
            }
            area.lineTo(x(16), y(0f)); area.close()
            clipRect(right = x(16) * line) {
                drawPath(area, primary.copy(alpha = 0.12f))
                drawPath(path, primary, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
            if (line >= 1f) {
                drawCircle(primary, 5.dp.toPx(), Offset(x(16), y(actual.last())))
            }
            // Projeção tracejada até o fim do mês.
            if (proj > 0f) {
                val from = Offset(x(16), y(actual.last()))
                val to = Offset(x(30), y(endUnits))
                val p = Path().apply { moveTo(from.x, from.y); lineTo(from.x + (to.x - from.x) * proj, from.y + (to.y - from.y) * proj) }
                drawPath(p, income, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f))))
            }
            if (dot > 0f) drawCircle(income, 6.dp.toPx() * dot, Offset(x(30), y(endUnits)))
        }
        Text(
            Res.s(R.string.tour_2_per_day, Money.format(perDay)), style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.graphicsLayer { alpha = dot },
        )
    }
}

/** Página 3 (RF-TOUR-01): "tv em 10x" se espalha pelas 10 faturas seguintes e o limite usado sobe. */
@Composable
private fun SceneCards(t: Float) {
    val tv = TourExamples.tv()
    val limit = TourExamples.nice(TourExamples.lunch() * 100)
    val used = 0.2f + 0.4f * seg(t, 0.62f, 0.9f)
    val phrase = remember(tv) { TourExamples.tvPhrase() }
    val next = remember { YearMonth.now().plusMonths(1) }
    SceneCard {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh, shape = CircleShape,
            modifier = Modifier.graphicsLayer { alpha = seg(t, 0f, 0.12f) },
        ) {
            Text(phrase, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
        }
        Surface(color = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary, shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.CreditCard, null)
                    Spacer(Modifier.width(10.dp))
                    Text(Res.s(R.string.onboarding_cartao_credito), style = MaterialTheme.typography.titleSmall)
                }
                Spacer(Modifier.height(12.dp))
                Text(Res.s(R.string.home_available_limit, Money.format((limit * (1f - used)).roundToLong())), style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(6.dp))
                SceneBar(used, MaterialTheme.colorScheme.onPrimary, MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.25f))
            }
        }
        Text(Res.s(R.string.tour_3_installments, Money.format(tv / 10)), style = MaterialTheme.typography.labelLarge)
        // As 10 parcelas, uma por fatura, aparecendo uma depois da outra.
        for (r in 0 until 2) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (c in 0 until 5) {
                    val i = r * 5 + c
                    val p = seg(t, 0.16f + i * 0.04f, 0.28f + i * 0.04f)
                    Box(
                        Modifier.weight(1f).height(34.dp)
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(Modifier.fillMaxSize().graphicsLayer { alpha = p }.background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(10.dp)))
                        Text(
                            Dates.monthShort(next.plusMonths(i.toLong())), style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer, maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

/** Página 4 (RF-TOUR-01): o lembrete toca, o orçamento enche e a meta avança. */
@Composable
private fun ScenePlan(t: Float) {
    val l = TourExamples.lunch()
    val rent = TourExamples.nice(l * 30)
    val goal = TourExamples.nice(l * 120)
    val warn = CryoTheme.colors.warning
    val bell = seg(t, 0.10f, 0.45f)
    val budget = 0.7f * seg(t, 0.38f, 0.68f)
    val saved = 0.45f * seg(t, 0.62f, 0.92f)
    SceneCard {
        Surface(
            color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().graphicsLayer { val a = seg(t, 0f, 0.14f); alpha = a; translationY = (1f - a) * -16.dp.toPx() },
        ) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(36.dp).background(warn.copy(alpha = 0.16f), CircleShape), contentAlignment = Alignment.Center) {
                    // O sino balança e para.
                    Icon(
                        Icons.Rounded.NotificationsActive, null, tint = warn,
                        modifier = Modifier.size(20.dp).graphicsLayer { rotationZ = sin(bell * 6 * PI).toFloat() * 18f * (1f - bell) },
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    Res.s(R.string.alert_bill_due, Res.s(R.string.sample_aluguel), Res.s(R.string.date_rel_tomorrow), Money.format(rent)),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(categoryIcon("cart"), Color(Palette.pick(9)))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row {
                    Text(Res.s(R.string.cat_cart_name), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    Text(Num.percent((budget * 100).roundToInt()), style = MaterialTheme.typography.labelLarge)
                }
                Spacer(Modifier.height(6.dp))
                SceneBar(budget, MaterialTheme.colorScheme.primary)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            val ringColor = Color(Palette.pick(6))
            val track = MaterialTheme.colorScheme.surfaceContainerHighest
            Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val w = 5.dp.toPx()
                    val d = size.minDimension - w
                    val tl = Offset(w / 2, w / 2)
                    drawArc(track, 0f, 360f, false, tl, Size(d, d), style = Stroke(w))
                    drawArc(ringColor, -90f, 360f * saved, false, tl, Size(d, d), style = Stroke(w, cap = StrokeCap.Round))
                }
                Icon(categoryIcon("flight"), null, tint = ringColor, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(Res.s(R.string.cat_flight_name), style = MaterialTheme.typography.bodyLarge)
                Text(
                    "${Money.format((goal * saved).roundToLong())} / ${Money.format(goal)}",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Página 5 (RF-TOUR-01): o cadeado fecha e aparecem as garantias de privacidade. */
@Composable
private fun ScenePrivacy(t: Float) {
    val s = seg(t, 0f, 0.3f)
    val click = seg(t, 0.42f, 0.58f)
    SceneCard {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Box(
                Modifier.size(120.dp)
                    .graphicsLayer { alpha = s; val k = 0.6f + 0.4f * s; scaleX = k; scaleY = k }
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (t >= 0.45f) Icons.Rounded.Lock else Icons.Rounded.LockOpen, null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(56.dp).graphicsLayer { val k = 1f + 0.15f * sin(click * PI).toFloat(); scaleX = k; scaleY = k },
                )
            }
        }
        FlowRow(
            Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SceneChip(Icons.Rounded.CloudOff, Res.s(R.string.tour_5_offline), seg(t, 0.55f, 0.7f))
            SceneChip(Icons.Rounded.PhoneAndroid, Res.s(R.string.tour_5_on_device), seg(t, 0.65f, 0.8f))
            SceneChip(Icons.Rounded.Fingerprint, Res.s(R.string.tour_5_lock), seg(t, 0.75f, 0.9f))
        }
    }
}
