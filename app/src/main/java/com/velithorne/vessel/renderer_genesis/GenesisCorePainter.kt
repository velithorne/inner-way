package com.velithorne.vessel.renderer_genesis

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.velithorne.vessel.model.GeneratedAnatomyState
import com.velithorne.vessel.model.VesselPaletteState
import com.velithorne.vessel.morphogenesis_core.MorphNodeKind
import com.velithorne.vessel.morphogenesis_core.StructuralGraph

object GenesisCorePainter {
    fun draw(
        scope: DrawScope,
        pod: Offset,
        minDim: Float,
        anatomy: GeneratedAnatomyState?,
        palette: VesselPaletteState,
    ) {
        val g: StructuralGraph = anatomy?.graph ?: return
        val core = g.nodes.firstOrNull { it.kind == MorphNodeKind.CORE_KNOT } ?: return
        val cx = pod.x + (core.nx - 0.5f) * minDim * 0.52f
        val cy = pod.y + (core.ny - 0.5f) * minDim * 0.52f
        val r = minDim * 0.045f * core.strength.coerceIn(0.25f, 1.1f)
        scope.drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    GenesisPalette.coreKnot(palette),
                    palette.innerChamberShadow.copy(alpha = 0.35f),
                ),
                center = Offset(cx, cy),
                radius = r * 2.2f,
            ),
            radius = r * 2.2f,
            center = Offset(cx, cy),
        )
    }
}
