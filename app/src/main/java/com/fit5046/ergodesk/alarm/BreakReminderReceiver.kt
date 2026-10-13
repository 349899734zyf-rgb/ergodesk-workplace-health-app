package com.fit5046.ergodesk.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BreakReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("ErgoDeskAlarm", "Break reminder received")
        NotificationHelper.showBreakReminder(context)
    }
}