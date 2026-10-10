package com.fit5046.ergodesk.data.repository
import com.fit5046.ergodesk.data.room.Assessment
import com.fit5046.ergodesk.data.room.AssessmentDAO

class RoomAssessmentRepository(
    private val assessmentDAO: AssessmentDAO
) {
    val allAssessments = assessmentDAO.getAllAssessments()

    val activeAssessment = assessmentDAO.getActiveAssessment()

    suspend fun insertAssessment(assessment: Assessment) {
        assessmentDAO.insertAssessment(assessment)
    }

    suspend fun updateAssessment(assessment: Assessment) {
        assessmentDAO.updateAssessment(assessment)
    }

    suspend fun deleteAssessment(assessment: Assessment) {
        assessmentDAO.deleteAssessment(assessment)
    }
}
