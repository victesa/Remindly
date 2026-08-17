package com.victorkirui.local.dao

import androidx.room.*
import com.victorkirui.local.entity.PendingSync

@Dao
interface PendingSyncDao {
    @Upsert
    suspend fun insert(pendingSync: PendingSync)

    @Query("DELETE FROM PendingSync WHERE itemId = :itemId")
    suspend fun deleteByItemId(itemId: String)

    @Query("SELECT * FROM PendingSync")
    suspend fun getAllPendingSyncs(): List<PendingSync>
}
