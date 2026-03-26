package com.aura.shell.knowledge.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "knowledge_item_tags",
    primaryKeys = ["item_id", "tag_key"],
    foreignKeys = [
        ForeignKey(
            entity = KnowledgeItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["item_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["tag_key"],
            childColumns = ["tag_key"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["tag_key"]),
        Index(value = ["item_id"]),
    ],
)
data class KnowledgeItemTagCrossRef(
    @ColumnInfo(name = "item_id") val itemId: String,
    @ColumnInfo(name = "tag_key") val tagKey: String,
)
