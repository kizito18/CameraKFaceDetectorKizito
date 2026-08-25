package com.binkes.camerakfacedetectorkizito.face

import android.content.Context
import androidx.camera.core.ImageAnalysis
import com.binkes.camerakfacedetectorkizito.MediaPipeFaceDetectorAndroid
import com.binkes.camerakfacedetectorkizito.MlKitFaceDetectorAndroid
import com.kashif.cameraK.controller.CameraController
import java.util.concurrent.atomic.AtomicReference


actual object FaceDetectorFactory {

    @Volatile
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    actual fun create(kind: DetectorKind): FaceDetector {
        val ctx = appContext
            ?: error("FaceDetectorFactory.init(applicationContext) must be called before create()")
        return when (kind) {
            DetectorKind.ML_KIT -> MlKitFaceDetectorAndroid()
            DetectorKind.MEDIAPIPE -> MediaPipeFaceDetectorAndroid(ctx)
        }
    }
}



actual fun attachFaceAnalyzer(
    controller: CameraController,
    onResult: (FaceAnalysisState?) -> Unit,
): AnalyzerHandle {
    val active = AtomicReference<FaceDetector?>(null)

    val analyzer = ImageAnalysis.Analyzer { proxy ->
        try {
            val detector = active.get()
            if (detector != null) {
                val result = detector.analyze(ImageFrame(proxy))
                onResult(result)
            }
        } catch (t: Throwable) {
            onResult(null)
        } finally {
            proxy.close()
        }
    }
    controller.registerImageAnalyzer(analyzer)

    return object : AnalyzerHandle {
        override fun setDetector(detector: FaceDetector?) {
            active.set(detector)
        }
        override fun detach() {
            active.set(null)
            controller.unregisterImageAnalyzer(analyzer)
        }
    }
}
