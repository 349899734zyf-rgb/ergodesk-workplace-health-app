package com.fit5046.ergodesk.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room database entity representing a stretch exercise routine.
 * Used for Assessment 4 data persistence.
 */
@Entity(tableName = "stretches")
data class StretchItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val targetArea: String,
    val durationSeconds: Int,
    val difficulty: String,
    val instructions: String, // Comma or newline separated steps
    val isCustom: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
