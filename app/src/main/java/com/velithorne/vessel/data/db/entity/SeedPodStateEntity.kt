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
)
