package com.velithorne.vessel.renderer_genesis

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.velithorne.vessel.model.GeneratedAnatomyState
import com.velithorne.vessel.model.VesselPaletteState
import com.velithorne.vessel.morphogenesis_core.MorphNodeKind

object GenesisReservePainter {
    fun draw(
        scope: DrawScope,
        pod: Offset,
        minDim: Float,
        anatomy: GeneratedAnatomyState?,
        palette: VesselPaletteState,
        @Suppress("UNUSED_PARAMETER") phaseSec: Float,
    ) {
        val node = anatomy?.graph?.nodes?.firstOrNull { it.kind == MorphNodeKind.RESERVE_BASIN_LOCUS }
            ?: return
        val rx = pod.x + (node.nx - 0.5f) * minDim * 0.5f
        val ry = pod.y + (node.ny - 0.5f) * minDim * 0.5f
        val w = minDim * 0.09f * node.strength
        val h = minDim * 0.055f * node.strength
        scope.drawOval(
            brush = Brush.radialGradient(
                colors = listOf(
                    palette.lungFrond.copy(alpha = 0.22f),
                    palette.gelMedium.copy(alpha = 0.05f),
                ),
                center = Offset(rx, ry),
                radius = kotlin.math.max(w, h) * 1.8f,
            ),
            topLeft = Offset(rx - w, ry - h),
            size = Size(w * 2f, h * 2f),
        )
    }
}
