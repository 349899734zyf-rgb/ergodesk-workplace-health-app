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
     * seconds; 0 if the user is on a break. If the bout
     * started before the first sample, the result is capped
     * at the window length.
     */
    fun sittingBoutSeconds(
        samples: List<MotionSample>,
        minBreakSeconds: Int = SittingBoutTracker.DEFAULT_MIN_BREAK_SECONDS
    ): Int {

        val tracker =
            SittingBoutTracker(minBreakSeconds)

        return samples.fold(0) { _, sample -> tracker.add(sample) }
    }
}

/*
 * Follows the current sitting bout one 1-second sample at
 * a time, so a live sensor stream can update it without
 * re-reading history.
 *
 * Only minBreakSeconds of continuous standing or moving
 * ends a bout, so shifting in the chair or standing up
 * for a few seconds does not reset the timer.
 */
class SittingBoutTracker(
    private val minBreakSeconds: Int = DEFAULT_MIN_BREAK_SECONDS
) {

    private var boutSeconds = 0
    private var inBout = false
    private var nonSittingRun = 0

    /*
     * Returns the bout length in seconds after this sample.
     */
    fun add(
        sample: MotionSample
    ): Int {

        if (PostureClassifier.classify(sample) == PostureState.SITTING) {
            inBout = true
            nonSittingRun = 0
        } else {
            nonSittingRun++
            if (nonSittingRun >= minBreakSeconds) {
                inBout = false
            }
        }

        boutSeconds =
            if (inBout) boutSeconds + 1 else 0

        return boutSeconds
    }

    companion object {

        const val DEFAULT_MIN_BREAK_SECONDS = 60

        /*
         * A break the user reports ("Take a break now") ends
         * the bout as well, even if the sensor did not see
         * them stand up: the bout is capped at the time
         * since that break.
         */
        fun withManualBreak(
            sensorBoutSeconds: Int,
            nowMillis: Long,
            lastBreakMillis: Long?
        ): Int {

            if (lastBreakMillis == null || lastBreakMillis > nowMillis) {
                return sensorBoutSeconds
            }

            val secondsSinceBreak =
                (nowMillis - lastBreakMillis) / 1000L

            return minOf(sensorBoutSeconds.toLong(), secondsSinceBreak).toInt()
        }

        // Long enough to see the start of a bout that runs
        // well past the relaxed 45-minute interval.
        const val LOOKBACK_MILLIS = 2 * 60 * 60 * 1000L
    }
}
