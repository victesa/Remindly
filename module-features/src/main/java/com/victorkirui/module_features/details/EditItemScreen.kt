package com.victorkirui.module_features.details

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.victorkirui.core.R
import com.victorkirui.core.ui.theme.*
import com.victorkirui.core.ui.component.RemindlyNavigationRail
import com.victorkirui.core.ui.component.RemindlyBottomNavigation
import com.victorkirui.core.util.DateUtils
import com.victorkirui.local.entity.Item
import com.victorkirui.local.entity.Reminder
import com.victorkirui.module_features.capturing.SourceIcon
import com.victorkirui.module_features.capturing.SourceIconProvider
import org.koin.androidx.compose.koinViewModel
import java.time.LocalDate
import java.time.LocalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditItemScreen(
    itemId: String,
    onBack: () -> Unit,
    onNavigateToHome: () -> Unit = {},
    onNavigateToInbox: () -> Unit = {},
    onNavigateToReminders: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    windowWidthSizeClass: WindowWidthSizeClass = WindowWidthSizeClass.Compact,
    viewModel: EditItemViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }

    LaunchedEffect(itemId) {
        viewModel.loadItem(itemId)
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            colors = DatePickerDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        selectedDate = java.time.Instant.ofEpochMilli(it)
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDate()
                        showDatePicker = false
                        showTimePicker = true
                    }
                }) { Text("Next", color = MaterialTheme.colorScheme.primary) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    selectedDayContainerColor = MaterialTheme.colorScheme.primary,
                    selectedDayContentColor = MaterialTheme.colorScheme.onPrimary,
                    todayContentColor = MaterialTheme.colorScheme.primary,
                    todayDateBorderColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState()
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            containerColor = MaterialTheme.colorScheme.surface,
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updateDeadline(
                        selectedDate,
                        LocalTime.of(timePickerState.hour, timePickerState.minute)
                    )
                    showTimePicker = false
                }) { Text("Confirm", color = MaterialTheme.colorScheme.primary) }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            },
            text = {
                TimePicker(
                    state = timePickerState,
                    colors = TimePickerDefaults.colors(
                        clockDialColor = MaterialTheme.colorScheme.surfaceVariant,
                        selectorColor = MaterialTheme.colorScheme.primary,
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        )
    }

    when (windowWidthSizeClass) {
        WindowWidthSizeClass.Expanded -> {
            EditItemContentExpanded(
                uiState = uiState,
                onBack = onBack,
                onNavigateToHome = onNavigateToHome,
                onNavigateToInbox = onNavigateToInbox,
                onNavigateToReminders = onNavigateToReminders,
                onNavigateToProfile = onNavigateToProfile,
                onTitleChange = viewModel::onTitleChange,
                onOrganizationChange = viewModel::onOrganizationChange,
                onLocationChange = viewModel::onLocationChange,
                onDeadlineClick = { showDatePicker = true },
                onNotesChange = viewModel::onNotesChange,
                onAddReminder = viewModel::addReminder,
                onRemoveReminder = viewModel::removeReminder,
                onRefreshReminders = viewModel::refreshReminders,
                onSave = {
                    viewModel.saveChanges { onBack() }
                }
            )
        }
        WindowWidthSizeClass.Medium -> {
            EditItemContentMedium(
                uiState = uiState,
                onBack = onBack,
                onNavigateToHome = onNavigateToHome,
                onNavigateToInbox = onNavigateToInbox,
                onNavigateToReminders = onNavigateToReminders,
                onNavigateToProfile = onNavigateToProfile,
                onTitleChange = viewModel::onTitleChange,
                onOrganizationChange = viewModel::onOrganizationChange,
                onLocationChange = viewModel::onLocationChange,
                onDeadlineClick = { showDatePicker = true },
                onNotesChange = viewModel::onNotesChange,
                onAddReminder = viewModel::addReminder,
                onRemoveReminder = viewModel::removeReminder,
                onRefreshReminders = viewModel::refreshReminders,
                onSave = {
                    viewModel.saveChanges { onBack() }
                }
            )
        }
        else -> {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                "Edit Item",
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        },
                        actions = {
                            TextButton(onClick = {
                                viewModel.saveChanges { onBack() }
                            }) {
                                Text(
                                    "Save",
                                    color = Color(0xFF2D6A4F),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                    )
                },
                containerColor = MaterialTheme.colorScheme.background
            ) { padding ->
                when (val state = uiState) {
                    EditItemUiState.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    is EditItemUiState.Error -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(state.message)
                        }
                    }
                    is EditItemUiState.Success -> {
                        EditItemContentSmall(
                            state = state,
                            padding = padding,
                            onTitleChange = viewModel::onTitleChange,
                            onOrganizationChange = viewModel::onOrganizationChange,
                            onLocationChange = viewModel::onLocationChange,
                            onDeadlineClick = { showDatePicker = true },
                            onNotesChange = viewModel::onNotesChange,
                            onAddReminder = viewModel::addReminder,
                            onRemoveReminder = viewModel::removeReminder,
                            onRefreshReminders = viewModel::refreshReminders,
                            onSave = {
                                viewModel.saveChanges { onBack() }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EditItemContentMedium(
    uiState: EditItemUiState,
    onBack: () -> Unit,
    onNavigateToHome: () -> Unit = {},
    onNavigateToInbox: () -> Unit = {},
    onNavigateToReminders: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onTitleChange: (String) -> Unit,
    onOrganizationChange: (String) -> Unit,
    onLocationChange: (String) -> Unit,
    onDeadlineClick: () -> Unit,
    onNotesChange: (String) -> Unit,
    onAddReminder: (String, String) -> Unit,
    onRemoveReminder: (Reminder) -> Unit,
    onRefreshReminders: () -> Unit,
    onSave: () -> Unit
) {
    Scaffold(
        modifier = Modifier.statusBarsPadding(),
        bottomBar = { 
            EditBottomNavigation(
                onHomeClick = onNavigateToHome,
                onInboxClick = onNavigateToInbox,
                onRemindersClick = onNavigateToReminders,
                onProfileClick = onNavigateToProfile
            ) 
        },
        containerColor = Color.White
    ) { padding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (uiState) {
                EditItemUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFF2D6A4F))
                    }
                }
                is EditItemUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(uiState.message)
                    }
                }
                is EditItemUiState.Success -> {
                    // Left Column: Original Capture
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(24.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = onBack) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                                }
                                Text(
                                    "Edit Item",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                            TextButton(onClick = onSave) {
                                Text("Save", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        CategoryInfoChip(uiState.category)
                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            "ORIGINAL CAPTURE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(0.85f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            if (uiState.item.originalMediaUri != null) {
                                AsyncImage(
                                    model = uiState.item.originalMediaUri,
                                    contentDescription = "Original Capture",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit
                                )
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Image, null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(48.dp))
                                    Text("Screenshot from Source", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val icon = SourceIconProvider.getIconForSource(uiState.item.source)
                                    if (icon is SourceIcon.Resource) {
                                        Icon(
                                            painter = painterResource(id = icon.resId),
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp),
                                            tint = Color.Unspecified
                                        )
                                    } else if (icon is SourceIcon.Vector) {
                                        Icon(
                                            imageVector = icon.imageVector,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Shared from ${uiState.item.source ?: "Unknown"}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = DateUtils.getTimeAgo(uiState.item.createdAt),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            "A captured moment, made useful.",
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }

                    VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)

                    // Right Column: Edit Details
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(24.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Edit Details",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            CategoryInfoChip(uiState.category)
                        }

                        if (uiState is EditItemUiState.Success && uiState.validationError != null) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = uiState.validationError,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.padding(12.dp),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        EditTextField(label = "Title", value = uiState.title, onValueChange = onTitleChange)
                        Spacer(modifier = Modifier.height(16.dp))
                        EditTextField(label = "Company", value = uiState.organization, onValueChange = onOrganizationChange)
                        Spacer(modifier = Modifier.height(16.dp))
                        EditTextField(label = "Location", value = uiState.location, onValueChange = onLocationChange)
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Box(modifier = Modifier.clickable { onDeadlineClick() }) {
                            EditTextField(
                                label = "Application Deadline",
                                value = DateUtils.formatDisplayDate(uiState.deadline),
                                onValueChange = {},
                                enabled = false,
                                trailingIcon = { Icon(Icons.Default.CalendarToday, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp)) }
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        EditTextField(label = "Notes", value = uiState.notes, onValueChange = onNotesChange, minLines = 2)

                        Spacer(modifier = Modifier.height(32.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "REMINDERS",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 1.sp
                            )
                            
                            IconButton(onClick = onRefreshReminders, modifier = Modifier.size(24.dp)) {
                                Icon(
                                    imageVector = Icons.Default.Sync,
                                    contentDescription = "Sync with Deadline",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth(),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column {
                                uiState.reminders.forEachIndexed { index, reminder ->
                                    ReminderEditRow(
                                        label = reminder.type.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
                                        date = reminder.reminderDateTime.substring(0, 10),
                                        onRemove = { onRemoveReminder(reminder) }
                                    )
                                    if (index < uiState.reminders.size - 1) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(horizontal = 16.dp),
                                            color = MaterialTheme.colorScheme.outlineVariant
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        TextButton(
                            onClick = { onAddReminder("CUSTOM", LocalDate.now().toString()) },
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Text("+ Add Reminder", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = onSave,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Save Changes", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EditItemContentExpanded(
    uiState: EditItemUiState,
    onBack: () -> Unit,
    onNavigateToHome: () -> Unit = {},
    onNavigateToInbox: () -> Unit = {},
    onNavigateToReminders: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onTitleChange: (String) -> Unit,
    onOrganizationChange: (String) -> Unit,
    onLocationChange: (String) -> Unit,
    onDeadlineClick: () -> Unit,
    onNotesChange: (String) -> Unit,
    onAddReminder: (String, String) -> Unit,
    onRemoveReminder: (Reminder) -> Unit,
    onRefreshReminders: () -> Unit,
    onSave: () -> Unit
) {
    Row(modifier = Modifier.fillMaxSize()) {
        // Side Navigation
        EditSideNavigation(
            onBack = onBack,
            onHomeClick = onNavigateToHome,
            onInboxClick = onNavigateToInbox,
            onRemindersClick = onNavigateToReminders,
            onProfileClick = onNavigateToProfile
        )

        when (uiState) {
            EditItemUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF2D6A4F))
                }
            }
            is EditItemUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(uiState.message)
                }
            }
            is EditItemUiState.Success -> {
                // Left Column: Original Capture & Info
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                        Text(
                            "Edit Item",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    CategoryInfoChip(uiState.category)

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        "ORIGINAL CAPTURE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(0.8f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF8FAF8)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (uiState.item.originalMediaUri != null) {
                            AsyncImage(
                                model = uiState.item.originalMediaUri,
                                contentDescription = "Original Capture",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Image, null, tint = Color.LightGray, modifier = Modifier.size(48.dp))
                                Text("Screenshot from Source", color = Color.Gray, fontSize = 12.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Surface(
                        color = Color(0xFFF4F6F4),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val icon = SourceIconProvider.getIconForSource(uiState.item.source)
                                if (icon is SourceIcon.Resource) {
                                    Icon(
                                        painter = painterResource(id = icon.resId),
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = Color.Unspecified
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
                                    text = "Shared from ${uiState.item.source ?: "Unknown"}",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                            Text(
                                text = DateUtils.getTimeAgo(uiState.item.createdAt),
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }

                VerticalDivider(color = Color(0xFFF0F0F0), thickness = 1.dp)

                // Right Column: Edit Details
                Column(
                    modifier = Modifier
                        .weight(1.5f)
                        .fillMaxHeight()
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Edit Details",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Button(
                            onClick = onSave,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp)
                        ) {
                            Text("Save", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                        }
                    }

                    if (uiState is EditItemUiState.Success && uiState.validationError != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = uiState.validationError,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    EditTextField(label = "Title", value = uiState.title, onValueChange = onTitleChange)
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Box(modifier = Modifier.weight(1f)) {
                            EditTextField(label = "Company", value = uiState.organization, onValueChange = onOrganizationChange)
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            EditTextField(label = "Location", value = uiState.location, onValueChange = onLocationChange)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Box(modifier = Modifier.weight(1f)) {
                            Box(modifier = Modifier.clickable { onDeadlineClick() }) {
                                EditTextField(
                                    label = "Application Deadline",
                                    value = DateUtils.formatDisplayDate(uiState.deadline),
                                    onValueChange = {},
                                    enabled = false,
                                    trailingIcon = { Icon(Icons.Default.CalendarToday, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp)) }
                                )
                            }
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            EditTextField(
                                label = "Job Type",
                                value = "Internship", // Mock/Metadata
                                onValueChange = {},
                                trailingIcon = { Icon(Icons.Default.KeyboardArrowDown, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    EditTextField(label = "Notes", value = uiState.notes, onValueChange = onNotesChange, minLines = 3)

                    Spacer(modifier = Modifier.height(32.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "REMINDERS",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.sp
                        )
                        
                        IconButton(onClick = onRefreshReminders, modifier = Modifier.size(24.dp)) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = "Sync with Deadline",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column {
                            uiState.reminders.forEachIndexed { index, reminder ->
                                ReminderEditRow(
                                    label = reminder.type.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
                                    date = reminder.reminderDateTime.substring(0, 10),
                                    onRemove = { onRemoveReminder(reminder) }
                                )
                                if (index < uiState.reminders.size - 1) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 16.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    TextButton(
                        onClick = { onAddReminder("CUSTOM", LocalDate.now().toString()) },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text("+ Add Reminder", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun EditItemContentSmall(
    state: EditItemUiState.Success,
    padding: PaddingValues,
    onTitleChange: (String) -> Unit,
    onOrganizationChange: (String) -> Unit,
    onLocationChange: (String) -> Unit,
    onDeadlineClick: () -> Unit,
    onNotesChange: (String) -> Unit,
    onAddReminder: (String, String) -> Unit,
    onRemoveReminder: (Reminder) -> Unit,
    onRefreshReminders: () -> Unit,
    onSave: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        CategoryInfoChip(state.category)

        if (state.validationError != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = state.validationError,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        EditTextField(label = "Title", value = state.title, onValueChange = onTitleChange)
        Spacer(modifier = Modifier.height(16.dp))
        EditTextField(label = "Company", value = state.organization, onValueChange = onOrganizationChange)
        Spacer(modifier = Modifier.height(16.dp))
        EditTextField(label = "Location", value = state.location, onValueChange = onLocationChange)
        Spacer(modifier = Modifier.height(16.dp))
        
        Box(modifier = Modifier.clickable { onDeadlineClick() }) {
            EditTextField(
                label = "Application Deadline", 
                value = DateUtils.formatDisplayDate(state.deadline), 
                onValueChange = {},
                enabled = false,
                trailingIcon = { Icon(Icons.Default.CalendarToday, null, tint = Color(0xFF2D6A4F), modifier = Modifier.size(20.dp)) }
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        EditTextField(label = "Notes", value = state.notes, onValueChange = onNotesChange, minLines = 3)

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "REMINDERS",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color.Gray,
                letterSpacing = 1.sp
            )
            
            IconButton(onClick = onRefreshReminders, modifier = Modifier.size(24.dp)) {
                Icon(
                    imageVector = Icons.Default.Sync,
                    contentDescription = "Sync with Deadline",
                    tint = Color(0xFF2D6A4F),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        Surface(
            color = Color(0xFFF9FAF9),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                state.reminders.forEachIndexed { index, reminder ->
                    ReminderEditRow(
                        label = reminder.type.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
                        date = reminder.reminderDateTime.substring(0, 10),
                        onRemove = { onRemoveReminder(reminder) }
                    )
                    if (index < state.reminders.size - 1) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = Color(0xFFF0F0F0)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(
            onClick = { onAddReminder("CUSTOM", LocalDate.now().toString()) },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("+ Add Reminder", color = Color(0xFF2D6A4F), fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onSave,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2D6A4F))
        ) {
            Text("Save Changes", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
        
        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun CategoryInfoChip(category: String) {
    Surface(
        color = Color(0xFFEAF2EE),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Label, null, tint = Color(0xFF2D6A4F), modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = category,
                color = Color(0xFF2D6A4F),
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun EditSideNavigation(
    onBack: () -> Unit,
    onHomeClick: () -> Unit,
    onInboxClick: () -> Unit,
    onRemindersClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(80.dp)
            .fillMaxHeight()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.Black)
        }
        
        Spacer(modifier = Modifier.height(48.dp))

        NavigationRailItem(
            icon = { Icon(Icons.Default.Home, "Home") },
            selected = false,
            onClick = onHomeClick,
            label = { Text("Home") }
        )
        NavigationRailItem(
            icon = { Icon(Icons.Default.Inbox, "Inbox") },
            selected = false,
            onClick = onInboxClick,
            label = { Text("Inbox") }
        )
        NavigationRailItem(
            icon = { Icon(Icons.Default.Notifications, "Reminders") },
            selected = false,
            onClick = onRemindersClick,
            label = { Text("Reminders") }
        )
        NavigationRailItem(
            icon = { Icon(Icons.Default.Person, "Profile") },
            selected = false,
            onClick = onProfileClick,
            label = { Text("Profile") }
        )
    }
}

@Composable
fun EditBottomNavigation(
    onHomeClick: () -> Unit,
    onInboxClick: () -> Unit,
    onRemindersClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 8.dp
    ) {
        NavigationBarItem(
            icon = { Icon(Icons.Default.Home, "Home") },
            selected = false,
            onClick = onHomeClick,
            label = { Text("Home") }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Inbox, "Inbox") },
            selected = false,
            onClick = onInboxClick,
            label = { Text("Inbox") }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Notifications, "Reminders") },
            selected = false,
            onClick = onRemindersClick,
            label = { Text("Reminders") }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Person, "Profile") },
            selected = false,
            onClick = onProfileClick,
            label = { Text("Profile") }
        )
    }
}

@Composable
fun EditTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    minLines: Int = 1,
    enabled: Boolean = true,
    trailingIcon: @Composable (() -> Unit)? = null,
    singleLine: Boolean = true
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                disabledBorderColor = MaterialTheme.colorScheme.outlineVariant,
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            minLines = minLines,
            enabled = enabled,
            trailingIcon = trailingIcon,
            singleLine = singleLine
        )
    }
}

@Composable
fun ReminderEditRow(label: String, date: String, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp, 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Color(0xFFEAF2EE)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Notifications, null, tint = Color(0xFF2D6A4F), modifier = Modifier.size(16.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
        Text(
            text = date,
            fontSize = 12.sp,
            color = Color.Gray
        )
        IconButton(onClick = onRemove) {
            Icon(Icons.Default.Close, null, tint = Color.LightGray, modifier = Modifier.size(16.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun EditItemPreview() {
    val sampleItem = Item(
        id = "1",
        userId = "sample",
        title = "Google Summer Internship 2025",
        summary = "Need to update CV and portfolio before applying.",
        category = "Jobs",
        deadline = "2025-08-31T10:00:00",
        eventDate = null,
        organization = "Google",
        source = "LinkedIn",
        createdAt = "2024-01-01T10:00:00",
        status = "active",
        originalMediaUri = null
    )
    RemindlyTheme {
        val sampleState = EditItemUiState.Success(
            item = sampleItem,
            title = "Google Summer Internship 2025",
            organization = "Google",
            location = "London, UK",
            deadline = "2025-08-31T10:00:00",
            notes = "Need to update CV and portfolio before applying.",
            category = "Jobs",
            reminders = listOf(
                Reminder(itemId = "1", reminderDateTime = "2025-08-24T09:00:00", type = "7_DAYS_BEFORE"),
                Reminder(itemId = "1", reminderDateTime = "2025-08-29T09:00:00", type = "2_DAYS_BEFORE"),
                Reminder(itemId = "1", reminderDateTime = "2025-08-31T09:00:00", type = "MORNING_OF")
            )
        )

        Scaffold(
            topBar = {
                @OptIn(ExperimentalMaterial3Api::class)
                TopAppBar(
                    title = {
                        Text(
                            "Edit Item",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {}) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        TextButton(onClick = {}) {
                            Text(
                                "Save",
                                color = Color(0xFF2D6A4F),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                )
            },
            containerColor = Color.White
        ) { padding ->
            EditItemContentSmall(
                state = sampleState,
                padding = padding,
                onTitleChange = {},
                onOrganizationChange = {},
                onLocationChange = {},
                onDeadlineClick = {},
                onNotesChange = {},
                onAddReminder = { _, _ -> },
                onRemoveReminder = {},
                onRefreshReminders = {},
                onSave = {}
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 1000, heightDp = 700)
@Composable
fun EditItemMediumPreview() {
    val sampleItem = Item(
        id = "1",
        userId = "sample",
        title = "Google Summer Internship 2025",
        summary = "Need to update CV and portfolio before applying.",
        category = "Jobs",
        deadline = "2025-08-31T10:00:00",
        eventDate = null,
        organization = "Google",
        source = "LinkedIn",
        createdAt = "2024-01-01T10:00:00",
        status = "active",
        originalMediaUri = null
    )
    RemindlyTheme {
        val sampleState = EditItemUiState.Success(
            item = sampleItem,
            title = "Google Summer Internship 2025",
            organization = "Google",
            location = "London, UK",
            deadline = "2025-08-31T10:00:00",
            notes = "Need to update CV and portfolio before applying.",
            category = "Jobs",
            reminders = listOf(
                Reminder(itemId = "1", reminderDateTime = "2025-08-24T09:00:00", type = "7_DAYS_BEFORE"),
                Reminder(itemId = "1", reminderDateTime = "2025-08-29T09:00:00", type = "2_DAYS_BEFORE"),
                Reminder(itemId = "1", reminderDateTime = "2025-08-31T09:00:00", type = "MORNING_OF")
            )
        )

        EditItemContentMedium(
            uiState = sampleState,
            onBack = {},
            onTitleChange = {},
            onOrganizationChange = {},
            onLocationChange = {},
            onDeadlineClick = {},
            onNotesChange = {},
            onAddReminder = { _, _ -> },
            onRemoveReminder = {},
            onRefreshReminders = {},
            onSave = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 1200, heightDp = 800)
@Composable
fun EditItemExpandedPreview() {
    val sampleItem = Item(
        id = "1",
        userId = "sample",
        title = "Google Summer Internship 2025",
        summary = "Need to update CV and portfolio before applying.",
        category = "Jobs",
        deadline = "2025-08-31T10:00:00",
        eventDate = null,
        organization = "Google",
        source = "LinkedIn",
        createdAt = "2024-01-01T10:00:00",
        status = "active",
        originalMediaUri = null
    )
    RemindlyTheme {
        val sampleState = EditItemUiState.Success(
            item = sampleItem,
            title = "Google Summer Internship 2025",
            organization = "Google",
            location = "London, UK",
            deadline = "2025-08-31T10:00:00",
            notes = "Need to update CV and portfolio before applying.",
            category = "Jobs",
            reminders = listOf(
                Reminder(itemId = "1", reminderDateTime = "2025-08-24T09:00:00", type = "7_DAYS_BEFORE"),
                Reminder(itemId = "1", reminderDateTime = "2025-08-29T09:00:00", type = "2_DAYS_BEFORE"),
                Reminder(itemId = "1", reminderDateTime = "2025-08-31T09:00:00", type = "MORNING_OF")
            )
        )

        EditItemContentExpanded(
            uiState = sampleState,
            onBack = {},
            onTitleChange = {},
            onOrganizationChange = {},
            onLocationChange = {},
            onDeadlineClick = {},
            onNotesChange = {},
            onAddReminder = { _, _ -> },
            onRemoveReminder = {},
            onRefreshReminders = {},
            onSave = {}
        )
    }
}
