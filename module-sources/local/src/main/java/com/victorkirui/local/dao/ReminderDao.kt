package com.victorkirui.local.dao

import androidx.room.*
import com.victorkirui.local.entity.Reminder
import com.victorkirui.local.entity.ReminderWithItem
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {
    @Upsert
    suspend fun insertReminders(reminders: List<Reminder>)

    @Query("SELECT * FROM Reminder WHERE itemId = :itemId ORDER BY reminderDateTime ASC")
    fun getRemindersForItem(itemId: String): Flow<List<Reminder>>

    @Transaction
    @Query("SELECT * FROM Reminder ORDER BY reminderDateTime ASC")
    fun getAllRemindersWithItems(): Flow<List<ReminderWithItem>>

    @Query("DELETE FROM Reminder WHERE itemId = :itemId")
    suspend fun deleteRemindersForItem(itemId: String)

    @Update
    suspend fun updateReminder(reminder: Reminder)
}
