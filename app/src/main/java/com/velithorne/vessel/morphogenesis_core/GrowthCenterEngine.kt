package com.velithorne.vessel.morphogenesis_core

import kotlin.math.cos
import kotlin.math.sin

object GrowthCenterEngine {

    fun fromGraph(graph: StructuralGraph, pressure: GrowthPressureState, timeSec: Float): List<GrowthCenter> {
        val centers = mutableListOf<GrowthCenter>()
        var i = 0
        for (n in graph.nodes) {
            if (n.kind != MorphNodeKind.TRANSIENT_GROWTH_CENTER) continue
            val wobble = sin(timeSec * 2.1f + i) * 0.012f
            centers += GrowthCenter(
                id = n.id,
                nx = n.nx + wobble,
                ny = n.ny + cos(timeSec * 1.7f + i) * 0.01f,
                intensity = n.strength * (0.6f + pressure.coherence * 0.4f),
                phase = (timeSec * 0.8f + i * 0.3f) % 6.28f,
                channel = pressure.maxComponent(),
            )
            i++
        }
        val mantle = graph.nodes.filter { it.kind == MorphNodeKind.HEAT_MANTLE_RIDGE && it.active }
        if (mantle.isNotEmpty()) {
            val m = mantle.first()
            centers += GrowthCenter(
                id = m.id + "_gc",
                nx = m.nx,
                ny = m.ny,
                intensity = m.strength * 0.4f + pressure.thermal * 0.35f,
                phase = timeSec * 1.2f,
                channel = PressureChannel.THERMAL,
            )
        }
        return centers
    }
}
