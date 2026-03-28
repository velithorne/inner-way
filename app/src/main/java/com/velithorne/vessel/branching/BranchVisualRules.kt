package com.velithorne.vessel.branching

/**
 * Per-family visual language — same synthetic species, different emphasis.
 * Values are **targets relative to 1.0** where 1.0 = balanced baseline; blended by affinity × strength.
 */
data class BranchVisualRule(
    /** Shell membrane rings / rim emphasis. */
    val shellBandMul: Float,
    /** Lateral bud / frond expressiveness. */
    val lateralFrondMul: Float,
    /** Upper crown / neural chamber. */
    val crownBloomMul: Float,
    /** Lower reserve bulb dominance. */
    val reserveBulbMul: Float,
    /** Inner haze / stratified chamber density. */
    val innerMassMul: Float,
    /** Bracing strokes / taut silhouette. */
    val bracingMul: Float,
    /** Horizontal silhouette spread (signal / thermal perimeter). */
    val contourStretchXMul: Float,
    /** Vertical silhouette (crown lift vs lower mass). */
    val contourStretchYMul: Float,
    /** Extra lateral reach beyond base frond. */
    val lateralReachMul: Float,
    /** Shell thickness vs base geometry (perimeter armor). */
    val shellThicknessMul: Float,
    /** Inner volume bias: -1 = lower mass, +1 = upper chamber, 0 = center. */
    val innerVolumeFocusY: Float,
    /** Thermal veil / edge warmth emphasis. */
    val thermalVeilEmphasisMul: Float,
    /** Palette warmth on shell edge (additive tint strength 0..1 scale). */
    val paletteWarmthBias: Float,
    /** Cool conductive accent on sides (cyan/teal emphasis). */
    val paletteCoolSideBias: Float,
    /** Upper neural / crown tint strength. */
    val paletteCrownTintBias: Float,
    /** Lower reservoir amber emphasis. */
    val paletteReserveTintBias: Float,
) {
    companion object {
        val balanced = BranchVisualRule(
            shellBandMul = 1f,
            lateralFrondMul = 1f,
            crownBloomMul = 1f,
            reserveBulbMul = 1f,
            innerMassMul = 1f,
            bracingMul = 1f,
            contourStretchXMul = 1f,
            contourStretchYMul = 1f,
            lateralReachMul = 1f,
            shellThicknessMul = 1f,
            innerVolumeFocusY = 0f,
            thermalVeilEmphasisMul = 1f,
            paletteWarmthBias = 0f,
            paletteCoolSideBias = 0f,
            paletteCrownTintBias = 0f,
            paletteReserveTintBias = 0f,
        )

        fun forBranch(b: LineageBranch): BranchVisualRule = when (b) {
            LineageBranch.THERMAL_SHELL -> BranchVisualRule(
                shellBandMul = 1.14f,
                lateralFrondMul = 0.94f,
                crownBloomMul = 0.96f,
                reserveBulbMul = 0.97f,
                innerMassMul = 1.05f,
                bracingMul = 1.05f,
                contourStretchXMul = 1.03f,
                contourStretchYMul = 0.98f,
                lateralReachMul = 0.92f,
                shellThicknessMul = 1.1f,
                innerVolumeFocusY = -0.15f,
                thermalVeilEmphasisMul = 1.18f,
                paletteWarmthBias = 0.22f,
                paletteCoolSideBias = -0.05f,
                paletteCrownTintBias = 0f,
                paletteReserveTintBias = 0.05f,
            )
            LineageBranch.SIGNAL_FROND -> BranchVisualRule(
                shellBandMul = 1.02f,
                lateralFrondMul = 1.2f,
                crownBloomMul = 1.02f,
                reserveBulbMul = 0.98f,
                innerMassMul = 0.98f,
                bracingMul = 0.98f,
                contourStretchXMul = 1.08f,
                contourStretchYMul = 1.02f,
                lateralReachMul = 1.16f,
                shellThicknessMul = 0.98f,
                innerVolumeFocusY = 0.05f,
                thermalVeilEmphasisMul = 0.95f,
                paletteWarmthBias = -0.04f,
                paletteCoolSideBias = 0.2f,
                paletteCrownTintBias = 0.04f,
                paletteReserveTintBias = 0f,
            )
            LineageBranch.CROWN_NEURAL -> BranchVisualRule(
                shellBandMul = 1.04f,
                lateralFrondMul = 0.98f,
                crownBloomMul = 1.18f,
                reserveBulbMul = 0.96f,
                innerMassMul = 1.04f,
                bracingMul = 1f,
                contourStretchXMul = 1f,
                contourStretchYMul = 1.08f,
                lateralReachMul = 1f,
                shellThicknessMul = 1f,
                innerVolumeFocusY = 0.35f,
                thermalVeilEmphasisMul = 0.92f,
                paletteWarmthBias = 0.06f,
                paletteCoolSideBias = 0.08f,
                paletteCrownTintBias = 0.18f,
                paletteReserveTintBias = 0f,
            )
            LineageBranch.RESERVE_BASIN -> BranchVisualRule(
                shellBandMul = 1.03f,
                lateralFrondMul = 0.96f,
                crownBloomMul = 0.97f,
                reserveBulbMul = 1.16f,
                innerMassMul = 1.06f,
                bracingMul = 1.02f,
                contourStretchXMul = 0.98f,
                contourStretchYMul = 1.06f,
                lateralReachMul = 0.95f,
                shellThicknessMul = 1.04f,
                innerVolumeFocusY = -0.42f,
                thermalVeilEmphasisMul = 1.02f,
                paletteWarmthBias = 0.1f,
                paletteCoolSideBias = -0.02f,
                paletteCrownTintBias = 0f,
                paletteReserveTintBias = 0.16f,
            )
            LineageBranch.ARCHIVE_CORE -> BranchVisualRule(
                shellBandMul = 1.06f,
                lateralFrondMul = 0.9f,
                crownBloomMul = 0.94f,
                reserveBulbMul = 1.02f,
                innerMassMul = 1.14f,
                bracingMul = 1.04f,
                contourStretchXMul = 0.97f,
                contourStretchYMul = 0.96f,
                lateralReachMul = 0.88f,
                shellThicknessMul = 1.06f,
                innerVolumeFocusY = 0f,
                thermalVeilEmphasisMul = 0.9f,
                paletteWarmthBias = -0.06f,
                paletteCoolSideBias = 0f,
                paletteCrownTintBias = 0.04f,
                paletteReserveTintBias = 0.06f,
            )
            LineageBranch.MOTION_BRACED -> BranchVisualRule(
                shellBandMul = 1.08f,
                lateralFrondMul = 0.96f,
                crownBloomMul = 0.98f,
                reserveBulbMul = 1f,
                innerMassMul = 1f,
                bracingMul = 1.14f,
                contourStretchXMul = 1.02f,
                contourStretchYMul = 1.02f,
                lateralReachMul = 0.94f,
                shellThicknessMul = 1.05f,
                innerVolumeFocusY = 0f,
                thermalVeilEmphasisMul = 1f,
                paletteWarmthBias = 0f,
                paletteCoolSideBias = 0.04f,
                paletteCrownTintBias = 0f,
                paletteReserveTintBias = 0f,
            )
            LineageBranch.BALANCED -> balanced
        }

        /**
         * Affinity-weighted blend: `1 + strength * Σ affinity[b] * (rule[b] - 1)` for multipliers;
         * bias fields use weighted mean × strength.
         */
        fun blend(affinities: BranchAffinity, strength: Float): BranchVisualRule {
            if (strength < 1e-4f) return balanced
            fun shiftMul(getter: (BranchVisualRule) -> Float): Float {
                var s = 0f
                for (b in LineageBranch.entries) {
                    s += affinities[b] * (getter(forBranch(b)) - 1f)
                }
                return (1f + s * strength).coerceIn(0.82f, 1.28f)
            }
            fun shiftBias(getter: (BranchVisualRule) -> Float): Float {
                var s = 0f
                for (b in LineageBranch.entries) {
                    s += affinities[b] * getter(forBranch(b))
                }
                return (s * strength).coerceIn(-0.35f, 0.35f)
            }
            return BranchVisualRule(
                shellBandMul = shiftMul { it.shellBandMul },
                lateralFrondMul = shiftMul { it.lateralFrondMul },
                crownBloomMul = shiftMul { it.crownBloomMul },
                reserveBulbMul = shiftMul { it.reserveBulbMul },
                innerMassMul = shiftMul { it.innerMassMul },
                bracingMul = shiftMul { it.bracingMul },
                contourStretchXMul = shiftMul { it.contourStretchXMul },
                contourStretchYMul = shiftMul { it.contourStretchYMul },
                lateralReachMul = shiftMul { it.lateralReachMul },
                shellThicknessMul = shiftMul { it.shellThicknessMul },
                innerVolumeFocusY = shiftBias { it.innerVolumeFocusY }.coerceIn(-0.55f, 0.55f),
                thermalVeilEmphasisMul = shiftMul { it.thermalVeilEmphasisMul },
                paletteWarmthBias = shiftBias { it.paletteWarmthBias },
                paletteCoolSideBias = shiftBias { it.paletteCoolSideBias },
                paletteCrownTintBias = shiftBias { it.paletteCrownTintBias },
                paletteReserveTintBias = shiftBias { it.paletteReserveTintBias },
            )
        }
    }
}
