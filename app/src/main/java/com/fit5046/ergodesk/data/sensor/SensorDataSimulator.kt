package com.fit5046.ergodesk.data.sensor

import android.content.Context

/*
 * Replays the labelled MotionSense timeline in place of
 * live phone sensors (A2 report, section 1.4).
 *
 * Wall-clock time maps onto the timeline, so any caller
 * (Home, SensorIngestWorker) can ask what the "sensor"
 * read at a given moment without a running service.
 * The timeline loops; speed > 1 plays it faster for demos.
 */
class SensorDataSimulator(
    private val timeline: List<MotionSample>,
    private val speed: Int = 1
) {

    init {
        require(timeline.isNotEmpty()) { "Timeline is empty" }
        require(speed >= 1) { "Speed must be at least 1" }
    }

    fun sampleAt(
        timeMillis: Long
    ): MotionSample {

        val second =
            Math.floorDiv(timeMillis, 1000L) * speed

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

    companion object {

        const val TIMELINE_ASSET = "motion_timeline.csv"

        fun fromAssets(
            context: Context,
            speed: Int = 1
        ): SensorDataSimulator {

            val lines =
                context.assets.open(TIMELINE_ASSET)
                    .bufferedReader()
                    .use { it.readLines() }

            return SensorDataSimulator(
                timeline = parseTimeline(lines),
                speed = speed
            )
        }

        /*
         * Parses motion_timeline.csv, built by
         * tools/build_motion_timeline.py:
         *   second,pitch,accMagnitude,label
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
                        accMagnitude = columns[2].toFloat(),
                        trueLabel = PostureState.valueOf(columns[3].trim())
                    )
                }
        }
    }
}
