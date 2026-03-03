package com.falcor.civilization.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val FalcorDark = darkColorScheme(
    primary = Color(0xFF58A6FF),
    onPrimary = Color(0xFF0D1117),
    primaryContainer = Color(0xFF1F6FEB),
    secondary = Color(0xFF8B949E),
    onSecondary = Color(0xFF0D1117),
    surface = Color(0xFF161B22),
    onSurface = Color(0xFFE6EDF3),
    surfaceVariant = Color(0xFF21262D),
    onSurfaceVariant = Color(0xFF8B949E),
    background = Color(0xFF0D1117),
    onBackground = Color(0xFFE6EDF3),
    error = Color(0xFFF85149),
    onError = Color(0xFF0D1117)
)

@Composable
fun FalcorCivilizationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = FalcorDark,
        typography = Typography(),
        content = content
    )
}
