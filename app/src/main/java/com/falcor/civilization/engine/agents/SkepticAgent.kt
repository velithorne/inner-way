package com.falcor.civilization.engine.agents

import com.falcor.civilization.engine.sim.ArtifactInjector

class SkepticAgent(private val seed: Long) {
    private val rng = java.util.Random(seed)

    /**
     * Select artifact attacks to try to produce false positives.
     */
    fun selectArtifactAttacks(runCount: Int): Map<Int, List<ArtifactInjector.ArtifactSpec>> {
        val result = mutableMapOf<Int, List<ArtifactInjector.ArtifactSpec>>()
        val types = ArtifactInjector.ARTIFACT_LIBRARY.keys.toList()
        for (i in 0 until runCount) {
            if (rng.nextDouble() < 0.4) {
                val numArtifacts = 1 + rng.nextInt(2)
                val selected = types.shuffled(rng).take(numArtifacts)
                result[i] = selected.map { type ->
                    val factory = ArtifactInjector.ARTIFACT_LIBRARY[type]!!
                    val amp = when (type) {
                        "thermal_drift" -> 0.5 + rng.nextDouble() * 1.0
                        "sensor_lag" -> 0.3 + rng.nextDouble() * 0.4
                        "rpm_ripple" -> 5.0 + rng.nextDouble() * 15.0
                        "emi_pickup" -> 0.02 + rng.nextDouble() * 0.05
                        "phase_aliasing" -> 0.01 + rng.nextDouble() * 0.03
                        "baseline_miscalibration" -> 0.5 + rng.nextDouble() * 1.5
                        "quantization" -> 0.5 + rng.nextDouble() * 0.5
                        else -> 0.5
                    }
                    factory(amp)
                }
            }
        }
        return result
    }

    fun createInjector(seed: Long, artifactSpecs: List<ArtifactInjector.ArtifactSpec>): ArtifactInjector {
        return ArtifactInjector(seed, artifactSpecs)
    }
}
