package com.victorkirui.module_features.profile

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.victorkirui.core.ui.theme.*
import com.victorkirui.core.repository.SubscriptionTier
import com.victorkirui.module_features.subscription.SubscriptionPaywallSheet
import com.victorkirui.core.R
import org.koin.androidx.compose.koinViewModel
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private fun formatDisplayTime(timeStr: String): String {
    return try {
        val time = LocalTime.parse(timeStr)
        time.format(DateTimeFormatter.ofPattern("hh:mm a", Locale.US))
    } catch (e: Exception) {
        timeStr
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    windowWidthSizeClass: WindowWidthSizeClass,
    onSignOut: () -> Unit = {},
    onShowWalkthrough: () -> Unit = {},
    onNavigateToHome: () -> Unit = {},
    viewModel: ProfileViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    var showDeletionDialog by remember { mutableStateOf(false) }

    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(viewModel.deletionResult) {
        viewModel.deletionResult.collect { result ->
            if (result.isSuccess) {
                onSignOut() // Redirect to auth on successful deletion
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "Failed to delete account. Please try again."
                android.widget.Toast.makeText(context, errorMsg, android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    var showTimePicker by remember { mutableStateOf(false) }
    var showLeadTimePicker by remember { mutableStateOf(false) }
    var showSubscriptionPlans by remember { mutableStateOf(false) }
    var showWidgetsGallery by remember { mutableStateOf(false) }

    if (showLeadTimePicker) {
        LeadTimePickerDialog(
            currentHours = uiState.deadlineAlertLeadTimeHours,
            onDismiss = { showLeadTimePicker = false },
            onSelected = { hours ->
                viewModel.setDeadlineAlertLeadTime(hours)
                showLeadTimePicker = false
            }
        )
    }

    val timePickerState = rememberTimePickerState(
        initialHour = uiState.preferredReminderTime.split(":")[0].toIntOrNull() ?: 8,
        initialMinute = uiState.preferredReminderTime.split(":")[1].take(2).toIntOrNull() ?: 0
    )

    if (showTimePicker) {
        TimePickerDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                Button(
                    onClick = {
                        val hour = if (timePickerState.hour < 10) "0${timePickerState.hour}" else "${timePickerState.hour}"
                        val minute = if (timePickerState.minute < 10) "0${timePickerState.minute}" else "${timePickerState.minute}"
                        viewModel.updateReminderTime("$hour:$minute")
                        showTimePicker = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2D6A4F)),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Confirm", fontWeight = FontWeight.Bold, color = Color.White) }
            },
            dismissButton = {
                TextButton(
                    onClick = { showTimePicker = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF2D6A4F))
                ) { Text("Cancel", fontWeight = FontWeight.SemiBold) }
            }
        ) {
            TimePicker(
                state = timePickerState,
                colors = TimePickerDefaults.colors(
                    clockDialColor = Color(0xFFEAF2EE),
                    clockDialSelectedContentColor = Color.White,
                    clockDialUnselectedContentColor = Color(0xFF2D6A4F),
                    selectorColor = Color(0xFF2D6A4F),
                    periodSelectorSelectedContainerColor = Color(0xFF2D6A4F),
                    periodSelectorSelectedContentColor = Color.White,
                    periodSelectorUnselectedContainerColor = Color(0xFFF4F6F4),
                    periodSelectorUnselectedContentColor = Color.Gray,
                    timeSelectorSelectedContainerColor = Color(0xFFEAF2EE),
                    timeSelectorSelectedContentColor = Color(0xFF2D6A4F),
                    timeSelectorUnselectedContainerColor = Color(0xFFF4F6F4),
                    timeSelectorUnselectedContentColor = Color.Black
                )
            )
        }
    }

    if (showDeletionDialog) {
        AlertDialog(
            onDismissRequest = { showDeletionDialog = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(24.dp),
            title = { Text("Delete Account", fontWeight = FontWeight.Bold, color = Color.Black) },
            text = { Text("Are you sure you want to delete your account? This action cannot be undone and all your data will be permanently removed.", color = Color(0xFF4A4A4A), fontSize = 14.sp) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteAccount()
                        showDeletionDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC0392B)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeletionDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF2D6A4F))
                ) {
                    Text("Cancel", fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }

    ProfileScreenContent(
        windowWidthSizeClass = windowWidthSizeClass,
        uiState = uiState,
        onSignOut = onSignOut,
        onDeleteAccount = { showDeletionDialog = true },
        onToggleMorningBriefing = viewModel::toggleMorningBriefing,
        onToggleDeadlineAlerts = viewModel::toggleDeadlineAlerts,
        onShowTimePicker = { showTimePicker = true },
        onShowLeadTimePicker = { showLeadTimePicker = true },
        onToggleCalendarSync = viewModel::toggleCalendarSync,
        onShowWidgetsGallery = { showWidgetsGallery = true },
        onUpgrade = {
            showSubscriptionPlans = true
        },
        onShowWalkthrough = onShowWalkthrough
    )

    if (showSubscriptionPlans) {
        SubscriptionPaywallSheet(
            onDismiss = { showSubscriptionPlans = false },
            onPlanSelected = { tier ->
                showSubscriptionPlans = false
                if (tier == SubscriptionTier.PREMIUM) {
                    onNavigateToHome()
                }
            }
        )
    }

    if (showWidgetsGallery) {
        WidgetsGallerySheet(
            onDismiss = { showWidgetsGallery = false }
        )
    }
}

@Composable
fun ProfileScreenContent(
    windowWidthSizeClass: WindowWidthSizeClass,
    uiState: ProfileUiState,
    onSignOut: () -> Unit,
    onDeleteAccount: () -> Unit,
    onToggleMorningBriefing: (Boolean) -> Unit,
    onToggleDeadlineAlerts: (Boolean) -> Unit,
    onShowTimePicker: () -> Unit,
    onShowLeadTimePicker: () -> Unit,
    onToggleCalendarSync: (Boolean) -> Unit = {},
    onShowWidgetsGallery: () -> Unit = {},
    onUpgrade: () -> Unit = {},
    onShowProAdPreview: () -> Unit = {},
    onShowWalkthrough: () -> Unit = {}
) {
    Scaffold(
        containerColor = Color.White,
        modifier = Modifier.statusBarsPadding()
    ) { paddingValues ->
        when (windowWidthSizeClass) {
            WindowWidthSizeClass.Expanded -> ProfileLargeContent(
                state = uiState,
                modifier = Modifier.padding(paddingValues),
                onSignOut = onSignOut,
                onDeleteAccount = onDeleteAccount,
                onToggleMorningBriefing = onToggleMorningBriefing,
                onToggleDeadlineAlerts = onToggleDeadlineAlerts,
                onShowTimePicker = onShowTimePicker,
                onShowLeadTimePicker = onShowLeadTimePicker,
                onToggleCalendarSync = onToggleCalendarSync,
                onShowWidgetsGallery = onShowWidgetsGallery,
                onUpgrade = onUpgrade,
                onShowWalkthrough = onShowWalkthrough
            )
            else -> ProfileSmallContent(
                state = uiState,
                modifier = Modifier.padding(paddingValues),
                onSignOut = onSignOut,
                onDeleteAccount = onDeleteAccount,
                onToggleMorningBriefing = onToggleMorningBriefing,
                onToggleDeadlineAlerts = onToggleDeadlineAlerts,
                onShowTimePicker = onShowTimePicker,
                onShowLeadTimePicker = onShowLeadTimePicker,
                onToggleCalendarSync = onToggleCalendarSync,
                onShowWidgetsGallery = onShowWidgetsGallery,
                onUpgrade = onUpgrade,
                onShowWalkthrough = onShowWalkthrough
            )
        }
    }
}

@Composable
fun LeadTimePickerDialog(
    currentHours: Int,
    onDismiss: () -> Unit,
    onSelected: (Int) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(24.dp),
        title = { Text("Deadline Alert Lead Time", fontWeight = FontWeight.Bold, color = Color.Black) },
        text = {
            Column {
                listOf(1, 2, 3, 6, 12, 24).forEach { hours ->
                    val isSelected = hours == currentHours
                    Surface(
                        color = if (isSelected) Color(0xFFEAF2EE) else Color.Transparent,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelected(hours) }
                            .padding(vertical = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { onSelected(hours) },
                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF2D6A4F))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (hours == 1) "1 hour before" else "$hours hours before",
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color(0xFF2D6A4F) else Color.Black
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF2D6A4F))
            ) { Text("Cancel", fontWeight = FontWeight.SemiBold) }
        }
    )
}

