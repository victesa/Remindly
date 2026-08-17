package com.victorkirui.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = Item::class,
            parentColumns = ["id"],
            childColumns = ["itemId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        )
    ]
)
data class Reminder(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val itemId: String,
    val reminderDateTime: String, // ISO 8601 format: YYYY-MM-DDTHH:MM:SS
    val type: String, // e.g., "DAILY", "INTERVAL", "MORNING_OF"
    val isCompleted: Boolean = false
)
