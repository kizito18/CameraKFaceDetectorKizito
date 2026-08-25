package com.binkes.camerakfacedetectorkizito.kizitoFaceRecognition



/** A single action the user must perform to prove liveness. */
enum class LivenessInstruction(val message: String) {
    BLINK("Blink both eyes"),
    SMILE("Give us a smile"),
    TURN_LEFT("Turn your head left"),
    TURN_RIGHT("Turn your head right"),
    RAISE_BROWS("Raise your eyebrows"),
}

/**
 * Random subset of challenges per session — makes the sequence unpredictable
 * so a pre-recorded video/photo can't be scripted against it in advance.
 */
fun randomChallengeSet(count: Int = 3): List<LivenessInstruction> =
    LivenessInstruction.entries.shuffled().take(count)

sealed interface LivenessGuidanceState {
    data object NoFace : LivenessGuidanceState
    data class InProgress(
        val instruction: LivenessInstruction,
        val stepIndex: Int,
        val totalSteps: Int,
    ) : LivenessGuidanceState
    data object HoldStill : LivenessGuidanceState
    data object Verified : LivenessGuidanceState
    data class TimedOut(val onInstruction: LivenessInstruction) : LivenessGuidanceState
}

data class LivenessResult(
    val passed: Boolean,
    val completedInstructions: List<LivenessInstruction>,
    val capturedFilePath: String? = null,
)



