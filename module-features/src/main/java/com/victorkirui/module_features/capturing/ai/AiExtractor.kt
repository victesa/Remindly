package com.victorkirui.module_features.capturing.ai

import android.net.Uri

interface AiExtractor {
    suspend fun extract(input: AiInput): AiResult
}

data class AiInput(
    val text: String?,
    val mediaUri: Uri?,
    val contentType: String,
    val idempotencyKey: String
)

sealed class AiResult {
    data class Success(
        val title: String,
        val summary: String?,
        val category: String,
        val deadline: String?,
        val eventDate: String?,
        val organization: String?,
        val strategy: String,
        val isReplayed: Boolean = false
    ) : AiResult()
    data class Error(val message: String) : AiResult()
}
