package com.fit5046.ergodesk.data.user

import android.content.Context
import com.google.firebase.auth.FirebaseAuth

/*
 * The signed-in account whose activity is being tracked.
 *
 * trackingStartMillis is when this account was first signed
 * in on this device: sensing, the timeline and totals never
 * count time from before then, so a new user starts at zero
 * even if the account itself was created earlier elsewhere.
 */
data class TrackedUser(
    val uid: String,
    val email: String?,
    val trackingStartMillis: Long
) {

    // The demo account gets seeded history for presentations.
    val isDemo: Boolean
        get() = email.equals(CurrentUser.DEMO_EMAIL, ignoreCase = true)
}

object CurrentUser {

    const val DEMO_EMAIL = "demo@ergodesk.com"

    private const val PREFS_NAME = "tracking_start"

    /*
     * Null when nobody is signed in. Works from workers too,
     * since Firebase keeps the session on the device.
     */
    fun get(
        context: Context
    ): TrackedUser? {

        val user =
            FirebaseAuth.getInstance().currentUser ?: return null

        return TrackedUser(
            uid = user.uid,
            email = user.email,
            trackingStartMillis = trackingStart(context, user.uid)
        )
    }

    /*
     * Recorded the first time an account is seen on this
     * device, then kept in SharedPreferences.
     */
    @Synchronized
    private fun trackingStart(
        context: Context,
        uid: String
    ): Long {

        val prefs =
            context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val saved =
            prefs.getLong(uid, 0L)

        if (saved > 0L) {
            return saved
        }

        val now =
            System.currentTimeMillis()

        prefs.edit().putLong(uid, now).apply()

        return now
    }
}
