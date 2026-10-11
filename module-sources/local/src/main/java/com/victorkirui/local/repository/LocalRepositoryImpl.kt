package com.victorkirui.local.repository

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
    private val reminderDao: ReminderDao,
    private val calendarManager: com.victorkirui.core.calendar.CalendarManager? = null
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
        val cleanUrl = normalizeUrl(url)
        val cleanText = text?.trim()?.takeIf { it.isNotBlank() && it.length >= 15 }
        if (cleanUrl == null && cleanText == null) return null

        val exactMatch = itemDao.findDuplicate(userId, url?.trim(), cleanText)
        if (exactMatch != null) return exactMatch

        return itemDao.findDuplicate(userId, cleanUrl, cleanText)
    }

    override suspend fun findDuplicateEx(userId: String, url: String?, text: String?, title: String?): Item? {
        val cleanUrl = normalizeUrl(url)
        val cleanText = text?.trim()?.takeIf { it.isNotBlank() && it.length >= 15 }
        val cleanTitle = title?.trim()?.takeIf { it.isNotBlank() && it != "Analysing..." && it != "Untitled" && it != "Saved Offline" }
        if (cleanUrl == null && cleanText == null && cleanTitle == null) return null

        val exactMatch = itemDao.findDuplicateEx(userId, url?.trim(), cleanText, cleanTitle)
        if (exactMatch != null) return exactMatch

        return itemDao.findDuplicateEx(userId, cleanUrl, cleanText, cleanTitle)
    }

    private fun normalizeUrl(url: String?): String? {
        if (url.isNullOrBlank()) return null
        val clean = url.trim()
        return try {
            val uri = android.net.Uri.parse(clean)
            val scheme = uri.scheme?.lowercase() ?: "https"
            val host = uri.host?.lowercase()?.removePrefix("www.") ?: return clean
            val path = uri.path?.trimEnd('/') ?: ""
            "$scheme://$host$path"
        } catch (_: Exception) {
            clean
        }
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

    private fun cleanupCalendarEvent(item: Item?) {
        val uri = item?.metadata?.get("calendar_event_uri")
        if (!uri.isNullOrBlank()) {
            calendarManager?.deleteEvent(uri)
        }
    }

    override suspend fun deleteItem(userId: String, id: String) {
        try {
            val item = itemDao.getItemById(id)
            cleanupCalendarEvent(item)
        } catch (e: Exception) {
            android.util.Log.e("LocalRepository", "Error cleaning calendar event for item $id", e)
        }

        database.withTransaction {
            reminderDao.deleteRemindersForItem(id)
            pendingSyncDao.deleteByItemId(id)
            itemDao.deleteById(userId, id)
        }
    }

    override suspend fun deleteItemsByCategory(userId: String, category: String) {
        try {
            val items = itemDao.getItemsByCategorySync(userId, category)
            items.forEach { cleanupCalendarEvent(it) }
        } catch (_: Exception) {}

        itemDao.deleteByCategory(userId, category)
    }

    override suspend fun deleteUncategorizedItems(userId: String) {
        try {
            val items = itemDao.getUncategorizedItemsSync(userId)
            items.forEach { cleanupCalendarEvent(it) }
        } catch (_: Exception) {}

        itemDao.deleteUncategorized(userId)
    }

    override suspend fun deletePendingSyncItems(userId: String) {
        try {
            val items = itemDao.getPendingSyncItemsSync(userId)
            items.forEach { cleanupCalendarEvent(it) }
        } catch (_: Exception) {}

        itemDao.deletePendingSync(userId)
    }

    override suspend fun deleteExpiredItems(userId: String) {
        val now = java.time.LocalDateTime.now()
        val retentionCutoff = now.minusDays(14) // Option B: 14-day grace period
        val allItems = itemDao.getAllItems(userId).first()
        
        allItems.forEach { item ->
            val deadlineStr = item.deadline ?: item.eventDate
            if (deadlineStr != null) {
                try {
                    val deadline = com.victorkirui.core.util.DateUtils.parseToDateTime(deadlineStr)

                    // Purge items whose deadline or event date passed more than 14 days ago
                    if (deadline.isBefore(retentionCutoff)) {
                        android.util.Log.d("LocalRepository", "Purging expired item older than 14 days: ${item.id} (Deadline: $deadlineStr)")
                        deleteItem(userId, item.id)
                    }
                } catch (_: Exception) { }
            }
        }
    }

    override suspend fun deleteAllItemsForUser(userId: String) {
        itemDao.deleteAllForUser(userId)
    }

    override suspend fun getAllItemsSyncForUser(userId: String): List<Item> {
        return itemDao.getAllItemsSyncForUser(userId)
    }

    override suspend fun clearUserData(userId: String) {
        try {
            database.clearAllTables()
        } catch (e: Exception) {
            android.util.Log.e("LocalRepository", "Failed clearAllTables in Room: ${e.message}", e)
            deleteAllItemsForUser(userId)
        }
    }

    override suspend fun <R> withTransaction(block: suspend () -> R): R {
        return database.withTransaction {
            block()
        }
    }
}
