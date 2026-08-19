package com.victorkirui.module_features.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.victorkirui.core.repository.ReminderSettingsRepository
import com.victorkirui.local.entity.Item
import com.victorkirui.local.entity.Reminder
import com.victorkirui.local.repository.LocalRepository
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
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    suspend fun scheduleRemindersForItem(item: Item) {
        val targetDateStr = item.deadline ?: item.eventDate ?: return
        val targetDate = try {
            if (targetDateStr.contains("T")) {
                if (targetDateStr.endsWith("Z")) {
                    java.time.Instant.parse(targetDateStr).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                } else {
                    LocalDateTime.parse(targetDateStr).toLocalDate()
                }
            } else if (targetDateStr.contains("-")) {
                LocalDate.parse(targetDateStr, dateFormatter)
            } else {
                return
            }
        } catch (e: Exception) {
            android.util.Log.e("ReminderScheduler", "Failed to parse date: $targetDateStr", e)
            return
        }
        val today = LocalDate.now()

        if (targetDate.isBefore(today)) {
            android.util.Log.d("ReminderScheduler", "Skipping reminders for past item: ${item.id}")
            return
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

        // Always add Morning Of reminder
        reminders.add(createReminder(item.id, targetDate, preferredTime, "MORNING_OF"))

        // --- TEST MODE ---
        if (item.id.startsWith("test_item")) {
            val testTime = LocalDateTime.now().plusSeconds(10)
            reminders.add(Reminder(
                itemId = item.id,
                reminderDateTime = testTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                type = "TEST_ALARM"
            ))
        }

        // Send a reminder an hour before if deadline is approaching
        if (item.deadline != null && settingsRepository.isDeadlineAlertsEnabled.first()) {
            val deadlineDateTime = LocalDateTime.of(targetDate, LocalTime.of(21, 0)) // Assuming 9pm as deadline time if not specified
            val alertTime = deadlineDateTime.minusHours(1)
            if (alertTime.isAfter(LocalDateTime.now())) {
                reminders.add(Reminder(
                    itemId = item.id,
                    reminderDateTime = alertTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    type = "DEADLINE_APPROACHING"
                ))
            }
        }

        // Save and Schedule Alarms
        localRepository.withTransaction {
            localRepository.deleteRemindersForItem(item.id)
            val distinctReminders = reminders.distinctBy { it.reminderDateTime }
            localRepository.saveReminders(distinctReminders)
            
            distinctReminders.forEach { scheduleAlarm(it) }
        }
    }

    private fun scheduleAlarm(reminder: Reminder) {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("item_id", reminder.itemId)
            putExtra("type", reminder.type)
            // Ensure unique intent for each reminder
            action = "com.victorkirui.remindly.ACTION_REMINDER_${reminder.itemId}_${reminder.type}"
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.hashCode(),
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

        if (triggerAt > System.currentTimeMillis()) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAt,
                pendingIntent
            )
            android.util.Log.d("ReminderScheduler", "Scheduled alarm for ${reminder.itemId} at ${reminder.reminderDateTime}")
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
