package com.victorkirui.core.calendar

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.CalendarContract
import android.util.Log
import com.victorkirui.core.util.DateUtils
import java.time.ZoneId
import java.util.*

class CalendarManager(private val context: Context) {

    /**
     * Adds an event to the primary calendar.
     * 
     * @param title Title of the event
     * @param description Summary/Notes for the event
     * @param eventDate ISO 8601 date string
     * @return The URI of the created event or null if failed
     */
    fun addEvent(title: String, description: String?, eventDate: String, location: String? = null): Uri? {
        val calendarId = getPrimaryCalendarId() ?: run {
            Log.e("CalendarManager", "No calendar found to write to.")
            return null
        }
        
        Log.d("CalendarManager", "Adding event to calendar ID: $calendarId. Date: $eventDate, Location: $location")

        return try {
            val isAllDay = !eventDate.contains("T") && !eventDate.contains(":")
            val dateTime = DateUtils.parseToDateTime(eventDate)
            
            val startMillis: Long
            val endMillis: Long

            if (isAllDay) {
                // All-day events must be in UTC at 00:00:00
                val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                    clear()
                    set(dateTime.year, dateTime.monthValue - 1, dateTime.dayOfMonth, 0, 0, 0)
                }
                startMillis = calendar.timeInMillis
                endMillis = startMillis + 24 * 60 * 60 * 1000 // 24 hours
            } else {
                startMillis = dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                endMillis = startMillis + 1800000 // Default 30 mins
            }

            val values = ContentValues().apply {
                put(CalendarContract.Events.DTSTART, startMillis)
                put(CalendarContract.Events.DTEND, endMillis)
                put(CalendarContract.Events.TITLE, title)
                put(CalendarContract.Events.DESCRIPTION, description ?: "Saved from Remindly")
                put(CalendarContract.Events.EVENT_LOCATION, location)
                put(CalendarContract.Events.CALENDAR_ID, calendarId)
                put(CalendarContract.Events.EVENT_TIMEZONE, if (isAllDay) "UTC" else TimeZone.getDefault().id)
                put(CalendarContract.Events.ALL_DAY, if (isAllDay) 1 else 0)
                put(CalendarContract.Events.STATUS, CalendarContract.Events.STATUS_CONFIRMED)
                put(CalendarContract.Events.AVAILABILITY, CalendarContract.Events.AVAILABILITY_BUSY)
                put(CalendarContract.Events.HAS_ALARM, 1)
                put(CalendarContract.Events.ACCESS_LEVEL, CalendarContract.Events.ACCESS_PRIVATE)
            }

            val uri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
            Log.d("CalendarManager", "Event inserted successfully. URI: $uri")
            
            if (uri != null) {
                val eventId = uri.lastPathSegment?.toLongOrNull()
                if (eventId != null) {
                    addReminder(eventId)
                }
            }
            uri
        } catch (e: Exception) {
            Log.e("CalendarManager", "Failed to add event: ${e.message}", e)
            null
        }
    }

    private fun addReminder(eventId: Long) {
        try {
            val values = ContentValues().apply {
                put(CalendarContract.Reminders.MINUTES, 30)
                put(CalendarContract.Reminders.EVENT_ID, eventId)
                put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
            }
            context.contentResolver.insert(CalendarContract.Reminders.CONTENT_URI, values)
            Log.d("CalendarManager", "Reminder added for event: $eventId")
        } catch (e: Exception) {
            Log.e("CalendarManager", "Failed to add reminder", e)
        }
    }

    /**
     * Finds the user's primary calendar ID.
     */
    private fun getPrimaryCalendarId(): Long? {
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.IS_PRIMARY,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.ACCOUNT_TYPE,
            CalendarContract.Calendars.SYNC_EVENTS
        )

        return try {
            context.contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                projection,
                null,
                null,
                null
            )?.use { cursor ->
                var firstId: Long? = null
                var googleCalendarId: Long? = null
                
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(0)
                    val isPrimary = cursor.getInt(1) != 0
                    val accountName = cursor.getString(2)
                    val accountType = cursor.getString(3)
                    val syncEnabled = cursor.getInt(4) != 0
                    
                    Log.d("CalendarManager", "Found calendar: id=$id, account=$accountName, type=$accountType, primary=$isPrimary, syncEnabled=$syncEnabled")
                    
                    if (firstId == null) firstId = id
                    
                    if (accountType == "com.google" && syncEnabled) {
                        // Priority 1: Primary Google Calendar with Sync enabled
                        if (isPrimary) {
                            Log.d("CalendarManager", "Returning priority 1: Primary Google calendar: $id")
                            return id
                        }
                        // Priority 2: Any Google Calendar with Sync enabled
                        if (googleCalendarId == null) {
                            googleCalendarId = id
                        }
                    }
                }
                
                if (googleCalendarId != null) {
                    Log.d("CalendarManager", "Returning priority 2: Google calendar: $googleCalendarId")
                    return googleCalendarId
                }

                if (firstId != null) {
                    Log.d("CalendarManager", "Returning priority 3: Fallback calendar: $firstId")
                    return firstId
                }

                Log.e("CalendarManager", "No calendars found at all.")
                null
            }
        } catch (e: Exception) {
            Log.e("CalendarManager", "Could not query calendars: ${e.message}", e)
            null
        }
    }
}
