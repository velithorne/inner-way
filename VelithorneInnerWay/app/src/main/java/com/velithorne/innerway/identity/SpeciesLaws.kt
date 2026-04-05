package com.velithorne.innerway.identity

import com.velithorne.innerway.genome.BaseGenome
import com.velithorne.innerway.mind.GrowthStage
import com.velithorne.innerway.perception.EnvironmentalContext

/**
 * Species laws as executable constraints. Every decision path should consult these gates.
 */
data class LawContext(
    val deviceStable: Boolean,
    val energyBudgetRemaining: Float,
    val animationCost: Float,
    val scanCost: Float,
    val thermalDistress: Boolean,
    val lowPower: Boolean,
    val memoryPressureHigh: Boolean,
    val hasMeasurableCause: Boolean,
    val memoryCount: Int,
    val stableCycles: Int,
    val careScore: Int,
    val survivalScore: Int,
    val userDirectCommand: Boolean,
    val perceptionCoverage: Float,
    val currentStage: GrowthStage,
    val targetStage: GrowthStage,
)

object SpeciesLaws {

    /** Law 1 — Survival before expression: no flourish if device or energy budget fails. */
    fun canAnimate(context: LawContext): Boolean {
        if (!context.deviceStable) return false
        if (context.thermalDistress && context.animationCost > 0.02f) return false
        if (context.lowPower && context.animationCost > 0.03f) return false
        return context.energyBudgetRemaining >= context.animationCost
    }

    /** Law 5 — Energy has cost. */
    fun canScan(context: LawContext): Boolean {
        if (!context.deviceStable) return false
        return context.energyBudgetRemaining >= context.scanCost
    }

    /** Law 8 — Growth must be earned. */
    fun canUnlockStage(context: LawContext): Boolean {
        if (context.userDirectCommand) return false
        if (context.memoryCount < 3) return false
        if (context.stableCycles < 1) return false
        return context.targetStage.ordinal <= context.currentStage.ordinal + 1
    }

    /** Law 11 — Pain is protective: contract under thermal or power pain. */
    fun shouldContract(context: LawContext): Boolean {
        return context.thermalDistress || context.lowPower || context.memoryPressureHigh
    }

    /** Law 7 — Calm is valid: dormancy when safe and still. */
    fun shouldSleep(context: LawContext): Boolean {
        val restingWindow = !context.thermalDistress && !context.lowPower
        return restingWindow && context.deviceStable
    }

    /** Law 2 — Body is truth: reject fabricated mood without measurable backing. */
    fun validateStateCause(hasBodySignal: Boolean, hasMemoryLink: Boolean): Boolean {
        return hasBodySignal || hasMemoryLink
    }

    /** Law 4/10 — Memory changes future; log major internal shifts. */
    fun mustLogAdaptation(previousSignature: String, nextSignature: String): Boolean {
        return previousSignature != nextSignature
    }

    /** Law 9 — User is environment: block puppetry channels. */
    fun isUserInfluenceAllowed(interactionKind: String): Boolean {
        return interactionKind in setOf("touch", "care_timing", "ambient_use", "charge_routine")
    }

    /** Law 12 — Perception is partial. */
    fun clampPerception(confidence: Float): Float = confidence.coerceIn(0f, 1f)

    /** Law 13 — Identity emerges from continuity (enforced by persistent stores + timeline). */
    fun identityContinuityScore(memoryCount: Int, uptimeHours: Float): Float {
        val mem = (memoryCount / 500f).coerceIn(0f, 1f)
        val time = (uptimeHours / 72f).coerceIn(0f, 1f)
        return (mem * 0.65f) + (time * 0.35f)
    }

    fun defaultLawContext(stage: GrowthStage): LawContext {
        return LawContext(
            deviceStable = true,
            energyBudgetRemaining = 1f,
            animationCost = 0.04f,
            scanCost = 0.06f,
            thermalDistress = false,
            lowPower = false,
            memoryPressureHigh = false,
            hasMeasurableCause = true,
            memoryCount = 0,
            stableCycles = 0,
            careScore = 0,
            survivalScore = 0,
            userDirectCommand = false,
            perceptionCoverage = 0.35f,
            currentStage = stage,
            targetStage = stage,
        )
    }

    fun baselineContextForPhase1(): LawContext {
        return defaultLawContext(GrowthStage.SEED).copy(
            energyBudgetRemaining = 0.85f,
            memoryCount = 1,
            perceptionCoverage = SpeciesLaws.clampPerception(0.25f),
        )
    }
}
