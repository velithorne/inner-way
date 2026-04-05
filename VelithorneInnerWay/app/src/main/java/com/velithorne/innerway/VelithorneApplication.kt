package com.velithorne.innerway

import android.app.Application
import androidx.work.Configuration
import com.velithorne.innerway.boot.BootSequence
import com.velithorne.innerway.memory.MemoryDatabase
import com.velithorne.innerway.memory.MemoryRepository
import com.velithorne.innerway.services.SpeciesCycleWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Application entry: wires persistent substrate (Room), asynchronous boot, and periodic cycles.
 */
class VelithorneApplication : Application(), Configuration.Provider {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()

    override fun onCreate() {
        super.onCreate()
        val repository = MemoryRepository(MemoryDatabase.get(this).memoryDao())
        applicationScope.launch {
            BootSequence.run(this@VelithorneApplication, repository)
        }
        SpeciesCycleWorker.schedule(this)
    }
}
