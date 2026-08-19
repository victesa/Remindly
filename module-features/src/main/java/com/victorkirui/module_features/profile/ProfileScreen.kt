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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.victorkirui.core.ui.theme.*
import com.victorkirui.core.R
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    windowWidthSizeClass: WindowWidthSizeClass,
    onSignOut: () -> Unit = {},
    viewModel: ProfileViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    var showTimePicker by remember { mutableStateOf(false) }
    val timePickerState = rememberTimePickerState(
        initialHour = uiState.preferredReminderTime.split(":")[0].toIntOrNull() ?: 8,
        initialMinute = uiState.preferredReminderTime.split(":")[1].take(2).toIntOrNull() ?: 0
    )

    if (showTimePicker) {
        TimePickerDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val hour = if (timePickerState.hour < 10) "0${timePickerState.hour}" else "${timePickerState.hour}"
                    val minute = if (timePickerState.minute < 10) "0${timePickerState.minute}" else "${timePickerState.minute}"
                    viewModel.updateReminderTime("$hour:$minute")
                    showTimePicker = false
                }) { Text("Confirm") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
            }
        ) {
            TimePicker(state = timePickerState)
        }
    }

    ProfileScreenContent(
        windowWidthSizeClass = windowWidthSizeClass,
        uiState = uiState,
        onSignOut = onSignOut,
        onToggleMorningBriefing = viewModel::toggleMorningBriefing,
        onToggleDeadlineAlerts = viewModel::toggleDeadlineAlerts,
        onShowTimePicker = { showTimePicker = true }
    )
}

@Composable
fun ProfileScreenContent(
    windowWidthSizeClass: WindowWidthSizeClass,
    uiState: ProfileUiState,
    onSignOut: () -> Unit,
    onToggleMorningBriefing: (Boolean) -> Unit,
    onToggleDeadlineAlerts: (Boolean) -> Unit,
    onShowTimePicker: () -> Unit
) {
    Scaffold(
        containerColor = Color.White
    ) { paddingValues ->
        when (windowWidthSizeClass) {
            WindowWidthSizeClass.Medium -> ProfileMediumContent(
                state = uiState,
                modifier = Modifier.padding(paddingValues),
                onSignOut = onSignOut,
                onToggleMorningBriefing = onToggleMorningBriefing,
                onToggleDeadlineAlerts = onToggleDeadlineAlerts,
                onShowTimePicker = onShowTimePicker
            )
            WindowWidthSizeClass.Expanded -> ProfileLargeContent(
                state = uiState,
                modifier = Modifier.padding(paddingValues),
                onSignOut = onSignOut,
                onToggleMorningBriefing = onToggleMorningBriefing,
                onToggleDeadlineAlerts = onToggleDeadlineAlerts,
                onShowTimePicker = onShowTimePicker
            )
            else -> ProfileSmallContent(
                state = uiState,
                modifier = Modifier.padding(paddingValues),
                onSignOut = onSignOut,
                onToggleMorningBriefing = onToggleMorningBriefing,
                onToggleDeadlineAlerts = onToggleDeadlineAlerts,
                onShowTimePicker = onShowTimePicker
            )
        }
    }
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
    onToggleMorningBriefing: (Boolean) -> Unit,
    onToggleDeadlineAlerts: (Boolean) -> Unit,
    onShowTimePicker: () -> Unit
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
        
        Spacer(modifier = Modifier.height(48.dp))
        
        ProfileSectionHeader("NOTIFICATIONS")
        NotificationsCard(
            state = state,
            onToggleMorningBriefing = onToggleMorningBriefing,
            onToggleDeadlineAlerts = onToggleDeadlineAlerts,
            onShowTimePicker = onShowTimePicker
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        ProfileSectionHeader("GENERAL")
        GeneralCard()

        Spacer(modifier = Modifier.height(32.dp))

        ProfileSectionHeader("FEEDBACK & RATING")
        SupportCard()
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Sign Out",
                color = Color(0xFFC0392B),
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                modifier = Modifier.clickable { onSignOut() }
            )
        }
    }
}

@Composable
fun ProfileMediumContent(
    state: ProfileUiState,
    modifier: Modifier = Modifier,
    onSignOut: () -> Unit,
    onToggleMorningBriefing: (Boolean) -> Unit,
    onToggleDeadlineAlerts: (Boolean) -> Unit,
    onShowTimePicker: () -> Unit
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
            
            Spacer(modifier = Modifier.height(32.dp))
            
            ProfileSectionHeader("GENERAL")
            GeneralCardMedium()
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
                onShowTimePicker = onShowTimePicker
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            ProfileSectionHeader("FEEDBACK & RATING")
            SupportCardMedium()
            
            Spacer(modifier = Modifier.height(40.dp))
            
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Sign Out",
                    color = Color(0xFFC0392B),
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp,
                    modifier = Modifier.clickable { onSignOut() }
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
    onToggleMorningBriefing: (Boolean) -> Unit,
    onToggleDeadlineAlerts: (Boolean) -> Unit,
    onShowTimePicker: () -> Unit
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
            
            Spacer(modifier = Modifier.height(48.dp))
            
            Spacer(modifier = Modifier.weight(1f))
            
            Text(
                "Sign Out",
                color = Color(0xFFC0392B),
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                modifier = Modifier.clickable { onSignOut() }
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
                onShowTimePicker = onShowTimePicker
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    ProfileSectionHeader("GENERAL")
                    GeneralCardLarge()
                }
                
                Column(modifier = Modifier.weight(1f)) {
                    ProfileSectionHeader("FEEDBACK & RATING")
                    SupportCardLarge()
                }
            }
        }
    }
}

