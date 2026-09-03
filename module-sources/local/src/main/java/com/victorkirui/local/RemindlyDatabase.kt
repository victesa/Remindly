package com.victorkirui.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.victorkirui.local.dao.ItemDao
import com.victorkirui.local.dao.PendingDeletionDao
import com.victorkirui.local.dao.PendingSyncDao
import com.victorkirui.local.dao.ReminderDao
import com.victorkirui.local.entity.Item
import com.victorkirui.local.entity.PendingDeletion
import com.victorkirui.local.entity.PendingSync
import com.victorkirui.local.entity.Reminder

@Database(entities = [Item::class, PendingSync::class, Reminder::class, PendingDeletion::class], version = 8)
@TypeConverters(Converters::class)
abstract class RemindlyDatabase : RoomDatabase() {
    abstract fun itemDao(): ItemDao
    abstract fun pendingSyncDao(): PendingSyncDao
    abstract fun pendingDeletionDao(): PendingDeletionDao
    abstract fun reminderDao(): ReminderDao

    companion object {
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE Item ADD COLUMN checklist TEXT")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `PendingDeletion` (`itemId` TEXT NOT NULL, `userId` TEXT NOT NULL, PRIMARY KEY(`itemId`))")
            }
        }
    }
}
