package com.fit5046.ergodesk.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fit5046.ergodesk.data.repository.RoomAssessmentRepository
import com.fit5046.ergodesk.data.room.Assessment
import com.fit5046.ergodesk.data.local.ErgoDeskDatabase
import com.fit5046.ergodesk.data.user.CurrentUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException

class AssessmentViewModel(application: Application) : AndroidViewModel(application) {

    private val userId = CurrentUser.get(application)?.uid ?: ""
    private val repository: RoomAssessmentRepository

    val allAssessments: kotlinx.coroutines.flow.Flow<List<Assessment>>
    val activeAssessment: kotlinx.coroutines.flow.Flow<Assessment?>

    init {
        val dao = ErgoDeskDatabase.getDatabase(application).assessmentDAO()
        repository = RoomAssessmentRepository(dao, userId)

        allAssessments = repository.allAssessments
        activeAssessment = repository.activeAssessment
    }

    private fun isCurrentUser(): Boolean =
        userId.isNotBlank() && CurrentUser.get(getApplication())?.uid == userId

    fun insertAssessment(
        assessment: Assessment,
        onResult: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            val success = if (!isCurrentUser()) false else try {
                withContext(Dispatchers.IO) {
                    repository.insertAssessment(assessment)
                }
                true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                false
            }
            onResult(success)
        }
    }

    fun updateAssessment(
        assessment: Assessment,
        onResult: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            val success = if (!isCurrentUser()) false else try {
                withContext(Dispatchers.IO) {
                    repository.updateAssessment(assessment)
                }
                true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                false
            }
            onResult(success)
        }
    }

    fun deleteAssessment(
        assessment: Assessment,
        onResult: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            val success = if (!isCurrentUser()) false else try {
                withContext(Dispatchers.IO) {
                    repository.deleteAssessment(assessment)
                }
                true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                false
            }
            onResult(success)
        }
    }
}