@Composable
fun NotificationsCard(
    state: ProfileUiState,
    onToggleMorningBriefing: (Boolean) -> Unit,
    onToggleDeadlineAlerts: (Boolean) -> Unit,
    onShowTimePicker: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFFF9FAF9)
    ) {
        Column(modifier = Modifier.padding(vertical = 12.dp)) {
            NotificationItem(
                title = "Morning Briefing",
                subtitle = "Daily summary at 8:00 AM",
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
                value = state.preferredReminderTime,
                onClick = onShowTimePicker
            )
        }
    }
}

@Composable
fun NotificationsCardMedium(
    state: ProfileUiState,
    onToggleMorningBriefing: (Boolean) -> Unit,
    onToggleDeadlineAlerts: (Boolean) -> Unit,
    onShowTimePicker: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFFF9FAF9)
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            NotificationItemMedium(
                title = "Morning Briefing",
                subtitle = "Daily summary at 8:00 AM",
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
                value = state.preferredReminderTime,
                icon = Icons.Outlined.AccessTime,
                onClick = onShowTimePicker
            )
        }
    }
}

@Composable
fun NotificationsCardLarge(
    state: ProfileUiState,
    onToggleMorningBriefing: (Boolean) -> Unit,
    onToggleDeadlineAlerts: (Boolean) -> Unit,
    onShowTimePicker: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFFF9FAF9)
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            NotificationItemMedium(
                title = "Morning Briefing",
                subtitle = "Daily summary at 8:00 AM",
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
                value = state.preferredReminderTime,
                icon = Icons.Outlined.AccessTime,
                onClick = onShowTimePicker
            )
        }
    }
}

@Composable
fun GeneralCardLarge() {
    val context = androidx.compose.ui.platform.LocalContext.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFFF9FAF9)
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            SettingItemMedium(
                title = "Privacy & Data",
                value = null,
                icon = Icons.Outlined.Shield,
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("Https://victor-kirui.dev/remindly/privacy-policy"))
                    context.startActivity(intent)
                }
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFFF0F0F0))
            SettingItemMedium(
                title = "About Remindly",
                value = "v1.0.0",
                icon = Icons.Outlined.Info
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
                        putExtra(Intent.EXTRA_EMAIL, arrayOf("victorkirui.dev@gmail.com"))
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
            Text(
                name.take(1).uppercase(),
                color = Color.White,
                fontSize = 36.sp,
                fontWeight = FontWeight.Normal
            )
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
            Text(
                name.take(1).uppercase(),
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Medium
            )
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
fun GeneralCard() {
    val context = androidx.compose.ui.platform.LocalContext.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFFF9FAF9)
    ) {
        Column(modifier = Modifier.padding(vertical = 12.dp)) {
            SettingItem(
                title = "Privacy & Data",
                value = null,
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("Https://victor-kirui.dev/remindly/privacy-policy"))
                    context.startActivity(intent)
                }
            )
            SettingItem(
                title = "About Remindly",
                value = "Version 1.0.0"
            )
        }
    }
}

@Composable
fun GeneralCardMedium() {
    val context = androidx.compose.ui.platform.LocalContext.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFFF9FAF9)
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            SettingItemMedium(
                title = "Privacy & Data",
                value = null,
                icon = Icons.Outlined.Shield,
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("Https://victor-kirui.dev/remindly/privacy-policy"))
                    context.startActivity(intent)
                }
            )
            SettingItemMedium(
                title = "About Remindly",
                value = "v1.0.0",
                icon = Icons.Outlined.Info
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
                        putExtra(Intent.EXTRA_EMAIL, arrayOf("victorkirui.dev@gmail.com"))
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
                value = "Email victorkirui.dev@gmail.com",
                onClick = {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:")
                        putExtra(Intent.EXTRA_EMAIL, arrayOf("victorkirui.dev@gmail.com"))
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
            onToggleMorningBriefing = {},
            onToggleDeadlineAlerts = {},
            onShowTimePicker = {}
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
            onToggleMorningBriefing = {},
            onToggleDeadlineAlerts = {},
            onShowTimePicker = {}
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
            onToggleMorningBriefing = {},
            onToggleDeadlineAlerts = {},
            onShowTimePicker = {}
        )
    }
}
