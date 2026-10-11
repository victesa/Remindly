package com.victorkirui.remote

import com.victorkirui.core.model.CaptureRequest
import com.victorkirui.core.model.CaptureResponse
import android.net.Uri

interface CaptureApiService : com.victorkirui.core.billing.PurchaseVerifier, com.victorkirui.core.data.AccountPurger {
    suspend fun getMe(): com.victorkirui.core.model.UserProfileResponse
    override suspend fun getUserProfile(): com.victorkirui.core.model.UserProfileResponse = getMe()
    suspend fun getCaptures(limit: Int? = null): List<com.victorkirui.core.model.StoredReminderItem>

    suspend fun getSyncDeltas(since: String?): com.victorkirui.core.model.SyncDeltaResponse
    suspend fun updateItem(id: String, updates: Map<String, Any?>): Boolean
    suspend fun deleteItem(id: String): Boolean
    override suspend fun purgeAccount(): Boolean
    override suspend fun verifyPurchase(purchaseToken: String, productId: String): com.victorkirui.core.model.VerifyPurchaseResponse
}
