package com.falcor.civilization.data

import android.content.Context
import androidx.room.Room

object FalcorDatabaseProvider {
    fun get(context: Context): FalcorDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            FalcorDatabase::class.java,
            "falcor_civilization_db"
        ).build()
    }
}
