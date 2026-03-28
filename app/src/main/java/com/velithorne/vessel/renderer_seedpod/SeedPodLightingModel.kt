package com.velithorne.vessel.renderer_seedpod

import com.velithorne.vessel.model.SeedPodLightingState
import com.velithorne.vessel.model.SeedPodVisualState
import com.velithorne.vessel.model.SeedThermalVisualState
import com.velithorne.vessel.physiology.PhysiologySnapshot

object SeedPodLightingModel {

    fun compute(
        physiology: PhysiologySnapshot,
        appearance: SeedPodVisualState,
        thermal: SeedThermalVisualState,
        tuning: SeedPodTuning,
    ): SeedPodLightingState {
        val s = physiology.species
        val v = s.vitality.coerceIn(0f, 1f)
        val r = s.recovery.coerceIn(0f, 1f)
        val n = s.neuralActivity.coerceIn(0f, 1f)
        val sig = s.signalArousal.coerceIn(0f, 1f)
        val f = s.fever.coerceIn(0f, 1f)

        val d = tuning.depth
        return SeedPodLightingState(
            rimLight = (d.rimLightBase + appearance.shellEdgeBright * 0.25f + v * 0.15f).coerceIn(0.15f, 1f),
            sideFalloff = (0.35f + (1f - v) * 0.2f).coerceIn(0.2f, 0.65f),
            coreBloom = (d.coreBloomBase + appearance.nucleusBloomMul * 0.2f + n * 0.2f).coerceIn(0.2f, 1f),
            shellCatchlight = (0.25f + appearance.shellOpacityMul * 0.15f + r * 0.12f).coerceIn(0.15f, 0.85f),
            thermalHotspot = thermal.hotspotAlpha.coerceIn(0f, 1f),
            lateralSheen = (0.15f + sig * 0.35f + f * 0.1f).coerceIn(0f, 1f),
            chamberSpotlight = appearance.spotlightStrength.coerceIn(0.2f, 1f),
        )
    }
}
