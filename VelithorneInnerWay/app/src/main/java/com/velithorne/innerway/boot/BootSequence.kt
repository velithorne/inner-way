package com.velithorne.innerway.boot

import android.content.Context
import com.velithorne.innerway.memory.MemoryRepository

/**
 * Ordered startup: identity seed, genome defaults, and first-run logging.
 * Later phases extend this with body calibration and sensor baselines.
 */
object BootSequence {

    suspend fun run(context: Context, repository: MemoryRepository) {
        SpeciesInitializer.ensureSpeciesSeed(context, repository)
    }
}
