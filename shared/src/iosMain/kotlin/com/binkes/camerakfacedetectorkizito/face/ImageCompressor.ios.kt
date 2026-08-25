package com.binkes.camerakfacedetectorkizito.face

import com.binkes.camerakfacedetectorkizito.data.PhotoData
import com.binkes.camerakfacedetectorkizito.utill.ConstantObject
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSData
import platform.Foundation.create
import platform.UIKit.UIGraphicsBeginImageContextWithOptions
import platform.UIKit.UIGraphicsEndImageContext
import platform.UIKit.UIGraphicsGetImageFromCurrentImageContext
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.darwin.DISPATCH_QUEUE_PRIORITY_DEFAULT
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_global_queue
import platform.darwin.dispatch_get_main_queue
import platform.posix.memcpy

// iosMain

actual interface ImageCompressor {
    actual fun start(
        imageData: PhotoData,
        onProgress: (Float) -> Unit,
        onComplete: (PhotoData) -> Unit,
        onError: (Throwable) -> Unit
    )

    actual fun cancel()
}

actual fun createImageCompressor(): ImageCompressor {
    return IOSImageCompressor()
}





private class IOSImageCompressor : ImageCompressor {

    private var isCancelled = false

    override fun start(
        imageData: PhotoData,
        onProgress: (Float) -> Unit,
        onComplete: (PhotoData) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        isCancelled = false

        val backgroundQueue = dispatch_get_global_queue(
            DISPATCH_QUEUE_PRIORITY_DEFAULT.toLong(),
            0u
        )
        dispatch_async(backgroundQueue) {

            try {
                dispatch_async(dispatch_get_main_queue()) {
                    onProgress(0.05f)
                }

                val originalSize = imageData.bytes.size
                // println("🗜️ iOS Original: ${originalSize / 1000}KB")

                if (originalSize <= ConstantObject.MAX_IMAGE_SIZE_BYTES) {
                    dispatch_async(dispatch_get_main_queue()) {
                        onProgress(1f)
                        onComplete(imageData)
                    }
                    return@dispatch_async
                }

                val uiImage = UIImage.imageWithData(imageData.bytes.toNSData())
                    ?: run {
                        dispatch_async(dispatch_get_main_queue()) {
                            onComplete(imageData)
                        }
                        return@dispatch_async
                    }

                dispatch_async(dispatch_get_main_queue()) {
                    onProgress(0.2f)
                }

                var bestBytes: ByteArray = imageData.bytes

                // ── QUALITY REDUCTION ─────────────────────
                val qualitySteps = listOf(0.95, 0.9, 0.85, 0.8, 0.75, 0.7, 0.65, 0.6, 0.55, 0.5)
                var pass = 0

                for (q in qualitySteps) {
                    if (isCancelled) return@dispatch_async

                    val data = UIImageJPEGRepresentation(uiImage, q)
                    val bytes = data?.toByteArray() ?: continue

                    bestBytes = bytes
                    pass++

                    val progress = 0.2f + (pass.toFloat() / qualitySteps.size * 0.4f)

                    dispatch_async(dispatch_get_main_queue()) {
                        onProgress(progress)
                    }

                    if (bytes.size <= ConstantObject.MAX_IMAGE_SIZE_BYTES) break
                }

                dispatch_async(dispatch_get_main_queue()) {
                    onProgress(0.6f)
                }

                // ── SCALING ───────────────────────────────
                if (bestBytes.size > ConstantObject.MAX_IMAGE_SIZE_BYTES) {

                    val scaleSteps = listOf(0.9, 0.8, 0.7, 0.6, 0.5, 0.4, 0.3)
                    var scalePass = 0

                    for (scale in scaleSteps) {
                        if (isCancelled) return@dispatch_async

                        val resizeImage = uiImage.resize(scale)

                        val qualities = listOf(0.85, 0.7, 0.55, 0.4, 0.25)

                        for (q in qualities) {
                            val data = UIImageJPEGRepresentation(resizeImage, 0.85)
                            val bytes = data?.toByteArray() ?: continue

                            bestBytes = bytes

                            if (bytes.size <= ConstantObject.MAX_IMAGE_SIZE_BYTES) break
                        }

                        scalePass++
                        val progress = 0.6f + (scalePass.toFloat() / scaleSteps.size * 0.35f)

                        dispatch_async(dispatch_get_main_queue()) {
                            onProgress(progress)
                        }

                        if (bestBytes.size <= ConstantObject.MAX_IMAGE_SIZE_BYTES) break
                    }
                }

                val result = PhotoData(
                    bytes = bestBytes,
                    fileName = imageData.fileName.replaceAfterLast(".", "jpg"),
                    mimeType = "image/jpeg"
                )

                dispatch_async(dispatch_get_main_queue()) {
                    onProgress(1f)
                    onComplete(result)
                }

            } catch (e: Throwable) {
                dispatch_async(dispatch_get_main_queue()) {
                    onError(e)
                }
            }
        }
    }

    override fun cancel() {
        isCancelled = true
    }
}



@OptIn(ExperimentalForeignApi::class)
private fun UIImage.resize(scale: Double): UIImage {

    val newSize = this.size.useContents {
        CGSizeMake(
            width * scale,
            height * scale
        )
    }

    UIGraphicsBeginImageContextWithOptions(
        newSize,
        false,
        1.0
    )

    val rect = newSize.useContents {
        CGRectMake(0.0, 0.0, width, height)
    }

    this.drawInRect(rect)

    val result = UIGraphicsGetImageFromCurrentImageContext()
    UIGraphicsEndImageContext()

    return result ?: this
}


// Helper extension to convert ByteArray to NSData
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private fun ByteArray.toNSData(): NSData {
    return this.usePinned {
        NSData.create(
            bytes = it.addressOf(0),
            length = this.size.toULong()
        )
    }
}

// Helper extension to convert NSData to ByteArray
@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    return ByteArray(this.length.toInt()).apply {
        usePinned { pinned ->
            memcpy(pinned.addressOf(0), this@toByteArray.bytes!!, this@toByteArray.length)
        }
    }
}


