// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.ui.widget

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.material3.ColorProviders
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import io.github.h3yk0.cryo.CryoApp
import io.github.h3yk0.cryo.MainActivity
import io.github.h3yk0.cryo.R
import io.github.h3yk0.cryo.data.WidgetPrefs
import io.github.h3yk0.cryo.domain.Ledger
import io.github.h3yk0.cryo.domain.Money
import io.github.h3yk0.cryo.domain.Res
import io.github.h3yk0.cryo.domain.WidgetBar
import io.github.h3yk0.cryo.domain.WidgetContent
import io.github.h3yk0.cryo.domain.WidgetContents
import io.github.h3yk0.cryo.domain.WidgetLine
import io.github.h3yk0.cryo.notify.Reminders
import io.github.h3yk0.cryo.ui.theme.CryoPalette
import java.time.LocalDate

/**
 * RF-WID-01..07 (1.7, LA9): o widget do Cryo na tela inicial. Ele se arruma pelo tamanho (pequeno: o número; médio: o
 * número e duas linhas; grande: as linhas escolhidas e o botão "Lançar"), mostra sempre os dados de verdade (nunca o modo
 * de teste) e os valores mesmo com o bloqueio (RF-WID-04: pôr o widget na tela é escolher a conveniência).
 */
class CryoWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(setOf(SMALL, MEDIUM, WIDE, LARGE, TALL))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val widgetId = runCatching { GlanceAppWidgetManager(context).getAppWidgetId(id) }.getOrDefault(0)
        val content = WidgetData.load(context, widgetId)
        provideContent { WidgetTheme { WidgetUi(content) } }
    }

    companion object {
        val SMALL = DpSize(110.dp, 56.dp)
        val MEDIUM = DpSize(180.dp, 110.dp)
        val WIDE = DpSize(260.dp, 110.dp)
        val LARGE = DpSize(260.dp, 200.dp)
        val TALL = DpSize(260.dp, 300.dp)
    }
}

/** O Android chama este recebedor para pôr, atualizar e tirar o widget. */
class CryoWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CryoWidget()

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        runCatching { WidgetPrefs(context).remove(appWidgetIds) }
    }
}

/** RF-WID-03: os números do widget, sempre dos dados de verdade. Nada aqui pode derrubar o widget nem o app. */
object WidgetData {
    suspend fun load(context: Context, widgetId: Int): WidgetContent {
        val config = runCatching { WidgetPrefs(context).config(widgetId) }.getOrDefault(io.github.h3yk0.cryo.domain.WidgetConfig())
        return runCatching {
            val app = context.applicationContext as CryoApp
            WidgetContents.of(Ledger(app.real.repo.loadOnce(), LocalDate.now()), config)
        }.getOrElse { emptyContent(config) }
    }

    fun emptyContent(config: io.github.h3yk0.cryo.domain.WidgetConfig) = WidgetContent(
        Res.s(R.string.app_name), null, 0, Res.s(R.string.widget_empty), emptyList(), emptyList(), emptyList(), config, empty = true,
    )
}

/** RF-WID-03: atualiza os widgets que estão na tela (quando algo muda no app). Sem widget nenhum, não faz nada. */
object WidgetUpdates {
    suspend fun refresh(context: Context) {
        runCatching {
            if (GlanceAppWidgetManager(context).getGlanceIds(CryoWidget::class.java).isNotEmpty()) CryoWidget().updateAll(context)
        }
    }
}

/** RF-WID-06: as cores do papel de parede no Android 12+; antes disso, as do Cryo (claro e escuro com o sistema). */
@Composable
internal fun WidgetTheme(content: @Composable () -> Unit) {
    if (Build.VERSION.SDK_INT >= 31) GlanceTheme(content = content)
    else GlanceTheme(colors = ColorProviders(light = CryoPalette.light, dark = CryoPalette.dark), content = content)
}

private val incomeColor = ColorProvider(day = CryoPalette.lightExt.income, night = CryoPalette.darkExt.income)
private val expenseColor = ColorProvider(day = CryoPalette.lightExt.expense, night = CryoPalette.darkExt.expense)

/**
 * Quanto cabe em cada tamanho (RF-WID-06: nada cortado). O topo (nome, número, dica) e o botão "Lançar" têm altura certa;
 * o que sobra vai para as linhas, na ordem em que aparecem (contas, orçamentos, metas), só as que cabem inteiras.
 */
internal data class WidgetFit(
    val padV: Int, val labelSp: Int, val valueSp: Int, val showHint: Boolean, val entry: Boolean,
    val bills: Int, val budgets: Int, val goals: Int, val noBills: Boolean, val oneLineBills: Boolean,
)

internal fun widgetFit(c: WidgetContent, widthDp: Float, heightDp: Float): WidgetFit {
    val h = heightDp
    val padV = if (h < 80) 8 else 12
    val labelH = if (h < 80) 16 else 18
    val valueSp = when {
        h >= 190 -> 28
        h >= 80 -> 22
        else -> 18
    }
    val valueH = if (c.mainValue == null) 0 else valueSp + 8
    val showHint = c.mainHint != null && (h >= 80 || c.mainValue == null)
    val hintH = if (showHint) 18 else 0
    val entry = WidgetLine.ENTRY in c.config.lines && !c.empty && h >= 150
    var left = h - (2 * padV + labelH + valueH + hintH + 6 + if (entry) 48 else 0)
    fun take(have: Int, rowH: Int): Int {
        val n = minOf(have, (left / rowH).toInt().coerceAtLeast(0))
        left -= n * rowH
        return n
    }
    // Até o widget médio, cada conta numa linha só ("Academia · em 5 dias"); no grande, em duas.
    val bills = take(c.bills.size, if (h < 150) 20 else 36)
    val noBills = WidgetLine.BILLS in c.config.lines && c.bills.isEmpty() && !c.empty && left >= 18
    if (noBills) left -= 18
    val budgets = take(c.budgets.size, 32)
    val goals = take(c.goals.size, 32)
    return WidgetFit(padV, if (h < 80) 11 else 12, valueSp, showHint, entry, bills, budgets, goals, noBills, oneLineBills = h < 150)
}

