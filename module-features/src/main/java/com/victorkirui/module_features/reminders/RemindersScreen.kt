package com.victorkirui.module_features.reminders

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.Dialog
import com.victorkirui.core.ui.theme.*
import com.victorkirui.core.ui.component.RemindlyNavigationRail
import com.victorkirui.core.ui.component.RemindlyBottomNavigation
import com.victorkirui.core.R
import com.victorkirui.local.entity.ReminderWithItem
import com.victorkirui.local.entity.Item
import com.victorkirui.core.util.DateUtils
import org.koin.androidx.compose.koinViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.*
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@Composable
fun RemindersScreen(
    windowWidthSizeClass: WindowWidthSizeClass,
    initialSelectedDate: String? = null,
    onNavigateToHome: () -> Unit = {},
    onNavigateToInbox: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    viewModel: RemindersViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val currentMonth by viewModel.currentMonth.collectAsState()

    LaunchedEffect(initialSelectedDate) {
        if (initialSelectedDate != null) {
            val parsedDate = DateUtils.parseToLocalDate(initialSelectedDate)
            if (parsedDate != null) {
                viewModel.onDateSelected(parsedDate)
            }
        }
    }

    RemindersScreenContent(
        uiState = uiState,
        selectedDate = selectedDate,
        currentMonth = currentMonth,
        onDateSelected = viewModel::onDateSelected,
        onMonthChanged = viewModel::onMonthChanged,
        windowWidthSizeClass = windowWidthSizeClass,
        onNavigateToHome = onNavigateToHome,
        onNavigateToInbox = onNavigateToInbox,
        onNavigateToProfile = onNavigateToProfile
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersScreenContent(
    uiState: RemindersUiState,
    selectedDate: LocalDate,
    currentMonth: YearMonth,
    onDateSelected: (LocalDate) -> Unit,
    onMonthChanged: (YearMonth) -> Unit,
    windowWidthSizeClass: WindowWidthSizeClass,
    onNavigateToHome: () -> Unit = {},
    onNavigateToInbox: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {}
) {
    val isLargeScreen = windowWidthSizeClass == WindowWidthSizeClass.Expanded
    val isMediumScreen = windowWidthSizeClass == WindowWidthSizeClass.Medium

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Scaffold(
            topBar = {
                if (!isLargeScreen) {
                    TopAppBar(
                        title = { 
                            Text(
                                "Calendar",
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            ) 
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                    )
                }
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->
            when (val state = uiState) {
                RemindersUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is RemindersUiState.Success -> {
                    if (isLargeScreen) {
                        LargeScreenRemindersLayout(
                            state = state,
                            selectedDate = selectedDate,
                            currentMonth = currentMonth,
                            onDateSelected = onDateSelected,
                            onMonthChanged = onMonthChanged
                        )
                    } else {
                        RemindersMobileContent(
                            state = state,
                            selectedDate = selectedDate,
                            currentMonth = currentMonth,
                            onDateSelected = onDateSelected,
                            onMonthChanged = onMonthChanged,
                            modifier = Modifier.padding(innerPadding),
                            referenceDate = if (selectedDate.year == 2025) selectedDate else LocalDate.now()
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RemindersMobileContent(
    state: RemindersUiState.Success,
    selectedDate: LocalDate,
    currentMonth: YearMonth,
    onDateSelected: (LocalDate) -> Unit,
    onMonthChanged: (YearMonth) -> Unit,
    modifier: Modifier = Modifier,
    referenceDate: LocalDate = LocalDate.now()
) {
    var isCalendarExpanded by remember { mutableStateOf(false) }
    var displayedWeekStart by remember { 
        mutableStateOf(selectedDate.minusDays((selectedDate.dayOfWeek.value % 7).toLong())) 
    }

    val today = LocalDate.now()
    val weekStart = displayedWeekStart
    val weekEnd = weekStart.plusDays(6)
    val todayWeekStart = today.minusDays((today.dayOfWeek.value % 7).toLong())
    val weekOffset = ChronoUnit.WEEKS.between(todayWeekStart, weekStart)

    val weekSectionTitle = when (weekOffset) {
        0L -> "THIS WEEK"
        1L -> "NEXT WEEK"
        -1L -> "LAST WEEK"
        else -> "WEEK OF ${weekStart.format(DateTimeFormatter.ofPattern("MMM d"))}".uppercase()
    }

    val weekItems = state.allItems.filter { item ->
        val dateStr = item.deadline ?: item.eventDate
        val date = DateUtils.parseToLocalDate(dateStr)
        date != null && !date.isBefore(weekStart) && !date.isAfter(weekEnd)
    }

    val weekItemIds = remember(weekItems) { weekItems.map { it.id }.toSet() }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            CalendarView(
                currentMonth = currentMonth,
                selectedDate = selectedDate,
                onDateSelected = onDateSelected,
                onMonthChanged = onMonthChanged,
                onWeekStartChanged = { newWeekStart -> displayedWeekStart = newWeekStart },
                allItems = state.allItems,
                isExpanded = isCalendarExpanded,
                onToggleExpand = { isCalendarExpanded = !isCalendarExpanded }
            )
        }

        if (!isCalendarExpanded) {
            item {
                Text(
                    weekSectionTitle,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF2D6A4F),
                    letterSpacing = 1.2.sp,
                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                )
            }

            if (weekItems.isNotEmpty()) {
                items(weekItems) { item ->
                    val reminderWithItem = state.allReminders.find { it.reminder.itemId == item.id }
                    if (reminderWithItem != null) {
                        UpcomingReminderCard(reminderWithItem, referenceDate)
                    } else {
                        SimpleItemCard(item, referenceDate)
                    }
                }
            } else {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFF9FAF9)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.EventAvailable,
                                contentDescription = null,
                                tint = Color(0xFF2D6A4F),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "No events scheduled for this week. Enjoy your free time!",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            item {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    thickness = 1.dp,
                    color = Color.LightGray.copy(alpha = 0.2f)
                )
            }

            val groupTitles = listOf("Today", "Tomorrow", "This Week", "Later")
            groupTitles.forEach { title ->
                val itemsInSection = state.upcomingGrouped[title]?.filter { !weekItemIds.contains(it.id) }
                if (!itemsInSection.isNullOrEmpty()) {
                    item {
                        Text(
                            title.uppercase(),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.Black.copy(alpha = 0.8f),
                            letterSpacing = 1.2.sp,
                            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                        )
                    }
                    items(itemsInSection) { item ->
                        val reminderWithItem = state.allReminders.find { it.reminder.itemId == item.id }
                        if (reminderWithItem != null) {
                            UpcomingReminderCard(reminderWithItem, referenceDate)
                        } else {
                            SimpleItemCard(item, referenceDate)
                        }
                    }
                }
            }
        } else {
            if (state.itemsForSelectedDate.isNotEmpty()) {
                item {
                    Text(
                        "SCHEDULE FOR ${selectedDate.format(DateTimeFormatter.ofPattern("MMMM d"))}".uppercase(),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF2D6A4F),
                        letterSpacing = 1.2.sp,
                        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                    )
                }
                items(state.itemsForSelectedDate) { item ->
                    val reminderWithItem = state.allReminders.find { it.reminder.itemId == item.id }
                    if (reminderWithItem != null) {
                        UpcomingReminderCard(reminderWithItem, referenceDate)
                    } else {
                        SimpleItemCard(item, referenceDate)
                    }
                }
            }

            item {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    thickness = 1.dp,
                    color = Color.LightGray.copy(alpha = 0.2f)
                )
            }

            val groupTitles = listOf("Today", "Tomorrow", "This Week", "Later")
            groupTitles.forEach { title ->
                val itemsInSection = state.upcomingGrouped[title]
                if (!itemsInSection.isNullOrEmpty()) {
                    item {
                        Text(
                            title.uppercase(),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.Black.copy(alpha = 0.8f),
                            letterSpacing = 1.2.sp,
                            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                        )
                    }
                    items(itemsInSection) { item ->
                        val reminderWithItem = state.allReminders.find { it.reminder.itemId == item.id }
                        if (reminderWithItem != null) {
                            UpcomingReminderCard(reminderWithItem, referenceDate)
                        } else {
                            SimpleItemCard(item, referenceDate)
                        }
                    }
                }
            }
        }
        
        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun SimpleItemCard(item: com.victorkirui.local.entity.Item, referenceDate: LocalDate = LocalDate.now()) {
    val dateStr = item.deadline ?: item.eventDate
    val date = DateUtils.parseToLocalDate(dateStr)

    val monthName = date?.month?.getDisplayName(TextStyle.SHORT, Locale.getDefault())?.uppercase() ?: ""
    val dayOfMonth = date?.dayOfMonth?.toString() ?: ""
    val daysLeft = if (date != null) ChronoUnit.DAYS.between(referenceDate, date) else -1

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Date Badge
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(40.dp)
            ) {
                Text(
                    monthName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp
                )
                Text(
                    dayOfMonth,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 24.sp
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Vertical Divider
            VerticalDivider(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(vertical = 8.dp),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(6.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            item.category ?: "Capture",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            if (daysLeft >= 0) {
                Surface(
                    color = if (daysLeft <= 3) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (daysLeft == 0L) "Today" else "$daysLeft d",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun LargeScreenRemindersLayout(
    state: RemindersUiState.Success,
    selectedDate: LocalDate,
    currentMonth: YearMonth,
    onDateSelected: (LocalDate) -> Unit,
    onMonthChanged: (YearMonth) -> Unit
) {
    Row(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        // Master Pane (Calendar)
        Column(
            modifier = Modifier
                .fillMaxWidth(.37f)
                .fillMaxHeight()
                .padding(horizontal = 24.dp, vertical = 32.dp)
        ) {
            Text(
                "Calendar",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            CalendarView(
                currentMonth = currentMonth,
                selectedDate = selectedDate,
                onDateSelected = onDateSelected,
                onMonthChanged = onMonthChanged,
                allItems = state.allItems,
                isLargeScreen = true,
                isExpanded = true,
                showExpandButton = false
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Items for selected date section in large screen
            if (state.itemsForSelectedDate.isNotEmpty()) {
                Text(
                    "SCHEDULE FOR ${selectedDate.format(DateTimeFormatter.ofPattern("MMMM d"))}".uppercase(),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF2D6A4F),
                    letterSpacing = 1.2.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(state.itemsForSelectedDate) { item ->
                        val reminderWithItem = state.allReminders.find { it.reminder.itemId == item.id }
                        if (reminderWithItem != null) {
                            UpcomingReminderCard(reminderWithItem)
                        } else {
                            SimpleItemCard(item)
                        }
                    }
                }
            } else {
                // Legend
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF2D6A4F)))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Reminder", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Spacer(modifier = Modifier.width(16.dp))
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFFC0392B)))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Urgent", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }
            }
        }

        // Vertical Divider
        VerticalDivider(
            thickness = 1.dp,
            color = Color.LightGray.copy(alpha = 0.3f)
        )

        // Detail Pane (Grouped Upcoming List)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(horizontal = 32.dp, vertical = 32.dp)
        ) {
            Text(
                "Upcoming",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val groupTitles = listOf("Today", "Tomorrow", "This Week", "Later")
                groupTitles.forEach { title ->
                    val itemsInSection = state.upcomingGrouped[title]
                    if (!itemsInSection.isNullOrEmpty()) {
                        item {
                            Text(
                                title.uppercase(),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.Black.copy(alpha = 0.8f),
                                letterSpacing = 1.2.sp,
                                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                            )
                        }
                        items(itemsInSection) { item ->
                            val reminderWithItem = state.allReminders.find { it.reminder.itemId == item.id }
                            if (reminderWithItem != null) {
                                UpcomingReminderCard(reminderWithItem)
                            } else {
                                SimpleItemCard(item)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarView(
    currentMonth: YearMonth,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onMonthChanged: (YearMonth) -> Unit,
    onWeekStartChanged: (LocalDate) -> Unit = {},
    allItems: List<com.victorkirui.local.entity.Item> = emptyList(),
    isLargeScreen: Boolean = false,
    isExpanded: Boolean = false,
    showExpandButton: Boolean = true,
    onToggleExpand: () -> Unit = {}
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    )

    if (showDatePicker) {
        Dialog(onDismissRequest = { showDatePicker = false }) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                modifier = Modifier
                    .width(340.dp)
                    .wrapContentHeight()
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 16.dp)
                ) {
                    DatePicker(
                        state = datePickerState,
                        showModeToggle = false,
                        title = null,
                        headline = null,
                        colors = DatePickerDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            selectedDayContainerColor = MaterialTheme.colorScheme.primary,
                            selectedDayContentColor = MaterialTheme.colorScheme.onPrimary,
                            todayContentColor = MaterialTheme.colorScheme.primary,
                            todayDateBorderColor = MaterialTheme.colorScheme.primary,
                            weekdayContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            dayContentColor = MaterialTheme.colorScheme.onSurface,
                            navigationContentColor = MaterialTheme.colorScheme.onSurface,
                            selectedYearContainerColor = MaterialTheme.colorScheme.primary,
                            selectedYearContentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.scale(0.9f)
                    )
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showDatePicker = false }) {
                            Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(onClick = {
                            datePickerState.selectedDateMillis?.let { millis ->
                                val date = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                                onDateSelected(date)
                            }
                            showDatePicker = false
                        }) {
                            Text("Confirm", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }
        }
    }

    val itemsByDate = remember(allItems) {
        allItems.groupBy { DateUtils.parseToLocalDate(it.deadline ?: it.eventDate) }
    }

    val monthRows = remember(currentMonth) {
        val firstDayOfMonth = currentMonth.atDay(1)
        val firstDayOfWeek = firstDayOfMonth.dayOfWeek.value % 7
        val daysInMonth = (1..currentMonth.lengthOfMonth()).toList()
        val emptyDaysBeforeCount = firstDayOfWeek
        val prevMonth = currentMonth.minusMonths(1)
        val lastDayOfPrevMonth = prevMonth.atEndOfMonth().dayOfMonth
        val emptyDaysBefore = (0 until emptyDaysBeforeCount).map { lastDayOfPrevMonth - emptyDaysBeforeCount + it + 1 }
        val daysAfterCount = 42 - (emptyDaysBeforeCount + daysInMonth.size)
        val daysAfter = (1..daysAfterCount).toList()
        val allDays = emptyDaysBefore.map { it to -1 } + daysInMonth.map { it to 0 } + daysAfter.map { it to 1 }
        allDays.chunked(7)
    }

    val initialPage = 1000
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { 2000 })
    val today = remember { LocalDate.now() }
    val baseWeekStart = remember { today.minusDays((today.dayOfWeek.value % 7).toLong()) }

    LaunchedEffect(pagerState.currentPage) {
        if (!isExpanded) {
            val weekOffset = pagerState.currentPage - initialPage
            val currentWeekStart = baseWeekStart.plusWeeks(weekOffset.toLong())
            val targetMonth = YearMonth.from(currentWeekStart)
            if (targetMonth != currentMonth) {
                onMonthChanged(targetMonth)
            }
            onWeekStartChanged(currentWeekStart)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
    ) {
        // Month Selector Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { showDatePicker = true }
            ) {
                Text(
                    text = "${currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${currentMonth.year}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    fontSize = if (isLargeScreen) 22.sp else 19.sp,
                    color = Color.Black
                )
                Icon(
                    Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = Color.Black
                )
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {
                    if (!isExpanded) {
                        onDateSelected(selectedDate.minusWeeks(1))
                    } else {
                        onMonthChanged(currentMonth.minusMonths(1))
                    }
                }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Prev", tint = Color.Gray)
                }
                IconButton(onClick = {
                    if (!isExpanded) {
                        onDateSelected(selectedDate.plusWeeks(1))
                    } else {
                        onMonthChanged(currentMonth.plusMonths(1))
                    }
                }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Next", tint = Color.Gray)
                }
            }
        }

        if (!isExpanded) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth()
            ) { page ->
                val weekOffset = page - initialPage
                val weekStart = baseWeekStart.plusWeeks(weekOffset.toLong())
                val weekDays = (0..6).map { weekStart.plusDays(it.toLong()) }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFFF9FAF9),
                    border = BorderStroke(1.dp, Color(0xFFEEEEEE))
                ) {
                    Row(
                        modifier = Modifier
                            .padding(vertical = 10.dp, horizontal = 4.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        weekDays.forEach { date ->
                            val isSelected = date == selectedDate
                            val isToday = date == LocalDate.now()
                            val dayName = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
                            val dayOfMonth = date.dayOfMonth.toString()
                            
                            val itemsOnDate = itemsByDate[date] ?: emptyList()
                            val hasItems = itemsOnDate.isNotEmpty()

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .width(42.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (isSelected) Color(0xFF2D6A4F) else Color.Transparent)
                                    .clickable { onDateSelected(date) }
                                    .padding(vertical = 8.dp)
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
                                    color = if (isSelected) Color.White else if (isToday) Color(0xFF2D6A4F) else Color.Black
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .size(4.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when {
                                                hasItems && isSelected -> Color.White
                                                hasItems -> Color(0xFF2D6A4F)
                                                else -> Color.Transparent
                                            }
                                        )
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Day Labels
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                val days = listOf("S", "M", "T", "W", "T", "F", "S")
                days.forEach { day ->
                    Text(
                        text = day,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Days Grid
            val prevMonth = currentMonth.minusMonths(1)

            monthRows.forEach { week ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    week.forEach { (day, monthOffset) ->
                        val date = when (monthOffset) {
                            -1 -> prevMonth.atDay(day)
                            1 -> currentMonth.plusMonths(1).atDay(day)
                            else -> currentMonth.atDay(day)
                        }
                        
                        val isSelected = date == selectedDate
                        val isToday = date == LocalDate.now()
                        val itemsOnDate = itemsByDate[date] ?: emptyList()
                        val hasItems = itemsOnDate.isNotEmpty()

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(vertical = 1.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clickable { onDateSelected(date) },
                                shape = if (isSelected) RoundedCornerShape(10.dp) else CircleShape,
                                color = when {
                                    isSelected -> Color(0xFF2D6A4F)
                                    isToday && !isSelected -> Color(0xFF2D6A4F).copy(alpha = 0.08f)
                                    else -> Color.Transparent
                                },
                                border = if (isToday && !isSelected) BorderStroke(1.dp, Color(0xFF2D6A4F).copy(alpha = 0.5f)) else null
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = day.toString(),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = when {
                                            isSelected -> Color.White
                                            isToday -> Color(0xFF2D6A4F)
                                            monthOffset != 0 -> Color.LightGray
                                            else -> Color.Black
                                        },
                                        fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 14.sp
                                    )
                                    if (hasItems) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Box(
                                            modifier = Modifier
                                                .size(3.dp)
                                                .clip(CircleShape)
                                                .background(if (isSelected) Color.White else Color(0xFF2D6A4F))
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            MonthStrip(
                selectedMonth = currentMonth,
                onMonthSelected = onMonthChanged
            )
        }

        if (showExpandButton) {
            // Expand/Collapse handle centered BELOW the calendar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onToggleExpand() }
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        if (isExpanded) "Collapse Calendar" else "Expand Calendar",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF2D6A4F),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = Color(0xFF2D6A4F),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun MonthStrip(
    selectedMonth: YearMonth,
    onMonthSelected: (YearMonth) -> Unit
) {
    val months = (1..12).map { YearMonth.of(selectedMonth.year, it) }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = selectedMonth.monthValue - 1)

    LazyRow(
        state = listState,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(months) { month ->
            val isSelected = month == selectedMonth
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onMonthSelected(month) },
                color = if (isSelected) Evergreen.copy(alpha = 0.1f) else Color.Transparent,
                shape = RoundedCornerShape(20.dp),
                border = if (isSelected) BorderStroke(1.dp, Evergreen) else null
            ) {
                Text(
                    text = month.month.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSelected) Evergreen else MutedSage,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun UpcomingReminderCard(reminderWithItem: ReminderWithItem, referenceDate: LocalDate = LocalDate.now()) {
    val dateStr = reminderWithItem.item.deadline ?: reminderWithItem.item.eventDate ?: reminderWithItem.reminder.reminderDateTime
    val date = DateUtils.parseToLocalDate(dateStr) ?: LocalDate.now()
    val monthName = date.month.getDisplayName(TextStyle.SHORT, Locale.getDefault()).uppercase()
    val dayOfMonth = date.dayOfMonth
    
    val daysLeft = ChronoUnit.DAYS.between(referenceDate, date)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Date Badge
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(36.dp)
            ) {
                Text(
                    monthName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 9.sp
                )
                Text(
                    dayOfMonth.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 20.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Vertical Divider
            VerticalDivider(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(vertical = 4.dp),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    reminderWithItem.item.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            reminderWithItem.item.category ?: "Other",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            if (daysLeft >= 0) {
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.error,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = if (daysLeft == 0L) "Today" else "$daysLeft ${if (daysLeft == 1L) "day" else "days"} left",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onError,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

@Composable
fun LargeReminderCard(reminderWithItem: ReminderWithItem) {
    val dateStr = reminderWithItem.item.deadline ?: reminderWithItem.item.eventDate ?: reminderWithItem.reminder.reminderDateTime
    val date = DateUtils.parseToLocalDate(dateStr) ?: LocalDate.now()
    val monthName = date.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())
    val dayOfMonth = date.dayOfMonth
    val daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), date)
    val isUrgent = daysLeft <= 3

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = dayOfMonth.toString(),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 28.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = monthName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                if (daysLeft >= 0) {
                    Surface(
                        color = if (isUrgent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = if (daysLeft == 0L) "Today" else "$daysLeft ${if (daysLeft == 1L) "day" else "days"} left",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isUrgent) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = reminderWithItem.item.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                fontSize = 24.sp
            )

            Spacer(modifier = Modifier.height(64.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        reminderWithItem.item.category ?: "Other",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 20.sp
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Mobile Screen")
@Composable
fun RemindersMobilePreview() {
    val sampleItem1 = com.victorkirui.local.entity.Item(
        id = "1", userId = "sample", title = "Interview — Spotify London", summary = null, category = "Job",
        deadline = "2025-06-24", eventDate = null, source = "LinkedIn", createdAt = "", status = ""
    )
    val sampleReminder1 = com.victorkirui.local.entity.Reminder(
        itemId = "1", reminderDateTime = "2025-06-24T08:00:00", type = "DAILY"
    )

    val sampleItem2 = com.victorkirui.local.entity.Item(
        id = "2", userId = "sample", title = "Scholarship Application Deadline", summary = null, category = "Scholarship",
        deadline = "2025-06-25", eventDate = null, source = "Gmail", createdAt = "", status = ""
    )
    val sampleReminder2 = com.victorkirui.local.entity.Reminder(
        itemId = "2", reminderDateTime = "2025-06-25T08:00:00", type = "DAILY"
    )

    val allReminders = listOf(
        ReminderWithItem(sampleReminder1, sampleItem1),
        ReminderWithItem(sampleReminder2, sampleItem2)
    )

    RemindlyTheme {
        RemindersScreenContent(
            uiState = RemindersUiState.Success(
                allReminders = allReminders,
                allItems = allReminders.map { it.item },
                selectedDate = LocalDate.of(2025, 6, 23),
                currentMonth = YearMonth.of(2025, 6),
                itemsForSelectedDate = emptyList(),
                upcomingGrouped = emptyMap()
            ),
            selectedDate = LocalDate.of(2025, 6, 23),
            currentMonth = YearMonth.of(2025, 6),
            onDateSelected = {},
            onMonthChanged = {},
            windowWidthSizeClass = WindowWidthSizeClass.Compact
        )
    }
}

@Preview(showBackground = true, name = "Large Screen", device = "spec:width=1920dp,height=1080dp,dpi=160")
@Composable
fun RemindersLargeScreenPreview() {
    val sampleItem1 = com.victorkirui.local.entity.Item(
        id = "1", userId = "sample", title = "Interview — Spotify London", summary = null, category = "Job",
        deadline = "2025-06-24", eventDate = null, source = "LinkedIn", createdAt = "", status = ""
    )
    val sampleReminder1 = com.victorkirui.local.entity.Reminder(
        itemId = "1", reminderDateTime = "2025-06-24T08:00:00", type = "DAILY"
    )
    
    val sampleItem2 = com.victorkirui.local.entity.Item(
        id = "2", userId = "sample", title = "Scholarship Application Deadline", summary = null, category = "Scholarship",
        deadline = "2025-06-25", eventDate = null, source = "Gmail", createdAt = "", status = ""
    )
    val sampleReminder2 = com.victorkirui.local.entity.Reminder(
        itemId = "2", reminderDateTime = "2025-06-25T08:00:00", type = "DAILY"
    )

    val sampleItem3 = com.victorkirui.local.entity.Item(
        id = "3", userId = "sample", title = "Google Internship Deadline", summary = null, category = "Job",
        deadline = "2025-06-26", eventDate = null, source = "LinkedIn", createdAt = "", status = ""
    )
    val sampleReminder3 = com.victorkirui.local.entity.Reminder(
        itemId = "3", reminderDateTime = "2025-06-26T08:00:00", type = "DAILY"
    )

    val sampleItem4 = com.victorkirui.local.entity.Item(
        id = "4", userId = "sample", title = "AI Hackathon — DevCircle", summary = null, category = "Event",
        deadline = "2025-07-01", eventDate = null, source = "WhatsApp", createdAt = "", status = ""
    )
    val sampleReminder4 = com.victorkirui.local.entity.Reminder(
        itemId = "4", reminderDateTime = "2025-07-01T08:00:00", type = "DAILY"
    )

    val sampleItem5 = com.victorkirui.local.entity.Item(
        id = "5", userId = "sample", title = "Flight to Amsterdam", summary = null, category = "Travel",
        deadline = "2025-07-12", eventDate = null, source = "Gmail", createdAt = "", status = ""
    )
    val sampleReminder5 = com.victorkirui.local.entity.Reminder(
        itemId = "5", reminderDateTime = "2025-07-12T08:00:00", type = "DAILY"
    )

    val allReminders = listOf(
        ReminderWithItem(sampleReminder1, sampleItem1),
        ReminderWithItem(sampleReminder2, sampleItem2),
        ReminderWithItem(sampleReminder3, sampleItem3),
        ReminderWithItem(sampleReminder4, sampleItem4),
        ReminderWithItem(sampleReminder5, sampleItem5)
    )

    RemindlyTheme {
        CompositionLocalProvider(androidx.compose.ui.platform.LocalContext provides androidx.compose.ui.platform.LocalContext.current) {
            RemindersScreenContent(
                uiState = RemindersUiState.Success(
                    allReminders = allReminders,
                    allItems = allReminders.map { it.item },
                    selectedDate = LocalDate.of(2025, 6, 23),
                    currentMonth = YearMonth.of(2025, 6),
                    itemsForSelectedDate = emptyList(),
                    upcomingGrouped = emptyMap()
                ),
                selectedDate = LocalDate.of(2025, 6, 23),
                currentMonth = YearMonth.of(2025, 6),
                onDateSelected = {},
                onMonthChanged = {},
                windowWidthSizeClass = WindowWidthSizeClass.Expanded
            )
        }
    }
}


