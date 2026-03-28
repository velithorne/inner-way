package com.velithorne.vessel.renderer_seedpod

import com.velithorne.vessel.config.SimulationMode
import com.velithorne.vessel.morphogenesis_core.CanonicalLifeEra

/**
 * Seed origin remains as inner relic — not a second outer body. Values multiply renderer inputs.
 */
data class SeedBurialState(
    /** Multiplier on [SeedTraceState.traceAlpha] — low when buried. */
    val traceAlphaMul: Float,
    /** Scales competing authored outer shell (vesica) — bury = shrink ghost. */
    val outerShellGhostScale: Float,
    /** 0..1 — extra fade on rear shell ovals so they don't read as second body. */
    val outerShellGhostAlphaMul: Float,
    /** Pixels — trace drawn slightly deeper (behind generated mass). */
    val burialDepthPx: Float,
    /** Knot scale — fossil-like when buried. */
    val relicKnotScaleMul: Float,
    /** Summary for UI when intensity sufficient. */
    val burialSummaryLine: String?,
)

object SeedBurialMapper {

    fun map(
        era: CanonicalLifeEra,
        generatedTopologyInfluence: Float,
        mode: SimulationMode,
    ): SeedBurialState {
        val g = generatedTopologyInfluence.coerceIn(0f, 1f)
        val dev = if (mode == SimulationMode.DEV_SIMULATION) 0.08f else 0f
        val eraBurial = when (era) {
            CanonicalLifeEra.SEED -> 0f
            CanonicalLifeEra.VEIL_STAGE -> 0.15f
            CanonicalLifeEra.CORE_ESTABLISHMENT -> 0.45f
            CanonicalLifeEra.BRANCHING_THRESHOLD -> 0.72f
            CanonicalLifeEra.ADULTHOOD -> 0.9f
        }
        val burial = (eraBurial + g * 0.35f + dev).coerceIn(0f, 1f)

        val traceAlphaMul = (1f - burial * 0.92f).coerceIn(0.08f, 1f)
        val outerGhost = (1f - burial * 0.55f).coerceIn(0.42f, 1f)
        val outerAlpha = (1f - burial * 0.75f).coerceIn(0.18f, 1f)
        val depthPx = burial * 5.5f
        val relicScale = (0.55f + (1f - burial) * 0.35f).coerceIn(0.38f, 0.92f)

        val summary = when {
            burial < 0.2f -> null
            burial < 0.5f -> "Seed trace softening — generated form emerging"
            burial < 0.75f -> "Original pod seam buried under self-assembled shell"
            else -> "Seed origin as inner relic — generated shell dominant"
        }

        return SeedBurialState(
            traceAlphaMul = traceAlphaMul,
            outerShellGhostScale = outerGhost,
            outerShellGhostAlphaMul = outerAlpha,
            burialDepthPx = depthPx,
            relicKnotScaleMul = relicScale,
            burialSummaryLine = summary,
        )
    }
}
