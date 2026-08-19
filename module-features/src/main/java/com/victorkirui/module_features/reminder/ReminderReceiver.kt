package com.victorkirui.module_features.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.victorkirui.core.notification.NotificationHelper
import com.victorkirui.local.repository.LocalRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ReminderReceiver : BroadcastReceiver(), KoinComponent {
    private val notificationHelper: NotificationHelper by inject()
    private val localRepository: LocalRepository by inject()

    override fun onReceive(context: Context, intent: Intent) {
        val itemId = intent.getStringExtra("item_id") ?: return
        val type = intent.getStringExtra("type") ?: "REMINDER"

        Log.d("ReminderReceiver", "Received reminder for itemId: $itemId, type: $type")

        CoroutineScope(Dispatchers.IO).launch {
            val item = localRepository.getItem(itemId)
            if (item != null) {
                // Ensure item is not DONE
                if (item.status == "DONE") {
                    Log.d("ReminderReceiver", "Skipping notification for DONE item: $itemId")
                    return@launch
                }

                val title = when (type) {
                    "DEADLINE_APPROACHING" -> "Deadline Approaching!"
                    "MORNING_OF" -> "Action Needed Today"
                    "7_DAYS_BEFORE" -> "Upcoming Deadline (7 days)"
                    "2_DAYS_BEFORE" -> "Upcoming Deadline (2 days)"
                    "TEST_ALARM" -> "Test Reminder"
                    else -> "Remindly Reminder"
                }
                notificationHelper.showNotification(title, item.title)
            }
        }
    }
}