@Composable
fun TimePickerDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        containerColor = Color.White,
        shape = RoundedCornerShape(24.dp),
        confirmButton = confirmButton,
        dismissButton = dismissButton,
        text = { content() }
    )
}

@Composable
fun ProfileSmallContent(
    state: ProfileUiState,
    modifier: Modifier = Modifier,
    onSignOut: () -> Unit,
    onDeleteAccount: () -> Unit,
    onToggleMorningBriefing: (Boolean) -> Unit,
    onToggleDeadlineAlerts: (Boolean) -> Unit,
    onShowTimePicker: () -> Unit,
    onShowLeadTimePicker: () -> Unit,
    onToggleCalendarSync: (Boolean) -> Unit,
    onShowWidgetsGallery: () -> Unit,
    onUpgrade: () -> Unit,
    onShowWalkthrough: () -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            "Profile",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Medium,
            color = Color.Black
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        UserInfoSection(name = state.userName, email = state.userEmail)

        Spacer(modifier = Modifier.height(32.dp))

            SubscriptionCard(
                tier = state.subscriptionTier, 
                quotaLimit = state.quotaLimit, 
                quotaRemaining = state.quotaRemaining, 
                onUpgrade = onUpgrade
            )

        if (state.subscriptionTier == SubscriptionTier.PREMIUM) {
            Spacer(modifier = Modifier.height(32.dp))
            ProfileSectionHeader("PRO FEATURES")
            PremiumFeaturesCard(
                isCalendarSyncEnabled = state.isCalendarSyncEnabled,
                onToggleCalendarSync = onToggleCalendarSync,
                onShowWidgetsGallery = onShowWidgetsGallery
            )
        }
        
        Spacer(modifier = Modifier.height(48.dp))
        
        ProfileSectionHeader("NOTIFICATIONS")
        NotificationsCard(
            state = state,
            onToggleMorningBriefing = onToggleMorningBriefing,
            onToggleDeadlineAlerts = onToggleDeadlineAlerts,
            onShowTimePicker = onShowTimePicker,
            onShowLeadTimePicker = onShowLeadTimePicker
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        ProfileSectionHeader("GENERAL")
        GeneralCard(onShowWalkthrough = onShowWalkthrough)

        Spacer(modifier = Modifier.height(32.dp))

        ProfileSectionHeader("FEEDBACK & RATING")
        SupportCard()
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Sign Out",
                color = Color(0xFFC0392B),
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                modifier = Modifier.clickable { onSignOut() }
            )
            
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "Delete Account",
                color = Color.Gray,
                fontSize = 13.sp,
                modifier = Modifier.clickable { onDeleteAccount() }
            )
        }
    }
}

