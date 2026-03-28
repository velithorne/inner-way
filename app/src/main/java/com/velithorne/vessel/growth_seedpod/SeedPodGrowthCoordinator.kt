package com.velithorne.vessel.growth_seedpod

import android.content.Context
import com.velithorne.vessel.background.AmbientGrowthAccumulator
import com.velithorne.vessel.background.AmbientProgressionApplicator
import com.velithorne.vessel.background.AmbientReopenProcessor
import com.velithorne.vessel.background.BackgroundTuning
import com.velithorne.vessel.background.ReturnSummaryComposer
import com.velithorne.vessel.config.GrowthProfile
import com.velithorne.vessel.config.SimulationMode
import com.velithorne.vessel.branching.BranchInfluenceModel
import com.velithorne.vessel.branching.BranchSelectionEngine
import com.velithorne.vessel.branching.BranchingTuning
import com.velithorne.vessel.data.LineageRepository
import com.velithorne.vessel.morphogenesis_core.MorphogenesisPersistenceCodec
import com.velithorne.vessel.morphogenesis_core.SelfAssemblyCoordinator
import com.velithorne.vessel.lineage.SeedPodReturnSummary
import com.velithorne.vessel.physiology.PhysiologySnapshot
import com.velithorne.vessel.progression.DevelopmentEngine
import com.velithorne.vessel.progression.ProgressionTuning
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlin.math.min

/**
 * Owns seed-pod growth state + **Room** persistence ([LineageRepository]).
 * **Irreversible stage** is advanced only by [com.velithorne.vessel.progression.DevelopmentEngine].
 * **Build reset:** [com.velithorne.vessel.growthtime.GrowthResetPolicy] clears Room — structural milestones reset with new specimen.
 */
