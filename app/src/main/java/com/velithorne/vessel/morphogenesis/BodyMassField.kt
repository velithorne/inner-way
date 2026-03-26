package com.velithorne.vessel.morphogenesis

import kotlin.math.exp

/**
 * Soft influence field: seed + organ graph + pressure zones → visible body occupancy.
 */
data class BodyMassFieldState(
    /** Whole-body fill factor for renderer (0..1). */
    val totalOccupancy: Float,
    val cranialInfluence: Float,
    val centralInfluence: Float,
    val lateralInfluence: Float,
    val lowerInfluence: Float,
    val shellEnvelope: Float,
    /** Metaball-like blend width (higher = softer, more merged mass). */
    val blendSoftness: Float,
)

object BodyMassField {

    fun sample(
        seed: SeedCore,
        genome: SpeciesGenome,
        field: GrowthPressureField,
        acc: PressureAccumulator,
        chamber: ChamberMassModel,
        tuning: GrowthTuning,
    ): BodyMassFieldState {
        val occ = (
            seed.seedDensity * tuning.bodyMassSeedWeight +
                chamber.cranialCortex * 0.18f +
                chamber.centralMetabolic * 0.22f +
                chamber.lateralSignal * 0.16f +
                chamber.lowerArchiveBasin * 0.2f +
                chamber.perimeterShell * 0.14f +
                genome.shellThickness * tuning.bodyMassGenomeShellWeight
            ).coerceIn(0.12f, 1f)

        val soft = (0.42f + field.centralChamber * 0.22f + (1f - acc.hunger) * 0.12f + tuning.bodyMassBlendBase)
            .coerceIn(0.28f, 0.92f)

        return BodyMassFieldState(
            totalOccupancy = occ,
            cranialInfluence = smooth(chamber.cranialCortex * (0.55f + field.cortical * 0.45f)),
            centralInfluence = smooth(chamber.centralMetabolic * (0.5f + field.centralChamber * 0.5f)),
            lateralInfluence = smooth(chamber.lateralSignal * (0.48f + field.lateralSignal * 0.52f)),
            lowerInfluence = smooth(chamber.lowerArchiveBasin * (0.5f + acc.archive * 0.35f)),
            shellEnvelope = smooth(chamber.perimeterShell * (0.45f + field.perimeterShell * 0.55f)),
            blendSoftness = soft,
        )
    }

    /** Simple smoothstep for metaball-style weighting. */
    private fun smooth(x: Float): Float {
        val t = x.coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }

    /**
     * Radial falloff from seed center (normalized dx, dy in ~[-1,1]).
     */
    fun radialMass(dx: Float, dy: Float, sigma: Float): Float {
        val s = sigma.coerceAtLeast(0.08f)
        val d2 = dx * dx + dy * dy
        return exp((-d2 / (2f * s * s)).toDouble()).toFloat()
    }
}
