package com.aura.shell.knowledge.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface KnowledgeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: KnowledgeItemEntity)

    @Update
    suspend fun update(item: KnowledgeItemEntity)

    @Delete
    suspend fun delete(item: KnowledgeItemEntity)

    @Query("SELECT * FROM knowledge_items WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): KnowledgeItemEntity?

    @Query("SELECT * FROM knowledge_items ORDER BY updated_at DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<KnowledgeItemEntity>>

    @Query("SELECT * FROM knowledge_items ORDER BY updated_at DESC LIMIT :limit")
    suspend fun getRecent(limit: Int): List<KnowledgeItemEntity>

    @Query(
        """
        SELECT * FROM knowledge_items
        WHERE rowid IN (SELECT rowid FROM knowledge_items_fts WHERE knowledge_items_fts MATCH :matchQuery)
        ORDER BY updated_at DESC
        LIMIT :limit
        """,
    )
    suspend fun searchFts(matchQuery: String, limit: Int): List<KnowledgeItemEntity>

    @Query("SELECT COUNT(*) FROM knowledge_items")
    suspend fun count(): Int
}
