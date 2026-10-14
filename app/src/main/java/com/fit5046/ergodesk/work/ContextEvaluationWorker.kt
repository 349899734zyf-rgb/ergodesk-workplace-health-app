package com.fit5046.ergodesk.work

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.fit5046.ergodesk.alarm.BreakAlarmScheduler
import com.fit5046.ergodesk.data.context.ContextEngine
import com.fit5046.ergodesk.data.context.ContextInput
import com.fit5046.ergodesk.data.context.WeatherCache
import com.fit5046.ergodesk.data.repository.RepositoryProvider
import com.fit5046.ergodesk.data.sensor.PostureClassifier
import com.fit5046.ergodesk.data.sensor.SensorDataSimulator
import com.fit5046.ergodesk.data.sensor.SittingBoutTracker
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.concurrent.TimeUnit

/*
 * Every 15 minutes: works out how long the user has been
 * sitting, asks ContextEngine whether a break is due and
 * hands exact delivery to AlarmManager (A2 report, 5.1).
 *
 * WorkManager may run late under Doze, so the worker only
 * decides; the alarm fires at the precise due time.
 */
class ContextEvaluationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {

        val now =
            System.currentTimeMillis()

        val samples =
            SensorDataSimulator.fromAssets(applicationContext)
                .samplesBetween(now - SittingBoutTracker.LOOKBACK_MILLIS, now)

        val sittingBoutMinutes =
            PostureClassifier.sittingBoutSeconds(samples) / 60

        val window =
            ContextEngine.complianceWindow(now)

        val history =
            RepositoryProvider.activityRepository
                .observeRecordsBetween(window.startMillis, window.endMillis)
                .first()

        val weather =
            WeatherCache(applicationContext).read(now)

        val decision =
            ContextEngine.evaluate(
                ContextInput(
                    sittingBoutMinutes = sittingBoutMinutes,
                    complianceRate = ContextEngine.complianceRate(history),
                    hourOfDay = Calendar.getInstance().apply {
                        timeInMillis = now
                    }.get(Calendar.HOUR_OF_DAY),
                    // TODO: calendar busy-state (needs READ_CALENDAR).
                    inMeeting = false,
                    weather = weather
                )
            )

        // A due-time before the next run gets an alarm; any
        // other outcome clears a stale one, e.g. the user
        // has since taken a break or a meeting started.
        val scheduled =
            decision.holdReason == null &&
                    sittingBoutMinutes > 0 &&
                    decision.minutesUntilDue <= PERIOD_MINUTES

        val delayMinutes =
            decision.minutesUntilDue.coerceAtLeast(0).toLong()

        if (scheduled) {
            // TODO(Suzie): pass decision.style and
            // decision.suggestion once the receiver accepts them.
            BreakAlarmScheduler.scheduleBreakReminder(
                applicationContext,
                delayMinutes,
            automatic = true
            )
        } else {
            BreakAlarmScheduler.cancelBreakReminder(applicationContext, automatic = true)
        }

        Log.d(
            TAG,
            "Sitting $sittingBoutMinutes min -> $decision, " +
                    if (scheduled) "alarm set in $delayMinutes min" else "alarm cleared"
        )

        Log.d(TAG, "Weather used: ${weather ?: "none cached yet"}")

        return Result.success()
    }

    companion object {

        private const val TAG = "ContextEvaluationWorker"

        private const val WORK_NAME = "context_evaluation"

        private const val PERIOD_MINUTES = 15L

        /*
         * Safe to call on every app start: KEEP leaves an
         * already scheduled job alone.
         */
        fun schedule(
            context: Context
        ) {

            val request =
                PeriodicWorkRequestBuilder<ContextEvaluationWorker>(
                    PERIOD_MINUTES, TimeUnit.MINUTES
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
