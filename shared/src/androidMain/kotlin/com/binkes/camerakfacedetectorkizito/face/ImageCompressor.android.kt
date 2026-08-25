package com.binkes.camerakfacedetectorkizito.face

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.binkes.camerakfacedetectorkizito.data.PhotoData
import com.binkes.camerakfacedetectorkizito.utill.ConstantObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream


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
    return AndroidImageCompressor()
}


class AndroidImageCompressor : ImageCompressor {
    private var currentJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun start(
        imageData: PhotoData,
        onProgress: (Float) -> Unit,
        onComplete: (PhotoData) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        cancel()

        currentJob = scope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) { onProgress(0.05f) }

                val originalSize = imageData.bytes.size
                //println("🗜️ Original: ${originalSize / 1000}KB")

                // ── Already small enough — skip compression ────
                if (originalSize <= ConstantObject.MAX_IMAGE_SIZE_BYTES) {
                    //  println("🗜️ Already under limit, skipping compression")
                    withContext(Dispatchers.Main) {
                        onProgress(1f)
                        onComplete(imageData)
                    }
                    return@launch
                }

                // ── Step 1: Decode full quality ────────────────
                val originalBitmap = BitmapFactory.decodeByteArray(
                    imageData.bytes, 0, imageData.bytes.size
                ) ?: run {
                    withContext(Dispatchers.Main) { onComplete(imageData) }
                    return@launch
                }

                withContext(Dispatchers.Main) { onProgress(0.2f) }

                //println("🗜️ Dimensions: ${originalBitmap.width}x${originalBitmap.height}")

                // ── Step 2: Try quality reduction FIRST ────────
                // before scaling — preserve dimensions as long as possible
                var bestBytes: ByteArray = imageData.bytes
                var bestBitmap = originalBitmap

                val highQualitySteps = listOf(95, 90, 85, 80, 75, 70, 65, 60, 55, 50)
                var pass = 0

                for (q in highQualitySteps) {
                    val stream = ByteArrayOutputStream()
                    withContext(Dispatchers.IO) {
                        bestBitmap.compress(Bitmap.CompressFormat.JPEG, q, stream)
                    }
                    bestBytes = stream.toByteArray()
                    stream.close()
                    pass++

                    val progressValue = 0.2f + (pass.toFloat() / highQualitySteps.size * 0.4f)
                    withContext(Dispatchers.Main) { onProgress(progressValue) }

                    // println("🗜️ Quality $q%: ${bestBytes.size / 1000}KB")

                    if (bestBytes.size <= ConstantObject.MAX_IMAGE_SIZE_BYTES) {
                        // println("🗜️ ✅ Achieved target at quality=$q%")
                        break
                    }
                }

                withContext(Dispatchers.Main) { onProgress(0.6f) }

                // ── Step 3: Scale only if quality alone wasn't enough
                if (bestBytes.size > ConstantObject.MAX_IMAGE_SIZE_BYTES) {
                    // println("🗜️ Quality reduction not enough, starting gentle scaling...")

                    // scale in small steps — preserve as much size as possible
                    val scaleSteps = listOf(0.9f, 0.8f, 0.7f, 0.6f, 0.5f, 0.4f, 0.3f)
                    var scalePass = 0

                    for (scale in scaleSteps) {
                        val newWidth  = (originalBitmap.width  * scale).toInt()
                        val newHeight = (originalBitmap.height * scale).toInt()

                        val scaledBitmap = Bitmap.createScaledBitmap(
                            originalBitmap, newWidth, newHeight, true
                        )

                        // try multiple qualities at this scale
                        val qualitiesAtScale = listOf(85, 70, 55, 40, 25)
                        for (q in qualitiesAtScale) {
                            val stream = ByteArrayOutputStream()
                            withContext(Dispatchers.IO) {
                                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, q, stream)
                            }
                            val bytes = stream.toByteArray()
                            stream.close()

                            // println("🗜️ Scale ${(scale * 100).toInt()}% at quality $q%: ${bytes.size / 1000}KB")

                            if (bytes.size <= ConstantObject.MAX_IMAGE_SIZE_BYTES) {
                                bestBytes = bytes
                                //println("🗜️ ✅ Achieved target at scale=${(scale * 100).toInt()}% quality=$q%")
                                scaledBitmap.recycle()

                                scalePass++
                                val progressValue = 0.6f + (scalePass.toFloat() / scaleSteps.size * 0.35f)
                                withContext(Dispatchers.Main) { onProgress(progressValue) }

                                break
                            }

                            bestBytes = bytes // keep smallest so far
                        }

                        scaledBitmap.recycle()

                        if (bestBytes.size <= ConstantObject.MAX_IMAGE_SIZE_BYTES) break

                        scalePass++
                        val progressValue = 0.6f + (scalePass.toFloat() / scaleSteps.size * 0.35f)
                        withContext(Dispatchers.Main) { onProgress(progressValue) }
                    }
                }

                originalBitmap.recycle()

                //println("🗜️ Final: ${bestBytes.size / 1000}KB — target was ${ConstantObject.MAX_IMAGE_SIZE_BYTES / 1000}KB")

                val result = PhotoData(
                    bytes    = bestBytes,
                    fileName = imageData.fileName.replaceAfterLast(".", "jpg"),
                    mimeType = "image/jpeg"
                )

                withContext(Dispatchers.Main) {
                    onProgress(1f)
                    onComplete(result)
                }

            } catch (e: Exception) {
                println("🗜️ Compression error: ${e.message}")
                withContext(Dispatchers.Main) { onError(e) }
            }
        }
    }

    override fun cancel() {
        currentJob?.cancel()
        currentJob = null
    }
}


private fun getFileNameFromUri(context: Context, uri: android.net.Uri): String {
    var fileName: String? = null
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val displayNameColumn = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            fileName = cursor.getString(displayNameColumn)
        }
    }
    return fileName ?: "image_${System.currentTimeMillis()}.jpg"
}