class SeedPodGrowthCoordinator(
    context: Context,
    private val lineageRepository: LineageRepository,
    profile: GrowthProfile,
) {
    private var state: SeedPodGrowthState
    private var lastWallMs: Long = System.currentTimeMillis()
    private var lastPersistMs: Long = 0L
    private var tick: Int = 0
    private lateinit var specimenId: String

    private var progressionTuning: ProgressionTuning = profile.progression
    private var branchingTuning: BranchingTuning = profile.branching
    private val ambientTuning: BackgroundTuning = profile.background
    private var seedGrowthTuning: SeedPodGrowthTuning = profile.seedPodGrowth

    /** Wall time when app went to background — for resume catch-up. */
    private var backgroundAtMs: Long = 0L

    private var maxOfflineCatchUpMs: Long = profile.seedPodMaxOfflineCatchUpMs
    private val simulationMode: SimulationMode = profile.mode

    private lateinit var selfAssembly: SelfAssemblyCoordinator

    /**
     * Call when dev evolution speed slider changes so structural + seed-pod pacing match [GrowthProfileScaler].
     */
    fun updateTuningFromProfile(profile: GrowthProfile) {
        progressionTuning = profile.progression
        branchingTuning = profile.branching
        seedGrowthTuning = profile.seedPodGrowth
        maxOfflineCatchUpMs = profile.seedPodMaxOfflineCatchUpMs
    }

    init {
        runBlocking(Dispatchers.IO) {
            specimenId = lineageRepository.ensureActiveSpecimenExists()
            state = lineageRepository.ensureSeedRowForSpecimen(specimenId)
            lastWallMs = state.display.lastWallClockMs
            selfAssembly = SelfAssemblyCoordinator(specimenId, simulationMode)
            MorphogenesisPersistenceCodec.decode(state.morphogenesisBlob)?.let { selfAssembly.restorePersisted(it) }
        }
    }

    fun markBackground() {
        backgroundAtMs = System.currentTimeMillis()
    }

    suspend fun catchUpOffline(phys: PhysiologySnapshot): SeedPodReturnSummary? {
        if (backgroundAtMs <= 0L) return null
        val elapsed = System.currentTimeMillis() - backgroundAtMs
        backgroundAtMs = 0L
        if (elapsed < 1_000L) return null
        val capped = elapsed.coerceAtMost(maxOfflineCatchUpMs)
        val prevEntity = lineageRepository.cachedSeedEntity
        var ambientSummary: SeedPodReturnSummary? = null
        var ambientSimSec = 0f
        withContext(Dispatchers.IO) {
            val unapplied = lineageRepository.unappliedEcologySnapshots(specimenId)
            if (unapplied.isNotEmpty() && elapsed >= ambientTuning.minAwayMsForAmbientProcess) {
                val acc = AmbientGrowthAccumulator.accumulate(unapplied, ambientTuning)
                val applied = AmbientProgressionApplicator.apply(state, acc, ambientTuning)
                var merged = applied.state
                val simSec = min(
                    ambientTuning.maxAmbientCatchUpSimulatedSec,
                    capped / 1000f,
                )
                val (afterSim, used) = AmbientReopenProcessor.simulateGrowthSteps(
                    initial = merged,
                    phys = phys,
                    dtSec = simSec,
                ) { p, st, dt -> advanceGrowth(p, st, dt) }
                ambientSimSec = used
                merged = afterSim
                state = merged
                val lastTs = unapplied.maxOfOrNull { it.timestampMillis } ?: System.currentTimeMillis()
                lineageRepository.updateLastAppliedSnapshotMillis(specimenId, lastTs)
                val affMoved = applied.affinityDeltas.any { it > 1e-4f }
                val readyMoved = applied.structuralReadinessDelta > 1e-5f
                if (affMoved || readyMoved ||
                    applied.adaptationNudges.let { n ->
                        n.thermal + n.signal + n.neural + n.recovery + n.reserve + n.archive > 1e-4f
                    }
                ) {
                    lineageRepository.persistAmbientFollowUp(
                        specimenId = specimenId,
                        nudges = applied.adaptationNudges,
                        affinityMoved = affMoved,
                        readinessMoved = readyMoved,
                    )
                }
                val ambientLines = ReturnSummaryComposer.compose(
                    capped / 1000L,
                    acc,
                    ambientTuning,
                    applied,
                )
                if (ambientLines.isNotEmpty()) {
                    ambientSummary = SeedPodReturnSummary(awaySeconds = capped / 1000L, lines = ambientLines)
                }
            }
        }
        var s = state
        var t = 0f
        val totalSec = (capped / 1000f - ambientSimSec).coerceAtLeast(0f)
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
        val base = if (lines.isNotEmpty()) {
            SeedPodReturnSummary(awaySeconds = capped / 1000L, lines = lines)
        } else null
        return ReturnSummaryComposer.mergeWithExisting(
            ambient = ambientSummary?.lines.orEmpty(),
            existing = base,
            awaySeconds = capped / 1000L,
        )
    }

    /**
     * Cold start / missed lifecycle: fold unapplied ecology rows without wall away time.
     */
    suspend fun foldPendingAmbientEcology(phys: PhysiologySnapshot): SeedPodReturnSummary? =
        withContext(Dispatchers.IO) {
            val unapplied = lineageRepository.unappliedEcologySnapshots(specimenId)
            if (unapplied.isEmpty()) return@withContext null
            val prevEntity = lineageRepository.cachedSeedEntity
            val acc = AmbientGrowthAccumulator.accumulate(unapplied, ambientTuning)
            val applied = AmbientProgressionApplicator.apply(state, acc, ambientTuning)
            var merged = applied.state
            val (afterSim, _) = AmbientReopenProcessor.simulateGrowthSteps(
                initial = merged,
                phys = phys,
                dtSec = ambientTuning.maxAmbientCatchUpSimulatedSec,
            ) { p, st, dt -> advanceGrowth(p, st, dt) }
            merged = afterSim
            state = merged
            val lastTs = unapplied.maxOfOrNull { it.timestampMillis } ?: System.currentTimeMillis()
            lineageRepository.updateLastAppliedSnapshotMillis(specimenId, lastTs)
            val affMoved = applied.affinityDeltas.any { it > 1e-4f }
            val readyMoved = applied.structuralReadinessDelta > 1e-5f
            if (affMoved || readyMoved ||
                applied.adaptationNudges.let { n ->
                    n.thermal + n.signal + n.neural + n.recovery + n.reserve + n.archive > 1e-4f
                }
            ) {
                lineageRepository.persistAmbientFollowUp(
                    specimenId = specimenId,
                    nudges = applied.adaptationNudges,
                    affinityMoved = affMoved,
                    readinessMoved = readyMoved,
                )
            }
            lineageRepository.persistGrowthStep(
                specimenId = specimenId,
                previousEntity = prevEntity,
                next = state,
                physiology = phys,
                offlineCatchUp = true,
                force = true,
            )
            val spanSec = run {
                val mn = unapplied.minOfOrNull { it.timestampMillis } ?: lastTs
                ((lastTs - mn) / 1000L).coerceAtLeast(1L)
            }
            val lines = ReturnSummaryComposer.compose(spanSec, acc, ambientTuning, applied)
            if (lines.isEmpty()) return@withContext null
            SeedPodReturnSummary(awaySeconds = spanSec, lines = lines)
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

    /**
     * Structural + branching step — used by foreground tick and bounded ambient catch-up.
     */
    fun advanceGrowth(phys: PhysiologySnapshot, prev: SeedPodGrowthState, dtSec: Float): SeedPodGrowthState =
        stepWithProgression(phys, prev, dtSec)

    private fun stepWithProgression(phys: PhysiologySnapshot, prev: SeedPodGrowthState, dtSec: Float): SeedPodGrowthState {
        val now = System.currentTimeMillis()
        val afterLive = SeedPodGrowthEngine.step(phys, prev, dtSec, seedGrowthTuning)
        val mat = SeedPodGrowthEngine.maturityScore(afterLive.display, seedGrowthTuning)
        val dev = DevelopmentEngine.step(
            prev = afterLive.structural,
            liveDisplay = afterLive.display,
            maturityScore = mat,
            phys = phys,
            dtSec = dtSec,
            nowMs = now,
            tuning = progressionTuning,
        )
        val markers = lineageRepository.getAdaptationMarkersSync(specimenId)
        val (deviceProfile, ecology) = BranchInfluenceModel.fromTelemetryForStep(phys.telemetry, markers)
        val targetAff = BranchInfluenceModel.targetAffinity(deviceProfile, ecology, branchingTuning)
        val branchNext = BranchSelectionEngine.step(
            prev = dev.structural.morphologyBranch,
            target = targetAff,
            stage = dev.structural.permanentStage,
            maturityHigh = dev.structural.confirmedMaturityHigh,
            dtSec = dtSec,
            tuning = branchingTuning,
        )
        val structuralWithBranch = dev.structural.copy(morphologyBranch = branchNext)
        val perm = structuralWithBranch.permanentStage
        val snap = selfAssembly.step(
            phys = phys,
            stage = perm,
            markers = markers,
            device = deviceProfile,
            nowMs = now,
            timeSec = dtSec,
        )
        val blob = MorphogenesisPersistenceCodec.encode(
            pressure = snap.pressure,
            hidden = snap.hidden,
            biography = snap.biography,
        )
        return afterLive.copy(
            display = afterLive.display.copy(stage = perm),
            structural = structuralWithBranch,
            morphogenesisBlob = blob,
            lastSelfAssembly = snap,
        )
    }

    fun current(): SeedPodGrowthState = state

    fun specimenId(): String = specimenId

    fun progressFraction(): Float = state.structural.smoothedStructuralProgress

    /**
     * After [LineageRepository.clearAllLineage] (e.g. manual dev reset), reload specimen + state from Room.
     */
    suspend fun reloadFromRepository() {
        specimenId = lineageRepository.ensureActiveSpecimenExists()
        state = lineageRepository.ensureSeedRowForSpecimen(specimenId)
        lastWallMs = state.display.lastWallClockMs
        backgroundAtMs = 0L
        selfAssembly = SelfAssemblyCoordinator(specimenId, simulationMode)
        MorphogenesisPersistenceCodec.decode(state.morphogenesisBlob)?.let { selfAssembly.restorePersisted(it) }
    }
}
