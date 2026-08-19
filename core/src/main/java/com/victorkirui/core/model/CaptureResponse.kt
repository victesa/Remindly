package com.victorkirui.core.model

data class CaptureResponse(
    val item: RemoteItem,
    val extraction: ExtractionInfo? = null
)

data class ExtractionInfo(
    val strategy: String,
    val confidence: Double,
    val ocrQualityScore: Double
)

data class RemoteItem(
    val id: String,
    val title: String,
    val summary: String?,
    val category: String?,
    val deadline: String?,
    val eventDate: String?,
    val state: String,
    val organization: String? = null,
    val source: String? = null,
    val sourceUrl: String? = null,
    val originalMediaUri: String? = null,
    val metadata: Map<String, Any>,
    val createdAt: String,
    val updatedAt: String
)
