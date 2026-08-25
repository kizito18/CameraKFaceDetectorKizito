package com.binkes.camerakfacedetectorkizito.kizitoFaceRecognition

import com.binkes.camerakfacedetectorkizito.face.FaceAnalysisState


sealed class FaceGuidanceState {
    data object NoFace : FaceGuidanceState()
    data object MoveCloser : FaceGuidanceState()
    data object MoveBack : FaceGuidanceState()
    data object CenterFace : FaceGuidanceState()
    data object LookStraight : FaceGuidanceState()
    data object HoldStill : FaceGuidanceState()
    data object BlinkNow : FaceGuidanceState()
    data object Verified : FaceGuidanceState()
    data object Capturing : FaceGuidanceState()
}

/**
 * Drives the same guidance state machine as the old FaceDetectionCamera,
 * but consumes CameraK's FaceAnalysisState frames instead of raw ML Kit Face
 * objects. Call [onFrame] from your analyzer callback; call [setCapturing]
 * around takePicture() the same way the old screen checked `isCapturing`.
 */
class FaceVerificationGuidanceEngine(
    private val requiredStableFrames: Int = 8,
    private val minFaceWidthRatio: Float = 0.25f,
    private val maxFaceWidthRatio: Float = 0.75f,
    private val verifiedMinFaceWidthRatio: Float = 0.42f, // post-verify drift check
    private val maxCenterOffset: Float = 0.18f,
    private val maxYawDegrees: Float = 20f,
    private val maxPitchDegrees: Float = 25f,
) {
    var state: FaceGuidanceState = FaceGuidanceState.NoFace
        private set

    private var stableFrames = 0
    private var verified = false
    private var blinkPhase = BlinkPhase.WAIT_OPEN
    private var capturing = false

    fun setCapturing(value: Boolean) {
        capturing = value
        if (value) state = FaceGuidanceState.Capturing
    }

    /*
    fun onFrame(face: FaceAnalysisState?): FaceGuidanceState {
        if (capturing) {
            state = FaceGuidanceState.Capturing
            return state
        }

        if (face == null) {
            reset()
            return state
        }

        if (verified) {
            val stillClose = face.faceWidthRatio >= verifiedMinFaceWidthRatio && face.inFrameBounds
            state = if (stillClose) {
                FaceGuidanceState.Verified
            } else {
                reset()
                FaceGuidanceState.NoFace
            }
            return state
        }

        if (!face.inFrameBounds) {
            stableFrames = 0
            state = FaceGuidanceState.CenterFace
            return state
        }
        if (face.faceWidthRatio < minFaceWidthRatio) {
            stableFrames = 0
            state = FaceGuidanceState.MoveCloser
            return state
        }
        if (face.faceWidthRatio > maxFaceWidthRatio) {
            stableFrames = 0
            state = FaceGuidanceState.MoveBack
            return state
        }
        if (face.horizontalOffset > maxCenterOffset || face.verticalOffset > maxCenterOffset) {
            stableFrames = 0
            state = FaceGuidanceState.CenterFace
            return state
        }
        if (kotlin.math.abs(face.yawDegrees) > maxYawDegrees || kotlin.math.abs(face.pitchDegrees) > maxPitchDegrees) {
            stableFrames = 0
            state = FaceGuidanceState.LookStraight
            return state
        }

        stableFrames++
        if (stableFrames < requiredStableFrames) {
            state = FaceGuidanceState.HoldStill
            return state
        }

        state = FaceGuidanceState.BlinkNow
        trackBlink(face)
        return state
    }

     */

    fun onFrame(face: FaceAnalysisState?): FaceGuidanceState {
        println(
            "LIVENESS_DEBUG frame: face=${
                if (face == null) "null" else
                    "widthRatio=${face.faceWidthRatio} hOff=${face.horizontalOffset} vOff=${face.verticalOffset} " +
                            "yaw=${face.yawDegrees} pitch=${face.pitchDegrees} inBounds=${face.inFrameBounds} " +
                            "leftEyeOpen=${face.leftEyeOpen} rightEyeOpen=${face.rightEyeOpen}"
            }"
        )

        if (capturing) {
            state = FaceGuidanceState.Capturing
            return state
        }

        if (face == null) {
            println("LIVENESS_DEBUG → NoFace (detector returned null)")
            reset()
            return state
        }

        if (verified) {
            val stillClose = face.faceWidthRatio >= verifiedMinFaceWidthRatio && face.inFrameBounds
            state = if (stillClose) {
                FaceGuidanceState.Verified
            } else {
                println("LIVENESS_DEBUG → dropping Verified: widthRatio=${face.faceWidthRatio} inBounds=${face.inFrameBounds}")
                reset()
                FaceGuidanceState.NoFace
            }
            return state
        }

        if (!face.inFrameBounds) {
            println("LIVENESS_DEBUG → CenterFace (out of bounds)")
            stableFrames = 0
            state = FaceGuidanceState.CenterFace
            return state
        }
        if (face.faceWidthRatio < minFaceWidthRatio) {
            println("LIVENESS_DEBUG → MoveCloser: widthRatio=${face.faceWidthRatio} < min=$minFaceWidthRatio")
            stableFrames = 0
            state = FaceGuidanceState.MoveCloser
            return state
        }
        if (face.faceWidthRatio > maxFaceWidthRatio) {
            println("LIVENESS_DEBUG → MoveBack: widthRatio=${face.faceWidthRatio} > max=$maxFaceWidthRatio")
            stableFrames = 0
            state = FaceGuidanceState.MoveBack
            return state
        }
        if (face.horizontalOffset > maxCenterOffset || face.verticalOffset > maxCenterOffset) {
            println("LIVENESS_DEBUG → CenterFace: hOff=${face.horizontalOffset} vOff=${face.verticalOffset} max=$maxCenterOffset")
            stableFrames = 0
            state = FaceGuidanceState.CenterFace
            return state
        }
        if (kotlin.math.abs(face.yawDegrees) > maxYawDegrees || kotlin.math.abs(face.pitchDegrees) > maxPitchDegrees) {
            println("LIVENESS_DEBUG → LookStraight: yaw=${face.yawDegrees} pitch=${face.pitchDegrees}")
            stableFrames = 0
            state = FaceGuidanceState.LookStraight
            return state
        }

        stableFrames++
        println("LIVENESS_DEBUG → HoldStill/BlinkNow progress: stableFrames=$stableFrames/$requiredStableFrames")
        if (stableFrames < requiredStableFrames) {
            state = FaceGuidanceState.HoldStill
            return state
        }

        state = FaceGuidanceState.BlinkNow
        trackBlink(face)
        return state
    }

    /** Requires a genuine open→closed→reopen cycle, matching the old eyesWereOpen/pendingReopen logic. */
    private fun trackBlink(face: FaceAnalysisState) {
        val eyesOpen = face.leftEyeOpen && face.rightEyeOpen
        val eyesClosed = !face.leftEyeOpen && !face.rightEyeOpen
        when (blinkPhase) {
            BlinkPhase.WAIT_OPEN -> if (eyesOpen) blinkPhase = BlinkPhase.WAIT_CLOSE
            BlinkPhase.WAIT_CLOSE -> if (eyesClosed) blinkPhase = BlinkPhase.WAIT_REOPEN
            BlinkPhase.WAIT_REOPEN -> if (eyesOpen) {
                verified = true
                blinkPhase = BlinkPhase.WAIT_OPEN
                state = FaceGuidanceState.Verified
            }
        }
    }

    fun reset() {
        stableFrames = 0
        verified = false
        blinkPhase = BlinkPhase.WAIT_OPEN
        state = FaceGuidanceState.NoFace
    }

    private enum class BlinkPhase { WAIT_OPEN, WAIT_CLOSE, WAIT_REOPEN }
}