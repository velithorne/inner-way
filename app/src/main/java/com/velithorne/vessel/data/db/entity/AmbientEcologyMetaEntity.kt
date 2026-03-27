package com.velithorne.vessel.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/**
 * One row per specimen — debounce timestamps and WorkManager bookkeeping.
 */
@Entity(
    tableName = "ambient_ecology_meta",
    foreignKeys = [
        ForeignKey(
            entity = SpecimenEntity::class,
            parentColumns = ["specimenId"],
            childColumns = ["specimenId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class AmbientEcologyMetaEntity(
    @PrimaryKey val specimenId: String,
    val lastSnapshotWriteMillis: Long = 0L,
    /** Snapshots at or before this timestamp have been folded into growth. */
    val lastAppliedSnapshotMillis: Long = 0L,
    val workLastScheduledMillis: Long = 0L,
    /** Background samples since last foreground (reset on FG). */
    val samplesSinceLastOpen: Int = 0,
)
