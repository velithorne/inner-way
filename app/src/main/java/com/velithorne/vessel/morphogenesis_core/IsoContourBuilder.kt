package com.velithorne.vessel.morphogenesis_core

import kotlin.math.max

/**
 * Maps tissue field samples to shell contour multipliers (discovered from fields, not fixed template).
 */
object IsoContourBuilder {

    data class ContourShape(
        /** Shell ellipse radius scale vs base. */
        val shellRxMul: Float,
        val shellRyMul: Float,
        /** Inner chamber inset scale. */
        val innerRxMul: Float,
        val innerRyMul: Float,
        /** Vertical bias for crown (-) vs reserve (+) */
        val verticalSkew: Float,
    )

    fun build(
        centerField: TissueField,
        archetype: SeedArchetype,
        era: CanonicalLifeEra,
    ): ContourShape {
        val den = centerField.density.coerceIn(0f, 2f)
        val res = centerField.reserve.coerceIn(0f, 2f)
        val sig = centerField.signal.coerceIn(0f, 2f)
        val th = centerField.thermalTension.coerceIn(0f, 2f)
        val arch = centerField.archiveBurden.coerceIn(0f, 2f)
        val eraMul = when (era) {
            CanonicalLifeEra.SEED -> 0.92f
            CanonicalLifeEra.VEIL_STAGE -> 0.98f
            CanonicalLifeEra.CORE_ESTABLISHMENT -> 1f
            CanonicalLifeEra.BRANCHING_THRESHOLD -> 1.04f
            CanonicalLifeEra.ADULTHOOD -> 1.08f
        }
        val rx = (1f + sig * 0.06f + th * 0.04f + archetype.frondPotential * 0.05f) * eraMul
        val ry = (1f + res * 0.07f + den * 0.04f + archetype.crownPotential * 0.04f) * eraMul
        val innerRx = (0.92f - arch * 0.04f + archetype.archivePlatePotential * 0.02f).coerceIn(0.78f, 0.98f)
        val innerRy = (0.9f - res * 0.05f + archetype.reserveBasinCompression * 0.06f).coerceIn(0.72f, 0.98f)
        val skew = (sig - res) * 0.08f * archetype.asymmetryTolerance
        return ContourShape(
            shellRxMul = rx.coerceIn(0.88f, 1.15f),
            shellRyMul = ry.coerceIn(0.88f, 1.15f),
            innerRxMul = innerRx,
            innerRyMul = innerRy,
            verticalSkew = skew.coerceIn(-0.12f, 0.12f),
        )
    }
}
