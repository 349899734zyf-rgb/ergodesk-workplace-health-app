package com.fit5046.ergodesk.ui.home

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fit5046.ergodesk.alarm.BreakAlarmScheduler
import com.fit5046.ergodesk.data.analytics.AnalyticsCalculator
import com.fit5046.ergodesk.data.analytics.AnalyticsPeriod
import com.fit5046.ergodesk.data.context.ContextEngine
import com.fit5046.ergodesk.data.repository.RepositoryProvider
import com.fit5046.ergodesk.data.sensor.ActivityIngest
import com.fit5046.ergodesk.data.sensor.ActivitySession
import com.fit5046.ergodesk.data.sensor.ActivitySessions
import com.fit5046.ergodesk.data.sensor.ManualBreakStore
import com.fit5046.ergodesk.data.sensor.PostureClassifier
import com.fit5046.ergodesk.data.sensor.PostureState
import com.fit5046.ergodesk.data.sensor.SensorDataSimulator
import com.fit5046.ergodesk.data.sensor.SittingBoutTracker
import com.fit5046.ergodesk.data.user.CurrentUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

private data class LiveSensing(
    val posture: PostureState,
    val sittingBoutSeconds: Int
)

class HomeViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository =
        RepositoryProvider.activityRepository(application)

    private val startMillis =
        System.currentTimeMillis()

    // Home is only reachable after login, but stay safe.
    private val user =
        CurrentUser.get()

    // Fixed when Home opens; a screen left open past
    // midnight keeps showing the previous day.
    private val today =
        AnalyticsCalculator.periodRange(AnalyticsPeriod.DAILY, startMillis)

    private val complianceWindow =
        ContextEngine.complianceWindow(startMillis)

    /*
     * Replays recent history into the tracker first, so the
     * bout is right immediately, then follows the live
     * feed. Restarts from scratch on every new collection.
     */
    private val liveSensing =
        flow {
            val simulator =
                SensorDataSimulator.fromAssets(getApplication())

            val now =
                System.currentTimeMillis()

            val tracker =
                SittingBoutTracker()

            simulator
                .samplesBetween(now - SittingBoutTracker.LOOKBACK_MILLIS, now)
                .forEach { tracker.add(it) }

            emitAll(
                simulator.liveSamples(now).map { sample ->
                    LiveSensing(
                        posture = PostureClassifier.classify(sample),
                        sittingBoutSeconds = tracker.add(sample)
                    )
                }
            )
        }.flowOn(Dispatchers.Default)

    val uiState: StateFlow<HomeUiState> =
        combine(
            liveSensing,
            repository
                .observeRecordsBetween(today.startMillis, today.endMillis)
                .map { it.firstOrNull() },
            repository
                .observeRecordsBetween(
                    complianceWindow.startMillis,
                    complianceWindow.endMillis
                ),
            user
                ?.let { ManualBreakStore.observe(application, it.uid) }
                ?: flowOf(null)
        ) { live, todayRecord, history, lastBreak ->

            // A bout ends at the user's own "Take a break now"
            // and never starts before their account existed.
            val boutSince =
                listOfNotNull(lastBreak, user?.trackingStartMillis).maxOrNull()

            HomeUiState.from(
                posture = live.posture,
                sittingBoutSeconds =
                    SittingBoutTracker.capSince(
                        live.sittingBoutSeconds,
                        System.currentTimeMillis(),
                        boutSince
                    ),
                today = todayRecord,
                history = history,
                hourOfDay = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            )
        }
            // The sensor ticks every second, but the state only
            // changes when a minute passes or data changes.
            .distinctUntilChanged()
            .onEach { Log.d(STATE_TAG, it.logSummary) }
            .stateIn(
                scope = viewModelScope,
                // Stops the 1-second feed shortly after Home
                // leaves the screen, keeping it across rotation.
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = HomeUiState()
            )

    /*
     * Today's sitting and break sessions, oldest first, for
     * the timeline and "Longest sit". Rebuilt once a minute
     * from the sensor samples since midnight, or since the
     * account was created if that was later today.
     */
    val todaySessions: StateFlow<List<ActivitySession>> =
        flow {
            val simulator =
                SensorDataSimulator.fromAssets(getApplication())

            while (true) {
                val now =
                    System.currentTimeMillis()

                // Samples fall on whole seconds.
                val firstSecondMillis =
                    Math.floorDiv(
                        maxOf(today.startMillis, user?.trackingStartMillis ?: 0L) + 999L,
                        1000L
                    ) * 1000L

                emit(
                    if (user == null || firstSecondMillis >= now) {
                        emptyList()
                    } else {
                        ActivitySessions.fromSamples(
                            samples = simulator.samplesBetween(firstSecondMillis, now),
                            firstSampleMillis = firstSecondMillis
                        )
                    }
                )

                delay(SESSIONS_REFRESH_MILLIS)
            }
        }
            .flowOn(Dispatchers.Default)
            .onEach { sessions ->
                Log.d(
                    STATE_TAG,
                    "Timeline: ${sessions.size} sessions today, " +
                            "longest sit ${ActivitySessions.longestSittingMinutes(sessions)} min"
                )
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    /*
     * "Take a break now": restarts the sitting timer, counts
     * a completed break for today and clears the pending
     * automatic reminder, since the user is already on a
     * break. A manual snooze is kept.
     *
     * Returns false, and counts nothing, when the last break
     * was under BREAK_COOLDOWN_MILLIS ago, so repeated taps
     * don't inflate break compliance.
     */
    fun takeBreakNow(): Boolean {

        val account =
            user ?: return false

        val now =
            System.currentTimeMillis()

        val lastBreak =
            ManualBreakStore.read(getApplication(), account.uid)

        if (lastBreak != null && now - lastBreak < BREAK_COOLDOWN_MILLIS) {
            Log.d(STATE_TAG, "Break ignored -> last one was under 5 min ago")
            return false
        }

        ManualBreakStore.record(getApplication(), account.uid, now)

        viewModelScope.launch {

            val existing =
                repository
                    .observeRecordsBetween(today.startMillis, today.endMillis)
                    .first()
                    .firstOrNull()

            val updated =
                ActivityIngest.addBreak(existing, now)

            repository.upsertRecord(updated)

            BreakAlarmScheduler.cancelBreakReminder(getApplication(), automatic = true)

            Log.d(
                STATE_TAG,
                "Break taken -> sitting timer restarted, " +
                        "${updated.breaksCompleted}/${updated.breakTarget} breaks today, " +
                        "automatic reminder cleared"
            )
        }

        return true
    }

    private companion object {
        const val STATE_TAG = "HomeState"
        const val SESSIONS_REFRESH_MILLIS = 60_000L
        const val BREAK_COOLDOWN_MILLIS = 5 * 60 * 1000L
    }
}
