package com.victorkirui.module_features.profile

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.victorkirui.core.R

data class WidgetGalleryItem(
    val id: String,
    val title: String,
    val category: String,
    val sizeLabel: String,
    val description: String,
    val receiverClassName: String,
    val previewType: WidgetPreviewType
)

enum class WidgetPreviewType {
    SMALL_CALENDAR,
    MEDIUM_CALENDAR,
    LARGE_CALENDAR,
    SMALL_UPCOMING,
    MEDIUM_UPCOMING,
    LARGE_UPCOMING
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetsGallerySheet(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedFilter by remember { mutableStateOf("ALL") }

    val allWidgets = remember {
        listOf(
            WidgetGalleryItem(
                id = "small_cal",
                title = "Small Calendar",
                category = "CALENDAR",
                sizeLabel = "2 x 1",
                description = "Shows today's month, date number, and active task count at a glance.",
                receiverClassName = "com.victorkirui.remindly.widget.SmallCalendarWidgetReceiver",
                previewType = WidgetPreviewType.SMALL_CALENDAR
            ),
            WidgetGalleryItem(
                id = "medium_cal",
                title = "Medium Calendar Strip",
                category = "CALENDAR",
                sizeLabel = "4 x 2",
                description = "Current month header, 7-day week strip with status dots, and quick +Capture button.",
                receiverClassName = "com.victorkirui.remindly.widget.MediumCalendarWidgetReceiver",
                previewType = WidgetPreviewType.MEDIUM_CALENDAR
            ),
            WidgetGalleryItem(
                id = "large_cal",
                title = "Large Month Grid",
                category = "CALENDAR",
                sizeLabel = "4 x 4",
                description = "Full interactive month calendar with < > month navigation controls and status dots.",
                receiverClassName = "com.victorkirui.remindly.widget.LargeCalendarWidgetReceiver",
                previewType = WidgetPreviewType.LARGE_CALENDAR
            ),
            WidgetGalleryItem(
                id = "small_up",
                title = "Small Next Deadline",
                category = "DEADLINES",
                sizeLabel = "2 x 1",
                description = "Displays the single next upcoming deadline title and days remaining urgency badge.",
                receiverClassName = "com.victorkirui.remindly.widget.SmallUpcomingWidgetReceiver",
                previewType = WidgetPreviewType.SMALL_UPCOMING
            ),
            WidgetGalleryItem(
                id = "medium_up",
                title = "Medium Upcoming List",
                category = "DEADLINES",
                sizeLabel = "4 x 2",
                description = "Top 3 upcoming deadlines with date columns, urgency badges, and See All link.",
                receiverClassName = "com.victorkirui.remindly.widget.MediumUpcomingWidgetReceiver",
                previewType = WidgetPreviewType.MEDIUM_UPCOMING
            ),
            WidgetGalleryItem(
                id = "large_up",
                title = "Large Categorized Deadlines",
                category = "DEADLINES",
                sizeLabel = "4 x 4",
                description = "Full categorized Urgent and Upcoming deadline cards with categories, badges & +Capture button.",
                receiverClassName = "com.victorkirui.remindly.widget.LargeUpcomingWidgetReceiver",
                previewType = WidgetPreviewType.LARGE_UPCOMING
            )
        )
    }

    val filteredWidgets = remember(selectedFilter) {
        if (selectedFilter == "ALL") allWidgets else allWidgets.filter { it.category == selectedFilter }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.White,
        contentColor = Color.Black,
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.LightGray) },
        modifier = Modifier.fillMaxHeight(0.92f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Widgets,
                        contentDescription = null,
                        tint = Color(0xFF2D6A4F),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            "Pro Widgets",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Text(
                            "6 Home Screen Widgets Available",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Category Filter Pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChipPill("All Widgets", selected = selectedFilter == "ALL") { selectedFilter = "ALL" }
                FilterChipPill("Calendar", selected = selectedFilter == "CALENDAR") { selectedFilter = "CALENDAR" }
                FilterChipPill("Deadlines", selected = selectedFilter == "DEADLINES") { selectedFilter = "DEADLINES" }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Widget Gallery Cards List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                items(filteredWidgets) { item ->
                    WidgetGalleryCard(
                        item = item,
                        onAddWidget = {
                            pinWidgetToHomeScreen(context, item.receiverClassName)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterChipPill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = if (selected) Color(0xFF2D6A4F) else Color(0xFFF4F6F4),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            color = if (selected) Color.White else Color.Black,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun WidgetGalleryCard(
    item: WidgetGalleryItem,
    onAddWidget: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFFF9FAF9),
        border = BorderStroke(1.dp, Color(0xFFEEEEEE))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Card Title + Size Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    item.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.Black
                )

                Surface(
                    color = Color(0xFF2D6A4F),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        item.sizeLabel,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                item.description,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Widget Live Preview Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F172A), shape = RoundedCornerShape(20.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                WidgetPreviewRender(item.previewType)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Add Widget Action Button
            Button(
                onClick = onAddWidget,
                modifier = Modifier.fillMaxWidth().height(44.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2D6A4F))
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add to Home Screen", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun WidgetPreviewRender(type: WidgetPreviewType) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            when (type) {
                WidgetPreviewType.SMALL_CALENDAR -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CalendarToday, contentDescription = null, tint = Color(0xFF2D6A4F), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("June", color = Color(0xFF2D6A4F), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("23", color = Color(0xFF2D6A4F), fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Text("2 items today", color = Color(0xFF3A506B), fontSize = 11.sp)
                }
                WidgetPreviewType.MEDIUM_CALENDAR -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("June 2025", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Surface(color = Color(0xFF2D6A4F), shape = RoundedCornerShape(8.dp)) {
                            Text("2 items today", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        listOf("Mon" to "21", "Tue" to "22", "Wed" to "23", "Thu" to "24", "Fri" to "25", "Sat" to "26", "Sun" to "27").forEach { (day, num) ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(day, fontSize = 10.sp, color = Color.Gray)
                                Surface(
                                    color = if (num == "23") Color(0xFF2D6A4F) else Color.Transparent,
                                    shape = CircleShape,
                                    modifier = Modifier.size(22.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(num, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (num == "23") Color.White else Color.Black)
                                    }
                                }
                            }
                        }
                    }
                }
                WidgetPreviewType.LARGE_CALENDAR -> {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("June 2025", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("<  >", fontWeight = FontWeight.Bold, color = Color.Gray, fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        listOf("M", "T", "W", "T", "F", "S", "S").forEach {
                            Text(it, fontSize = 10.sp, color = Color.Gray, modifier = Modifier.width(18.dp), textAlign = TextAlign.Center)
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("• Full interactive month grid with dots", fontSize = 11.sp, color = Color.Gray)
                }
                WidgetPreviewType.SMALL_UPCOMING -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.NotificationsNone, contentDescription = null, tint = Color(0xFF2D6A4F), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Next deadline", color = Color(0xFF2D6A4F), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Spotify Interview", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(color = Color(0xFFC0392B), shape = RoundedCornerShape(8.dp)) {
                        Text("1 day left", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                    }
                }
                WidgetPreviewType.MEDIUM_UPCOMING -> {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Upcoming", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("See all", color = Color(0xFF2D6A4F), fontSize = 11.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("24\nJUN", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Spotify Interview", fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Surface(color = Color(0xFFC0392B), shape = RoundedCornerShape(6.dp)) {
                            Text("1 day", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                }
                WidgetPreviewType.LARGE_UPCOMING -> {
                    Text("Upcoming Deadlines", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("URGENT", color = Color(0xFFC0392B), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Surface(color = Color(0xFFFFF5F5), shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("24 JUN", fontSize = 10.sp, color = Color(0xFFC0392B), fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Spotify Interview", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Surface(color = Color(0xFFC0392B), shape = RoundedCornerShape(6.dp)) {
                                Text("1 day left", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun pinWidgetToHomeScreen(context: Context, receiverClassName: String) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context.packageName, receiverClassName)
            if (appWidgetManager.isRequestPinAppWidgetSupported) {
                appWidgetManager.requestPinAppWidget(component, null, null)
            } else {
                Toast.makeText(context, "Long-press your home screen -> Widgets -> Remindly to add!", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(context, "Long-press your home screen -> Widgets -> Remindly to add!", Toast.LENGTH_LONG).show()
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Long-press your home screen -> Widgets -> Remindly to add!", Toast.LENGTH_LONG).show()
    }
}
