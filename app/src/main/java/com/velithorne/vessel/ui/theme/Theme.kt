package com.velithorne.vessel.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val VesselDarkColors = darkColorScheme(
    primary = VesselAccent,
    onPrimary = Color(0xFF031312),
    secondary = VesselAccentDim,
    onSecondary = VesselTextPrimary,
    background = VesselBg,
    onBackground = VesselTextPrimary,
    surface = VesselBgElevated,
    onSurface = VesselTextPrimary,
    outline = VesselOutline,
    error = VesselDanger,
    onError = Color.Black,
)

@Composable
fun VelithorneVesselTheme(
    @Suppress("UNUSED_PARAMETER") darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    // Phase 1 is dark-only diagnostic UI regardless of system setting.
    MaterialTheme(
        colorScheme = VesselDarkColors,
        typography = VesselTypography,
        content = content,
    )
}
