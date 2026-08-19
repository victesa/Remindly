package com.victorkirui.module_features.home

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
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
import com.victorkirui.module_features.inbox.SearchField
import androidx.compose.ui.text.style.TextOverflow
import org.koin.androidx.compose.koinViewModel
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.*

@Composable
fun HomeScreen(
    windowWidthSizeClass: WindowWidthSizeClass,
    onNavigateToInbox: () -> Unit = {},
    onNavigateToReminders: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToItem: (String) -> Unit = {},
    onCaptureClick: () -> Unit = {},
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
        onCaptureClick = onCaptureClick,
        onSearchQueryChange = { viewModel.onSearchQueryChange(it) }
    )
}

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    windowWidthSizeClass: WindowWidthSizeClass,
    onNavigateToInbox: () -> Unit = {},
    onNavigateToReminders: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToItem: (String) -> Unit = {},
    onCaptureClick: () -> Unit = {},
    onSearchQueryChange: (String) -> Unit = {}
) {
    val isLargeScreen = windowWidthSizeClass == WindowWidthSizeClass.Expanded
    val isMediumScreen = windowWidthSizeClass == WindowWidthSizeClass.Medium
    var isSearchVisible by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    val searchQuery = (uiState as? HomeUiState.Success)?.searchQuery ?: ""

    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
        if (!isLargeScreen && !isMediumScreen) {
            RemindlyTopBar(
                onSearchToggle = { 
                    isSearchVisible = !isSearchVisible
                    if (!isSearchVisible) {
                        onSearchQueryChange("")
                        focusManager.clearFocus()
                    }
                },
                isSearchActive = isSearchVisible
            ) 
        } 

        AnimatedVisibility(
            visible = isSearchVisible && !isLargeScreen && !isMediumScreen,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                SearchField(
                    placeholder = "Search captures...",
                    text = searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier.focusRequester(focusRequester)
                )
            }
            
            LaunchedEffect(isSearchVisible) {
                if (isSearchVisible) {
                    focusRequester.requestFocus()
                }
            }
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
                        onSearchQueryChange = onSearchQueryChange,
                        onCaptureClick = onCaptureClick
                    )
                    WindowWidthSizeClass.Medium -> HomeScreenMediumContent(
                        state = state,
                        modifier = Modifier.weight(1f),
                        onNavigateToItem = onNavigateToItem,
                        onSearchQueryChange = onSearchQueryChange,
                        onCaptureClick = onCaptureClick
                    )
                    else -> HomeScreenContent(
                        state = state,
                        modifier = Modifier.weight(1f),
                        isLargeScreen = false,
                        onNavigateToItem = onNavigateToItem
                    )
                }
            }
        }
    }
}

