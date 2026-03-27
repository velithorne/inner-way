package com.velithorne.vessel.renderer_seedpod

import com.velithorne.vessel.growth_seedpod.SeedPodDisplayState
import com.velithorne.vessel.model.VesselPaletteState
import com.velithorne.vessel.physiology.PhysiologySnapshot
import com.velithorne.vessel.renderer.VesselPalette

/**
 * Physiology + seed pod growth → [SeedPodSceneState]. Does **not** use [com.velithorne.vessel.renderer.VesselRenderer].
 */
class SeedPodRenderer(
    private val tuning: SeedPodTuning = SeedPodTuning(),
) {

    fun map(physiology: PhysiologySnapshot, podDisplay: SeedPodDisplayState): SeedPodSceneState {
        val s = physiology.species
        val telem = physiology.telemetry
        val signalStrained = when {
            telem.networkConnected == false -> 0.55f
            else -> 0.12f
        }
        val palette: VesselPaletteState = VesselPalette.fromSpecies(s, signalStrained, com.velithorne.vessel.renderer.RenderTuning())
        val seedPodPalette = SeedPodPalette(base = palette)

        val appearance = SeedPodMaterialSystem.deriveAppearance(physiology, podDisplay, palette, tuning)
        val materialState = SeedPodMaterialSystem.deriveMaterialState(physiology, appearance)
        val depthState = SeedPodPseudoVolumeMapper.map(physiology, podDisplay.stage, appearance, tuning)
        val buds = SeedPodMaterialSystem.deriveBuds(podDisplay, tuning, appearance)
        val thermal = SeedPodMaterialSystem.deriveThermal(physiology, podDisplay, palette, tuning)
        val lightingState = SeedPodLightingModel.compute(physiology, appearance, thermal, tuning)

        val vitalityGlow = (s.vitality * 1.15f).coerceIn(0f, 1.4f)
        val stressShiver = (s.stress * tuning.stressShiverDegrees / 8f).coerceIn(0f, 1f)
        val particleDensity = (
            0.2f + s.respiration * 0.15f + s.neuralActivity * 0.2f + s.fever * 0.12f
            ).coerceIn(0f, 1f)

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
            vitalityGlow = vitalityGlow,
            stressTint = s.stress.coerceIn(0f, 1f),
            feverIntensity = s.fever.coerceIn(0f, 1f),
            sleepDimming = (s.sleepPressure * 0.42f).coerceIn(0f, 0.42f),
            neuralDrive = s.neuralActivity.coerceIn(0f, 1f),
            particleDensity = particleDensity,
            stressShiver = stressShiver,
            legacyVesselRendererActive = false,
        )
    }
}
