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

class MediumUpcomingWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val today = LocalDate.now()
        val itemsList = mutableListOf<UpcomingWidgetItemData>()

        val isPro = WidgetUtils.isUserPro(context)

        if (isPro) {
            try {
                withContext(Dispatchers.IO) {
                    withTimeoutOrNull(3000L) {
                        val db = WidgetDatabaseHelper.getDatabase(context)

                        val upcomingItems = db.itemDao().getUpcomingItemsSync()

                        // Find top 3 upcoming deadlines sorted by date
                        upcomingItems
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
                            .take(3)
                            .forEach { (item, date) ->
                                val daysLeft = ChronoUnit.DAYS.between(today, date)
                                val dayOfMonth = date.dayOfMonth.toString()
                                val monthShort = date.month.getDisplayName(DateTextStyle.SHORT, Locale.getDefault()).uppercase()

                                val badgeText = when {
                                    daysLeft < 0 -> "Overdue"
                                    daysLeft == 0L -> "Today"
                                    daysLeft == 1L -> "1 day"
                                    else -> "$daysLeft days"
                                }
                                val isRedBadge = daysLeft <= 3

                                itemsList.add(
                                    UpcomingWidgetItemData(
                                        id = item.id,
                                        title = item.title,
                                        dayOfMonth = dayOfMonth,
                                        monthShort = monthShort,
                                        badgeText = badgeText,
                                        isRedBadge = isRedBadge
                                    )
                                )
                            }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.w("MediumUpcomingWidget", "Error fetching Room data: ${e.message}")
            }
        }

        provideContent {
            if (!isPro) {
                ProWidgetPlaceholder("Medium Upcoming Widget")
            } else {
                MediumUpcomingWidgetContent(
                    itemsList = itemsList,
                    today = today
                )
            }
        }
    }

    @Composable
    private fun MediumUpcomingWidgetContent(
        itemsList: List<UpcomingWidgetItemData>,
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
            // --- TOP HEADER ROW: Icon + Upcoming Title + See All ---
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_notification),
                        contentDescription = null,
                        modifier = GlanceModifier.size(16.dp)
                    )
                    Spacer(modifier = GlanceModifier.width(6.dp))
                    Text(
                        text = "Upcoming",
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

            Spacer(modifier = GlanceModifier.height(8.dp))

            // --- UPCOMING DEADLINE ROWS (TOP 3) ---
            if (itemsList.isEmpty()) {
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
            } else {
                Column(
                    modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    itemsList.forEach { item ->
                        Row(
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                                .clickable(
                                    actionStartActivity<MainActivity>(
                                        actionParametersOf(WidgetIntentConstants.PARAM_ITEM_ID to item.id)
                                    )
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Date Column (24 / JUN)
                            Column(
                                modifier = GlanceModifier.width(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = item.dayOfMonth,
                                    style = TextStyle(
                                        color = ColorProvider(R.color.widget_dark_text),
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

                            // Item Title (Spotify Interview)
                            Text(
                                text = item.title,
                                style = TextStyle(
                                    color = ColorProvider(R.color.widget_dark_text),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                maxLines = 1,
                                modifier = GlanceModifier.defaultWeight()
                            )

                            Spacer(modifier = GlanceModifier.width(8.dp))

                            // Urgency Badge Pill (1 day / 8 days)
                            Box(
                                modifier = GlanceModifier
                                    .background(
                                        if (item.isRedBadge) R.color.widget_red_urgent else R.color.widget_green_light
                                    )
                                    .cornerRadius(10.dp)
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = item.badgeText,
                                    style = TextStyle(
                                        color = ColorProvider(
                                            if (item.isRedBadge) R.color.widget_bg_white else R.color.widget_green
                                        ),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class UpcomingWidgetItemData(
    val id: String,
    val title: String,
    val dayOfMonth: String,
    val monthShort: String,
    val badgeText: String,
    val isRedBadge: Boolean
)
