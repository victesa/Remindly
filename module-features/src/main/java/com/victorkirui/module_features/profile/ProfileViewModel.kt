package com.victorkirui.module_features.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.victorkirui.core.notification.NotificationHelper
import com.victorkirui.core.repository.AuthRepository
import com.victorkirui.core.repository.ReminderSettingsRepository
import com.victorkirui.core.repository.ThemeRepository
import com.victorkirui.local.entity.Item
import com.victorkirui.local.repository.LocalRepository
import com.victorkirui.module_features.reminder.ReminderScheduler
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDateTime

class ProfileViewModel(
    private val settingsRepository: ReminderSettingsRepository,
    private val themeRepository: ThemeRepository,
    private val authRepository: AuthRepository,
    private val notificationHelper: NotificationHelper,
    private val reminderScheduler: ReminderScheduler,
    private val localRepository: LocalRepository
) : ViewModel() {

    val uiState: StateFlow<ProfileUiState> = combine(
        settingsRepository.preferredReminderTime,
        settingsRepository.isMorningBriefingEnabled,
        settingsRepository.isDeadlineAlertsEnabled,
        themeRepository.isDarkTheme,
        authRepository.currentUser
    ) { args ->
        val time = args[0] as String
        val morningBriefing = args[1] as Boolean
        val deadlineAlerts = args[2] as Boolean
        val isDark = args[3] as Boolean?
        val user = args[4] as com.google.firebase.auth.FirebaseUser?

        ProfileUiState(
            userName = user?.displayName ?: "User",
            userEmail = user?.email ?: "",
            preferredReminderTime = time,
            isMorningBriefingEnabled = morningBriefing,
            isDeadlineAlertsEnabled = deadlineAlerts,
            isDarkTheme = isDark
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ProfileUiState()
    )

    fun updateReminderTime(time: String) {
        viewModelScope.launch {
            settingsRepository.setPreferredReminderTime(time)
        }
    }

    fun toggleMorningBriefing(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setMorningBriefingEnabled(enabled)
        }
    }

    fun toggleDeadlineAlerts(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setDeadlineAlertsEnabled(enabled)
        }
    }

    fun setDarkTheme(enabled: Boolean?) {
        viewModelScope.launch {
            themeRepository.setDarkTheme(enabled)
        }
    }

    fun sendTestNotification() {
        notificationHelper.showNotification(
            "Test Notification",
            "This is an immediate test notification from Remindly!"
        )
    }

    fun scheduleTestReminder() {
        viewModelScope.launch {
            val dummyItem = Item(
                id = "test_item_${System.currentTimeMillis()}",
                title = "Test Scheduled Reminder",
                summary = "Checking if Alarms work correctly",
                category = "Test",
                deadline = java.time.LocalDate.now().toString(),
                eventDate = null,
                createdAt = LocalDateTime.now().toString(),
                status = "pending"
            )
            localRepository.saveItem(dummyItem)
            reminderScheduler.scheduleRemindersForItem(dummyItem)
        }
    }
}

data class ProfileUiState(
    val userName: String = "",
    val userEmail: String = "",
    val preferredReminderTime: String = "08:00 AM",
    val isMorningBriefingEnabled: Boolean = true,
    val isDeadlineAlertsEnabled: Boolean = true,
    val isDarkTheme: Boolean? = null
)
