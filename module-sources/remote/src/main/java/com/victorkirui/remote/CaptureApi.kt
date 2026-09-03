package com.victorkirui.remote

import com.victorkirui.core.model.CaptureRequest
import com.victorkirui.core.model.CaptureResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.PartMap

interface CaptureApi {
    @GET("v1/quota")
    suspend fun getQuota(): com.victorkirui.core.model.QuotaResponse

    @GET("v1/items")
    suspend fun getItems(): com.victorkirui.core.model.GetItemsResponse

    @PATCH("v1/items/{id}")
    suspend fun updateItem(
        @retrofit2.http.Path("id") id: String,
        @Body updates: Map<String, @JvmSuppressWildcards Any?>
    ): com.victorkirui.core.model.CaptureResponse

    @DELETE("v1/items/{id}")
    suspend fun deleteItem(@retrofit2.http.Path("id") id: String): retrofit2.Response<Unit>

    @POST("v1/account/delete")
    suspend fun purgeAccount(): retrofit2.Response<Unit>
}
