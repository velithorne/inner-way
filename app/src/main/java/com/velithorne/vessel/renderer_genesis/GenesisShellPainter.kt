package com.velithorne.vessel.renderer_genesis

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.velithorne.vessel.model.GeneratedAnatomyState
import com.velithorne.vessel.model.VesselPaletteState
import com.velithorne.vessel.renderer_genesis.GenesisMaterialSystem.emphasis
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Partial shell arcs from field shell pressure — not a closed vesica oval. */
object GenesisShellPainter {
    fun draw(
        scope: DrawScope,
        pod: Offset,
        minDim: Float,
        anatomy: GeneratedAnatomyState?,
        palette: VesselPaletteState,
        phaseSec: Float,
    ) {
        val em = emphasis(anatomy)
        if (em.shellVeilAlpha < 0.08f) return
        val rx = minDim * 0.14f
        val ry = minDim * 0.16f
        val path = Path()
        val start = -PI.toFloat() * 0.35f
        val sweep = PI.toFloat() * 0.9f
        val steps = 24
        for (i in 0..steps) {
            val t = start + sweep * (i / steps.toFloat())
            val x = pod.x + cos(t).toFloat() * rx * (0.85f + em.fieldCoherence * 0.15f)
            val shellP = anatomy?.tissueCenter?.shellPressure ?: 0f
            val y = pod.y + sin(t).toFloat() * ry * (0.9f + shellP * 0.12f)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        scope.drawPath(
            path = path,
            color = GenesisPalette.shellVeil(palette).copy(alpha = em.shellVeilAlpha),
            style = Stroke(width = 2.2f),
        )
    }
}
