package com.velithorne.innerway.memory

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "memories")
data class MemoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestampMillis: Long,
    val bodyConditionsSummary: String,
    val interpretedState: String,
    val triggeredBehavior: String,
    val importantEvents: String,
    val growthTransition: String? = null,
    val stressIncident: Boolean = false,
    val careIncident: Boolean = false,
    val memoryKind: MemoryKind = MemoryKind.GENERAL,
)
