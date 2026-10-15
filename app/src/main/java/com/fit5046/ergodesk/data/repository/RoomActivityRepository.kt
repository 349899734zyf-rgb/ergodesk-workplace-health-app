package com.fit5046.ergodesk.data.repository

import com.fit5046.ergodesk.data.analytics.AnalyticsCalculator
import com.fit5046.ergodesk.data.analytics.AnalyticsPeriod
import com.fit5046.ergodesk.data.local.ActivityRecordDao
import com.fit5046.ergodesk.data.model.ActivityRecord
import com.fit5046.ergodesk.data.model.ActivityRecordEntity
import com.fit5046.ergodesk.data.user.CurrentUser
import com.fit5046.ergodesk.data.user.TrackedUser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Calendar

/*
 * ActivityRepository backed by the shared Room database,
 * scoped to the signed-in account (CurrentUser).
 *
 * New accounts start empty. Only the demo account gets
 * SEED_DAYS of history before today on first use, so
 * weekly and monthly reports have something to show in
 * presentations. Today is never seeded.
 */
class RoomActivityRepository(
    private val dao: ActivityRecordDao,
    private val currentUser: () -> TrackedUser? = CurrentUser::get
) : ActivityRepository {

    private val seedLock = Mutex()

    private val checkedUsers = mutableSetOf<String>()

    override fun observeRecordsBetween(
        startMillis: Long,
        endMillis: Long
    ): Flow<List<ActivityRecord>> = flow {

        val user =
            currentUser()

        if (user == null) {
            emit(emptyList())
            return@flow
        }

        seedDemoHistory(user)

        emitAll(
            dao.observeBetween(user.uid, startMillis, endMillis)
                .map { rows -> rows.map { it.toRecord() } }
        )
    }

    /*
     * Ignored when nobody is signed in.
     */
    override suspend fun upsertRecord(
        record: ActivityRecord
    ) {

        val user =
            currentUser() ?: return

        seedDemoHistory(user)
        dao.upsert(ActivityRecordEntity.from(record, user.uid))
    }

    private suspend fun seedDemoHistory(
        user: TrackedUser
    ) {

        if (!user.isDemo) {
            return
        }

        seedLock.withLock {
            if (checkedUsers.add(user.uid) && dao.countFor(user.uid) == 0) {
                dao.insertAll(
                    seedHistory(System.currentTimeMillis())
                        .map { ActivityRecordEntity.from(it, user.uid) }
                )
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
