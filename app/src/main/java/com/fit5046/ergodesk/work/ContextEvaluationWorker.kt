package com.fit5046.ergodesk.work

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.fit5046.ergodesk.alarm.BreakAlarmScheduler
import com.fit5046.ergodesk.data.analytics.AnalyticsCalculator
import com.fit5046.ergodesk.data.analytics.AnalyticsPeriod
import com.fit5046.ergodesk.data.context.ContextEngine
import com.fit5046.ergodesk.data.context.ContextInput
import com.fit5046.ergodesk.data.repository.RepositoryProvider
import com.fit5046.ergodesk.data.sensor.PostureClassifier
import com.fit5046.ergodesk.data.sensor.SensorDataSimulator
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
                .samplesBetween(now - LOOKBACK_MILLIS, now)

        val sittingBoutMinutes =
            PostureClassifier.sittingBoutSeconds(samples) / 60

        val today =
            AnalyticsCalculator.periodRange(AnalyticsPeriod.DAILY, now)

        val historyStart =
            Calendar.getInstance().apply {
                timeInMillis = today.startMillis
                add(Calendar.DAY_OF_YEAR, -COMPLIANCE_DAYS)
            }.timeInMillis

        val history =
            RepositoryProvider.activityRepository
                .observeRecordsBetween(historyStart, today.startMillis)
                .first()

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
                    // TODO: read the cache WeatherSyncWorker fills.
                    weather = null
                )
            )

        // A due-time before the next run gets an alarm; any
        // other outcome clears a stale one, e.g. the user
        // has since taken a break or a meeting started.
        val scheduled =
            decision.holdReason == null &&
                    sittingBoutMinutes > 0 &&
                    decision.minutesUntilDue <= PERIOD_MINUTES

        if (scheduled) {
            // TODO(Suzie): pass decision.style and
            // decision.suggestion once the receiver accepts them.
            BreakAlarmScheduler.scheduleBreakReminder(
                applicationContext,
                decision.minutesUntilDue.coerceAtLeast(0).toLong()
            )
        } else {
            BreakAlarmScheduler.cancelBreakReminder(applicationContext)
        }

        Log.d(
            TAG,
            "Sitting ${sittingBoutMinutes} min -> $decision, alarm ${if (scheduled) "set" else "cleared"}"
        )

        return Result.success()
    }

    companion object {

        private const val TAG = "ContextEvaluationWorker"

        private const val WORK_NAME = "context_evaluation"

        private const val PERIOD_MINUTES = 15L

        // Long enough to see the start of a bout that runs
        // well past the relaxed 45-minute interval.
        private const val LOOKBACK_MILLIS = 2 * 60 * 60 * 1000L

        private const val COMPLIANCE_DAYS = 7

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
