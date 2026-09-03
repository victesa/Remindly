package com.victorkirui.module_features.home

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.victorkirui.core.monitoring.Analytics
import com.victorkirui.core.repository.AuthRepository
import com.victorkirui.core.repository.SubscriptionRepository
import com.victorkirui.local.entity.Item
import com.victorkirui.local.repository.LocalRepository
import com.victorkirui.module_features.capturing.SyncDownWorker
import com.victorkirui.module_features.capturing.SyncWorker
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val authRepository: AuthRepository,
    private val localRepository: LocalRepository,
    private val subscriptionRepository: SubscriptionRepository,
    private val analytics: Analytics,
    private val context: Context
) : ViewModel() {

    private val workManager = WorkManager.getInstance(context)

    init {
        // Trigger a sync and cleanup check on startup
        SyncWorker.schedule(context)
        
        viewModelScope.launch {
            authRepository.currentUser.value?.uid?.let { userId ->
                localRepository.deleteExpiredItems(userId)
            }
        }
    }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    val hasPendingProCelebration: Flow<Boolean> = subscriptionRepository.hasPendingProCelebration

    fun consumeProCelebration() {
        viewModelScope.launch {
            subscriptionRepository.consumeProCelebration()
        }
    }

    private val isSyncingFlow: Flow<Boolean> = workManager
        .getWorkInfosForUniqueWorkFlow("SyncDownHistory")
        .map { workInfos ->
            val isRunning = workInfos.any { it.state == WorkInfo.State.RUNNING }
            Log.d("HomeViewModel", "SyncDownHistory work state: running=$isRunning (${workInfos.size} infos)")
            isRunning
        }
        .catch { e ->
            Log.w("HomeViewModel", "Error observing SyncDownHistory work state", e)
            emit(false)
        }

    private val baseUiState: Flow<HomeUiState> = authRepository.currentUser
        .flatMapLatest { user ->
            val userId = user?.uid ?: ""
            val userName = user?.displayName ?: ""
            val displayName = if (userName.isBlank()) "User" else userName

            if (userId.isEmpty()) {
                flowOf(HomeUiState.Success(emptyList(), emptyList(), emptyList(), 0, emptyList(), userName = displayName))
            } else {
                combine(
                    localRepository.getAllItems(userId),
                    localRepository.getAllCategories(userId),
                    subscriptionRepository.subscriptionTier,
                    subscriptionRepository.isCalendarSyncEnabled,
                    _searchQuery
                ) { allItems, categories, tier, calendarSync, query ->
                    val today = LocalDate.now()
                    
                    // Filter out items marked as DONE for the main view
                    val activeAndPendingItems = allItems.filter { it.status != "DONE" }

                    // Apply search filter if query is not blank
                    val filteredItems = if (query.isBlank()) {
                        activeAndPendingItems
                    } else {
                        activeAndPendingItems.filter { item ->
                            item.title.contains(query, ignoreCase = true) ||
                            (item.summary?.contains(query, ignoreCase = true) ?: false) ||
                            (item.category?.contains(query, ignoreCase = true) ?: false) ||
                            (item.organization?.contains(query, ignoreCase = true) ?: false) ||
                            (item.source?.contains(query, ignoreCase = true) ?: false)
                        }
                    }
                    
                    // Today Items: Items with deadline or event date today
                    val todayItems = filteredItems.filter { item ->
                        val dateToCompare = item.deadline ?: item.eventDate
                        if (dateToCompare == null) return@filter false
                        try {
                            val date = parseDate(dateToCompare)
                            date == today
                        } catch (e: Exception) {
                            false
                        }
                    }

                    // Last Analysed: Recently analyzed or captured items
                    val lastAnalysed = filteredItems
                        .sortedByDescending { it.createdAt }
                        .take(5)

                    // Upcoming: Items with deadlines in the future
                    val upcomingItems = filteredItems.filter { item ->
                        val deadline = item.deadline
                        if (deadline == null) return@filter false
                        try {
                            val date = parseDate(deadline)
                            date.isAfter(today)
                        } catch (e: Exception) {
                            false
                        }
                    }.sortedBy { it.deadline }

                    HomeUiState.Success(
                        todayItems = todayItems,
                        lastAnalysed = lastAnalysed,
                        upcomingItems = upcomingItems,
                        categoriesCount = activeAndPendingItems.size,
                        allItems = activeAndPendingItems,
                        userName = displayName,
                        searchResults = if (query.isBlank()) emptyList() else filteredItems,
                        searchQuery = query,
                        subscriptionTier = tier,
                        isCalendarSyncEnabled = calendarSync,
                        isSyncing = false
                    )
                }
            }
        }

    val uiState: StateFlow<HomeUiState> = combine(baseUiState, isSyncingFlow) { state, isSyncing ->
        if (state is HomeUiState.Success) {
            state.copy(isSyncing = isSyncing)
        } else {
            state
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState.Success(emptyList(), emptyList(), emptyList(), 0, emptyList())
    )

    fun onSearchQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
        if (newQuery.length >= 3) {
            analytics.logEvent("home_search", mapOf("query_length" to newQuery.length))
        }
    }

    fun refresh() {
        Log.d("HomeViewModel", "Manual pull-to-refresh requested.")
        SyncDownWorker.schedule(context)
    }

    fun updateUserName(name: String) {
        viewModelScope.launch {
            authRepository.updateDisplayName(name)
        }
    }

    private fun parseDate(dateStr: String): LocalDate {
        return if (dateStr.contains("T")) {
            if (dateStr.endsWith("Z")) {
                java.time.Instant.parse(dateStr).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
            } else {
                java.time.LocalDateTime.parse(dateStr).toLocalDate()
            }
        } else {
            LocalDate.parse(dateStr)
        }
    }
}

sealed class HomeUiState {
    object Loading : HomeUiState()
    data class Success(
        val todayItems: List<Item>,
        val lastAnalysed: List<Item>,
        val upcomingItems: List<Item>,
        val categoriesCount: Int,
        val allItems: List<Item>,
        val userName: String = "User",
        val searchResults: List<Item> = emptyList(),
        val searchQuery: String = "",
        val subscriptionTier: com.victorkirui.core.repository.SubscriptionTier = com.victorkirui.core.repository.SubscriptionTier.FREE,
        val isCalendarSyncEnabled: Boolean = true,
        val isSyncing: Boolean = false
    ) : HomeUiState()
}
