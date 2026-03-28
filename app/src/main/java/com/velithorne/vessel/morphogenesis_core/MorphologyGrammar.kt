package com.velithorne.vessel.morphogenesis_core

/**
 * Species grammar — keeps forms recognizable; biases topology by pressure + archetype.
 */
object MorphologyGrammar {

    fun biasNodeY(kind: MorphNodeKind, pressure: GrowthPressureState): Float {
        return when (kind) {
            MorphNodeKind.RESERVE_BASIN_LOCUS -> 0.72f + pressure.reserve * 0.06f
            MorphNodeKind.CROWN_CHAMBER -> 0.18f - pressure.signal * 0.04f
            MorphNodeKind.ARCHIVE_PLATE_SEED -> 0.55f + pressure.archive * 0.08f
            MorphNodeKind.FROND_ROOT -> 0.48f
            MorphNodeKind.HEAT_MANTLE_RIDGE -> 0.35f + pressure.thermal * 0.1f
            MorphNodeKind.BRACE_ANCHOR -> 0.5f
            MorphNodeKind.CORE_KNOT -> 0.45f
            MorphNodeKind.TRANSIENT_GROWTH_CENTER -> 0.42f
            MorphNodeKind.SCAR_ANCHOR -> 0.5f
            MorphNodeKind.REROUTE_JUNCTION -> 0.48f
        }
    }

    fun biasNodeX(kind: MorphNodeKind, pressure: GrowthPressureState, asym: Float): Float {
        val lateral = pressure.signal * 0.08f + pressure.motion * 0.06f
        val jitter = when (kind) {
            MorphNodeKind.FROND_ROOT -> 0.12f * asym
            else -> 0.04f * asym
        }
        return 0.5f + lateral * (if (kind == MorphNodeKind.FROND_ROOT) 1f else 0.3f) + jitter
    }
}
