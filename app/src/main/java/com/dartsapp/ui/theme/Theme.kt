package com.dartsapp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary        = DartPurpleLight,
    onPrimary      = DartBackground,
    secondary      = DartBlueLight,
    onSecondary    = DartOnBackground,
    background     = DartBackground,
    onBackground   = DartOnBackground,
    surface        = DartSurface,
    onSurface      = DartOnSurface,
    surfaceVariant = DartSurfaceVariant,
    error          = DartRedLight,
    onError        = DartBackground,
)

@Composable
fun DartsAppTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
