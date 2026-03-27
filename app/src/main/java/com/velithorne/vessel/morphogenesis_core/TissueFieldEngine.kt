package com.velithorne.vessel.morphogenesis_core

import kotlin.math.exp
import kotlin.math.hypot

/**
 * Graph nodes emit Gaussian bumps into field channels; blended for contour discovery.
 */
object TissueFieldEngine {

    fun sampleAt(
        nx: Float,
        ny: Float,
        graph: StructuralGraph,
        pressure: GrowthPressureState,
        hidden: InternalHiddenState,
    ): TissueField {
        var d = 0f
        var r = 0f
        var s = 0f
        var th = 0f
        var sup = 0f
        var ar = 0f
        var rep = 0f
        var sh = 0f
        var mut = 0f
        for (n in graph.nodes) {
            if (!n.active) continue
            val w = gauss(nx, ny, n.nx, n.ny, 0.14f) * n.strength
            when (n.kind) {
                MorphNodeKind.CORE_KNOT -> {
                    d += w * 1.1f
                    s += w * 0.4f
                }
                MorphNodeKind.RESERVE_BASIN_LOCUS -> r += w * 1.2f
                MorphNodeKind.CROWN_CHAMBER -> {
                    d += w * 0.4f
                    s += w * 0.5f
                }
                MorphNodeKind.ARCHIVE_PLATE_SEED -> ar += w
                MorphNodeKind.FROND_ROOT -> s += w * 0.9f
                MorphNodeKind.BRACE_ANCHOR -> sup += w
                MorphNodeKind.HEAT_MANTLE_RIDGE -> th += w
                MorphNodeKind.TRANSIENT_GROWTH_CENTER -> d += w * 0.8f
                MorphNodeKind.SCAR_ANCHOR -> rep += w * 0.6f
                MorphNodeKind.REROUTE_JUNCTION -> sup += w * 0.5f
            }
        }
        sh += pressure.thermal * 0.35f + hidden.thermalTension * 0.4f
        mut += pressure.mutation * hidden.lineageConfidence
        r += pressure.reserve * 0.4f + hidden.reserveLevel * 0.3f
        rep += hidden.repairDebt * 0.5f
        return TissueField(
            density = d.coerceIn(0f, 2f),
            reserve = r.coerceIn(0f, 2f),
            signal = s.coerceIn(0f, 2f),
            thermalTension = (th + pressure.thermal * 0.5f).coerceIn(0f, 2f),
            supportTension = sup.coerceIn(0f, 2f),
            archiveBurden = (ar + pressure.archive * 0.5f).coerceIn(0f, 2f),
            repairDebt = rep.coerceIn(0f, 2f),
            shellPressure = (sh + pressure.coherence * 0.2f).coerceIn(0f, 2f),
            mutationPressure = mut.coerceIn(0f, 2f),
        )
    }

    private fun gauss(x: Float, y: Float, cx: Float, cy: Float, sigma: Float): Float {
        val d = hypot((x - cx).toDouble(), (y - cy).toDouble()).toFloat()
        return exp(-(d * d) / (2f * sigma * sigma + 1e-4f))
    }
}
