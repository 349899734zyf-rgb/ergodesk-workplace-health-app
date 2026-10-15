package com.fit5046.ergodesk.work

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.fit5046.ergodesk.data.analytics.AnalyticsCalculator
import com.fit5046.ergodesk.data.analytics.AnalyticsPeriod
import com.fit5046.ergodesk.data.repository.RepositoryProvider
import com.fit5046.ergodesk.data.sensor.ActivityIngest
import com.fit5046.ergodesk.data.sensor.ActivitySessions
import com.fit5046.ergodesk.data.sensor.ManualBreakStore
import com.fit5046.ergodesk.data.sensor.SensorDataSimulator
import com.fit5046.ergodesk.data.sensor.TrackingLog
import com.fit5046.ergodesk.data.settings.SettingsRepository
import com.fit5046.ergodesk.data.user.CurrentUser
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/*
 * Every 15 minutes: rebuilds today's sitting and break
 * sessions from the motion data and stores the day's
 * sitting and active minutes in Room (A2 report, 5.1).
 * Recomputing the whole day keeps the totals exact even
 * when WorkManager runs late.
 *
 * Motion comes from SensorDataSimulator until real phone
 * sensors are wired in.
 */
class SensorIngestWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {

        val now =
            System.currentTimeMillis()

        val user =
            CurrentUser.get(applicationContext)

        if (user == null) {
            Log.d(TAG, "No one signed in -> skipped")
            return Result.success()
        }

        val settings =
            SettingsRepository.read(applicationContext, user.uid)

        val today =
            AnalyticsCalculator.periodRange(AnalyticsPeriod.DAILY, now)

        // Only time when sensing was on counts: since the account
        // was first signed in here, and with background
        // monitoring off, only while the app was open.
        val sessions =
            ActivitySessions.forIntervals(
                simulator = SensorDataSimulator.fromAssets(applicationContext),
                intervals = TrackingLog.intervals(
                    context = applicationContext,
                    userId = user.uid,
                    trackingStartMillis = user.trackingStartMillis,
                    fromMillis = today.startMillis,
                    toMillis = now
                ),
                manualBreaks = ManualBreakStore.read(applicationContext, user.uid),
                minBreakSeconds = settings.minBreakSeconds
            )

        val summary =
            ActivitySessions.summarize(sessions)

        val repository =
            RepositoryProvider.activityRepository(applicationContext)

        val existing =
            repository
                .observeRecordsBetween(today.startMillis, today.endMillis)
                .first()
                .firstOrNull()

        val updated =
            ActivityIngest.withTodayTotals(
                existing = existing,
                summary = summary,
                hourly = ActivitySessions.hourly(sessions, today.startMillis),
                nowMillis = now
            )

        repository.upsertRecord(updated)

        Log.d(
            TAG,
            "Today so far: ${sessions.size} sessions -> " +
                    "sitting ${existing?.sittingMinutes ?: 0} -> ${updated.sittingMinutes} min, " +
                    "active ${existing?.activeMinutes ?: 0} -> ${updated.activeMinutes} min"
        )

        // What the sensor showed most recently, as evidence the
        // worker detected the changes between sitting and breaks.
        val clock =
            SimpleDateFormat("HH:mm:ss", Locale.ENGLISH)

        Log.d(
            TAG,
            "Detected recently: " +
                    sessions.takeLast(RECENT_SESSIONS_LOGGED).joinToString(", ") { session ->
                        "${session.type} ${clock.format(Date(session.startMillis))}-" +
                                "${clock.format(Date(session.endMillis))} (${session.minutes} min)"
                    }
        )

        return Result.success()
    }

    companion object {

        private const val TAG = "SensorIngestWorker"

        private const val WORK_NAME = "sensor_ingest"

        private const val RECENT_SESSIONS_LOGGED = 3

        /*
         * Safe to call on every app start: KEEP leaves an
         * already scheduled job alone.
         */
        fun schedule(
            context: Context
        ) {

            val request =
                PeriodicWorkRequestBuilder<SensorIngestWorker>(
                    15, TimeUnit.MINUTES
                ).build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    request
                )
        }
    }
}
