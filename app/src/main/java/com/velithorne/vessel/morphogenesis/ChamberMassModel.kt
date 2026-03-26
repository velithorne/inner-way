package com.velithorne.vessel.morphogenesis

/**
 * Emergent chamber regions — cranial, central metabolic, lateral signal, lower archive, perimeter shell.
 * Values are 0..1 occupancy / fill strength.
 */
data class ChamberMassModel(
    val cranialCortex: Float,
    val centralMetabolic: Float,
    val lateralSignal: Float,
    val lowerArchiveBasin: Float,
    val perimeterShell: Float,
)
