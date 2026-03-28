package com.velithorne.vessel.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.velithorne.vessel.data.db.entity.ReturnSummaryEntity

@Dao
interface ReturnSummaryDao {
    @Query("SELECT * FROM return_summary_meta WHERE id = 0 LIMIT 1")
    suspend fun getMeta(): ReturnSummaryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(meta: ReturnSummaryEntity)

    @Query("DELETE FROM return_summary_meta")
    suspend fun deleteAll()
}
