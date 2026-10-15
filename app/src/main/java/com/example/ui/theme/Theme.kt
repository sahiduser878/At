package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = NeonMagenta,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF4A0E2E),
    onPrimaryContainer = Color(0xFFFFD9E2),
    secondary = NeonPurple,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF331B54),
    onSecondaryContainer = Color(0xFFE9DDFF),
    tertiary = ElectricCyan,
    onTertiary = Color.Black,
    tertiaryContainer = Color(0xFF004F58),
    onTertiaryContainer = Color(0xFFA6EEFF),
    background = CyberBgDark,
    onBackground = TextPrimary,
    surface = CyberSurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = CyberSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = CyberCardBorder
)

@Composable
fun MyApplicationTheme(
    accentColor: Color = NeonMagenta,
    content: @Composable () -> Unit
) {
    val dynamicScheme = DarkColorScheme.copy(
        primary = accentColor
    )

    MaterialTheme(
        colorScheme = dynamicScheme,
        typography = Typography,
        content = content
    )
}
