package com.velithorne.vessel.renderer_seedpod

import androidx.compose.ui.graphics.Color
import com.velithorne.vessel.growth_seedpod.SeedPodDisplayState
import com.velithorne.vessel.growth_seedpod.SeedPodGrowthStage
import com.velithorne.vessel.model.SeedBudVisualState
import com.velithorne.vessel.model.SeedPodMaterialState
import com.velithorne.vessel.model.SeedPodVisualState
import com.velithorne.vessel.model.SeedThermalVisualState
import com.velithorne.vessel.model.VesselPaletteState
import com.velithorne.vessel.physiology.PhysiologySnapshot
/**
 * Maps physiology + display growth + stage → material parameters for the seed pod renderer.
 */
object SeedPodMaterialSystem {

    fun deriveAppearance(
        physiology: PhysiologySnapshot,
        podDisplay: SeedPodDisplayState,
        palette: VesselPaletteState,
        tuning: SeedPodTuning,
    ): SeedPodVisualState {
        val s = physiology.species
        val stage = podDisplay.stage
        val sm = stageMultipliers(stage, tuning)

        val vitality = s.vitality.coerceIn(0f, 1f)
        val hunger = s.hunger.coerceIn(0f, 1f)
        val fever = s.fever.coerceIn(0f, 1f)
        val recovery = s.recovery.coerceIn(0f, 1f)
        val neural = s.neuralActivity.coerceIn(0f, 1f)
        val signal = s.signalArousal.coerceIn(0f, 1f)
        val structural = s.structuralLoad.coerceIn(0f, 1f)
        val stress = s.stress.coerceIn(0f, 1f)

        val coherence = podDisplay.podCoherence.coerceIn(0f, 1f)

        val shellOpacityMul = (
            sm.shellDim * (0.72f + vitality * 0.28f) * (1f - hunger * 0.22f) * (0.85f + coherence * 0.15f)
            ).coerceIn(0.35f, 1.15f)

        val shellEdgeBright = (
            0.55f + signal * 0.22f + neural * 0.12f + sm.edgeBoost
            ).coerceIn(0.35f, 1.2f)

        val innerHaze = (
            podDisplay.tissueHaze * sm.hazeMul + recovery * 0.12f + fever * 0.06f
            ).coerceIn(0f, 1f)

        val nucleusBright = (
            0.55f + vitality * 0.35f + neural * 0.15f - hunger * 0.2f + sm.nucleusBoost
            ).coerceIn(0.25f, 1.35f)

        val nucleusBloom = (
            0.4f + neural * 0.35f + recovery * 0.2f - stress * 0.1f
            ).coerceIn(0.2f, 1.2f)

        val facetAlpha = (
            tuning.facetBaseAlpha + structural * 0.35f + sm.facetBoost
            ).coerceIn(0.06f, 0.55f)

        val growthFront = (
            podDisplay.shellThickening * 0.45f + podDisplay.tissueHaze * 0.35f + sm.growthFrontMul
            ).coerceIn(0f, 1f)

        val spotlight = (0.35f + sm.spotlightMul + vitality * 0.2f).coerceIn(0.25f, 0.95f)
        val glass = (tuning.glassReflectionBase + recovery * 0.08f).coerceIn(0.06f, 0.28f)

        val particleScale = (0.75f + physiology.species.respiration * 0.15f + fever * 0.1f).coerceIn(0.6f, 1.25f)

        return SeedPodVisualState(
            shellOpacityMul = shellOpacityMul,
            shellEdgeBright = shellEdgeBright,
            innerHazeDensity = innerHaze,
            nucleusBrightnessMul = nucleusBright,
            nucleusBloomMul = nucleusBloom,
            facetLineAlpha = facetAlpha,
            growthFrontAlpha = growthFront,
            spotlightStrength = spotlight,
            glassReflectionAlpha = glass,
            particleScale = particleScale,
            stageBudScale = sm.budScale,
            shellClosedness = sm.closedness,
        )
    }

    fun deriveMaterialState(
        physiology: PhysiologySnapshot,
        appearance: SeedPodVisualState,
    ): SeedPodMaterialState {
        val s = physiology.species
        return SeedPodMaterialState(
            shellTranslucency = appearance.shellOpacityMul.coerceIn(0.3f, 1.2f),
            shellThicknessNorm = (0.35f + s.fever * 0.15f + s.structuralLoad * 0.1f).coerceIn(0.2f, 1f),
            edgeBrightness = appearance.shellEdgeBright,
            innerHaze = appearance.innerHazeDensity,
            thermalHaze = s.fever.coerceIn(0f, 1f),
            recoverySmoothing = s.recovery.coerceIn(0f, 1f),
            hungerDim = s.hunger.coerceIn(0f, 1f),
        )
    }

