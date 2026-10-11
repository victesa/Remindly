package com.victorkirui.module_features.inbox

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.text.style.TextOverflow
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
import com.victorkirui.core.util.DateUtils
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

    InboxScreenContent(
        uiState = uiState,
        onCategorySelected = { categoryId ->
            viewModel.selectCategory(categoryId)
            if (windowWidthSizeClass != WindowWidthSizeClass.Expanded) {
                val encodedCategory = java.net.URLEncoder.encode(categoryId, "UTF-8")
                onNavigateToCategory(encodedCategory)
            }
        },
        onDeleteCategories = { categoryIds ->
            viewModel.deleteCategories(categoryIds)
        },
        onNavigateToItem = onNavigateToItem,
        windowWidthSizeClass = windowWidthSizeClass,
        onNavigateToHome = onNavigateToHome,
        onNavigateToReminders = onNavigateToReminders,
        onNavigateToProfile = onNavigateToProfile,
        onDeleteItem = { itemId -> viewModel.deleteItem(itemId) },
        onDeleteItems = { itemIds -> viewModel.deleteItems(itemIds) },
        onMarkItemsAsDone = { itemIds -> viewModel.markItemsAsDone(itemIds) },
        searchQuery = (uiState as? InboxUiState.Success)?.searchQuery ?: "",
        onSearchQueryChange = { query -> viewModel.onSearchQueryChange(query) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboxScreenContent(
    uiState: InboxUiState,
    onCategorySelected: (String) -> Unit,
    onDeleteCategories: (Set<String>) -> Unit,
    onNavigateToItem: (String) -> Unit,
    windowWidthSizeClass: WindowWidthSizeClass,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onNavigateToHome: () -> Unit = {},
    onNavigateToReminders: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onDeleteItem: (String) -> Unit = {},
    onDeleteItems: (Set<String>) -> Unit = {},
    onMarkItemsAsDone: (Set<String>) -> Unit = {}
) {
    val isLargeScreen = windowWidthSizeClass == WindowWidthSizeClass.Expanded
    val isMediumScreen = windowWidthSizeClass == WindowWidthSizeClass.Medium

    var selectedCategories by remember { mutableStateOf(setOf<String>()) }
    val isSelectionMode = selectedCategories.isNotEmpty()

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Scaffold(
            topBar = {
                if (isSelectionMode) {
                    TopAppBar(
                        title = {
                            Text(
                                "${selectedCategories.size} selected",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { selectedCategories = emptySet() }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear selection", tint = Color.Black)
                            }
                        },
                        actions = {
                            val state = uiState as? InboxUiState.Success
                            if (state != null) {
                                val allVisibleIds = state.categories.map { it.id }.toSet()
                                val isAllSelected = selectedCategories.size == allVisibleIds.size
                                
                                FilledTonalButton(
                                    onClick = {
                                        selectedCategories = if (isAllSelected) emptySet() else allVisibleIds
                                    },
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = Color(0xFFEAF2EE),
                                        contentColor = Color(0xFF2D6A4F)
                                    )
                                ) {
                                    Text(
                                        if (isAllSelected) "Deselect All" else "Select All",
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(4.dp))

                            var showConfirmDelete by remember { mutableStateOf(false) }
                            if (showConfirmDelete) {
                                AlertDialog(
                                    onDismissRequest = { showConfirmDelete = false },
                                    title = { Text("Delete Categories") },
                                    text = { Text("Delete all items in the selected ${selectedCategories.size} categories? This cannot be undone.") },
                                    confirmButton = {
                                        TextButton(onClick = {
                                            onDeleteCategories(selectedCategories)
                                            selectedCategories = emptySet()
                                            showConfirmDelete = false
                                        }) {
                                            Text("Delete", color = Color.Red)
                                        }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = { showConfirmDelete = false }) {
                                            Text("Cancel")
                                        }
                                    }
                                )
                            }
                            IconButton(onClick = { showConfirmDelete = true }) {
                                Icon(
                                    Icons.Default.Delete, 
                                    contentDescription = "Delete selected",
                                    tint = Color(0xFFC0392B)
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.White,
                            titleContentColor = Color.Black,
                            navigationIconContentColor = Color.Black,
                            actionIconContentColor = Color.Black
                        )
                    )
                }
            },
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
                        if (isLargeScreen) {
                            LargeScreenInboxLayout(
                                categories = state.categories,
                                items = state.items,
                                selectedCategoryId = state.selectedCategoryId,
                                selectedCategories = selectedCategories,
                                searchQuery = searchQuery,
                                onSearchQueryChange = onSearchQueryChange,
                                onCategorySelected = { id ->
                                    if (isSelectionMode) {
                                        selectedCategories = if (selectedCategories.contains(id)) {
                                            selectedCategories - id
                                        } else {
                                            selectedCategories + id
                                        }
                                    } else {
                                        onCategorySelected(id)
                                    }
                                },
                                onCategoryLongClick = { id ->
                                    if (!isSelectionMode) {
                                        selectedCategories = setOf(id)
                                    }
                                },
                                onItemClick = onNavigateToItem,
                                onDeleteItem = onDeleteItem,
                                onDeleteItems = onDeleteItems,
                                onMarkItemsAsDone = onMarkItemsAsDone,
                                isMediumScreen = isMediumScreen
                            )
                        } else {
                            SmallScreenInboxLayout(
                                categories = state.categories,
                                selectedCategories = selectedCategories,
                                searchQuery = searchQuery,
                                onSearchQueryChange = onSearchQueryChange,
                                onCategoryClick = { id ->
                                    if (isSelectionMode) {
                                        selectedCategories = if (selectedCategories.contains(id)) {
                                            selectedCategories - id
                                        } else {
                                            selectedCategories + id
                                        }
                                    } else {
                                        onCategorySelected(id)
                                    }
                                },
                                onCategoryLongClick = { id ->
                                    if (!isSelectionMode) {
                                        selectedCategories = setOf(id)
                                    }
                                }
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
    selectedCategories: Set<String>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onCategoryClick: (String) -> Unit,
    onCategoryLongClick: (String) -> Unit
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
        
        SearchField(
            placeholder = "Search your captures...",
            text = searchQuery,
            onValueChange = onSearchQueryChange
        )
        
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
                    val isMultiSelected = selectedCategories.contains(category.id)
                    CategoryItemCard(
                        category = category,
                        isSelected = isMultiSelected,
                        isMultiSelected = isMultiSelected,
                        onClick = { onCategoryClick(category.id) },
                        onLongClick = { onCategoryLongClick(category.id) }
                    )
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                InboxEmptyCard(
                    title = "No categories yet",
                    subtitle = "Captures and notes will appear here organized by category."
                )
            }
        }
    }
}

@Composable
fun InboxEmptyCard(
    title: String = "No captures yet",
    subtitle: String = "Captured notes, documents, and web links will appear here organized by category."
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color(0xFFEAF2EE)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Inbox,
                contentDescription = null,
                tint = Color(0xFF2D6A4F),
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
fun LargeScreenInboxLayout(
    categories: List<InboxCategory>,
    items: List<Item>,
    selectedCategoryId: String?,
    selectedCategories: Set<String>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onCategorySelected: (String) -> Unit,
    onCategoryLongClick: (String) -> Unit,
    onItemClick: (String) -> Unit,
    onDeleteItem: (String) -> Unit = {},
    onDeleteItems: (Set<String>) -> Unit = {},
    onMarkItemsAsDone: (Set<String>) -> Unit = {},
    isMediumScreen: Boolean = false
) {
    var selectedItemIds by remember(selectedCategoryId) { mutableStateOf(setOf<String>()) }
    val isItemSelectionMode = selectedItemIds.isNotEmpty()
    var showConfirmDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(categories, selectedCategoryId) {
        if (selectedCategoryId == null && categories.isNotEmpty()) {
            onCategorySelected(categories.first().id)
        }
    }

    if (showConfirmDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDeleteDialog = false },
            title = { Text("Delete ${selectedItemIds.size} captures?") },
            text = { Text("Selected captures will be permanently deleted. This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteItems(selectedItemIds)
                    selectedItemIds = emptySet()
                    showConfirmDeleteDialog = false
                }) {
                    Text("Delete", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Row(modifier = Modifier.fillMaxSize()) {
        // Master Sidebar (Category List)
        Column(
            modifier = Modifier
                .fillMaxWidth(if (isMediumScreen) 0.36f else 0.32f)
                .fillMaxHeight()
                .background(Color(0xFFF7F8F7))
                .padding(horizontal = 16.dp, vertical = 24.dp)
        ) {
            Text(
                "Inbox",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            SearchField(
                placeholder = "Search captures...",
                text = searchQuery,
                onValueChange = onSearchQueryChange
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                "${categories.sumOf { it.count }} items across ${categories.size} categories",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (categories.isNotEmpty()) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(categories) { category ->
                        val isMultiSelected = selectedCategories.contains(category.id)
                        val isViewing = category.id == selectedCategoryId
                        CategoryItemCard(
                            category = category,
                            isSelected = isViewing,
                            isMultiSelected = isMultiSelected,
                            onClick = { onCategorySelected(category.id) },
                            onLongClick = { onCategoryLongClick(category.id) }
                        )
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    InboxEmptyCard(
                        title = "No categories",
                        subtitle = "Captures will appear here"
                    )
                }
            }
        }

        // Vertical Divider
        VerticalDivider(
            thickness = 1.dp,
            color = Color.LightGray.copy(alpha = 0.2f)
        )

        // Detail Pane (Items List)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(Color.White)
                .padding(horizontal = 28.dp, vertical = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isItemSelectionMode) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        IconButton(onClick = { selectedItemIds = emptySet() }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear selection")
                        }
                        Text(
                            "${selectedItemIds.size} selected",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalButton(
                            onClick = {
                                onMarkItemsAsDone(selectedItemIds)
                                selectedItemIds = emptySet()
                            },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = Color(0xFFEAF2EE),
                                contentColor = Color(0xFF2D6A4F)
                            )
                        ) {
                            Icon(
                                Icons.Default.Check, 
                                contentDescription = null, 
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Mark Done")
                        }

                        IconButton(onClick = { showConfirmDeleteDialog = true }) {
                            Icon(
                                Icons.Default.Delete, 
                                contentDescription = "Delete selected",
                                tint = Color(0xFFC0392B)
                            )
                        }
                    }
                } else {
                    Column {
                        Text(
                            if (selectedCategoryId == "PENDING_SYNC") "Pending Sync" else (selectedCategoryId ?: "Captures"),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        val count = items.size
                        Text(
                            "$count ${if (count == 1) "capture" else "captures"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            if (items.isNotEmpty()) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(items) { item ->
                        val isSelected = selectedItemIds.contains(item.id)
                        CaptureItemCard(
                            item = item,
                            isSelected = isSelected,
                            onClick = {
                                if (isItemSelectionMode) {
                                    selectedItemIds = if (isSelected) {
                                        selectedItemIds - item.id
                                    } else {
                                        selectedItemIds + item.id
                                    }
                                } else {
                                    onItemClick(item.id)
                                }
                            },
                            onLongClick = {
                                selectedItemIds = if (isSelected) {
                                    selectedItemIds - item.id
                                } else {
                                    selectedItemIds + item.id
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            } else if (selectedCategoryId != null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    InboxEmptyCard(
                        title = "No items in this category",
                        subtitle = "Captured notes or documents for $selectedCategoryId will appear here."
                    )
                }
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    InboxEmptyCard(
                        title = "Select a category",
                        subtitle = "Click on any category on the left to view its captured items."
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CategoryItemCard(
    category: InboxCategory,
    isSelected: Boolean,
    isMultiSelected: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {}
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(20.dp),
        color = when {
            isMultiSelected -> Color(0xFFEAF2EE)
            isSelected -> Color(0xFFEAF2EE)
            else -> Color(0xFFF9FAF9)
        },
        border = if (isSelected || isMultiSelected) BorderStroke(1.5.dp, Color(0xFF2D6A4F)) else BorderStroke(1.dp, Color(0xFFEEEEEE))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color.White),
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
            
            Spacer(modifier = Modifier.width(14.dp))
            
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
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    category.latestItemTitle.ifBlank { "${category.count} items" },
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 12.sp,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))

            if (isMultiSelected) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = Color(0xFF2D6A4F),
                    modifier = Modifier.size(22.dp)
                )
            } else {
                Surface(
                    color = Color(0xFF2D6A4F),
                    shape = CircleShape
                ) {
                    Text(
                        category.count.toString(),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CaptureItemCard(
    item: Item,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(88.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) Color(0xFFEAF2EE) else Color(0xFFF9FAF9),
        border = if (isSelected) BorderStroke(1.5.dp, Color(0xFF2D6A4F)) else BorderStroke(1.dp, Color(0xFFEEEEEE))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Brand/Source Icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                val icon = SourceIconProvider.getIconForSource(item.source)
                
                if (icon is SourceIcon.Resource) {
                    Icon(
                        painter = painterResource(id = icon.resId),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = Color(0xFF2D6A4F)
                    )
                } else if (icon is SourceIcon.Vector) {
                    Icon(
                        imageVector = icon.imageVector,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = Color(0xFF2D6A4F)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(14.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        item.source ?: "Capture",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = Color.Gray
                    )

                    if (item.status == "PENDING") {
                        Text(" · ", color = Color.Gray, fontSize = 11.sp)
                        Text(
                            "Sync Pending",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = Color(0xFF2D6A4F),
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Text(" · ", color = Color.Gray, fontSize = 11.sp)
                    
                    val deadlineValue = item.deadline
                    val isDone = item.status == "DONE"
                    val deadlineText = if (isDone) {
                        "Marked as Done"
                    } else if (deadlineValue != null) {
                        DateUtils.getDeadlineText(deadlineValue)
                    } else {
                        DateUtils.getTimeAgo(item.createdAt)
                    }
                    
                    val deadlineDate = DateUtils.parseToLocalDate(deadlineValue)
                    val daysUntil = if (deadlineDate != null) ChronoUnit.DAYS.between(LocalDate.now(), deadlineDate) else Long.MAX_VALUE
                    val isUrgent = !isDone && daysUntil <= 2
                    
                    Text(
                        deadlineText,
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = if (isUrgent) Color(0xFFC0392B) else if (isDone) Color(0xFF2D6A4F) else Color.Gray,
                        fontWeight = if (isUrgent || isDone) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (isSelected) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = Color(0xFF2D6A4F),
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = Color.LightGray,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun SearchField(
    placeholder: String, 
    text: String, 
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFFF4F6F4)
    ) {
        BasicTextField(
            value = text,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.Black),
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (text.isEmpty()) {
                            Text(
                                text = placeholder,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                        }
                        innerTextField()
                    }
                }
            }
        )
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
                    Item(id = "1", userId = "sample", title = "Spotify Software Engineer — London", summary = null, category = "Jobs", deadline = futureDate, eventDate = null, source = "LinkedIn", createdAt = "", status = "Active"),
                    Item(id = "2", userId = "sample", title = "Google Summer Internship 2025", summary = null, category = "Jobs", deadline = soonDate, eventDate = null, source = "LinkedIn", createdAt = "", status = "Active"),
                    Item(id = "3", userId = "sample", title = "Meta University Program 2025", summary = null, category = "Jobs", deadline = upcomingDate, eventDate = null, source = "Gmail", createdAt = "", status = "Active"),
                    Item(id = "4", userId = "sample", title = "Amazon SDE Internship", summary = null, category = "Jobs", deadline = upcomingDate, eventDate = null, source = "LinkedIn", createdAt = "", status = "Active"),
                    Item(id = "5", userId = "sample", title = "React Native Developer — Remote", summary = null, category = "Jobs", deadline = null, eventDate = null, source = "WhatsApp", createdAt = "", status = "Active"),
                    Item(id = "6", userId = "sample", title = "Junior iOS Developer — Berlin", summary = null, category = "Jobs", deadline = null, eventDate = null, source = "Gmail", createdAt = "", status = "Active")
                ),
                selectedCategoryId = "Jobs"
            ),
            onCategorySelected = {},
            onDeleteCategories = {},
            onNavigateToItem = {},
            searchQuery = "",
            onSearchQueryChange = {},
            windowWidthSizeClass = WindowWidthSizeClass.Expanded
        )
    }
}
