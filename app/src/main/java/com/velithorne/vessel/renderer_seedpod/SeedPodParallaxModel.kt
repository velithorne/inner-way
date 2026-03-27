package com.velithorne.vessel.renderer_seedpod

import androidx.compose.ui.geometry.Offset
import com.velithorne.vessel.model.SeedPodDepthState
import com.velithorne.vessel.model.SeedPodLayerState

/**
 * Per-layer parallax from device tilt / parallax offset (gentle, mobile-safe).
 */
object SeedPodParallaxModel {

    fun compute(
        parallax: Offset,
        depth: SeedPodDepthState,
        tuning: SeedPodTuning,
    ): SeedPodLayerState {
        val px = parallax.x
        val py = parallax.y
        val stageD = depth.stageDepthSeparation
        val td = tuning.depth
        fun o(mul: Float): Offset = Offset(px * mul * stageD, py * mul * stageD)

        return SeedPodLayerState(
            rearAtmosphere = o(td.parallaxRearAtmosphere),
            rearShell = o(td.parallaxRearShell),
            innerHaze = o(td.parallaxInnerHaze),
            nucleus = o(td.parallaxNucleus),
            midChamber = o(td.parallaxMidChamber),
            budsCrown = o(td.parallaxBudCrown * depth.budDepthMul),
            budsLateral = o(td.parallaxBudLateral * depth.budDepthMul),
            budsReserve = o(td.parallaxBudReserve * depth.budDepthMul),
            frontShell = o(td.parallaxFrontShell),
            rimLight = o(td.parallaxRim),
            glass = Offset(-px * td.parallaxGlassOpposite * 0.35f, -py * td.parallaxGlassOpposite * 0.35f),
        )
    }
}
