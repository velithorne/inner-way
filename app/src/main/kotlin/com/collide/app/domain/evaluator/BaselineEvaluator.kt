package com.collide.app.domain.evaluator

import com.collide.app.domain.engine.BackendCompressorEngine
import com.collide.app.domain.model.BaselineResult
import com.collide.app.domain.model.BaselineStrategy
import com.collide.app.domain.model.BackendCompressor

class BaselineEvaluator {

    /**
     * Compute baseline.
     *
     * Phase 2 note: baseline is measured as raw compressed bytes only (no metadata overhead),
     * because a baseline has no transform chain to account for. The candidate's totalEncodedSize
     * (which includes all overhead) is compared against this baseline, making the comparison
     * deliberately conservative for the candidate — a candidate must beat the baseline even
     * after paying all its per-transform metadata costs.
     */
    fun evaluate(input: ByteArray, strategy: BaselineStrategy): BaselineResult {
        val start = System.currentTimeMillis()
        val backend = when (strategy) {
            BaselineStrategy.RAW_DEFLATE -> BackendCompressor.DEFLATE_DEFAULT
            BaselineStrategy.DEFLATE_BEST_COMPRESSION -> BackendCompressor.DEFLATE_BEST_COMPRESSION
        }
        val compressed = BackendCompressorEngine.compress(input, backend)
        val elapsed = System.currentTimeMillis() - start
        return BaselineResult(
            strategy = strategy,
            encodedSize = compressed.size.toLong(),
            elapsedMs = elapsed
        )
    }
}
