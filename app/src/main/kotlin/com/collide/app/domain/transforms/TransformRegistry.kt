package com.collide.app.domain.transforms

object TransformRegistry {
    val all: List<ReversibleTransform> = listOf(
        IdentityTransform(),
        ByteRunRLETransform(),
        ZeroRunRLETransform(),
        Delta8Transform(),
        XorPrevByteTransform(),
        FixedBlockShuffleTransform(blockSize = 4),
        FixedBlockShuffleTransform(blockSize = 8),
        MoveToFrontTransform()
    )

    val allExcludingIdentity: List<ReversibleTransform> = all.filter { it.spec.id != "identity" }

    fun byId(id: String): ReversibleTransform? = all.firstOrNull { it.spec.id == id }
}
