// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.h3yk0.cryo.data.ThemeMode

/* Paleta própria "gelo" usada quando as cores do papel de parede (Material You) estão desligadas
 * ou o aparelho é anterior ao Android 12. */
private val LightColors = lightColorScheme(
    primary = Color(0xFF00677F), onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFB8EAFF), onPrimaryContainer = Color(0xFF001F28),
    secondary = Color(0xFF4C626B), onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCFE6F1), onSecondaryContainer = Color(0xFF071E26),
    tertiary = Color(0xFF5B5B7E), onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFE1DFFF), onTertiaryContainer = Color(0xFF181837),
    error = Color(0xFFBA1A1A), onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6), onErrorContainer = Color(0xFF410002),
    background = Color(0xFFF6FAFD), onBackground = Color(0xFF171C1F),
    surface = Color(0xFFF6FAFD), onSurface = Color(0xFF171C1F),
    surfaceVariant = Color(0xFFDBE4E8), onSurfaceVariant = Color(0xFF40484C),
    outline = Color(0xFF70787D), outlineVariant = Color(0xFFBFC8CC),
    inverseSurface = Color(0xFF2C3134), inverseOnSurface = Color(0xFFEDF1F4), inversePrimary = Color(0xFF5FD4FD),
    surfaceContainerLowest = Color(0xFFFFFFFF), surfaceContainerLow = Color(0xFFF0F4F7),
    surfaceContainer = Color(0xFFEAEEF1), surfaceContainerHigh = Color(0xFFE4E9EC),
    surfaceContainerHighest = Color(0xFFDFE3E6),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF5FD4FD), onPrimary = Color(0xFF003543),
    primaryContainer = Color(0xFF004D61), onPrimaryContainer = Color(0xFFB8EAFF),
    secondary = Color(0xFFB3CAD4), onSecondary = Color(0xFF1E333C),
    secondaryContainer = Color(0xFF354A53), onSecondaryContainer = Color(0xFFCFE6F1),
    tertiary = Color(0xFFC3C3EB), onTertiary = Color(0xFF2D2D4D),
    tertiaryContainer = Color(0xFF434465), onTertiaryContainer = Color(0xFFE1DFFF),
    error = Color(0xFFFFB4AB), onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A), onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF0F1417), onBackground = Color(0xFFDFE3E6),
    surface = Color(0xFF0F1417), onSurface = Color(0xFFDFE3E6),
    surfaceVariant = Color(0xFF40484C), onSurfaceVariant = Color(0xFFBFC8CC),
    outline = Color(0xFF8A9296), outlineVariant = Color(0xFF40484C),
    inverseSurface = Color(0xFFDFE3E6), inverseOnSurface = Color(0xFF2C3134), inversePrimary = Color(0xFF00677F),
    surfaceContainerLowest = Color(0xFF0A0F11), surfaceContainerLow = Color(0xFF171C1F),
    surfaceContainer = Color(0xFF1B2023), surfaceContainerHigh = Color(0xFF262B2E),
    surfaceContainerHighest = Color(0xFF303538),
)

/** Cores de significado fixo (entrada = verde, saída = vermelho, atenção = âmbar), sempre com bom contraste. */
@Immutable
data class CryoColors(
    val income: Color,
    val incomeContainer: Color,
    val onIncomeContainer: Color,
    val expense: Color,
    val expenseContainer: Color,
    val onExpenseContainer: Color,
    val warning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    /** DS-ICO-05: o dourado da marca (o C do ícone). Só para a marca e celebrações, nunca para entrada/saída de dinheiro. */
    val gold: Color,
    val goldDeep: Color,
    val isDark: Boolean,
)

/**
 * DS-PROF (1.5): as cores da profundidade, calculadas a partir do esquema de cores do momento (paleta "gelo" ou Material You).
 * [grouped] é o fundo das telas; [card], o dos cartões que ficam por cima; as sombras são largas e claras.
 */
@Immutable
data class CryoDepth(
    val grouped: Color,
    val card: Color,
    val cardFaded: Color,
    val shadow: Color,
    val shadowContact: Color,
    val highlight: Color,
    val headerStart: Color,
    val headerEnd: Color,
    val headerGlow: Color,
    val headerShadow: Color,
)

private val LightExt = CryoColors(
    income = Color(0xFF1B6D2F), incomeContainer = Color(0xFFB6F2BB), onIncomeContainer = Color(0xFF002109),
    expense = Color(0xFFB3261E), expenseContainer = Color(0xFFFFDAD6), onExpenseContainer = Color(0xFF410002),
    warning = Color(0xFF8A5100), warningContainer = Color(0xFFFFDCBE), onWarningContainer = Color(0xFF2C1600),
    gold = Color(0xFFF0A81E), goldDeep = Color(0xFFB9780A),
    isDark = false,
)

