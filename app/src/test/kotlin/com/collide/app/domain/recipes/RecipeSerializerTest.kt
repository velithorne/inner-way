package com.collide.app.domain.recipes

import com.collide.app.domain.model.BackendCompressor
import com.collide.app.domain.model.RecipeSpec
import org.junit.Assert.*
import org.junit.Test

class RecipeSerializerTest {

    @Test
    fun `serialize and deserialize round trip`() {
        val recipe = RecipeSpec(
            id = "test_r1",
            transformIds = listOf("delta8", "xor_prev"),
            backend = BackendCompressor.DEFLATE_DEFAULT
        )
        val json = RecipeSerializer.serialize(recipe)
        val restored = RecipeSerializer.deserialize(json)

        assertEquals(recipe.id, restored.id)
        assertEquals(recipe.transformIds, restored.transformIds)
        assertEquals(recipe.backend, restored.backend)
    }

    @Test
    fun `single transform serializes correctly`() {
        val recipe = RecipeSpec("r2", listOf("identity"), BackendCompressor.DEFLATE_BEST_COMPRESSION)
        val json = RecipeSerializer.serialize(recipe)
        val restored = RecipeSerializer.deserialize(json)
        assertEquals(1, restored.transformIds.size)
        assertEquals("identity", restored.transformIds[0])
    }

    @Test
    fun `serialized json is valid json string`() {
        val recipe = RecipeSpec("r3", listOf("block_shuffle_4"), BackendCompressor.DEFLATE_DEFAULT)
        val json = RecipeSerializer.serialize(recipe)
        assertTrue(json.startsWith("{"))
        assertTrue(json.endsWith("}"))
    }

    @Test
    fun `empty transform list serializes correctly`() {
        val recipe = RecipeSpec("r4", emptyList(), BackendCompressor.DEFLATE_DEFAULT)
        val json = RecipeSerializer.serialize(recipe)
        val restored = RecipeSerializer.deserialize(json)
        assertTrue(restored.transformIds.isEmpty())
    }
}
