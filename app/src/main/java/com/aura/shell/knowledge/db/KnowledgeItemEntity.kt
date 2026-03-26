package com.aura.shell.knowledge.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.PrimaryKey

@Entity(tableName = "knowledge_items")
data class KnowledgeItemEntity(
    @PrimaryKey val id: String,
    val title: String,
    @ColumnInfo(name = "full_text")
    val fullText: String,
    @ColumnInfo(name = "snippet_preview")
    val snippetPreview: String,
    @ColumnInfo(name = "source_type")
    val sourceType: String,
    @ColumnInfo(name = "source_uri")
    val sourceUri: String?,
    @ColumnInfo(name = "file_name")
    val fileName: String?,
    @ColumnInfo(name = "mime_type")
    val mimeType: String?,
    @ColumnInfo(name = "tags_csv")
    val tagsCsv: String?,
    @ColumnInfo(name = "extraction_status")
    val extractionStatus: String,
    @ColumnInfo(name = "is_aura_authored")
    val isAuraAuthored: Boolean,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
)

@Fts4(contentEntity = KnowledgeItemEntity::class)
@Entity(tableName = "knowledge_items_fts")
data class KnowledgeItemFts(
    val title: String,
    @ColumnInfo(name = "full_text")
    val fullText: String,
    @ColumnInfo(name = "snippet_preview")
    val snippetPreview: String,
    @ColumnInfo(name = "file_name")
    val fileName: String?,
)