private val DarkExt = CryoColors(
    income = Color(0xFF8BD893), incomeContainer = Color(0xFF00531C), onIncomeContainer = Color(0xFFB6F2BB),
    expense = Color(0xFFFFB4AB), expenseContainer = Color(0xFF93000A), onExpenseContainer = Color(0xFFFFDAD6),
    warning = Color(0xFFFFB870), warningContainer = Color(0xFF693C00), onWarningContainer = Color(0xFFFFDCBE),
    gold = Color(0xFFFFC94D), goldDeep = Color(0xFFB9780A),
    isDark = true,
)

/**
 * RF-WID-06 (1.7): as cores do Cryo para quem desenha fora do Compose do app (o widget, com o Glance). As mesmas do app, no
 * claro e no escuro.
 */
object CryoPalette {
    val light: androidx.compose.material3.ColorScheme get() = LightColors
    val dark: androidx.compose.material3.ColorScheme get() = DarkColors
    val lightExt: CryoColors get() = LightExt
    val darkExt: CryoColors get() = DarkExt
}

val LocalCryoColors = staticCompositionLocalOf { LightExt }

/**
 * DS-TOM-01 (reforma visual da 1.6, LA10-1): um tom de prioridade. [container] é o fundo pastel do bloco; [onContainer], o texto
 * (contraste de 4,5:1 ou mais sobre o fundo); [accent], o ícone e o número em destaque.
 */
@Immutable
data class CryoTone(val container: Color, val onContainer: Color, val accent: Color)

/**
 * DS-TOM-01: os quatro tons brandos, por prioridade. São de significado (como o verde e o vermelho): não mudam com o papel de
 * parede. DS-TOM-02: só nos blocos que pedem prioridade, no máximo um ou dois por tela.
 */
@Immutable
data class CryoTones(val alert: CryoTone, val attention: CryoTone, val good: CryoTone, val info: CryoTone)

private val LightTones = CryoTones(
    alert = CryoTone(container = Color(0xFFFBE7E6), onContainer = Color(0xFF561716), accent = Color(0xFFBF3A35)),
    attention = CryoTone(container = Color(0xFFFFF0DC), onContainer = Color(0xFF4A2A00), accent = Color(0xFFA86200)),
    good = CryoTone(container = Color(0xFFE2F3E5), onContainer = Color(0xFF123B1C), accent = Color(0xFF2B7A3B)),
    info = CryoTone(container = Color(0xFFE2F0F7), onContainer = Color(0xFF0D3747), accent = Color(0xFF1C7595)),
)

private val DarkTones = CryoTones(
    alert = CryoTone(container = Color(0xFF3B2324), onContainer = Color(0xFFFFDAD7), accent = Color(0xFFFF8C86)),
    attention = CryoTone(container = Color(0xFF3A2D1B), onContainer = Color(0xFFFFE1BF), accent = Color(0xFFFFB866)),
    good = CryoTone(container = Color(0xFF1D3323), onContainer = Color(0xFFCAEFD1), accent = Color(0xFF88D898)),
    info = CryoTone(container = Color(0xFF1A2F39), onContainer = Color(0xFFCFEAF6), accent = Color(0xFF7DCDE9)),
)

val LocalCryoTones = staticCompositionLocalOf { LightTones }

/**
 * DS-COR-06 (1.7, RF-ANA-02): a paleta de temperatura do calendário de calor, do frio ao quente. [levels] são os níveis 1 a 5
 * (azul-claro, azul, amarelo, laranja, vermelho; o nível 0, sem gasto, fica no tom do cartão) e [on] é a cor do número do
 * dia em cada um, com contraste de 4,5:1 ou mais. Não muda com o papel de parede: é significado, como os tons (DS-TOM-01).
 */
@Immutable
data class CryoHeat(val levels: List<Color>, val on: List<Color>)

private val LightHeat = CryoHeat(
    levels = listOf(Color(0xFFD6ECFA), Color(0xFF7DBBEA), Color(0xFFF7D774), Color(0xFFF4A259), Color(0xFFC8372D)),
    on = listOf(Color(0xFF0B3550), Color(0xFF062A44), Color(0xFF3D2E00), Color(0xFF3B1D00), Color(0xFFFFFFFF)),
)

private val DarkHeat = CryoHeat(
    levels = listOf(Color(0xFF1F3B52), Color(0xFF2F6FA3), Color(0xFFC9A43A), Color(0xFFD2793A), Color(0xFFC63E35)),
    on = listOf(Color(0xFFCFE8FA), Color(0xFFFFFFFF), Color(0xFF1F1600), Color(0xFF1F0E00), Color(0xFFFFFFFF)),
)

