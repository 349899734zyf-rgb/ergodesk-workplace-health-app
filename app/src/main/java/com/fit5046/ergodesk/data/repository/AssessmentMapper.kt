package com.fit5046.ergodesk.data.repository

import com.fit5046.ergodesk.data.model.ErgonomicAssessment
import com.fit5046.ergodesk.data.room.Assessment

fun ErgonomicAssessment.toRoomAssessment(
    active: Boolean = false
): Assessment {
    return Assessment(
        uid = id.toInt(),
        date = createdAt.toString(),
        jobType = jobType,
        startTime = workStartTime,
        endTime = workEndTime,
        concerns = (listOf(mainDiscomfortArea) + concernAreas.toList())
            .joinToString("|"),
        notes = notes,
        isActive = active
    )
}

fun Assessment.toErgonomicAssessment(): ErgonomicAssessment {
    val areas = concerns.split("|")

    return ErgonomicAssessment(
        id = uid.toLong(),
        createdAt = date.toLongOrNull() ?: System.currentTimeMillis(),
        jobType = jobType,
        mainDiscomfortArea = areas.firstOrNull() ?: "None",
        workStartTime = startTime,
        workEndTime = endTime,
        concernAreas = areas.drop(1).filter { it.isNotBlank() }.toSet(),
        notes = notes
    )
}
