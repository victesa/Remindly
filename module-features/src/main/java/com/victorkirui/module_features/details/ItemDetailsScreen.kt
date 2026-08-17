package com.victorkirui.module_features.details

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import coil.compose.AsyncImage
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.ui.platform.LocalContext
import android.app.Activity
import com.victorkirui.local.entity.Item
import com.victorkirui.local.entity.Reminder
import com.victorkirui.core.ui.theme.*
import com.victorkirui.core.R
import com.victorkirui.module_features.capturing.SourceIcon
import com.victorkirui.module_features.capturing.SourceIconProvider
import org.koin.androidx.compose.koinViewModel
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import androidx.compose.ui.platform.LocalUriHandler
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import androidx.compose.foundation.clickable
import androidx.compose.ui.text.style.TextOverflow

private fun getTimeAgo(dateTimeStr: String): String {
    return try {
        val past = java.time.Instant.parse(dateTimeStr).atZone(java.time.ZoneId.systemDefault()).toLocalDateTime()
        val now = LocalDateTime.now()
        val minutes = ChronoUnit.MINUTES.between(past, now)
        val hours = ChronoUnit.HOURS.between(past, now)
        val days = ChronoUnit.DAYS.between(past, now)

        when {
            minutes < 1 -> "Saved just now"
            minutes < 60 -> "Saved ${minutes}m ago"
            hours < 24 -> "Saved ${hours}h ago"
            days == 1L -> "Saved yesterday"
            days < 7 -> "Saved ${days}d ago"
            else -> "Saved on ${past.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))}"
        }
    } catch (e: Exception) {
        "Saved recently"
    }
}

