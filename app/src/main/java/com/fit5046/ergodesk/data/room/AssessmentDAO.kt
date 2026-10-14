package com.fit5046.ergodesk.data.room

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AssessmentDAO {

    @Query("SELECT * FROM assessments")
    fun getAllAssessments(): Flow<List<Assessment>>

    @Insert
    suspend fun insertAssessment(assessment: Assessment)

    @Update
    suspend fun updateAssessment(assessment: Assessment)

    @Delete
    suspend fun deleteAssessment(assessment: Assessment)

    @Query("SELECT * FROM assessments WHERE isActive = 1 LIMIT 1")
    fun getActiveAssessment(): Flow<Assessment?>
}
