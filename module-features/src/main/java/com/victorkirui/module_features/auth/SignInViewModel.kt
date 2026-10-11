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

class SignInViewModel(
    private val authRepository: AuthRepository,
    private val captureApiService: CaptureApiService,
    private val subscriptionRepository: SubscriptionRepository,
    private val localRepository: LocalRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<SignInUiState>(SignInUiState.Idle)
    val uiState = _uiState.asStateFlow()

    fun signIn(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = SignInUiState.Error("Please fill in all fields")
            return
        }

        _uiState.value = SignInUiState.Loading
        viewModelScope.launch {
            val result = authRepository.signIn(email, password)
            if (result.isSuccess) {
                val tier = refreshEntitlementOnSignIn()
                if (tier == SubscriptionTier.PREMIUM) {
                    val context: android.content.Context = org.koin.core.context.GlobalContext.get().get()
                    SyncDownWorker.schedule(context)
                } else {
                    // Free user: ensure local Room database is clean of any auto-backed-up items
                    val uid = result.getOrNull()?.user?.uid ?: authRepository.currentUser.value?.uid ?: ""
                    if (uid.isNotEmpty()) {
                        try {
                            localRepository.deleteAllItemsForUser(uid)
                        } catch (_: Exception) {}
                    }
                }
                _uiState.value = SignInUiState.Success
            } else {
                _uiState.value = SignInUiState.Error(result.exceptionOrNull()?.message ?: "Sign in failed")
            }
        }
    }

    fun signInWithGoogle(idToken: String) {
        _uiState.value = SignInUiState.Loading
        viewModelScope.launch {
            val result = authRepository.signInWithGoogle(idToken)
            if (result.isSuccess) {
                val tier = refreshEntitlementOnSignIn()
                if (tier == SubscriptionTier.PREMIUM) {
                    val context: android.content.Context = org.koin.core.context.GlobalContext.get().get()
                    SyncDownWorker.schedule(context)
                } else {
                    // Free user: ensure local Room database is clean of any auto-backed-up items
                    val uid = result.getOrNull()?.user?.uid ?: authRepository.currentUser.value?.uid ?: ""
                    if (uid.isNotEmpty()) {
                        try {
                            localRepository.deleteAllItemsForUser(uid)
                        } catch (_: Exception) {}
                    }
                }
                _uiState.value = SignInUiState.Success
            } else {
                _uiState.value = SignInUiState.Error(result.exceptionOrNull()?.message ?: "Google sign in failed")
            }
        }
    }

    private suspend fun refreshEntitlementOnSignIn(): SubscriptionTier {
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
                    val name = profile.user?.name
                    if (!name.isNullOrBlank() && user != null && user.displayName.isNullOrBlank()) {
                        authRepository.updateDisplayName(name)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.w("SignInViewModel", "GET /v1/me check failed during sign-in: ${e.message}")
            }

            val targetTier = if (isProUser) SubscriptionTier.PREMIUM else SubscriptionTier.FREE
            android.util.Log.d("SignInViewModel", "Updating subscription tier on sign-in: $targetTier")
            subscriptionRepository.updateTier(targetTier)
            return targetTier
        } catch (e: Exception) {
            android.util.Log.w("SignInViewModel", "Failed to sync profile/tier during sign-in: ${e.message}")
            return SubscriptionTier.FREE
        }
    }

    fun resetPassword(email: String) {
        if (email.isBlank()) {
            _uiState.value = SignInUiState.Error("Please enter your email to reset password")
            return
        }

        _uiState.value = SignInUiState.Loading
        viewModelScope.launch {
            val result = authRepository.sendPasswordResetEmail(email)
            if (result.isSuccess) {
                _uiState.value = SignInUiState.ResetEmailSent
            } else {
                _uiState.value = SignInUiState.Error(result.exceptionOrNull()?.message ?: "Failed to send reset email")
            }
        }
    }

    fun resetState() {
        _uiState.value = SignInUiState.Idle
    }
}

sealed class SignInUiState {
    object Idle : SignInUiState()
    object Loading : SignInUiState()
    object Success : SignInUiState()
    object ResetEmailSent : SignInUiState()
    data class Error(val message: String) : SignInUiState()
}
