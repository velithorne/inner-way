package com.velithorne.vessel.data

import android.content.Context
import com.velithorne.vessel.BuildConfig
import com.velithorne.vessel.branching.BranchingTuning
import com.velithorne.vessel.background.BackgroundTuning
import com.velithorne.vessel.background.EcologySnapshot
import com.velithorne.vessel.data.db.VesselDatabase
import com.velithorne.vessel.data.db.entity.AmbientEcologyMetaEntity
import com.velithorne.vessel.data.db.entity.AmbientEventEntity
import com.velithorne.vessel.data.db.mapper.EcologySnapshotMapper
import com.velithorne.vessel.data.db.entity.AdaptationEventEntity
import com.velithorne.vessel.background.AmbientProgressionApplicator
import com.velithorne.vessel.background.toLabel
import com.velithorne.vessel.data.db.entity.GrowthEventEntity
import com.velithorne.vessel.data.db.entity.GrowthStageEventEntity
import com.velithorne.vessel.data.db.entity.ReturnSummaryEntity
import com.velithorne.vessel.data.db.entity.SeedPodStateEntity
import com.velithorne.vessel.data.db.entity.SpecimenEntity
import com.velithorne.vessel.data.db.mapper.EventMapper
import com.velithorne.vessel.data.db.mapper.GrowthStateMapper
import com.velithorne.vessel.data.db.mapper.SpecimenMapper
import com.velithorne.vessel.data.prefs.SeedPodStateStore
import com.velithorne.vessel.growth_seedpod.SeedPodExplainer
import com.velithorne.vessel.growth_seedpod.SeedPodGrowthBudget
import com.velithorne.vessel.growth_seedpod.SeedPodGrowthEngine
import com.velithorne.vessel.growth_seedpod.SeedPodGrowthStage
import com.velithorne.vessel.growth_seedpod.SeedPodGrowthState
import com.velithorne.vessel.branching.BranchAffinity
import com.velithorne.vessel.branching.BranchExplainer
import com.velithorne.vessel.branching.DeviceProfile
import com.velithorne.vessel.branching.LineageBranch
import com.velithorne.vessel.branching.UsageEcologyProfileBuilder
import com.velithorne.vessel.progression.DevelopmentMilestone
import com.velithorne.vessel.progression.MilestoneBits
import com.velithorne.vessel.progression.ProgressBarModelFactory
import com.velithorne.vessel.lineage.AdaptationKind
import com.velithorne.vessel.lineage.AdaptationMarker
import com.velithorne.vessel.lineage.GrowthEventType
import com.velithorne.vessel.lineage.GrowthHistory
import com.velithorne.vessel.lineage.LineageEngine
import com.velithorne.vessel.lineage.LineageSummary
import com.velithorne.vessel.lineage.SpecimenAge
import com.velithorne.vessel.lineage.SpecimenIdentity
import com.velithorne.vessel.lineage.SpecimenLineage
import com.velithorne.vessel.lineage.StageTransition
import com.velithorne.vessel.physiology.PhysiologySnapshot
import com.velithorne.vessel.progression.StructuralGrowthState
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlin.math.max

/**
 * Single active specimen + Room persistence. **Build reset:** [clearAllLineage] when version changes
 * (see [com.velithorne.vessel.growth_seedpod.SeedPodResetPolicy]).
 */
