package com.studentmarketplace.data.repository

import com.studentmarketplace.data.model.CreatePurchaseRequestBody
import com.studentmarketplace.data.model.PurchaseRequest
import com.studentmarketplace.data.model.PurchaseRequestState
import com.studentmarketplace.data.remote.PurchaseRequestApiClient

object PurchaseRequestRepository {
    suspend fun createRequest(listingId: Int): PurchaseRequest =
        PurchaseRequestApiClient.service.createRequest(CreatePurchaseRequestBody(listingId))

    suspend fun getBuyerRequests(): List<PurchaseRequest> =
        PurchaseRequestApiClient.service.getBuyerRequests()

    suspend fun getSellerRequests(): List<PurchaseRequest> =
        PurchaseRequestApiClient.service.getSellerRequests()

    suspend fun acceptRequest(requestId: Int): PurchaseRequestState =
        PurchaseRequestApiClient.service.acceptRequest(requestId)

    suspend fun declineRequest(requestId: Int): PurchaseRequestState =
        PurchaseRequestApiClient.service.declineRequest(requestId)

    suspend fun cancelRequest(requestId: Int): PurchaseRequestState =
        PurchaseRequestApiClient.service.cancelRequest(requestId)
}