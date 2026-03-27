package com.velithorne.vessel.lineage

import com.velithorne.vessel.branching.BranchAffinity
import com.velithorne.vessel.branching.BranchingTuning
import com.velithorne.vessel.branching.LineageBranch
import com.velithorne.vessel.data.db.entity.SeedPodStateEntity
import com.velithorne.vessel.growth_seedpod.SeedPodDisplayState
import com.velithorne.vessel.growth_seedpod.SeedPodGrowthStage
import com.velithorne.vessel.growth_seedpod.SeedPodGrowthState
import com.velithorne.vessel.physiology.PhysiologySnapshot
import kotlin.math.abs

private const val CROWN_THRESH = 0.22f
private const val LAT_THRESH = 0.2f
private const val RES_THRESH = 0.22f

/**
 * Diff snapshots, emit growth events, stage transitions, adaptation updates, return-summary lines.
 */
object LineageEngine {

    data class PersistenceBatch(
        val stageTransition: StageTransitionRecord?,
        val growthEvents: List<GrowthEventRecord>,
        val adaptationUpserts: List<AdaptationUpsert>,
        val returnSummaryLines: List<String>,
        val newLastVisibleGrowthMs: Long,
        val newLastStageTransitionMs: Long,
        val newLastAdaptationUpdateMs: Long,
    )

    data class StageTransitionRecord(
        val from: SeedPodGrowthStage,
        val to: SeedPodGrowthStage,
        val channel: String,
        val explanation: String,
    )

    data class GrowthEventRecord(
        val type: GrowthEventType,
        val region: String,
        val magnitude: Float,
        val driver: String,
        val explanation: String,
        val offline: Boolean,
    )

    data class AdaptationUpsert(
        val kind: AdaptationKind,
        val deltaIntensity: Float,
        val label: String,
    )

