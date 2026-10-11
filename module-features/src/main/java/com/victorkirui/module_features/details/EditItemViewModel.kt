package com.victorkirui.module_features.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.victorkirui.local.entity.Item
import com.victorkirui.local.entity.Reminder
import com.victorkirui.local.repository.LocalRepository
import com.victorkirui.core.repository.AuthRepository
import com.victorkirui.module_features.reminder.ReminderScheduler
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class EditItemViewModel(
    private val authRepository: AuthRepository,
    private val localRepository: LocalRepository,
    private val subscriptionRepository: com.victorkirui.core.repository.SubscriptionRepository,
    private val captureApiService: com.victorkirui.remote.CaptureApiService,
    private val reminderScheduler: ReminderScheduler
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

    fun updateDeadline(date: java.time.LocalDate, time: java.time.LocalTime) {
        val dateTime = java.time.LocalDateTime.of(date, time)
        val isoStr = dateTime.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        onDeadlineChange(isoStr)
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

    fun refreshReminders() {
        val state = _uiState.value as? EditItemUiState.Success ?: return
        val item = originalItem ?: return
        viewModelScope.launch {
            val updatedItem = item.copy(deadline = state.deadline)
            val newReminders = reminderScheduler.calculateReminders(updatedItem)
            updateSuccessState { it.copy(reminders = newReminders) }
        }
    }

    fun saveChanges(onSuccess: () -> Unit = {}) {
        val state = _uiState.value as? EditItemUiState.Success ?: return
        val item = originalItem ?: return

        // 1. Validate Deadline
        if (state.deadline.isNotBlank()) {
            try {
                val today = java.time.LocalDate.now()
                val deadlineDate = com.victorkirui.core.util.DateUtils.parseToLocalDate(state.deadline)
                
                if (deadlineDate != null && deadlineDate.isBefore(today)) {
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

            val cleanEventDate = com.victorkirui.core.util.DateUtils.sanitizeEventDate(item.eventDate, state.deadline)

            val updatedItem = item.copy(
                title = state.title,
                organization = state.organization,
                deadline = state.deadline,
                eventDate = cleanEventDate,
                summary = state.notes,
                metadata = updatedMetadata
            )

            localRepository.updateItem(updatedItem)

            val tier = subscriptionRepository.subscriptionTier.first()
            if (tier == com.victorkirui.core.repository.SubscriptionTier.PREMIUM) {
                try {
                    val allowedCategory = mapToAllowedCategory(state.category)
                    captureApiService.updateItem(item.id, mapOf(
                        "title" to state.title,
                        "summary" to state.notes,
                        "deadline" to state.deadline,
                        "organization" to state.organization,
                        "category" to allowedCategory
                    ))
                } catch (e: Exception) {
                    android.util.Log.e("EditItemViewModel", "Failed to sync edits to backend: ${e.message}")
                }
            }
            
            // If deadline changed, re-calculate automatic reminders to match new date.
            // Otherwise, respect manual edits in the current state.
            val finalReminders = if (state.deadline != item.deadline) {
                reminderScheduler.calculateReminders(updatedItem)
            } else {
                state.reminders
            }

            reminderScheduler.updateAlarms(item.id, finalReminders.map { it.copy(id = 0) })
            
            onSuccess()
        }
    }

    private fun updateSuccessState(update: (EditItemUiState.Success) -> EditItemUiState.Success) {
        val currentState = _uiState.value
        if (currentState is EditItemUiState.Success) {
            _uiState.value = update(currentState)
        }
    }

    private fun mapToAllowedCategory(raw: String): String {
        val upper = raw.trim().uppercase()
        val allowed = setOf(
            "JOB", "EVENT", "SCHOLARSHIP", "MEETING", "EXAM", "ASSIGNMENT", 
            "BILL", "PAYMENT", "APPOINTMENT", "SUBSCRIPTION", "TRAVEL", 
            "HEALTH", "SHOPPING", "DOCUMENT", "PERSONAL", "OTHER"
        )
        if (allowed.contains(upper)) return upper
        return when {
            upper.contains("WORK") || upper.contains("CAREER") -> "JOB"
            upper.contains("SCHOOL") || upper.contains("EDUCATION") -> "EXAM"
            upper.contains("RECEIPT") || upper.contains("FINANCE") -> "BILL"
            upper.contains("FLIGHT") || upper.contains("TRIP") -> "TRAVEL"
            else -> "OTHER"
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
