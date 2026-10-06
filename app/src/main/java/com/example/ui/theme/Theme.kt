package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = SignalBuyGreen,
    onPrimary = Color.Black,
    primaryContainer = SignalBuyGreenContainer,
    onPrimaryContainer = SignalBuyGreen,
    secondary = TechCyan,
    onSecondary = Color.Black,
    secondaryContainer = ObsidianElevated,
    onSecondaryContainer = TechCyan,
    tertiary = GoldAccent,
    background = ObsidianBackground,
    onBackground = TextPrimary,
    surface = ObsidianSurface,
    onSurface = TextPrimary,
    surfaceVariant = ObsidianCard,
    onSurfaceVariant = TextSecondary,
    outline = ObsidianBorder,
    error = SignalSellRed,
    errorContainer = SignalSellRedContainer,
    onError = Color.White
)

@Composable
fun MZKAITradingTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
