package com.victorkirui.local.repository

import com.victorkirui.local.entity.Item
import com.victorkirui.local.entity.PendingSync
import com.victorkirui.local.entity.Reminder
import com.victorkirui.local.entity.ReminderWithItem
import kotlinx.coroutines.flow.Flow

interface LocalRepository {
    suspend fun saveItem(item: Item): Long
    suspend fun updateItem(item: Item)
    suspend fun getItem(id: String): Item?
    fun getItemFlow(id: String): Flow<Item?>
    fun getAllItems(): Flow<List<Item>>
    fun getItemsByCategory(category: String): Flow<List<Item>>
    fun getAllCategories(): Flow<List<String>>
    
    suspend fun addPendingSync(sync: PendingSync)
    suspend fun removePendingSync(itemId: String)
    suspend fun getAllPendingSyncs(): List<PendingSync>

    suspend fun saveReminders(reminders: List<Reminder>)
    fun getRemindersForItem(itemId: String): Flow<List<Reminder>>
    fun getAllRemindersWithItems(): Flow<List<ReminderWithItem>>
    suspend fun deleteRemindersForItem(itemId: String)
    suspend fun updateReminder(reminder: Reminder)

    suspend fun deleteItem(id: String)

    suspend fun <R> withTransaction(block: suspend () -> R): R
}