@Composable
fun ProfileMediumContent(
    state: ProfileUiState,
    modifier: Modifier = Modifier,
    onSignOut: () -> Unit,
    onDeleteAccount: () -> Unit,
    onToggleMorningBriefing: (Boolean) -> Unit,
    onToggleDeadlineAlerts: (Boolean) -> Unit,
    onShowTimePicker: () -> Unit,
    onShowLeadTimePicker: () -> Unit,
    onToggleCalendarSync: (Boolean) -> Unit,
    onShowWidgetsGallery: () -> Unit,
    onUpgrade: () -> Unit,
    onShowWalkthrough: () -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 24.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Profile",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Medium,
                color = Color.Black
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            UserInfoRow(name = state.userName, email = state.userEmail)

            Spacer(modifier = Modifier.height(24.dp))

                SubscriptionCard(
                tier = state.subscriptionTier, 
                quotaLimit = state.quotaLimit, 
                quotaRemaining = state.quotaRemaining, 
                onUpgrade = onUpgrade
            )

            if (state.subscriptionTier == SubscriptionTier.PREMIUM) {
                Spacer(modifier = Modifier.height(24.dp))
                ProfileSectionHeader("PRO FEATURES")
                PremiumFeaturesCard(
                    isCalendarSyncEnabled = state.isCalendarSyncEnabled,
                    onToggleCalendarSync = onToggleCalendarSync,
                    onShowWidgetsGallery = onShowWidgetsGallery
                )
            }
        }

        VerticalDivider(
            modifier = Modifier.padding(horizontal = 24.dp).fillMaxHeight(),
            color = Color(0xFFF0F0F0),
            thickness = 1.dp
        )

        Column(
            modifier = Modifier
                .weight(1.1f)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                "Settings",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Medium,
                color = Color.Black
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            ProfileSectionHeader("NOTIFICATIONS")
            NotificationsCardMedium(
                state = state,
                onToggleMorningBriefing = onToggleMorningBriefing,
                onToggleDeadlineAlerts = onToggleDeadlineAlerts,
                onShowTimePicker = onShowTimePicker,
                onShowLeadTimePicker = onShowLeadTimePicker
            )
            
            Spacer(modifier = Modifier.height(24.dp))

            ProfileSectionHeader("GENERAL")
            GeneralCardMedium(onShowWalkthrough = onShowWalkthrough)

            Spacer(modifier = Modifier.height(24.dp))
            
            ProfileSectionHeader("FEEDBACK & RATING")
            SupportCardMedium()
            
            Spacer(modifier = Modifier.height(40.dp))
            
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Sign Out",
                    color = Color(0xFFC0392B),
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp,
                    modifier = Modifier.clickable { onSignOut() }
                )
                
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    "Delete Account",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    modifier = Modifier.clickable { onDeleteAccount() }
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun ProfileLargeContent(
    state: ProfileUiState,
    modifier: Modifier = Modifier,
    onSignOut: () -> Unit,
    onDeleteAccount: () -> Unit,
    onToggleMorningBriefing: (Boolean) -> Unit,
    onToggleDeadlineAlerts: (Boolean) -> Unit,
    onShowTimePicker: () -> Unit,
    onShowLeadTimePicker: () -> Unit,
    onToggleCalendarSync: (Boolean) -> Unit,
    onShowWidgetsGallery: () -> Unit,
    onUpgrade: () -> Unit,
    onShowWalkthrough: () -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 48.dp, vertical = 32.dp)
    ) {
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            UserInfoSection(name = state.userName, email = state.userEmail)

            Spacer(modifier = Modifier.height(32.dp))

                SubscriptionCard(
                tier = state.subscriptionTier, 
                quotaLimit = state.quotaLimit, 
                quotaRemaining = state.quotaRemaining, 
                onUpgrade = onUpgrade
            )

            if (state.subscriptionTier == SubscriptionTier.PREMIUM) {
                Spacer(modifier = Modifier.height(32.dp))
                ProfileSectionHeader("PRO FEATURES")
                PremiumFeaturesCard(
                    isCalendarSyncEnabled = state.isCalendarSyncEnabled,
                    onToggleCalendarSync = onToggleCalendarSync,
                    onShowWidgetsGallery = onShowWidgetsGallery
                )
            }
            
            Spacer(modifier = Modifier.height(48.dp))
            
            Spacer(modifier = Modifier.weight(1f))
            
            Text(
                "Sign Out",
                color = Color(0xFFC0392B),
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                modifier = Modifier.clickable { onSignOut() }
            )
            
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "Delete Account",
                color = Color.Gray,
                fontSize = 13.sp,
                modifier = Modifier.clickable { onDeleteAccount() }
            )
        }

        VerticalDivider(
            modifier = Modifier.padding(horizontal = 48.dp).fillMaxHeight(),
            color = Color(0xFFF0F0F0),
            thickness = 1.dp
        )

        Column(
            modifier = Modifier
                .weight(1.6f)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                "Settings",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Medium,
                color = Color.Black
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            ProfileSectionHeader("NOTIFICATIONS")
            NotificationsCardLarge(
                state = state,
                onToggleMorningBriefing = onToggleMorningBriefing,
                onToggleDeadlineAlerts = onToggleDeadlineAlerts,
                onShowTimePicker = onShowTimePicker,
                onShowLeadTimePicker = onShowLeadTimePicker
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            ProfileSectionHeader("GENERAL")
            GeneralCardLarge(onShowWalkthrough = onShowWalkthrough)

            Spacer(modifier = Modifier.height(32.dp))

            ProfileSectionHeader("FEEDBACK & RATING")
            SupportCardLarge()
        }
    }
}

