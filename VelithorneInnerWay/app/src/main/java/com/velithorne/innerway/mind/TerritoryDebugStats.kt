package com.velithorne.innerway.mind

/**
 * Aggregates for the debug panel — territory colonization metrics.
 */
data class TerritoryDebugStats(
    val favoredDirectionRad: Float,
    val avgFrontierAffinity: Float,
    val avgSafeZoneAffinity: Float,
    val exploredCellCount: Int,
    val matureTerritoryCells: Int,
    val preferenceMode: TerritoryPreferenceMode,
    val gridCols: Int,
    val gridRows: Int,
)