private fun openMedia(context: android.content.Context, uriString: String) {
    try {
        val uri = android.net.Uri.parse(uriString)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            if (uri.scheme == "file") {
                val file = File(uri.path!!)
                val contentUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                setDataAndType(contentUri, context.contentResolver.getType(contentUri) ?: if (file.extension == "pdf") "application/pdf" else "image/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } else {
                setData(uri)
            }
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        android.util.Log.e("ItemDetails", "Failed to open media: ${e.message}")
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun ItemDetailsScreen(
    itemId: String,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    viewModel: ItemDetailsViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val windowSizeClass = calculateWindowSizeClass(context as Activity)

    LaunchedEffect(itemId) {
        viewModel.loadItem(itemId)
    }

    ItemDetailsScreenContent(
        uiState = uiState, 
        onBack = onBack,
        onEdit = onEdit,
        onDelete = {
            viewModel.deleteItem()
            onBack()
        },
        onMarkAsDone = {
            viewModel.markAsDone()
        },
        onUnmarkAsDone = {
            viewModel.unmarkAsDone()
        },
        windowWidthSizeClass = windowSizeClass.widthSizeClass
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemDetailsScreenContent(
    uiState: ItemDetailsUiState,
    onBack: () -> Unit,
    onEdit: () -> Unit = {},
    onDelete: () -> Unit = {},
    onMarkAsDone: () -> Unit = {},
    onUnmarkAsDone: () -> Unit = {},
    windowWidthSizeClass: WindowWidthSizeClass = WindowWidthSizeClass.Compact
) {
    val isExpanded = windowWidthSizeClass != WindowWidthSizeClass.Compact
    var showMenu by rememberSaveable { mutableStateOf(false) }
    var showDeleteConfirm by rememberSaveable { mutableStateOf(false) }
    var showDoneSuccess by rememberSaveable { mutableStateOf(false) }
    var showUndoConfirm by rememberSaveable { mutableStateOf(false) }

    val item = (uiState as? ItemDetailsUiState.Success)?.item
    val isDone = item?.status == "DONE"

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Capture?") },
            text = { Text("This action cannot be undone. Are you sure you want to delete this capture?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showUndoConfirm) {
        AlertDialog(
            onDismissRequest = { showUndoConfirm = false },
            title = { Text("Undo Mark as Done?") },
            text = { Text("Do you want to set this capture back to active?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showUndoConfirm = false
                        onUnmarkAsDone()
                    }
                ) {
                    Text("Undo")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUndoConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDoneSuccess) {
        AlertDialog(
            onDismissRequest = { showDoneSuccess = false },
            confirmButton = {
                TextButton(onClick = { showDoneSuccess = false }) {
                    Text("OK")
                }
            },
            title = { Text("Success") },
            text = { Text("Capture successfully marked as done.") }
        )
    }

    Scaffold(
        topBar = {
            if (!isExpanded) {
                TopAppBar(
                    title = { 
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = item?.title ?: "Details",
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            if (isDone) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Done. Click to undo",
                                    tint = Color(0xFF2D6A4F),
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clickable { showUndoConfirm = true }
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.Black
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = onEdit) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit",
                                tint = Color.Black
                            )
                        }
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(
                                    imageVector = Icons.Default.MoreHoriz,
                                    contentDescription = "More",
                                    tint = Color.Black
                                )
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false },
                                modifier = Modifier.background(Color.White)
                            ) {
                                if (!isDone) {
                                    DropdownMenuItem(
                                        text = { Text("Mark as Done") },
                                        onClick = {
                                            onMarkAsDone()
                                            showMenu = false
                                            showDoneSuccess = true
                                        },
                                        leadingIcon = {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2D6A4F))
                                        }
                                    )
                                } else {
                                    DropdownMenuItem(
                                        text = { Text("Undo Mark as Done") },
                                        onClick = {
                                            showUndoConfirm = true
                                            showMenu = false
                                        },
                                        leadingIcon = {
                                            Icon(Icons.Default.History, contentDescription = null, tint = Color.Gray)
                                        }
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                                    onClick = {
                                        showMenu = false
                                        showDeleteConfirm = true
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                    }
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.White
                    )
                )
            }
        },
        containerColor = Color.White
    ) { innerPadding ->
        when (val state = uiState) {
            ItemDetailsUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is ItemDetailsUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(state.message)
                }
            }
            is ItemDetailsUiState.Success -> {
                if (isExpanded) {
                    TwoPaneLayout(
                        state.item,
                        state.reminders,
                        innerPadding,
                        onDelete = onDelete,
                        onMarkAsDone = onMarkAsDone,
                        onUnmarkAsDone = onUnmarkAsDone
                    )
                } else {
                    SinglePaneLayout(state.item, state.reminders, innerPadding)
                }
            }
        }
    }
}

@Composable
fun SinglePaneLayout(item: Item, reminders: List<Reminder>, innerPadding: PaddingValues) {
    Column(
        modifier = Modifier
            .padding(innerPadding)
            .fillMaxSize()
    ) {
        SourceSubheader(item)
        ItemDetailsContent(item, reminders)
    }
}

@Composable
fun TwoPaneLayout(
    item: Item,
    reminders: List<Reminder>,
    innerPadding: PaddingValues,
    onDelete: () -> Unit = {},
    onMarkAsDone: () -> Unit = {},
    onUnmarkAsDone: () -> Unit = {}
) {
    var showMenu by rememberSaveable { mutableStateOf(false) }
    var showDeleteConfirm by rememberSaveable { mutableStateOf(false) }
    var showDoneSuccess by rememberSaveable { mutableStateOf(false) }
    var showUndoConfirm by rememberSaveable { mutableStateOf(false) }
    
    val isDone = item.status == "DONE"

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Capture?") },
            text = { Text("This action cannot be undone. Are you sure you want to delete this capture?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showUndoConfirm) {
        AlertDialog(
            onDismissRequest = { showUndoConfirm = false },
            title = { Text("Undo Mark as Done?") },
            text = { Text("Do you want to set this capture back to active?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showUndoConfirm = false
                        onUnmarkAsDone()
                    }
                ) {
                    Text("Undo")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUndoConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDoneSuccess) {
        AlertDialog(
            onDismissRequest = { showDoneSuccess = false },
            confirmButton = {
                TextButton(onClick = { showDoneSuccess = false }) {
                    Text("OK")
                }
            },
            title = { Text("Success") },
            text = { Text("Capture successfully marked as done.") }
        )
    }

    Row(
        modifier = Modifier
            .padding(innerPadding)
            .fillMaxSize()
    ) {
        // Left Pane
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Large Title for Expanded Layout
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (isDone) {
                        Spacer(modifier = Modifier.width(12.dp))
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Done. Click to undo",
                            tint = Color(0xFF2D6A4F),
                            modifier = Modifier
                                .size(28.dp)
                                .clickable { showUndoConfirm = true }
                        )
                    }
                }

                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreHoriz,
                            contentDescription = "More",
                            tint = Color.Black
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.background(Color.White)
                    ) {
                        if (!isDone) {
                            DropdownMenuItem(
                                text = { Text("Mark as Done") },
                                onClick = {
                                    onMarkAsDone()
                                    showMenu = false
                                    showDoneSuccess = true
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2D6A4F))
                                }
                            )
                        } else {
                            DropdownMenuItem(
                                text = { Text("Undo Mark as Done") },
                                onClick = {
                                    showUndoConfirm = true
                                    showMenu = false
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.History, contentDescription = null, tint = Color.Gray)
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                showMenu = false
                                showDeleteConfirm = true
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Compact Pill-style Subheader
            Surface(
                color = Color(0xFFF4F6F4),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
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
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Shared from ${item.source ?: "Unknown"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = getTimeAgo(item.createdAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Original Capture Section in Left Pane
            OriginalCaptureSection(item)
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Open Link Button at the bottom of Left Pane
            OpenLinkButton(item.sourceUrl)
        }

        // Vertical Divider
        VerticalDivider(
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.fillMaxHeight()
        )

        // Right Pane
        Column(
            modifier = Modifier
                .weight(1.2f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AiSummarySection(item)
            MetadataSection(item)
            RemindersSection(reminders)
            RelatedSection()
        }
    }
}

@Composable
fun SourceSubheader(item: Item) {
    Surface(
        color = Color(0xFFF4F6F4),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val icon = SourceIconProvider.getIconForSource(item.source)
                if (icon is SourceIcon.Resource) {
                    Icon(
                        painter = painterResource(id = icon.resId),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = Color(0xFF2D6A4F)
                    )
                } else if (icon is SourceIcon.Vector) {
                    Icon(
                        imageVector = icon.imageVector,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = Color(0xFF2D6A4F)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Shared from ${item.source ?: "Unknown"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = getTimeAgo(item.createdAt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun OriginalCaptureSection(item: Item) {
    val originalMediaUri = item.originalMediaUri
    if (originalMediaUri != null) {
        val context = LocalContext.current
        SectionTitle("ORIGINAL CAPTURE")
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .clickable { openMedia(context, originalMediaUri) },
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = originalMediaUri,
                contentDescription = "Original Capture",
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .aspectRatio(0.75f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFF8FAF8)),
                contentScale = ContentScale.Fit
            )
        }
    }
}

@Composable
fun AiSummarySection(item: Item) {
    item.summary?.let { summary ->
        RemindlyCard(containerColor = MaterialTheme.colorScheme.primaryContainer) { // SoftMint
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "AI SUMMARY",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

@Composable
fun MetadataSection(item: Item) {
    RemindlyCard {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            MetadataRow(
                icon = Icons.Default.CalendarToday,
                label = "Deadline",
                value = item.deadline ?: "N/A",
                isUrgent = item.deadline != null
            )
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            )
            MetadataRow(
                icon = Icons.Default.Business,
                label = "Organisation",
                value = item.organization ?: "N/A",
                isBold = true
            )
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            )
            MetadataRow(
                icon = Icons.Default.Label,
                label = "Category",
                value = item.category ?: "N/A",
                isBadge = true
            )
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            )
            
            val icon = SourceIconProvider.getIconForSource(item.source)
            MetadataRow(
                icon = (icon as? SourceIcon.Vector)?.imageVector ?: Icons.Default.Link,
                iconRes = (icon as? SourceIcon.Resource)?.resId,
                label = "Source",
                value = item.source ?: "N/A",
                isSource = true
            )
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            )
            MetadataRow(
                icon = Icons.Default.AccessTime,
                label = "Saved",
                value = getTimeAgo(item.createdAt),
                isBold = true
            )
        }
    }
}

@Composable
fun RemindersSection(reminders: List<Reminder>) {
    if (reminders.isEmpty()) return
    
    SectionTitle("REMINDERS")
    RemindlyCard {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            reminders.forEachIndexed { index, reminder ->
                ReminderRow(
                    label = reminder.type.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
                    value = reminder.reminderDateTime.substring(0, 10) // Simple date extract
                )
                if (index < reminders.size - 1) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                }
            }
        }
    }
}

@Composable
fun RelatedSection() {
    SectionTitle("RELATED")
    RemindlyCard {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            RelatedRow(
                title = "Google SWE New Grad 2025",
                category = "Job",
                icon = R.drawable.ic_linkedin
            )
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            )
            RelatedRow(
                title = "Google Interview Prep Guide",
                category = "Resource",
                icon = Icons.Default.PlayCircle,
                isVector = true
            )
        }
    }
}

@Composable
fun RelatedRow(
    title: String,
    category: String,
    icon: Any,
    isVector: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp, 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Brand icon on the left for Expanded Layout
        Surface(
            modifier = Modifier.size(28.dp),
            shape = RoundedCornerShape(14.dp),
            color = if (isVector) Color(0xFFE3EEE7) else Color.Transparent
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (isVector) {
                    Icon(
                        imageVector = icon as ImageVector,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color(0xFF2D6A4F)
                    )
                } else {
                    Icon(
                        painter = painterResource(id = icon as Int),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = Color.Unspecified
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium
        )
        
        Surface(
            color = Color(0xFFF0F4F2),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = category,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Spacer(modifier = Modifier.width(8.dp))

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun OpenLinkButton(url: String?) {
    val uriHandler = LocalUriHandler.current
    Button(
        onClick = { url?.let { uriHandler.openUri(it) } },
        enabled = url != null,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Link,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Open Link",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun ItemDetailsContent(item: Item, reminders: List<Reminder>, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OriginalCaptureSection(item)
        AiSummarySection(item)
        MetadataSection(item)
        
        // Contact Section in single pane
        if (item.organization != null || item.sourceUrl != null) {
            SectionTitle("CONTACT")
            RemindlyCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val initial = item.organization?.take(1) ?: item.source?.take(1) ?: "?"
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFFE3EEE7) // LightEvergreen
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = initial.uppercase(),
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2D6A4F),
                                fontSize = 14.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.organization ?: "Company",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Source: ${item.source ?: "Unknown"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val uriHandler = LocalUriHandler.current
                        val sourceUrl = item.sourceUrl
                        if (sourceUrl != null) {
                            ContactIconButton(
                                icon = Icons.Default.Link,
                                onClick = { uriHandler.openUri(sourceUrl) }
                            )
                        }
                        if (item.source?.lowercase()?.contains("linkedin") == true && sourceUrl != null) {
                            ContactIconButton(
                                icon = Icons.Default.Link,
                                isLinkedIn = true,
                                onClick = { uriHandler.openUri(sourceUrl) }
                            )
                        }
                    }
                }
            }
        }

        RemindersSection(reminders)
        
        Spacer(modifier = Modifier.height(16.dp))
        OpenLinkButton(item.sourceUrl)
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun ContactIconButton(icon: ImageVector, isLinkedIn: Boolean = false, onClick: () -> Unit = {}) {
    Surface(
        modifier = Modifier
            .size(36.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (isLinkedIn) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_linkedin),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ReminderRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp, 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(20.dp),
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.primary
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = Color.White
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
    )
}

@Composable
fun MetadataRow(
    icon: ImageVector,
    label: String,
    value: String,
    isUrgent: Boolean = false,
    isBadge: Boolean = false,
    isBold: Boolean = false,
    isSource: Boolean = false,
    iconRes: Int? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp, 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (iconRes != null) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = Color(0xFF2D6A4F)
            )
        } else {
            Icon(
                icon,
                null,
                modifier = Modifier.size(20.dp),
                tint = if (isSource) Color(0xFF2D6A4F) else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            modifier = Modifier.width(100.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(8.dp))
        if (isBadge) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer, // SagePill
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = value,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        } else if (isSource) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        } else {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isUrgent || isBold) FontWeight.Bold else FontWeight.Normal,
                color = if (isUrgent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun RemindlyCard(
    containerColor: Color = MaterialTheme.colorScheme.surface, // Frost
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = containerColor
    ) {
        Column(content = content)
    }
}

@Preview(showBackground = true)
@Composable
fun ItemDetailsPreview() {
    val sampleItem = Item(
        id = "1",
        title = "Google Internship",
        summary = "Google is hiring software engineering interns for Summer 2025. The role is based in London and requires experience in Python or Java. Application deadline is 31 August 2025.",
        category = "Job Opportunity",
        deadline = "31 August 2025",
        eventDate = "June 1, 2024",
        organization = "Google",
        source = "LinkedIn",
        sourceUrl = "https://linkedin.com/jobs/123",
        originalMediaUri = "null",
        createdAt = "2024-05-01T10:00:00",
        status = "Active"
    )
    RemindlyTheme {
        ItemDetailsScreenContent(
            uiState = ItemDetailsUiState.Success(sampleItem),
            onBack = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ItemDetailsDonePreview() {
    val sampleItem = Item(
        id = "1",
        title = "Google Internship (Completed)",
        summary = "Google is hiring software engineering interns for Summer 2025.",
        category = "Job Opportunity",
        deadline = "31 August 2025",
        eventDate = "June 1, 2024",
        organization = "Google",
        source = "LinkedIn",
        sourceUrl = "https://linkedin.com/jobs/123",
        originalMediaUri = "null",
        createdAt = "2024-05-01T10:00:00",
        status = "DONE"
    )
    RemindlyTheme {
        ItemDetailsScreenContent(
            uiState = ItemDetailsUiState.Success(sampleItem),
            onBack = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 1000, heightDp = 800)
@Composable
fun ItemDetailsLargeScreenPreview() {
    val sampleItem = Item(
        id = "1",
        title = "Google Internship",
        summary = "Google is hiring software engineering interns for Summer 2025. The role is based in London and requires experience in Python or Java. Application deadline is 31 August 2025.",
        category = "Job Opportunity",
        deadline = "31 August 2025",
        eventDate = "June 1, 2024",
        organization = "Google",
        source = "LinkedIn",
        sourceUrl = "https://linkedin.com/jobs/123",
        originalMediaUri = "https://example.com/screenshot.png",
        createdAt = "2024-05-01T10:00:00",
        status = "Active"
    )
    RemindlyTheme {
        ItemDetailsScreenContent(
            uiState = ItemDetailsUiState.Success(sampleItem),
            onBack = {},
            windowWidthSizeClass = WindowWidthSizeClass.Expanded
        )
    }
}
