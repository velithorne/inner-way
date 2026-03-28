package com.velithorne.vessel.renderer_seedpod

import com.velithorne.vessel.config.SimulationMode
import com.velithorne.vessel.model.BiographyVisualState
import com.velithorne.vessel.model.BranchVisualState
import com.velithorne.vessel.model.GeneratedAnatomyState
import com.velithorne.vessel.growth_seedpod.SeedPodDisplayState
import com.velithorne.vessel.model.VesselPaletteState
import com.velithorne.vessel.physiology.PhysiologySnapshot
import com.velithorne.vessel.progression.LiveExpressionMapper
import com.velithorne.vessel.renderer.VesselPalette
import com.velithorne.vessel.genesis.SeedGenesisSnapshot
import com.velithorne.vessel.juvenile_form.JuvenileArchitectureEngine
import com.velithorne.vessel.model.BirthTraitState
import com.velithorne.vessel.model.GenesisVisualState
import com.velithorne.vessel.model.MinimumViableBodyState
import com.velithorne.vessel.morphogenesis_core.GrowthPressureState
import com.velithorne.vessel.growth_seedpod.SeedPodGrowthStage

/**
 * Physiology + seed pod growth → [SeedPodSceneState]. Does **not** use [com.velithorne.vessel.renderer.VesselRenderer].
 */
class SeedPodRenderer(
    private val tuning: SeedPodTuning = SeedPodTuning(),
) {

    fun map(
        physiology: PhysiologySnapshot,
        podDisplay: SeedPodDisplayState,
        branchVisual: BranchVisualState = BranchVisualState.neutral(),
        generatedAnatomy: GeneratedAnatomyState? = null,
        biographyVisual: BiographyVisualState = BiographyVisualState.neutral(),
        animTimeSec: Float = 0f,
        simulationMode: SimulationMode = SimulationMode.RELEASE_REALTIME,
        structuralProgress: Float = 0f,
        growthPressure: GrowthPressureState? = null,
        genesisSnapshot: SeedGenesisSnapshot? = null,
    ): SeedPodSceneState {
        val s = physiology.species
        val telem = physiology.telemetry
        val signalStrained = when {
            telem.networkConnected == false -> 0.55f
            else -> 0.12f
        }
        val paletteBase: VesselPaletteState = VesselPalette.fromSpecies(s, signalStrained, com.velithorne.vessel.renderer.RenderTuning())
        val palette = BranchPaletteTint.apply(paletteBase, branchVisual)
        val seedPodPalette = SeedPodPalette(base = palette)

        val live = LiveExpressionMapper.map(physiology)
        val appearance = SeedPodMaterialSystem.deriveAppearance(physiology, podDisplay, palette, tuning, live, branchVisual)
        val materialState = SeedPodMaterialSystem.deriveMaterialState(physiology, appearance, branchVisual)
        val depthState = SeedPodPseudoVolumeMapper.map(
            physiology, podDisplay.stage, appearance, tuning, branchVisual, generatedAnatomy,
        )
        val buds = SeedPodMaterialSystem.deriveBuds(podDisplay, tuning, appearance, live, branchVisual)
        val thermal = SeedPodMaterialSystem.deriveThermal(physiology, podDisplay, palette, tuning)
        val lightingState = SeedPodLightingModel.compute(physiology, appearance, thermal, tuning)

        val vitalityGlow = (s.vitality * 1.15f).coerceIn(0f, 1.4f)
        val stressShiver = (s.stress * tuning.stressShiverDegrees / 8f).coerceIn(0f, 1f)
        val particleDensity = (
            0.2f + s.respiration * 0.15f + s.neuralActivity * 0.2f + s.fever * 0.12f
            ).coerceIn(0f, 1f)

        val genesisBirthStage = isGenesisBirthStage(podDisplay.stage)
        val vis = VisibilityOverrideMapper.map(
            anatomy = generatedAnatomy,
            biography = biographyVisual,
            branch = branchVisual,
            stage = podDisplay.stage,
            structuralProgress = structuralProgress,
            mode = simulationMode,
            pressure = growthPressure,
            genesisBirthStage = genesisBirthStage,
        )

        val gs = genesisSnapshot?.state
        val genesisVisual = if (gs != null) {
            GenesisVisualState(
                genesisRenderPathActive = genesisBirthStage,
                legacySeedScaffoldSuppressed = genesisBirthStage,
                minimumViableBodyLabel = com.velithorne.vessel.genesis.BirthExplainer.minimumViableLabel(gs.minimumViableBody),
                genesisContourDriver = gs.genesisContourDriver,
            )
        } else {
            GenesisVisualState(
                genesisRenderPathActive = false,
                legacySeedScaffoldSuppressed = false,
                minimumViableBodyLabel = "",
                genesisContourDriver = "",
            )
        }
        val mvbState = if (gs != null) {
            MinimumViableBodyState(gs.minimumViableBody, com.velithorne.vessel.genesis.BirthExplainer.minimumViableLabel(gs.minimumViableBody))
        } else null
        val birthTraits = if (gs != null) {
            BirthTraitState(
                symmetryHint = gs.traits.symmetryBias,
                coherenceHint = gs.traits.coherenceBias,
                asymmetryHint = gs.traits.latentAsymmetryBias,
            )
        } else null

        val juvenileForm = JuvenileArchitectureEngine.build(
            stage = podDisplay.stage,
            lead = branchVisual.leadingBranch,
            branchReadiness = branchVisual.branchReadiness,
            visualExpression = branchVisual.visualExpressionMagnitude,
            visible = vis.visible,
            biography = biographyVisual,
            mode = simulationMode,
        )

        return SeedPodSceneState(
            physiology = physiology,
            podDisplay = podDisplay,
            palette = palette,
            seedPodPalette = seedPodPalette,
            appearance = appearance,
            materialState = materialState,
            depthState = depthState,
            lightingState = lightingState,
            buds = buds,
            thermal = thermal,
            branchVisual = branchVisual,
            vitalityGlow = vitalityGlow,
            stressTint = s.stress.coerceIn(0f, 1f),
            feverIntensity = s.fever.coerceIn(0f, 1f),
            sleepDimming = (s.sleepPressure * 0.42f).coerceIn(0f, 0.42f),
            neuralDrive = s.neuralActivity.coerceIn(0f, 1f),
            particleDensity = particleDensity,
            stressShiver = stressShiver,
            legacyVesselRendererActive = false,
            liveExpression = live,
            generatedAnatomy = generatedAnatomy,
            biographyVisual = biographyVisual,
            animTimeSec = animTimeSec,
            visibleMorphology = vis.visible,
            seedTrace = vis.seedTrace,
            generatedTopology = vis.topology,
            fallbackMode = vis.fallbackMode,
            seedBurial = vis.seedBurial,
            contourGeometry = vis.contourGeometry,
            rerouteIntegration = vis.rerouteIntegration,
            juvenileForm = juvenileForm,
            genesisVisual = genesisVisual,
            genesisBirthStage = genesisBirthStage,
            minimumViableBody = mvbState,
            birthTraits = birthTraits,
            genesisSnapshot = genesisSnapshot,
            growthPressure = growthPressure,
        )
    }

    private fun isGenesisBirthStage(stage: SeedPodGrowthStage): Boolean =
        stage.ordinal <= SeedPodGrowthStage.ACTIVATING_POD.ordinal
}
