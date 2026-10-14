package com.fit5046.ergodesk.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fit5046.ergodesk.data.network.model.WeatherAdvice
import com.fit5046.ergodesk.data.repository.WeatherRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel managing live weather observations and contextual ergonomic suggestions.
 * Exposes reactive StateFlow for Compose UI.
 * Member 4 responsibility.
 */
class WeatherViewModel(
    private val repository: WeatherRepository = WeatherRepository()
) : ViewModel() {

    private val _weatherAdvice = MutableStateFlow(repository.getFallbackAdvice())
    val weatherAdvice: StateFlow<WeatherAdvice> = _weatherAdvice.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        // Automatically fetch live Melbourne weather when ViewModel initializes
        fetchWeather("Melbourne")
    }

    /**
     * Queries current weather for the specified city and updates context-aware suggestions.
     */
    fun fetchWeather(cityName: String = "Melbourne") {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            val result = repository.getWeather(cityName)
            result.onSuccess { response ->
                _weatherAdvice.value = repository.deriveErgonomicAdvice(response)
                _errorMessage.value = null
            }.onFailure { error ->
                _errorMessage.value = error.message
                // Keep existing/fallback advice so UI remains smooth and uninterrupted
            }

            _isLoading.value = false
        }
    }
}
