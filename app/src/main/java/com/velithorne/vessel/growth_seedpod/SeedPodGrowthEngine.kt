package com.velithorne.vessel.growth_seedpod

import com.velithorne.vessel.physiology.PhysiologySnapshot
import com.velithorne.vessel.progression.StructuralGrowthState
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
        tuning: SeedPodGrowthTuning = SeedPodGrowthTuning.default(),
    ): SeedPodGrowthState {
        val s = phys.species
        val b0 = prev.budget
        val gain = BUDGET_GAIN * tuning.budgetGainMultiplier
        val decay = BUDGET_DECAY * tuning.budgetDecayMultiplier
        fun add(base: Float, pressure: Float) =
            (base + pressure * gain * dtSec - decay * base * dtSec).coerceIn(0f, 1f)

        val budget = SeedPodGrowthBudget(
            crown = add(b0.crown, s.neuralActivity),
            lateral = add(b0.lateral, s.signalArousal * 0.85f + s.respiration * 0.15f),
            reserve = add(b0.reserve, s.hunger * 0.6f + (1f - s.vitality) * 0.25f),
            shell = add(b0.shell, s.structuralLoad * 0.5f + s.stress * 0.35f),
            thermal = add(b0.thermal, s.fever * 0.7f + s.stress * 0.2f),
            coherence = add(b0.coherence, s.recovery * 0.65f + (1f - s.stress) * 0.2f),
        )

        val d0 = prev.display
        val lerp = LERP * tuning.displayLerpMultiplier
        fun toward(current: Float, target: Float): Float =
            current + (target - current) * (lerp * max(0.5f, dtSec * 60f / 16f)).coerceIn(0.04f, 0.45f)

        val crownT = (budget.crown * 0.55f + s.neuralActivity * 0.35f).coerceIn(0f, 1f)
        val latT = (budget.lateral * 0.5f + s.signalArousal * 0.4f).coerceIn(0f, 1f)
        val resT = (budget.reserve * 0.45f + s.hunger * 0.45f).coerceIn(0f, 1f)
        val shellT = (budget.shell * 0.4f + s.structuralLoad * 0.35f).coerceIn(0f, 1f)
        val thermT = (budget.thermal * 0.5f + s.fever * 0.4f).coerceIn(0f, 1f)
        val hazeT = ((budget.crown + budget.lateral) * 0.35f + s.recovery * 0.25f).coerceIn(0f, 0.85f)
        val cohT = (budget.coherence * 0.55f + s.recovery * 0.35f).coerceIn(0f, 1f)

        val display = d0.copy(
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
        // Permanent stage updated in [SeedPodGrowthCoordinator] via [com.velithorne.vessel.progression.DevelopmentEngine].
        return SeedPodGrowthState(
            display = display.copy(stage = prev.display.stage),
            budget = budget,
            structural = prev.structural,
        )
    }

    /** 0..1 — how fully the pod has refined after reaching the chambering phase. */
    fun maturityScore(d: SeedPodDisplayState, tuning: SeedPodGrowthTuning = SeedPodGrowthTuning.default()): Float {
        val lat = (d.lateralBudLeft + d.lateralBudRight) * 0.5f
        val m = (
            d.crownNub * 0.12f + lat * 0.12f + d.reserveBulb * 0.1f +
                d.tissueHaze * 0.2f + d.podCoherence * 0.18f +
                d.shellThickening * 0.16f + d.thermalVeil * 0.12f
            ) * tuning.maturityScoreMultiplier
        return m.coerceIn(0f, 1f)
    }

    fun initialDisplay(): SeedPodDisplayState = SeedPodDisplayState(
        stage = SeedPodGrowthStage.DORMANT_POD,
        // Keep activity = crown + lateralL + lateralR + reserve + haze < 0.18 so first frame stays DORMANT.
        crownNub = 0.03f,
        lateralBudLeft = 0.03f,
        lateralBudRight = 0.03f,
        reserveBulb = 0.04f,
        shellThickening = 0.14f,
        thermalVeil = 0.06f,
        tissueHaze = 0.03f,
        podCoherence = 0.5f,
        lastWallClockMs = System.currentTimeMillis(),
    )

}
