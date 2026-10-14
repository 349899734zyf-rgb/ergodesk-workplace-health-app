package com.fit5046.ergodesk.data.repository

import com.fit5046.ergodesk.data.analytics.AnalyticsCalculator
import com.fit5046.ergodesk.data.analytics.AnalyticsPeriod
import com.fit5046.ergodesk.data.local.ActivityRecordDao
import com.fit5046.ergodesk.data.model.ActivityRecord
import com.fit5046.ergodesk.data.model.ActivityRecordEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Calendar

/*
 * ActivityRepository backed by the shared Room database.
 *
 * On first use an empty table gets SEED_DAYS of history
 * before today, so weekly and monthly reports have
 * something to compare against. Today is never seeded:
 * SensorIngestWorker builds it from sensor data.
 */
class RoomActivityRepository(
    private val dao: ActivityRecordDao
) : ActivityRepository {

    private val seedLock = Mutex()

    @Volatile
    private var seeded = false

    override fun observeRecordsBetween(
        startMillis: Long,
        endMillis: Long
    ): Flow<List<ActivityRecord>> = flow {

        ensureSeeded()

        emitAll(
            dao.observeBetween(startMillis, endMillis)
                .map { rows -> rows.map { it.toRecord() } }
        )
    }

    override suspend fun upsertRecord(
        record: ActivityRecord
    ) {
        ensureSeeded()
        dao.upsert(ActivityRecordEntity.from(record))
    }

    private suspend fun ensureSeeded() {

        if (seeded) {
            return
        }

        seedLock.withLock {
            if (!seeded) {
                if (dao.count() == 0) {
                    dao.insertAll(
                        seedHistory(System.currentTimeMillis())
                            .map { ActivityRecordEntity.from(it) }
                    )
                }
                seeded = true
            }
        }
    }

    companion object {

        const val SEED_DAYS = 60

        /*
         * Demo history for the SEED_DAYS full days before
         * today, reusing the prototype pattern.
         */
        fun seedHistory(
            nowMillis: Long
        ): List<ActivityRecord> {

            val todayStart =
                AnalyticsCalculator
                    .periodRange(AnalyticsPeriod.DAILY, nowMillis)
                    .startMillis

            val firstDay =
                Calendar.getInstance().apply {
                    timeInMillis = todayStart
                    add(Calendar.DAY_OF_YEAR, -SEED_DAYS)
                }.timeInMillis

            return FakeActivityRepository
                .generatePrototypeRecords()
                .filter { it.timestamp in firstDay until todayStart }
        }
    }
}
