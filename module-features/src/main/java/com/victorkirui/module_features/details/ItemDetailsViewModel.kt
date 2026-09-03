package com.victorkirui.module_features.details

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.victorkirui.core.repository.AuthRepository
import com.victorkirui.local.entity.Item
import com.victorkirui.local.entity.PendingSync
import com.victorkirui.local.entity.Reminder
import com.victorkirui.local.repository.LocalRepository
import com.victorkirui.core.repository.SubscriptionRepository
import com.victorkirui.core.repository.SubscriptionTier
import com.victorkirui.module_features.capturing.SyncWorker
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

class ItemDetailsViewModel(
    private val authRepository: AuthRepository,
    private val localRepository: LocalRepository,
    private val subscriptionRepository: SubscriptionRepository,
    private val calendarManager: com.victorkirui.core.calendar.CalendarManager,
    private val captureApiService: com.victorkirui.remote.CaptureApiService,
    context: Context
) : ViewModel() {
    
    private val appContext = context.applicationContext

    private val _itemId = MutableStateFlow<String?>(null)

    private val _calendarSyncResult = MutableSharedFlow<Boolean>()
    val calendarSyncResult = _calendarSyncResult.asSharedFlow()

    private val _lastSyncedEventUri = MutableStateFlow<String?>(null)
    val lastSyncedEventUri = _lastSyncedEventUri.asStateFlow()

    val uiState: StateFlow<ItemDetailsUiState> = _itemId
        .filterNotNull()
        .flatMapLatest { id ->
            combine(
                localRepository.getItemFlow(id),
                localRepository.getRemindersForItem(id),
                subscriptionRepository.subscriptionTier
            ) { item, reminders, tier ->
                if (item != null) {
                    // Update sync URI from metadata if available
                    val syncedUri = item.metadata?.get("calendar_event_uri")
                    if (syncedUri != null && _lastSyncedEventUri.value == null) {
                        _lastSyncedEventUri.value = syncedUri
                    }
                    ItemDetailsUiState.Success(item, reminders, tier)
                } else {
                    ItemDetailsUiState.Error("Item not found")
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ItemDetailsUiState.Loading
        )

    fun loadItem(itemId: String) {
        _lastSyncedEventUri.value = null // Reset for new item
        _itemId.value = itemId
    }

    fun deleteItem() {
        val id = _itemId.value ?: return
        val userId = authRepository.currentUser.value?.uid ?: return
        viewModelScope.launch {
            val tier = subscriptionRepository.subscriptionTier.first()
            if (tier == SubscriptionTier.PREMIUM) {
                localRepository.addPendingDeletion(com.victorkirui.local.entity.PendingDeletion(id, userId))
                com.victorkirui.module_features.capturing.SyncWorker.schedule(appContext)
            }
            localRepository.deleteItem(userId, id)
        }
    }

    fun markAsDone() {
        val id = _itemId.value ?: return
        val userId = authRepository.currentUser.value?.uid ?: return
        viewModelScope.launch {
            val item = localRepository.getItem(id) ?: return@launch

            val today = LocalDate.now()
            val deadline = item.deadline
            val isOverdue = deadline != null && try { LocalDate.parse(deadline).isBefore(today) } catch(e:Exception) { false }

            if (isOverdue) {
                localRepository.deleteItem(userId, id)
            } else {
                localRepository.updateItem(item.copy(status = "DONE"))
            }

            val tier = subscriptionRepository.subscriptionTier.first()
            if (tier == SubscriptionTier.PREMIUM) {
                try {
                    captureApiService.updateItem(id, mapOf("state" to "DONE"))
                } catch (e: Exception) {
                    android.util.Log.e("ItemDetailsViewModel", "Failed to sync markAsDone to backend: ${e.message}")
                }
            }
        }
    }

    fun unmarkAsDone() {
        val id = _itemId.value ?: return
        viewModelScope.launch {
            val item = localRepository.getItem(id) ?: return@launch
            localRepository.updateItem(item.copy(status = "ACTIVE"))

            val tier = subscriptionRepository.subscriptionTier.first()
            if (tier == SubscriptionTier.PREMIUM) {
                try {
                    captureApiService.updateItem(id, mapOf("state" to "ACTIVE"))
                } catch (e: Exception) {
                    android.util.Log.e("ItemDetailsViewModel", "Failed to sync unmarkAsDone to backend: ${e.message}")
                }
            }
        }
    }

    fun retryAnalysis() {
        val id = _itemId.value ?: return
        viewModelScope.launch {
            val item = localRepository.getItem(id) ?: return@launch
            localRepository.withTransaction {
                localRepository.updateItem(item.copy(status = "PENDING", title = "Analysing..."))
                localRepository.addPendingSync(com.victorkirui.local.entity.PendingSync(itemId = id))
            }
            SyncWorker.schedule(appContext)
        }
    }

    fun syncToCalendar() {
        val state = uiState.value as? ItemDetailsUiState.Success ?: return
        val item = state.item
        val eventDate = item.eventDate ?: item.deadline ?: return
        
        viewModelScope.launch {
            val uri = calendarManager.addEvent(
                title = item.title,
                description = item.summary,
                eventDate = eventDate,
                location = item.metadata?.get("location") ?: item.organization
            )
            if (uri != null) {
                _lastSyncedEventUri.value = uri.toString()
                
                // Persist the sync status in metadata
                val updatedMetadata = (item.metadata ?: emptyMap()).toMutableMap()
                updatedMetadata["google_calendar_synced"] = "true"
                updatedMetadata["calendar_event_uri"] = uri.toString()
                localRepository.updateItem(item.copy(metadata = updatedMetadata))
            }
            _calendarSyncResult.emit(uri != null)
        }
    }
}

sealed class ItemDetailsUiState {
    object Loading : ItemDetailsUiState()
    data class Success(val item: Item, val reminders: List<Reminder> = emptyList(), val tier: SubscriptionTier = SubscriptionTier.FREE) : ItemDetailsUiState()
    data class Error(val message: String) : ItemDetailsUiState()
}