@Composable
fun NotificationsCard(
    state: ProfileUiState,
    onToggleMorningBriefing: (Boolean) -> Unit,
    onToggleDeadlineAlerts: (Boolean) -> Unit,
    onShowTimePicker: () -> Unit,
    onShowLeadTimePicker: () -> Unit
) {
    val formattedTime = formatDisplayTime(state.preferredReminderTime)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFFF9FAF9)
    ) {
        Column(modifier = Modifier.padding(vertical = 12.dp)) {
            NotificationItem(
                title = "Morning Briefing",
                subtitle = "Daily summary at $formattedTime",
                checked = state.isMorningBriefingEnabled,
                onCheckedChange = onToggleMorningBriefing
            )
            NotificationItem(
                title = "Deadline Alerts",
                subtitle = "7 days, 2 days, morning of",
                checked = state.isDeadlineAlertsEnabled,
                onCheckedChange = onToggleDeadlineAlerts
            )
            SettingItem(
                title = "Notification Time",
                value = formattedTime,
                onClick = onShowTimePicker
            )
            SettingItem(
                title = "Deadline Lead Time",
                value = if (state.deadlineAlertLeadTimeHours == 1) "1 hour before" else "${state.deadlineAlertLeadTimeHours} hours before",
                onClick = onShowLeadTimePicker
            )
        }
    }
}

@Composable
fun NotificationsCardMedium(
    state: ProfileUiState,
    onToggleMorningBriefing: (Boolean) -> Unit,
    onToggleDeadlineAlerts: (Boolean) -> Unit,
    onShowTimePicker: () -> Unit,
    onShowLeadTimePicker: () -> Unit
) {
    val formattedTime = formatDisplayTime(state.preferredReminderTime)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFFF9FAF9)
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            NotificationItemMedium(
                title = "Morning Briefing",
                subtitle = "Daily summary at $formattedTime",
                checked = state.isMorningBriefingEnabled,
                icon = Icons.Outlined.Notifications,
                onCheckedChange = onToggleMorningBriefing
            )
            NotificationItemMedium(
                title = "Deadline Alerts",
                subtitle = "7d, 2d, morning of",
                checked = state.isDeadlineAlertsEnabled,
                icon = Icons.Outlined.Notifications,
                onCheckedChange = onToggleDeadlineAlerts
            )
            SettingItemMedium(
                title = "Notification Time",
                value = formattedTime,
                icon = Icons.Outlined.AccessTime,
                onClick = onShowTimePicker
            )
            SettingItemMedium(
                title = "Deadline Lead Time",
                value = if (state.deadlineAlertLeadTimeHours == 1) "1 hour before" else "${state.deadlineAlertLeadTimeHours} hours before",
                icon = Icons.Outlined.Timer,
                onClick = onShowLeadTimePicker
            )
        }
    }
}

