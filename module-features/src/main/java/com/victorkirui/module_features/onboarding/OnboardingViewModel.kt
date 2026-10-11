package com.victorkirui.module_features.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.victorkirui.core.repository.OnboardingRepository
import com.victorkirui.core.repository.ReminderSettingsRepository
import kotlinx.coroutines.launch

class OnboardingViewModel(
    private val onboardingRepository: OnboardingRepository,
    private val settingsRepository: ReminderSettingsRepository
) : ViewModel() {

    fun completeOnboarding(deadlineAlertsEnabled: Boolean = true, morningBriefingEnabled: Boolean = true) {
        viewModelScope.launch {
            settingsRepository.setDeadlineAlertsEnabled(deadlineAlertsEnabled)
            settingsRepository.setMorningBriefingEnabled(morningBriefingEnabled)
            onboardingRepository.setHasSeenOnboarding(true)
        }
    }
}
