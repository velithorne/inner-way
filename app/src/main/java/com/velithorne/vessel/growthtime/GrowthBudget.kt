package com.velithorne.vessel.growthtime

/**
 * Per-channel developmental budgets (0..1), spent to accelerate visible growth.
 */
data class GrowthBudget(
    val crownGrowthBudget: Float = 0f,
    val frondGrowthBudget: Float = 0f,
    val reservoirGrowthBudget: Float = 0f,
    val shellGrowthBudget: Float = 0f,
    val archiveGrowthBudget: Float = 0f,
    val tendonGrowthBudget: Float = 0f,
    val recoveryRepairBudget: Float = 0f,
)
