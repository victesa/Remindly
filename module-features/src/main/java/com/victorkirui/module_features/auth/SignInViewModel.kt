package com.victorkirui.module_features.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.victorkirui.core.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SignInViewModel(
    private val authRepository: AuthRepository
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
                _uiState.value = SignInUiState.Success
            } else {
                _uiState.value = SignInUiState.Error(result.exceptionOrNull()?.message ?: "Google sign in failed")
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
    data class Error(val message: String) : SignInUiState()
}
