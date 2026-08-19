package com.victorkirui.module_features.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.victorkirui.local.entity.Item
import com.victorkirui.local.repository.LocalRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class CategoryDetailViewModel(
    private val localRepository: LocalRepository
) : ViewModel() {

    private val _categoryName = MutableStateFlow("")
    
    val uiState: StateFlow<CategoryDetailUiState> = _categoryName.flatMapLatest { name ->
        when {
            name.isEmpty() -> flowOf(CategoryDetailUiState.Idle)
            name == "PENDING_SYNC" -> {
                localRepository.getAllItems().map { all ->
                    val pending = all.filter { it.status == "PENDING" }
                    CategoryDetailUiState.Success(name, pending)
                }
            }
            name == "UNCATEGORIZED" -> {
                localRepository.getAllItems().map { all ->
                    // Show items that are processed but have no category
                    val items = all.filter { it.category.isNullOrBlank() && it.status != "PENDING" && it.status != "DONE" }
                    CategoryDetailUiState.Success(name, items)
                }
            }
            else -> {
                localRepository.getItemsByCategory(name).map { items ->
                    // Filter out DONE items to match Inbox view behavior
                    val activeItems = items.filter { it.status != "DONE" }
                    CategoryDetailUiState.Success(name, activeItems)
                }
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CategoryDetailUiState.Loading
    )

    fun setCategory(name: String) {
        _categoryName.value = name
    }

    fun deleteItems(itemIds: Set<String>) {
        viewModelScope.launch {
            try {
                itemIds.forEach { id ->
                    localRepository.deleteItem(id)
                }
            } catch (e: Exception) {
                android.util.Log.e("CategoryDetailViewModel", "Failed to delete items: ${e.message}", e)
            }
        }
    }

    fun markItemsAsDone(itemIds: Set<String>) {
        viewModelScope.launch {
            itemIds.forEach { id ->
                val item = localRepository.getItem(id)
                if (item != null) {
                    localRepository.updateItem(item.copy(status = "DONE"))
                }
            }
        }
    }
}

sealed class CategoryDetailUiState {
    object Idle : CategoryDetailUiState()
    object Loading : CategoryDetailUiState()
    data class Success(val categoryName: String, val items: List<Item>) : CategoryDetailUiState()
}
