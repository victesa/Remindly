package com.victorkirui.remote

import com.victorkirui.core.model.CaptureRequest
import com.victorkirui.core.model.CaptureResponse
import android.net.Uri

interface CaptureApiService {
    suspend fun getQuota(): com.victorkirui.core.model.QuotaResponse
    suspend fun getCaptures(): List<com.victorkirui.core.model.StoredReminderItem>
    suspend fun updateItem(id: String, updates: Map<String, Any?>): Boolean
    suspend fun deleteItem(id: String): Boolean
    suspend fun purgeAccount(): Boolean
}
