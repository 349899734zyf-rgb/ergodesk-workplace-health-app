package com.fit5046.ergodesk.data.repository
import com.fit5046.ergodesk.data.room.Assessment
import com.fit5046.ergodesk.data.room.AssessmentDAO

class RoomAssessmentRepository(
    private val assessmentDAO: AssessmentDAO,
    private val userId: String
) {
    val allAssessments = assessmentDAO.getAllAssessments(userId)

    val activeAssessment = assessmentDAO.getActiveAssessment(userId)

    suspend fun insertAssessment(assessment: Assessment) {
        assessmentDAO.insertAssessment(assessment.copy(userId = userId))
    }

    suspend fun updateAssessment(assessment: Assessment) {
        assessmentDAO.updateAssessment(assessment.uid, userId, assessment.date, assessment.jobType, assessment.startTime, assessment.endTime, assessment.concerns, assessment.notes, assessment.isActive).also { check(it > 0) }
    }

    suspend fun deleteAssessment(assessment: Assessment) {
        assessmentDAO.deleteAssessment(assessment.uid, userId).also { check(it > 0) }
    }
}
