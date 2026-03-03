package com.falcor.civilization.engine.sim

import kotlinx.serialization.Serializable
import kotlin.math.*

/**
 * Injects adversarial artifacts into sim stream. Deterministic given seed.
 */
class ArtifactInjector(
    private val seed: Long,
    private val artifacts: List<ArtifactSpec>
) {
    private val rng = java.util.Random(seed)

    fun inject(t: Double, values: Map<String, Double>): Map<String, Double>? {
        var result = values.toMutableMap()
        var modified = false
        for (spec in artifacts) {
            when (spec.type) {
                "thermal_drift" -> {
                    val channel = spec.channel ?: "temp_ir"
                    result[channel] = (result[channel] ?: 0.0) + spec.amplitude * t / 60.0
                    modified = true
                }
                "sensor_lag" -> {
                    // Simulated by smoothing - we apply a delay effect via exponential
                    val channel = spec.channel ?: "temp_contact"
                    val lag = spec.amplitude
                    result[channel] = (result[channel] ?: 0.0) * (1 - lag) + (values[channel] ?: 0.0) * lag
                    modified = true
                }
                "rpm_ripple" -> {
                    val freq = spec.frequency ?: 2.0
                    result["rpm"] = (result["rpm"] ?: 0.0) + spec.amplitude * sin(2 * PI * freq * t)
                    modified = true
                }
                "emi_pickup" -> {
                    val freq = spec.frequency ?: 50.0
                    result["em_amp"] = (result["em_amp"] ?: 0.0) + spec.amplitude * sin(2 * PI * freq * t)
                    modified = true
                }
                "phase_aliasing" -> {
                    val channel = spec.channel ?: "vib_rms"
                    val aliasFreq = spec.frequency ?: 0.5
                    result[channel] = (result[channel] ?: 0.0) + spec.amplitude * sin(2 * PI * aliasFreq * t)
                    modified = true
                }
                "baseline_miscalibration" -> {
                    val channel = spec.channel ?: "temp_ir"
                    result[channel] = (result[channel] ?: 0.0) + spec.amplitude
                    modified = true
                }
                "quantization" -> {
                    val resolution = (spec.amplitude * 100).toInt().coerceAtLeast(2)
                    for (ch in listOf("temp_ir", "temp_contact", "rpm")) {
                        val v = result[ch] ?: 0.0
                        result[ch] = round(v * resolution) / resolution
                    }
                    modified = true
                }
            }
        }
        return if (modified) result else null
    }

    @Serializable
    data class ArtifactSpec(
        val type: String,
        val amplitude: Double,
        val frequency: Double? = null,
        val channel: String? = null
    )

    companion object {
        val ARTIFACT_LIBRARY = mapOf(
            "thermal_drift" to { amp: Double -> ArtifactSpec("thermal_drift", amp, channel = "temp_ir") },
            "sensor_lag" to { amp: Double -> ArtifactSpec("sensor_lag", amp.coerceIn(0.0, 1.0)) },
            "rpm_ripple" to { amp: Double -> ArtifactSpec("rpm_ripple", amp, frequency = 2.0) },
            "emi_pickup" to { amp: Double -> ArtifactSpec("emi_pickup", amp, frequency = 50.0) },
            "phase_aliasing" to { amp: Double -> ArtifactSpec("phase_aliasing", amp, frequency = 0.5) },
            "baseline_miscalibration" to { amp: Double -> ArtifactSpec("baseline_miscalibration", amp, channel = "temp_ir") },
            "quantization" to { amp: Double -> ArtifactSpec("quantization", amp) }
        )
    }
}
