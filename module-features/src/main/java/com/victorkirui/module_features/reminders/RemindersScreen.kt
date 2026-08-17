package com.victorkirui.module_features.reminders

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.victorkirui.core.ui.theme.*
import com.victorkirui.core.ui.component.RemindlyNavigationRail
import com.victorkirui.core.ui.component.RemindlyBottomNavigation
import com.victorkirui.core.R
import com.victorkirui.local.entity.ReminderWithItem
import org.koin.androidx.compose.koinViewModel
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.*
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@Composable
fun RemindersScreen(
    windowWidthSizeClass: WindowWidthSizeClass,
    onNavigateToHome: () -> Unit = {},
    onNavigateToInbox: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    viewModel: RemindersViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val currentMonth by viewModel.currentMonth.collectAsState()

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

    Row(modifier = Modifier.fillMaxSize().background(Color.White)) {
        if (isLargeScreen) {
            RemindlyNavigationRail(
                currentScreen = "Reminders",
                onHomeClick = onNavigateToHome,
                onInboxClick = onNavigateToInbox,
                onRemindersClick = { /* Already here */ },
                onProfileClick = onNavigateToProfile
            )
        }

        Scaffold(
            topBar = {
                if (!isLargeScreen) {
                    TopAppBar(
                        title = { 
                            Text(
                                "Reminders",
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp,
                                color = Color.Black
                            ) 
                        },
                        actions = {
                            IconButton(onClick = { /* Search */ }) {
                                Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.Black)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                    )
                }
            },
            containerColor = Color.White
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
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            CalendarView(
                currentMonth = currentMonth,
                selectedDate = selectedDate,
                onDateSelected = onDateSelected,
                onMonthChanged = onMonthChanged,
                reminders = state.allReminders
            )
        }

        item {
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                thickness = 0.5.dp,
                color = Color.LightGray.copy(alpha = 0.3f)
            )
        }

        item {
            Text(
                "UPCOMING",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color.Gray,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
            )
        }

        items(state.upcomingReminders) { reminderWithItem ->
            UpcomingReminderCard(reminderWithItem, referenceDate)
        }
        
        item {
            Spacer(modifier = Modifier.height(16.dp))
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
    Row(modifier = Modifier.fillMaxSize()) {
        // Master Pane (Calendar)
        Column(
            modifier = Modifier
                .fillMaxWidth(.37f)
                .fillMaxHeight()
                .padding(horizontal = 24.dp, vertical = 32.dp)
        ) {
            Text(
                "Reminders",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            CalendarView(
                currentMonth = currentMonth,
                selectedDate = selectedDate,
                onDateSelected = onDateSelected,
                onMonthChanged = onMonthChanged,
                reminders = state.allReminders,
                isLargeScreen = true
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
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

        // Vertical Divider
        VerticalDivider(
            thickness = 1.dp,
            color = Color.LightGray.copy(alpha = 0.3f)
        )

        // Detail Pane (Upcoming List)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(horizontal = 32.dp, vertical = 32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Upcoming",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Text(
                    "${state.upcomingReminders.size} reminders",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(state.upcomingReminders) { reminderWithItem ->
                    LargeReminderCard(reminderWithItem)
                }
            }
        }
    }
}

@Composable
fun CalendarView(
    currentMonth: YearMonth,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onMonthChanged: (YearMonth) -> Unit,
    reminders: List<ReminderWithItem>,
    isLargeScreen: Boolean = false
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Month Selector
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 40.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { onMonthChanged(currentMonth.minusMonths(1)) }) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Month")
            }
            Text(
                text = "${currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${currentMonth.year}",
                fontWeight = FontWeight.Bold,
                fontSize = if (isLargeScreen) 18.sp else 16.sp,
                modifier = Modifier.width(150.dp),
                textAlign = TextAlign.Center
            )
            IconButton(onClick = { onMonthChanged(currentMonth.plusMonths(1)) }) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Next Month")
            }
        }

        Spacer(modifier = Modifier.height(if (isLargeScreen) 16.dp else 8.dp))

        // Day Names
        Row(modifier = Modifier.fillMaxWidth()) {
            val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
            days.forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    fontSize = 11.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(if (isLargeScreen) 8.dp else 4.dp))

        // Days Grid
        val firstDayOfMonth = currentMonth.atDay(1)
        val lastDayOfMonth = currentMonth.atEndOfMonth()
        val firstDayOfWeek = firstDayOfMonth.dayOfWeek.value // 1 (Mon) to 7 (Sun)
        
        val daysInMonth = (1..currentMonth.lengthOfMonth()).toList()
        val emptyDaysBeforeCount = firstDayOfWeek - 1
        val prevMonth = currentMonth.minusMonths(1)
        val lastDayOfPrevMonth = prevMonth.atEndOfMonth().dayOfMonth
        val emptyDaysBefore = (0 until emptyDaysBeforeCount).map { lastDayOfPrevMonth - emptyDaysBeforeCount + it + 1 }
        
        val daysAfterCount = 42 - (emptyDaysBeforeCount + daysInMonth.size)
        val daysAfter = (1..daysAfterCount).toList()

        val allDays = emptyDaysBefore.map { it to -1 } + daysInMonth.map { it to 0 } + daysAfter.map { it to 1 }
        
        val rows = allDays.chunked(7)
        
        rows.forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { (day, monthOffset) ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(if (isLargeScreen) 1.4f else 1.8f)
                            .padding(vertical = 1.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val date = when (monthOffset) {
                            -1 -> prevMonth.atDay(day)
                            1 -> currentMonth.plusMonths(1).atDay(day)
                            else -> currentMonth.atDay(day)
                        }
                        
                        val isSelected = date == selectedDate
                        val hasReminders = reminders.any { 
                            LocalDate.parse(it.reminder.reminderDateTime.substring(0, 10)) == date 
                        }
                        val isUrgent = reminders.any {
                            val rDate = LocalDate.parse(it.reminder.reminderDateTime.substring(0, 10))
                            rDate == date && ChronoUnit.DAYS.between(LocalDate.now(), rDate) <= 3
                        }

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) Color(0xFF2D6A4F) else Color.Transparent)
                                .clickable { onDateSelected(date) }
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = day.toString(),
                                    color = if (isSelected) Color.White else if (monthOffset != 0) Color.LightGray else Color.Black,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = if(isLargeScreen) 20.sp else 14.sp
                                )
                                if (hasReminders && !isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(3.dp)
                                            .clip(CircleShape)
                                            .background(if (isUrgent) Color(0xFFC0392B) else Color(0xFF2D6A4F))
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UpcomingReminderCard(reminderWithItem: ReminderWithItem, referenceDate: LocalDate = LocalDate.now()) {
    val date = LocalDate.parse(reminderWithItem.reminder.reminderDateTime.substring(0, 10))
    val monthName = date.month.getDisplayName(TextStyle.SHORT, Locale.getDefault()).uppercase()
    val dayOfMonth = date.dayOfMonth
    
    val daysLeft = ChronoUnit.DAYS.between(referenceDate, date)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FAF9))
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
                    color = Color.Gray,
                    fontSize = 10.sp
                )
                Text(
                    dayOfMonth.toString(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2D6A4F),
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
                color = Color.LightGray.copy(alpha = 0.3f)
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    reminderWithItem.item.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    color = Color.Black
                )
                
                Spacer(modifier = Modifier.height(6.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = Color(0xFFEAF2EE),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            reminderWithItem.item.category ?: "Other",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF2D6A4F),
                            fontWeight = FontWeight.Medium
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))

                    val sourceIcon = when (reminderWithItem.item.source?.lowercase()) {
                        "linkedin" -> R.drawable.ic_linkedin
                        "gmail" -> R.drawable.ic_gmail
                        else -> null
                    }
                    
                    if (sourceIcon != null) {
                        Icon(
                            painter = painterResource(id = sourceIcon),
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = Color.Unspecified
                        )
                    } else {
                        Icon(
                            Icons.Default.Link,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = Color.Gray
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "from ${reminderWithItem.item.source ?: "Unknown"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }

            if (daysLeft >= 0) {
                Surface(
                    color = Color(0xFFC0392B),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (daysLeft == 0L) "Today" else "$daysLeft ${if (daysLeft == 1L) "day" else "days"} left",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun LargeReminderCard(reminderWithItem: ReminderWithItem) {
    val date = LocalDate.parse(reminderWithItem.reminder.reminderDateTime.substring(0, 10))
    val monthName = date.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())
    val dayOfMonth = date.dayOfMonth
    val daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), date)
    val isUrgent = daysLeft <= 3

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FAF9))
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
                        color = Color(0xFF2D6A4F),
                        fontSize = 28.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = monthName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                if (daysLeft >= 0) {
                    Surface(
                        color = if (isUrgent) Color(0xFFC0392B) else Color(0xFFEAF2EE),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = if (daysLeft == 0L) "Today" else "$daysLeft ${if (daysLeft == 1L) "day" else "days"} left",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isUrgent) Color.White else Color(0xFF2D6A4F),
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
                color = Color.Black,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                fontSize = 24.sp
            )

            Spacer(modifier = Modifier.height(64.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = Color(0xFF2D6A4F),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        reminderWithItem.item.category ?: "Other",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 20.sp
                    )
                }

                val sourceIcon = when (reminderWithItem.item.source?.lowercase()) {
                    "linkedin" -> R.drawable.ic_linkedin
                    "gmail" -> R.drawable.ic_gmail
                    "whatsapp" -> Icons.Default.ChatBubbleOutline
                    else -> Icons.Default.Link
                }

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    if (sourceIcon is Int) {
                        Icon(
                            painter = painterResource(id = sourceIcon),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = Color.Unspecified
                        )
                    } else {
                        Icon(
                            imageVector = sourceIcon as ImageVector,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = Color.Gray
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Mobile Screen")
@Composable
fun RemindersMobilePreview() {
    val sampleItem1 = com.victorkirui.local.entity.Item(
        id = "1", title = "Interview — Spotify London", summary = null, category = "Job",
        deadline = "2025-06-24", eventDate = null, source = "LinkedIn", createdAt = "", status = ""
    )
    val sampleReminder1 = com.victorkirui.local.entity.Reminder(
        itemId = "1", reminderDateTime = "2025-06-24T08:00:00", type = "DAILY"
    )

    val sampleItem2 = com.victorkirui.local.entity.Item(
        id = "2", title = "Scholarship Application Deadline", summary = null, category = "Scholarship",
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
                selectedDate = LocalDate.of(2025, 6, 23),
                currentMonth = YearMonth.of(2025, 6),
                upcomingReminders = allReminders
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
        id = "1", title = "Interview — Spotify London", summary = null, category = "Job",
        deadline = "2025-06-24", eventDate = null, source = "LinkedIn", createdAt = "", status = ""
    )
    val sampleReminder1 = com.victorkirui.local.entity.Reminder(
        itemId = "1", reminderDateTime = "2025-06-24T08:00:00", type = "DAILY"
    )
    
    val sampleItem2 = com.victorkirui.local.entity.Item(
        id = "2", title = "Scholarship Application Deadline", summary = null, category = "Scholarship",
        deadline = "2025-06-25", eventDate = null, source = "Gmail", createdAt = "", status = ""
    )
    val sampleReminder2 = com.victorkirui.local.entity.Reminder(
        itemId = "2", reminderDateTime = "2025-06-25T08:00:00", type = "DAILY"
    )

    val sampleItem3 = com.victorkirui.local.entity.Item(
        id = "3", title = "Google Internship Deadline", summary = null, category = "Job",
        deadline = "2025-06-26", eventDate = null, source = "LinkedIn", createdAt = "", status = ""
    )
    val sampleReminder3 = com.victorkirui.local.entity.Reminder(
        itemId = "3", reminderDateTime = "2025-06-26T08:00:00", type = "DAILY"
    )

    val sampleItem4 = com.victorkirui.local.entity.Item(
        id = "4", title = "AI Hackathon — DevCircle", summary = null, category = "Event",
        deadline = "2025-07-01", eventDate = null, source = "WhatsApp", createdAt = "", status = ""
    )
    val sampleReminder4 = com.victorkirui.local.entity.Reminder(
        itemId = "4", reminderDateTime = "2025-07-01T08:00:00", type = "DAILY"
    )

    val sampleItem5 = com.victorkirui.local.entity.Item(
        id = "5", title = "Flight to Amsterdam", summary = null, category = "Travel",
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
                    selectedDate = LocalDate.of(2025, 6, 23),
                    currentMonth = YearMonth.of(2025, 6),
                    upcomingReminders = allReminders
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