/** RF-WID-01, RF-WID-05 e RF-WID-06: o desenho do widget, pelo tamanho que ele tem agora. */
@Composable
internal fun WidgetUi(c: WidgetContent) {
    val size = LocalSize.current
    val compact = size.width < 170.dp
    val fit = widgetFit(c, size.width.value, size.height.value)
    val context = androidx.glance.LocalContext.current
    val open = Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
    Column(
        roundedBackground(GlanceModifier.fillMaxSize(), button = false)
            .padding(horizontal = 14.dp, vertical = fit.padV.dp).clickable(actionStartActivity(open)),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(ImageProvider(R.drawable.ic_cryo_badge), null, GlanceModifier.size(if (fit.labelSp < 12) 14.dp else 16.dp))
            Spacer(GlanceModifier.width(6.dp))
            Text(c.mainLabel, style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = fit.labelSp.sp), maxLines = 1)
        }
        val value = c.mainValue
        if (value != null) {
            Text(
                if (compact) Money.compact(value) else (if (c.mainSign > 0) "+ " else "") + Money.format(value),
                style = TextStyle(
                    fontSize = fit.valueSp.sp, fontWeight = FontWeight.Bold,
                    color = when {
                        c.mainSign > 0 -> incomeColor
                        c.mainSign < 0 || value < 0 -> expenseColor
                        else -> GlanceTheme.colors.onSurface
                    },
                ),
                maxLines = 1,
            )
        }
        if (fit.showHint) c.mainHint?.let { h ->
            Text(h, style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp), maxLines = 1)
        }
        if (fit.bills + fit.budgets + fit.goals > 0 || fit.noBills) Spacer(GlanceModifier.height(6.dp))
        c.bills.take(fit.bills).forEach { b ->
            Row(GlanceModifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                if (fit.oneLineBills) {
                    Text(
                        b.name + " · " + b.detail, GlanceModifier.defaultWeight(),
                        style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 12.sp), maxLines = 1,
                    )
                } else {
                    Column(GlanceModifier.defaultWeight()) {
                        Text(b.name, style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 13.sp, fontWeight = FontWeight.Medium), maxLines = 1)
                        Text(b.detail, style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 11.sp), maxLines = 1)
                    }
                }
                Text(Money.format(b.amount), style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = if (fit.oneLineBills) 12.sp else 13.sp), maxLines = 1)
            }
        }
        if (fit.noBills) {
            Text(Res.s(R.string.widget_no_bills), style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp), maxLines = 1)
        }
        c.budgets.take(fit.budgets).forEach { BarRow(it) }
        c.goals.take(fit.goals).forEach { BarRow(it) }
        if (fit.entry) {
            Spacer(GlanceModifier.defaultWeight())
            // RF-WID-05: "Lançar" abre o registro por frase (passa pelo bloqueio, como sempre).
            val entry = Intent(context, MainActivity::class.java).putExtra(Reminders.EXTRA_OPEN, "entry")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            Box(
                roundedBackground(GlanceModifier.fillMaxWidth().height(40.dp), button = true)
                    .clickable(actionStartActivity(entry)),
                contentAlignment = Alignment.Center,
            ) {
                Text(Res.s(R.string.widget_entry), style = TextStyle(color = GlanceTheme.colors.onPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium))
            }
        }
    }
}

/**
 * RF-WID-06: os cantos arredondados. No Android 12+ o Glance arredonda e usa as cores do papel de parede; no 8 a 11 ele não
 * arredonda, então o fundo é uma forma pronta (claro e escuro com o sistema).
 */
@Composable
private fun roundedBackground(m: GlanceModifier, button: Boolean): GlanceModifier =
    if (Build.VERSION.SDK_INT >= 31) {
        if (button) m.cornerRadius(20.dp).background(GlanceTheme.colors.primary) else m.cornerRadius(22.dp).background(GlanceTheme.colors.widgetBackground)
    } else {
        m.background(ImageProvider(if (button) R.drawable.widget_button_bg else R.drawable.widget_bg))
    }

@Composable
private fun BarRow(b: WidgetBar) {
    Column(GlanceModifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(b.name, GlanceModifier.defaultWeight(), style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 13.sp), maxLines = 1)
            Text(b.detail, style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp), maxLines = 1)
        }
        Spacer(GlanceModifier.height(3.dp))
        LinearProgressIndicator(
            progress = b.ratio.coerceIn(0f, 1f),
            modifier = GlanceModifier.fillMaxWidth().height(6.dp),
            color = if (b.ratio > 1f) expenseColor else GlanceTheme.colors.primary,
            backgroundColor = GlanceTheme.colors.surfaceVariant,
        )
    }
}
