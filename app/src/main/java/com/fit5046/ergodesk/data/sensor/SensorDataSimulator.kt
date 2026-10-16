package com.fit5046.ergodesk.data.sensor

import android.content.Context
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/*
 * Replays the labelled MotionSense timeline in place of
 * live phone sensors (A2 report, section 1.4).
 *
 * Wall-clock time maps onto the timeline, so workers can
 * ask what the "sensor" read at any moment without a
 * running service, and Home can follow liveSamples().
 * The timeline loops.
 */
class SensorDataSimulator(
    private val timeline: List<MotionSample>
) {

    init {
        require(timeline.isNotEmpty()) { "Timeline is empty" }
    }

    fun sampleAt(
        timeMillis: Long
    ): MotionSample {

        val second =
            Math.floorDiv(timeMillis, 1000L)

        return timeline[Math.floorMod(second, timeline.size.toLong()).toInt()]
    }

    /*
     * One sample per second in [startMillis, endMillis),
     * e.g. the last 15 minutes for SensorIngestWorker.
     */
    fun samplesBetween(
        startMillis: Long,
        endMillis: Long
    ): List<MotionSample> {

        val firstSecond =
            Math.floorDiv(startMillis + 999L, 1000L)

        val lastSecond =
            Math.floorDiv(endMillis - 1L, 1000L)

        return (firstSecond..lastSecond).map { sampleAt(it * 1000L) }
    }

    /*
     * Live sensor feed (A2 report, section 1.4): one sample
     * per second from fromMillis onwards, each emitted once
     * its second has arrived. Pairs with samplesBetween(_,
     * fromMillis) for history without gaps or repeats.
     */
    fun liveSamples(
        fromMillis: Long,
        clock: () -> Long = System::currentTimeMillis
    ): Flow<MotionSample> = flow {

        var second =
            Math.floorDiv(fromMillis + 999L, 1000L)

        while (true) {
            val waitMillis =
                second * 1000L - clock()

            if (waitMillis > 0) {
                delay(waitMillis)
            }

            emit(sampleAt(second * 1000L))
            second++
        }
    }

    companion object {

        const val TIMELINE_ASSET = "motion_timeline.csv"

        fun fromAssets(
            context: Context
        ): SensorDataSimulator {

            val lines =
                context.assets.open(TIMELINE_ASSET)
                    .bufferedReader()
                    .use { it.readLines() }

            return SensorDataSimulator(parseTimeline(lines))
        }

        /*
         * Parses motion_timeline.csv, built by
         * tools/build_motion_timeline.py:
         *   second,pitch,accMagnitude,label
         * The label column is the dataset's ground truth;
         * the app classifies samples itself.
         */
        fun parseTimeline(
            lines: List<String>
        ): List<MotionSample> {

            return lines
                .drop(1)
                .filter { it.isNotBlank() }
                .map { line ->
                    val columns = line.split(",")
                    MotionSample(
                        pitch = columns[1].toFloat(),
                        accMagnitude = columns[2].toFloat()
                    )
                }
        }
    }
}
