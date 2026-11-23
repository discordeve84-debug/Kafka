package com.example.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Anime Cyber-Atelier & Obsidian Precision Tokens
val ObsidianBg = Color(0xFF090B14)
val ObsidianSurface = Color(0xFF111524)
val ObsidianSurfaceElevated = Color(0xFF192036)
val ObsidianSurfaceHighest = Color(0xFF232B47)
val ObsidianBorder = Color(0xFF2B3556)

val KafkaMint = Color(0xFF2DD4BF)
val KafkaMintDark = Color(0xFF0F766E)
val KafkaMintContainer = Color(0xFF0E3A3A)

val KafkaSakura = Color(0xFFF472B6)
val KafkaSakuraDark = Color(0xFFDB2777)
val KafkaSakuraContainer = Color(0xFF421734)

val KafkaAmber = Color(0xFFF59E0B)
val KafkaAmberContainer = Color(0xFF452A06)

val KafkaCyan = Color(0xFF818CF8)
val KafkaCyanContainer = Color(0xFF1E2250)

val KafkaVerifiedGreen = Color(0xFF10B981)
val KafkaWarningAmber = Color(0xFFFBBF24)
val KafkaErrorRed = Color(0xFFEF4444)
val KafkaErrorContainer = Color(0xFF4C1419)

val TextPrimaryDark = Color(0xFFF1F5F9)
val TextSecondaryDark = Color(0xFF9AA7C4)

// Clean Light Theme Tokens
val AlabasterBg = Color(0xFFF7F6FC)
val AlabasterSurface = Color(0xFFFFFFFF)
val AlabasterSurfaceElevated = Color(0xFFF0EEF9)
val AlabasterBorder = Color(0xFFDCD8EE)
val TextPrimaryLight = Color(0xFF111322)
val TextSecondaryLight = Color(0xFF4E5573)

val KafkaDarkColorScheme = darkColorScheme(
    primary = KafkaMint,
    onPrimary = Color(0xFF032320),
    primaryContainer = KafkaMintContainer,
    onPrimaryContainer = Color(0xFF99F6E4),
    secondary = KafkaSakura,
    onSecondary = Color(0xFF33061E),
    secondaryContainer = KafkaSakuraContainer,
    onSecondaryContainer = Color(0xFFFBCFE8),
    tertiary = KafkaCyan,
    onTertiary = Color(0xFF0B1038),
    tertiaryContainer = KafkaCyanContainer,
    onTertiaryContainer = Color(0xFFE0E7FF),
    background = ObsidianBg,
    onBackground = TextPrimaryDark,
    surface = ObsidianSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = ObsidianSurfaceElevated,
    onSurfaceVariant = TextSecondaryDark,
    surfaceContainerLowest = ObsidianBg,
    surfaceContainerLow = ObsidianSurface,
    surfaceContainer = ObsidianSurfaceElevated,
    surfaceContainerHigh = ObsidianSurfaceHighest,
    outline = ObsidianBorder,
    outlineVariant = Color(0xFF1F2740),
    error = KafkaErrorRed,
    onError = Color.White,
    errorContainer = KafkaErrorContainer,
    onErrorContainer = Color(0xFFFECACA),
)

val KafkaLightColorScheme = lightColorScheme(
    primary = KafkaMintDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCCFBF1),
    onPrimaryContainer = Color(0xFF042F2E),
    secondary = KafkaSakuraDark,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFCE7F3),
    onSecondaryContainer = Color(0xFF500724),
    tertiary = Color(0xFF4F46E5),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE0E7FF),
    onTertiaryContainer = Color(0xFF1E1B4B),
    background = AlabasterBg,
    onBackground = TextPrimaryLight,
    surface = AlabasterSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = AlabasterSurfaceElevated,
    onSurfaceVariant = TextSecondaryLight,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = AlabasterBg,
    surfaceContainer = AlabasterSurfaceElevated,
    surfaceContainerHigh = Color(0xFFE5E1F5),
    outline = AlabasterBorder,
    outlineVariant = Color(0xFFE5E1F5),
    error = Color(0xFFDC2626),
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF7F1D1D),
)
