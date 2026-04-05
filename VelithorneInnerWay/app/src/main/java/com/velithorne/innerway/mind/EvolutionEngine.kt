package com.velithorne.innerway.mind

import com.velithorne.innerway.genome.EvolutionGenome
import com.velithorne.innerway.memory.MemoryRepository

/**
 * Determines stage unlocks from memory depth, stability windows, and survival history.
 * Phase 7 implements full scoring; Phase 1 exposes structure only.
 */
class EvolutionEngine(
    private val repository: MemoryRepository,
) {

    suspend fun evaluateCurrentStage(): GrowthStage {
        val count = repository.countMemories()
        return when {
            count < 5 -> GrowthStage.SEED
            count < 50 -> GrowthStage.INFANT
            count < 200 -> GrowthStage.CHILD
            count < 500 -> GrowthStage.ADOLESCENT
            else -> GrowthStage.MATURE
        }
    }

    fun canUnlockStage(
        target: GrowthStage,
        memoryCount: Int,
        stableCycleScore: Int,
        lowStressWindows: Int,
        careScore: Int,
        survivalScore: Int,
        genome: EvolutionGenome,
    ): Boolean {
        if (memoryCount < genome.unlockMemoryFloor) return false
        if (stableCycleScore < genome.stableCyclesRequired) return false
        if (lowStressWindows < genome.lowStressWindowsRequired) return false
        if (careScore < genome.carePatternFloor) return false
        if (survivalScore < genome.survivalHistoryFloor) return false
        return target.ordinal <= GrowthStage.MATURE.ordinal
    }
}
