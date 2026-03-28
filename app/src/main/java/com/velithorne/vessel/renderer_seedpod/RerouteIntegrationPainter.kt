package com.velithorne.vessel.renderer_seedpod

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.velithorne.vessel.model.BiographyVisualState
import com.velithorne.vessel.model.RerouteIntegrationState
import com.velithorne.vessel.model.VisibleMorphologyState
import com.velithorne.vessel.model.VesselPaletteState
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/**
 * Reroute conduits emerge from frond-root loci and read as embedded seams, not flat UI strokes.
 */
object RerouteIntegrationPainter {

    fun draw(
        scope: DrawScope,
        pod: Offset,
        radii: SeedPodContourBuilder.PodRadii,
        bio: BiographyVisualState,
        visible: VisibleMorphologyState,
        integration: RerouteIntegrationState,
        palette: VesselPaletteState,
        phaseSec: Float,
        pass: ReroutePass,
    ) {
        val n = bio.rerouteCount
        if (n <= 0) return
        val g = visible.generatedTopologyInfluence.coerceIn(0f, 1f)
        if (g < 0.12f) return
        val emb = integration.embeddedStrength.coerceIn(0f, 1f)
        val sub = integration.subsurfaceAlpha.coerceIn(0f, 1f)
        val rx = radii.shellRx
        val ry = radii.shellRy
        val shellR = max(rx, ry)
        val leftRootX = pod.x + (visible.frondRootLeftNx - 0.5f) * shellR * 2.1f
        val rightRootX = pod.x + (visible.frondRootRightNx - 0.5f) * shellR * 2.1f
        val attachY = pod.y + shellR * 0.02f
        val alphaMul = when (pass) {
            ReroutePass.SUBSURFACE_BEHIND -> sub * (0.35f + emb * 0.4f)
            ReroutePass.THROUGH_SURFACE -> sub * 0.45f * emb * g
        }
        val count = minOf(n, 6)
        for (i in 0 until count) {
            val side = if (i % 2 == 0) -1f else 1f
            val rootX = if (side < 0) leftRootX else rightRootX
            val rootY = attachY
            val ang = 0.25f + i * 0.4f + sin(phaseSec * 0.25f + i) * 0.12f
            val path = Path()
            val midX = pod.x + cos(ang) * rx * (0.62f + emb * 0.18f)
            val midY = pod.y + sin(ang) * ry * (0.58f + emb * 0.12f)
            val tipX = pod.x + cos(ang + 0.08f) * rx * 0.88f
            val tipY = pod.y + sin(ang + 0.08f) * ry * 0.85f
            path.moveTo(rootX, rootY)
            path.quadraticTo(midX, midY, tipX, tipY)
            val baseA = (0.05f + minOf(n, 8) * 0.035f) * g * alphaMul
            scope.drawPath(
                path = path,
                brush = Brush.linearGradient(
                    colors = listOf(
                        palette.accentSignal.copy(alpha = baseA * 0.9f),
                        palette.neuralPathway.copy(alpha = baseA * 0.45f),
                        Color(0xFF88A0C0).copy(alpha = baseA * 0.35f),
                    ),
                    start = Offset(rootX, rootY),
                    end = Offset(tipX, tipY),
                ),
                style = Stroke(
                    width = 1.4f + emb * 2.2f + i * 0.12f,
                    cap = StrokeCap.Round,
                ),
            )
        }
    }

    enum class ReroutePass {
        SUBSURFACE_BEHIND,
        THROUGH_SURFACE,
    }
}
