package com.velithorne.vessel.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "adaptation_markers",
    primaryKeys = ["specimenId", "kindOrdinal"],
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
data class AdaptationEventEntity(
    val specimenId: String,
    val kindOrdinal: Int,
    val accumulatedIntensity: Float,
    val lastTriggeredAtMillis: Long,
    val visibleBiasApplied: Float,
    val explanationLabel: String,
)
