package com.collide.app.domain.evaluator

import com.collide.app.domain.engine.BackendCompressorEngine
import com.collide.app.domain.model.BaselineResult
import com.collide.app.domain.model.BaselineStrategy
import com.collide.app.domain.model.BackendCompressor

class BaselineEvaluator {

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
