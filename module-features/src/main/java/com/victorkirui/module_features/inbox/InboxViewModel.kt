package com.victorkirui.module_features.inbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.victorkirui.local.entity.Item
import com.victorkirui.local.repository.LocalRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi

@OptIn(ExperimentalCoroutinesApi::class)
class InboxViewModel(
    private val localRepository: LocalRepository
) : ViewModel() {

    private val _selectedCategoryId = MutableStateFlow<String?>(null)
    val selectedCategoryId = _selectedCategoryId.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    val uiState: StateFlow<InboxUiState> = combine(
        localRepository.getAllItems(),
        localRepository.getAllCategories(),
        _selectedCategoryId,
        _searchQuery
    ) { allItems, allCategoryNames, selectedId, query ->
        val categories = mutableListOf<InboxCategory>()
        
        // 1. Filter items by search query if present
        val filteredItems = if (query.isBlank()) {
            allItems
        } else {
            allItems.filter { 
                it.title.contains(query, ignoreCase = true) || 
                (it.summary?.contains(query, ignoreCase = true) ?: false) ||
                (it.category?.contains(query, ignoreCase = true) ?: false)
            }
        }

        // 2. Map existing categories (Only active/processed items)
        val activeItems = allItems.filter { it.status != "DONE" && it.status != "PENDING" }
        val filteredActiveItems = filteredItems.filter { it.status != "DONE" && it.status != "PENDING" }
        
        // Pending Sync items (Virtual Category)
        val pendingItems = allItems.filter { it.status == "PENDING" }
        val filteredPendingItems = filteredItems.filter { it.status == "PENDING" }
        
        if (pendingItems.isNotEmpty()) {
            val matches = query.isBlank() || "Pending Sync".contains(query, ignoreCase = true) || filteredPendingItems.isNotEmpty()
            if (matches) {
                categories.add(
                    InboxCategory(
                        id = "PENDING_SYNC",
                        latestItemTitle = pendingItems.first().title,
                        count = pendingItems.size,
                        hasPendingSync = true,
                        iconType = "sync"
                    )
                )
            }
        }
        
        allCategoryNames.forEach { name ->
            val itemsInCategory = activeItems.filter { it.category == name }
            val hasPendingInCategory = allItems.any { it.category == name && it.status == "PENDING" }
            
            if (itemsInCategory.isNotEmpty() || hasPendingInCategory) {
                val matches = query.isBlank() || 
                             name.contains(query, ignoreCase = true) || 
                             filteredActiveItems.any { it.category == name }
                
                if (matches) {
                    categories.add(
                        InboxCategory(
                            id = name,
                            latestItemTitle = itemsInCategory.firstOrNull()?.title ?: "Syncing...",
                            count = itemsInCategory.size,
                            hasPendingSync = hasPendingInCategory,
                            iconType = getIconForCategory(name)
                        )
                    )
                }
            }
        }

        // 3. Uncategorized items
        val uncategorized = activeItems.filter { it.category.isNullOrBlank() }
        if (uncategorized.isNotEmpty()) {
            val matches = query.isBlank() || "Uncategorized".contains(query, ignoreCase = true) || filteredActiveItems.any { it.category.isNullOrBlank() }
            if (matches) {
                categories.add(
                    InboxCategory(
                        id = "UNCATEGORIZED",
                        latestItemTitle = uncategorized.first().title,
                        count = uncategorized.size,
                        hasPendingSync = false,
                        iconType = "category"
                    )
                )
            }
        }

        // UI filtering logic
        val finalSelectedId = selectedId ?: categories.firstOrNull()?.id
        
        val displayItems = when (finalSelectedId) {
            "PENDING_SYNC" -> filteredPendingItems
            "UNCATEGORIZED" -> filteredActiveItems.filter { it.category.isNullOrBlank() }
            null -> emptyList()
            else -> filteredActiveItems.filter { it.category == finalSelectedId }
        }
        
        InboxUiState.Success(categories, displayItems, finalSelectedId, query)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = InboxUiState.Loading
    )

    fun onSearchQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
    }

    private fun getIconForCategory(name: String): String = when(name.lowercase()) {
        "jobs", "work", "career" -> "work"
        "scholarships", "school", "education" -> "school"
        "travel", "flights", "trip" -> "flight"
        "events", "meetups" -> "event"
        "bills", "receipts", "finance" -> "receipt"
        else -> "category"
    }

    fun selectCategory(categoryId: String) {
        _selectedCategoryId.value = categoryId
    }

    fun deleteItem(itemId: String) {
        viewModelScope.launch {
            try {
                localRepository.deleteItem(itemId)
            } catch (e: Exception) {
                android.util.Log.e("InboxViewModel", "Failed to delete item: ${e.message}", e)
            }
        }
    }

    fun deleteCategories(categoryIds: Set<String>) {
        viewModelScope.launch {
            try {
                categoryIds.forEach { id ->
                    when (id) {
                        "PENDING_SYNC" -> localRepository.deletePendingSyncItems()
                        "UNCATEGORIZED" -> localRepository.deleteUncategorizedItems()
                        else -> localRepository.deleteItemsByCategory(id)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("InboxViewModel", "Failed to delete categories: ${e.message}", e)
            }
        }
    }
}

data class InboxCategory(
    val id: String,
    val latestItemTitle: String,
    val count: Int,
    val hasPendingSync: Boolean = false,
    val iconType: String
)

sealed class InboxUiState {
    object Loading : InboxUiState()
    data class Success(
        val categories: List<InboxCategory>,
        val items: List<Item>,
        val selectedCategoryId: String?,
        val searchQuery: String = ""
    ) : InboxUiState()
}
