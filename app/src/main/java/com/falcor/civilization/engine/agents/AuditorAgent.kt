package com.falcor.civilization.engine.agents

import com.falcor.civilization.domain.ObservationPoint
import com.falcor.civilization.engine.analysis.BenjaminiHochberg
import kotlin.math.*

class AuditorAgent {
    private val MIN_SAMPLE_SIZE = 30
    private val MAX_TEMP_C = 150.0
    private val MIN_TEMP_C = -20.0
    private val MAX_RPM = 10000.0
    private val MAX_DRIFT_RATE = 2.0
    private val SATURATION_THRESHOLD = 0.99

    data class AuditResult(
        val passed: Boolean,
        val reasons: List<String>,
        val correctedPValues: Map<String, Double>? = null
    )

    fun audit(
        observations: List<ObservationPoint>,
        rawPValues: Map<String, Double>,
        preregMinSampleSize: Int,
        alpha: Double
    ): AuditResult {
        val reasons = mutableListOf<String>()

        if (observations.size < maxOf(MIN_SAMPLE_SIZE, preregMinSampleSize)) {
            reasons.add("Sample size ${observations.size} below minimum ${maxOf(MIN_SAMPLE_SIZE, preregMinSampleSize)}")
        }

        val energyCheck = checkEnergyBounds(observations)
        if (!energyCheck.first) reasons.add(energyCheck.second)

        val driftCheck = checkDrift(observations)
        if (!driftCheck.first) reasons.add(driftCheck.second)

        val saturationCheck = checkSaturation(observations)
        if (!saturationCheck.first) reasons.add(saturationCheck.second)

        val corrected = BenjaminiHochberg.correct(rawPValues.values.toList(), alpha)
        val pMap = rawPValues.keys.zip(corrected).toMap()
        val anyRejected = corrected.any { it <= alpha }

        if (reasons.isNotEmpty()) {
            return AuditResult(
                passed = false,
                reasons = reasons,
                correctedPValues = pMap
            )
        }

        return AuditResult(
            passed = true,
            reasons = emptyList(),
            correctedPValues = pMap
        )
    }

    private fun checkEnergyBounds(obs: List<ObservationPoint>): Pair<Boolean, String> {
        for (o in obs) {
            val temp = o.channels["temp_ir"] ?: o.channels["temp_contact"] ?: 0.0
            if (temp > MAX_TEMP_C || temp < MIN_TEMP_C) {
                return false to "Temperature $temp out of physical bounds [$MIN_TEMP_C, $MAX_TEMP_C]"
            }
            val rpm = o.channels["rpm"] ?: 0.0
            if (rpm > MAX_RPM || rpm < 0) {
                return false to "RPM $rpm out of bounds [0, $MAX_RPM]"
            }
        }
        return true to ""
    }

    private fun checkDrift(obs: List<ObservationPoint>): Pair<Boolean, String> {
        if (obs.size < 10) return true to ""
        val temp = obs.map { it.channels["temp_ir"] ?: 0.0 }
        val t = obs.map { it.t }
        val slope = linearSlope(t, temp)
        if (abs(slope) > MAX_DRIFT_RATE) {
            return false to "Thermal drift rate $slope exceeds max $MAX_DRIFT_RATE C/s"
        }
        return true to ""
    }

    private fun checkSaturation(obs: List<ObservationPoint>): Pair<Boolean, String> {
        val maxEm = obs.maxOfOrNull { it.channels["em_amp"] ?: 0.0 } ?: 0.0
        if (maxEm > SATURATION_THRESHOLD) {
            return false to "Sensor saturation detected (em_amp = $maxEm)"
        }
        return true to ""
    }

    private fun linearSlope(x: List<Double>, y: List<Double>): Double {
        val n = minOf(x.size, y.size)
        if (n < 2) return 0.0
        val meanX = x.average()
        val meanY = y.average()
        var num = 0.0
        var den = 0.0
        for (i in 0 until n) {
            val dx = x[i] - meanX
            num += dx * (y[i] - meanY)
            den += dx * dx
        }
        return if (abs(den) < 1e-10) 0.0 else num / den
    }
}
