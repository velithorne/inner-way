package com.velithorne.innerway.render

/**
 * Low-resolution overlay on the display (habitat substrate).
 */
data class TerritoryMap(
    val cols: Int,
    val rows: Int,
    val cells: List<TerritoryCell>,
) {
    init {
        require(cells.size == cols * rows) { "TerritoryMap: cells size ${cells.size} != ${cols * rows}" }
    }

    fun cellAt(xIndex: Int, yIndex: Int): TerritoryCell =
        cells[yIndex * cols + xIndex]
}
