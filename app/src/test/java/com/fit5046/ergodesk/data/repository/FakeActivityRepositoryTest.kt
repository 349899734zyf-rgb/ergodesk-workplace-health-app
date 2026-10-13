package com.fit5046.ergodesk.data.repository

import com.fit5046.ergodesk.data.model.ActivityRecord
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class FakeActivityRepositoryTest {

    private fun millis(day: Int, hour: Int = 12): Long {
        return Calendar.getInstance().apply {
            clear()
            set(2026, Calendar.OCTOBER, day, hour, 0, 0)
        }.timeInMillis
    }

    private fun record(timestamp: Long, sitting: Int = 300) =
        ActivityRecord(timestamp, sitting, 100, 5, 8)

    @Test
    fun observeRecordsBetween_returnsOnlyRecordsInRange_endExclusive() = runBlocking {
        val repository = FakeActivityRepository(
            listOf(record(millis(12)), record(millis(13)), record(millis(14)))
        )

        val result = repository
            .observeRecordsBetween(millis(13, hour = 0), millis(14, hour = 0))
            .first()

        assertEquals(listOf(millis(13)), result.map { it.timestamp })
    }

    @Test
    fun upsertRecord_replacesRecordForSameDay() = runBlocking {
        val repository = FakeActivityRepository(listOf(record(millis(13), sitting = 300)))

        repository.upsertRecord(record(millis(13, hour = 18), sitting = 420))

        val result = repository
            .observeRecordsBetween(millis(13, hour = 0), millis(14, hour = 0))
            .first()

        assertEquals(1, result.size)
        assertEquals(420, result.single().sittingMinutes)
    }

    @Test
    fun upsertRecord_addsNewDayInTimestampOrder() = runBlocking {
        val repository = FakeActivityRepository(listOf(record(millis(14))))

        repository.upsertRecord(record(millis(12)))

        val result = repository
            .observeRecordsBetween(millis(1), millis(30))
            .first()

        assertEquals(listOf(millis(12), millis(14)), result.map { it.timestamp })
    }

    @Test
    fun prototypeRecords_haveOneRecordPerDayIncludingToday() {
        val records = FakeActivityRepository.generatePrototypeRecords()

        assertEquals(731, records.size)

        val today = Calendar.getInstance()
        assertTrue(records.any {
            val day = Calendar.getInstance().apply { timeInMillis = it.timestamp }
            day.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                    day.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
        })
    }
}
