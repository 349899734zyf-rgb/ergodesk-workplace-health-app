package com.fit5046.ergodesk.data.sensor

enum class SessionType {
    SITTING,
    BREAK
}

/*
 * One stretch of sitting or of being on a break (standing
 * or moving), [startMillis, endMillis).
 */
data class ActivitySession(
    val type: SessionType,
    val startMillis: Long,
    val endMillis: Long
) {

    val minutes: Int
        get() = ((endMillis - startMillis) / 60_000L).toInt()
}

/*
 * Splits a day of 1-second sensor samples into sitting and
 * break sessions for Home's timeline. Pure Kotlin.
 *
 * Uses the same rule as SittingBoutTracker: a run shorter
 * than minRunSeconds is not a change of activity, so
 * standing up for a few seconds stays part of the sitting
 * session, and a brief sit stays part of a break.
 */
object ActivitySessions {

    private data class Run(
        val type: SessionType,
        val start: Int,
        var length: Int
    )

    /*
     * samples[i] is the reading at firstSampleMillis + i s.
     */
    fun fromSamples(
        samples: List<MotionSample>,
        firstSampleMillis: Long,
        minRunSeconds: Int = SittingBoutTracker.DEFAULT_MIN_BREAK_SECONDS
    ): List<ActivitySession> {

        val runs = mutableListOf<Run>()

        samples.forEachIndexed { index, sample ->

            val type =
                if (PostureClassifier.classify(sample) == PostureState.SITTING) {
                    SessionType.SITTING
                } else {
                    SessionType.BREAK
                }

            val last = runs.lastOrNull()

            if (last != null && last.type == type) {
                last.length++
            } else {
                runs += Run(type, index, 1)
            }
        }

        // Short runs take the type of the run before them,
        // then neighbours of the same type are joined.
        val sessions = mutableListOf<Run>()

        runs.forEach { run ->

            val previous = sessions.lastOrNull()

            val type =
                if (run.length < minRunSeconds && previous != null) previous.type else run.type

            if (previous != null && previous.type == type) {
                previous.length += run.length
            } else {
                sessions += Run(type, run.start, run.length)
            }
        }

        return sessions.map { run ->
            ActivitySession(
                type = run.type,
                startMillis = firstSampleMillis + run.start * 1000L,
                endMillis = firstSampleMillis + (run.start + run.length) * 1000L
            )
        }
    }

    fun longestSittingMinutes(
        sessions: List<ActivitySession>
    ): Int {
        return sessions
            .filter { it.type == SessionType.SITTING }
            .maxOfOrNull { it.minutes }
            ?: 0
    }
}
