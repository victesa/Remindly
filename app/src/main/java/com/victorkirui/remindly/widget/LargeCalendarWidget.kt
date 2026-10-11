@file:Suppress("RestrictedApi")

package com.victorkirui.remindly.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.*
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
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

class LargeCalendarWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val today = LocalDate.now()
        val dayDotsMap = mutableMapOf<LocalDate, DotType>()

        val isPro = WidgetUtils.isUserPro(context)

        if (isPro) {
            try {
                withContext(Dispatchers.IO) {
                    withTimeoutOrNull(3000L) {
                        val db = WidgetDatabaseHelper.getDatabase(context)

                        val activeItems = db.itemDao().getAllActiveItemsSync()

                        activeItems.forEach { item ->
                            val dateStr = item.deadline ?: item.eventDate
                            if (dateStr != null) {
                                try {
                                    val itemDate = com.victorkirui.core.util.DateUtils.parseToLocalDate(dateStr)

                                    val isUrgent = item.deadline?.let { deadline ->
                                        try {
                                            val deadlineDate = com.victorkirui.core.util.DateUtils.parseToLocalDate(deadline)
                                            deadlineDate != null && ChronoUnit.DAYS.between(today, deadlineDate) <= 1
                                        } catch (_: Exception) { false }
                                    } ?: false

                                    if (itemDate != null) {
                                        val currentDot = dayDotsMap[itemDate]
                                        if (isUrgent) {
                                            dayDotsMap[itemDate] = DotType.RED
                                        } else if (currentDot != DotType.RED) {
                                            dayDotsMap[itemDate] = DotType.GREEN
                                        }
                                    }
                                } catch (_: Exception) {}
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.w("LargeCalendarWidget", "Error fetching Room data: ${e.message}")
            }
        }

        provideContent {
            if (!isPro) {
                ProWidgetPlaceholder("Large Calendar Widget")
            } else {
                val prefs = currentState<Preferences>()
                val monthOffset = prefs[MONTH_OFFSET_KEY] ?: 0

                val targetMonth = today.plusMonths(monthOffset.toLong())
                val monthName = targetMonth.month.getDisplayName(DateTextStyle.FULL, Locale.getDefault())
                val year = targetMonth.year
                val headerTitle = "$monthName $year"

                LargeCalendarWidgetContent(
                    headerTitle = headerTitle,
                    targetMonth = targetMonth,
                    today = today,
                    dayDotsMap = dayDotsMap
                )
            }
        }
    }

    @Composable
    private fun LargeCalendarWidgetContent(
        headerTitle: String,
        targetMonth: LocalDate,
        today: LocalDate,
        dayDotsMap: Map<LocalDate, DotType>
    ) {
        val firstDayOfMonth = targetMonth.withDayOfMonth(1)
        val firstDayOfWeek = firstDayOfMonth.dayOfWeek.value // 1 (Mon) .. 7 (Sun)
        val daysInMonth = targetMonth.lengthOfMonth()
        val prevMonthDays = targetMonth.minusMonths(1).lengthOfMonth()

        // Generate 6 weeks calendar grid
        val weeks = remember(targetMonth) {
            val grid = mutableListOf<List<CalendarCellData>>()
            var currentDay = 1 - (firstDayOfWeek - 1)

            for (weekIndex in 0..5) {
                val week = mutableListOf<CalendarCellData>()
                for (colIndex in 0..6) {
                    val (dayNumber, isCurrentMonth, cellDate) = when {
                        currentDay < 1 -> {
                            val actualDay = prevMonthDays + currentDay
                            val date = targetMonth.minusMonths(1).withDayOfMonth(actualDay)
                            Triple(actualDay, false, date)
                        }
                        currentDay > daysInMonth -> {
                            val actualDay = currentDay - daysInMonth
                            val date = targetMonth.plusMonths(1).withDayOfMonth(actualDay)
                            Triple(actualDay, false, date)
                        }
                        else -> {
                            val date = targetMonth.withDayOfMonth(currentDay)
                            Triple(currentDay, true, date)
                        }
                    }

                    week.add(
                        CalendarCellData(
                            dayNumber = dayNumber,
                            isCurrentMonth = isCurrentMonth,
                            date = cellDate,
                            isToday = cellDate == today,
                            dotType = dayDotsMap[cellDate] ?: DotType.NONE
                        )
                    )
                    currentDay++
                }
                grid.add(week)
            }
            grid
        }

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
            // --- TOP HEADER ROW: Month Year + Controls ---
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = headerTitle,
                    style = TextStyle(
                        color = ColorProvider(R.color.widget_dark_text),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                )

                Spacer(modifier = GlanceModifier.defaultWeight())

                // Navigation Controls (< >)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = GlanceModifier
                            .size(28.dp)
                            .clickable(actionRunCallback<ChangeMonthAction>(actionParametersOf(MONTH_DELTA_PARAM to -1))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "<",
                            style = TextStyle(
                                color = ColorProvider(R.color.widget_gray_label),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    Spacer(modifier = GlanceModifier.width(8.dp))

                    Box(
                        modifier = GlanceModifier
                            .size(28.dp)
                            .clickable(actionRunCallback<ChangeMonthAction>(actionParametersOf(MONTH_DELTA_PARAM to 1))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = ">",
                            style = TextStyle(
                                color = ColorProvider(R.color.widget_gray_label),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }

            Spacer(modifier = GlanceModifier.height(10.dp))

            // --- DAY OF WEEK HEADERS (Mon - Sun) ---
            val dayHeaders = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                dayHeaders.forEach { day ->
                    Column(
                        modifier = GlanceModifier.defaultWeight(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = day,
                            style = TextStyle(
                                color = ColorProvider(R.color.widget_gray_label),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }
            }

            Spacer(modifier = GlanceModifier.height(6.dp))

            // --- MONTH CALENDAR GRID (6 WEEKS) ---
            weeks.forEach { week ->
                Row(
                    modifier = GlanceModifier.fillMaxWidth().padding(vertical = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    week.forEach { cell ->
                        Column(
                            modifier = GlanceModifier
                                .defaultWeight()
                                .clickable(
                                    actionStartActivity<MainActivity>(
                                        actionParametersOf(WidgetIntentConstants.PARAM_SELECTED_DATE to cell.date.toString())
                                    )
                                ),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Date Number Box
                            Box(
                                modifier = GlanceModifier
                                    .size(28.dp)
                                    .background(
                                        if (cell.isToday) R.color.widget_green else android.R.color.transparent
                                    )
                                    .cornerRadius(14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = cell.dayNumber.toString(),
                                    style = TextStyle(
                                        color = ColorProvider(
                                            when {
                                                cell.isToday -> R.color.widget_bg_white
                                                !cell.isCurrentMonth -> R.color.widget_gray_label
                                                else -> R.color.widget_dark_text
                                            }
                                        ),
                                        fontSize = 13.sp,
                                        fontWeight = if (cell.isToday) FontWeight.Bold else FontWeight.Medium
                                    )
                                )
                            }

                            // Status Indicator Dot
                            Box(
                                modifier = GlanceModifier
                                    .size(4.dp)
                                    .padding(top = 1.dp)
                                    .background(
                                        when (cell.dotType) {
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
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                )

                Spacer(modifier = GlanceModifier.defaultWeight())

                // + Capture Pill Button (Launches app with capture sheet active)
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

    companion object {
        val MONTH_OFFSET_KEY = intPreferencesKey("large_widget_month_offset")
        val MONTH_DELTA_PARAM = ActionParameters.Key<Int>("month_delta_param")
    }
}

class ChangeMonthAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val delta = parameters[LargeCalendarWidget.MONTH_DELTA_PARAM] ?: 0
        updateAppWidgetState(context, glanceId) { prefs ->
            val currentOffset = prefs[LargeCalendarWidget.MONTH_OFFSET_KEY] ?: 0
            val newOffset = (currentOffset + delta).coerceIn(-12, 12)
            prefs[LargeCalendarWidget.MONTH_OFFSET_KEY] = newOffset
        }
        LargeCalendarWidget().update(context, glanceId)
    }
}

private data class CalendarCellData(
    val dayNumber: Int,
    val isCurrentMonth: Boolean,
    val date: LocalDate,
    val isToday: Boolean,
    val dotType: DotType
)
