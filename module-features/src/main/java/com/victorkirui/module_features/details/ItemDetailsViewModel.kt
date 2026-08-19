package com.victorkirui.module_features.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.victorkirui.local.entity.Item
import com.victorkirui.local.entity.Reminder
import com.victorkirui.local.repository.LocalRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

class ItemDetailsViewModel(
    private val localRepository: LocalRepository
) : ViewModel() {

    private val _itemId = MutableStateFlow<String?>(null)

    val uiState: StateFlow<ItemDetailsUiState> = _itemId
        .filterNotNull()
        .flatMapLatest { id ->
            combine(
                localRepository.getItemFlow(id),
                localRepository.getRemindersForItem(id)
            ) { item, reminders ->
                if (item != null) {
                    ItemDetailsUiState.Success(item, reminders)
                } else {
                    ItemDetailsUiState.Error("Item not found")
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ItemDetailsUiState.Loading
        )

    fun loadItem(itemId: String) {
        _itemId.value = itemId
    }

    fun deleteItem() {
        val id = _itemId.value ?: return
        viewModelScope.launch {
            localRepository.deleteItem(id)
        }
    }

    fun markAsDone() {
        val id = _itemId.value ?: return
        viewModelScope.launch {
            val item = localRepository.getItem(id) ?: return@launch

            val today = LocalDate.now()
            val deadline = item.deadline
            val isOverdue = deadline != null && LocalDate.parse(deadline).isBefore(today)

            if (isOverdue) {
                // If deadline has passed and it's done, delete it automatically
                localRepository.deleteItem(id)
            } else {
                // Otherwise just mark as DONE
                localRepository.updateItem(item.copy(status = "DONE"))
            }
        }
    }

    fun unmarkAsDone() {
        val id = _itemId.value ?: return
        viewModelScope.launch {
            val item = localRepository.getItem(id) ?: return@launch
            localRepository.updateItem(item.copy(status = "ACTIVE"))
        }
    }
}

sealed class ItemDetailsUiState {
    object Loading : ItemDetailsUiState()
    data class Success(val item: Item, val reminders: List<Reminder> = emptyList()) : ItemDetailsUiState()
    data class Error(val message: String) : ItemDetailsUiState()
}
