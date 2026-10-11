package com.victorkirui.module_features.home

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Devices.FOLDABLE
import androidx.compose.ui.tooling.preview.Devices.TABLET
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.victorkirui.core.ui.theme.*
import com.victorkirui.core.R
import com.victorkirui.local.entity.Item
import com.victorkirui.core.util.DateUtils
import com.victorkirui.module_features.capturing.SourceIcon
import com.victorkirui.module_features.capturing.SourceIconProvider
import com.victorkirui.module_features.capturing.SyncDownWorker
import com.victorkirui.core.repository.SubscriptionRepository
import com.victorkirui.core.repository.SubscriptionTier
import com.victorkirui.module_features.inbox.SearchField
import androidx.compose.ui.text.style.TextOverflow
import org.koin.androidx.compose.koinViewModel
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.*

@Composable
fun HomeScreen(
    windowWidthSizeClass: WindowWidthSizeClass,
    onNavigateToInbox: () -> Unit = {},
    onNavigateToReminders: (String?) -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToItem: (String) -> Unit = {},
    onNavigateToCategory: (String) -> Unit = {},
    viewModel: HomeViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    HomeScreen(
        uiState = uiState,
        windowWidthSizeClass = windowWidthSizeClass,
        onNavigateToInbox = onNavigateToInbox,
        onNavigateToReminders = onNavigateToReminders,
        onNavigateToProfile = onNavigateToProfile,
        onNavigateToItem = onNavigateToItem,
        onNavigateToCategory = onNavigateToCategory,
        onSearchQueryChange = { viewModel.onSearchQueryChange(it) },
        onNameUpdate = { viewModel.updateUserName(it) },
        onRefresh = { viewModel.refresh() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    windowWidthSizeClass: WindowWidthSizeClass,
    onNavigateToInbox: () -> Unit = {},
    onNavigateToReminders: (String?) -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToItem: (String) -> Unit = {},
    onNavigateToCategory: (String) -> Unit = {},
    onSearchQueryChange: (String) -> Unit = {},
    onNameUpdate: (String) -> Unit = {},
    onRefresh: () -> Unit = {}
) {
    val isLargeScreen = windowWidthSizeClass == WindowWidthSizeClass.Expanded
    val isMediumScreen = windowWidthSizeClass == WindowWidthSizeClass.Medium
    var isSearchSheetVisible by remember { mutableStateOf(false) }

    val subscriptionRepository: SubscriptionRepository = org.koin.compose.koinInject()
    val activeSubscriptionTier by subscriptionRepository.subscriptionTier.collectAsState(initial = SubscriptionTier.FREE)
    val isInitialSyncPending by subscriptionRepository.isInitialSyncPending.collectAsState(initial = false)

    if (isInitialSyncPending) {
        SyncLoadingDialog("Syncing your saved items...")
    }

    val searchQuery = (uiState as? HomeUiState.Success)?.searchQuery ?: ""
    val searchResults = (uiState as? HomeUiState.Success)?.searchResults ?: emptyList()

    if (isSearchSheetVisible) {
        SearchSheet(
            searchQuery = searchQuery,
            onSearchQueryChange = onSearchQueryChange,
            searchResults = searchResults,
            onNavigateToItem = onNavigateToItem,
            onDismiss = {
                isSearchSheetVisible = false
                onSearchQueryChange("")
            }
        )
    }

    val pullRefreshState = rememberPullToRefreshState()
    val isSyncingFromState = (uiState as? HomeUiState.Success)?.isSyncing ?: false
    var isManualRefreshing by remember { mutableStateOf(false) }

    LaunchedEffect(isSyncingFromState) {
        if (!isSyncingFromState) {
            isManualRefreshing = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pullToRefresh(
                state = pullRefreshState,
                isRefreshing = isManualRefreshing,
                onRefresh = {
                    isManualRefreshing = true
                    onRefresh()
                }
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .statusBarsPadding()
        ) {
            if (!isLargeScreen) {
                RemindlyTopBar(
                    onSearchToggle = { isSearchSheetVisible = true },
                    isSearchActive = isSearchSheetVisible,
                    subscriptionTier = activeSubscriptionTier
                ) 
            } 
            
            if (isManualRefreshing) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().height(2.dp),
                    color = Color(0xFF2D6A4F),
                    trackColor = Color.Transparent
                )
            }
            
            when (val state = uiState) {
            HomeUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF2D6A4F))
                }
            }
            is HomeUiState.Success -> {
                when (windowWidthSizeClass) {
                    WindowWidthSizeClass.Expanded -> HomeScreenLargeContent(
                        state = state,
                        modifier = Modifier.weight(1f),
                        onNavigateToItem = onNavigateToItem,
                        onNavigateToInbox = onNavigateToInbox,
                        onNavigateToReminders = onNavigateToReminders,
                        onSearchClick = { isSearchSheetVisible = true },
                        onNavigateToCategory = onNavigateToCategory
                    )
                    else -> HomeScreenContent(
                        state = state,
                        modifier = Modifier.weight(1f),
                        isLargeScreen = false,
                        onNavigateToItem = onNavigateToItem,
                        onNavigateToInbox = onNavigateToInbox,
                        onNavigateToReminders = onNavigateToReminders,
                        onNavigateToCategory = onNavigateToCategory
                    )
                }
            }
        }
    }
}
}

