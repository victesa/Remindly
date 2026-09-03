package com.victorkirui.module_features.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.victorkirui.core.repository.AuthRepository
import com.victorkirui.core.repository.ReminderSettingsRepository
import com.victorkirui.core.repository.SubscriptionRepository
import com.victorkirui.core.repository.SubscriptionTier
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.victorkirui.module_features.reminder.BriefingWorker
import org.koin.core.component.KoinComponent
import java.time.LocalTime

class ProfileViewModel(
    private val settingsRepository: ReminderSettingsRepository,
    private val subscriptionRepository: SubscriptionRepository,
    private val authRepository: AuthRepository,
    private val reminderScheduler: com.victorkirui.module_features.reminder.ReminderScheduler
) : ViewModel(), KoinComponent {

    val uiState: StateFlow<ProfileUiState> = combine(
        settingsRepository.preferredReminderTime,
        settingsRepository.isMorningBriefingEnabled,
        settingsRepository.isDeadlineAlertsEnabled,
        settingsRepository.deadlineAlertLeadTimeHours,
        authRepository.currentUser,
        subscriptionRepository.subscriptionTier,
        subscriptionRepository.quotaLimit,
        subscriptionRepository.remainingQuota,
        subscriptionRepository.isCalendarSyncEnabled
    ) { args ->
        val time = args[0] as String
        val morningBriefing = args[1] as Boolean
        val deadlineAlerts = args[2] as Boolean
        val leadTime = args[3] as Int
        val user = args[4] as com.google.firebase.auth.FirebaseUser?
        val tier = args[5] as SubscriptionTier
        val limit = args[6] as Int
        val remaining = args[7] as Int
        val calendarSync = args[8] as Boolean

        ProfileUiState(
            userName = user?.displayName ?: "User",
            userEmail = user?.email ?: "",
            preferredReminderTime = time,
            isMorningBriefingEnabled = morningBriefing,
            isDeadlineAlertsEnabled = deadlineAlerts,
            deadlineAlertLeadTimeHours = leadTime,
            subscriptionTier = tier,
            isCalendarSyncEnabled = calendarSync,
            quotaLimit = limit,
            quotaRemaining = remaining
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ProfileUiState()
    )

    fun updateReminderTime(time: String) {
        viewModelScope.launch {
            settingsRepository.setPreferredReminderTime(time)
            // Reschedule daily briefing via ReminderScheduler and cancel legacy WorkManager job
            try {
                val preferredTime = LocalTime.parse(time)
                reminderScheduler.scheduleDailyBriefing(preferredTime)
                val context: android.content.Context = org.koin.core.context.GlobalContext.get().get()
                androidx.work.WorkManager.getInstance(context).cancelUniqueWork("MorningBriefingWork")
            } catch (e: Exception) {
                android.util.Log.e("ProfileViewModel", "Failed to reschedule briefing", e)
            }
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

    fun setDeadlineAlertLeadTime(hours: Int) {
        viewModelScope.launch {
            settingsRepository.setDeadlineAlertLeadTimeHours(hours)
        }
    }

    private val _deletionResult = MutableSharedFlow<Result<Unit>>()
    val deletionResult = _deletionResult.asSharedFlow()

    fun deleteAccount() {
        viewModelScope.launch {
            val result = authRepository.deleteAccount()
            _deletionResult.emit(result)
        }
    }

    fun setSubscriptionTier(tier: SubscriptionTier) {
        viewModelScope.launch {
            subscriptionRepository.setSubscriptionTier(tier)
        }
    }

    fun toggleCalendarSync(enabled: Boolean) {
        viewModelScope.launch {
            subscriptionRepository.setCalendarSyncEnabled(enabled)
        }
    }
}

data class ProfileUiState(
    val userName: String = "",
    val userEmail: String = "",
    val preferredReminderTime: String = "08:00 AM",
    val isMorningBriefingEnabled: Boolean = true,
    val isDeadlineAlertsEnabled: Boolean = true,
    val deadlineAlertLeadTimeHours: Int = 1,
    val subscriptionTier: SubscriptionTier = SubscriptionTier.FREE,
    val isCalendarSyncEnabled: Boolean = true,
    val quotaLimit: Int = -1,
    val quotaRemaining: Int = -1
)
