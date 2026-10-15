package com.fit5046.ergodesk.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fit5046.ergodesk.data.context.WeatherCache
import com.fit5046.ergodesk.data.context.WeatherSnapshot
import com.fit5046.ergodesk.data.network.model.WeatherAdvice
import com.fit5046.ergodesk.data.repository.WeatherRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel managing live weather observations and contextual ergonomic suggestions.
 * Reuses WeatherCache filled by WeatherSyncWorker to avoid redundant API queries.
 * Exposes reactive StateFlow for Compose UI.
 * Member 4 responsibility.
 */
// @JvmOverloads gives the (Application) constructor that the
// default ViewModel factory looks for; without it the app crashes.
class WeatherViewModel @JvmOverloads constructor(
    application: Application,
    private val repository: WeatherRepository = WeatherRepository(),
    private val cache: WeatherCache = WeatherCache(application)
) : AndroidViewModel(application) {

    private val _weatherAdvice = MutableStateFlow(
        cache.read()?.let { repository.adviceFromSnapshot(it) } ?: repository.getUnavailableAdvice()
    )
    val weatherAdvice: StateFlow<WeatherAdvice> = _weatherAdvice.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        // If cache is empty or stale, query live weather; otherwise reuse WeatherCache
        if (cache.read() == null) {
            fetchWeather("Melbourne")
        }
    }

    /**
     * Queries current weather for the specified city, updates UI, and refreshes WeatherCache.
     */
    fun fetchWeather(cityName: String = "Melbourne", force: Boolean = false) {
        if (!force) {
            val existing = cache.read()
            if (existing != null) {
                _weatherAdvice.value = repository.adviceFromSnapshot(existing)
                return
            }
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            val result = repository.getWeather(cityName)
            result.onSuccess { response ->
                val advice = repository.deriveErgonomicAdvice(response)
                _weatherAdvice.value = advice
                _errorMessage.value = null

                // Sync with WeatherCache so ContextEngine / HomeViewModel share fresh data
                val snapshot = WeatherSnapshot(
                    temperatureC = response.current.tempC,
                    condition = response.current.condition.text,
                    isOutdoorFriendly = advice.isOutdoorFriendly
                )
                cache.save(snapshot)
            }.onFailure { error ->
                _errorMessage.value = error.message
                // If there is no valid cache, transparently show unavailable state (Issue 8)
                if (cache.read() == null) {
                    _weatherAdvice.value = repository.getUnavailableAdvice()
                }
            }

            _isLoading.value = false
        }
    }
}
