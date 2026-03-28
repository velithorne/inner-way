package com.velithorne.vessel.renderer_genesis

import androidx.compose.ui.geometry.Offset
import com.velithorne.vessel.morphogenesis_core.GrowthPressureState

object GenesisViewportMapper {
    fun bodyOffset(minDim: Float, pressure: GrowthPressureState?): Offset {
        val p = pressure ?: return Offset.Zero
        val ax = (p.signal - 0.5f) * minDim * 0.04f
        val ay = (p.reserve - 0.5f) * minDim * 0.03f
        return Offset(ax, ay)
    }
}
