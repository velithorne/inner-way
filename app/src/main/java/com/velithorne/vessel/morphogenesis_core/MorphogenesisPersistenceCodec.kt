package com.velithorne.vessel.morphogenesis_core

/**
 * Compact persistence for self-assembly memory (pressure + hidden + biography flags).
 * Scars are re-derived over time from adaptation markers; we persist structural memory scalars only.
 */
object MorphogenesisPersistenceCodec {

    private const val V = 1

    fun encode(
        pressure: GrowthPressureState,
        hidden: InternalHiddenState,
        biography: BiographyState,
    ): String {
        val bits = ThresholdEventKind.entries.foldIndexed(0L) { i, acc, ev ->
            if (ev in biography.thresholdFlags) acc or (1L shl i) else acc
        }
        return buildString {
            append(V).append(';')
            append(pressure.thermal).append(',')
            append(pressure.reserve).append(',')
            append(pressure.starvation).append(',')
            append(pressure.recovery).append(',')
            append(pressure.signal).append(',')
            append(pressure.archive).append(',')
            append(pressure.motion).append(',')
            append(pressure.circadian).append(',')
            append(pressure.isolation).append(',')
            append(pressure.coherence).append(',')
            append(pressure.mutation).append(';')
            append(hidden.reserveLevel).append(',')
            append(hidden.thermalTension).append(',')
            append(hidden.signalCoherence).append(',')
            append(hidden.repairDebt).append(',')
            append(hidden.growthPressure).append(',')
            append(hidden.lineageConfidence).append(';')
            append(biography.rerouteCount).append(',')
            append(biography.moltCount).append(',')
            append(bits)
        }
    }

    fun decode(raw: String?): MorphogenesisPersisted? {
        if (raw.isNullOrBlank()) return null
        val parts = raw.split(';')
        if (parts.size < 4) return null
        if (parts[0].toIntOrNull() != V) return null
        val p = parts[1].split(',')
        val h = parts[2].split(',')
        val b = parts[3].split(',')
        if (p.size < 11 || h.size < 6 || b.size < 3) return null
        val pressure = GrowthPressureState(
            thermal = p[0].toFloat(),
            reserve = p[1].toFloat(),
            starvation = p[2].toFloat(),
            recovery = p[3].toFloat(),
            signal = p[4].toFloat(),
            archive = p[5].toFloat(),
            motion = p[6].toFloat(),
            circadian = p[7].toFloat(),
            isolation = p[8].toFloat(),
            coherence = p[9].toFloat(),
            mutation = p[10].toFloat(),
        )
        val hidden = InternalHiddenState(
            reserveLevel = h[0].toFloat(),
            thermalTension = h[1].toFloat(),
            signalCoherence = h[2].toFloat(),
            repairDebt = h[3].toFloat(),
            growthPressure = h[4].toFloat(),
            lineageConfidence = h[5].toFloat(),
        )
        val reroute = b[0].toInt()
        val molt = b[1].toInt()
        val bits = b[2].toLong()
        val flags = ThresholdEventKind.entries.filterIndexed { i, _ -> (bits shr i) and 1L != 0L }.toSet()
        val biography = BiographyState(
            scars = emptyList(),
            thresholdFlags = flags,
            rerouteCount = reroute,
            moltCount = molt,
        )
        return MorphogenesisPersisted(pressure, hidden, biography)
    }
}

data class MorphogenesisPersisted(
    val pressure: GrowthPressureState,
    val hidden: InternalHiddenState,
    val biography: BiographyState,
)
