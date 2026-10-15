package com.fit5046.ergodesk.work

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.fit5046.ergodesk.data.context.WeatherCache
import com.fit5046.ergodesk.data.context.WeatherSnapshot
import com.fit5046.ergodesk.data.repository.WeatherRepository
import java.util.concurrent.TimeUnit

/*
 * Every 15 minutes, when online and the battery is not
 * low: fetches current weather through WeatherRepository
 * (Retrofit, Member 4) and caches it for
 * ContextEvaluationWorker (A2 report, section 5.1).
 */
class WeatherSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {

        val repository =
            WeatherRepository()

        return repository.getWeather(CITY).fold(
            onSuccess = { response ->

                val snapshot =
                    WeatherSnapshot(
                        temperatureC = response.current.tempC,
                        condition = response.current.condition.text,
                        isOutdoorFriendly =
                            repository.deriveErgonomicAdvice(response).isOutdoorFriendly
                    )

                WeatherCache(applicationContext).save(snapshot)

                Log.d(TAG, "Fetched $CITY: $snapshot -> cached")

                Result.success()
            },
            onFailure = { error ->

                // WorkManager retries with backoff; the old
                // cache stays usable until it expires.
                Log.w(TAG, "Fetch failed: ${error.message} -> retry later")

                Result.retry()
            }
        )
    }

    companion object {

        private const val TAG = "WeatherSyncWorker"

        private const val WORK_NAME = "weather_sync"

        // Same city as WeatherViewModel on Home.
        private const val CITY = "Melbourne"

        /*
         * Safe to call on every app start: KEEP leaves an
         * already scheduled job alone.
         */
        fun schedule(
            context: Context
        ) {

            val constraints =
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .setRequiresBatteryNotLow(true)
                    .build()

            val request =
                PeriodicWorkRequestBuilder<WeatherSyncWorker>(
                    15, TimeUnit.MINUTES
                )
                    .setConstraints(constraints)
                    .build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    request
                )
        }
    }
}
