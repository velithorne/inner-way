package com.velithorne.vessel.telemetry

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.core.content.ContextCompat
import java.util.concurrent.atomic.AtomicReference

/**
 * Registers accelerometer, game rotation vector (fallback rotation vector), and ambient light.
 * Call [start] / [stop] around foreground to avoid background sensor drain and leaks.
 */
class SensorMonitor(
    private val appContext: Context,
) {
    private val sensorManager: SensorManager? =
        ContextCompat.getSystemService(appContext, SensorManager::class.java)

    private val accelRef = AtomicReference(FloatArray(3))
    private val rotRef = AtomicReference(FloatArray(4)) // rotation vector length 4 or 5; use first 4
    private val luxRef = AtomicReference<Float?>(null)
    private val hasLight = AtomicReference(false)

    @Volatile
    private var registered = false

    private val listener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            if (event == null) return
            when (event.sensor.type) {
                Sensor.TYPE_ACCELEROMETER -> accelRef.set(event.values.clone())
                Sensor.TYPE_GAME_ROTATION_VECTOR,
                Sensor.TYPE_ROTATION_VECTOR,
                    -> {
                    val v = event.values
                    val copy = FloatArray(4)
                    copy[0] = v.getOrElse(0) { 0f }
                    copy[1] = v.getOrElse(1) { 0f }
                    copy[2] = v.getOrElse(2) { 0f }
                    copy[3] = v.getOrElse(3) { 0f }
                    rotRef.set(copy)
                }

                Sensor.TYPE_LIGHT -> {
                    hasLight.set(true)
                    luxRef.set(event.values.firstOrNull())
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    fun start() {
        val sm = sensorManager ?: return
        if (registered) return
        sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let { s ->
            sm.registerListener(listener, s, SensorManager.SENSOR_DELAY_UI)
        }
        val rotationSensor = sm.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
            ?: sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        rotationSensor?.let { s ->
            sm.registerListener(listener, s, SensorManager.SENSOR_DELAY_UI)
        }
        sm.getDefaultSensor(Sensor.TYPE_LIGHT)?.let { s ->
            sm.registerListener(listener, s, SensorManager.SENSOR_DELAY_NORMAL)
        }
        registered = true
    }

    fun stop() {
        val sm = sensorManager ?: return
        if (!registered) return
        sm.unregisterListener(listener)
        registered = false
    }

    fun snapshot(): SensorSnapshot {
        return SensorSnapshot(
            accelerometer = accelRef.get()?.copyOf(),
            rotationVector = rotRef.get()?.copyOf(),
            ambientLux = luxRef.get(),
            lightSensorPresent = hasLight.get() || sensorManager?.getDefaultSensor(Sensor.TYPE_LIGHT) != null,
        )
    }

    data class SensorSnapshot(
        val accelerometer: FloatArray?,
        val rotationVector: FloatArray?,
        val ambientLux: Float?,
        val lightSensorPresent: Boolean,
    )
}

private fun FloatArray.getOrElse(index: Int, default: () -> Float): Float {
    return if (index in indices) this[index] else default()
}

private fun FloatArray?.firstOrNull(): Float? {
    if (this == null || isEmpty()) return null
    return this[0]
}
