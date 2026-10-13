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
 * dataset. Only tests read it; the app must classify.
 */
data class MotionSample(
    val pitch: Float,
    val accMagnitude: Float,
    val trueLabel: PostureState
)

data class PostureSummary(
    val sittingSeconds: Int,
    val standingSeconds: Int,
    val movingSeconds: Int
)

/*
 * Rule-based posture classifier for a phone in the front
 * trouser pocket. Pure Kotlin so it runs in JVM tests.
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

    fun summarize(
        samples: List<MotionSample>
    ): PostureSummary {

        val states =
            samples.map { classify(it) }

        return PostureSummary(
            sittingSeconds = states.count { it == PostureState.SITTING },
            standingSeconds = states.count { it == PostureState.STANDING },
            movingSeconds = states.count { it == PostureState.MOVING }
        )
    }

    /*
     * Length of the sitting bout the samples end in, in
     * seconds; 0 if the user is on a break.
     *
     * Only minBreakSeconds of continuous standing or moving
     * ends a bout, so shifting in the chair or standing up
     * for a few seconds does not reset the timer. If the
     * bout started before the first sample, the result is
     * capped at the window length.
     */
    fun sittingBoutSeconds(
        samples: List<MotionSample>,
        minBreakSeconds: Int = 60
    ): Int {

        var boutStart: Int? = null
        var nonSittingRun = 0

        samples.forEachIndexed { index, sample ->
            if (classify(sample) == PostureState.SITTING) {
                if (boutStart == null) {
                    boutStart = index
                }
                nonSittingRun = 0
            } else {
                nonSittingRun++
                if (nonSittingRun >= minBreakSeconds) {
                    boutStart = null
                }
            }
        }

        return boutStart?.let { samples.size - it } ?: 0
    }
}
