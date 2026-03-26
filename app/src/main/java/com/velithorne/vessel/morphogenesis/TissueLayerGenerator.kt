package com.velithorne.vessel.morphogenesis

import com.velithorne.vessel.util.Smoothing

object TissueLayerGenerator {

    fun envelope(
        genome: SpeciesGenome,
        field: GrowthPressureField,
        acc: PressureAccumulator,
        prev: TissueEnvelopeState?,
        tuning: GrowthTuning,
    ): TissueEnvelopeState {
        val raw = TissueEnvelopeState(
            shellOpacityMul = 0.85f + genome.shellThickness * 0.25f + field.perimeterShell * 0.15f,
            gelEnvelopeMul = 0.9f + field.cortical * 0.2f + genome.cranialExpansionBias * 0.1f,
            archiveLamellaDensity = 0.4f + genome.archiveLamellaBias * 0.35f + acc.archive * 0.2f,
            coolingVeilStrength = 0.35f + genome.coolingVeilBias * 0.4f + acc.thermal * 0.25f,
        )
        if (prev == null) return raw
        val a = tuning.genomeDriftAlpha
        return TissueEnvelopeState(
            shellOpacityMul = Smoothing.lerp(prev.shellOpacityMul, raw.shellOpacityMul, a),
            gelEnvelopeMul = Smoothing.lerp(prev.gelEnvelopeMul, raw.gelEnvelopeMul, a),
            archiveLamellaDensity = Smoothing.lerp(prev.archiveLamellaDensity, raw.archiveLamellaDensity, a),
            coolingVeilStrength = Smoothing.lerp(prev.coolingVeilStrength, raw.coolingVeilStrength, a),
        )
    }
}
