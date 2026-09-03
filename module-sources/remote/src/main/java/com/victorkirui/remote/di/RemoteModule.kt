package com.victorkirui.remote.di

import android.util.Log
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import com.victorkirui.core.repository.AuthRepository
import com.victorkirui.core.repository.SubscriptionRepository
import com.victorkirui.core.repository.SubscriptionTier
import com.victorkirui.remote.CaptureApi
import com.victorkirui.remote.CaptureApiService
import com.victorkirui.remote.CaptureApiServiceImpl
import com.victorkirui.remote.StringifiedTypeAdapterFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

val remoteModule = module {
    single<OkHttpClient> {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val authRepository: AuthRepository = get()
        val subscriptionRepository: com.victorkirui.core.repository.SubscriptionRepository = get()

        OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .addInterceptor { chain ->
                val getAuthToken = { force: Boolean -> runBlocking { authRepository.getIdToken(force) } }
                val getTier = { runBlocking { subscriptionRepository.subscriptionTier.first() } }
                
                var token = getAuthToken(false)
                val tier = getTier()
                
                val buildRequest = { authToken: String? ->
                    val builder = chain.request().newBuilder()
                        .addHeader("X-Tunnel-Skip-Anti-Phishing-Page", "true")
                        .addHeader("X-User-Tier", tier.name.lowercase())
                        .addHeader("CF-Access-Client-Id", com.victorkirui.core.BuildConfig.CF_ACCESS_CLIENT_ID)
                        .addHeader("CF-Access-Client-Secret", com.victorkirui.core.BuildConfig.CF_ACCESS_CLIENT_SECRET)
                    
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        builder.addHeader("X-Client-Date", java.time.Instant.now().toString())
                        builder.addHeader("X-User-Timezone", java.time.ZoneId.systemDefault().id)
                    }

                    if (authToken != null) {
                        builder.addHeader("Authorization", "Bearer $authToken")
                    }
                    builder.build()
                }

                val initialRequest = buildRequest(token)
                Log.d("RemoteModule", "Sending request to: ${initialRequest.url}")

                var response = chain.proceed(initialRequest)
                
                // Adhere to 401 retry contract
                if (response.code == 401) {
                    Log.w("RemoteModule", "401 Unauthorized. Retrying with fresh token...")
                    response.close()
                    token = getAuthToken(true) // Force refresh
                    val retryRequest = buildRequest(token)
                    response = chain.proceed(retryRequest)
                }

                val responseBody = response.body
                if (response.code !in 200..299) {
                    if (responseBody != null) {
                        try {
                            val source = responseBody.source()
                            source.request(Long.MAX_VALUE)
                            val bodyString = source.buffer.clone().readUtf8()
                            Log.e("RemoteModule", "HTTP Error ${response.code} for ${response.request.url}: $bodyString")
                        } catch (e: Exception) {
                            Log.w("RemoteModule", "Failed to log error body: ${e.message}")
                        }
                    }
                } else if (responseBody != null) {
                    try {
                        val contentType = responseBody.contentType()
                        val source = responseBody.source()
                        source.request(Long.MAX_VALUE)
                        val rawBodyString = source.buffer.clone().readUtf8()
                        Log.d("RemoteModule", "Raw response body for ${response.request.url}: $rawBodyString")

                        var cleanString = if (rawBodyString.startsWith("\uFEFF")) rawBodyString.substring(1) else rawBodyString
                        cleanString = cleanString.trim()

                        val contentTypeStr = contentType?.toString() ?: ""
                        val isHtml = contentTypeStr.contains("text/html", ignoreCase = true) ||
                                cleanString.startsWith("<") ||
                                cleanString.startsWith("<!DOCTYPE", ignoreCase = true)

                        if (isHtml) {
                            Log.e("RemoteModule", "Cloudflare Access / Auth error: Received HTML login page instead of JSON for ${response.request.url}")
                            response = response.newBuilder()
                                .code(401)
                                .message("Cloudflare Access authentication required")
                                .body("{\"error\": \"Cloudflare Access authentication required\", \"success\": false}".toResponseBody("application/json".toMediaTypeOrNull()))
                                .build()
                        } else {
                            while (cleanString.startsWith("\"") && cleanString.endsWith("\"") && cleanString.length > 2) {
                                try {
                                    val reader = com.google.gson.stream.JsonReader(java.io.StringReader(cleanString))
                                    reader.isLenient = true
                                    @Suppress("DEPRECATION")
                                    val parsed = JsonParser().parse(reader)
                                    if (parsed.isJsonPrimitive && parsed.asJsonPrimitive.isString) {
                                        var unwrapped = parsed.asString
                                        if (unwrapped.startsWith("\uFEFF")) unwrapped = unwrapped.substring(1)
                                        unwrapped = unwrapped.trim()
                                        if (unwrapped == cleanString) break
                                        cleanString = unwrapped
                                    } else {
                                        break
                                    }
                                } catch (e: Exception) {
                                    Log.w("RemoteModule", "Failed to unwrap stringified step for ${response.request.url}: ${e.message}")
                                    break
                                }
                            }

                            if (cleanString != rawBodyString && (cleanString.startsWith("{") || cleanString.startsWith("["))) {
                                Log.d("RemoteModule", "Cleaned/Unwrapped JSON response body for ${response.request.url}: $cleanString")
                                val newBody = cleanString.toResponseBody(contentType ?: "application/json".toMediaTypeOrNull())
                                response = response.newBuilder().body(newBody).build()
                            }
                        }
                    } catch (e: Exception) {
                        Log.w("RemoteModule", "Failed to clean response body: ${e.message}")
                    }
                }

                Log.d("RemoteModule", "Received response for ${response.request.url}: -> ${response.code}")
                response
            }
            .build()
    }

    single<Retrofit> {
        val gson = GsonBuilder()
            .registerTypeAdapterFactory(StringifiedTypeAdapterFactory())
            .create()
        val baseUrl = "https://remindly-backend-v2.victorkirui-dev.workers.dev/"
        Log.d("RemoteModule", "Base URL: $baseUrl")
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .client(get())
            .build()
    }

    single { get<Retrofit>().create(CaptureApi::class.java) }
    single<CaptureApiService> { CaptureApiServiceImpl(get(), androidContext()) }
    single<com.victorkirui.core.data.DataClearer> {
        object : com.victorkirui.core.data.DataClearer {
            override suspend fun clearUserData(userId: String) {
                try {
                    val api: CaptureApiService = get()
                    val success = api.purgeAccount()
                    Log.d("RemoteModule", "Purged remote account data for $userId: success=$success")
                } catch (e: Exception) {
                    Log.w("RemoteModule", "Failed to purge remote account data for $userId: ${e.message}")
                }
            }
        }
    }
}
