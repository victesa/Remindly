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
import com.victorkirui.local.entity.Item
import com.victorkirui.remindly.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class SmallUpcomingWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val today = LocalDate.now()
        var nextItem: Item? = null
        var daysLeft: Long = 0

        val isPro = WidgetUtils.isUserPro(context)

        if (isPro) {
            try {
                withContext(Dispatchers.IO) {
                    withTimeoutOrNull(3000L) {
                        val db = WidgetDatabaseHelper.getDatabase(context)

                        val upcomingItems = db.itemDao().getUpcomingItemsSync()

                        // Find next upcoming deadline sorted by date
                        nextItem = upcomingItems
                            .mapNotNull { item ->
                                val dateStr = item.deadline ?: item.eventDate
                                if (dateStr != null) {
                                    val date = com.victorkirui.core.util.DateUtils.parseToLocalDate(dateStr)
                                    if (date != null) Pair(item, date) else null
                                } else null
                            }
                            .filter { (_, date) -> !date.isBefore(today) }
                            .minByOrNull { (_, date) -> date }
                            ?.let { (item, date) ->
                                daysLeft = ChronoUnit.DAYS.between(today, date)
                                item
                            }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.w("SmallUpcomingWidget", "Error fetching Room data: ${e.message}")
            }
        }

        val title = nextItem?.title ?: "No upcoming deadlines"
        val badgeText = when {
            nextItem == null -> "All clear"
            daysLeft < 0 -> "Overdue"
            daysLeft == 0L -> "Today"
            daysLeft == 1L -> "1 day left"
            else -> "$daysLeft days left"
        }
        val isGreenBadge = nextItem == null || daysLeft > 3

        provideContent {
            if (!isPro) {
                ProWidgetPlaceholder("Upcoming Widget")
            } else {
                SmallUpcomingWidgetContent(
                    title = title,
                    badgeText = badgeText,
                    isGreenBadge = isGreenBadge,
                    itemId = nextItem?.id
                )
            }
        }
    }

    @Composable
    private fun SmallUpcomingWidgetContent(
        title: String,
        badgeText: String,
        isGreenBadge: Boolean,
        itemId: String?
    ) {
        val clickAction = if (itemId != null) {
            actionStartActivity<MainActivity>(
                actionParametersOf(WidgetIntentConstants.PARAM_ITEM_ID to itemId)
            )
        } else {
            actionStartActivity<MainActivity>()
        }

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(R.color.widget_bg_white)
                .cornerRadius(24.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .clickable(clickAction),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Header Row (Bell Icon + Next deadline)
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    provider = ImageProvider(R.drawable.ic_notification),
                    contentDescription = null,
                    modifier = GlanceModifier.size(16.dp)
                )
                Spacer(modifier = GlanceModifier.width(6.dp))
                Text(
                    text = "Next deadline",
                    style = TextStyle(
                        color = ColorProvider(R.color.widget_green),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
            }

            Spacer(modifier = GlanceModifier.height(6.dp))

            // Main Title (e.g. "Spotify Interview")
            Text(
                text = title,
                style = TextStyle(
                    color = ColorProvider(R.color.widget_dark_text),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 1
            )

            Spacer(modifier = GlanceModifier.height(8.dp))

            // Urgency Pill Badge (e.g. "1 day left")
            Box(
                modifier = GlanceModifier
                    .background(if (isGreenBadge) R.color.widget_green else R.color.widget_red_urgent)
                    .cornerRadius(10.dp)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = badgeText,
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
