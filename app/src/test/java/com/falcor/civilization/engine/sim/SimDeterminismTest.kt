package com.falcor.civilization.engine.sim

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class SimDeterminismTest {

    @Test
    fun sameSeedProducesIdenticalOutput() = runTest {
        val scenario = Simulator.SCENARIO_MILD_EFFECT
        val sim1 = Simulator(42L, scenario, 10.0, 5.0, null)
        val sim2 = Simulator(42L, scenario, 10.0, 5.0, null)
        val obs1 = sim1.stream().toList()
        val obs2 = sim2.stream().toList()
        assertEquals(obs1.size, obs2.size)
        obs1.zip(obs2).forEach { (a, b) ->
            assertEquals(a.t, b.t, 1e-10)
            a.channels.forEach { (k, v) ->
                assertEquals(v, b.channels[k]!!, 1e-10)
            }
        }
    }

    @Test
    fun differentSeedProducesDifferentOutput() = runTest {
        val scenario = Simulator.SCENARIO_MILD_EFFECT
        val sim1 = Simulator(42L, scenario, 10.0, 5.0, null)
        val sim2 = Simulator(43L, scenario, 10.0, 5.0, null)
        val obs1 = sim1.stream().toList()
        val obs2 = sim2.stream().toList()
        val different = obs1.zip(obs2).any { (a, b) ->
            a.channels.any { (k, v) -> kotlin.math.abs(v - (b.channels[k] ?: 0.0)) > 1e-6 }
        }
        assertTrue(different)
    }
}
