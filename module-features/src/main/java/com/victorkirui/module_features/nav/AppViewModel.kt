package com.victorkirui.module_features.nav

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.victorkirui.core.repository.AuthRepository
import com.victorkirui.core.repository.OnboardingRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

sealed interface AppUiState {
    object Loading : AppUiState
    object Onboarding : AppUiState
    object Unauthenticated : AppUiState
    data class Authenticated(val userId: String) : AppUiState
}

class AppViewModel(
    authRepository: AuthRepository,
    onboardingRepository: OnboardingRepository
) : ViewModel() {

    val appUiState: StateFlow<AppUiState> = combine(
        authRepository.currentUser,
        onboardingRepository.hasSeenOnboarding
    ) { user, hasSeen ->
        when {
            user != null -> AppUiState.Authenticated(user.uid)
            hasSeen -> AppUiState.Unauthenticated
            else -> AppUiState.Onboarding
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AppUiState.Loading
    )
}
