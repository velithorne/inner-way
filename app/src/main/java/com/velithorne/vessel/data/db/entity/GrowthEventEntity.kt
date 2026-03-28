package com.velithorne.vessel.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "growth_events",
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
data class GrowthEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val specimenId: String,
    val timestampMillis: Long,
    val eventTypeOrdinal: Int,
    val affectedRegion: String,
    val magnitude: Float,
    val primaryDriver: String,
    val explanation: String,
    val offlineCatchUp: Boolean,
)
