package com.velithorne.vessel.renderer_genesis

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.velithorne.vessel.model.GeneratedAnatomyState
import com.velithorne.vessel.model.VesselPaletteState
import com.velithorne.vessel.morphogenesis_core.TissueField
import kotlin.math.cos
import kotlin.math.sin

object GenesisFieldPainter {
    fun draw(
        scope: DrawScope,
        pod: Offset,
        minDim: Float,
        anatomy: GeneratedAnatomyState?,
        palette: VesselPaletteState,
        phaseSec: Float,
    ) {
        val f: TissueField = anatomy?.tissueCenter ?: return
        val r = minDim * 0.22f
        val ox = (f.signal - 0.5f) * minDim * 0.06f
        val oy = (f.reserve - 0.5f) * minDim * 0.05f
        val c = pod + Offset(ox, oy)
        val pulse = 0.85f + sin(phaseSec * 0.7f).toFloat() * 0.15f
        scope.drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    GenesisPalette.fieldSignal(palette).copy(alpha = 0.14f * f.signal * pulse),
                    GenesisPalette.fieldReserve(palette).copy(alpha = 0.06f * f.reserve),
                ),
                center = c,
                radius = r * (0.9f + f.density * 0.25f),
            ),
            radius = r * (0.9f + f.density * 0.25f),
            center = c,
        )
    }
}
