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
        
        // We no longer show the Processing state immediately to avoid triggering the dialog.
        // The UI should show a toast (handled by the Caller/Activity).

        viewModelScope.launch {
            when (val result = capturingUseCase(shareContent)) {
                is CaptureResult.Success -> {
                    Log.d("CapturingViewModel", "Capture successful")
                    _uiState.value = CapturingUiState.Success
                    notificationHelper.showNotification("Capture Success", "Content processed and reminders set.")
                }
                is CaptureResult.Error -> {
                    Log.e("CapturingViewModel", "Capture failed: ${result.message}")
                    _uiState.value = CapturingUiState.Error(result.message)
                    notificationHelper.showNotification("Capture Failed", result.message)
                }
                is CaptureResult.SavedLocallyOnly -> {
                    Log.i("CapturingViewModel", "Capture saved locally only (pending sync)")
                    _uiState.value = CapturingUiState.SavedLocally(result.message)
                    notificationHelper.showNotification("Saved Offline", "Analysis will retry shortly.")
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
    data class SavedLocally(val message: String) : CapturingUiState()
    data class Error(val message: String) : CapturingUiState()
    data class Overdue(val message: String) : CapturingUiState()
}
