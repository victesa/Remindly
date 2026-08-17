package com.victorkirui.local.entity

import androidx.room.Embedded
import androidx.room.Relation

data class ItemAndPendingSync(
    @Embedded
    val item: Item,

    @Relation(
        parentColumn = "id",
        entityColumn = "itemId"
    )
    val pendingSync: List<PendingSync>
)