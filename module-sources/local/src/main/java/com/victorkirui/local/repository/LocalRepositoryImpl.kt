package com.victorkirui.local.repository

import com.victorkirui.local.dao.ItemDao
import com.victorkirui.local.dao.PendingSyncDao
import com.victorkirui.local.dao.ReminderDao
import com.victorkirui.local.RemindlyDatabase
import com.victorkirui.local.entity.*
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow

class LocalRepositoryImpl(
    private val database: RemindlyDatabase,
    private val itemDao: ItemDao,
    private val pendingSyncDao: PendingSyncDao,
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

    override fun getAllItems(): Flow<List<Item>> {
        return itemDao.getAllItems()
    }

    override fun getItemsByCategory(category: String): Flow<List<Item>> {
        return itemDao.getItemsByCategory(category)
    }

    override fun getAllCategories(): Flow<List<String>> {
        return itemDao.getAllCategories()
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

    override suspend fun saveReminders(reminders: List<Reminder>) {
        reminderDao.insertReminders(reminders)
    }

    override fun getRemindersForItem(itemId: String): Flow<List<Reminder>> {
        return reminderDao.getRemindersForItem(itemId)
    }

    override fun getAllRemindersWithItems(): Flow<List<ReminderWithItem>> {
        return reminderDao.getAllRemindersWithItems()
    }

    override suspend fun deleteRemindersForItem(itemId: String) {
        reminderDao.deleteRemindersForItem(itemId)
    }

    override suspend fun updateReminder(reminder: Reminder) {
        reminderDao.updateReminder(reminder)
    }

    override suspend fun deleteItem(id: String) {
        database.withTransaction {
            reminderDao.deleteRemindersForItem(id)
            pendingSyncDao.deleteByItemId(id)
            itemDao.deleteById(id)
        }
    }

    override suspend fun deleteItemsByCategory(category: String) {
        itemDao.deleteByCategory(category)
    }

    override suspend fun deleteUncategorizedItems() {
        itemDao.deleteUncategorized()
    }

    override suspend fun deletePendingSyncItems() {
        itemDao.deletePendingSync()
    }

    override suspend fun <R> withTransaction(block: suspend () -> R): R {
        return database.withTransaction {
            block()
        }
    }
}
