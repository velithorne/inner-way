package com.falcor.civilization.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "prereg_plans",
    foreignKeys = [ForeignKey(
        entity = HypothesisEntity::class,
        parentColumns = ["id"],
        childColumns = ["hypothesisId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("hypothesisId")]
)
data class PreregPlanEntity(
    @PrimaryKey val id: String,
    val hypothesisId: String,
    val planJson: String,
    val planHash: String,
    val frozenAt: Long?,
    val isFrozen: Boolean
)
