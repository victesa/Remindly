package com.victorkirui.core.model

data class CaptureRequest(
    val itemId: String,
    val contentType: String,
    val extractedText: String?,
    val capturedAt: String,
    val metadata: CaptureMetadata
)

data class CaptureMetadata(
    val source: String,
    val timezone: String
)
