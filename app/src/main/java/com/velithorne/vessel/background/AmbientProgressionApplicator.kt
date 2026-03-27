package com.velithorne.vessel.background

import com.velithorne.vessel.branching.BranchAffinity
import com.velithorne.vessel.branching.LineageBranch
import com.velithorne.vessel.growth_seedpod.SeedPodGrowthStage
import com.velithorne.vessel.growth_seedpod.SeedPodGrowthState
import com.velithorne.vessel.lineage.AdaptationKind

/**
 * Applies accumulated ambient ecology into **structural** state: growth budgets, stage readiness nudges,
 * branch affinities, and adaptation marker deltas — bounded, monotonic where required.
 *
 * Does not run physiology or redraw; used only during reopen / cold-start folding.
 */
object AmbientProgressionApplicator {

    data class AdaptationNudges(
        val thermal: Float,
        val signal: Float,
        val neural: Float,
        val recovery: Float,
        val reserve: Float,
        val archive: Float,
    )

    data class AppliedAmbient(
        val state: SeedPodGrowthState,
        val adaptationNudges: AdaptationNudges,
        val structuralReadinessDelta: Float,
        val affinityDeltas: FloatArray,
    )

    fun apply(
        base: SeedPodGrowthState,
        acc: AmbientGrowthAccumulator.Result,
        tuning: BackgroundTuning,
    ): AppliedAmbient {
        val mergedBudget = AmbientReopenProcessor.mergeBudget(base, acc.budgetDelta).budget
        var s = base.copy(budget = mergedBudget)
        val n = acc.snapshotCount.coerceAtLeast(1)
        val scale = (n / 20f).coerceIn(0.35f, 1.4f)

        // Post-mature structural accumulation nudge (same stages as [DevelopmentEngine] post-chamber)
        var readinessDelta = 0f
        val st = s.structural.permanentStage
        if (st.ordinal >= SeedPodGrowthStage.CHAMBER_MATURED.ordinal &&
            st.ordinal < SeedPodGrowthStage.SPECIALIZATION_ESTABLISHED.ordinal
        ) {
            val raw = acc.ambientStructuralReadinessNudge * tuning.ambientStructuralReadinessScale * scale
            readinessDelta = raw.coerceIn(0f, tuning.maxAmbientStructuralReadinessPerApply)
            val nextAcc = (s.structural.nextStageAccum + readinessDelta).coerceIn(0f, 1f)
            s = s.copy(structural = s.structural.copy(nextStageAccum = nextAcc))
        }

        // Branch affinity nudge toward dominant ambient family (normalized)
        val affDeltas = FloatArray(LineageBranch.entries.size) { 0f }
        val target = dominantBranchFromCounts(acc)
        if (target != LineageBranch.BALANCED) {
            val amt = (acc.ambientAffinityNudge * tuning.ambientAffinityNudgeScale * scale)
                .coerceIn(0f, tuning.maxAmbientAffinityNudgePerApply)
            val aff = s.structural.morphologyBranch.affinities
            val arr = aff.asArray().copyOf()
            val ti = target.ordinal
            arr[ti] = (arr[ti] + amt).coerceIn(0f, 1f)
            // Rebalance others slightly so sum stays ~1 after normalization
            val others = LineageBranch.entries.filter { it != target && it != LineageBranch.BALANCED }
            val shave = amt / others.size.coerceAtLeast(1)
            for (b in others) {
                val j = b.ordinal
                arr[j] = (arr[j] - shave).coerceIn(0.01f, 1f)
            }
            affDeltas[ti] = amt
            val newAff = BranchAffinity.fromArray(arr)
            s = s.copy(
                structural = s.structural.copy(
                    morphologyBranch = s.structural.morphologyBranch.copy(affinities = newAff),
                ),
            )
        }

        // Adaptation marker nudges from ecology pattern counts (persisted separately)
        val adap = AdaptationNudges(
            thermal = (acc.warmThermalSamples * 0.004f * scale).coerceIn(0f, tuning.maxAmbientAdaptDelta),
            signal = (acc.cellularSamples * 0.0035f * scale).coerceIn(0f, tuning.maxAmbientAdaptDelta),
            neural = (acc.idleStableSamples * 0.0025f * scale).coerceIn(0f, tuning.maxAmbientAdaptDelta),
            recovery = (acc.chargingSamples * 0.003f * scale).coerceIn(0f, tuning.maxAmbientAdaptDelta),
            reserve = (acc.reserveStressSamples * 0.004f * scale).coerceIn(0f, tuning.maxAmbientAdaptDelta),
            archive = (acc.archiveHeavySamples * 0.0035f * scale).coerceIn(0f, tuning.maxAmbientAdaptDelta),
        )

        return AppliedAmbient(
            state = s,
            adaptationNudges = adap,
            structuralReadinessDelta = readinessDelta,
            affinityDeltas = affDeltas,
        )
    }

    /** Maps ambient pattern counts to a target branch for soft affinity pull. */
    fun dominantBranchFromCounts(acc: AmbientGrowthAccumulator.Result): LineageBranch = when (acc.dominantDriver) {
        "signal_mobile" -> LineageBranch.SIGNAL_FROND
        "thermal" -> LineageBranch.THERMAL_SHELL
        "charging_recovery", "idle_coherence" -> LineageBranch.RESERVE_BASIN
        "reserve_stress" -> LineageBranch.RESERVE_BASIN
        "archive" -> LineageBranch.ARCHIVE_CORE
        "motion" -> LineageBranch.MOTION_BRACED
        else -> LineageBranch.BALANCED
    }
}

fun AdaptationKind.toLabel(): String = when (this) {
    AdaptationKind.THERMAL -> "thermal habit"
    AdaptationKind.SIGNAL -> "signal habit"
    AdaptationKind.NEURAL -> "coherence habit"
    AdaptationKind.RECOVERY -> "recovery habit"
    AdaptationKind.RESERVE -> "reserve habit"
    AdaptationKind.ARCHIVE -> "archive habit"
}
