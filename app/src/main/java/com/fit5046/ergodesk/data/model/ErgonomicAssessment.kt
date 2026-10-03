package com.fit5046.ergodesk.data.model

data class ErgonomicAssessment(
    val id: Long,
    val createdAt: Long,
    val jobType: String,
    val mainDiscomfortArea: String,
    val workStartTime: String,
    val workEndTime: String,
    val concernAreas: Set<String>,
    val notes: String
)