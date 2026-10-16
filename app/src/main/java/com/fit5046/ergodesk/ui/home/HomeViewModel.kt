package com.fit5046.ergodesk.ui.home

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fit5046.ergodesk.alarm.BreakAlarmScheduler
import com.fit5046.ergodesk.data.analytics.AnalyticsCalculator
import com.fit5046.ergodesk.data.analytics.AnalyticsPeriod
import com.fit5046.ergodesk.data.context.ContextEngine
import com.fit5046.ergodesk.data.context.QuietHours
import com.fit5046.ergodesk.data.context.WeatherCache
import com.fit5046.ergodesk.data.repository.RepositoryProvider
import com.fit5046.ergodesk.data.sensor.ActivityIngest
import com.fit5046.ergodesk.data.sensor.ActivitySession
import com.fit5046.ergodesk.data.sensor.ActivitySessions
import com.fit5046.ergodesk.data.sensor.ManualBreakStore
import com.fit5046.ergodesk.data.sensor.PostureClassifier
import com.fit5046.ergodesk.data.sensor.PostureState
import com.fit5046.ergodesk.data.sensor.ForegroundTracker
import com.fit5046.ergodesk.data.sensor.SensorDataSimulator
import com.fit5046.ergodesk.data.sensor.TrackingLog
import com.fit5046.ergodesk.data.settings.SettingsRepository
import com.fit5046.ergodesk.data.settings.UserSettings
import com.fit5046.ergodesk.data.user.CurrentUser
import com.fit5046.ergodesk.data.user.TrackedUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository =
        RepositoryProvider.activityRepository(application)

    private val startMillis =
        System.currentTimeMillis()

    // Home is only reachable after login, but stay safe.
    private val user =
        CurrentUser.get(application)

    // Fixed when Home opens; a screen left open past
    // midnight keeps showing the previous day.
    private val today =
        AnalyticsCalculator.periodRange(AnalyticsPeriod.DAILY, startMillis)

    private val complianceWindow =
        ContextEngine.complianceWindow(startMillis)

    // Filled by WeatherSyncWorker; picks the break suggestion.
    private val weatherCache =
        WeatherCache(application)

    private val manualBreaks: Flow<List<Long>> =
        user
            ?.let { ManualBreakStore.observe(application, it.uid) }
            ?: flowOf(emptyList())

    init {
        // Home is open, so sensing is on; this also starts it
        // right after sign-in.
        ForegroundTracker.update()
    }

    // Saved Settings; a change shows up on Home at once.
    private val settings: Flow<UserSettings> =
        user
            ?.let { SettingsRepository.observe(application, it.uid) }
            ?: flowOf(UserSettings())

    // "Now: Sitting / Standing / Moving", from the live feed.
    // Each change is logged with the readings behind it, so
    // Logcat shows the classifier reacting to the sensor.
    private val livePosture: Flow<PostureState> =
        flow {
            val simulator =
                SensorDataSimulator.fromAssets(getApplication())

            emitAll(
                simulator
                    .liveSamples(System.currentTimeMillis())
                    .map { sample -> sample to PostureClassifier.classify(sample) }
            )
        }
            .distinctUntilChanged { old, new -> old.second == new.second }
            .onEach { (sample, posture) ->
                Log.d(
                    FEED_TAG,
                    "Sensor now reads $posture " +
                            "(thigh pitch ${"%.2f".format(sample.pitch)} rad, " +
                            "movement ${"%.2f".format(sample.accMagnitude)} g)"
                )
            }
            .map { it.second }
            .flowOn(Dispatchers.Default)

    /*
     * Today's sitting and break sessions, oldest first: the
     * single source for the ring, "Sitting today", "Longest
     * sit" and the timeline. Rebuilt every few seconds from
     * the sensor samples for today's tracked intervals, and at
     * once when the user logs a break or changes movement
     * sensitivity or quiet hours. Quiet hours are never
     * tracked.
     */
    val todaySessions: StateFlow<List<ActivitySession>> =
        combine(
            manualBreaks,
            settings.map { it.minBreakSeconds to it.quietHours }.distinctUntilChanged()
        ) { breaks, (minBreakSeconds, quietHours) -> Triple(breaks, minBreakSeconds, quietHours) }
            .flatMapLatest { (breaks, minBreakSeconds, quietHours) ->
                flow {
                    val simulator =
                        SensorDataSimulator.fromAssets(getApplication())

                    while (true) {
                        emit(
                            user?.let {
                                sessionsSoFar(simulator, it, breaks, minBreakSeconds, quietHours)
                            } ?: emptyList()
                        )

                        delay(SESSIONS_REFRESH_MILLIS)
                    }
                }
            }
            .flowOn(Dispatchers.Default)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    // Today's sessions up to now, for the tracked intervals
    // minus quiet hours.
    private fun sessionsSoFar(
        simulator: SensorDataSimulator,
        account: TrackedUser,
        manualBreaks: List<Long>,
        minBreakSeconds: Int,
        quietHours: QuietHours
    ): List<ActivitySession> {

        return ActivitySessions.forIntervals(
            simulator = simulator,
            intervals = TrackingLog.withoutQuietHours(
                TrackingLog.intervals(
                    context = getApplication(),
                    userId = account.uid,
                    trackingStartMillis = account.trackingStartMillis,
                    fromMillis = today.startMillis,
                    toMillis = System.currentTimeMillis()
                ),
                quietHours
            ),
            manualBreaks = manualBreaks,
            minBreakSeconds = minBreakSeconds
        )
    }

    val uiState: StateFlow<HomeUiState> =
        combine(
            livePosture,
            todaySessions,
            repository
                .observeRecordsBetween(today.startMillis, today.endMillis)
                .map { it.firstOrNull() },
            repository
                .observeRecordsBetween(
                    complianceWindow.startMillis,
                    complianceWindow.endMillis
                ),
            settings
        ) { posture, sessions, todayRecord, history, userSettings ->

            HomeUiState.from(
                posture = posture,
                sessions = ActivitySessions.summarize(sessions, System.currentTimeMillis()),
                today = todayRecord,
                history = history,
                minuteOfDay = ContextEngine.minuteOfDay(System.currentTimeMillis()),
                weather = weatherCache.read(),
                settings = userSettings
            )
        }
            // Sessions refresh every few seconds, but the state
            // only changes when a minute passes or data changes.
            .distinctUntilChanged()
            .onEach { Log.d(STATE_TAG, it.logSummary) }
            .stateIn(
                scope = viewModelScope,
                // Stops the sensor feed shortly after Home
                // leaves the screen, keeping it across rotation.
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = HomeUiState()
            )

    /*
     * "Take a break now": the next 5 minutes count as a break
     * (Home turns blue), counts a completed break for today
     * and clears the pending automatic reminder, since the
     * user is already on a break. A manual snooze is kept.
     *
     * Today's Room record is updated at once, so Analytics
     * doesn't wait for the next SensorIngestWorker run.
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
            ManualBreakStore.last(getApplication(), account.uid)

        if (lastBreak != null && now - lastBreak < BREAK_COOLDOWN_MILLIS) {
            Log.d(STATE_TAG, "Break ignored -> last one was under 5 min ago")
            return false
        }

        ManualBreakStore.record(getApplication(), account.uid, now)

        viewModelScope.launch(Dispatchers.Default) {

            val accountSettings =
                SettingsRepository.read(getApplication(), account.uid)

            val sessions =
                sessionsSoFar(
                    simulator = SensorDataSimulator.fromAssets(getApplication()),
                    account = account,
                    manualBreaks = ManualBreakStore.read(getApplication(), account.uid),
                    minBreakSeconds = accountSettings.minBreakSeconds,
                    quietHours = accountSettings.quietHours
                )

            val existing =
                repository
                    .observeRecordsBetween(today.startMillis, today.endMillis)
                    .first()
                    .firstOrNull()

            val updated =
                ActivityIngest.withTodayTotals(
                    existing = existing,
                    summary = ActivitySessions.summarize(sessions),
                    hourly = ActivitySessions.hourly(sessions, today.startMillis),
                    nowMillis = now
                )

            repository.upsertRecord(updated)

            BreakAlarmScheduler.cancelBreakReminder(getApplication(), automatic = true)

            Log.d(
                STATE_TAG,
                "Break taken -> 5-min break started, " +
                        "${updated.breaksCompleted}/${updated.breakTarget} breaks today, " +
                        "automatic reminder cleared"
            )
        }

        return true
    }

    private companion object {
        const val STATE_TAG = "HomeState"
        const val FEED_TAG = "SensorFeed"
        const val SESSIONS_REFRESH_MILLIS = 10_000L
        // Same length as a logged break, so a second tap
        // can only come once the first break is over.
        const val BREAK_COOLDOWN_MILLIS = ActivitySessions.LOGGED_BREAK_MILLIS
    }
}
