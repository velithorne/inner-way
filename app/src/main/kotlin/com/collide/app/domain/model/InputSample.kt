package com.collide.app.domain.model

data class InputSample(
    val fileName: String,
    val filePath: String?,
    val bytes: ByteArray,
    val mimeType: String? = null
) {
    val size: Int get() = bytes.size

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as InputSample
        if (fileName != other.fileName) return false
        if (!bytes.contentEquals(other.bytes)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = fileName.hashCode()
        result = 31 * result + bytes.contentHashCode()
        return result
    }
}
