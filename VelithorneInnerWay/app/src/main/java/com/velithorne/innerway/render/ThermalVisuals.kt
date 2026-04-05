package com.velithorne.innerway.render

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

fun thermalTint(thermalRatio: Float): Color {
    val t = thermalRatio.coerceIn(0f, 1f)
    val chill = Color(0xFF4FD1C5)
    val heat = Color(0xFFE07A5F)
    return lerp(chill, heat, t)
}
