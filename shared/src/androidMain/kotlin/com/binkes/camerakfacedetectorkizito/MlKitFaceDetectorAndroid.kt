package com.binkes.camerakfacedetectorkizito

import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import com.binkes.camerakfacedetectorkizito.face.FaceAnalysisState
import com.binkes.camerakfacedetectorkizito.face.FaceDetector
import com.binkes.camerakfacedetectorkizito.face.ImageFrame
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.util.concurrent.atomic.AtomicBoolean


/*
private const val TAG = "MlKitFace"

class MlKitFaceDetectorAndroid : FaceDetector {

    private val detector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_NONE)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .setMinFaceSize(0.1f)
            .build(),
    )

    @Volatile
    private var latestResult: FaceAnalysisState? = null
    private val inFlight = AtomicBoolean(false)

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(frame: ImageFrame): FaceAnalysisState? {
        // Drop frame if a previous detection is still in flight — never block the analyzer thread.
        if (!inFlight.compareAndSet(false, true)) {
            return latestResult
        }

        val bitmap = try {
            val raw = frame.imageProxy.toBitmap()
            val rotation = frame.imageProxy.imageInfo.rotationDegrees
            if (rotation == 0) raw else raw.rotated(rotation)
        } catch (e: Throwable) {
            Log.w(TAG, "toBitmap failed", e)
            inFlight.set(false)
            return latestResult
        }

        detector.process(InputImage.fromBitmap(bitmap, 0))
            .addOnSuccessListener { faces ->
                latestResult = faces.firstOrNull()?.toState()
                inFlight.set(false)
            }
            .addOnFailureListener { error ->
                Log.w(TAG, "Detection failed", error)
                inFlight.set(false)
            }

        return latestResult
    }

    override fun close() {
        detector.close()
        latestResult = null
    }

    private fun Face.toState(): FaceAnalysisState {
        val smile = smilingProbability ?: 0f
        val leftEye = leftEyeOpenProbability ?: 1f
        val rightEye = rightEyeOpenProbability ?: 1f
        val tilt = headEulerAngleZ

        return FaceAnalysisState(
            isSmiling = smile > 0.5f,
            leftEyeOpen = leftEye > 0.5f,
            rightEyeOpen = rightEye > 0.5f,
            headTiltDegrees = tilt,
            winkLeft = leftEye < 0.3f && rightEye > 0.5f,
            winkRight = rightEye < 0.3f && leftEye > 0.5f,
        )
    }

    private fun Bitmap.rotated(degrees: Int): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
    }
}

 */

private const val TAG = "MlKitFace"

class MlKitFaceDetectorAndroid : FaceDetector {

    private val detector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_NONE)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .setMinFaceSize(0.1f)
            .build(),
    )

    @Volatile
    private var latestResult: FaceAnalysisState? = null
    private val inFlight = AtomicBoolean(false)

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(frame: ImageFrame): FaceAnalysisState? {
        if (!inFlight.compareAndSet(false, true)) {
            return latestResult
        }

        val bitmap = try {
            val raw = frame.imageProxy.toBitmap()
            val rotation = frame.imageProxy.imageInfo.rotationDegrees
            if (rotation == 0) raw else raw.rotated(rotation)
        } catch (e: Throwable) {
            Log.w(TAG, "toBitmap failed", e)
            inFlight.set(false)
            return latestResult
        }

        // bitmap is ALREADY rotated at this point, so its width/height are the
        // correct "upright" frame dimensions that face.boundingBox is measured against.
        val frameWidth = bitmap.width
        val frameHeight = bitmap.height

        detector.process(InputImage.fromBitmap(bitmap, 0))
            .addOnSuccessListener { faces ->
                latestResult = faces.firstOrNull()?.toState(frameWidth, frameHeight)
            }
            .addOnFailureListener { error ->
                Log.w(TAG, "Detection failed", error)
            }
            .addOnCompleteListener {
                inFlight.set(false)
            }

        return latestResult
    }

    override fun close() {
        detector.close()
        latestResult = null
    }

    private fun Face.toState(frameWidth: Int, frameHeight: Int): FaceAnalysisState {
        val smile = smilingProbability ?: 0f
        val leftEye = leftEyeOpenProbability ?: 1f
        val rightEye = rightEyeOpenProbability ?: 1f
        val tilt = headEulerAngleZ

        val box = boundingBox
        val faceWidthRatio = box.width().toFloat() / frameWidth.toFloat()

        val faceCenterX = box.centerX().toFloat()
        val faceCenterY = box.centerY().toFloat()
        val horizontalOffset = kotlin.math.abs(faceCenterX - frameWidth / 2f) / frameWidth
        val verticalOffset = kotlin.math.abs(faceCenterY - frameHeight / 2f) / frameHeight

        val inFrameBounds = box.left >= 0 && box.top >= 0 &&
                box.right <= frameWidth && box.bottom <= frameHeight

        Log.d(TAG, "box=$box frame=${frameWidth}x$frameHeight widthRatio=$faceWidthRatio hOff=$horizontalOffset vOff=$verticalOffset yaw=$headEulerAngleY pitch=$headEulerAngleX inBounds=$inFrameBounds")

        return FaceAnalysisState(
            isSmiling = smile > 0.5f,
            leftEyeOpen = leftEye > 0.5f,
            rightEyeOpen = rightEye > 0.5f,
            headTiltDegrees = tilt,
            winkLeft = leftEye < 0.3f && rightEye > 0.5f,
            winkRight = rightEye < 0.3f && leftEye > 0.5f,
            faceWidthRatio = faceWidthRatio,
            horizontalOffset = horizontalOffset,
            verticalOffset = verticalOffset,
            yawDegrees = headEulerAngleY,
            pitchDegrees = headEulerAngleX,
            inFrameBounds = inFrameBounds,
        )
    }

    private fun Bitmap.rotated(degrees: Int): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
    }
}
