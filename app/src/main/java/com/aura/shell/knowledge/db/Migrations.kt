package com.aura.shell.knowledge.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.aura.shell.knowledge.TagNormalizer

val KNOWLEDGE_MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS knowledge_tags (
                tag_key TEXT NOT NULL PRIMARY KEY
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS knowledge_item_tags (
                item_id TEXT NOT NULL,
                tag_key TEXT NOT NULL,
                PRIMARY KEY(item_id, tag_key),
                FOREIGN KEY(item_id) REFERENCES knowledge_items(id) ON DELETE CASCADE,
                FOREIGN KEY(tag_key) REFERENCES knowledge_tags(tag_key) ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_knowledge_item_tags_tag_key ON knowledge_item_tags(tag_key)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_knowledge_item_tags_item_id ON knowledge_item_tags(item_id)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS knowledge_manual_links (
                from_item_id TEXT NOT NULL,
                to_item_id TEXT NOT NULL,
                created_at INTEGER NOT NULL,
                PRIMARY KEY(from_item_id, to_item_id),
                FOREIGN KEY(from_item_id) REFERENCES knowledge_items(id) ON DELETE CASCADE,
                FOREIGN KEY(to_item_id) REFERENCES knowledge_items(id) ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_knowledge_manual_links_from ON knowledge_manual_links(from_item_id)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_knowledge_manual_links_to ON knowledge_manual_links(to_item_id)")

        // Best-effort: split legacy tags_csv into junction rows
        val cursor = db.query("SELECT id, tags_csv FROM knowledge_items WHERE tags_csv IS NOT NULL AND tags_csv != ''")
        cursor.use {
            val idCol = it.getColumnIndex("id")
            val tagsCol = it.getColumnIndex("tags_csv")
            while (it.moveToNext()) {
                val id = it.getString(idCol) ?: continue
                val csv = it.getString(tagsCol) ?: continue
                for (part in csv.split(',', ';', ' ')) {
                    val key = TagNormalizer.normalize(part)
                    if (key.length < 2) continue
                    db.execSQL("INSERT OR IGNORE INTO knowledge_tags(tag_key) VALUES(?)", arrayOf(key))
                    db.execSQL(
                        "INSERT OR IGNORE INTO knowledge_item_tags(item_id, tag_key) VALUES(?, ?)",
                        arrayOf(id, key),
                    )
                }
            }
        }
    }
}
