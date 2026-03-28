package com.velithorne.vessel.morphogenesis_core

/**
 * Slow structural memory — body responds more to this than instant telemetry.
 */
data class GrowthPressureState(
    val thermal: Float = 0f,
    val reserve: Float = 0f,
    val starvation: Float = 0f,
    val recovery: Float = 0f,
    val signal: Float = 0f,
    val archive: Float = 0f,
    val motion: Float = 0f,
    val circadian: Float = 0f,
    val isolation: Float = 0f,
    val coherence: Float = 0f,
    val mutation: Float = 0f,
) {
    companion object {
        fun fromChannels(map: Map<PressureChannel, Float>): GrowthPressureState {
            fun v(c: PressureChannel) = map[c] ?: 0f
            return GrowthPressureState(
                thermal = v(PressureChannel.THERMAL),
                reserve = v(PressureChannel.RESERVE),
                starvation = v(PressureChannel.STARVATION),
                recovery = v(PressureChannel.RECOVERY),
                signal = v(PressureChannel.SIGNAL),
                archive = v(PressureChannel.ARCHIVE),
                motion = v(PressureChannel.MOTION),
                circadian = v(PressureChannel.CIRCADIAN),
                isolation = v(PressureChannel.ISOLATION),
                coherence = v(PressureChannel.COHERENCE),
                mutation = v(PressureChannel.MUTATION),
            )
        }
    }

    fun maxComponent(): PressureChannel {
        val pairs = listOf(
            PressureChannel.THERMAL to thermal,
            PressureChannel.RESERVE to reserve,
            PressureChannel.STARVATION to starvation,
            PressureChannel.RECOVERY to recovery,
            PressureChannel.SIGNAL to signal,
            PressureChannel.ARCHIVE to archive,
            PressureChannel.MOTION to motion,
            PressureChannel.CIRCADIAN to circadian,
            PressureChannel.ISOLATION to isolation,
            PressureChannel.COHERENCE to coherence,
            PressureChannel.MUTATION to mutation,
        )
        return pairs.maxByOrNull { it.second }!!.first
    }
}
