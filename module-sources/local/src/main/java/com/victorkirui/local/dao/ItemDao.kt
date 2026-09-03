package com.victorkirui.local.dao

import androidx.room.*
import com.victorkirui.local.entity.Item
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemDao {
    @Upsert
    suspend fun upsert(item: Item): Long

    @Update
    suspend fun update(item: Item)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(item: Item): Long

    @Query("SELECT * FROM Item WHERE id = :id")
    suspend fun getItemById(id: String): Item?

    @Query("SELECT * FROM Item WHERE id = :id")
    fun getItemByIdFlow(id: String): Flow<Item?>

    @Query("SELECT COUNT(*) FROM Item WHERE (deadline LIKE :datePrefix || '%' OR eventDate LIKE :datePrefix || '%') AND status != 'DONE'")
    suspend fun getTodayItemsCount(datePrefix: String): Int

    @Query("SELECT * FROM Item WHERE status != 'DONE'")
    suspend fun getAllActiveItemsSync(): List<Item>

    @Query("SELECT * FROM Item WHERE (deadline IS NOT NULL OR eventDate IS NOT NULL) AND status != 'DONE'")
    suspend fun getUpcomingItemsSync(): List<Item>

    @Query("SELECT * FROM Item WHERE userId = :userId ORDER BY createdAt DESC")
    fun getAllItems(userId: String): Flow<List<Item>>

    @Query("SELECT * FROM Item WHERE userId = :userId AND category = :category ORDER BY createdAt DESC")
    fun getItemsByCategory(userId: String, category: String): Flow<List<Item>>

    @Query("SELECT DISTINCT category FROM Item WHERE userId = :userId AND category IS NOT NULL")
    fun getAllCategories(userId: String): Flow<List<String>>

    @Query("SELECT * FROM Item WHERE userId = :userId AND (title LIKE '%' || :query || '%' OR summary LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%') ORDER BY createdAt DESC")
    fun searchItems(userId: String, query: String): Flow<List<Item>>

    @Query("SELECT * FROM Item WHERE userId = :userId AND ((:url IS NOT NULL AND sourceUrl = :url) OR (:text IS NOT NULL AND extractedText = :text)) LIMIT 1")
    suspend fun findDuplicate(userId: String, url: String?, text: String?): Item?

    @Query("SELECT * FROM Item WHERE userId = :userId AND ((:url IS NOT NULL AND sourceUrl = :url) OR (:text IS NOT NULL AND extractedText = :text) OR (:title IS NOT NULL AND title = :title)) LIMIT 1")
    suspend fun findDuplicateEx(userId: String, url: String?, text: String?, title: String?): Item?

    @Query("DELETE FROM Item WHERE userId = :userId AND id = :id")
    suspend fun deleteById(userId: String, id: String)

    @Query("DELETE FROM Item WHERE userId = :userId AND category = :category")
    suspend fun deleteByCategory(userId: String, category: String)

    @Query("DELETE FROM Item WHERE userId = :userId AND (category IS NULL OR category = '')")
    suspend fun deleteUncategorized(userId: String)

    @Query("DELETE FROM Item WHERE userId = :userId AND status = 'PENDING'")
    suspend fun deletePendingSync(userId: String)

    @Query("DELETE FROM Item WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)
}
