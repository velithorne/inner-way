package com.velithorne.vessel.renderer_genesis

import com.velithorne.vessel.model.GeneratedAnatomyState
import com.velithorne.vessel.morphogenesis_core.TissueField

/** Field-derived emphasis for genesis-only drawing. */
data class GenesisDrawEmphasis(
    val fieldCoherence: Float,
    val shellVeilAlpha: Float,
    val reserveGlow: Float,
    val signalSpread: Float,
)

object GenesisMaterialSystem {
    fun emphasis(anatomy: GeneratedAnatomyState?): GenesisDrawEmphasis {
        val f: TissueField = anatomy?.tissueCenter ?: return GenesisDrawEmphasis(0.35f, 0.12f, 0.25f, 0.2f)
        return GenesisDrawEmphasis(
            fieldCoherence = f.density.coerceIn(0f, 1f),
            shellVeilAlpha = (f.thermalTension * 0.4f + f.density * 0.35f).coerceIn(0.06f, 0.45f),
            reserveGlow = f.reserve.coerceIn(0f, 1f),
            signalSpread = f.signal.coerceIn(0f, 1f),
        )
    }
}
