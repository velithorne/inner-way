package com.aura.shell.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AuraDarkScheme = darkColorScheme(
    primary = AuraAccent,
    onPrimary = Color(0xFF00100E),
    primaryContainer = AuraAccentDim,
    onPrimaryContainer = AuraTextPrimary,
    secondary = AuraTextSecondary,
    onSecondary = AuraBackground,
    tertiary = AuraAccent,
    background = AuraBackground,
    onBackground = AuraTextPrimary,
    surface = AuraSurface,
    onSurface = AuraTextPrimary,
    surfaceVariant = AuraSurfaceElevated,
    onSurfaceVariant = AuraTextSecondary,
    outline = AuraOutline,
)

@Composable
fun AuraShellTheme(
    content: @Composable () -> Unit,
) {
    val colors = AuraDarkScheme

    MaterialTheme(
        colorScheme = colors,
        typography = AuraTypography,
        content = content,
    )
}
