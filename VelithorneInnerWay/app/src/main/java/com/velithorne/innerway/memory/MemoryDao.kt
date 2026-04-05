package com.velithorne.innerway.memory

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(memory: MemoryEntity): Long

    @Query("SELECT COUNT(*) FROM memories")
    suspend fun count(): Int

    @Query("SELECT * FROM memories ORDER BY timestampMillis DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM memories ORDER BY timestampMillis DESC LIMIT :limit")
    suspend fun getRecent(limit: Int): List<MemoryEntity>
}
