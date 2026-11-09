package com.studentmarketplace.data.model

import com.google.gson.annotations.SerializedName

data class ApiListing(
    @SerializedName("listing_id") val listingId: Int,
    @SerializedName("seller_id") val sellerId: Int,
    val title: String,
    val description: String,
    val price: Double,
    @SerializedName("condition") val itemCondition: String,
    val category: String,
    @SerializedName("module_code") val moduleCode: String?,
    val status: String,
    @SerializedName("date_created") val dateCreated: String,
    @SerializedName("has_image") val hasImage: Int = 0
)