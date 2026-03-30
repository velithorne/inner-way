package com.collide.app.engine

import com.collide.app.domain.engine.collision.RecipeLibrary
import com.collide.app.domain.engine.collision.RecipeSerializer
import com.collide.app.domain.model.CollisionMode
import com.collide.app.domain.model.CollisionRecipe
import com.collide.app.domain.model.RecipeOperation
import org.junit.Assert.*
import org.junit.Test

class RecipeSerializerTest {

    private val serializer = RecipeSerializer()

    @Test
    fun `serializes and deserializes single-input recipe`() {
        val recipe = CollisionRecipe(
            id = "test01",
            name = "Test Recipe",
            description = "For testing",
            mode = CollisionMode.SINGLE_INPUT_MUTATION,
            operations = listOf(
                RecipeOperation(RecipeOperation.STRIP_WHITESPACE_COMMENTS),
                RecipeOperation(RecipeOperation.ABSTRACT_IDENTIFIERS)
            )
        )
        val json = serializer.serialize(recipe)
        val restored = serializer.deserialize(json)

        assertNotNull("deserialized recipe should not be null", restored)
        assertEquals("id should match", recipe.id, restored?.id)
        assertEquals("name should match", recipe.name, restored?.name)
        assertEquals("mode should match", recipe.mode, restored?.mode)
        assertEquals("operation count should match", recipe.operations.size, restored?.operations?.size)
        assertEquals("first op should match", recipe.operations[0].operationId, restored?.operations?.get(0)?.operationId)
    }

    @Test
    fun `serializes and deserializes dual-input recipe`() {
        val recipe = CollisionRecipe(
            id = "d01",
            name = "Branch Splice",
            description = "Splice branches",
            mode = CollisionMode.DUAL_INPUT_RECOMBINATION,
            operations = listOf(
                RecipeOperation(RecipeOperation.SPLICE_BRANCH_B_INTO_A),
                RecipeOperation(RecipeOperation.COMPARE_STRUCTURE)
            )
        )
        val json = serializer.serialize(recipe)
        val restored = serializer.deserialize(json)

        assertNotNull(restored)
        assertEquals(CollisionMode.DUAL_INPUT_RECOMBINATION, restored?.mode)
    }

    @Test
    fun `serializes recipe with params`() {
        val recipe = CollisionRecipe(
            id = "s05",
            name = "Op Sub",
            description = "Operator substitution",
            mode = CollisionMode.SINGLE_INPUT_MUTATION,
            operations = listOf(
                RecipeOperation(RecipeOperation.SUBSTITUTE_OPERATORS, mapOf("family" to "arithmetic"))
            )
        )
        val json = serializer.serialize(recipe)
        val restored = serializer.deserialize(json)

        assertNotNull(restored)
        assertEquals("family param should be preserved",
            "arithmetic",
            restored?.operations?.get(0)?.params?.get("family"))
    }

    @Test
    fun `returns null for invalid json`() {
        val result = serializer.deserialize("not valid json {{{")
        assertNull("invalid json should return null", result)
    }

    @Test
    fun `serializable key is stable`() {
        val recipe = RecipeLibrary.SINGLE_INPUT_RECIPES.first()
        val key1 = recipe.toSerializableKey()
        val key2 = recipe.toSerializableKey()
        assertEquals("serializable key should be stable", key1, key2)
    }

    @Test
    fun `all library recipes are serializable`() {
        val allRecipes = RecipeLibrary.SINGLE_INPUT_RECIPES + RecipeLibrary.DUAL_INPUT_RECIPES
        for (recipe in allRecipes) {
            val json = serializer.serialize(recipe)
            val restored = serializer.deserialize(json)
            assertNotNull("recipe '${recipe.id}' should be serializable", restored)
            assertEquals("recipe '${recipe.id}' id should match after round-trip", recipe.id, restored?.id)
        }
    }
}
