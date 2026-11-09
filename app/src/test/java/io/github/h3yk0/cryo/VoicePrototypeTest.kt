// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.h3yk0.cryo.data.db.Account
import io.github.h3yk0.cryo.data.db.CreditCard
import io.github.h3yk0.cryo.domain.Money
import io.github.h3yk0.cryo.domain.ParseContext
import io.github.h3yk0.cryo.domain.PhraseParser
import io.github.h3yk0.cryo.ui.components.IconBadge
import io.github.h3yk0.cryo.ui.components.categoryIcon
import io.github.h3yk0.cryo.ui.components.depthCard
import io.github.h3yk0.cryo.ui.components.raised
import io.github.h3yk0.cryo.ui.theme.CryoTheme
import io.github.h3yk0.cryo.data.ThemeMode
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.LocalDate
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/*
 * PROTÓTIPO (1.6, para o Hanry aprovar o visual): a tela de "ouvindo" com o eco. Fica nos testes até a aprovação; a
 * versão de verdade vai para o app com o Whisper e as animações do Compose (DS-MOV).
 *
 * A cena é desenhada a partir do tempo (t, em ms), para cada quadro sair igual: o volume da voz faz o microfone "respirar",
 * cada sílaba solta um anel de eco, cada palavra entendida entra no texto com um brilho, e o que o Cryo reconhece (valor,
 * categoria, cartão, data) vira um cartãozinho que salta.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], application = TestApp::class, qualifiers = "+w393dp-h852dp-xxhdpi")
class VoicePrototypeTest {
    @get:Rule val rule = createComposeRule()

    /** A frase falada, palavra por palavra, com o momento (ms) em que cada uma é reconhecida. */
    private val words = listOf(700 to "Mercado", 1350 to "120", 1750 to "reais", 2150 to "no", 2450 to "Cartão", 2850 to "Roxo", 3350 to "ontem")

    /** Sílabas (ms, força de 0 a 1): cada uma é um pico de voz e solta um anel de eco. */
    private val syllables = listOf(
        480 to 0.7, 600 to 0.9, 720 to 0.6, // mer-ca-do
        1080 to 0.95, 1200 to 0.7, 1310 to 0.55, 1420 to 0.85, 1530 to 0.6, // cen-to-e-vin-te
        1650 to 0.75, 1760 to 0.5, // re-ais
        2060 to 0.45, // no
        2300 to 0.85, 2430 to 0.65, // car-tão
        2690 to 0.9, 2810 to 0.6, // ro-xo
        3180 to 0.8, 3300 to 0.55, // on-tem
    )
    private val endOfSpeech = 3500
    private val understood = 4300

    private val ctx by lazy {
        JvmStrings.install()
        ParseContext(
            LocalDate.of(2026, 11, 9), listOf(Account(1, "Conta principal", color = 0)),
            listOf(CreditCard(1, "Cartão Roxo", 500_000, 3, 10, 0)), Personas.categories, defaultAccountId = 1,
        )
    }

    /** Volume da voz no instante t: soma de "montinhos" em volta de cada sílaba. */
    private fun level(t: Int): Float {
        var v = 0.0
        for ((at, s) in syllables) {
            val d = (t - at) / 70.0
            v += s * exp(-d * d)
        }
        return min(1.0, v + 0.04 * (1 + sin(t / 37.0))).toFloat().takeIf { t < endOfSpeech + 150 } ?: 0f
    }

    private fun easeOut(x: Float) = 1 - (1 - x) * (1 - x) * (1 - x)

    /** Salto de mola: passa um pouco de 1 e volta. */
    private fun spring(x: Float): Float {
        if (x <= 0f) return 0f
        if (x >= 1f) return 1f
        return (1 - exp(-6.0 * x) * kotlin.math.cos(9.0 * x)).toFloat()
    }

    @Composable
    private fun Scene(t: Int) {
        val primary = MaterialTheme.colorScheme.primary
        val heard = words.filter { it.first <= t }.map { it.second }
        val parsed = if (heard.isEmpty()) null else PhraseParser(ctx).parse(heard.joinToString(" "))
        val done = t >= understood
        Column(Modifier.fillMaxSize().background(CryoTheme.depth.grouped).padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(48.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!done) Box(Modifier.size(10.dp).clip(CircleShape).background(CryoTheme.colors.expense))
                else Icon(Icons.Rounded.CheckCircle, null, tint = CryoTheme.colors.income, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    if (done) "Entendi" else if (t > endOfSpeech) "Entendendo…" else "Ouvindo…",
                    style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.weight(1f))
                Text("só no celular", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            // O texto falado, palavra por palavra: cada palavra nova entra subindo, com um brilho que se apaga.
            FlowRow(Modifier.fillMaxWidth().height(132.dp).padding(top = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                words.filter { it.first <= t }.forEach { (at, w) ->
                    val k = ((t - at) / 450f).coerceIn(0f, 1f)
                    Text(
                        w, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold,
                        color = lerp(primary, MaterialTheme.colorScheme.onSurface, easeOut(k)),
                        modifier = Modifier.graphicsLayer { translationY = (1 - easeOut(min(1f, k * 2))) * 26f; alpha = min(1f, k * 3) },
                    )
                }
            }
            // O eco: anéis que saem do microfone a cada sílaba.
            Box(Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val c = Offset(size.width / 2, size.height / 2)
                    val r0 = 48.dp.toPx()
                    val rMax = size.minDimension / 2
                    val lv = level(t)
                    drawCircle(Brush.radialGradient(listOf(primary.copy(alpha = 0.10f + 0.22f * lv), Color.Transparent), c, r0 * (1.6f + 1.4f * lv)), r0 * (1.6f + 1.4f * lv), c)
                    for ((at, s) in syllables) {
                        val x = (t - at) / 1100f
                        if (x < 0f || x > 1f) continue
                        val r = r0 + (rMax - r0) * easeOut(x)
                        val a = (1 - x) * (1 - x) * (0.20f + 0.55f * s.toFloat())
                        drawCircle(primary.copy(alpha = a), r, c, style = Stroke((1.5f + 5f * s.toFloat() * (1 - x)).dp.toPx()))
                    }
                }
                val lv = level(t)
                Box(
                    Modifier.size(96.dp).scale(1f + 0.14f * lv).raised(CircleShape, primary, shadowColor = primary.copy(alpha = 0.45f), glow = (14 + 18 * lv).dp),
                    contentAlignment = Alignment.Center,
                ) { Icon(if (done) Icons.Rounded.CheckCircle else Icons.Rounded.Mic, null, tint = Color.White, modifier = Modifier.size(40.dp)) }
            }
            // O que o Cryo entendeu, em cartõezinhos que saltam quando aparecem.
            val chips = buildList {
                parsed?.amount?.let { add(Triple(words.first { it.second == "120" }.first + 150, Icons.Rounded.Payments, Money.format(it))) }
                parsed?.categoryId?.let { id -> Personas.categories.firstOrNull { it.id == id }?.let { add(Triple(words[0].first + 250, categoryIcon(it.icon), it.name)) } }
                if (parsed?.cardId != null) add(Triple(words[5].first + 150, Icons.Rounded.CreditCard, "Cartão Roxo"))
                if (parsed != null && parsed.date != ctx.today) add(Triple(words[6].first + 150, Icons.Rounded.CalendarToday, "Ontem"))
            }
            FlowRow(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                chips.forEach { (at, icon, label) -> Chip(icon, label, spring(((t - at) / 500f)), alpha = min(1f, max(0f, (t - at) / 160f))) }
            }
            Spacer(Modifier.weight(1f))
            if (done) {
                val k = spring((t - understood) / 500f)
                Surface(
                    shape = RoundedCornerShape(50), color = primary,
                    modifier = Modifier.fillMaxWidth().height(56.dp).scale(0.9f + 0.1f * k).alpha(min(1f, (t - understood) / 200f)),
                ) { Box(contentAlignment = Alignment.Center) { Text("Salvar · R$ 120,00", color = Color.White, style = MaterialTheme.typography.titleMedium) } }
            }
            Spacer(Modifier.height(40.dp))
        }
    }

    @Composable
    private fun Chip(icon: ImageVector, label: String, k: Float, alpha: Float) {
        Row(
            Modifier.scale(0.6f + 0.4f * k).alpha(alpha).depthCard(RoundedCornerShape(50)).padding(start = 6.dp, end = 14.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconBadge(icon, MaterialTheme.colorScheme.primary, 30.dp)
            Spacer(Modifier.width(8.dp))
            Text(label, style = MaterialTheme.typography.titleSmall)
        }
    }

    /** Quadros da animação (só com -PtourGif=true): app/screenshots/voz_gif/, para montar o GIF. */
    @Test fun quadrosDoEco() {
        assumeTrue(System.getProperty("cryo.tourGif") == "true")
        val t = mutableIntStateOf(0)
        rule.mainClock.autoAdvance = false
        rule.setContent { CryoTheme(mode = ThemeMode.LIGHT, dynamicColor = false) { Scene(t.intValue) } }
        val dir = File("screenshots/voz_gif").apply { mkdirs() }
        var frame = 0
        while (t.intValue <= 5600) {
            rule.mainClock.advanceTimeByFrame()
            rule.onRoot().captureRoboImage(File(dir, "f_%03d.png".format(frame++)).path)
            t.intValue += 60
        }
    }
}
