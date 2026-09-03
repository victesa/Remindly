package com.victorkirui.remindly.widget

import android.content.Context
import androidx.room.Room
import com.victorkirui.local.RemindlyDatabase

object WidgetDatabaseHelper {
    @Volatile
    private var INSTANCE: RemindlyDatabase? = null

    fun getDatabase(context: Context): RemindlyDatabase {
        return INSTANCE ?: synchronized(this) {
            val instance = Room.databaseBuilder(
                context.applicationContext,
                RemindlyDatabase::class.java,
                "remindly_db"
            )
            .addMigrations(RemindlyDatabase.MIGRATION_6_7, RemindlyDatabase.MIGRATION_7_8)
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
            INSTANCE = instance
            instance
        }
    }
}
