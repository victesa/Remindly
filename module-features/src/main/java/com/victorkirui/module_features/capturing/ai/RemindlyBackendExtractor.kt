package com.victorkirui.module_features.capturing.ai

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.InputStream
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class RemindlyBackendExtractor(
    private val context: Context,
    private val backendBaseUrl: String = "https://8wzmrlnc-3000.uks1.devtunnels.ms"
) : AiExtractor {

    private val gson = Gson()
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val MAX_RETRIES = 2

    override suspend fun extract(input: AiInput): AiResult = withContext(Dispatchers.IO) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            return@withContext AiResult.Error("User is not authenticated. Please sign in.")
        }

        var lastError = "Extraction failed"
        
        for (attempt in 0..MAX_RETRIES) {
            try {
                if (attempt > 0) {
                    Log.d("RemindlyBackend", "Retry attempt $attempt for key: ${input.idempotencyKey}")
                }

                val idTokenResult = currentUser.getIdToken(false).await()
                val idToken = idTokenResult.token ?: throw Exception("Failed to retrieve Firebase ID Token")

                val multipartBuilder = MultipartBody.Builder().setType(MultipartBody.FORM)
                input.text?.takeIf { it.isNotBlank() }?.let {
                    multipartBuilder.addFormDataPart("text", it)
                }

                input.mediaUri?.let { uri ->
                    val bytes = readUriBytes(uri)
                    if (bytes != null) {
                        val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
                        val requestBody = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
                        multipartBuilder.addFormDataPart("image", "document_upload.jpg", requestBody)
                    }
                }

                val request = Request.Builder()
                    .url("$backendBaseUrl/v1/extract-data")
                    .addHeader("Authorization", "Bearer $idToken")
                    .addHeader("Idempotency-Key", input.idempotencyKey)
                    .post(multipartBuilder.build())
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string() ?: ""
                    val isReplayed = response.header("Idempotency-Replayed") == "true"
                    val retryAfter = response.header("Retry-After")?.toLongOrNull()

                    Log.d("RemindlyBackend", "Status: ${response.code}, Body: $responseBody")

                    when (response.code) {
                        200 -> {
                            val apiResponse = gson.fromJson(responseBody, BackendApiResponse::class.java)
                            if (apiResponse.success && apiResponse.data != null) {
                                val data = apiResponse.data
                                return@withContext AiResult.Success(
                                    title = data.title,
                                    summary = data.summary,
                                    category = data.category ?: "OTHER",
                                    deadline = data.deadline,
                                    eventDate = data.eventDate,
                                    organization = data.organization,
                                    strategy = data.strategy ?: "BACKEND",
                                    isReplayed = isReplayed
                                )
                            }
                            lastError = "Invalid server response data."
                        }
                        429, 503, 504 -> {
                            // Transient failures - handle retry logic
                            if (attempt < MAX_RETRIES) {
                                val backoffMs = calculateBackoff(attempt, retryAfter)
                                delay(backoffMs)
                                lastError = "Server busy (${response.code})"
                                // continue loop
                            } else {
                                return@withContext AiResult.Error("Daily limit reached or server unavailable. Please try again later.")
                            }
                        }
                        401 -> return@withContext AiResult.Error("Authentication failed. Please sign in again.")
                        400, 403, 404, 422 -> {
                            // Permanent failures - do not retry
                            val apiResponse = try { gson.fromJson(responseBody, BackendApiResponse::class.java) } catch(e:Exception) { null }
                            return@withContext AiResult.Error(apiResponse?.error?.message ?: "Request failed (${response.code})")
                        }
                        else -> {
                            lastError = "Server error (${response.code})"
                            if (attempt < MAX_RETRIES) {
                                delay(calculateBackoff(attempt, null))
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("RemindlyBackend", "Request failed attempt $attempt", e)
                lastError = e.message ?: "Connection error"
                if (attempt < MAX_RETRIES) {
                    delay(calculateBackoff(attempt, null))
                }
            }
        }

        AiResult.Error(lastError)
    }

    private fun calculateBackoff(attempt: Int, retryAfterSeconds: Long?): Long {
        if (retryAfterSeconds != null) return retryAfterSeconds * 1000
        
        // Attempt 0 (immediate) -> next is attempt 1
        // Attempt 1: 500-1500ms
        // Attempt 2: 1500-3500ms
        val base = if (attempt == 0) 1000L else 2500L
        val jitter = Random.nextLong(-500, 500)
        return base + jitter
    }

    private fun readUriBytes(uri: Uri): ByteArray? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            inputStream?.use { it.readBytes() }
        } catch (e: Exception) {
            null
        }
    }

    private data class BackendApiResponse(
        val success: Boolean,
        val data: ExtractedPayload?,
        val quota: QuotaPayload?,
        val error: ErrorPayload?
    )

    private data class ExtractedPayload(
        val title: String,
        val summary: String?,
        val category: String?,
        val deadline: String?,
        val eventDate: String?,
        val organization: String?,
        val strategy: String?
    )

    private data class QuotaPayload(
        val limit: Int,
        val remaining: Int,
        val resetInSeconds: Int
    )

    private data class ErrorPayload(
        val code: String,
        val message: String
    )
}
