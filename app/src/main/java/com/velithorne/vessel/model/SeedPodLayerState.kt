package com.velithorne.vessel.model

import androidx.compose.ui.geometry.Offset

/**
 * Cached parallax offsets per logical depth layer (canvas space, pixels).
 */
data class SeedPodLayerState(
    val rearAtmosphere: Offset,
    val rearShell: Offset,
    val innerHaze: Offset,
    val nucleus: Offset,
    val midChamber: Offset,
    val budsCrown: Offset,
    val budsLateral: Offset,
    val budsReserve: Offset,
    val frontShell: Offset,
    val rimLight: Offset,
    val glass: Offset,
)
