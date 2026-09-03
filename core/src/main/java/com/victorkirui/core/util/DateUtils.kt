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
            val past = parseToDateTime(dateTimeStr)
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
            val date = parseToDateTime(deadline).toLocalDate()
            val today = java.time.LocalDate.now()
            val days = ChronoUnit.DAYS.between(today, date)
            when {
                days == 0L -> "Deadline Today"
                days == 1L -> "Deadline in 1 day"
                days in 2..7 -> "Deadline in $days days"
                days < 0 -> "Overdue"
                else -> {
                    val formatter = DateTimeFormatter.ofPattern("MMM dd")
                    "Deadline: ${date.format(formatter)}"
                }
            }
        } catch (e: Exception) {
            ""
        }
    }

    fun getReadableDeadline(deadline: String?): String {
        if (deadline == null) return "No deadline"
        return try {
            val dateTime = parseToDateTime(deadline)
            val date = dateTime.toLocalDate()
            val today = java.time.LocalDate.now()
            val days = ChronoUnit.DAYS.between(today, date)
            
            val formattedDate = formatDisplayDate(deadline)
            
            val relative = when {
                days == 0L -> "Today"
                days == 1L -> "Tomorrow"
                days == -1L -> "Yesterday"
                days > 1 && days <= 7 -> "In $days days"
                days < -1 -> "${Math.abs(days)} days ago"
                else -> null
            }
            
            if (relative != null) "$formattedDate ($relative)" else formattedDate
        } catch (e: Exception) {
            deadline
        }
    }

    fun formatDisplayDate(dateStr: String?): String {
        if (dateStr == null) return "No date"
        return try {
            val hasTime = dateStr.contains("T") || dateStr.contains(":") || dateStr.contains(" ") && dateStr.trim().split(" ").size > 1
            val dateTime = parseToDateTime(dateStr)
            val now = LocalDateTime.now()
            
            val pattern = when {
                !hasTime -> if (dateTime.year == now.year) "MMM dd" else "MMM dd yyyy"
                dateTime.year == now.year -> "MMM dd, HH:mm"
                else -> "MMM dd yyyy, HH:mm"
            }
            
            dateTime.format(DateTimeFormatter.ofPattern(pattern))
        } catch (e: Exception) {
            dateStr
        }
    }

    fun parseToDateTime(dateStr: String): LocalDateTime {
        return try {
            val cleaned = dateStr.replace(" ", "T")
            if (cleaned.contains("T")) {
                if (cleaned.endsWith("Z")) {
                    Instant.parse(cleaned).atZone(ZoneId.systemDefault()).toLocalDateTime()
                } else {
                    val parts = cleaned.split("T")
                    val datePart = parts[0]
                    var timePart = parts[1]
                    
                    // Handle HH:mm or HH:mm:ss
                    if (timePart.count { it == ':' } == 0) {
                        timePart = "${timePart}:00:00"
                    } else if (timePart.count { it == ':' } == 1) {
                        timePart = "${timePart}:00"
                    }
                    
                    LocalDateTime.parse("${datePart}T${timePart}")
                }
            } else {
                // Default to 9:00 AM if only date is provided
                java.time.LocalDate.parse(cleaned).atTime(9, 0, 0)
            }
        } catch (e: Exception) {
            // Fallback for partial or malformed strings
            LocalDateTime.now().plusDays(1) 
        }
    }
}
