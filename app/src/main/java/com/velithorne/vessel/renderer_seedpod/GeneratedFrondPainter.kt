package com.velithorne.vessel.renderer_seedpod

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.velithorne.vessel.model.SeedBudVisualState
import com.velithorne.vessel.model.VisibleMorphologyState
import com.velithorne.vessel.model.VesselPaletteState
import kotlin.math.max
import kotlin.math.sin

/** Generated frond routing — roots from graph-derived positions; asymmetric when [VisibleMorphologyState.frondAsymmetryMul] is high. */
object GeneratedFrondPainter {

    fun draw(
        scope: DrawScope,
        pod: Offset,
        minDim: Float,
        buds: SeedBudVisualState,
        radii: SeedPodContourBuilder.PodRadii,
        palette: VesselPaletteState,
        visible: VisibleMorphologyState,
        phaseSec: Float,
    ) {
        val g = visible.generatedTopologyInfluence.coerceIn(0f, 1f)
        if (g < 0.12f) return
        val shellR = max(radii.shellRx, radii.bandMidRx)
        val latL = buds.lateralLeft * g
        val latR = buds.lateralRight * g
        if (latL < 0.02f && latR < 0.02f) return
        val yBase = pod.y + minDim * 0.02f
        val asym = visible.frondAsymmetryMul
        val leftRootX = pod.x + (visible.frondRootLeftNx - 0.5f) * shellR * 2.1f
        val rightRootX = pod.x + (visible.frondRootRightNx - 0.5f) * shellR * 2.1f
        val curve = visible.frondCurvatureMul
        val drawFrond: (Float, Float, Float, Float) -> Unit = { strength, rootX, side, flip ->
            if (strength >= 0.02f) {
                val path = Path()
                val spread = shellR * (0.55f + strength * 0.35f) * curve
                val lift = shellR * (0.12f + strength * 0.2f) * visible.frondDensityMul
                val cx = rootX + side * spread * 0.4f * (1f + asym * flip)
                val cy = yBase - lift
                val c1x = rootX + side * spread * 0.35f
                val c1y = yBase - lift * 0.35f
                val c2x = cx + side * spread * 0.4f * sin(phaseSec * 0.5f) * 0.08f
                val c2y = cy + lift * 0.15f
                path.moveTo(rootX, yBase)
                path.cubicTo(c1x, c1y, c2x, c2y, cx + side * spread * 0.85f, cy)
                scope.drawPath(
                    path = path,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            palette.accentSignal.copy(alpha = 0.15f + strength * 0.35f * g),
                            palette.accentSignal.copy(alpha = 0.02f),
                        ),
                        start = Offset(rootX, yBase),
                        end = Offset(cx + side * spread, cy),
                    ),
                    style = Stroke(width = 2.2f + strength * 4f * g, cap = StrokeCap.Round),
                )
            }
        }
        drawFrond(latL, leftRootX, -1f, 1f)
        drawFrond(latR, rightRootX, 1f, -1f)
    }
}