    fun deriveBuds(
        podDisplay: SeedPodDisplayState,
        tuning: SeedPodTuning,
        stageVisual: SeedPodVisualState,
    ): SeedBudVisualState {
        fun gate(raw: Float, threshold: Float): Float {
            if (raw < threshold) return 0f
            val over = (raw - threshold) / (1f - threshold + 1e-4f)
            return (over * stageVisual.stageBudScale).coerceIn(0f, 1.2f)
        }
        val crown = gate(podDisplay.crownNub, tuning.budVisibilityThresholdCrown)
        val latL = gate(podDisplay.lateralBudLeft, tuning.budVisibilityThresholdLateral)
        val latR = gate(podDisplay.lateralBudRight, tuning.budVisibilityThresholdLateral)
        val res = gate(podDisplay.reserveBulb, tuning.budVisibilityThresholdReserve)
        return SeedBudVisualState(crown = crown, lateralLeft = latL, lateralRight = latR, reserve = res)
    }

    fun deriveThermal(
        physiology: PhysiologySnapshot,
        podDisplay: SeedPodDisplayState,
        palette: VesselPaletteState,
        tuning: SeedPodTuning,
    ): SeedThermalVisualState {
        val fever = physiology.species.fever.coerceIn(0f, 1f)
        val stress = physiology.species.stress.coerceIn(0f, 1f)
        val tv = podDisplay.thermalVeil.coerceIn(0f, 1f)
        val edge = (tv * 0.55f + fever * 0.35f + stress * 0.15f).coerceIn(0f, 1f)
        val hot = palette.thermalHot.copy(alpha = 0.35f + fever * 0.4f)
        val cool = palette.shellRimCool.copy(alpha = 0.5f + (1f - fever) * 0.2f)
        return SeedThermalVisualState(
            edgeShimmer = (edge * tuning.thermalShimmerStrength).coerceIn(0f, 1f),
            hotspotAlpha = (fever * 0.45f + stress * 0.15f).coerceIn(0f, 0.85f),
            warmTint = hot,
            coolRim = cool,
        )
    }

    private data class StageMul(
        val shellDim: Float,
        val edgeBoost: Float,
        val hazeMul: Float,
        val nucleusBoost: Float,
        val facetBoost: Float,
        val growthFrontMul: Float,
        val spotlightMul: Float,
        val budScale: Float,
        val closedness: Float,
    )

    private fun stageMultipliers(stage: SeedPodGrowthStage, tuning: SeedPodTuning): StageMul {
        val g = tuning.stageVisual
        return when (stage) {
            SeedPodGrowthStage.DORMANT_POD -> StageMul(
                shellDim = g.dormantShellDim,
                edgeBoost = g.dormantEdge,
                hazeMul = g.dormantHaze,
                nucleusBoost = g.dormantNucleus,
                facetBoost = g.dormantFacet,
                growthFrontMul = g.dormantGrowthFront,
                spotlightMul = g.dormantSpotlight,
                budScale = g.dormantBudScale,
                closedness = g.dormantClosedness,
            )
            SeedPodGrowthStage.ACTIVATING_POD -> StageMul(
                shellDim = g.activatingShellDim,
                edgeBoost = g.activatingEdge,
                hazeMul = g.activatingHaze,
                nucleusBoost = g.activatingNucleus,
                facetBoost = g.activatingFacet,
                growthFrontMul = g.activatingGrowthFront,
                spotlightMul = g.activatingSpotlight,
                budScale = g.activatingBudScale,
                closedness = g.activatingClosedness,
            )
            SeedPodGrowthStage.GERMINATING_POD -> StageMul(
                shellDim = g.germinatingShellDim,
                edgeBoost = g.germinatingEdge,
                hazeMul = g.germinatingHaze,
                nucleusBoost = g.germinatingNucleus,
                facetBoost = g.germinatingFacet,
                growthFrontMul = g.germinatingGrowthFront,
                spotlightMul = g.germinatingSpotlight,
                budScale = g.germinatingBudScale,
                closedness = g.germinatingClosedness,
            )
            SeedPodGrowthStage.EARLY_BUDDING -> StageMul(
                shellDim = g.buddingShellDim,
                edgeBoost = g.buddingEdge,
                hazeMul = g.buddingHaze,
                nucleusBoost = g.buddingNucleus,
                facetBoost = g.buddingFacet,
                growthFrontMul = g.buddingGrowthFront,
                spotlightMul = g.buddingSpotlight,
                budScale = g.buddingBudScale,
                closedness = g.buddingClosedness,
            )
            SeedPodGrowthStage.EARLY_CHAMBERING -> StageMul(
                shellDim = g.chamberingShellDim,
                edgeBoost = g.chamberingEdge,
                hazeMul = g.chamberingHaze,
                nucleusBoost = g.chamberingNucleus,
                facetBoost = g.chamberingFacet,
                growthFrontMul = g.chamberingGrowthFront,
                spotlightMul = g.chamberingSpotlight,
                budScale = g.chamberingBudScale,
                closedness = g.chamberingClosedness,
            )
        }
    }
}
