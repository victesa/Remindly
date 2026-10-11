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
import com.victorkirui.core.repository.SubscriptionTier
import com.victorkirui.local.entity.Item
import com.victorkirui.local.repository.LocalRepository
import com.victorkirui.module_features.capturing.SyncDownWorker
import com.victorkirui.module_features.capturing.SyncWorker
import com.victorkirui.module_features.inbox.InboxCategory
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

    private val _showProAd = MutableStateFlow(false)
    val showProAd = _showProAd.asStateFlow()

    init {
        // Trigger a local sync-up on startup
        SyncWorker.schedule(context)
        
        viewModelScope.launch {
            val tier = subscriptionRepository.subscriptionTier.first()
            if (tier == SubscriptionTier.PREMIUM) {
                SyncDownWorker.schedule(context)
            } else {
                if (subscriptionRepository.shouldShowProAd()) {
                    _showProAd.value = true
                    subscriptionRepository.recordProAdShown()
                }
            }
            authRepository.currentUser.value?.uid?.let { userId ->
                localRepository.deleteExpiredItems(userId)
            }
        }
    }

    fun dismissProAd() {
        _showProAd.value = false
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
            val firebaseUser = user ?: com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
            val userId = firebaseUser?.uid ?: ""
            val rawUserName = firebaseUser?.displayName?.trim()
            val emailPrefix = firebaseUser?.email?.substringBefore("@")?.trim()
            val fallbackName = emailPrefix?.split(".", "_", "-")
                ?.joinToString(" ") { word -> word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() } }

            val displayName = when {
                !rawUserName.isNullOrBlank() -> rawUserName
                !fallbackName.isNullOrBlank() -> fallbackName
                else -> "User"
            }


            if (userId.isEmpty()) {
                Log.w("HomeViewModel", "No userId found for HomeViewModel baseUiState")
                flowOf(HomeUiState.Loading)
            } else {
                combine(
                    localRepository.getAllItems(userId),
                    localRepository.getAllCategories(userId),
                    subscriptionRepository.subscriptionTier,
                    subscriptionRepository.isCalendarSyncEnabled,
                    _searchQuery
                ) { allItems, allCategoryNames, tier, calendarSync, query ->
                    Log.d("HomeViewModel", "combine triggered for userId='$userId': totalInDb=${allItems.size}, categoriesInDb=$allCategoryNames")
                    val today = LocalDate.now()
                    
                    // Filter out items marked as DONE for the main view
                    val activeAndPendingItems = allItems.filter { it.status != "DONE" }

                    val categoryList = mutableListOf<InboxCategory>()
                    val activeItems = allItems.filter { it.status != "DONE" && it.status != "PENDING" }
                    val pendingItems = allItems.filter { it.status == "PENDING" }
                    
                    if (pendingItems.isNotEmpty()) {
                        categoryList.add(
                            InboxCategory(
                                id = "PENDING_SYNC",
                                latestItemTitle = pendingItems.first().title,
                                count = pendingItems.size,
                                hasPendingSync = true,
                                iconType = "sync"
                            )
                        )
                    }
                    
                    allCategoryNames.forEach { name ->
                        val itemsInCategory = activeItems.filter { it.category == name }
                        val hasPendingInCategory = allItems.any { it.category == name && it.status == "PENDING" }
                        
                        if (itemsInCategory.isNotEmpty() || hasPendingInCategory) {
                            categoryList.add(
                                InboxCategory(
                                    id = name,
                                    latestItemTitle = itemsInCategory.firstOrNull()?.title ?: "Syncing...",
                                    count = itemsInCategory.size,
                                    hasPendingSync = hasPendingInCategory,
                                    iconType = getIconForCategory(name)
                                )
                            )
                        }
                    }

                    val uncategorized = activeItems.filter { it.category.isNullOrBlank() }
                    if (uncategorized.isNotEmpty()) {
                        categoryList.add(
                            InboxCategory(
                                id = "UNCATEGORIZED",
                                latestItemTitle = uncategorized.first().title,
                                count = uncategorized.size,
                                hasPendingSync = false,
                                iconType = "category"
                            )
                        )
                    }

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
                        categories = categoryList,
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
        started = SharingStarted.Lazily,
        initialValue = HomeUiState.Loading
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
        return try {
            com.victorkirui.core.util.DateUtils.parseToDateTime(dateStr).toLocalDate()
        } catch (e: Exception) {
            LocalDate.now()
        }
    }

    private fun getIconForCategory(name: String): String = when(name.lowercase()) {
        "jobs", "work", "career" -> "work"
        "scholarships", "school", "education" -> "school"
        "travel", "flights", "trip" -> "flight"
        "events", "meetups" -> "event"
        "bills", "receipts", "finance" -> "receipt"
        else -> "category"
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
        val categories: List<InboxCategory> = emptyList(),
        val userName: String = "User",
        val searchResults: List<Item> = emptyList(),
        val searchQuery: String = "",
        val subscriptionTier: com.victorkirui.core.repository.SubscriptionTier = com.victorkirui.core.repository.SubscriptionTier.FREE,
        val isCalendarSyncEnabled: Boolean = true,
        val isSyncing: Boolean = false
    ) : HomeUiState()
}
