package com.victorkirui.module_features.details

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.victorkirui.core.R
import com.victorkirui.core.ui.theme.*
import com.victorkirui.local.entity.Item
import com.victorkirui.local.entity.Reminder
import org.koin.androidx.compose.koinViewModel

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

    LaunchedEffect(itemId) {
        viewModel.loadItem(itemId)
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
                onDeadlineChange = viewModel::onDeadlineChange,
                onNotesChange = viewModel::onNotesChange,
                onAddReminder = viewModel::addReminder,
                onRemoveReminder = viewModel::removeReminder,
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
                onDeadlineChange = viewModel::onDeadlineChange,
                onNotesChange = viewModel::onNotesChange,
                onAddReminder = viewModel::addReminder,
                onRemoveReminder = viewModel::removeReminder,
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
                containerColor = Color.White
            ) { padding ->
                when (val state = uiState) {
                    EditItemUiState.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Color(0xFF2D6A4F))
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
                            onDeadlineChange = viewModel::onDeadlineChange,
                            onNotesChange = viewModel::onNotesChange,
                            onAddReminder = viewModel::addReminder,
                            onRemoveReminder = viewModel::removeReminder,
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
    onDeadlineChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onAddReminder: (String, String) -> Unit,
    onRemoveReminder: (Reminder) -> Unit,
    onSave: () -> Unit
) {
    Scaffold(
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
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                }
                                Text(
                                    "Edit Item",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                            TextButton(onClick = onSave) {
                                Text("Save", color = Color(0xFF2D6A4F), fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
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
                                .aspectRatio(0.85f)
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
                                    Text("Screenshot from LinkedIn", color = Color.Gray, fontSize = 12.sp)
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
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_linkedin),
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = Color.Unspecified
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Shared from LinkedIn",
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                }
                                Text(
                                    text = "Saved yesterday",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            "A captured moment, made useful.",
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                            fontSize = 12.sp,
                            color = Color.LightGray
                        )
                    }

                    VerticalDivider(color = Color(0xFFF0F0F0), thickness = 1.dp)

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
                                fontSize = 18.sp
                            )
                            Surface(
                                color = Color(0xFFEAF2EE),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.BusinessCenter, null, tint = Color(0xFF2D6A4F), modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(uiState.category, color = Color(0xFF2D6A4F), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
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
                        EditTextField(label = "Company", value = uiState.organization, onValueChange = onOrganizationChange)
                        Spacer(modifier = Modifier.height(16.dp))
                        EditTextField(label = "Location", value = uiState.location, onValueChange = onLocationChange)
                        Spacer(modifier = Modifier.height(16.dp))
                        EditTextField(
                            label = "Application Deadline",
                            value = uiState.deadline,
                            onValueChange = onDeadlineChange,
                            trailingIcon = { Icon(Icons.Default.CalendarToday, null, tint = Color(0xFF2D6A4F), modifier = Modifier.size(20.dp)) }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        EditTextField(label = "Notes", value = uiState.notes, onValueChange = onNotesChange, minLines = 2)

                        Spacer(modifier = Modifier.height(32.dp))

                        Text(
                            "REMINDERS",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray,
                            letterSpacing = 1.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Surface(
                            color = Color(0xFFF9FAF9),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
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
                                            color = Color(0xFFF0F0F0)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        TextButton(
                            onClick = { onAddReminder("CUSTOM", "2025-08-31") },
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Text("+ Add Reminder", color = Color(0xFF2D6A4F), fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = onSave,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2D6A4F))
                        ) {
                            Text("Save Changes", fontWeight = FontWeight.Bold)
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
    onDeadlineChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onAddReminder: (String, String) -> Unit,
    onRemoveReminder: (Reminder) -> Unit,
    onSave: () -> Unit
) {
    Row(modifier = Modifier.fillMaxSize()) {
        // Side Navigation (Placeholder matching mockup)
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
                                Text("Screenshot from LinkedIn", color = Color.Gray, fontSize = 12.sp)
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
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_linkedin),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = Color.Unspecified
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Shared from LinkedIn",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                            Text(
                                text = "Saved yesterday",
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
                            fontSize = 20.sp
                        )
                        Button(
                            onClick = onSave,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2D6A4F)),
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp)
                        ) {
                            Text("Save", fontWeight = FontWeight.Bold)
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
                            EditTextField(
                                label = "Application Deadline",
                                value = uiState.deadline,
                                onValueChange = onDeadlineChange,
                                trailingIcon = { Icon(Icons.Default.CalendarToday, null, tint = Color(0xFF2D6A4F), modifier = Modifier.size(20.dp)) }
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            EditTextField(
                                label = "Job Type",
                                value = "Internship", // Mock/Metadata
                                onValueChange = {},
                                trailingIcon = { Icon(Icons.Default.KeyboardArrowDown, null, tint = Color.Gray) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    EditTextField(label = "Notes", value = uiState.notes, onValueChange = onNotesChange, minLines = 3)

                    Spacer(modifier = Modifier.height(32.dp))

                    Text(
                        "REMINDERS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Surface(
                        color = Color(0xFFF9FAF9),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
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
                                        color = Color(0xFFF0F0F0)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    TextButton(
                        onClick = { onAddReminder("CUSTOM", "2025-08-31") },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text("+ Add Reminder", color = Color(0xFF2D6A4F), fontWeight = FontWeight.Bold)
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
    onDeadlineChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onAddReminder: (String, String) -> Unit,
    onRemoveReminder: (Reminder) -> Unit,
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
        EditTextField(
            label = "Application Deadline", 
            value = state.deadline, 
            onValueChange = onDeadlineChange,
            trailingIcon = { Icon(Icons.Default.CalendarToday, null, tint = Color(0xFF2D6A4F), modifier = Modifier.size(20.dp)) }
        )
        Spacer(modifier = Modifier.height(16.dp))
        EditTextField(label = "Notes", value = state.notes, onValueChange = onNotesChange, minLines = 3)

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            "REMINDERS",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = Color.Gray,
            letterSpacing = 1.sp
        )
        
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
            onClick = { onAddReminder("CUSTOM", "2025-08-31") },
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
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_linkedin),
                contentDescription = null,
                tint = Color(0xFF2D6A4F),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Editing a $category item – fields are tailored to this category",
                fontSize = 12.sp,
                color = Color(0xFF2D6A4F),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun EditSideNavigation(
    onBack: () -> Unit = {},
    onHomeClick: () -> Unit = {},
    onInboxClick: () -> Unit = {},
    onRemindersClick: () -> Unit = {},
    onProfileClick: () -> Unit = {}
) {
    NavigationRail(
        containerColor = Color.White,
        header = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Surface(modifier = Modifier.size(32.dp), shape = CircleShape, color = Color(0xFFF0F4F2)) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("R", fontWeight = FontWeight.Bold, color = Color(0xFF2D6A4F))
                    }
                }
            }
        },
        modifier = Modifier.width(80.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        NavigationRailItem(
            icon = { Icon(Icons.Default.Home, null) },
            label = { Text("Home", fontSize = 10.sp) },
            selected = false,
            onClick = onHomeClick,
            colors = NavigationRailItemDefaults.colors(unselectedIconColor = Color.Gray, unselectedTextColor = Color.Gray)
        )
        NavigationRailItem(
            icon = { Icon(Icons.Default.Inbox, null) },
            label = { Text("Inbox", fontSize = 10.sp) },
            selected = true,
            onClick = onInboxClick,
            colors = NavigationRailItemDefaults.colors(selectedIconColor = Color(0xFF2D6A4F), selectedTextColor = Color(0xFF2D6A4F), indicatorColor = Color(0xFFE3EEE7))
        )
        NavigationRailItem(
            icon = { Icon(Icons.Default.Notifications, null) },
            label = { Text("Reminders", fontSize = 10.sp) },
            selected = false,
            onClick = onRemindersClick,
            colors = NavigationRailItemDefaults.colors(unselectedIconColor = Color.Gray, unselectedTextColor = Color.Gray)
        )
        NavigationRailItem(
            icon = { Icon(Icons.Default.AccountCircle, null) },
            label = { Text("Profile", fontSize = 10.sp) },
            selected = false,
            onClick = onProfileClick,
            colors = NavigationRailItemDefaults.colors(unselectedIconColor = Color.Gray, unselectedTextColor = Color.Gray)
        )
        Spacer(modifier = Modifier.weight(1f))
        Text("Save", color = Color(0xFF2D6A4F), fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(bottom = 24.dp))
    }
}

@Composable
fun EditBottomNavigation(
    onHomeClick: () -> Unit = {},
    onInboxClick: () -> Unit = {},
    onRemindersClick: () -> Unit = {},
    onProfileClick: () -> Unit = {}
) {
    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 0.dp,
        modifier = Modifier.height(80.dp)
    ) {
        NavigationBarItem(
            icon = { Icon(Icons.Default.Home, null) },
            label = { Text("Home") },
            selected = false,
            onClick = onHomeClick,
            colors = NavigationBarItemDefaults.colors(unselectedIconColor = Color.Gray, unselectedTextColor = Color.Gray)
        )
        NavigationBarItem(
            icon = {
                Surface(
                    color = Color(0xFFE3EEE7),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                        Icon(Icons.Default.Inbox, null, tint = Color(0xFF2D6A4F))
                    }
                }
            },
            label = { Text("Inbox", color = Color(0xFF2D6A4F)) },
            selected = true,
            onClick = onInboxClick,
            colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFF2D6A4F), indicatorColor = Color.Transparent)
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Notifications, null) },
            label = { Text("Reminders") },
            selected = false,
            onClick = onRemindersClick,
            colors = NavigationBarItemDefaults.colors(unselectedIconColor = Color.Gray, unselectedTextColor = Color.Gray)
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.AccountCircle, null) },
            label = { Text("Profile") },
            selected = false,
            onClick = onProfileClick,
            colors = NavigationBarItemDefaults.colors(unselectedIconColor = Color.Gray, unselectedTextColor = Color.Gray)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    minLines: Int = 1,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    Column {
        Text(
            text = label,
            fontSize = 11.sp,
            color = Color.Gray,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(start = 12.dp, bottom = 4.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = Color(0xFFE0E0E0),
                focusedBorderColor = Color(0xFF2D6A4F),
                unfocusedContainerColor = Color.White,
                focusedContainerColor = Color.White
            ),
            minLines = minLines,
            trailingIcon = trailingIcon,
            textStyle = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
        )
    }
}

