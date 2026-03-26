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

    // --- Tags ---
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTag(tag: TagEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertItemTag(ref: KnowledgeItemTagCrossRef)

    @Query("DELETE FROM knowledge_item_tags WHERE item_id = :itemId AND tag_key = :tagKey")
    suspend fun deleteItemTag(itemId: String, tagKey: String)

    @Query(
        """
        SELECT tag_key FROM knowledge_item_tags
        WHERE item_id = :itemId
        ORDER BY tag_key ASC
        """,
    )
    suspend fun getTagKeysForItem(itemId: String): List<String>

    @Query(
        """
        SELECT k.* FROM knowledge_items k
        INNER JOIN knowledge_item_tags t ON k.id = t.item_id
        WHERE t.tag_key = :tagKey
        ORDER BY k.updated_at DESC
        LIMIT :limit
        """,
    )
    suspend fun getItemsWithTag(tagKey: String, limit: Int): List<KnowledgeItemEntity>

    @Query("SELECT DISTINCT tag_key FROM knowledge_tags ORDER BY tag_key ASC LIMIT :limit")
    suspend fun getAllTagKeys(limit: Int): List<String>

    @Query(
        """
        SELECT tag_key FROM knowledge_item_tags
        GROUP BY tag_key
        ORDER BY COUNT(*) DESC
        LIMIT :limit
        """,
    )
    suspend fun getPopularTagKeys(limit: Int): List<String>

    // --- Manual links (single direction A -> B; queries consider both) ---
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertManualLink(link: KnowledgeManualLinkEntity)

    @Query(
        """
        DELETE FROM knowledge_manual_links
        WHERE (from_item_id = :fromId AND to_item_id = :toId)
           OR (from_item_id = :toId AND to_item_id = :fromId)
        """,
    )
    suspend fun deleteManualLinkBidirectional(fromId: String, toId: String)

    @Query(
        """
        SELECT * FROM knowledge_items WHERE id IN (
            SELECT to_item_id FROM knowledge_manual_links WHERE from_item_id = :itemId
            UNION
            SELECT from_item_id FROM knowledge_manual_links WHERE to_item_id = :itemId
        )
        """,
    )
    suspend fun getManuallyLinkedItems(itemId: String): List<KnowledgeItemEntity>

    @Query(
        """
        SELECT EXISTS(
            SELECT 1 FROM knowledge_manual_links
            WHERE (from_item_id = :a AND to_item_id = :b) OR (from_item_id = :b AND to_item_id = :a)
        )
        """,
    )
    suspend fun manualLinkExists(a: String, b: String): Boolean
}
