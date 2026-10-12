package com.fit5046.ergodesk.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fit5046.ergodesk.data.model.StretchItemEntity
import com.fit5046.ergodesk.data.repository.StretchRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel managing state and operations for Stretch Library and Details.
 * Connects Compose UI to Room database persistence.
 */
class StretchViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = StretchRepository.getInstance(application)

    val searchQuery = MutableStateFlow("")
    val selectedCategory = MutableStateFlow("All")

    init {
        // Automatically seed default ergonomic exercises into Room on first launch
        viewModelScope.launch {
            repository.seedDefaultStretchesIfEmpty()
        }
    }

    /**
     * Reactive StateFlow of filtered stretches reacting automatically to
     * database changes, search queries, and category selection.
     */
    val filteredStretches: StateFlow<List<StretchItemEntity>> = combine(
        repository.allStretches,
        searchQuery,
        selectedCategory
    ) { allItems, query, category ->
        allItems.filter { item ->
            val matchesCategory = (category == "All") || item.targetArea.equals(category, ignoreCase = true)
            val matchesQuery = item.title.contains(query, ignoreCase = true) ||
                    item.targetArea.contains(query, ignoreCase = true)
            matchesCategory && matchesQuery
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun onSearchQueryChanged(newQuery: String) {
        searchQuery.value = newQuery
    }

    fun onCategorySelected(newCategory: String) {
        selectedCategory.value = newCategory
    }

    /**
     * Inserts a custom stretch routine configured by the user into Room database.
     */
    fun addCustomStretch(
        title: String,
        targetArea: String,
        durationSeconds: Int,
        instructions: List<String>
    ) {
        viewModelScope.launch {
            val entity = StretchItemEntity(
                title = title.trim(),
                targetArea = targetArea,
                durationSeconds = durationSeconds,
                difficulty = "Custom",
                instructions = instructions.joinToString("\n"),
                isCustom = true
            )
            repository.insertStretch(entity)
        }
    }

    fun deleteStretch(id: Long) {
        viewModelScope.launch {
            repository.deleteById(id)
        }
    }

    fun getStretchById(id: Long) = repository.getStretchById(id)
}
