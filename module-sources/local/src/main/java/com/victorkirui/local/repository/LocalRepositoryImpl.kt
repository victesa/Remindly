package com.victorkirui.local.repository

import com.victorkirui.core.util.DateUtils
import com.victorkirui.local.dao.ItemDao
import com.victorkirui.local.dao.PendingSyncDao
import com.victorkirui.local.dao.ReminderDao
import com.victorkirui.local.RemindlyDatabase
import com.victorkirui.local.entity.*
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime

class LocalRepositoryImpl(
    private val database: RemindlyDatabase,
    private val itemDao: ItemDao,
    private val pendingSyncDao: PendingSyncDao,
    private val pendingDeletionDao: com.victorkirui.local.dao.PendingDeletionDao,
    private val reminderDao: ReminderDao
) : LocalRepository {
    
    override suspend fun saveItem(item: Item): Long {
        return itemDao.upsert(item)
    }

    override suspend fun updateItem(item: Item) {
        itemDao.update(item)
    }

    override suspend fun getItem(id: String): Item? {
        return itemDao.getItemById(id)
    }

    override fun getItemFlow(id: String): Flow<Item?> {
        return itemDao.getItemByIdFlow(id)
    }

    override fun getAllItems(userId: String): Flow<List<Item>> {
        return itemDao.getAllItems(userId)
    }

    override fun getItemsByCategory(userId: String, category: String): Flow<List<Item>> {
        return itemDao.getItemsByCategory(userId, category)
    }

    override fun getAllCategories(userId: String): Flow<List<String>> {
        return itemDao.getAllCategories(userId)
    }

    override fun searchItems(userId: String, query: String): Flow<List<Item>> {
        return itemDao.searchItems(userId, query)
    }

    override suspend fun findDuplicate(userId: String, url: String?, text: String?): Item? {
        if (url == null && text == null) return null
        return itemDao.findDuplicate(userId, url, text)
    }

    override suspend fun findDuplicateEx(userId: String, url: String?, text: String?, title: String?): Item? {
        if (url == null && text == null && title == null) return null
        return itemDao.findDuplicateEx(userId, url, text, title)
    }

    override suspend fun addPendingSync(sync: PendingSync) {
        pendingSyncDao.insert(sync)
    }

    override suspend fun removePendingSync(itemId: String) {
        pendingSyncDao.deleteByItemId(itemId)
    }

    override suspend fun getAllPendingSyncs(): List<PendingSync> {
        return pendingSyncDao.getAllPendingSyncs()
    }

    override suspend fun addPendingDeletion(deletion: com.victorkirui.local.entity.PendingDeletion) {
        pendingDeletionDao.insert(deletion)
    }

    override suspend fun removePendingDeletion(itemId: String) {
        pendingDeletionDao.deleteByItemId(itemId)
    }

    override suspend fun getAllPendingDeletions(): List<com.victorkirui.local.entity.PendingDeletion> {
        return pendingDeletionDao.getAllPendingDeletions()
    }

    override suspend fun saveReminders(reminders: List<Reminder>): List<Long> {
        return reminderDao.insertReminders(reminders)
    }

    override fun getRemindersForItem(itemId: String): Flow<List<Reminder>> {
        return reminderDao.getRemindersForItem(itemId)
    }

    override fun getAllRemindersWithItems(userId: String): Flow<List<ReminderWithItem>> {
        return reminderDao.getAllRemindersWithItems(userId)
    }

    override suspend fun deleteRemindersForItem(itemId: String) {
        reminderDao.deleteRemindersForItem(itemId)
    }

    override suspend fun updateReminder(reminder: Reminder) {
        reminderDao.updateReminder(reminder)
    }

    override suspend fun deleteItem(userId: String, id: String) {
        database.withTransaction {
            reminderDao.deleteRemindersForItem(id)
            pendingSyncDao.deleteByItemId(id)
            itemDao.deleteById(userId, id)
        }
    }

    override suspend fun deleteItemsByCategory(userId: String, category: String) {
        itemDao.deleteByCategory(userId, category)
    }

    override suspend fun deleteUncategorizedItems(userId: String) {
        itemDao.deleteUncategorized(userId)
    }

    override suspend fun deletePendingSyncItems(userId: String) {
        itemDao.deletePendingSync(userId)
    }

    override suspend fun deleteExpiredItems(userId: String) {
        val now = LocalDateTime.now()
        val allItems = itemDao.getAllItems(userId).first()
        
        allItems.forEach { item ->
            val deadlineStr = item.deadline ?: item.eventDate
            if (deadlineStr != null) {
                try {
                    val deadline = DateUtils.parseToDateTime(deadlineStr)

                    if (deadline.isBefore(now)) {
                        android.util.Log.d("LocalRepository", "Deleting expired item: ${item.id} (Deadline: $deadlineStr)")
                        deleteItem(userId, item.id)
                    }
                } catch (_: Exception) { }
            }
        }
    }

    override suspend fun deleteAllItemsForUser(userId: String) {
        itemDao.deleteAllForUser(userId)
    }

    override suspend fun clearUserData(userId: String) {
        deleteAllItemsForUser(userId)
    }

    override suspend fun <R> withTransaction(block: suspend () -> R): R {
        return database.withTransaction {
            block()
        }
    }
}
