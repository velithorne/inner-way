package com.velithorne.vessel.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "growth_stage_events",
    foreignKeys = [
        ForeignKey(
            entity = SpecimenEntity::class,
            parentColumns = ["specimenId"],
            childColumns = ["specimenId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("specimenId"), Index("timestampMillis")],
)
data class GrowthStageEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val specimenId: String,
    val timestampMillis: Long,
    val fromStageOrdinal: Int,
    val toStageOrdinal: Int,
    val triggeringChannel: String,
    val explanation: String,
)
