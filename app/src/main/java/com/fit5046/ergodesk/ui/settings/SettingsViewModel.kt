package com.fit5046.ergodesk.ui.settings

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fit5046.ergodesk.data.sensor.ForegroundTracker
import com.fit5046.ergodesk.data.sensor.TrackingLog
import com.fit5046.ergodesk.data.settings.SettingsRepository
import com.fit5046.ergodesk.data.settings.UserSettings
import com.fit5046.ergodesk.data.user.CurrentUser
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/*
 * Settings edits a draft; nothing changes in the app until
 * the user taps "Save settings". Saved values go to
 * SettingsRepository, which Home and the workers read.
 */
class SettingsViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val user =
        CurrentUser.get(application)

    private fun currentAccountMatches(): Boolean {
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid
        return user != null && currentUid == user.uid
    }

    val accountEmail: String =
        user?.email ?: "Not signed in"

    private val saved =
        MutableStateFlow(
            user?.let { SettingsRepository.read(application, it.uid) } ?: UserSettings()
        )

    private val _draft =
        MutableStateFlow(saved.value)

    val draft: StateFlow<UserSettings> =
        _draft.asStateFlow()

    val hasUnsavedChanges: StateFlow<Boolean> =
        combine(_draft, saved) { draft, current -> draft != current }
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun update(
        transform: (UserSettings) -> UserSettings
    ) {
        _draft.update(transform)
    }

    fun save(): Boolean {
        val account = user ?: return false

        if (!currentAccountMatches()) {
            return false
        }
        SettingsRepository.save(getApplication(), account.uid, _draft.value)
        saved.value = _draft.value
        ForegroundTracker.update()
        Log.d(TAG, "Saved -> ${_draft.value}")
        return true
    }

    fun discardChanges() {
        _draft.value = saved.value
    }

    /*
     * Stops sensing for this account and ends the Firebase
     * session, so nothing is recorded after sign-out.
     */
    fun signOut() {
        user?.let {
            TrackingLog.setActive(getApplication(), it.uid, it.trackingStartMillis, active = false)
        }
        FirebaseAuth.getInstance().signOut()
        Log.d(TAG, "Signed out")
    }

    private companion object {
        const val TAG = "Settings"
    }
}