@Composable
fun NotificationsCardLarge(
    state: ProfileUiState,
    onToggleMorningBriefing: (Boolean) -> Unit,
    onToggleDeadlineAlerts: (Boolean) -> Unit,
    onShowTimePicker: () -> Unit,
    onShowLeadTimePicker: () -> Unit
) {
    val formattedTime = formatDisplayTime(state.preferredReminderTime)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFFF9FAF9)
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            NotificationItemMedium(
                title = "Morning Briefing",
                subtitle = "Daily summary at $formattedTime",
                checked = state.isMorningBriefingEnabled,
                icon = Icons.Outlined.Notifications,
                onCheckedChange = onToggleMorningBriefing
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFFF0F0F0))
            NotificationItemMedium(
                title = "Deadline Alerts",
                subtitle = "7d, 2d, morning of",
                checked = state.isDeadlineAlertsEnabled,
                icon = Icons.Outlined.Notifications,
                onCheckedChange = onToggleDeadlineAlerts
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFFF0F0F0))
            SettingItemMedium(
                title = "Notification Time",
                value = formattedTime,
                icon = Icons.Outlined.AccessTime,
                onClick = onShowTimePicker
            )
            SettingItemMedium(
                title = "Deadline Lead Time",
                value = if (state.deadlineAlertLeadTimeHours == 1) "1 hour before" else "${state.deadlineAlertLeadTimeHours} hours before",
                icon = Icons.Outlined.Timer,
                onClick = onShowLeadTimePicker
            )
        }
    }
}

@Composable
fun GeneralCardLarge(onShowWalkthrough: () -> Unit = {}) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFFF9FAF9)
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            SettingItemMedium(
                title = "App Tour & Capabilities",
                value = null,
                icon = Icons.Outlined.HelpOutline,
                onClick = onShowWalkthrough
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFFF0F0F0))
            SettingItemMedium(
                title = "Privacy & Data",
                value = null,
                icon = Icons.Outlined.Shield,
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://victor-kirui.dev/remindly/privacy-policy"))
                    context.startActivity(intent)
                }
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFFF0F0F0))
            SettingItemMedium(
                title = "Terms & Conditions",
                value = null,
                icon = Icons.Outlined.Description,
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://victor-kirui.dev/remindly/terms-and-conditions"))
                    context.startActivity(intent)
                }
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFFF0F0F0))
            SettingItemMedium(
                title = "About Remindly",
                value = remember(context) {
                    try { "v${context.packageManager.getPackageInfo(context.packageName, 0).versionName}" } catch (e: Exception) { "v2.0.0" }
                },
                icon = Icons.Outlined.Info,
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://victor-kirui.dev/remindly"))
                    context.startActivity(intent)
                }
            )
        }
    }
}

@Composable
fun SupportCardLarge() {
    val context = androidx.compose.ui.platform.LocalContext.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFFF9FAF9)
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            SettingItemMedium(
                title = "Send Feedback",
                value = null,
                icon = Icons.Outlined.ChatBubbleOutline,
                onClick = {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:")
                        putExtra(Intent.EXTRA_EMAIL, arrayOf("remindly.productivity@gmail.com"))
                        putExtra(Intent.EXTRA_SUBJECT, "Remindly Feedback")
                    }
                    context.startActivity(intent)
                }
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFFF0F0F0))
            SettingItemMedium(
                title = "Rate Remindly",
                value = null,
                icon = Icons.Outlined.StarOutline,
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}"))
                    context.startActivity(intent)
                }
            )
        }
    }
}

