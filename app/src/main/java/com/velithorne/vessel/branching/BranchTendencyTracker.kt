package com.velithorne.vessel.branching

import com.velithorne.vessel.progression.StructuralGrowthState

/**
 * Read-only view of persisted lineage tendency + morphology branch (for UI/debug).
 */
object BranchTendencyTracker {

    fun morphologySummary(structural: StructuralGrowthState): String {
        val b = structural.morphologyBranch
        val lead = b.leadingBranch()
        return "${lead.displayName} · readiness ${(b.branchReadiness * 100f).toInt()}% · commit ${b.commitmentLevel}"
    }
}
