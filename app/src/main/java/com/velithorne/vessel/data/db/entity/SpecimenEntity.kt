package com.velithorne.vessel.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "specimens")
data class SpecimenEntity(
    @PrimaryKey val specimenId: String,
    val displayLabel: String,
    val seedType: String,
    val creationTimestampMillis: Long,
    val lastUpdateTimestampMillis: Long,
    val buildVersionCreatedOn: Int,
    val lineageGeneration: Int,
    val active: Boolean,
)
