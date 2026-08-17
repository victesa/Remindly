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
        _selectedDate,
        _currentMonth
    ) { allReminders, selectedDate, currentMonth ->
        RemindersUiState.Success(
            allReminders = allReminders,
            selectedDate = selectedDate,
            currentMonth = currentMonth,
            upcomingReminders = allReminders.filter { 
                val reminderDate = LocalDate.parse(it.reminder.reminderDateTime.substring(0, 10))
                !reminderDate.isBefore(LocalDate.now())
            }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = RemindersUiState.Loading
    )

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
        val selectedDate: LocalDate,
        val currentMonth: YearMonth,
        val upcomingReminders: List<ReminderWithItem>
    ) : RemindersUiState()
}
