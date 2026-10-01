// SPDX-License-Identifier: GPL-3.0-or-later
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
    val isDark: Boolean,
)

private val LightExt = CryoColors(
    income = Color(0xFF1B6D2F), incomeContainer = Color(0xFFB6F2BB), onIncomeContainer = Color(0xFF002109),
    expense = Color(0xFFB3261E), expenseContainer = Color(0xFFFFDAD6), onExpenseContainer = Color(0xFF410002),
    warning = Color(0xFF8A5100), warningContainer = Color(0xFFFFDCBE), onWarningContainer = Color(0xFF2C1600),
    isDark = false,
)

private val DarkExt = CryoColors(
    income = Color(0xFF8BD893), incomeContainer = Color(0xFF00531C), onIncomeContainer = Color(0xFFB6F2BB),
    expense = Color(0xFFFFB4AB), expenseContainer = Color(0xFF93000A), onExpenseContainer = Color(0xFFFFDAD6),
    warning = Color(0xFFFFB870), warningContainer = Color(0xFF693C00), onWarningContainer = Color(0xFFFFDCBE),
    isDark = true,
)

val LocalCryoColors = staticCompositionLocalOf { LightExt }

object CryoTheme {
    val colors: CryoColors
        @Composable get() = LocalCryoColors.current
}

private val CryoTypography = Typography().let { t ->
    t.copy(
        displayLarge = t.displayLarge.copy(fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum"),
        displayMedium = t.displayMedium.copy(fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum"),
        displaySmall = t.displaySmall.copy(fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum"),
        headlineLarge = t.headlineLarge.copy(fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum"),
        headlineMedium = t.headlineMedium.copy(fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum"),
        headlineSmall = t.headlineSmall.copy(fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum"),
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
    CompositionLocalProvider(LocalCryoColors provides if (dark) DarkExt else LightExt) {
        MaterialTheme(colorScheme = scheme, typography = CryoTypography, shapes = CryoShapes, content = content)
    }
}
