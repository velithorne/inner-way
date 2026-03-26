package com.aura.shell.knowledge

import androidx.room.Room
import com.aura.shell.command.CommandDispatch
import com.aura.shell.command.CommandRouter
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
class TagsAndRelationsTest {

    private lateinit var db: KnowledgeDatabase
    private lateinit var repo: KnowledgeRepository
    private val router = CommandRouter()

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
    fun addTag_thenSearchByTag() = runBlocking {
        val id = repo.insertNote("Phase 9", "benchmark ideas")
        repo.addTagToItem(id, "benchmark")
        val tagged = repo.searchByTag("benchmark", 20)
        assertEquals(1, tagged.size)
        assertTrue(tagged[0].tagKeys.contains("benchmark"))
    }

    @Test
    fun removeTag() = runBlocking {
        val id = repo.insertNote("A", "b")
        repo.addTagToItem(id, "x")
        repo.removeTagFromItem(id, "x")
        assertTrue(repo.getTagKeysForItem(id).isEmpty())
    }

    @Test
    fun manualLink_surfacesAsRelated() = runBlocking {
        val a = repo.insertNote("One", "alpha beta")
        val b = repo.insertNote("Two", "gamma")
        repo.linkItemsManually(a, b)
        val rel = repo.relatedFor(a, 10)
        assertTrue(rel.any { it.id == b && it.strength == RelatedStrength.MANUAL_LINK })
    }

    @Test
    fun sharedTag_infersRelated() = runBlocking {
        val a = repo.insertNote("CSV data", "rows")
        val b = repo.insertNote("More", "rows two")
        repo.addTagToItem(a, "bench")
        repo.addTagToItem(b, "bench")
        val rel = repo.relatedFor(a, 10)
        assertTrue(rel.any { it.id == b })
    }

    @Test
    fun showItemsTagged_opensTagRoute() = runBlocking {
        val d = router.route(
            "show items tagged benchmark",
            emptyList(),
            emptyList(),
            knowledgeRepository = repo,
        )
        assertTrue(d is CommandDispatch.OpenKnowledge && (d as CommandDispatch.OpenKnowledge).path == "tag:benchmark")
    }

    @Test
    fun addTagCurrent_requiresContext() = runBlocking {
        KnowledgeContextStore.setCurrentKnowledgeItem(null)
        val d = router.route(
            "add tag research",
            emptyList(),
            emptyList(),
            knowledgeRepository = repo,
        )
        assertTrue(d is CommandDispatch.Unknown)
    }
}
