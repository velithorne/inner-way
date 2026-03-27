package com.velithorne.vessel.renderer_seedpod

import androidx.compose.ui.graphics.Color
import com.velithorne.vessel.growth_seedpod.SeedPodDisplayState
import com.velithorne.vessel.growth_seedpod.SeedPodGrowthStage
import com.velithorne.vessel.model.VesselPaletteState
import com.velithorne.vessel.physiology.PhysiologySnapshot

/**
 * Everything the seed pod canvas needs — **no** [com.velithorne.vessel.renderer.VesselSceneState].
 */
data class SeedPodSceneState(
    val physiology: PhysiologySnapshot,
    val podDisplay: SeedPodDisplayState,
    val palette: VesselPaletteState,
    val vitalityGlow: Float,
    val stressTint: Float,
    val feverIntensity: Float,
    val sleepDimming: Float,
    val neuralDrive: Float,
    val particleDensity: Float,
    val stressShiver: Float,
    /** Legacy organism renderer is inactive when this pipeline is used — always false for UI/debug. */
    val legacyVesselRendererActive: Boolean,
) {
    val stage: SeedPodGrowthStage get() = podDisplay.stage
}
