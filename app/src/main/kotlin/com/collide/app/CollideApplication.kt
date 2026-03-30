package com.collide.app

import android.app.Application
import com.collide.app.data.db.AppDatabase
import com.collide.app.data.repository.EventRepository
import com.collide.app.data.settings.CollideSettingsStore

class CollideApplication : Application() {
    val database by lazy { AppDatabase.getInstance(this) }
    val eventRepository by lazy { EventRepository(database) }
    val settingsStore by lazy { CollideSettingsStore(this) }
}
