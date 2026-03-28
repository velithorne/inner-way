package com.velithorne.vessel.renderer_seedpod

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.velithorne.vessel.model.VesselPaletteState
import kotlin.math.max

/** Rear chamber depth fog + volumetric bands + specimen spotlight base. */
object SeedPodAtmospherePainter {

    fun drawBackdrop(
        scope: DrawScope,
        w: Float,
        h: Float,
        parallax: Offset,
        palette: VesselPaletteState,
        scene: SeedPodSceneState,
        tuning: SeedPodTuning,
    ) {
        val ox = parallax.x * 0.06f
        val oy = parallax.y * 0.05f
        scope.drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF101828), Color(0xFF080C14), Color(0xFF030508)),
                startY = oy,
                endY = h + oy,
            ),
            topLeft = Offset.Zero,
            size = Size(w, h),
        )
        val fogA = (
            0.05f + scene.particleDensity * 0.12f + scene.feverIntensity * 0.06f +
                scene.materialState.thermalHaze * 0.05f + scene.materialState.innerHaze * 0.04f
            ).coerceIn(0.04f, 0.24f)
        val mist = palette.chamberMist
        val dm = tuning.depth
        scope.drawRect(
            brush = Brush.verticalGradient(
                colorStops = arrayOf(
                    0f to mist.copy(alpha = 0f),
                    0.28f to mist.copy(alpha = fogA * 0.12f * dm.chamberFogRearMul),
                    0.55f to mist.copy(alpha = fogA * 0.45f),
                    1f to mist.copy(alpha = fogA * 0.85f),
                ),
                startY = 0f,
                endY = h,
            ),
            topLeft = Offset.Zero,
            size = Size(w, h),
        )
    }

    fun drawRearDepthFog(
        scope: DrawScope,
        w: Float,
        h: Float,
        pod: Offset,
        layerOffset: Offset,
        rearDarkening: Float,
        tuning: SeedPodTuning,
    ) {
        val c = pod + layerOffset
        val a = (rearDarkening * tuning.depth.occlusionAlphaMax * 1.2f).coerceIn(0.04f, 0.35f)
        scope.drawOval(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF020408).copy(alpha = a * 0.85f),
                    Color(0xFF000000).copy(alpha = 0f),
                ),
                center = Offset(c.x, c.y + h * 0.02f),
                radius = maxOf(w, h) * 0.38f,
            ),
            topLeft = Offset(c.x - w * 0.35f, c.y - h * 0.25f),
            size = Size(w * 0.7f, h * 0.55f),
        )
    }

    fun drawVolumetricBand(
        scope: DrawScope,
        w: Float,
        h: Float,
        parallax: Offset,
        depthBand: Float,
    ) {
        val y = h * (0.25f + depthBand * 0.12f) + parallax.y * 0.03f
        scope.drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color(0xFF0A1420).copy(alpha = 0f),
                    Color(0xFF0A1420).copy(alpha = 0.06f),
                    Color(0xFF0A1420).copy(alpha = 0f),
                ),
                startX = 0f,
                endX = w,
            ),
            topLeft = Offset(0f, y),
            size = Size(w, h * 0.08f),
        )
    }
}
