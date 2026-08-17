package com.victorkirui.module_features.inbox

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.victorkirui.core.ui.theme.*
import com.victorkirui.core.ui.component.RemindlyNavigationRail
import com.victorkirui.core.ui.component.RemindlyBottomNavigation
import com.victorkirui.module_features.details.CategoryDetailContent
import com.victorkirui.core.R
import com.victorkirui.local.entity.Item
import com.victorkirui.module_features.capturing.SourceIcon
import com.victorkirui.module_features.capturing.SourceIconProvider
import org.koin.androidx.compose.koinViewModel
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Composable
fun InboxScreen(
    windowWidthSizeClass: WindowWidthSizeClass,
    onNavigateToHome: () -> Unit = {},
    onNavigateToReminders: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToCategory: (String) -> Unit = {},
    onNavigateToItem: (String) -> Unit = {},
    viewModel: InboxViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedCategoryId by viewModel.selectedCategoryId.collectAsState()

    InboxScreenContent(
        uiState = uiState,
        selectedCategoryId = selectedCategoryId,
        onCategorySelected = { categoryId ->
            viewModel.selectCategory(categoryId)
            if (windowWidthSizeClass == WindowWidthSizeClass.Compact) {
                onNavigateToCategory(categoryId)
            }
        },
        onNavigateToItem = onNavigateToItem,
        windowWidthSizeClass = windowWidthSizeClass,
        onNavigateToHome = onNavigateToHome,
        onNavigateToReminders = onNavigateToReminders,
        onNavigateToProfile = onNavigateToProfile,
        onDeleteItem = { itemId -> viewModel.deleteItem(itemId) }
    )
}

