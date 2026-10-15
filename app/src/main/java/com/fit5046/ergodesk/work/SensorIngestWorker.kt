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
import com.fit5046.ergodesk.data.sensor.PostureClassifier
import com.fit5046.ergodesk.data.sensor.SensorDataSimulator
import com.fit5046.ergodesk.data.user.CurrentUser
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/*
 * Every 15 minutes: reads the last window of motion data,
 * classifies sitting vs active and adds the minutes to
 * today's ActivityRecord (A2 report, section 5.1).
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

        // Only time since the account was first signed in here counts.
        val windowStart =
            maxOf(now - WINDOW_MILLIS, user.trackingStartMillis)

        if (windowStart >= now) {
            return Result.success()
        }

        val samples =
            SensorDataSimulator.fromAssets(applicationContext)
                .samplesBetween(windowStart, now)

        val summary =
            PostureClassifier.summarize(samples)

        val repository =
            RepositoryProvider.activityRepository(applicationContext)

        val today =
            AnalyticsCalculator.periodRange(AnalyticsPeriod.DAILY, now)

        val existing =
            repository
                .observeRecordsBetween(today.startMillis, today.endMillis)
                .first()
                .firstOrNull()

        val updated =
            ActivityIngest.addToDailyRecord(existing, summary, now)

        repository.upsertRecord(updated)

        Log.d(
            TAG,
            "Window: ${samples.size} s -> $summary"
        )
        Log.d(
            TAG,
            "Today: sitting ${existing?.sittingMinutes ?: 0} -> ${updated.sittingMinutes} min, " +
                    "active ${existing?.activeMinutes ?: 0} -> ${updated.activeMinutes} min"
        )

        return Result.success()
    }

    companion object {

        private const val TAG = "SensorIngestWorker"

        private const val WORK_NAME = "sensor_ingest"

        // Matches the period, so consecutive runs do not
        // overlap. WorkManager may run a little late, which
        // can leave small gaps between windows.
        private const val WINDOW_MILLIS = 15 * 60 * 1000L

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
