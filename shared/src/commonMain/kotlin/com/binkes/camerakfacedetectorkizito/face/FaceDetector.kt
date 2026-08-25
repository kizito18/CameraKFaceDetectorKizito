package com.binkes.camerakfacedetectorkizito.face

data class FaceAnalysisState(
    val isSmiling: Boolean,
    val leftEyeOpen: Boolean,
    val rightEyeOpen: Boolean,
    val headTiltDegrees: Float,
    val jawOpen: Boolean = false,
    val browRaised: Boolean = false,
    val mouthOpen: Boolean = false,
    val winkLeft: Boolean = false,
    val winkRight: Boolean = false,


    // ── Added for proximity/centering guidance ──────────────
    // Populate these from the detected face's bounding box, the same way
    // your old Android FaceDetectionCamera.processFaces() did:
    //   faceWidthRatio   = box.width()  / frameWidth
    //   horizontalOffset = abs(box.centerX() - frameWidth/2f)  / frameWidth
    //   verticalOffset   = abs(box.centerY() - frameHeight/2f) / frameHeight
    //   yawDegrees       = face.headEulerAngleY (ML Kit) / equivalent (MediaPipe)
    //   pitchDegrees     = face.headEulerAngleX (ML Kit) / equivalent (MediaPipe)
    //   inFrameBounds    = box fully inside [0,frameWidth] x [0,frameHeight]
    // Defaults (0f / true) mean "unknown" — proximity guidance won't gate
    // anything until your detector actually fills these in.
    val faceWidthRatio: Float = 0f,
    val horizontalOffset: Float = 0f,
    val verticalOffset: Float = 0f,
    val yawDegrees: Float = 0f,
    val pitchDegrees: Float = 0f,
    val inFrameBounds: Boolean = true,
)

enum class DetectorKind { ML_KIT, MEDIAPIPE }

interface FaceDetector {
    fun analyze(frame: ImageFrame): FaceAnalysisState?
    fun close()
}
