package com.victorkirui.local.dao

import androidx.room.*
import com.victorkirui.local.entity.PendingDeletion

@Dao
interface PendingDeletionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(deletion: PendingDeletion)

    @Query("SELECT * FROM PendingDeletion")
    suspend fun getAllPendingDeletions(): List<PendingDeletion>

    @Query("DELETE FROM PendingDeletion WHERE itemId = :itemId")
    suspend fun deleteByItemId(itemId: String)

    @Query("DELETE FROM PendingDeletion WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)
}
