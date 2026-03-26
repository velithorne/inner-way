package com.velithorne.vessel.renderer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import com.velithorne.vessel.model.VesselPaletteState
import kotlin.math.cos
import kotlin.math.sin

/** Subtle fronds, lamellae spines, crown bloom — pressure-driven protrusions. */
object BuddingPainter {

    fun draw(
        scope: DrawScope,
        center: Offset,
        width: Float,
        height: Float,
        scene: VesselSceneState,
        palette: VesselPaletteState,
        phase: Float,
    ) {
        val gen = scene.generated
        val shellPath = VesselContourBuilder.bodyShellPath(center, width, height, gen)

        scope.clipPath(shellPath) {
            // Lateral fronds (signal)
            val fr = gen.budSignalFrondMul.coerceIn(0f, 1f)
            if (fr > 0.06f) {
                for (side in listOf(-1f, 1f)) {
                    val base = Offset(center.x + side * width * 0.22f, center.y + height * 0.06f)
                    val path = Path().apply {
                        moveTo(base.x, base.y)
                        quadraticTo(
                            base.x + side * width * 0.18f * fr,
                            base.y - height * 0.12f * fr,
                            base.x + side * width * 0.26f * fr,
                            base.y - height * 0.02f * fr,
                        )
                    }
                    scope.drawPath(
                        path = path,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                palette.accentSignal.copy(alpha = fr * 0.14f),
                                palette.lungFrond.copy(alpha = fr * 0.08f),
                            ),
                            start = base,
                            end = Offset(base.x + side * width * 0.2f, base.y),
                        ),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.6f + fr * 1.2f),
                    )
                }
            }
            // Thermal veil spines (upper arc)
            val th = gen.budThermalVeilMul.coerceIn(0f, 1f)
            if (th > 0.06f) {
                val p = Path().apply {
                    moveTo(center.x - width * 0.2f, center.y - height * 0.35f)
                    quadraticTo(center.x, center.y - height * (0.48f + th * 0.06f), center.x + width * 0.2f, center.y - height * 0.35f)
                }
                scope.drawPath(
                    path = p,
                    color = palette.thermalEdge.copy(alpha = th * 0.1f),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.2f + th * 1f),
                )
            }
            // Archive lamellae (lower strata lines)
            val ar = gen.budArchiveLamellaMul.coerceIn(0f, 1f)
            if (ar > 0.06f) {
                val y0 = center.y + height * 0.22f
                for (i in 0 until 4) {
                    val t = i / 3f
                    val yy = y0 + t * height * 0.14f * ar
                    val wob = sin(phase + i * 0.7f) * width * 0.015f
                    scope.drawLine(
                        color = palette.archivePlate.copy(alpha = ar * 0.11f * (1f - t * 0.3f)),
                        start = Offset(center.x - width * 0.22f + wob, yy),
                        end = Offset(center.x + width * 0.22f - wob, yy),
                        strokeWidth = 1f + ar * 0.8f,
                    )
                }
            }
            // Neural crown bloom (upper)
            val cr = gen.budNeuralCrownMul.coerceIn(0f, 1f)
            if (cr > 0.05f) {
                val crown = Offset(center.x + cos(phase) * width * 0.04f, center.y - height * 0.36f)
                scope.drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            palette.cortexNode.copy(alpha = cr * 0.12f),
                            palette.cortexFilament.copy(alpha = cr * 0.06f),
                            Color(0xFF000000).copy(alpha = 0f),
                        ),
                        center = crown,
                        radius = width * 0.2f * cr,
                    ),
                    radius = width * 0.22f * cr,
                    center = crown,
                )
            }
        }
    }
}
