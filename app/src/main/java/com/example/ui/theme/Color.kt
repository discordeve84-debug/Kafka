package com.example.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Kafka Succubus Dark Fantasy Palette: Deep Royal Purples + Sharp Crimson Accents
val ObsidianBg = Color(0xFF09040F)
val ObsidianSurface = Color(0xFF12081C)
val ObsidianSurfaceElevated = Color(0xFF1C0E2B)
val ObsidianSurfaceHighest = Color(0xFF28153B)
val ObsidianBorder = Color(0xFF53256B)

// Primary: Sharp Succubus Crimson (#EB1E63)
val KafkaCrimson = Color(0xFFEB1E63)
val KafkaCrimsonDark = Color(0xFFB9124E)
val KafkaCrimsonContainer = Color(0xFF500A29)

// Preserved symbol aliases
val KafkaMint = KafkaCrimson
val KafkaMintDark = KafkaCrimsonDark
val KafkaMintContainer = KafkaCrimsonContainer

// Secondary: Soft Sakura-Rose Edge Highlight (#F69EC4)
val KafkaSakura = Color(0xFFF69EC4)
val KafkaSakuraDark = Color(0xFF9D174D)
val KafkaSakuraContainer = Color(0xFF3E1233)

// Tertiary: Arcane Royal Amethyst (#C084FC)
val KafkaAmethyst = Color(0xFFC084FC)
val KafkaAmethystContainer = Color(0xFF331452)
val KafkaCyan = KafkaAmethyst
val KafkaCyanContainer = KafkaAmethystContainer

// Semantic Status & Gold Accents
val KafkaAmber = Color(0xFFF59E0B)
val KafkaAmberContainer = Color(0xFF452A06)
val KafkaVerifiedGreen = Color(0xFF10B981)
val KafkaWarningAmber = Color(0xFFFBBF24)
val KafkaErrorRed = Color(0xFFF43F5E)
val KafkaErrorContainer = Color(0xFF520C21)

val TextPrimaryDark = Color(0xFFFCEEF7)
val TextSecondaryDark = Color(0xFFD0AFDC)

// Rose-Amethyst Light Theme Tokens (for optional light mode toggle)
val AlabasterBg = Color(0xFFFAF5FF)
val AlabasterSurface = Color(0xFFFFFFFF)
val AlabasterSurfaceElevated = Color(0xFFF4EAFC)
val AlabasterBorder = Color(0xFFDCC3EB)
val TextPrimaryLight = Color(0xFF1E0926)
val TextSecondaryLight = Color(0xFF623B72)

val KafkaDarkColorScheme = darkColorScheme(
    primary = KafkaCrimson,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = KafkaCrimsonContainer,
    onPrimaryContainer = Color(0xFFFFD6E6),
    secondary = KafkaSakura,
    onSecondary = Color(0xFF36041D),
    secondaryContainer = KafkaSakuraContainer,
    onSecondaryContainer = Color(0xFFFDE0EE),
    tertiary = KafkaAmethyst,
    onTertiary = Color(0xFF1E0638),
    tertiaryContainer = KafkaAmethystContainer,
    onTertiaryContainer = Color(0xFFF2E6FF),
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
    outlineVariant = Color(0xFF37184A),
    error = KafkaErrorRed,
    onError = Color.White,
    errorContainer = KafkaErrorContainer,
    onErrorContainer = Color(0xFFFFD6DF),
)

val KafkaLightColorScheme = lightColorScheme(
    primary = KafkaCrimsonDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFCE7F3),
    onPrimaryContainer = Color(0xFF500724),
    secondary = KafkaSakuraDark,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFAD8E9),
    onSecondaryContainer = Color(0xFF420822),
    tertiary = Color(0xFF7E22CE),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF3E8FF),
    onTertiaryContainer = Color(0xFF2E1065),
    background = AlabasterBg,
    onBackground = TextPrimaryLight,
    surface = AlabasterSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = AlabasterSurfaceElevated,
    onSurfaceVariant = TextSecondaryLight,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = AlabasterBg,
    surfaceContainer = AlabasterSurfaceElevated,
    surfaceContainerHigh = Color(0xFFEBD9FA),
    outline = AlabasterBorder,
    outlineVariant = Color(0xFFE5CFF5),
    error = Color(0xFFE11D48),
    onError = Color.White,
    errorContainer = Color(0xFFFFE4E6),
    onErrorContainer = Color(0xFF881337),
)
