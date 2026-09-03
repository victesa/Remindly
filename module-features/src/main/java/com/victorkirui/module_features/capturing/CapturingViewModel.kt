package com.victorkirui.module_features.capturing

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.victorkirui.core.model.ShareContent
import com.victorkirui.core.notification.NotificationHelper
import com.victorkirui.core.monitoring.Analytics
import com.victorkirui.core.monitoring.CrashReporter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CapturingViewModel(
    private val capturingUseCase: CapturingUseCase,
    private val notificationHelper: NotificationHelper,
    private val analytics: Analytics,
    private val crashReporter: CrashReporter
) : ViewModel() {

    private val _uiState = MutableStateFlow<CapturingUiState>(CapturingUiState.Idle)
    val uiState = _uiState.asStateFlow()

    fun capture(shareContent: ShareContent) {
        Log.d("CapturingViewModel", "Capture started for content: $shareContent")
        
        analytics.logEvent("capture_started", mapOf(
            "type" to when(shareContent) {
                is ShareContent.Text -> "text"
                is ShareContent.Image -> "image"
                is ShareContent.Pdf -> "pdf"
                else -> "unknown"
            }
        ))

        viewModelScope.launch {
            try {
                when (val result = capturingUseCase(shareContent)) {
                    is CaptureResult.Success -> {
                        Log.d("CapturingViewModel", "Capture successful")
                        _uiState.value = CapturingUiState.Success
                        notificationHelper.showNotification("Capture Success", "Content processed and reminders set.")
                        analytics.logEvent("capture_success")
                    }
                    is CaptureResult.Error -> {
                        Log.e("CapturingViewModel", "Capture failed: ${result.message}")
                        _uiState.value = CapturingUiState.Error(result.message)
                        notificationHelper.showNotification("Capture Failed", result.message)
                        analytics.logEvent("capture_error", mapOf("message" to result.message))
                    }
                    is CaptureResult.SavedLocallyOnly -> {
                        Log.i("CapturingViewModel", "Capture saved locally only (pending sync)")
                        _uiState.value = CapturingUiState.SavedLocally(result.message)
                        notificationHelper.showNotification("Saved Offline", "Analysis will retry shortly.")
                        analytics.logEvent("capture_offline")
                    }
                    is CaptureResult.Overdue -> {
                        Log.i("CapturingViewModel", "Capture failed: item is overdue")
                        val date = result.item.deadline ?: result.item.eventDate
                        _uiState.value = CapturingUiState.Overdue(
                            "This item is long overdue ($date). Remindly only captures upcoming events and deadlines."
                        )
                        notificationHelper.showNotification("Capture Overdue", "The item was not saved because it is in the past.")
                        analytics.logEvent("capture_overdue")
                    }
                    is CaptureResult.UpgradeRequired -> {
                        _uiState.value = CapturingUiState.UpgradeRequired(result.message)
                        analytics.logEvent("capture_upgrade_required")
                    }
                    is CaptureResult.Duplicate -> {
                        _uiState.value = CapturingUiState.Duplicate(result.existingItemId)
                        notificationHelper.showNotification("Duplicate Content", "This has already been captured.")
                        analytics.logEvent("capture_duplicate")
                    }
                }
            } catch (e: Exception) {
                Log.e("CapturingViewModel", "Exception during capture", e)
                crashReporter.recordException(e)
                _uiState.value = CapturingUiState.Error("An unexpected error occurred during analysis.")
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
    data class UpgradeRequired(val message: String) : CapturingUiState()
    data class Duplicate(val itemId: String) : CapturingUiState()
}
