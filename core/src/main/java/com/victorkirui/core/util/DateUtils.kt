package com.victorkirui.core.util

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

object DateUtils {
    fun getTimeAgo(dateTimeStr: String?): String {
        if (dateTimeStr.isNullOrEmpty()) return "Recently"
        return try {
            val past = Instant.parse(dateTimeStr).atZone(ZoneId.systemDefault()).toLocalDateTime()
            val now = LocalDateTime.now()
            
            val minutes = ChronoUnit.MINUTES.between(past, now)
            val hours = ChronoUnit.HOURS.between(past, now)
            val days = ChronoUnit.DAYS.between(past, now)

            when {
                minutes < 1 -> "Just now"
                minutes < 60 -> "${minutes}m ago"
                hours < 24 -> "${hours}h ago"
                days < 7 -> "${days}d ago"
                else -> past.format(DateTimeFormatter.ofPattern("MMM dd"))
            }
        } catch (e: Exception) {
            "Recently"
        }
    }

    fun getDeadlineText(deadline: String?): String {
        if (deadline == null) return ""
        return try {
            val date = java.time.LocalDate.parse(deadline)
            val today = java.time.LocalDate.now()
            val days = ChronoUnit.DAYS.between(today, date)
            when {
                days == 0L -> "Deadline Today"
                days == 1L -> "Deadline in 1 day"
                days in 2..7 -> "Deadline in $days days"
                days < 0 -> "Overdue"
                else -> "Deadline: ${date.format(DateTimeFormatter.ofPattern("MMM dd"))}"
            }
        } catch (e: Exception) {
            ""
        }
    }
}
