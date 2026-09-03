package com.victorkirui.local.repository

import com.victorkirui.core.data.DataClearer
import com.victorkirui.local.entity.Item
import com.victorkirui.local.entity.PendingSync
import com.victorkirui.local.entity.Reminder
import com.victorkirui.local.entity.ReminderWithItem
import kotlinx.coroutines.flow.Flow

interface LocalRepository : DataClearer {
    suspend fun saveItem(item: Item): Long
    suspend fun updateItem(item: Item)
    suspend fun getItem(id: String): Item?
    fun getItemFlow(id: String): Flow<Item?>
    fun getAllItems(userId: String): Flow<List<Item>>
    fun getItemsByCategory(userId: String, category: String): Flow<List<Item>>
    fun getAllCategories(userId: String): Flow<List<String>>
    fun searchItems(userId: String, query: String): Flow<List<Item>>
    suspend fun findDuplicate(userId: String, url: String?, text: String?): Item?
    suspend fun findDuplicateEx(userId: String, url: String?, text: String?, title: String?): Item?
    
    suspend fun addPendingSync(sync: PendingSync)
    suspend fun removePendingSync(itemId: String)
    suspend fun getAllPendingSyncs(): List<PendingSync>

    suspend fun addPendingDeletion(deletion: com.victorkirui.local.entity.PendingDeletion)
    suspend fun removePendingDeletion(itemId: String)
    suspend fun getAllPendingDeletions(): List<com.victorkirui.local.entity.PendingDeletion>

    suspend fun saveReminders(reminders: List<Reminder>): List<Long>
    fun getRemindersForItem(itemId: String): Flow<List<Reminder>>
    fun getAllRemindersWithItems(userId: String): Flow<List<ReminderWithItem>>
    suspend fun deleteRemindersForItem(itemId: String)
    suspend fun updateReminder(reminder: Reminder)

    suspend fun deleteItem(userId: String, id: String)
    suspend fun deleteItemsByCategory(userId: String, category: String)
    suspend fun deleteUncategorizedItems(userId: String)
    suspend fun deletePendingSyncItems(userId: String)
    suspend fun deleteExpiredItems(userId: String)
    suspend fun deleteAllItemsForUser(userId: String)

    suspend fun <R> withTransaction(block: suspend () -> R): R
}
