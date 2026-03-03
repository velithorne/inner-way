package com.falcor.civilization.engine.sim

import com.falcor.civilization.domain.ObservationPoint
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.math.*

/**
 * Deterministic signal simulator. Given seed, produces identical output.
 */
class Simulator(
    private val seed: Long,
    private val scenario: SimScenario,
    private val sampleRateHz: Double,
    private val durationSec: Double,
    private val artifactInjector: ArtifactInjector? = null
) {
    private val rng = java.util.Random(seed)
    private val channels = listOf(
        "temp_ir", "temp_contact", "ambient", "rpm", "em_amp", "vib_rms", "motor_power"
    )

    fun stream(): Flow<ObservationPoint> = flow {
        val dt = 1.0 / sampleRateHz
        var t = 0.0
        val endT = durationSec

        // First-order system time constants (seconds)
        val tauTemp = scenario.params["tauTemp"] ?: 5.0
        val tauRpm = scenario.params["tauRpm"] ?: 0.5
        val baselineRpm = scenario.params["baselineRpm"] ?: 1500.0
        val effectSize = scenario.params["effectSize"] ?: 0.0
        val noiseLevel = scenario.params["noiseLevel"] ?: 0.02
        val driftRate = scenario.params["driftRate"] ?: 0.0
        val stepAt = scenario.params["stepAt"] ?: -1.0
        val stepSize = scenario.params["stepSize"] ?: 0.0

        var tempIr = 25.0
        var tempContact = 25.0
        var ambient = 25.0
        var rpm = baselineRpm
        var emAmp = 0.1
        var vibRms = 0.01
        var motorPower = 100.0

        while (t < endT) {
            // Baseline dynamics
            val targetTemp = 25.0 + driftRate * t
            tempIr += (targetTemp - tempIr) * (dt / tauTemp) + (rng.nextGaussian() * noiseLevel)
            tempContact += (targetTemp - tempContact) * (dt / tauTemp) + (rng.nextGaussian() * noiseLevel)
            ambient = targetTemp + (rng.nextGaussian() * noiseLevel * 0.5)

            val targetRpm = baselineRpm + if (t > stepAt && stepAt > 0) stepSize else 0.0
            rpm += (targetRpm - rpm) * (dt / tauRpm) + (rng.nextGaussian() * 2.0)

            // Mild effect: small coupling between rpm and em_amp
            emAmp = 0.1 + effectSize * (rpm - baselineRpm) / 1000.0 + (rng.nextGaussian() * 0.01)
            vibRms = 0.01 + abs(rpm - baselineRpm) * 0.00001 + (rng.nextGaussian() * 0.002)
            motorPower = 100.0 + (rpm - baselineRpm) * 0.05 + (rng.nextGaussian() * 2.0)

            var values = mapOf(
                "temp_ir" to tempIr,
                "temp_contact" to tempContact,
                "ambient" to ambient,
                "rpm" to rpm,
                "em_amp" to max(0.0, emAmp),
                "vib_rms" to max(0.0, vibRms),
                "motor_power" to max(0.0, motorPower)
            )

            artifactInjector?.inject(t, values)?.let { values = it }

            emit(ObservationPoint(t = t, channels = values))
            t += dt
        }
    }

    companion object {
        val SCENARIO_NOISE_ONLY = SimScenario(
            id = "noise_only",
            name = "Noise Only",
            description = "Baseline noise, no effect",
            params = mapOf("effectSize" to 0.0, "noiseLevel" to 0.02, "tauTemp" to 5.0, "tauRpm" to 0.5, "baselineRpm" to 1500.0)
        )
        val SCENARIO_MILD_EFFECT = SimScenario(
            id = "mild_effect",
            name = "Mild Effect",
            description = "Small rpm-em coupling",
            params = mapOf("effectSize" to 0.15, "noiseLevel" to 0.02, "tauTemp" to 5.0, "tauRpm" to 0.5, "baselineRpm" to 1500.0)
        )
        val SCENARIO_STEP_CHANGE = SimScenario(
            id = "step_change",
            name = "Step Change",
            description = "RPM step at 30s",
            params = mapOf("effectSize" to 0.1, "stepAt" to 30.0, "stepSize" to 50.0, "noiseLevel" to 0.02, "tauTemp" to 5.0, "tauRpm" to 0.5, "baselineRpm" to 1500.0)
        )
        val SCENARIO_DRIFT = SimScenario(
            id = "drift",
            name = "Drift Environment",
            description = "Thermal drift over time",
            params = mapOf("effectSize" to 0.0, "driftRate" to 0.1, "noiseLevel" to 0.02, "tauTemp" to 5.0, "tauRpm" to 0.5, "baselineRpm" to 1500.0)
        )
        val SCENARIO_RESONANCE = SimScenario(
            id = "resonance",
            name = "Resonance-like",
            description = "Parameter coupling mimicking resonance",
            params = mapOf("effectSize" to 0.2, "noiseLevel" to 0.03, "tauTemp" to 3.0, "tauRpm" to 0.3, "baselineRpm" to 1500.0)
        )

        val ALL_SCENARIOS = listOf(
            SCENARIO_NOISE_ONLY,
            SCENARIO_MILD_EFFECT,
            SCENARIO_STEP_CHANGE,
            SCENARIO_DRIFT,
            SCENARIO_RESONANCE
        )
    }
}

data class SimScenario(
    val id: String,
    val name: String,
    val description: String,
    val params: Map<String, Double>
)
