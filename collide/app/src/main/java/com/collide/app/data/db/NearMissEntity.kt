package com.collide.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "near_misses")
data class NearMissEntity(
    @PrimaryKey val id: String,
    val timestamp: Long,
    val runId: String,
    val recipeId: String,
    val recipeName: String,
    val bestScore: Float,
    val candidatePreview: String,
    val classificationReason: String
)
