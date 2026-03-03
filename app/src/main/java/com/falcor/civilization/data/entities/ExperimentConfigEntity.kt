package com.falcor.civilization.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "experiment_configs",
    foreignKeys = [ForeignKey(
        entity = PreregPlanEntity::class,
        parentColumns = ["id"],
        childColumns = ["preregPlanId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("preregPlanId")]
)
data class ExperimentConfigEntity(
    @PrimaryKey val id: String,
    val preregPlanId: String,
    val seed: Long,
    val simScenario: String,
    val sensorSet: String,
    val controlParamsJson: String,
    val createdAt: Long
)
