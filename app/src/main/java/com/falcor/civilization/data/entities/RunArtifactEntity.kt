package com.falcor.civilization.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "run_artifacts",
    foreignKeys = [ForeignKey(
        entity = RunEntity::class,
        parentColumns = ["id"],
        childColumns = ["runId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("runId")]
)
data class RunArtifactEntity(
    @PrimaryKey val id: String,
    val runId: String,
    val artifactType: String,
    val paramsJson: String,
    val injectedByAgent: String,
    val createdAt: Long
)
