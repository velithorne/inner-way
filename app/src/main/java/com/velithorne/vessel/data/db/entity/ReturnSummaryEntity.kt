package com.velithorne.vessel.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "return_summary_meta")
data class ReturnSummaryEntity(
    @PrimaryKey val id: Int = 0,
    /** Morphogenesis temporal return summary key. */
    val lastShownKey: String?,
    val lastBackgroundAtMillis: Long,
    /** Seed pod lineage return sheet key. */
    val seedPodLastShownKey: String? = null,
)
