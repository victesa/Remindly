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

    @Query("SELECT * FROM Item ORDER BY createdAt DESC")
    fun getAllItems(): Flow<List<Item>>

    @Query("SELECT * FROM Item WHERE category = :category ORDER BY createdAt DESC")
    fun getItemsByCategory(category: String): Flow<List<Item>>

    @Query("SELECT DISTINCT category FROM Item WHERE category IS NOT NULL")
    fun getAllCategories(): Flow<List<String>>
    @Query("DELETE FROM Item WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM Item WHERE category = :category")
    suspend fun deleteByCategory(category: String)

    @Query("DELETE FROM Item WHERE category IS NULL OR category = ''")
    suspend fun deleteUncategorized()

    @Query("DELETE FROM Item WHERE status = 'PENDING'")
    suspend fun deletePendingSync()
}
