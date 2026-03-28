package com.velithorne.vessel.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "ecology_snapshots",
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
data class EcologySnapshotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val specimenId: String,
    val timestampMillis: Long,
    val batteryPct: Float?,
    val isCharging: Boolean?,
    val batteryTempC: Float?,
    val powerSaveEnabled: Boolean?,
    val storageUsedPct: Float?,
    val lowMemoryFlag: Boolean?,
    val networkConnected: Boolean?,
    val networkTypeOrdinal: Int,
    val networkMetered: Boolean?,
    val motionIntensityApprox: Float?,
    val screenInteractive: Boolean?,
    val uptimeMillis: Long,
    val specimenStageOrdinal: Int,
    val branchLeadingOrdinal: Int,
    val sourceTypeOrdinal: Int,
    val notes: String = "",
)
