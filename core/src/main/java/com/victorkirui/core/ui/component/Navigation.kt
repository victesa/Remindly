package com.victorkirui.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun RemindlyNavigationRail(
    currentScreen: String,
    onHomeClick: () -> Unit = {},
    onInboxClick: () -> Unit = {},
    onRemindersClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onAddClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .width(80.dp)
            .fillMaxHeight()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Logo
        Text(
            "R",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black,
            color = Color(0xFF2D6A4F),
            modifier = Modifier.padding(bottom = 48.dp)
        )

        NavigationRailItem(
            icon = { Icon(if (currentScreen == "Home") Icons.Filled.Home else Icons.Outlined.Home, "Home") },
            label = "Home",
            selected = currentScreen == "Home",
            onClick = onHomeClick
        )
        NavigationRailItem(
            icon = { Icon(if (currentScreen == "Inbox") Icons.Filled.Inbox else Icons.Outlined.Inbox, "Inbox") },
            label = "Inbox",
            selected = currentScreen == "Inbox",
            onClick = onInboxClick
        )
        NavigationRailItem(
            icon = { Icon(if (currentScreen == "Reminders") Icons.Filled.Notifications else Icons.Outlined.Notifications, "Reminders") },
            label = "Reminders",
            selected = currentScreen == "Reminders",
            onClick = onRemindersClick
        )
        NavigationRailItem(
            icon = { Icon(if (currentScreen == "Profile") Icons.Filled.Person else Icons.Outlined.Person, "Profile") },
            label = "Profile",
            selected = currentScreen == "Profile",
            onClick = onProfileClick
        )

        Spacer(modifier = Modifier.weight(1f))

        // FAB
        FloatingActionButton(
            onClick = onAddClick,
            containerColor = Color(0xFF2D6A4F),
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(Icons.Default.Add, "Add")
        }
    }
}

@Composable
fun NavigationRailItem(
    icon: @Composable () -> Unit,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (selected) Color(0xFFEAF2EE) else Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            CompositionLocalProvider(
                LocalContentColor provides if (selected) Color(0xFF2D6A4F) else Color.Gray
            ) {
                icon()
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) Color(0xFF2D6A4F) else Color.Gray,
            fontSize = 11.sp
        )
    }
}

@Composable
fun RemindlyBottomNavigation(
    currentScreen: String,
    onHomeClick: () -> Unit = {},
    onInboxClick: () -> Unit = {},
    onRemindersClick: () -> Unit = {},
    onProfileClick: () -> Unit = {}
) {
    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 0.dp, // Reduced elevation to minimize "gap" effect if any
        windowInsets = WindowInsets.navigationBars // Ensure it handles system nav bar insets
    ) {
        val items = listOf(
            BottomNavItem("Home", Icons.Outlined.Home, Icons.Filled.Home, onHomeClick),
            BottomNavItem("Inbox", Icons.Outlined.Inbox, Icons.Filled.Inbox, onInboxClick),
            BottomNavItem("Reminders", Icons.Outlined.Notifications, Icons.Filled.Notifications, onRemindersClick),
            BottomNavItem("Profile", Icons.Outlined.Person, Icons.Filled.Person, onProfileClick)
        )

        items.forEach { item ->
            val isSelected = currentScreen == item.label
            NavigationBarItem(
                selected = isSelected,
                onClick = item.onClick,
                icon = {
                    if (isSelected) {
                        Surface(
                            color = Color(0xFFEAF2EE),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(item.selectedIcon, contentDescription = item.label, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
                        }
                    } else {
                        Icon(item.unselectedIcon, contentDescription = item.label)
                    }
                },
                label = { Text(item.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFF2D6A4F),
                    unselectedIconColor = Color.Gray,
                    selectedTextColor = Color(0xFF2D6A4F),
                    unselectedTextColor = Color.Gray,
                    indicatorColor = Color.Transparent
                )
            )
        }
    }
}

data class BottomNavItem(
    val label: String,
    val unselectedIcon: ImageVector,
    val selectedIcon: ImageVector,
    val onClick: () -> Unit
)
