package com.studentmarketplace.data.model

import com.google.gson.annotations.SerializedName

data class PurchaseRequest(
    @SerializedName("request_id") val requestId: Int,
    @SerializedName("listing_id") val listingId: Int,
    @SerializedName("buyer_id") val buyerId: Int,
    @SerializedName("seller_id") val sellerId: Int,
    val status: String,
    @SerializedName("date_requested") val dateRequested: String? = null,
    @SerializedName("date_updated") val dateUpdated: String? = null,
    val title: String? = null,
    val price: Double? = null
)

data class CreatePurchaseRequestBody(
    @SerializedName("listing_id") val listingId: Int
)

data class PurchaseRequestState(
    @SerializedName("request_id") val requestId: Int,
    val status: String
)