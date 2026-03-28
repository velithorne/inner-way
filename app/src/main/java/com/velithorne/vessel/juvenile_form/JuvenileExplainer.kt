package com.velithorne.vessel.juvenile_form

import com.velithorne.vessel.branching.LineageBranch

object JuvenileExplainer {

    fun headline(form: JuvenileFormState): String {
        if (!form.active) return ""
        val lead = when (form.bodyPlan.dominantRegion) {
            JuvenileRegion.CROWN_REGION -> "Crown-dominant"
            JuvenileRegion.LATERAL_REGION_LEFT,
            JuvenileRegion.LATERAL_REGION_RIGHT,
            -> "Lateral-dominant"
            JuvenileRegion.RESERVE_REGION -> "Reserve-basin–weighted"
            JuvenileRegion.SHELL_REGION -> "Shell-armored"
            JuvenileRegion.SUPPORT_REGION -> "Support-braced"
            JuvenileRegion.ARCHIVE_REGION -> "Archive-dense"
            else -> "Core-balanced"
        }
        return "Juvenile form emerging: $lead juvenile architecture"
    }

    fun regionLine(form: JuvenileFormState): String {
        if (!form.active) return ""
        val p = form.bodyPlan
        val parts = mutableListOf<String>()
        if (p.crownMass > 0.55f) parts += "elevated crown region"
        if (p.lateralMass > 0.55f) parts += "widened lateral chambers"
        if (p.reserveMass > 0.55f) parts += "deepened reserve basin"
        if (p.shellMass > 0.55f) parts += "reinforced shell bands"
        if (p.supportMass > 0.55f) parts += "visible brace geometry"
        if (p.archiveMass > 0.55f) parts += "compact core massing"
        if (parts.isEmpty()) parts += "integrated moderate regions"
        return "Visible body architecture: ${parts.joinToString(", ")}"
    }

    fun transitionLine(form: JuvenileFormState): String {
        if (!form.active) return ""
        return when {
            form.transition.topologyExpandedBeyondSeed && form.transition.seedTraceOnlyLineageMemory ->
                "Topology expanded beyond seed phase — origin held as lineage trace only"
            form.transition.topologyExpandedBeyondSeed ->
                "First juvenile body plan — regions structurally differentiated"
            else ->
                "Juvenile architecture forming — seed trace still partially visible"
        }
    }

    fun branchFamilyLine(lead: LineageBranch): String =
        when (lead) {
            LineageBranch.THERMAL_SHELL -> "Thermal shell family: perimeter plates, protected core"
            LineageBranch.SIGNAL_FROND -> "Signal frond family: lateral routing, open posture"
            LineageBranch.CROWN_NEURAL -> "Crown neural family: upper dominance, layered crown"
            LineageBranch.RESERVE_BASIN -> "Reserve basin family: weighted lower body, deep basin"
            LineageBranch.ARCHIVE_CORE -> "Archive core family: dense inward architecture"
            LineageBranch.MOTION_BRACED -> "Motion-braced family: tension stabilizers, posture bias"
            LineageBranch.BALANCED -> "Balanced family: even region integration"
        }
}
