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

    /** One line when visible morphology reinforcement crosses a delta (lineage history). */
    fun visibleReinforceLine(lead: LineageBranch, expressionMag: Float): String {
        val pct = (expressionMag * 100f).toInt().coerceIn(0, 100)
        return when (lead) {
            LineageBranch.THERMAL_SHELL -> "Perimeter shell bands and thermal veil are visibly strengthening ($pct% expression)."
            LineageBranch.SIGNAL_FROND -> "Lateral fronds and conductive sheen are visibly extending ($pct% expression)."
            LineageBranch.CROWN_NEURAL -> "Upper crown bloom and neural chamber lift are visibly deepening ($pct% expression)."
            LineageBranch.RESERVE_BASIN -> "Lower reserve chamber mass is visibly swelling ($pct% expression)."
            LineageBranch.ARCHIVE_CORE -> "Inner chamber density and stratified haze are visibly compacting ($pct% expression)."
            LineageBranch.MOTION_BRACED -> "Structural bracing and taut silhouette are visibly tightening ($pct% expression)."
            LineageBranch.BALANCED -> "Balanced morphology cues are gently clarifying ($pct% expression)."
        }
    }

    /** Short bullet for Lineage “visible traits forming” summary. */
    fun visibleTraitsSummary(lead: LineageBranch): String = when (lead) {
        LineageBranch.THERMAL_SHELL -> "thicker shell rings, warmer perimeter veil"
        LineageBranch.SIGNAL_FROND -> "extended lateral fronds, conductive side sheen"
        LineageBranch.CROWN_NEURAL -> "lifted crown bloom, brighter upper chamber"
        LineageBranch.RESERVE_BASIN -> "deeper lower reserve bulb, reservoir shading"
        LineageBranch.ARCHIVE_CORE -> "denser inner haze, compact core mass"
        LineageBranch.MOTION_BRACED -> "tension bracing lines, stabilized outline"
        LineageBranch.BALANCED -> "subtle multi-channel hints, smooth silhouette"
    }

    /** Short deterministic lines matching product copy (telemetry + history weighted). */
    fun vignetteLine(lead: LineageBranch, ecology: UsageEcologyProfile): String =
        when (lead) {
            LineageBranch.SIGNAL_FROND ->
                "Sustained mobile connectivity is driving lateral signal specialization."
            LineageBranch.THERMAL_SHELL ->
                "Repeated thermal strain is thickening shell-oriented development."
            LineageBranch.CROWN_NEURAL ->
                "High neural load is favoring crown-dominant differentiation."
            LineageBranch.RESERVE_BASIN ->
                "Reserve deficit history is biasing lower-basin endurance growth."
            LineageBranch.ARCHIVE_CORE ->
                "Archive burden is densifying the inner chamber core."
            LineageBranch.MOTION_BRACED ->
                "Active handling rhythm is bracing the pod for stability."
            LineageBranch.BALANCED ->
                if (ecology.erraticStressRhythm > 0.45f) {
                    "Mixed stress rhythm keeps morphology moderate across families."
                } else {
                    "Balanced ecology — no single pathway dominates yet."
                }
        }
}
