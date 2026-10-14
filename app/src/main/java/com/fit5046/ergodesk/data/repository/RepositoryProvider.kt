package com.fit5046.ergodesk.data.repository

/*
 * Single place that decides which repository
 * implementation the app uses.
 *
 * TODO(merge with Room): replace FakeActivityRepository
 * with the Room-backed implementation.
 */
object RepositoryProvider {

    val activityRepository: ActivityRepository by lazy {
        FakeActivityRepository()
    }
}
