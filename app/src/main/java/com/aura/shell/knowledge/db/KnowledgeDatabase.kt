package com.aura.shell.knowledge.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        KnowledgeItemEntity::class,
        KnowledgeItemFts::class,
        TagEntity::class,
        KnowledgeItemTagCrossRef::class,
        KnowledgeManualLinkEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class KnowledgeDatabase : RoomDatabase() {
    abstract fun knowledgeDao(): KnowledgeDao

    companion object {
        fun create(context: Context): KnowledgeDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                KnowledgeDatabase::class.java,
                "aura_knowledge.db",
            )
                .addMigrations(KNOWLEDGE_MIGRATION_1_2)
                .build()
        }
    }
}
