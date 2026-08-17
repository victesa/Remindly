package com.victorkirui.remote

import com.victorkirui.core.model.CaptureRequest
import com.victorkirui.core.model.CaptureResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.PartMap

interface CaptureApi {
    @POST("v1/items/ingest")
    suspend fun captureJson(@Body request: CaptureRequest): CaptureResponse

    @Multipart
    @POST("v1/items/ingest")
    suspend fun captureMultipartV2(
        @PartMap parts: Map<String, @JvmSuppressWildcards RequestBody>,
        @Part media: MultipartBody.Part
    ): CaptureResponse
}
