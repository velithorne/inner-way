package com.collide.app.domain.recipes

import com.collide.app.domain.model.BackendCompressor
import com.collide.app.domain.model.RecipeSpec
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

object RecipeSerializer {
    private val gson = Gson()

    private data class RecipeJson(
        @SerializedName("id") val id: String,
        @SerializedName("transforms") val transforms: List<String>,
        @SerializedName("backend") val backend: String
    )

    fun serialize(recipe: RecipeSpec): String {
        val json = RecipeJson(
            id = recipe.id,
            transforms = recipe.transformIds,
            backend = recipe.backend.name
        )
        return gson.toJson(json)
    }

    fun deserialize(json: String): RecipeSpec {
        val rj = gson.fromJson(json, RecipeJson::class.java)
        return RecipeSpec(
            id = rj.id,
            transformIds = rj.transforms,
            backend = BackendCompressor.valueOf(rj.backend)
        )
    }
}
