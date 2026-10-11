package com.victorkirui.module_features.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.victorkirui.core.repository.AuthRepository
import com.victorkirui.core.repository.SubscriptionRepository
import com.victorkirui.core.repository.SubscriptionTier
import com.victorkirui.local.repository.LocalRepository
import com.victorkirui.module_features.capturing.SyncDownWorker
import com.victorkirui.remote.CaptureApiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SignUpViewModel(
    private val authRepository: AuthRepository,
    private val captureApiService: CaptureApiService,
    private val subscriptionRepository: SubscriptionRepository,
    private val localRepository: LocalRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<SignUpUiState>(SignUpUiState.Idle)
    val uiState = _uiState.asStateFlow()

    fun signUp(email: String, password: String, name: String) {
        if (email.isBlank() || password.isBlank() || name.isBlank()) {
            _uiState.value = SignUpUiState.Error("Please fill in all fields")
            return
        }

        _uiState.value = SignUpUiState.Loading
        viewModelScope.launch {
            val result = authRepository.signUp(email, password, name)
            if (result.isSuccess) {
                val tier = refreshEntitlementOnSignUp()
                if (tier == SubscriptionTier.PREMIUM) {
                    val context: android.content.Context = org.koin.core.context.GlobalContext.get().get()
                    SyncDownWorker.schedule(context)
                } else {
                    val uid = result.getOrNull()?.user?.uid ?: authRepository.currentUser.value?.uid ?: ""
                    if (uid.isNotEmpty()) {
                        try {
                            localRepository.deleteAllItemsForUser(uid)
                        } catch (_: Exception) {}
                    }
                }
                _uiState.value = SignUpUiState.Success
            } else {
                _uiState.value = SignUpUiState.Error(result.exceptionOrNull()?.message ?: "Sign up failed")
            }
        }
    }

    fun signInWithGoogle(idToken: String) {
        _uiState.value = SignUpUiState.Loading
        viewModelScope.launch {
            val result = authRepository.signInWithGoogle(idToken)
            if (result.isSuccess) {
                val tier = refreshEntitlementOnSignUp()
                if (tier == SubscriptionTier.PREMIUM) {
                    val context: android.content.Context = org.koin.core.context.GlobalContext.get().get()
                    SyncDownWorker.schedule(context)
                } else {
                    val uid = result.getOrNull()?.user?.uid ?: authRepository.currentUser.value?.uid ?: ""
                    if (uid.isNotEmpty()) {
                        try {
                            localRepository.deleteAllItemsForUser(uid)
                        } catch (_: Exception) {}
                    }
                }
                _uiState.value = SignUpUiState.Success
            } else {
                _uiState.value = SignUpUiState.Error(result.exceptionOrNull()?.message ?: "Google sign in failed")
            }
        }
    }

    private suspend fun refreshEntitlementOnSignUp(): SubscriptionTier {
        try {
            var isProUser = false
            val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser

            try {
                val profile = captureApiService.getMe()
                if (profile.success) {
                    isProUser = profile.isProUser
                    if (profile.quotaLimit >= 0 && profile.quotaRemaining >= 0) {
                        subscriptionRepository.updateQuota(profile.quotaLimit, profile.quotaRemaining)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.w("SignUpViewModel", "GET /v1/me check failed during sign-up: ${e.message}")
            }

            val targetTier = if (isProUser) SubscriptionTier.PREMIUM else SubscriptionTier.FREE
            subscriptionRepository.updateTier(targetTier)
            return targetTier
        } catch (e: Exception) {
            android.util.Log.w("SignUpViewModel", "Failed to sync profile/tier during sign-up: ${e.message}")
            return SubscriptionTier.FREE
        }
    }

    fun resetState() {
        _uiState.value = SignUpUiState.Idle
    }
}

sealed class SignUpUiState {
    object Idle : SignUpUiState()
    object Loading : SignUpUiState()
    object Success : SignUpUiState()
    data class Error(val message: String) : SignUpUiState()
}
