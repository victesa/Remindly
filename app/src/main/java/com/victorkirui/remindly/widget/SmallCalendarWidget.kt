@file:Suppress("RestrictedApi")

package com.victorkirui.remindly.widget

import android.content.ComponentName
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
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
import java.util.Locale

class SmallCalendarWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val today = LocalDate.now()
        val monthName = today.month.getDisplayName(DateTextStyle.FULL, Locale.getDefault())
        val dayOfMonth = today.dayOfMonth

        val isPro = WidgetUtils.isUserPro(context)
        var todayCount = 0

        if (isPro) {
            try {
                withContext(Dispatchers.IO) {
                    withTimeoutOrNull(3000L) {
                        val db = WidgetDatabaseHelper.getDatabase(context)
                        todayCount = db.itemDao().getTodayItemsCount(today.toString())
                    }
                }
            } catch (e: Exception) {
                android.util.Log.w("SmallCalendarWidget", "Error fetching Room data: ${e.message}")
            }
        }

        val subtitleText = when (todayCount) {
            0 -> "No items today"
            1 -> "1 item today"
            else -> "$todayCount items today"
        }

        provideContent {
            if (!isPro) {
                ProWidgetPlaceholder("Calendar Widget")
            } else {
                SmallCalendarWidgetContent(
                    monthName = monthName,
                    dayOfMonth = dayOfMonth,
                    subtitleText = subtitleText
                )
            }
        }
    }

    @Composable
    private fun SmallCalendarWidgetContent(
        monthName: String,
        dayOfMonth: Int,
        subtitleText: String
    ) {
        val context = LocalContext.current
        val componentName = ComponentName(context, MainActivity::class.java)

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(R.color.widget_bg_white)
                .cornerRadius(24.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .clickable(actionStartActivity(componentName)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Header Row (Icon + Month)
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    provider = ImageProvider(R.drawable.ic_calendar_widget),
                    contentDescription = null,
                    modifier = GlanceModifier.size(16.dp)
                )
                Spacer(modifier = GlanceModifier.width(6.dp))
                Text(
                    text = monthName,
                    style = TextStyle(
                        color = ColorProvider(R.color.widget_green),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Spacer(modifier = GlanceModifier.height(4.dp))

            // Main Day Number
            Text(
                text = dayOfMonth.toString(),
                style = TextStyle(
                    color = ColorProvider(R.color.widget_green),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
            )

            Spacer(modifier = GlanceModifier.height(2.dp))

            // Subtitle
            Text(
                text = subtitleText,
                style = TextStyle(
                    color = ColorProvider(R.color.widget_subtext_slate),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            )
        }
    }
}