@Composable
fun RemindlyTopBar(onSearchToggle: () -> Unit = {}, isSearchActive: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 12.dp, top = 16.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "Remindly",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2D6A4F),
            letterSpacing = (-0.5).sp
        )
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
    onNavigateToItem: (String) -> Unit = {}
) {
    val scrollState = rememberScrollState()
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp)
    ) {
        GreetingSection(userName = "Victor", attentionCount = state.todayItems.size)
        
        Spacer(modifier = Modifier.height(24.dp))
        
        CalendarStrip(items = state.allItems)
        
        Spacer(modifier = Modifier.height(32.dp))
        
        SectionHeader("TODAY")
        if (state.todayItems.isNotEmpty()) {
            state.todayItems.forEach { item ->
                TaskItem(
                    title = item.title,
                    time = item.eventDate ?: "Today",
                    icon = Icons.Default.AccessTime,
                    iconBgColor = Color(0xFFEAF2EE),
                    tag = "Today",
                    tagColor = Color(0xFFFEF5E7),
                    tagTextColor = Color(0xFFD35400),
                    onClick = { onNavigateToItem(item.id) }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        } else {
            EmptySectionPlaceholder("No tasks for today. Stay productive!")
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        SectionHeader("LAST ANALYSED")
        if (state.lastAnalysed.isNotEmpty()) {
            AnalysedSection(state.lastAnalysed.take(3), onNavigateToItem = onNavigateToItem)
        } else {
            EmptySectionPlaceholder("Recent captures will appear here.")
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        SectionHeader("UPCOMING")
        if (state.upcomingItems.isNotEmpty()) {
            state.upcomingItems.take(3).forEach { item ->
                val deadline = item.deadline
                val daysLeft = if (deadline != null) ChronoUnit.DAYS.between(LocalDate.now(), LocalDate.parse(deadline)) else 0
                val icon = SourceIconProvider.getIconForSource(item.source)
                UpcomingTaskItem(
                    title = item.title,
                    subtitle = item.organization ?: item.source ?: "",
                    tag = "$daysLeft days left",
                    iconRes = (icon as? SourceIcon.Resource)?.resId,
                    icon = (icon as? SourceIcon.Vector)?.imageVector,
                    onClick = { onNavigateToItem(item.id) }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        } else {
            EmptySectionPlaceholder("No upcoming deadlines. You're ahead!")
        }
        
        Spacer(modifier = Modifier.height(100.dp))
    }
}

@Composable
fun EmptySectionPlaceholder(text: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFFF9FAF9),
        border = BorderStroke(1.dp, Color(0xFFEEEEEE))
    ) {
        Box(
            modifier = Modifier.padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray.copy(alpha = 0.7f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
fun HomeScreenLargeContent(
    state: HomeUiState.Success,
    modifier: Modifier = Modifier,
    onNavigateToItem: (String) -> Unit = {},
    onSearchQueryChange: (String) -> Unit = {},
    onCaptureClick: () -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 24.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            GreetingSection(userName = "Victor", attentionCount = state.todayItems.size, titleSize = 22.sp, titleColor = Color(0xFF111111))
            Spacer(modifier = Modifier.height(32.dp))
            MonthCalendar(
                items = state.allItems,
                dayFontSize = 15.sp,
                titleFontSize = 18.sp,
                titleColor = Color(0xFF111111),
                labelColor = Color(0xFF6B7C6E),
                dateColor = Color(0xFF111111)
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clickable { onCaptureClick() },
                color = Evergreen,
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("New Capture", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.width(48.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            LargeSearchBar(
                text = state.searchQuery,
                onValueChange = onSearchQueryChange
            )
            Spacer(modifier = Modifier.height(32.dp))

            SectionHeader("TODAY", fontSize = 12.sp, color = Color(0xFF6B7C6E))
            if (state.todayItems.isNotEmpty()) {
                state.todayItems.forEach { item ->
                    TaskItem(
                        title = item.title,
                        time = item.eventDate ?: "Today",
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
                    Spacer(modifier = Modifier.height(12.dp))
                }
            } else {
                EmptySectionPlaceholder("No tasks for today.")
            }

            Spacer(modifier = Modifier.height(20.dp))

            SectionHeader("LAST ANALYSED", fontSize = 12.sp, color = Color(0xFF6B7C6E))
            if (state.lastAnalysed.isNotEmpty()) {
                AnalysedSectionLarge(state.lastAnalysed, onNavigateToItem = onNavigateToItem)
            } else {
                EmptySectionPlaceholder("Recent captures will appear here.")
            }

            Spacer(modifier = Modifier.height(32.dp))

            SectionHeader("UPCOMING", fontSize = 12.sp, color = Color(0xFF6B7C6E))
            if (state.upcomingItems.isNotEmpty()) {
                state.upcomingItems.take(3).forEach { item ->
                    val deadline = item.deadline
                    val daysLeft = if (deadline != null) ChronoUnit.DAYS.between(LocalDate.now(), LocalDate.parse(deadline)) else 0
                    val icon = SourceIconProvider.getIconForSource(item.source)
                    UpcomingTaskItem(
                        title = item.title,
                        subtitle = item.organization ?: item.source ?: "",
                        tag = "$daysLeft days left",
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
    onSearchQueryChange: (String) -> Unit = {},
    onCaptureClick: () -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 24.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            MonthCalendar(items = state.allItems, showLegend = true, dayFontSize = 13.sp)
            Spacer(modifier = Modifier.weight(1f))
            
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clickable { onCaptureClick() },
                color = Evergreen,
                shape = RoundedCornerShape(14.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("Capture", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            GreetingSection(userName = "Victor", attentionCount = state.todayItems.size, titleSize = 20.sp, bodySize = 13.sp)
        }

        VerticalDivider(
            modifier = Modifier.padding(horizontal = 24.dp).fillMaxHeight(),
            color = Color(0xFFF0F0F0),
            thickness = 1.dp
        )

        Column(
            modifier = Modifier
                .weight(1.2f)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Remindly",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2D6A4F),
                    fontSize = 20.sp
                )
                IconButton(onClick = { /* Search */ }) {
                    Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.Black, modifier = Modifier.size(20.dp))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            LargeSearchBar(
                text = state.searchQuery,
                onValueChange = onSearchQueryChange
            )

            Spacer(modifier = Modifier.height(24.dp))

            SectionHeader("TODAY", fontSize = 10.sp)
            if (state.todayItems.isNotEmpty()) {
                state.todayItems.forEach { item ->
                    TaskItem(
                        title = item.title,
                        time = item.eventDate ?: "Today",
                        icon = Icons.Default.AccessTime,
                        iconBgColor = Color(0xFFEAF2EE),
                        tag = "Today",
                        tagColor = Color(0xFF2D6A4F),
                        tagTextColor = Color.White,
                        titleFontSize = 14.sp,
                        onClick = { onNavigateToItem(item.id) }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            } else {
                EmptySectionPlaceholder("No tasks for today.")
            }

            Spacer(modifier = Modifier.height(20.dp))

            SectionHeader("LAST ANALYSED", fontSize = 10.sp)
            if (state.lastAnalysed.isNotEmpty()) {
                AnalysedSectionMedium(state.lastAnalysed.take(3), onNavigateToItem = onNavigateToItem)
            } else {
                EmptySectionPlaceholder("No recent captures.")
            }

            Spacer(modifier = Modifier.height(32.dp))

            SectionHeader("UPCOMING", fontSize = 10.sp)
            if (state.upcomingItems.isNotEmpty()) {
                state.upcomingItems.take(3).forEach { item ->
                    val deadline = item.deadline
                    val daysLeft = if (deadline != null) ChronoUnit.DAYS.between(LocalDate.now(), LocalDate.parse(deadline)) else 0
                    val icon = SourceIconProvider.getIconForSource(item.source)
                    UpcomingTaskItem(
                        title = item.title,
                        subtitle = item.organization ?: item.source ?: "",
                        tag = "$daysLeft days left",
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
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun GreetingSection(userName: String, attentionCount: Int, titleSize: TextUnit = 22.sp, bodySize: TextUnit = 14.sp, titleColor: Color = Color.Black) {
    Column {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Good morning, $userName",
            style = MaterialTheme.typography.headlineSmall.copy(fontSize = titleSize),
            fontWeight = FontWeight.ExtraBold,
            color = titleColor
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            "You have $attentionCount things needing attention today.",
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = bodySize),
            color = Color.Gray,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun CalendarStrip(items: List<Item>) {
    val today = LocalDate.now()
    val days = (0..6).map { today.plusDays(it.toLong() - 2) }
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFFF9FAF9)
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
                
                val hasItemsOnDay = items.any { 
                    (it.deadline == date.toString() || it.eventDate == date.toString()) && 
                    (date >= today)
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(42.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isSelected) Color(0xFF2D6A4F) else Color.Transparent)
                        .padding(vertical = 10.dp)
                ) {
                    Text(
                        dayName,
                        fontSize = 11.sp,
                        color = if (isSelected) Color.White else Color.Gray,
                        fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        dayOfMonth,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.White else Color.Black
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .size(3.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) Color.White 
                                else if (hasItemsOnDay) Color(0xFF2D6A4F) 
                                else Color.Transparent
                            )
                    )
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String, fontSize: TextUnit = 11.sp, color: Color = Color.Gray) {
    Text(
        title.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = color,
        letterSpacing = 1.2.sp,
        modifier = Modifier.padding(bottom = 16.dp, top = 4.dp),
        fontSize = fontSize
    )
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
fun AnalysedSection(items: List<Item>, onNavigateToItem: (String) -> Unit = {}) {
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
                timeAgo = if (item.status == "PENDING") "Processing..." else "Analyzed ${DateUtils.getTimeAgo(item.createdAt)}",
                status = item.status,
                iconRes = (icon as? SourceIcon.Resource)?.resId,
                icon = (icon as? SourceIcon.Vector)?.imageVector,
                onClick = { onNavigateToItem(item.id) }
            )
        }
    }
}

@Composable
fun AnalysedSectionLarge(items: List<Item>, onNavigateToItem: (String) -> Unit = {}) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        items.take(3).forEach { item ->
            val icon = SourceIconProvider.getIconForSource(item.source)
            AnalysedCard(
                modifier = Modifier.weight(1f),
                title = item.title,
                category = item.category ?: "Capture",
                timeAgo = if (item.status == "PENDING") "Processing..." else DateUtils.getTimeAgo(item.createdAt),
                status = item.status,
                iconRes = (icon as? SourceIcon.Resource)?.resId,
                icon = (icon as? SourceIcon.Vector)?.imageVector,
                titleFontSize = 12.sp,
                titleColor = Color(0xFF111111),
                timeColor = Color(0xFF6B7C6E),
                badgeColor = Color(0xFF2D6A4F),
                onClick = { onNavigateToItem(item.id) }
            )
        }
    }
}

@Composable
fun AnalysedSectionMedium(items: List<Item>, onNavigateToItem: (String) -> Unit = {}) {
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
                timeAgo = if (item.status == "PENDING") "Processing..." else DateUtils.getTimeAgo(item.createdAt),
                status = item.status,
                iconRes = (icon as? SourceIcon.Resource)?.resId,
                icon = (icon as? SourceIcon.Vector)?.imageVector,
                titleFontSize = 12.sp,
                padding = 10.dp,
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
    titleFontSize: TextUnit = 13.sp,
    titleColor: Color = Color.Black,
    timeColor: Color = Color.Gray,
    badgeColor: Color = Color(0xFF2D6A4F),
    padding: Dp = 12.dp,
    onClick: () -> Unit = {}
) {
    val isAnalyzing = status == "PENDING"
    
    Surface(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFFF9FAF9)
    ) {
        Column(modifier = Modifier.padding(padding)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    if (iconRes != null) {
                        Icon(
                            painter = painterResource(id = iconRes),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color(0xFF2D6A4F)
                        )
                    } else if (icon != null) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color(0xFF2D6A4F)
                        )
                    }
                }
                
                Surface(
                    color = if (isAnalyzing) Color(0xFFFFF7E6) else Color(0xFFEAF2EE),
                    shape = CircleShape
                ) {
                    Text(
                        if (isAnalyzing) "Analyzing..." else "Analysed",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isAnalyzing) Color(0xFFD48806) else badgeColor
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                title,
                fontWeight = FontWeight.Bold,
                fontSize = titleFontSize,
                maxLines = 2,
                minLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = titleColor,
                lineHeight = (titleFontSize.value + 3).sp
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Surface(
                color = if (isAnalyzing) Color(0xFFF0F0F0) else Color(0xFF2D6A4F),
                shape = CircleShape
            ) {
                Text(
                    if (isAnalyzing) "Processing" else category,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isAnalyzing) Color.Gray else Color.White
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                timeAgo,
                style = MaterialTheme.typography.bodySmall,
                color = timeColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun LargeSearchBar(text: String, onValueChange: (String) -> Unit) {
    SearchField(
        placeholder = "Search captures...",
        text = text,
        onValueChange = onValueChange
    )
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
                        
                        val itemsOnDay = items.filter { it.deadline == date.toString() || it.eventDate == date.toString() }
                        val hasDot = itemsOnDay.isNotEmpty() && !isPast
                        val isUrgent = itemsOnDay.any { 
                            val deadline = it.deadline
                            deadline != null && ChronoUnit.DAYS.between(LocalDate.now(), LocalDate.parse(deadline)) <= 2
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

@Preview(showBackground = true, device = FOLDABLE)
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

private val PreviewHomeUiState = HomeUiState.Success(
    todayItems = listOf(
        Item(
            id = "1",
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
