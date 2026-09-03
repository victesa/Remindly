package com.victorkirui.module_features.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.victorkirui.core.repository.ReminderSettingsRepository
import com.victorkirui.local.entity.Item
import com.victorkirui.local.entity.Reminder
import com.victorkirui.local.repository.LocalRepository
import com.victorkirui.core.util.DateUtils
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class ReminderScheduler(
    private val context: Context,
    private val localRepository: LocalRepository,
    private val settingsRepository: ReminderSettingsRepository
) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    suspend fun scheduleRemindersForItem(item: Item) {
        if (item.status == "DONE") {
            android.util.Log.d("ReminderScheduler", "Skipping schedule for DONE item: ${item.id}")
            return
        }

        // 1. Check if already past deadline
        val targetDateStr = item.deadline ?: item.eventDate
        if (targetDateStr != null) {
            val deadline = DateUtils.parseToDateTime(targetDateStr)
            val now = LocalDateTime.now()
            if (deadline.isBefore(now)) {
                android.util.Log.d("ReminderScheduler", "Item ${item.id} is already past deadline ($targetDateStr). Deleting.")
                localRepository.deleteItem(item.userId, item.id)
                return
            }
        }

        val reminders = calculateReminders(item)
        updateAlarms(item.id, reminders)
    }

    suspend fun updateAlarms(itemId: String, reminders: List<Reminder>) {
        localRepository.withTransaction {
            // 1. Cancel existing alarms
            val existingReminders = localRepository.getRemindersForItem(itemId).first()
            existingReminders.forEach { cancelAlarm(it) }

            // 2. Clear old reminders from DB
            localRepository.deleteRemindersForItem(itemId)

            // 3. Save new reminders and get their real IDs
            val distinctReminders = reminders.distinctBy { it.reminderDateTime + it.type }
            val ids = localRepository.saveReminders(distinctReminders)
            
            // 4. Schedule new alarms using real IDs
            distinctReminders.zip(ids).forEach { (reminder, id) ->
                scheduleAlarm(reminder.copy(id = id))
            }
        }
    }

    fun scheduleDailyBriefing(preferredTime: LocalTime) {
        val now = LocalDateTime.now()
        var briefingDateTime = LocalDateTime.of(now.toLocalDate(), preferredTime)
        
        if (briefingDateTime.isBefore(now)) {
            briefingDateTime = briefingDateTime.plusDays(1)
        }

        val reminder = Reminder(
            id = 1001, // Fixed ID for briefing
            itemId = "system_briefing",
            reminderDateTime = briefingDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            type = "MORNING_BRIEF"
        )
        
        scheduleAlarm(reminder)
        android.util.Log.d("ReminderScheduler", "Scheduled daily briefing for $briefingDateTime")
    }

    suspend fun calculateReminders(item: Item): List<Reminder> {
        val targetDateStr = item.deadline ?: item.eventDate ?: return emptyList()
        val targetDateTime = DateUtils.parseToDateTime(targetDateStr)
        val targetDate = targetDateTime.toLocalDate()

        val today = LocalDate.now()
        if (targetDate.isBefore(today)) {
            return emptyList()
        }

        val preferredTimeStr = settingsRepository.preferredReminderTime.first()
        val preferredTime = try {
            LocalTime.parse(preferredTimeStr)
        } catch (e: Exception) {
            LocalTime.of(8, 0)
        }

        val daysUntil = ChronoUnit.DAYS.between(today, targetDate)
        val reminders = mutableListOf<Reminder>()

        // Schedule based on interval logic
        when {
            daysUntil <= 7 -> {
                for (i in 1..daysUntil) {
                    reminders.add(createReminder(item.id, today.plusDays(i), preferredTime, "DAILY"))
                }
            }
            daysUntil <= 14 -> {
                var current = today.plusDays(2)
                while (current.isBefore(targetDate)) {
                    reminders.add(createReminder(item.id, current, preferredTime, "INTERVAL"))
                    current = current.plusDays(2)
                }
                val dayBefore = targetDate.minusDays(1)
                if (reminders.none { it.reminderDateTime.startsWith(dayBefore.toString()) }) {
                    reminders.add(createReminder(item.id, dayBefore, preferredTime, "INTERVAL"))
                }
            }
            daysUntil <= 30 -> {
                var current = today.plusDays(7)
                while (current.isBefore(targetDate)) {
                    reminders.add(createReminder(item.id, current, preferredTime, "WEEKLY"))
                    current = current.plusWeeks(1)
                }
                reminders.add(createReminder(item.id, targetDate.minusDays(3), preferredTime, "INTERVAL"))
                reminders.add(createReminder(item.id, targetDate.minusDays(1), preferredTime, "INTERVAL"))
            }
            else -> {
                var current = today.plusWeeks(2)
                while (current.isBefore(targetDate.minusWeeks(2))) {
                    reminders.add(createReminder(item.id, current, preferredTime, "BI_WEEKLY"))
                    current = current.plusWeeks(2)
                }
                reminders.add(createReminder(item.id, targetDate.minusDays(14), preferredTime, "INTERVAL"))
                reminders.add(createReminder(item.id, targetDate.minusDays(7), preferredTime, "INTERVAL"))
                reminders.add(createReminder(item.id, targetDate.minusDays(3), preferredTime, "INTERVAL"))
                reminders.add(createReminder(item.id, targetDate.minusDays(1), preferredTime, "INTERVAL"))
            }
        }

        // --- TEST MODE ---
        if (item.id.startsWith("test_item")) {
            val testTime = LocalDateTime.now().plusSeconds(10)
            reminders.add(Reminder(
                itemId = item.id,
                reminderDateTime = testTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                type = "TEST_ALARM"
            ))
        }

        // Send a reminder before the deadline
        if (settingsRepository.isDeadlineAlertsEnabled.first()) {
            val finalDeadlineTime = targetDateTime
            val now = LocalDateTime.now()
            
            // Set alert based on configurable lead time (default 1 hour)
            val leadTimeHours = settingsRepository.deadlineAlertLeadTimeHours.first()
            val alertTime = finalDeadlineTime.minusHours(leadTimeHours.toLong())
            
            if (alertTime.isAfter(now)) {
                reminders.add(Reminder(
                    itemId = item.id,
                    reminderDateTime = alertTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    type = "DEADLINE_APPROACHING"
                ))
                android.util.Log.d("ReminderScheduler", "Scheduled approaching alert for ${item.id} at $alertTime")
            } else if (finalDeadlineTime.isAfter(now)) {
                // CATCH-UP LOGIC:
                // If the regular alert time has already passed, but the deadline is still in the future,
                // schedule a nudge for 10 seconds from now to ensure the user gets notified immediately after capture/edit.
                val catchUpTime = now.plusSeconds(10)
                reminders.add(Reminder(
                    itemId = item.id,
                    reminderDateTime = catchUpTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    type = "DEADLINE_APPROACHING"
                ))
                android.util.Log.d("ReminderScheduler", "Scheduled immediate catch-up alert for ${item.id} at $catchUpTime")
            }

            // --- AUTO-CLEANUP ALARM ---
            // Set an alarm for the exact deadline time to mark it as passed/delete it
            if (finalDeadlineTime.isAfter(now)) {
                reminders.add(Reminder(
                    itemId = item.id,
                    reminderDateTime = finalDeadlineTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    type = "DEADLINE_EXPIRY"
                ))
            }
        }
        return reminders
    }

    private fun cancelAlarm(reminder: Reminder) {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = "com.victorkirui.remindly.ACTION_REMINDER_${reminder.itemId}_${reminder.type}"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.id.toInt(), // Use stable ID
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        android.util.Log.d("ReminderScheduler", "Cancelled alarm for ${reminder.itemId} at ${reminder.reminderDateTime} (ID: ${reminder.id})")
    }

    private fun scheduleAlarm(reminder: Reminder) {
        // Check for exact alarm permission on Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                android.util.Log.e("ReminderScheduler", "Cannot schedule exact alarms: permission not granted")
                return
            }
        }

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("item_id", reminder.itemId)
            putExtra("type", reminder.type)
            action = "com.victorkirui.remindly.ACTION_REMINDER_${reminder.itemId}_${reminder.type}"
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.id.toInt(), // Use stable ID
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerAt = try {
            LocalDateTime.parse(reminder.reminderDateTime)
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        } catch (e: Exception) {
            android.util.Log.e("ReminderScheduler", "Failed to parse trigger time: ${reminder.reminderDateTime}", e)
            return
        }

        val now = System.currentTimeMillis()
        if (triggerAt > now) {
            try {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAt,
                    pendingIntent
                )
                android.util.Log.d("ReminderScheduler", "Scheduled alarm for ${reminder.itemId} at ${reminder.reminderDateTime} (ID: ${reminder.id}) in ${(triggerAt - now) / 1000}s")
            } catch (e: SecurityException) {
                android.util.Log.e("ReminderScheduler", "SecurityException scheduling exact alarm", e)
            }
        } else {
            android.util.Log.w("ReminderScheduler", "Skipping past alarm for ${reminder.itemId} at ${reminder.reminderDateTime}")
        }
    }

    private fun createReminder(itemId: String, date: LocalDate, time: LocalTime, type: String): Reminder {
        val dateTime = LocalDateTime.of(date, time)
        return Reminder(
            itemId = itemId,
            reminderDateTime = dateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            type = type
        )
    }
}
