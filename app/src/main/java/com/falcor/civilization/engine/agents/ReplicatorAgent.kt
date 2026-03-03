package com.falcor.civilization.engine.agents

import com.falcor.civilization.engine.sim.Simulator
import com.falcor.civilization.util.IdGenerator
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ReplicatorAgent(private val seed: Long) {
    private val rng = java.util.Random(seed)
    private val json = Json { prettyPrint = false }

    data class ReplicationConfig(
        val id: String,
        val findingId: String,
        val originalConfigId: String,
        val seed: Long,
        val simScenario: Simulator.SimScenario,
        val sensorSet: ScientistAgent.SensorSetInfo,
        val isNegativeControl: Boolean
    )

    fun scheduleReplications(
        findingId: String,
        originalConfigId: String,
        originalScenarioId: String,
        count: Int
    ): List<ReplicationConfig> {
        val scenarios = Simulator.ALL_SCENARIOS
        val baseScenario = scenarios.find { it.id == originalScenarioId } ?: Simulator.SCENARIO_MILD_EFFECT
        return (0 until count).map { i ->
            val useNegativeControl = i == count - 1
            val scenario = if (useNegativeControl) Simulator.SCENARIO_NOISE_ONLY else baseScenario
            ReplicationConfig(
                id = IdGenerator.generate(),
                findingId = findingId,
                originalConfigId = originalConfigId,
                seed = rng.nextLong().and(0x7FFFFFFFFFFFFFFFL),
                simScenario = scenario,
                sensorSet = ScientistAgent.DEFAULT_SENSOR_SET,
                isNegativeControl = useNegativeControl
            )
        }
    }

    /**
     * Update replication score: (successful replications + 1) / (total replications + 1)
     */
    fun computeReplicationScore(successfulReplications: Int, totalReplications: Int): Double {
        if (totalReplications == 0) return 0.0
        return (successfulReplications + 1).toDouble() / (totalReplications + 2)
    }
}
