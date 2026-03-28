package com.velithorne.vessel.renderer

import com.velithorne.vessel.morphogenesis.GerminationStage

/**
 * Explicit canvas presentation mode — decouples UI copy from what we actually draw.
 * Early modes must not show the legacy full-body scaffold.
 */
enum class VesselStageRenderMode {
    SEED_ONLY,
    GERMINATING,
    EARLY_BRANCHING,
    MID_FORMATION,
    ADVANCED_FORMATION,
    ;

    val suppressesLegacyBody: Boolean
        get() = when (this) {
            SEED_ONLY, GERMINATING, EARLY_BRANCHING -> true
            MID_FORMATION, ADVANCED_FORMATION -> false
        }

    val seedDominant: Boolean
        get() = when (this) {
            SEED_ONLY, GERMINATING, EARLY_BRANCHING -> true
            else -> false
        }

    companion object {

        /**
         * Maps classifier stage + lagging silhouette blend to a render mode.
         * [seedFormBlend] 1 = vesica/seed shape, 0 = adult spindle — lower values unlock body integration.
         */
        fun fromGermination(
            stage: GerminationStage,
            seedFormBlend: Float,
        ): VesselStageRenderMode {
            val blend = seedFormBlend.coerceIn(0f, 1f)
            return when (stage) {
                GerminationStage.DORMANT_SEED,
                GerminationStage.ACTIVATED_SEED,
                -> SEED_ONLY
                GerminationStage.GERMINATING ->
                    when {
                        blend >= 0.78f -> GERMINATING
                        blend >= 0.58f -> EARLY_BRANCHING
                        else -> MID_FORMATION
                    }
                GerminationStage.CHAMBER_FORMATION ->
                    when {
                        blend >= 0.52f -> EARLY_BRANCHING
                        blend >= 0.28f -> MID_FORMATION
                        else -> ADVANCED_FORMATION
                    }
                GerminationStage.BRANCHING ->
                    when {
                        blend >= 0.42f -> EARLY_BRANCHING
                        blend >= 0.22f -> MID_FORMATION
                        else -> ADVANCED_FORMATION
                    }
                GerminationStage.RESERVOIR_DEEPENING,
                GerminationStage.SHELL_THICKENING,
                -> when {
                    blend >= 0.18f -> MID_FORMATION
                    else -> ADVANCED_FORMATION
                }
                GerminationStage.STABILIZING -> ADVANCED_FORMATION
            }
        }
    }
}
