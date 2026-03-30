package com.collide.app.domain.transforms

import com.collide.app.domain.model.TransformSpec

class IdentityTransform : ReversibleTransform {
    override val spec = TransformSpec(
        id = "identity",
        displayName = "Identity",
        estimatedCostMs = 0
    )

    override fun checkApplicability(input: ByteArray) = ApplicabilityStatus.APPLICABLE

    override fun encode(input: ByteArray) = TransformOutput(
        bytes = input.copyOf(),
        metadata = ByteArray(0)
    )

    override fun decode(encoded: ByteArray, metadata: ByteArray) = encoded.copyOf()

    override fun humanSummary() = "Identity (pass-through)"
}
