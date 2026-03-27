package com.velithorne.vessel.growth_seedpod

import android.content.Context
import com.velithorne.vessel.data.LineageRepository
import com.velithorne.vessel.lineage.SeedPodReturnSummary
import com.velithorne.vessel.physiology.PhysiologySnapshot
import com.velithorne.vessel.progression.DevelopmentEngine
import com.velithorne.vessel.progression.ProgressionTuning
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlin.math.min

/**
 * Owns seed-pod growth state + **Room** persistence ([LineageRepository]).
 * **Irreversible stage** is advanced only by [com.velithorne.vessel.progression.DevelopmentEngine].
 * **Build reset:** [com.velithorne.vessel.growthtime.GrowthResetPolicy] clears Room — structural milestones reset with new specimen.
 */
class SeedPodGrowthCoordinator(
    context: Context,
    private val lineageRepository: LineageRepository,
) {
    private var state: SeedPodGrowthState
    private var lastWallMs: Long = System.currentTimeMillis()
    private var lastPersistMs: Long = 0L
    private var tick: Int = 0
    private lateinit var specimenId: String

    private val progressionTuning = ProgressionTuning()

    /** Wall time when app went to background — for resume catch-up. */
    private var backgroundAtMs: Long = 0L

    private val maxOfflineCatchUpMs = 120_000L

    init {
        runBlocking(Dispatchers.IO) {
            specimenId = lineageRepository.ensureActiveSpecimenExists()
            state = lineageRepository.ensureSeedRowForSpecimen(specimenId)
            lastWallMs = state.display.lastWallClockMs
        }
    }

    fun markBackground() {
        backgroundAtMs = System.currentTimeMillis()
    }

    fun catchUpOffline(phys: PhysiologySnapshot): SeedPodReturnSummary? {
        if (backgroundAtMs <= 0L) return null
        val elapsed = System.currentTimeMillis() - backgroundAtMs
        backgroundAtMs = 0L
        if (elapsed < 1_000L) return null
        val capped = elapsed.coerceAtMost(maxOfflineCatchUpMs)
        val prevEntity = lineageRepository.cachedSeedEntity
        var s = state
        var t = 0f
        val totalSec = capped / 1000f
        while (t < totalSec) {
            val dt = min(2f, totalSec - t)
            s = stepWithProgression(phys, s, dt)
            t += dt
        }
        state = s
        lastWallMs = System.currentTimeMillis()
        val batch = lineageRepository.persistGrowthStep(
            specimenId = specimenId,
            previousEntity = prevEntity,
            next = state,
            physiology = phys,
            offlineCatchUp = true,
            force = true,
        )
        val lines = batch?.returnSummaryLines.orEmpty()
        if (lines.isEmpty()) return null
        return SeedPodReturnSummary(awaySeconds = capped / 1000L, lines = lines)
    }

    fun process(phys: PhysiologySnapshot): SeedPodGrowthState {
        val now = System.currentTimeMillis()
        val dtSec = ((now - lastWallMs) / 1000f).coerceIn(0.001f, 2f)
        lastWallMs = now
        val prevEntity = lineageRepository.cachedSeedEntity
        state = stepWithProgression(phys, state, dtSec)
        tick++

        val stageChanged = prevEntity?.stageOrdinal != state.structural.permanentStage.ordinal
        val periodic = (now - lastPersistMs) > 8_000L || tick % 30 == 0
        val force = stageChanged || periodic
        lineageRepository.persistGrowthStep(
            specimenId = specimenId,
            previousEntity = prevEntity,
            next = state,
            physiology = phys,
            offlineCatchUp = false,
            force = force,
        )
        if (force) lastPersistMs = now
        return state
    }

    private fun stepWithProgression(phys: PhysiologySnapshot, prev: SeedPodGrowthState, dtSec: Float): SeedPodGrowthState {
        val now = System.currentTimeMillis()
        val afterLive = SeedPodGrowthEngine.step(phys, prev, dtSec)
        val mat = SeedPodGrowthEngine.maturityScore(afterLive.display)
        val dev = DevelopmentEngine.step(
            prev = afterLive.structural,
            liveDisplay = afterLive.display,
            maturityScore = mat,
            phys = phys,
            dtSec = dtSec,
            nowMs = now,
            tuning = progressionTuning,
        )
        val perm = dev.structural.permanentStage
        return afterLive.copy(
            display = afterLive.display.copy(stage = perm),
            structural = dev.structural,
        )
    }

    fun current(): SeedPodGrowthState = state

    fun specimenId(): String = specimenId

    fun progressFraction(): Float = state.structural.smoothedStructuralProgress
}
