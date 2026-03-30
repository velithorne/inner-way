package com.collide.app.domain.engine.collision

import com.collide.app.domain.model.CollisionMode
import com.collide.app.domain.model.CollisionRecipe
import com.collide.app.domain.model.RecipeOperation
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Serializes and deserializes CollisionRecipe for persistence and replay.
 * Uses Gson for JSON; all fields are stable across versions.
 */
class RecipeSerializer {

    private val gson = Gson()

    fun serialize(recipe: CollisionRecipe): String {
        return gson.toJson(RecipeJson(
            id = recipe.id,
            name = recipe.name,
            description = recipe.description,
            mode = recipe.mode.name,
            operations = recipe.operations.map { OpJson(it.operationId, it.params) },
            seed = recipe.seed
        ))
    }

    fun deserialize(json: String): CollisionRecipe? {
        return try {
            val r = gson.fromJson(json, RecipeJson::class.java)
            CollisionRecipe(
                id = r.id,
                name = r.name,
                description = r.description,
                mode = CollisionMode.valueOf(r.mode),
                operations = r.operations.map { RecipeOperation(it.operationId, it.params) },
                seed = r.seed
            )
        } catch (e: Exception) {
            null
        }
    }

    private data class RecipeJson(
        val id: String,
        val name: String,
        val description: String,
        val mode: String,
        val operations: List<OpJson>,
        val seed: Long
    )

    private data class OpJson(
        val operationId: String,
        val params: Map<String, String>
    )
}
