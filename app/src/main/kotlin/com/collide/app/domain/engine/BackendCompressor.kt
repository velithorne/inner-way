package com.collide.app.domain.engine

import com.collide.app.domain.model.BackendCompressor
import java.io.ByteArrayOutputStream
import java.util.zip.Deflater
import java.util.zip.Inflater

object BackendCompressorEngine {

    fun compress(input: ByteArray, backend: BackendCompressor): ByteArray {
        val level = when (backend) {
            BackendCompressor.DEFLATE_DEFAULT -> Deflater.DEFAULT_COMPRESSION
            BackendCompressor.DEFLATE_BEST_COMPRESSION -> Deflater.BEST_COMPRESSION
        }
        val deflater = Deflater(level, true)
        deflater.setInput(input)
        deflater.finish()
        val baos = ByteArrayOutputStream(input.size)
        val buf = ByteArray(8192)
        while (!deflater.finished()) {
            val count = deflater.deflate(buf)
            baos.write(buf, 0, count)
        }
        deflater.end()
        return baos.toByteArray()
    }

    fun decompress(compressed: ByteArray, expectedSize: Int): ByteArray {
        val inflater = Inflater(true)
        inflater.setInput(compressed)
        val out = ByteArray(expectedSize)
        val n = inflater.inflate(out)
        inflater.end()
        if (n != expectedSize) {
            throw IllegalStateException("Decompressed $n bytes, expected $expectedSize")
        }
        return out
    }

    /**
     * Decompress without knowing expected size (iterative grow strategy).
     */
    fun decompressUnknownSize(compressed: ByteArray): ByteArray {
        val inflater = Inflater(true)
        inflater.setInput(compressed)
        val baos = ByteArrayOutputStream(compressed.size * 2)
        val buf = ByteArray(8192)
        while (!inflater.finished()) {
            val count = inflater.inflate(buf)
            if (count == 0 && !inflater.finished()) break
            baos.write(buf, 0, count)
        }
        inflater.end()
        return baos.toByteArray()
    }
}
