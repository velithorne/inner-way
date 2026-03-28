package com.velithorne.vessel.renderer_seedpod

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.velithorne.vessel.juvenile_form.JuvenileFormState
import com.velithorne.vessel.juvenile_form.JuvenileRegion
import com.velithorne.vessel.model.VesselPaletteState
import kotlin.math.max

object JuvenileRegionPainter {

    fun draw(
        scope: DrawScope,
        pod: Offset,
        radii: SeedPodContourBuilder.PodRadii,
        palette: VesselPaletteState,
        form: JuvenileFormState,
    ) {
        if (!form.active) return
        val m = form.regions.masses
        val rx = max(radii.shellRx, radii.shellRy)
        fun glow(at: Offset, r: Float, alpha: Float) {
            scope.drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        palette.gelMedium.copy(alpha = alpha * 0.35f),
                        Color(0xFF000000).copy(alpha = 0f),
                    ),
                    center = at,
                    radius = r * 2.2f,
                ),
                radius = r,
                center = at,
            )
        }
        val jc = form.transition.juvenileEmergence
        m[JuvenileRegion.CROWN_REGION]?.let { w ->
            if (w > 0.35f) glow(Offset(pod.x, pod.y - rx * 0.72f), rx * 0.18f * w * jc, w)
        }
        m[JuvenileRegion.RESERVE_REGION]?.let { w ->
            if (w > 0.35f) glow(Offset(pod.x, pod.y + rx * 0.68f), rx * 0.22f * w * jc, w)
        }
        m[JuvenileRegion.LATERAL_REGION_LEFT]?.let { w ->
            if (w > 0.35f) glow(Offset(pod.x - rx * 0.75f, pod.y + rx * 0.05f), rx * 0.14f * w * jc, w)
        }
        m[JuvenileRegion.LATERAL_REGION_RIGHT]?.let { w ->
            if (w > 0.35f) glow(Offset(pod.x + rx * 0.75f, pod.y + rx * 0.05f), rx * 0.14f * w * jc, w)
        }
        m[JuvenileRegion.CORE_REGION]?.let { w ->
            if (w > 0.4f) glow(pod, rx * 0.2f * w * jc, w * 0.7f)
        }
    }
}
