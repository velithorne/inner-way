package com.aura.shell.knowledge

import androidx.room.Room
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import com.aura.shell.knowledge.db.KnowledgeDatabase

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class KnowledgeRepositorySearchTest {

    private lateinit var db: KnowledgeDatabase
    private lateinit var repo: KnowledgeRepository

    @Before
    fun setup() {
        val ctx = RuntimeEnvironment.getApplication()
        db = Room.inMemoryDatabaseBuilder(ctx, KnowledgeDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = KnowledgeRepository(ctx, db)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun fts_findsByTitle() = runBlocking {
        repo.insertNote("Launcher plan", "phase 4 knowledge")
        val hits = repo.searchAll("launcher", 10)
        assertTrue(hits.isNotEmpty())
    }

    @Test
    fun delete_removes() = runBlocking {
        val id = repo.insertNote("T", "x")
        repo.delete(id)
        assertEquals(null, repo.getById(id))
    }
}
