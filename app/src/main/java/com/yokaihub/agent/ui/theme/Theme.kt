package com.yokaihub.agent.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Cyber-Shrine / Phantom Spirit Palette
val ShrineObsidian = Color(0xFF090C12)
val ShadowSlate = Color(0xFF121722)
val ElevatedShrine = Color(0xFF1A2232)
val ShrineBorder = Color(0xFF263248)

val KitsuneCyan = Color(0xFF00E5FF)
val ToriiCrimson = Color(0xFFFF2E54)
val YokaiViolet = Color(0xFF7C4DFF)
val SpiritJade = Color(0xFF00E676)
val FoxAmber = Color(0xFFFFB300)

val GhostWhite = Color(0xFFF0F4F8)
val MutedSpirit = Color(0xFF8A99AD)

private val YokaiDarkColorScheme = darkColorScheme(
    primary = KitsuneCyan,
    onPrimary = ShrineObsidian,
    secondary = YokaiViolet,
    onSecondary = GhostWhite,
    tertiary = ToriiCrimson,
    background = ShrineObsidian,
    onBackground = GhostWhite,
    surface = ShadowSlate,
    onSurface = GhostWhite,
    surfaceVariant = ElevatedShrine,
    onSurfaceVariant = MutedSpirit,
    outline = ShrineBorder,
    error = ToriiCrimson
)

val YokaiTypography = Typography(
    titleLarge = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        letterSpacing = 1.2.sp,
        color = GhostWhite
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        letterSpacing = 0.6.sp,
        color = GhostWhite
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        color = GhostWhite
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 19.sp,
        color = MutedSpirit
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        color = KitsuneCyan
    )
)

@Composable
fun YokaiHubTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = YokaiDarkColorScheme,
        typography = YokaiTypography,
        content = content
    )
}
