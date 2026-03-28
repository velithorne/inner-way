package com.velithorne.vessel.model

import com.velithorne.vessel.morphogenesis_core.BiographyState
import com.velithorne.vessel.morphogenesis_core.ScarPlate

data class BiographyVisualState(
    val scars: List<ScarPlate>,
    val rerouteCount: Int,
    val thresholdSummary: String,
) {
    companion object {
        fun from(state: BiographyState): BiographyVisualState {
            val summary = buildString {
                if (state.thresholdFlags.isNotEmpty()) {
                    append(state.thresholdFlags.joinToString(" · ") { it.name.replace('_', ' ').lowercase() })
                }
                if (state.rerouteCount > 0) {
                    if (isNotEmpty()) append(" · ")
                    append("reroutes ${state.rerouteCount}")
                }
                if (state.moltCount > 0) {
                    if (isNotEmpty()) append(" · ")
                    append("molts ${state.moltCount}")
                }
            }
            return BiographyVisualState(
                scars = state.scars,
                rerouteCount = state.rerouteCount,
                thresholdSummary = summary.ifEmpty { "baseline biography" },
            )
        }

        fun neutral() = BiographyVisualState(
            scars = emptyList(),
            rerouteCount = 0,
            thresholdSummary = "",
        )

        fun fromNullable(state: BiographyState?): BiographyVisualState =
            if (state == null) neutral() else from(state)
    }
}
