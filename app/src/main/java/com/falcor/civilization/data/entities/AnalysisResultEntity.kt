package com.falcor.civilization.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "analysis_results",
    foreignKeys = [ForeignKey(
        entity = RunEntity::class,
        parentColumns = ["id"],
        childColumns = ["runId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("runId")]
)
data class AnalysisResultEntity(
    @PrimaryKey val id: String,
    val runId: String,
    val metricsJson: String,
    val pValuesJson: String,
    val posteriorJson: String,
    val correctionJson: String,
    val decision: String,
    val createdAt: Long
)
