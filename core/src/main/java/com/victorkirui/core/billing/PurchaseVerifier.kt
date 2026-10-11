package com.victorkirui.core.billing

import com.victorkirui.core.model.UserProfileResponse
import com.victorkirui.core.model.VerifyPurchaseResponse

interface PurchaseVerifier {
    suspend fun verifyPurchase(purchaseToken: String, productId: String): VerifyPurchaseResponse
    suspend fun getUserProfile(): UserProfileResponse? = null
}
