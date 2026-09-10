package com.fit5046.ergodesk.data.model

data class ActivityRecord(
    val timestamp: Long,
    val sittingMinutes: Int,
    val activeMinutes: Int,
    val breaksCompleted: Int,
    val breakTarget: Int
)