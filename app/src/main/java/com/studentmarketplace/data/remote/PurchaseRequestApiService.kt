package com.studentmarketplace.data.remote

import com.studentmarketplace.data.model.CreatePurchaseRequestBody
import com.studentmarketplace.data.model.PurchaseRequest
import com.studentmarketplace.data.model.PurchaseRequestState
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.GET
import retrofit2.http.Path

interface PurchaseRequestApiService {
    @POST("purchase-requests")
    suspend fun createRequest(@Body body: CreatePurchaseRequestBody): PurchaseRequest

    @GET("purchase-requests/buyer")
    suspend fun getBuyerRequests(): List<PurchaseRequest>

    @GET("purchase-requests/seller")
    suspend fun getSellerRequests(): List<PurchaseRequest>

    @PATCH("purchase-requests/{requestId}/accept")
    suspend fun acceptRequest(@Path("requestId") requestId: Int): PurchaseRequestState

    @PATCH("purchase-requests/{requestId}/decline")
    suspend fun declineRequest(@Path("requestId") requestId: Int): PurchaseRequestState

    @PATCH("purchase-requests/{requestId}/cancel")
    suspend fun cancelRequest(@Path("requestId") requestId: Int): PurchaseRequestState
}

object PurchaseRequestApiClient {
    val service: PurchaseRequestApiService by lazy {
        ListingApiClient.retrofit.create(PurchaseRequestApiService::class.java)
    }
}