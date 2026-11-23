// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.widget.FrameLayout
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.onRoot
import androidx.glance.appwidget.GlanceRemoteViews
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.h3yk0.cryo.data.ThemeMode
import io.github.h3yk0.cryo.domain.GeneralBudget
import io.github.h3yk0.cryo.domain.Ledger
import io.github.h3yk0.cryo.domain.WidgetConfig
import io.github.h3yk0.cryo.domain.WidgetContent
import io.github.h3yk0.cryo.domain.WidgetContents
import io.github.h3yk0.cryo.domain.WidgetLine
import io.github.h3yk0.cryo.domain.WidgetMain
import io.github.h3yk0.cryo.domain.predict.MonthEnds
import io.github.h3yk0.cryo.ui.theme.CryoTheme
import io.github.h3yk0.cryo.ui.widget.WidgetConfigScreen
import io.github.h3yk0.cryo.ui.widget.WidgetTheme
import io.github.h3yk0.cryo.ui.widget.WidgetUi
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.LocalDate

/** RF-WID-01..06 (1.7, LA9): o conteúdo do widget, as escolhas, os tamanhos (imagens 135–138) e a tela de editar. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], application = TestApp::class, qualifiers = "+w393dp-h852dp-xxhdpi")
class WidgetTest {
    @get:Rule val rule = createComposeRule()

    private lateinit var c: AppContainer
    private val day = LocalDate.now()
    private val ctx get() = ApplicationProvider.getApplicationContext<android.app.Application>()

    @Before fun setUp() {
        c = AppContainer(ctx)
        runBlocking { seed(c, day) }
        rule.waitUntil(10_000) { (c.repo.snapshot.value?.txs?.size ?: 0) > 50 }
    }

    private fun ledger() = Ledger(c.repo.snapshot.value!!, day)

    /** RF-WID-02: a escolha guardada vai e volta; sem escolha (ou com lixo), o padrão. */
    @Test fun escolhaGuardada() {
        val cfg = WidgetConfig(WidgetMain.NET_WORTH, setOf(WidgetLine.GOALS, WidgetLine.BUDGETS))
        assertEquals(cfg, WidgetConfig.decode(cfg.encode()))
        assertEquals(WidgetConfig(), WidgetConfig.decode(null))
        assertEquals(WidgetConfig(), WidgetConfig.decode("QUALQUER|COISA"))
        assertEquals(setOf(WidgetLine.BILLS, WidgetLine.ENTRY), WidgetConfig().lines)
        assertEquals(WidgetMain.MONTH_END, WidgetConfig().main)
    }

    /** RF-WID-03: os mesmos números do app (o fim do mês é o MonthEnds.of; o saldo e o patrimônio, os do Ledger). */
    @Test fun mesmosNumerosDoApp() {
        val l = ledger()
        assertEquals(MonthEnds.of(l).total, WidgetContents.of(l, WidgetConfig(WidgetMain.MONTH_END)).mainValue)
        assertEquals(l.totalBalance, WidgetContents.of(l, WidgetConfig(WidgetMain.BALANCE)).mainValue)
        assertEquals(l.netWorth, WidgetContents.of(l, WidgetConfig(WidgetMain.NET_WORTH)).mainValue)
        val general = WidgetContents.of(l, WidgetConfig(WidgetMain.GENERAL))
        val usage = GeneralBudget.usage(l, java.time.YearMonth.from(day))
        if (usage == null) assertNull(general.mainValue) else assertEquals(usage.remaining, general.mainValue)
    }

    /** RF-WID-02: só o que a pessoa escolheu aparece; as próximas contas são as a pagar ainda abertas, na ordem do vencimento. */
    @Test fun soOQueFoiEscolhido() {
        val l = ledger()
        val all = WidgetContents.of(l, WidgetConfig(WidgetMain.MONTH_END, WidgetLine.entries.toSet()))
        assertTrue(all.bills.size <= WidgetContents.MAX_ROWS)
        val none = WidgetContents.of(l, WidgetConfig(WidgetMain.MONTH_END, emptySet()))
        assertTrue(none.bills.isEmpty() && none.budgets.isEmpty() && none.goals.isEmpty())
        val open = l.billStatuses(java.time.YearMonth.from(day)).filter { it.payment == null && it.bill.kind == io.github.h3yk0.cryo.data.db.CategoryKind.EXPENSE }
        if (open.isNotEmpty()) assertEquals(open.first().bill.name, all.bills.first().name)
    }

    /** Desenha o widget de verdade (o RemoteViews que o Glance entrega ao Android) e salva a imagem. */
    @OptIn(androidx.glance.appwidget.ExperimentalGlanceRemoteViewsApi::class)
    private fun render(content: WidgetContent, size: DpSize, name: String, dark: Boolean = false) {
        val rv = runBlocking { GlanceRemoteViews().compose(ctx, size) { WidgetTheme { WidgetUi(content) } } }.remoteViews
        val dm = ctx.resources.displayMetrics
        val w = (size.width.value * dm.density).toInt()
        val h = (size.height.value * dm.density).toInt()
        val view = rv.apply(ctx, FrameLayout(ctx))
        view.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY))
        view.layout(0, 0, w, h)
        val pad = (16 * dm.density).toInt()
        val bmp = Bitmap.createBitmap(w + pad * 2, h + pad * 2, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        // Um "papel de parede" neutro atrás, para ver as bordas do widget.
        canvas.drawColor(if (dark) 0xFF1E2A33.toInt() else 0xFF8FB3C9.toInt())
        canvas.translate(pad.toFloat(), pad.toFloat())
        view.draw(canvas)
        roundCorners(view, bmp, pad)
        val out = File("screenshots/$name.png").apply { parentFile?.mkdirs() }
        out.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        assertTrue(out.length() > 0)
    }

    /**
     * No Android 12+ o `cornerRadius` do Glance vira o recorte da própria vista (`clipToOutline`), que só a placa de vídeo
     * desenha: o desenho em software do teste sai com os cantos retos. Aqui o recorte é refeito à mão em cada vista que pede
     * (o widget e o botão), com a cor de fora do canto, para a imagem mostrar o que o celular mostra.
     */
    private fun roundCorners(root: View, bmp: Bitmap, pad: Int) {
        val clips = mutableListOf<Pair<android.graphics.Rect, Float>>()
        fun walk(v: View, x: Int, y: Int) {
            val left = x + v.left
            val top = y + v.top
            val provider = v.outlineProvider
            if (v.clipToOutline && provider != null && v.width > 0 && v.height > 0) {
                val o = android.graphics.Outline()
                provider.getOutline(v, o)
                if (o.radius > 0f) clips += android.graphics.Rect(left, top, left + v.width, top + v.height) to o.radius
            }
            if (v is android.view.ViewGroup) for (i in 0 until v.childCount) walk(v.getChildAt(i), left - v.scrollX, top - v.scrollY)
        }
        walk(root, pad, pad)
        // Os de dentro primeiro (o botão sobre o fundo do widget), o widget inteiro por último (sobre o papel de parede).
        for ((r, radius) in clips.reversed()) {
            val outsideX = (r.left - 1).coerceAtLeast(0)
            val outside = bmp.getPixel(outsideX, r.centerY().coerceIn(0, bmp.height - 1))
            val rad = radius.coerceAtMost(minOf(r.width(), r.height()) / 2f)
            val n = kotlin.math.ceil(rad).toInt()
            for (dy in 0 until n) for (dx in 0 until n) {
                // Distância do pixel ao centro do círculo do canto; fora do raio, a cor de fora (com meia transição na borda).
                val d = kotlin.math.hypot(rad - dx - 0.5f, rad - dy - 0.5f)
                val keep = (rad - d + 0.5f).coerceIn(0f, 1f)
                if (keep >= 1f) continue
                for ((px, py) in listOf(r.left + dx to r.top + dy, r.right - 1 - dx to r.top + dy, r.left + dx to r.bottom - 1 - dy, r.right - 1 - dx to r.bottom - 1 - dy)) {
                    if (px !in 0 until bmp.width || py !in 0 until bmp.height) continue
                    bmp.setPixel(px, py, mix(bmp.getPixel(px, py), outside, keep))
                }
            }
        }
    }

    private fun mix(inside: Int, outside: Int, keep: Float): Int {
        fun ch(shift: Int) = ((inside shr shift and 0xFF) * keep + (outside shr shift and 0xFF) * (1 - keep)).toInt().coerceIn(0, 255)
        return (0xFF shl 24) or (ch(16) shl 16) or (ch(8) shl 8) or ch(0)
    }

    /** RF-WID-01 e RF-WID-06: o widget nos três tamanhos (imagens 135–137). */
    @Test fun tresTamanhos() {
        val l = ledger()
        val all = WidgetContents.of(l, WidgetConfig(WidgetMain.MONTH_END, setOf(WidgetLine.BILLS, WidgetLine.BUDGETS, WidgetLine.ENTRY)))
        render(all, DpSize(120.dp, 60.dp), "135_widget_pequeno")
        render(all, DpSize(200.dp, 120.dp), "136_widget_medio")
        render(all, DpSize(260.dp, 300.dp), "137_widget_grande")
        val goals = WidgetContents.of(l, WidgetConfig(WidgetMain.NET_WORTH, setOf(WidgetLine.GOALS, WidgetLine.ENTRY)))
        render(goals, DpSize(260.dp, 200.dp), "137b_widget_patrimonio_metas")
    }

    /** RF-WID-06: no Android 8 a 11 o fundo arredondado vem de uma forma pronta (o Glance não arredonda ali). */
    @Config(sdk = [30])
    @Test fun androidAntigo() {
        val l = ledger()
        val all = WidgetContents.of(l, WidgetConfig(WidgetMain.MONTH_END, setOf(WidgetLine.BILLS, WidgetLine.ENTRY)))
        render(all, DpSize(260.dp, 200.dp), "137c_widget_android11")
    }

    /** RF-WID-02: a tela de editar o widget (imagem 138) muda a escolha. */
    @Config(qualifiers = "+w393dp-h1000dp-xxhdpi")
    @Test fun telaDeEditar() {
        var saved: WidgetConfig? = null
        rule.setContent {
            CryoTheme(mode = ThemeMode.LIGHT, dynamicColor = false) {
                androidx.compose.material3.Surface { WidgetConfigScreen(WidgetConfig()) { saved = it } }
            }
        }
        rule.onNodeWithText("Patrimônio").performClick()
        rule.onNodeWithText("Metas").performScrollTo().performClick()
        rule.onRoot().captureRoboImage("screenshots/138_widget_editar.png")
        rule.onNodeWithText("Salvar").performScrollTo().performClick()
        assertEquals(WidgetMain.NET_WORTH, saved?.main)
        assertTrue(WidgetLine.GOALS in saved!!.lines)
        assertTrue(WidgetLine.BILLS in saved!!.lines)
    }
}
