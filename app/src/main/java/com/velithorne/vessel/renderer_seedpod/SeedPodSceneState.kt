package com.velithorne.vessel.renderer_seedpod

import com.velithorne.vessel.growth_seedpod.SeedPodDisplayState
import com.velithorne.vessel.growth_seedpod.SeedPodGrowthStage
import com.velithorne.vessel.model.BranchVisualState
import com.velithorne.vessel.model.SeedBudVisualState
import com.velithorne.vessel.model.SeedPodDepthState
import com.velithorne.vessel.model.SeedPodLightingState
import com.velithorne.vessel.model.SeedPodMaterialState
import com.velithorne.vessel.model.SeedPodVisualState
import com.velithorne.vessel.model.SeedThermalVisualState
import com.velithorne.vessel.model.VesselPaletteState
import com.velithorne.vessel.physiology.PhysiologySnapshot
import com.velithorne.vessel.progression.LiveExpressionState

/**
 * Everything the seed pod canvas needs — **no** [com.velithorne.vessel.renderer.VesselSceneState].
 */
data class SeedPodSceneState(
    val physiology: PhysiologySnapshot,
    val podDisplay: SeedPodDisplayState,
    val palette: VesselPaletteState,
    val seedPodPalette: SeedPodPalette,
    val appearance: SeedPodVisualState,
    val materialState: SeedPodMaterialState,
    val depthState: SeedPodDepthState,
    val lightingState: SeedPodLightingState,
    val buds: SeedBudVisualState,
    val thermal: SeedThermalVisualState,
    /** Subtle lineage morphology bias for this device. */
    val branchVisual: BranchVisualState,
    val vitalityGlow: Float,
    val stressTint: Float,
    val feverIntensity: Float,
    val sleepDimming: Float,
    val neuralDrive: Float,
    val particleDensity: Float,
    val stressShiver: Float,
    /** Reversible live presentation — does not reduce structural stage. */
    val liveExpression: LiveExpressionState,
    /** Legacy organism renderer is inactive when this pipeline is used — always false for UI/debug. */
    val legacyVesselRendererActive: Boolean,
) {
    val stage: SeedPodGrowthStage get() = podDisplay.stage
}