@Composable
fun RemindlyTopBar(onSearchToggle: () -> Unit = {}, isSearchActive: Boolean = false, subscriptionTier: com.victorkirui.core.repository.SubscriptionTier = com.victorkirui.core.repository.SubscriptionTier.FREE) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 12.dp, top = 16.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Remindly",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2D6A4F),
                letterSpacing = (-0.5).sp
            )
            if (subscriptionTier == com.victorkirui.core.repository.SubscriptionTier.PREMIUM) {
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    color = Color(0xFFB8860B),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        "Pro",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        IconButton(onClick = onSearchToggle) {
            Icon(
                if (isSearchActive) Icons.Default.Close else Icons.Default.Search,
                contentDescription = "Search",
                tint = Color.Black.copy(alpha = 0.7f),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun HomeScreenContent(
    state: HomeUiState.Success,
    modifier: Modifier = Modifier,
    isLargeScreen: Boolean,
    onNavigateToItem: (String) -> Unit = {},
    onNavigateToInbox: () -> Unit = {},
    onNavigateToReminders: (String?) -> Unit = {},
    onNavigateToCategory: (String) -> Unit = {}
) {
    val isPremium = state.subscriptionTier == com.victorkirui.core.repository.SubscriptionTier.PREMIUM
    val context = androidx.compose.ui.platform.LocalContext.current
    var showNotificationBanner by remember {
        mutableStateOf(
            android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU &&
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }

    val notificationLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { _ ->
        showNotificationBanner = false
    }

    if (state.allItems.isEmpty()) {
        val scrollState = rememberScrollState()
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            GreetingSection(
                userName = state.userName, 
                attentionCount = state.todayItems.size,
                isPremium = isPremium
            )

            if (showNotificationBanner) {
                Spacer(modifier = Modifier.height(12.dp))
                NotificationPermissionBanner(
                    onEnableClick = {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                            notificationLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            showNotificationBanner = false
                        }
                    },
                    onDismissClick = { showNotificationBanner = false }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            CalendarStrip(items = state.allItems)
            
            if (isPremium && state.isCalendarSyncEnabled) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Sync,
                        contentDescription = null,
                        tint = Color(0xFF4285F4),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "Auto-sync enabled",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        fontSize = 10.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            HomeEmptyStateCard(modifier = Modifier.fillMaxWidth())

            Spacer(modifier = Modifier.height(24.dp))
        }
    } else {
        val scrollState = rememberScrollState()
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp)
        ) {
            GreetingSection(
                userName = state.userName, 
                attentionCount = state.todayItems.size,
                isPremium = isPremium
            )

            if (showNotificationBanner) {
                Spacer(modifier = Modifier.height(12.dp))
                NotificationPermissionBanner(
                    onEnableClick = {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                            notificationLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            showNotificationBanner = false
                        }
                    },
                    onDismissClick = { showNotificationBanner = false }
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            CalendarStrip(items = state.allItems)
            
            if (isPremium && state.isCalendarSyncEnabled) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Sync,
                        contentDescription = null,
                        tint = Color(0xFF4285F4),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "Auto-sync enabled",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        fontSize = 10.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            SectionHeader("TODAY", onViewAllClick = { onNavigateToReminders(LocalDate.now().toString()) })
            if (state.todayItems.isNotEmpty()) {
                state.todayItems.take(5).forEach { item ->
                    val displayTime = item.deadline?.let { "Deadline: ${DateUtils.formatDisplayDate(it)}" }
                        ?: item.eventDate?.let { DateUtils.formatDisplayDate(it) }
                        ?: "Today"
                    
                    if (isPremium) {
                        ProTaskItem(
                            title = item.title,
                            time = displayTime,
                            summary = item.summary,
                            tag = "Today",
                            onClick = { onNavigateToItem(item.id) }
                        )
                    } else {
                        TaskItem(
                            title = item.title,
                            time = displayTime,
                            icon = Icons.Default.AccessTime,
                            iconBgColor = Color(0xFFEAF2EE),
                            tag = "Today",
                            tagColor = Color(0xFFFEF5E7),
                            tagTextColor = Color(0xFFD35400),
                            onClick = { onNavigateToItem(item.id) }
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
            } else {
                EmptySectionPlaceholder("No tasks for today. Stay productive!")
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            SectionHeader("LAST ANALYSED", onViewAllClick = onNavigateToInbox)
            if (state.lastAnalysed.isNotEmpty()) {
                AnalysedSection(state.lastAnalysed, isPremium = isPremium, onNavigateToItem = onNavigateToItem)
            } else {
                EmptySectionPlaceholder("Recent captures will appear here.")
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            SectionHeader("UPCOMING", onViewAllClick = { onNavigateToReminders(LocalDate.now().plusDays(1).toString()) })
            if (state.upcomingItems.isNotEmpty()) {
                state.upcomingItems.take(5).forEach { item ->
                    val deadline = item.deadline
                    val daysLeft = if (deadline != null) {
                        try {
                            ChronoUnit.DAYS.between(LocalDate.now(), DateUtils.parseToDateTime(deadline).toLocalDate())
                        } catch (e: Exception) { 0 }
                    } else 0
                    val icon = SourceIconProvider.getIconForSource(item.source)
                    UpcomingTaskItem(
                        title = item.title,
                        subtitle = item.organization ?: item.source ?: "",
                        tag = when {
                            daysLeft == 0L -> "Today"
                            daysLeft == 1L -> "1 day left"
                            daysLeft < 0 -> "Overdue"
                            else -> "$daysLeft days left"
                        },
                        iconRes = (icon as? SourceIcon.Resource)?.resId,
                        icon = (icon as? SourceIcon.Vector)?.imageVector,
                        onClick = { onNavigateToItem(item.id) }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            } else {
                EmptySectionPlaceholder("No upcoming deadlines. You're ahead!")
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            SectionHeader("CATEGORIES", onViewAllClick = onNavigateToInbox)
            if (state.categories.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.categories.take(5).forEach { category ->
                        HomeCategoryCard(
                            category = category,
                            onClick = { onNavigateToCategory(category.id) }
                        )
                    }
                }
            } else {
                EmptySectionPlaceholder("No categories yet.")
            }
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun HomeEmptyStateCard(
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFFF9FAF9),
        border = BorderStroke(1.dp, Color(0xFFE0E0E0))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFEAF2EE)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = Color(0xFF2D6A4F),
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                "You're all set!",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                "No captures or deadlines yet. Use the Capture button to analyze job postings, events, or notes with AI.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                color = Color(0xFF2D6A4F).copy(alpha = 0.08f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFF2D6A4F).copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = Color(0xFF2D6A4F),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Tip: Share links or screenshots directly into Remindly",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun EmptySectionPlaceholder(text: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFF9FAF9)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.CheckCircle,
                contentDescription = null,
                tint = Color(0xFF2D6A4F),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun HomeScreenLargeContent(
    state: HomeUiState.Success,
    modifier: Modifier = Modifier,
    onNavigateToItem: (String) -> Unit = {},
    onNavigateToInbox: () -> Unit = {},
    onNavigateToReminders: (String?) -> Unit = {},
    onSearchClick: () -> Unit = {},
    onNavigateToCategory: (String) -> Unit = {}
) {
    val isPremium = state.subscriptionTier == com.victorkirui.core.repository.SubscriptionTier.PREMIUM
    
    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Text(
                    "Remindly",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2D6A4F),
                    fontSize = 22.sp
                )
                if (isPremium) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = Color(0xFFD4AF37),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            "Pro",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            GreetingSection(
                userName = state.userName, 
                attentionCount = state.todayItems.size, 
                titleSize = 22.sp, 
                titleColor = Color(0xFF111111),
                isPremium = isPremium
            )
            Spacer(modifier = Modifier.height(24.dp))
            MonthCalendar(
                items = state.allItems,
                dayFontSize = 15.sp,
                titleFontSize = 18.sp,
                titleColor = Color(0xFF111111),
                labelColor = Color(0xFF6B7C6E),
                dateColor = Color(0xFF111111)
            )
            
            if (isPremium && state.isCalendarSyncEnabled) {
                Row(
                    modifier = Modifier.padding(top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = Color(0xFF4285F4),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Google Calendar synced",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            SectionHeader("CATEGORIES", fontSize = 12.sp, color = Color(0xFF6B7C6E), onViewAllClick = onNavigateToInbox)
            if (state.categories.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.categories.take(5).forEach { category ->
                        HomeCategoryCard(
                            category = category,
                            onClick = { onNavigateToCategory(category.id) }
                        )
                    }
                }
            } else {
                EmptySectionPlaceholder("No categories yet.")
            }
        }

        Spacer(modifier = Modifier.width(48.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            LargeSearchBar(onClick = onSearchClick)
            Spacer(modifier = Modifier.height(32.dp))

            SectionHeader("TODAY", fontSize = 12.sp, color = Color(0xFF6B7C6E), onViewAllClick = { onNavigateToReminders(LocalDate.now().toString()) })
            if (state.todayItems.isNotEmpty()) {
                state.todayItems.take(5).forEach { item ->
                    val displayTime = item.deadline?.let { "Deadline: ${DateUtils.formatDisplayDate(it)}" }
                        ?: item.eventDate?.let { DateUtils.formatDisplayDate(it) }
                        ?: "Today"
                    
                    if (isPremium) {
                        ProTaskItem(
                            title = item.title,
                            time = displayTime,
                            summary = item.summary,
                            tag = "Today",
                            onClick = { onNavigateToItem(item.id) }
                        )
                    } else {
                        TaskItem(
                            title = item.title,
                            time = displayTime,
                            icon = Icons.Default.AccessTime,
                            iconBgColor = Color(0xFFEAF2EE),
                            tag = "Today",
                            tagColor = Color(0xFFFEF5E7),
                            tagTextColor = Color(0xFF2D6A4F),
                            titleFontSize = 15.sp,
                            titleColor = Color(0xFF111111),
                            timeColor = Color(0xFF6B7C6E),
                            tagFontSize = 11.sp,
                            onClick = { onNavigateToItem(item.id) }
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
            } else {
                EmptySectionPlaceholder("No tasks for today.")
            }

            Spacer(modifier = Modifier.height(20.dp))

            SectionHeader("LAST ANALYSED", fontSize = 12.sp, color = Color(0xFF6B7C6E), onViewAllClick = onNavigateToInbox)
            if (state.lastAnalysed.isNotEmpty()) {
                AnalysedSectionLarge(state.lastAnalysed, isPremium = isPremium, onNavigateToItem = onNavigateToItem)
            } else {
                EmptySectionPlaceholder("Recent captures will appear here.")
            }

            Spacer(modifier = Modifier.height(32.dp))

            SectionHeader("UPCOMING", fontSize = 12.sp, color = Color(0xFF6B7C6E), onViewAllClick = { onNavigateToReminders(LocalDate.now().plusDays(1).toString()) })
            if (state.upcomingItems.isNotEmpty()) {
                state.upcomingItems.take(5).forEach { item ->
                    val deadline = item.deadline
                    val daysLeft = if (deadline != null) {
                        try {
                            ChronoUnit.DAYS.between(LocalDate.now(), DateUtils.parseToDateTime(deadline).toLocalDate())
                        } catch (e: Exception) { 0 }
                    } else 0
                    val icon = SourceIconProvider.getIconForSource(item.source)
                    UpcomingTaskItem(
                        title = item.title,
                        subtitle = item.organization ?: item.source ?: "",
                        tag = when {
                            daysLeft == 0L -> "Today"
                            daysLeft == 1L -> "1 day left"
                            daysLeft < 0 -> "Overdue"
                            else -> "$daysLeft days left"
                        },
                        iconRes = (icon as? SourceIcon.Resource)?.resId,
                        icon = (icon as? SourceIcon.Vector)?.imageVector,
                        titleSize = 15.sp,
                        titleColor = Color(0xFF111111),
                        subtitleColor = Color(0xFF6B7C6E),
                        subtitleSize = 12.sp,
                        tagFontSize = 11.sp,
                        onClick = { onNavigateToItem(item.id) }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            } else {
                EmptySectionPlaceholder("No upcoming deadlines.")
            }
        }
    }
}

@Composable
fun HomeScreenMediumContent(
    state: HomeUiState.Success,
    modifier: Modifier = Modifier,
    onNavigateToItem: (String) -> Unit = {},
    onNavigateToInbox: () -> Unit = {},
    onNavigateToReminders: (String?) -> Unit = {},
    onSearchClick: () -> Unit = {},
    onNavigateToCategory: (String) -> Unit = {}
) {
    val isPremium = state.subscriptionTier == com.victorkirui.core.repository.SubscriptionTier.PREMIUM

    if (state.allItems.isEmpty()) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            // App Title Branding & Search
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Remindly",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2D6A4F),
                        fontSize = 24.sp
                    )
                    if (isPremium) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = Color(0xFFD4AF37),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                "Pro",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                IconButton(onClick = onSearchClick) {
                    Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.Black, modifier = Modifier.size(24.dp))
                }
            }

            GreetingSection(
                userName = state.userName,
                attentionCount = state.todayItems.size,
                titleSize = 20.sp,
                bodySize = 13.sp,
                isPremium = isPremium
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Week Calendar Strip
            CalendarStrip(items = state.allItems)

            if (isPremium && state.isCalendarSyncEnabled) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Sync,
                        contentDescription = null,
                        tint = Color(0xFF4285F4),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "Auto-sync enabled",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            HomeEmptyStateCard(modifier = Modifier.weight(1f))

            Spacer(modifier = Modifier.height(16.dp))
        }
    } else {
        val scrollState = rememberScrollState()
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            // App Title Branding & Search
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Remindly",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2D6A4F),
                        fontSize = 24.sp
                    )
                    if (isPremium) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = Color(0xFFD4AF37),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                "Pro",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                IconButton(onClick = onSearchClick) {
                    Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.Black, modifier = Modifier.size(24.dp))
                }
            }

            GreetingSection(
                userName = state.userName,
                attentionCount = state.todayItems.size,
                titleSize = 20.sp,
                bodySize = 13.sp,
                isPremium = isPremium
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Week Calendar Strip
            CalendarStrip(items = state.allItems)

            if (isPremium && state.isCalendarSyncEnabled) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Sync,
                        contentDescription = null,
                        tint = Color(0xFF4285F4),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "Auto-sync enabled",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Two Columns: Today & Categories (Left), Last Analysed & Upcoming (Right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Left Column: TODAY & CATEGORIES
                Column(modifier = Modifier.weight(1f)) {
                    SectionHeader("TODAY", fontSize = 11.sp, color = Color(0xFF6B7C6E), onViewAllClick = { onNavigateToReminders(LocalDate.now().toString()) })
                    if (state.todayItems.isNotEmpty()) {
                        state.todayItems.take(5).forEach { item ->
                            val displayTime = item.deadline?.let { "Deadline: ${DateUtils.formatDisplayDate(it)}" }
                                ?: item.eventDate?.let { DateUtils.formatDisplayDate(it) }
                                ?: "Today"
                            
                            if (isPremium) {
                                ProTaskItem(
                                    title = item.title,
                                    time = displayTime,
                                    summary = item.summary,
                                    tag = "Today",
                                    onClick = { onNavigateToItem(item.id) }
                                )
                            } else {
                                TaskItem(
                                    title = item.title,
                                    time = displayTime,
                                    icon = Icons.Default.AccessTime,
                                    iconBgColor = Color(0xFFEAF2EE),
                                    tag = "Today",
                                    tagColor = Color(0xFF2D6A4F),
                                    tagTextColor = Color.White,
                                    titleFontSize = 14.sp,
                                    onClick = { onNavigateToItem(item.id) }
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    } else {
                        EmptySectionPlaceholder("No tasks for today.")
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    SectionHeader("CATEGORIES", fontSize = 11.sp, color = Color(0xFF6B7C6E), onViewAllClick = onNavigateToInbox)
                    if (state.categories.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            state.categories.take(5).forEach { category ->
                                HomeCategoryCard(
                                    category = category,
                                    onClick = { onNavigateToCategory(category.id) }
                                )
                            }
                        }
                    } else {
                        EmptySectionPlaceholder("No categories yet.")
                    }
                }

                // Right Column: LAST ANALYSED & UPCOMING
                Column(modifier = Modifier.weight(1f)) {
                    SectionHeader("LAST ANALYSED", fontSize = 11.sp, color = Color(0xFF6B7C6E), onViewAllClick = onNavigateToInbox)
                    if (state.lastAnalysed.isNotEmpty()) {
                        AnalysedSectionMedium(state.lastAnalysed, isPremium = isPremium, onNavigateToItem = onNavigateToItem)
                    } else {
                        EmptySectionPlaceholder("No recent captures.")
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    SectionHeader("UPCOMING", fontSize = 11.sp, color = Color(0xFF6B7C6E), onViewAllClick = { onNavigateToReminders(LocalDate.now().plusDays(1).toString()) })
                    if (state.upcomingItems.isNotEmpty()) {
                        state.upcomingItems.take(5).forEach { item ->
                            val deadline = item.deadline
                            val daysLeft = if (deadline != null) {
                                try {
                                    ChronoUnit.DAYS.between(LocalDate.now(), DateUtils.parseToDateTime(deadline).toLocalDate())
                                } catch (e: Exception) { 0 }
                            } else 0
                            val icon = SourceIconProvider.getIconForSource(item.source)
                            UpcomingTaskItem(
                                title = item.title,
                                subtitle = item.organization ?: item.source ?: "",
                                tag = when {
                                    daysLeft == 0L -> "Today"
                                    daysLeft == 1L -> "1 day left"
                                    daysLeft < 0 -> "Overdue"
                                    else -> "$daysLeft days left"
                                },
                                iconRes = (icon as? SourceIcon.Resource)?.resId,
                                icon = (icon as? SourceIcon.Vector)?.imageVector,
                                titleSize = 14.sp,
                                subtitleSize = 12.sp,
                                onClick = { onNavigateToItem(item.id) }
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    } else {
                        EmptySectionPlaceholder("No upcoming deadlines.")
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun GreetingSection(
    userName: String, 
    attentionCount: Int, 
    titleSize: TextUnit = 22.sp, 
    bodySize: TextUnit = 14.sp, 
    titleColor: Color = Color.Black,
    isPremium: Boolean = false
) {
    val hour = LocalTime.now().hour
    val greeting = when (hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        in 17..21 -> "Good evening"
        else -> "Good night"
    }
    val firstName = remember(userName) { userName.substringBefore(" ") }
    Column {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "$greeting, $firstName",
            style = MaterialTheme.typography.headlineSmall.copy(fontSize = titleSize),
            fontWeight = FontWeight.ExtraBold,
            color = titleColor
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            if (isPremium) "You have $attentionCount opportunities that need attention this week." 
            else "You have $attentionCount things needing attention today.",
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = bodySize),
            color = Color.Gray,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun ProTaskItem(
    title: String,
    time: String,
    summary: String?,
    tag: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFFF9FAF9),
        border = BorderStroke(1.dp, Color(0xFFEEEEEE))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEAF2EE)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (title.lowercase().contains("scholarship")) Icons.Default.WarningAmber else Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = if (title.lowercase().contains("scholarship")) Color(0xFFC0392B) else Color(0xFF2D6A4F),
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.Black
                    )
                    Text(
                        time,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (time.lowercase().contains("tomorrow")) Color(0xFFD35400) else Color.Gray
                    )
                }
                
                Surface(
                    color = Color.White,
                    shape = CircleShape,
                    border = BorderStroke(1.dp, Color(0xFFF0F0F0))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            tag,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray
                        )
                    }
                }
            }
            
            if (summary != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color(0xFF2D6A4F),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = summary,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF6B7C6E),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    }
                    
                    // Quick Sync Button
                    IconButton(
                        onClick = onClick, // Navigates to details where sync is prominent
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.CalendarToday,
                            contentDescription = "Sync",
                            tint = Color(0xFF4285F4),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CalendarStrip(items: List<Item>) {
    val today = LocalDate.now()
    val sunday = today.minusDays(today.dayOfWeek.value % 7L)
    val days = (0..6).map { sunday.plusDays(it.toLong()) }
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .padding(vertical = 12.dp, horizontal = 4.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            days.forEach { date ->
                val isSelected = date == today
                val dayName = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
                val dayOfMonth = date.dayOfMonth.toString()
                
                val hasItemsOnDay = items.any { item ->
                    val deadlineDate = item.deadline?.let { DateUtils.parseToDateTime(it).toLocalDate() }
                    val eventDate = item.eventDate?.let { DateUtils.parseToDateTime(it).toLocalDate() }
                    (deadlineDate == date || eventDate == date) && (date >= today)
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(42.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .padding(vertical = 10.dp)
                ) {
                    Text(
                        dayName,
                        fontSize = 11.sp,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        dayOfMonth,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .size(3.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.onPrimary 
                                else if (hasItemsOnDay) MaterialTheme.colorScheme.primary 
                                else Color.Transparent
                            )
                    )
                }
            }
        }
    }
}

@Composable
fun SectionHeader(
    title: String, 
    fontSize: TextUnit = 11.sp, 
    color: Color = Color.Gray,
    onViewAllClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp, top = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = color,
            letterSpacing = 1.2.sp,
            fontSize = fontSize
        )
        if (onViewAllClick != null) {
            TextButton(
                onClick = onViewAllClick,
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
            ) {
                Text(
                    "View All",
                    color = Color(0xFF2D6A4F),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun TaskItem(
    title: String,
    time: String,
    icon: ImageVector,
    iconBgColor: Color,
    tag: String? = null,
    tagColor: Color = Color.Transparent,
    tagTextColor: Color = Color.Transparent,
    timeColor: Color = Color.Gray,
    titleFontSize: TextUnit = 16.sp,
    titleColor: Color = Color.Black,
    tagFontSize: TextUnit = 11.sp,
    onClick: () -> Unit = {}
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFFF9FAF9)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(iconBgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (iconBgColor == Color(0xFFFDEDEC)) Color(0xFFC0392B) else Color(0xFF2D6A4F),
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    fontWeight = FontWeight.Bold,
                    fontSize = titleFontSize,
                    color = titleColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    time,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                    color = timeColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            if (tag != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    color = tagColor,
                    shape = CircleShape
                ) {
                    Text(
                        tag,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        fontSize = tagFontSize,
                        fontWeight = FontWeight.Bold,
                        color = tagTextColor
                    )
                }
            }
        }
    }
}

@Composable
fun UpcomingCarouselMedium(
    items: List<Item>,
    onNavigateToItem: (String) -> Unit = {}
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(end = 12.dp)
    ) {
        items(items.take(3)) { item ->
            val deadline = item.deadline
            val daysLeft = if (deadline != null) {
                try {
                    ChronoUnit.DAYS.between(LocalDate.now(), DateUtils.parseToDateTime(deadline).toLocalDate())
                } catch (e: Exception) { 0 }
            } else 0
            val icon = SourceIconProvider.getIconForSource(item.source)
            UpcomingTaskCard(
                modifier = Modifier.width(220.dp),
                title = item.title,
                subtitle = item.organization ?: item.source ?: "",
                tag = when {
                    daysLeft == 0L -> "Today"
                    daysLeft == 1L -> "1 day left"
                    daysLeft < 0 -> "Overdue"
                    else -> "$daysLeft days left"
                },
                iconRes = (icon as? SourceIcon.Resource)?.resId,
                icon = (icon as? SourceIcon.Vector)?.imageVector,
                onClick = { onNavigateToItem(item.id) }
            )
        }
    }
}

@Composable
fun UpcomingTaskCard(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    tag: String,
    iconRes: Int? = null,
    icon: ImageVector? = null,
    onClick: () -> Unit = {}
) {
    Surface(
        modifier = modifier
            .height(110.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFFF9FAF9),
        border = BorderStroke(1.dp, Color(0xFFEEEEEE))
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(36.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = Color.White
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (iconRes != null) {
                            Icon(
                                painter = painterResource(id = iconRes),
                                contentDescription = null,
                                tint = Color(0xFF2D6A4F),
                                modifier = Modifier.size(18.dp)
                            )
                        } else if (icon != null) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = Color(0xFF2D6A4F),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Surface(
                    color = Color(0xFF2D6A4F),
                    shape = CircleShape
                ) {
                    Text(
                        tag,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Column {
                Text(
                    title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFF0D253F),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    subtitle,
                    fontSize = 12.sp,
                    color = Color(0xFF91A5BA),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun UpcomingTaskItem(
    title: String,
    subtitle: String,
    tag: String,
    iconRes: Int? = null,
    icon: ImageVector? = null,
    titleSize: TextUnit = 16.sp,
    subtitleSize: TextUnit = 13.sp,
    titleColor: Color = Color(0xFF0D253F),
    subtitleColor: Color = Color(0xFF91A5BA),
    tagFontSize: TextUnit = 11.sp,
    onClick: () -> Unit = {}
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFFF9FAF9)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color.White
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (iconRes != null) {
                        Icon(
                            painter = painterResource(id = iconRes),
                            contentDescription = null,
                            tint = Color(0xFF2D6A4F),
                            modifier = Modifier.size(18.dp)
                        )
                    } else if (icon != null) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = Color(0xFF2D6A4F),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    fontWeight = FontWeight.Bold,
                    fontSize = titleSize,
                    color = titleColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = (titleSize.value + 2).sp
                )
                Text(
                    subtitle,
                    fontSize = subtitleSize,
                    color = subtitleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Surface(
                color = Color(0xFF2D6A4F),
                shape = CircleShape
            ) {
                Text(
                    tag,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    fontSize = tagFontSize,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun HomeCategoryCard(
    category: com.victorkirui.module_features.inbox.InboxCategory,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFF9FAF9),
        border = BorderStroke(1.dp, Color(0xFFEEEEEE))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFEAF2EE)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when(category.iconType) {
                        "work" -> Icons.Default.WorkOutline
                        "school" -> Icons.Default.School
                        "event" -> Icons.Default.Event
                        "receipt" -> Icons.Default.Receipt
                        "flight" -> Icons.Default.Flight
                        "sync" -> Icons.Default.Sync
                        "history" -> Icons.Default.History
                        else -> Icons.Default.Category
                    },
                    contentDescription = null,
                    tint = Color(0xFF2D6A4F),
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    category.id,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Surface(
                color = Color(0xFF2D6A4F),
                shape = CircleShape
            ) {
                Text(
                    category.count.toString(),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun AnalysedSection(items: List<Item>, isPremium: Boolean = false, onNavigateToItem: (String) -> Unit = {}) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(end = 20.dp)
    ) {
        items(items) { item ->
            val icon = SourceIconProvider.getIconForSource(item.source)
            AnalysedCard(
                modifier = Modifier.width(180.dp),
                title = item.title,
                category = item.category ?: "Capture",
                timeAgo = when (item.status) {
                    "PENDING" -> "Processing..."
                    "RETRYING" -> "Retrying soon..."
                    "FAILED" -> "Failed to process"
                    else -> {
                        val analyzedAt = item.metadata?.get("analyzed_at")
                        if (analyzedAt != null) {
                            "Analyzed ${DateUtils.getTimeAgo(analyzedAt)}"
                        } else {
                            "Captured ${DateUtils.getTimeAgo(item.createdAt)}"
                        }
                    }
                },
                status = item.status,
                iconRes = (icon as? SourceIcon.Resource)?.resId,
                icon = (icon as? SourceIcon.Vector)?.imageVector,
                isPremium = isPremium,
                onClick = { onNavigateToItem(item.id) }
            )
        }
    }
}

@Composable
fun AnalysedSectionLarge(items: List<Item>, isPremium: Boolean = false, onNavigateToItem: (String) -> Unit = {}) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(end = 16.dp)
    ) {
        items(items.take(3)) { item ->
            val icon = SourceIconProvider.getIconForSource(item.source)
            AnalysedCard(
                modifier = Modifier.width(180.dp),
                title = item.title,
                category = item.category ?: "Capture",
                timeAgo = when (item.status) {
                    "PENDING" -> "Processing..."
                    "RETRYING" -> "Retrying soon..."
                    "FAILED" -> "Failed"
                    else -> {
                        val analyzedAt = item.metadata?.get("analyzed_at")
                        if (analyzedAt != null) DateUtils.getTimeAgo(analyzedAt) else DateUtils.getTimeAgo(item.createdAt)
                    }
                },
                status = item.status,
                iconRes = (icon as? SourceIcon.Resource)?.resId,
                icon = (icon as? SourceIcon.Vector)?.imageVector,
                padding = 12.dp,
                isPremium = isPremium,
                onClick = { onNavigateToItem(item.id) }
            )
        }
    }
}

@Composable
fun AnalysedSectionMedium(items: List<Item>, isPremium: Boolean = false, onNavigateToItem: (String) -> Unit = {}) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(end = 12.dp)
    ) {
        items(items) { item ->
            val icon = SourceIconProvider.getIconForSource(item.source)
            AnalysedCard(
                modifier = Modifier.width(160.dp),
                title = item.title,
                category = item.category ?: "Capture",
                timeAgo = when (item.status) {
                    "PENDING" -> "Processing..."
                    "RETRYING" -> "Retrying soon..."
                    "FAILED" -> "Failed"
                    else -> {
                        val analyzedAt = item.metadata?.get("analyzed_at")
                        if (analyzedAt != null) DateUtils.getTimeAgo(analyzedAt) else DateUtils.getTimeAgo(item.createdAt)
                    }
                },
                status = item.status,
                iconRes = (icon as? SourceIcon.Resource)?.resId,
                icon = (icon as? SourceIcon.Vector)?.imageVector,
                padding = 10.dp,
                isPremium = isPremium,
                onClick = { onNavigateToItem(item.id) }
            )
        }
    }
}

@Composable
fun AnalysedCard(
    modifier: Modifier = Modifier,
    title: String,
    category: String,
    timeAgo: String,
    status: String = "DONE",
    iconRes: Int? = null,
    icon: ImageVector? = null,
    padding: Dp = 12.dp,
    badgeColor: Color = Color(0xFF2D6A4F),
    isPremium: Boolean = false,
    onClick: () -> Unit = {}
) {
    val (badgeText, badgeBgColor, badgeTextColor) = when (status) {
        "PENDING" -> Triple("Analyzing...", Color(0xFFFFF7E6), Color(0xFFD48806))
        "FAILED" -> Triple("Failed", Color(0xFFFFF1F0), Color(0xFFCF1322))
        "RETRYING" -> Triple("Retrying...", Color(0xFFF0F5FF), Color(0xFF1D39C4))
        "READY", "DONE", "ACTIVE" -> Triple("Analysed", Color(0xFFEAF2EE), if (isPremium) Color(0xFF2D6A4F) else badgeColor)
        else -> Triple("Captured", Color(0xFFF0F0F0), Color.Gray)
    }

    val categoryText = if (status == "PENDING" || status == "RETRYING") "Processing" else category
    val categoryBgColor = if (status == "PENDING" || status == "RETRYING" || status == "FAILED") Color(0xFFF0F0F0) else Color(0xFF2D6A4F)
    val categoryTextColor = if (status == "PENDING" || status == "RETRYING" || status == "FAILED") Color.Gray else Color.White
    
    Surface(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(if (isPremium) 24.dp else 20.dp),
        color = Color(0xFFF9FAF9),
        border = BorderStroke(1.dp, if (isPremium) Color(0xFFD4AF37).copy(alpha = 0.3f) else Color(0xFFF0F0F0))
    ) {
        Column(modifier = Modifier.padding(padding)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    if (iconRes != null) {
                        Icon(
                            painter = painterResource(id = iconRes),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = Color(0xFF2D6A4F)
                        )
                    } else if (icon != null) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = Color(0xFF2D6A4F)
                        )
                    }
                }
                
                Surface(
                    color = badgeBgColor,
                    shape = CircleShape
                ) {
                    Text(
                        badgeText,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeTextColor
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                title,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                maxLines = 2,
                minLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = Color.Black,
                lineHeight = 18.sp
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Surface(
                color = if (isPremium) Color(0xFF2D6A4F) else categoryBgColor,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    categoryText,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isPremium) Color.White else categoryTextColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                timeAgo,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun LargeSearchBar(onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFFF4F6F4)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Search captures...", color = Color.Gray)
        }
    }
}

@Composable
fun MonthCalendar(
    items: List<Item> = emptyList(),
    showLegend: Boolean = false,
    dayFontSize: TextUnit = 14.sp,
    titleFontSize: TextUnit = 18.sp,
    titleColor: Color = Color.Black,
    labelColor: Color = Color.Gray,
    dateColor: Color = Color.Black
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = if (showLegend) Color.Transparent else Color(0xFFF9FAF9)
    ) {
        Column(modifier = Modifier.padding(if (showLegend) 0.dp else 24.dp)) {
            val currentMonth = LocalDate.now().withDayOfMonth(1) // Assuming current month for simplicity
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.ChevronLeft, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(if (showLegend) 20.dp else 24.dp))
                Text(
                    "${currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${currentMonth.year}",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    fontSize = titleFontSize,
                    color = titleColor
                )
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(if (showLegend) 20.dp else 24.dp))
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            val daysOfWeek = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                daysOfWeek.forEach { day ->
                    Text(day, modifier = Modifier.width(36.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center, fontSize = 12.sp, color = labelColor, fontWeight = FontWeight.Medium)
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Generate simple calendar data for the current month
            val calendarData = remember(currentMonth) {
                val weeks = mutableListOf<List<Int>>()
                var firstDayOfMonth = currentMonth.dayOfWeek.value // 1 (Mon) to 7 (Sun)
                val daysInMonth = currentMonth.lengthOfMonth()
                val prevMonthDays = currentMonth.minusMonths(1).lengthOfMonth()
                
                var currentDay = 1 - (firstDayOfMonth - 1)
                for (i in 0..5) {
                    val week = mutableListOf<Int>()
                    for (j in 0..6) {
                        if (currentDay < 1) {
                            week.add(prevMonthDays + currentDay)
                        } else if (currentDay > daysInMonth) {
                            week.add(currentDay - daysInMonth)
                        } else {
                            week.add(currentDay)
                        }
                        currentDay++
                    }
                    weeks.add(week)
                }
                weeks
            }
            
            calendarData.forEachIndexed { rowIndex, week ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = if (showLegend) 10.dp else 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    week.forEachIndexed { colIndex, day ->
                        val isPrevMonth = rowIndex == 0 && day > 20
                        val isNextMonth = rowIndex >= 4 && day < 15
                        val isCurrentMonth = !isPrevMonth && !isNextMonth
                        
                        val date = if (isCurrentMonth) {
                            currentMonth.withDayOfMonth(day)
                        } else if (isPrevMonth) {
                            currentMonth.minusMonths(1).withDayOfMonth(day)
                        } else {
                            currentMonth.plusMonths(1).withDayOfMonth(day)
                        }
                        
                        val isToday = date == LocalDate.now()
                        val isPast = date.isBefore(LocalDate.now())
                        
                        val itemsOnDay = items.filter { item ->
                            val deadlineDate = item.deadline?.let { DateUtils.parseToDateTime(it).toLocalDate() }
                            val eventDate = item.eventDate?.let { DateUtils.parseToDateTime(it).toLocalDate() }
                            deadlineDate == date || eventDate == date
                        }
                        val hasDot = itemsOnDay.isNotEmpty() && !isPast
                        val isUrgent = itemsOnDay.any { item ->
                            val deadlineDate = item.deadline?.let { DateUtils.parseToDateTime(it).toLocalDate() }
                            deadlineDate != null && ChronoUnit.DAYS.between(LocalDate.now(), deadlineDate) <= 2
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.width(36.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(if (showLegend) 28.dp else 32.dp)
                                    .clip(CircleShape)
                                    .background(if (isToday) Color(0xFF2D6A4F) else Color.Transparent),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    day.toString(),
                                    fontSize = dayFontSize,
                                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isToday) Color.White else if (!isCurrentMonth) Color.LightGray else dateColor
                                )
                            }
                            
                            if (hasDot) {
                                val dotColor = if (isUrgent) Color.Red else Color(0xFF2D6A4F)
                                Box(
                                    modifier = Modifier
                                        .padding(top = 2.dp)
                                        .size(3.dp)
                                        .clip(CircleShape)
                                        .background(dotColor)
                                )
                            } else {
                                Spacer(modifier = Modifier.height(5.dp))
                            }
                        }
                    }
                }
            }

            if (showLegend) {
                Spacer(modifier = Modifier.height(24.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF2D6A4F)))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Has items", fontSize = 10.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.width(16.dp))
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color.Red))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Urgent", fontSize = 10.sp, color = Color.Gray)
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
fun HomeSmallScreenPreview() {
    RemindlyTheme {
        HomeScreen(
            uiState = PreviewHomeUiState,
            windowWidthSizeClass = WindowWidthSizeClass.Compact
        )
    }
}

@Preview(showBackground = true, device = "spec:width=673dp,height=841dp")
@Composable
fun HomeMediumScreenPreview() {
    RemindlyTheme {
        HomeScreen(
            uiState = PreviewHomeUiState,
            windowWidthSizeClass = WindowWidthSizeClass.Medium
        )
    }
}

@Preview(showBackground = true, device = TABLET)
@Composable
fun HomeLargeScreenPreview() {
    RemindlyTheme {
        HomeScreen(
            uiState = PreviewHomeUiState,
            windowWidthSizeClass = WindowWidthSizeClass.Expanded
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchSheet(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    searchResults: List<Item>,
    onNavigateToItem: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    
    val focusRequester = remember { FocusRequester() }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)) },
        modifier = Modifier.fillMaxHeight(0.9f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            Text(
                "Search Captures",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            SearchField(
                placeholder = "Type to search...",
                text = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier
                    .padding(bottom = 8.dp)
                    .focusRequester(focusRequester)
            )
            
            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (searchQuery.isBlank()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Search, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(64.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Looking for something?", color = Color.Gray)
                    }
                }
            } else if (searchResults.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(64.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No results for \"$searchQuery\"", color = Color.Gray)
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    items(searchResults) { item ->
                        val icon = SourceIconProvider.getIconForSource(item.source)
                        TaskItem(
                            title = item.title,
                            time = item.deadline?.let { DateUtils.formatDisplayDate(it) } ?: item.eventDate?.let { DateUtils.formatDisplayDate(it) } ?: "No date set",
                            icon = (icon as? SourceIcon.Vector)?.imageVector ?: Icons.Default.Description,
                            iconBgColor = Color(0xFFEAF2EE),
                            tag = item.category,
                            tagColor = Color(0xFFFEF5E7),
                            tagTextColor = Color(0xFFD35400),
                            onClick = { 
                                onDismiss()
                                onNavigateToItem(item.id) 
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationPermissionBanner(
    onEnableClick: () -> Unit,
    onDismissClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFEAF2EE),
        border = BorderStroke(1.dp, Color(0xFF2D6A4F).copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2D6A4F).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = Color(0xFF2D6A4F),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Turn On Notifications",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Text(
                        text = "Enable alerts to stay updated on task deadlines.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.DarkGray,
                        fontSize = 11.sp
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onEnableClick) {
                    Text("Enable", color = Color(0xFF2D6A4F), fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = onDismissClick, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = Color.Gray)
                }
            }
        }
    }
}

private val PreviewHomeUiState = HomeUiState.Success(
    todayItems = listOf(
        Item(
            id = "1",
            userId = "sample",
            title = "Check emails",
            summary = null,
            category = null,
            deadline = "2026-08-04",
            eventDate = null,
            createdAt = "2026-08-04T10:18:17.485Z",
            status = "pending"
        ),
        Item(
            id = "2",
            userId = "sample",
            title = "Meeting with team",
            summary = null,
            category = null,
            deadline = null,
            eventDate = "2026-08-04",
            organization = "Remindly Team",
            createdAt = "2026-08-04T10:18:17.485Z",
            status = "pending"
        )
    ),
    lastAnalysed = listOf(
        Item(
            id = "3",
            userId = "sample",
            title = "Project Proposal",
            summary = "Analysis of the new project",
            category = "Project",
            deadline = null,
            eventDate = null,
            source = "PDF",
            createdAt = "2026-08-04T08:18:17.485Z",
            status = "analysed"
        )
    ),
    upcomingItems = listOf(
        Item(
            id = "4",
            userId = "sample",
            title = "Buy groceries",
            summary = null,
            category = null,
            deadline = "2026-08-06",
            eventDate = null,
            createdAt = "2026-08-04T10:18:17.485Z",
            status = "pending"
        )
    ),
    categoriesCount = 3,
    allItems = emptyList()
)

@Composable
fun SyncLoadingDialog(message: String = "Syncing your saved items...") {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = { },
        properties = androidx.compose.ui.window.DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(36.dp),
                    color = Color(0xFF2D6A4F),
                    strokeWidth = 3.dp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black
                )
            }
        }
    }
}
