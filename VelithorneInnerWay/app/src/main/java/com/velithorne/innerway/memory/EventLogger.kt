package com.velithorne.innerway.memory

/**
 * Structured logging into persistent memory (Law 10 — adaptation must be logged).
 */
class EventLogger(
    private val repository: MemoryRepository,
) {

    suspend fun log(
        bodySummary: String,
        interpretedState: String,
        behavior: String,
        important: String,
        kind: MemoryKind = MemoryKind.GENERAL,
        stress: Boolean = false,
        care: Boolean = false,
        growth: String? = null,
    ) {
        repository.insert(
            MemoryEntity(
                timestampMillis = System.currentTimeMillis(),
                bodyConditionsSummary = bodySummary,
                interpretedState = interpretedState,
                triggeredBehavior = behavior,
                importantEvents = important,
                growthTransition = growth,
                stressIncident = stress,
                careIncident = care,
                memoryKind = kind,
            )
        )
    }
}
