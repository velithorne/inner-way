package com.velithorne.vessel.juvenile_form

import com.velithorne.vessel.branching.LineageBranch
import com.velithorne.vessel.config.SimulationMode
import com.velithorne.vessel.growth_seedpod.SeedPodGrowthStage
import com.velithorne.vessel.model.BiographyVisualState
import com.velithorne.vessel.model.BodyRegionState
import com.velithorne.vessel.model.JuvenileVisualState
import com.velithorne.vessel.model.TopologyExpansionState
import com.velithorne.vessel.model.VisibleMorphologyState
import kotlin.math.abs

object JuvenileArchitectureEngine {

    fun inactiveState(): JuvenileFormState = inactive()

    fun build(
        stage: SeedPodGrowthStage,
        lead: LineageBranch,
        branchReadiness: Float,
        visualExpression: Float,
        visible: VisibleMorphologyState,
        biography: BiographyVisualState,
        mode: SimulationMode,
    ): JuvenileFormState {
        val tuning = JuvenileTuning.forMode(mode)
        if (stage.ordinal < tuning.minStageOrdinalForJuvenile) {
            return inactive()
        }
        val base = JuvenileFormFamily.basePlan(lead)
        val g = visible.generatedTopologyInfluence.coerceIn(0f, 1f)
        val f = visible.fallbackSeedInfluence.coerceIn(0f, 1f)
        val readiness = branchReadiness.coerceIn(0f, 1f)
        val expr = visualExpression.coerceIn(0f, 1f)

        var emergence = (
            g * tuning.emergenceFromTopologyMul +
                (1f - f) * 0.15f +
                readiness * 0.22f +
                expr * 0.18f +
                tuning.devEmergenceBoost
            ).coerceIn(0f, 1f)
        emergence = (emergence * (0.55f + stage.ordinal / 20f)).coerceIn(0f, 1f)

        val blend = tuning.regionMassBlend
        val plan = blendPlan(base, visible, blend, tuning, lead)

        val traits = JuvenileTraitMap(
            upperBodyRise = (plan.crownMass * 0.45f + g * 0.35f).coerceIn(0f, 1f),
            lateralWingSpan = (plan.lateralMass * 0.5f + visible.frondAsymmetryMul * 0.25f).coerceIn(0f, 1f),
            lowerBasinDepth = (plan.reserveMass * 0.55f + visible.reserveBasinDepthMul * 0.2f).coerceIn(0f, 1f),
            shellPlateEmphasis = (plan.shellMass * 0.5f + visible.shellUpperPlate * 0.25f).coerceIn(0f, 1f),
            supportBraceVisibility = (plan.supportMass * 0.45f).coerceIn(0f, 1f),
            contourBodyLikeness = (emergence * 0.7f + g * 0.3f).coerceIn(0f, 1f),
            viewportOverflowHint = (emergence * tuning.viewportOverflowSoftness * 1.4f).coerceIn(0f, 0.45f),
        )

        val masses = mapOf(
            JuvenileRegion.CROWN_REGION to plan.crownMass,
            JuvenileRegion.CORE_REGION to plan.coreMass,
            JuvenileRegion.LATERAL_REGION_LEFT to plan.lateralMass * 0.92f,
            JuvenileRegion.LATERAL_REGION_RIGHT to plan.lateralMass * 0.92f,
            JuvenileRegion.RESERVE_REGION to plan.reserveMass,
            JuvenileRegion.SHELL_REGION to plan.shellMass,
            JuvenileRegion.SUPPORT_REGION to plan.supportMass,
            JuvenileRegion.ARCHIVE_REGION to plan.archiveMass,
            JuvenileRegion.SCAR_OVERLAY to (biography.scars.size * 0.08f).coerceIn(0f, 0.55f),
            JuvenileRegion.REROUTE_OVERLAY to (biography.rerouteCount * 0.06f).coerceIn(0f, 0.5f),
        )

        val regions = BodyRegionState(
            masses = masses,
            crownDepth = 0.35f + plan.crownMass * 0.4f,
            coreDepth = 0.42f + plan.coreMass * 0.35f,
            lateralDepth = 0.38f + plan.lateralMass * 0.35f,
            reserveDepth = 0.48f + plan.reserveMass * 0.42f,
        )

        val expansion = JuvenileTopologyEngine.expand(plan, visible, tuning, emergence)

        val jVis = JuvenileVisualState(
            crownElevationMul = 1f + traits.upperBodyRise * tuning.contourExpansionStrength * 1.8f,
            lateralSpanMul = 1f + traits.lateralWingSpan * tuning.topologyDivergenceStrength * 1.6f,
            reserveDropMul = 1f + traits.lowerBasinDepth * 0.9f,
            shellForwardMul = 1f + traits.shellPlateEmphasis * 0.55f,
            supportCrossMul = 1f + traits.supportBraceVisibility * 0.7f,
            bodySilhouetteStretchX = 1f + (plan.lateralMass - 0.5f) * tuning.branchArchitectureWeight * 0.35f,
            bodySilhouetteStretchY = 1f + (plan.crownMass - plan.reserveMass) * tuning.branchArchitectureWeight * 0.25f,
            chamberOrganBias = emergence * 0.85f,
            active = emergence > 0.18f,
        )

        val transition = JuvenileTransitionState(
            juvenileEmergence = emergence,
            topologyExpandedBeyondSeed = expansion.expansionFactor > 0.35f,
            seedTraceOnlyLineageMemory = emergence > 0.62f && g > 0.55f,
        )

        return JuvenileFormState(
            active = jVis.active,
            bodyPlan = plan,
            traits = traits,
            transition = transition,
            regions = regions,
            expansion = expansion,
            visual = jVis,
        )
    }

