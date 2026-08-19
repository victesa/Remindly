package com.victorkirui.module_features.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.victorkirui.local.entity.Item
import com.victorkirui.local.repository.LocalRepository
import kotlinx.coroutines.flow.*
import java.time.LocalDate

class HomeViewModel(
    private val localRepository: LocalRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    val uiState: StateFlow<HomeUiState> = combine(
        localRepository.getAllItems(),
        localRepository.getAllCategories(),
        _searchQuery
    ) { allItems, categories, query ->
        val today = LocalDate.now()
        
        // Filter out items marked as DONE for the main view
        val activeAndPendingItems = allItems.filter { it.status != "DONE" }

        // Apply search filter if query is not blank
        val filteredItems = if (query.isBlank()) {
            activeAndPendingItems
        } else {
            activeAndPendingItems.filter { item ->
                item.title.contains(query, ignoreCase = true) ||
                (item.summary?.contains(query, ignoreCase = true) ?: false) ||
                (item.category?.contains(query, ignoreCase = true) ?: false) ||
                (item.organization?.contains(query, ignoreCase = true) ?: false) ||
                (item.source?.contains(query, ignoreCase = true) ?: false)
            }
        }
        
        // Today Items: Items with deadline or event date today
        val todayItems = filteredItems.filter { item ->
            val deadline = item.deadline
            if (deadline == null) return@filter false
            try {
                val date = parseDate(deadline)
                date == today
            } catch (e: Exception) {
                false
            }
        }

        // Last Analysed: Recently analyzed or captured items
        val lastAnalysed = filteredItems
            .sortedByDescending { it.createdAt }
            .take(5)

        // Upcoming: Items with deadlines in the future
        val upcomingItems = filteredItems.filter { item ->
            val deadline = item.deadline
            if (deadline == null) return@filter false
            try {
                val date = parseDate(deadline)
                date.isAfter(today)
            } catch (e: Exception) {
                false
            }
        }.sortedBy { it.deadline }

        HomeUiState.Success(
            todayItems = todayItems,
            lastAnalysed = lastAnalysed,
            upcomingItems = upcomingItems,
            categoriesCount = activeAndPendingItems.size,
            allItems = activeAndPendingItems,
            searchQuery = query
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState.Loading
    )

    fun onSearchQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
    }

    private fun parseDate(dateStr: String): LocalDate {
        return if (dateStr.contains("T")) {
            if (dateStr.endsWith("Z")) {
                java.time.Instant.parse(dateStr).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
            } else {
                java.time.LocalDateTime.parse(dateStr).toLocalDate()
            }
        } else {
            LocalDate.parse(dateStr)
        }
    }
}

sealed class HomeUiState {
    object Loading : HomeUiState()
    data class Success(
        val todayItems: List<Item>,
        val lastAnalysed: List<Item>,
        val upcomingItems: List<Item>,
        val categoriesCount: Int,
        val allItems: List<Item>,
        val searchQuery: String = ""
    ) : HomeUiState()
}
