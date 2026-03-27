package com.velithorne.vessel.renderer_seedpod

import androidx.compose.ui.graphics.Color
import com.velithorne.vessel.model.VesselPaletteState

/**
 * Seed-pod–specific palette extensions (silicon / mineral / gel) layered on [VesselPaletteState].
 */
data class SeedPodPalette(
    val base: VesselPaletteState,
    val siliconVeil: Color = Color(0xFF8EC5D4),
    val mineralFracture: Color = Color(0xFF5A7A8A),
    val conductiveSeam: Color = Color(0xFF6FD4E8),
    val gelEnvelope: Color = Color(0xFF4A6B78),
    val nucleusDeep: Color = Color(0xFF1A2A44),
    val nucleusMid: Color = Color(0xFF3D5A8C),
    val shellBandOuter: Color = Color(0xFF6A9EAC),
    val shellBandInner: Color = Color(0xFF3D5C6A),
)
