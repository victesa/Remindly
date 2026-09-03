package com.victorkirui.module_features.capturing.ai

import android.net.Uri

interface AiExtractor {
    suspend fun extract(input: AiInput): AiResult
}

data class AiInput(
    val text: String?,
    val url: String? = null,
    val mediaUri: Uri?,
    val contentType: String,
    val idempotencyKey: String
)

sealed class AiResult {
    data class Success(
        val id: String? = null,
        val title: String,
        val summary: String?,
        val category: String,
        val deadline: String?,
        val eventDate: String?,
        val organization: String?,
        val strategy: String,
        val checklist: List<String>? = null,
        val isReplayed: Boolean = false
    ) : AiResult()
    data class Error(
        val message: String,
        val isTransient: Boolean = false
    ) : AiResult()
}
