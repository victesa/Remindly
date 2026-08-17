package com.victorkirui.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.victorkirui.local.dao.ItemDao
import com.victorkirui.local.dao.PendingSyncDao
import com.victorkirui.local.dao.ReminderDao
import com.victorkirui.local.entity.Item
import com.victorkirui.local.entity.PendingSync
import com.victorkirui.local.entity.Reminder

@Database(entities = [Item::class, PendingSync::class, Reminder::class], version = 5)
@TypeConverters(Converters::class)
abstract class RemindlyDatabase : RoomDatabase() {
    abstract fun itemDao(): ItemDao
    abstract fun pendingSyncDao(): PendingSyncDao
    abstract fun reminderDao(): ReminderDao
}
