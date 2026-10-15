package com.fit5046.ergodesk.data.sensor

enum class PostureState {
    SITTING,
    STANDING,
    MOVING
}

/*
 * One second of phone motion, already reduced to the two
 * features the classifier needs.
 *
 * trueLabel is the ground truth from the MotionSense
 * dataset, kept for checking accuracy; the app classifies.
 */
data class MotionSample(
    val pitch: Float,
    val accMagnitude: Float,
    val trueLabel: PostureState
)

/*
 * Rule-based posture classifier for a phone in the front
 * trouser pocket. Pure Kotlin, no Android dependencies.
 *
 * Thresholds come from the 1-second MotionSense windows
 * in motion_timeline.csv:
 *   accMagnitude  sit/stand <= 0.14 g, walk/jog >= 0.42 g
 *   pitch         sit -0.48..-0.31 rad, stand -1.53..-1.31 rad
 */
object PostureClassifier {

    const val MOVING_ACC_THRESHOLD = 0.3f

    // Sitting puts the thigh near horizontal, so pitch is
    // closer to 0 than when standing.
    const val SITTING_PITCH_THRESHOLD = -0.9f

    fun classify(
        sample: MotionSample
    ): PostureState {

        return when {
            sample.accMagnitude >= MOVING_ACC_THRESHOLD ->
                PostureState.MOVING

            sample.pitch > SITTING_PITCH_THRESHOLD ->
                PostureState.SITTING

            else ->
                PostureState.STANDING
        }
    }
}
