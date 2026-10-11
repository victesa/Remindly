package com.victorkirui.module_features.details

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.victorkirui.core.ui.theme.*
import com.victorkirui.core.util.DateUtils
import com.victorkirui.module_features.capturing.SourceIcon
import com.victorkirui.module_features.capturing.SourceIconProvider
import com.victorkirui.core.R
import com.victorkirui.local.entity.Item
import org.koin.androidx.compose.koinViewModel
import java.time.LocalDate

import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDetailScreen(
    categoryName: String,
    onBack: () -> Unit,
    onItemClick: (String) -> Unit = {},
    viewModel: CategoryDetailViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedItemIds by remember { mutableStateOf(setOf<String>()) }
    val isSelectionMode = selectedItemIds.isNotEmpty()

    val selectedItems = (uiState as? CategoryDetailUiState.Success)?.items?.filter { selectedItemIds.contains(it.id) } ?: emptyList()
    val canMarkAsDone = selectedItems.isNotEmpty() && selectedItems.all { it.status != "DONE" }
    
    var showBulkDeleteConfirm by remember { mutableStateOf(false) }
    var showBulkDoneConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(categoryName) {
        viewModel.setCategory(categoryName)
    }

    if (showBulkDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showBulkDeleteConfirm = false },
            title = { Text("Delete ${selectedItemIds.size} captures?") },
            text = { Text("This action cannot be undone. Are you sure you want to delete these captures?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteItems(selectedItemIds)
                        selectedItemIds = emptySet()
                        showBulkDeleteConfirm = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBulkDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showBulkDoneConfirm) {
        AlertDialog(
            onDismissRequest = { showBulkDoneConfirm = false },
            title = { Text("Mark ${selectedItemIds.size} as Done?") },
            text = { Text("Selected captures will be marked as completed.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.markItemsAsDone(selectedItemIds)
                        selectedItemIds = emptySet()
                        showBulkDoneConfirm = false
                    }
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBulkDoneConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            if (isSelectionMode) {
                TopAppBar(
                    title = {
                        Text(
                            "${selectedItemIds.size} selected",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 20.sp,
                            color = Color.Black
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { selectedItemIds = emptySet() }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear selection", tint = Color.Black)
                        }
                    },
                    actions = {
                        val allItems = (uiState as? CategoryDetailUiState.Success)?.items ?: emptyList()
                        val isAllSelected = selectedItemIds.size == allItems.size
                        
                        IconButton(onClick = {
                            selectedItemIds = if (isAllSelected) emptySet() else allItems.map { it.id }.toSet()
                        }) {
                            Icon(
                                if (isAllSelected) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                                contentDescription = "Select all",
                                tint = Color.Black
                            )
                        }
                        IconButton(
                            onClick = { showBulkDoneConfirm = true },
                            enabled = canMarkAsDone
                        ) {
                            Icon(
                                Icons.Default.CheckCircle, 
                                contentDescription = "Mark as done", 
                                tint = if (canMarkAsDone) Color(0xFF2D6A4F) else Color.Gray.copy(alpha = 0.5f)
                            )
                        }
                        IconButton(onClick = { showBulkDeleteConfirm = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFC0392B))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFEAF2EE))
                )
            } else {
                TopAppBar(
                    title = {
                        Text(
                            categoryName,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 20.sp,
                            color = Color.Black
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.Black)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                )
            }
        },
        containerColor = Color.White
    ) { paddingValues ->
        when (val state = uiState) {
            CategoryDetailUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF2D6A4F))
                }
            }
            is CategoryDetailUiState.Success -> {
                CategoryDetailContent(
                    categoryName = state.categoryName,
                    items = state.items,
                    selectedItemIds = selectedItemIds,
                    onToggleSelection = { id ->
                        selectedItemIds = if (selectedItemIds.contains(id)) {
                            selectedItemIds - id
                        } else {
                            selectedItemIds + id
                        }
                    },
                    modifier = Modifier.padding(paddingValues),
                    onItemClick = onItemClick
                )
            }
            else -> {}
        }
    }
}

