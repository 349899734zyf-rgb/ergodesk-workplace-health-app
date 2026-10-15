
package com.fit5046.ergodesk.alarm

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.fit5046.ergodesk.MainActivity
import com.fit5046.ergodesk.R
import com.fit5046.ergodesk.data.settings.SettingsRepository
import com.fit5046.ergodesk.data.user.CurrentUser

object NotificationHelper {

    private const val NOTIFICATION_ID = 1001

    fun showBreakReminder(context: Context) {

        val userId = CurrentUser.get(context)?.uid ?: return
        val style = SettingsRepository.read(context, userId).reminderStyle

        val channelId = when (style) {
            "Sound + notification" -> "break_sound"
            "Vibration only" -> "break_vibration"
            "Silent banner" -> "break_silent"
            else -> "break_notification"
        }

        createNotificationChannel(context, channelId)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            2001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Time for a break")
            .setContentText(
                "You've been sitting for a while. Take a short movement break."
            )
            .setPriority(
                if (style == "Silent banner")
                    NotificationCompat.PRIORITY_LOW
                else
                    NotificationCompat.PRIORITY_DEFAULT
            )
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        if (
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            NotificationManagerCompat.from(context)
                .notify(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotificationChannel(
        context: Context,
        channelId: String
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val importance = when (channelId) {
            "break_silent" -> NotificationManager.IMPORTANCE_LOW
            else -> NotificationManager.IMPORTANCE_DEFAULT
        }

        val channel = NotificationChannel(
            channelId,
            "Break Reminders",
            importance
        )

        when (channelId) {
            "break_sound" -> {
                channel.enableVibration(false)
                channel.setSound(
                    RingtoneManager.getDefaultUri(
                        RingtoneManager.TYPE_NOTIFICATION
                    ),
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .build()
                )
            }

            "break_vibration" -> {
                channel.setSound(null, null)
                channel.enableVibration(true)
            }

            "break_silent" -> {
                channel.setSound(null, null)
                channel.enableVibration(false)
            }

            else -> {
                channel.setSound(null, null)
                channel.enableVibration(false)
            }
        }

        val manager = context.getSystemService(
            Context.NOTIFICATION_SERVICE
        ) as NotificationManager

        manager.createNotificationChannel(channel)
    }
}
