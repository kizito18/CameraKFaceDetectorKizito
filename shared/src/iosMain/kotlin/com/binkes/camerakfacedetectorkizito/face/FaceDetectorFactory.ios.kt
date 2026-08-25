package com.binkes.camerakfacedetectorkizito.face

import com.binkes.camerakfacedetectorkizito.FaceDetectorBridge
import com.binkes.camerakfacedetectorkizito.SwiftFaceDetector
import com.kashif.cameraK.controller.CameraController
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFoundation.AVCaptureConnection
import platform.AVFoundation.AVCaptureOutput
import platform.AVFoundation.AVCaptureVideoDataOutput
import platform.AVFoundation.AVCaptureVideoDataOutputSampleBufferDelegateProtocol
import platform.AVFoundation.AVCaptureVideoOrientationPortrait
import platform.AVFoundation.AVMediaTypeVideo
import platform.CoreMedia.CMSampleBufferRef
import platform.CoreVideo.kCVPixelBufferPixelFormatTypeKey
import platform.CoreVideo.kCVPixelFormatType_32BGRA
import platform.darwin.NSObject
import platform.darwin.dispatch_queue_create
import kotlin.concurrent.Volatile

//ios main

actual object FaceDetectorFactory {
    actual fun create(kind: DetectorKind): FaceDetector {
        val swift = when (kind) {
            DetectorKind.ML_KIT -> requireNotNull(FaceDetectorBridge.mlKit) {
                "ML Kit detector not registered. Call FaceDetectorBridge.mlKit = MLKitFaceDetectorImpl() from iOSApp."
            }
            DetectorKind.MEDIAPIPE -> requireNotNull(FaceDetectorBridge.mediaPipe) {
                "MediaPipe detector not registered. Call FaceDetectorBridge.mediaPipe = MediaPipeFaceDetectorImpl() from iOSApp."
            }
        }
        return SwiftBackedFaceDetector(swift)
    }
}

private class SwiftBackedFaceDetector(private val swift: SwiftFaceDetector) : FaceDetector {
    override fun analyze(frame: ImageFrame): FaceAnalysisState? = swift.analyzeFrame(frame)
    override fun close() = swift.close()
}






@OptIn(ExperimentalForeignApi::class)
actual fun attachFaceAnalyzer(
    controller: CameraController,
    onResult: (FaceAnalysisState?) -> Unit,
): AnalyzerHandle {
    val state = AnalyzerState()

    val delegate: AVCaptureVideoDataOutputSampleBufferDelegateProtocol =
        FaceAnalyzerDelegate { buffer ->
            val detector = state.detector ?: return@FaceAnalyzerDelegate
            try {
                val result = detector.analyze(ImageFrame(buffer))
                onResult(result)
            } catch (t: Throwable) {
                onResult(null)
            }
        }

    val queue = dispatch_queue_create("ly.tahaben.face_analyzer", null)
    val output = AVCaptureVideoDataOutput()
    output.setSampleBufferDelegate(delegate, queue)
    output.videoSettings = mapOf(
        kCVPixelBufferPixelFormatTypeKey to kCVPixelFormatType_32BGRA,
    )

    controller.queueConfigurationChange {
        controller.safeAddOutput(output)
        // Force portrait orientation on the data output's connection so the buffer
        // arrives upright. The detectors can then use UIImage.Orientation.up.
        val connection = output.connectionWithMediaType(AVMediaTypeVideo)
        if (connection != null && connection.isVideoOrientationSupported()) {
            connection.setVideoOrientation(AVCaptureVideoOrientationPortrait)
            println("FaceAnalyzer[iOS]: forced AVCaptureConnection videoOrientation = portrait")
        } else {
            println("FaceAnalyzer[iOS]: connection=${connection != null}, orientationSupported=${connection?.isVideoOrientationSupported()}")
        }
    }

    return IosAnalyzerHandle(output, delegate, state)
}

private class AnalyzerState {
    @Volatile
    var detector: FaceDetector? = null
}

@OptIn(ExperimentalForeignApi::class)
private class IosAnalyzerHandle(
    private val output: AVCaptureVideoDataOutput,
    @Suppress("unused")
    private val delegateKeepAlive: AVCaptureVideoDataOutputSampleBufferDelegateProtocol,
    private val state: AnalyzerState,
) : AnalyzerHandle {
    override fun setDetector(detector: FaceDetector?) {
        state.detector = detector
    }
    override fun detach() {
        state.detector = null
        output.setSampleBufferDelegate(null, null)
    }
}

@OptIn(ExperimentalForeignApi::class)
private class FaceAnalyzerDelegate(
    private val onFrame: (CMSampleBufferRef) -> Unit,
) : NSObject(), AVCaptureVideoDataOutputSampleBufferDelegateProtocol {
    private var loggedFirstFrame = false
    override fun captureOutput(
        output: AVCaptureOutput,
        didOutputSampleBuffer: CMSampleBufferRef?,
        fromConnection: AVCaptureConnection,
    ) {
        val buffer = didOutputSampleBuffer ?: return
        if (!loggedFirstFrame) {
            loggedFirstFrame = true
            println("FaceAnalyzer[iOS]: first frame received from AVCaptureVideoDataOutput")
        }
        onFrame(buffer)
    }
}
