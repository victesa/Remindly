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
import com.victorkirui.core.repository.SubscriptionTier
import com.victorkirui.core.util.DateUtils
import com.victorkirui.module_features.capturing.SourceIcon
import com.victorkirui.module_features.capturing.SourceIconProvider
import org.koin.androidx.compose.koinViewModel
import androidx.compose.ui.platform.LocalUriHandler
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import androidx.compose.foundation.clickable
import androidx.compose.ui.text.style.TextOverflow


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

    val calendarPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[android.Manifest.permission.WRITE_CALENDAR] == true) {
            viewModel.syncToCalendar()
        }
    }

    val calendarSyncResult by viewModel.calendarSyncResult.collectAsState(initial = false)
    val lastSyncedEventUri by viewModel.lastSyncedEventUri.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.calendarSyncResult.collect { success ->
            if (success) {
                android.widget.Toast.makeText(context, "Added to Google Calendar", android.widget.Toast.LENGTH_SHORT).show()
            } else {
                android.widget.Toast.makeText(context, "Failed to add to calendar", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

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
        onRetry = {
            viewModel.retryAnalysis()
        },
        onSyncToCalendar = {
            val permissions = arrayOf(
                android.Manifest.permission.WRITE_CALENDAR,
                android.Manifest.permission.READ_CALENDAR
            )
            val allGranted = permissions.all { 
                androidx.core.content.ContextCompat.checkSelfPermission(context, it) == android.content.pm.PackageManager.PERMISSION_GRANTED 
            }

            if (allGranted) {
                viewModel.syncToCalendar()
            } else {
                calendarPermissionLauncher.launch(permissions)
            }
        },
        onViewInCalendar = {
            lastSyncedEventUri?.let { uriStr ->
                try {
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        data = android.net.Uri.parse(uriStr)
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    android.util.Log.e("ItemDetails", "Failed to open calendar event: ${e.message}")
                }
            }
        },
        windowWidthSizeClass = windowSizeClass.widthSizeClass,
        hasSynced = lastSyncedEventUri != null
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
    onRetry: () -> Unit = {},
    onSyncToCalendar: () -> Unit = {},
    onViewInCalendar: () -> Unit = {},
    windowWidthSizeClass: WindowWidthSizeClass = WindowWidthSizeClass.Compact,
    hasSynced: Boolean = false
) {
    val isExpanded = windowWidthSizeClass != WindowWidthSizeClass.Compact
    var showMenu by rememberSaveable { mutableStateOf(false) }
    var showDeleteConfirm by rememberSaveable { mutableStateOf(false) }
    var showDoneSuccess by rememberSaveable { mutableStateOf(false) }
    var showUndoConfirm by rememberSaveable { mutableStateOf(false) }
    var showSubscriptionPlans by rememberSaveable { mutableStateOf(false) }

    if (showSubscriptionPlans) {
        com.victorkirui.module_features.subscription.SubscriptionPaywallSheet(
            onDismiss = { showSubscriptionPlans = false },
            onPlanSelected = { showSubscriptionPlans = false }
        )
    }

    val item = (uiState as? ItemDetailsUiState.Success)?.item
    val isDone = item?.status == "DONE"

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
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
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
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
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
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
                        /* Removed redundant sync icon here as there is a prominent button in the content */
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
                                val state = uiState as? ItemDetailsUiState.Success
                                if (state != null && state.tier == SubscriptionTier.PREMIUM && (state.item.eventDate != null || state.item.deadline != null)) {
                                    DropdownMenuItem(
                                        text = { Text("Sync to Calendar") },
                                        onClick = {
                                            onSyncToCalendar()
                                            showMenu = false
                                        },
                                        leadingIcon = {
                                            Icon(Icons.Default.CalendarToday, contentDescription = null, tint = Color(0xFF2D6A4F))
                                        }
                                    )
                                }
                                
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
                Column(
                    modifier = Modifier
                        .padding(innerPadding)
                        .then(if (isExpanded) Modifier.statusBarsPadding() else Modifier)
                ) {
                    StatusBanner(state.item.status, onRetry)
                    if (isExpanded) {
                        TwoPaneLayout(
                            state.item,
                            state.reminders,
                            PaddingValues(0.dp), // innerPadding handled by Column
                            onDelete = onDelete,
                            onMarkAsDone = onMarkAsDone,
                            onUnmarkAsDone = onUnmarkAsDone,
                            onRetry = onRetry,
                            onSyncToCalendar = onSyncToCalendar,
                            onViewInCalendar = onViewInCalendar,
                            tier = state.tier,
                            hasSynced = hasSynced,
                            onUpgrade = { showSubscriptionPlans = true }
                        )
                    } else {
                        SinglePaneLayout(
                            state.item,
                            state.reminders,
                            PaddingValues(0.dp),
                            state.tier,
                            onRetry = onRetry,
                            onSyncToCalendar = onSyncToCalendar,
                            onViewInCalendar = onViewInCalendar,
                            hasSynced = hasSynced,
                            onUpgrade = { showSubscriptionPlans = true }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatusBanner(status: String, onRetry: () -> Unit) {
    if (status == "FAILED" || status == "RETRYING") {
        val (text, color, icon) = when (status) {
            "FAILED" -> Triple("Analysis failed. Remindly couldn't extract details.", Color(0xFFFFF1F0), Icons.Default.Error)
            else -> Triple("Analysis in progress. Retrying soon...", Color(0xFFF0F5FF), Icons.Default.Sync)
        }
        
        Surface(
            color = color,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, contentDescription = null, tint = if (status == "FAILED") Color(0xFFCF1322) else Color(0xFF1D39C4))
                Spacer(modifier = Modifier.width(12.dp))
                Text(text, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                if (status == "FAILED") {
                    TextButton(onClick = onRetry) {
                        Text("Retry", fontWeight = FontWeight.Bold, color = Color(0xFFCF1322))
                    }
                }
            }
        }
    }
}

@Composable
fun SinglePaneLayout(
    item: Item, 
    reminders: List<Reminder>, 
    innerPadding: PaddingValues, 
    tier: SubscriptionTier = SubscriptionTier.FREE,
    onRetry: () -> Unit = {},
    onSyncToCalendar: () -> Unit = {},
    onViewInCalendar: () -> Unit = {},
    hasSynced: Boolean = false,
    onUpgrade: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .padding(innerPadding)
            .fillMaxSize()
    ) {
        SourceSubheader(item)
        ItemDetailsContent(
            item = item,
            reminders = reminders,
            tier = tier,
            onRetry = onRetry,
            onSyncToCalendar = onSyncToCalendar,
            onViewInCalendar = onViewInCalendar,
            hasSynced = hasSynced,
            onUpgrade = onUpgrade
        )
    }
}

@Composable
fun TwoPaneLayout(
    item: Item,
    reminders: List<Reminder>,
    innerPadding: PaddingValues,
    onDelete: () -> Unit = {},
    onMarkAsDone: () -> Unit = {},
    onUnmarkAsDone: () -> Unit = {},
    onRetry: () -> Unit = {},
    onSyncToCalendar: () -> Unit = {},
    onViewInCalendar: () -> Unit = {},
    tier: SubscriptionTier = SubscriptionTier.FREE,
    hasSynced: Boolean = false,
    onUpgrade: () -> Unit = {}
) {
    var showMenu by rememberSaveable { mutableStateOf(false) }
    var showDeleteConfirm by rememberSaveable { mutableStateOf(false) }
    var showDoneSuccess by rememberSaveable { mutableStateOf(false) }
    var showUndoConfirm by rememberSaveable { mutableStateOf(false) }
    
    val isDone = item.status == "DONE"

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
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
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
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
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
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
                        if (tier == SubscriptionTier.PREMIUM && (item.eventDate != null || item.deadline != null)) {
                            DropdownMenuItem(
                                text = { Text("Sync to Calendar") },
                                onClick = {
                                    onSyncToCalendar()
                                    showMenu = false
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.CalendarToday, contentDescription = null, tint = Color(0xFF2D6A4F))
                                }
                            )
                        }

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
                        text = DateUtils.getTimeAgo(item.createdAt),
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
            AiSummarySection(
                item = item,
                tier = tier,
                onRetryAnalysis = onRetry,
                onUpgrade = onUpgrade
            )
            
            if (tier == SubscriptionTier.PREMIUM && (item.eventDate != null || item.deadline != null)) {
                Button(
                    onClick = onSyncToCalendar,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4285F4))
                ) {
                    Icon(Icons.Default.CalendarToday, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Sync to Google Calendar", fontWeight = FontWeight.Bold)
                }
            }

            ChecklistSection(item.checklist)
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
                text = DateUtils.getTimeAgo(item.createdAt),
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
fun AiSummarySection(
    item: Item,
    tier: SubscriptionTier = SubscriptionTier.FREE,
    onRetryAnalysis: () -> Unit = {},
    onUpgrade: () -> Unit = {}
) {
    val summary = item.summary
    if (!summary.isNullOrBlank()) {
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
        return
    }

    if (tier == SubscriptionTier.PREMIUM) {
        // Pro user, but summary is missing/null (e.g. captured before upgrading)
        RemindlyCard(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, null, tint = Color(0xFF2D6A4F), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("AI Summary", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("No AI summary generated for this capture yet.", style = MaterialTheme.typography.bodySmall, color = Color.DarkGray)
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onRetryAnalysis,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2D6A4F)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Generate AI Summary", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    } else {
        // Free user
        RemindlyCard(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Executive Summary", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        Text("Upgrade to Premium for rich AI summaries of your captures.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onUpgrade,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2D6A4F)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Upgrade to Pro", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
fun ChecklistSection(checklist: List<String>?) {
    if (checklist.isNullOrEmpty()) return
    
    SectionTitle("ACTIONABLE STEPS")
    RemindlyCard {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            checklist.forEach { step ->
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        Icons.Default.RadioButtonUnchecked,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp).padding(top = 2.dp),
                        tint = Color(0xFF2D6A4F)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = step,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Black
                    )
                }
            }
        }
    }
}

@Composable
fun MetadataSection(item: Item) {
    RemindlyCard {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            if (item.deadline != null) {
                MetadataRow(
                    icon = Icons.Default.CalendarToday,
                    label = "Deadline",
                    value = DateUtils.getReadableDeadline(item.deadline),
                    isUrgent = true
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant
                )
            }
            
            if (item.eventDate != null) {
                MetadataRow(
                    icon = Icons.Default.Event,
                    label = "Event Date",
                    value = DateUtils.getReadableDeadline(item.eventDate),
                    isBold = true
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant
                )
            }

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
                value = DateUtils.getTimeAgo(item.createdAt),
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
                    value = DateUtils.formatDisplayDate(reminder.reminderDateTime)
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
fun ItemDetailsContent(
    item: Item, 
    reminders: List<Reminder>, 
    modifier: Modifier = Modifier, 
    tier: SubscriptionTier = SubscriptionTier.FREE,
    onRetry: () -> Unit = {},
    onSyncToCalendar: () -> Unit = {},
    onViewInCalendar: () -> Unit = {},
    hasSynced: Boolean = false,
    onUpgrade: () -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OriginalCaptureSection(item)
        AiSummarySection(item, tier, onRetryAnalysis = onRetry, onUpgrade = onUpgrade)
        
        if (tier == SubscriptionTier.PREMIUM && (item.eventDate != null || item.deadline != null)) {
            Button(
                onClick = if (hasSynced) onViewInCalendar else onSyncToCalendar, 
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (hasSynced) Color(0xFFEAF2EE) else Color(0xFF4285F4)
                )
            ) {
                Icon(
                    imageVector = if (hasSynced) Icons.Default.CheckCircle else Icons.Default.CalendarToday, 
                    contentDescription = null,
                    tint = if (hasSynced) Color(0xFF2D6A4F) else Color.White
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (hasSynced) "View in Google Calendar" else "Sync to Google Calendar", 
                    fontWeight = FontWeight.Bold,
                    color = if (hasSynced) Color(0xFF2D6A4F) else Color.White
                )
            }
        }

        ChecklistSection(item.checklist)
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
        userId = "sample",
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
            onBack = {},
            onRetry = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ItemDetailsDonePreview() {
    val sampleItem = Item(
        id = "1",
        userId = "sample",
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
            onBack = {},
            onRetry = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 1000, heightDp = 800)
@Composable
fun ItemDetailsLargeScreenPreview() {
    val sampleItem = Item(
        id = "1",
        userId = "sample",
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
            onRetry = {},
            windowWidthSizeClass = WindowWidthSizeClass.Expanded
        )
    }
}
