package com.fit5046.ergodesk.data.repository

import com.fit5046.ergodesk.data.model.ActivityRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.util.Calendar
import kotlin.math.abs

/*
 * In-memory ActivityRepository used until the Room
 * database is merged. Unit tests can pass their own
 * records through the constructor.
 */
class FakeActivityRepository(
    initialRecords: List<ActivityRecord> =
        generatePrototypeRecords()
) : ActivityRepository {

    private val records =
        MutableStateFlow(
            initialRecords.sortedBy { it.timestamp }
        )

    override fun observeRecordsBetween(
        startMillis: Long,
        endMillis: Long
    ): Flow<List<ActivityRecord>> {

        return records.map { all ->
            all.filter {
                it.timestamp >= startMillis &&
                        it.timestamp < endMillis
            }
        }
    }

    override suspend fun upsertRecord(
        record: ActivityRecord
    ) {

        records.update { all ->
            (all.filterNot {
                isSameDay(it.timestamp, record.timestamp)
            } + record).sortedBy { it.timestamp }
        }
    }

    companion object {

        /*
         * One record per day for a year either side of today.
         * Values vary by weekday so that the weekly and monthly
         * totals in Analytics are not flat.
         */
        fun generatePrototypeRecords(): List<ActivityRecord> {

            return (-365..365).map { offset ->

                val calendar =
                    Calendar.getInstance().apply {
                        add(Calendar.DAY_OF_YEAR, offset)
                        set(Calendar.HOUR_OF_DAY, 12)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }

                val pattern =
                    abs(offset % 7)

                ActivityRecord(
                    timestamp = calendar.timeInMillis,
                    sittingMinutes = 330 + pattern * 14,
                    activeMinutes = 105 + (6 - pattern) * 8,
                    breaksCompleted = 4 + pattern % 4,
                    breakTarget = 8
                )
            }
        }

        private fun isSameDay(
            firstMillis: Long,
            secondMillis: Long
        ): Boolean {

            val first =
                Calendar.getInstance().apply {
                    timeInMillis = firstMillis
                }

            val second =
                Calendar.getInstance().apply {
                    timeInMillis = secondMillis
                }

            return first.get(Calendar.YEAR) ==
                    second.get(Calendar.YEAR) &&
                    first.get(Calendar.DAY_OF_YEAR) ==
                    second.get(Calendar.DAY_OF_YEAR)
        }
    }
}
