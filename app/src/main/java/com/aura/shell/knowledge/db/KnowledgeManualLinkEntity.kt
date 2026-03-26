package com.aura.shell.knowledge.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "knowledge_manual_links",
    primaryKeys = ["from_item_id", "to_item_id"],
    foreignKeys = [
        ForeignKey(
            entity = KnowledgeItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["from_item_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = KnowledgeItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["to_item_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["from_item_id"]),
        Index(value = ["to_item_id"]),
    ],
)
data class KnowledgeManualLinkEntity(
    @ColumnInfo(name = "from_item_id") val fromItemId: String,
    @ColumnInfo(name = "to_item_id") val toItemId: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)
