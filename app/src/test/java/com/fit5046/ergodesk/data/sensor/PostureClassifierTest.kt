package com.fit5046.ergodesk.data.sensor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class PostureClassifierTest {

    private fun sample(pitch: Float, acc: Float) =
        MotionSample(pitch, acc, PostureState.SITTING)

    @Test
    fun highAcceleration_isMoving_whateverThePitch() {
        assertEquals(PostureState.MOVING, PostureClassifier.classify(sample(-0.4f, 1.0f)))
        assertEquals(PostureState.MOVING, PostureClassifier.classify(sample(-1.4f, 0.3f)))
    }

    @Test
    fun stillWithFlatThigh_isSitting() {
        assertEquals(PostureState.SITTING, PostureClassifier.classify(sample(-0.4f, 0.02f)))
    }

    @Test
    fun stillWithUprightThigh_isStanding() {
        assertEquals(PostureState.STANDING, PostureClassifier.classify(sample(-1.4f, 0.02f)))
    }

    @Test
    fun summarize_countsSecondsPerState() {
        val summary = PostureClassifier.summarize(
            listOf(
                sample(-0.4f, 0.02f),
                sample(-0.4f, 0.02f),
                sample(-1.4f, 0.02f),
                sample(-0.8f, 1.2f)
            )
        )

        assertEquals(PostureSummary(2, 1, 1), summary)
    }

    @Test
    fun summarize_emptyWindow_isAllZero() {
        assertEquals(PostureSummary(0, 0, 0), PostureClassifier.summarize(emptyList()))
    }

    /* ---------------- Sitting bout ---------------- */

    private fun seconds(count: Int, pitch: Float, acc: Float) =
        List(count) { sample(pitch, acc) }

    private val sit = Pair(-0.4f, 0.02f)
    private val walk = Pair(-0.8f, 1.0f)

    private fun sitting(count: Int) = seconds(count, sit.first, sit.second)
    private fun walking(count: Int) = seconds(count, walk.first, walk.second)

    @Test
    fun sittingBout_countsSecondsSinceLastBreak() {
        val samples = sitting(100) + walking(60) + sitting(300)

        assertEquals(300, PostureClassifier.sittingBoutSeconds(samples))
    }

    @Test
    fun sittingBout_shortInterruption_doesNotReset() {
        val samples = sitting(100) + walking(20) + sitting(300)

        assertEquals(420, PostureClassifier.sittingBoutSeconds(samples))
    }

    @Test
    fun sittingBout_onBreakNow_isZero() {
        assertEquals(0, PostureClassifier.sittingBoutSeconds(sitting(100) + walking(90)))
        assertEquals(0, PostureClassifier.sittingBoutSeconds(emptyList()))
    }

    /*
     * Checks the thresholds against the real MotionSense
     * timeline shipped in assets. Unit tests run from the
     * app module directory.
     */
    @Test
    fun matchesDatasetLabels_onBundledTimeline() {
        val timeline = SensorDataSimulator.parseTimeline(
            File("src/main/assets/${SensorDataSimulator.TIMELINE_ASSET}").readLines()
        )

        val correct = timeline.count { PostureClassifier.classify(it) == it.trueLabel }
        val accuracy = correct.toDouble() / timeline.size

        assertTrue(timeline.map { it.trueLabel }.toSet() == PostureState.entries.toSet())
        assertTrue("accuracy was $accuracy", accuracy >= 0.95)
    }
}
