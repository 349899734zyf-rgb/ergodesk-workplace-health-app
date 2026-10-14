package com.fit5046.ergodesk.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.util.Log

object BreakAlarmScheduler {

    private const val MANUAL_REQUEST_CODE = 1001
    private const val AUTO_REQUEST_CODE = 1002
    private const val TAG = "ErgoDeskAlarm"

    fun scheduleBreakReminder(
        context: Context,
        delayMinutes: Long,
        automatic: Boolean = false
    ) {
        val alarmManager =
            context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(
            context,
            BreakReminderReceiver::class.java
        )

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            if (automatic) AUTO_REQUEST_CODE else MANUAL_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerTime =
            SystemClock.elapsedRealtime() + (delayMinutes * 60_000L)

        alarmManager.setAndAllowWhileIdle(
            AlarmManager.ELAPSED_REALTIME_WAKEUP,
            triggerTime,
            pendingIntent
        )

        Log.d(TAG, "Break reminder scheduled in $delayMinutes minutes")
    }

    fun cancelBreakReminder(context: Context, automatic: Boolean = false) {

        val alarmManager =
            context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(
            context,
            BreakReminderReceiver::class.java
        )

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            if (automatic) AUTO_REQUEST_CODE else MANUAL_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pendingIntent)

        Log.d(TAG, "Break reminder cancelled")
    }
}