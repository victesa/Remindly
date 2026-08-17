package com.victorkirui.local.entity

import androidx.room.Embedded
import androidx.room.Relation

data class ReminderWithItem(
    @Embedded val reminder: Reminder,
    @Relation(
        parentColumn = "itemId",
        entityColumn = "id"
    )
    val item: Item
)
