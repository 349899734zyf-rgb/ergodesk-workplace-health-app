package com.fit5046.ergodesk.data.sensor

import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class SensorDataSimulatorTest {

    // Three seconds: sit, stand, move. pitch doubles as an index.
    private val timeline = listOf(
        MotionSample(0f, 0.01f, PostureState.SITTING),
        MotionSample(1f, 0.01f, PostureState.STANDING),
        MotionSample(2f, 1.00f, PostureState.MOVING)
    )

    private fun indexAt(simulator: SensorDataSimulator, millis: Long) =
        simulator.sampleAt(millis).pitch.toInt()

    @Test
    fun sampleAt_mapsWholeSecondsAndLoops() {
        val simulator = SensorDataSimulator(timeline)

        assertEquals(0, indexAt(simulator, 0L))
        assertEquals(0, indexAt(simulator, 999L))
        assertEquals(1, indexAt(simulator, 1_000L))
        assertEquals(2, indexAt(simulator, 2_500L))
        assertEquals(0, indexAt(simulator, 3_000L))
    }

    @Test
    fun sampleAt_speedSkipsAhead() {
        val simulator = SensorDataSimulator(timeline, speed = 2)

        assertEquals(2, indexAt(simulator, 1_000L))
        assertEquals(1, indexAt(simulator, 2_000L))
    }

    @Test
    fun samplesBetween_returnsOneSamplePerSecond() {
        val simulator = SensorDataSimulator(timeline)

        val samples = simulator.samplesBetween(0L, 15 * 60 * 1000L)

        assertEquals(900, samples.size)
        assertEquals(
            mapOf(
                PostureState.SITTING to 300,
                PostureState.STANDING to 300,
                PostureState.MOVING to 300
            ),
            samples.groupingBy { it.trueLabel }.eachCount()
        )
    }

    @Test
    fun samplesBetween_emptyRange_isEmpty() {
        val simulator = SensorDataSimulator(timeline)

        assertEquals(0, simulator.samplesBetween(5_000L, 5_000L).size)
    }

    @Test
    fun liveSamples_continuesWhereHistoryEnds() = runBlocking {
        val simulator = SensorDataSimulator(timeline)
        val from = 1_500L

        val history = simulator.samplesBetween(from - 2_000L, from)
        val live = simulator.liveSamples(from, clock = { 10_000L }).take(3).toList()

        // History covers seconds 0 and 1, live starts at second 2.
        assertEquals(listOf(0, 1), history.map { it.pitch.toInt() })
        assertEquals(listOf(2, 0, 1), live.map { it.pitch.toInt() })
    }

    @Test
    fun parseTimeline_readsColumnsAndSkipsHeader() {
        val parsed = SensorDataSimulator.parseTimeline(
            listOf(
                "second,pitch,accMagnitude,label",
                "0,-0.417,0.011,SITTING",
                "1,-1.401,0.020,STANDING",
                ""
            )
        )

        assertEquals(
            listOf(
                MotionSample(-0.417f, 0.011f, PostureState.SITTING),
                MotionSample(-1.401f, 0.020f, PostureState.STANDING)
            ),
            parsed
        )
    }
}
