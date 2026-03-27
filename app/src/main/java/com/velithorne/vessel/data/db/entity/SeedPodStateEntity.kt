package com.velithorne.vessel.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "seed_pod_state",
    foreignKeys = [
        ForeignKey(
            entity = SpecimenEntity::class,
            parentColumns = ["specimenId"],
            childColumns = ["specimenId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("specimenId")],
)
data class SeedPodStateEntity(
    @PrimaryKey val specimenId: String,
    val stageOrdinal: Int,
    val crownNub: Float,
    val lateralBudLeft: Float,
    val lateralBudRight: Float,
    val reserveBulb: Float,
    val shellThickening: Float,
    val thermalVeil: Float,
    val tissueHaze: Float,
    val podCoherence: Float,
    val lastWallClockMs: Long,
    val budgetCrown: Float,
    val budgetLateral: Float,
    val budgetReserve: Float,
    val budgetShell: Float,
    val budgetThermal: Float,
    val budgetCoherence: Float,
    val lastVisibleGrowthMs: Long,
    val lastStageTransitionMs: Long,
    val lastAdaptationUpdateMs: Long,
    /** Irreversible milestone bit flags — [com.velithorne.vessel.progression.DevelopmentMilestone]. */
    val milestoneFlags: Long = 0L,
    val nextStageAccum: Float = 0f,
    val confirmedMaturityHigh: Float = 0f,
    val leanThermal: Float = 0.25f,
    val leanNeural: Float = 0.25f,
    val leanSignal: Float = 0.25f,
    val leanReserve: Float = 0.25f,
    val smoothedStructuralProgress: Float = 0f,
    val structuralStageEnteredAtMs: Long = 0L,
    /** [com.velithorne.vessel.branching.BranchAffinity] as seven floats. */
    val affinity0: Float = 1f / 7f,
    val affinity1: Float = 1f / 7f,
    val affinity2: Float = 1f / 7f,
    val affinity3: Float = 1f / 7f,
    val affinity4: Float = 1f / 7f,
    val affinity5: Float = 1f / 7f,
    val affinity6: Float = 1f / 7f,
    val branchReadiness: Float = 0f,
    val leadingBranchOrdinal: Int = 6,
    val branchCommitmentLevel: Int = 0,
    /** Last persisted branch visual expression 0..1 — for lineage event thresholds. */
    val lastVisualExpressionMagnitude: Float = 0f,
)
