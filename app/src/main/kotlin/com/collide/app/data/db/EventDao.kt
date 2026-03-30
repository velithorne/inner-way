package com.collide.app.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {
    @Query("SELECT * FROM events ORDER BY timestamp DESC")
    fun getAllEvents(): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE id = :id")
    suspend fun getEventById(id: Long): EventEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: EventEntity): Long

    @Delete
    suspend fun deleteEvent(event: EventEntity)

    @Query("SELECT COUNT(*) FROM events")
    fun getEventCount(): Flow<Int>

    @Query("DELETE FROM events")
    suspend fun deleteAllEvents()
}
