package com.fit5046.ergodesk.data.repository

import com.fit5046.ergodesk.data.model.ActivityRecord
import java.util.Calendar
import kotlin.math.abs

object AnalyticsRepository {

    /*
     * Assessment 2 prototype data source.
     *
     * Raw daily records are generated here.
     * Daily, Weekly and Monthly results are NOT hard-coded
     * in AnalyticsScreen.
     *
     * In Assessment 4, this repository can be replaced
     * with Room and activity-sensing data.
     */

    private val records: List<ActivityRecord> by lazy {
        generatePrototypeRecords()
    }

    fun getDailyRecords(
        selectedDateMillis: Long
    ): List<ActivityRecord> {

        return records.filter {
            isSameDay(
                it.timestamp,
                selectedDateMillis
            )
        }
    }

    fun getWeeklyRecords(
        selectedDateMillis: Long
    ): List<ActivityRecord> {

        val start =
            startOfWeek(
                selectedDateMillis
            )

        val end =
            Calendar.getInstance().apply {
                timeInMillis = start
                add(
                    Calendar.DAY_OF_YEAR,
                    7
                )
            }.timeInMillis

        return records.filter {
            it.timestamp >= start &&
                    it.timestamp < end
        }
    }

    fun getMonthlyRecords(
        selectedDateMillis: Long
    ): List<ActivityRecord> {

        val selected =
            Calendar.getInstance().apply {
                timeInMillis =
                    selectedDateMillis
            }

        return records.filter { record ->

            val recordDate =
                Calendar.getInstance().apply {
                    timeInMillis =
                        record.timestamp
                }

            recordDate.get(Calendar.YEAR) ==
                    selected.get(Calendar.YEAR) &&
                    recordDate.get(Calendar.MONTH) ==
                    selected.get(Calendar.MONTH)
        }
    }

    fun getPreviousDailyRecords(
        selectedDateMillis: Long
    ): List<ActivityRecord> {

        val previous =
            Calendar.getInstance().apply {
                timeInMillis =
                    selectedDateMillis

                add(
                    Calendar.DAY_OF_YEAR,
                    -1
                )
            }

        return getDailyRecords(
            previous.timeInMillis
        )
    }

    fun getPreviousWeeklyRecords(
        selectedDateMillis: Long
    ): List<ActivityRecord> {

        val previous =
            Calendar.getInstance().apply {
                timeInMillis =
                    selectedDateMillis

                add(
                    Calendar.DAY_OF_YEAR,
                    -7
                )
            }

        return getWeeklyRecords(
            previous.timeInMillis
        )
    }

    fun getPreviousMonthlyRecords(
        selectedDateMillis: Long
    ): List<ActivityRecord> {

        val previous =
            Calendar.getInstance().apply {
                timeInMillis =
                    selectedDateMillis

                add(
                    Calendar.MONTH,
                    -1
                )
            }

        return getMonthlyRecords(
            previous.timeInMillis
        )
    }

    private fun generatePrototypeRecords():
            List<ActivityRecord> {

        val result =
            mutableListOf<ActivityRecord>()

        for (offset in -365..365) {

            val calendar =
                Calendar.getInstance().apply {

                    add(
                        Calendar.DAY_OF_YEAR,
                        offset
                    )

                    set(
                        Calendar.HOUR_OF_DAY,
                        12
                    )

                    set(
                        Calendar.MINUTE,
                        0
                    )

                    set(
                        Calendar.SECOND,
                        0
                    )

                    set(
                        Calendar.MILLISECOND,
                        0
                    )
                }

            /*
             * Raw prototype records vary by date.
             * Weekly and Monthly totals will later
             * be calculated from these records.
             */
            val pattern =
                abs(offset % 7)

            val sittingMinutes =
                330 + pattern * 14

            val activeMinutes =
                105 + (6 - pattern) * 8

            val breaksCompleted =
                4 + pattern % 4

            result.add(
                ActivityRecord(
                    timestamp =
                        calendar.timeInMillis,
                    sittingMinutes =
                        sittingMinutes,
                    activeMinutes =
                        activeMinutes,
                    breaksCompleted =
                        breaksCompleted,
                    breakTarget = 8
                )
            )
        }

        return result
    }

    private fun isSameDay(
        firstMillis: Long,
        secondMillis: Long
    ): Boolean {

        val first =
            Calendar.getInstance().apply {
                timeInMillis =
                    firstMillis
            }

        val second =
            Calendar.getInstance().apply {
                timeInMillis =
                    secondMillis
            }

        return first.get(Calendar.YEAR) ==
                second.get(Calendar.YEAR) &&
                first.get(Calendar.DAY_OF_YEAR) ==
                second.get(Calendar.DAY_OF_YEAR)
    }

    private fun startOfWeek(
        selectedDateMillis: Long
    ): Long {

        val calendar =
            Calendar.getInstance().apply {

                timeInMillis =
                    selectedDateMillis

                firstDayOfWeek =
                    Calendar.MONDAY

                set(
                    Calendar.DAY_OF_WEEK,
                    Calendar.MONDAY
                )

                set(
                    Calendar.HOUR_OF_DAY,
                    0
                )

                set(
                    Calendar.MINUTE,
                    0
                )

                set(
                    Calendar.SECOND,
                    0
                )

                set(
                    Calendar.MILLISECOND,
                    0
                )
            }

        return calendar.timeInMillis
    }
}