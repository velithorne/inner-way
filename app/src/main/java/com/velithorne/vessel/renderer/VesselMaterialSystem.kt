package com.velithorne.vessel.renderer

import com.velithorne.vessel.model.VesselMaterialState
import com.velithorne.vessel.physiology.SpeciesState

/** Maps physiology scalars to presentation material knobs (Phase 5). */
object VesselMaterialSystem {

    fun derive(s: SpeciesState, tuning: RenderTuning): VesselMaterialState {
        val vital = s.vitality.coerceIn(0f, 1f)
        val stress = s.stress.coerceIn(0f, 1f)
        val fever = s.fever.coerceIn(0f, 1f)
        val hunger = s.hunger.coerceIn(0f, 1f)
        val recovery = s.recovery.coerceIn(0f, 1f)
        val sleep = s.sleepPressure.coerceIn(0f, 1f)
        val structural = s.structuralLoad.coerceIn(0f, 1f)
        val signal = s.signalArousal.coerceIn(0f, 1f)

        val shellFill = (tuning.shellFillOpacityBase + vital * tuning.shellFillOpacityVitalityScale)
            .coerceIn(tuning.shellFillOpacityMin, tuning.shellFillOpacityMax) * (1f - sleep * tuning.sleepDimMax * 0.45f)
        val edgeA = (tuning.shellEdgeAlphaBase + vital * tuning.shellEdgeAlphaVitalityScale + stress * 0.08f)
            .coerceIn(0.08f, 0.55f) * (1f - hunger * 0.2f)

        return VesselMaterialState(
            shellFillOpacity = shellFill,
            shellEdgeAlpha = edgeA,
            shellEdgeThicknessPx = tuning.shellEdgeThicknessMin +
                stress * tuning.shellEdgeThicknessStressScale + fever * 0.35f,
            innerHazeAlpha = tuning.innerHazeAlphaBase + structural * 0.12f + fever * 0.06f,
            organTranslucency = (0.35f + vital * 0.35f - stress * 0.08f - fever * 0.06f).coerceIn(0.2f, 0.85f),
            heatTintStrength = fever * tuning.thermalTintDisplayScale + stress * 0.12f,
            recoverySheenAlpha = recovery * tuning.recoverySheenScale * (1f - sleep * 0.4f),
            sleepGlobalDim = sleep * tuning.sleepDimMax,
            vitalityCoherence = (vital * 0.55f + (1f - stress) * 0.25f + recovery * 0.2f).coerceIn(0f, 1f),
            pathwayBaseAlpha = tuning.pathwayBaseAlpha * (0.65f + signal * 0.35f + s.neuralActivity * 0.25f).coerceIn(0.35f, 1.15f),
            pathwayPulseSpeedMul = 0.85f + vital * 0.25f + fever * 0.15f,
            thermalEdgeBleed = fever * tuning.thermalEdgeBleedScale + stress * 0.15f,
            thermalShimmerStrength = fever * tuning.thermalShimmerDisplayScale + stress * 0.12f,
            organHaloIntensity = tuning.organHaloBase * (0.7f + vital * 0.35f - hunger * 0.25f).coerceIn(0.35f, 1.25f),
            archiveStrataContrast = (structural * 0.75f + (1f - vital) * 0.15f).coerceIn(0f, 1f),
            selectionPeerDim = tuning.selectionNonSelectedDim,
            selectionFocusBoost = tuning.selectionFocusIntensity,
        )
    }
}
