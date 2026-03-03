package com.falcor.civilization.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "findings",
    foreignKeys = [ForeignKey(
        entity = HypothesisEntity::class,
        parentColumns = ["id"],
        childColumns = ["hypothesisId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("hypothesisId")]
)
data class FindingEntity(
    @PrimaryKey val id: String,
    val hypothesisId: String,
    val summary: String,
    val evidenceScore: Double,
    val replicationScore: Double,
    val promotedAt: Long,
    val retiredAt: Long?
)
