package com.collide.app.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// COLLIDE dark scientific palette
private val CollideBackground = Color(0xFF0A0A0F)
private val CollideSurface = Color(0xFF13131A)
private val CollideSurfaceVariant = Color(0xFF1C1C28)
private val CollideAccent = Color(0xFF00E5FF)         // Cyan — primary interactive
private val CollideAccentAlt = Color(0xFF7C4DFF)      // Violet — secondary accent
private val CollideWinner = Color(0xFF00E676)         // Green — wins
private val CollideFailure = Color(0xFFFF1744)        // Red — failures
private val CollideWarning = Color(0xFFFFAB00)        // Amber — warnings
private val CollideOnBackground = Color(0xFFE0E0F0)
private val CollideOnSurface = Color(0xFFB0B0CC)

val CollideColorScheme = darkColorScheme(
    primary = CollideAccent,
    onPrimary = Color(0xFF003040),
    primaryContainer = Color(0xFF00374F),
    onPrimaryContainer = CollideAccent,
    secondary = CollideAccentAlt,
    onSecondary = Color(0xFF20006A),
    secondaryContainer = Color(0xFF2E0090),
    onSecondaryContainer = Color(0xFFE9DEFF),
    tertiary = CollideWinner,
    onTertiary = Color(0xFF003820),
    background = CollideBackground,
    onBackground = CollideOnBackground,
    surface = CollideSurface,
    onSurface = CollideOnSurface,
    surfaceVariant = CollideSurfaceVariant,
    onSurfaceVariant = Color(0xFF8080A0),
    error = CollideFailure,
    onError = Color(0xFF690000),
    outline = Color(0xFF404060)
)

// Expose palette constants for direct use in screens
object CollideColors {
    val background = CollideBackground
    val surface = CollideSurface
    val surfaceVariant = CollideSurfaceVariant
    val accent = CollideAccent
    val accentAlt = CollideAccentAlt
    val winner = CollideWinner
    val failure = CollideFailure
    val warning = CollideWarning
    val onBackground = CollideOnBackground
    val onSurface = CollideOnSurface
    val muted = Color(0xFF505070)
}

@Composable
fun CollideTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CollideColorScheme,
        typography = CollideTypography,
        content = content
    )
}
