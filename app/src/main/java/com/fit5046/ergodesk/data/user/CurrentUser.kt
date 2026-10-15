package com.fit5046.ergodesk.data.user

import com.google.firebase.auth.FirebaseAuth

/*
 * The signed-in account whose activity is being tracked.
 *
 * trackingStartMillis is when the account was created:
 * sensing, the timeline and totals never count time from
 * before then, so a new account starts at zero.
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

    /*
     * Null when nobody is signed in. Works from workers too,
     * since Firebase keeps the session on the device.
     */
    fun get(): TrackedUser? {

        val user =
            FirebaseAuth.getInstance().currentUser ?: return null

        return TrackedUser(
            uid = user.uid,
            email = user.email,
            trackingStartMillis = user.metadata?.creationTimestamp ?: 0L
        )
    }
}
