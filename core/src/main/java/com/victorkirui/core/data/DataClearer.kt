package com.victorkirui.core.data

interface DataClearer {
    suspend fun clearUserData(userId: String)
}
