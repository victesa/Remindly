@file:Suppress("RestrictedApi")

package com.victorkirui.remindly.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.victorkirui.core.R
import com.victorkirui.remindly.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.time.LocalDate
import java.time.format.TextStyle as DateTextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

class LargeUpcomingWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val today = LocalDate.now()
        val urgentList = mutableListOf<LargeUpcomingItemData>()
        val regularList = mutableListOf<LargeUpcomingItemData>()

        val isPro = WidgetUtils.isUserPro(context)

        if (isPro) {
            try {
                withContext(Dispatchers.IO) {
                    withTimeoutOrNull(3000L) {
                        val db = WidgetDatabaseHelper.getDatabase(context)

                        val upcomingItems = db.itemDao().getUpcomingItemsSync()

                        val sortedItems = upcomingItems
                            .mapNotNull { item ->
                                val dateStr = item.deadline ?: item.eventDate
                                if (dateStr != null) {
                                    try {
                                        val date = if (dateStr.contains("T")) {
                                            java.time.LocalDateTime.parse(dateStr.substringBefore("Z")).toLocalDate()
                                        } else {
                                            LocalDate.parse(dateStr)
                                        }
                                        Pair(item, date)
                                    } catch (_: Exception) { null }
                                } else null
                            }
                            .filter { (_, date) -> !date.isBefore(today) }
                            .sortedBy { (_, date) -> date }

                        sortedItems.forEach { (item, date) ->
                            val daysLeft = ChronoUnit.DAYS.between(today, date)
                            val dayOfMonth = date.dayOfMonth.toString()
                            val monthShort = date.month.getDisplayName(DateTextStyle.SHORT, Locale.getDefault()).uppercase()

                            val subtitle = item.category ?: item.source ?: "Reminder"

                            val badgeText = when {
                                daysLeft < 0 -> "Overdue"
                                daysLeft == 0L -> "Today"
                                daysLeft == 1L -> "1 day left"
                                else -> "$daysLeft days left"
                            }

                            val itemData = LargeUpcomingItemData(
                                id = item.id,
                                title = item.title,
                                subtitle = subtitle,
                                dayOfMonth = dayOfMonth,
                                monthShort = monthShort,
                                badgeText = badgeText,
                                daysLeft = daysLeft
                            )

                            if (daysLeft <= 2 && urgentList.size < 2) {
                                urgentList.add(itemData)
                            } else if (regularList.size < 3) {
                                regularList.add(itemData)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.w("LargeUpcomingWidget", "Error fetching Room data: ${e.message}")
            }
        }

        provideContent {
            if (!isPro) {
                ProWidgetPlaceholder("Large Upcoming Widget")
            } else {
                LargeUpcomingWidgetContent(
                    urgentList = urgentList,
                    regularList = regularList,
                    today = today
                )
            }
        }
    }

    @Composable
    private fun LargeUpcomingWidgetContent(
        urgentList: List<LargeUpcomingItemData>,
        regularList: List<LargeUpcomingItemData>,
        today: LocalDate
    ) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(R.color.widget_bg_white)
                .cornerRadius(28.dp)
                .padding(16.dp)
                .clickable(
                    actionStartActivity<MainActivity>(
                        actionParametersOf(
                            WidgetIntentConstants.PARAM_SELECTED_DATE to today.toString()
                        )
                    )
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // --- TOP HEADER ROW: Icon + Upcoming Deadlines + See All ---
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_notification),
                        contentDescription = null,
                        modifier = GlanceModifier.size(18.dp)
                    )
                    Spacer(modifier = GlanceModifier.width(6.dp))
                    Text(
                        text = "Upcoming Deadlines",
                        style = TextStyle(
                            color = ColorProvider(R.color.widget_dark_text),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                Spacer(modifier = GlanceModifier.defaultWeight())

                // "See all" action link
                Text(
                    text = "See all",
                    style = TextStyle(
                        color = ColorProvider(R.color.widget_green),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    modifier = GlanceModifier.clickable(
                        actionStartActivity<MainActivity>(
                            actionParametersOf(
                                WidgetIntentConstants.PARAM_SELECTED_DATE to today.toString()
                            )
                        )
                    )
                )
            }

            Spacer(modifier = GlanceModifier.height(10.dp))

            // --- SECTION 1: URGENT ---
            if (urgentList.isNotEmpty()) {
                Text(
                    text = "URGENT",
                    style = TextStyle(
                        color = ColorProvider(R.color.widget_red_urgent),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                )

                Spacer(modifier = GlanceModifier.height(4.dp))

                urgentList.forEach { item ->
                    Box(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .background(R.color.widget_red_light)
                            .cornerRadius(16.dp)
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .clickable(
                                actionStartActivity<MainActivity>(
                                    actionParametersOf(WidgetIntentConstants.PARAM_ITEM_ID to item.id)
                                )
                            )
                    ) {
                        Row(
                            modifier = GlanceModifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Date Column (Red)
                            Column(
                                modifier = GlanceModifier.width(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = item.dayOfMonth,
                                    style = TextStyle(
                                        color = ColorProvider(R.color.widget_red_urgent),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Text(
                                    text = item.monthShort,
                                    style = TextStyle(
                                        color = ColorProvider(R.color.widget_red_urgent),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }

                            Spacer(modifier = GlanceModifier.width(12.dp))

                            // Center Info Column (Title + Category)
                            Column(
                                modifier = GlanceModifier.defaultWeight()
                            ) {
                                Text(
                                    text = item.title,
                                    style = TextStyle(
                                        color = ColorProvider(R.color.widget_dark_text),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    maxLines = 1
                                )
                                Text(
                                    text = item.subtitle,
                                    style = TextStyle(
                                        color = ColorProvider(R.color.widget_gray_label),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    maxLines = 1
                                )
                            }

                            Spacer(modifier = GlanceModifier.width(8.dp))

                            // Urgent Badge (Solid Red Pill)
                            Box(
                                modifier = GlanceModifier
                                    .background(R.color.widget_red_urgent)
                                    .cornerRadius(10.dp)
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = item.badgeText,
                                    style = TextStyle(
                                        color = ColorProvider(R.color.widget_bg_white),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = GlanceModifier.height(8.dp))
            }

            // --- SECTION 2: UPCOMING ---
            if (regularList.isNotEmpty()) {
                Text(
                    text = "UPCOMING",
                    style = TextStyle(
                        color = ColorProvider(R.color.widget_gray_label),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                )

                Spacer(modifier = GlanceModifier.height(4.dp))

                regularList.forEach { item ->
                    Row(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable(
                                actionStartActivity<MainActivity>(
                                    actionParametersOf(WidgetIntentConstants.PARAM_ITEM_ID to item.id)
                                )
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Date Column (Green / Gray)
                        Column(
                            modifier = GlanceModifier.width(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = item.dayOfMonth,
                                style = TextStyle(
                                    color = ColorProvider(R.color.widget_green),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                text = item.monthShort,
                                style = TextStyle(
                                    color = ColorProvider(R.color.widget_gray_label),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }

                        Spacer(modifier = GlanceModifier.width(12.dp))

                        // Center Info Column (Title + Category)
                        Column(
                            modifier = GlanceModifier.defaultWeight()
                        ) {
                            Text(
                                text = item.title,
                                style = TextStyle(
                                    color = ColorProvider(R.color.widget_dark_text),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                maxLines = 1
                            )
                            Text(
                                text = item.subtitle,
                                style = TextStyle(
                                    color = ColorProvider(R.color.widget_gray_label),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                maxLines = 1
                            )
                        }

                        Spacer(modifier = GlanceModifier.width(8.dp))

                        // Regular Badge (Soft Green Pill)
                        Box(
                            modifier = GlanceModifier
                                .background(R.color.widget_green_light)
                                .cornerRadius(10.dp)
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = item.badgeText,
                                style = TextStyle(
                                    color = ColorProvider(R.color.widget_green),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }
            }

            if (urgentList.isEmpty() && regularList.isEmpty()) {
                Box(
                    modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No upcoming deadlines. You're ahead!",
                        style = TextStyle(
                            color = ColorProvider(R.color.widget_gray_label),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }

            Spacer(modifier = GlanceModifier.defaultWeight())

            // --- BOTTOM ROW: Branding + Capture Button ---
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Remindly",
                    style = TextStyle(
                        color = ColorProvider(R.color.widget_green),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                )

                Spacer(modifier = GlanceModifier.defaultWeight())

                // + Capture Pill Button
                Box(
                    modifier = GlanceModifier
                        .background(R.color.widget_green)
                        .cornerRadius(14.dp)
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                        .clickable(
                            actionStartActivity<MainActivity>(
                                actionParametersOf(WidgetIntentConstants.PARAM_OPEN_CAPTURE to true)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "+ Capture",
                        style = TextStyle(
                            color = ColorProvider(R.color.widget_bg_white),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    }
}

private data class LargeUpcomingItemData(
    val id: String,
    val title: String,
    val subtitle: String,
    val dayOfMonth: String,
    val monthShort: String,
    val badgeText: String,
    val daysLeft: Long
)
