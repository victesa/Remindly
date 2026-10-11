@file:Suppress("RestrictedApi")

package com.victorkirui.remindly.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
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
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle as DateTextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

class MediumCalendarWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val today = LocalDate.now()
        val monthName = today.month.getDisplayName(DateTextStyle.FULL, Locale.getDefault())
        val year = today.year
        val headerTitle = "$monthName $year"

        val monday = today.with(DayOfWeek.MONDAY)
        val weekDays = (0..6).map { monday.plusDays(it.toLong()) }

        val isPro = WidgetUtils.isUserPro(context)
        var todayCount = 0
        val dayDotsMap = mutableMapOf<LocalDate, DotType>()

        if (isPro) {
            try {
                withContext(Dispatchers.IO) {
                    withTimeoutOrNull(3000L) {
                        val db = WidgetDatabaseHelper.getDatabase(context)

                        todayCount = db.itemDao().getTodayItemsCount(today.toString())
                        val activeItems = db.itemDao().getAllActiveItemsSync()

                        weekDays.forEach { date ->
                            val itemsForDay = activeItems.filter { item ->
                                val itemDate = item.deadline ?: item.eventDate
                                itemDate != null && itemDate.startsWith(date.toString())
                            }

                            if (itemsForDay.isNotEmpty()) {
                                val hasUrgent = itemsForDay.any { item ->
                                    val deadline = item.deadline
                                    if (deadline != null) {
                                        try {
                                            val itemLocalDate = com.victorkirui.core.util.DateUtils.parseToLocalDate(deadline)
                                            itemLocalDate != null && ChronoUnit.DAYS.between(today, itemLocalDate) <= 1
                                        } catch (_: Exception) { false }
                                    } else false
                                }

                                dayDotsMap[date] = if (hasUrgent) DotType.RED else DotType.GREEN
                            } else {
                                dayDotsMap[date] = DotType.NONE
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.w("MediumCalendarWidget", "Error fetching Room data: ${e.message}")
            }
        }

        val todayCountText = when (todayCount) {
            0 -> "No items today"
            1 -> "1 item today"
            else -> "$todayCount items today"
        }

        provideContent {
            if (!isPro) {
                ProWidgetPlaceholder("Medium Calendar Widget")
            } else {
                MediumCalendarWidgetContent(
                    headerTitle = headerTitle,
                    todayCountText = todayCountText,
                    weekDays = weekDays,
                    today = today,
                    dayDotsMap = dayDotsMap
                )
            }
        }
    }

    @Composable
    private fun MediumCalendarWidgetContent(
        headerTitle: String,
        todayCountText: String,
        weekDays: List<LocalDate>,
        today: LocalDate,
        dayDotsMap: Map<LocalDate, DotType>
    ) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(R.color.widget_bg_white)
                .cornerRadius(28.dp)
                .padding(16.dp)
                .clickable(actionStartActivity<MainActivity>()),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // --- TOP ROW: Month Year + Today Badge ---
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = headerTitle,
                    style = TextStyle(
                        color = ColorProvider(R.color.widget_dark_text),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                )

                Spacer(modifier = GlanceModifier.defaultWeight())

                // Today Count Badge (Green Pill)
                Box(
                    modifier = GlanceModifier
                        .background(R.color.widget_green)
                        .cornerRadius(12.dp)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = todayCountText,
                        style = TextStyle(
                            color = ColorProvider(R.color.widget_bg_white),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            Spacer(modifier = GlanceModifier.height(10.dp))

            // --- MIDDLE ROW: 7-Day Calendar Strip ---
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                weekDays.forEach { date ->
                    val isToday = date == today
                    val dayName = date.dayOfWeek.getDisplayName(DateTextStyle.SHORT, Locale.getDefault())
                    val dotType = dayDotsMap[date] ?: DotType.NONE

                    Column(
                        modifier = GlanceModifier
                            .defaultWeight()
                            .clickable(
                                actionStartActivity<MainActivity>(
                                    actionParametersOf(WidgetIntentConstants.PARAM_SELECTED_DATE to date.toString())
                                )
                            ),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Day Label (Mon, Tue, etc.)
                        Text(
                            text = dayName,
                            style = TextStyle(
                                color = ColorProvider(R.color.widget_gray_label),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )

                        Spacer(modifier = GlanceModifier.height(4.dp))

                        // Day Number Box (Green Circle if Today)
                        Box(
                            modifier = GlanceModifier
                                .size(28.dp)
                                .background(if (isToday) R.color.widget_green else android.R.color.transparent)
                                .cornerRadius(14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = date.dayOfMonth.toString(),
                                style = TextStyle(
                                    color = ColorProvider(if (isToday) R.color.widget_bg_white else R.color.widget_dark_text),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }

                        Spacer(modifier = GlanceModifier.height(4.dp))

                        // Status Dot (Green / Red / None)
                        Box(
                            modifier = GlanceModifier
                                .size(4.dp)
                                .background(
                                    when (dotType) {
                                        DotType.GREEN -> R.color.widget_green
                                        DotType.RED -> R.color.widget_red_urgent
                                        DotType.NONE -> android.R.color.transparent
                                    }
                                )
                                .cornerRadius(2.dp)
                        ) {}
                    }
                }
            }

            Spacer(modifier = GlanceModifier.height(10.dp))

            // --- BOTTOM ROW: Branding + Capture Button ---
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Remindly",
                    style = TextStyle(
                        color = ColorProvider(R.color.widget_green),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                )

                Spacer(modifier = GlanceModifier.defaultWeight())

                // + Capture Pill Button (Launches app with capture sheet active)
                Box(
                    modifier = GlanceModifier
                        .background(R.color.widget_green)
                        .cornerRadius(14.dp)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
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

enum class DotType {
    NONE, GREEN, RED
}
