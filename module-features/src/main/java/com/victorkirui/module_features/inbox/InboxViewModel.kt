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

    private val _selectedCategoryId = MutableStateFlow<String?>("Jobs")
    val selectedCategoryId = _selectedCategoryId.asStateFlow()

    val uiState: StateFlow<InboxUiState> = _selectedCategoryId.flatMapLatest { categoryId ->
        combine(
            localRepository.getAllItems(),
            localRepository.getAllCategories()
        ) { allItems, allCategoryNames ->
            val pendingItems = allItems.filter { it.status == "PENDING" }
            
            val categories = mutableListOf<InboxCategory>()
            
            // Add Virtual "Pending Sync" category if there are pending items
            if (pendingItems.isNotEmpty()) {
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

            categories.addAll(allCategoryNames.map { name ->
                val categoryItems = allItems.filter { it.category == name }
                InboxCategory(
                    id = name,
                    latestItemTitle = categoryItems.firstOrNull()?.title ?: "",
                    count = categoryItems.size,
                    hasPendingSync = categoryItems.any { it.status == "PENDING" },
                    iconType = when(name.lowercase()) {
                        "jobs" -> "work"
                        "scholarships" -> "school"
                        "travel" -> "flight"
                        else -> "category"
                    }
                )
            })
            
            val items = when (categoryId) {
                "PENDING_SYNC" -> pendingItems
                null -> emptyList()
                else -> allItems.filter { it.category == categoryId }
            }
            
            InboxUiState.Success(categories, items)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = InboxUiState.Loading
    )

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
        val items: List<Item>
    ) : InboxUiState()
}
