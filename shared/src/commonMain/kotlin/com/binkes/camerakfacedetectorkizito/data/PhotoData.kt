package com.binkes.camerakfacedetectorkizito.data



data class PhotoData(
    val bytes: ByteArray,
    val fileName: String,
    val mimeType: String = "image/jpeg"
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as PhotoData
        return bytes.contentEquals(other.bytes) &&
                fileName == other.fileName &&
                mimeType == other.mimeType
    }

    override fun hashCode(): Int {
        var result = bytes.contentHashCode()
        result = 31 * result + fileName.hashCode()
        result = 31 * result + mimeType.hashCode()
        return result
    }
}

