package com.victorkirui.module_features.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.victorkirui.core.repository.OnboardingRepository
import kotlinx.coroutines.launch

class OnboardingViewModel(
    private val onboardingRepository: OnboardingRepository
) : ViewModel() {

    fun completeOnboarding() {
        viewModelScope.launch {
            onboardingRepository.setHasSeenOnboarding(true)
        }
    }
}