@Composable
fun ReminderEditRow(label: String, date: String, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(24.dp),
            shape = CircleShape,
            color = Color(0xFF2D6A4F)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(14.dp))
            }
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
        title = "Google Summer Internship 2025",
        summary = "Need to update CV and portfolio before applying.",
        category = "Jobs",
        deadline = "31 August 2025",
        eventDate = null,
        organization = "Google",
        source = "LinkedIn",
        createdAt = "2024-01-01",
        status = "active",
        originalMediaUri = null
    )
    RemindlyTheme {
        val sampleState = EditItemUiState.Success(
            item = sampleItem,
            title = "Google Summer Internship 2025",
            organization = "Google",
            location = "London, UK",
            deadline = "31 August 2025",
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
                onDeadlineChange = {},
                onNotesChange = {},
                onAddReminder = { _, _ -> },
                onRemoveReminder = {},
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
        title = "Google Summer Internship 2025",
        summary = "Need to update CV and portfolio before applying.",
        category = "Jobs",
        deadline = "31 August 2025",
        eventDate = null,
        organization = "Google",
        source = "LinkedIn",
        createdAt = "2024-01-01",
        status = "active",
        originalMediaUri = null
    )
    RemindlyTheme {
        val sampleState = EditItemUiState.Success(
            item = sampleItem,
            title = "Google Summer Internship 2025",
            organization = "Google",
            location = "London, UK",
            deadline = "31 August 2025",
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
            onDeadlineChange = {},
            onNotesChange = {},
            onAddReminder = { _, _ -> },
            onRemoveReminder = {},
            onSave = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 1200, heightDp = 800)
@Composable
fun EditItemExpandedPreview() {
    val sampleItem = Item(
        id = "1",
        title = "Google Summer Internship 2025",
        summary = "Need to update CV and portfolio before applying.",
        category = "Jobs",
        deadline = "31 August 2025",
        eventDate = null,
        organization = "Google",
        source = "LinkedIn",
        createdAt = "2024-01-01",
        status = "active",
        originalMediaUri = null
    )
    RemindlyTheme {
        val sampleState = EditItemUiState.Success(
            item = sampleItem,
            title = "Google Summer Internship 2025",
            organization = "Google",
            location = "London, UK",
            deadline = "31 August 2025",
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
            onDeadlineChange = {},
            onNotesChange = {},
            onAddReminder = { _, _ -> },
            onRemoveReminder = {},
            onSave = {}
        )
    }
}
