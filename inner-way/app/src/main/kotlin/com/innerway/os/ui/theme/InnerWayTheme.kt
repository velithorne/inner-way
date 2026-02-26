package com.innerway.os.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val InnerWayDarkColorScheme = darkColorScheme(
    primary = RedPrimary,
    onPrimary = TextPrimary,
    surface = BlackDeep,
    onSurface = TextPrimary,
    background = BlackDeep,
    onBackground = TextPrimary,
    secondary = Grey,
    onSecondary = TextSecondary,
    error = Red,
    onError = BlackDeep
)

@Composable
fun InnerWayTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = InnerWayDarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode && view.context is Activity) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = BlackDeep.toArgb()
            window.navigationBarColor = BlackDeep.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
