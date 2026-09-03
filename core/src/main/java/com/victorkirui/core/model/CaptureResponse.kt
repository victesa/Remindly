package com.victorkirui.core.model

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
    val extractedAt: String? = null,
    val sourceType: String? = null,
    val inputSnippet: String? = null,
    val persistedSource: String? = null,
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