    private fun blendPlan(
        base: JuvenileBodyPlan,
        visible: VisibleMorphologyState,
        blend: Float,
        tuning: JuvenileTuning,
        lead: LineageBranch,
    ): JuvenileBodyPlan {
        val w = blend * tuning.branchArchitectureWeight
        val crown = (base.crownMass * (1f - w) + visible.crownChamberRadiusMul * 0.08f * w).coerceIn(0.15f, 1f)
        val lat = (base.lateralMass * (1f - w) + visible.frondDensityMul * 0.35f * w).coerceIn(0.15f, 1f)
        val res = (base.reserveMass * (1f - w) + visible.reserveBasinDepthMul * 0.3f * w).coerceIn(0.15f, 1f)
        val shell = (base.shellMass * (1f - w) + (visible.shellUpperPlate + visible.shellLowerPlate) * 0.25f * w).coerceIn(0.15f, 1f)
        val sup = (base.supportMass * (1f - w) + visible.visibleAsymmetryScore * 0.4f * w).coerceIn(0.15f, 1f)
        val arch = (base.archiveMass * (1f - w) + visible.shellLeftWing * 0.15f * w).coerceIn(0.15f, 1f)
        val core = (base.coreMass * (1f - w) + 0.5f * w).coerceIn(0.15f, 1f)
        val dom = when {
            crown >= lat && crown >= res && crown >= shell -> JuvenileRegion.CROWN_REGION
            lat >= res && lat >= shell -> JuvenileRegion.LATERAL_REGION_LEFT
            res >= shell -> JuvenileRegion.RESERVE_REGION
            shell >= sup -> JuvenileRegion.SHELL_REGION
            else -> base.dominantRegion
        }
        return JuvenileBodyPlan(
            crownMass = crown,
            coreMass = core,
            lateralMass = lat,
            reserveMass = res,
            shellMass = shell,
            supportMass = sup,
            archiveMass = arch,
            dominantRegion = dom,
        )
    }

    private fun inactive(): JuvenileFormState = JuvenileFormState(
        active = false,
        bodyPlan = JuvenileFormFamily.basePlan(LineageBranch.BALANCED),
        traits = JuvenileTraitMap(0f, 0f, 0f, 0f, 0f, 0f, 0f),
        transition = JuvenileTransitionState(0f, false, false),
        regions = BodyRegionState(
            masses = emptyMap(),
            crownDepth = 0.3f,
            coreDepth = 0.3f,
            lateralDepth = 0.3f,
            reserveDepth = 0.3f,
        ),
        expansion = TopologyExpansionState(0f, 0f, 0f, 0f),
        visual = JuvenileVisualState.inactive(),
    )
}
