package com.victorkirui.remote.di

import android.util.Log
import com.victorkirui.core.repository.AuthRepository
import com.victorkirui.remote.CaptureApi
import com.victorkirui.remote.CaptureApiService
import com.victorkirui.remote.CaptureApiServiceImpl
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
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

        OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .addInterceptor { chain ->
                val getAuthToken = { force: Boolean -> runBlocking { authRepository.getIdToken(force) } }
                var token = getAuthToken(false)
                
                val buildRequest = { authToken: String? ->
                    val builder = chain.request().newBuilder()
                        .addHeader("X-Tunnel-Skip-Anti-Phishing-Page", "true")
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

                if (response.code == 402) {
                    val bodyString = response.peekBody(1024 * 1024).string()
                    Log.e("RemoteModule", "402 Payment Required detected! Response Body: $bodyString")
                }

                Log.d("RemoteModule", "Received response for ${response.request.url}: -> ${response.code}")
                response
            }
            .build()
    }

    single<Retrofit> {
        val baseUrl = "https://remindly-backend.victorkirui-dev.workers.dev/"
        Log.d("RemoteModule", "Base URL: $baseUrl")
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .client(get())
            .build()
    }

    single { get<Retrofit>().create(CaptureApi::class.java) }
    single<CaptureApiService> { CaptureApiServiceImpl(get(), androidContext()) }
}
