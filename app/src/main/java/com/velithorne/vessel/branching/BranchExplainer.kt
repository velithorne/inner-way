package com.velithorne.vessel.branching

import com.velithorne.vessel.growth_seedpod.SeedPodGrowthStage

/**
 * Deterministic copy for Vessel / Lineage — why this specimen leans a given way.
 */
object BranchExplainer {

    fun leadingLine(branch: LineageBranch, readiness: Float, stage: SeedPodGrowthStage): String {
        if (stage.ordinal < SeedPodGrowthStage.LINEAGE_DIFFERENTIATING.ordinal) {
            return ""
        }
        val label = branch.displayName
        val r = (readiness * 100f).toInt().coerceIn(0, 100)
        return when {
            stage.ordinal >= SeedPodGrowthStage.SPECIALIZATION_EMERGING.ordinal ->
                "Emerging specialization: $label ($r% lineage clarity)"
            stage.ordinal >= SeedPodGrowthStage.FIRST_BRANCH_FORMING.ordinal ->
                "Lineage leaning: $label — first branch form expressing"
            else ->
                "Differentiation bias: $label ($r% readiness)"
        }
    }

    fun secondaryLine(affinities: BranchAffinity, lead: LineageBranch): String? {
        val pairs = LineageBranch.entries.filter { it != LineageBranch.BALANCED && it != lead }
            .map { it to affinities[it] }
            .sortedByDescending { it.second }
        val second = pairs.firstOrNull() ?: return null
        if (second.second < 0.18f) return null
        return "Secondary tendency: ${second.first.displayName}"
    }

    fun reasonLine(device: DeviceProfile, ecology: UsageEcologyProfile, lead: LineageBranch): String {
        return when (lead) {
            LineageBranch.THERMAL_SHELL ->
                if (ecology.thermalStrainHistory > 0.45f || device.lowThermalHeadroom > 0.45f) {
                    "Repeated thermal strain and limited headroom favor a protective shell morphology."
                } else {
                    "Thermal ecology and charging rhythm are biasing perimeter shell development."
                }
            LineageBranch.SIGNAL_FROND ->
                if (ecology.mobileDataHeavy > 0.5f || ecology.signalPressureHistory > 0.45f) {
                    "Sustained mobile connectivity and signal pressure are extending lateral fronds."
                } else {
                    "Network ecology is steering conductive lateral growth."
                }
            LineageBranch.CROWN_NEURAL ->
                if (ecology.neuralLoadHistory > 0.45f || device.highSensorRichness > 0.45f) {
                    "High neural load and rich sensing favor crown-dominant differentiation."
                } else {
                    "Cognitive-load ecology is lifting upper-chamber complexity."
                }
            LineageBranch.RESERVE_BASIN ->
                if (ecology.reserveStressHistory > 0.45f) {
                    "Reserve deficit history is deepening the lower endurance basin."
                } else {
                    "Energy rhythm is weighting the lower reservoir chamber."
                }
            LineageBranch.ARCHIVE_CORE ->
                if (ecology.archiveBurdenHistory > 0.45f || device.storageDenseProfile > 0.45f) {
                    "Archive burden and dense storage are compacting the inner chamber mass."
                } else {
                    "Structural archive pressure is densifying internal geometry."
                }
            LineageBranch.MOTION_BRACED ->
                if (device.highMotionLife > 0.45f) {
                    "Active motion ecology is bracing the silhouette for stabilization."
                } else {
                    "Handling patterns favor tension-braced form."
                }
            LineageBranch.BALANCED ->
                "No single pressure dominates — the specimen remains balanced across pathways."
        }
    }
}
