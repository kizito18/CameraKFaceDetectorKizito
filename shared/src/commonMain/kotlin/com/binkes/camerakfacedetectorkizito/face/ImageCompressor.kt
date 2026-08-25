package com.binkes.camerakfacedetectorkizito.face

import com.binkes.camerakfacedetectorkizito.data.PhotoData


// Image Compressor Interface
expect interface ImageCompressor {
    fun start(
        imageData: PhotoData,
        onProgress: (Float) -> Unit,
        onComplete: (PhotoData) -> Unit,
        onError: (Throwable) -> Unit
    )
    fun cancel()
}

// Factory to create compressor
expect fun createImageCompressor(): ImageCompressor