@Composable
fun SubscriptionPlansDialog(
    currentTier: SubscriptionTier,
    onDismiss: () -> Unit,
    onTierChange: (SubscriptionTier) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(24.dp),
        titleContentColor = Color.Black,
        textContentColor = Color(0xFF4A4A4A),
        confirmButton = {
            if (currentTier == SubscriptionTier.FREE) {
                Button(
                    onClick = { onTierChange(SubscriptionTier.PREMIUM) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2D6A4F)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Upgrade Now", fontWeight = FontWeight.Bold)
                }
            } else {
                TextButton(
                    onClick = { onTierChange(SubscriptionTier.FREE) },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Gray)
                ) {
                    Text("Switch to Free (Test)", fontWeight = FontWeight.Medium)
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF2D6A4F))
            ) { Text("Close", fontWeight = FontWeight.SemiBold) }
        },
        title = {
            Text(
                "Remindly Premium",
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Get more out of your AI memory with Remindly Premium.")
                
                PlanFeatureRow("Structured Captures", true)
                PlanFeatureRow("Actionable Steps", true)
                PlanFeatureRow("Daily Briefings", true)
                PlanFeatureRow("Gemini AI Voice & Assistant Actions", currentTier == SubscriptionTier.PREMIUM, isPremium = true)
                PlanFeatureRow("Rich AI Summaries", currentTier == SubscriptionTier.PREMIUM, isPremium = true)
                PlanFeatureRow("Visual OCR (Flyers/Receipts)", currentTier == SubscriptionTier.PREMIUM, isPremium = true)
                PlanFeatureRow("Deep Web Synthesis", currentTier == SubscriptionTier.PREMIUM, isPremium = true)
                PlanFeatureRow("Multi-Page PDF Processing (up to 50 pages)", currentTier == SubscriptionTier.PREMIUM, isPremium = true)
                PlanFeatureRow("250 Monthly AI Extractions (25x Quota)", currentTier == SubscriptionTier.PREMIUM, isPremium = true)

            }
        }
    )
}

@Composable
private fun PlanFeatureRow(text: String, isIncluded: Boolean, isPremium: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = if (isIncluded) Icons.Default.CheckCircle else Icons.Default.Circle,
            contentDescription = null,
            tint = if (isIncluded) Color(0xFF2D6A4F) else Color.LightGray,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isIncluded) Color.Black else Color.Gray,
            textDecoration = if (!isIncluded && !isPremium) TextDecoration.LineThrough else null
        )
        if (isPremium && !isIncluded) {
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                color = Color(0xFFF3E5F5),
                shape = CircleShape
            ) {
                Text(
                    "PRO",
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF7B1FA2)
                )
            }
        }
    }
}

