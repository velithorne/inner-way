package com.velithorne.vessel.morphogenesis

/**
 * Deterministic explanations from smoothed pressures + genome.
 */
object GrowthExplainer {

    fun explain(
        acc: PressureAccumulator,
        genome: SpeciesGenome,
        field: GrowthPressureField,
        tuning: GrowthTuning,
    ): List<String> {
        val lines = mutableListOf<String>()
        if (acc.thermal > tuning.explanationPressureFloor && genome.coolingVeilBias > 0.35f) {
            lines += "Perimeter veil responds to thermal load; cooling channels prioritize shell routing."
        }
        if (acc.signal > tuning.explanationPressureFloor && field.lateralSignal > 0.45f) {
            lines += "Signal branches extend under sustained transport and metered channel strain."
        }
        if (acc.archive > tuning.explanationPressureFloor) {
            lines += "Archive basin widens as storage burden and structural strata accumulate."
        }
        if (acc.hunger > tuning.explanationPressureFloor && acc.reserve < 0.45f) {
            lines += "Lower reservoir contracts when reserve deficit repeats across ticks."
        }
        if (acc.neural > 0.42f) {
            lines += "Cortical crown branches where neural throughput and interactive load stay elevated."
        }
        if (acc.motion > 0.4f) {
            lines += "Support tendons tense when motion and stabilization demand rise."
        }
        if (acc.recovery > 0.48f && acc.thermal < 0.35f) {
            lines += "Shell thickening eases when recovery tone rises and thermal stress falls."
        }
        if (lines.isEmpty()) {
            lines += "Morphogenesis stable; pressures within nominal growth envelope."
        }
        return lines.take(4)
    }

    fun statusLabel(acc: PressureAccumulator, visibleActivity: Float): String = when {
        visibleActivity > 0.55f -> "Branching"
        acc.signal > 0.55f -> "Signal adapting"
        acc.thermal > 0.52f -> "Cooling adaptation"
        acc.archive > 0.5f -> "Archive densifying"
        acc.recovery > 0.5f && acc.thermal < 0.35f -> "Stabilizing"
        else -> "Growing"
    }

    fun statusLine(acc: PressureAccumulator): String {
        val parts = mutableListOf<String>()
        if (acc.signal > 0.45f) parts += "lateral conduits active"
        if (acc.thermal > 0.42f) parts += "thermal veil engaged"
        if (acc.archive > 0.4f) parts += "basin accreting"
        return if (parts.isEmpty()) "Tissue coherence steady."
        else parts.joinToString(" · ")
    }
}
