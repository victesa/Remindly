package com.victorkirui.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Item(
    @PrimaryKey
    val id: String,
    val title: String,
    val summary: String?,
    val category: String?,
    val deadline: String?,
    val eventDate: String?,
    val organization: String? = null,
    val source: String? = null,
    val sourceUrl: String? = null,
    val originalMediaUri: String? = null,
    val extractedText: String? = null,
    val contentType: String = "TEXT",
    val metadata: Map<String, String>? = null,
    val createdAt: String,
    val status: String
)
