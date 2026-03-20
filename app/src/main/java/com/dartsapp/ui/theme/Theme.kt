package com.dartsapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = DartGreenLight,
    onPrimary = DartBackground,
    secondary = DartGold,
    background = DartBackground,
    surface = DartSurface,
    onSurface = DartOnSurface,
    error = DartRedLight
)

private val LightColorScheme = lightColorScheme(
    primary = DartGreen,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    secondary = DartGold,
    background = androidx.compose.ui.graphics.Color.White,
    surface = androidx.compose.ui.graphics.Color(0xFFF5F5F5),
    onSurface = androidx.compose.ui.graphics.Color(0xFF212121),
    error = DartRed
)

@Composable
fun DartsAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
