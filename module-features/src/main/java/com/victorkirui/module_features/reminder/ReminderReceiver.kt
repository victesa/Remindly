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
        val allReminders = localRepository.getAllRemindersWithItems(userId).first()
        
        val itemsToday = allReminders.filter {
            try {
                val reminderDate = LocalDateTime.parse(it.reminder.reminderDateTime).toLocalDate()
                reminderDate.isEqual(today) && it.item.status != "DONE"
            } catch (e: Exception) {
                it.reminder.reminderDateTime.startsWith(today.toString()) && it.item.status != "DONE"
            }
        }.map { it.item }.distinctBy { it.id }

        if (itemsToday.isNotEmpty()) {
            val count = itemsToday.size
            val (title, message) = if (count == 1) {
                "Today's Goal" to "You have one item needing your attention today: ${itemsToday.first().title}. You've got this!"
            } else {
                "Daily Briefing" to "You have $count things lined up for today. Try to work on them before their deadlines."
            }
            notificationHelper.showNotification(title, message, notificationId = 1001)
        } else {
            val engagementMessages = listOf(
                "Your schedule is clear! It’s a great time to explore new opportunities.",
                "No deadlines today. Why not take a few minutes to organize your upcoming goals?",
                "A clear day ahead! Keep sharing interesting captures with Remindly.",
                "Nothing on the list for today. Spend some time searching for your next big thing."
            )
            notificationHelper.showNotification("Plan Your Day", engagementMessages.random(), notificationId = 1001)
        }
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
