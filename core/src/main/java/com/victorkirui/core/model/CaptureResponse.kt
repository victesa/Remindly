package com.victorkirui.core.model

import com.google.gson.annotations.SerializedName

data class CaptureResponse(
    val item: RemoteItem? = null,
    val extraction: ExtractionInfo? = null
)


data class GetItemsResponse(
    val success: Boolean = false,
    val userId: String? = null,
    val userTier: String? = null,
    val count: Int = 0,
    val items: List<StoredReminderItem>? = emptyList()
)

data class StoredReminderItem(
    val id: String = "",
    val userId: String? = null,
    val title: String? = null,
    val summary: String? = null,
    val category: String? = null,
    val deadline: String? = null,
    val eventDate: String? = null,
    val organization: String? = null,
    val source: String? = null,
    val sourceUrl: String? = null,
    val url: String? = null,
    val state: String? = null,
    val status: String? = null,
    val extractedAt: String? = null,
    val createdAt: String? = null,
    val sourceType: String? = null,
    val inputSnippet: String? = null,
    val persistedSource: String? = null,
    val checklist: List<String>? = null,
    val metadata: Map<String, Any?>? = null,
    val data: ExtractedReminderPayload? = null
)


data class ExtractedReminderPayload(
    val title: String = "Untitled",
    val summary: String? = null,
    val category: String = "OTHER",
    val deadline: String? = null,
    val eventDate: String? = null,
    val organization: String? = null,
    val url: String? = null,
    val strategy: String = "BACKEND",
    val tier: String = "free",
    val confidenceScore: Double? = null,
    val actionableItems: List<String>? = null
)

data class ExtractionInfo(
    val strategy: String = "BACKEND",
    val confidence: Double = 0.0,
    val ocrQualityScore: Double = 0.0
)

data class RemoteItem(
    val id: String? = null,
    val title: String? = null,
    val summary: String? = null,
    val category: String? = null,
    val deadline: String? = null,
    val eventDate: String? = null,
    val state: String? = null,
    val organization: String? = null,
    val source: String? = null,
    val sourceUrl: String? = null,
    val originalMediaUri: String? = null,
    val checklist: List<String>? = null,
    val metadata: Map<String, Any>? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

data class QuotaResponse(
    val success: Boolean = false,
    val quota: QuotaInfo? = null
)

data class QuotaInfo(
    val tier: String = "free",
    val limit: Int = 0,
    val remaining: Int = 0,
    val resetInSeconds: Int = 0
)

data class UserProfileResponse(
    val success: Boolean = false,
    val user: UserInfo? = null,
    val subscription: SubscriptionInfo? = null,
    val quota: QuotaInfo? = null,
    @SerializedName("userTier")
    val flatUserTier: String? = null,
    @SerializedName("userId")
    val flatUserId: String? = null,
    @SerializedName("isProUser")
    private val rawIsProUser: Boolean? = null,
    @SerializedName("quotaLimit")
    private val flatQuotaLimit: Int = -1,
    @SerializedName("quotaRemaining")
    private val flatQuotaRemaining: Int = -1,
    val message: String? = null
) {
    val tier: String
        get() = subscription?.tier ?: flatUserTier ?: quota?.tier ?: "free"

    val isProUser: Boolean
        get() {
            val currentTier = tier.lowercase()
            val isProTier = (currentTier == "premium" || currentTier == "pro")
            if (!isProTier) return false

            val status = subscription?.status?.lowercase()
            if (status != null) {
                return status == "active" || status == "trialing"
            }

            return rawIsProUser == true
        }

    val quotaLimit: Int
        get() = if (quota?.limit != null && quota.limit >= 0) quota.limit else flatQuotaLimit

    val quotaRemaining: Int
        get() = if (quota?.remaining != null && quota.remaining >= 0) quota.remaining else flatQuotaRemaining

    val userId: String?
        get() = user?.uid ?: flatUserId
}

data class UserInfo(
    val uid: String? = null,
    val email: String? = null,
    val name: String? = null
)

data class SubscriptionInfo(
    val tier: String? = null,
    val status: String? = null,
    val productId: String? = null,
    val expiryTimeMillis: Long? = null
)


data class VerifyPurchaseRequest(
    val purchaseToken: String,
    val productId: String,
    val userId: String? = null
)

data class VerifyPurchaseResponse(
    val success: Boolean = false,
    val message: String? = null,
    val entitlement: EntitlementInfo? = null
)

data class EntitlementInfo(
    val tier: String? = null,
    val status: String? = null
)

data class SyncDeltaResponse(
    val success: Boolean = false,
    val syncTimestamp: String? = null,
    val updatedItems: List<StoredReminderItem>? = emptyList(),
    val deletedItemIds: List<String>? = emptyList(),
    val message: String? = null
)

