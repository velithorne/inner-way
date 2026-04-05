package com.velithorne.innerway.export

import com.velithorne.innerway.identity.VelithorneIdentity
import com.velithorne.innerway.memory.MemoryEntity
import com.velithorne.innerway.perception.EnvironmentalContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Serializes organism state for backup or diagnostics (Phase 8 can add file export).
 */
object StateExporter {

    fun toJson(
        identity: VelithorneIdentity,
        environment: EnvironmentalContext,
        memories: List<MemoryEntity>,
    ): String {
        val root = JSONObject()
        root.put("species", identity.speciesName)
        root.put("lawVersion", identity.lawVersion)
        root.put("instance", identity.instanceId)
        root.put(
            "environment",
            JSONObject().apply {
                put("energyRatio", environment.energyRatio.toDouble())
                put("charging", environment.charging)
                put("thermalRatio", environment.thermalRatio.toDouble())
                put("nervousLoad", environment.nervousLoad.toDouble())
                put("storageFreeRatio", environment.storageFreeRatio.toDouble())
                put("motionEnergy", environment.motionEnergy.toDouble())
                put("networkOpenness", environment.networkOpenness.toDouble())
                put("circadianPhase", environment.circadianPhase.toDouble())
                put("screenAwake", environment.screenAwake)
                put("timestampMillis", environment.timestampMillis)
            },
        )
        val arr = JSONArray()
        memories.take(64).forEach { m ->
            arr.put(
                JSONObject().apply {
                    put("id", m.id)
                    put("timestampMillis", m.timestampMillis)
                    put("interpretedState", m.interpretedState)
                    put("behavior", m.triggeredBehavior)
                    put("important", m.importantEvents)
                    put("kind", m.memoryKind.name)
                },
            )
        }
        root.put("memories", arr)
        return root.toString(2)
    }
}
