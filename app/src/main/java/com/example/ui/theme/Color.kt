package com.example.ui.theme

import androidx.compose.ui.graphics.Color

val CyberBgDark = Color(0xFF070814)
val CyberSurfaceDark = Color(0xFF111427)
val CyberSurfaceVariant = Color(0xFF181B34)
val CyberCardBorder = Color(0x338B5CF6)

val NeonMagenta = Color(0xFFF43F5E)
val NeonPurple = Color(0xFF8B5CF6)
val ElectricCyan = Color(0xFF06B6D4)
val EmeraldMatrix = Color(0xFF10B981)
val AmberGold = Color(0xFFF59E0B)

val TextPrimary = Color(0xFFF8FAFC)
val TextSecondary = Color(0xFF94A3B8)
val TextTertiary = Color(0xFF64748B)

val OrbGradient1 = Color(0xFFF43F5E)
val OrbGradient2 = Color(0xFF8B5CF6)
val OrbGradient3 = Color(0xFF06B6D4)

fun parseAccentColor(hex: String, defaultColor: Color = NeonMagenta): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        defaultColor
    }
}
