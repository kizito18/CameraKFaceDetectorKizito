package com.binkes.camerakfacedetectorkizito

import com.binkes.camerakfacedetectorkizito.face.FaceAnalysisState
import com.binkes.camerakfacedetectorkizito.face.ImageFrame

interface SwiftFaceDetector {
    fun analyzeFrame(frame: ImageFrame): FaceAnalysisState?
    fun close()
}

object FaceDetectorBridge {
    var mlKit: SwiftFaceDetector? = null
    var mediaPipe: SwiftFaceDetector? = null
}
