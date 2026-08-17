package com.victorkirui.module_features.capturing

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.victorkirui.core.model.ShareContent
import com.victorkirui.core.notification.NotificationHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CapturingViewModel(
    private val capturingUseCase: CapturingUseCase,
    private val notificationHelper: NotificationHelper
) : ViewModel() {

    private val _uiState = MutableStateFlow<CapturingUiState>(CapturingUiState.Idle)
    val uiState = _uiState.asStateFlow()

    fun capture(shareContent: ShareContent) {
        Log.d("CapturingViewModel", "Capture started for content: $shareContent")
        
        // Always show the initial processing state in the dialog
        _uiState.value = CapturingUiState.Processing

        // We launch in viewModelScope, but since this is usually triggered from an Intent 
        // that finishes the activity quickly, we should consider a more robust background solution 
        // (like WorkManager) in the future. For now, this works as long as the process stays alive.
        viewModelScope.launch {
            when (val result = capturingUseCase(shareContent)) {
                CaptureResult.Success -> {
                    Log.d("CapturingViewModel", "Capture successful")
                    _uiState.value = CapturingUiState.Success
                    notificationHelper.showNotification("Capture Success", "Content processed and reminders set.")
                }
                is CaptureResult.Error -> {
                    Log.e("CapturingViewModel", "Capture failed: ${result.message}")
                    _uiState.value = CapturingUiState.Error(result.message)
                    notificationHelper.showNotification("Capture Failed", result.message)
                }
                CaptureResult.SavedLocallyOnly -> {
                    Log.i("CapturingViewModel", "Capture saved locally only (pending sync)")
                    _uiState.value = CapturingUiState.Success // Still show success to user as it's saved
                    notificationHelper.showNotification("Saved", "Content saved locally and will sync later.")
                }
                is CaptureResult.Overdue -> {
                    Log.i("CapturingViewModel", "Capture failed: item is overdue")
                    val date = result.item.deadline ?: result.item.eventDate
                    _uiState.value = CapturingUiState.Overdue(
                        "This item is long overdue ($date). Remindly only captures upcoming events and deadlines."
                    )
                    notificationHelper.showNotification("Capture Overdue", "The item was not saved because it is in the past.")
                }
            }
        }
    }

    fun resetState() {
        _uiState.value = CapturingUiState.Idle
    }
}

sealed class CapturingUiState {
    object Idle : CapturingUiState()
    object Processing : CapturingUiState()
    object Success : CapturingUiState()
    data class Error(val message: String) : CapturingUiState()
    data class Overdue(val message: String) : CapturingUiState()
}
