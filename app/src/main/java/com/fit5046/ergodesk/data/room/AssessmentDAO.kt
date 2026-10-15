package com.fit5046.ergodesk.data.room

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AssessmentDAO {

    @Query("SELECT * FROM assessments WHERE userId = :userId")
    fun getAllAssessments(userId: String): Flow<List<Assessment>>

    @Insert
    suspend fun insertAssessment(assessment: Assessment)

    @Query("UPDATE assessments SET date = :date, jobType = :jobType, startTime = :startTime, endTime = :endTime, concerns = :concerns, notes = :notes, isActive = :isActive WHERE uid = :uid AND userId = :userId")
    suspend fun updateAssessment(uid: Int, userId: String, date: String, jobType: String, startTime: String, endTime: String, concerns: String, notes: String, isActive: Boolean): Int

    @Query("DELETE FROM assessments WHERE uid = :uid AND userId = :userId")
    suspend fun deleteAssessment(uid: Int, userId: String): Int

    @Query("SELECT * FROM assessments WHERE isActive = 1 AND userId = :userId LIMIT 1")
    fun getActiveAssessment(userId: String): Flow<Assessment?>
}
