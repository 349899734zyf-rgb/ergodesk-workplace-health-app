package com.fit5046.ergodesk.data.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "assessments")
data class Assessment(
    @PrimaryKey(autoGenerate = true)
    val uid: Int = 0,
    val userId: String = "",
    val date: String,
    val jobType: String,
    val startTime: String,
    val endTime: String,
    val concerns: String,
    val notes: String,
    val isActive: Boolean = false
)