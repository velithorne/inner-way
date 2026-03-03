package com.falcor.civilization.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "hypotheses",
    foreignKeys = [ForeignKey(
        entity = ProjectEntity::class,
        parentColumns = ["id"],
        childColumns = ["projectId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("projectId")]
)
data class HypothesisEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val title: String,
    val description: String,
    val createdAt: Long,
    val status: String // DRAFT, PREREGISTERED, TESTING, PROMOTED, RETIRED
)
