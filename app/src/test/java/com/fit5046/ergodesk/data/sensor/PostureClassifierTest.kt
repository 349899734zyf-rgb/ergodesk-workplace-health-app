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
