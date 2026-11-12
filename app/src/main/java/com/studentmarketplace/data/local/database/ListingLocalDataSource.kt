package com.studentmarketplace.data.local.database

import kotlinx.coroutines.flow.Flow

class ListingLocalDataSource(
    private val listingDao: ListingDao
) {

    fun getAllListings(): Flow<List<ListingEntity>> {
        return listingDao.getAllListings()
    }

    suspend fun saveListings(listings: List<ListingEntity>) {
        listingDao.saveListings(listings)
    }

    suspend fun getListingById(id: Int): ListingEntity? {
        return listingDao.getListingById(id)
    }

    suspend fun clearListings() {
        listingDao.clearListings()
    }
}