@Composable
fun SubscriptionCard(tier: SubscriptionTier, quotaLimit: Int, quotaRemaining: Int, onUpgrade: () -> Unit) {
    val context = LocalContext.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (tier == SubscriptionTier.FREE) {
                    Modifier.clickable { onUpgrade() }
                } else {
                    Modifier
                }
            ),
        shape = RoundedCornerShape(24.dp),
        color = if (tier == SubscriptionTier.PREMIUM) Color(0xFFEAF2EE) else MaterialTheme.colorScheme.surface,
        border = if (tier == SubscriptionTier.PREMIUM) BorderStroke(1.dp, Color(0xFF2D6A4F).copy(alpha = 0.3f)) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (tier == SubscriptionTier.PREMIUM) "Remindly Pro" else "Free Plan",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (tier == SubscriptionTier.PREMIUM) Color(0xFF2D6A4F) else MaterialTheme.colorScheme.onSurface
                        )
                        if (tier == SubscriptionTier.PREMIUM) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                Icons.Default.Verified,
                                contentDescription = "Verified Pro",
                                tint = Color(0xFF2D6A4F),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Text(
                        text = if (tier == SubscriptionTier.PREMIUM) "All Pro features unlocked" else "Basic AI extraction",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    
                    if (quotaLimit > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { (quotaRemaining.toFloat() / quotaLimit.toFloat()).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth(0.6f).height(4.dp).clip(CircleShape),
                            color = Color(0xFF2D6A4F),
                            trackColor = Color.LightGray.copy(alpha = 0.5f)
                        )
                        Text(
                            text = "$quotaRemaining / $quotaLimit AI requests remaining",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
                
                if (tier == SubscriptionTier.FREE) {
                    Button(
                        onClick = onUpgrade,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2D6A4F)),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Upgrade", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                } else {
                    TextButton(
                        onClick = {
                            try {
                                val intent = Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://play.google.com/store/account/subscriptions?package=${context.packageName}")
                                )
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                android.widget.Toast.makeText(context, "Opening Google Play Subscriptions...", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Text(
                            "Manage",
                            color = Color(0xFF2D6A4F),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PremiumFeaturesCard(
    isCalendarSyncEnabled: Boolean,
    onToggleCalendarSync: (Boolean) -> Unit,
    onShowWidgetsGallery: () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.all { it }
        if (granted) {
            onToggleCalendarSync(true)
        } else {
            android.widget.Toast.makeText(context, "Calendar permission is required to auto-sync events.", android.widget.Toast.LENGTH_SHORT).show()
            onToggleCalendarSync(false)
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFFF9FAF9)
    ) {
        Column(modifier = Modifier.padding(vertical = 12.dp)) {
            NotificationItem(
                title = "Calendar Auto-Sync",
                subtitle = "Auto-sync deadlines & events to your phone calendar",
                checked = isCalendarSyncEnabled,
                onCheckedChange = { enabling ->
                    if (enabling) {
                        val hasWrite = androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.WRITE_CALENDAR) == android.content.pm.PackageManager.PERMISSION_GRANTED
                        val hasRead = androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CALENDAR) == android.content.pm.PackageManager.PERMISSION_GRANTED
                        if (hasWrite && hasRead) {
                            onToggleCalendarSync(true)
                        } else {
                            permissionLauncher.launch(
                                arrayOf(
                                    android.Manifest.permission.WRITE_CALENDAR,
                                    android.Manifest.permission.READ_CALENDAR
                                )
                            )
                        }
                    } else {
                        onToggleCalendarSync(false)
                    }
                }
            )
            SettingItem(
                title = "Widgets",
                value = "Browse & add 6 Pro home screen widgets",
                onClick = onShowWidgetsGallery
            )
        }
    }
}

@Composable
fun UserInfoSection(name: String, email: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(Color(0xFF2D6A4F)),
            contentAlignment = Alignment.Center
        ) {
            if (name.isNotBlank() && name != "User") {
                Text(
                    name.take(1).uppercase(),
                    color = Color.White,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Normal
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Profile",
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            name,
            fontSize = 24.sp,
            fontWeight = FontWeight.Medium,
            color = Color.Black
        )
        
        Text(
            email,
            fontSize = 14.sp,
            color = Color(0xFF6B7C6E)
        )
    }
}

@Composable
fun UserInfoRow(name: String, email: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(Color(0xFF2D6A4F)),
            contentAlignment = Alignment.Center
        ) {
            if (name.isNotBlank() && name != "User") {
                Text(
                    name.take(1).uppercase(),
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Medium
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Profile",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column {
            Text(
                name,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black
            )
            Text(
                email,
                fontSize = 13.sp,
                color = Color(0xFF6B7C6E)
            )
        }
    }
}

@Composable
fun ProfileSectionHeader(title: String) {
    Text(
        title,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        color = Color(0xFF6B7C6E),
        letterSpacing = 1.2.sp,
        modifier = Modifier.padding(bottom = 12.dp)
    )
}

@Composable
fun GeneralCard(onShowWalkthrough: () -> Unit = {}) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFFF9FAF9)
    ) {
        Column(modifier = Modifier.padding(vertical = 12.dp)) {
            SettingItem(
                title = "App Tour & Capabilities",
                value = null,
                onClick = onShowWalkthrough
            )
            SettingItem(
                title = "Privacy & Data",
                value = null,
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://victor-kirui.dev/remindly/privacy-policy"))
                    context.startActivity(intent)
                }
            )
            SettingItem(
                title = "Terms & Conditions",
                value = null,
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://victor-kirui.dev/remindly/terms-and-conditions"))
                    context.startActivity(intent)
                }
            )
            SettingItem(
                title = "About Remindly",
                value = remember(context) {
                    try { "Version ${context.packageManager.getPackageInfo(context.packageName, 0).versionName}" } catch (e: Exception) { "Version 2.0.0" }
                },
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://victor-kirui.dev/remindly"))
                    context.startActivity(intent)
                }
            )
        }
    }
}

@Composable
fun GeneralCardMedium(onShowWalkthrough: () -> Unit = {}) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFFF9FAF9)
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            SettingItemMedium(
                title = "App Tour & Capabilities",
                value = null,
                icon = Icons.Outlined.Help,
                onClick = onShowWalkthrough
            )
            SettingItemMedium(
                title = "Privacy & Data",
                value = null,
                icon = Icons.Outlined.Shield,
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://victor-kirui.dev/remindly/privacy-policy"))
                    context.startActivity(intent)
                }
            )
            SettingItemMedium(
                title = "Terms & Conditions",
                value = null,
                icon = Icons.Outlined.Description,
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://victor-kirui.dev/remindly/terms-and-conditions"))
                    context.startActivity(intent)
                }
            )

            SettingItemMedium(
                title = "About Remindly",
                value = remember(context) {
                    try { "v${context.packageManager.getPackageInfo(context.packageName, 0).versionName}" } catch (e: Exception) { "v2.0.0" }
                },
                icon = Icons.Outlined.Info,
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://victor-kirui.dev/remindly"))
                    context.startActivity(intent)
                }
            )
        }
    }
}

