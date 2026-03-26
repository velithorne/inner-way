package com.velithorne.vessel.telemetry

import android.hardware.SensorManager

/**
 * Derives motion intensity from accelerometer deltas and pitch/roll from the rotation vector.
 */
class MotionMonitor {

    private val prevAccel = FloatArray(3)
    private var hasPrev = false

    fun derive(sensor: SensorMonitor.SensorSnapshot): MotionReading {
        val accel = sensor.accelerometer
        var intensity: Float? = null
        if (accel != null && accel.size >= 3) {
            if (hasPrev) {
                val dx = accel[0] - prevAccel[0]
                val dy = accel[1] - prevAccel[1]
                val dz = accel[2] - prevAccel[2]
                intensity = kotlin.math.sqrt(dx * dx + dy * dy + dz * dz)
            }
            prevAccel[0] = accel[0]
            prevAccel[1] = accel[1]
            prevAccel[2] = accel[2]
            hasPrev = true
        } else {
            hasPrev = false
        }

        val orient = orientationDegrees(sensor.rotationVector)
        val lux = when {
            !sensor.lightSensorPresent -> null
            else -> sensor.ambientLux
        }

        return MotionReading(
            rawMotionIntensity = intensity,
            pitchDeg = orient?.first,
            rollDeg = orient?.second,
            ambientLux = lux,
        )
    }

    fun reset() {
        hasPrev = false
        prevAccel.fill(0f)
    }

    private fun orientationDegrees(rotation: FloatArray?): Pair<Float, Float>? {
        if (rotation == null || rotation.size < 3) return null
        val rotationMatrix = FloatArray(9)
        val orientation = FloatArray(3)
        return try {
            // getRotationMatrixFromVector signature varies across API stubs; avoid relying on a boolean return.
            SensorManager.getRotationMatrixFromVector(rotationMatrix, rotation)
            SensorManager.getOrientation(rotationMatrix, orientation)
            val pitch = Math.toDegrees(orientation[1].toDouble()).toFloat()
            val roll = Math.toDegrees(orientation[2].toDouble()).toFloat()
            pitch to roll
        } catch (_: Throwable) {
            null
        }
    }

    data class MotionReading(
        val rawMotionIntensity: Float?,
        val pitchDeg: Float?,
        val rollDeg: Float?,
        val ambientLux: Float?,
    )
}
