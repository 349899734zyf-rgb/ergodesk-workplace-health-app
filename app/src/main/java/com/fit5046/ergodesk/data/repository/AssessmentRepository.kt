package com.fit5046.ergodesk.data.repository

import androidx.compose.runtime.mutableStateListOf
import com.fit5046.ergodesk.data.model.ErgonomicAssessment
import java.util.Calendar

object AssessmentRepository {

    /*
     * Assessment 2 in-memory prototype repository.
     *
     * Add / Edit / Delete operations actually modify
     * this state list.
     *
     * In Assessment 4 this implementation can be
     * replaced by Room persistence.
     */

    private val _assessments =
        mutableStateListOf<ErgonomicAssessment>()

    val assessments:
            List<ErgonomicAssessment>
        get() = _assessments

    init {
        createPrototypeRecords()
    }

    fun addAssessment(
        assessment: ErgonomicAssessment
    ) {

        _assessments.add(
            index = 0,
            element = assessment
        )
    }

    fun updateAssessment(
        assessment: ErgonomicAssessment
    ) {

        val index =
            _assessments.indexOfFirst {
                it.id == assessment.id
            }

        if (index >= 0) {
            _assessments[index] =
                assessment
        }
    }

    fun deleteAssessment(
        id: Long
    ) {

        _assessments.removeAll {
            it.id == id
        }
    }

    fun getAssessment(
        id: Long
    ): ErgonomicAssessment? {

        return _assessments.firstOrNull {
            it.id == id
        }
    }

    private fun createPrototypeRecords() {

        if (_assessments.isNotEmpty()) {
            return
        }

        val today =
            Calendar.getInstance()

        val oneWeekAgo =
            Calendar.getInstance().apply {
                add(
                    Calendar.DAY_OF_YEAR,
                    -7
                )
            }

        _assessments.add(
            ErgonomicAssessment(
                id = 1L,
                createdAt =
                    today.timeInMillis,
                jobType =
                    "University student",
                mainDiscomfortArea =
                    "Neck",
                workStartTime =
                    "09:00",
                workEndTime =
                    "17:00",
                concernAreas =
                    setOf(
                        "Lower back"
                    ),
                notes = ""
            )
        )

        _assessments.add(
            ErgonomicAssessment(
                id = 2L,
                createdAt =
                    oneWeekAgo.timeInMillis,
                jobType =
                    "Office worker",
                mainDiscomfortArea =
                    "Shoulders",
                workStartTime =
                    "08:30",
                workEndTime =
                    "17:30",
                concernAreas =
                    emptySet(),
                notes = ""
            )
        )
    }
}