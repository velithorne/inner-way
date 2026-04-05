package com.velithorne.innerway.body

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.sqrt

/**
 * Motion / muscle: accelerometer magnitude and frame-to-frame delta → normalized motion energy (0..1).
 * Register [start] from [android.app.Application.onCreate]; samples are thread-safe for [sample].
 */
class MotionMuscleSystem(
    context: Context,
) : SensorEventListener {

    private val appContext = context.applicationContext
    private val sensorManager = appContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private val lock = Any()
    private var lastX = 0f
    private var lastY = 0f
    private var lastZ = 0f
    private var hasLast = false

    @Volatile
    private var smoothedMotion = 0f

    @Volatile
    private var lastMagnitude = 9.81f

    @Volatile
    private var lastDelta = 0f

    private val lastSampleMillis = AtomicLong(System.currentTimeMillis())

    data class Sample(
        val motionIntensity: Float,
        val magnitudeMs2: Float,
        val deltaMs2: Float,
        val timestampMillis: Long,
    )

    fun start() {
        accelerometer?.let { sensor ->
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type != Sensor.TYPE_ACCELEROMETER) return
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        val magnitude = sqrt(x * x + y * y + z * z)
        lastMagnitude = magnitude

        val delta: Float
        synchronized(lock) {
            delta = if (!hasLast) {
                hasLast = true
                0f
            } else {
                val dx = x - lastX
                val dy = y - lastY
                val dz = z - lastZ
                sqrt(dx * dx + dy * dy + dz * dz)
            }
            lastX = x
            lastY = y
            lastZ = z
        }
        lastDelta = delta

        // Normalize jerk-like delta; typical idle noise ~0–0.5, shake can exceed 15+ m/s² between frames.
        val normalized = (delta / MAX_DELTA_MS2).coerceIn(0f, 1f)
        smoothedMotion = smoothedMotion * EMA_ALPHA + normalized * (1f - EMA_ALPHA)

        lastSampleMillis.set(System.currentTimeMillis())
    }

    suspend fun sample(): Sample {
        val t = lastSampleMillis.get()
        return Sample(
            motionIntensity = smoothedMotion.coerceIn(0f, 1f),
            magnitudeMs2 = lastMagnitude,
            deltaMs2 = lastDelta,
            timestampMillis = t,
        )
    }

    companion object {
        private const val MAX_DELTA_MS2 = 22f
        private const val EMA_ALPHA = 0.82f
    }
}
