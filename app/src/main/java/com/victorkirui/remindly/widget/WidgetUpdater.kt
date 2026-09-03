package com.victorkirui.remindly.widget

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.glance.appwidget.updateAll

object WidgetUpdater {

    fun setWidgetsEnabled(context: Context, enabled: Boolean) {
        try {
            val pm = context.packageManager
            val receivers = listOf(
                "com.victorkirui.remindly.widget.SmallCalendarWidgetReceiver",
                "com.victorkirui.remindly.widget.MediumCalendarWidgetReceiver",
                "com.victorkirui.remindly.widget.LargeCalendarWidgetReceiver",
                "com.victorkirui.remindly.widget.SmallUpcomingWidgetReceiver",
                "com.victorkirui.remindly.widget.MediumUpcomingWidgetReceiver",
                "com.victorkirui.remindly.widget.LargeUpcomingWidgetReceiver"
            )

            val newState = if (enabled) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }

            receivers.forEach { receiverClassName ->
                val component = ComponentName(context.packageName, receiverClassName)
                if (pm.getComponentEnabledSetting(component) != newState) {
                    pm.setComponentEnabledSetting(
                        component,
                        newState,
                        PackageManager.DONT_KILL_APP
                    )
                    android.util.Log.d("WidgetUpdater", "Set widget $receiverClassName enabled state to: $enabled")
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("WidgetUpdater", "Failed to toggle widget component state: ${e.message}")
        }
    }

    suspend fun updateWidgets(context: Context) {
        try {
            val isPro = WidgetUtils.isUserPro(context)
            setWidgetsEnabled(context, isPro)

            if (isPro) {
                android.util.Log.d("WidgetUpdater", "User is Pro. Updating all 6 Glance widgets...")
                SmallCalendarWidget().updateAll(context)
                MediumCalendarWidget().updateAll(context)
                LargeCalendarWidget().updateAll(context)
                SmallUpcomingWidget().updateAll(context)
                MediumUpcomingWidget().updateAll(context)
                LargeUpcomingWidget().updateAll(context)
                android.util.Log.d("WidgetUpdater", "All 6 Glance widgets updated successfully.")
            } else {
                android.util.Log.d("WidgetUpdater", "User is Free. Disabled widget components from Android launcher.")
            }
        } catch (e: Exception) {
            android.util.Log.w("WidgetUpdater", "Failed to update Glance widgets: ${e.message}", e)
        }
    }
}
