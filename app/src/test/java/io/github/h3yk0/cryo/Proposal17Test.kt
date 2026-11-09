// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.EventNote
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.h3yk0.cryo.data.AppSettings
import io.github.h3yk0.cryo.data.ThemeMode
import io.github.h3yk0.cryo.ui.LocalNav
import io.github.h3yk0.cryo.ui.Navigator
import io.github.h3yk0.cryo.ui.components.IconBadge
import io.github.h3yk0.cryo.ui.components.LocalContainer
import io.github.h3yk0.cryo.ui.components.LocalSettings
import io.github.h3yk0.cryo.ui.components.depthCard
import io.github.h3yk0.cryo.ui.components.raised
import io.github.h3yk0.cryo.ui.screens.HomeScreen
import io.github.h3yk0.cryo.ui.theme.CryoTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.LocalDate
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.sin
import kotlin.random.Random

/*
 * PROPOSTA DA 1.7 (para o Hanry aprovar): barra de baixo com o botão de voz ao alcance do polegar (como a One UI), como
 * ficariam as notificações e dois "momentos" animados (meta concluída e limite estourado). Fica nos testes até a aprovação;
 * a versão de verdade vai para o app. Só roda com -PtourGif=true (imagens 63–66 e quadros em app/screenshots/proposta17/).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], application = TestApp::class, qualifiers = "+w393dp-h852dp-xxhdpi")
class Proposal17Test {
    @get:Rule val rule = createComposeRule()
    private lateinit var c: AppContainer
    private val settings = AppSettings(onboardingDone = true, defaultAccountId = 1, dynamicColor = false)

    @Before fun setUp() {
        assumeTrue(System.getProperty("cryo.tourGif") == "true")
        io.github.h3yk0.cryo.domain.engine.EngineRunner.inline = true
        c = AppContainer(ApplicationProvider.getApplicationContext())
        runBlocking { seed(c, LocalDate.now()); c.autoBackup.clear() }
        val deadline = System.currentTimeMillis() + 10_000
        while ((c.repo.snapshot.value?.txs?.size ?: 0) < 50 && System.currentTimeMillis() < deadline) Thread.sleep(50)
    }

    @Composable
    private fun Wrap(content: @Composable () -> Unit) {
        CryoTheme(mode = ThemeMode.LIGHT, dynamicColor = false) {
            val nav = rememberNavController()
            val navigator = remember { Navigator(nav) }
            CompositionLocalProvider(LocalContainer provides c, LocalSettings provides settings, LocalNav provides navigator) {
                Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) { content() }
            }
        }
    }

    /* ------------------------------------ barra com o botão de voz ------------------------------------ */

    @Composable
    private fun BarItem(icon: ImageVector, label: String, selected: Boolean, modifier: Modifier) {
        Column(modifier.padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.size(width = 56.dp, height = 30.dp).clip(RoundedCornerShape(50))
                    .background(if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent),
                contentAlignment = Alignment.Center,
            ) { Icon(icon, null, tint = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant) }
            Spacer(Modifier.height(4.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
        }
    }

    /** A barra de baixo da proposta: Início, Extrato, [microfone], Planejar, Análises. */
    @Composable
    private fun VoiceBar() {
        val d = CryoTheme.depth
        val primary = MaterialTheme.colorScheme.primary
        Box(Modifier.fillMaxWidth().height(104.dp)) {
            Row(
                Modifier.fillMaxWidth().height(80.dp).align(Alignment.BottomCenter)
                    .dropShadow(RectangleShape, Shadow(radius = 16.dp, color = d.shadow, offset = DpOffset(0.dp, (-2).dp)))
                    .background(d.card),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BarItem(Icons.Rounded.Home, "Início", true, Modifier.weight(1f))
                BarItem(Icons.AutoMirrored.Rounded.ReceiptLong, "Extrato", false, Modifier.weight(1f))
                Spacer(Modifier.weight(1.1f))
                BarItem(Icons.AutoMirrored.Rounded.EventNote, "Planejar", false, Modifier.weight(1f))
                BarItem(Icons.Rounded.Insights, "Análises", false, Modifier.weight(1f))
            }
            Column(Modifier.align(Alignment.TopCenter), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier.size(68.dp).raised(CircleShape, primary, shadowColor = primary.copy(alpha = 0.45f), glow = 16.dp),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.Mic, null, tint = Color.White, modifier = Modifier.size(32.dp)) }
                Spacer(Modifier.height(6.dp))
                Text("Falar", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = primary)
            }
        }
    }

    @Composable
    private fun HomeWithVoiceBar() {
        Scaffold(bottomBar = { VoiceBar() }) { pad -> Box(Modifier.padding(pad)) { HomeScreen() } }
    }

    @Test fun barraComVoz() {
        rule.setContent { Wrap { HomeWithVoiceBar() } }
        rule.waitForIdle()
        rule.onRoot().captureRoboImage("screenshots/63_proposta_barra_voz.png")
    }

    /** As zonas do polegar segurando com uma mão (o princípio da One UI: tocar embaixo, olhar em cima). */
    @Test fun zonaDoPolegar() {
        rule.setContent {
            Wrap {
                Box(Modifier.fillMaxSize()) {
                    HomeWithVoiceBar()
                    Canvas(Modifier.fillMaxSize()) {
                        val h = size.height
                        val bands = listOf(
                            Triple(0f, 0.36f, Color(0xFFE53935)), Triple(0.36f, 0.62f, Color(0xFFFFB300)), Triple(0.62f, 1f, Color(0xFF43A047)),
                        )
                        for ((a, b, col) in bands) drawRect(col.copy(alpha = 0.16f), Offset(0f, h * a), Size(size.width, h * (b - a)))
                    }
                    Label("Difícil: precisa esticar ou usar as duas mãos", Color(0xFFC62828), Modifier.align(Alignment.TopStart).padding(top = 250.dp, start = 12.dp))
                    Label("Esticando o dedo", Color(0xFFB26A00), Modifier.align(Alignment.TopStart).padding(top = 470.dp, start = 12.dp))
                    Label("Fácil: o polegar alcança", Color(0xFF2E7D32), Modifier.align(Alignment.BottomStart).padding(bottom = 112.dp, start = 12.dp))
                }
            }
        }
        rule.waitForIdle()
        rule.onRoot().captureRoboImage("screenshots/64_proposta_zona_polegar.png")
    }

    @Composable
    private fun Label(text: String, color: Color, modifier: Modifier) {
        Text(
            text, color = Color.White, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold,
            modifier = modifier.clip(RoundedCornerShape(50)).background(color.copy(alpha = 0.92f)).padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }

    /* ------------------------------------------ notificações ------------------------------------------ */

    private class Note(
        val title: String, val text: String, val time: String = "agora",
        val progress: Float? = null, val chart: Boolean = false, val actions: List<String> = emptyList(), val danger: Boolean = false,
    )

    @Composable
    private fun NoteCard(n: Note) {
        val primary = MaterialTheme.colorScheme.primary
        Column(Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(24.dp)).background(Color.White).padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.foundation.Image(painterResource(R.drawable.ic_cryo_badge), null, Modifier.size(20.dp).clip(RoundedCornerShape(5.dp)))
                Spacer(Modifier.width(8.dp))
                Text("Cryo · ${n.time}", style = MaterialTheme.typography.labelMedium, color = Color(0xFF5F6368))
                Spacer(Modifier.weight(1f))
                Icon(Icons.Rounded.KeyboardArrowDown, null, tint = Color(0xFF5F6368), modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text(n.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = Color(0xFF1F1F1F))
            Text(n.text, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF444746))
            n.progress?.let {
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { it }, modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50)),
                    color = if (n.danger) CryoTheme.colors.expense else Color(0xFFF29900), trackColor = Color(0xFFE8EAED), drawStopIndicator = {},
                )
            }
            if (n.chart) {
                Spacer(Modifier.height(10.dp))
                WeekChart(1f, Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0xFFF1F5F8)))
            }
            if (n.actions.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    n.actions.forEach { Text(it, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = primary) }
                }
            }
        }
    }

    /** O gráfico da semana que iria dentro da notificação (no Android 12+ ele pode vir animado: as barras crescendo). */
    @Composable
    private fun WeekChart(k: Float, modifier: Modifier) {
        val primary = MaterialTheme.colorScheme.primary
        val days = listOf("S", "T", "Q", "Q", "S", "S", "D")
        val v = listOf(0.35f, 0.6f, 0.2f, 0.45f, 0.9f, 0.7f, 0.25f)
        Column(modifier.padding(12.dp)) {
            Canvas(Modifier.fillMaxWidth().height(96.dp)) {
                val w = size.width / v.size
                // A linha da média: o que é normal para você.
                val avg = size.height * (1 - 0.55f)
                drawLine(Color(0xFF9AA0A6), Offset(0f, avg), Offset(size.width, avg), strokeWidth = 1.5.dp.toPx(), pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 8f)))
                v.forEachIndexed { i, x ->
                    val hh = size.height * x * k
                    drawRoundRect(
                        if (x > 0.55f) primary.copy(alpha = 0.55f) else primary, Offset(i * w + w * 0.22f, size.height - hh), Size(w * 0.56f, hh),
                        androidx.compose.ui.geometry.CornerRadius(6.dp.toPx()),
                    )
                }
            }
            Row { days.forEach { Text(it, Modifier.weight(1f), textAlign = TextAlign.Center, fontSize = 11.sp, color = Color(0xFF5F6368)) } }
        }
    }

    @Config(qualifiers = "+w393dp-h1160dp-xxhdpi")
    @Test fun notificacoes() {
        val notes = listOf(
            Note("Alimentação: 80% do limite", "Ainda é dia 18. Segurando o delivery até sexta, você fecha o mês no azul.", progress = 0.8f),
            Note("A fatura do Roxo fecha amanhã", "Compras a partir de amanhã só vencem em janeiro: bom dia para aquela compra grande 😉", time = "8:55"),
            Note("Luz vence amanhã · R$ 180,00", "Já separou? Se já pagou, marque aqui mesmo.", time = "9:00", actions = listOf("Já paguei", "Lembrar amanhã")),
            Note("Hoje está zerado por aqui", "Foi um dia sem gastos? Sua sequência está em 6 dias 🔥", time = "20:30", actions = listOf("Dia sem gastos ✓", "Anotar")),
            Note("Sua semana: R$ 340,00", "15% a menos que o normal. Sexta foi o dia mais caro 👏", time = "dom 19:00", chart = true),
            Note("Faltam R$ 200 para a Viagem para a praia 🏖️", "No seu ritmo, você chega lá em 3 semanas.", time = "ontem"),
        )
        rule.setContent {
            CryoTheme(mode = ThemeMode.LIGHT, dynamicColor = false) {
                Column(
                    Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF0E2A3A), Color(0xFF1B4B5F), Color(0xFF3A6E7A))))
                        .padding(horizontal = 12.dp, vertical = 20.dp),
                ) {
                    Text("Notificações", color = Color.White, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 8.dp, bottom = 8.dp))
                    notes.forEach { NoteCard(it) }
                }
            }
        }
        rule.waitForIdle()
        rule.onRoot().captureRoboImage("screenshots/65_proposta_notificacoes.png")
    }

    /** Na tela de bloqueio: o Cryo avisa que tem algo, sem nome nem valor (RF-SEG). */
    @Config(qualifiers = "+w393dp-h852dp-xxhdpi")
    @Test fun telaDeBloqueio() {
        rule.setContent {
            CryoTheme(mode = ThemeMode.DARK, dynamicColor = false) {
                Column(
                    Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF0B1F2B), Color(0xFF16394A)))).padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(Modifier.height(90.dp))
                    Text("9:41", color = Color.White, fontSize = 84.sp, fontWeight = FontWeight.Light)
                    Text("Segunda-feira, 9 de novembro", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(48.dp))
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Color.White.copy(alpha = 0.16f)).padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            androidx.compose.foundation.Image(painterResource(R.drawable.ic_cryo_badge), null, Modifier.size(20.dp).clip(RoundedCornerShape(5.dp)))
                            Spacer(Modifier.width(8.dp))
                            Text("Cryo · agora", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelMedium)
                        }
                        Spacer(Modifier.height(6.dp))
                        Text("3 avisos novos", color = Color.White, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text("Desbloqueie para ver", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodyMedium)
                    }
                    Spacer(Modifier.weight(1f))
                    Text(
                        "Na tela de bloqueio, nada de nomes nem valores: só depois de desbloquear.",
                        color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 40.dp),
                    )
                }
            }
        }
        rule.waitForIdle()
        rule.onRoot().captureRoboImage("screenshots/66_proposta_bloqueio.png")
    }

    /* ------------------------------------------ momentos animados ------------------------------------------ */

    private fun easeOut(x: Float) = 1 - (1 - x) * (1 - x) * (1 - x)
    private fun clamp(x: Float) = x.coerceIn(0f, 1f)
    private fun spring(x: Float): Float = if (x <= 0f) 0f else if (x >= 1f) 1f else (1 - exp(-6.0 * x) * cos(9.0 * x)).toFloat()

    private class Particle(val angle: Float, val speed: Float, val spin: Float, val size: Float, val color: Color, val diamond: Boolean)

    private val particles: List<Particle> = Random(3).let { r ->
        val colors = listOf(Color.White, Color(0xFFBEE3F8), Color(0xFF9ED0E6), Color(0xFFE8C26A), Color(0xFFF3D98B))
        List(54) {
            Particle(
                angle = (r.nextFloat() * 2 * PI).toFloat(), speed = 0.45f + r.nextFloat() * 0.75f, spin = (r.nextFloat() - 0.5f) * 720f,
                size = 6f + r.nextFloat() * 10f, color = colors[r.nextInt(colors.size)], diamond = r.nextFloat() < 0.65f,
            )
        }
    }

    /** Meta concluída: o anel completa, o ícone salta, cristais de gelo explodem e caem. */
    @Composable
    private fun GoalMoment(t: Int) {
        val gold = Color(0xFFE8C26A)
        val ring = 0.92f + 0.08f * easeOut(clamp(t / 600f))
        val pop = 1f + 0.18f * (spring(clamp((t - 600) / 500f)) - clamp((t - 600) / 500f)).coerceAtLeast(-1f)
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF0D3B4F), Color(0xFF155E75), Color(0xFF0D3B4F)))), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2, size.height * 0.40f)
                val tt = (t - 600) / 1000f
                if (tt > 0f) for (p in particles) {
                    val life = clamp(tt * 1.1f)
                    val dist = p.speed * size.width * 0.75f * easeOut(clamp(tt * 1.6f))
                    val fall = 900f * tt * tt
                    val pos = center + Offset(cos(p.angle) * dist, sin(p.angle) * dist + fall)
                    val a = 1f - clamp((tt - 0.9f) / 0.6f)
                    rotate(p.spin * tt, pos) {
                        if (p.diamond) {
                            val s = p.size * 1.4f
                            val path = Path().apply { moveTo(pos.x, pos.y - s); lineTo(pos.x + s * 0.55f, pos.y); lineTo(pos.x, pos.y + s); lineTo(pos.x - s * 0.55f, pos.y); close() }
                            drawPath(path, p.color.copy(alpha = a * (if (life < 1f) 1f else 1f)))
                        } else drawCircle(p.color.copy(alpha = a), p.size * 0.5f, pos)
                    }
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.offset(y = (-40).dp)) {
                Box(Modifier.size(170.dp), contentAlignment = Alignment.Center) {
                    Canvas(Modifier.fillMaxSize()) {
                        val sw = 12.dp.toPx()
                        drawArc(Color.White.copy(alpha = 0.18f), 0f, 360f, false, Offset(sw / 2, sw / 2), Size(size.width - sw, size.height - sw), style = Stroke(sw))
                        drawArc(lerp(Color(0xFF8FD3F4), gold, clamp((t - 500) / 300f)), -90f, 360f * ring, false, Offset(sw / 2, sw / 2), Size(size.width - sw, size.height - sw), style = Stroke(sw, cap = StrokeCap.Round))
                    }
                    Box(Modifier.size(118.dp).scale(pop).clip(CircleShape).background(Color.White.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                        Text("🏖️", fontSize = 58.sp)
                    }
                }
                Spacer(Modifier.height(28.dp))
                val k1 = easeOut(clamp((t - 700) / 450f))
                Text(
                    "Meta concluída!", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.graphicsLayer { translationY = (1 - k1) * 40f; alpha = k1 },
                )
                val k2 = easeOut(clamp((t - 850) / 450f))
                Text(
                    "Viagem para a praia · R$ 4.000,00 guardados", color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp).graphicsLayer { translationY = (1 - k2) * 30f; alpha = k2 },
                )
                Text(
                    "Foram 7 meses. Bora para a próxima?", color = Color.White.copy(alpha = 0.75f), style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 6.dp).alpha(k2),
                )
            }
            val k3 = spring(clamp((t - 1150) / 500f))
            Surface(
                shape = RoundedCornerShape(50), color = gold,
                modifier = Modifier.align(Alignment.BottomCenter).padding(start = 24.dp, end = 24.dp, bottom = 56.dp).fillMaxWidth().height(56.dp)
                    .scale(0.85f + 0.15f * k3).alpha(clamp((t - 1150) / 200f)),
            ) { Box(contentAlignment = Alignment.Center) { Text("Ver minhas metas", color = Color(0xFF3A2A00), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) } }
        }
    }

    /** Limite estourado (ao salvar o gasto): a barra passa dos 100%, o cartão treme, e o Cryo sugere de onde tirar. */
    @Composable
    private fun LimitMoment(t: Int) {
        val red = CryoTheme.colors.expense
        val amber = Color(0xFFF29900)
        val slide = spring(clamp(t / 450f))
        val fill = 0.92f + 0.16f * easeOut(clamp((t - 350) / 650f))
        val shake = if (t in 950..1450) (sin((t - 950) / 500f * 6 * PI) * 14 * (1 - (t - 950) / 500f)).toFloat() else 0f
        Box(Modifier.fillMaxSize().background(CryoTheme.depth.grouped)) {
            // Por trás: a tela de onde a pessoa salvou (aqui só uma sugestão dela), escurecida.
            Column(Modifier.fillMaxSize().padding(20.dp)) {
                repeat(5) { Box(Modifier.fillMaxWidth().height(72.dp).padding(vertical = 6.dp).depthCard()) }
            }
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.38f * slide)))
            Column(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                    .graphicsLayer { translationY = (1 - slide) * 900f; translationX = shake.dp.toPx() }
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)).background(CryoTheme.depth.card).padding(24.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(Icons.Rounded.Restaurant, red, 44.dp)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Alimentação passou do limite", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        Text("Faltam 12 dias para o mês acabar", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(20.dp))
                Box(Modifier.fillMaxWidth().height(14.dp)) {
                    Canvas(Modifier.fillMaxSize()) {
                        val r = androidx.compose.ui.geometry.CornerRadius(size.height / 2)
                        drawRoundRect(Color(0xFFE8EAED), cornerRadius = r)
                        val over = fill > 1f
                        val w = size.width / 1.15f
                        drawRoundRect(if (over) lerp(amber, red, clamp((fill - 1f) / 0.03f)) else amber, size = Size(w * fill, size.height), cornerRadius = r)
                        drawLine(Color(0xFF3C4043), Offset(w, -6f), Offset(w, size.height + 6f), strokeWidth = 2.dp.toPx())
                    }
                }
                Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
                    Text(io.github.h3yk0.cryo.domain.Money.format((100_000 * fill).toLong()), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = if (fill > 1f) red else MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.weight(1f))
                    Text("limite R$ 1.000,00", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                val tip = easeOut(clamp((t - 1450) / 450f))
                Row(
                    Modifier.padding(top = 18.dp).fillMaxWidth().alpha(tip).graphicsLayer { translationY = (1 - tip) * 24f }
                        .clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.secondaryContainer).padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.Savings, null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                    Spacer(Modifier.width(10.dp))
                    Text("Lazer ainda tem R$ 150,00 livres. Passar R$ 80,00 de lá para cobrir?", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                }
                Spacer(Modifier.height(16.dp))
                Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.primary, modifier = Modifier.fillMaxWidth().height(54.dp).alpha(max(0.35f, tip))) {
                    Box(contentAlignment = Alignment.Center) { Text("Passar R$ 80,00 de Lazer", color = Color.White, style = MaterialTheme.typography.titleMedium) }
                }
                Text("Entendi", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium, modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 14.dp, bottom = 8.dp))
            }
        }
    }

    private fun frames(name: String, lastMs: Int, scene: @Composable (Int) -> Unit) {
        val t = mutableIntStateOf(0)
        rule.mainClock.autoAdvance = false
        rule.setContent { CryoTheme(mode = ThemeMode.LIGHT, dynamicColor = false) { scene(t.intValue) } }
        val dir = File("screenshots/proposta17/$name").apply { deleteRecursively(); mkdirs() }
        var frame = 0
        while (t.intValue <= lastMs) {
            rule.mainClock.advanceTimeByFrame()
            rule.onRoot().captureRoboImage(File(dir, "f_%03d.png".format(frame++)).path)
            t.intValue += 40
        }
    }

    @Test fun momentoMeta() = frames("meta", 2600) { GoalMoment(it) }

    @Test fun momentoLimite() = frames("limite", 2600) { LimitMoment(it) }

    @Test fun momentoMetaImagem() {
        rule.setContent { CryoTheme(mode = ThemeMode.LIGHT, dynamicColor = false) { GoalMoment(1500) } }
        rule.waitForIdle()
        rule.onRoot().captureRoboImage("screenshots/67_proposta_meta.png")
    }

    @Test fun momentoLimiteImagem() {
        rule.setContent { CryoTheme(mode = ThemeMode.LIGHT, dynamicColor = false) { LimitMoment(2600) } }
        rule.waitForIdle()
        rule.onRoot().captureRoboImage("screenshots/68_proposta_limite.png")
    }

}