class LineageRepository(
    context: Context,
    private val branchingTuning: BranchingTuning = BranchingTuning(),
    private val backgroundTuning: BackgroundTuning = BackgroundTuning(),
) {
    private val db = VesselDatabase.create(context)
    private val specimenDao = db.specimenDao()
    private val seedDao = db.seedPodStateDao()
    private val growthDao = db.growthEventDao()
    private val adaptDao = db.adaptationDao()
    private val returnDao = db.returnSummaryDao()
    private val ecologyDao = db.ecologySnapshotDao()
    private val ambientEventDao = db.ambientEventDao()
    private val ambientMetaDao = db.ambientEcologyMetaDao()
    private val legacyPrefs = SeedPodStateStore(context)

    /** In-memory cache of last persisted pod row for diffing (updated after each persist). */
    @Volatile
    var cachedSeedEntity: SeedPodStateEntity? = null
        private set

    @Volatile
    private var cachedSpecimenId: String? = null

    /** Active specimen id after [ensureActiveSpecimenExists] — for ambient hooks. */
    val activeSpecimenIdOrNull: String? get() = cachedSpecimenId

    /**
     * **Where saved state is restored:** load from Room, or seed row from prefs if missing.
     */
    suspend fun ensureSeedRowForSpecimen(specimenId: String): SeedPodGrowthState {
        seedDao.getForSpecimen(specimenId)?.let {
            cachedSeedEntity = it
            return GrowthStateMapper.toGrowthState(it)
        }
        migrateFromPrefsIfNeeded(specimenId)
        seedDao.getForSpecimen(specimenId)?.let {
            cachedSeedEntity = it
            return GrowthStateMapper.toGrowthState(it)
        }
        val nowInit = System.currentTimeMillis()
        val initial = SeedPodGrowthState(
            display = SeedPodGrowthEngine.initialDisplay(),
            budget = SeedPodGrowthBudget(),
            structural = StructuralGrowthState.initial(nowInit),
        )
        val now = System.currentTimeMillis()
        val row = GrowthStateMapper.toEntity(
            specimenId = specimenId,
            state = initial,
            lastVisibleGrowthMs = now,
            lastStageTransitionMs = 0L,
            lastAdaptationUpdateMs = 0L,
        )
        seedDao.upsert(row)
        cachedSeedEntity = row
        return initial
    }

    suspend fun getActiveIdentity(): SpecimenIdentity? =
        specimenDao.getActiveSpecimen()?.let { SpecimenMapper.toDomain(it) }

    suspend fun loadGrowthState(specimenId: String): SeedPodGrowthState? {
        val e = seedDao.getForSpecimen(specimenId) ?: return null
        cachedSeedEntity = e
        return GrowthStateMapper.toGrowthState(e)
    }

    /**
     * **Restore path:** if Room empty, import legacy [SeedPodStateStore] snapshot for active specimen.
     */
    private suspend fun migrateFromPrefsIfNeeded(specimenId: String): Boolean {
        val fromPrefs = legacyPrefs.loadOrNull(BuildConfig.VERSION_CODE) ?: return false
        val now = System.currentTimeMillis()
        val e = GrowthStateMapper.toEntity(
            specimenId = specimenId,
            state = fromPrefs,
            lastVisibleGrowthMs = now,
            lastStageTransitionMs = 0L,
            lastAdaptationUpdateMs = 0L,
        )
        seedDao.upsert(e)
        cachedSeedEntity = e
        return true
    }

    /**
     * Ensures one active specimen + seed row exist (first launch or after reset). Migrates legacy prefs once.
     */
    suspend fun ensureActiveSpecimenExists(): String {
        specimenDao.getActiveSpecimen()?.let {
            cachedSpecimenId = it.specimenId
            if (seedDao.getForSpecimen(it.specimenId) == null) {
                if (!migrateFromPrefsIfNeeded(it.specimenId)) {
                    ensureSeedRowForSpecimen(it.specimenId)
                }
            } else {
                cachedSeedEntity = seedDao.getForSpecimen(it.specimenId)
            }
            return it.specimenId
        }
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val identity = SpecimenIdentity(
            specimenId = id,
            displayLabel = "Specimen 01",
            seedType = "silicon_seed_pod",
            creationTimestampMillis = now,
            lastUpdateTimestampMillis = now,
            buildVersionCreatedOn = BuildConfig.VERSION_CODE,
            lineageGeneration = 1,
            active = true,
        )
        specimenDao.upsert(SpecimenMapper.toEntity(identity))
        cachedSpecimenId = id
        if (!migrateFromPrefsIfNeeded(id)) {
            ensureSeedRowForSpecimen(id)
        }
        return id
    }

    /**
     * Persist growth step + lineage events. Throttled unless [force] or meaningful batch content.
     */
    fun persistGrowthStep(
        specimenId: String,
        previousEntity: SeedPodStateEntity?,
        next: SeedPodGrowthState,
        physiology: PhysiologySnapshot,
        offlineCatchUp: Boolean,
        force: Boolean,
    ): LineageEngine.PersistenceBatch? {
        val now = System.currentTimeMillis()
        val batch = LineageEngine.buildBatch(
            previousEntity,
            next,
            physiology,
            offlineCatchUp,
            now,
            branchingTuning,
        )
        val important = batch.stageTransition != null || batch.growthEvents.isNotEmpty() ||
            batch.returnSummaryLines.isNotEmpty() || offlineCatchUp
        if (!force && !important) return null

        runBlocking(Dispatchers.IO) {
            specimenDao.getActiveSpecimen()?.let { sp ->
                specimenDao.upsert(
                    sp.copy(lastUpdateTimestampMillis = now),
                )
            }

            val entity = GrowthStateMapper.toEntity(
                specimenId = specimenId,
                state = next,
                lastVisibleGrowthMs = if (important) batch.newLastVisibleGrowthMs else (previousEntity?.lastVisibleGrowthMs ?: now),
                lastStageTransitionMs = if (important) batch.newLastStageTransitionMs else (previousEntity?.lastStageTransitionMs ?: 0L),
                lastAdaptationUpdateMs = if (important) batch.newLastAdaptationUpdateMs else (previousEntity?.lastAdaptationUpdateMs ?: 0L),
                lastVisualExpressionMagnitude = if (important) batch.newLastVisualExpressionMagnitude else (previousEntity?.lastVisualExpressionMagnitude ?: 0f),
            )
            seedDao.upsert(entity)
            cachedSeedEntity = entity

            if (!important) return@runBlocking

            batch.stageTransition?.let { st ->
                growthDao.insertStageEvent(
                    GrowthStageEventEntity(
                        specimenId = specimenId,
                        timestampMillis = now,
                        fromStageOrdinal = st.from.ordinal,
                        toStageOrdinal = st.to.ordinal,
                        triggeringChannel = st.channel,
                        explanation = st.explanation,
                    ),
                )
            }
            for (ge in batch.growthEvents) {
                growthDao.insertGrowthEvent(
                    GrowthEventEntity(
                        specimenId = specimenId,
                        timestampMillis = now,
                        eventTypeOrdinal = ge.type.ordinal,
                        affectedRegion = ge.region,
                        magnitude = ge.magnitude,
                        primaryDriver = ge.driver,
                        explanation = ge.explanation,
                        offlineCatchUp = ge.offline,
                    ),
                )
            }
            for (a in batch.adaptationUpserts) {
                val existing = adaptDao.getAllForSpecimen(specimenId).find { it.kindOrdinal == a.kind.ordinal }
                val acc = (existing?.accumulatedIntensity ?: 0f) + a.deltaIntensity
                adaptDao.upsert(
                    AdaptationEventEntity(
                        specimenId = specimenId,
                        kindOrdinal = a.kind.ordinal,
                        accumulatedIntensity = acc.coerceIn(0f, 2.5f),
                        lastTriggeredAtMillis = now,
                        visibleBiasApplied = acc.coerceIn(0f, 1f),
                        explanationLabel = a.label,
                    ),
                )
            }
        }
        return batch
    }

    suspend fun getGrowthHistory(specimenId: String, limit: Int = 24): GrowthHistory {
        val st = growthDao.recentStageEvents(specimenId, limit).map { EventMapper.stageToDomain(it) }
        val ev = growthDao.recentGrowthEvents(specimenId, limit).map { EventMapper.growthToDomain(it) }
        return GrowthHistory(stageTransitions = st, growthEvents = ev)
    }

    /** For synchronous growth step (UI thread) — lightweight read. */
    fun getAdaptationMarkersSync(specimenId: String): List<AdaptationMarker> =
        runBlocking(Dispatchers.IO) {
            adaptDao.getAllForSpecimen(specimenId).map { e ->
                AdaptationMarker(
                    kind = AdaptationKind.entries[e.kindOrdinal],
                    accumulatedIntensity = e.accumulatedIntensity,
                    lastTriggeredAtMillis = e.lastTriggeredAtMillis,
                    visibleBiasApplied = e.visibleBiasApplied,
                    explanationLabel = e.explanationLabel,
                )
            }
        }

    suspend fun getAdaptationMarkers(specimenId: String): List<AdaptationMarker> =
        adaptDao.getAllForSpecimen(specimenId).map { e ->
            com.velithorne.vessel.lineage.AdaptationMarker(
                kind = com.velithorne.vessel.lineage.AdaptationKind.entries[e.kindOrdinal],
                accumulatedIntensity = e.accumulatedIntensity,
                lastTriggeredAtMillis = e.lastTriggeredAtMillis,
                visibleBiasApplied = e.visibleBiasApplied,
                explanationLabel = e.explanationLabel,
            )
        }

    fun formatAge(creationMs: Long): SpecimenAge {
        val age = max(0L, System.currentTimeMillis() - creationMs)
        val d = age / 86_400_000L
        val h = (age % 86_400_000L) / 3_600_000L
        val short = when {
            d > 0 -> "${d}d ${h}h"
            h > 0 -> "${h}h"
            else -> "<1h"
        }
        return SpecimenAge(ageMillis = age, formattedShort = short)
    }

    suspend fun buildLineageView(specimenId: String): SpecimenLineage? {
        val sp = specimenDao.getActiveSpecimen() ?: return null
        val id = SpecimenMapper.toDomain(sp)
        val seed = seedDao.getForSpecimen(specimenId) ?: return null
        val st = SeedPodGrowthStage.entries.getOrNull(seed.stageOrdinal)
            ?: SeedPodGrowthStage.DORMANT_POD
        val stageLabel = SeedPodExplainer.stageLabel(st)
        val nextLabel = ProgressBarModelFactory.build(
            st,
            seed.smoothedStructuralProgress,
            seed.nextStageAccum,
            0f,
        ).nextStageLabel
        val entered = seed.structuralStageEnteredAtMs.takeIf { it > 0L } ?: seed.lastWallClockMs
        val dwellMs = max(0L, System.currentTimeMillis() - entered)
        val dwellStr = when {
            dwellMs >= 86_400_000L -> "${dwellMs / 86_400_000L}d in stage"
            dwellMs >= 3_600_000L -> "${dwellMs / 3_600_000L}h in stage"
            else -> "${dwellMs / 60_000L}m in stage"
        }
        val lt = seed.leanThermal
        val ln = seed.leanNeural
        val ls = seed.leanSignal
        val lr = seed.leanReserve
        val tendency = when {
            lt >= ln && lt >= ls && lt >= lr -> "Leaning thermal / shell specialization path"
            ln >= ls && ln >= lr -> "Leaning neural / crown path"
            ls >= lr -> "Leaning signal / lateral branch path"
            else -> "Leaning reserve / endurance path"
        }
        val unlocked = buildString {
            val names = listOf(
                DevelopmentMilestone.CROWN_BUD to "Crown",
                DevelopmentMilestone.LATERAL_BUDS to "Laterals",
                DevelopmentMilestone.RESERVE_BULB to "Reserve",
                DevelopmentMilestone.CHAMBER_MATURED to "Chamber",
                DevelopmentMilestone.LINEAGE_DIFFERENTIATION to "Lineage diff",
                DevelopmentMilestone.SPECIALIZATION_READY to "Spec ready",
            )
            val on = names.filter { MilestoneBits.has(seed.milestoneFlags, it.first) }.map { it.second }
            if (on.isEmpty()) append("No milestones recorded yet")
            else append(on.joinToString(" · "))
        }
        val hist = getGrowthHistory(specimenId, 8)
        val lastEvent = hist.growthEvents.firstOrNull()?.explanation
            ?: hist.stageTransitions.firstOrNull()?.explanation
            ?: "—"
        val markers = getAdaptationMarkersSync(specimenId)
        val ecology = UsageEcologyProfileBuilder.fromMarkers(markers)
        val lead = LineageBranch.entries.getOrNull(seed.leadingBranchOrdinal) ?: LineageBranch.BALANCED
        val branchSummary = BranchExplainer.leadingLine(lead, seed.branchReadiness, st)
        val branchReason = buildString {
            append(BranchExplainer.reasonLine(DeviceProfile.neutral(), ecology, lead))
            append("\n")
            append(BranchExplainer.vignetteLine(lead, ecology))
        }
        val aff = BranchAffinity.fromArray(
            floatArrayOf(
                seed.affinity0, seed.affinity1, seed.affinity2, seed.affinity3,
                seed.affinity4, seed.affinity5, seed.affinity6,
            ),
        )
        val secondary = BranchExplainer.secondaryLine(aff, lead)
        val peakBranch = LineageBranch.entries.maxByOrNull { aff[it] } ?: LineageBranch.BALANCED
        val branchBlock = buildString {
            if (branchSummary.isNotEmpty()) append(branchSummary)
            secondary?.let {
                if (isNotEmpty()) append("\n")
                append(it)
            }
            if (isEmpty()) {
                append(
                    "Strongest affinity: ${peakBranch.displayName} (${(aff[peakBranch] * 100f).toInt()}%) · " +
                        "lead ${lead.displayName} (${(aff[lead] * 100f).toInt()}%)",
                )
            }
        }
        val affLine = LineageBranch.entries
            .filter { it != LineageBranch.BALANCED }
            .sortedByDescending { aff[it] }
            .take(4)
            .joinToString(" · ") { b -> "${b.displayName} ${(aff[b] * 100f).toInt()}%" }
        return SpecimenLineage(
            identity = id,
            age = formatAge(id.creationTimestampMillis),
            currentStageLabel = stageLabel,
            nextStageTargetLabel = nextLabel,
            timeInCurrentStageFormatted = dwellStr,
            lineageTendencyLine = tendency,
            unlockedMilestonesSummary = unlocked,
            lastMajorChangeLabel = lastEvent,
            lastStageTransitionMillis = seed.lastStageTransitionMs.takeIf { it > 0 },
            lastGrowthEventMillis = hist.growthEvents.firstOrNull()?.timestampMillis,
            branchReadinessPercent = (seed.branchReadiness * 100f).toInt().coerceIn(0, 100),
            leadingBranchLabel = lead.displayName,
            branchFamilyBlurb = lead.briefDescription,
            branchSummaryLine = branchBlock,
            branchReasonLine = branchReason,
            branchAffinityPercentsLine = affLine,
            visibleTraitsFormingLine = "Visible traits forming: ${BranchExplainer.visibleTraitsSummary(lead)}",
        )
    }

    fun lineageSummaryFromIdentity(identity: SpecimenIdentity): LineageSummary =
        LineageSummary(
            headline = "${identity.displayLabel} · ${identity.seedType.replace('_', ' ')}",
            subline = "Lineage gen ${identity.lineageGeneration} · build ${identity.buildVersionCreatedOn}",
        )

    /** **Reset on new build:** wipe Room + legacy seed prefs + return meta. */
    suspend fun clearAllLineage() {
        growthDao.deleteAllGrowth()
        growthDao.deleteAllStage()
        adaptDao.deleteAll()
        seedDao.deleteAll()
        specimenDao.deleteAll()
        returnDao.deleteAll()
        ecologyDao.deleteAll()
        ambientEventDao.deleteAll()
        ambientMetaDao.deleteAll()
        legacyPrefs.clear()
        cachedSeedEntity = null
        cachedSpecimenId = null
    }

    suspend fun insertEcologySnapshot(snap: EcologySnapshot, force: Boolean = false): Boolean {
        val id = cachedSpecimenId ?: return false
        val meta = ambientMetaDao.get(id) ?: AmbientEcologyMetaEntity(specimenId = id)
        if (!force && snap.timestampMillis - meta.lastSnapshotWriteMillis < backgroundTuning.snapshotWriteDebounceMs) {
            return false
        }
        val entity = EcologySnapshotMapper.toEntity(id, snap)
        ecologyDao.insert(entity)
        val count = ecologyDao.countForSpecimen(id)
        val max = backgroundTuning.maxEcologySnapshotsPerSpecimen
        if (count > max) ecologyDao.deleteOldestExcess(id, count - max)
        ambientMetaDao.upsert(
            meta.copy(
                lastSnapshotWriteMillis = snap.timestampMillis,
                samplesSinceLastOpen = meta.samplesSinceLastOpen + 1,
            ),
        )
        return true
    }

    suspend fun insertAmbientEvent(kind: String, detail: String) {
        val id = cachedSpecimenId ?: return
        ambientEventDao.insert(
            AmbientEventEntity(
                specimenId = id,
                timestampMillis = System.currentTimeMillis(),
                kind = kind,
                detail = detail,
            ),
        )
        val count = ambientEventDao.countForSpecimen(id)
        val max = backgroundTuning.maxAmbientEventsPerSpecimen
        if (count > max) ambientEventDao.deleteOldestExcess(id, count - max)
    }

    suspend fun getAmbientEcologyMeta(specimenId: String): AmbientEcologyMetaEntity? =
        ambientMetaDao.get(specimenId)

    suspend fun upsertAmbientMeta(e: AmbientEcologyMetaEntity) {
        ambientMetaDao.upsert(e)
    }

    suspend fun resetSamplesSinceOpen(specimenId: String) {
        val m = ambientMetaDao.get(specimenId) ?: return
        ambientMetaDao.upsert(m.copy(samplesSinceLastOpen = 0))
    }

    suspend fun updateLastAppliedSnapshotMillis(specimenId: String, ms: Long) {
        val m = ambientMetaDao.get(specimenId) ?: AmbientEcologyMetaEntity(specimenId = specimenId)
        ambientMetaDao.upsert(m.copy(lastAppliedSnapshotMillis = ms))
    }

    suspend fun unappliedEcologySnapshots(specimenId: String): List<EcologySnapshot> {
        val meta = ambientMetaDao.get(specimenId) ?: return emptyList()
        val rows = ecologyDao.since(specimenId, meta.lastAppliedSnapshotMillis)
        return rows.map { EcologySnapshotMapper.fromEntity(it) }
    }

    suspend fun recentEcologySnapshots(specimenId: String, limit: Int = 12): List<EcologySnapshot> =
        ecologyDao.recent(specimenId, limit).map { EcologySnapshotMapper.fromEntity(it) }

    suspend fun recentAmbientEvents(specimenId: String, limit: Int = 8) =
        ambientEventDao.recent(specimenId, limit)

    /** Debounced ecology write from foreground — same rules as [insertEcologySnapshot]. */
    suspend fun tryRecordEcologySnapshot(snap: EcologySnapshot): Boolean = insertEcologySnapshot(snap)

    suspend fun getReturnMeta(): ReturnSummaryEntity? = returnDao.getMeta()

    suspend fun saveReturnMeta(lastShownKey: String?, backgroundAt: Long) {
        val existing = returnDao.getMeta()
        returnDao.upsert(
            ReturnSummaryEntity(
                lastShownKey = lastShownKey,
                lastBackgroundAtMillis = backgroundAt,
                seedPodLastShownKey = existing?.seedPodLastShownKey,
                lastAmbientSummaryHash = existing?.lastAmbientSummaryHash,
            ),
        )
    }

    suspend fun shouldShowSeedPodReturn(key: String): Boolean {
        val m = returnDao.getMeta() ?: return true
        return m.seedPodLastShownKey != key
    }

    /** True if either the full line key or the ambient fingerprint changed since last dismiss. */
    suspend fun shouldShowSeedPodReturnCombined(key: String, ambientSummaryHash: String): Boolean {
        val m = returnDao.getMeta() ?: return true
        return m.seedPodLastShownKey != key || m.lastAmbientSummaryHash != ambientSummaryHash
    }

    suspend fun markSeedPodReturnShown(key: String, ambientSummaryHash: String) {
        val m = returnDao.getMeta()
        returnDao.upsert(
            ReturnSummaryEntity(
                lastShownKey = m?.lastShownKey,
                lastBackgroundAtMillis = m?.lastBackgroundAtMillis ?: System.currentTimeMillis(),
                seedPodLastShownKey = key,
                lastAmbientSummaryHash = ambientSummaryHash,
            ),
        )
    }

    /**
     * Persists adaptation bumps + optional growth event after ambient folding (no full physiology batch).
     */
    suspend fun persistAmbientFollowUp(
        specimenId: String,
        nudges: AmbientProgressionApplicator.AdaptationNudges,
        affinityMoved: Boolean,
        readinessMoved: Boolean,
    ) {
        suspend fun bump(kind: AdaptationKind, delta: Float) {
            if (delta < 1e-4f) return
            val existing = adaptDao.getAllForSpecimen(specimenId).find { it.kindOrdinal == kind.ordinal }
            val acc = ((existing?.accumulatedIntensity ?: 0f) + delta).coerceIn(0f, 2.5f)
            adaptDao.upsert(
                AdaptationEventEntity(
                    specimenId = specimenId,
                    kindOrdinal = kind.ordinal,
                    accumulatedIntensity = acc,
                    lastTriggeredAtMillis = System.currentTimeMillis(),
                    visibleBiasApplied = acc.coerceIn(0f, 1f),
                    explanationLabel = kind.toLabel(),
                ),
            )
        }
        bump(AdaptationKind.THERMAL, nudges.thermal)
        bump(AdaptationKind.SIGNAL, nudges.signal)
        bump(AdaptationKind.NEURAL, nudges.neural)
        bump(AdaptationKind.RECOVERY, nudges.recovery)
        bump(AdaptationKind.RESERVE, nudges.reserve)
        bump(AdaptationKind.ARCHIVE, nudges.archive)
        if (affinityMoved || readinessMoved) {
            growthDao.insertGrowthEvent(
                GrowthEventEntity(
                    specimenId = specimenId,
                    timestampMillis = System.currentTimeMillis(),
                    eventTypeOrdinal = GrowthEventType.AMBIENT_ECOLOGY_INFLUENCE.ordinal,
                    affectedRegion = "ambient_ecology",
                    magnitude = (if (readinessMoved) 1f else 0f) + (if (affinityMoved) 0.5f else 0f),
                    primaryDriver = "stored_snapshots",
                    explanation = buildString {
                        if (readinessMoved) append("Structural readiness nudged from offline ecology.")
                        if (affinityMoved) {
                            if (isNotEmpty()) append(" ")
                            append("Morphology affinities drifted from ambient phone patterns.")
                        }
                    },
                    offlineCatchUp = true,
                ),
            )
        }
    }

}
