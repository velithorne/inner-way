package com.falcor.civilization.engine.agents

import com.falcor.civilization.domain.PreregPlanContent
import com.falcor.civilization.engine.sim.Simulator
import com.falcor.civilization.util.IdGenerator
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ScientistAgent(private val seed: Long) {
    private val rng = java.util.Random(seed)
    private val json = Json { prettyPrint = false }

    private val hypothesisTemplates = listOf(
        Triple(
            "RPM-EM Coupling",
            "Mechanical RPM variation induces measurable EM amplitude change in sensor system",
            listOf("slope_em_amp", "cross_corr_rpm_em", "delta_em_amp")
        ),
        Triple(
            "Thermal Drift Detection",
            "Temperature sensors exhibit predictable drift under controlled conditions",
            listOf("slope_temp_ir", "slope_temp_contact", "delta_temp")
        ),
        Triple(
            "Vibration-RPM Correlation",
            "Vibration RMS correlates with rotational speed in motor system",
            listOf("median_vib_rms", "cross_corr_vib_rpm", "fft_peak_rpm")
        )
    )

    fun proposeHypothesis(projectId: String): HypothesisProposal {
        val template = hypothesisTemplates[rng.nextInt(hypothesisTemplates.size)]
        return HypothesisProposal(
            id = IdGenerator.generate(),
            projectId = projectId,
            title = template.first,
            description = template.second,
            primaryMetrics = template.third
        )
    }

    fun generatePreregPlan(hypothesisId: String, primaryMetrics: List<String>): PreregPlan {
        val metrics = primaryMetrics + listOf("slope_temp_ir", "slope_rpm", "delta_em_amp")
        val thresholds = metrics.associateWith { 0.2 }
        val stoppingRule = "fixed_n"
        val exclusionCriteria = listOf("sensor_saturation", "drift_exceeded", "energy_bounds_violated")
        val alpha = 0.05
        val minSampleSize = 30
        val content = PreregPlanContent(
            metrics = metrics.distinct(),
            thresholds = thresholds,
            stoppingRule = stoppingRule,
            exclusionCriteria = exclusionCriteria,
            alpha = alpha,
            minSampleSize = minSampleSize
        )
        val planJson = json.encodeToString(content)
        return PreregPlan(
            id = IdGenerator.generate(),
            hypothesisId = hypothesisId,
            content = content,
            planJson = planJson
        )
    }

    fun generateExperimentConfigs(preregPlanId: String, count: Int): List<ExperimentConfig> {
        val scenarios = Simulator.ALL_SCENARIOS
        return (0 until count).map {
            val scenario = scenarios[rng.nextInt(scenarios.size)]
            ExperimentConfig(
                id = IdGenerator.generate(),
                preregPlanId = preregPlanId,
                seed = rng.nextLong().and(0x7FFFFFFFFFFFFFFFL),
                simScenario = scenario,
                sensorSet = DEFAULT_SENSOR_SET
            )
        }
    }

    data class HypothesisProposal(
        val id: String,
        val projectId: String,
        val title: String,
        val description: String,
        val primaryMetrics: List<String>
    )

    data class PreregPlan(
        val id: String,
        val hypothesisId: String,
        val content: PreregPlanContent,
        val planJson: String
    )

    data class ExperimentConfig(
        val id: String,
        val preregPlanId: String,
        val seed: Long,
        val simScenario: Simulator.SimScenario,
        val sensorSet: SensorSetInfo
    )

    data class SensorSetInfo(
        val channels: List<String> = listOf("temp_ir", "temp_contact", "ambient", "rpm", "em_amp", "vib_rms", "motor_power"),
        val sampleRateHz: Double = 10.0,
        val resolution: Int = 1000
    )

    companion object {
        val DEFAULT_SENSOR_SET = SensorSetInfo()
    }
}
