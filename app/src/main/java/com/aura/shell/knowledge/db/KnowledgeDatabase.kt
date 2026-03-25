package com.aura.shell.knowledge.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [KnowledgeItemEntity::class, KnowledgeItemFts::class],
    version = 1,
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
            ).build()
        }
    }
}
