package com.victorkirui.module_features.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.victorkirui.local.entity.Item
import com.victorkirui.local.entity.Reminder
import com.victorkirui.local.repository.LocalRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class EditItemViewModel(
    private val localRepository: LocalRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<EditItemUiState>(EditItemUiState.Loading)
    val uiState: StateFlow<EditItemUiState> = _uiState.asStateFlow()

    private var originalItem: Item? = null

    fun loadItem(itemId: String) {
        viewModelScope.launch {
            _uiState.value = EditItemUiState.Loading
            val item = localRepository.getItem(itemId)
            if (item != null) {
                originalItem = item
                localRepository.getRemindersForItem(itemId).take(1).collect { reminders ->
                    _uiState.value = EditItemUiState.Success(
                        item = item,
                        title = item.title,
                        organization = item.organization ?: "",
                        location = item.metadata?.get("location") ?: "",
                        deadline = item.deadline ?: "",
                        notes = item.summary ?: "",
                        category = item.category ?: "",
                        reminders = reminders
                    )
                }
            } else {
                _uiState.value = EditItemUiState.Error("Item not found")
            }
        }
    }

    fun onTitleChange(newTitle: String) {
        updateSuccessState { it.copy(title = newTitle) }
    }

    fun onOrganizationChange(newOrg: String) {
        updateSuccessState { it.copy(organization = newOrg) }
    }

    fun onLocationChange(newLocation: String) {
        updateSuccessState { it.copy(location = newLocation) }
    }

    fun onDeadlineChange(newDeadline: String) {
        updateSuccessState { it.copy(deadline = newDeadline) }
    }

    fun onNotesChange(newNotes: String) {
        updateSuccessState { it.copy(notes = newNotes) }
    }

    fun addReminder(type: String, dateTime: String) {
        val newItemId = originalItem?.id ?: return
        val newReminder = Reminder(
            itemId = newItemId,
            reminderDateTime = dateTime,
            type = type
        )
        updateSuccessState { it.copy(reminders = it.reminders + newReminder) }
    }

    fun removeReminder(reminder: Reminder) {
        updateSuccessState { it.copy(reminders = it.reminders.filter { r -> r != reminder }) }
    }

    fun saveChanges(onSuccess: () -> Unit = {}) {
        val state = _uiState.value as? EditItemUiState.Success ?: return
        val item = originalItem ?: return

        // 1. Validate Deadline
        if (state.deadline.isNotBlank()) {
            try {
                // Try to parse both YYYY-MM-DD and human readable if possible, 
                // but let's stick to standard validation for now.
                val today = java.time.LocalDate.now()
                val deadlineDate = java.time.LocalDate.parse(state.deadline)
                
                if (deadlineDate.isBefore(today)) {
                    _uiState.value = state.copy(validationError = "Deadline cannot be in the past.")
                    return
                }
            } catch (e: Exception) {
                // If parsing fails, we allow save but maybe it's just invalid text.
                // In a production app, we'd force a date picker.
            }
        }

        viewModelScope.launch {
            val updatedMetadata = (item.metadata ?: emptyMap()).toMutableMap()
            updatedMetadata["location"] = state.location

            val updatedItem = item.copy(
                title = state.title,
                organization = state.organization,
                deadline = state.deadline,
                summary = state.notes,
                metadata = updatedMetadata
            )

            localRepository.updateItem(updatedItem)
            
            // For reminders, it's easier to replace them for now or diff them.
            localRepository.deleteRemindersForItem(item.id)
            localRepository.saveReminders(state.reminders.map { it.copy(id = 0) })
            
            onSuccess()
        }
    }

    private fun updateSuccessState(update: (EditItemUiState.Success) -> EditItemUiState.Success) {
        val currentState = _uiState.value
        if (currentState is EditItemUiState.Success) {
            _uiState.value = update(currentState)
        }
    }
}

sealed class EditItemUiState {
    object Loading : EditItemUiState()
    data class Success(
        val item: Item,
        val title: String,
        val organization: String,
        val location: String,
        val deadline: String,
        val notes: String,
        val category: String,
        val reminders: List<Reminder>,
        val validationError: String? = null
    ) : EditItemUiState()
    data class Error(val message: String) : EditItemUiState()
}
