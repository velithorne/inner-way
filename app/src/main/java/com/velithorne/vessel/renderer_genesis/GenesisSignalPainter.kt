package com.velithorne.vessel.renderer_genesis

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.velithorne.vessel.model.GeneratedAnatomyState
import com.velithorne.vessel.model.VesselPaletteState
import com.velithorne.vessel.morphogenesis_core.MorphNodeKind

object GenesisSignalPainter {
    fun draw(
        scope: DrawScope,
        pod: Offset,
        minDim: Float,
        anatomy: GeneratedAnatomyState?,
        palette: VesselPaletteState,
        phaseSec: Float,
    ) {
        val f = anatomy?.graph?.nodes?.filter { it.kind == MorphNodeKind.FROND_ROOT }.orEmpty()
        if (f.isEmpty()) return
        for (n in f) {
            val tx = pod.x + (n.nx - 0.5f) * minDim * 0.55f
            val ty = pod.y + (n.ny - 0.5f) * minDim * 0.55f
            val path = Path()
            path.moveTo(pod.x, pod.y + minDim * 0.02f)
            path.quadraticTo((pod.x + tx) * 0.5f, pod.y + ty * 0.5f, tx, ty)
            scope.drawPath(
                path = path,
                color = palette.accentSignal.copy(alpha = 0.18f + n.strength * 0.25f),
                style = Stroke(width = 2f + n.strength * 2f),
            )
        }
    }
}
