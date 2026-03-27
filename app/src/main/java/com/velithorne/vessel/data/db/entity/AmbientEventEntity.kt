package com.velithorne.vessel.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "ambient_events",
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
data class AmbientEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val specimenId: String,
    val timestampMillis: Long,
    /** Short code e.g. BG, FG, CHG, NET */
    val kind: String,
    val detail: String,
)
