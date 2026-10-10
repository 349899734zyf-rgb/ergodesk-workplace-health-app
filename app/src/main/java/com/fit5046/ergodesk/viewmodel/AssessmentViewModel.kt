
package com.fit5046.ergodesk.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fit5046.ergodesk.data.repository.RoomAssessmentRepository
import com.fit5046.ergodesk.data.room.Assessment
import com.fit5046.ergodesk.data.room.ErgoDeskDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AssessmentViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: RoomAssessmentRepository

    val allAssessments: kotlinx.coroutines.flow.Flow<List<Assessment>>
    val activeAssessment: kotlinx.coroutines.flow.Flow<Assessment?>

    init {
        val dao = ErgoDeskDatabase.getDatabase(application).assessmentDAO()
        repository = RoomAssessmentRepository(dao)

        allAssessments = repository.allAssessments
        activeAssessment = repository.activeAssessment
    }

    fun insertAssessment(assessment: Assessment) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertAssessment(assessment)
        }
    }

    fun updateAssessment(assessment: Assessment) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateAssessment(assessment)
        }
    }

    fun deleteAssessment(assessment: Assessment) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteAssessment(assessment)
        }
    }
}
