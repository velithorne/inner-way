package com.aura.shell.knowledge.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "knowledge_tags")
data class TagEntity(
    @PrimaryKey @ColumnInfo(name = "tag_key") val tagKey: String,
)
