package com.fit5046.ergodesk.data.sensor

import android.content.Context
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.fit5046.ergodesk.data.settings.SettingsRepository
import com.fit5046.ergodesk.data.user.CurrentUser

/*
 * Decides when sensing is on and records it in TrackingLog:
 * always while ErgoDesk is in the foreground, and in the
 * background only when "Enable sitting monitoring" is on.
 * Closing the app or switching to another app with
 * monitoring off pauses sensing until the user returns.
 *
 * Installed once from MainActivity; update() is also called
 * after sign-in and after Settings are saved.
 */
object ForegroundTracker : DefaultLifecycleObserver {

    private const val TAG = "ForegroundTracker"

    private lateinit var appContext: Context

    @Volatile
    private var installed = false

    @Volatile
    var isInForeground = false
        private set

    @Synchronized
    fun install(
        context: Context
    ) {
        if (installed) {
            return
        }
        appContext = context.applicationContext
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        installed = true
    }

    override fun onStart(owner: LifecycleOwner) {
        isInForeground = true
        update()
    }

    override fun onStop(owner: LifecycleOwner) {
        isInForeground = false
        update()
    }

    fun update() {

        if (!installed) {
            return
        }

        val user =
            CurrentUser.get(appContext) ?: return

        val backgroundMonitoring =
            SettingsRepository.read(appContext, user.uid).monitoringEnabled

        val active =
            isInForeground || backgroundMonitoring

        TrackingLog.setActive(appContext, user.uid, user.trackingStartMillis, active)

        Log.d(
            TAG,
            "Sensing ${if (active) "on" else "paused"} " +
                    "(app ${if (isInForeground) "open" else "in background"}, " +
                    "background monitoring ${if (backgroundMonitoring) "on" else "off"})"
        )
    }
}
