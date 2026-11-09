package com.studentmarketplace.data.repository

import com.studentmarketplace.data.model.ApiListing
import com.studentmarketplace.data.remote.ListingApiClient

object ListingRepository {
    suspend fun getListings(): List<ApiListing> = ListingApiClient.service.getListings()

    suspend fun getListingById(id: Int): ApiListing =
        ListingApiClient.service.getListingById(id)

    suspend fun getListingImage(id: Int): ByteArray =
        ListingApiClient.service.getListingImage(id).use { it.bytes() }
}