package com.collide.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [EventEntity::class, RunSummaryEntity::class, NearMissEntity::class],
    version = 1,
    exportSchema = false
)
abstract class CollideDatabase : RoomDatabase() {

    abstract fun eventDao(): EventDao
    abstract fun runSummaryDao(): RunSummaryDao
    abstract fun nearMissDao(): NearMissDao

    companion object {
        @Volatile private var INSTANCE: CollideDatabase? = null

        fun getInstance(context: Context): CollideDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    CollideDatabase::class.java,
                    "collide_db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
