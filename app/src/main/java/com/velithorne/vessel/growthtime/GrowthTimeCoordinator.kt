package com.velithorne.vessel.growthtime

import android.content.Context
import com.velithorne.vessel.data.prefs.GrowthStateStore
import com.velithorne.vessel.morphogenesis.MorphogenesisEngine
import com.velithorne.vessel.morphogenesis.MorphogenesisSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Telemetry-derived target → lagging display + budgets + persistence.
 */
class GrowthTimeCoordinator(
    context: Context,
    private val morphogenesisEngine: MorphogenesisEngine,
    timeTuning: TimeTuning,
) {
    private val timeTuning = timeTuning
    private val growthStore = GrowthStateStore(context)

    private var budget: GrowthBudget = GrowthBudget()
    private var display: DisplayMorphState? = null
    private var stageEnteredAtMs: Long = GrowthClock.nowMillis()
    private var lastWallMs: Long = GrowthClock.nowMillis()
    private var backgroundAtMs: Long = 0L
    private val _returnSummary = MutableStateFlow<GrowthSessionSummary?>(null)
    /** Emits when offline catch-up produces a summary, or null after [dismissReturnSummary]. */
    val returnSummary: StateFlow<GrowthSessionSummary?> = _returnSummary.asStateFlow()

    init {
        // Growth reset runs in AppContainer before GrowthTimeCoordinator is constructed.
        growthStore.loadDisplayOrNull()?.let { (d, se) ->
            display = d
            stageEnteredAtMs = se
            growthStore.loadBudget()?.let { budget = it }
            lastWallMs = d.lastWallClockMs
        }
    }

    fun markBackground() {
        backgroundAtMs = GrowthClock.nowMillis()
    }

    private fun consumeBackgroundIfNeeded(target: MorphogenesisSnapshot) {
        if (backgroundAtMs <= 0L) return
        val elapsed = GrowthClock.nowMillis() - backgroundAtMs
        backgroundAtMs = 0L
        if (elapsed > 800L) catchUpOffline(elapsed, target)
    }

    /**
     * Call after [MorphogenesisEngine.update] with fresh target.
     */
    fun process(target: MorphogenesisSnapshot): MorphogenesisSnapshot {
        consumeBackgroundIfNeeded(target)
        val now = GrowthClock.nowMillis()
        var dtSec = GrowthClock.secondsFromMillis(GrowthClock.elapsedSince(lastWallMs, now))
        lastWallMs = now

        // Cap dt to avoid huge jumps on debugger pause
        dtSec = dtSec.coerceIn(0f, 5f)

        if (display == null) {
            display = MorphLagEngine.initialFromTarget(target).copy(lastWallClockMs = now)
            stageEnteredAtMs = now
        }

        budget = GrowthBudgetAccumulator.accumulate(budget, target.accumulated, dtSec, timeTuning)

        var d = display!!
        d = MorphLagEngine.step(d, target, budget, dtSec, timeTuning)

        val (newStage, newEntered) = StageGateEngine.stepTowardTarget(
            current = d.temporalStage,
            targetGermination = target.germinationStage,
            nowMs = now,
            stageEnteredAtMs = stageEnteredAtMs,
            tuning = timeTuning,
        )
        stageEnteredAtMs = newEntered
        d = d.copy(temporalStage = newStage, lastWallClockMs = now)

        display = d

        growthStore.save(d, budget, stageEnteredAtMs)

        return MorphogenesisDisplayMerge.merge(target, d)
    }

    /** Offline catch-up: call once on resume with elapsed background ms. */
    fun catchUpOffline(elapsedMs: Long, target: MorphogenesisSnapshot) {
        if (elapsedMs < 1_000L) return
        val capped = elapsedMs.coerceAtMost(timeTuning.maxOfflineCatchUpMs)
        val prev = display
        var d = display ?: MorphLagEngine.initialFromTarget(target)
        var b = budget
        OfflineCatchUpEngine.simulateCatchUp(capped, timeTuning, { dtSec, _ ->
            b = GrowthBudgetAccumulator.accumulate(b, target.accumulated, dtSec, timeTuning)
            d = MorphLagEngine.step(d, target, b, dtSec, timeTuning)
        }, target)
        budget = b
        val now = GrowthClock.nowMillis()
        val (ns, ne) = StageGateEngine.stepTowardTarget(
            current = d.temporalStage,
            targetGermination = target.germinationStage,
            nowMs = now,
            stageEnteredAtMs = stageEnteredAtMs,
            tuning = timeTuning,
        )
        stageEnteredAtMs = ne
        d = d.copy(temporalStage = ns, lastWallClockMs = now)
        display = d
        lastWallMs = now
        _returnSummary.value = ReturnRevealEngine.summarize(
            prevDisplay = prev,
            nowDisplay = d,
            acc = target.accumulated,
            offlineSeconds = capped / 1000L,
        )
        growthStore.save(d, budget, stageEnteredAtMs)
    }

    fun dismissReturnSummary() {
        _returnSummary.value = null
    }

    /**
     * Clears morphogenesis temporal state (prefs) — use after manual dev specimen reset or lineage wipe.
     */
    fun resetSessionState() {
        growthStore.clear()
        budget = GrowthBudget()
        display = null
        stageEnteredAtMs = GrowthClock.nowMillis()
        lastWallMs = GrowthClock.nowMillis()
        backgroundAtMs = 0L
        _returnSummary.value = null
    }

    /**
     * Human-readable label for the strongest developmental budget channel (for UI).
     */
    fun activeBudgetChannelLabel(): String {
        val b = budget
        val entries = listOf(
            "Crown" to b.crownGrowthBudget,
            "Frond" to b.frondGrowthBudget,
            "Reservoir" to b.reservoirGrowthBudget,
            "Shell" to b.shellGrowthBudget,
            "Archive" to b.archiveGrowthBudget,
            "Tendon" to b.tendonGrowthBudget,
            "Recovery" to b.recoveryRepairBudget,
        )
        val max = entries.maxByOrNull { it.second } ?: return "Balanced"
        return if (max.second < 0.08f) "Balanced" else "${max.first} channel"
    }

    /** One-line hint from the last return summary, if any. */
    fun recentAwayLine(): String =
        _returnSummary.value?.deltas?.firstOrNull()?.message.orEmpty()

    fun displayProgressFraction(): Float {
        val d = display ?: return 0f
        return (d.seedCore.germinationProgress * 0.35f + d.growthVisuals.chamberFillVisual * 0.35f + d.seedFormBlend * 0.3f).coerceIn(0f, 1f)
    }

    fun timeTuning(): TimeTuning = timeTuning
}
