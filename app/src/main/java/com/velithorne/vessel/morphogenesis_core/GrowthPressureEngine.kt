package com.velithorne.vessel.morphogenesis_core

import com.velithorne.vessel.physiology.PhysiologySnapshot
import com.velithorne.vessel.telemetry.TelemetrySnapshot
import com.velithorne.vessel.telemetry.NetworkTransport
import kotlin.math.abs

/**
 * Derives instant channel pressures and EMA-steps historical state.
 */
object GrowthPressureEngine {

    fun instant(phys: PhysiologySnapshot, telem: TelemetrySnapshot): Map<PressureChannel, Float> {
        val s = phys.species
        val t = telem
        val thermal = (s.fever * 0.55f + s.stress * 0.25f + (if (t.batteryTempC != null && t.batteryTempC!! > 38f) 0.15f else 0f)).coerceIn(0f, 1f)
        val reserve = (s.hunger * 0.45f + (1f - s.vitality) * 0.35f).coerceIn(0f, 1f)
        val starvation = s.hunger.coerceIn(0f, 1f)
        val recovery = s.recovery.coerceIn(0f, 1f)
        val signal = (s.signalArousal * 0.5f + (if (t.networkType == NetworkTransport.CELLULAR) 0.2f else 0.05f)).coerceIn(0f, 1f)
        val archive = s.structuralLoad.coerceIn(0f, 1f)
        val motion = (t.motionIntensity ?: 0f).coerceIn(0f, 1f) * 0.9f + abs(s.neuralActivity - 0.5f)
        val circadian = (1f - s.sleepPressure).coerceIn(0f, 1f)
        val isolation = if (t.networkConnected == false) 0.7f else 0.15f
        val coherence = s.recovery * 0.4f + (1f - s.stress) * 0.35f + s.vitality * 0.25f
        val mutation = (s.stress * 0.4f + (1f - coherence) * 0.35f + abs(s.signalArousal - s.neuralActivity) * 0.25f).coerceIn(0f, 1f)
        return mapOf(
            PressureChannel.THERMAL to thermal,
            PressureChannel.RESERVE to reserve,
            PressureChannel.STARVATION to starvation,
            PressureChannel.RECOVERY to recovery,
            PressureChannel.SIGNAL to signal,
            PressureChannel.ARCHIVE to archive,
            PressureChannel.MOTION to motion.coerceIn(0f, 1f),
            PressureChannel.CIRCADIAN to circadian,
            PressureChannel.ISOLATION to isolation.coerceIn(0f, 1f),
            PressureChannel.COHERENCE to coherence.coerceIn(0f, 1f),
            PressureChannel.MUTATION to mutation,
        )
    }

    fun step(prev: GrowthPressureState, instant: Map<PressureChannel, Float>, alphaSlow: Float, alphaFast: Float): GrowthPressureState {
        fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t
        fun g(c: PressureChannel) = instant[c] ?: 0f
        return GrowthPressureState(
            thermal = lerp(prev.thermal, g(PressureChannel.THERMAL), alphaSlow),
            reserve = lerp(prev.reserve, g(PressureChannel.RESERVE), alphaSlow),
            starvation = lerp(prev.starvation, g(PressureChannel.STARVATION), alphaFast),
            recovery = lerp(prev.recovery, g(PressureChannel.RECOVERY), alphaFast),
            signal = lerp(prev.signal, g(PressureChannel.SIGNAL), alphaSlow),
            archive = lerp(prev.archive, g(PressureChannel.ARCHIVE), alphaSlow),
            motion = lerp(prev.motion, g(PressureChannel.MOTION), alphaSlow),
            circadian = lerp(prev.circadian, g(PressureChannel.CIRCADIAN), alphaSlow),
            isolation = lerp(prev.isolation, g(PressureChannel.ISOLATION), alphaSlow),
            coherence = lerp(prev.coherence, g(PressureChannel.COHERENCE), alphaSlow),
            mutation = lerp(prev.mutation, g(PressureChannel.MUTATION), alphaSlow),
        )
    }
}
