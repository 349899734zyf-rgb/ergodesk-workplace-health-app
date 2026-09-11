package com.fit5046.ergodesk.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BreakReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        NotificationHelper.showBreakReminder(context)
    }
}