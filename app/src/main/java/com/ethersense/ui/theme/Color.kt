package com.ethersense.ui.theme

import androidx.compose.ui.graphics.Color

// Primary Palette - Cyan
val CyanPrimary = Color(0xFF00D9FF)
val CyanLight = Color(0xFF4DE8FF)
val CyanDark = Color(0xFF00A3C4)
val CyanGlow = Color(0x6600D9FF)

// Secondary Palette - Purple
val PurpleSecondary = Color(0xFF9C27B0)
val PurpleLight = Color(0xFFCE93D8)
val PurpleDark = Color(0xFF7B1FA2)
val PurpleGlow = Color(0x669C27B0)

// Signal Quality Colors
val SignalExcellent = Color(0xFF4CAF50)
val SignalGood = Color(0xFF8BC34A)
val SignalFair = Color(0xFFFFC107)
val SignalPoor = Color(0xFFFF9800)
val SignalWeak = Color(0xFFF44336)

// Background Palette
val BackgroundDark = Color(0xFF0D0D1A)
val BackgroundDarker = Color(0xFF080810)
val SurfaceDark = Color(0xFF1A1A2E)
val SurfaceVariant = Color(0xFF252540)
val SurfaceElevated = Color(0xFF2D2D4A)

// Text Colors
val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFFB0B0C0)
val TextTertiary = Color(0xFF707080)
val TextDisabled = Color(0xFF505060)

// Accent Colors
val AccentGreen = Color(0xFF00E676)
val AccentRed = Color(0xFFFF5252)
val AccentYellow = Color(0xFFFFD740)
val AccentOrange = Color(0xFFFF6E40)

// Glow Effects
val GlowCyan = Color(0x4000D9FF)
val GlowPurple = Color(0x409C27B0)
val GlowGreen = Color(0x404CAF50)
val GlowRed = Color(0x40F44336)

// Gradients
object GradientColors {
    val cyanToPurple = listOf(CyanPrimary, PurpleSecondary)
    val greenToYellow = listOf(SignalExcellent, SignalFair)
    val yellowToRed = listOf(SignalFair, SignalWeak)
    val surfaceGradient = listOf(SurfaceDark, BackgroundDark)
}
