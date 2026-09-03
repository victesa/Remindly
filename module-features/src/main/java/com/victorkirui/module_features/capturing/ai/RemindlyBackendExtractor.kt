package com.victorkirui.module_features.capturing.ai

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import com.victorkirui.core.repository.SubscriptionRepository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.InputStream
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class RemindlyBackendExtractor(
    private val context: Context,
    private val subscriptionRepository: SubscriptionRepository,
    private val backendBaseUrl: String = "https://remindly-backend-v2.victorkirui-dev.workers.dev"
) : AiExtractor {

    private val gson = Gson()
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val activeRequests = ConcurrentHashMap.newKeySet<String>()
    private val MAX_RETRIES = 2

    override suspend fun extract(input: AiInput): AiResult = withContext(Dispatchers.IO) {
        // 1. Single-flight guard
        if (!activeRequests.add(input.idempotencyKey)) {
            Log.d("RemindlyBackend", "Single-flight guard: Request for key ${input.idempotencyKey} is already in-flight.")
            return@withContext AiResult.Error("Analysis already in progress.", isTransient = true)
        }

        try {
            var lastError = "Extraction failed"
            val currentUser = FirebaseAuth.getInstance().currentUser
            val tier = subscriptionRepository.subscriptionTier.first()
            val currentDate = Instant.now().toString()
            val timezone = ZoneId.systemDefault().id

            for (attempt in 0..MAX_RETRIES) {
                try {
                    if (attempt > 0) {
                        Log.d("RemindlyBackend", "Retry attempt $attempt for key: ${input.idempotencyKey}")
                    }

                    val idToken = try {
                        currentUser?.getIdToken(false)?.await()?.token
                    } catch (e: Exception) {
                        null
                    }

                    val requestBuilder = Request.Builder()
                        .url("$backendBaseUrl/v1/extract-data")
                        .addHeader("Idempotency-Key", input.idempotencyKey)
                        .addHeader("X-User-Tier", tier.name.lowercase())
                        .addHeader("X-Client-Date", currentDate)
                        .addHeader("X-User-Timezone", timezone)
                        .addHeader("CF-Access-Client-Id", com.victorkirui.core.BuildConfig.CF_ACCESS_CLIENT_ID)
                        .addHeader("CF-Access-Client-Secret", com.victorkirui.core.BuildConfig.CF_ACCESS_CLIENT_SECRET)

                    if (idToken != null) {
                        requestBuilder.addHeader("Authorization", "Bearer $idToken")
                    }

                    val request: Request = if (input.mediaUri != null) {
                        // MULTIPART request
                        val multipartBuilder = MultipartBody.Builder().setType(MultipartBody.FORM)
                        input.text?.let { multipartBuilder.addFormDataPart("text", it) }
                        input.url?.let { multipartBuilder.addFormDataPart("url", it) }
                        multipartBuilder.addFormDataPart("idempotencyKey", input.idempotencyKey)
                        multipartBuilder.addFormDataPart("currentDate", currentDate)
                        multipartBuilder.addFormDataPart("timezone", timezone)

                        val bytes = readUriBytes(input.mediaUri)
                        if (bytes != null) {
                            val mimeType = context.contentResolver.getType(input.mediaUri) ?: "image/jpeg"
                            val imageBody = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
                            multipartBuilder.addFormDataPart("image", "capture.jpg", imageBody)
                        }
                        requestBuilder.post(multipartBuilder.build()).build()
                    } else {
                        // JSON request
                        val jsonMap = mutableMapOf<String, Any?>(
                            "idempotencyKey" to input.idempotencyKey,
                            "currentDate" to currentDate,
                            "timezone" to timezone
                        )
                        input.text?.let { jsonMap["text"] = it }
                        input.url?.let { jsonMap["url"] = it }
                        
                        val jsonBody = gson.toJson(jsonMap).toRequestBody("application/json".toMediaTypeOrNull())
                        requestBuilder.post(jsonBody).build()
                    }

                    httpClient.newCall(request).execute().use { response ->
                        val responseBody = response.body?.string() ?: ""
                        val isReplayed = response.header("Idempotency-Replayed") == "true"
                        val retryAfter = response.header("Retry-After")?.toLongOrNull()
                        val requestId = response.header("X-Request-Id")
                        
                        val rateLimit = response.header("X-RateLimit-Limit")?.toIntOrNull()
                        val rateRemaining = response.header("X-RateLimit-Remaining")?.toIntOrNull()

                        if (rateLimit != null && rateRemaining != null) {
                            subscriptionRepository.updateQuota(rateLimit, rateRemaining)
                        }

                        Log.d("RemindlyBackend", "Status: ${response.code}, RequestId: $requestId, Replayed: $isReplayed")

                        when (response.code) {
                            200 -> {
                                var cleanString = responseBody.trim()
                                if (cleanString.startsWith("\uFEFF")) cleanString = cleanString.substring(1).trim()

                                val contentTypeStr = response.header("Content-Type") ?: ""
                                val isHtml = contentTypeStr.contains("text/html", ignoreCase = true) ||
                                        cleanString.startsWith("<") ||
                                        cleanString.startsWith("<!DOCTYPE", ignoreCase = true)

                                if (isHtml) {
                                    Log.e("RemindlyBackend", "Cloudflare Access / Auth error: Backend returned HTML login page instead of JSON")
                                    return@withContext AiResult.Error("Authentication failed: Cloudflare Access login page returned.", isTransient = false)
                                }

                                while (cleanString.startsWith("\"") && cleanString.endsWith("\"") && cleanString.length > 2) {
                                    try {
                                        val reader = com.google.gson.stream.JsonReader(java.io.StringReader(cleanString))
                                        reader.isLenient = true
                                        @Suppress("DEPRECATION")
                                        val parsed = com.google.gson.JsonParser().parse(reader)
                                        if (parsed.isJsonPrimitive && parsed.asJsonPrimitive.isString) {
                                            var unwrapped = parsed.asString
                                            if (unwrapped.startsWith("\uFEFF")) unwrapped = unwrapped.substring(1)
                                            unwrapped = unwrapped.trim()
                                            if (unwrapped == cleanString) break
                                            cleanString = unwrapped
                                        } else break
                                    } catch (e: Exception) { break }
                                }

                                val apiResponse = gson.fromJson(cleanString, BackendApiResponse::class.java)
                                apiResponse.quota?.let { q ->
                                    subscriptionRepository.updateQuota(q.limit, q.remaining)
                                }

                                if (apiResponse.success && apiResponse.data != null) {
                                    val data = apiResponse.data
                                    return@withContext AiResult.Success(
                                        id = data.id,
                                        title = data.title ?: "Untitled",
                                        summary = data.summary,
                                        category = data.category ?: "OTHER",
                                        deadline = data.deadline,
                                        eventDate = data.eventDate,
                                        organization = data.organization,
                                        strategy = data.strategy ?: "BACKEND",
                                        checklist = data.checklist,
                                        isReplayed = isReplayed
                                    )
                                }
                                lastError = "Invalid server response data."
                            }
                            429, 503, 504 -> {
                                if (attempt < MAX_RETRIES) {
                                    val backoffMs = calculateBackoff(attempt, retryAfter)
                                    delay(backoffMs)
                                    lastError = "Server busy (${response.code})"
                                } else {
                                    return@withContext AiResult.Error("Server busy or unavailable. Please try again later.", isTransient = true)
                                }
                            }
                            401 -> return@withContext AiResult.Error("Authentication failed. Please sign in again.", isTransient = false)
                            400, 403, 404, 422 -> {
                                val apiResponse = try { gson.fromJson(responseBody, BackendApiResponse::class.java) } catch(e:Exception) { null }
                                return@withContext AiResult.Error(apiResponse?.error?.message ?: "Request failed (${response.code})", isTransient = false)
                            }
                            else -> {
                                return@withContext AiResult.Error("Server error (${response.code})", isTransient = true)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("RemindlyBackend", "Request failed attempt $attempt", e)
                    lastError = e.message ?: "Connection error"
                    val shouldRetry = e is SocketTimeoutException || e is ConnectException || e is UnknownHostException
                    if (shouldRetry && attempt < MAX_RETRIES) {
                        delay(calculateBackoff(attempt, null))
                    } else {
                        return@withContext AiResult.Error(lastError, isTransient = true)
                    }
                }
            }
            AiResult.Error(lastError, isTransient = true)
        } finally {
            activeRequests.remove(input.idempotencyKey)
        }
    }

    private fun calculateBackoff(attempt: Int, retryAfterSeconds: Long?): Long {
        if (retryAfterSeconds != null) return retryAfterSeconds * 1000
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
        val error: ErrorPayload?,
        val metadata: Map<String, Any>?
    )

    private data class ExtractedPayload(
        val id: String?,
        val title: String?,
        val summary: String?,
        val category: String?,
        val deadline: String?,
        val eventDate: String?,
        val organization: String?,
        val strategy: String?,
        val checklist: List<String>?,
        val extractedAt: String?
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
