package com.velithorne.vessel.model

import com.velithorne.vessel.physiology.OrganState
import com.velithorne.vessel.physiology.OrganType

/** Live panel row data for the anatomy sheet. */
data class OrganInspectionState(
    val info: OrganInfo,
    val organState: OrganState?,
    val conditionSummary: String,
    val reactionNote: String,
) {
    companion object {
        fun build(organ: OrganType, organs: List<com.velithorne.vessel.physiology.OrganState>): OrganInspectionState {
            val info = OrganInfo.forType(organ)
            val o = organs.firstOrNull { it.organType == organ }
            return OrganInspectionState(
                info = info,
                organState = o,
                conditionSummary = deterministicCondition(o),
                reactionNote = deterministicReaction(organ, o),
            )
        }

        private fun deterministicCondition(o: OrganState?): String {
            if (o == null) return "Readout degraded — organ projection unavailable."
            val parts = mutableListOf<String>()
            when {
                o.health > 0.65f -> parts += "tissue coherence high"
                o.health > 0.4f -> parts += "homeostatic drift moderate"
                else -> parts += "strain visible in tissue envelope"
            }
            when {
                o.load > 0.65f -> parts += "load elevated"
                o.load > 0.35f -> parts += "moderate demand"
                else -> parts += "load tractable"
            }
            when {
                o.inflammation > 0.55f -> parts += "inflammatory halo pronounced"
                o.inflammation > 0.25f -> parts += "mild inflammatory shimmer"
            }
            return "Current state: " + parts.joinToString(", ") + "."
        }

        private fun deterministicReaction(organ: OrganType, o: OrganState?): String {
            if (o == null) return "Reacting to: host telemetry gaps."
            return when (organ) {
                OrganType.METABOLIC_HEART -> when {
                    o.reserve < 0.35f && o.load > 0.5f ->
                        "Reacting to: low reserve under rising metabolic pull."

                    o.activity > 0.65f ->
                        "Reacting to: active regeneration / charge coupling."

                    else ->
                        "Reacting to: battery envelope, charging state, vitality exchange."
                }
                OrganType.CORTEX_CLUSTER -> when {
                    o.load > 0.6f ->
                        "Reacting to: dense sensory traffic and stress coupling."

                    else ->
                        "Reacting to: interactive surface load and neural throughput proxy."
                }
                OrganType.NEURAL_GEL -> when {
                    o.load > 0.55f ->
                        "Reacting to: squeezed plasticity—memory pressure dominating."

                    else ->
                        "Reacting to: buffer depth vs. retention demand."
                }
                OrganType.ARCHIVE_VAULT -> when {
                    o.load > 0.55f ->
                        "Reacting to: archive strata compressing under storage burden."

                    else ->
                        "Reacting to: archival density vs. headroom."
                }
                OrganType.SIGNAL_LUNGS -> when {
                    o.load > 0.5f ->
                        "Reacting to: constrained channel — labored signal exchange."

                    else ->
                        "Reacting to: connectivity class and respiration cadence."
                }
                OrganType.VESTIBULAR_MUSCULATURE -> when {
                    o.activity > 0.55f ->
                        "Reacting to: locomotor shear — stabilizers fighting inertia."

                    else ->
                        "Reacting to: motion intensity and orientation deltas."
                }
                OrganType.THERMAL_MEMBRANE -> when {
                    o.inflammation > 0.55f ->
                        "Reacting to: acute thermal load bleeding through the shell."

                    else ->
                        "Reacting to: pack temperature skew and systemic stress."
                }
            }
        }
    }
}
