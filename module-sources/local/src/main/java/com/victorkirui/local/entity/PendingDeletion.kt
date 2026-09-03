package com.victorkirui.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class PendingDeletion(
    @PrimaryKey
    val itemId: String,
    val userId: String
)
