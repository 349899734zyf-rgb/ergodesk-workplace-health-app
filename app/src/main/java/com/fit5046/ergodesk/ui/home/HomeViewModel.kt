package com.fit5046.ergodesk.ui.home

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fit5046.ergodesk.data.analytics.AnalyticsCalculator
import com.fit5046.ergodesk.data.analytics.AnalyticsPeriod
import com.fit5046.ergodesk.data.context.ContextEngine
import com.fit5046.ergodesk.data.repository.RepositoryProvider
import com.fit5046.ergodesk.data.sensor.PostureClassifier
import com.fit5046.ergodesk.data.sensor.PostureState
import com.fit5046.ergodesk.data.sensor.SensorDataSimulator
import com.fit5046.ergodesk.data.sensor.SittingBoutTracker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
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
                )
        ) { live, todayRecord, history ->

            HomeUiState.from(
                posture = live.posture,
                sittingBoutSeconds = live.sittingBoutSeconds,
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

    private companion object {
        const val STATE_TAG = "HomeState"
    }
}
