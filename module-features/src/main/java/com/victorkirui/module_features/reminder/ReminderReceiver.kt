package com.victorkirui.module_features.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.victorkirui.core.notification.NotificationHelper
import com.victorkirui.core.repository.AuthRepository
import com.victorkirui.core.repository.ReminderSettingsRepository
import com.victorkirui.local.repository.LocalRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class ReminderReceiver : BroadcastReceiver(), KoinComponent {
    private val notificationHelper: NotificationHelper by inject()
    private val localRepository: LocalRepository by inject()
    private val authRepository: AuthRepository by inject()
    private val settingsRepository: ReminderSettingsRepository by inject()
    private val reminderScheduler: ReminderScheduler by inject()

    override fun onReceive(context: Context, intent: Intent) {
        val itemId = intent.getStringExtra("item_id") ?: ""
        val type = intent.getStringExtra("type") ?: "REMINDER"

        Log.d("ReminderReceiver", "onReceive: itemId=$itemId, type=$type")

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (type == "MORNING_BRIEF") {
                    handleMorningBriefing()
                    // Reschedule for tomorrow
                    val preferredTimeStr = settingsRepository.preferredReminderTime.first()
                    val preferredTime = LocalTime.parse(preferredTimeStr)
                    reminderScheduler.scheduleDailyBriefing(preferredTime)
                } else if (itemId.isNotEmpty()) {
                    handleItemReminder(itemId, type)
                }
            } catch (e: Exception) {
                Log.e("ReminderReceiver", "Error in onReceive", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun handleMorningBriefing() {
        val userId = authRepository.currentUser.value?.uid ?: return
        val isEnabled = settingsRepository.isMorningBriefingEnabled.first()
        if (!isEnabled) return

        val today = LocalDate.now()
        val allItems = localRepository.getAllItems(userId).first().filter {
            it.status != "DONE" && it.status != "FAILED"
        }

        val pendingCount = allItems.size
        val dueTodayItems = allItems.filter { item ->
            val deadlineDate = com.victorkirui.core.util.DateUtils.parseToLocalDate(item.deadline)
            val eventDate = com.victorkirui.core.util.DateUtils.parseToLocalDate(item.eventDate)
            (deadlineDate?.isEqual(today) == true) || (eventDate?.isEqual(today) == true)
        }
        val dueTodayCount = dueTodayItems.size

        val (title, message) = when {
            dueTodayCount > 0 -> {
                val dueText = if (dueTodayCount == 1) {
                    "1 item due today: \"${dueTodayItems.first().title}\""
                } else {
                    "$dueTodayCount items due today"
                }
                val pendingText = if (pendingCount == dueTodayCount) {
                    ""
                } else {
                    " ($pendingCount total pending)"
                }
                "Daily Briefing" to "$dueText$pendingText. Stay on top of your goals!"
            }
            pendingCount > 0 -> {
                val pendingStr = if (pendingCount == 1) "1 pending item" else "$pendingCount pending items"
                "Daily Briefing" to "No deadlines due today. You have $pendingStr in your pipeline."
            }
            else -> {
                val engagementMessages = listOf(
                    "Your schedule is clear today! Great time to capture new opportunities.",
                    "No pending items or deadlines today. Enjoy your day!",
                    "All caught up! Keep capturing interesting notes and events with Remindly.",
                    "A clear day ahead! Explore new goals and opportunities."
                )
                "Plan Your Day" to engagementMessages.random()
            }
        }

        notificationHelper.showNotification(title, message, notificationId = 1001)
    }

    private suspend fun handleItemReminder(itemId: String, type: String) {
        val item = localRepository.getItem(itemId) ?: return
        
        if (type == "DEADLINE_EXPIRY") {
            Log.d("ReminderReceiver", "Deadline expired for $itemId. Deleting.")
            localRepository.deleteItem(item.userId, itemId)
            return
        }

        if (item.status == "DONE") return

        val title = when (type) {
            "DEADLINE_APPROACHING" -> "Deadline Approaching!"
            "DAILY" -> "Daily Task Reminder"
            "INTERVAL" -> "Upcoming Task"
            "WEEKLY" -> "Weekly Reminder"
            "BI_WEEKLY" -> "Bi-Weekly Reminder"
            "TEST_ALARM" -> "Test Reminder"
            else -> "Remindly Reminder"
        }
        
        val notificationId = itemId.hashCode() + type.hashCode()
        notificationHelper.showNotification(title, item.title, itemId, notificationId)
    }
}
