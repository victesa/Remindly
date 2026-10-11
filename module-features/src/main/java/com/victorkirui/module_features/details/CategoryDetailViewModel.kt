package com.victorkirui.module_features.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.victorkirui.local.entity.Item
import com.victorkirui.local.repository.LocalRepository
import com.victorkirui.core.repository.AuthRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi

@OptIn(ExperimentalCoroutinesApi::class)
class CategoryDetailViewModel(
    private val authRepository: AuthRepository,
    private val localRepository: LocalRepository,
    private val subscriptionRepository: com.victorkirui.core.repository.SubscriptionRepository,
    private val captureApiService: com.victorkirui.remote.CaptureApiService
) : ViewModel() {

    private val _categoryName = MutableStateFlow("")
    
    val uiState: StateFlow<CategoryDetailUiState> = authRepository.currentUser.flatMapLatest { user ->
        val firebaseUser = user ?: com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        val userId = firebaseUser?.uid ?: ""
        _categoryName.flatMapLatest { name ->
            when {
                userId.isEmpty() -> flowOf(CategoryDetailUiState.Idle)
                name.isEmpty() -> flowOf(CategoryDetailUiState.Idle)
                name == "PENDING_SYNC" -> {
                    localRepository.getAllItems(userId).map { all ->
                        val pending = all.filter { it.status == "PENDING" }
                        CategoryDetailUiState.Success(name, pending)
                    }
                }
                name == "UNCATEGORIZED" -> {
                    localRepository.getAllItems(userId).map { all ->
                        // Show items that are processed but have no category
                        val items = all.filter { it.category.isNullOrBlank() && it.status != "PENDING" && it.status != "DONE" }
                        CategoryDetailUiState.Success(name, items)
                    }
                }
                name == "HISTORY" -> {
                    localRepository.getAllItems(userId).map { all ->
                        val historyItems = all.filter { it.status == "DONE" }
                        CategoryDetailUiState.Success(name, historyItems)
                    }
                }
                else -> {
                    localRepository.getItemsByCategory(userId, name).map { items ->
                        // Filter out DONE items to match Inbox view behavior
                        val activeItems = items.filter { it.status != "DONE" }
                        CategoryDetailUiState.Success(name, activeItems)
                    }
                }
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CategoryDetailUiState.Idle
    )

    fun setCategory(name: String) {
        _categoryName.value = name
    }

    fun deleteItems(itemIds: Set<String>) {
        val userId = authRepository.currentUser.value?.uid ?: return
        viewModelScope.launch {
            try {
                val tier = subscriptionRepository.subscriptionTier.first()
                val isPremium = tier == com.victorkirui.core.repository.SubscriptionTier.PREMIUM
                
                itemIds.forEach { id ->
                    if (isPremium) {
                        localRepository.addPendingDeletion(com.victorkirui.local.entity.PendingDeletion(id, userId))
                    }
                    localRepository.deleteItem(userId, id)
                }
                if (isPremium) {
                    com.victorkirui.module_features.capturing.SyncWorker.schedule(
                        org.koin.core.context.GlobalContext.get().get()
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("CategoryDetailViewModel", "Failed to delete items: ${e.message}", e)
            }
        }
    }

    fun markItemsAsDone(itemIds: Set<String>) {
        viewModelScope.launch {
            val tier = subscriptionRepository.subscriptionTier.first()
            val isPremium = tier == com.victorkirui.core.repository.SubscriptionTier.PREMIUM

            itemIds.forEach { id ->
                if (isPremium) {
                    captureApiService.updateItem(id, mapOf("state" to "DONE"))
                }
                val item = localRepository.getItem(id)
                if (item != null) {
                    localRepository.updateItem(item.copy(status = "DONE"))
                }
            }
        }
    }
}

sealed class CategoryDetailUiState {
    object Idle : CategoryDetailUiState()
    object Loading : CategoryDetailUiState()
    data class Success(val categoryName: String, val items: List<Item>) : CategoryDetailUiState()
}
