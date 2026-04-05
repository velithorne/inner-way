package com.velithorne.innerway.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Void = Color(0xFF0A0C10)
private val Mist = Color(0xFF1A2332)
private val Pulse = Color(0xFF4FD1C5)
private val Ember = Color(0xFFE07A5F)

@Composable
fun VelithorneApp(viewModel: VelithorneViewModel) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Pulse,
            secondary = Ember,
            tertiary = Mist,
            background = Void,
            surface = Mist,
            onPrimary = Void,
            onSecondary = Void,
            onBackground = Color(0xFFE6F1F5),
            onSurface = Color(0xFFE6F1F5),
        ),
    ) {
        Surface(color = MaterialTheme.colorScheme.background) {
            VelithorneScreen(viewModel = viewModel)
        }
    }
}