@Composable
fun CategoryDetailContent(
    categoryName: String,
    items: List<Item>,
    selectedItemIds: Set<String> = emptySet(),
    onToggleSelection: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    onItemClick: (String) -> Unit
) {
    val activeItems = items.filter { it.status != "DONE" }
    val completedItems = items.filter { it.status == "DONE" }

    val deadlineSoon = activeItems.filter { item ->
        val deadline = item.deadline
        val date = DateUtils.parseToLocalDate(deadline)
        date != null && ChronoUnit.DAYS.between(LocalDate.now(), date) <= 5
    }
    val upcoming = activeItems.filter { item ->
        val deadline = item.deadline
        val date = DateUtils.parseToLocalDate(deadline)
        date != null && ChronoUnit.DAYS.between(LocalDate.now(), date) > 5
    }
    val noDeadline = activeItems.filter { it.deadline == null }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFFEAF2EE),
                border = BorderStroke(1.dp, Color(0xFF2D6A4F).copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when(categoryName.lowercase()) {
                                "work", "jobs", "job" -> Icons.Default.WorkOutline
                                "school", "education" -> Icons.Default.School
                                "event", "events" -> Icons.Default.Event
                                "receipt", "bills" -> Icons.Default.ReceiptLong
                                "flight", "travel" -> Icons.Default.Flight
                                "sync" -> Icons.Default.Sync
                                else -> Icons.Default.FolderOpen
                            },
                            contentDescription = null,
                            tint = Color(0xFF2D6A4F),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Text(
                            categoryName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color(0xFF2D6A4F)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            "${items.size} ${if (items.size == 1) "capture" else "captures"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        if (deadlineSoon.isNotEmpty()) {
            item { CategorySectionHeader("URGENT DEADLINES", type = "URGENT") }
            items(deadlineSoon) { item ->
                val icon = SourceIconProvider.getIconForSource(item.source)
                val isSelected = selectedItemIds.contains(item.id)
                CategoryItemCard(
                    title = item.title,
                    subtitle = getSubtitleText(item),
                    iconRes = (icon as? SourceIcon.Resource)?.resId,
                    icon = (icon as? SourceIcon.Vector)?.imageVector,
                    isUrgent = true,
                    isSelected = isSelected,
                    onClick = {
                        if (selectedItemIds.isNotEmpty()) onToggleSelection(item.id)
                        else onItemClick(item.id)
                    },
                    onLongClick = { onToggleSelection(item.id) }
                )
            }
        }

        if (upcoming.isNotEmpty()) {
            item { CategorySectionHeader("SCHEDULED", type = "SCHEDULED") }
            items(upcoming) { item ->
                val icon = SourceIconProvider.getIconForSource(item.source)
                val isSelected = selectedItemIds.contains(item.id)
                CategoryItemCard(
                    title = item.title,
                    subtitle = getSubtitleText(item),
                    iconRes = (icon as? SourceIcon.Resource)?.resId,
                    icon = (icon as? SourceIcon.Vector)?.imageVector,
                    isSelected = isSelected,
                    onClick = {
                        if (selectedItemIds.isNotEmpty()) onToggleSelection(item.id)
                        else onItemClick(item.id)
                    },
                    onLongClick = { onToggleSelection(item.id) }
                )
            }
        }

        if (noDeadline.isNotEmpty()) {
            item { CategorySectionHeader("NO SET DEADLINE", type = "DEFAULT") }
            items(noDeadline) { item ->
                val icon = SourceIconProvider.getIconForSource(item.source)
                val isSelected = selectedItemIds.contains(item.id)
                CategoryItemCard(
                    title = item.title,
                    subtitle = getSubtitleText(item),
                    iconRes = (icon as? SourceIcon.Resource)?.resId,
                    icon = (icon as? SourceIcon.Vector)?.imageVector,
                    isSelected = isSelected,
                    onClick = {
                        if (selectedItemIds.isNotEmpty()) onToggleSelection(item.id)
                        else onItemClick(item.id)
                    },
                    onLongClick = { onToggleSelection(item.id) }
                )
            }
        }

        if (completedItems.isNotEmpty()) {
            item { CategorySectionHeader("COMPLETED", type = "COMPLETED") }
            items(completedItems) { item ->
                val icon = SourceIconProvider.getIconForSource(item.source)
                val isSelected = selectedItemIds.contains(item.id)
                CategoryItemCard(
                    title = item.title,
                    subtitle = getSubtitleText(item),
                    iconRes = (icon as? SourceIcon.Resource)?.resId,
                    icon = (icon as? SourceIcon.Vector)?.imageVector,
                    isSelected = isSelected,
                    isDone = true,
                    onClick = {
                        if (selectedItemIds.isNotEmpty()) onToggleSelection(item.id)
                        else onItemClick(item.id)
                    },
                    onLongClick = { onToggleSelection(item.id) }
                )
            }
        }
        
        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

private fun getSubtitleText(item: Item): String {
    if (item.status == "DONE") return "Marked as completed"
    val syncText = if (item.status == "PENDING") " [Syncing...]" else ""
    val deadlineText = DateUtils.getDeadlineText(item.deadline)
    return if (deadlineText.isNotEmpty()) {
        deadlineText + syncText
    } else {
        "Captured ${DateUtils.getTimeAgo(item.createdAt)}$syncText"
    }
}

@Composable
fun CategorySectionHeader(title: String, type: String = "DEFAULT") {
    val (dotColor, textColor) = when (type) {
        "URGENT" -> Color(0xFFC0392B) to Color(0xFFC0392B)
        "COMPLETED" -> Color(0xFF2D6A4F) to Color(0xFF2D6A4F)
        "SCHEDULED" -> Color(0xFF2D6A4F) to Color(0xFF2D6A4F)
        else -> Color.Gray to Color.Gray
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            title,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = textColor,
            letterSpacing = 1.2.sp
        )
    }
}

@Composable
fun CategoryItemCard(
    title: String,
    subtitle: String,
    iconRes: Int? = null,
    icon: ImageVector? = null,
    isUrgent: Boolean = false,
    isSelected: Boolean = false,
    isDone: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {}
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (isDone && !isSelected) 0.6f else 1f)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { onLongClick() }
                )
            },
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) Color(0xFFEAF2EE) else Color(0xFFF9FAF9),
        border = if (isSelected) BorderStroke(1.5.dp, Color(0xFF2D6A4F)) else BorderStroke(1.dp, Color(0xFFEEEEEE))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) Color(0xFF2D6A4F) 
                        else if (isDone) Color(0xFFEAF2EE)
                        else Color.White
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                } else if (isDone) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Done",
                        tint = Color(0xFF2D6A4F),
                        modifier = Modifier.size(20.dp)
                    )
                } else if (iconRes != null) {
                    Icon(
                        painter = painterResource(id = iconRes),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = Color(0xFF2D6A4F)
                    )
                } else if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = Color(0xFF2D6A4F)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(14.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = if (isDone) Color.Gray else Color.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textDecoration = if (isDone) TextDecoration.LineThrough else null
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    color = if (isUrgent) Color(0xFFC0392B) else if (isDone) Color(0xFF2D6A4F) else Color.Gray,
                    fontWeight = if (isUrgent || isDone) FontWeight.Bold else FontWeight.Normal
                )
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

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
fun CategoryDetailScreenPreview() {
    RemindlyTheme {
        CategoryDetailScreen(
            categoryName = "Jobs",
            onBack = {}
        )
    }
}
