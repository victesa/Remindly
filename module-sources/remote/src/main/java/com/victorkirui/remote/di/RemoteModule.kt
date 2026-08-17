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
    single {
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
                val token = runBlocking { authRepository.getIdToken() }
                val requestBuilder = chain.request().newBuilder()
                    .addHeader("X-Tunnel-Skip-Anti-Phishing-Page", "true")
                
                if (token != null) {
                    requestBuilder.addHeader("Authorization", "Bearer $token")
                }
                
                val request = requestBuilder.build()
                Log.d("RemoteModule", "Sending request to: ${request.url}")

                try {
                    val response = chain.proceed(request)
                    Log.d("RemoteModule", "Received response for ${response.request.url}: ${response.code}")
                    response
                } catch (e: Exception) {
                    Log.e("RemoteModule", "Request failed for ${request.url}: ${e.message}", e)
                    throw e
                }
            }
            .build()
    }

    single {
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
