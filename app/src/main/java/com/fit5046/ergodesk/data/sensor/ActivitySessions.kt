package com.fit5046.ergodesk.data.sensor

enum class SessionType {
    SITTING,
    BREAK,

    // "Take a break now": LOGGED_BREAK_MILLIS counted as a
    // break even if the sensor still reads sitting.
    LOGGED_BREAK
}

/*
 * One stretch of sitting or of being on a break (standing,
 * moving, or a logged break), [startMillis, endMillis).
 */
data class ActivitySession(
    val type: SessionType,
    val startMillis: Long,
    val endMillis: Long
) {

    val seconds: Long
        get() = (endMillis - startMillis) / 1000L

    val minutes: Int
        get() = (seconds / 60L).toInt()
}

/*
 * The three sitting figures Home shows, all from the same
 * session list so they always agree:
 * total today >= longest session >= current session.
 */
data class SessionSummary(
    val sittingSeconds: Long,
    val activeSeconds: Long,
    val longestSittingSeconds: Long,
    val currentSittingSeconds: Long,
    // Set instead of currentSittingSeconds while on a break.
    val currentBreakSeconds: Long,
    val currentBreakIsLogged: Boolean
) {

    val onBreak: Boolean
        get() = currentBreakSeconds > 0L
}

/*
 * Splits a day of 1-second sensor samples into sitting and
 * break sessions. Pure Kotlin apart from reading samples
 * from the simulator.
 *
 * A run shorter than minRunSeconds is not a change of
 * activity, so standing up for a few seconds stays part of
 * the sitting session, and a brief sit stays part of a
 * break.
 */
object ActivitySessions {

    // Standing or moving this long ends a sitting session
    // (movement sensitivity level 3, the default).
    const val MIN_BREAK_SECONDS = 60

    // How long a "Take a break now" counts as a break.
    const val LOGGED_BREAK_MILLIS = 5 * 60 * 1000L

    // How far back ContextEvaluationWorker looks for the
    // start of the current session; well past the relaxed
    // 45-minute interval.
    const val LOOKBACK_MILLIS = 2 * 60 * 60 * 1000L

    private data class Run(
        val type: SessionType,
        val start: Int,
        var length: Int
    )

    /*
     * Sessions for [startMillis, endMillis), with the user's
     * manual breaks applied. Used by Home and both workers.
     */
    fun forRange(
        simulator: SensorDataSimulator,
        startMillis: Long,
        endMillis: Long,
        manualBreaks: List<Long>,
        // From Settings: movement sensitivity.
        minBreakSeconds: Int = MIN_BREAK_SECONDS
    ): List<ActivitySession> {

        // Samples fall on whole seconds.
        val firstSecondMillis =
            Math.floorDiv(startMillis + 999L, 1000L) * 1000L

        if (firstSecondMillis >= endMillis) {
            return emptyList()
        }

        val sessions =
            fromSamples(
                samples = simulator.samplesBetween(firstSecondMillis, endMillis),
                firstSampleMillis = firstSecondMillis,
                minRunSeconds = minBreakSeconds
            )

        return withManualBreaks(
            sessions,
            manualBreaks.filter { it in firstSecondMillis until endMillis }
        )
    }

    /*
     * Sessions for each interval when sensing was on (see
     * TrackingLog), in order. Gaps between intervals count as
     * neither sitting nor a break, and sessions on either
     * side of a gap are never joined.
     */
    fun forIntervals(
        simulator: SensorDataSimulator,
        intervals: List<LongRange>,
        manualBreaks: List<Long>,
        minBreakSeconds: Int = MIN_BREAK_SECONDS
    ): List<ActivitySession> {

        return intervals.flatMap { interval ->
            forRange(
                simulator = simulator,
                startMillis = interval.first,
                endMillis = interval.last + 1,
                manualBreaks = manualBreaks,
                minBreakSeconds = minBreakSeconds
            )
        }
    }

    /*
     * samples[i] is the reading at firstSampleMillis + i s.
     */
    fun fromSamples(
        samples: List<MotionSample>,
        firstSampleMillis: Long,
        minRunSeconds: Int = MIN_BREAK_SECONDS
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

    /*
     * "Take a break now" turns the next LOGGED_BREAK_MILLIS of
     * a sitting session into a break, even if the sensor did
     * not see the user stand up; sitting resumes afterwards.
     * A press during a sensed break changes nothing, the
     * user is already on one.
     */
    fun withManualBreaks(
        sessions: List<ActivitySession>,
        breakTimes: List<Long>
    ): List<ActivitySession> {

        if (breakTimes.isEmpty()) {
            return sessions
        }

        val result = mutableListOf<ActivitySession>()

        sessions.forEach { session ->

            if (session.type != SessionType.SITTING) {
                result += session
                return@forEach
            }

            var start = session.startMillis

            breakTimes
                .filter { it >= session.startMillis && it < session.endMillis }
                .forEach { time ->

                    // Pressed again during a logged break.
                    if (time < start) {
                        return@forEach
                    }

                    if (time > start) {
                        result += session.copy(startMillis = start, endMillis = time)
                    }

                    val breakEnd =
                        minOf(time + LOGGED_BREAK_MILLIS, session.endMillis)

                    result += ActivitySession(SessionType.LOGGED_BREAK, time, breakEnd)
                    start = breakEnd
                }

            if (start < session.endMillis) {
                result += session.copy(startMillis = start)
            }
        }

        return result
    }

    fun summarize(
        sessions: List<ActivitySession>
    ): SessionSummary {

        val sitting =
            sessions.filter { it.type == SessionType.SITTING }

        // The session running now: sitting, or some kind of break.
        val current =
            sessions.lastOrNull()

        val currentSitting =
            current?.takeIf { it.type == SessionType.SITTING }

        val currentBreak =
            current?.takeIf { it.type != SessionType.SITTING }

        return SessionSummary(
            sittingSeconds = sitting.sumOf { it.seconds },
            activeSeconds = sessions.filter { it.type != SessionType.SITTING }.sumOf { it.seconds },
            longestSittingSeconds = sitting.maxOfOrNull { it.seconds } ?: 0L,
            currentSittingSeconds = currentSitting?.seconds ?: 0L,
            // At least one second, so a break that just began
            // already counts as being on a break.
            currentBreakSeconds = currentBreak?.let { maxOf(it.seconds, 1L) } ?: 0L,
            currentBreakIsLogged = currentBreak?.type == SessionType.LOGGED_BREAK
        )
    }
}
