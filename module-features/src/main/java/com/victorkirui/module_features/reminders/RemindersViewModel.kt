package com.victorkirui.module_features.reminders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.victorkirui.local.entity.ReminderWithItem
import com.victorkirui.local.repository.LocalRepository
import kotlinx.coroutines.flow.*
import java.time.LocalDate
import java.time.YearMonth

class RemindersViewModel(
    private val localRepository: LocalRepository
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate = _selectedDate.asStateFlow()

    private val _currentMonth = MutableStateFlow(YearMonth.now())
    val currentMonth = _currentMonth.asStateFlow()

    val uiState: StateFlow<RemindersUiState> = combine(
        localRepository.getAllRemindersWithItems(),
        localRepository.getAllItems(),
        _selectedDate,
        _currentMonth
    ) { allReminders, allItems, selectedDate, currentMonth ->
        val today = LocalDate.now()
        
        // Group items for "Upcoming" section by date relationship
        val upcomingGrouped = allItems
            .filter { it.status != "DONE" }
            .filter { 
                val dateStr = it.deadline ?: it.eventDate
                if (dateStr != null) {
                    val date = parseDate(dateStr)
                    !date.isBefore(today)
                } else false
            }
            .groupBy { 
                val date = parseDate(it.deadline ?: it.eventDate!!)
                when {
                    date == today -> "Today"
                    date == today.plusDays(1) -> "Tomorrow"
                    date.isBefore(today.plusWeeks(1)) -> "This Week"
                    else -> "Later"
                }
            }

        RemindersUiState.Success(
            allReminders = allReminders,
            allItems = allItems,
            selectedDate = selectedDate,
            currentMonth = currentMonth,
            itemsForSelectedDate = allItems.filter { 
                val dateStr = it.deadline ?: it.eventDate
                dateStr != null && parseDate(dateStr) == selectedDate
            },
            upcomingGrouped = upcomingGrouped
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = RemindersUiState.Loading
    )

    private fun parseDate(dateStr: String): LocalDate {
        return try {
            if (dateStr.contains("T")) {
                if (dateStr.endsWith("Z")) {
                    java.time.Instant.parse(dateStr).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                } else {
                    java.time.LocalDateTime.parse(dateStr).toLocalDate()
                }
            } else {
                LocalDate.parse(dateStr)
            }
        } catch (e: Exception) {
            LocalDate.now() // Fallback
        }
    }

    fun onDateSelected(date: LocalDate) {
        _selectedDate.value = date
        _currentMonth.value = YearMonth.from(date)
    }

    fun onMonthChanged(month: YearMonth) {
        _currentMonth.value = month
    }
}

sealed class RemindersUiState {
    object Loading : RemindersUiState()
    data class Success(
        val allReminders: List<ReminderWithItem>,
        val allItems: List<com.victorkirui.local.entity.Item>,
        val selectedDate: LocalDate,
        val currentMonth: YearMonth,
        val itemsForSelectedDate: List<com.victorkirui.local.entity.Item>,
        val upcomingGrouped: Map<String, List<com.victorkirui.local.entity.Item>>
    ) : RemindersUiState()
}