val LocalCryoHeat = staticCompositionLocalOf { LightHeat }

/** O degradê do ícone (DS-ICO-02), usado no cabeçalho do Início quando as cores do papel de parede estão desligadas. */
private val IconStart = Color(0xFF0A4F66)
private val IconEnd = Color(0xFF168FAD)

private fun depthFor(scheme: ColorScheme, dark: Boolean, dynamic: Boolean): CryoDepth {
    // Com Material You, o cabeçalho segue a cor principal do papel de parede (mais escura em cima, como a luz do ícone).
    val start = if (dynamic) lerp(scheme.primary, Color.Black, if (dark) 0.62f else 0.38f) else if (dark) lerp(IconStart, Color.Black, 0.35f) else IconStart
    val end = if (dynamic) lerp(scheme.primary, Color.Black, if (dark) 0.40f else 0.08f) else if (dark) lerp(IconEnd, Color.Black, 0.40f) else IconEnd
    return if (dark) CryoDepth(
        grouped = scheme.surfaceContainerLowest, card = scheme.surfaceContainer, cardFaded = scheme.surfaceContainerLow,
        shadow = Color(0x99000000), shadowContact = Color(0x66000000), highlight = Color(0x1FFFFFFF),
        headerStart = start, headerEnd = end, headerGlow = Color(0x2E5FD4FD), headerShadow = Color(0xB3000000),
    ) else CryoDepth(
        grouped = scheme.surfaceContainer, card = scheme.surfaceContainerLowest, cardFaded = scheme.surfaceContainerLow,
        shadow = Color(0x1F0A3A4A), shadowContact = Color(0x140A3A4A), highlight = Color(0x00FFFFFF),
        headerStart = start, headerEnd = end, headerGlow = Color(0x403CC6E3), headerShadow = Color(0x400A3A4A),
    )
}

val LocalCryoDepth = staticCompositionLocalOf { depthFor(LightColors, dark = false, dynamic = false) }

object CryoTheme {
    val colors: CryoColors
        @Composable get() = LocalCryoColors.current

    /** DS-PROF: cores da profundidade (fundo agrupado, cartões, sombras, cabeçalho). */
    val depth: CryoDepth
        @Composable get() = LocalCryoDepth.current

    /** DS-TOM: os tons pastel por prioridade (alerta, atenção, bom, informação). */
    val tones: CryoTones
        @Composable get() = LocalCryoTones.current

    /** DS-COR-06: a paleta de temperatura do calendário de calor. */
    val heat: CryoHeat
        @Composable get() = LocalCryoHeat.current
}

private val CryoTypography = Typography().let { t ->
    t.copy(
        displayLarge = t.displayLarge.copy(fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum"),
        displayMedium = t.displayMedium.copy(fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum"),
        headlineLarge = t.headlineLarge.copy(fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum"),
        headlineMedium = t.headlineMedium.copy(fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum"),
        headlineSmall = t.headlineSmall.copy(fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum"),
        // DS-PROF-09: números grandes com peso e espaçamento levemente fechado.
        displaySmall = t.displaySmall.copy(fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum", letterSpacing = (-0.5).sp),
        titleLarge = t.titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = t.titleMedium.copy(fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum"),
        titleSmall = t.titleSmall.copy(fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum"),
        bodyLarge = t.bodyLarge.copy(fontFeatureSettings = "tnum"),
        bodyMedium = t.bodyMedium.copy(fontFeatureSettings = "tnum"),
        labelLarge = t.labelLarge.copy(fontWeight = FontWeight.SemiBold),
    )
}

private val CryoShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun isDarkTheme(mode: ThemeMode): Boolean = when (mode) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

@Composable
fun CryoTheme(mode: ThemeMode = ThemeMode.SYSTEM, dynamicColor: Boolean = true, content: @Composable () -> Unit) {
    val dark = isDarkTheme(mode)
    val ctx = LocalContext.current
    val scheme: ColorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= 31 -> if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        dark -> DarkColors
        else -> LightColors
    }
    val dynamic = dynamicColor && Build.VERSION.SDK_INT >= 31
    val depth = depthFor(scheme, dark, dynamic)
    // DS-PROF-02: o fundo das telas (e das barras de cima) é o "agrupado"; os cartões ficam por cima, mais claros.
    val grouped = scheme.copy(background = depth.grouped, surface = depth.grouped)
    CompositionLocalProvider(
        LocalCryoColors provides if (dark) DarkExt else LightExt,
        LocalCryoDepth provides depth,
        LocalCryoTones provides if (dark) DarkTones else LightTones,
        LocalCryoHeat provides if (dark) DarkHeat else LightHeat,
    ) {
        MaterialTheme(colorScheme = grouped, typography = CryoTypography, shapes = CryoShapes, content = content)
    }
}
