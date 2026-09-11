package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CyberDarkColorScheme = darkColorScheme(
    primary = CyberCyan,
    onPrimary = Color(0xFF00363D),
    primaryContainer = Color(0xFF004F58),
    onPrimaryContainer = Color(0xFF97F0FF),

    secondary = ElectricViolet,
    onSecondary = Color(0xFF28065B),
    secondaryContainer = Color(0xFF3F2074),
    onSecondaryContainer = Color(0xFFEADBFF),

    tertiary = CyberEmerald,
    onTertiary = Color(0xFF003919),
    tertiaryContainer = Color(0xFF005327),
    onTertiaryContainer = Color(0xFF6CFFA0),

    error = EmergencyCrimson,
    onError = Color.White,
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),

    background = ObsidianBg,
    onBackground = TextPrimary,

    surface = SlateSurface,
    onSurface = TextPrimary,
    surfaceVariant = GunmetalCard,
    onSurfaceVariant = TextSecondary,

    outline = CyberBorder,
    outlineVariant = Color(0xFF1D283C)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Default to dark aesthetic as requested
    dynamicColor: Boolean = false, // Keep cyber dark palette consistent
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = CyberDarkColorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun AutoClickAiTheme(
    content: @Composable () -> Unit,
) {
    MyApplicationTheme(content = content)
}