@Composable
fun InboxScreenContent(
    uiState: InboxUiState,
    selectedCategoryId: String?,
    onCategorySelected: (String) -> Unit,
    onNavigateToItem: (String) -> Unit,
    windowWidthSizeClass: WindowWidthSizeClass,
    onNavigateToHome: () -> Unit = {},
    onNavigateToReminders: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onDeleteItem: (String) -> Unit = {}
) {
    val isLargeScreen = windowWidthSizeClass == WindowWidthSizeClass.Expanded
    val isMediumScreen = windowWidthSizeClass == WindowWidthSizeClass.Medium

    Row(modifier = Modifier.fillMaxSize().background(Color.White)) {
        Scaffold(
            containerColor = Color.White
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues)) {
                when (val state = uiState) {
                    InboxUiState.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    is InboxUiState.Success -> {
                        if (isLargeScreen || isMediumScreen) {
                            LargeScreenInboxLayout(
                                categories = state.categories,
                                items = state.items,
                                selectedCategoryId = selectedCategoryId,
                                onCategorySelected = onCategorySelected,
                                onItemClick = onNavigateToItem,
                                onDeleteItem = onDeleteItem
                            )
                        } else {
                            SmallScreenInboxLayout(
                                categories = state.categories,
                                onCategoryClick = onCategorySelected
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SmallScreenInboxLayout(
    categories: List<InboxCategory>,
    onCategoryClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        Text(
            "Inbox",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        SearchField(placeholder = "Search your captures...")
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            "${categories.sumOf { it.count }} items across ${categories.size} categories",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        if (categories.isNotEmpty()) {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(categories) { category ->
                    CategoryItemCard(
                        category = category,
                        isSelected = false,
                        onClick = { onCategoryClick(category.id) }
                    )
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "No categories yet. Start capturing!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun LargeScreenInboxLayout(
    categories: List<InboxCategory>,
    items: List<Item>,
    selectedCategoryId: String?,
    onCategorySelected: (String) -> Unit,
    onItemClick: (String) -> Unit,
    onDeleteItem: (String) -> Unit = {}
) {
    Row(modifier = Modifier.fillMaxSize()) {
        // Master Pane (Category List)
        Column(
            modifier = Modifier
                .width(360.dp)
                .fillMaxHeight()
                .padding(horizontal = 24.dp, vertical = 32.dp)
        ) {
            Text(
                "Inbox",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            SearchField(placeholder = "Search your captures...")
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                "${categories.sumOf { it.count }} items across ${categories.size} categories",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            if (categories.isNotEmpty()) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(categories) { category ->
                        CategoryItemCard(
                            category = category,
                            isSelected = category.id == selectedCategoryId,
                            onClick = { onCategorySelected(category.id) }
                        )
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "No categories",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
            }
        }

        // Vertical Divider
        VerticalDivider(
            thickness = 1.dp,
            color = Color.LightGray.copy(alpha = 0.3f)
        )

        // Detail Pane (Items List)
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
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        if (selectedCategoryId == "PENDING_SYNC") "Pending Sync" else (selectedCategoryId ?: "Select a Category"),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
                
                IconButton(onClick = { /* Search within category */ }) {
                    Icon(Icons.Default.Search, contentDescription = "Search", modifier = Modifier.size(28.dp))
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            if (items.isNotEmpty()) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(items.chunked(2)) { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            rowItems.forEach { item ->
                                var showDeleteDialog by remember { mutableStateOf(false) }

                                if (showDeleteDialog) {
                                    AlertDialog(
                                        onDismissRequest = { showDeleteDialog = false },
                                        title = { Text("Delete Capture") },
                                        text = { Text("Are you sure you want to delete this capture? This action cannot be undone.") },
                                        confirmButton = {
                                            TextButton(onClick = { 
                                                onDeleteItem(item.id)
                                                showDeleteDialog = false 
                                            }) {
                                                Text("Delete", color = Color.Red)
                                            }
                                        },
                                        dismissButton = {
                                            TextButton(onClick = { showDeleteDialog = false }) {
                                                Text("Cancel")
                                            }
                                        }
                                    )
                                }

                                Box(modifier = Modifier.weight(1f)) {
                                    CaptureItemCard(
                                        item = item, 
                                        onClick = { onItemClick(item.id) }
                                    )
                                    // Add a long-press or a small delete icon
                                    IconButton(
                                        onClick = { showDeleteDialog = true },
                                        modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.DeleteOutline, 
                                            contentDescription = "Delete",
                                            tint = Color.Gray.copy(alpha = 0.5f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                            if (rowItems.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            } else if (selectedCategoryId != null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "No items in this category",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "Select a category to view items",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
fun ItemsGridRow(items: List<Item>, onItemClick: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items.forEach { item ->
            CaptureItemCard(item = item, onClick = { onItemClick(item.id) }, modifier = Modifier.weight(1f))
        }
        if (items.size == 1) {
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun CategoryItemCard(
    category: InboxCategory,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) Color.White else Color(0xFFF9FAF9),
        border = if (isSelected) BorderStroke(1.5.dp, Color(0xFF2D6A4F)) else null
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFEAF2EE)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when(category.iconType) {
                        "work" -> Icons.Default.WorkOutline
                        "school" -> Icons.Default.School
                        "event" -> Icons.Default.Event
                        "receipt" -> Icons.Default.ReceiptLong
                        "flight" -> Icons.Default.Flight
                        "sync" -> Icons.Default.Sync
                        else -> Icons.Default.Category
                    },
                    contentDescription = null,
                    tint = Color(0xFF2D6A4F),
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        category.id,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.Black
                    )
                    if (category.hasPendingSync) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = "Syncing",
                            tint = Color(0xFF2D6A4F),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                Text(
                    category.latestItemTitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    maxLines = 1
                )
            }
            
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF2D6A4F)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    category.count.toString(),
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color.LightGray,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun CaptureItemCard(
    item: Item,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(100.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFFF9FAF9)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Brand/Source Icon
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                val icon = SourceIconProvider.getIconForSource(item.source)
                
                if (icon is SourceIcon.Resource) {
                    Icon(
                        painter = painterResource(id = icon.resId),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = Color(0xFF2D6A4F)
                    )
                } else if (icon is SourceIcon.Vector) {
                    Icon(
                        imageVector = icon.imageVector,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = Color(0xFF2D6A4F)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        item.source ?: "Unknown",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )

                    if (item.status == "PENDING") {
                        Text(" · ", color = Color.Gray)
                        Text(
                            "Sync Pending",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF2D6A4F),
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Text(" · ", color = Color.Gray)
                    
                    val deadlineValue = item.deadline
                    val isDone = item.status == "DONE"
                    val deadlineText = if (isDone) {
                        "Marked as Done"
                    } else if (deadlineValue != null) {
                        val deadline = LocalDate.parse(deadlineValue)
                        val days = ChronoUnit.DAYS.between(LocalDate.now(), deadline)
                        when {
                            days == 0L -> "Deadline Today"
                            days == 1L -> "Deadline in 1 day"
                            days <= 7L -> "Deadline in $days days"
                            else -> "Deadline: ${deadline.dayOfMonth} ${deadline.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }}"
                        }
                    } else {
                        "Saved: 2 weeks ago"
                    }
                    
                    val isUrgent = !isDone && deadlineValue != null && ChronoUnit.DAYS.between(LocalDate.now(), LocalDate.parse(deadlineValue)) <= 2
                    
                    Text(
                        deadlineText,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isUrgent) Color(0xFFC0392B) else if (isDone) Color(0xFF2D6A4F) else Color.Gray,
                        fontWeight = if (isUrgent || isDone) FontWeight.Medium else FontWeight.Normal
                    )
                }
            }
            
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color.LightGray,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun SearchField(placeholder: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFFF4F6F4)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(placeholder, color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = Color(0xFFC0392B).copy(alpha = 0.7f),
        letterSpacing = 1.sp,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Preview(showBackground = true, widthDp = 1100, heightDp = 800)
@Composable
fun InboxLargeScreenPreview() {
    val futureDate = LocalDate.now().plusDays(2).toString()
    val soonDate = LocalDate.now().plusDays(5).toString()
    val upcomingDate = LocalDate.now().plusDays(15).toString()

    RemindlyTheme {
        InboxScreenContent(
            uiState = InboxUiState.Success(
                categories = listOf(
                    InboxCategory("Jobs", "Google Summer Internship", 12, iconType = "work"),
                    InboxCategory("Scholarships", "Chevening Scholarship", 6, iconType = "school"),
                    InboxCategory("Events", "AWS Summit London 2025", 4, iconType = "event"),
                    InboxCategory("Bills", "Electricity bill — March", 2, iconType = "receipt"),
                    InboxCategory("Travel", "Flight to Amsterdam", 1, iconType = "flight"),
                    InboxCategory("Other", "Team standup notes", 9, iconType = "category")
                ),
                items = listOf(
                    Item(id = "1", title = "Spotify Software Engineer — London", summary = null, category = "Jobs", deadline = futureDate, eventDate = null, source = "LinkedIn", createdAt = "", status = "Active"),
                    Item(id = "2", title = "Google Summer Internship 2025", summary = null, category = "Jobs", deadline = soonDate, eventDate = null, source = "LinkedIn", createdAt = "", status = "Active"),
                    Item(id = "3", title = "Meta University Program 2025", summary = null, category = "Jobs", deadline = upcomingDate, eventDate = null, source = "Gmail", createdAt = "", status = "Active"),
                    Item(id = "4", title = "Amazon SDE Internship", summary = null, category = "Jobs", deadline = upcomingDate, eventDate = null, source = "LinkedIn", createdAt = "", status = "Active"),
                    Item(id = "5", title = "React Native Developer — Remote", summary = null, category = "Jobs", deadline = null, eventDate = null, source = "WhatsApp", createdAt = "", status = "Active"),
                    Item(id = "6", title = "Junior iOS Developer — Berlin", summary = null, category = "Jobs", deadline = null, eventDate = null, source = "Gmail", createdAt = "", status = "Active")
                )
            ),
            selectedCategoryId = "Jobs",
            onCategorySelected = {},
            onNavigateToItem = {},
            windowWidthSizeClass = WindowWidthSizeClass.Expanded
        )
    }
}
