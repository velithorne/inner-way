package com.velithorne.innerway.perception

import com.velithorne.innerway.body.BatteryBloodSystem
import com.velithorne.innerway.body.CircadianRhythmSystem
import com.velithorne.innerway.body.MotionMuscleSystem
import com.velithorne.innerway.body.NervousSystem
import com.velithorne.innerway.body.SignalRespirationSystem
import com.velithorne.innerway.body.StorageSkeletonSystem
import com.velithorne.innerway.body.ThermalBodySystem

/**
 * Merges body subsystem samples into a single environmental truth vector.
 */
class SensorFusion(
    private val battery: BatteryBloodSystem,
    private val thermal: ThermalBodySystem,
    private val nervous: NervousSystem,
    private val storage: StorageSkeletonSystem,
    private val motion: MotionMuscleSystem,
    private val signal: SignalRespirationSystem,
    private val circadian: CircadianRhythmSystem,
) {

    suspend fun fuse(): EnvironmentalContext {
        val blood = battery.sample()
        val heat = thermal.sample()
        val strain = nervous.sample()
        val skeleton = storage.sample()
        val muscle = motion.sample()
        val breath = signal.sample()
        val rhythm = circadian.sample()

        return EnvironmentalContext(
            energyRatio = blood.energyRatio,
            charging = blood.charging,
            thermalRatio = heat.thermalStressRatio,
            nervousLoad = strain.cognitivePressure,
            storageFreeRatio = skeleton.freeRatio,
            motionEnergy = muscle.motionIntensity,
            motionMagnitude = muscle.magnitudeMs2,
            motionDelta = muscle.deltaMs2,
            networkOpenness = breath.atmosphereOpenness,
            circadianPhase = rhythm.circadianPhase,
            isNightWindow = rhythm.isNightWindow,
            screenAwake = rhythm.screenLikelyAwake,
            timestampMillis = maxOf(
                blood.timestampMillis,
                heat.timestampMillis,
                strain.timestampMillis,
                skeleton.timestampMillis,
                muscle.timestampMillis,
                breath.timestampMillis,
                rhythm.timestampMillis,
            ),
        )
    }
}
