package com.velithorne.vessel.renderer_seedpod

import androidx.compose.ui.geometry.Offset
import com.velithorne.vessel.growth_seedpod.SeedPodGrowthStage
import com.velithorne.vessel.model.BranchVisualState
import com.velithorne.vessel.model.SeedPodDepthState
import com.velithorne.vessel.model.SeedPodVisualState
import com.velithorne.vessel.physiology.PhysiologySnapshot
import kotlin.math.max

/**
 * Maps stage + physiology + appearance → pseudo-volume / depth scalars (no mesh — 2.5D only).
 */
object SeedPodPseudoVolumeMapper {

    fun map(
        physiology: PhysiologySnapshot,
        stage: SeedPodGrowthStage,
        appearance: SeedPodVisualState,
        tuning: SeedPodTuning,
        branch: BranchVisualState = BranchVisualState.neutral(),
    ): SeedPodDepthState {
        val s = physiology.species
        val vitality = s.vitality.coerceIn(0f, 1f)
        val hunger = s.hunger.coerceIn(0f, 1f)
        val fever = s.fever.coerceIn(0f, 1f)
        val recovery = s.recovery.coerceIn(0f, 1f)
        val neural = s.neuralActivity.coerceIn(0f, 1f)
        val signal = s.signalArousal.coerceIn(0f, 1f)
        val structural = s.structuralLoad.coerceIn(0f, 1f)

        val stageSep = when (stage) {
            SeedPodGrowthStage.DORMANT_POD -> 0.55f
            SeedPodGrowthStage.ACTIVATING_POD -> 0.68f
            SeedPodGrowthStage.GERMINATING_POD -> 0.82f
            SeedPodGrowthStage.EARLY_BUDDING -> 0.92f
            SeedPodGrowthStage.EARLY_CHAMBERING -> 0.96f
            SeedPodGrowthStage.CHAMBER_MATURED -> 1f
            SeedPodGrowthStage.LINEAGE_DIFFERENTIATING,
            SeedPodGrowthStage.FIRST_BRANCH_FORMING,
            SeedPodGrowthStage.BRANCH_STABILIZING,
            SeedPodGrowthStage.SPECIALIZATION_EMERGING,
            SeedPodGrowthStage.SPECIALIZATION_ESTABLISHED,
            -> 1f
        }

        val td = tuning.depth
        val shellThick = (
            td.shellFrontThicknessBase +
                appearance.shellOpacityMul * 0.12f +
                fever * 0.08f +
                (1f - hunger) * 0.06f
            ).coerceIn(0.25f, 1f) * branch.shellBandMul

        val rearDark = (
            td.rearDarkeningBase +
                structural * 0.15f +
                hunger * 0.12f -
                recovery * 0.08f
            ).coerceIn(0.12f, 0.55f)

        val innerVol = (
            appearance.innerHazeDensity * 0.4f + stageSep * 0.35f + neural * 0.15f
            ).coerceIn(0.2f, 1f) * branch.innerMassMul

        val budDepth = (
            td.budDepthMulBase + signal * 0.12f + stageSep * 0.15f
            ).coerceIn(0.45f, 1.15f) * (0.92f + branch.lateralFrondMul * 0.08f)

        val stageNucleusBias = when (stage) {
            SeedPodGrowthStage.DORMANT_POD -> -0.08f
            SeedPodGrowthStage.ACTIVATING_POD -> 0.02f
            SeedPodGrowthStage.GERMINATING_POD -> 0.08f
            SeedPodGrowthStage.EARLY_BUDDING -> 0.12f
            SeedPodGrowthStage.EARLY_CHAMBERING -> 0.1f
            SeedPodGrowthStage.CHAMBER_MATURED -> 0.14f
            SeedPodGrowthStage.LINEAGE_DIFFERENTIATING,
            SeedPodGrowthStage.FIRST_BRANCH_FORMING,
            SeedPodGrowthStage.BRANCH_STABILIZING,
            SeedPodGrowthStage.SPECIALIZATION_EMERGING,
            SeedPodGrowthStage.SPECIALIZATION_ESTABLISHED,
            -> 0.16f
        }
        val burial = (
            td.nucleusBurialBase +
                stageNucleusBias +
                (1f - vitality) * 0.12f +
                hunger * 0.1f
            ).coerceIn(0.72f, 0.98f) * (1f - (branch.crownBloomMul - 1f) * 0.25f)

        val recessY = td.nucleusDepthOffsetPx * (1.1f - burial)

        return SeedPodDepthState(
            nucleusRecessOffset = Offset(0f, recessY * branch.bracingMul),
            shellThicknessVisual = shellThick.coerceIn(0.2f, 1.15f),
            rearDarkening = rearDark,
            innerVolumeExpand = innerVol.coerceIn(0.15f, 1.05f),
            budDepthMul = budDepth.coerceIn(0.4f, 1.2f),
            stageDepthSeparation = stageSep,
            nucleusBurialScale = burial.coerceIn(0.65f, 0.99f),
        )
    }
}