    fun buildBatch(
        previousEntity: SeedPodStateEntity?,
        next: SeedPodGrowthState,
        physiology: PhysiologySnapshot,
        offlineCatchUp: Boolean,
        nowMs: Long,
    ): PersistenceBatch {
        val prevDisplay = previousEntity?.let { e ->
            SeedPodDisplayState(
                stage = SeedPodGrowthStage.entries.getOrNull(e.stageOrdinal) ?: SeedPodGrowthStage.DORMANT_POD,
                crownNub = e.crownNub,
                lateralBudLeft = e.lateralBudLeft,
                lateralBudRight = e.lateralBudRight,
                reserveBulb = e.reserveBulb,
                shellThickening = e.shellThickening,
                thermalVeil = e.thermalVeil,
                tissueHaze = e.tissueHaze,
                podCoherence = e.podCoherence,
                lastWallClockMs = e.lastWallClockMs,
            )
        }
        val prevStructuralStage = previousEntity?.let { e ->
            SeedPodGrowthStage.entries.getOrNull(e.stageOrdinal) ?: SeedPodGrowthStage.DORMANT_POD
        }
        val d = next.display
        val events = mutableListOf<GrowthEventRecord>()
        val adaptations = mutableListOf<AdaptationUpsert>()
        val lines = mutableListOf<String>()
        val s = physiology.species

        var lastVis = previousEntity?.lastVisibleGrowthMs ?: nowMs
        val prevLastStage = previousEntity?.lastStageTransitionMs ?: 0L
        val prevLastAdapt = previousEntity?.lastAdaptationUpdateMs ?: 0L
        var lastStage = prevLastStage
        var lastAdapt = prevLastAdapt

        // Irreversible structural stage transition (not live display flicker)
        var stageRec: StageTransitionRecord? = null
        val nextStructural = next.structural.permanentStage
        if (prevStructuralStage != null && prevStructuralStage != nextStructural) {
            val ch = dominantChannel(next, physiology)
            val expl = LineageExplainer.stageTransition(prevStructuralStage, nextStructural, ch)
            stageRec = StageTransitionRecord(prevStructuralStage, nextStructural, ch, expl)
            lastStage = nowMs
            lines += expl
        }

        val prevLeadOrd = previousEntity?.leadingBranchOrdinal
        val nextLeadOrd = next.structural.morphologyBranch.leadingBranchOrdinal
        if (previousEntity != null && prevLeadOrd != null &&
            prevLeadOrd != nextLeadOrd &&
            next.structural.permanentStage.ordinal >= com.velithorne.vessel.growth_seedpod.SeedPodGrowthStage.FIRST_BRANCH_FORMING.ordinal
        ) {
            val fromB = LineageBranch.entries.getOrNull(prevLeadOrd) ?: LineageBranch.BALANCED
            val toB = LineageBranch.entries.getOrNull(nextLeadOrd) ?: LineageBranch.BALANCED
            events += GrowthEventRecord(
                type = GrowthEventType.BRANCH_LEAD_CHANGED,
                region = "morphology_branch",
                magnitude = next.structural.morphologyBranch.affinities[toB],
                driver = "lineage_affinity",
                explanation = "Leading morphology tendency shifted from ${fromB.displayName} toward ${toB.displayName}.",
                offline = offlineCatchUp,
            )
            lines += "Morphology family lead updated toward ${toB.displayName}."
        }

        val bt = BranchingTuning()
        if (previousEntity != null) {
            val prevR = previousEntity.branchReadiness
            val nextR = next.structural.morphologyBranch.branchReadiness
            val matureEnough =
                next.structural.permanentStage.ordinal >= SeedPodGrowthStage.CHAMBER_MATURED.ordinal
            if (matureEnough && prevR < bt.branchReadinessEventThreshold && nextR >= bt.branchReadinessEventThreshold) {
                events += GrowthEventRecord(
                    type = GrowthEventType.BRANCH_READINESS_UNLOCKED,
                    region = "branch_readiness",
                    magnitude = nextR,
                    driver = "structural_maturity",
                    explanation = "Lineage morphology branching unlocked — affinities now accumulate toward a family.",
                    offline = offlineCatchUp,
                )
                lines += "Branch readiness unlocked — morphology families can diverge."
            }
            val prevAff = BranchAffinity.fromArray(
                floatArrayOf(
                    previousEntity.affinity0,
                    previousEntity.affinity1,
                    previousEntity.affinity2,
                    previousEntity.affinity3,
                    previousEntity.affinity4,
                    previousEntity.affinity5,
                    previousEntity.affinity6,
                ),
            )
            val nextAff = next.structural.morphologyBranch.affinities
            var bestBranch: LineageBranch? = null
            var bestDelta = 0f
            for (b in LineageBranch.entries) {
                if (b == LineageBranch.BALANCED) continue
                val d = nextAff[b] - prevAff[b]
                if (d > bestDelta && d >= bt.branchAffinityStrengthenDelta) {
                    bestDelta = d
                    bestBranch = b
                }
            }
            if (bestBranch != null &&
                next.structural.permanentStage.ordinal >= SeedPodGrowthStage.LINEAGE_DIFFERENTIATING.ordinal
            ) {
                events += GrowthEventRecord(
                    type = GrowthEventType.BRANCH_TENDENCY_STRENGTHENED,
                    region = "affinity",
                    magnitude = bestDelta,
                    driver = bestBranch.name,
                    explanation = "${bestBranch.displayName} tendency strengthened (${(bestDelta * 100f).toInt()}% shift).",
                    offline = offlineCatchUp,
                )
            }
        }

        // Threshold crossings (first time above threshold)
        if (prevDisplay != null) {
            if (prevDisplay.crownNub < CROWN_THRESH && d.crownNub >= CROWN_THRESH) {
                val driver = if (s.neuralActivity > 0.45f) "neural load" else "chamber activity"
                events += GrowthEventRecord(
                    GrowthEventType.CROWN_VISIBLE,
                    "crown",
                    d.crownNub,
                    driver,
                    LineageExplainer.crownVisible(driver),
                    offlineCatchUp,
                )
                lines += LineageExplainer.crownVisible(driver)
            }
            val prevLat = maxOf(prevDisplay.lateralBudLeft, prevDisplay.lateralBudRight)
            val nextLat = maxOf(d.lateralBudLeft, d.lateralBudRight)
            if (prevLat < LAT_THRESH && nextLat >= LAT_THRESH) {
                val driver = if (s.signalArousal > 0.5f) "signal pressure" else "connectivity"
                events += GrowthEventRecord(
                    GrowthEventType.LATERAL_EMERGED,
                    "lateral",
                    nextLat,
                    driver,
                    LineageExplainer.lateralExpanded(driver),
                    offlineCatchUp,
                )
                lines += LineageExplainer.lateralExpanded(driver)
            }
            if (prevDisplay.reserveBulb > RES_THRESH && d.reserveBulb <= RES_THRESH - 0.08f) {
                events += GrowthEventRecord(
                    GrowthEventType.RESERVE_CONTRACTED,
                    "reserve",
                    d.reserveBulb,
                    "hunger",
                    LineageExplainer.reserveContracted(),
                    offlineCatchUp,
                )
                lines += LineageExplainer.reserveContracted()
            }
            if (d.reserveBulb > prevDisplay.reserveBulb + 0.12f && d.reserveBulb > 0.35f) {
                events += GrowthEventRecord(
                    GrowthEventType.RESERVE_EXPANDED,
                    "reserve",
                    d.reserveBulb,
                    "reserve",
                    LineageExplainer.reserveExpanded(),
                    offlineCatchUp,
                )
                lines += LineageExplainer.reserveExpanded()
            }
            if (d.shellThickening > prevDisplay.shellThickening + 0.1f) {
                val thermal = s.fever > 0.35f
                events += GrowthEventRecord(
                    GrowthEventType.SHELL_THICKENED,
                    "shell",
                    d.shellThickening,
                    if (thermal) "thermal" else "load",
                    LineageExplainer.shellThickened(thermal),
                    offlineCatchUp,
                )
                lines += LineageExplainer.shellThickened(thermal)
            }
            if (d.tissueHaze > prevDisplay.tissueHaze + 0.12f && d.tissueHaze > 0.28f) {
                events += GrowthEventRecord(
                    GrowthEventType.CHAMBER_ENVELOPE,
                    "envelope",
                    d.tissueHaze,
                    "inactive growth",
                    LineageExplainer.chamberEnvelope(),
                    offlineCatchUp,
                )
                if (offlineCatchUp) lines += LineageExplainer.chamberEnvelope()
            }
            if (d.podCoherence > prevDisplay.podCoherence + 0.1f && s.recovery > 0.4f) {
                events += GrowthEventRecord(
                    GrowthEventType.COHERENCE_IMPROVED,
                    "shell",
                    d.podCoherence,
                    "recovery",
                    LineageExplainer.coherenceImproved(),
                    offlineCatchUp,
                )
                lines += LineageExplainer.coherenceImproved()
            }
        }

        // Adaptation markers (EMA-style accumulation)
        fun bump(kind: AdaptationKind, raw: Float, label: String) {
            if (raw < 0.12f) return
            adaptations += AdaptationUpsert(kind, raw * 0.02f, label)
        }
        bump(AdaptationKind.THERMAL, s.fever, LineageExplainer.adaptationLabel(AdaptationKind.THERMAL))
        bump(AdaptationKind.SIGNAL, s.signalArousal, LineageExplainer.adaptationLabel(AdaptationKind.SIGNAL))
        bump(AdaptationKind.NEURAL, s.neuralActivity, LineageExplainer.adaptationLabel(AdaptationKind.NEURAL))
        bump(AdaptationKind.RECOVERY, s.recovery, LineageExplainer.adaptationLabel(AdaptationKind.RECOVERY))
        bump(AdaptationKind.RESERVE, s.hunger, LineageExplainer.adaptationLabel(AdaptationKind.RESERVE))
        bump(AdaptationKind.ARCHIVE, s.structuralLoad, LineageExplainer.adaptationLabel(AdaptationKind.ARCHIVE))
        if (adaptations.isNotEmpty()) lastAdapt = nowMs

        // Visible growth timestamp
        if (prevDisplay == null || significantDelta(prevDisplay, d)) {
            lastVis = nowMs
        }

        return PersistenceBatch(
            stageTransition = stageRec,
            growthEvents = events,
            adaptationUpserts = adaptations,
            returnSummaryLines = lines.distinct().take(5),
            newLastVisibleGrowthMs = lastVis,
            newLastStageTransitionMs = lastStage,
            newLastAdaptationUpdateMs = lastAdapt,
        )
    }

    private fun significantDelta(a: SeedPodDisplayState, b: SeedPodDisplayState): Boolean =
        abs(a.crownNub - b.crownNub) > 0.04f ||
            abs(a.lateralBudLeft - b.lateralBudLeft) > 0.04f ||
            abs(a.reserveBulb - b.reserveBulb) > 0.05f ||
            abs(a.shellThickening - b.shellThickening) > 0.05f

    private fun dominantChannel(state: SeedPodGrowthState, phys: PhysiologySnapshot): String {
        val b = state.budget
        val max = listOf(
            "crown" to b.crown,
            "lateral" to b.lateral,
            "reserve" to b.reserve,
            "shell" to b.shell,
            "thermal" to b.thermal,
            "coherence" to b.coherence,
        ).maxByOrNull { it.second }?.first ?: "coherence"
        return max
    }
}
