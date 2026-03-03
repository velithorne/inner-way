package com.falcor.civilization.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "runs",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ExperimentConfigEntity::class,
            parentColumns = ["id"],
            childColumns = ["configId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("projectId"), Index("configId")]
)
data class RunEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val configId: String?,
    val startedAt: Long,
    val endedAt: Long?,
    val mode: String, // SIM, HARDWARE
    val blind: Boolean,
    val status: String // PENDING, RUNNING, COMPLETED, FAILED, CANCELLED
)
