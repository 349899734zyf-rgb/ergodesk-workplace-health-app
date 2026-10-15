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
import com.fit5046.ergodesk.data.context.DayOffCalendar
import com.fit5046.ergodesk.data.context.WeatherCache
import com.fit5046.ergodesk.data.repository.RepositoryProvider
import com.fit5046.ergodesk.data.sensor.ActivitySessions
import com.fit5046.ergodesk.data.sensor.ManualBreakStore
import com.fit5046.ergodesk.data.sensor.SensorDataSimulator
import com.fit5046.ergodesk.data.sensor.TrackingLog
import com.fit5046.ergodesk.data.settings.SettingsRepository
import com.fit5046.ergodesk.data.user.CurrentUser
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
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

        val user =
            CurrentUser.get(applicationContext)

        if (user == null) {
            BreakAlarmScheduler.cancelBreakReminder(applicationContext, automatic = true)
            Log.d(TAG, "No one signed in -> skipped, automatic reminder cleared")
            return Result.success()
        }

        val settings =
            SettingsRepository.read(applicationContext, user.uid)

        if (!settings.monitoringEnabled) {
            BreakAlarmScheduler.cancelBreakReminder(applicationContext, automatic = true)
            Log.d(TAG, "Background monitoring is off in Settings -> no reminders, automatic reminder cleared")
            return Result.success()
        }

        // The same sessions Home shows: a bout ends when the
        // user stands up or presses "Take a break now", and
        // never starts before they were first signed in here.
        val sessions =
            ActivitySessions.forIntervals(
                simulator = SensorDataSimulator.fromAssets(applicationContext),
                intervals = TrackingLog.withoutQuietHours(
                    TrackingLog.intervals(
                        context = applicationContext,
                        userId = user.uid,
                        trackingStartMillis = user.trackingStartMillis,
                        fromMillis = now - ActivitySessions.LOOKBACK_MILLIS,
                        toMillis = now
                    ),
                    settings.quietHours
                ),
                manualBreaks = ManualBreakStore.read(applicationContext, user.uid),
                minBreakSeconds = settings.minBreakSeconds
            )

        val sittingBoutMinutes =
            (ActivitySessions.summarize(sessions, now).currentSittingSeconds / 60L).toInt()

        val window =
            ContextEngine.complianceWindow(now)

        val history =
            RepositoryProvider.activityRepository(applicationContext)
                .observeRecordsBetween(window.startMillis, window.endMillis)
                .first()

        // Weather only counts if the user wants weather-based suggestions.
        val weather =
            WeatherCache(applicationContext).read(now).takeIf { settings.weatherAware }

        val decision =
            ContextEngine.evaluate(
                ContextInput(
                    sittingBoutMinutes = sittingBoutMinutes,
                    complianceRate = ContextEngine.complianceRate(history),
                    minuteOfDay = ContextEngine.minuteOfDay(now),
                    baseIntervalMinutes = settings.breakIntervalMinutes,
                    smartMode = settings.smartMode,
                    // TODO: calendar busy-state (needs READ_CALENDAR).
                    inMeeting = false,
                    dayOffReason =
                        if (settings.calendarAware) {
                            DayOffCalendar.dayOffReason(LocalDate.now())
                        } else {
                            null
                        },
                    weather = weather,
                    quietHours = settings.quietHours
                )
            )

        val delayMinutes =
            decision.minutesUntilDue.coerceAtLeast(0).toLong()

        // The alarm rings later than now, so check that moment
        // too: it must not fall in quiet hours or on a day off.
        val dueAtMillis =
            now + delayMinutes * 60_000L

        val heldWhenDue =
            ContextEngine.isQuietTime(ContextEngine.minuteOfDay(dueAtMillis), settings.quietHours) ||
                    (settings.calendarAware && DayOffCalendar.dayOffReason(
                        Instant.ofEpochMilli(dueAtMillis).atZone(ZoneId.systemDefault()).toLocalDate()
                    ) != null)

        // A due-time before the next run gets an alarm; any
        // other outcome clears a stale one, e.g. the user
        // has since taken a break or a meeting started.
        val scheduled =
            decision.holdReason == null &&
                    !heldWhenDue &&
                    sittingBoutMinutes > 0 &&
                    decision.minutesUntilDue <= PERIOD_MINUTES

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
