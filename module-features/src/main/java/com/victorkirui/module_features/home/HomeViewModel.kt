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

    val uiState: StateFlow<HomeUiState> = combine(
        localRepository.getAllItems(),
        localRepository.getAllCategories()
    ) { allItems, categories ->
        val today = LocalDate.now()
        
        // Only show items that are not marked as DONE
        val items = allItems.filter { it.status != "DONE" }
        
        val todayItems = items.filter { it.deadline == today.toString() }
        val lastAnalysed = items.take(5)
        val upcomingItems = items.filter { it.deadline != null && LocalDate.parse(it.deadline).isAfter(today) }
            .sortedBy { it.deadline }

        HomeUiState.Success(
            todayItems = todayItems,
            lastAnalysed = lastAnalysed,
            upcomingItems = upcomingItems,
            categoriesCount = items.size,
            allItems = items
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState.Loading
    )
}

sealed class HomeUiState {
    object Loading : HomeUiState()
    data class Success(
        val todayItems: List<Item>,
        val lastAnalysed: List<Item>,
        val upcomingItems: List<Item>,
        val categoriesCount: Int,
        val allItems: List<Item>
    ) : HomeUiState()
}
