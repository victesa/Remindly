package com.victorkirui.remote

import com.victorkirui.core.model.GetItemsResponse
import com.victorkirui.core.model.StoredReminderItem
import com.victorkirui.core.model.SyncDeltaResponse
import com.victorkirui.core.model.UserProfileResponse
import com.victorkirui.core.model.VerifyPurchaseRequest
import com.victorkirui.core.model.VerifyPurchaseResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface CaptureApi {
    @GET("v1/me")
    suspend fun getMe(): UserProfileResponse

    @GET("v1/items")
    suspend fun getItems(
        @Query("limit") limit: Int? = null
    ): GetItemsResponse


    @GET("v1/items/sync")
    suspend fun getSyncDeltas(
        @Query("since") since: String?
    ): SyncDeltaResponse

    @PATCH("v1/items/{id}")
    suspend fun updateItem(
        @Path("id") id: String,
        @Body updates: Map<String, @JvmSuppressWildcards Any?>
    ): StoredReminderItem

    @DELETE("v1/items/{id}")
    suspend fun deleteItem(@Path("id") id: String): Response<Unit>

    @POST("v1/account/delete")
    suspend fun purgeAccount(): Response<Unit>

    @POST("v1/billing/verify-purchase")
    suspend fun verifyPurchase(
        @Body request: VerifyPurchaseRequest
    ): VerifyPurchaseResponse
}
