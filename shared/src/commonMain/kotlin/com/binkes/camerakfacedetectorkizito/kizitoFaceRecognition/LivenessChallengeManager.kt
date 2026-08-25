package com.binkes.camerakfacedetectorkizito.kizitoFaceRecognition


import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.binkes.camerakfacedetectorkizito.face.FaceAnalysisState


/**
 * Drives a sequence of liveness challenges from a stream of [FaceAnalysisState]
 * frames. Feed every analyzer frame via [onFrame]; requirements are debounced
 * across [holdFrames] consecutive satisfying frames before a step advances.
 *
 * NOTE: verify the sign convention of [FaceAnalysisState.headTiltDegrees] against
 * your detector output (ML Kit vs MediaPipe may differ) and flip the comparisons
 * in [onFrame] if TURN_LEFT/TURN_RIGHT feel reversed on device.
 */
class LivenessChallengeManager(
    private val challenges: List<LivenessInstruction> = randomChallengeSet(),
    private val holdFrames: Int = 3,
    private val turnThresholdDegrees: Float = 15f,
) {
    var state by mutableStateOf<LivenessGuidanceState>(LivenessGuidanceState.NoFace)
        private set

    private var stepIndex = 0
    private var holdCounter = 0
    private var blinkPhase = BlinkPhase.WAIT_OPEN
    private val completed = mutableListOf<LivenessInstruction>()

    val isVerified: Boolean get() = state is LivenessGuidanceState.Verified

    fun onFrame(face: FaceAnalysisState?) {
        if (state is LivenessGuidanceState.Verified) return

        if (face == null) {
            holdCounter = 0
            state = LivenessGuidanceState.NoFace
            return
        }
        if (stepIndex >= challenges.size) {
            state = LivenessGuidanceState.Verified
            return
        }

        val current = challenges[stepIndex]
        state = LivenessGuidanceState.InProgress(current, stepIndex, challenges.size)

        val satisfied = when (current) {
            LivenessInstruction.BLINK -> handleBlink(face)
            LivenessInstruction.SMILE -> face.isSmiling
            LivenessInstruction.TURN_LEFT -> face.headTiltDegrees <= -turnThresholdDegrees
            LivenessInstruction.TURN_RIGHT -> face.headTiltDegrees >= turnThresholdDegrees
            LivenessInstruction.RAISE_BROWS -> face.browRaised
        }

        if (satisfied) {
            holdCounter++
            if (holdCounter >= holdFrames) {
                completed += current
                holdCounter = 0
                blinkPhase = BlinkPhase.WAIT_OPEN
                stepIndex++
                state = if (stepIndex >= challenges.size) {
                    LivenessGuidanceState.Verified
                } else {
                    LivenessGuidanceState.HoldStill
                }
            }
        } else {
            holdCounter = 0
        }
    }

    /** Requires a genuine close-then-reopen cycle, not just "eyes currently closed". */
    private fun handleBlink(face: FaceAnalysisState): Boolean {
        val eyesClosed = !face.leftEyeOpen && !face.rightEyeOpen
        val eyesOpen = face.leftEyeOpen && face.rightEyeOpen
        return when (blinkPhase) {
            BlinkPhase.WAIT_OPEN -> {
                if (eyesOpen) blinkPhase = BlinkPhase.WAIT_CLOSE
                false
            }
            BlinkPhase.WAIT_CLOSE -> {
                if (eyesClosed) blinkPhase = BlinkPhase.WAIT_REOPEN
                false
            }
            BlinkPhase.WAIT_REOPEN -> eyesOpen
        }
    }

    fun timeoutCurrentStep() {
        if (stepIndex < challenges.size && state !is LivenessGuidanceState.Verified) {
            state = LivenessGuidanceState.TimedOut(challenges[stepIndex])
        }
    }

    fun reset() {
        stepIndex = 0
        holdCounter = 0
        blinkPhase = BlinkPhase.WAIT_OPEN
        completed.clear()
        state = LivenessGuidanceState.NoFace
    }

    fun result(capturedFilePath: String? = null) = LivenessResult(
        passed = state is LivenessGuidanceState.Verified,
        completedInstructions = completed.toList(),
        capturedFilePath = capturedFilePath,
    )

    private enum class BlinkPhase { WAIT_OPEN, WAIT_CLOSE, WAIT_REOPEN }
}