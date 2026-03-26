package com.aura.shell

import android.app.Application
import com.aura.shell.knowledge.db.KnowledgeDatabase

class AuraApplication : Application() {

    val knowledgeDatabase: KnowledgeDatabase by lazy { KnowledgeDatabase.create(this) }
    val knowledgeRepository: com.aura.shell.knowledge.KnowledgeRepository by lazy { com.aura.shell.knowledge.KnowledgeRepository(this) }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: AuraApplication
            private set
    }
}
