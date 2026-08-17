package com.victorkirui.module_features.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.victorkirui.core.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SignUpViewModel(
    private val authRepository: AuthRepository
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
                _uiState.value = SignUpUiState.Success
            } else {
                _uiState.value = SignUpUiState.Error(result.exceptionOrNull()?.message ?: "Google sign in failed")
            }
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
