package com.collide.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// COLLIDE dark lab theme — functional, scientific, no decoration
val CollideBackground = Color(0xFF09090D)
val CollideSurface = Color(0xFF13131A)
val CollideCard = Color(0xFF1A1A24)
val CollideAccent = Color(0xFF00CFFF)       // Cyan accent
val CollideAccentAlt = Color(0xFF7B61FF)    // Purple alt accent
val CollideGreen = Color(0xFF00E676)        // Event found
val CollideAmber = Color(0xFFFFAB00)        // Warning / in-progress
val CollideRed = Color(0xFFFF5252)          // Error / failed
val CollideDim = Color(0xFF4A4A5A)          // Dimmed text
val CollideOnSurface = Color(0xFFCCCCDD)
val CollideOnBackground = Color(0xFFEEEEFF)

private val CollideDarkColorScheme = darkColorScheme(
    primary = CollideAccent,
    onPrimary = Color(0xFF001F28),
    primaryContainer = Color(0xFF002F3A),
    onPrimaryContainer = CollideAccent,
    secondary = CollideAccentAlt,
    onSecondary = Color(0xFF120043),
    secondaryContainer = Color(0xFF20005E),
    onSecondaryContainer = CollideAccentAlt,
    tertiary = CollideGreen,
    background = CollideBackground,
    onBackground = CollideOnBackground,
    surface = CollideSurface,
    onSurface = CollideOnSurface,
    surfaceVariant = CollideCard,
    onSurfaceVariant = Color(0xFFAAAAAA),
    error = CollideRed,
    outline = Color(0xFF2A2A3A),
    outlineVariant = Color(0xFF1E1E2E)
)

@Composable
fun CollideTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CollideDarkColorScheme,
        typography = CollideTypography,
        content = content
    )
}
