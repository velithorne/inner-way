package com.falcor.civilization

import android.app.Application
import com.falcor.civilization.data.FalcorDatabase
import com.falcor.civilization.data.FalcorDatabaseProvider
import java.io.File

class FalcorApp : Application() {
    val database: FalcorDatabase by lazy {
        FalcorDatabaseProvider.get(this)
    }
    val appFilesDir: File by lazy {
        getExternalFilesDir(null) ?: filesDir
    }
}
