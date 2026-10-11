package com.victorkirui.core.util

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

object DateUtils {

    /**
     * Parses an ISO date string (UTC or with timezone offset or local) into a global Instant timeline point.
     */
    fun parseToInstant(dateStr: String?): Instant? {
        if (dateStr.isNullOrBlank()) return null
        return try {
            val cleaned = dateStr.trim().replace(" ", "T")
            when {
                // ISO 8601 string with UTC 'Z' or offset like +03:00 / -05:00
                cleaned.endsWith("Z") || (cleaned.contains("T") && (cleaned.substringAfter("T").contains("+") || (cleaned.indexOf("-", cleaned.indexOf("T")) > 0 && !cleaned.endsWith("T")))) -> {
                    try {
                        Instant.parse(cleaned)
                    } catch (_: Exception) {
                        try {
                            OffsetDateTime.parse(cleaned).toInstant()
                        } catch (_: Exception) {
                            val withoutZ = cleaned.removeSuffix("Z").substringBefore("+")
                            val local = parseToDateTime(withoutZ)
                            local.atZone(ZoneId.systemDefault()).toInstant()
                        }
                    }
                }
                cleaned.contains("T") -> {
                    val localDateTime = parseToDateTime(cleaned)
                    localDateTime.atZone(ZoneId.systemDefault()).toInstant()
                }
                else -> {
                    val localDate = LocalDate.parse(cleaned)
                    localDate.atStartOfDay(ZoneId.systemDefault()).toInstant()
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Parses ISO date strings (e.g. "2026-09-11T23:59:59.000Z") into a LocalDateTime
     * in the system's default local timezone.
     */
    fun parseToDateTime(dateStr: String): LocalDateTime {
        return try {
            val cleaned = dateStr.trim().replace(" ", "T")
            if (cleaned.endsWith("Z") || (cleaned.contains("T") && (cleaned.substringAfter("T").contains("+") || (cleaned.indexOf("-", cleaned.indexOf("T")) > 0)))) {
                val instant = parseToInstant(cleaned)
                if (instant != null) {
                    return instant.atZone(ZoneId.systemDefault()).toLocalDateTime()
                }
            }
            if (cleaned.contains("T")) {
                val withoutZ = if (cleaned.endsWith("Z")) cleaned.dropLast(1) else cleaned
                val noOffset = when {
                    withoutZ.contains("+") -> withoutZ.substringBefore("+")
                    withoutZ.indexOf("-", withoutZ.indexOf("T")) > 0 -> {
                        val tIndex = withoutZ.indexOf("T")
                        val dashIndex = withoutZ.indexOf("-", tIndex + 1)
                        withoutZ.substring(0, dashIndex)
                    }
                    else -> withoutZ
                }
                
                val parts = noOffset.split("T")
                val datePart = parts[0]
                var timePart = parts[1]
                if (timePart.contains(".")) {
                    timePart = timePart.substringBefore(".")
                }
                if (timePart.count { it == ':' } == 0) timePart = "$timePart:00:00"
                if (timePart.count { it == ':' } == 1) timePart = "$timePart:00"
                
                LocalDateTime.parse("${datePart}T${timePart}")
            } else {
                LocalDate.parse(cleaned).atTime(23, 59, 59)
            }
        } catch (e: Exception) {
            LocalDateTime.now().plusDays(1)
        }
    }

    fun parseToLocalDate(dateStr: String?): LocalDate? {
        if (dateStr.isNullOrBlank()) return null
        return try {
            parseToDateTime(dateStr).toLocalDate()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Sanitizes eventDate relative to deadline.
     * If eventDate is before deadline (e.g. opening date vs closing deadline),
     * eventDate is considered an opening/start date rather than the event/deadline date,
     * so return null for eventDate.
     */
    fun sanitizeEventDate(eventDate: String?, deadline: String?): String? {
        if (eventDate.isNullOrBlank()) return null
        if (deadline.isNullOrBlank()) return eventDate

        return try {
            val eventLocalDate = parseToLocalDate(eventDate)
            val deadlineLocalDate = parseToLocalDate(deadline)
            if (eventLocalDate != null && deadlineLocalDate != null && eventLocalDate.isBefore(deadlineLocalDate)) {
                null
            } else {
                eventDate
            }
        } catch (_: Exception) {
            eventDate
        }
    }

    fun formatDisplayDate(dateStr: String?): String {
        if (dateStr.isNullOrBlank()) return "No date"
        return try {
            val dateTime = parseToDateTime(dateStr)
            val now = LocalDateTime.now()
            
            val hour = dateTime.hour
            val minute = dateTime.minute
            val second = dateTime.second
            
            // Check if time is default start/end-of-day timestamp attached when no specific hour is specified
            val isDefaultTime = (hour == 0 && minute == 0 && second == 0) || 
                                (hour == 23 && minute == 59) ||
                                (!dateStr.contains("T") && !dateStr.contains(":"))
                                
            val pattern = when {
                isDefaultTime -> if (dateTime.year == now.year) "MMM dd" else "MMM dd, yyyy"
                dateTime.year == now.year -> "MMM dd, hh:mm a"
                else -> "MMM dd, yyyy, hh:mm a"
            }
            
            dateTime.format(DateTimeFormatter.ofPattern(pattern, Locale.US))
        } catch (e: Exception) {
            dateStr
        }
    }

    fun getReadableDeadline(deadline: String?): String {
        if (deadline.isNullOrBlank()) return "No deadline"
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

    fun getDeadlineText(deadline: String?): String {
        if (deadline.isNullOrBlank()) return ""
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
                    val formatter = DateTimeFormatter.ofPattern("MMM dd", Locale.US)
                    "Deadline: ${date.format(formatter)}"
                }
            }
        } catch (e: Exception) {
            ""
        }
    }

    fun getTimeAgo(dateTimeStr: String?): String {
        if (dateTimeStr.isNullOrEmpty()) return "Recently"
        return try {
            val pastInstant = parseToInstant(dateTimeStr) ?: return "Recently"
            val nowInstant = Instant.now()
            
            val seconds = ChronoUnit.SECONDS.between(pastInstant, nowInstant)
            val minutes = ChronoUnit.MINUTES.between(pastInstant, nowInstant)
            val hours = ChronoUnit.HOURS.between(pastInstant, nowInstant)
            val days = ChronoUnit.DAYS.between(pastInstant, nowInstant)

            when {
                seconds < 60 -> "Just now"
                minutes < 60 -> "${minutes}m ago"
                hours < 24 -> "${hours}h ago"
                days < 7 -> "${days}d ago"
                else -> {
                    val pastZoned = pastInstant.atZone(ZoneId.systemDefault())
                    pastZoned.format(DateTimeFormatter.ofPattern("MMM dd", Locale.US))
                }
            }
        } catch (e: Exception) {
            "Recently"
        }
    }
}
