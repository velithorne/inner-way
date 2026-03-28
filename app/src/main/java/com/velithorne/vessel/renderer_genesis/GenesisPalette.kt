package com.velithorne.vessel.renderer_genesis

import androidx.compose.ui.graphics.Color
import com.velithorne.vessel.model.VesselPaletteState

object GenesisPalette {
    fun fieldSignal(p: VesselPaletteState): Color = p.accentSignal.copy(alpha = 0.85f)
    fun fieldReserve(p: VesselPaletteState): Color = p.lungFrond.copy(alpha = 0.75f)
    fun coreKnot(p: VesselPaletteState): Color = p.heartCore.copy(alpha = 0.9f)
    fun shellVeil(p: VesselPaletteState): Color = p.shellEdge.copy(alpha = 0.55f)
}
