package com.velithorne.vessel.growth_seedpod

import com.velithorne.vessel.physiology.PhysiologySnapshot
import kotlin.math.max

/**
 * Maps physiology → local pod growth only (no legacy morphogenesis body).
 */
object SeedPodGrowthEngine {

    private const val BUDGET_GAIN = 0.018f
    private const val BUDGET_DECAY = 0.004f
    private const val LERP = 0.12f

    fun step(
        phys: PhysiologySnapshot,
        prev: SeedPodGrowthState,
        dtSec: Float,
    ): SeedPodGrowthState {
        val s = phys.species
        val b0 = prev.budget
        fun add(base: Float, pressure: Float) =
            (base + pressure * BUDGET_GAIN * dtSec - BUDGET_DECAY * base * dtSec).coerceIn(0f, 1f)

        val budget = SeedPodGrowthBudget(
            crown = add(b0.crown, s.neuralActivity),
            lateral = add(b0.lateral, s.signalArousal * 0.85f + s.respiration * 0.15f),
            reserve = add(b0.reserve, s.hunger * 0.6f + (1f - s.vitality) * 0.25f),
            shell = add(b0.shell, s.structuralLoad * 0.5f + s.stress * 0.35f),
            thermal = add(b0.thermal, s.fever * 0.7f + s.stress * 0.2f),
            coherence = add(b0.coherence, s.recovery * 0.65f + (1f - s.stress) * 0.2f),
        )

        val d0 = prev.display
        fun toward(current: Float, target: Float): Float =
            current + (target - current) * (LERP * max(0.5f, dtSec * 60f / 16f)).coerceIn(0.04f, 0.35f)

        val crownT = (budget.crown * 0.55f + s.neuralActivity * 0.35f).coerceIn(0f, 1f)
        val latT = (budget.lateral * 0.5f + s.signalArousal * 0.4f).coerceIn(0f, 1f)
        val resT = (budget.reserve * 0.45f + s.hunger * 0.45f).coerceIn(0f, 1f)
        val shellT = (budget.shell * 0.4f + s.structuralLoad * 0.35f).coerceIn(0f, 1f)
        val thermT = (budget.thermal * 0.5f + s.fever * 0.4f).coerceIn(0f, 1f)
        val hazeT = ((budget.crown + budget.lateral) * 0.35f + s.recovery * 0.25f).coerceIn(0f, 0.85f)
        val cohT = (budget.coherence * 0.55f + s.recovery * 0.35f).coerceIn(0f, 1f)

        var display = d0.copy(
            crownNub = toward(d0.crownNub, crownT),
            lateralBudLeft = toward(d0.lateralBudLeft, latT),
            lateralBudRight = toward(d0.lateralBudRight, latT * 0.95f),
            reserveBulb = toward(d0.reserveBulb, resT),
            shellThickening = toward(d0.shellThickening, shellT),
            thermalVeil = toward(d0.thermalVeil, thermT),
            tissueHaze = toward(d0.tissueHaze, hazeT),
            podCoherence = toward(d0.podCoherence, cohT),
            lastWallClockMs = System.currentTimeMillis(),
        )

        display = display.copy(stage = classifyStage(display))

        return SeedPodGrowthState(display = display, budget = budget)
    }

    fun initialDisplay(): SeedPodDisplayState = SeedPodDisplayState(
        stage = SeedPodGrowthStage.DORMANT_POD,
        crownNub = 0.04f,
        lateralBudLeft = 0.03f,
        lateralBudRight = 0.03f,
        reserveBulb = 0.06f,
        shellThickening = 0.12f,
        thermalVeil = 0.08f,
        tissueHaze = 0.05f,
        podCoherence = 0.55f,
        lastWallClockMs = System.currentTimeMillis(),
    )

    private fun classifyStage(d: SeedPodDisplayState): SeedPodGrowthStage {
        val activity = d.crownNub + d.lateralBudLeft + d.reserveBulb + d.tissueHaze
        return when {
            activity < 0.18f && d.shellThickening < 0.22f -> SeedPodGrowthStage.DORMANT_POD
            activity < 0.35f -> SeedPodGrowthStage.ACTIVATING_POD
            activity < 0.55f -> SeedPodGrowthStage.GERMINATING_POD
            d.tissueHaze < 0.42f -> SeedPodGrowthStage.EARLY_BUDDING
            else -> SeedPodGrowthStage.EARLY_CHAMBERING
        }
    }
}
