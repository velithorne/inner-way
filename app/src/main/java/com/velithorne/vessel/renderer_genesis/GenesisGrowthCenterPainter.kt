package com.velithorne.vessel.renderer_genesis

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.velithorne.vessel.model.GeneratedAnatomyState
import com.velithorne.vessel.model.VesselPaletteState
import com.velithorne.vessel.morphogenesis_core.GrowthCenter

object GenesisGrowthCenterPainter {
    fun draw(
        scope: DrawScope,
        pod: Offset,
        minDim: Float,
        anatomy: GeneratedAnatomyState?,
        palette: VesselPaletteState,
        phaseSec: Float,
    ) {
        val centers: List<GrowthCenter> = anatomy?.growthCenters ?: return
        for (gc in centers) {
            val px = pod.x + (gc.nx - 0.5f) * minDim * 0.55f
            val py = pod.y + (gc.ny - 0.5f) * minDim * 0.55f
            val rad = minDim * 0.028f * gc.intensity.coerceIn(0.15f, 1.2f)
            scope.drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        palette.accentSignal.copy(alpha = 0.35f * gc.intensity),
                        palette.gelMedium.copy(alpha = 0.08f),
                    ),
                    center = Offset(px, py),
                    radius = rad * 3f,
                ),
                radius = rad * 3f,
                center = Offset(px, py),
            )
        }
    }
}