@Composable
fun SupportCardMedium() {
    val context = androidx.compose.ui.platform.LocalContext.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFFF9FAF9)
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            SettingItemMedium(
                title = "Send Feedback",
                value = null,
                icon = Icons.Outlined.ChatBubbleOutline,
                onClick = {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:")
                        putExtra(Intent.EXTRA_EMAIL, arrayOf("remindly.productivity@gmail.com"))
                        putExtra(Intent.EXTRA_SUBJECT, "Remindly Feedback")
                    }
                    context.startActivity(intent)
                }
            )
            SettingItemMedium(
                title = "Rate Remindly",
                value = null,
                icon = Icons.Outlined.StarOutline,
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}"))
                    context.startActivity(intent)
                }
            )
        }
    }
}

@Composable
fun SupportCard() {
    val context = androidx.compose.ui.platform.LocalContext.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFFF9FAF9)
    ) {
        Column(modifier = Modifier.padding(vertical = 12.dp)) {
            SettingItem(
                title = "Send Feedback",
                value = "Email remindly.productivity@gmail.com",
                onClick = {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:")
                        putExtra(Intent.EXTRA_EMAIL, arrayOf("remindly.productivity@gmail.com"))
                        putExtra(Intent.EXTRA_SUBJECT, "Remindly Feedback")
                    }
                    context.startActivity(intent)
                }
            )
            SettingItem(
                title = "Rate Remindly",
                value = "On Google Play Store",
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}"))
                    context.startActivity(intent)
                }
            )
        }
    }
}

@Composable
fun NotificationItem(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                color = Color.Black
            )
            Text(
                subtitle,
                fontSize = 13.sp,
                color = Color(0xFF6B7C6E)
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF2D6A4F),
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color(0xFFE0E0E0),
                uncheckedBorderColor = Color.Transparent
            )
        )
    }
}

@Composable
fun NotificationItemMedium(title: String, subtitle: String, checked: Boolean, icon: ImageVector, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = Color(0xFF2D6A4F), modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    title,
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp,
                    color = Color.Black
                )
                Text(
                    subtitle,
                    fontSize = 11.sp,
                    color = Color(0xFF6B7C6E)
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF2D6A4F),
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color(0xFFE0E0E0),
                uncheckedBorderColor = Color.Transparent
            ),
            modifier = Modifier.scale(0.85f)
        )
    }
}

@Composable
fun SettingItem(title: String, value: String?, onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                color = Color.Black
            )
            if (value != null) {
                Text(
                    value,
                    fontSize = 13.sp,
                    color = Color(0xFF6B7C6E)
                )
            }
        }
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Color(0xFF6B7C6E).copy(alpha = 0.5f),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun SettingItemMedium(title: String, value: String?, icon: ImageVector, onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = Color(0xFF2D6A4F), modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                title,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
                color = Color.Black
            )
        }
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (value != null) {
                Text(
                    value,
                    fontSize = 12.sp,
                    color = Color(0xFF6B7C6E),
                    modifier = Modifier.padding(end = 4.dp)
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color(0xFF6B7C6E).copy(alpha = 0.5f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

private fun Modifier.scale(scale: Float) = this.then(
    Modifier.layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        layout((placeable.width * scale).toInt(), (placeable.height * scale).toInt()) {
            placeable.placeRelative(0, 0)
        }
    }
)

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
fun ProfileSmallScreenPreview() {
    RemindlyTheme {
        ProfileScreenContent(
            windowWidthSizeClass = WindowWidthSizeClass.Compact,
            uiState = ProfileUiState(
                userName = "Victor Kirui",
                userEmail = "victor@example.com"
            ),
            onSignOut = {},
            onDeleteAccount = {},
            onToggleMorningBriefing = {},
            onToggleDeadlineAlerts = {},
            onShowTimePicker = {},
            onShowLeadTimePicker = {},
            onUpgrade = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 840, heightDp = 600)
@Composable
fun ProfileMediumScreenPreview() {
    RemindlyTheme {
        ProfileScreenContent(
            windowWidthSizeClass = WindowWidthSizeClass.Medium,
            uiState = ProfileUiState(
                userName = "Victor Kirui",
                userEmail = "victor@example.com"
            ),
            onSignOut = {},
            onDeleteAccount = {},
            onToggleMorningBriefing = {},
            onToggleDeadlineAlerts = {},
            onShowTimePicker = {},
            onShowLeadTimePicker = {},
            onUpgrade = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 1200, heightDp = 800)
@Composable
fun ProfileLargeScreenPreview() {
    RemindlyTheme {
        ProfileScreenContent(
            windowWidthSizeClass = WindowWidthSizeClass.Expanded,
            uiState = ProfileUiState(
                userName = "Victor Kirui",
                userEmail = "victor@example.com"
            ),
            onSignOut = {},
            onDeleteAccount = {},
            onToggleMorningBriefing = {},
            onToggleDeadlineAlerts = {},
            onShowTimePicker = {},
            onShowLeadTimePicker = {},
            onUpgrade = {}
        )
    }
}
